package com.kuudrahelper.utils;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.List;

public final class KuudraTierDetector {

    private static int     kuudraTier       = 0;
    private static int     tickCounter      = 0;
    private static boolean inKuudraHollow   = false;
    private static boolean inDungeonHub     = false;
    private static boolean inForgottenSkull = false;

    private KuudraTierDetector() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;
            if (++tickCounter < 20) return;
            tickCounter = 0;
            detectAll(client);
        });
    }

    private static void detectAll(Minecraft client) {
        inKuudraHollow   = false;
        inDungeonHub     = false;
        inForgottenSkull = false;
        kuudraTier       = 0;

        for (String line : getSidebarLines(client)) {
            String clean = line.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();

            if (clean.contains("Kuudra's Hollow") || clean.contains("Kuudra"))
                inKuudraHollow = true;
            if (clean.contains("Dungeon Hub"))     inDungeonHub     = true;
            if (clean.contains("Forgotten Skull")) inForgottenSkull = true;

            if (inKuudraHollow && kuudraTier == 0) {
                if      (clean.contains("(T1)")) kuudraTier = 1;
                else if (clean.contains("(T2)")) kuudraTier = 2;
                else if (clean.contains("(T3)")) kuudraTier = 3;
                else if (clean.contains("(T4)")) kuudraTier = 4;
                else if (clean.contains("(T5)")) kuudraTier = 5;
            }
        }
    }

    private static List<String> getSidebarLines(Minecraft client) {
        List<String> lines = new ArrayList<>();
        if (client.level == null) return lines;

        Scoreboard scoreboard = client.level.getScoreboard();
        Objective  sidebar    = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return lines;

        for (ScoreHolder holder : scoreboard.getTrackedPlayers()) {
            String     name = holder.getScoreboardName();
            PlayerTeam team = scoreboard.getPlayersTeam(name);
            lines.add(PlayerTeam.formatNameForTeam(team,
                    Component.literal(name)).getString());
        }
        return lines;
    }

    public static int     getTier()              { return kuudraTier; }
    public static boolean isInKuudraHollow()     { return inKuudraHollow; }
    public static boolean isInDungeonHub()        { return inDungeonHub; }
    public static boolean isInForgottenSkull()    { return inForgottenSkull; }

    public static void reset() { kuudraTier = 0; tickCounter = 0; }
}