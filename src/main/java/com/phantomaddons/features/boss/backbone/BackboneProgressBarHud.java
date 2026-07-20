package com.phantomaddons.features.boss.backbone;

import com.phantomaddons.PhantomConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class BackboneProgressBarHud {

    private static final float BASE_SCALE  = 3.0f;
    private static final int   BAR_SEGMENTS = 20;

    private BackboneProgressBarHud() {}

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "backbone_progress_bar"),
                (drawContext, tickDelta) -> render(drawContext));
    }

    public static void render(GuiGraphicsExtractor drawContext) {
        if (!BackboneProgressBar.isVisible()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) return;

        float progress = BackboneProgressBar.getProgress();
        int filled = Math.round(progress * BAR_SEGMENTS);
        int percent = Math.round(progress * 100f);

        String fillColor = progress > 0.85f ? "§a" : progress > 0.6f ? "§6" : "§c";
        StringBuilder bar = new StringBuilder("§8[");
        bar.append(fillColor).append("|".repeat(Math.max(0, filled)));
        bar.append("§f").append("|".repeat(Math.max(0, BAR_SEGMENTS - filled)));
        bar.append("§8] §b").append(percent).append('%');

        Component comp = Component.literal(bar.toString());

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        float cx = PhantomConfig.getBackboneProgressBarHudX() * screenW;
        float cy = PhantomConfig.getBackboneProgressBarHudY() * screenH;
        float s  = BASE_SCALE * PhantomConfig.getBackboneProgressBarHudScale();

        var matrices = drawContext.pose();
        matrices.pushMatrix();
        matrices.translate(cx, cy);
        matrices.scale(s, s);

        int textWidth = mc.font.width(comp);
        drawContext.text(mc.font, comp, -textWidth / 2, 0, 0xFFFFFFFF, true);

        matrices.popMatrix();
    }
}
