package com.kuudrahelper.features.pearls;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TrajectorySolver {

    private static final double GRAVITY       = 0.03;
    private static final double DRAG          = 0.99;
    private static final double SPEED         = 1.5;
    private static final int    MAX_TICKS     = 120;
    private static final int    ITER          = 20;
    private static final double MIN_THETA     = Math.toRadians(0.5);
    private static final double MAX_THETA     = Math.toRadians(89.5);
    private static final double EPS           = Math.toRadians(0.05);
    private static final double LOG_DRAG      = Math.log(0.99);
    private static final double INV_OMD       = 99.99999999999991;
    private static final double SKY_THRESHOLD = Math.toRadians(41.0);
    private static final double[] DRAG_POW    = new double[121];

    static {
        DRAG_POW[0] = 1.0;
        for (int i = 1; i <= 120; i++) DRAG_POW[i] = DRAG_POW[i - 1] * 0.99;
    }

    private TrajectorySolver() {}

    public static SolveResult solveFlat(Vec3 start, Vec3 target) {
        return solveInternal(start, target, false);
    }

    public static SolveResult solveSky(Vec3 start, Vec3 target) {
        return solveInternal(start, target, true);
    }

    public static SolveResult solveClosest(Vec3 start, Vec3 target) {
        double dx = target.x - start.x, dz = target.z - start.z;
        double R  = Math.hypot(dx, dz);
        if (R < 0.5) return null;
        double ux = dx / R, uz = dz / R;

        double c     = R * 0.010000000000000009 / SPEED;
        double theta;
        if (c >= 1.0) {
            theta = MIN_THETA;
        } else {
            theta = Math.max(Math.acos(c) - EPS, MIN_THETA);
        }

        double cos = Math.cos(theta), sin = Math.sin(theta);
        double vx  = SPEED * cos * ux, vy = SPEED * sin, vz = SPEED * cos * uz;
        double len = Math.sqrt(vx * vx + vy * vy + vz * vz);
        Vec3 aimDir = new Vec3(vx / len, vy / len, vz / len);
        return new SolveResult(aimDir, -1L, theta >= SKY_THRESHOLD);
    }

    public static boolean pathIntersects(Vec3 spawn, Vec3 aimDir, AABB box, int maxTicks) {
        double vx = SPEED * aimDir.x;
        double vy = SPEED * aimDir.y;
        double vz = SPEED * aimDir.z;
        double x = spawn.x, y = spawn.y, z = spawn.z;
        for (int i = 0; i < maxTicks; i++) {
            vy -= GRAVITY;
            vx *= DRAG; vy *= DRAG; vz *= DRAG;
            x += vx; y += vy; z += vz;
            if (box.contains(x, y, z)) return true;
        }
        return false;
    }

    public static double predictBouncedY(double y, double v, double topBound, double bottomBound, int ticksAhead) {
        for (int i = 0; i < ticksAhead; i++) {
            y += v;
            if (!Double.isNaN(topBound) && y > topBound) {
                y = topBound - (y - topBound);
                v = -Math.abs(v);
            } else if (!Double.isNaN(bottomBound) && y < bottomBound) {
                y = bottomBound + (bottomBound - y);
                v = Math.abs(v);
            }
        }
        return y;
    }

    public static boolean pathIntersectsPredictedBox(Vec3 spawn, Vec3 aimDir, AABB box, double boxBaseY,
                                                      java.util.function.IntToDoubleFunction predictedY,
                                                      int startDelayTicks, int maxTicks) {
        double vx = SPEED * aimDir.x;
        double vy = SPEED * aimDir.y;
        double vz = SPEED * aimDir.z;
        double x = spawn.x, y = spawn.y, z = spawn.z;
        for (int i = 0; i < maxTicks; i++) {
            vy -= GRAVITY;
            vx *= DRAG; vy *= DRAG; vz *= DRAG;
            x += vx; y += vy; z += vz;
            int globalTick = startDelayTicks + i + 1;
            AABB shifted = box.move(0.0, predictedY.applyAsDouble(globalTick) - boxBaseY, 0.0);
            if (shifted.contains(x, y, z)) return true;
        }
        return false;
    }

    public static long estimateFlightMs(Vec3 spawn, Vec3 aimDir, Vec3 target) {
        double dx   = target.x - spawn.x;
        double dz   = target.z - spawn.z;
        double R    = Math.sqrt(dx * dx + dz * dz);
        double aimH = Math.sqrt(aimDir.x * aimDir.x + aimDir.z * aimDir.z);
        if (aimH < 1e-6) return -1L;
        double nReal = horizTicksContinuous(R, SPEED * aimH);
        return (!Double.isNaN(nReal) && nReal > 0) ? Math.round(nReal * 50.0) : -1L;
    }

    private static SolveResult solveInternal(Vec3 start, Vec3 target, boolean highArc) {
        double dx = target.x - start.x;
        double dz = target.z - start.z;
        double dy = target.y - start.y;
        double R  = Math.hypot(dx, dz);
        if (R < 0.5) return null;

        double ux = dx / R, uz = dz / R;
        double c  = R * 0.010000000000000009 / SPEED;
        if (c >= 1.0) return null;

        double thetaCap = (c <= 0) ? MAX_THETA : Math.acos(c);
        double maxTheta = Math.min(MAX_THETA, thetaCap - EPS);
        if (maxTheta <= MIN_THETA) return null;

        Double theta = highArc
                ? scanFromHigh(R, dy, MIN_THETA, maxTheta)
                : scanFromLow (R, dy, MIN_THETA, maxTheta);
        if (theta == null) return null;

        theta = refine(theta, R, dy, MIN_THETA, maxTheta);
        if (theta == null) return null;
        if (Math.abs(vertErr(theta, R, dy)) > 1.0) return null;

        double cos = Math.cos(theta), sin = Math.sin(theta);
        double vx  = SPEED * cos * ux;
        double vy  = SPEED * sin;
        double vz  = SPEED * cos * uz;
        double len = Math.sqrt(vx * vx + vy * vy + vz * vz);
        Vec3 aimDir = new Vec3(vx / len, vy / len, vz / len);

        double nReal   = horizTicksContinuous(R, SPEED * cos);
        long flightMs  = (!Double.isNaN(nReal) && nReal > 0) ? Math.round(nReal * 50.0) : -1L;
        boolean isSky  = theta >= SKY_THRESHOLD;
        return new SolveResult(aimDir, flightMs, isSky);
    }

    private static Double scanFromLow(double R, double dy, double lo, double hi) {
        int STEPS = 120;
        double step       = (hi - lo) / 120.0;
        double fPrev      = vertErr(lo, R, dy);
        double bestAbs    = Double.isNaN(fPrev) ? Double.MAX_VALUE : Math.abs(fPrev);
        double bestTheta  = lo;
        for (int i = 1; i <= STEPS; i++) {
            double theta = lo + i * step;
            double fCur  = vertErr(theta, R, dy);
            if (!Double.isNaN(fPrev) && !Double.isNaN(fCur) && fPrev * fCur <= 0)
                return refineInBracket(theta - step, theta, R, dy);
            if (!Double.isNaN(fCur) && Math.abs(fCur) < bestAbs) {
                bestAbs = Math.abs(fCur); bestTheta = theta;
            }
            fPrev = fCur;
        }
        return bestAbs <= 1.5 ? bestTheta : null;
    }

    private static Double scanFromHigh(double R, double dy, double lo, double hi) {
        int STEPS = 120;
        double step       = (hi - lo) / 120.0;
        double fPrev      = vertErr(hi, R, dy);
        double bestAbs    = Double.isNaN(fPrev) ? Double.MAX_VALUE : Math.abs(fPrev);
        double bestTheta  = hi;
        for (int i = 1; i <= STEPS; i++) {
            double theta = hi - i * step;
            double fCur  = vertErr(theta, R, dy);
            if (!Double.isNaN(fPrev) && !Double.isNaN(fCur) && fPrev * fCur <= 0)
                return refineInBracket(theta, theta + step, R, dy);
            if (!Double.isNaN(fCur) && Math.abs(fCur) < bestAbs) {
                bestAbs = Math.abs(fCur); bestTheta = theta;
            }
            fPrev = fCur;
        }
        return bestAbs <= 1.5 ? bestTheta : null;
    }

    private static Double refineInBracket(double lo, double hi, double R, double dy) {
        double fLo = vertErr(lo, R, dy);
        for (int j = 0; j < 20; j++) {
            double mid = (lo + hi) / 2.0;
            double fm  = vertErr(mid, R, dy);
            if (Double.isNaN(fm)) break;
            if (Math.abs(hi - lo) < 1e-6) return mid;
            if (fLo * fm <= 0) { hi = mid; }
            else               { lo = mid; fLo = fm; }
        }
        return (lo + hi) / 2.0;
    }

    private static Double refine(double theta, double R, double dy, double lo, double hi) {
        double step = Math.toRadians(0.6);
        for (int round = 0; round < 6; round++) {
            double tL = clamp(theta - step, lo, hi);
            double tR = clamp(theta + step, lo, hi);
            double eC = Math.abs(vertErr(theta, R, dy));
            double eL = Math.abs(vertErr(tL,    R, dy));
            double eR = Math.abs(vertErr(tR,    R, dy));
            if      (eL < eC) theta = tL;
            else if (eR < eC) theta = tR;
            step *= 0.5;
        }
        return theta;
    }

    private static double vertErr(double theta, double R, double dy) {
        double cos = Math.cos(theta);
        if (cos < 1e-9) return Double.NaN;
        double nReal = horizTicksContinuous(R, SPEED * cos);
        if (Double.isNaN(nReal) || nReal <= 0) return Double.NaN;
        return vertDispContinuous(theta, nReal) - dy;
    }

    private static double horizTicksContinuous(double R, double vH) {
        double x = R * 0.010000000000000009 / vH;
        if (x <= 0 || x >= 1) return Double.NaN;
        return Math.log1p(-x) / LOG_DRAG;
    }

    private static double vertDispContinuous(double theta, double nReal) {
        double dn  = Math.exp(nReal * LOG_DRAG);
        double omd = 1.0 - dn;
        return SPEED * Math.sin(theta) * omd * INV_OMD
                - GRAVITY * (nReal - omd * INV_OMD) * INV_OMD;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public record SolveResult(Vec3 aimDir, long flightMs, boolean isSky) {}
}