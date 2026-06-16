package com.kuudrahelper.features;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.NotificationHud;
import com.kuudrahelper.features.supplies.SupplyProgressHud;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BuildProgressHud {

    private static final double BUILD_DELAY_S = 4.15;
    private static final Pattern PROTECT_PATTERN = Pattern.compile("Protect Elle \\((\\d+)%\\)");

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
    }

    public static int getCurrentProgress() {
        return lastProgress;
    }

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "build_progress_hud"),
                (ctx, tc) -> {
                    if (!active) return;

                    long now = System.currentTimeMillis();
                    double elapsed = (now - phaseStartMs) / 1000.0;

                    // Fire Build Started notification at 4.15s
                    if (!buildStartedNotified && elapsed >= BUILD_DELAY_S
                            && KuudraConfig.isBuildStartedNotifyEnabled()) {
                        buildStartedNotified = true;
                        NotificationHud.show("§eBuild Started!", 3000);
                    }

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

                    // Line 1: header
                    String header = "§e§lBuild Progress:";
                    int hw = mc.font.width(header);
                    ctx.text(mc.font, header, -hw / 2, 0, 0xFFFFFF, true);

                    // Line 2: countdown then percentage
                    String line2;
                    if (elapsed < BUILD_DELAY_S) {
                        double remaining = BUILD_DELAY_S - elapsed;
                        line2 = String.format("Build Starts: %.2fs", remaining);
                    } else {
                        String sbLine = SupplyProgressHud.readSidebarForSubstring(mc, "Protect Elle");
                        if (sbLine != null) {
                            Matcher m = PROTECT_PATTERN.matcher(sbLine);
                            if (m.find()) {
                                int pct = Integer.parseInt(m.group(1));
                                lastProgress = pct;
                                line2 = "Progress: " + pct + "%";
                            } else {
                                line2 = "Progress: ?%";
                            }
                        } else {
                            line2 = "Progress: ?%";
                        }
                    }

                    int lw = mc.font.width(line2);
                    ctx.text(mc.font, line2, -lw / 2, mc.font.lineHeight + 2, 0xFFFFFF, true);

                    matrices.popMatrix();
                });
    }
}
