package com.kuudrahelper.features.misckuudra;

import com.kuudrahelper.KuudraConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class NotificationHud {

    private record Notification(String text, long expiresAt) {}

    private static final List<Notification> notifications = new ArrayList<>();
    private static volatile String countdown = null;

    private NotificationHud() {}

    public static void show(String text, long durationMs) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            long now = System.currentTimeMillis();
            notifications.removeIf(n -> n.expiresAt() <= now);
            notifications.add(new Notification(text, now + durationMs));
        });
    }

    public static void setCountdown(String text) { countdown = text; }
    public static void clearCountdown()          { countdown = null; }

    public static void reset() {
        notifications.clear();
        countdown = null;
    }

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "notification_hud"),
                (ctx, tc) -> {
                    long now = System.currentTimeMillis();
                    notifications.removeIf(n -> n.expiresAt() <= now);
                    String cd = countdown;
                    if (notifications.isEmpty() && cd == null) return;

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.font == null) return;

                    int screenW = mc.getWindow().getGuiScaledWidth();
                    int screenH = mc.getWindow().getGuiScaledHeight();

                    float cx = KuudraConfig.getNotificationHudX() * screenW;
                    float cy = KuudraConfig.getNotificationHudY() * screenH;
                    float scale = KuudraConfig.getNotificationHudScale();

                    var matrices = ctx.pose();
                    matrices.pushMatrix();
                    matrices.translate(cx, cy);
                    matrices.scale(scale, scale);

                    int lineH = mc.font.lineHeight + 2;
                    int totalLines = notifications.size() + (cd != null ? 1 : 0);
                    int startY = -(totalLines * lineH) / 2;
                    int row = 0;

                    if (cd != null) {
                        int tw = mc.font.width(cd);
                        ctx.text(mc.font, cd, -tw / 2, startY + row * lineH, 0xFFFFFFFF, true);
                        row++;
                    }
                    for (Notification n : notifications) {
                        String text = n.text();
                        int tw = mc.font.width(text);
                        ctx.text(mc.font, text, -tw / 2, startY + row * lineH, 0xFFFFFFFF, true);
                        row++;
                    }

                    matrices.popMatrix();
                });
    }
}
