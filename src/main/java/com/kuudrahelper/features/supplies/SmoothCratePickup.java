package com.kuudrahelper.features.supplies;

public final class SmoothCratePickup {

    private static final long ANIM_DURATION_MS = 350L;

    private static volatile int  animStartPercent  = 0;
    private static volatile int  animTargetPercent = 0;
    private static volatile long animStartMs       = -1L;
    private static volatile int  segments          = 20;

    private SmoothCratePickup() {}

    public static void onPercentUpdate(int percent, int totalSegments) {
        animStartPercent  = getDisplayPercent();
        animTargetPercent = percent;
        animStartMs       = System.currentTimeMillis();
        if (totalSegments > 0) segments = totalSegments;
    }

    public static void reset() {
        animStartPercent  = 0;
        animTargetPercent = 0;
        animStartMs       = -1L;
        segments          = 20;
    }

    public static int getDisplayPercent() {
        if (animStartMs < 0) return animTargetPercent;
        long elapsed = System.currentTimeMillis() - animStartMs;
        if (elapsed >= ANIM_DURATION_MS) return animTargetPercent;
        float t = elapsed / (float) ANIM_DURATION_MS;
        return Math.round(animStartPercent + (animTargetPercent - animStartPercent) * t);
    }

    public static int getSegments() { return segments; }
}
