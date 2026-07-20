package com.phantomaddons.features.dungeons;

import com.phantomaddons.PhantomConfig;
import net.minecraft.client.Minecraft;

/*
This feature is excluded from the standard version of the mod
 */
public final class DungeonsGfs {

    private static final String TOXIC_TRIGGER =
            "[BOSS] Wither King: We will decide it all, here, now.";
    private static final String TWILIGHT_TRIGGER =
            "[BOSS] Wither King: I no longer wish to fight, but I know that will not stop you.";

    private DungeonsGfs() {}

    public static void onChat(String raw) {
        if (!com.phantomaddons.Edition.CURRENT.fullFeatureSet) return; // not included in this edition
        String clean = raw.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();

        if (PhantomConfig.isAutoGfsToxicEnabled() && clean.equals(TOXIC_TRIGGER)) {
            runGfs("toxic_arrow_poison", PhantomConfig.getToxicAmount());
        } else if (PhantomConfig.isAutoGfsTwilightEnabled() && clean.equals(TWILIGHT_TRIGGER)) {
            runGfs("twilight_arrow_poison", PhantomConfig.getTwilightAmount());
        }
    }

    private static void runGfs(String item, int amount) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.connection.sendCommand("gfs " + item + " " + amount);
    }
}
