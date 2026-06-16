package com.kuudrahelper.features.kuudra;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.mixin.BossHealthOverlayAccessor;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.resources.Identifier;

import java.util.Map;

public final class KuudraHpHud {

    private static final int BAR_W = 160;
    private static final int BAR_H = 8;

    private KuudraHpHud() {}

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "kuudra_hp_hud"),
                (ctx, tc) -> {
                    if (!KuudraConfig.isKuudraHpHudEnabled()) return;
                    if (!isActivePhase()) return;

                    float progress = getKuudraProgress();
                    if (progress < 0) return;

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.font == null) return;

                    int screenW = mc.getWindow().getGuiScaledWidth();
                    float scale = KuudraConfig.getKuudraHpHudScale();

                    float cx = KuudraConfig.getKuudraHpHudX() * screenW;
                    float cy = KuudraConfig.getKuudraHpHudY() * mc.getWindow().getGuiScaledHeight();

                    var m = ctx.pose();
                    m.pushMatrix();
                    m.translate(cx, cy);
                    m.scale(scale, scale);

                    int scaledW = BAR_W;
                    int filled  = Math.round(scaledW * progress);

                    // Background
                    ctx.fill(-scaledW / 2, 0, scaledW / 2, BAR_H, 0xAA000000);
                    // Filled portion
                    ctx.fill(-scaledW / 2, 0, -scaledW / 2 + filled, BAR_H, barColor(progress));
                    // Border
                    ctx.fill(-scaledW / 2 - 1, -1,     scaledW / 2 + 1, 0,         0xFF000000);
                    ctx.fill(-scaledW / 2 - 1, BAR_H,  scaledW / 2 + 1, BAR_H + 1, 0xFF000000);
                    ctx.fill(-scaledW / 2 - 1, -1,     -scaledW / 2,    BAR_H + 1, 0xFF000000);
                    ctx.fill( scaledW / 2,     -1,      scaledW / 2 + 1, BAR_H + 1, 0xFF000000);

                    // HP text centred above bar
                    String pct = String.format("§cKuudra §f%.1f%%", progress * 100f);
                    int tw = mc.font.width(pct);
                    ctx.text(mc.font, pct, -tw / 2, -mc.font.lineHeight - 2, 0xFFFFFFFF, true);

                    m.popMatrix();
                });
    }

    private static float getKuudraProgress() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null) return -1;
        var overlay = mc.gui.getBossOverlay();
        Map<?, LerpingBossEvent> events =
                ((BossHealthOverlayAccessor) overlay).kuudrahelper$getEvents();
        for (LerpingBossEvent event : events.values()) {
            String name = event.getName().getString()
                    .replaceAll("§[0-9a-fk-orA-FK-OR]", "").toLowerCase();
            if (name.contains("kuudra")) return event.getProgress();
        }
        return -1;
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

    // Green at full HP → red at 0 HP
    private static int barColor(float progress) {
        int r = Math.min(255, (int) (2 * (1 - progress) * 255));
        int g = Math.min(255, (int) (2 * progress * 255));
        return 0xFF000000 | (r << 16) | (g << 8);
    }
}
