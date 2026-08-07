package com.phantomaddons.features.build;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.build.buildprogress.BuildProgressTracker;
import com.phantomaddons.features.supplies.pearlwaypoints.PearlLocation;
import com.phantomaddons.phase.KuudraPhaseTracker;
import com.phantomaddons.phase.KuudraPhaseTracker.Phase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.phantomaddons.utils.AlwaysOnTopRenderTypes;
import com.phantomaddons.utils.WorldRenderCollector;

public final class BuildBeaconRenderer {

    private static final float R   = 0.15f;
    private static final float TOP = 200f;

    private BuildBeaconRenderer() {}

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!PhantomConfig.isBuildBeaconsEnabled()) return;
        if (KuudraPhaseTracker.getPhase() != Phase.BUILD) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Vec3     cam = camera.position();
        double   cx  = cam.x, cy = cam.y, cz = cam.z;
        Matrix4f m   = matrices.last().pose();

        SubmitNodeCollector collector = WorldRenderCollector.get();
        if (collector == null) return;

        for (PearlLocation loc : PearlLocation.values()) {
            if (BuildProgressTracker.isComplete(loc)) continue;

            int   pct = BuildProgressTracker.getProgress(loc);
            int[] rgb = progressColour(pct);
            int   r   = rgb[0], g = rgb[1], b = rgb[2];

            double bx = loc.landingPos.x - cx;
            double by = loc.landingPos.y - cy;
            double bz = loc.landingPos.z - cz;

            for (int pass = 0; pass < 2; pass++) {
                var type = pass == 1 ? AlwaysOnTopRenderTypes.debugQuads() : RenderTypes.debugQuads();
                collector.submitCustomGeometry(matrices, type, (pose, vc) -> addBeam(vc, m, bx, by, bz, r, g, b,
                        (int)(PhantomConfig.getBuildBeaconAlpha() * 255)));
            }
        }
    }

    private static void addBeam(VertexConsumer vc, Matrix4f m,
                                double bx, double by, double bz,
                                int r, int g, int b, int a) {
        float x0 = (float)(bx - R), x1 = (float)(bx + R);
        float z0 = (float)(bz - R), z1 = (float)(bz + R);
        float y0 = (float) by;
        float y1 = (float)(by + TOP);

        quad(vc, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a,  0, 0, 1);
        quad(vc, m, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, r, g, b, a,  0, 0,-1);
        quad(vc, m, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, r, g, b, a,  1, 0, 0);
        quad(vc, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a, -1, 0, 0);
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

    private static int[] progressColour(int pct) {
        if (pct < 0)   return new int[]{ 255, 255, 255 };
        if (pct >= 75) return new int[]{  68, 255,  68 };
        if (pct >= 50) return new int[]{ 255, 255,   0 };
        if (pct >= 25) return new int[]{ 255, 153,   0 };
        return new int[]{ 255,  51,  51 };
    }
}