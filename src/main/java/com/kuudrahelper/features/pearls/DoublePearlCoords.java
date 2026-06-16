package com.kuudrahelper.features.pearls;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DoublePearlCoords {

    private static final Pattern IQ_PATTERN = Pattern.compile(
            "\\[IQ]\\s+(.+?)\\s+x:\\s*(-?[\\d,.]+),\\s*y:\\s*(-?[\\d,.]+),\\s*z:\\s*(-?[\\d,.]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PA_PATTERN = Pattern.compile(
            "\\[Phantom]\\s+(.+?)\\s+x:\\s*(-?[\\d.]+)\\s+y:\\s*(-?[\\d.]+)\\s+z:\\s*(-?[\\d.]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Map<PickupLocation, Vec3> landingCache =
            new EnumMap<>(PickupLocation.class);

    private DoublePearlCoords() {}

    public static void reset() { landingCache.clear(); }

    public static Vec3 getBestLanding(PickupLocation loc) {
        return landingCache.get(loc);
    }

    public static void onChat(String raw) {
        String clean = raw.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();

        Matcher m = IQ_PATTERN.matcher(clean);
        if (!m.find()) {
            m = PA_PATTERN.matcher(clean);
            if (!m.find()) return;
        }

        PickupLocation loc = parseLoc(m.group(1).trim());
        if (loc == null) return;

        double x = coord(m.group(2));
        double y = coord(m.group(3));
        double z = coord(m.group(4));

        Minecraft mc = Minecraft.getInstance();
        Vec3 playerSpawn = mc.player != null
                ? new Vec3(mc.player.getX(), mc.player.getY() + 1.6, mc.player.getZ())
                : null;

        Vec3 best = findBest2x2(new Vec3(x, y, z), playerSpawn);
        landingCache.put(loc, best);
    }

    private static Vec3 findBest2x2(Vec3 hint, Vec3 playerSpawn) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return hint;

        int cx = (int) Math.floor(hint.x);
        int cy = (int) Math.floor(hint.y);
        int cz = (int) Math.floor(hint.z);

        // Two candidates: best reachable (solveSky succeeds), best valid (any non-lava 2x2)
        Vec3   bestReachable   = null;
        double bestReachableD2 = Double.MAX_VALUE;
        Vec3   bestValid       = null;
        double bestValidD2     = Double.MAX_VALUE;

        for (int dy = -3; dy <= 3; dy++) {
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    int bx = cx + dx, by = cy + dy, bz = cz + dz;
                    if (!is2x2Valid(mc, bx, by, bz)) continue;

                    Vec3   cand = new Vec3(bx + 1.0, by + 1.0, bz + 1.0);
                    double d2   = cand.distanceToSqr(hint);

                    if (d2 < bestValidD2) { bestValidD2 = d2; bestValid = cand; }

                    if (playerSpawn != null
                            && TrajectorySolver.solveSky(playerSpawn, cand) != null
                            && d2 < bestReachableD2) {
                        bestReachableD2 = d2;
                        bestReachable   = cand;
                    }
                }
            }
        }

        // Prefer a spot the solver can actually reach; fall back to closest valid 2x2
        if (bestReachable != null) return bestReachable;
        if (bestValid     != null) return bestValid;
        return hint;
    }

    private static boolean is2x2Valid(Minecraft mc, int x, int y, int z) {
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                BlockPos floor = new BlockPos(x + dx, y, z + dz);
                var fs = mc.level.getBlockState(floor);
                if (fs.isAir()
                        || fs.is(Blocks.LAVA)
                        || fs.is(Blocks.FIRE)
                        || fs.is(Blocks.WATER)) return false;
                if (!mc.level.getBlockState(floor.above()).isAir()) return false;
            }
        }
        return true;
    }

    private static PickupLocation parseLoc(String name) {
        String n = name.toLowerCase();
        if (n.contains("shop"))                              return PickupLocation.SHOP;
        if (n.contains("cannon") || n.contains("x_cannon")) return PickupLocation.X_CANNON;
        if (n.contains("square"))                            return PickupLocation.SQUARE;
        if (n.contains("triangle"))                          return PickupLocation.TRIANGLE;
        if (n.contains("slash"))                             return PickupLocation.SLASH;
        if (n.contains("equals"))                            return PickupLocation.EQUALS;
        return null;
    }

    private static double coord(String s) {
        return Double.parseDouble(s.replace(',', '.'));
    }
}