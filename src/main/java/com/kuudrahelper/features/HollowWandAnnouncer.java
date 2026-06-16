package com.kuudrahelper.features;

import com.kuudrahelper.KuudraConfig;
import net.minecraft.client.Minecraft;

import java.util.regex.Pattern;

public final class HollowWandAnnouncer {

    // Matches a title that is brackets enclosing exactly two non-whitespace symbols,
    // with optional surrounding whitespace — e.g. "[ ⚡ ⚡ ]" or "[✿✿]"
    private static final Pattern WAND_TITLE =
            Pattern.compile("^\\s*\\[\\s*\\S\\s+\\S\\s*\\]\\s*$");

    private static long lastSentMs = 0;
    private static final long COOLDOWN_MS = 2000;

    private HollowWandAnnouncer() {}

    public static void onTitle(String text) {
        if (!KuudraConfig.isHollowWandEnabled()) return;
        String stripped = text.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
        if (!WAND_TITLE.matcher(stripped).matches()) return;

        long now = System.currentTimeMillis();
        if (now - lastSentMs < COOLDOWN_MS) return;
        lastSentMs = now;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null)
            mc.getConnection().sendCommand("pc W");
    }
}
