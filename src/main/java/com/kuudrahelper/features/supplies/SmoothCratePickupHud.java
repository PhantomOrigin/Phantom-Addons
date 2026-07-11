package com.kuudrahelper.features.supplies;

import com.kuudrahelper.KuudraConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class SmoothCratePickupHud {

    private static final float BASE_SCALE = 4.0f;

    private SmoothCratePickupHud() {}

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "smooth_crate_pickup"),
                (drawContext, tickDelta) -> {
            if (!KuudraConfig.isSmoothCratePickupEnabled()) return;
            if (PearlTitleListener.getActiveComponent() == null) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.font == null) return;

            int percent  = Math.max(0, Math.min(100, SmoothCratePickup.getDisplayPercent()));
            int segments = SmoothCratePickup.getSegments();
            int filled   = Math.round(segments * (percent / 100f));

            StringBuilder bar = new StringBuilder("§6[");
            for (int i = 0; i < segments; i++) {
                bar.append(i < filled ? "§a|" : "§8|");
            }
            bar.append("§6] §e").append(percent).append('%');

            Component comp = Component.literal(bar.toString());

            int screenW = mc.getWindow().getGuiScaledWidth();
            int screenH = mc.getWindow().getGuiScaledHeight();

            float cx = KuudraConfig.getPearlTitleHudX() * screenW;
            float cy = KuudraConfig.getPearlTitleHudY() * screenH;
            float s  = BASE_SCALE * KuudraConfig.getPearlTitleHudScale();

            var matrices = drawContext.pose();
            matrices.pushMatrix();
            matrices.translate(cx, cy);
            matrices.scale(s, s);

            int textWidth = mc.font.width(comp);
            drawContext.text(mc.font, comp, -textWidth / 2, 0, 0xFFFFFFFF, true);

            matrices.popMatrix();
        });
    }
}
