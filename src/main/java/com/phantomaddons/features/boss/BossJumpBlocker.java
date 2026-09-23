package com.phantomaddons.features.boss;

import com.phantomaddons.PhantomConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class BossJumpBlocker {

    private static final long BLOCK_DURATION_MS = 5_000L;

    private static long bossPhaseStartMs = -1L;

    private BossJumpBlocker() {}

    public static void onBossPhaseStart() {
        bossPhaseStartMs = System.currentTimeMillis();
    }

    public static void reset() {
        bossPhaseStartMs = -1L;
    }

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!PhantomConfig.isBlockBossPhaseJumpEnabled()) return;
            if (bossPhaseStartMs < 0) return;
            if (client.player == null || client.options == null) return;
            if (System.currentTimeMillis() - bossPhaseStartMs >= BLOCK_DURATION_MS) return;

            client.options.keyJump.setDown(false);
        });
    }
}
