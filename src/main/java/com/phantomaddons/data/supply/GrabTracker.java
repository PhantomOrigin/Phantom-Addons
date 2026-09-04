package com.phantomaddons.data.supply;

import com.phantomaddons.data.GameplayDatabase;
import com.phantomaddons.data.tentacle.TentacleTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GrabTracker {

    private static final double REACH_THRESHOLD = 2.0; // blocks: top segment counts as "reached" the player
    private static final long APPROACH_COOLDOWN_TICKS = 40; // don't spam an approach row every tick while lingering close
    private static final long GRAB_LINK_WINDOW_TICKS = 10; // a mount starting within this many ticks of an approach is attributed to it
    private static final double FALLBACK_MATCH_RADIUS = 6.0; // blocks: cap on the "nearest tentacle" fallback so a far-off tentacle never gets blamed for an unrelated mount

    private record PendingApproach(long eventId, long expireTick) {}

    private static final Map<Long, PendingApproach> pendingApproaches = new HashMap<>();
    private static final Map<Long, Long> lastApproachTick = new HashMap<>();

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
        pendingApproaches.clear();
        lastApproachTick.clear();
        activeGrabId = null;
        activeGrabInstanceId = null;
        PearlCancelLogger.onGrabEnd();
    }

    public static void tick(Minecraft client, long tick) {
        Player player = client.player;
        lastKnownTick = tick;
        Long attemptId = SupplyAttemptTracker.getActiveAttemptId();

        if (attemptId != null) {
            detectApproaches(player, attemptId, tick);
        }
        pendingApproaches.entrySet().removeIf(e -> tick > e.getValue().expireTick());

        Entity vehicle = player.getVehicle();
        boolean mountedNow = player.isPassenger() && vehicle != null && !(vehicle instanceof ArmorStand);
        if (mountedNow && activeGrabId == null) {
            onGrabStart(player, attemptId, tick);
        } else if (!mountedNow && activeGrabId != null) {
            onGrabEnd(player, tick);
        }
    }

    private static void detectApproaches(Player player, long attemptId, long tick) {
        for (TentacleTracker.TentacleSnapshot snap : TentacleTracker.getLiveInstances()) {
            if (snap.topPos().distanceTo(player.position()) > REACH_THRESHOLD) continue;

            Long lastTick = lastApproachTick.get(snap.instanceId());
            if (lastTick != null && tick - lastTick < APPROACH_COOLDOWN_TICKS) continue;
            lastApproachTick.put(snap.instanceId(), tick);

            long eventId = GameplayDatabase.insert(
                    "INSERT INTO tentacle_approach_events (attempt_id, instance_id, reached_tick, reached_at, " +
                            "tentacle_x, tentacle_y, tentacle_z, tentacle_speed, tentacle_chain_size, " +
                            "player_x, player_y, player_z, player_yaw, player_pitch, resulted_in_grab) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                    attemptId, snap.instanceId(), tick, GameplayDatabase.now(),
                    snap.topPos().x, snap.topPos().y, snap.topPos().z, snap.avgSpeed(), snap.chainSize(),
                    player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());

            pendingApproaches.put(snap.instanceId(), new PendingApproach(eventId, tick + GRAB_LINK_WINDOW_TICKS));
        }
    }

    private static void onGrabStart(Player player, Long attemptId, long tick) {
        Entity vehicle = player.getVehicle();
        Long instanceId = vehicle != null ? TentacleTracker.findInstanceForEntity(vehicle.getUUID()) : null;
        if (instanceId == null) instanceId = nearestInstanceId(player);

        Long approachEventId = null;
        if (instanceId != null) {
            PendingApproach pending = pendingApproaches.remove(instanceId);
            if (pending != null) {
                approachEventId = pending.eventId();
                GameplayDatabase.update("UPDATE tentacle_approach_events SET resulted_in_grab = 1 WHERE event_id = ?", approachEventId);
            }
        }

        activeGrabId = GameplayDatabase.insert(
                "INSERT INTO grab_events (approach_event_id, attempt_id, instance_id, mount_start_tick, mount_started_at) " +
                        "VALUES (?, ?, ?, ?, ?)",
                approachEventId, attemptId, instanceId, tick, GameplayDatabase.now());
        activeGrabInstanceId = instanceId;
        PearlCancelLogger.onGrabStart(activeGrabId);
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
            double d = snap.topPos().distanceToSqr(player.position());
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
