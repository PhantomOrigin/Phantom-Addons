package com.phantomaddons.features.boss.mana;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.PhantomAddons;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ManaTracker {

    private static final Pattern FRACTION_PATTERN = Pattern.compile("([\\d,]+)\\s*/\\s*([\\d,]+)");
    private static final int MANA_FRACTION_INDEX = 2; // 1st = Health, 2nd = Mana

    private static volatile int currentMana = -1;

    private static long lastUnmatchedLogMs = 0L;
    private static final long UNMATCHED_LOG_INTERVAL_MS = 2000L;

    private ManaTracker() {}

    public static int getCurrentMana() { return currentMana; }

    public static void onChat(String clean) {
        Matcher m = FRACTION_PATTERN.matcher(clean);
        int index = 0;
        String manaCurrent = null;
        while (m.find()) {
            index++;
            if (index == MANA_FRACTION_INDEX) {
                manaCurrent = m.group(1);
                break;
            }
        }

        if (manaCurrent == null) {
            if (PhantomConfig.isDeveloperFeaturesEnabled() && !clean.isBlank()) {
                long now = System.currentTimeMillis();
                if (now - lastUnmatchedLogMs > UNMATCHED_LOG_INTERVAL_MS) {
                    lastUnmatchedLogMs = now;
                    PhantomAddons.LOGGER.info("[ManaTracker] Unmatched action bar text: \"{}\"", clean);
                }
            }
            return;
        }
        try {
            currentMana = Integer.parseInt(manaCurrent.replace(",", ""));
        } catch (NumberFormatException ignored) {}
    }

    public static void reset() {
        currentMana = -1;
    }
}
