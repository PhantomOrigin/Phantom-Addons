package com.kuudrahelper.features.supplies;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.KuudraHelperMod;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import com.kuudrahelper.phase.KuudraPhaseTracker.Phase;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PearlWaypointManager {

    public  static final double WAYPOINT_DIST       = 50.0;
    private static final double CORNER_OFFSET       = 0.9;
    private static final double RECOMPUTE_THRESHOLD = 0.0025000000000000005;

    private static volatile List<PearlWaypointState> snapshot       = Collections.emptyList();
    private static volatile boolean                  trackingPickup = false;
    private static volatile long                     pickupStartMs  = -1L;
    private static          Vec3                     lastPearlSpawn = null;

    private static final Map<PickupLocation, Vec3> doublePearlLandings = new EnumMap<>(PickupLocation.class);

    private PearlWaypointManager() {}

    public static List<PearlWaypointState> getSnapshot()      { return snapshot; }
    public static boolean                  isTrackingPickup() { return trackingPickup; }

    public static void onPickupStart(int percent) {
        if (!trackingPickup) {
            trackingPickup = true;
            pickupStartMs  = System.currentTimeMillis();
            WaypointLines.resetPearlThrowCount();
            KuudraHelperMod.LOGGER.info("[PearlWaypoint] Pickup started ({}%)", percent);
        }
    }

    public static void onPickupEnd() {
        trackingPickup = false;
        pickupStartMs  = -1L;
        KuudraHelperMod.LOGGER.info("[PearlWaypoint] Pickup ended");
    }

    public static void reset() {
        snapshot       = Collections.emptyList();
        trackingPickup = false;
        pickupStartMs  = -1L;
        lastPearlSpawn = null;
        NoPre.reset();
        SupplyTracker.reset();
        DoublePearlCoords.reset();
        doublePearlLandings.clear();
    }

    public static void tickUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (KuudraPhaseTracker.getPhase() != Phase.SUPPLIES) {
            snapshot = Collections.emptyList();
            return;
        }
        SupplyTracker.tick();
        Vec3 spawn     = pearlSpawnPos(mc);
        lastPearlSpawn = spawn;
        rebuildSnapshot(spawn, mc.player.getEyePosition());
    }

    public static void frameUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (KuudraPhaseTracker.getPhase() != Phase.SUPPLIES) return;
        Vec3 spawn = pearlSpawnPos(mc);
        if (lastPearlSpawn != null
                && lastPearlSpawn.distanceToSqr(spawn) < RECOMPUTE_THRESHOLD) {
            refreshTimers();
        } else {
            lastPearlSpawn = spawn;
            rebuildSnapshot(spawn, mc.player.getEyePosition());
        }
    }

    public static long computeThrowForFlight(long flightMs) {
        return computeThrowIn(flightMs);
    }

    public static Vec3 pearlSpawnPos(Minecraft mc) {
        return new Vec3(mc.player.getX(), mc.player.getY() + 1.6, mc.player.getZ());
    }

    private static void rebuildSnapshot(Vec3 spawn, Vec3 eye) {
        PickupLocation myPickup   = PickupLocation.closest(eye);
        PearlLocation  myTarget   = resolveMyTarget(myPickup);
        boolean        hasMyTarget = myTarget != null && !SupplyTracker.isCompleted(myTarget);

        List<PearlWaypointState> next = new ArrayList<>();

        for (PearlLocation loc : PearlLocation.values()) {
            if (SupplyTracker.isCompleted(loc)) continue;
            boolean isMyTarget = (loc == myTarget);
            if (isMyTarget || KuudraConfig.isShowAllWaypoints() || !hasMyTarget)
                addLocationEntries(loc, spawn, isMyTarget, false, next);
        }

        if (KuudraConfig.isPearlDoubleEnabled()) {
            rebuildDoublePearls(myPickup, spawn, next);
        }

        snapshot = Collections.unmodifiableList(next);
    }

    private static void addLocationEntries(PearlLocation loc, Vec3 spawn,
                                           boolean isMyTarget, boolean isDouble,
                                           List<PearlWaypointState> out) {
        TrajectorySolver.SolveResult flatResult = null;
        int sizeBefore = out.size();

        if (KuudraConfig.isPearlFlatEnabled()) {
            flatResult = TrajectorySolver.solveFlat(spawn, loc.targetPos);
            if (flatResult == null && KuudraConfig.isPearlSkyEnabled()) {
                TrajectorySolver.SolveResult fallback =
                        TrajectorySolver.solveSky(spawn, loc.targetPos);
                if (fallback != null) out.add(buildState(loc, spawn, fallback, false, isMyTarget));
            } else if (flatResult != null) {
                out.add(buildState(loc, spawn, flatResult, false, isMyTarget));
            }
        }

        if (KuudraConfig.isPearlSkyEnabled()) {
            TrajectorySolver.SolveResult skyResult =
                    TrajectorySolver.solveSky(spawn, loc.targetPos);
            if (skyResult != null && skyResult.isSky()
                    && !(flatResult == null && KuudraConfig.isPearlFlatEnabled())) {
                out.add(buildState(loc, spawn, skyResult, false, isMyTarget));
            }
        }

        if (out.size() == sizeBefore) {
            Vec3 fallDir = directionalFallbackAim(spawn, loc.targetPos);
            if (fallDir != null) {
                long flightMs = TrajectorySolver.estimateFlightMs(spawn, fallDir, loc.targetPos);
                long throwIn  = computeThrowIn(flightMs > 0 ? flightMs : 0);
                out.add(new PearlWaypointState(loc, fallDir, new Vec3[4],
                        false, false, isMyTarget, flightMs, throwIn));
            }
        }
    }

    private static Vec3 directionalFallbackAim(Vec3 spawn, Vec3 target) {
        double dx    = target.x - spawn.x;
        double dz    = target.z - spawn.z;
        double horiz = Math.hypot(dx, dz);
        if (horiz < 0.001) return null;
        double cos45 = Math.sqrt(0.5);
        return new Vec3(cos45 * dx / horiz, cos45, cos45 * dz / horiz);
    }

    private static PearlWaypointState buildState(PearlLocation loc, Vec3 spawn,
                                                 TrajectorySolver.SolveResult result,
                                                 boolean isDouble, boolean isMyTarget) {
        Vec3   centre  = loc.targetPos;
        Vec3[] corners = {
                centre.add(-CORNER_OFFSET, 0, -CORNER_OFFSET),
                centre.add( CORNER_OFFSET, 0, -CORNER_OFFSET),
                centre.add( CORNER_OFFSET, 0,  CORNER_OFFSET),
                centre.add(-CORNER_OFFSET, 0,  CORNER_OFFSET),
        };
        Vec3[] cornerAimDirs = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            TrajectorySolver.SolveResult r = result.isSky()
                    ? TrajectorySolver.solveSky (spawn, corners[i])
                    : TrajectorySolver.solveFlat(spawn, corners[i]);
            if (r != null) cornerAimDirs[i] = r.aimDir();
        }
        long flight  = isDouble
                ? result.flightMs() + Math.round(KuudraConfig.getDoublePearlDelayS() * 1000L)
                : result.flightMs();
        long throwIn = computeThrowIn(flight);
        return new PearlWaypointState(loc, result.aimDir(), cornerAimDirs,
                result.isSky(), isDouble, isMyTarget, flight, throwIn);
    }

    private static void rebuildDoublePearls(PickupLocation pickup, Vec3 spawn,
                                            List<PearlWaypointState> out) {
        if (pickup == PickupLocation.SLASH) {
            addDoublePearlToPosition(
                    PickupLocation.SQUARE,
                    findDisplayLocForPickup(PickupLocation.SQUARE),
                    spawn, out, true);

            if (!SupplyTracker.isCompleted(PearlLocation.X_CANNON)) {
                addDoublePearlToPosition(
                        PickupLocation.X_CANNON,
                        PearlLocation.X_CANNON,
                        spawn, out, false);
            }
        } else if (pickup == PickupLocation.EQUALS) {
            if (!SupplyTracker.isCompleted(PearlLocation.SHOP)) {
                addDoublePearlToPosition(
                        PickupLocation.SHOP,
                        PearlLocation.SHOP,
                        spawn, out, true);
            }
        }
    }

    private static PearlLocation findDisplayLocForPickup(PickupLocation pickup) {
        Vec3          pos      = pickup.position;
        PearlLocation best     = PearlLocation.values()[0];
        double        bestDist = Double.MAX_VALUE;
        for (PearlLocation loc : PearlLocation.values()) {
            double d = loc.landingPos.distanceToSqr(pos);
            if (d < bestDist) { bestDist = d; best = loc; }
        }
        return best;
    }

    private static void addDoublePearlToPosition(PickupLocation targetPickup,
                                                 PearlLocation  displayLoc,
                                                 Vec3 spawn,
                                                 List<PearlWaypointState> out,
                                                 boolean isHighlighted) {
        Vec3 target = doublePearlLandings.computeIfAbsent(targetPickup,
                p -> DoublePearlCoords.findValidLandingNear(p.position));

        TrajectorySolver.SolveResult r = TrajectorySolver.solveSky(spawn, target);
        boolean isSky = true;
        if (r == null) {
            r = TrajectorySolver.solveFlat(spawn, target);
            isSky = false;
        }

        long delayMs = Math.round(KuudraConfig.getDoublePearlDelayS() * 1000L);

        if (r == null) {
            Vec3 fallDir = directionalFallbackAim(spawn, target);
            if (fallDir == null) return;
            long flightMs = TrajectorySolver.estimateFlightMs(spawn, fallDir, target);
            long flight   = (flightMs > 0 ? flightMs : 0) + delayMs;
            long throwIn  = computeThrowIn(flight);
            out.add(new PearlWaypointState(displayLoc, fallDir, new Vec3[4],
                    false, true, isHighlighted, flight, throwIn));
            return;
        }

        Vec3[] corners = {
                target.add(-CORNER_OFFSET, 0, -CORNER_OFFSET),
                target.add( CORNER_OFFSET, 0, -CORNER_OFFSET),
                target.add( CORNER_OFFSET, 0,  CORNER_OFFSET),
                target.add(-CORNER_OFFSET, 0,  CORNER_OFFSET),
        };
        Vec3[] cornerAimDirs = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            TrajectorySolver.SolveResult cr = isSky
                    ? TrajectorySolver.solveSky (spawn, corners[i])
                    : TrajectorySolver.solveFlat(spawn, corners[i]);
            if (cr != null) cornerAimDirs[i] = cr.aimDir();
        }

        long flight  = r.flightMs() + delayMs;
        long throwIn = computeThrowIn(flight);
        out.add(new PearlWaypointState(displayLoc, r.aimDir(), cornerAimDirs,
                isSky, true, isHighlighted, flight, throwIn));
    }

    private static void refreshTimers() {
        List<PearlWaypointState> cur = snapshot;
        if (cur.isEmpty()) return;
        List<PearlWaypointState> updated = new ArrayList<>(cur.size());
        for (PearlWaypointState s : cur) {
            long throwIn = computeThrowIn(s.optimalFlightMs());
            updated.add(new PearlWaypointState(
                    s.target(), s.centerAimDir(), s.cornerAimDirs(),
                    s.isSky(), s.isDouble(), s.isMyTarget(),
                    s.optimalFlightMs(), throwIn));
        }
        snapshot = Collections.unmodifiableList(updated);
    }

    private static long computeThrowIn(long flightMs) {
        if (flightMs < 0L) return Long.MAX_VALUE;
        long pickupDuration = KuudraConfig.getPickupDurationMs();
        long ping           = KuudraConfig.getLowPing();
        if (trackingPickup && pickupStartMs >= 0L) {
            return pickupStartMs + pickupDuration - System.currentTimeMillis() - flightMs - ping;
        }
        return Math.min(pickupDuration - flightMs - ping, Long.MAX_VALUE - 1);
    }

    private static PearlLocation resolveMyTarget(PickupLocation pickup) {
        return pickup == PickupLocation.SQUARE
                ? NoPre.getMissingLocation()
                : pickup.pearlTarget;
    }

    private static PearlLocation closestPearlLocation(Vec3 pos) {
        PearlLocation best     = null;
        double        bestDist = Double.MAX_VALUE;
        for (PearlLocation loc : PearlLocation.values()) {
            double d = loc.landingPos.distanceToSqr(pos);
            if (d < bestDist) { bestDist = d; best = loc; }
        }
        return best;
    }
}