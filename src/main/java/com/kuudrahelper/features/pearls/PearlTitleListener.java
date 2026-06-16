package com.kuudrahelper.features.pearls;

import com.kuudrahelper.phase.KuudraPhaseTracker;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.kuudrahelper.KuudraHelperMod;

public final class PearlTitleListener {

    private static final Pattern PROGRESS_PATTERN =
            Pattern.compile("\\[\\|+]\\s*(\\d+)%");
    private static final long TITLE_TIMEOUT_MS = 1500L;
    private static volatile long lastTitleMs = -1L;
    private static volatile int lastPercent = -1;
    private static volatile boolean tracking = false;
    private static volatile Component activeComponent = null;

    private PearlTitleListener() {}

    public static void onTitleText(String raw) {

        if (KuudraPhaseTracker.getPhase()
                != KuudraPhaseTracker.Phase.SUPPLIES)
            return;

        String clean = raw.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();

        if (!clean.contains("[|")) return;

        Matcher m = PROGRESS_PATTERN.matcher(clean);
        if (!m.find()) return;

        int percent = Integer.parseInt(m.group(1));
        long now = System.currentTimeMillis();

        lastTitleMs = now;

        if (!tracking) {
            tracking = true;
            lastPercent = percent;
            PearlWaypointManager.onPickupStart(percent);
            return;
        }

        if (percent < lastPercent) {
            tracking = false;
            PearlWaypointManager.onPickupEnd();
            PearlWaypointManager.onPickupStart(percent);
            tracking = true;
        }

        lastPercent = percent;

        if (percent >= 100) {
            tracking = false;
            lastPercent = -1;
            PearlWaypointManager.onPickupEnd();
        }
    }

    public static void tick() {

        if (!tracking) return;

        long now = System.currentTimeMillis();

        if (now - lastTitleMs > TITLE_TIMEOUT_MS) {

            tracking = false;
            lastPercent = -1;

            PearlWaypointManager.onPickupEnd();
        }
    }

    public static void reset() {
        lastTitleMs     = -1L;
        lastPercent     = -1;
        tracking        = false;
        activeComponent = null;
    }

    // ── Pearl title HUD support ───────────────────────────────────────────────

    public static boolean isMatchingTitle(String stripped) {
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.SUPPLIES) return false;
        return stripped.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim().contains("[|");
    }

    public static void setActiveComponent(Component comp) {
        activeComponent = comp;
    }

    public static Component getActiveComponent() {
        if (!tracking) return null;
        if (lastTitleMs < 0 || System.currentTimeMillis() - lastTitleMs > TITLE_TIMEOUT_MS) return null;
        return activeComponent;
    }
}