package com.kuudrahelper.features;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import com.kuudrahelper.phase.KuudraPhaseTracker.Phase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class StunPreviewRenderer {

    private static final double REF_X = -161, REF_Y = 49, REF_Z = -186;
    private static final int    TX = -168, TY = 27, TZ = -167;
    private static final int    TR = 30, TG = 195, TB = 255;
    private static final int    T_FILL    = 255;
    private static final int    T_OUTLINE = 255;

    private StunPreviewRenderer() {}

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!KuudraConfig.isStunPreviewEnabled()) return;
        if (KuudraPhaseTracker.getPhase() != Phase.EATEN) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Vec3   playerPos = mc.player.getPosition(tickDelta);
        double px = playerPos.x, py = playerPos.y, pz = playerPos.z;
        double dx = px - REF_X,  dy = py - REF_Y,  dz = pz - REF_Z;

        Vec3     cam = camera.position();
        double   cx  = cam.x, cy = cam.y, cz = cam.z;
        Matrix4f m   = matrices.last().pose();

        MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();

        double wx = TX + dx - cx;
        double wy = TY + dy - cy;
        double wz = TZ + dz - cz;

        for (int pass = 0; pass < 2; pass++) {
            if (pass == 1) GL11.glDepthFunc(519);

            VertexConsumer vf = imm.getBuffer(RenderTypes.debugQuads());
            VertexConsumer vl = imm.getBuffer(RenderTypes.lines());
            addFill   (vf, m, wx, wy, wz, wx+1, wy+1, wz+1, TR, TG, TB, T_FILL);
            addOutline(vl, m, wx, wy, wz, wx+1, wy+1, wz+1, TR, TG, TB, T_OUTLINE);
            imm.endBatch();

            if (pass == 1) GL11.glDepthFunc(515);
        }
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