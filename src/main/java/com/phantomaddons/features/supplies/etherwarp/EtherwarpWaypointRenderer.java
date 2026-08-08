package com.phantomaddons.features.supplies.etherwarp;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.phase.KuudraPhaseTracker;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public final class EtherwarpWaypointRenderer {

    private static final int[][] GROUP_BRIGHT = {
        {  0, 255, 255 }, // Shop     — cyan
        {255, 200,   0 }, // X Cannon — orange/gold
        {200,   0, 255 }, // Square   — purple
    };
    private static final int[][] GROUP_DARK = {
        {  0,  80,  80 }, // Shop     — dark cyan
        {100,  70,   0 }, // X Cannon — dark orange
        { 80,   0, 100 }, // Square   — dark purple
    };

    private static final int[] COLOR_PRIORITY_BRIGHT = { 255,  50,  50 };
    private static final int[] COLOR_PRIORITY_DARK   = { 120,   0,   0 };
    private static final int[] COLOR_HOVER           = {   0, 255,   0 };

    private static final int   OUTLINE_A = 255;
    private static final int   FILL_A    = 80;
    private static final float Y_OFFSET  = 0.002f;

    private EtherwarpWaypointRenderer() {}

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!PhantomConfig.isEtherwarpWaypointsEnabled()) return;
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.SUPPLIES) return;
        if (!EtherwarpWaypointManager.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Vec3 camPos    = camera.position();
        Vec3 playerPos = mc.player.position();
        Matrix4f m     = matrices.last().pose();

        //? if <26.2 {
        /*MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();

        EtherwarpWaypointManager.updatePriority();
        String priorityDest = EtherwarpWaypointManager.getStickyPriorityZone();

        List<EtherwarpWaypointManager.EtherwarpGroup> groups = EtherwarpWaypointManager.GROUPS;

        Vec3 pearlDest = EtherwarpWaypointManager.getEffectivePearlDest(playerPos);

        GL11.glDepthFunc(GL11.GL_ALWAYS);

        for (int gi = 0; gi < groups.size(); gi++) {
            EtherwarpWaypointManager.EtherwarpGroup group = groups.get(gi);
            boolean isPriority = priorityDest != null && priorityDest.equals(group.zone());

            for (Vec3 target : group.targets()) {
                boolean hovered = hoverCheck(mc, target);
                int[] col = staticColor(gi, hovered, isPriority);
                drawTopFace(imm, m, target, camPos, col[0], col[1], col[2]);
            }
            imm.endBatch();

            if (pearlDest == null) continue;

            for (Vec3 target : group.targets()) {
                Vec3 relPos = playerPos.add(target.subtract(pearlDest));
                boolean hovered = hoverCheck(mc, target) || hoverCheck(mc, relPos);
                int[] col = relColor(gi, hovered, isPriority);
                drawTopFace(imm, m, relPos, camPos, col[0], col[1], col[2]);
            }
            imm.endBatch();
        }

        GL11.glDepthFunc(GL11.GL_LEQUAL);
        *///?} else {
        SubmitNodeCollector collector = WorldRenderCollector.get();
        if (collector == null) return;

        EtherwarpWaypointManager.updatePriority();
        String priorityDest = EtherwarpWaypointManager.getStickyPriorityZone();

        List<EtherwarpWaypointManager.EtherwarpGroup> groups = EtherwarpWaypointManager.GROUPS;

        Vec3 pearlDest = EtherwarpWaypointManager.getEffectivePearlDest(playerPos);

        for (int gi = 0; gi < groups.size(); gi++) {
            EtherwarpWaypointManager.EtherwarpGroup group = groups.get(gi);
            boolean isPriority = priorityDest != null && priorityDest.equals(group.zone());

            for (Vec3 target : group.targets()) {
                boolean hovered = hoverCheck(mc, target);
                int[] col = staticColor(gi, hovered, isPriority);
                matrices.pushPose();
                matrices.translate(target.x - camPos.x, target.y - camPos.y, target.z - camPos.z);
                drawTopFace(matrices, collector, m, target, camPos, col[0], col[1], col[2]);
                matrices.popPose();
            }

            if (pearlDest == null) continue;

            for (Vec3 target : group.targets()) {
                Vec3 relPos = playerPos.add(target.subtract(pearlDest));
                boolean hovered = hoverCheck(mc, target) || hoverCheck(mc, relPos);
                int[] col = relColor(gi, hovered, isPriority);
                matrices.pushPose();
                matrices.translate(relPos.x - camPos.x, relPos.y - camPos.y, relPos.z - camPos.z);
                drawTopFace(matrices, collector, m, relPos, camPos, col[0], col[1], col[2]);
                matrices.popPose();
            }
        }
        //?}
    }

    private static int[] staticColor(int gi, boolean hovered, boolean priority) {
        if (hovered)  return COLOR_HOVER;
        if (priority) return COLOR_PRIORITY_DARK;
        return GROUP_DARK[gi];
    }

    private static int[] relColor(int gi, boolean hovered, boolean priority) {
        if (hovered)  return COLOR_HOVER;
        if (priority) return COLOR_PRIORITY_BRIGHT;
        return GROUP_BRIGHT[gi];
    }

    //? if <26.2 {
    /*private static void drawTopFace(MultiBufferSource.BufferSource imm, Matrix4f m,
                                     Vec3 center, Vec3 camPos, int r, int g, int b) {
        float x0 = (float)(center.x - 0.5 - camPos.x);
        float x1 = (float)(center.x + 0.5 - camPos.x);
        float y  = (float)(center.y         - camPos.y) + Y_OFFSET;
        float z0 = (float)(center.z - 0.5 - camPos.z);
        float z1 = (float)(center.z + 0.5 - camPos.z);

        VertexConsumer lines = imm.getBuffer(RenderTypes.lines());
        edge(lines, m, x0, y, z0, x1, y, z0, r, g, b);
        edge(lines, m, x1, y, z0, x1, y, z1, r, g, b);
        edge(lines, m, x1, y, z1, x0, y, z1, r, g, b);
        edge(lines, m, x0, y, z1, x0, y, z0, r, g, b);
        imm.endBatch(RenderTypes.lines());

        VertexConsumer quads = imm.getBuffer(RenderTypes.debugQuads());
        quad(quads, m, x0, y, z0, x1, y, z0, x1, y, z1, x0, y, z1, r, g, b, FILL_A,  0, 1, 0);
        quad(quads, m, x0, y, z1, x1, y, z1, x1, y, z0, x0, y, z0, r, g, b, FILL_A,  0,-1, 0);
        imm.endBatch(RenderTypes.debugQuads());
    }
    *///?} else {
    private static void drawTopFace(PoseStack matrices, SubmitNodeCollector collector, Matrix4f m,
                                     Vec3 center, Vec3 camPos, int r, int g, int b) {
        float x0 = (float)(center.x - 0.5 - camPos.x);
        float x1 = (float)(center.x + 0.5 - camPos.x);
        float y  = (float)(center.y         - camPos.y) + Y_OFFSET;
        float z0 = (float)(center.z - 0.5 - camPos.z);
        float z1 = (float)(center.z + 0.5 - camPos.z);

        collector.submitCustomGeometry(matrices, AlwaysOnTopRenderTypes.lines(), (pose, lines) -> {
            edge(lines, m, x0, y, z0, x1, y, z0, r, g, b);
            edge(lines, m, x1, y, z0, x1, y, z1, r, g, b);
            edge(lines, m, x1, y, z1, x0, y, z1, r, g, b);
            edge(lines, m, x0, y, z1, x0, y, z0, r, g, b);
        });

        collector.submitCustomGeometry(matrices, AlwaysOnTopRenderTypes.debugQuads(), (pose, quads) -> {
            quad(quads, m, x0, y, z0, x1, y, z0, x1, y, z1, x0, y, z1, r, g, b, FILL_A,  0, 1, 0);
            quad(quads, m, x0, y, z1, x1, y, z1, x1, y, z0, x0, y, z0, r, g, b, FILL_A,  0,-1, 0);
        });
    }
    //?}

    private static boolean hoverCheck(Minecraft mc, Vec3 center) {
        Entity cam = mc.getCameraEntity();
        if (cam == null) return false;
        Vec3 eye  = cam.getEyePosition();
        Vec3 look = cam.getLookAngle();
        if (Math.abs(look.y) < 0.00001) return false;
        double t = (center.y - eye.y) / look.y;
        if (t < 0.5 || t > 200.0) return false;
        Vec3 hit = eye.add(look.scale(t));
        return Math.abs(hit.x - center.x) < 0.5 && Math.abs(hit.z - center.z) < 0.5;
    }

    private static void edge(VertexConsumer vc, Matrix4f m,
                              float x0, float y0, float z0,
                              float x1, float y1, float z1,
                              int r, int g, int b) {
        float dx = x1-x0, dy = y1-y0, dz = z1-z0;
        float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len == 0) return;
        vc.addVertex(m, x0, y0, z0).setColor(r, g, b, OUTLINE_A).setNormal(dx/len, dy/len, dz/len).setLineWidth(2.0f);
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, OUTLINE_A).setNormal(dx/len, dy/len, dz/len).setLineWidth(2.0f);
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
