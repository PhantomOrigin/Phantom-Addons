package com.phantomaddons.features.stundps;

import com.phantomaddons.utils.TextUtil;
import com.phantomaddons.PhantomConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/*
This feature is excluded from the standard version of the mod
 */
public final class CannonAutoClose {

    private static final String TRIGGER    = "You purchased Human Cannonball!";
    private static final int    MAX_CLOSES = 2;
    private static final long   TIMEOUT_MS = 400L;

    private static volatile int  closesLeft = 0;
    private static volatile long windowEnd  = -1L;

    private CannonAutoClose() {}

    public static void register() {
        if (!com.phantomaddons.Edition.CURRENT.fullFeatureSet) return; // not included in this edition
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !PhantomConfig.isCannonAutoCloseEnabled()) return;
            String text = TextUtil.stripColor(message.getString());
            if (text.contains(TRIGGER)) arm();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!PhantomConfig.isCannonAutoCloseEnabled()) return;
            if (closesLeft <= 0) return;

            if (System.currentTimeMillis() > windowEnd) { closesLeft = 0; return; }

            if (!(client.screen instanceof AbstractContainerScreen)) return;
            if (client.screen instanceof InventoryScreen)  return;
            if (client.player != null && client.player.isUsingItem()) return;
            if (client.options.keyAttack.isDown()) return;

            client.player.closeContainer();
            closesLeft--;
        });
    }

    public static void reset() {
        closesLeft = 0;
        windowEnd  = -1L;
    }

    private static void arm() {
        closesLeft = MAX_CLOSES;
        windowEnd  = System.currentTimeMillis() + TIMEOUT_MS;
    }
}
