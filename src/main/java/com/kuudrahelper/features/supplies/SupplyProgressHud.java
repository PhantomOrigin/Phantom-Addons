package com.kuudrahelper.features.supplies;

import com.kuudrahelper.KuudraConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SupplyProgressHud {

    private static final double SPAWN_DELAY_S = 9.0;
    private static final Pattern SUPPLY_PATTERN = Pattern.compile("Rescue Supplies \\((\\d+)/(\\d+)\\)");

    private static volatile boolean active       = false;
    private static volatile long    phaseStartMs = 0;

    private SupplyProgressHud() {}

    public static long getPhaseStartMs() { return phaseStartMs; }

    public static void onSuppliesStart() {
        active       = true;
        phaseStartMs = System.currentTimeMillis();
    }

    public static void reset() {
        active       = false;
        phaseStartMs = 0;
    }

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "supply_progress_hud"),
                (ctx, tc) -> {
                    if (!KuudraConfig.isSupplyProgressHudEnabled()) return;
                    if (!active) return;

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.font == null) return;

                    long now = System.currentTimeMillis();
                    double elapsed = (now - phaseStartMs) / 1000.0;

                    int screenW = mc.getWindow().getGuiScaledWidth();
                    int screenH = mc.getWindow().getGuiScaledHeight();

                    float cx = KuudraConfig.getSupplyProgressHudX() * screenW;
                    float cy = KuudraConfig.getSupplyProgressHudY() * screenH;
                    float scale = KuudraConfig.getSupplyProgressHudScale();

                    var matrices = ctx.pose();
                    matrices.pushMatrix();
                    matrices.translate(cx, cy);
                    matrices.scale(scale, scale);

                    // Line 1: header
                    String header = "§e§lSupplies Progress:";
                    int hw = mc.font.width(header);
                    ctx.text(mc.font, header, -hw / 2, 0, 0xFFFFFF, true);

                    // Line 2: countdown then gathered count
                    String line2;
                    if (elapsed < SPAWN_DELAY_S) {
                        double remaining = SPAWN_DELAY_S - elapsed;
                        line2 = String.format("Supplies Spawn: %.2fs", remaining);
                    } else {
                        String sbLine = readSidebarForSubstring(mc, "Rescue Supplies");
                        if (sbLine != null) {
                            Matcher m = SUPPLY_PATTERN.matcher(sbLine);
                            line2 = m.find()
                                    ? "Supplies Gathered: " + m.group(1) + "/" + m.group(2)
                                    : "Supplies Gathered: ?/?";
                        } else {
                            line2 = "Supplies Gathered: ?/?";
                        }
                    }

                    int lw = mc.font.width(line2);
                    ctx.text(mc.font, line2, -lw / 2, mc.font.lineHeight + 2, 0xFFFFFF, true);

                    matrices.popMatrix();
                });
    }

    public static String readSidebarForSubstring(Minecraft mc, String substring) {
        if (mc.level == null) return null;
        Scoreboard sb = mc.level.getScoreboard();
        Objective sidebar = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return null;

        for (var entry : sb.listPlayerScores(sidebar)) {
            String owner = entry.owner();
            PlayerTeam team = sb.getPlayerTeam(owner);
            String raw;
            if (team != null) {
                raw = team.getPlayerPrefix().getString()
                        + owner
                        + team.getPlayerSuffix().getString();
            } else {
                raw = owner;
            }
            String clean = raw.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
            if (clean.contains(substring)) return clean;
        }
        return null;
    }
}
