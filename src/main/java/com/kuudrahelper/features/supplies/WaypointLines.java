package com.kuudrahelper.features.supplies;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Set;

public final class WaypointLines {

    private static final double SUPPLY_RANGE = 12.0;


    private static final Set<PickupLocation> SECOND_SUPPLY_LOCATIONS =
            EnumSet.of(PickupLocation.SHOP, PickupLocation.SQUARE, PickupLocation.X_CANNON);

    private static volatile int pearlsThrownThisPickup = 0;

    private WaypointLines() {}

    public static void resetPearlThrowCount() {
        pearlsThrownThisPickup = 0;
    }

    public static void onPearlThrown() {
        if (PearlWaypointManager.isTrackingPickup()) {
            pearlsThrownThisPickup++;
        }
    }

    public static Vec3 getTarget(Player player) {
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.SUPPLIES) return null;

        boolean picking = PearlWaypointManager.isTrackingPickup();

        if (!picking && KuudraConfig.isWaypointLinesSuppliesEnabled()) {
            Vec3 supplyTarget = getSupplyTarget(player.position());
            if (supplyTarget != null) return supplyTarget;
        }

        if (picking && KuudraConfig.isWaypointLinesFlatPearlsEnabled()) {
            Vec3 spawn = new Vec3(player.getX(), player.getY() + 1.6, player.getZ());
            return getPearlTarget(player.position(), spawn);
        }

        return null;
    }


    private static Vec3 getSupplyTarget(Vec3 playerPos) {
        SupplyCluster only = null;
        for (SupplyCluster cluster : SupplyWaypointTracker.detectedClusters) {
            if (cluster.center.distanceTo(playerPos) > SUPPLY_RANGE) continue;
            if (only != null) return null; // more than one candidate — ambiguous
            only = cluster;
        }
        return only != null ? only.center : null;
    }

    private static Vec3 getPearlTarget(Vec3 playerPos, Vec3 spawn) {
        PickupLocation loc = PickupLocation.closest(playerPos);
        boolean flatUsable = loc.pearlTarget != null; // e.g. Square has no flat pearl

        boolean atPreLocation = !SECOND_SUPPLY_LOCATIONS.contains(loc);

        boolean doubleAllowed = atPreLocation && KuudraConfig.isPearlDoubleEnabled();
        boolean etherAllowed  = atPreLocation && KuudraConfig.isEtherwarpWaypointsEnabled();
        KuudraConfig.SecondSupplyPreference pref = KuudraConfig.getSecondSupplyPreference();

        PearlWaypointState doubleState = null;
        if (doubleAllowed && pref == KuudraConfig.SecondSupplyPreference.DOUBLE_PEARL) {
            for (PearlWaypointState s : PearlWaypointManager.getSnapshot()) {
                if (s.isDouble() && s.isMyTarget()) { doubleState = s; break; }
            }
        }
        boolean doubleUsable = doubleState != null;

        PearlWaypointState flatState = null;
        if (flatUsable) {
            for (PearlWaypointState s : PearlWaypointManager.getSnapshot()) {
                if (!s.isDouble() && s.isMyTarget() && s.target() == loc.pearlTarget) { flatState = s; break; }
            }
        }

        int thrown = pearlsThrownThisPickup;
        int slot = 0;

        if (doubleUsable) {
            if (thrown == slot) {
                return doubleState.centerAimDir() != null ? aimWaypoint(spawn, doubleState.centerAimDir()) : null;
            }
            slot++;
        }
        if (flatUsable) {
            if (thrown == slot) {
                if (flatState != null && flatState.centerAimDir() != null) {
                    return aimWaypoint(spawn, flatState.centerAimDir());
                }
                return loc.pearlTarget.landingPos.add(0, 3.0, 0); // fallback if a trajectory couldn't be solved this frame
            }
            slot++;
        }
        if (etherAllowed) {
            Vec3 etherTarget = EtherwarpWaypointManager.getPriorityTargetPos(playerPos);
            if (etherTarget != null) return etherTarget;
        }
        return null;
    }

    private static Vec3 aimWaypoint(Vec3 spawn, Vec3 aimDir) {
        return spawn.add(aimDir.scale(50.0));
    }
}
