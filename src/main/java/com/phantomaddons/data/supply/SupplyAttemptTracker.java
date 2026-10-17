package com.phantomaddons.data.supply;

import com.phantomaddons.data.GameplayDataLogger;
import com.phantomaddons.data.tentacle.TentacleCubeTracker;

public final class SupplyAttemptTracker {

    private SupplyAttemptTracker() {}

    public static void reset() {
        TentacleCubeTracker.onPickupEnd(-1);
    }

    public static void onPickupStart(int startPercent) {
        if (!GameplayDataLogger.isEnabled()) return;
        TentacleCubeTracker.onPickupStart(startPercent);
    }

    public static void onPickupPercent(int percent) {
        if (!GameplayDataLogger.isEnabled()) return;
        TentacleCubeTracker.onPickupPercent(percent);
    }

    public static void onSupplyDropped() {
        if (!GameplayDataLogger.isEnabled()) return;
        TentacleCubeTracker.onSupplyDropped();
    }

    public static void onPickupEnd(int finalPercent) {
        if (!GameplayDataLogger.isEnabled()) return;
        TentacleCubeTracker.onPickupEnd(finalPercent);
    }
}
