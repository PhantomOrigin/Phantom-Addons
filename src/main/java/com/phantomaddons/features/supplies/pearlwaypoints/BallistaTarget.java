package com.phantomaddons.features.supplies.pearlwaypoints;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class BallistaTarget {

    private static final double MIN_X = -105.5, MAX_X = -97.5;
    private static final double MIN_Z = -109.5, MAX_Z = -100.5;

    public static final double LANDING_Y = 79.0;

    public static final Vec3 CENTER = new Vec3((MIN_X + MAX_X) / 2, LANDING_Y, (MIN_Z + MAX_Z) / 2);

    private BallistaTarget() {}

    public static Vec3 closestLanding(Vec3 pos) {
        return new Vec3(Mth.clamp(pos.x, MIN_X, MAX_X), LANDING_Y, Mth.clamp(pos.z, MIN_Z, MAX_Z));
    }
}
