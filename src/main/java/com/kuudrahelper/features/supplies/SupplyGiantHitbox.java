package com.kuudrahelper.features.supplies;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.pearls.PearlWaypointManager;
import com.kuudrahelper.features.pearls.PearlWaypointState;
import com.kuudrahelper.features.pearls.TrajectorySolver;
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

    private static final int    PEARL_PATH_TICKS = 120;

    private static final int R = 255, G = 80, B = 0;
    private static final int OUTLINE_A = 220;
    private static final int FILL_A    = 30;

    private static final double MAX_GROUND_Y = 67.0;

    private static Giant   warningGiant = null;
    private static int     lastGiantId  = -1;
    private static double  lastGiantY   = Double.NaN;
    private static double  smoothedVy   = 0.0;
    private static double  topBound     = Double.NaN;
    private static double  bottomBound  = Double.NaN;

    private static volatile boolean showWarning = false;

    private SupplyGiantHitbox() {}

    public static boolean isWarningActive() { return showWarning; }

    public static void tick(Minecraft client) {
        if (!KuudraConfig.isSupplyGiantHitboxEnabled()
                || client.level == null || client.player == null
                || KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.SUPPLIES
                || !PearlWaypointManager.isTrackingPickup()) {
            warningGiant = null;
            showWarning  = false;
            resetTracking();
            return;
        }

        Giant nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (Entity e : client.level.entitiesForRendering()) {
            if (!(e instanceof Giant g)) continue;
            if (g.getY() >= MAX_GROUND_Y) continue;
            if (!isCarryingSupply(g)) continue;
            double distSq = g.distanceToSqr(client.player);
            if (distSq < nearestDistSq) { nearestDistSq = distSq; nearest = g; }
        }

        if (nearest == null) {
            warningGiant = null;
            showWarning  = false;
            resetTracking();
            return;
        }

        if (nearest.getId() != lastGiantId) resetTracking();
        lastGiantId = nearest.getId();

        double y = nearest.getY();
        if (!Double.isNaN(lastGiantY)) {
            double delta = y - lastGiantY;
            if (smoothedVy > 0 && delta < 0)      topBound    = lastGiantY;
            else if (smoothedVy < 0 && delta > 0) bottomBound = lastGiantY;
            if (delta != 0) smoothedVy = smoothedVy * 0.7 + delta * 0.3;
        }
        lastGiantY = y;

        Vec3 myAimDir = null;
        long throwInMs = 0L;
        for (PearlWaypointState s : PearlWaypointManager.getSnapshot()) {
            if (s.isMyTarget()) { myAimDir = s.centerAimDir(); throwInMs = s.throwInMs(); break; }
        }

        if (myAimDir == null) {
            warningGiant = null;
            showWarning  = false;
            return;
        }

        Vec3 spawn = PearlWaypointManager.pearlSpawnPos(client);
        int  delayTicks = throwInMs > 0L ? (int) Math.round(throwInMs / 50.0) : 0;

        double baseY = y, v = smoothedVy, top = topBound, bottom = bottomBound;
        boolean intersects = TrajectorySolver.pathIntersectsPredictedBox(
                spawn, myAimDir, nearest.getBoundingBox(), baseY,
                globalTick -> TrajectorySolver.predictBouncedY(baseY, v, top, bottom, globalTick),
                delayTicks, PEARL_PATH_TICKS);

        warningGiant = intersects ? nearest : null;
        showWarning  = intersects;
    }

    private static void resetTracking() {
        lastGiantId = -1;
        lastGiantY  = Double.NaN;
        smoothedVy  = 0.0;
        topBound    = Double.NaN;
        bottomBound = Double.NaN;
    }

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!KuudraConfig.isSupplyGiantHitboxEnabled()) return;
        Giant g = warningGiant;
        if (g == null || g.isRemoved()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 cam = camera.position();
        Matrix4f m = matrices.last().pose();
        AABB bb = g.getBoundingBox().inflate(0.5);
        double x1 = bb.minX - cam.x, y1 = bb.minY - cam.y, z1 = bb.minZ - cam.z;
        double x2 = bb.maxX - cam.x, y2 = bb.maxY - cam.y, z2 = bb.maxZ - cam.z;

        MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();

        VertexConsumer vf = imm.getBuffer(RenderTypes.debugQuads());
        addFill(vf, m, x1, y1, z1, x2, y2, z2);
        imm.endBatch();

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
