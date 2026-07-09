package com.kuudrahelper.features.supplies;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.pearls.PearlWaypointManager;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class GiantHitboxOutline {

    private static final double EDGE_DIST = 2.0;
    private static final int    OUTLINE_A = 220;

    private static Set<Giant> outlined = Collections.emptySet();

    private GiantHitboxOutline() {}

    public static void tick(Minecraft client) {
        if (!KuudraConfig.isGiantHitboxEnabled()) { outlined = Collections.emptySet(); return; }
        if (client.level == null || client.player == null) { outlined = Collections.emptySet(); return; }
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.SUPPLIES) { outlined = Collections.emptySet(); return; }
        if (!PearlWaypointManager.isTrackingPickup()) { outlined = Collections.emptySet(); return; }

        Vec3 playerPos = client.player.position();
        Set<Giant> next = new HashSet<>();
        for (Entity e : client.level.entitiesForRendering()) {
            if (!(e instanceof Giant g)) continue;
            if (!isCarryingSupply(g)) continue;
            if (distSqToBox(playerPos, g.getBoundingBox()) <= EDGE_DIST * EDGE_DIST) next.add(g);
        }
        outlined = next;
    }

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!KuudraConfig.isGiantHitboxEnabled()) return;
        Set<Giant> giants = outlined;
        if (giants.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int col   = KuudraConfig.getGiantHitboxColor();
        int rc    = (col >> 16) & 0xFF, gc = (col >> 8) & 0xFF, bc = col & 0xFF;
        int fillA = Math.max(1, (int) (255f * KuudraConfig.getGiantHitboxFillOpacity()));
        boolean filled = KuudraConfig.isGiantHitboxFilled();

        Vec3 cam = camera.position();
        Matrix4f m = matrices.last().pose();
        MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();

        if (filled) {
            VertexConsumer vf = imm.getBuffer(RenderTypes.debugQuads());
            for (Giant g : giants) {
                if (g.isRemoved()) continue;
                AABB bb = g.getBoundingBox();
                addFill(vf, m,
                        bb.minX - cam.x, bb.minY - cam.y, bb.minZ - cam.z,
                        bb.maxX - cam.x, bb.maxY - cam.y, bb.maxZ - cam.z,
                        rc, gc, bc, fillA);
            }
            imm.endBatch();
        }

        GL11.glDepthFunc(GL11.GL_ALWAYS);
        VertexConsumer vl = imm.getBuffer(RenderTypes.lines());
        for (Giant g : giants) {
            if (g.isRemoved()) continue;
            AABB bb = g.getBoundingBox();
            addOutline(vl, m,
                    bb.minX - cam.x, bb.minY - cam.y, bb.minZ - cam.z,
                    bb.maxX - cam.x, bb.maxY - cam.y, bb.maxZ - cam.z,
                    rc, gc, bc);
        }
        imm.endBatch();
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }

    private static boolean isCarryingSupply(Giant g) {
        ItemStack hand = g.getMainHandItem();
        if (hand.isEmpty()) return false;
        return hand.is(Items.PLAYER_HEAD) || hand.is(Items.CARVED_PUMPKIN) || hand.is(Items.PUMPKIN);
    }

    private static double distSqToBox(Vec3 p, AABB box) {
        double dx = Math.max(box.minX - p.x, Math.max(0, p.x - box.maxX));
        double dy = Math.max(box.minY - p.y, Math.max(0, p.y - box.maxY));
        double dz = Math.max(box.minZ - p.z, Math.max(0, p.z - box.maxZ));
        return dx * dx + dy * dy + dz * dz;
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
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, OUTLINE_A).setNormal(nx, ny, nz).setLineWidth(2.5f);
        vc.addVertex(m, x2, y2, z2).setColor(r, g, b, OUTLINE_A).setNormal(nx, ny, nz).setLineWidth(2.5f);
    }

    private static void addFill(VertexConsumer vc, Matrix4f m,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                int r, int g, int b, int a) {
        float ax = (float) x1, ay = (float) y1, az = (float) z1;
        float bx = (float) x2, by = (float) y2, bz = (float) z2;
        quad(vc, m, ax,ay,az, bx,ay,az, bx,ay,bz, ax,ay,bz,  0,-1, 0, r,g,b,a);
        quad(vc, m, ax,ay,bz, bx,ay,bz, bx,ay,az, ax,ay,az,  0, 1, 0, r,g,b,a);
        quad(vc, m, ax,by,az, ax,by,bz, bx,by,bz, bx,by,az,  0, 1, 0, r,g,b,a);
        quad(vc, m, bx,by,az, bx,by,bz, ax,by,bz, ax,by,az,  0,-1, 0, r,g,b,a);
        quad(vc, m, ax,ay,az, ax,by,az, bx,by,az, bx,ay,az,  0, 0,-1, r,g,b,a);
        quad(vc, m, bx,ay,az, bx,by,az, ax,by,az, ax,ay,az,  0, 0, 1, r,g,b,a);
        quad(vc, m, ax,ay,bz, bx,ay,bz, bx,by,bz, ax,by,bz,  0, 0, 1, r,g,b,a);
        quad(vc, m, ax,by,bz, bx,by,bz, bx,ay,bz, ax,ay,bz,  0, 0,-1, r,g,b,a);
        quad(vc, m, ax,ay,az, ax,ay,bz, ax,by,bz, ax,by,az, -1, 0, 0, r,g,b,a);
        quad(vc, m, ax,by,az, ax,by,bz, ax,ay,bz, ax,ay,az,  1, 0, 0, r,g,b,a);
        quad(vc, m, bx,ay,az, bx,by,az, bx,by,bz, bx,ay,bz,  1, 0, 0, r,g,b,a);
        quad(vc, m, bx,ay,bz, bx,by,bz, bx,by,az, bx,ay,az, -1, 0, 0, r,g,b,a);
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float nx, float ny, float nz,
                             int r, int g, int b, int a) {
        vc.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setNormal(nx, ny, nz);
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz);
        vc.addVertex(m, x2, y2, z2).setColor(r, g, b, a).setNormal(nx, ny, nz);
        vc.addVertex(m, x3, y3, z3).setColor(r, g, b, a).setNormal(nx, ny, nz);
    }
}
