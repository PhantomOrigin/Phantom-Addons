package com.phantomaddons.data.tentacle;

import com.phantomaddons.PhantomConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
//? if <26.2 {
/*import net.minecraft.client.renderer.MultiBufferSource;
*///?} else {
import net.minecraft.client.renderer.SubmitNodeCollector;
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

public final class TentacleHighlightRenderer {

    private static final int OUTLINE_A = 220;
    private static final int FILL_A    = 30;

    private TentacleHighlightRenderer() {}

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!PhantomConfig.isTentacleHighlightEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 cam = camera.position();
        Matrix4f m = matrices.last().pose();

        //? if <26.2 {
        /*MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();
        boolean drewAny = false;
        *///?} else {
        SubmitNodeCollector collector = WorldRenderCollector.get();
        if (collector == null) return;
        //?}

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof AbstractCubeMob mob)) continue;
            Long zoneId = TentacleTracker.getZoneIdForEntity(mob.getUUID());
            if (zoneId == null) continue;

            int[] rgb = colorForZone(zoneId);
            AABB bb = mob.getBoundingBox().inflate(0.05);
            double x1 = bb.minX - cam.x, y1 = bb.minY - cam.y, z1 = bb.minZ - cam.z;
            double x2 = bb.maxX - cam.x, y2 = bb.maxY - cam.y, z2 = bb.maxZ - cam.z;

            //? if <26.2 {
            /*addFill(imm.getBuffer(RenderTypes.debugQuads()), m, x1, y1, z1, x2, y2, z2, rgb);
            addOutline(imm.getBuffer(RenderTypes.lines()), m, x1, y1, z1, x2, y2, z2, rgb);
            drewAny = true;
            *///?} else {
            matrices.pushPose();
            matrices.translate((x1 + x2) / 2.0, (y1 + y2) / 2.0, (z1 + z2) / 2.0);
            collector.submitCustomGeometry(matrices, RenderTypes.debugQuads(),
                    (pose, vf) -> addFill(vf, m, x1, y1, z1, x2, y2, z2, rgb));
            collector.submitCustomGeometry(matrices, RenderTypes.lines(),
                    (pose, vc) -> addOutline(vc, m, x1, y1, z1, x2, y2, z2, rgb));
            matrices.popPose();
            //?}
        }

        //? if <26.2 {
        /*if (drewAny) imm.endBatch();
        *///?}
    }

    private static int[] colorForZone(long zoneId) {
        float hue = ((zoneId * 0x9E3779B1L) & 0xFFFFFFFFL) / 4294967296.0f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.85f, 1.0f);
        return new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF};
    }

    private static void addOutline(VertexConsumer vc, Matrix4f m,
                                   double x1, double y1, double z1,
                                   double x2, double y2, double z2,
                                   int[] rgb) {
        int r = rgb[0], g = rgb[1], b = rgb[2];
        float ax = (float) x1, ay = (float) y1, az = (float) z1;
        float bx = (float) x2, by = (float) y2, bz = (float) z2;
        line(vc, m, ax, ay, az, bx, ay, az, r, g, b,  1, 0, 0);
        line(vc, m, bx, ay, az, bx, ay, bz, r, g, b,  0, 0, 1);
        line(vc, m, bx, ay, bz, ax, ay, bz, r, g, b, -1, 0, 0);
        line(vc, m, ax, ay, bz, ax, ay, az, r, g, b,  0, 0,-1);
        line(vc, m, ax, by, az, bx, by, az, r, g, b,  1, 0, 0);
        line(vc, m, bx, by, az, bx, by, bz, r, g, b,  0, 0, 1);
        line(vc, m, bx, by, bz, ax, by, bz, r, g, b, -1, 0, 0);
        line(vc, m, ax, by, bz, ax, by, az, r, g, b,  0, 0,-1);
        line(vc, m, ax, ay, az, ax, by, az, r, g, b,  0, 1, 0);
        line(vc, m, bx, ay, az, bx, by, az, r, g, b,  0, 1, 0);
        line(vc, m, bx, ay, bz, bx, by, bz, r, g, b,  0, 1, 0);
        line(vc, m, ax, ay, bz, ax, by, bz, r, g, b,  0, 1, 0);
    }

    private static void line(VertexConsumer vc, Matrix4f m,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             int r, int g, int b,
                             float nx, float ny, float nz) {
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, OUTLINE_A).setNormal(nx, ny, nz).setLineWidth(2.5f);
        vc.addVertex(m, x2, y2, z2).setColor(r, g, b, OUTLINE_A).setNormal(nx, ny, nz).setLineWidth(2.5f);
    }

    private static void addFill(VertexConsumer vc, Matrix4f m,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                int[] rgb) {
        int r = rgb[0], g = rgb[1], b = rgb[2];
        float ax = (float) x1, ay = (float) y1, az = (float) z1;
        float bx = (float) x2, by = (float) y2, bz = (float) z2;
        quad(vc, m, ax,ay,az, bx,ay,az, bx,ay,bz, ax,ay,bz, r,g,b,  0,-1, 0);
        quad(vc, m, ax,ay,bz, bx,ay,bz, bx,ay,az, ax,ay,az, r,g,b,  0, 1, 0);
        quad(vc, m, ax,by,az, ax,by,bz, bx,by,bz, bx,by,az, r,g,b,  0, 1, 0);
        quad(vc, m, bx,by,az, bx,by,bz, ax,by,bz, ax,by,az, r,g,b,  0,-1, 0);
        quad(vc, m, ax,ay,az, ax,by,az, bx,by,az, bx,ay,az, r,g,b,  0, 0,-1);
        quad(vc, m, bx,ay,az, bx,by,az, ax,by,az, ax,ay,az, r,g,b,  0, 0, 1);
        quad(vc, m, ax,ay,bz, bx,ay,bz, bx,by,bz, ax,by,bz, r,g,b,  0, 0, 1);
        quad(vc, m, ax,by,bz, bx,by,bz, bx,ay,bz, ax,ay,bz, r,g,b,  0, 0,-1);
        quad(vc, m, ax,ay,az, ax,ay,bz, ax,by,bz, ax,by,az, r,g,b, -1, 0, 0);
        quad(vc, m, ax,by,az, ax,by,bz, ax,ay,bz, ax,ay,az, r,g,b,  1, 0, 0);
        quad(vc, m, bx,ay,az, bx,by,az, bx,by,bz, bx,ay,bz, r,g,b,  1, 0, 0);
        quad(vc, m, bx,ay,bz, bx,by,bz, bx,by,az, bx,ay,az, r,g,b, -1, 0, 0);
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             int r, int g, int b,
                             float nx, float ny, float nz) {
        vc.addVertex(m, x0, y0, z0).setColor(r, g, b, FILL_A).setNormal(nx, ny, nz);
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, FILL_A).setNormal(nx, ny, nz);
        vc.addVertex(m, x2, y2, z2).setColor(r, g, b, FILL_A).setNormal(nx, ny, nz);
        vc.addVertex(m, x3, y3, z3).setColor(r, g, b, FILL_A).setNormal(nx, ny, nz);
    }
}
