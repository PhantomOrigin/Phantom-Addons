package com.kuudrahelper.features.supplies;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class EtherwarpWaypointManager {

    private static final double PRE_RANGE = 20.0;

    private static final double PEARL_SPEED     = 1.5;
    private static final double PEARL_DRAG      = 0.99;
    private static final double PEARL_GRAVITY   = 0.03;
    private static final int    PEARL_MAX_TICKS = 120;

    public record PreSpot(Vec3 pos, Vec3[] defaultLandings) {}

    public static final PreSpot[] PRE_SPOTS = {
        new PreSpot(new Vec3(-81.0, 76.0, -143.0), new Vec3[] {
            new Vec3( -98.0, 79.0, -113.0), // Shop     → SHOP beacon
            new Vec3(-106.0, 79.0, -113.0), // X Cannon → X beacon
            new Vec3( -98.0, 79.0,  -99.0), // Square   → SLASH beacon
        }),
        new PreSpot(new Vec3(-67.5, 77.0, -122.5), new Vec3[] {
            new Vec3( -94.0, 79.0, -106.0), // Shop     → TRIANGLE beacon
            new Vec3(-110.0, 79.0, -106.0), // X Cannon → X_CANNON beacon
            new Vec3(-106.0, 79.0,  -99.0), // Square   → EQUALS beacon
        }),
        new PreSpot(new Vec3(-113.5, 77.0, -68.5), new Vec3[] {
            new Vec3( -98.0, 79.0, -113.0), // Shop     → SHOP beacon
            new Vec3(-110.0, 79.0, -106.0), // X Cannon → X_CANNON beacon
            new Vec3( -98.0, 79.0,  -99.0), // Square   → SLASH beacon
        }),
        new PreSpot(new Vec3(-65.5, 76.0, -87.5), new Vec3[] {
            new Vec3( -98.0, 79.0, -113.0), // Shop     → SHOP beacon
            new Vec3(-106.0, 79.0, -113.0), // X Cannon → X beacon
            new Vec3(-106.0, 79.0,  -99.0), // Square   → EQUALS beacon
        }),
    };

    public record EtherwarpGroup(String zone, List<Vec3> targets) {}

    public static final List<EtherwarpGroup> GROUPS = List.of(
        new EtherwarpGroup("Shop",
            List.of(new Vec3(-72.5, 79.0, -135.5),
                    new Vec3(-87.5, 79.0, -127.5))),
        new EtherwarpGroup("X Cannon",
            List.of(new Vec3(-135.5, 79.0, -126.5),
                    new Vec3(-129.5, 79.0, -114.5))),
        new EtherwarpGroup("Square",
            List.of(new Vec3(-139.5, 79.0, -89.5)))
    );

    private static Vec3   currentPearlDest    = null;
    private static String stickyPriorityZone  = null;
    private static boolean active             = false;

    private EtherwarpWaypointManager() {}

    public static void onSuppliesStart() {
        active = true;
        stickyPriorityZone = null;
        currentPearlDest = null;
    }

    public static void reset() {
        active = false;
        stickyPriorityZone = null;
        currentPearlDest = null;
    }

    public static void updatePriority() {
        String fresh = CratePriority.getDestination();
        if (fresh != null) stickyPriorityZone = fresh;
    }

    public static String getStickyPriorityZone() { return stickyPriorityZone; }

    public static boolean isNearAnyPreSpot(Vec3 playerPos) {
        for (PreSpot ps : PRE_SPOTS)
            if (playerPos.distanceTo(ps.pos()) < PRE_RANGE) return true;
        return false;
    }

    public static Vec3 getEffectivePearlDest(Vec3 playerPos) {
        if (currentPearlDest != null) return currentPearlDest;
        PreSpot nearest = nearestPreSpot(playerPos);
        if (nearest == null) return null;
        if (stickyPriorityZone != null) {
            for (int gi = 0; gi < GROUPS.size(); gi++) {
                if (GROUPS.get(gi).zone().equals(stickyPriorityZone)) {
                    return nearest.defaultLandings()[gi];
                }
            }
        }
        return null;
    }

    private static PreSpot nearestPreSpot(Vec3 playerPos) {
        PreSpot best     = null;
        double  bestDist = PRE_RANGE;
        for (PreSpot ps : PRE_SPOTS) {
            double d = playerPos.distanceTo(ps.pos());
            if (d < bestDist) { bestDist = d; best = ps; }
        }
        return best;
    }

    public static void onPearlThrow(Minecraft mc, Player player) {
        if (!active || mc.level == null) return;
        Vec3 landing = simulatePearlLanding(mc, player);
        if (landing != null) currentPearlDest = landing;
    }

    private static Vec3 simulatePearlLanding(Minecraft mc, Player player) {
        Vec3 pos = player.getEyePosition();
        Vec3 vel = player.getLookAngle().scale(PEARL_SPEED);

        for (int i = 0; i < PEARL_MAX_TICKS; i++) {
            vel = new Vec3(vel.x * PEARL_DRAG, vel.y * PEARL_DRAG - PEARL_GRAVITY, vel.z * PEARL_DRAG);
            Vec3 nextPos = pos.add(vel);

            BlockHitResult hit = mc.level.clip(new ClipContext(
                    pos, nextPos,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player));

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos bp = hit.getBlockPos();
                return new Vec3(bp.getX() + 0.5, bp.getY() + 1.0, bp.getZ() + 0.5);
            }
            pos = nextPos;
        }
        return null;
    }

    public static boolean isActive() { return active; }
}
