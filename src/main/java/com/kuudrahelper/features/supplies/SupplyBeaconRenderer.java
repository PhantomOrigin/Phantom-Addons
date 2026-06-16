package com.kuudrahelper.features.supplies;

import com.kuudrahelper.KuudraConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class SupplyBeaconRenderer {

    private static final float R = 0.15f;

    private static final double BEACON_BOTTOM_Y = 0.0;
    private static final double BEACON_TOP_Y    = 200.0;

    private SupplyBeaconRenderer() {}

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!KuudraConfig.isSupplyBeaconsEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Vec3 cam = camera.position();
        double cx = cam.x, cy = cam.y, cz = cam.z;
        Matrix4f m = matrices.last().pose();

        MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();
        int alpha = (int)(KuudraConfig.getBeaconAlpha() * 255);

        for (Vec3 pos : SupplyWaypointTracker.pingBeacons.values()) {
            drawBeacon(imm, m, pos.x - cx, pos.z - cz, cy, 255, 170, 0, alpha);
        }

        Vec3 playerPos = mc.player.position();
        for (SupplyCluster cluster : SupplyWaypointTracker.detectedClusters) {
            if (cluster.center.distanceTo(playerPos) > 3.0) {
                drawBeacon(imm, m,
                        cluster.center.x - cx,
                        cluster.center.z - cz,
                        cy, 0, 200, 255, alpha);
            }
        }
    }

    private static void drawBeacon(MultiBufferSource.BufferSource imm, Matrix4f m,
                                    double bx, double bz, double cy,
                                    int r, int g, int b, int a) {
        double y0 = BEACON_BOTTOM_Y - cy;
        double y1 = BEACON_TOP_Y   - cy;

        VertexConsumer vc = imm.getBuffer(RenderTypes.debugQuads());
        addBeam(vc, m, bx, y0, y1, bz, r, g, b, a);
        imm.endBatch();
    }

    private static void addBeam(VertexConsumer vc, Matrix4f m,
                                 double bx, double y0, double y1, double bz,
                                 int r, int g, int b, int a) {
        float x0 = (float)(bx - R), x1 = (float)(bx + R);
        float z0 = (float)(bz - R), z1 = (float)(bz + R);
        float bottom = (float) y0;
        float top    = (float) y1;

        quad(vc, m, x0, bottom, z1, x1, bottom, z1, x1, top, z1, x0, top, z1, r, g, b, a,  0, 0,  1);
        quad(vc, m, x1, bottom, z0, x0, bottom, z0, x0, top, z0, x1, top, z0, r, g, b, a,  0, 0, -1);
        quad(vc, m, x1, bottom, z1, x1, bottom, z0, x1, top, z0, x1, top, z1, r, g, b, a,  1, 0,  0);
        quad(vc, m, x0, bottom, z0, x0, bottom, z1, x0, top, z1, x0, top, z0, r, g, b, a, -1, 0,  0);
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                              float x0, float y0, float z0,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float x3, float y3, float z3,
                              int r, int g, int b, int a,
                              float nx, float ny, float nz) {
        vc.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setNormal(nx, ny, nz);
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz);
        vc.addVertex(m, x2, y2, z2).setColor(r, g, b, a).setNormal(nx, ny, nz);
        vc.addVertex(m, x3, y3, z3).setColor(r, g, b, a).setNormal(nx, ny, nz);
    }
}
