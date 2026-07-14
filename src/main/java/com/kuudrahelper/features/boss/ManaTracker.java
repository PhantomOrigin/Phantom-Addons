package com.kuudrahelper.features.boss;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.KuudraHelperMod;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ManaTracker {

    private static final Pattern MANA_PATTERN = Pattern.compile("([\\d,]+)\\s*/\\s*([\\d,]+)[^\\d]*Mana", Pattern.CASE_INSENSITIVE);

    private static volatile int currentMana = -1;

    private static long lastUnmatchedLogMs = 0L;
    private static final long UNMATCHED_LOG_INTERVAL_MS = 2000L;

    private ManaTracker() {}

    public static int getCurrentMana() { return currentMana; }

    public static void onChat(String clean) {
        Matcher m = MANA_PATTERN.matcher(clean);
        if (!m.find()) {
            if (KuudraConfig.isDeveloperFeaturesEnabled() && !clean.isBlank()) {
                long now = System.currentTimeMillis();
                if (now - lastUnmatchedLogMs > UNMATCHED_LOG_INTERVAL_MS) {
                    lastUnmatchedLogMs = now;
                    KuudraHelperMod.LOGGER.info("[ManaTracker] Unmatched action bar text: \"{}\"", clean);
                }
            }
            return;
        }
        try {
            currentMana = Integer.parseInt(m.group(1).replace(",", ""));
        } catch (NumberFormatException ignored) {}
    }

    public static void reset() {
        currentMana = -1;
    }
}
