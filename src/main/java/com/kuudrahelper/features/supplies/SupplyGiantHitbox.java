package com.kuudrahelper.features.supplies;

import com.kuudrahelper.KuudraConfig;
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

public final class SupplyGiantHitbox {

    // Max distance squared to trigger alert (~5.5 blocks, matching IQ)
    private static final double ALERT_DIST_SQ = 30.25;

    // Alert color: red/orange
    private static final int R = 255, G = 80, B = 0;
    private static final int OUTLINE_A = 220;
    private static final int FILL_A    = 30;

    // Giant eye-Y threshold: supply carriers walk at ground level in the arena
    private static final double MAX_EYE_Y = 67.0;

    private static Giant alertGiant = null;

    private SupplyGiantHitbox() {}

    public static void tick(Minecraft client) {
        if (!KuudraConfig.isSupplyGiantHitboxEnabled()) { alertGiant = null; return; }
        if (client.level == null || client.player == null) { alertGiant = null; return; }
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.SUPPLIES) { alertGiant = null; return; }

        AABB playerBox = client.player.getBoundingBox();
        Giant closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Entity e : client.level.entitiesForRendering()) {
            if (!(e instanceof Giant g)) continue;
            if (g.getEyeY() >= MAX_EYE_Y) continue;
            if (!isCarryingSupply(g)) continue;

            double distSq = g.distanceToSqr(client.player);
            if (distSq > ALERT_DIST_SQ) continue;
            if (!g.getBoundingBox().inflate(0.5).intersects(playerBox)) continue;

            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = g;
            }
        }
        alertGiant = closest;
    }

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!KuudraConfig.isSupplyGiantHitboxEnabled()) return;
        Giant g = alertGiant;
        if (g == null || g.isRemoved()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 cam = camera.position();
        Matrix4f m = matrices.last().pose();
        AABB bb = g.getBoundingBox().inflate(0.5);
        double x1 = bb.minX - cam.x, y1 = bb.minY - cam.y, z1 = bb.minZ - cam.z;
        double x2 = bb.maxX - cam.x, y2 = bb.maxY - cam.y, z2 = bb.maxZ - cam.z;

        MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();

        // Filled faces (semi-transparent)
        VertexConsumer vf = imm.getBuffer(RenderTypes.debugQuads());
        addFill(vf, m, x1, y1, z1, x2, y2, z2);
        imm.endBatch();

        // Outline (always visible through walls)
        GL11.glDepthFunc(GL11.GL_ALWAYS);
        VertexConsumer vl = imm.getBuffer(RenderTypes.lines());
        addOutline(vl, m, x1, y1, z1, x2, y2, z2);
        imm.endBatch();
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }

    private static boolean isCarryingSupply(Giant g) {
        ItemStack hand = g.getMainHandItem();
        if (hand.isEmpty()) return false;
        return hand.is(Items.PLAYER_HEAD) || hand.is(Items.CARVED_PUMPKIN) || hand.is(Items.PUMPKIN);
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
        vc.addVertex(m, x1, y1, z1).setColor(R, G, B, OUTLINE_A).setNormal(nx, ny, nz).setLineWidth(2.5f);
        vc.addVertex(m, x2, y2, z2).setColor(R, G, B, OUTLINE_A).setNormal(nx, ny, nz).setLineWidth(2.5f);
    }

    private static void addFill(VertexConsumer vc, Matrix4f m,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2) {
        float ax = (float) x1, ay = (float) y1, az = (float) z1;
        float bx = (float) x2, by = (float) y2, bz = (float) z2;
        quad(vc, m, ax,ay,az, bx,ay,az, bx,ay,bz, ax,ay,bz,  0,-1, 0);
        quad(vc, m, ax,ay,bz, bx,ay,bz, bx,ay,az, ax,ay,az,  0, 1, 0);
        quad(vc, m, ax,by,az, ax,by,bz, bx,by,bz, bx,by,az,  0, 1, 0);
        quad(vc, m, bx,by,az, bx,by,bz, ax,by,bz, ax,by,az,  0,-1, 0);
        quad(vc, m, ax,ay,az, ax,by,az, bx,by,az, bx,ay,az,  0, 0,-1);
        quad(vc, m, bx,ay,az, bx,by,az, ax,by,az, ax,ay,az,  0, 0, 1);
        quad(vc, m, ax,ay,bz, bx,ay,bz, bx,by,bz, ax,by,bz,  0, 0, 1);
        quad(vc, m, ax,by,bz, bx,by,bz, bx,ay,bz, ax,ay,bz,  0, 0,-1);
        quad(vc, m, ax,ay,az, ax,ay,bz, ax,by,bz, ax,by,az, -1, 0, 0);
        quad(vc, m, ax,by,az, ax,by,bz, ax,ay,bz, ax,ay,az,  1, 0, 0);
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
