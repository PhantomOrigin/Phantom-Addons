package com.kuudrahelper.features.pearls;

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