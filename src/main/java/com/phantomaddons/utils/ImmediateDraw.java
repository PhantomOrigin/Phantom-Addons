package com.phantomaddons.utils;

import com.mojang.blaze3d.vertex.VertexConsumer;
//? if <26.3 {
import net.minecraft.client.renderer.StagedVertexBuffer;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
*///?}
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.ArrayList;
import java.util.List;

public final class ImmediateDraw {

    //? if <26.3 {
    private static final StagedVertexBuffer BUFFER =
            new StagedVertexBuffer(() -> "phantomaddons_immediate", 262144);

    private record Pending(RenderType type, StagedVertexBuffer.Draw draw) {}

    private static final List<Pending> pending = new ArrayList<>();

    private ImmediateDraw() {}

    public static VertexConsumer begin(RenderType type) {
        StagedVertexBuffer.Draw draw = BUFFER.appendDraw(type.format(), type.primitiveTopology());
        pending.add(new Pending(type, draw));
        return BUFFER.getVertexBuilder(draw);
    }

    public static void flush() {
        if (pending.isEmpty()) return;
        BUFFER.upload();
        for (Pending p : pending) {
            StagedVertexBuffer.ExecuteInfo info = BUFFER.getExecuteInfo(p.draw());
            if (info != null) {
                p.type().prepare().drawFromBuffer(info);
            }
        }
        pending.clear();
        BUFFER.endFrame();
    }
    //?} else {
    /*private record Pending(RenderType type, RecordedVertices vertices) {}

    private static final List<Pending> pending = new ArrayList<>();

    private static boolean drawingAfterTerrain;

    private ImmediateDraw() {}

    public static boolean isDrawingAfterTerrain() {
        return drawingAfterTerrain;
    }

    public static VertexConsumer begin(RenderType type) {
        RecordedVertices vertices = new RecordedVertices();
        pending.add(new Pending(type, vertices));
        return vertices;
    }

    public static void flush() {
        SubmitNodeCollector collector = WorldRenderCollector.get();
        if (collector != null && !pending.isEmpty()) {
            PoseStack stack = new PoseStack();
            drawingAfterTerrain = true;
            try {
                for (Pending p : pending) {
                    collector.submitCustomGeometry(stack, p.type(), (pose, consumer) -> p.vertices().replayInto(consumer));
                }
            } finally {
                drawingAfterTerrain = false;
            }
        }
        pending.clear();
    }
    *///?}
}
