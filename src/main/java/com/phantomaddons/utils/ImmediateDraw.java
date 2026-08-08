package com.phantomaddons.utils;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.ArrayList;
import java.util.List;

public final class ImmediateDraw {

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
}
