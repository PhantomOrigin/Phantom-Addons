package com.kuudrahelper.features;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.NotificationHud;
import com.kuudrahelper.KuudraHelperMod;
import net.minecraft.client.Minecraft;

public final class SoloDetector {

    private static final int    SOLO_THRESHOLD = 3;
    private static final String TRIGGER        = "was final killed";

    private static boolean active     = false;
    private static int     finalKills = 0;
    private static boolean announced  = false;

    private SoloDetector() {}

    public static void onPhaseStart() {
        active     = true;
        finalKills = 0;
        announced  = false;
        KuudraHelperMod.LOGGER.info("[SoloDetector] Started tracking final kills");
    }

    public static void onPhaseEnd() {
        active     = false;
        finalKills = 0;
        announced  = false;
    }

    public static void onChat(String raw) {
        if (!active || !KuudraConfig.isSoloDetectorEnabled()) return;
        if (announced) return;

        String clean = raw.replaceAll("§[0-9a-fk-orA-FK-OR]", "").toLowerCase();

        if (clean.contains(TRIGGER)) {
            finalKills++;
            KuudraHelperMod.LOGGER.info("[SoloDetector] Final kill detected ({}/{})",
                    finalKills, SOLO_THRESHOLD);

            if (finalKills >= SOLO_THRESHOLD) {
                announced = true;
                triggerSoloAlert();
            }
        }
    }

    private static void triggerSoloAlert() {
        if (KuudraConfig.isSoloNotifyEnabled()) {
            NotificationHud.show("§cSOLO!", 3000);
        }

        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player == null) return;
            client.player.connection.sendCommand("pc [Phantom] SOLO!");
            KuudraHelperMod.LOGGER.info("[SoloDetector] Sent /pc [Phantom] SOLO!");
        });
    }

    public static boolean isActive()      { return active; }
    public static int     getFinalKills() { return finalKills; }
    public static boolean isAnnounced()   { return announced; }
}
