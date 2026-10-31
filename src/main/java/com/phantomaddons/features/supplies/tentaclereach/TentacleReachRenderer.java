package com.phantomaddons.features.supplies.tentaclereach;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.phantomaddons.PhantomConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
//? if <26.2 {
/*import net.minecraft.client.renderer.MultiBufferSource;
*///?} else {
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.phantomaddons.utils.WorldRenderCollector;
//?}
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TentacleReachRenderer {

    private static final double R = TentacleReach.RADIUS, CENTER_Y = TentacleReach.CENTER_Y;
    private static final int[] SPHERE_RGB = {70, 220, 255};
    private static final int[] INSIDE_RGB = {255, 40, 40};
    private static final int[] LINE_RGB = {255, 150, 30};

    private static final double SEARCH_FROM_Y = 80;
    private static final int    BARRIER_SEARCH_TOP = 120;
    private static final double DRAW_MARGIN = 8.0;
    private static final int    LON = 96, LAT = 48;
    private static final double SAGITTA = 0.002;
    private static final int    CLIP_SIDES = 360;
    private static final int    DIFF_SIDES = 180;
    private static final long   GROUND_RETRY_MS = 1000, GROUND_REFRESH_MS = 5000, GEOMETRY_REFRESH_MS = 500;
    private static final int    GROUND_BUDGET = 600;
    private static final int    LINE_A = 255;
    private static final float  LINE_WIDTH = 3.0f;

    private record Lines(long at, List<double[]> lines) {}

    private record Fills(long at, String key, List<double[]> cyan, List<double[]> red, List<double[]> lavaCyan, List<double[]> lavaRed) {}

    private static final Map<String, Lines> lineCache = new HashMap<>();
    private static Fills fillCache;
    private static final Map<Long, double[]> ground = new HashMap<>();
    private static int groundBudget;
    private static double lavaY = Double.NaN;

    private static final float[] UX = new float[(LAT + 1) * (LON + 1)], UY = new float[UX.length], UZ = new float[UX.length];

    static {
        for (int i = 0; i <= LAT; i++) {
            double phi = -Math.PI / 2.0 + Math.PI * i / LAT;
            for (int j = 0; j <= LON; j++) {
                double theta = Math.PI * 2.0 * j / LON;
                int k = i * (LON + 1) + j;
                UX[k] = (float) (Math.cos(phi) * Math.cos(theta));
                UY[k] = (float) Math.sin(phi);
                UZ[k] = (float) (Math.cos(phi) * Math.sin(theta));
            }
        }
    }

    private TentacleReachRenderer() {}

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        if (!PhantomConfig.isTentacleReachEnabled()) return;
        List<double[]> all = TentacleReach.getCentres();
        if (all.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        double px = mc.player.getX(), pz = mc.player.getZ();
        List<double[]> near = new ArrayList<>();
        for (double[] c : all) if (Math.hypot(c[0] - px, c[1] - pz) <= R + DRAW_MARGIN) near.add(c);
        if (near.isEmpty()) return;

        groundBudget = GROUND_BUDGET;
        boolean groundMode = PhantomConfig.isTentacleReachGround();
        boolean lavaToo = PhantomConfig.isTentacleReachLava();
        int alpha = Math.max(1, Math.min(255, Math.round(PhantomConfig.getTentacleReachOpacity() * 255f)));
        Vec3 cam = camera.position();
        Matrix4f m = new Matrix4f(matrices.last().pose());

        //? if <26.2 {
        /*MultiBufferSource.BufferSource imm = mc.renderBuffers().bufferSource();
        fill(imm.getBuffer(RenderTypes.debugQuads()), m, cam, near, groundMode, lavaToo, alpha);
        imm.endBatch();
        outline(imm.getBuffer(RenderTypes.lines()), m, cam, near);
        imm.endBatch();
        *///?} else {
        SubmitNodeCollector collector = WorldRenderCollector.get();
        if (collector == null) return;

        matrices.pushPose();
        matrices.translate(near.get(0)[0] - cam.x, mc.player.getY() - cam.y, near.get(0)[1] - cam.z);
        collector.submitCustomGeometry(matrices, RenderTypes.debugQuads(),
                (pose, vf) -> fill(vf, m, cam, near, groundMode, lavaToo, alpha));
        collector.submitCustomGeometry(matrices, RenderTypes.lines(),
                (pose, vl) -> outline(vl, m, cam, near));
        matrices.popPose();
        //?}
    }

    private static void fill(VertexConsumer vc, Matrix4f m, Vec3 cam, List<double[]> centres, boolean groundMode,
                             boolean lavaToo, int alpha) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) return;

        boolean[] red = new boolean[centres.size()];
        for (int k = 0; k < red.length; k++) {
            double flat = Math.hypot(mc.player.getX() - centres.get(k)[0], mc.player.getZ() - centres.get(k)[1]);
            double up = mc.player.getY() - CENTER_Y;
            red[k] = flat * flat + up * up <= R * R;
        }

        if (!groundMode) {
            spheres(vc, m, cam, centres, red, alpha);
            return;
        }
        Fills f = fillsFor(level, centres, red);
        shapes(vc, m, cam, f.cyan(), SPHERE_RGB, alpha);
        shapes(vc, m, cam, f.red(), INSIDE_RGB, alpha);
        if (lavaToo) {
            shapes(vc, m, cam, f.lavaCyan(), SPHERE_RGB, alpha);
            shapes(vc, m, cam, f.lavaRed(), INSIDE_RGB, alpha);
        }
    }

    private static void outline(VertexConsumer vc, Matrix4f m, Vec3 cam, List<double[]> centres) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) return;
        for (double[] c : centres) {
            double flat = Math.hypot(mc.player.getX() - c[0], mc.player.getZ() - c[1]), up = mc.player.getY() - CENTER_Y;
            int[] rgb = flat * flat + up * up <= R * R ? INSIDE_RGB : LINE_RGB;   // red while you're inside this sphere
            for (double[] g : linesFor(level, c[0], c[1])) {
                double lift = lift(g[0] - cam.x, g[2] - cam.z) + 0.002;   // a hair above the highlight so the line stays on top of it
                line(vc, m, rgb,
                        (float) (g[0] + g[6] * lift - cam.x), (float) (g[1] + g[7] * lift - cam.y), (float) (g[2] + g[8] * lift - cam.z),
                        (float) (g[3] + g[6] * lift - cam.x), (float) (g[4] + g[7] * lift - cam.y), (float) (g[5] + g[8] * lift - cam.z));
            }
        }
    }

    private static void shapes(VertexConsumer vc, Matrix4f m, Vec3 cam, List<double[]> polys, int[] rgb, int alpha) {
        for (double[] p : polys) {
            int n = (p.length - 3) / 3;
            double lift = lift(p[3] - cam.x, p[5] - cam.z);
            float[] x = new float[n], y = new float[n], z = new float[n];
            for (int k = 0; k < n; k++) {
                x[k] = (float) (p[3 + 3 * k] + p[0] * lift - cam.x);
                y[k] = (float) (p[4 + 3 * k] + p[1] * lift - cam.y);
                z[k] = (float) (p[5 + 3 * k] + p[2] * lift - cam.z);
            }
            for (int k = 1; k + 1 < n; k++) {
                quad(vc, m, rgb, alpha, x[0], y[0], z[0], x[k], y[k], z[k], x[k + 1], y[k + 1], z[k + 1], x[k + 1], y[k + 1], z[k + 1]);
            }
        }
    }

    private static double lift(double dx, double dz) {
        return 0.002 + 2.6e-6 * (dx * dx + dz * dz);
    }

    // ── Spheres ───────────────────────────────────────────────────────────────

    private static void spheres(VertexConsumer vc, Matrix4f m, Vec3 cam, List<double[]> centres, boolean[] red, int a) {
        float rr = (float) (R / (Math.cos(Math.PI / (2.0 * LAT)) * Math.cos(Math.PI / LON)));

        for (int s = 0; s < centres.size(); s++) {
            if (duplicate(centres, red, s)) continue;
            int[] rgb = red[s] ? INSIDE_RGB : SPHERE_RGB;
            double cx = centres.get(s)[0], cz = centres.get(s)[1];

            List<float[]> planes = new ArrayList<>();
            for (int j = 0; j < centres.size(); j++) {
                if (j == s || (!red[j] && red[s]) || duplicate(centres, red, j)) continue;   // a cyan sphere never hides a red one
                double dx = centres.get(j)[0] - cx, dz = centres.get(j)[1] - cz, d = Math.hypot(dx, dz);
                if (d < 1.0e-3 || d >= 2.0 * R) continue;
                planes.add(new float[] { (float) (dx / d), (float) (dz / d), (float) (d / 2.0) });
            }

            float ox = (float) (cx - cam.x), oy = (float) (CENTER_Y - cam.y), oz = (float) (cz - cam.z);
            for (int i = 0; i < LAT; i++) {
                for (int j = 0; j < LON; j++) {
                    int[] k = { i * (LON + 1) + j, i * (LON + 1) + j + 1, (i + 1) * (LON + 1) + j + 1, (i + 1) * (LON + 1) + j };
                    List<float[]> poly = new ArrayList<>(4);
                    for (int c = 0; c < 4; c++) poly.add(new float[] { rr * UX[k[c]], rr * UY[k[c]], rr * UZ[k[c]] });
                    for (float[] plane : planes) {
                        poly = clipPlane(poly, plane);
                        if (poly.size() < 3) break;
                    }
                    if (poly.size() < 3) continue;
                    float[] v0 = poly.get(0);
                    for (int c = 1; c + 1 < poly.size(); c++) {
                        float[] v1 = poly.get(c), v2 = poly.get(c + 1);
                        quad(vc, m, rgb, a, ox + v0[0], oy + v0[1], oz + v0[2], ox + v1[0], oy + v1[1], oz + v1[2],
                                ox + v2[0], oy + v2[1], oz + v2[2], ox + v2[0], oy + v2[1], oz + v2[2]);
                    }
                }
            }
        }
    }

    private static List<float[]> clipPlane(List<float[]> poly, float[] plane) {
        float nx = plane[0], nz = plane[1], t = plane[2];
        List<float[]> out = new ArrayList<>(poly.size() + 1);
        for (int i = 0; i < poly.size(); i++) {
            float[] p = poly.get(i), q = poly.get((i + 1) % poly.size());
            float dp = p[0] * nx + p[2] * nz - t, dq = q[0] * nx + q[2] * nz - t;
            if (dp <= 0) out.add(p);
            if ((dp < 0 && dq > 0) || (dp > 0 && dq < 0)) {
                float f = dp / (dp - dq);
                out.add(new float[] { p[0] + (q[0] - p[0]) * f, p[1] + (q[1] - p[1]) * f, p[2] + (q[2] - p[2]) * f });
            }
        }
        return out;
    }

    private static boolean duplicate(List<double[]> centres, boolean[] red, int s) {
        for (int j = 0; j < s; j++) {
            if (Math.abs(centres.get(j)[0] - centres.get(s)[0]) < 1.0e-3 && Math.abs(centres.get(j)[1] - centres.get(s)[1]) < 1.0e-3
                    && (red[j] || !red[s])) return true;
        }
        return false;
    }

    // ── Ground ────────────────────────────────────────────────────────────────

    private static double[] groundAt(Level level, int bx, int bz) {
        long id = ((long) bx << 32) ^ (bz & 0xffffffffL);
        double[] e = ground.get(id);
        long now = System.currentTimeMillis();
        boolean stale = e == null || now - (long) e[1] > (Double.isNaN(e[0]) ? GROUND_RETRY_MS : GROUND_REFRESH_MS);
        if (stale && groundBudget > 0) {
            groundBudget--;
            double[] s = scan(level, bx, bz);   // refreshed because the bridge and such get destroyed
            e = new double[] { s[0], now, s[1], s[2] };
            ground.put(id, e);
        }
        return e == null ? new double[] { Double.NaN, 0, 0, 0 } : e;
    }

    private static double[] scan(Level level, int bx, int bz) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(bx, 0, bz);
        boolean barrier = false;
        for (int y = BARRIER_SEARCH_TOP; y > (int) SEARCH_FROM_Y && !barrier; y--) {
            pos.setY(y);
            barrier = level.getBlockState(pos).is(Blocks.BARRIER);
        }
        for (int y = (int) SEARCH_FROM_Y; y >= level.getMinY(); y--) {
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.is(Blocks.BARRIER)) { barrier = true; continue; }
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!state.getFluidState().isEmpty() && shape.isEmpty()) {
                double top = y + state.getFluidState().getHeight(level, pos);
                boolean lava = state.getFluidState().is(FluidTags.LAVA);
                if (lava) lavaY = top;
                return new double[] { top, lava ? 1 : 0, barrier ? 1 : 0 };
            }
            if (shape.isEmpty()) continue;
            if (y == (int) SEARCH_FROM_Y) return new double[] { lavaY, 1, barrier ? 1 : 0 };
            return new double[] { y + shape.max(Direction.Axis.Y), 0, barrier ? 1 : 0 };
        }
        return new double[] { Double.NaN, 0, barrier ? 1 : 0 };
    }

    private record Grid(int x0, int z0, int w, int h, double[][] top, boolean[][] lava) {}

    private static Grid grid(Level level, List<double[]> centres) {
        double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (double[] c : centres) {
            minX = Math.min(minX, c[0]); maxX = Math.max(maxX, c[0]);
            minZ = Math.min(minZ, c[1]); maxZ = Math.max(maxZ, c[1]);
        }
        int x0 = (int) Math.floor(minX - R) - 1, z0 = (int) Math.floor(minZ - R) - 1;
        int w = (int) Math.floor(maxX + R) + 2 - x0 + 1, h = (int) Math.floor(maxZ + R) + 2 - z0 + 1;
        double[][] top = new double[w][h];
        boolean[][] lava = new boolean[w][h], barrier = new boolean[w][h], near = new boolean[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                top[i][j] = Double.NaN;
                for (double[] c : centres) {
                    if (Math.hypot(x0 + i + 0.5 - c[0], z0 + j + 0.5 - c[1]) <= R + 1.5) { near[i][j] = true; break; }
                }
                if (!near[i][j]) continue;
                double[] e = groundAt(level, x0 + i, z0 + j);
                top[i][j] = e[0];
                lava[i][j] = e[2] > 0.5;
                barrier[i][j] = e[3] > 0.5;
            }
        }

        boolean[][] reached = new boolean[w][h];
        int[][] steps = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (double[] c : centres) {
            int si = (int) Math.floor(c[0]) - x0, sj = (int) Math.floor(c[1]) - z0;
            if (si < 0 || sj < 0 || si >= w || sj >= h || reached[si][sj]) continue;
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            reached[si][sj] = true;
            queue.add(new int[] { si, sj });
            while (!queue.isEmpty()) {
                int[] at = queue.poll();
                for (int[] d : steps) {
                    int ni = at[0] + d[0], nj = at[1] + d[1];
                    if (ni < 0 || nj < 0 || ni >= w || nj >= h || reached[ni][nj] || !near[ni][nj] || barrier[ni][nj]) continue;
                    reached[ni][nj] = true;
                    queue.add(new int[] { ni, nj });
                }
            }
        }
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) if (!reached[i][j] || barrier[i][j]) top[i][j] = Double.NaN;
        return new Grid(x0, z0, w, h, top, lava);
    }

    // ── The line where the sphere meets the ground ────────────────────────────

    private static List<double[]> linesFor(Level level, double cx, double cz) {
        String id = Math.round(cx * 100) + "," + Math.round(cz * 100);
        long now = System.currentTimeMillis();
        Lines known = lineCache.get(id);
        if (known != null && now - known.at() < GEOMETRY_REFRESH_MS) return known.lines();

        Grid grid = grid(level, List.of(new double[] { cx, cz }));
        int x0 = grid.x0(), z0 = grid.z0(), w = grid.w(), h = grid.h();
        double[][] top = grid.top();

        List<double[]> lines = new ArrayList<>();
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                double g = top[i][j];
                if (Double.isNaN(g)) continue;
                int bx = x0 + i, bz = z0 + j;

                double dy = g - CENTER_Y;
                if (dy * dy < R * R) {
                    double rho = Math.sqrt(R * R - dy * dy);
                    for (double[] arc : arcsInRect(cx, cz, rho, bx, bz, bx + 1, bz + 1)) {
                        for (int k = 0; k + 3 < arc.length; k += 2) {
                            lines.add(new double[] { arc[k], g, arc[k + 1], arc[k + 2], g, arc[k + 3], 0, 1, 0 });
                        }
                    }
                }
                if (i + 1 < w) sideLines(lines, cx, cz, true, bx, bz, g, top[i + 1][j]);
                if (j + 1 < h) sideLines(lines, cx, cz, false, bx, bz, g, top[i][j + 1]);
            }
        }
        lineCache.put(id, new Lines(now, lines));
        return lines;
    }

    private static void sideLines(List<double[]> lines, double cx, double cz, boolean xBorder, int bx, int bz, double gA, double gB) {
        if (Double.isNaN(gA) || Double.isNaN(gB) || Math.abs(gA - gB) < 1.0e-6) return;
        double lo = Math.min(gA, gB), hi = Math.max(gA, gB);
        boolean aTaller = gA > gB;
        double plane = xBorder ? bx + 1 : bz + 1;
        double d = plane - (xBorder ? cx : cz);
        if (Math.abs(d) >= R) return;
        double rho = Math.sqrt(R * R - d * d);          // the sphere cuts that plane in a circle this big
        double cu = xBorder ? cz : cx;
        double u0 = xBorder ? bz : bx;
        double nx = xBorder ? (aTaller ? 1 : -1) : 0, nz = xBorder ? 0 : (aTaller ? 1 : -1);
        for (double[] arc : arcsInRect(cu, CENTER_Y, rho, u0, lo, u0 + 1, hi)) {
            for (int k = 0; k + 3 < arc.length; k += 2) {
                lines.add(xBorder
                        ? new double[] { plane, arc[k + 1], arc[k], plane, arc[k + 3], arc[k + 2], nx, 0, nz }
                        : new double[] { arc[k], arc[k + 1], plane, arc[k + 2], arc[k + 3], plane, nx, 0, nz });
            }
        }
    }

    // ── The highlighted ground ────────────────────────────────────────────────

    private static Fills fillsFor(Level level, List<double[]> centres, boolean[] red) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < centres.size(); k++) {
            sb.append(Math.round(centres.get(k)[0] * 100)).append(',').append(Math.round(centres.get(k)[1] * 100)).append(red[k] ? 'r' : 'c').append(';');
        }
        String key = sb.toString();
        long now = System.currentTimeMillis();
        if (fillCache != null && fillCache.key().equals(key) && now - fillCache.at() < GEOMETRY_REFRESH_MS) return fillCache;

        Grid grid = grid(level, centres);
        int x0 = grid.x0(), z0 = grid.z0(), w = grid.w(), h = grid.h();
        double[][] top = grid.top();
        boolean[][] lava = grid.lava();

        List<double[]> cyan = new ArrayList<>(), redShapes = new ArrayList<>(), lavaCyan = new ArrayList<>(), lavaRed = new ArrayList<>();
        int n = centres.size();
        double[] cu = new double[n], cv = new double[n], rho = new double[n];
        boolean[] isRed = red;
        List<double[]> cyanUv = new ArrayList<>(), redUv = new ArrayList<>();

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                double g = top[i][j];
                if (Double.isNaN(g)) continue;
                int bx = x0 + i, bz = z0 + j;

                double dy = g - CENTER_Y;
                if (dy * dy < R * R) {
                    double r = Math.sqrt(R * R - dy * dy);
                    for (int k = 0; k < n; k++) { cu[k] = centres.get(k)[0]; cv[k] = centres.get(k)[1]; rho[k] = r; }
                    cyanUv.clear(); redUv.clear();
                    unionUv(cu, cv, rho, isRed, bx, bz, bx + 1, bz + 1, cyanUv, redUv);
                    for (double[] uv : cyanUv) (lava[i][j] ? lavaCyan : cyan).add(shape(uv, 0, 1, 0, g, -1, false, 0));
                    for (double[] uv : redUv) (lava[i][j] ? lavaRed : redShapes).add(shape(uv, 0, 1, 0, g, -1, false, 0));
                }

                if (i + 1 < w) sideFill(centres, isRed, true, bx, bz, g, top[i + 1][j], lava[i][j], lava[i + 1][j], cyan, redShapes, lavaCyan, lavaRed);
                if (j + 1 < h) sideFill(centres, isRed, false, bx, bz, g, top[i][j + 1], lava[i][j], lava[i][j + 1], cyan, redShapes, lavaCyan, lavaRed);
            }
        }
        fillCache = new Fills(now, key, cyan, redShapes, lavaCyan, lavaRed);
        return fillCache;
    }

    private static void sideFill(List<double[]> centres, boolean[] red, boolean xBorder, int bx, int bz, double gA, double gB,
                                 boolean lavaA, boolean lavaB, List<double[]> cyan, List<double[]> redShapes,
                                 List<double[]> lavaCyan, List<double[]> lavaRed) {
        if (Double.isNaN(gA) || Double.isNaN(gB) || Math.abs(gA - gB) < 1.0e-6) return;
        double lo = Math.min(gA, gB), hi = Math.max(gA, gB);
        boolean aTaller = gA > gB;
        double plane = xBorder ? bx + 1 : bz + 1;
        double u0 = xBorder ? bz : bx;
        double nx = xBorder ? (aTaller ? 1 : -1) : 0, nz = xBorder ? 0 : (aTaller ? 1 : -1);
        int n = centres.size();
        double[] cu = new double[n], cv = new double[n], rho = new double[n];
        boolean any = false;
        for (int k = 0; k < n; k++) {
            double d = plane - (xBorder ? centres.get(k)[0] : centres.get(k)[1]);
            cu[k] = xBorder ? centres.get(k)[1] : centres.get(k)[0];
            cv[k] = CENTER_Y;
            rho[k] = Math.abs(d) < R ? Math.sqrt(R * R - d * d) : -1;   // the sphere cuts that plane in a circle this big
            any |= rho[k] > 0;
        }
        if (!any) return;

        List<double[]> cyanUv = new ArrayList<>(), redUv = new ArrayList<>();
        unionUv(cu, cv, rho, red, u0, lo, u0 + 1, hi, cyanUv, redUv);
        boolean lava = aTaller ? lavaA : lavaB;
        for (double[] uv : cyanUv) (lava ? lavaCyan : cyan).add(shape(uv, nx, 0, nz, 0, plane, xBorder, 1));
        for (double[] uv : redUv) (lava ? lavaRed : redShapes).add(shape(uv, nx, 0, nz, 0, plane, xBorder, 1));
    }

    private static double[] shape(double[] uv, double nx, double ny, double nz, double y, double plane, boolean xBorder, int mode) {
        int n = uv.length / 2;
        double[] out = new double[3 + n * 3];
        out[0] = nx; out[1] = ny; out[2] = nz;
        for (int k = 0; k < n; k++) {
            double u = uv[2 * k], v = uv[2 * k + 1];
            if (mode == 0) {
                out[3 + 3 * k] = u; out[4 + 3 * k] = y; out[5 + 3 * k] = v;
            } else {
                out[3 + 3 * k] = xBorder ? plane : u; out[4 + 3 * k] = v; out[5 + 3 * k] = xBorder ? u : plane;
            }
        }
        return out;
    }

    private static void unionUv(double[] cu, double[] cv, double[] rho, boolean[] red, double u0, double v0, double u1, double v1,
                                List<double[]> cyanOut, List<double[]> redOut) {
        List<Integer> reds = new ArrayList<>(), cyans = new ArrayList<>();
        boolean cyanFull = false;
        for (int k = 0; k < cu.length; k++) {
            if (rho[k] <= 0) continue;
            double nearU = Math.max(0, Math.max(u0 - cu[k], cu[k] - u1)), nearV = Math.max(0, Math.max(v0 - cv[k], cv[k] - v1));
            if (Math.hypot(nearU, nearV) >= rho[k]) continue;
            double farU = Math.max(Math.abs(u0 - cu[k]), Math.abs(u1 - cu[k])), farV = Math.max(Math.abs(v0 - cv[k]), Math.abs(v1 - cv[k]));
            boolean full = Math.hypot(farU, farV) <= rho[k];
            if (full && red[k]) { redOut.add(new double[] { u0, v0, u1, v0, u1, v1, u0, v1 }); return; }   // all red
            (red[k] ? reds : cyans).add(k);
            cyanFull |= full && !red[k];
        }
        if (reds.isEmpty() && cyans.isEmpty()) return;
        if (reds.isEmpty() && cyanFull) { cyanOut.add(new double[] { u0, v0, u1, v0, u1, v1, u0, v1 }); return; }

        List<double[]> square = new ArrayList<>(List.of(
                new double[] { u0, v0 }, new double[] { u1, v0 }, new double[] { u1, v1 }, new double[] { u0, v1 }));
        for (int k : reds) {
            List<double[]> cell = clipToDisk(powerCell(square, k, reds, cu, cv, rho), cu[k], cv[k], rho[k]);
            if (cell.size() >= 3) redOut.add(flatten(cell));
        }
        for (int k : cyans) {
            List<double[]> cell = clipToDisk(powerCell(square, k, cyans, cu, cv, rho), cu[k], cv[k], rho[k]);
            if (cell.size() < 3) continue;
            List<List<double[]>> pieces = new ArrayList<>();
            pieces.add(cell);
            for (int r : reds) pieces = minusDisk(pieces, cu[r], cv[r], rho[r]);
            for (List<double[]> piece : pieces) cyanOut.add(flatten(piece));
        }
    }

    private static List<double[]> powerCell(List<double[]> poly, int k, List<Integer> group, double[] cu, double[] cv, double[] rho) {
        for (int j : group) {
            if (j == k) continue;
            double delta = (rho[j] * rho[j] - cu[j] * cu[j] - cv[j] * cv[j]) - (rho[k] * rho[k] - cu[k] * cu[k] - cv[k] * cv[k]);
            double a = -2.0 * (cu[k] - cu[j]), b = -2.0 * (cv[k] - cv[j]);
            if (Math.abs(a) < 1.0e-9 && Math.abs(b) < 1.0e-9) {
                if (delta > 1.0e-9 || (Math.abs(delta) <= 1.0e-9 && j < k)) return new ArrayList<>();
                continue;
            }
            poly = clipHalf(poly, a, b, -delta);
            if (poly.size() < 3) return poly;
        }
        return poly;
    }

    private static List<double[]> clipToDisk(List<double[]> poly, double cu, double cv, double rho) {
        if (poly.size() < 3) return poly;
        boolean inside = true;
        for (double[] p : poly) if (Math.hypot(p[0] - cu, p[1] - cv) > rho) { inside = false; break; }
        if (inside) return poly;
        double apothem = rho * Math.cos(Math.PI / CLIP_SIDES);
        for (int k = 0; k < CLIP_SIDES && poly.size() >= 3; k++) {
            double a = Math.PI * 2.0 * (k + 0.5) / CLIP_SIDES, nu = Math.cos(a), nv = Math.sin(a);
            poly = clipHalf(poly, nu, nv, apothem + nu * cu + nv * cv);
        }
        return poly;
    }

    private static List<List<double[]>> minusDisk(List<List<double[]>> pieces, double cu, double cv, double rho) {
        List<List<double[]>> out = new ArrayList<>();
        double apothem = rho * Math.cos(Math.PI / DIFF_SIDES);
        for (List<double[]> piece : pieces) {
            boolean inside = true;
            for (double[] p : piece) if (Math.hypot(p[0] - cu, p[1] - cv) > rho) { inside = false; break; }
            if (inside) continue;
            if (distanceTo(piece, cu, cv) >= rho) { out.add(piece); continue; }

            List<double[]> rest = piece;
            for (int k = 0; k < DIFF_SIDES && rest.size() >= 3; k++) {
                double a = Math.PI * 2.0 * (k + 0.5) / DIFF_SIDES, nu = Math.cos(a), nv = Math.sin(a);
                double limit = apothem + nu * cu + nv * cv;
                List<double[]> beyond = clipHalf(rest, -nu, -nv, -limit);
                if (beyond.size() >= 3) out.add(beyond);
                rest = clipHalf(rest, nu, nv, limit);
            }
        }
        return out;
    }

    private static double distanceTo(List<double[]> poly, double pu, double pv) {
        boolean inside = true;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < poly.size(); i++) {
            double[] p = poly.get(i), q = poly.get((i + 1) % poly.size());
            double eu = q[0] - p[0], ev = q[1] - p[1];
            if (eu * (pv - p[1]) - ev * (pu - p[0]) < 0) inside = false;
            double len2 = eu * eu + ev * ev;
            double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((pu - p[0]) * eu + (pv - p[1]) * ev) / len2));
            best = Math.min(best, Math.hypot(pu - (p[0] + eu * t), pv - (p[1] + ev * t)));
        }
        return inside ? 0 : best;
    }

    private static List<double[]> clipHalf(List<double[]> poly, double a, double b, double c) {
        List<double[]> out = new ArrayList<>(poly.size() + 1);
        for (int i = 0; i < poly.size(); i++) {
            double[] p = poly.get(i), q = poly.get((i + 1) % poly.size());
            double dp = a * p[0] + b * p[1] - c, dq = a * q[0] + b * q[1] - c;
            if (dp <= 0) out.add(p);
            if ((dp < 0 && dq > 0) || (dp > 0 && dq < 0)) {
                double t = dp / (dp - dq);
                out.add(new double[] { p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t });
            }
        }
        return out;
    }

    private static double[] flatten(List<double[]> poly) {
        double[] out = new double[poly.size() * 2];
        for (int i = 0; i < poly.size(); i++) { out[i * 2] = poly.get(i)[0]; out[i * 2 + 1] = poly.get(i)[1]; }
        return out;
    }

    // ── Circles in rectangles ─────────────────────────────────────────────────

    private static List<double[]> arcsInRect(double cu, double cv, double rho, double u0, double v0, double u1, double v1) {
        List<double[]> out = new ArrayList<>();
        if (rho < 1.0e-3) return out;
        List<Double> hits = new ArrayList<>(8);
        for (double u : new double[] { u0, u1 }) {
            double du = u - cu;
            if (Math.abs(du) > rho) continue;
            double s = Math.sqrt(rho * rho - du * du);
            for (double v : new double[] { cv + s, cv - s }) if (v >= v0 - 1.0e-9 && v <= v1 + 1.0e-9) hits.add(angle(v - cv, du));
        }
        for (double v : new double[] { v0, v1 }) {
            double dv = v - cv;
            if (Math.abs(dv) > rho) continue;
            double s = Math.sqrt(rho * rho - dv * dv);
            for (double u : new double[] { cu + s, cu - s }) if (u >= u0 - 1.0e-9 && u <= u1 + 1.0e-9) hits.add(angle(dv, u - cu));
        }
        if (hits.size() < 2) return out;
        hits.sort(Comparator.naturalOrder());
        hits.add(hits.get(0) + Math.PI * 2.0);

        double step = 2.0 * Math.acos(Math.max(0.0, 1.0 - SAGITTA / rho));
        for (int i = 0; i + 1 < hits.size(); i++) {
            double a = hits.get(i), b = hits.get(i + 1);
            if (b - a < 1.0e-9) continue;
            double mid = (a + b) / 2.0, mu = cu + rho * Math.cos(mid), mv = cv + rho * Math.sin(mid);
            if (mu <= u0 || mu >= u1 || mv <= v0 || mv >= v1) continue;   // this stretch of the circle is outside the rectangle
            int n = Math.max(1, (int) Math.ceil((b - a) / step));
            double[] run = new double[(n + 1) * 2];
            for (int k = 0; k <= n; k++) {
                double t = a + (b - a) * k / n;
                run[2 * k] = cu + rho * Math.cos(t);
                run[2 * k + 1] = cv + rho * Math.sin(t);
            }
            out.add(run);
        }
        return out;
    }

    private static double angle(double dv, double du) {
        double a = Math.atan2(dv, du);
        return a < 0 ? a + Math.PI * 2.0 : a;
    }

    // ── Vertices ──────────────────────────────────────────────────────────────

    private static void line(VertexConsumer vc, Matrix4f m, int[] rgb,
                             float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;
        vc.addVertex(m, x1, y1, z1).setColor(rgb[0], rgb[1], rgb[2], LINE_A).setNormal(nx, ny, nz).setLineWidth(LINE_WIDTH);
        vc.addVertex(m, x2, y2, z2).setColor(rgb[0], rgb[1], rgb[2], LINE_A).setNormal(nx, ny, nz).setLineWidth(LINE_WIDTH);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, int[] rgb, int a,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        corner(vc, m, rgb, a, x0, y0, z0, 1);
        corner(vc, m, rgb, a, x1, y1, z1, 1);
        corner(vc, m, rgb, a, x2, y2, z2, 1);
        corner(vc, m, rgb, a, x3, y3, z3, 1);
        corner(vc, m, rgb, a, x3, y3, z3, -1);
        corner(vc, m, rgb, a, x2, y2, z2, -1);
        corner(vc, m, rgb, a, x1, y1, z1, -1);
        corner(vc, m, rgb, a, x0, y0, z0, -1);
    }

    private static void corner(VertexConsumer vc, Matrix4f m, int[] rgb, int a, float x, float y, float z, float ny) {
        vc.addVertex(m, x, y, z).setColor(rgb[0], rgb[1], rgb[2], a).setNormal(0, ny, 0);
    }
}
