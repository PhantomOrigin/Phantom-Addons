package com.phantomaddons.features.misckuudra;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.PhantomAddons;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;

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
    private static int  pendingTriggerTicks = -1;
    private static boolean triggerArmed = false;

    private static boolean tempDisabled = false;

    private AutoRequeue() {}

    public static void reset() {
        PhantomAddons.LOGGER.info("[AutoRequeue] reset() — requeued=false, backup timer cleared");
        requeued = false;
        pendingBackupRequeueAtMs = -1L;
        backupRetriesLeft = 0;
        pendingTriggerTicks = -1;
        triggerArmed = false;
    }

    private static boolean isInvisible(LocalPlayer player) {
        return player.hasEffect(MobEffects.INVISIBILITY);
    }

    public static void resetSession() {
        reset();
        tempDisabled = false;
    }

    public static void setTempDisabled(boolean disabled) {
        tempDisabled = disabled;
    }

    public static boolean isTempDisabled() { return tempDisabled; }

    private static boolean isActive() {
        return PhantomConfig.isAutoRequeueEnabled() && !tempDisabled;
    }

    public static void trigger() {
        if (!isActive()) {
            PhantomAddons.LOGGER.info("[AutoRequeue] trigger() called but feature is disabled — skipping");
            return;
        }
        PhantomAddons.LOGGER.info("[AutoRequeue] trigger() -> scheduling /instancerequeue for next tick (phase-end trigger)");
        pendingTriggerTicks = 1;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingTriggerTicks > 0) {
                pendingTriggerTicks--;
                if (pendingTriggerTicks == 0) {
                    pendingTriggerTicks = -1;
                    triggerArmed = true;
                }
            }

            if (triggerArmed) {
                if (!isActive()) {
                    PhantomAddons.LOGGER.info("[AutoRequeue] deferred trigger cancelled — feature disabled");
                    triggerArmed = false;
                } else if (client.player == null || client.getConnection() == null) {
                } else if (isInvisible(client.player)) {
                } else {
                    triggerArmed = false;
                    PhantomAddons.LOGGER.info("[AutoRequeue] deferred trigger fired -> sending /instancerequeue");
                    client.execute(() -> client.getConnection().sendCommand("instancerequeue"));
                }
            }

            if (pendingBackupRequeueAtMs < 0) return;
            if (!isActive()) {
                PhantomAddons.LOGGER.info("[AutoRequeue] backup timer cancelled — feature disabled");
                pendingBackupRequeueAtMs = -1L;
                return;
            }
            if (System.currentTimeMillis() < pendingBackupRequeueAtMs) return;
            if (client.player == null || client.getConnection() == null) return;
            if (isInvisible(client.player)) return;
            pendingBackupRequeueAtMs = -1L;
            if (backupRetriesLeft > 0) {
                backupRetriesLeft--;
                pendingBackupRequeueAtMs = System.currentTimeMillis() + BACKUP_REQUEUE_DELAY_MS;
                PhantomAddons.LOGGER.info("[AutoRequeue] backup timer fired -> sending /instancerequeue ({} retries left)", backupRetriesLeft);
            } else {
                PhantomAddons.LOGGER.info("[AutoRequeue] backup timer fired -> sending /instancerequeue (final attempt)");
            }
            client.execute(() -> client.getConnection().sendCommand("instancerequeue"));
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!PhantomConfig.isAutoRequeueEnabled()) return;
            String raw   = message.getString().replaceAll("[§&][0-9a-fk-orA-FK-OR]", "").trim();
            String lower = raw.toLowerCase().trim();

            if (lower.equals(ELLE_FISHING)) {
                PhantomAddons.LOGGER.info("[AutoRequeue] saw Elle-fishing line -> reset()");
                reset();
                return;
            }

            if (lower.contains(REQUEUE_CONFIRM)) {
                PhantomAddons.LOGGER.info("[AutoRequeue] saw requeue confirmation -> requeued=true");
                requeued = true;
                pendingBackupRequeueAtMs = -1L;
                backupRetriesLeft = 0;
                return;
            }

            if (requeued) return;

            if (lower.contains(REQUEUE_CLICK) && pendingBackupRequeueAtMs < 0) {
                PhantomAddons.LOGGER.info("[AutoRequeue] saw 'click here to re-queue' line -> backup timer armed ({}ms, {} retries)", BACKUP_REQUEUE_DELAY_MS, MAX_BACKUP_RETRIES);
                backupRetriesLeft = MAX_BACKUP_RETRIES;
                pendingBackupRequeueAtMs = System.currentTimeMillis() + BACKUP_REQUEUE_DELAY_MS;
            }
        });
    }
}
