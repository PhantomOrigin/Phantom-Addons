package com.kuudrahelper.features;

import com.kuudrahelper.KuudraConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

public final class AutoRequeue {

    private static final String ELLE_FISHING =
            "[npc] elle: okay adventurers, i will go and fish up kuudra!";
    private static final String REQUEUE_CONFIRM = "you have been re-queued!";
    private static final String REQUEUE_CLICK   = "click here to re-queue into kuudra's hollow!";
    private static final long BACKUP_REQUEUE_DELAY_MS = 1_500L;

    private static boolean requeued = false;
    private static long pendingBackupRequeueAtMs = -1L;

    private AutoRequeue() {}

    public static void reset() {
        requeued = false;
        pendingBackupRequeueAtMs = -1L;
    }

    public static void trigger() {
        if (!KuudraConfig.isAutoRequeueEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        mc.execute(() -> mc.getConnection().sendCommand("instancerequeue"));
    }

    public static void onServerJoin() {
        requeued = true;
        pendingBackupRequeueAtMs = -1L;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingBackupRequeueAtMs < 0) return;
            if (!KuudraConfig.isAutoRequeueEnabled()) {
                pendingBackupRequeueAtMs = -1L;
                return;
            }
            if (System.currentTimeMillis() < pendingBackupRequeueAtMs) return;
            if (client.player == null || client.getConnection() == null) return;
            pendingBackupRequeueAtMs = -1L;
            client.execute(() -> client.getConnection().sendCommand("instancerequeue"));
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!KuudraConfig.isAutoRequeueEnabled()) return;

            String raw   = message.getString().replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
            String lower = raw.toLowerCase().trim();

            if (raw.equals(ELLE_FISHING)) {
                reset();
                return;
            }

            if (lower.contains(REQUEUE_CONFIRM)) {
                requeued = true;
                return;
            }

            if (requeued) return;

            if (raw.contains(REQUEUE_CLICK)) {
                requeued = true;
                pendingBackupRequeueAtMs = System.currentTimeMillis();
            }
        });
    }
}
