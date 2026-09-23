package com.phantomaddons.data.supply;

import com.phantomaddons.data.GameplayDatabase;
import com.phantomaddons.data.RunRecorder;
import com.phantomaddons.data.tentacle.TentacleTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GrabTracker {

    private static final double HORIZONTAL_MOVE_EPSILON = 0.01; // blocks/tick: above float noise, below any real intentional movement
    private static final long MOTION_BUFFER_TICKS = 60; // ~3s of trailing anchor motion kept per live tentacle
    private static final long GRAB_LINK_WINDOW_TICKS = 10; // a mount starting within this many ticks of the anchor moving is attributed to it
    private static final double FALLBACK_MATCH_RADIUS = 6.0; // blocks: cap on the "nearest tentacle" fallback so a far-off tentacle never gets blamed for an unrelated mount
    private static final long ORPHAN_ATTACH_WINDOW_TICKS = 200; // ~10s: a grab that starts before any pickup attempt exists (e.g. grabbed while walking up to the crate) still gets attributed once one starts shortly after

    private record MotionSample(long tick, double x, double y, double z, double distanceToPlayer, double horizSpeed, int chainSize) {}
    private record PendingLink(long reachId, long expireTick) {}
    private record OrphanGrab(long grabId, long tick) {}

    private static final Map<Long, Deque<MotionSample>> motionBuffers = new HashMap<>();
    private static final Map<Long, Vec3> lastAnchorPos = new HashMap<>();
    private static final Map<Long, Boolean> movingState = new HashMap<>();
    private static final Map<Long, PendingLink> pendingGrabLinks = new HashMap<>();
    private static final List<OrphanGrab> orphanGrabs = new java.util.ArrayList<>();

    private static Long activeGrabId = null;
    private static Long activeGrabInstanceId = null;
    private static long lastKnownTick = 0;

    private GrabTracker() {}

    public static Long getActiveGrabId() { return activeGrabId; }

    public static void reset() {
        if (activeGrabId != null) {
            GameplayDatabase.update(
                    "UPDATE grab_events SET mount_end_tick = ?, mount_ended_at = ? WHERE grab_id = ? AND mount_end_tick IS NULL",
                    lastKnownTick, GameplayDatabase.now(), activeGrabId);
        }
        motionBuffers.clear();
        lastAnchorPos.clear();
        movingState.clear();
        pendingGrabLinks.clear();
        orphanGrabs.clear();
        activeGrabId = null;
        activeGrabInstanceId = null;
        PearlCancelLogger.onGrabEnd();
    }

    public static void tick(Minecraft client, long tick) {
        Player player = client.player;
        lastKnownTick = tick;
        Long attemptId = SupplyAttemptTracker.getActiveAttemptId();
        Long runId = RunRecorder.getActiveRunId();

        if (runId != null) {
            trackAnchorMovement(player, runId, attemptId, tick);
        }
        pendingGrabLinks.entrySet().removeIf(e -> tick > e.getValue().expireTick());

        Entity vehicle = player.getVehicle();
        boolean mountedNow = player.isPassenger() && vehicle != null && !(vehicle instanceof ArmorStand);
        if (mountedNow && activeGrabId == null) {
            onGrabStart(player, attemptId, tick);
        } else if (!mountedNow && activeGrabId != null) {
            onGrabEnd(player, tick);
        }
    }

    private static void trackAnchorMovement(Player player, long runId, Long attemptId, long tick) {
        Vec3 playerPos = player.position();

        for (TentacleTracker.TentacleSnapshot snap : TentacleTracker.getLiveInstances()) {
            long instanceId = snap.instanceId();
            Vec3 anchorPos = snap.anchorPos();

            Vec3 prev = lastAnchorPos.put(instanceId, anchorPos);
            double horizSpeed = prev == null ? 0.0 : Math.hypot(anchorPos.x - prev.x, anchorPos.z - prev.z);

            Deque<MotionSample> buffer = motionBuffers.computeIfAbsent(instanceId, k -> new ArrayDeque<>());
            buffer.addLast(new MotionSample(tick, anchorPos.x, anchorPos.y, anchorPos.z,
                    anchorPos.distanceTo(playerPos), horizSpeed, snap.chainSize()));
            while (!buffer.isEmpty() && tick - buffer.peekFirst().tick() > MOTION_BUFFER_TICKS) buffer.pollFirst();

            if (prev == null) continue;

            boolean movingNow = horizSpeed > HORIZONTAL_MOVE_EPSILON;
            boolean wasMoving = Boolean.TRUE.equals(movingState.put(instanceId, movingNow));
            if (!movingNow || wasMoving) continue;

            long reachId = GameplayDatabase.insert(
                    "INSERT INTO tentacle_reach_attempts (run_id, instance_id, attempt_id, reached_tick, reached_at, " +
                            "tentacle_x, tentacle_y, tentacle_z, tentacle_speed, tentacle_chain_size, " +
                            "player_x, player_y, player_z, player_yaw, player_pitch) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    runId, instanceId, attemptId, tick, GameplayDatabase.now(),
                    anchorPos.x, anchorPos.y, anchorPos.z, horizSpeed, snap.chainSize(),
                    player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());

            for (MotionSample s : buffer) {
                GameplayDatabase.insert(
                        "INSERT INTO tentacle_reach_motion (reach_id, tick, x, y, z, distance_to_player, speed, chain_size) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        reachId, s.tick(), s.x(), s.y(), s.z(), s.distanceToPlayer(), s.horizSpeed(), s.chainSize());
            }

            pendingGrabLinks.put(instanceId, new PendingLink(reachId, tick + GRAB_LINK_WINDOW_TICKS));
        }
    }

    private static void onGrabStart(Player player, Long attemptId, long tick) {
        Entity vehicle = player.getVehicle();
        Long instanceId = vehicle != null ? TentacleTracker.findInstanceForEntity(vehicle.getUUID()) : null;
        if (instanceId == null) instanceId = nearestInstanceId(player);

        Long reachId = null;
        if (instanceId != null) {
            PendingLink pending = pendingGrabLinks.remove(instanceId);
            if (pending != null) {
                reachId = pending.reachId();
                GameplayDatabase.update("UPDATE tentacle_reach_attempts SET resulted_in_grab = 1 WHERE reach_id = ?", reachId);
            }
        }

        activeGrabId = GameplayDatabase.insert(
                "INSERT INTO grab_events (reach_id, attempt_id, instance_id, mount_start_tick, mount_started_at) " +
                        "VALUES (?, ?, ?, ?, ?)",
                reachId, attemptId, instanceId, tick, GameplayDatabase.now());
        activeGrabInstanceId = instanceId;
        if (attemptId == null && activeGrabId != null && activeGrabId != -1) {
            orphanGrabs.add(new OrphanGrab(activeGrabId, tick));
        }
        PearlCancelLogger.onGrabStart(activeGrabId);
    }

    public static void attachPendingAttempt(long attemptId, long tick) {
        orphanGrabs.removeIf(o -> tick - o.tick() > ORPHAN_ATTACH_WINDOW_TICKS);
        for (OrphanGrab o : orphanGrabs) {
            GameplayDatabase.update("UPDATE grab_events SET attempt_id = ? WHERE grab_id = ?", attemptId, o.grabId());
        }
        orphanGrabs.clear();
    }

    private static void onGrabEnd(Player player, long tick) {
        Vec3 vel = player.getDeltaMovement();
        GameplayDatabase.update(
                "UPDATE grab_events SET mount_end_tick = ?, mount_ended_at = ?, " +
                        "release_velocity_x = ?, release_velocity_y = ?, release_velocity_z = ? WHERE grab_id = ?",
                tick, GameplayDatabase.now(), vel.x, vel.y, vel.z, activeGrabId);
        PearlCancelLogger.onGrabEnd();
        activeGrabId = null;
        activeGrabInstanceId = null;
    }

    private static Long nearestInstanceId(Player player) {
        Long best = null;
        double bestDistSq = FALLBACK_MATCH_RADIUS * FALLBACK_MATCH_RADIUS;
        for (TentacleTracker.TentacleSnapshot snap : TentacleTracker.getLiveInstances()) {
            double d = snap.anchorPos().distanceToSqr(player.position());
            if (d < bestDistSq) { bestDistSq = d; best = snap.instanceId(); }
        }
        return best;
    }

    public static void onAttemptEnded(long attemptId, String outcome) {
        record PendingGrabRow(long grabId) {}

        List<PendingGrabRow> rows = GameplayDatabase.query(
                "SELECT grab_id FROM grab_events WHERE attempt_id = ? AND cancel_result IS NULL",
                rs -> new PendingGrabRow(rs.getLong("grab_id")),
                attemptId);

        String result = switch (outcome) {
            case "success" -> "cancelled";
            case "abandoned" -> "abandoned";
            default -> "failed_to_cancel";
        };
        for (PendingGrabRow row : rows) {
            GameplayDatabase.update("UPDATE grab_events SET cancel_result = ? WHERE grab_id = ?", result, row.grabId());
        }
    }
}
