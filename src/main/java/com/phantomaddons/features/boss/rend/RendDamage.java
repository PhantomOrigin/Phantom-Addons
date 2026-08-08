package com.phantomaddons.features.boss.rend;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.phase.KuudraPhaseTracker;
import com.phantomaddons.phase.KuudraPhaseTracker.Phase;
import com.phantomaddons.utils.KuudraTierDetector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
//? if <26.2 {
/*import net.minecraft.world.entity.monster.Slime;
*///?} else {
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
//?}


public final class RendDamage {

    public  static final int  KUUDRA_SLIME_MAX_HP = 25_000;
    private static final int  KUUDRA_MAX_HP       = KUUDRA_SLIME_MAX_HP;
    private static final int  MIN_PULL_DIFF  = 1_666;
    private static final int  HP_MULTIPLIER  = 9_600;

    private static int  lastHp       = -1;
    private static long phaseStartMs = -1L;

    private RendDamage() {}

    public static void onKillPhaseStart() {
        if (phaseStartMs >= 0) return; // already started — keep original timestamp and lastHp
        lastHp       = -1;
        phaseStartMs = System.currentTimeMillis();
    }

    public static void onBossPhaseStart() {
        lastHp       = -1;
        phaseStartMs = System.currentTimeMillis();
    }

    public static void reset() {
        lastHp       = -1;
        phaseStartMs = -1L;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!PhantomConfig.isRendDamageEnabled()) return;
            if (client.level == null || client.player == null) return;
            if (KuudraTierDetector.getTier() != 5) return;

            Phase phase = KuudraPhaseTracker.getPhase();
            if (phase == Phase.SUPPLIES || phase == Phase.BUILD
                    || phase == Phase.EATEN || phase == Phase.SKIP
                    || phase == Phase.END || phase == Phase.NONE) return;

            AbstractCubeMob kuudra = findKuudra(client);
            if (kuudra == null) return;

            int hp = Math.min((int) kuudra.getHealth(), KUUDRA_MAX_HP);
            if (hp <= 0) { reset(); return; }

            if (lastHp < 0) { lastHp = hp; return; }

            int diff = lastHp - hp;
            lastHp = hp;

            if (diff >= MIN_PULL_DIFF) {
                long damage  = (long) diff * HP_MULTIPLIER;
                long pingAdjustMs = PhantomConfig.getLowPing() / 2L;
                long elapsed = phaseStartMs >= 0
                        ? Math.max(0L, System.currentTimeMillis() - phaseStartMs - pingAdjustMs)
                        : 0;

                client.player.sendSystemMessage(Component.literal(
                        String.format("§f[PhantomAddons]§r §fSomeone pulled for %s%s §fdamage at §a%s§f.",
                                pullColor(diff),
                                formatDamage(damage),
                                formatElapsed(elapsed))
                ));
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    private static AbstractCubeMob findKuudra(Minecraft mc) {
        if (mc.level == null) return null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof AbstractCubeMob slime && slime.getSize() == 30)
                return slime;
        }
        return null;
    }

    private static String pullColor(int diff) {
        if (diff <= 4_166) return "§c";   // < ~40M
        if (diff <= 7_291) return "§e";   // 40–70M
        return "§a";                       // > 70M
    }

    private static String formatDamage(long n) {
        if (n >= 1_000_000_000L) return String.format("%.1fB", n / 1_000_000_000.0);
        if (n >= 1_000_000L)     return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000L)         return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    private static String formatElapsed(long ms) {
        long secs   = ms / 1_000;
        long millis = ms % 1_000;
        return String.format("%d.%03ds", secs, millis);
    }
}
