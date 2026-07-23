package com.phantomaddons.features.miscskyblock;

import com.phantomaddons.PhantomConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public final class PredictedBobber {

    private static final double LAUNCH_SPEED = 1.5;
    private static final double GRAVITY      = 0.03;
    private static final double DRAG         = 0.92;
    private static final long   MAX_FLIGHT_MS = 4000; // safety net against a cast that never lands
    private static final int    GHOST_ID     = -424242;

    private static boolean active     = false;
    private static boolean landed     = false;
    private static long    launchAtMs = 0; // when simulated motion should actually start (post ping-delay)
    private static Vec3    launchPos  = Vec3.ZERO;
    private static Vec3    launchVel  = Vec3.ZERO;
    private static Vec3    landedPos  = null; // set once landed, so we stop re-simulating past it
    private static float   yaw, pitch;
    private static FishingHook ghost = null;

    private PredictedBobber() {}

    public static void onCast(Player player) {
        if (!PhantomConfig.isLegacyRodPhysicsEnabled()) return;
        if (!(player.level() instanceof ClientLevel level)) return;

        discardGhost(level);

        yaw = player.getYRot();
        pitch = player.getXRot();
        float yawRad = yaw * Mth.DEG_TO_RAD;
        float pitchRad = pitch * Mth.DEG_TO_RAD;
        launchVel = new Vec3(
                -Mth.sin(yawRad) * Mth.cos(pitchRad),
                -Mth.sin(pitchRad),
                Mth.cos(yawRad) * Mth.cos(pitchRad)
        ).normalize().scale(LAUNCH_SPEED);

        launchPos = player.getEyePosition();
        launchAtMs = System.currentTimeMillis();
        landed = false;
        landedPos = null;
        active = true;

        ghost = new FishingHook(player, level, 0, 0);
        ghost.setId(GHOST_ID);
        ghost.setOwner(player);
        ghost.snapTo(launchPos.x, launchPos.y, launchPos.z, yaw, pitch);
        level.addEntity(ghost);
    }

    public static void tick() {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ghost == null || !(mc.player.level() instanceof ClientLevel level)) { reset(); return; }

        long now = System.currentTimeMillis();

        if (landed && mc.player.fishing == null) { reset(); return; }
        if (now - launchAtMs > MAX_FLIGHT_MS) { reset(); return; }

        Vec3 pos;
        if (landed) {
            pos = landedPos;
        } else {
            long elapsedMs = now - launchAtMs;
            int fullTicks = (int) (elapsedMs / 50);
            double partialTick = (elapsedMs % 50) / 50.0;

            Vec3 simPos = launchPos;
            Vec3 simVel = launchVel;
            for (int i = 0; i < fullTicks; i++) {
                Vec3 next = simPos.add(simVel);
                if (hitsSomething(level, next)) {
                    landed = true;
                    landedPos = next;
                    break;
                }
                simPos = next;
                simVel = new Vec3(simVel.x * DRAG, (simVel.y - GRAVITY) * DRAG, simVel.z * DRAG);
            }
            if (!landed) {
                Vec3 next = simPos.add(simVel.scale(partialTick));
                if (hitsSomething(level, next)) {
                    landed = true;
                    landedPos = next;
                } else {
                    simPos = next;
                }
            }
            pos = landed ? landedPos : simPos;
        }

        ghost.snapTo(pos.x, pos.y, pos.z, yaw, pitch);
    }

    private static boolean hitsSomething(ClientLevel level, Vec3 p) {
        BlockPos bp = BlockPos.containing(p.x, p.y, p.z);
        FluidState fluid = level.getFluidState(bp);
        if (!fluid.isEmpty()) return true;
        return !level.getBlockState(bp).isAir();
    }

    public static boolean isReplacing(Entity entity) {
        return active && entity == ghost;
    }

    public static boolean isSuppressingRealHook(Entity realHook) {
        return active && Minecraft.getInstance().player != null && realHook == Minecraft.getInstance().player.fishing;
    }

    public static void reset() {
        active = false;
        landed = false;
        Minecraft mc = Minecraft.getInstance();
        if (ghost != null && mc.level instanceof ClientLevel level) {
            discardGhost(level);
        }
        ghost = null;
    }

    private static void discardGhost(ClientLevel level) {
        if (ghost == null) return;
        level.removeEntity(ghost.getId(), Entity.RemovalReason.DISCARDED);
        ghost.discard();
    }
}
