package com.phantomaddons.data.supply;

import com.phantomaddons.data.GameplayDataLogger;
import com.phantomaddons.data.GameplayDatabase;
import com.phantomaddons.data.RunRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class SupplyAttemptTracker {

    private static Long activeAttemptId = null;

    private SupplyAttemptTracker() {}

    public static Long getActiveAttemptId() { return activeAttemptId; }

    public static void reset() {
        if (activeAttemptId == null) return;
        long attemptId = activeAttemptId;
        GameplayDatabase.update(
                "UPDATE supply_attempts SET end_tick = ?, ended_at = ?, outcome = 'abandoned' WHERE attempt_id = ?",
                GameplayDataLogger.currentTick(), GameplayDatabase.now(), attemptId);
        GrabTracker.onAttemptEnded(attemptId, "abandoned");
        activeAttemptId = null;
    }

    public static void onPickupStart(int startPercent) {
        Long runId = RunRecorder.getActiveRunId();
        if (!GameplayDataLogger.isEnabled() || runId == null) return;
        if (activeAttemptId != null) onPickupEnd(-1); // stale attempt never got an end callback; close it out

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        activeAttemptId = GameplayDatabase.insert(
                "INSERT INTO supply_attempts (run_id, start_tick, started_at, start_x, start_y, start_z) VALUES (?, ?, ?, ?, ?, ?)",
                runId, GameplayDataLogger.currentTick(), GameplayDatabase.now(),
                player.getX(), player.getY(), player.getZ());
    }

    public static void onPickupEnd(int finalPercent) {
        if (activeAttemptId == null) return;
        long attemptId = activeAttemptId;
        String outcome = finalPercent >= 100 ? "success" : "failure";

        GameplayDatabase.update(
                "UPDATE supply_attempts SET end_tick = ?, ended_at = ?, outcome = ?, final_percent = ? WHERE attempt_id = ?",
                GameplayDataLogger.currentTick(), GameplayDatabase.now(), outcome, finalPercent, attemptId);

        GrabTracker.onAttemptEnded(attemptId, outcome);
        activeAttemptId = null;
    }
}
