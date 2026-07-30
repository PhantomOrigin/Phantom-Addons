package com.phantomaddons.features.misckuudra.chesttracking;

import com.phantomaddons.utils.TextUtil;
import com.phantomaddons.utils.KuudraTierDetector;
import com.phantomaddons.PhantomAddons;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TabListChestSync {

    private static final Pattern CHEST_PATTERN =
            Pattern.compile("Chests:[\\s\\u00a0]*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");

    private static final String CHEST_MARKER = "Chests";

    private static int tickCooldown = 0;
    private static final int CHECK_INTERVAL = 20;

    private TabListChestSync() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getConnection() == null) return;
            if (!KuudraTierDetector.isInDungeonHub()) return;
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

            String stripped = TextUtil.stripColor(entry.getTabListDisplayName().getString());
            if (!containsIgnoreCase(stripped, CHEST_MARKER)) continue;

            String line = WHITESPACE_RUN
                    .matcher(stripped.replace('\u00a0', ' ')) // normalize non-breaking spaces
                    .replaceAll(" ")                          // collapse multiple spaces
                    .trim();

            Matcher m = CHEST_PATTERN.matcher(line);
            if (!m.find()) continue;

            int tabTotal = Integer.parseInt(m.group(1));
            if (tabTotal != ChestTracker.getTotal()) {
                PhantomAddons.LOGGER.info(
                        "[TabListChestSync] Tab={} tracker={} — syncing",
                        tabTotal, ChestTracker.getTotal());
                ChestTracker.syncFromTabList(tabTotal);
            }
            return;
        }
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        int limit = haystack.length() - needle.length();
        for (int i = 0; i <= limit; i++) {
            if (haystack.regionMatches(true, i, needle, 0, needle.length())) return true;
        }
        return false;
    }
}
