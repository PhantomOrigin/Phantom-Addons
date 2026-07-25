package com.phantomaddons.features.boss.backbone;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.boss.rend.BonemerangHitTracker;
import com.phantomaddons.phase.KuudraPhaseTracker;

public final class BackboneProgressBar {

    private static final int BASE_HIT_TICKS = 22;

    private static int     ticksRemaining = -1;
    private static int     startingTicks  = -1;
    private static boolean completed      = false;

    private BackboneProgressBar() {}

    static {
        BonemerangHitTracker.addOnBackHitListener(BackboneProgressBar::onRealHit);
    }

    public static void onBonemerangThrow() {
        if (!PhantomConfig.isBackboneProgressBarEnabled()) return;
        if (!PhantomConfig.isBackboneProgressBarOutsideKuudraEnabled() && !isKillPhase()) return;
        startingTicks  = BASE_HIT_TICKS + 1;
        ticksRemaining = startingTicks;
        completed = false;
    }

    public static void tick() {
        if (ticksRemaining <= 0) return;
        if (!PhantomConfig.isBackboneProgressBarOutsideKuudraEnabled() && !isKillPhase()) { reset(); return; }
        ticksRemaining--;
        if (ticksRemaining == 0 && !completed) {
            complete();
        }
    }

    private static void onRealHit() {
        if (ticksRemaining <= 0 || completed) return;
        ticksRemaining = 0;
        complete();
    }

    private static void complete() {
        completed = true;
        ticksRemaining = 0;
        PhantomConfig.playNotificationSound(PhantomConfig.SOUND_BACKBONE_DONE);
    }

    public static void reset() {
        ticksRemaining = -1;
        startingTicks  = -1;
        completed = false;
    }

    public static boolean isVisible() {
        return PhantomConfig.isBackboneProgressBarEnabled() && ticksRemaining > 0;
    }

    public static float getProgress() {
        if (startingTicks <= 0) return 0f;
        float elapsed = startingTicks - ticksRemaining;
        return Math.max(0f, Math.min(1f, elapsed / startingTicks));
    }

    private static boolean isKillPhase() {
        KuudraPhaseTracker.Phase p = KuudraPhaseTracker.getPhase();
        return p == KuudraPhaseTracker.Phase.STUN
            || p == KuudraPhaseTracker.Phase.DPS
            || p == KuudraPhaseTracker.Phase.BOSS;
    }
}
