package com.phantomaddons.data.tentacle;

import com.phantomaddons.data.GameplayDatabase;

import java.util.ArrayList;
import java.util.List;

public final class TentacleZoneMatcher {

    private static final double MATCH_RADIUS = 4.0;

    private record Zone(long id, double x, double z) {}

    private static final List<Zone> zones = new ArrayList<>();
    private static boolean loaded = false;

    private TentacleZoneMatcher() {}

    public static synchronized void load() {
        if (loaded) return;
        zones.clear();
        zones.addAll(GameplayDatabase.query(
                "SELECT zone_id, anchor_x, anchor_z FROM tentacle_zones",
                rs -> new Zone(rs.getLong("zone_id"), rs.getDouble("anchor_x"), rs.getDouble("anchor_z"))));
        loaded = true;
    }

    public static synchronized long matchOrCreate(double x, double y, double z) {
        if (!loaded) load();

        Zone best = null;
        double bestDistSq = MATCH_RADIUS * MATCH_RADIUS;
        for (Zone zone : zones) {
            double dx = zone.x - x, dz = zone.z - z;
            double distSq = dx * dx + dz * dz;
            if (distSq <= bestDistSq) {
                best = zone;
                bestDistSq = distSq;
            }
        }
        if (best != null) return best.id;

        long id = GameplayDatabase.insert(
                "INSERT INTO tentacle_zones (label, anchor_x, anchor_y, anchor_z, created_at) VALUES (?, ?, ?, ?, ?)",
                null, x, y, z, GameplayDatabase.now());
        zones.add(new Zone(id, x, z));
        return id;
    }
}
