package com.kuudrahelper.features;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

public final class PearlRefill {

    private static final int  TICK_INTERVAL        = 3;
    private static final int  THRESHOLD_NORMAL      = 14;
    private static final int  THRESHOLD_URGENT      = 4;
    private static final long MIN_ATTEMPT_MS        = 500L;
    private static final long NORMAL_COOLDOWN_MS    = 3000L;
    private static final long REPLY_WINDOW_MS       = 2000L;
    private static final long COOLDOWN_BACKOFF_MS   = 1200L;

    private static final String MOVED_PREFIX  = "Moved";
    private static final String MOVED_SUFFIX  = "Ender Pearl from your Sacks to your inventory.";
    private static final String COOLDOWN_MSG  =
            "Command Failed: This command is on cooldown! Try again in about a second!";

    private enum State { IDLE, WAITING_CHAT, COOLDOWN }

    private static State state              = State.IDLE;
    private static long  lastAttemptMs      = 0L;
    private static long  replyWindowEndMs   = 0L;
    private static long  backoffUntilMs     = 0L;
    private static int   preRequestCount    = 0;
    private static int   tickMod            = 0;

    private PearlRefill() {}

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (state != State.WAITING_CHAT) return;
            String text = message.getString().replaceAll("§[0-9a-fk-orA-FK-OR]", "");
            if (text.startsWith(MOVED_PREFIX) && text.endsWith(MOVED_SUFFIX)) {
                state = State.IDLE;
            } else if (text.equals(COOLDOWN_MSG)) {
                state = State.COOLDOWN;
                backoffUntilMs = System.currentTimeMillis() + COOLDOWN_BACKOFF_MS;
            }
        });
    }

    public static void reset() {
        state           = State.IDLE;
        lastAttemptMs   = 0L;
        replyWindowEndMs = 0L;
        backoffUntilMs  = 0L;
        preRequestCount = 0;
        tickMod         = 0;
    }

    public static void tick(Minecraft mc) {
        if (!KuudraConfig.isPearlRefillEnabled()) return;
        if (!KuudraPhaseTracker.isRunActive()) return;
        if (mc.player == null || mc.getConnection() == null) return;

        tickMod++;
        if (tickMod < TICK_INTERVAL) return;
        tickMod = 0;

        long now = System.currentTimeMillis();

        if (state == State.COOLDOWN) {
            if (now >= backoffUntilMs) state = State.IDLE;
            else return;
        }

        if (state == State.WAITING_CHAT) {
            if (now > replyWindowEndMs) {
                // Timed out — check if count increased
                int current = countPearls(mc);
                if (current > preRequestCount) {
                    state = State.IDLE;
                } else {
                    lastAttemptMs = now;
                    state = State.IDLE;
                }
            }
            return;
        }

        // IDLE
        int count = countPearls(mc);
        if (count == 0) return;

        boolean urgent = count < THRESHOLD_URGENT;
        boolean normal = count < THRESHOLD_NORMAL && (now - lastAttemptMs) >= NORMAL_COOLDOWN_MS;
        if (!urgent && !normal) return;
        if ((now - lastAttemptMs) < MIN_ATTEMPT_MS) return;

        int needed = 16 - count;
        preRequestCount  = count;
        lastAttemptMs    = now;
        replyWindowEndMs = now + REPLY_WINDOW_MS;
        state            = State.WAITING_CHAT;
        mc.getConnection().sendCommand("gfs ender_pearl " + needed);
    }

    private static int countPearls(Minecraft mc) {
        int total = 0;
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !stack.is(Items.ENDER_PEARL)) continue;
            String name = stack.getHoverName().getString()
                    .replaceAll("§[0-9a-fk-orA-FK-OR]", "");
            if (name.equals("Ender Pearl")) total += stack.getCount();
        }
        return total;
    }
}
