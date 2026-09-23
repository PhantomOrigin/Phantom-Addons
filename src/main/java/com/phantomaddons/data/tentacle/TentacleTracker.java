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
    private static final long DESPAWN_GRACE_TICKS = 20; // render-list flicker tolerance before a segment is considered gone — entitiesForRendering() can drop an entity for longer than a few ticks (camera turns, frustum culling) without it having actually despawned
    private static final long INSTANCE_RETIRE_TICKS = 100; // ~5s with no live segments before a chain is considered fully retracted
    private static final long SAMPLE_INTERVAL_TICKS = 4;

    private static final class SegmentState {
        long segmentId;
        long instanceId;
        long lastSeenTick;
        Vec3 lastPos;
        boolean isAnchor;
    }

    private static final class InstanceState {
        long instanceId;
        long zoneId;
        long lastLiveTick;
        long lastAnchorSampleTick = -1;
        double baseX, baseZ;
        int maxChainSize = 0;
        UUID anchorUuid;
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
            if (hasVisibleName(mob)) continue; // ordinary magma cubes carry a real nametag; tentacle segments don't
            if (mob.distanceToSqr(client.player) > MAX_TRACK_DISTANCE * MAX_TRACK_DISTANCE) continue;

            UUID uuid = mob.getUUID();
            SegmentState seg = segmentsByUuid.get(uuid);
            if (seg != null) {
                seg.lastPos = mob.position();
                seg.lastSeenTick = tick;
                InstanceState inst = instancesById.get(seg.instanceId);
                if (inst != null) {
                    inst.lastLiveTick = tick;
                    if (mob.isInvisible() && inst.anchorUuid == null) {
                        inst.anchorUuid = uuid;
                        seg.isAnchor = true;
                        GameplayDatabase.update("UPDATE tentacle_segments SET is_anchor = 1 WHERE segment_id = ?", seg.segmentId);
                    }
                }
            } else {
                unknown.add(mob);
            }
        }

        refreshInstanceBases(tick);
        assignUnknown(unknown, runId, tick);
        sampleAnchors(tick);
        expireStaleSegments(tick);
        retireStaleInstances(tick);
    }

    private static void refreshInstanceBases(long tick) {
        for (InstanceState inst : instancesById.values()) {
            if (inst.lastLiveTick != tick) continue;

            if (inst.anchorUuid != null) {
                SegmentState anchor = segmentsByUuid.get(inst.anchorUuid);
                if (anchor != null && anchor.lastPos != null) {
                    inst.baseX = anchor.lastPos.x;
                    inst.baseZ = anchor.lastPos.z;
                    continue;
                }
            }

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
            List<List<Entity>> matches = new ArrayList<>();
            for (List<Entity> cluster : clusters) {
                for (Entity member : cluster) {
                    if (horizontalDist(member.position(), pos) <= CLUSTER_RADIUS) { matches.add(cluster); break; }
                }
            }
            if (matches.isEmpty()) {
                List<Entity> fresh = new ArrayList<>();
                fresh.add(e);
                clusters.add(fresh);
            } else {
                List<Entity> target = matches.get(0);
                target.add(e);
                for (int i = 1; i < matches.size(); i++) {
                    target.addAll(matches.get(i));
                    clusters.remove(matches.get(i));
                }
            }
        }

        for (List<Entity> cluster : clusters) {
            Entity anchorEntity = null;
            for (Entity e : cluster) {
                if (e instanceof AbstractCubeMob mob && mob.isInvisible()) { anchorEntity = e; break; }
            }
            cluster.sort(Comparator.comparingDouble(en -> en.position().y));
            Vec3 basePos = anchorEntity != null ? anchorEntity.position() : cluster.get(0).position();
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
                instance.zoneId = zoneId;
                instance.baseX = bx;
                instance.baseZ = bz;
                instancesById.put(instanceId, instance);
            }
            instance.lastLiveTick = tick;

            int rank = 0;
            for (Entity e : cluster) {
                boolean isAnchor = e == anchorEntity;
                String uuidStr = e.getUUID().toString();
                int chainPos = rank++;

                Long existingSegmentId = findExistingSegmentId(instance.instanceId, uuidStr);
                long segmentId;
                if (existingSegmentId != null) {
                    segmentId = existingSegmentId;
                    GameplayDatabase.update("UPDATE tentacle_segments SET despawned = 0, is_anchor = ? WHERE segment_id = ?",
                            isAnchor ? 1 : 0, segmentId);
                } else {
                    segmentId = GameplayDatabase.insert(
                            "INSERT INTO tentacle_segments (instance_id, entity_uuid, entity_type, chain_position, first_seen_tick, is_anchor) " +
                                    "VALUES (?, ?, ?, ?, ?, ?)",
                            instance.instanceId, uuidStr, e.getClass().getSimpleName(), chainPos, tick, isAnchor ? 1 : 0);
                }
                SegmentState seg = new SegmentState();
                seg.segmentId = segmentId;
                seg.instanceId = instance.instanceId;
                seg.lastSeenTick = tick;
                seg.lastPos = e.position();
                seg.isAnchor = isAnchor;
                segmentsByUuid.put(e.getUUID(), seg);
                instance.liveSegments.add(e.getUUID());
                if (isAnchor && instance.anchorUuid == null) instance.anchorUuid = e.getUUID();
            }
            if (instance.liveSegments.size() > instance.maxChainSize) {
                instance.maxChainSize = instance.liveSegments.size();
                GameplayDatabase.update("UPDATE tentacle_instances SET max_chain_size = ? WHERE instance_id = ?",
                        instance.maxChainSize, instance.instanceId);
            }
        }
    }

    private static Long findExistingSegmentId(long instanceId, String entityUuid) {
        List<Long> rows = GameplayDatabase.query(
                "SELECT segment_id FROM tentacle_segments WHERE instance_id = ? AND entity_uuid = ?",
                rs -> rs.getLong("segment_id"), instanceId, entityUuid);
        return rows.isEmpty() ? null : rows.get(0);
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

    private static void sampleAnchors(long tick) {
        for (InstanceState inst : instancesById.values()) {
            if (inst.lastLiveTick != tick || inst.anchorUuid == null) continue;
            if (inst.lastAnchorSampleTick >= 0 && tick - inst.lastAnchorSampleTick < SAMPLE_INTERVAL_TICKS) continue;

            SegmentState anchor = segmentsByUuid.get(inst.anchorUuid);
            if (anchor == null || anchor.lastPos == null) continue;

            inst.lastAnchorSampleTick = tick;
            GameplayDatabase.insert(
                    "INSERT INTO tentacle_anchor_samples (instance_id, tick, sampled_at, x, y, z, chain_size) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    inst.instanceId, tick, GameplayDatabase.now(), anchor.lastPos.x, anchor.lastPos.y, anchor.lastPos.z, inst.liveSegments.size());
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
            if (inst != null) {
                inst.liveSegments.remove(e.getKey());
                if (e.getKey().equals(inst.anchorUuid)) inst.anchorUuid = null;
            }
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

    private static boolean hasVisibleName(Entity e) {
        return e.hasCustomName() && e.getCustomName() != null && !e.getCustomName().getString().isBlank();
    }

    private static double horizontalDist(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    // ── Snapshot API for the grab tracker ───────────────────────────────────────

    public record TentacleSnapshot(long instanceId, Vec3 anchorPos, int chainSize) {}

    public static List<TentacleSnapshot> getLiveInstances() {
        List<TentacleSnapshot> out = new ArrayList<>();
        for (InstanceState inst : instancesById.values()) {
            if (inst.anchorUuid == null) continue;
            SegmentState anchor = segmentsByUuid.get(inst.anchorUuid);
            if (anchor == null || anchor.lastPos == null) continue;
            out.add(new TentacleSnapshot(inst.instanceId, anchor.lastPos, inst.liveSegments.size()));
        }
        return out;
    }

    public static Long findInstanceForEntity(UUID entityUuid) {
        SegmentState seg = segmentsByUuid.get(entityUuid);
        return seg != null ? seg.instanceId : null;
    }

    public static Long getZoneIdForEntity(UUID entityUuid) {
        SegmentState seg = segmentsByUuid.get(entityUuid);
        if (seg == null) return null;
        InstanceState inst = instancesById.get(seg.instanceId);
        return inst != null ? inst.zoneId : null;
    }
}
