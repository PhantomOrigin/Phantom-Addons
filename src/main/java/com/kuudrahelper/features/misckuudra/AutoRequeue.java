package com.kuudrahelper.features.misckuudra;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.KuudraHelperMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

public final class AutoRequeue {

    private static final String ELLE_FISHING =
            "[npc] elle: okay adventurers, i will go and fish up kuudra!";
    private static final String REQUEUE_CONFIRM = "you have been re-queued!";
    private static final String REQUEUE_CLICK   = "click here to re-queue into kuudra's hollow!";
    private static final long BACKUP_REQUEUE_DELAY_MS = 1_500L;
    private static final int  MAX_BACKUP_RETRIES  = 5;

    private static boolean requeued = false;
    private static long pendingBackupRequeueAtMs = -1L;
    private static int  backupRetriesLeft = 0;

    private AutoRequeue() {}

    public static void reset() {
        KuudraHelperMod.LOGGER.info("[AutoRequeue] reset() — requeued=false, backup timer cleared");
        requeued = false;
        pendingBackupRequeueAtMs = -1L;
        backupRetriesLeft = 0;
    }

    public static void trigger() {
        if (!KuudraConfig.isAutoRequeueEnabled()) {
            KuudraHelperMod.LOGGER.info("[AutoRequeue] trigger() called but feature is disabled — skipping");
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            KuudraHelperMod.LOGGER.info("[AutoRequeue] trigger() called but player/connection is null — skipping");
            return;
        }
        KuudraHelperMod.LOGGER.info("[AutoRequeue] trigger() -> sending /instancerequeue (phase-end trigger)");
        mc.execute(() -> mc.getConnection().sendCommand("instancerequeue"));
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingBackupRequeueAtMs < 0) return;
            if (!KuudraConfig.isAutoRequeueEnabled()) {
                KuudraHelperMod.LOGGER.info("[AutoRequeue] backup timer cancelled — feature disabled");
                pendingBackupRequeueAtMs = -1L;
                return;
            }
            if (System.currentTimeMillis() < pendingBackupRequeueAtMs) return;
            if (client.player == null || client.getConnection() == null) return;
            pendingBackupRequeueAtMs = -1L;
            if (backupRetriesLeft > 0) {
                backupRetriesLeft--;
                pendingBackupRequeueAtMs = System.currentTimeMillis() + BACKUP_REQUEUE_DELAY_MS;
                KuudraHelperMod.LOGGER.info("[AutoRequeue] backup timer fired -> sending /instancerequeue ({} retries left)", backupRetriesLeft);
            } else {
                KuudraHelperMod.LOGGER.info("[AutoRequeue] backup timer fired -> sending /instancerequeue (final attempt)");
            }
            client.execute(() -> client.getConnection().sendCommand("instancerequeue"));
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!KuudraConfig.isAutoRequeueEnabled()) return;
            String raw   = message.getString().replaceAll("[§&][0-9a-fk-orA-FK-OR]", "").trim();
            String lower = raw.toLowerCase().trim();

            if (lower.equals(ELLE_FISHING)) {
                KuudraHelperMod.LOGGER.info("[AutoRequeue] saw Elle-fishing line -> reset()");
                reset();
                return;
            }

            if (lower.contains(REQUEUE_CONFIRM)) {
                KuudraHelperMod.LOGGER.info("[AutoRequeue] saw requeue confirmation -> requeued=true");
                requeued = true;
                pendingBackupRequeueAtMs = -1L;
                backupRetriesLeft = 0;
                return;
            }

            if (requeued) return;

            if (lower.contains(REQUEUE_CLICK) && pendingBackupRequeueAtMs < 0) {
                KuudraHelperMod.LOGGER.info("[AutoRequeue] saw 'click here to re-queue' line -> backup timer armed ({}ms, {} retries)", BACKUP_REQUEUE_DELAY_MS, MAX_BACKUP_RETRIES);
                backupRetriesLeft = MAX_BACKUP_RETRIES;
                pendingBackupRequeueAtMs = System.currentTimeMillis() + BACKUP_REQUEUE_DELAY_MS;
            }
        });
    }
}
