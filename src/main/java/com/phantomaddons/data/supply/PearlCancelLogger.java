package com.phantomaddons.data.supply;

import com.phantomaddons.data.GameplayDataLogger;
import com.phantomaddons.data.tentacle.TentacleCubeTracker;
import net.minecraft.world.entity.player.Player;

public final class PearlCancelLogger {

    private PearlCancelLogger() {}

    public static void onPearlThrow(Player player) {
        if (!GameplayDataLogger.isEnabled()) return;
        TentacleCubeTracker.onPearlThrow(player);
    }
}
