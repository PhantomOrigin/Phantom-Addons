package com.phantomaddons.features.stundps;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.phase.KuudraPhaseTracker;
import com.phantomaddons.phase.KuudraPhaseTracker.Phase;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class StunPreviewRenderer {

    private static final double REF_X = -161, REF_Y = 49, REF_Z = -186;

    private record Pod(int x, int y, int z) {}
    private static final Pod POD_LEFT  = new Pod(-153, 27, -173);   // stand: 152.5 28 -172.5
    private static final Pod POD_RIGHT = new Pod(-168, 28, -168);  // stand: -167.5 29 -167.5
    private static final Pod POD_BACK  = new Pod(-156, 28, -157);  // stand: -155.5 29 -156.5

    private static final int    TR = 30, TG = 195, TB = 255;
    private static final int    T_FILL    = 255;
    private static final int    T_OUTLINE = 255;

    private static final int SR = 80, SG = 255, SB = 80;
    private static final int S_FILL    = 60;
    private static final int S_OUTLINE = 200;

    private StunPreviewRenderer() {}

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!PhantomConfig.isStunPreviewEnabled()) return;
        if (KuudraPhaseTracker.getPhase() != Phase.EATEN) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Vec3   playerPos = mc.player.getPosition(tickDelta);
        double px = playerPos.x, py = playerPos.y, pz = playerPos.z;
        double dx = px - REF_X,  dy = py - REF_Y,  dz = pz - REF_Z;

        Vec3     cam = camera.position();
        double   cx  = cam.x, cy = cam.y, cz = cam.z;
        Matrix4f m   = matrices.last().pose();

        //? if <26.2 {
        /*MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();

        for (int pass = 0; pass < 2; pass++) {
            if (pass == 1) GL11.glDepthFunc(519);

            VertexConsumer vf = imm.getBuffer(RenderTypes.debugQuads());
            if (PhantomConfig.isStunPreviewLeftEnabled())  renderPodFill(vf, m, POD_LEFT,  dx, dy, dz, cx, cy, cz);
            if (PhantomConfig.isStunPreviewRightEnabled()) renderPodFill(vf, m, POD_RIGHT, dx, dy, dz, cx, cy, cz);
            if (PhantomConfig.isStunPreviewBackEnabled())  renderPodFill(vf, m, POD_BACK,  dx, dy, dz, cx, cy, cz);
            imm.endBatch(RenderTypes.debugQuads());

            VertexConsumer vl = imm.getBuffer(RenderTypes.lines());
            if (PhantomConfig.isStunPreviewLeftEnabled())  renderPodOutline(vl, m, POD_LEFT,  dx, dy, dz, cx, cy, cz);
            if (PhantomConfig.isStunPreviewRightEnabled()) renderPodOutline(vl, m, POD_RIGHT, dx, dy, dz, cx, cy, cz);
            if (PhantomConfig.isStunPreviewBackEnabled())  renderPodOutline(vl, m, POD_BACK,  dx, dy, dz, cx, cy, cz);
            imm.endBatch(RenderTypes.lines());

            if (pass == 1) GL11.glDepthFunc(515);
        }
        *///?} else {
        SubmitNodeCollector collector = WorldRenderCollector.get();
        if (collector == null) return;

        for (int pass = 0; pass < 2; pass++) {
            var quadsType = pass == 1 ? AlwaysOnTopRenderTypes.debugQuads() : RenderTypes.debugQuads();
            var linesType = pass == 1 ? AlwaysOnTopRenderTypes.lines()      : RenderTypes.lines();

            if (PhantomConfig.isStunPreviewLeftEnabled())
                submitPod(matrices, collector, m, quadsType, linesType, POD_LEFT, dx, dy, dz, cx, cy, cz);
            if (PhantomConfig.isStunPreviewRightEnabled())
                submitPod(matrices, collector, m, quadsType, linesType, POD_RIGHT, dx, dy, dz, cx, cy, cz);
            if (PhantomConfig.isStunPreviewBackEnabled())
                submitPod(matrices, collector, m, quadsType, linesType, POD_BACK, dx, dy, dz, cx, cy, cz);
        }
        //?}
    }

    //? if >=26.2 {
    private static void submitPod(PoseStack matrices, SubmitNodeCollector collector, Matrix4f m,
                                   net.minecraft.client.renderer.rendertype.RenderType quadsType,
                                   net.minecraft.client.renderer.rendertype.RenderType linesType,
                                   Pod pod, double dx, double dy, double dz, double cx, double cy, double cz) {
        double wx = pod.x() + dx - cx;
        double wy = pod.y() + dy - cy;
        double wz = pod.z() + dz - cz;

        matrices.pushPose();
        matrices.translate(wx + 0.5, wy + 0.5, wz + 0.5);

        collector.submitCustomGeometry(matrices, quadsType, (pose, vf) -> renderPodFill(vf, m, pod, dx, dy, dz, cx, cy, cz));
        collector.submitCustomGeometry(matrices, linesType, (pose, vl) -> renderPodOutline(vl, m, pod, dx, dy, dz, cx, cy, cz));

        matrices.popPose();
    }
    //?}

    private static void renderPodFill(VertexConsumer vf, Matrix4f m, Pod pod,
                                       double dx, double dy, double dz, double cx, double cy, double cz) {
        double wx = pod.x() + dx - cx;
        double wy = pod.y() + dy - cy;
        double wz = pod.z() + dz - cz;
        double sx = pod.x() - cx;
        double sy = pod.y() - cy;
        double sz = pod.z() - cz;
        addFill(vf, m, sx, sy, sz, sx+1, sy+1, sz+1, SR, SG, SB, S_FILL);
        addFill(vf, m, wx, wy, wz, wx+1, wy+1, wz+1, TR, TG, TB, T_FILL);
    }

    private static void renderPodOutline(VertexConsumer vl, Matrix4f m, Pod pod,
                                          double dx, double dy, double dz, double cx, double cy, double cz) {
        double wx = pod.x() + dx - cx;
        double wy = pod.y() + dy - cy;
        double wz = pod.z() + dz - cz;
        double sx = pod.x() - cx;
        double sy = pod.y() - cy;
        double sz = pod.z() - cz;
        addOutline(vl, m, sx, sy, sz, sx+1, sy+1, sz+1, SR, SG, SB, S_OUTLINE);
        addOutline(vl, m, wx, wy, wz, wx+1, wy+1, wz+1, TR, TG, TB, T_OUTLINE);
    }

    private static void addFill(VertexConsumer vc, Matrix4f m,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                int r, int g, int b, int a) {
        float ax=(float)x1, ay=(float)y1, az=(float)z1;
        float bx=(float)x2, by=(float)y2, bz=(float)z2;
        // Bottom
        vc.addVertex(m,ax,ay,az).setColor(r,g,b,a).setNormal(0,-1,0);
        vc.addVertex(m,bx,ay,az).setColor(r,g,b,a).setNormal(0,-1,0);
        vc.addVertex(m,bx,ay,bz).setColor(r,g,b,a).setNormal(0,-1,0);
        vc.addVertex(m,ax,ay,bz).setColor(r,g,b,a).setNormal(0,-1,0);
        vc.addVertex(m,ax,ay,bz).setColor(r,g,b,a).setNormal(0, 1,0);
        vc.addVertex(m,bx,ay,bz).setColor(r,g,b,a).setNormal(0, 1,0);
        vc.addVertex(m,bx,ay,az).setColor(r,g,b,a).setNormal(0, 1,0);
        vc.addVertex(m,ax,ay,az).setColor(r,g,b,a).setNormal(0, 1,0);
        // Top
        vc.addVertex(m,ax,by,az).setColor(r,g,b,a).setNormal(0, 1,0);
        vc.addVertex(m,ax,by,bz).setColor(r,g,b,a).setNormal(0, 1,0);
        vc.addVertex(m,bx,by,bz).setColor(r,g,b,a).setNormal(0, 1,0);
        vc.addVertex(m,bx,by,az).setColor(r,g,b,a).setNormal(0, 1,0);
        vc.addVertex(m,bx,by,az).setColor(r,g,b,a).setNormal(0,-1,0);
        vc.addVertex(m,bx,by,bz).setColor(r,g,b,a).setNormal(0,-1,0);
        vc.addVertex(m,ax,by,bz).setColor(r,g,b,a).setNormal(0,-1,0);
        vc.addVertex(m,ax,by,az).setColor(r,g,b,a).setNormal(0,-1,0);
        // North (−Z)
        vc.addVertex(m,ax,ay,az).setColor(r,g,b,a).setNormal(0,0,-1);
        vc.addVertex(m,ax,by,az).setColor(r,g,b,a).setNormal(0,0,-1);
        vc.addVertex(m,bx,by,az).setColor(r,g,b,a).setNormal(0,0,-1);
        vc.addVertex(m,bx,ay,az).setColor(r,g,b,a).setNormal(0,0,-1);
        vc.addVertex(m,bx,ay,az).setColor(r,g,b,a).setNormal(0,0, 1);
        vc.addVertex(m,bx,by,az).setColor(r,g,b,a).setNormal(0,0, 1);
        vc.addVertex(m,ax,by,az).setColor(r,g,b,a).setNormal(0,0, 1);
        vc.addVertex(m,ax,ay,az).setColor(r,g,b,a).setNormal(0,0, 1);
        // South (+Z)
        vc.addVertex(m,ax,ay,bz).setColor(r,g,b,a).setNormal(0,0, 1);
        vc.addVertex(m,bx,ay,bz).setColor(r,g,b,a).setNormal(0,0, 1);
        vc.addVertex(m,bx,by,bz).setColor(r,g,b,a).setNormal(0,0, 1);
        vc.addVertex(m,ax,by,bz).setColor(r,g,b,a).setNormal(0,0, 1);
        vc.addVertex(m,ax,by,bz).setColor(r,g,b,a).setNormal(0,0,-1);
        vc.addVertex(m,bx,by,bz).setColor(r,g,b,a).setNormal(0,0,-1);
        vc.addVertex(m,bx,ay,bz).setColor(r,g,b,a).setNormal(0,0,-1);
        vc.addVertex(m,ax,ay,bz).setColor(r,g,b,a).setNormal(0,0,-1);
        // West (−X)
        vc.addVertex(m,ax,ay,az).setColor(r,g,b,a).setNormal(-1,0,0);
        vc.addVertex(m,ax,ay,bz).setColor(r,g,b,a).setNormal(-1,0,0);
        vc.addVertex(m,ax,by,bz).setColor(r,g,b,a).setNormal(-1,0,0);
        vc.addVertex(m,ax,by,az).setColor(r,g,b,a).setNormal(-1,0,0);
        vc.addVertex(m,ax,by,az).setColor(r,g,b,a).setNormal( 1,0,0);
        vc.addVertex(m,ax,by,bz).setColor(r,g,b,a).setNormal( 1,0,0);
        vc.addVertex(m,ax,ay,bz).setColor(r,g,b,a).setNormal( 1,0,0);
        vc.addVertex(m,ax,ay,az).setColor(r,g,b,a).setNormal( 1,0,0);
        // East (+X)
        vc.addVertex(m,bx,ay,az).setColor(r,g,b,a).setNormal( 1,0,0);
        vc.addVertex(m,bx,by,az).setColor(r,g,b,a).setNormal( 1,0,0);
        vc.addVertex(m,bx,by,bz).setColor(r,g,b,a).setNormal( 1,0,0);
        vc.addVertex(m,bx,ay,bz).setColor(r,g,b,a).setNormal( 1,0,0);
        vc.addVertex(m,bx,ay,bz).setColor(r,g,b,a).setNormal(-1,0,0);
        vc.addVertex(m,bx,by,bz).setColor(r,g,b,a).setNormal(-1,0,0);
        vc.addVertex(m,bx,by,az).setColor(r,g,b,a).setNormal(-1,0,0);
        vc.addVertex(m,bx,ay,az).setColor(r,g,b,a).setNormal(-1,0,0);
    }

    private static void addOutline(VertexConsumer vc, Matrix4f m,
                                   double x1, double y1, double z1,
                                   double x2, double y2, double z2,
                                   int r, int g, int b, int a) {
        float ax=(float)x1, ay=(float)y1, az=(float)z1;
        float bx=(float)x2, by=(float)y2, bz=(float)z2;
        line(vc,m, ax,ay,az, bx,ay,az, r,g,b,a,  1, 0, 0);
        line(vc,m, bx,ay,az, bx,ay,bz, r,g,b,a,  0, 0, 1);
        line(vc,m, bx,ay,bz, ax,ay,bz, r,g,b,a, -1, 0, 0);
        line(vc,m, ax,ay,bz, ax,ay,az, r,g,b,a,  0, 0,-1);
        line(vc,m, ax,by,az, bx,by,az, r,g,b,a,  1, 0, 0);
        line(vc,m, bx,by,az, bx,by,bz, r,g,b,a,  0, 0, 1);
        line(vc,m, bx,by,bz, ax,by,bz, r,g,b,a, -1, 0, 0);
        line(vc,m, ax,by,bz, ax,by,az, r,g,b,a,  0, 0,-1);
        line(vc,m, ax,ay,az, ax,by,az, r,g,b,a,  0, 1, 0);
        line(vc,m, bx,ay,az, bx,by,az, r,g,b,a,  0, 1, 0);
        line(vc,m, bx,ay,bz, bx,by,bz, r,g,b,a,  0, 1, 0);
        line(vc,m, ax,ay,bz, ax,by,bz, r,g,b,a,  0, 1, 0);
    }

    private static void line(VertexConsumer vc, Matrix4f m,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             int r, int g, int b, int a,
                             float nx, float ny, float nz) {
        vc.addVertex(m,x1,y1,z1).setColor(r,g,b,a).setNormal(nx,ny,nz).setLineWidth(3.0f);
        vc.addVertex(m,x2,y2,z2).setColor(r,g,b,a).setNormal(nx,ny,nz).setLineWidth(3.0f);
    }
}
