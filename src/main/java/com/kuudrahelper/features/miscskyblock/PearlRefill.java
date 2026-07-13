package com.kuudrahelper.features.miscskyblock;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/*
This feature is excluded from the standard version of the mod
 */
public final class PearlRefill {

    private static final int  TICK_INTERVAL       = 3;
    private static final int  THRESHOLD_URGENT    = 4;
    private static final int  THRESHOLD_NORMAL    = 14;
    private static final int  TARGET_COUNT        = 16;
    private static final long MIN_ATTEMPT_MS      = 500L;
    private static final long NORMAL_SUPPRESS_MS  = 3000L;
    private static final long REPLY_WINDOW_MS     = 2000L;
    private static final long COOLDOWN_BACKOFF_MS = 1200L;

    private static final String MOVED_PREFIX = "Moved";
    private static final String MOVED_SUFFIX = " Ender Pearl from your Sacks to your inventory.";
    private static final String COOLDOWN_MSG =
            "Command Failed: This command is on cooldown! Try again in about a second!";

    private enum State { IDLE, WAITING_CHAT, COOLDOWN }

    private static State   state           = State.IDLE;
    private static long    lastInteractMs  = 0L;
    private static long    lastAttemptMs   = 0L;
    private static long    backoffUntilMs  = 0L;
    private static int     preRequestCount = -1;
    private static int     tickMod         = 0;
    private static boolean prevUseDown     = false;

    private PearlRefill() {}

    public static void register() {
        if (!com.kuudrahelper.Edition.CURRENT.fullFeatureSet) return; // not included in this edition
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!KuudraConfig.isPearlRefillEnabled()) return;
            if (state != State.WAITING_CHAT) return;

            long now = System.currentTimeMillis();
            if (now - lastAttemptMs > REPLY_WINDOW_MS) return;

            String text = message.getString().replaceAll("§[0-9a-fk-orA-FK-OR]", "");
            if (text.startsWith(MOVED_PREFIX) && text.endsWith(MOVED_SUFFIX)) {
                resetWaiting();
                state = State.IDLE;
            } else if (text.equals(COOLDOWN_MSG)) {
                resetWaiting();
                state = State.COOLDOWN;
                backoffUntilMs = now + COOLDOWN_BACKOFF_MS;
            }
        });
    }

    public static void reset() {
        state           = State.IDLE;
        lastInteractMs  = 0L;
        lastAttemptMs   = 0L;
        backoffUntilMs  = 0L;
        preRequestCount = -1;
        tickMod         = 0;
        prevUseDown     = false;
    }

    public static void tick(Minecraft mc) {
        if (!com.kuudrahelper.Edition.CURRENT.fullFeatureSet) return; // not included in this edition
        if (!KuudraConfig.isPearlRefillEnabled()) return;
        if (!KuudraConfig.isPearlRefillOutsideKuudraEnabled() && !KuudraPhaseTracker.isRunActive()) return;
        if (mc.player == null || mc.getConnection() == null) return;

        trackManualInteract(mc);

        tickMod++;
        if (tickMod < TICK_INTERVAL) return;
        tickMod = 0;

        long now = System.currentTimeMillis();

        if (state == State.COOLDOWN) {
            if (now >= backoffUntilMs) state = State.IDLE;
            else return;
        }

        if (state == State.WAITING_CHAT) {
            if (now - lastAttemptMs > REPLY_WINDOW_MS) {
                int current = getPearlCount(mc);
                boolean increased = preRequestCount >= 0 && current > preRequestCount;
                resetWaiting();
                state = State.IDLE;
                if (!increased) lastAttemptMs = now;
            }
            return;
        }

        // IDLE
        int count = getPearlCount(mc);
        if (count <= 0) return;

        int urgentThreshold = THRESHOLD_URGENT + (KuudraConfig.getLowPing() / 100);
        boolean urgent = count < urgentThreshold;
        boolean normal = count < THRESHOLD_NORMAL
                && (now - lastAttemptMs) > NORMAL_SUPPRESS_MS
                && (now - lastInteractMs) > NORMAL_SUPPRESS_MS;
        if (!urgent && !normal) return;
        if ((now - lastAttemptMs) < MIN_ATTEMPT_MS) return;

        int needed = TARGET_COUNT - count;
        if (needed < THRESHOLD_NORMAL) lastInteractMs = now;

        preRequestCount = count;
        lastAttemptMs   = now;
        state           = State.WAITING_CHAT;
        mc.getConnection().sendCommand("gfs ender_pearl " + needed);
    }

    private static void trackManualInteract(Minecraft mc) {
        if (mc.player == null || mc.options == null) { prevUseDown = false; return; }
        boolean down = mc.options.keyUse.isDown();
        if (down && !prevUseDown) {
            ItemStack stack = mc.player.getMainHandItem();
            if (!stack.isEmpty() && stack.is(Items.ENDER_PEARL) && stack.getCount() < TARGET_COUNT) {
                lastInteractMs = System.currentTimeMillis();
            }
        }
        prevUseDown = down;
    }

    private static void resetWaiting() {
        preRequestCount = -1;
    }

    private static int getPearlCount(Minecraft mc) {
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(Items.ENDER_PEARL)) {
                return stack.getCount();
            }
        }
        return 0;
    }
}
