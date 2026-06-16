package com.kuudrahelper.features.kuudra;

import com.kuudrahelper.KuudraConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ManaDrainAnnouncer {

    // Hypixel chat message sent when the Endstone Sword ability fires
    private static final Pattern EXTREME_FOCUS_PATTERN =
            Pattern.compile("Used Extreme Focus! \\((\\d+) Mana\\)");

    private static final double AFFECT_RADIUS = 5.0;

    private ManaDrainAnnouncer() {}

    public static void onChat(String clean) {
        if (!KuudraConfig.isManaDrainAnnouncerEnabled()) return;
        Matcher m = EXTREME_FOCUS_PATTERN.matcher(clean);
        if (!m.find()) return;

        String mana = m.group(1);
        int affected = countAffectedPlayers();

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null)
            mc.getConnection().sendCommand("pc [Phantom] Mana Drain: " + mana + " mana on " + affected + " player" + (affected == 1 ? "" : "s") + "!");
    }

    private static int countAffectedPlayers() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return 0;

        Vec3 self = mc.player.position();
        int count = 0;
        for (Player p : mc.level.players()) {
            if (p == mc.player) continue;
            if (!isRealPlayer(mc, p)) continue;
            if (p.position().distanceTo(self) <= AFFECT_RADIUS) count++;
        }
        return count;
    }

    // Filter out NPCs: real players have a non-null PlayerInfo with ping >= 1
    private static boolean isRealPlayer(Minecraft mc, Player p) {
        PlayerInfo info = mc.getConnection().getPlayerInfo(p.getUUID());
        return info != null && info.getLatency() >= 1;
    }
}
