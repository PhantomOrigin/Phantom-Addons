package com.phantomaddons.features.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;


public final class IchorRadiusRenderer {

    private static final float RADIUS   = 8f;
    private static final float HEIGHT   = 0.5f;
    private static final int   SEGMENTS = 32;
    private static final int   OUTLINE_A = 200;
    private static final int   FILL_A    = 60;

    private IchorRadiusRenderer() {}

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        float[] fraction = new float[1];
        Vec3 center = IchorRadiusTracker.getActivePool(fraction);
        if (center == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int[] color = lerpColor(fraction[0]);
        int r = color[0], g = color[1], b = color[2];

        Vec3 cam = camera.position();
        double baseX = center.x - cam.x;
        double baseY = center.y - cam.y;
        double baseZ = center.z - cam.z;

        Matrix4f m = matrices.last().pose();
        MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();

        VertexConsumer vf = imm.getBuffer(RenderTypes.debugQuads());
        addCylinderSideFill(vf, m, baseX, baseY, baseZ, r, g, b, FILL_A);
        imm.endBatch(RenderTypes.debugQuads());

        VertexConsumer vl = imm.getBuffer(RenderTypes.lines());
        addCylinderOutline(vl, m, baseX, baseY, baseZ, r, g, b, OUTLINE_A);
        imm.endBatch(RenderTypes.lines());
    }
    
    private static int[] lerpColor(float fraction) {
        int red   = Math.round((1f - fraction) * 255f);
        int green = Math.round(fraction * 255f);
        return new int[]{red, green, 0};
    }

    private static void addCylinderSideFill(VertexConsumer vc, Matrix4f m, double bx, double by, double bz,
                                            int r, int g, int b, int a) {
        double topY = by + HEIGHT;
        for (int i = 0; i < SEGMENTS; i++) {
            double a1 = i * (Math.PI * 2 / SEGMENTS);
            double a2 = (i + 1) * (Math.PI * 2 / SEGMENTS);
            double x1 = bx + Math.cos(a1) * RADIUS, z1 = bz + Math.sin(a1) * RADIUS;
            double x2 = bx + Math.cos(a2) * RADIUS, z2 = bz + Math.sin(a2) * RADIUS;

            quad(vc, m, x1, by, z1, x2, by, z2, x2, topY, z2, x1, topY, z1, r, g, b, a);
            quad(vc, m, x2, by, z2, x1, by, z1, x1, topY, z1, x2, topY, z2, r, g, b, a);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             double x0, double y0, double z0,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             double x3, double y3, double z3,
                             int r, int g, int b, int a) {
        vc.addVertex(m, (float) x0, (float) y0, (float) z0).setColor(r, g, b, a).setNormal(0, 1, 0);
        vc.addVertex(m, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a).setNormal(0, 1, 0);
        vc.addVertex(m, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a).setNormal(0, 1, 0);
        vc.addVertex(m, (float) x3, (float) y3, (float) z3).setColor(r, g, b, a).setNormal(0, 1, 0);
    }

    private static void addCylinderOutline(VertexConsumer vc, Matrix4f m, double bx, double by, double bz,
                                           int r, int g, int b, int a) {
        double topY = by + HEIGHT;
        for (int i = 0; i < SEGMENTS; i++) {
            double a1 = i * (Math.PI * 2 / SEGMENTS);
            double a2 = (i + 1) * (Math.PI * 2 / SEGMENTS);
            double x1 = bx + Math.cos(a1) * RADIUS, z1 = bz + Math.sin(a1) * RADIUS;
            double x2 = bx + Math.cos(a2) * RADIUS, z2 = bz + Math.sin(a2) * RADIUS;

            line(vc, m, x1, by, z1, x2, by, z2, r, g, b, a);
            line(vc, m, x1, topY, z1, x2, topY, z2, r, g, b, a);
            if (i % 4 == 0) line(vc, m, x1, by, z1, x1, topY, z1, r, g, b, a);
        }
    }

    private static void line(VertexConsumer vc, Matrix4f m,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             int r, int g, int b, int a) {
        vc.addVertex(m, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a).setNormal(1, 0, 0).setLineWidth(2.5f);
        vc.addVertex(m, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a).setNormal(1, 0, 0).setLineWidth(2.5f);
    }
}
