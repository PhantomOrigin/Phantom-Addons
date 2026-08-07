package com.phantomaddons.utils;

import net.minecraft.client.renderer.SubmitNodeCollector;

// 26.2 replaced the old immediate-mode MultiBufferSource.BufferSource model (get a VertexConsumer,
// add vertices, endBatch) with a deferred SubmitNodeCollector.submitCustomGeometry(...) callback,
// only obtainable from inside Fabric API's LevelRenderEvents.COLLECT_SUBMITS event for the current
// frame. Rather than threading a new parameter through every renderer's render(...) signature (and
// every call site), the dispatcher stashes the current frame's collector here before calling them,
// so each renderer just reads it directly — same pattern as reading Minecraft.getInstance().
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
