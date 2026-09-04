package com.phantomaddons.data.tentacle;

import com.phantomaddons.data.GameplayDatabase;
import com.phantomaddons.data.RunRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
//? if <26.2 {
/*import net.minecraft.world.entity.monster.Slime;
*///?} else {
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
//?}
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TentacleTracker {

    private static final int KUUDRA_SIZE = 30; // Kuudra's own body — excluded defensively; it isn't present during SUPPLIES anyway
    private static final double MAX_TRACK_DISTANCE = 40.0; // blocks from player; noise filter
    private static final double CLUSTER_RADIUS = 1.5; // horizontal blocks: segments of one chain stack close together
    private static final double INSTANCE_REUSE_RADIUS = 3.0; // horizontal blocks: reattach fresh segments to a recently-live instance at ~same spot
    private static final long DESPAWN_GRACE_TICKS = 3; // render-list flicker tolerance before a segment is considered gone
    private static final long INSTANCE_RETIRE_TICKS = 100; // ~5s with no live segments before a chain is considered fully retracted
    private static final long SAMPLE_INTERVAL_TICKS = 4;

    private static final class SegmentState {
        long segmentId;
        long instanceId;
        long lastSeenTick;
        long lastSampleTick = -1;
        Vec3 lastPos;
        double lastSpeed = 0.0;
    }

    private static final class InstanceState {
        long instanceId;
        long lastLiveTick;
        double baseX, baseZ;
        int maxChainSize = 0;
        final Set<UUID> liveSegments = new HashSet<>();
    }

    private static final Map<UUID, SegmentState> segmentsByUuid = new HashMap<>();
    private static final Map<Long, InstanceState> instancesById = new HashMap<>();

    private TentacleTracker() {}

    public static void reset() {
        for (SegmentState seg : segmentsByUuid.values()) {
            GameplayDatabase.update("UPDATE tentacle_segments SET last_seen_tick = ?, despawned = 1 WHERE segment_id = ?",
                    seg.lastSeenTick, seg.segmentId);
        }
        for (InstanceState inst : instancesById.values()) {
            GameplayDatabase.update("UPDATE tentacle_instances SET last_seen_tick = ?, last_seen_at = ?, active = 0 WHERE instance_id = ?",
                    inst.lastLiveTick, GameplayDatabase.now(), inst.instanceId);
        }
        segmentsByUuid.clear();
        instancesById.clear();
    }

    public static void tick(Minecraft client, long tick) {
        Long runId = RunRecorder.getActiveRunId();
        if (runId == null) return;

        List<Entity> unknown = new ArrayList<>();

        for (Entity e : client.level.entitiesForRendering()) {
            if (!(e instanceof AbstractCubeMob mob)) continue;
            if (mob.getSize() == KUUDRA_SIZE) continue;
            if (mob.distanceToSqr(client.player) > MAX_TRACK_DISTANCE * MAX_TRACK_DISTANCE) continue;

            UUID uuid = mob.getUUID();
            SegmentState seg = segmentsByUuid.get(uuid);
            if (seg != null) {
                Vec3 newPos = mob.position();
                if (seg.lastPos != null) seg.lastSpeed = seg.lastPos.distanceTo(newPos);
                seg.lastPos = newPos;
                seg.lastSeenTick = tick;
                InstanceState inst = instancesById.get(seg.instanceId);
                if (inst != null) inst.lastLiveTick = tick;
            } else {
                unknown.add(mob);
            }
        }

        refreshInstanceBases(tick);
        assignUnknown(unknown, runId, tick);
        maybeSample(tick);
        expireStaleSegments(tick);
        retireStaleInstances(tick);
    }

    private static void refreshInstanceBases(long tick) {
        for (InstanceState inst : instancesById.values()) {
            if (inst.lastLiveTick != tick) continue;
            Vec3 basePos = null;
            for (UUID uuid : inst.liveSegments) {
                SegmentState seg = segmentsByUuid.get(uuid);
                if (seg == null || seg.lastPos == null) continue;
                if (basePos == null || seg.lastPos.y < basePos.y) basePos = seg.lastPos;
            }
            if (basePos != null) {
                inst.baseX = basePos.x;
                inst.baseZ = basePos.z;
            }
        }
    }

    private static void assignUnknown(List<Entity> unknown, long runId, long tick) {
        List<List<Entity>> clusters = new ArrayList<>();
        for (Entity e : unknown) {
            Vec3 pos = e.position();
            List<Entity> target = null;
            for (List<Entity> cluster : clusters) {
                for (Entity member : cluster) {
                    if (horizontalDist(member.position(), pos) <= CLUSTER_RADIUS) { target = cluster; break; }
                }
                if (target != null) break;
            }
            if (target == null) {
                target = new ArrayList<>();
                clusters.add(target);
            }
            target.add(e);
        }

        for (List<Entity> cluster : clusters) {
            cluster.sort(Comparator.comparingDouble(en -> en.position().y));
            Vec3 basePos = cluster.get(0).position();
            double bx = basePos.x, by = basePos.y, bz = basePos.z;

            InstanceState instance = findReusableInstance(bx, bz);
            if (instance == null) {
                long zoneId = TentacleZoneMatcher.matchOrCreate(bx, by, bz);
                long instanceId = GameplayDatabase.insert(
                        "INSERT INTO tentacle_instances (run_id, zone_id, first_seen_tick, first_seen_at, spawn_x, spawn_y, spawn_z) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        runId, zoneId, tick, GameplayDatabase.now(), bx, by, bz);
                instance = new InstanceState();
                instance.instanceId = instanceId;
                instance.baseX = bx;
                instance.baseZ = bz;
                instancesById.put(instanceId, instance);
            }
            instance.lastLiveTick = tick;

            int rank = 0;
            for (Entity e : cluster) {
                long segmentId = GameplayDatabase.insert(
                        "INSERT INTO tentacle_segments (instance_id, entity_uuid, entity_type, chain_position, first_seen_tick) " +
                                "VALUES (?, ?, ?, ?, ?)",
                        instance.instanceId, e.getUUID().toString(), e.getClass().getSimpleName(), rank++, tick);
                SegmentState seg = new SegmentState();
                seg.segmentId = segmentId;
                seg.instanceId = instance.instanceId;
                seg.lastSeenTick = tick;
                seg.lastPos = e.position();
                segmentsByUuid.put(e.getUUID(), seg);
                instance.liveSegments.add(e.getUUID());
            }
            if (instance.liveSegments.size() > instance.maxChainSize) {
                instance.maxChainSize = instance.liveSegments.size();
                GameplayDatabase.update("UPDATE tentacle_instances SET max_chain_size = ? WHERE instance_id = ?",
                        instance.maxChainSize, instance.instanceId);
            }
        }
    }

    private static InstanceState findReusableInstance(double x, double z) {
        InstanceState best = null;
        double bestDistSq = INSTANCE_REUSE_RADIUS * INSTANCE_REUSE_RADIUS;
        for (InstanceState inst : instancesById.values()) {
            double dx = inst.baseX - x, dz = inst.baseZ - z;
            double distSq = dx * dx + dz * dz;
            if (distSq <= bestDistSq) { best = inst; bestDistSq = distSq; }
        }
        return best;
    }

    private static void maybeSample(long tick) {
        for (SegmentState seg : segmentsByUuid.values()) {
            if (seg.lastSeenTick != tick) continue;
            if (seg.lastSampleTick >= 0 && tick - seg.lastSampleTick < SAMPLE_INTERVAL_TICKS) continue;
            seg.lastSampleTick = tick;
            InstanceState inst = instancesById.get(seg.instanceId);
            int chainSize = inst != null ? inst.liveSegments.size() : 1;
            GameplayDatabase.insert(
                    "INSERT INTO tentacle_samples (segment_id, tick, sampled_at, x, y, z, speed, chain_size) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    seg.segmentId, tick, GameplayDatabase.now(), seg.lastPos.x, seg.lastPos.y, seg.lastPos.z, seg.lastSpeed, chainSize);
        }
    }

    private static void expireStaleSegments(long tick) {
        Iterator<Map.Entry<UUID, SegmentState>> it = segmentsByUuid.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, SegmentState> e = it.next();
            SegmentState seg = e.getValue();
            if (tick - seg.lastSeenTick < DESPAWN_GRACE_TICKS) continue;

            GameplayDatabase.update("UPDATE tentacle_segments SET last_seen_tick = ?, despawned = 1 WHERE segment_id = ?",
                    seg.lastSeenTick, seg.segmentId);
            InstanceState inst = instancesById.get(seg.instanceId);
            if (inst != null) inst.liveSegments.remove(e.getKey());
            it.remove();
        }
    }

    private static void retireStaleInstances(long tick) {
        Iterator<Map.Entry<Long, InstanceState>> it = instancesById.entrySet().iterator();
        while (it.hasNext()) {
            InstanceState inst = it.next().getValue();
            if (!inst.liveSegments.isEmpty()) continue;
            if (tick - inst.lastLiveTick < INSTANCE_RETIRE_TICKS) continue;

            GameplayDatabase.update("UPDATE tentacle_instances SET last_seen_tick = ?, last_seen_at = ?, active = 0 WHERE instance_id = ?",
                    inst.lastLiveTick, GameplayDatabase.now(), inst.instanceId);
            it.remove();
        }
    }

    private static double horizontalDist(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    // ── Snapshot API for the grab/approach tracker ──────────────────────────────

    public record TentacleSnapshot(long instanceId, Vec3 topPos, double avgSpeed, int chainSize) {}

    public static List<TentacleSnapshot> getLiveInstances() {
        List<TentacleSnapshot> out = new ArrayList<>();
        for (InstanceState inst : instancesById.values()) {
            if (inst.liveSegments.isEmpty()) continue;
            Vec3 top = null;
            double speedSum = 0;
            int count = 0;
            for (UUID uuid : inst.liveSegments) {
                SegmentState seg = segmentsByUuid.get(uuid);
                if (seg == null || seg.lastPos == null) continue;
                if (top == null || seg.lastPos.y > top.y) top = seg.lastPos;
                speedSum += seg.lastSpeed;
                count++;
            }
            if (top == null) continue;
            out.add(new TentacleSnapshot(inst.instanceId, top, count > 0 ? speedSum / count : 0, inst.liveSegments.size()));
        }
        return out;
    }

    public static Long findInstanceForEntity(UUID entityUuid) {
        SegmentState seg = segmentsByUuid.get(entityUuid);
        return seg != null ? seg.instanceId : null;
    }
}
