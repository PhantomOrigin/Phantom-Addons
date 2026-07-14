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
        SKIP,
        BOSS,
        END
    }

    private static Phase   currentPhase = Phase.NONE;
    private static boolean runActive    = false;

    public static void init() {
        // Listen on GAME events — handles both regular chat and action bar/overlay
        // system messages. Hypixel routes some phase triggers (e.g. "Ballista ready",
        // "eaten by Kuudra") as overlay system messages, so we must NOT filter by overlay.
        ClientReceiveMessageEvents.ALLOW_GAME.register((text, overlay) -> {
            handle(text.getString());
            return true;
        });

        // Also listen on CHAT events — Hypixel routes some kill-feed messages
        // (e.g. "destroyed one of Kuudra's pods!") through the signed-chat pipeline.
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

        // Debug: log every message that contains kuudra-relevant keywords so we can
        // see exactly what text the server is sending for phase transitions.
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

        // ── EATEN / BOSS (T1/T2) ─────────────────────────────────────────────────
        if (msg.contains("The Ballista is finally ready")) {
            int tier = KuudraTierDetector.getTier();
            setPhase((tier == 1 || tier == 2) ? Phase.BOSS : Phase.EATEN);
            return;
        }

        // ── STUN ─────────────────────────────────────────────────────────────────
        // "has been eaten by Kuudra!" — always set runActive here so mid-run joins work
        if (msg.contains("has been eaten by Kuudra!")) {
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

        // ── SKIP ─────────────────────────────────────────────────────────────────
        if (msg.contains("SURELY THAT'S IT") || msg.contains("POW! SURELY")) {
            runActive = true;
            setPhase(Phase.SKIP);
            checkBossYTransition();
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
        // Any player (not just the local one) dropping below y10 during SKIP should
        // trigger BOSS instantly — previously only checked the local player, so a run
        // where someone else fell first (while you stayed up) never transitioned at all.
        for (Player p : mc.level.players()) {
            if (p.getY() < 10.0) {
                setPhase(Phase.BOSS);
                return;
            }
        }
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
