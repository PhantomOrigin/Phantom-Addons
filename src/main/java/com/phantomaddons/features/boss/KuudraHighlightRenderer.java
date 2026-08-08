package com.phantomaddons.features.boss;

import com.phantomaddons.PhantomConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
//? if <26.2 {
/*import net.minecraft.client.renderer.MultiBufferSource;
import org.lwjgl.opengl.GL11;
*///?} else {
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.phantomaddons.utils.AlwaysOnTopRenderTypes;
import com.phantomaddons.utils.WorldRenderCollector;
//?}
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
//? if <26.2 {
/*import net.minecraft.world.entity.monster.Slime;
*///?} else {
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
//?}
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class KuudraHighlightRenderer {

    private static final int KUUDRA_SIZE = 30;
    private static final int R = 255, G = 0, B = 0;
    private static final int OUTLINE_A = 255;
    private static final int FILL_A    = 13; // ~5% opacity

    private KuudraHighlightRenderer() {}

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!PhantomConfig.isKuudraHighlightEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3     cam = camera.position();
        double   cx  = cam.x, cy = cam.y, cz = cam.z;
        Matrix4f m   = matrices.last().pose();

        boolean filled = PhantomConfig.isKuudraHighlightFilled();
        //? if <26.2 {
        /*MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof AbstractCubeMob slime) || slime.getSize() != KUUDRA_SIZE) continue;

            AABB bb = slime.getBoundingBox().inflate(-0.15);
            double x1 = bb.minX - cx, y1 = bb.minY - cy, z1 = bb.minZ - cz;
            double x2 = bb.maxX - cx, y2 = bb.maxY - cy, z2 = bb.maxZ - cz;

            if (filled) {
                VertexConsumer vf = imm.getBuffer(RenderTypes.debugQuads());
                addFill(vf, m, x1, y1, z1, x2, y2, z2);
                imm.endBatch();
            }

            GL11.glDepthFunc(GL11.GL_ALWAYS);
            VertexConsumer vl = imm.getBuffer(RenderTypes.lines());
            addOutline(vl, m, x1, y1, z1, x2, y2, z2);
            imm.endBatch();
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        }
        *///?} else {
        SubmitNodeCollector collector = WorldRenderCollector.get();
        if (collector == null) return;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof AbstractCubeMob slime) || slime.getSize() != KUUDRA_SIZE) continue;

            AABB bb = slime.getBoundingBox().inflate(-0.15);
            double x1 = bb.minX - cx, y1 = bb.minY - cy, z1 = bb.minZ - cz;
            double x2 = bb.maxX - cx, y2 = bb.maxY - cy, z2 = bb.maxZ - cz;

            matrices.pushPose();
            matrices.translate((x1 + x2) / 2.0, (y1 + y2) / 2.0, (z1 + z2) / 2.0);

            if (filled) {
                collector.submitCustomGeometry(matrices, RenderTypes.debugQuads(),
                        (pose, vf) -> addFill(vf, m, x1, y1, z1, x2, y2, z2));
            }

            collector.submitCustomGeometry(matrices, AlwaysOnTopRenderTypes.lines(),
                    (pose, vl) -> addOutline(vl, m, x1, y1, z1, x2, y2, z2));

            matrices.popPose();
        }
        //?}
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
        vc.addVertex(m, x1, y1, z1).setColor(R, G, B, OUTLINE_A).setNormal(nx, ny, nz).setLineWidth(3.0f);
        vc.addVertex(m, x2, y2, z2).setColor(R, G, B, OUTLINE_A).setNormal(nx, ny, nz).setLineWidth(3.0f);
    }

    private static void addFill(VertexConsumer vc, Matrix4f m,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2) {
        float ax = (float) x1, ay = (float) y1, az = (float) z1;
        float bx = (float) x2, by = (float) y2, bz = (float) z2;
        // Bottom
        quad(vc, m, ax,ay,az, bx,ay,az, bx,ay,bz, ax,ay,bz,  0,-1, 0);
        quad(vc, m, ax,ay,bz, bx,ay,bz, bx,ay,az, ax,ay,az,  0, 1, 0);
        // Top
        quad(vc, m, ax,by,az, ax,by,bz, bx,by,bz, bx,by,az,  0, 1, 0);
        quad(vc, m, bx,by,az, bx,by,bz, ax,by,bz, ax,by,az,  0,-1, 0);
        // North (−Z)
        quad(vc, m, ax,ay,az, ax,by,az, bx,by,az, bx,ay,az,  0, 0,-1);
        quad(vc, m, bx,ay,az, bx,by,az, ax,by,az, ax,ay,az,  0, 0, 1);
        // South (+Z)
        quad(vc, m, ax,ay,bz, bx,ay,bz, bx,by,bz, ax,by,bz,  0, 0, 1);
        quad(vc, m, ax,by,bz, bx,by,bz, bx,ay,bz, ax,ay,bz,  0, 0,-1);
        // West (−X)
        quad(vc, m, ax,ay,az, ax,ay,bz, ax,by,bz, ax,by,az, -1, 0, 0);
        quad(vc, m, ax,by,az, ax,by,bz, ax,ay,bz, ax,ay,az,  1, 0, 0);
        // East (+X)
        quad(vc, m, bx,ay,az, bx,by,az, bx,by,bz, bx,ay,bz,  1, 0, 0);
        quad(vc, m, bx,ay,bz, bx,by,bz, bx,by,az, bx,ay,az, -1, 0, 0);
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float nx, float ny, float nz) {
        vc.addVertex(m, x0, y0, z0).setColor(R, G, B, FILL_A).setNormal(nx, ny, nz);
        vc.addVertex(m, x1, y1, z1).setColor(R, G, B, FILL_A).setNormal(nx, ny, nz);
        vc.addVertex(m, x2, y2, z2).setColor(R, G, B, FILL_A).setNormal(nx, ny, nz);
        vc.addVertex(m, x3, y3, z3).setColor(R, G, B, FILL_A).setNormal(nx, ny, nz);
    }
}
