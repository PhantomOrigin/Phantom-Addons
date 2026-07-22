package com.phantomaddons.features.supplies;

import com.phantomaddons.PhantomConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SupplyWaypointTracker {

    private static final int PING_TICK            = 182;
    private static final int PING_TICK_ANNOUNCE   = 190;

    private static final double LOAD_RANGE = 80.0;

    private static final AABB ARENA = new AABB(-135, 60, -130, -65, 100, -65);

    private static final String[]  SPOT_NAMES = {
            "Shop", "Triangle", "X", "X Cannon", "Equals", "Slash", "Square"
    };
    private static final Vec3[] SPOT_POS = {
            new Vec3( -81.0, 76.0, -143.0),   // Shop
            new Vec3( -67.5, 77.0, -122.5),   // Triangle
            new Vec3(-142.5, 77.0, -148.0),   // X
            new Vec3(-143.0, 76.0, -125.0),   // X Cannon
            new Vec3( -65.5, 76.0,  -87.5),   // Equals
            new Vec3(-113.5, 77.0,  -68.5),   // Slash
            new Vec3(-143.0, 76.0,  -80.0),   // Square
    };

    private static final boolean[] IS_SECOND_SUPPLY = {
            true,   // 0 Shop
            false,  // 1 Triangle
            false,  // 2 X
            true,   // 3 X Cannon
            false,  // 4 Equals
            false,  // 5 Slash
            true,   // 6 Square
    };

    private static final Pattern PING_PATTERN = Pattern.compile(
            "\\[Phantom] ([\\w ]+) x: (-?\\d+\\.\\d+) y: (-?\\d+\\.\\d+) z: (-?\\d+\\.\\d+)");

    private static int ticksSinceStart         = -1;
    private static boolean pingFired           = false;
    private static boolean pingAnnounceFired   = false;

    public static final List<SupplyCluster> detectedClusters = new ArrayList<>();
    public static int playerSpotIdx = -1; // set at tick 182 when pings fire

    public static final Map<String, Vec3> pingBeacons = new LinkedHashMap<>();

    private SupplyWaypointTracker() {}

    public static void onSuppliesStart() {
        ticksSinceStart        = 0;
        pingFired              = false;
        pingAnnounceFired      = false;
        detectedClusters.clear();
    }

    public static void reset() {
        ticksSinceStart        = -1;
        pingFired              = false;
        pingAnnounceFired      = false;
        playerSpotIdx          = -1;
        detectedClusters.clear();
        pingBeacons.clear();
    }

    public static void tick(Minecraft mc) {
        boolean needsTracking = PhantomConfig.isSupplyBeaconsEnabled()
                || PhantomConfig.isNoPreAnnounceEnabled()
                || PhantomConfig.isSupplyLocationAnnounceEnabled()
                || PhantomConfig.isSupplyHitboxEnabled()
                || PhantomConfig.isSupplyRodRadiusEnabled()
                || PhantomConfig.isSupplyPearlHitboxEnabled()
                || PhantomConfig.isCratePriorityEnabled();
        if (!needsTracking) return;
        if (mc.player == null || mc.level == null) return;
        if (ticksSinceStart < 0) return;

        ticksSinceStart++;
        updateClusters(mc);

        if (!pingFired && ticksSinceStart == PING_TICK) {
            pingFired = true;
            playerSpotIdx = closestSpotIndex(mc.player.position());
            if (PhantomConfig.isSupplyLocationAnnounceEnabled()) firePings(mc);
        }

        if (!pingAnnounceFired && ticksSinceStart == PING_TICK_ANNOUNCE) {
            pingAnnounceFired = true;
        }

        if (PhantomConfig.isSupplyBeaconsEnabled()) validatePingBeacons(mc);
    }

    private static final double ZOMBIE_MATCH_RANGE_SQ = 4.0;

    private static void updateClusters(Minecraft mc) {
        detectedClusters.clear();

        List<Vec3> roughCenters = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Giant giant)) continue;
            if (giant.getY() >= 67.0) continue;
            double angle = (giant.getYRot() + 130.0) * Math.PI / 180.0;
            roughCenters.add(new Vec3(
                    giant.getX() + 0.5 + 3.7 * Math.cos(angle),
                    75.0,
                    giant.getZ() + 0.5 + 3.7 * Math.sin(angle)
            ));
        }
        if (roughCenters.isEmpty()) return;

        List<Zombie> zombies = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Zombie z && z.isAlive()) zombies.add(z);
        }

        for (Vec3 rough : roughCenters) {
            detectedClusters.add(new SupplyCluster(List.of(refinedCenter(rough, zombies))));
        }
    }

    private static Vec3 refinedCenter(Vec3 rough, List<Zombie> zombies) {
        double sx = 0, sy = 0, sz = 0;
        int count = 0;
        for (Zombie z : zombies) {
            double dx = z.getX() - rough.x, dz = z.getZ() - rough.z;
            if (dx * dx + dz * dz > ZOMBIE_MATCH_RANGE_SQ) continue;
            sx += z.getX(); sy += z.getY(); sz += z.getZ();
            count++;
        }
        return count > 0 ? new Vec3(sx / count, sy / count, sz / count) : rough;
    }
    
    private static final double PING_MAX_DIST = 20.0;

    private static void firePings(Minecraft mc) {
        if (mc.getConnection() == null) return;

        SupplyCluster best        = null;
        int           bestSpotIdx = -1;
        double        bestDist    = Double.MAX_VALUE;
        for (SupplyCluster cluster : detectedClusters) {
            for (int i = 0; i < SPOT_POS.length; i++) {
                double d = cluster.center.distanceTo(SPOT_POS[i]);
                if (d < bestDist) { bestDist = d; best = cluster; bestSpotIdx = i; }
            }
        }

        if (best == null || bestDist > PING_MAX_DIST) return;
        if (!IS_SECOND_SUPPLY[bestSpotIdx]) return;

        Vec3   pos = best.center;
        String msg = String.format("[Phantom] %s x: %.2f y: %.2f z: %.2f",
                SPOT_NAMES[bestSpotIdx], pos.x, pos.y, pos.z);
        PartyChatQueue.send(msg);
    }

    public static void onChat(String raw) {
        if (!PhantomConfig.isSupplyBeaconsEnabled()) return;

        Matcher m = PING_PATTERN.matcher(raw);
        if (!m.find()) return;

        String zoneName = m.group(1);
        double x = Double.parseDouble(m.group(2));
        double y = Double.parseDouble(m.group(3));
        double z = Double.parseDouble(m.group(4));

        pingBeacons.put(zoneName, new Vec3(x, y, z));
    }

    private static void validatePingBeacons(Minecraft mc) {
        Vec3 playerPos = mc.player.position();
        Iterator<Map.Entry<String, Vec3>> it = pingBeacons.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, Vec3> entry = it.next();
            Vec3 beacon = entry.getValue();

            if (playerPos.distanceTo(beacon) <= LOAD_RANGE) {
                // Player can see entities here — if no cluster nearby, supply is gone
                boolean present = detectedClusters.stream()
                        .anyMatch(c -> c.center.distanceTo(beacon) < 5.0);
                if (!present) it.remove();
            }
        }
    }

    private static final double X_XCANNON_Z_BOUNDARY = -136.0;

    private static int closestSpotIndex(Vec3 pos) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < SPOT_POS.length; i++) {
            double d = pos.distanceTo(SPOT_POS[i]);
            if (d < bestDist) { bestDist = d; best = i; }
        }
        if (best == 2 || best == 3) { // X, X Cannon
            return pos.z < X_XCANNON_Z_BOUNDARY ? 2 : 3;
        }
        return best;
    }
}
