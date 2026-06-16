package com.kuudrahelper.features;

import com.kuudrahelper.KuudraConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class BuildProgressHud {

    private static final double BUILD_DELAY_S = 4.15;

    private static volatile boolean active              = false;
    private static volatile long    phaseStartMs        = 0;
    private static volatile int     lastProgress        = -1;
    private static volatile boolean buildStartedNotified = false;

    private BuildProgressHud() {}

    public static void onBuildStart() {
        active               = true;
        phaseStartMs         = System.currentTimeMillis();
        lastProgress         = -1;
        buildStartedNotified = false;
    }

    public static void reset() {
        active               = false;
        phaseStartMs         = 0;
        lastProgress         = -1;
        buildStartedNotified = false;
        NotificationHud.clearCountdown();
    }

    public static int getCurrentProgress() {
        return lastProgress;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!active) return;
            long now     = System.currentTimeMillis();
            double elapsed = (now - phaseStartMs) / 1000.0;

            if (elapsed < BUILD_DELAY_S) {
                double remaining = BUILD_DELAY_S - elapsed;
                NotificationHud.setCountdown(String.format("§eBuild Starts: §f%.2fs", remaining));
            } else {
                NotificationHud.clearCountdown();
                if (!buildStartedNotified && KuudraConfig.isBuildStartedNotifyEnabled()) {
                    buildStartedNotified = true;
                    NotificationHud.show("§eBuild Started!", 3000);
                }
            }
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "build_progress_hud"),
                (ctx, tc) -> {
                    if (!active) return;

                    long now = System.currentTimeMillis();
                    double elapsed = (now - phaseStartMs) / 1000.0;
                    if (elapsed < BUILD_DELAY_S) return;

                    if (!KuudraConfig.isBuildProgressHudEnabled()) return;

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.font == null) return;

                    int screenW = mc.getWindow().getGuiScaledWidth();
                    int screenH = mc.getWindow().getGuiScaledHeight();

                    float cx = KuudraConfig.getBuildProgressHudX() * screenW;
                    float cy = KuudraConfig.getBuildProgressHudY() * screenH;
                    float scale = KuudraConfig.getBuildProgressHudScale();

                    var matrices = ctx.pose();
                    matrices.pushMatrix();
                    matrices.translate(cx, cy);
                    matrices.scale(scale, scale);

                    int total = 0, count = 0;
                    for (com.kuudrahelper.features.pearls.PearlLocation loc
                            : com.kuudrahelper.features.pearls.PearlLocation.values()) {
                        int p = BuildProgressTracker.getProgress(loc);
                        total += Math.max(0, p);
                        count++;
                    }
                    if (count > 0) lastProgress = total / count;

                    String header = "§e§lBuild Progress:";
                    int hw = mc.font.width(header);
                    ctx.text(mc.font, header, -hw / 2, 0, 0xFFFFFFFF, true);

                    String line2 = lastProgress >= 0 ? "Progress: " + lastProgress + "%" : "Progress: 0%";
                    int lw = mc.font.width(line2);
                    ctx.text(mc.font, line2, -lw / 2, mc.font.lineHeight + 2, 0xFFFFFFFF, true);

                    matrices.popMatrix();
                });
    }
}
