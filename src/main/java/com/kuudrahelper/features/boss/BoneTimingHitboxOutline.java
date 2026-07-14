package com.kuudrahelper.features.boss;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class BoneTimingHitboxOutline {

    private static final int OUTLINE_A   = 26; // ~10% opacity
    private static final int FILL_A      = 26; // ~10% opacity
    private static final int KUUDRA_SIZE = 30;

    private BoneTimingHitboxOutline() {}

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!KuudraConfig.isBoneTimingHitboxOutlineEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.BOSS) return;

        AABB[] boxes = BoneTimingAssist.KUUDRA_LOGGED_HITBOXES;
        if (KuudraConfig.isBoneTimingHitboxOutlineOnlyCurrentDirection()) {
            AABB current = closestToLiveKuudra(mc);
            if (current == null) return; // no live Kuudra found — nothing to narrow down to
            boxes = new AABB[]{current};
        }

        int col = KuudraConfig.getBoneTimingHitboxOutlineColor();
        int r = (col >> 16) & 0xFF, g = (col >> 8) & 0xFF, b = col & 0xFF;
        boolean filled = KuudraConfig.isBoneTimingHitboxOutlineFilled();

        Vec3     cam = camera.position();
        double   cx  = cam.x, cy = cam.y, cz = cam.z;
        Matrix4f m   = matrices.last().pose();

        MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();

        for (AABB box : boxes) {
            double x1 = box.minX - cx, y1 = box.minY - cy, z1 = box.minZ - cz;
            double x2 = box.maxX - cx, y2 = box.maxY - cy, z2 = box.maxZ - cz;

            if (filled) {
                VertexConsumer vf = imm.getBuffer(RenderTypes.debugQuads());
                addFill(vf, m, x1, y1, z1, x2, y2, z2, r, g, b);
                imm.endBatch();
            }

            GL11.glDepthFunc(GL11.GL_ALWAYS);
            VertexConsumer vl = imm.getBuffer(RenderTypes.lines());
            addOutline(vl, m, x1, y1, z1, x2, y2, z2, r, g, b);
            imm.endBatch();
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        }
    }

    private static AABB closestToLiveKuudra(Minecraft mc) {
        Vec3 pos = null;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof Slime s)) continue;
            if (s.getSize() != KUUDRA_SIZE || s.getHealth() <= 0) continue;
            pos = s.position();
            break;
        }
        if (pos == null) return null;

        AABB   best     = null;
        double bestDist = Double.MAX_VALUE;
        for (AABB box : BoneTimingAssist.KUUDRA_LOGGED_HITBOXES) {
            Vec3 center = new Vec3((box.minX + box.maxX) / 2.0, (box.minY + box.maxY) / 2.0, (box.minZ + box.maxZ) / 2.0);
            double d = center.distanceToSqr(pos);
            if (d < bestDist) { bestDist = d; best = box; }
        }
        return best;
    }

    private static void addOutline(VertexConsumer vc, Matrix4f m,
                                   double x1, double y1, double z1,
                                   double x2, double y2, double z2,
                                   int r, int g, int b) {
        float ax = (float) x1, ay = (float) y1, az = (float) z1;
        float bx = (float) x2, by = (float) y2, bz = (float) z2;
        line(vc, m, ax, ay, az, bx, ay, az,  1, 0, 0, r, g, b);
        line(vc, m, bx, ay, az, bx, ay, bz,  0, 0, 1, r, g, b);
        line(vc, m, bx, ay, bz, ax, ay, bz, -1, 0, 0, r, g, b);
        line(vc, m, ax, ay, bz, ax, ay, az,  0, 0,-1, r, g, b);
        line(vc, m, ax, by, az, bx, by, az,  1, 0, 0, r, g, b);
        line(vc, m, bx, by, az, bx, by, bz,  0, 0, 1, r, g, b);
        line(vc, m, bx, by, bz, ax, by, bz, -1, 0, 0, r, g, b);
        line(vc, m, ax, by, bz, ax, by, az,  0, 0,-1, r, g, b);
        line(vc, m, ax, ay, az, ax, by, az,  0, 1, 0, r, g, b);
        line(vc, m, bx, ay, az, bx, by, az,  0, 1, 0, r, g, b);
        line(vc, m, bx, ay, bz, bx, by, bz,  0, 1, 0, r, g, b);
        line(vc, m, ax, ay, bz, ax, by, bz,  0, 1, 0, r, g, b);
    }

    private static void line(VertexConsumer vc, Matrix4f m,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float nx, float ny, float nz,
                             int r, int g, int b) {
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, OUTLINE_A).setNormal(nx, ny, nz).setLineWidth(3.0f);
        vc.addVertex(m, x2, y2, z2).setColor(r, g, b, OUTLINE_A).setNormal(nx, ny, nz).setLineWidth(3.0f);
    }

    private static void addFill(VertexConsumer vc, Matrix4f m,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                int r, int g, int b) {
        float ax = (float) x1, ay = (float) y1, az = (float) z1;
        float bx = (float) x2, by = (float) y2, bz = (float) z2;
        // Bottom
        quad(vc, m, ax,ay,az, bx,ay,az, bx,ay,bz, ax,ay,bz,  0,-1, 0, r, g, b);
        quad(vc, m, ax,ay,bz, bx,ay,bz, bx,ay,az, ax,ay,az,  0, 1, 0, r, g, b);
        // Top
        quad(vc, m, ax,by,az, ax,by,bz, bx,by,bz, bx,by,az,  0, 1, 0, r, g, b);
        quad(vc, m, bx,by,az, bx,by,bz, ax,by,bz, ax,by,az,  0,-1, 0, r, g, b);
        // North (−Z)
        quad(vc, m, ax,ay,az, ax,by,az, bx,by,az, bx,ay,az,  0, 0,-1, r, g, b);
        quad(vc, m, bx,ay,az, bx,by,az, ax,by,az, ax,ay,az,  0, 0, 1, r, g, b);
        // South (+Z)
        quad(vc, m, ax,ay,bz, bx,ay,bz, bx,by,bz, ax,by,bz,  0, 0, 1, r, g, b);
        quad(vc, m, ax,by,bz, bx,by,bz, bx,ay,bz, ax,ay,bz,  0, 0,-1, r, g, b);
        // West (−X)
        quad(vc, m, ax,ay,az, ax,ay,bz, ax,by,bz, ax,by,az, -1, 0, 0, r, g, b);
        quad(vc, m, ax,by,az, ax,by,bz, ax,ay,bz, ax,ay,az,  1, 0, 0, r, g, b);
        // East (+X)
        quad(vc, m, bx,ay,az, bx,by,az, bx,by,bz, bx,ay,bz,  1, 0, 0, r, g, b);
        quad(vc, m, bx,ay,bz, bx,by,bz, bx,by,az, bx,ay,az, -1, 0, 0, r, g, b);
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float nx, float ny, float nz,
                             int r, int g, int b) {
        vc.addVertex(m, x0, y0, z0).setColor(r, g, b, FILL_A).setNormal(nx, ny, nz);
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, FILL_A).setNormal(nx, ny, nz);
        vc.addVertex(m, x2, y2, z2).setColor(r, g, b, FILL_A).setNormal(nx, ny, nz);
        vc.addVertex(m, x3, y3, z3).setColor(r, g, b, FILL_A).setNormal(nx, ny, nz);
    }
}
