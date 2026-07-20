package com.phantomaddons.features.misckuudra;

import com.phantomaddons.PhantomConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class HollowWandAnnouncer {

    private static final String ICHOR_MSG    = "Casting Spell: Ichor Pool!";
    private static final String SPIRIT_MSG   = "Casting Spell: Spirit Spark!";
    private static final String RUSH_MSG     = "Casting Spell: Hollowed Rush!";
    private static final String WIND_MSG     = "Casting Spell: Raging Wind!";

    private static long lastSentMs  = 0;
    private static final long COOLDOWN_MS = 2000;

    private HollowWandAnnouncer() {}

    public static void onChat(String clean) {
        if (!PhantomConfig.isHollowWandEnabled()) return;
        if (!clean.contains("Casting Spell:")) return;

        String abbrev;
        if      (clean.contains(ICHOR_MSG))  abbrev = "Ichor Pool";
        else if (clean.contains(SPIRIT_MSG)) abbrev = "Spirit Spark";
        else if (clean.contains(RUSH_MSG))   abbrev = "Hollowed Rush";
        else if (clean.contains(WIND_MSG))   abbrev = "Raging Wind";
        else return;

        long now = System.currentTimeMillis();
        if (now - lastSentMs < COOLDOWN_MS) return;
        lastSentMs = now;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) return;

        Player p = mc.player;
        String pos = String.format("(%d, %d, %d)",
                (int) p.getX(), (int) p.getY(), (int) p.getZ());
        mc.getConnection().sendCommand("pc [Phantom] " + abbrev + " @ " + pos);
    }

    public static void onTitle(String text) {}
}
