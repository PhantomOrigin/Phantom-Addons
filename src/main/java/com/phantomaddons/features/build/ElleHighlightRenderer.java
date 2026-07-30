package com.phantomaddons.features.build;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.phase.KuudraPhaseTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class ElleHighlightRenderer {

    private static final int BOX_R = 255, BOX_G = 0, BOX_B = 0;
    private static final int OUTLINE_A = 255;

    private static final int BCN_R = 0, BCN_G = 255, BCN_B = 255;
    private static final float BCN_W = 0.15f;
    private static final float BCN_TOP = 200f;
    private static final int BCN_A = 160;

    private ElleHighlightRenderer() {}

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!PhantomConfig.isElleHighlightEnabled()) return;
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.BUILD) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity elle = findElle(mc);
        if (elle == null) return;

        Vec3     cam = camera.position();
        Matrix4f m   = matrices.last().pose();
        MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();

        AABB bb = elle.getBoundingBox();
        double x1 = bb.minX - cam.x, y1 = bb.minY - cam.y, z1 = bb.minZ - cam.z;
        double x2 = bb.maxX - cam.x, y2 = bb.maxY - cam.y, z2 = bb.maxZ - cam.z;

        GL11.glDepthFunc(GL11.GL_ALWAYS);
        VertexConsumer lines = imm.getBuffer(RenderTypes.lines());
        addOutline(lines, m, x1, y1, z1, x2, y2, z2);
        imm.endBatch();
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        if (!PhantomConfig.isElleHighlightBeaconEnabled()) return;

        double bx = (bb.minX + bb.maxX) / 2.0 - cam.x;
        double by = bb.minY - cam.y;
        double bz = (bb.minZ + bb.maxZ) / 2.0 - cam.z;

        for (int pass = 0; pass < 2; pass++) {
            if (pass == 1) GL11.glDepthFunc(GL11.GL_ALWAYS);
            VertexConsumer quads = imm.getBuffer(RenderTypes.debugQuads());
            addBeam(quads, m, bx, by, bz);
            imm.endBatch();
            if (pass == 1) GL11.glDepthFunc(GL11.GL_LEQUAL);
        }
    }

    private static Entity findElle(Minecraft mc) {
        for (Entity e : mc.level.entitiesForRendering()) {
            String name = e.getName().getString();
            if (name.equals("Elle") || name.contains("Elle")) return e;
        }
        return null;
    }

    private static void addOutline(VertexConsumer vc, Matrix4f m,
                                   double x1, double y1, double z1,
                                   double x2, double y2, double z2) {
        float ax = (float) x1, ay = (float) y1, az = (float) z1;
        float bx = (float) x2, by = (float) y2, bz = (float) z2;
        line(vc, m, ax, ay, az, bx, ay, az,  1, 0, 0);
        line(vc, m, bx, ay, az, bx, ay, bz,  0, 0, 1);
        line(vc, m, bx, ay, bz, ax, ay, bz, -1, 0, 0);
        line(vc, m, ax, ay, bz, ax, ay, az,  0, 0,-1);
        line(vc, m, ax, by, az, bx, by, az,  1, 0, 0);
        line(vc, m, bx, by, az, bx, by, bz,  0, 0, 1);
        line(vc, m, bx, by, bz, ax, by, bz, -1, 0, 0);
        line(vc, m, ax, by, bz, ax, by, az,  0, 0,-1);
        line(vc, m, ax, ay, az, ax, by, az,  0, 1, 0);
        line(vc, m, bx, ay, az, bx, by, az,  0, 1, 0);
        line(vc, m, bx, ay, bz, bx, by, bz,  0, 1, 0);
        line(vc, m, ax, ay, bz, ax, by, bz,  0, 1, 0);
    }

    private static void line(VertexConsumer vc, Matrix4f m,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float nx, float ny, float nz) {
        vc.addVertex(m, x1, y1, z1).setColor(BOX_R, BOX_G, BOX_B, OUTLINE_A).setNormal(nx, ny, nz).setLineWidth(2.0f);
        vc.addVertex(m, x2, y2, z2).setColor(BOX_R, BOX_G, BOX_B, OUTLINE_A).setNormal(nx, ny, nz).setLineWidth(2.0f);
    }

    private static void addBeam(VertexConsumer vc, Matrix4f m,
                                double bx, double by, double bz) {
        float x0 = (float)(bx - BCN_W), x1 = (float)(bx + BCN_W);
        float z0 = (float)(bz - BCN_W), z1 = (float)(bz + BCN_W);
        float y0 = (float) by;
        float y1 = (float)(by + BCN_TOP);
        quad(vc, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1,  0, 0, 1);
        quad(vc, m, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0,  0, 0,-1);
        quad(vc, m, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1,  1, 0, 0);
        quad(vc, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0);
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float nx, float ny, float nz) {
        vc.addVertex(m, x0, y0, z0).setColor(BCN_R, BCN_G, BCN_B, BCN_A).setNormal(nx, ny, nz);
        vc.addVertex(m, x1, y1, z1).setColor(BCN_R, BCN_G, BCN_B, BCN_A).setNormal(nx, ny, nz);
        vc.addVertex(m, x2, y2, z2).setColor(BCN_R, BCN_G, BCN_B, BCN_A).setNormal(nx, ny, nz);
        vc.addVertex(m, x3, y3, z3).setColor(BCN_R, BCN_G, BCN_B, BCN_A).setNormal(nx, ny, nz);
    }
}
