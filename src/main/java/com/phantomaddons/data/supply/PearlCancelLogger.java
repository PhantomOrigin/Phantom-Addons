package com.phantomaddons.data.supply;

import com.phantomaddons.data.GameplayDataLogger;
import com.phantomaddons.data.GameplayDatabase;
import com.phantomaddons.data.RunRecorder;
import net.minecraft.world.entity.player.Player;


public final class PearlCancelLogger {

    private static final float GROUND_PITCH_THRESHOLD = 45.0f;

    private static Long currentGrabId = null;
    private static int sequenceInGrab = 0;

    private PearlCancelLogger() {}

    static void onGrabStart(Long grabId) {
        currentGrabId = grabId;
        sequenceInGrab = 0;
    }

    static void onGrabEnd() {
        currentGrabId = null;
        sequenceInGrab = 0;
    }

    public static void onPearlThrow(Player player) {
        if (!GameplayDataLogger.isEnabled() || currentGrabId == null) return;
        if (player.getXRot() < GROUND_PITCH_THRESHOLD) return;

        Long runId = RunRecorder.getActiveRunId();
        if (runId == null) return;

        sequenceInGrab++;
        boolean isFirst = sequenceInGrab == 1;
        double x = player.getX(), y = player.getY(), z = player.getZ();
        float yaw = player.getYRot(), pitch = player.getXRot();

        GameplayDatabase.insert(
                "INSERT INTO pearl_throws (grab_id, run_id, tick, thrown_at, player_x, player_y, player_z, " +
                        "player_yaw, player_pitch, sequence_in_grab, is_first_in_grab) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                currentGrabId, runId, GameplayDataLogger.currentTick(), GameplayDatabase.now(),
                x, y, z, yaw, pitch, sequenceInGrab, isFirst ? 1 : 0);

        if (isFirst) {
            GameplayDatabase.update(
                    "UPDATE grab_events SET cancel_x = ?, cancel_y = ?, cancel_z = ?, " +
                            "cancel_block_x = ?, cancel_block_y = ?, cancel_block_z = ?, " +
                            "cancel_yaw = ?, cancel_pitch = ? WHERE grab_id = ?",
                    x, y, z, (long) Math.floor(x), (long) Math.floor(y), (long) Math.floor(z), yaw, pitch, currentGrabId);
        }
    }
}
