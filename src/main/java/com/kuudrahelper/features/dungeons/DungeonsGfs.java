package com.kuudrahelper.features.dungeons;

import com.kuudrahelper.KuudraConfig;
import net.minecraft.client.Minecraft;

public final class DungeonsGfs {

    private static final String TOXIC_TRIGGER =
            "[BOSS] Wither King: We will decide it all, here, now.";
    private static final String TWILIGHT_TRIGGER =
            "[BOSS] Wither King: I no longer wish to fight, but I know that will not stop you.";

    private DungeonsGfs() {}

    public static void onChat(String raw) {
        String clean = raw.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();

        if (KuudraConfig.isAutoGfsToxicEnabled() && clean.equals(TOXIC_TRIGGER)) {
            runGfs("toxic_arrow_poison", KuudraConfig.getToxicAmount());
        } else if (KuudraConfig.isAutoGfsTwilightEnabled() && clean.equals(TWILIGHT_TRIGGER)) {
            runGfs("twilight_arrow_poison", KuudraConfig.getTwilightAmount());
        }
    }

    private static void runGfs(String item, int amount) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.connection.sendCommand("gfs " + item + " " + amount);
    }
}
