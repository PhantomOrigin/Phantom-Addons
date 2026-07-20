package com.phantomaddons.features.supplies.pearlwaypoints;

import net.minecraft.world.phys.Vec3;

public record PearlWaypointState(
        PearlLocation target,
        Vec3          centerAimDir,
        Vec3[]        cornerAimDirs,
        boolean       isSky,
        boolean       isDouble,
        boolean       isMyTarget,
        long          optimalFlightMs,
        long          throwInMs
) {}