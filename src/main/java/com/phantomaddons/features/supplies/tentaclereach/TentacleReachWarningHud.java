package com.phantomaddons.features.supplies.tentaclereach;

import com.phantomaddons.PhantomConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class TentacleReachWarningHud {

    private static final String TEXT = "§c§lINSIDE TENTACLE RANGE!";

    private TentacleReachWarningHud() {}

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "tentacle_reach_warning"),
                (ctx, tc) -> {
                    if (!PhantomConfig.isTentacleReachEnabled() || !PhantomConfig.isTentacleReachWarning()) return;
                    if (!TentacleReach.isInside()) return;

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.font == null) return;

                    var matrices = ctx.pose();
                    matrices.pushMatrix();
                    matrices.translate(PhantomConfig.getTentacleReachWarnHudX() * mc.getWindow().getGuiScaledWidth(),
                            PhantomConfig.getTentacleReachWarnHudY() * mc.getWindow().getGuiScaledHeight());
                    float scale = PhantomConfig.getTentacleReachWarnHudScale();
                    matrices.scale(scale, scale);
                    ctx.text(mc.font, TEXT, -mc.font.width(TEXT) / 2, -mc.font.lineHeight / 2, 0xFFFFFFFF, true);
                    matrices.popMatrix();
                });
    }
}
