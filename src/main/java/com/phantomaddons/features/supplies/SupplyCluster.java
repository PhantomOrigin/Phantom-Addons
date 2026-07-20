package com.phantomaddons.features.supplies;

import net.minecraft.world.phys.Vec3;

import java.util.List;


public final class SupplyCluster {

    public final Vec3 center;
    public final Vec3 pingPos;

    SupplyCluster(List<Vec3> positions) {
        double sx = 0, sy = 0, sz = 0;
        for (Vec3 p : positions) { sx += p.x; sy += p.y; sz += p.z; }
        int n = positions.size();
        center = new Vec3(sx / n, sy / n, sz / n);

        Vec3 closest = positions.get(0), furthest = positions.get(0);
        double minD = Double.MAX_VALUE, maxD = -1;
        for (Vec3 p : positions) {
            double d = center.distanceTo(p);
            if (d < minD) { minD = d; closest = p; }
            if (d > maxD) { maxD = d; furthest = p; }
        }

        pingPos = new Vec3(
                (closest.x + furthest.x) / 2.0,
                75.0,
                (closest.z + furthest.z) / 2.0
        );
    }
}
