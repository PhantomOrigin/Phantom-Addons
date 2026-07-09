package com.kuudrahelper.features.supplies;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class DoublePearlWarningHud {

    private static final float SCALE = 3.0f;
    private static final String TEXT = "DOUBLE PEARL!";

    private DoublePearlWarningHud() {}

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "double_pearl_warning"),
                (drawContext, tickDelta) -> {
            if (!SupplyGiantHitbox.isWarningActive()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.font == null) return;

            int screenW = mc.getWindow().getGuiScaledWidth();
            int screenH = mc.getWindow().getGuiScaledHeight();

            Component comp = Component.literal(TEXT);
            var matrices = drawContext.pose();
            matrices.pushMatrix();
            matrices.translate(screenW / 2f, screenH * 0.25f);
            matrices.scale(SCALE, SCALE);

            int textWidth = mc.font.width(comp);
            drawContext.text(mc.font, comp, -textWidth / 2, 0, 0xFFFF3333, true);

            matrices.popMatrix();
        });
    }
}
