package com.phantomaddons.features.boss;

import com.phantomaddons.utils.TextUtil;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.misckuudra.NotificationHud;
import com.phantomaddons.PhantomAddons;
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
        PhantomAddons.LOGGER.info("[SoloDetector] Started tracking final kills");
    }

    public static void onPhaseEnd() {
        active     = false;
        finalKills = 0;
        announced  = false;
    }

    public static void onChat(String raw) {
        if (!active || !PhantomConfig.isSoloDetectorEnabled()) return;
        if (announced) return;

        String clean = TextUtil.stripColor(raw).toLowerCase();

        if (clean.contains(TRIGGER)) {
            finalKills++;
            PhantomAddons.LOGGER.info("[SoloDetector] Final kill detected ({}/{})",
                    finalKills, SOLO_THRESHOLD);

            if (finalKills >= SOLO_THRESHOLD) {
                announced = true;
                triggerSoloAlert();
            }
        }
    }

    private static void triggerSoloAlert() {
        if (PhantomConfig.isSoloNotifyEnabled()) {
            NotificationHud.show("§cSOLO!", 3000);
            PhantomConfig.playNotificationSound(PhantomConfig.SOUND_SOLO);
        }

        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player == null) return;
            client.player.connection.sendCommand("pc [Phantom] SOLO!");
            PhantomAddons.LOGGER.info("[SoloDetector] Sent /pc [Phantom] SOLO!");
        });
    }

    public static boolean isActive()      { return active; }
    public static int     getFinalKills() { return finalKills; }
    public static boolean isAnnounced()   { return announced; }
}
