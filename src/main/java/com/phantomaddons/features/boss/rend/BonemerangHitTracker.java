package com.phantomaddons.features.boss.rend;

import com.phantomaddons.features.boss.KuudraHpHud;
import com.phantomaddons.mixin.ItemDisplayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class BonemerangHitTracker {

    private static final int    PENDING_MAX_AGE_TICKS = 20;
    private static final int    TRACK_MAX_AGE_TICKS   = 220;
    private static final double AABB_PAD = 0.75;

    private enum Phase { OUTBOUND, RETURNING }

    private static boolean pendingThrow    = false;
    private static int     pendingAgeTicks = 0;
    private static double  sourceX, sourceY, sourceZ;

    private static Display.ItemDisplay tracked = null;
    private static int     trackedAgeTicks = 0;
    private static Phase   phase = Phase.OUTBOUND;
    private static boolean hasTurned = false;
    private static double  lastSourceDistSq, maxSourceDistSq;
    private static double  prevX, prevY, prevZ, currX, currY, currZ;
    private static boolean backHitFiredThisThrow = false;

    private static final List<Runnable> onBackHitListeners = new ArrayList<>();

    private BonemerangHitTracker() {}

    public static void addOnBackHitListener(Runnable callback) {
        onBackHitListeners.add(callback);
    }

    public static void onThrow(double eyeX, double eyeY, double eyeZ) {
        tracked = null;
        pendingThrow = true;
        pendingAgeTicks = 0;
        sourceX = eyeX;
        sourceY = eyeY;
        sourceZ = eyeZ;
        backHitFiredThisThrow = false;
    }

    public static void reset() {
        pendingThrow = false;
        tracked = null;
        backHitFiredThisThrow = false;
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null) return;

        if (pendingThrow) {
            pendingAgeTicks++;
            if (pendingAgeTicks > PENDING_MAX_AGE_TICKS) {
                pendingThrow = false;
            } else if (tracked == null) {
                for (Entity e : mc.level.entitiesForRendering()) {
                    if (!(e instanceof Display.ItemDisplay disp)) continue;
                    if (disp == tracked) continue;
                    ItemStack stack = ((ItemDisplayAccessor) disp).kuudrahelper$getItemStack();
                    if (!isBonemerang(stack)) continue;
                    claim(disp);
                    break;
                }
            }
        }

        if (tracked == null) return;

        if (!tracked.isAlive() || tracked.isRemoved()) {
            tracked = null;
            return;
        }

        prevX = currX; prevY = currY; prevZ = currZ;
        currX = tracked.getX(); currY = tracked.getY(); currZ = tracked.getZ();
        updatePhase();
        trackedAgeTicks++;
        if (trackedAgeTicks > TRACK_MAX_AGE_TICKS) { tracked = null; return; }

        checkHit();
    }

    private static void claim(Display.ItemDisplay disp) {
        tracked = disp;
        trackedAgeTicks = 0;
        pendingThrow = false;
        currX = disp.getX(); currY = disp.getY(); currZ = disp.getZ();
        prevX = currX; prevY = currY; prevZ = currZ;
        lastSourceDistSq = distSq(currX, currY, currZ, sourceX, sourceY, sourceZ);
        maxSourceDistSq = lastSourceDistSq;
        hasTurned = false;
        phase = Phase.OUTBOUND;
    }

    private static void updatePhase() {
        double distSq = distSq(currX, currY, currZ, sourceX, sourceY, sourceZ);
        if (distSq > maxSourceDistSq + 0.001) {
            maxSourceDistSq = distSq;
            if (!hasTurned) phase = Phase.OUTBOUND;
        } else if (distSq < lastSourceDistSq - 0.001) {
            hasTurned = true;
            phase = Phase.RETURNING;
        }
        if (hasTurned) phase = Phase.RETURNING;
        lastSourceDistSq = distSq;
    }

    private static void checkHit() {
        if (backHitFiredThisThrow || phase != Phase.RETURNING) return;

        LivingEntity kuudra = KuudraHpHud.getKuudra();
        if (kuudra == null || !kuudra.isAlive()) return;

        AABB box = kuudra.getBoundingBox().inflate(AABB_PAD);
        boolean hit = segmentIntersectsBox(box, prevX, prevY, prevZ, currX, currY, currZ)
                || box.contains(prevX, prevY, prevZ)
                || box.contains(currX, currY, currZ);
        if (!hit) return;

        backHitFiredThisThrow = true;
        for (Runnable listener : onBackHitListeners) listener.run();
    }

    private static boolean isBonemerang(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        String id = customData.copyTag().getStringOr("id", "");
        return id.equals("BONE_BOOMERANG") || id.equals("STARRED_BONE_BOOMERANG");
    }

    private static double distSq(double ax, double ay, double az, double bx, double by, double bz) {
        double dx = ax - bx, dy = ay - by, dz = az - bz;
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean segmentIntersectsBox(AABB box, double ax, double ay, double az,
                                                 double bx, double by, double bz) {
        double tMin = 0.0, tMax = 1.0;
        double dx = bx - ax, dy = by - ay, dz = bz - az;

        double[] tx = slabT(ax, dx, box.minX, box.maxX, tMin, tMax);
        if (tx == null) return false;
        double[] ty = slabT(ay, dy, box.minY, box.maxY, tx[0], tx[1]);
        if (ty == null) return false;
        double[] tz = slabT(az, dz, box.minZ, box.maxZ, ty[0], ty[1]);
        return tz != null;
    }

    private static double[] slabT(double origin, double dir, double min, double max, double tMin, double tMax) {
        if (Math.abs(dir) < 1.0E-12) {
            return (origin < min || origin > max) ? null : new double[]{tMin, tMax};
        }
        double inv = 1.0 / dir;
        double t1 = (min - origin) * inv;
        double t2 = (max - origin) * inv;
        if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
        if (t1 > tMin) tMin = t1;
        if (t2 < tMax) tMax = t2;
        return tMin > tMax ? null : new double[]{tMin, tMax};
    }
}
