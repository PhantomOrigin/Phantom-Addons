package com.phantomaddons.features.supplies;

import com.phantomaddons.utils.TextUtil;
import com.phantomaddons.features.misckuudra.NotificationHud;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SupplyProgressHud {

    private static final double SPAWN_DELAY_S = 9.0;
    private static final Pattern SUPPLY_PATTERN = Pattern.compile("Rescue Supplies \\((\\d+)/(\\d+)\\)");

    private static volatile boolean active       = false;
    private static volatile long    phaseStartMs = 0;

    private SupplyProgressHud() {}

    public static long getPhaseStartMs() { return phaseStartMs; }

    public static void onSuppliesStart() {
        active       = true;
        phaseStartMs = System.currentTimeMillis();
    }

    public static void reset() {
        active       = false;
        phaseStartMs = 0;
        NotificationHud.clearCountdown();
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!active) return;
            long now     = System.currentTimeMillis();
            double elapsed = (now - phaseStartMs) / 1000.0;
            if (elapsed < SPAWN_DELAY_S) {
                double remaining = SPAWN_DELAY_S - elapsed;
                NotificationHud.setCountdown(String.format("§eSupplies Spawn: §f%.2fs", remaining));
            } else {
                NotificationHud.clearCountdown();
            }
        });
    }

    public static String readSidebarForSubstring(Minecraft mc, String substring) {
        if (mc.level == null) return null;
        Scoreboard sb = mc.level.getScoreboard();
        Objective sidebar = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return null;

        for (var entry : sb.listPlayerScores(sidebar)) {
            String owner = entry.owner();
            PlayerTeam team = sb.getPlayerTeam(owner);
            String raw;
            if (team != null) {
                raw = team.getPlayerPrefix().getString()
                        + owner
                        + team.getPlayerSuffix().getString();
            } else {
                raw = owner;
            }
            String clean = TextUtil.stripColor(raw).trim();
            if (clean.contains(substring)) return clean;
        }
        return null;
    }
}
