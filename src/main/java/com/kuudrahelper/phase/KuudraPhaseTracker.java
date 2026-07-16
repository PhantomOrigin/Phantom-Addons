package com.kuudrahelper.phase;

import com.kuudrahelper.KuudraHelperMod;
import com.kuudrahelper.utils.KuudraTierDetector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class KuudraPhaseTracker {

    public enum Phase {
        NONE,
        SUPPLIES,
        BUILD,
        EATEN,
        STUN,
        DPS,
        SKIP,   // T5 only — falling/teleporting into the boss arena, before BOSS
        BOSS,   // T5 only — the actual timed bonemerang/rend kill phase
        KILL,   // T1/T2 only — straight from BUILD, no eaten/stun/dps/skip mechanic
        DEATH,  // T3/T4 only — straight from DPS on the "POW! SURELY" message, no skip/fall
        END
    }

    private static Phase   currentPhase = Phase.NONE;
    private static boolean runActive    = false;

    private static long lastSkipDebugLogMs = 0L;
    private static final long SKIP_DEBUG_LOG_INTERVAL_MS = 1000L;

    public static void init() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((text, overlay) -> {
            handle(text.getString());
            return true;
        });

        ClientReceiveMessageEvents.CHAT.register((text, signed, sender, params, ts) ->
            handle(text.getString())
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            checkBossYTransition();
        });
    }

    public static String stripFormatting(String msg) {
        return msg.replaceAll("§[0-9a-fk-or]", "");
    }

    private static void handle(String raw) {
        String msg = stripFormatting(raw);

        if (msg.contains("Kuudra") || msg.contains("Ballista") || msg.contains("supplies")
                || msg.contains("fish up") || msg.contains("eaten") || msg.contains("pods")
                || msg.contains("Elle") || msg.contains("begin")) {
            KuudraHelperMod.LOGGER.info("[PhaseTracker] msg='{}'", msg);
        }

        // ── Run reset ─────────────────────────────────────────────────────────────
        if (msg.contains("Talk with me to begin")) {
            runActive = false;
            setPhase(Phase.NONE);
            return;
        }

        // ── SUPPLIES ─────────────────────────────────────────────────────────────
        if (msg.contains("I will go and fish up Kuudra")) {
            runActive = true;
            setPhase(Phase.SUPPLIES);
            return;
        }

        // ── BUILD ─────────────────────────────────────────────────────────────────
        if (msg.contains("Great work collecting my supplies")) {
            setPhase(Phase.BUILD);
            return;
        }

        // ── EATEN / KILL (T1/T2) ─────────────────────────────────────────────────
        if (msg.contains("The Ballista is finally ready")) {
            int tier = KuudraTierDetector.getTier();
            setPhase((tier == 1 || tier == 2) ? Phase.KILL : Phase.EATEN);
            return;
        }

        // ── STUN ─────────────────────────────────────────────────────────────────
        if (msg.contains("has been eaten by Kuudra!") && !msg.startsWith("Elle ") && currentPhase == Phase.EATEN) {
            runActive = true;
            setPhase(Phase.STUN);
            return;
        }

        // ── DPS ──────────────────────────────────────────────────────────────────
        if (msg.contains("destroyed one of Kuudra's pods!")) {
            runActive = true;
            setPhase(Phase.DPS);
            return;
        }

        // ── SKIP (T5) / DEATH (T1-T4) ────────────────────────────────────────────
        if (msg.contains("SURELY THAT'S IT") || msg.contains("POW! SURELY")) {
            runActive = true;
            int tier = KuudraTierDetector.getTier();
            if (tier >= 1 && tier <= 4) {
                setPhase(Phase.DEATH);
            } else {
                setPhase(Phase.SKIP);
                checkBossYTransition();
            }
            return;
        }

        // ── END ───────────────────────────────────────────────────────────────────
        if (msg.contains("KUUDRA DOWN") || msg.contains("DEFEAT KUUDRA")
                || msg.contains("KUUDRA HAS BEEN DEFEATED")) {
            runActive = false;
            setPhase(Phase.END);
        }
    }

    private static void checkBossYTransition() {
        if (currentPhase != Phase.SKIP) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        logSkipDebug(mc);

        for (Player p : mc.level.players()) {
            if (p.getY() < 10.0) {
                setPhase(Phase.BOSS);
                return;
            }
        }
    }
    
    private static void logSkipDebug(Minecraft mc) {
        if (!com.kuudrahelper.KuudraConfig.isDeveloperFeaturesEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastSkipDebugLogMs < SKIP_DEBUG_LOG_INTERVAL_MS) return;
        lastSkipDebugLogMs = now;

        StringBuilder sb = new StringBuilder();
        for (Player p : mc.level.players()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(p.getScoreboardName()).append("=").append(String.format("%.2f", p.getY()));
        }
        KuudraHelperMod.LOGGER.info("[PhaseTracker] SKIP y-check tick: {}", sb);
    }

    private static void setPhase(Phase newPhase) {
        if (newPhase == currentPhase) return;
        KuudraHelperMod.LOGGER.info("[PhaseTracker] phase {} -> {}", currentPhase, newPhase);
        currentPhase = newPhase;
        KuudraPhaseEvents.onPhaseChanged(newPhase);
    }

    public static void reset() {
        currentPhase = Phase.NONE;
    }

    public static void forcePhase(Phase phase) {
        runActive = (phase != Phase.NONE && phase != Phase.END);
        currentPhase = phase;
        KuudraPhaseEvents.onPhaseChanged(phase);
    }

    public static Phase   getPhase()      { return currentPhase; }
    public static boolean is(Phase phase) { return currentPhase == phase; }
    public static boolean isRunActive()   { return runActive; }
}
