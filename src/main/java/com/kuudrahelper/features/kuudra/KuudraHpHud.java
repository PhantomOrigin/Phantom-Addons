package com.kuudrahelper.features.kuudra;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;

public final class KuudraHpHud {

    private static final int   BAR_W          = 160;
    private static final int   BAR_H          = 8;
    private static final int   DPS_MULTIPLIER = 9_600;
    private static final float KUUDRA_MAX_HP  = 25_000f;
    private static final float KUUDRA_RAW_MAX = 100_000f;
    private static final float DECOY_MAX_HP   = 2_000f;

    private static Slime   cachedKuudra  = null;
    private static float   smoothProgress = -1f;
    private static long    lastFrameMs    = -1L;
    // Display values — written by tick, read by render
    private static volatile float   displayHp      = 0f;
    private static volatile float   displayMaxHp   = 0f;
    private static volatile float   lastProgress   = -1f;
    private static volatile boolean showKillMarker = false;

    private KuudraHpHud() {}

    public static void reset() {
        cachedKuudra  = null;
        smoothProgress = -1f;
        lastFrameMs   = -1L;
        displayHp     = 0f;
        displayMaxHp  = 0f;
        lastProgress  = -1f;
        showKillMarker = false;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) { cachedKuudra = null; return; }
            if (!isActivePhase()) { return; }

            // Validate cache: clear if dead
            if (cachedKuudra != null) {
                if (cachedKuudra.isRemoved() || !cachedKuudra.isAlive()) {
                    cachedKuudra = null;
                }
            }

            // If cached entity's HP dropped to decoy-level, clear and rescan
            if (cachedKuudra != null) {
                float hp = cachedKuudra.getHealth();
                if (hp > 0 && hp < DECOY_MAX_HP) {
                    cachedKuudra = null;
                } else {
                    updateDisplayValues();
                    return;
                }
            }

            // Rescan: pick size-30 slime with highest current health (real Kuudra >> decoy ~1024)
            Slime best = null;
            for (Entity e : client.level.entitiesForRendering()) {
                if (!(e instanceof Slime s) || s.getSize() != 30) continue;
                if (best == null || s.getHealth() > best.getHealth()) best = s;
            }
            if (best != null && best.getHealth() >= DECOY_MAX_HP) {
                cachedKuudra = best;
                updateDisplayValues();
            }
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "kuudra_hp_hud"),
                (ctx, tc) -> {
                    if (!KuudraConfig.isKuudraHpHudEnabled()) return;
                    if (!isActivePhase()) { smoothProgress = -1f; return; }

                    float progress = lastProgress;
                    if (progress < 0) { smoothProgress = -1f; return; }

                    long now = System.currentTimeMillis();
                    if (smoothProgress < 0 || lastFrameMs < 0) {
                        smoothProgress = progress;
                    } else {
                        float dt = Math.min((now - lastFrameMs) / 1000f, 0.1f);
                        smoothProgress += (progress - smoothProgress) * Math.min(1f, dt * 20f);
                    }
                    lastFrameMs = now;

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.font == null) return;

                    float scale = KuudraConfig.getKuudraHpHudScale();
                    float cx = KuudraConfig.getKuudraHpHudX() * mc.getWindow().getGuiScaledWidth();
                    float cy = KuudraConfig.getKuudraHpHudY() * mc.getWindow().getGuiScaledHeight();

                    var m = ctx.pose();
                    m.pushMatrix();
                    m.translate(cx, cy);
                    m.scale(scale, scale);

                    int half    = BAR_W / 2;
                    int filledI = Math.round(BAR_W * smoothProgress);

                    ctx.fill(-half, 0, half, BAR_H, 0xAA000000);
                    if (filledI > 0)
                        ctx.fill(-half, 0, -half + filledI, BAR_H, barColor(smoothProgress));
                    ctx.fill(-half - 1, -1,    half + 1, 0,         0xFF000000);
                    ctx.fill(-half - 1, BAR_H, half + 1, BAR_H + 1, 0xFF000000);
                    ctx.fill(-half - 1, -1,   -half,     BAR_H + 1, 0xFF000000);
                    ctx.fill( half,     -1,    half + 1,  BAR_H + 1, 0xFF000000);

                    // 25% threshold marker: shows where Kuudra transitions to kill phase (240m display)
                    if (showKillMarker) {
                        int markerX = -half + Math.round(BAR_W * 0.25f); // 25k / 100k = 25%
                        ctx.fill(markerX, -1, markerX + 1, BAR_H + 1, 0xFFFFFFFF);
                    }

                    String label;
                    if (KuudraConfig.isKuudraHpShowRaw()) {
                        label = "§f" + formatExact(displayHp) + " §7/ §f" + formatExact(displayMaxHp);
                    } else {
                        label = String.format("§f%.1f%%", smoothProgress * 100f);
                    }
                    int tw = mc.font.width(label);
                    ctx.text(mc.font, label, -tw / 2, -mc.font.lineHeight - 2, 0xFFFFFFFF, true);

                    m.popMatrix();
                });
    }

    private static void updateDisplayValues() {
        if (cachedKuudra == null) { lastProgress = -1f; return; }

        float hp = cachedKuudra.getHealth();
        if (hp <= 0) return; // not yet synced — keep last known values

        // KIC approach: HP-based mode only, no phase tracker
        if (hp <= KUUDRA_MAX_HP) {
            // Kill phase: raw HP is 0-25000, display multiplied by 9600
            displayHp      = hp * DPS_MULTIPLIER;
            displayMaxHp   = KUUDRA_MAX_HP * DPS_MULTIPLIER;
            lastProgress   = Math.min(hp / KUUDRA_MAX_HP, 1f);
            showKillMarker = false;
        } else {
            // Pre-kill: raw HP is 25000-100000
            displayHp      = hp;
            displayMaxHp   = KUUDRA_RAW_MAX;
            lastProgress   = Math.min(hp / KUUDRA_RAW_MAX, 1f);
            showKillMarker = true;
        }
    }

    private static boolean isActivePhase() {
        KuudraPhaseTracker.Phase p = KuudraPhaseTracker.getPhase();
        return p == KuudraPhaseTracker.Phase.EATEN
                || p == KuudraPhaseTracker.Phase.STUN
                || p == KuudraPhaseTracker.Phase.DPS
                || p == KuudraPhaseTracker.Phase.SKIP
                || p == KuudraPhaseTracker.Phase.BOSS
                || p == KuudraPhaseTracker.Phase.END;
    }

    /** Exact integer formatting with comma thousands separators, no abbreviations. */
    private static String formatExact(float hp) {
        return String.format("%,d", (long) hp);
    }

    private static int barColor(float progress) {
        int r = Math.min(255, (int) (2 * (1 - progress) * 255));
        int g = Math.min(255, (int) (2 * progress * 255));
        return 0xFF000000 | (r << 16) | (g << 8);
    }
}
