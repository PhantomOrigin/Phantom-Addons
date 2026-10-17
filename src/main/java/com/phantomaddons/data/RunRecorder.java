package com.phantomaddons.data;

import com.phantomaddons.data.tentacle.TentacleCubeTracker;

public final class RunRecorder {

    private RunRecorder() {}

    public static void onRunStart() {
        if (!GameplayDataLogger.isEnabled()) return;
        TentacleCubeTracker.reset();
        GameplayDataLogger.markRunStart();
    }

    public static void onRunEnd() { TentacleCubeTracker.reset(); }

    public static void onRunAbandoned() { TentacleCubeTracker.reset(); }
}
