package com.kuudrahelper.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

public final class KickedTimerHud {

    private static final long DURATION_MS = 60_000L;
    private static final float SCALE      = 3.0f;

    private static volatile long kickedAtMs = -1;
    private static volatile boolean dinged  = false;

    private KickedTimerHud() {}

    public static void onKicked() {
        kickedAtMs = System.currentTimeMillis();
        dinged     = false;
    }

    public static void onServerJoin() {
        if (kickedAtMs < 0) return;
        if (System.currentTimeMillis() - kickedAtMs >= 5_000L) {
            kickedAtMs = -1;
            dinged     = false;
        }
    }

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onServerJoin());

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "kicked_timer"),
                (ctx, tickDelta) -> {
                    long kicked = kickedAtMs;
                    if (kicked < 0) return;

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) return;

                    long elapsed = System.currentTimeMillis() - kicked;
                    long remaining = (DURATION_MS - elapsed) / 1000L;

                    String line1;
                    String line2;
                    int color1, color2;

                    if (remaining > 0) {
                        line1  = "§cKicked from Skyblock";
                        line2  = String.valueOf(remaining);
                        color1 = 0xFFFF5555;
                        color2 = remaining <= 10 ? 0xFFFF5555 : 0xFFFFFFFF;
                    } else {
                        if (!dinged) {
                            dinged = true;
                            mc.getSoundManager().play(
                                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 2.0f));
                        }
                        line1  = "§eRejoin Skyblock Now";
                        line2  = null;
                        color1 = 0xFFFFFF55;
                        color2 = 0;
                    }

                    int screenW = mc.getWindow().getGuiScaledWidth();
                    int screenH = mc.getWindow().getGuiScaledHeight();
                    int cx = screenW / 2;
                    int cy = screenH / 2;

                    var matrices = ctx.pose();
                    matrices.pushMatrix();
                    matrices.translate(cx, cy - 20);

                    if (line2 != null) {
                        int lw1 = mc.font.width(line1);
                        ctx.text(mc.font, Component.literal(line1), -lw1 / 2, 0, color1, true);

                        matrices.translate(0, mc.font.lineHeight + 4);
                        matrices.scale(SCALE, SCALE);
                        int lw2 = mc.font.width(line2);
                        ctx.text(mc.font, Component.literal(line2), -lw2 / 2, 0, color2, true);
                    } else {
                        matrices.scale(SCALE, SCALE);
                        int lw1 = mc.font.width(line1);
                        ctx.text(mc.font, Component.literal(line1), -lw1 / 2, 0, color1, true);
                    }

                    matrices.popMatrix();
                });
    }
}
