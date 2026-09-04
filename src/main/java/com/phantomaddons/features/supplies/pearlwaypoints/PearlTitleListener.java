package com.phantomaddons.features.supplies.pearlwaypoints;

import com.phantomaddons.utils.TextUtil;
import com.phantomaddons.features.supplies.smoothcrate.SmoothCratePickup;
import com.phantomaddons.phase.KuudraPhaseTracker;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.phantomaddons.PhantomAddons;

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

        String clean = TextUtil.stripColor(raw).trim();

        if (!clean.contains("[|")) return;

        Matcher m = PROGRESS_PATTERN.matcher(clean);
        if (!m.find()) return;

        int percent = Integer.parseInt(m.group(1));
        int segments = countSegments(clean);
        long now = System.currentTimeMillis();

        lastTitleMs = now;

        if (!tracking) {
            tracking = true;
            lastPercent = percent;
            PearlWaypointManager.onPickupStart(percent);
            com.phantomaddons.data.supply.SupplyAttemptTracker.onPickupStart(percent);
            SmoothCratePickup.onPercentUpdate(percent, segments);
            return;
        }

        if (percent < lastPercent) {
            int endedAtPercent = lastPercent;
            tracking = false;
            PearlWaypointManager.onPickupEnd();
            com.phantomaddons.data.supply.SupplyAttemptTracker.onPickupEnd(endedAtPercent);
            PearlWaypointManager.onPickupStart(percent);
            com.phantomaddons.data.supply.SupplyAttemptTracker.onPickupStart(percent);
            tracking = true;
            SmoothCratePickup.reset();
        }

        lastPercent = percent;
        SmoothCratePickup.onPercentUpdate(percent, segments);

        if (percent >= 100) {
            tracking = false;
            lastPercent = -1;
            PearlWaypointManager.onPickupEnd();
            com.phantomaddons.data.supply.SupplyAttemptTracker.onPickupEnd(100);
        }
    }

    private static int countSegments(String clean) {
        int idx = clean.indexOf('[');
        int end = clean.indexOf(']', idx < 0 ? 0 : idx);
        if (idx < 0 || end < 0) return 20;
        int count = 0;
        for (int i = idx; i < end; i++) if (clean.charAt(i) == '|') count++;
        return count > 0 ? count : 20;
    }

    public static void tick() {

        if (!tracking) return;

        long now = System.currentTimeMillis();

        if (now - lastTitleMs > TITLE_TIMEOUT_MS) {

            int endedAtPercent = lastPercent;
            tracking = false;
            lastPercent = -1;

            PearlWaypointManager.onPickupEnd();
            com.phantomaddons.data.supply.SupplyAttemptTracker.onPickupEnd(endedAtPercent);
        }
    }

    public static void reset() {
        lastTitleMs     = -1L;
        lastPercent     = -1;
        tracking        = false;
        activeComponent = null;
        SmoothCratePickup.reset();
    }

    // ── Pearl title HUD support ───────────────────────────────────────────────

    public static boolean isMatchingTitle(String stripped) {
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.SUPPLIES) return false;
        return TextUtil.stripColor(stripped).trim().contains("[|");
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