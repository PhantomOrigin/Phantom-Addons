package com.kuudrahelper.features.misckuudra;

import com.kuudrahelper.KuudraHelperMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TabListChestSync {

    private static final Pattern CHEST_PATTERN =
            Pattern.compile("Chests:[\\s\\u00a0]*(\\d+)", Pattern.CASE_INSENSITIVE);

    private static int tickCooldown = 0;
    private static final int CHECK_INTERVAL = 20;

    private TabListChestSync() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getConnection() == null) return;
            if (tickCooldown > 0) { tickCooldown--; return; }
            tickCooldown = CHECK_INTERVAL;
            checkTabList(client);
        });
    }

    private static void checkTabList(Minecraft client) {
        Collection<PlayerInfo> entries = client.getConnection().getListedOnlinePlayers();
        if (entries == null || entries.isEmpty()) return;

        for (PlayerInfo entry : entries) {
            if (entry.getTabListDisplayName() == null) continue;

            String line = entry.getTabListDisplayName().getString()
                    .replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                    .replace('\u00a0', ' ')   // normalize non-breaking spaces
                    .replaceAll("\\s+", " ")  // collapse multiple spaces
                    .trim();

            Matcher m = CHEST_PATTERN.matcher(line);
            if (!m.find()) continue;

            int tabTotal = Integer.parseInt(m.group(1));
            if (tabTotal != ChestTracker.getTotal()) {
                KuudraHelperMod.LOGGER.info(
                        "[TabListChestSync] Tab={} tracker={} — syncing",
                        tabTotal, ChestTracker.getTotal());
                ChestTracker.syncFromTabList(tabTotal);
            }
            return;
        }
    }
}
