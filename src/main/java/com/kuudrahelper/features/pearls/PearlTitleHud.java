package com.kuudrahelper.features.pearls;

import com.kuudrahelper.KuudraConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class PearlTitleHud {

    private static final float BASE_SCALE = 4.0f;

    private PearlTitleHud() {}

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "pearl_title"),
                (drawContext, tickDelta) -> {
            Component comp = PearlTitleListener.getActiveComponent();
            if (comp == null) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.font == null) return;

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
