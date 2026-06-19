package com.kuudrahelper.features.supplies;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.NotificationHud;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CratePriority {

    private static final String[] SPOT_TO_PLAYER_PRE = {
            null,        // 0 Shop
            "Triangle",  // 1 Triangle
            "X",         // 2 X
            null,        // 3 X Cannon
            "Equals",    // 4 Equals
            "Slash",     // 5 Slash
            null,        // 6 Square
    };

    private static final Map<String, Map<String, String>> PRIORITY;
    static {
        PRIORITY = new HashMap<>();
        PRIORITY.put("X",        Map.of("X","Shop",     "Slash","Square",   "Equals","Square",   "Triangle","X Cannon"));
        PRIORITY.put("X Cannon", Map.of("X","Square",   "Slash","Square",   "Equals","Shop",     "Triangle","Shop"));
        PRIORITY.put("Square",   Map.of("X","X Cannon", "Slash","X Cannon", "Equals","Shop",     "Triangle","Shop"));
        PRIORITY.put("Slash",    Map.of("X","X Cannon", "Slash","Shop",     "Equals","Square",   "Triangle","Square"));
        PRIORITY.put("Equals",   Map.of("X","X Cannon", "Slash","Square",   "Equals","Shop",     "Triangle","Square"));
        PRIORITY.put("Triangle", Map.of("X","X Cannon", "Slash","Square",   "Equals","Square",   "Triangle","Shop"));
        PRIORITY.put("Shop",     Map.of("X","X Cannon", "Slash","Square",   "Equals","Square",   "Triangle","X Cannon"));
    }

    private static final Set<String> VALID_SPOTS = Set.of(
            "X", "X Cannon", "Square", "Slash", "Equals", "Triangle", "Shop"
    );

    private static final Pattern NO_PRE_PARTY = Pattern.compile(
            "Party > (?:\\[.*?\\] )?\\w+: No ([A-Za-z ]+)!"
    );

    private static String destination = null;
    private static long expiresAt = 0;

    private CratePriority() {}

    public static void reset() {
        destination = null;
        expiresAt = 0;
    }

    public static String getDestination() {
        if (destination == null || System.currentTimeMillis() > expiresAt) return null;
        return destination;
    }

    public static void onChat(String clean) {
        Matcher m = NO_PRE_PARTY.matcher(clean);
        if (!m.find()) return;

        String missingSpot = m.group(1).trim();
        if (!VALID_SPOTS.contains(missingSpot)) return;

        if (KuudraConfig.isNoPreNotifyEnabled()) {
            NotificationHud.show("§fNo " + missingSpot + "!", 6000);
        }

        if (!KuudraConfig.isCratePriorityEnabled()) return;

        int playerSpot = SupplyWaypointTracker.playerSpotIdx;
        if (playerSpot < 0 || playerSpot >= SPOT_TO_PLAYER_PRE.length) return;
        String playerPre = SPOT_TO_PLAYER_PRE[playerSpot];
        if (playerPre == null) return;

        Map<String, String> inner = PRIORITY.get(missingSpot);
        if (inner == null) return;
        String dest = inner.get(playerPre);
        if (dest == null) return;

        destination = dest;
        expiresAt = System.currentTimeMillis() + 8000;
    }

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "crate_priority"),
                (ctx, tc) -> {
                    if (!KuudraConfig.isCratePriorityEnabled()) return;
                    if (destination == null || System.currentTimeMillis() > expiresAt) return;

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.font == null) return;

                    int screenW = mc.getWindow().getGuiScaledWidth();
                    int screenH = mc.getWindow().getGuiScaledHeight();

                    float cx = KuudraConfig.getCratePriorityHudX() * screenW;
                    float cy = KuudraConfig.getCratePriorityHudY() * screenH;
                    float scale = KuudraConfig.getCratePriorityHudScale();

                    var matrices = ctx.pose();
                    matrices.pushMatrix();
                    matrices.translate(cx, cy);
                    matrices.scale(scale, scale);

                    String text = "§eGo " + destination + "!";
                    int tw = mc.font.width(text);
                    ctx.text(mc.font, text, -tw / 2, -mc.font.lineHeight / 2, 0xFFFFFFFF, true);

                    matrices.popMatrix();
                });
    }
}
