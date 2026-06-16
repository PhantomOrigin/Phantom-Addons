package com.kuudrahelper.features.supplies;

import com.kuudrahelper.KuudraConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SupplyWaypointTracker {

    private static final int PING_TICK            = 182; // 9.1s — supply location ping + no-pre detection
    private static final int PING_TICK_ANNOUNCE   = 190; // 9.5s — no-pre announcement

    private static final double LOAD_RANGE = 80.0;

    private static final AABB ARENA = new AABB(-135, 60, -130, -65, 100, -65);

    // ── Supply spots (known pickup positions, one per crate) ─────────────────

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
    // Zone name sent in party ping — 3 possible values regardless of which spot the supply is at
    private static final String[] SPOT_ZONE = {
            "Shop",     // 0 Shop
            "Shop",     // 1 Triangle → same zone as Shop
            "X Cannon", // 2 X         → X Cannon zone
            "X Cannon", // 3 X Cannon
            "Square",   // 4 Equals    → Square zone
            "Square",   // 5 Slash     → Square zone
            "Square",   // 6 Square
    };

    private static final Pattern PING_PATTERN = Pattern.compile(
            "\\[Phantom] ([\\w ]+) x: (-?\\d+\\.\\d+) y: (-?\\d+\\.\\d+) z: (-?\\d+\\.\\d+)");

    // ── State ─────────────────────────────────────────────────────────────────

    private static int ticksSinceStart         = -1;
    private static boolean pingFired           = false;
    private static boolean pingAnnounceFired   = false;

    public static final List<SupplyCluster> detectedClusters = new ArrayList<>();
    public static int playerSpotIdx = -1; // set at tick 182 when pings fire

    public static final Map<String, Vec3> pingBeacons = new LinkedHashMap<>();

    private SupplyWaypointTracker() {}

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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

    // ── Tick ──────────────────────────────────────────────────────────────────

    public static void tick(Minecraft mc) {
        boolean needsTracking = KuudraConfig.isSupplyBeaconsEnabled()
                || KuudraConfig.isNoPreAnnounceEnabled()
                || KuudraConfig.isSupplyLocationAnnounceEnabled()
                || KuudraConfig.isSupplyHitboxEnabled()
                || KuudraConfig.isSupplyRodRadiusEnabled()
                || KuudraConfig.isSupplyPearlHitboxEnabled();
        if (!needsTracking) return;
        if (mc.player == null || mc.level == null) return;
        if (ticksSinceStart < 0) return;

        ticksSinceStart++;
        updateClusters(mc);

        if (!pingFired && ticksSinceStart == PING_TICK) {
            pingFired = true;
            if (KuudraConfig.isSupplyLocationAnnounceEnabled()) firePings(mc);
        }

        if (!pingAnnounceFired && ticksSinceStart == PING_TICK_ANNOUNCE) {
            pingAnnounceFired = true;
        }

        if (KuudraConfig.isSupplyBeaconsEnabled()) validatePingBeacons(mc);
    }

    // ── Entity clustering ─────────────────────────────────────────────────────

    private static void updateClusters(Minecraft mc) {
        detectedClusters.clear();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Giant giant)) continue;
            if (giant.getY() >= 67.0) continue;
            double angle = (giant.getYRot() + 130.0) * Math.PI / 180.0;
            Vec3 crate = new Vec3(
                    giant.getX() + 0.5 + 3.7 * Math.cos(angle),
                    75.0,
                    giant.getZ() + 0.5 + 3.7 * Math.sin(angle)
            );
            detectedClusters.add(new SupplyCluster(List.of(crate)));
        }
    }

    // ── Pinging ───────────────────────────────────────────────────────────────

    private static void firePings(Minecraft mc) {
        if (mc.getConnection() == null) return;

        playerSpotIdx = closestSpotIndex(mc.player.position());
        Vec3  playerSpotPos = SPOT_POS[playerSpotIdx];
        String zoneName     = SPOT_ZONE[playerSpotIdx];

        // Ping only the single cluster closest to the player's own pre spot,
        // labelled with that spot's zone name — avoids misidentifying e.g. Triangle as Shop.
        SupplyCluster best     = null;
        double        bestDist = Double.MAX_VALUE;
        for (SupplyCluster cluster : detectedClusters) {
            double d = cluster.center.distanceTo(playerSpotPos);
            if (d < bestDist) { bestDist = d; best = cluster; }
        }

        if (best == null) return;

        Vec3   pos = best.center;
        String msg = String.format("[Phantom] %s x: %.2f y: %.2f z: %.2f",
                zoneName, pos.x, pos.y, pos.z);
        mc.getConnection().sendCommand("pc " + msg);
    }

    // ── Chat parsing ──────────────────────────────────────────────────────────

    public static void onChat(String raw) {
        if (!KuudraConfig.isSupplyBeaconsEnabled()) return;

        Matcher m = PING_PATTERN.matcher(raw);
        if (!m.find()) return;

        String zoneName = m.group(1);
        double x = Double.parseDouble(m.group(2));
        double y = Double.parseDouble(m.group(3));
        double z = Double.parseDouble(m.group(4));

        pingBeacons.put(zoneName, new Vec3(x, y, z));
    }

    // ── Beacon invalidation ───────────────────────────────────────────────────

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int closestSpotIndex(Vec3 pos) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < SPOT_POS.length; i++) {
            double d = pos.distanceTo(SPOT_POS[i]);
            if (d < bestDist) { bestDist = d; best = i; }
        }
        return best;
    }
}
