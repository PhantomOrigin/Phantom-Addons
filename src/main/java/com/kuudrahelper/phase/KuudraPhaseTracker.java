package com.kuudrahelper.phase;

import com.kuudrahelper.utils.KuudraTierDetector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

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
        ClientReceiveMessageEvents.ALLOW_GAME.register((text, overlay) -> {
            handle(text.getString());
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            // T5 only: dropping below Y=10 in DPS/SKIP signals transition to kill phase
            int tier = KuudraTierDetector.getTier();
            if ((tier == 5 || tier == 0) &&
                    (currentPhase == Phase.DPS || currentPhase == Phase.SKIP) &&
                    client.player.getY() < 10.0) {
                setPhase(Phase.BOSS);
            }
        });
    }

    public static String stripFormatting(String msg) {
        return msg.replaceAll("§[0-9a-fk-or]", "");
    }

    private static void handle(String msg) {
        msg = stripFormatting(msg);

        if (msg.equals("[NPC] Elle: Talk with me to begin!")) {
            setPhase(Phase.NONE);
            runActive = false;
            return;
        }

        if (msg.equals("[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!")) {
            runActive = true;
            setPhase(Phase.SUPPLIES);
            return;
        }

        if (msg.equals("[NPC] Elle: OMG! Great work collecting my supplies!")) {
            setPhase(Phase.BUILD);
            return;
        }

        if (msg.equals("[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!")) {
            int tier = KuudraTierDetector.getTier();
            setPhase((tier == 1 || tier == 2) ? Phase.BOSS : Phase.EATEN);
            return;
        }

        if (runActive && msg.contains("has been eaten by Kuudra!")) {
            setPhase(Phase.STUN);
            return;
        }

        if (runActive && msg.contains("destroyed one of Kuudra's pods!")) {
            setPhase(Phase.DPS);
            return;
        }

        if (msg.equals("[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!")) {
            setPhase(Phase.SKIP);
            return;
        }

        if (msg.contains("KUUDRA DOWN") || msg.contains("DEFEAT KUUDRA")) {
            setPhase(Phase.END);
            runActive = false;
        }
    }

    private static void setPhase(Phase newPhase) {
        if (newPhase == currentPhase) return;
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