package com.phantomaddons.features.boss;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.phase.KuudraPhaseTracker;
import com.phantomaddons.utils.KuudraTierDetector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
//? if <26.2 {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.lwjgl.opengl.GL11;
*///?} else {
import com.phantomaddons.utils.AlwaysOnTopRenderTypes;
import com.phantomaddons.utils.ImmediateDraw;
//?}
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class RightPeakBlockHighlight {

    private static final AABB BLOCK = new AABB(-129, 12, -104, -128, 13, -103).inflate(0.002);
    private static final int R = 255, G = 128, B = 0;
    private static final int OUTLINE_A = 255;
    private static final int FILL_A    = 70;

    private RightPeakBlockHighlight() {}

    private static long drawCalls;

    private static boolean active() {
        if (!PhantomConfig.isRightPeakBlockHighlightEnabled()) return false;
        Minecraft mc = Minecraft.getInstance();
        boolean on = mc.level != null && mc.player != null && (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.NONE || KuudraTierDetector.getTier() > 0);
        if (on) drawCalls++;
        return on;
    }

    public static String debugStatus() {
        Minecraft mc = Minecraft.getInstance();
        double dist = mc.player == null ? -1 : mc.player.position().distanceTo(BLOCK.getCenter());
        return "toggle=" + PhantomConfig.isRightPeakBlockHighlightEnabled()
                + " phase=" + KuudraPhaseTracker.getPhase()
                + " tier=" + KuudraTierDetector.getTier()
                + " drawCalls=" + drawCalls
                + " distance=" + (int) dist;
    }

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        //? if <26.2 {
        /*if (!active()) return;

        Vec3     cam = camera.position();
        Matrix4f m   = matrices.last().pose();
        double x1 = BLOCK.minX - cam.x, y1 = BLOCK.minY - cam.y, z1 = BLOCK.minZ - cam.z;
        double x2 = BLOCK.maxX - cam.x, y2 = BLOCK.maxY - cam.y, z2 = BLOCK.maxZ - cam.z;

        MultiBufferSource.BufferSource imm = Minecraft.getInstance().renderBuffers().bufferSource();

        GL11.glDepthFunc(GL11.GL_ALWAYS);
        VertexConsumer vf = imm.getBuffer(RenderTypes.debugQuads());
        addFill(vf, m, x1, y1, z1, x2, y2, z2);
        imm.endBatch();

        VertexConsumer vl = imm.getBuffer(RenderTypes.lines());
        addOutline(vl, m, x1, y1, z1, x2, y2, z2);
        imm.endBatch();
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        *///?}
    }

    //? if <26.2 {
    /*public static void renderAlwaysOnTop(PoseStack matrices, Camera camera, float tickDelta) {}
    *///?} else {
    public static void renderAlwaysOnTop(PoseStack matrices, Camera camera, float tickDelta) {
        if (!active()) return;

        Vec3     cam = camera.position();
        Matrix4f m   = matrices.last().pose();
        double x1 = BLOCK.minX - cam.x, y1 = BLOCK.minY - cam.y, z1 = BLOCK.minZ - cam.z;
        double x2 = BLOCK.maxX - cam.x, y2 = BLOCK.maxY - cam.y, z2 = BLOCK.maxZ - cam.z;

        addFill(ImmediateDraw.begin(AlwaysOnTopRenderTypes.debugQuads()), m, x1, y1, z1, x2, y2, z2);
        addOutline(ImmediateDraw.begin(AlwaysOnTopRenderTypes.lines()), m, x1, y1, z1, x2, y2, z2);
    }
    //?}

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
