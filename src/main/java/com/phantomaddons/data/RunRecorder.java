package com.phantomaddons.data;

import com.phantomaddons.PhantomAddons;
import com.phantomaddons.data.supply.GrabTracker;
import com.phantomaddons.data.supply.SupplyAttemptTracker;
import com.phantomaddons.data.tentacle.TentacleTracker;
import com.phantomaddons.utils.KuudraTierDetector;
import net.fabricmc.loader.api.FabricLoader;

public final class RunRecorder {

    private static Long activeRunId = null;

    private RunRecorder() {}

    public static Long getActiveRunId() { return activeRunId; }

    public static void onRunStart() {
        if (!GameplayDataLogger.isEnabled()) return;
        if (activeRunId != null) onRunAbandoned();
        clearTrackerState();

        String modVersion = FabricLoader.getInstance()
                .getModContainer("phantomaddons")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        String mcVersion = FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");

        activeRunId = GameplayDatabase.insert(
                "INSERT INTO runs (started_at, mc_version, mod_version) VALUES (?, ?, ?)",
                GameplayDatabase.now(),
                mcVersion,
                modVersion);

        PhantomAddons.LOGGER.info("[RunRecorder] Run started: {}", activeRunId);
    }

    public static void onRunEnd() {
        closeActiveRun();
    }

    public static void onRunAbandoned() {
        closeActiveRun();
    }

    private static void closeActiveRun() {
        if (activeRunId == null) return;
        GameplayDatabase.update(
                "UPDATE runs SET ended_at = ?, tier = ? WHERE run_id = ?",
                GameplayDatabase.now(),
                KuudraTierDetector.getTier() > 0 ? KuudraTierDetector.getTier() : KuudraTierDetector.getLastKnownTier(),
                activeRunId);
        PhantomAddons.LOGGER.info("[RunRecorder] Run closed: {}", activeRunId);
        activeRunId = null;
        clearTrackerState();
    }

    private static void clearTrackerState() {
        TentacleTracker.reset();
        GrabTracker.reset();
        SupplyAttemptTracker.reset();
    }
}
