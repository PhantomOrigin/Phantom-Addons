package com.phantomaddons.features.supplies.tentaclewarn;

import com.phantomaddons.PhantomConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class TentacleGrabWarningHud {

    private static final String TEXT = "§c§l!!!";

    private TentacleGrabWarningHud() {}

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "tentacle_grab_warning"),
                (ctx, tc) -> {
                    if (!PhantomConfig.isTentacleGrabWarningEnabled() || !PhantomConfig.isTentacleGrabHudEnabled()) return;
                    if (!TentacleGrabWarning.isGrabLikely()) return;

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.font == null) return;

                    float cx = PhantomConfig.getTentacleWarnHudX() * mc.getWindow().getGuiScaledWidth();
                    float cy = PhantomConfig.getTentacleWarnHudY() * mc.getWindow().getGuiScaledHeight();
                    float scale = PhantomConfig.getTentacleWarnHudScale();

                    var matrices = ctx.pose();
                    matrices.pushMatrix();
                    matrices.translate(cx, cy);
                    matrices.scale(scale, scale);
                    ctx.text(mc.font, TEXT, -mc.font.width(TEXT) / 2, -mc.font.lineHeight / 2, 0xFFFFFFFF, true);
                    matrices.popMatrix();
                });
    }
}
