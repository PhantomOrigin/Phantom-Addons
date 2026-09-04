package com.phantomaddons.data;

import com.phantomaddons.PhantomAddons;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.data.supply.GrabTracker;
import com.phantomaddons.data.supply.SupplyAttemptTracker;
import com.phantomaddons.data.tentacle.TentacleTracker;
import com.phantomaddons.data.tentacle.TentacleZoneMatcher;
import com.phantomaddons.phase.KuudraPhaseTracker;
import net.minecraft.client.Minecraft;

public final class GameplayDataLogger {

    private static boolean initialized = false;
    private static long sessionTick = 0L;

    private GameplayDataLogger() {}

    public static boolean isEnabled() { return PhantomConfig.isTentacleDbLoggingEnabled(); }

    public static long currentTick() { return sessionTick; }

    public static void tick(Minecraft client) {
        if (!isEnabled() || client.player == null || client.level == null) return;
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.SUPPLIES) return;

        if (!initialized) {
            GameplayDatabase.get();
            TentacleZoneMatcher.load();
            initialized = true;
        }

        sessionTick++;

        try {
            TentacleTracker.tick(client, sessionTick);
            GrabTracker.tick(client, sessionTick);
        } catch (RuntimeException e) {
            PhantomAddons.LOGGER.error("[GameplayDataLogger] tick failed", e);
        }
    }

    /** Called from {@code PhantomAddons.resetAll()} on disconnect/rejoin. */
    public static void onDisconnect() {
        RunRecorder.onRunAbandoned();
        TentacleTracker.reset();
        GrabTracker.reset();
        SupplyAttemptTracker.reset();
    }
}
