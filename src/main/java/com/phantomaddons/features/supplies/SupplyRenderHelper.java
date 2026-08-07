package com.phantomaddons.features.supplies;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.miscskyblock.PredictedBobber;
import com.phantomaddons.phase.KuudraPhaseTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.phantomaddons.utils.WorldRenderCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class SupplyRenderHelper {

    private static final int LB_R = 0, LB_G = 191, LB_B = 255;
    private static final int RD_R = 60, RD_G = 220, RD_B = 90; // rod radius circle turns this green
    private static final int OUTLINE_A = 200;
    private static final int FILL_A    = 13;  // ~5% opacity

    private static final int PH_R = 255, PH_G = 255, PH_B = 255;
    private static final int PH_A = 160;

    private static final float  ROD_RADIUS   = 5.0f;
    private static final int    CIRCLE_SEGS  = 48;
    private static final double LAVA_SURFACE = 75.0; // supply crates always sit at this Y

    private SupplyRenderHelper() {}

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        boolean showHitbox     = PhantomConfig.isSupplyHitboxEnabled();
        boolean showRodRadius  = PhantomConfig.isSupplyRodRadiusEnabled();
        boolean showPearlHitbox = PhantomConfig.isSupplyPearlHitboxEnabled();

        if (!showHitbox && !showRodRadius && !showPearlHitbox) return;
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.SUPPLIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 camPos = camera.position();
        Matrix4f m  = matrices.last().pose();

        List<SupplyCluster> clusters = SupplyWaypointTracker.detectedClusters;
        List<Zombie> zombies = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Zombie z)) continue;
            if (!z.isAlive()) continue;
            boolean nearCluster = false;
            for (SupplyCluster c : clusters) {
                double dx = z.getX() - c.center.x;
                double dz = z.getZ() - c.center.z;
                if (dx * dx + dz * dz <= 4.0) { nearCluster = true; break; }
            }
            if (!nearCluster) continue;
            zombies.add(z);
        }

        SubmitNodeCollector collector = WorldRenderCollector.get();
        if (collector == null) return;

        if (showHitbox || showPearlHitbox) {
            collector.submitCustomGeometry(matrices, RenderTypes.lines(), (pose, vc) -> {
                for (Zombie z : zombies) {
                    AABB bb = z.getBoundingBox();
                    double x1 = bb.minX - camPos.x, y1 = bb.minY - camPos.y, z1 = bb.minZ - camPos.z;
                    double x2 = bb.maxX - camPos.x, y2 = bb.maxY - camPos.y, z2 = bb.maxZ - camPos.z;

                    if (showHitbox) {
                        addOutline(vc, m, x1, y1, z1, x2, y2, z2, LB_R, LB_G, LB_B, OUTLINE_A);
                    }
                    if (showPearlHitbox) {
                        addOutline(vc, m, x1, y1, z1, x2, y2, z2, PH_R, PH_G, PH_B, PH_A);
                    }
                }
            });

            if (showHitbox) {
                collector.submitCustomGeometry(matrices, RenderTypes.debugQuads(), (pose, vc) -> {
                    for (Zombie z : zombies) {
                        AABB bb = z.getBoundingBox();
                        double x1 = bb.minX - camPos.x, y1 = bb.minY - camPos.y, z1 = bb.minZ - camPos.z;
                        double x2 = bb.maxX - camPos.x, y2 = bb.maxY - camPos.y, z2 = bb.maxZ - camPos.z;
                        addFill(vc, m, x1, y1, z1, x2, y2, z2, LB_R, LB_G, LB_B, FILL_A);
                    }
                });
            }
        }

        if (showRodRadius && !zombies.isEmpty()) {
            Vec3 bobberInLava = getBobberPosInLava(mc);
            List<Vec3> centers = clusterCenters(zombies);
            collector.submitCustomGeometry(matrices, RenderTypes.lines(), (pose, lines) -> {
                for (Vec3 ctr : centers) {
                    double drawY = Math.max(ctr.y, LAVA_SURFACE);
                    boolean inRange = bobberInLava != null && isWithinRadiusXZ(bobberInLava, ctr, ROD_RADIUS);
                    int r = inRange ? RD_R : LB_R;
                    int g = inRange ? RD_G : LB_G;
                    int b = inRange ? RD_B : LB_B;
                    drawHorizontalCircle(lines, m,
                            ctr.x - camPos.x, drawY - camPos.y, ctr.z - camPos.z,
                            ROD_RADIUS, r, g, b, OUTLINE_A);
                }
            });
        }
    }

    private static Vec3 getBobberPosInLava(Minecraft mc) {
        Vec3 pos = PhantomConfig.isLegacyRodPhysicsEnabled()
                ? PredictedBobber.getGhostPosition()
                : (mc.player != null && mc.player.fishing != null ? mc.player.fishing.position() : null);
        if (pos == null || mc.level == null) return null;
        // A bobber resting/floating on the surface sits right at the fluid block's top boundary, so
        // its own Y can land in the air block a hair above the surface — and if it's fully submerged
        // (sunk below the surface into a pool that goes deeper than one block), only checking its own
        // block would still catch that, but checking a couple blocks down too makes both cases robust
        // without needing an exact Y match.
        BlockPos bp = BlockPos.containing(pos.x, pos.y, pos.z);
        for (int dy = 0; dy >= -2; dy--) {
            if (mc.level.getFluidState(bp.offset(0, dy, 0)).is(FluidTags.LAVA)) return pos;
        }
        return null;
    }

    private static boolean isWithinRadiusXZ(Vec3 pos, Vec3 center, float radius) {
        double dx = pos.x - center.x;
        double dz = pos.z - center.z;
        return dx * dx + dz * dz <= (double) radius * radius;
    }

    private static List<Vec3> clusterCenters(List<Zombie> zombies) {
        boolean[] used = new boolean[zombies.size()];
        List<Vec3> centers = new ArrayList<>();
        for (int i = 0; i < zombies.size(); i++) {
            if (used[i]) continue;
            Vec3 seed = zombies.get(i).position();
            double sx = seed.x, sy = seed.y, sz = seed.z;
            int count = 1;
            used[i] = true;
            for (int j = i + 1; j < zombies.size(); j++) {
                if (used[j]) continue;
                if (seed.distanceTo(zombies.get(j).position()) <= 3.5) {
                    Vec3 p = zombies.get(j).position();
                    sx += p.x; sy += p.y; sz += p.z;
                    count++;
                    used[j] = true;
                }
            }
            centers.add(new Vec3(sx / count, sy / count, sz / count));
        }
        return centers;
    }

    private static void addOutline(VertexConsumer vc, Matrix4f m,
                                   double x1, double y1, double z1,
                                   double x2, double y2, double z2,
                                   int r, int g, int b, int a) {
        float ax = (float)x1, ay = (float)y1, az = (float)z1;
        float bx = (float)x2, by = (float)y2, bz = (float)z2;
        cline(vc, m, ax, ay, az, bx, ay, az, r, g, b, a,  1, 0, 0);
        cline(vc, m, bx, ay, az, bx, ay, bz, r, g, b, a,  0, 0, 1);
        cline(vc, m, bx, ay, bz, ax, ay, bz, r, g, b, a, -1, 0, 0);
        cline(vc, m, ax, ay, bz, ax, ay, az, r, g, b, a,  0, 0,-1);
        cline(vc, m, ax, by, az, bx, by, az, r, g, b, a,  1, 0, 0);
        cline(vc, m, bx, by, az, bx, by, bz, r, g, b, a,  0, 0, 1);
        cline(vc, m, bx, by, bz, ax, by, bz, r, g, b, a, -1, 0, 0);
        cline(vc, m, ax, by, bz, ax, by, az, r, g, b, a,  0, 0,-1);
        cline(vc, m, ax, ay, az, ax, by, az, r, g, b, a, 0, 1, 0);
        cline(vc, m, bx, ay, az, bx, by, az, r, g, b, a, 0, 1, 0);
        cline(vc, m, bx, ay, bz, bx, by, bz, r, g, b, a, 0, 1, 0);
        cline(vc, m, ax, ay, bz, ax, by, bz, r, g, b, a, 0, 1, 0);
    }

    private static void cline(VertexConsumer vc, Matrix4f m,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               int r, int g, int b, int a,
                               float nx, float ny, float nz) {
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(3.0f);
        vc.addVertex(m, x2, y2, z2).setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(3.0f);
    }

    private static void addFill(VertexConsumer vc, Matrix4f m,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                int r, int g, int b, int a) {
        float ax = (float)x1, ay = (float)y1, az = (float)z1;
        float bx = (float)x2, by = (float)y2, bz = (float)z2;
        cquad(vc, m, ax,ay,az, bx,ay,az, bx,ay,bz, ax,ay,bz, r,g,b,a,  0,-1, 0);
        cquad(vc, m, ax,ay,bz, bx,ay,bz, bx,ay,az, ax,ay,az, r,g,b,a,  0, 1, 0);
        cquad(vc, m, ax,by,az, ax,by,bz, bx,by,bz, bx,by,az, r,g,b,a,  0, 1, 0);
        cquad(vc, m, bx,by,az, bx,by,bz, ax,by,bz, ax,by,az, r,g,b,a,  0,-1, 0);
        cquad(vc, m, ax,ay,az, ax,by,az, bx,by,az, bx,ay,az, r,g,b,a,  0, 0,-1);
        cquad(vc, m, bx,ay,az, bx,by,az, ax,by,az, ax,ay,az, r,g,b,a,  0, 0, 1);
        cquad(vc, m, ax,ay,bz, bx,ay,bz, bx,by,bz, ax,by,bz, r,g,b,a,  0, 0, 1);
        cquad(vc, m, ax,by,bz, bx,by,bz, bx,ay,bz, ax,ay,bz, r,g,b,a,  0, 0,-1);
        cquad(vc, m, ax,ay,az, ax,ay,bz, ax,by,bz, ax,by,az, r,g,b,a, -1, 0, 0);
        cquad(vc, m, ax,by,az, ax,by,bz, ax,ay,bz, ax,ay,az, r,g,b,a,  1, 0, 0);
        cquad(vc, m, bx,ay,az, bx,by,az, bx,by,bz, bx,ay,bz, r,g,b,a,  1, 0, 0);
        cquad(vc, m, bx,ay,bz, bx,by,bz, bx,by,az, bx,ay,az, r,g,b,a, -1, 0, 0);
    }

    private static void cquad(VertexConsumer vc, Matrix4f m,
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

    private static void drawHorizontalCircle(VertexConsumer vc, Matrix4f m,
                                             double cx, double cy, double cz,
                                             float radius, int r, int g, int b, int a) {
        for (int i = 0; i < CIRCLE_SEGS; i++) {
            double a0 = 2 * Math.PI * i       / CIRCLE_SEGS;
            double a1 = 2 * Math.PI * (i + 1) / CIRCLE_SEGS;
            float x0 = (float)(cx + radius * Math.cos(a0));
            float z0 = (float)(cz + radius * Math.sin(a0));
            float x1 = (float)(cx + radius * Math.cos(a1));
            float z1 = (float)(cz + radius * Math.sin(a1));
            vc.addVertex(m, x0, (float)cy, z0).setColor(r, g, b, a).setNormal(0f, 1f, 0f).setLineWidth(3.0f);
            vc.addVertex(m, x1, (float)cy, z1).setColor(r, g, b, a).setNormal(0f, 1f, 0f).setLineWidth(3.0f);
        }
    }
}
