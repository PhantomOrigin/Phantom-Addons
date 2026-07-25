package com.phantomaddons.features.miscskyblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class BobberComparisonRenderer {

    private static final float CROSS_SIZE = 0.25f;
    private static final int GHOST_COLOR_A = 255, GHOST_COLOR_B = 255, GHOST_COLOR_C = 0;   // yellow
    private static final int REAL_COLOR_A  = 0,   REAL_COLOR_B  = 255, REAL_COLOR_C  = 255; // cyan
    private static final int LINK_A = 255, LINK_B = 255, LINK_C = 255; // white
    private static final int ALPHA = 220;

    private BobberComparisonRenderer() {}

    public static void render(PoseStack matrices, Camera camera) {
        if (!FishingHookDebugTracker.isEnabled()) return;

        Vec3 cam = camera.position();
        Matrix4f m = matrices.last().pose();
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();
        VertexConsumer vc = imm.getBuffer(RenderTypes.lines());

        GL11.glDepthFunc(GL11.GL_ALWAYS);

        drawPair(vc, m, cam, PredictedBobber.getDebugEntryPos(), FishingHookDebugTracker.getEntryPos());
        drawPair(vc, m, cam, PredictedBobber.getDebugLowestPos(), FishingHookDebugTracker.getLowestPos());
        drawPair(vc, m, cam, PredictedBobber.getDebugRestPos(), FishingHookDebugTracker.getRestPos());

        imm.endBatch(RenderTypes.lines());
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }

    private static void drawPair(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3 ghost, Vec3 real) {
        if (ghost != null) cross(vc, m, cam, ghost, GHOST_COLOR_A, GHOST_COLOR_B, GHOST_COLOR_C);
        if (real != null) cross(vc, m, cam, real, REAL_COLOR_A, REAL_COLOR_B, REAL_COLOR_C);
        if (ghost != null && real != null) {
            line(vc, m, cam.x, cam.y, cam.z, ghost, real, LINK_A, LINK_B, LINK_C);
        }
    }

    private static void cross(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3 p, int r, int g, int b) {
        double x = p.x - cam.x, y = p.y - cam.y, z = p.z - cam.z;
        line(vc, m, x - CROSS_SIZE, y, z, x + CROSS_SIZE, y, z, r, g, b);
        line(vc, m, x, y - CROSS_SIZE, z, x, y + CROSS_SIZE, z, r, g, b);
        line(vc, m, x, y, z - CROSS_SIZE, x, y, z + CROSS_SIZE, r, g, b);
    }

    private static void line(VertexConsumer vc, Matrix4f m,
                             double camX, double camY, double camZ,
                             Vec3 from, Vec3 to,
                             int r, int g, int b) {
        line(vc, m, from.x - camX, from.y - camY, from.z - camZ,
                to.x - camX, to.y - camY, to.z - camZ, r, g, b);
    }

    private static void line(VertexConsumer vc, Matrix4f m,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             int r, int g, int b) {
        vc.addVertex(m, (float) x1, (float) y1, (float) z1).setColor(r, g, b, ALPHA).setNormal(1, 0, 0).setLineWidth(2.5f);
        vc.addVertex(m, (float) x2, (float) y2, (float) z2).setColor(r, g, b, ALPHA).setNormal(1, 0, 0).setLineWidth(2.5f);
    }
}
