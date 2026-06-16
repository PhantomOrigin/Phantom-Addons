package com.kuudrahelper.features.pearls;

import net.minecraft.world.phys.Vec3;

public enum PickupLocation {
    SHOP      ("Shop",     new Vec3( -77, 79, -134), PearlLocation.SHOP),
    TRIANGLE  ("Triangle", new Vec3( -67, 77, -122), PearlLocation.TRIANGLE),
    EQUALS    ("Equals",   new Vec3( -64, 76,  -87), PearlLocation.EQUALS),
    SLASH     ("Slash",    new Vec3(-111, 76,  -68), PearlLocation.SLASH),
    SQUARE    ("Square",   new Vec3(-141, 77,  -86), null),
    X_CANNON  ("X Cannon", new Vec3(-134, 79, -126), PearlLocation.X_CANNON),
    X         ("X",        new Vec3(-134, 77, -138), PearlLocation.X);

    public final String        displayName;
    public final Vec3          position;
    public final PearlLocation pearlTarget;

    PickupLocation(String name, Vec3 pos, PearlLocation target) {
        this.displayName = name;
        this.position    = pos;
        this.pearlTarget = target;
    }

    public static PickupLocation closest(Vec3 playerPos) {
        PickupLocation best     = SHOP;
        double         bestDist = Double.MAX_VALUE;
        for (PickupLocation loc : values()) {
            double d = loc.position.distanceToSqr(playerPos);
            if (d < bestDist) { bestDist = d; best = loc; }
        }
        return best;
    }
}