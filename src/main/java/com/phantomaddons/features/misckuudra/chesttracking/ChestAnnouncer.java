package com.phantomaddons.features.misckuudra.chesttracking;

import com.phantomaddons.PhantomConfig;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

public final class ChestAnnouncer {

    private static final long COOLDOWN_MS = 5_000L;
    private static long lastAnnounceMs = 0L;

    private ChestAnnouncer() {}

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!PhantomConfig.isChestAnnouncerEnabled()) return;

            String clean = message.getString()
                    .replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                    .trim();

            if (clean.contains("!chests")) handleChests();
            if (clean.contains("!pb"))     handlePb();
        });
    }

    private static void handleChests() {
        if (!canAnnounce()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        int total   = ChestTracker.getTotal();
        int success = ChestTracker.getSuccess();
        int fail    = ChestTracker.getFail();
        int left    = Math.max(0, 60 - total);

        String cmd = String.format(
                "pc Chests: %d/60 (%d:%d) | | %d runs left",
                total, success, fail, left);

        mc.execute(() -> mc.getConnection().sendCommand(cmd));
    }

    private static void handlePb() {
        if (!canAnnounce()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        int    tier = PhantomConfig.getHighestTierPlayed();
        double pb   = PhantomConfig.getTotalRunPb(tier);

        if (pb >= 9999) return;

        String cmd = "pc T" + tier + " Kuudra PB: " + PhantomConfig.formatTime(pb);
        mc.execute(() -> mc.getConnection().sendCommand(cmd));
    }

    private static boolean canAnnounce() {
        long now = System.currentTimeMillis();
        if (now - lastAnnounceMs < COOLDOWN_MS) return false;
        lastAnnounceMs = now;
        return true;
    }
}
