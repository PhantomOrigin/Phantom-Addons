package com.phantomaddons.data;

import com.phantomaddons.PhantomAddons;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.data.supply.SupplyAttemptTracker;
import com.phantomaddons.data.tentacle.TentacleCubeTracker;
import com.phantomaddons.phase.KuudraPhaseTracker;
import net.minecraft.client.Minecraft;

public final class GameplayDataLogger {

    private static long sessionTick = 0L;
    private static long runStartTick = 0L;

    private GameplayDataLogger() {}

    public static boolean isEnabled() {
        return SupplyShare.isActive() || PhantomConfig.isTentacleHighlightEnabled() || PhantomConfig.isTentacleSpotBeaconsEnabled();
    }

    public static long currentTick() { return sessionTick; }

    public static long runTick() { return sessionTick - runStartTick; }

    static void markRunStart() { runStartTick = sessionTick; }

    public static void tick(Minecraft client) {
        SupplyShare.tick(client);

        if (!isEnabled() || client.player == null || client.level == null) return;
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.SUPPLIES) return;

        sessionTick++;

        try {
            TentacleCubeTracker.tick(client, runTick());
        } catch (RuntimeException e) {
            PhantomAddons.LOGGER.error("[GameplayDataLogger] tick failed", e);
        }
    }

    public static void onDisconnect() {
        RunRecorder.onRunAbandoned();
        TentacleCubeTracker.reset();
        SupplyAttemptTracker.reset();
    }
}
