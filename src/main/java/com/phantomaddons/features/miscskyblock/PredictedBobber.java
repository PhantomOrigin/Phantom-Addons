package com.phantomaddons.features.miscskyblock;

import com.phantomaddons.PhantomConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;

public final class PredictedBobber {

    private static final double GRAVITY       = 0.03;
    private static final double DRAG          = 0.895;
    private static final double LAUNCH_SPEED  = 1.7;
    private static final long   MAX_FLIGHT_MS = 4000; // safety net against a cast that never lands
    private static final int    GHOST_ID      = -424242;
    private static final double FLUID_ENTRY_DAMP_Y = 0.55;
    private static final double BOB_PULL    = 0.045;
    private static final double MAX_RISE_SPEED = 0.13;
    private static final double BOB_DRAG_XZ = 0.82;
    private static final int    MAX_FLOAT_CATCHUP_TICKS = 40; // cap per-frame catch-up after a lag spike

    private static boolean active      = false;
    private static boolean landed      = false;
    private static boolean floating    = false;
    private static long    launchAtMs  = 0;
    private static Vec3    launchPos   = Vec3.ZERO;
    private static Vec3    launchVel   = Vec3.ZERO;
    private static Vec3    landedPos   = null;
    private static Vec3    floatPos    = null;
    private static Vec3    floatVel    = Vec3.ZERO;
    private static double  floatSurfaceY = 0;
    private static long    floatLastTickMs = 0;
    private static float   yaw, pitch;
    private static FishingHook ghost = null;
    private static Vec3 debugEntryPos  = null;
    private static Vec3 debugLowestPos = null;
    private static Vec3 debugRestPos   = null;

    private PredictedBobber() {}

    public static Vec3 getDebugEntryPos()  { return debugEntryPos; }
    public static Vec3 getDebugLowestPos() { return debugLowestPos; }
    public static Vec3 getDebugRestPos()   { return debugRestPos; }

    public static Vec3 getGhostPosition() {
        return active && ghost != null ? ghost.position() : null;
    }

    public static void onCast(Player player) {
        if (!PhantomConfig.isLegacyRodPhysicsEnabled()) return;
        if (!(player.level() instanceof ClientLevel level)) return;
        discardGhost(level);
        yaw = player.getYRot();
        pitch = player.getXRot();
        FishingHook realFishing = player.fishing;
        ghost = new FishingHook(player, level, 0, 0);
        player.fishing = realFishing;
        ghost.setId(GHOST_ID);

        launchPos = ghost.position();
        launchVel = deterministicLaunchVelocity(yaw, pitch);
        ghost.setDeltaMovement(launchVel);
        launchAtMs = System.currentTimeMillis();
        landed = false;
        landedPos = null;
        floating = false;
        floatPos = null;
        active = true;
        debugEntryPos = null;
        debugLowestPos = null;
        debugRestPos = null;

        level.addEntity(ghost);
    }

    private static Vec3 deterministicLaunchVelocity(float yawDeg, float pitchDeg) {
        float yawRad = yawDeg * Mth.DEG_TO_RAD;
        float pitchRad = pitchDeg * Mth.DEG_TO_RAD;
        Vec3 dir = new Vec3(
                -Mth.sin(yawRad) * Mth.cos(pitchRad),
                -Mth.sin(pitchRad),
                Mth.cos(yawRad) * Mth.cos(pitchRad)
        );
        return dir.scale(LAUNCH_SPEED);
    }

    public static boolean isActive() {
        return active;
    }

    public static void onRetrieve() {
        reset();
    }

    public static void tick() {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ghost == null || !(mc.player.level() instanceof ClientLevel level)) { reset(); return; }

        if (!mc.player.getMainHandItem().is(net.minecraft.world.item.Items.FISHING_ROD)) {
            reset();
            return;
        }

        long now = System.currentTimeMillis();
        if (!landed && !floating && now - launchAtMs > MAX_FLIGHT_MS) { reset(); return; }

        Vec3 pos;
        if (landed) {
            pos = landedPos;
        } else if (floating) {
            pos = advanceFloating(level, now);
        } else {
            pos = simulateFlight(level, now);
            if (floating) {
                pos = advanceFloating(level, now);
            }
        }

        if (landed) debugRestPos = landedPos;

        ghost.snapTo(pos.x, pos.y, pos.z, yaw, pitch);
    }

    private static final int COLLISION_SUBSTEPS = 8;

    private static Vec3 simulateFlight(ClientLevel level, long now) {
        long elapsedMs = now - launchAtMs;
        int fullTicks = (int) (elapsedMs / 50);
        double partialTick = (elapsedMs % 50) / 50.0;

        Vec3 simPos = launchPos;
        Vec3 simVel = launchVel;
        for (int i = 0; i < fullTicks; i++) {
            simVel = new Vec3(simVel.x, simVel.y - GRAVITY, simVel.z);
            Vec3 hit = advanceWithCollision(level, simPos, simVel, 1.0);
            if (hit != null) {
                if (isFluid(level, hit)) {
                    beginFloating(level, hit, simVel, launchAtMs + (i + 1) * 50L);
                    return floatPos;
                }
                landed = true;
                landedPos = hit;
                return landedPos;
            }
            simPos = simPos.add(simVel);
            simVel = simVel.scale(DRAG);
        }

        Vec3 previewVel = new Vec3(simVel.x, simVel.y - GRAVITY * partialTick, simVel.z);
        Vec3 hit = advanceWithCollision(level, simPos, previewVel, partialTick);
        if (hit != null) {
            if (isFluid(level, hit)) {
                beginFloating(level, hit, previewVel, now);
                return floatPos;
            }
            landed = true;
            landedPos = hit;
            return landedPos;
        }
        return simPos.add(previewVel.scale(partialTick));
    }

    private static Vec3 advanceWithCollision(ClientLevel level, Vec3 from, Vec3 vel, double fraction) {
        Vec3 step = vel.scale(fraction / COLLISION_SUBSTEPS);
        Vec3 pos = from;
        for (int s = 0; s < COLLISION_SUBSTEPS; s++) {
            pos = pos.add(step);
            if (isSolid(level, pos) || isFluid(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static void beginFloating(ClientLevel level, Vec3 entryPos, Vec3 entryVel, long entryAtMs) {
        floating = true;
        floatPos = entryPos;
        floatVel = new Vec3(entryVel.x, entryVel.y * FLUID_ENTRY_DAMP_Y, entryVel.z);
        floatLastTickMs = entryAtMs;
        debugEntryPos = entryPos;
        debugLowestPos = entryPos;
        floatSurfaceY = findColumnSurfaceY(level, BlockPos.containing(entryPos.x, entryPos.y, entryPos.z));
    }

    private static double findColumnSurfaceY(ClientLevel level, BlockPos entryBp) {
        BlockPos.MutableBlockPos mp = entryBp.mutable();
        while (!level.getFluidState(mp.above()).isEmpty()) {
            mp.move(0, 1, 0);
        }
        return mp.getY() + level.getFluidState(mp).getHeight(level, mp);
    }

    private static Vec3 advanceFloating(ClientLevel level, long now) {
        int ticksToRun = Math.min((int) ((now - floatLastTickMs) / 50), MAX_FLOAT_CATCHUP_TICKS);
        for (int i = 0; i < ticksToRun; i++) {
            BlockPos bp = BlockPos.containing(floatPos.x, floatPos.y, floatPos.z);
            if (level.getFluidState(bp).isEmpty()) {
                floating = false;
                launchPos = floatPos;
                launchVel = floatVel;
                launchAtMs = floatLastTickMs;
                return simulateFlight(level, now);
            }

            double force = (floatPos.y + floatVel.y) - floatSurfaceY;
            if (Math.abs(force) < 0.01) force += Math.signum(force) * 0.1;

            double nextVelY = Math.min(floatVel.y - force * BOB_PULL, MAX_RISE_SPEED);
            floatVel = new Vec3(floatVel.x * BOB_DRAG_XZ, nextVelY, floatVel.z * BOB_DRAG_XZ);
            Vec3 next = floatPos.add(floatVel);

            if (isSolid(level, next)) {
                landed = true;
                landedPos = next;
                floating = false;
                return landedPos;
            }

            if (floatPos.y < floatSurfaceY && next.y >= floatSurfaceY && floatVel.y > 0) {
                landed = true;
                landedPos = new Vec3(next.x, floatSurfaceY, next.z);
                floating = false;
                return landedPos;
            }

            floatPos = next;
            if (debugLowestPos == null || floatPos.y < debugLowestPos.y) debugLowestPos = floatPos;
        }
        floatLastTickMs += (long) ticksToRun * 50L;
        return floatPos;
    }

    private static boolean isSolid(ClientLevel level, Vec3 p) {
        BlockPos bp = BlockPos.containing(p.x, p.y, p.z);
        return !level.getBlockState(bp).getCollisionShape(level, bp).isEmpty();
    }

    private static boolean isFluid(ClientLevel level, Vec3 p) {
        BlockPos bp = BlockPos.containing(p.x, p.y, p.z);
        var fluidState = level.getFluidState(bp);
        if (fluidState.isEmpty()) return false;
        double surfaceY = bp.getY() + fluidState.getHeight(level, bp);
        return p.y <= surfaceY;
    }

    public static boolean isReplacing(Entity entity) {
        return active && entity == ghost;
    }

    public static boolean isSuppressingRealHook(Entity realHook) {
        if (!PhantomConfig.isLegacyRodPhysicsEnabled()) return false;
        if (!(realHook instanceof FishingHook hook)) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && hook == mc.player.fishing;
    }

    public static void reset() {
        active = false;
        landed = false;
        floating = false;
        floatPos = null;
        Minecraft mc = Minecraft.getInstance();
        if (ghost != null && mc.level instanceof ClientLevel level) {
            discardGhost(level);
        }
        ghost = null;
    }

    private static void discardGhost(ClientLevel level) {
        if (ghost == null) return;
        Minecraft mc = Minecraft.getInstance();
        FishingHook realFishing = (mc.player != null) ? mc.player.fishing : null;
        level.removeEntity(ghost.getId(), Entity.RemovalReason.DISCARDED);
        ghost.discard();
        if (mc.player != null) mc.player.fishing = realFishing;
    }
}
