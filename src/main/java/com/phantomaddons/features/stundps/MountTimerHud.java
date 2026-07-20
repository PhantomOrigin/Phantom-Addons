package com.phantomaddons.features.stundps;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.utils.Phase2BuildTracker;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class MountTimerHud {

    private static final float SCALE       = 3.0f;
    private static final int   COLOR_WARN  = 0xFFFF5555;
    private static final int   COLOR_NORM  = 0xFFFFFFFF;

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "mount_timer"),
                (drawContext, tickDelta) -> {
            if (!PhantomConfig.isEatenTimerEnabled()) return;
            int timer = Phase2BuildTracker.getMountTimer();
            if (timer <= 0) return;

            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;

            int pingTicks    = PhantomConfig.isEatenTimerSubtractPingEnabled() ? PhantomConfig.getLowPing() / 50 : 0;
            int displayTimer = Math.max(0, timer - pingTicks);

            String text = String.valueOf(displayTimer);
            int color   = displayTimer <= 20 ? COLOR_WARN : COLOR_NORM;

            int screenW = client.getWindow().getGuiScaledWidth();
            int screenH = client.getWindow().getGuiScaledHeight();

            var matrices = drawContext.pose();
            matrices.pushMatrix();
            matrices.translate(PhantomConfig.getMountTimerHudX() * screenW, PhantomConfig.getMountTimerHudY() * screenH);
            float s = SCALE * PhantomConfig.getMountTimerHudScale();
            matrices.scale(s, s);

            int textWidth = client.font.width(text);
            drawContext.text(
                    client.font,
                    Component.literal(text),
                    -textWidth / 2, 0,
                    color, true);

            matrices.popMatrix();
        });
    }
}
