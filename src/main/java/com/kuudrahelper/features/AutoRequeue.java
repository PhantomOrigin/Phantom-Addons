package com.kuudrahelper.features;

import com.kuudrahelper.KuudraConfig;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

public final class AutoRequeue {

    private static final String ELLE_FISHING =
            "[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!";
    private static final String REQUEUE_CONFIRM = "you have been re-queued!";
    private static final String REQUEUE_CLICK   = "Click HERE to re-queue into Kuudra's Hollow!";

    private static boolean requeued = false;

    private AutoRequeue() {}

    public static void reset() {
        requeued = false;
    }

    public static void trigger() {
        if (!KuudraConfig.isAutoRequeueEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        mc.execute(() -> mc.getConnection().sendCommand("instancerequeue"));
    }

    public static void onServerJoin() {
        requeued = true;
    }

    public static void register() {
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
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.getConnection() == null) return;
                mc.execute(() -> mc.getConnection().sendCommand("instancerequeue"));
            }
        });
    }
}
