package com.phantomaddons.features.supplies.tentaclewarn;

import com.phantomaddons.PhantomConfig;
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
import com.phantomaddons.utils.ImmediateDraw;
import com.phantomaddons.utils.WorldRenderCollector;
//?}
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public final class TentacleGrabWarningRenderer {

    private static final int[] RED    = {255, 50, 50};
    private static final int[] YELLOW = {255, 215, 40};
    private static final int[] DARK_RED    = {130, 0, 0};
    private static final int[] DARK_YELLOW = {150, 110, 0};
    private static final int OUTLINE_A = 255;
    private static final int FILL_A = 255;   // solid
    private static final float LINE_WIDTH = 3.5f;

    private TentacleGrabWarningRenderer() {}

    private static int[] colour(TentacleGrabWarning.Target t) { return t.primary() ? RED : YELLOW; }

    private static int[] outlineColour(TentacleGrabWarning.Target t) { return t.primary() ? DARK_RED : DARK_YELLOW; }

    private static Vec3 lineStart(Camera camera) {
        return camera.position().add(Vec3.directionFromRotation(camera.xRot(), camera.yRot()));
    }

    private static Vec3 lineEnd(TentacleGrabWarning.Target t) {
        return t.anchor().add(0.0, 0.26, 0.0);
    }

    private static boolean aimedAt(Camera camera, TentacleGrabWarning.Target t) {
        Vec3 from = camera.position();
        Vec3 to = from.add(Vec3.directionFromRotation(camera.xRot(), camera.yRot()).scale(128.0));
        for (TentacleGrabWarning.Box b : t.boxes()) {
            double rad = Math.toRadians(b.rotationDeg()), cos = Math.cos(rad), sin = Math.sin(rad);
            Vec3 a = toLocal(from.subtract(b.center()), cos, sin), z = toLocal(to.subtract(b.center()), cos, sin);
            AABB local = new AABB(-b.halfWidth(), -b.halfHeight(), -b.halfWidth(), b.halfWidth(), b.halfHeight(), b.halfWidth()).inflate(0.1);
            if (local.clip(a, z).isPresent()) return true;
        }
        return false;
    }

    private static Vec3 toLocal(Vec3 v, double cos, double sin) {
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    private static float[] corners(TentacleGrabWarning.Box b, Vec3 cam) {
        double rad = Math.toRadians(b.rotationDeg()), cos = Math.cos(rad), sin = Math.sin(rad);
        float[] k = new float[24];
        for (int i = 0; i < 8; i++) {
            double lx = (i & 1) == 0 ? -b.halfWidth() : b.halfWidth();
            double ly = (i & 2) == 0 ? -b.halfHeight() : b.halfHeight();
            double lz = (i & 4) == 0 ? -b.halfWidth() : b.halfWidth();
            k[i * 3]     = (float) (b.center().x - cam.x + lx * cos + lz * sin);
            k[i * 3 + 1] = (float) (b.center().y - cam.y + ly);
            k[i * 3 + 2] = (float) (b.center().z - cam.z - lx * sin + lz * cos);
        }
        return k;
    }

    private static boolean wantsLine(Camera camera, TentacleGrabWarning.Target t) {
        return t.primary() && PhantomConfig.isTentacleGrabLineEnabled() && !aimedAt(camera, t);
    }

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!PhantomConfig.isTentacleGrabWarningEnabled()) return;
        List<TentacleGrabWarning.Target> targets = TentacleGrabWarning.getTargets();
        if (targets.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 cam = camera.position();
        Matrix4f m = matrices.last().pose();

        //? if <26.2 {
        /*MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();

        VertexConsumer vf = imm.getBuffer(RenderTypes.debugQuads());
        for (TentacleGrabWarning.Target t : targets) {
            for (TentacleGrabWarning.Box b : t.boxes()) {
                addFill(vf, m, corners(b, cam), colour(t), FILL_A);
            }
        }
        imm.endBatch();

        VertexConsumer vo = imm.getBuffer(RenderTypes.lines());
        for (TentacleGrabWarning.Target t : targets) {
            for (TentacleGrabWarning.Box b : t.boxes()) {
                addOutline(vo, m, corners(b, cam), outlineColour(t));
            }
        }
        imm.endBatch();

        // Only the guide line shows through walls.
        GL11.glDepthFunc(GL11.GL_ALWAYS);
        VertexConsumer vl = imm.getBuffer(RenderTypes.lines());
        for (TentacleGrabWarning.Target t : targets) {
            if (wantsLine(camera, t)) {
                Vec3 s = lineStart(camera), e = lineEnd(t);
                line(vl, m, (float) (s.x - cam.x), (float) (s.y - cam.y), (float) (s.z - cam.z),
                        (float) (e.x - cam.x), (float) (e.y - cam.y), (float) (e.z - cam.z), RED, LINE_WIDTH);
            }
        }
        imm.endBatch();
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        *///?} else {
        SubmitNodeCollector collector = WorldRenderCollector.get();
        if (collector == null) return;

        for (TentacleGrabWarning.Target t : targets) {
            int[] rgb = colour(t), edge = outlineColour(t);
            for (TentacleGrabWarning.Box b : t.boxes()) {
                float[] k = corners(b, cam);

                matrices.pushPose();
                matrices.translate(b.center().x - cam.x, b.center().y - cam.y, b.center().z - cam.z);
                collector.submitCustomGeometry(matrices, RenderTypes.debugQuads(),
                        (pose, vf) -> addFill(vf, m, k, rgb, FILL_A));
                collector.submitCustomGeometry(matrices, RenderTypes.lines(),
                        (pose, vl) -> addOutline(vl, m, k, edge));
                matrices.popPose();
            }
        }
        //?}
    }

    //? if <26.2 {
    /*public static void renderAlwaysOnTop(PoseStack matrices, Camera camera, float tickDelta) {}
    *///?} else {
    public static void renderAlwaysOnTop(PoseStack matrices, Camera camera, float tickDelta) {
        if (!PhantomConfig.isTentacleGrabWarningEnabled()) return;
        List<TentacleGrabWarning.Target> targets = TentacleGrabWarning.getTargets();
        if (targets.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 cam = camera.position();
        Matrix4f m = matrices.last().pose();

        for (TentacleGrabWarning.Target t : targets) {
            if (wantsLine(camera, t)) {
                Vec3 s = lineStart(camera), e = lineEnd(t);
                VertexConsumer ll = ImmediateDraw.begin(AlwaysOnTopRenderTypes.lines());
                line(ll, m, (float) (s.x - cam.x), (float) (s.y - cam.y), (float) (s.z - cam.z),
                        (float) (e.x - cam.x), (float) (e.y - cam.y), (float) (e.z - cam.z), RED, LINE_WIDTH);
            }
        }
    }
    //?}

    private static void addOutline(VertexConsumer vc, Matrix4f m, float[] k, int[] rgb) {
        for (int a = 0; a < 8; a++) {
            for (int bit = 1; bit < 8; bit <<= 1) {
                int b = a | bit;
                if (b == a) continue;
                line(vc, m, k[a * 3], k[a * 3 + 1], k[a * 3 + 2], k[b * 3], k[b * 3 + 1], k[b * 3 + 2], rgb, 2.5f);
            }
        }
    }

    private static void line(VertexConsumer vc, Matrix4f m,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             int[] rgb, float width) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;
        vc.addVertex(m, x1, y1, z1).setColor(rgb[0], rgb[1], rgb[2], OUTLINE_A).setNormal(nx, ny, nz).setLineWidth(width);
        vc.addVertex(m, x2, y2, z2).setColor(rgb[0], rgb[1], rgb[2], OUTLINE_A).setNormal(nx, ny, nz).setLineWidth(width);
    }

    private static void addFill(VertexConsumer vc, Matrix4f m, float[] k, int[] rgb, int a) {
        for (int axis = 0; axis < 3; axis++) {
            int u = 1 << ((axis + 1) % 3), v = 1 << ((axis + 2) % 3);
            for (int side = 0; side < 2; side++) {
                int base = side << axis;
                int p0 = base, p1 = base | u, p2 = base | u | v, p3 = base | v;
                face(vc, m, k, p0, p1, p2, p3, rgb, a);
                face(vc, m, k, p3, p2, p1, p0, rgb, a);
            }
        }
    }

    private static void face(VertexConsumer vc, Matrix4f m, float[] k, int p0, int p1, int p2, int p3, int[] rgb, int a) {
        float ux = k[p1 * 3] - k[p0 * 3], uy = k[p1 * 3 + 1] - k[p0 * 3 + 1], uz = k[p1 * 3 + 2] - k[p0 * 3 + 2];
        float vx = k[p3 * 3] - k[p0 * 3], vy = k[p3 * 3 + 1] - k[p0 * 3 + 1], vz = k[p3 * 3 + 2] - k[p0 * 3 + 2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len == 0) return;
        nx /= len; ny /= len; nz /= len;
        quad(vc, m,
                k[p0 * 3], k[p0 * 3 + 1], k[p0 * 3 + 2], k[p1 * 3], k[p1 * 3 + 1], k[p1 * 3 + 2],
                k[p2 * 3], k[p2 * 3 + 1], k[p2 * 3 + 2], k[p3 * 3], k[p3 * 3 + 1], k[p3 * 3 + 2],
                nx, ny, nz, rgb[0], rgb[1], rgb[2], a);
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
