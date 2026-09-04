package com.phantomaddons.utils;

import net.minecraft.client.renderer.SubmitNodeCollector;

public final class WorldRenderCollector {

    private static SubmitNodeCollector current;

    private WorldRenderCollector() {}

    public static void set(SubmitNodeCollector collector) {
        current = collector;
    }

    public static SubmitNodeCollector get() {
        return current;
    }
}
