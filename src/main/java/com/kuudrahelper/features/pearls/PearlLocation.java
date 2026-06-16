package com.kuudrahelper.features.pearls;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public enum PearlLocation {
    SHOP       ("Shop",     -98.0,  79.0, -113.0),
    TRIANGLE   ("Triangle", -94.0,  79.0, -106.0),
    SLASH      ("Slash",    -98.0,  79.0,  -99.0),
    EQUALS     ("Equals",  -106.0,  79.0,  -99.0),
    X_CANNON   ("X Cannon",-110.0,  79.0, -106.0),
    X          ("X",       -106.0,  79.0, -113.0);

    public final String displayName;
    public final Vec3   landingPos;
    public final Vec3   targetPos;

    public static final double WAYPOINT_HALF_SIZE = 0.25;

    PearlLocation(String name, double x, double y, double z) {
        this.displayName = name;
        this.landingPos  = new Vec3(x, y, z);
        this.targetPos   = new Vec3(x, y + 0.2, z);
    }

    public Vec3 minCorner() {
        return landingPos.subtract(WAYPOINT_HALF_SIZE, WAYPOINT_HALF_SIZE, WAYPOINT_HALF_SIZE);
    }

    public Vec3 maxCorner() {
        return landingPos.add(WAYPOINT_HALF_SIZE, WAYPOINT_HALF_SIZE, WAYPOINT_HALF_SIZE);
    }

    public AABB getWaypointBox() {
        double h      = WAYPOINT_HALF_SIZE;
        Vec3   center = landingPos.add(0, 3.0, 0);
        return new AABB(
                center.x - h, center.y - h, center.z - h,
                center.x + h, center.y + h, center.z + h
        );
    }
}