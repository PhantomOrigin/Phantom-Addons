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

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!KuudraConfig.isAutoRequeueEnabled()) return;

            String raw   = message.getString().replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
            String lower = raw.toLowerCase().replace(" ", " ").trim();

            // New run started — reset so requeue fires again on the next end
            if (raw.equals(ELLE_FISHING)) {
                reset();
                return;
            }

            // Hypixel confirmed we were requeued — prevent double-send
            if (lower.contains(REQUEUE_CONFIRM)) {
                requeued = true;
                return;
            }

            if (requeued) return;

            boolean isEnd = lower.startsWith("kuudradown")
                    || lower.startsWith("defeat")
                    || raw.contains(REQUEUE_CLICK);

            if (isEnd) {
                requeued = true;
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.getConnection() == null) return;
                mc.execute(() -> mc.getConnection().sendCommand("instancerequeue"));
            }
        });
    }
}
