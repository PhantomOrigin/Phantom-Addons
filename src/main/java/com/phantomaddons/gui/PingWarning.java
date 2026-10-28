package com.phantomaddons.gui;

import com.phantomaddons.Edition;
import com.phantomaddons.PhantomConfig;

import java.util.function.BooleanSupplier;

public final class PingWarning {

    public static final String FEATURE_TEXT =
            "Set \"Ping (ms) - Lowest normal ping\" in the About tab. While it is 0 this feature may not work as intended.";
    public static final String SETTING_TEXT =
            "Set this to your lowest normal ping. Features you have turned on use it and may not work as intended while it is 0.";

    private PingWarning() {}

    public static boolean unset() { return PhantomConfig.getLowPing() <= 0; }

    public static boolean eatenTimerUsesPing() {
        return PhantomConfig.isEatenTimerEnabled() && PhantomConfig.isEatenTimerSubtractPingEnabled();
    }

    public static boolean anyFeatureOn() {
        return PhantomConfig.isRendDamageEnabled()
                || PhantomConfig.isRendTrackerEnabled()
                || PhantomConfig.isPickoblockEnabled()
                || (Edition.CURRENT.fullFeatureSet && PhantomConfig.isPearlRefillEnabled())
                || eatenTimerUsesPing()
                || PhantomConfig.isPearlWaypointsEnabled();
    }

    public static boolean active() { return unset() && anyFeatureOn(); }

    public static BooleanSupplier when(BooleanSupplier featureOn) {
        return () -> unset() && featureOn.getAsBoolean();
    }
}
