package com.phantomaddons.features.supplies.waypointlines;

import com.phantomaddons.PhantomConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class WaypointLinesRenderer {

    private static final int R = 255, G = 235, B = 60;
    private static final int ALPHA = 200;

    private WaypointLinesRenderer() {}

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!PhantomConfig.isWaypointLinesEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 target = WaypointLines.getTarget(mc.player);
        if (target == null) return;

        Vec3 camPos = camera.position();
        Vec3 direction = Vec3.directionFromRotation(camera.xRot(), camera.yRot());
        Vec3 start = camPos.add(direction);
        Matrix4f m  = matrices.last().pose();

        MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();
        VertexConsumer lines = imm.getBuffer(RenderTypes.lines());

        GL11.glDepthFunc(GL11.GL_ALWAYS);
        line(lines, m,
                (float)(start.x  - camPos.x), (float)(start.y  - camPos.y), (float)(start.z  - camPos.z),
                (float)(target.x - camPos.x), (float)(target.y - camPos.y), (float)(target.z - camPos.z));
        imm.endBatch(RenderTypes.lines());
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }

    private static void line(VertexConsumer vc, Matrix4f m,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;
        vc.addVertex(m, x1, y1, z1).setColor(R, G, B, ALPHA).setNormal(nx, ny, nz).setLineWidth(3.5f);
        vc.addVertex(m, x2, y2, z2).setColor(R, G, B, ALPHA).setNormal(nx, ny, nz).setLineWidth(3.5f);
    }
}
