package com.phantomaddons.features.misckuudra.chesttracking;

import com.phantomaddons.utils.TextUtil;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.PhantomAddons;
import com.phantomaddons.features.misckuudra.profittracker.ChestValueOverlay;
import com.phantomaddons.utils.KuudraTierDetector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class ChestTracker {

    private static final String WIN    = "KUUDRA DOWN";
    private static final String DEFEAT = "DEFEAT";
    private static final String PAID_CHEST = "PAID CHEST REWARDS";
    private static final String FREE_CHEST = "FREE CHEST REWARDS";

    private static final long TEN_MINUTES_MS = 10L * 60 * 1000;

    private static long lastKuudraHollowMs = 0L;

    private static int total   = 0;
    private static int success = 0;
    private static int fail    = 0;

    private static boolean pendingPcMessage  = false;
    private static long    lastChestOpenMs  = 0;
    private static final long CHEST_SYNC_COOLDOWN_MS = 6_000;


    private ChestTracker() {}

    public static void init() {
        total   = PhantomConfig.getChestTotal();
        success = PhantomConfig.getChestSuccess();
        fail    = PhantomConfig.getChestFail();

        if (total > 60) {
            int excess = total - 60;
            total = 60;
            success = Math.max(0, success - excess);
            save();
        }

        if (total == 0) {
            success = 0;
            fail    = 0;
            save();
        }

        registerChatListener();
        registerTickListener();
        registerHud();
    }

    private static void registerChatListener() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((text, overlay) -> {
            if (!overlay) handleMessage(text.getString());
            return true;
        });
    }

    private static void handleMessage(String raw) {
        String clean = TextUtil.stripColor(raw).trim();

        if (clean.contains(WIN)) {
            success++;
            total = Math.min(total + 1, 60);
            save();
            PhantomAddons.LOGGER.info("[ChestTracker] Win  total={} s={} f={}", total, success, fail);
            if (total == 59) pendingPcMessage = true;
            return;
        }

        if (clean.contains(DEFEAT)) {
            fail++;
            total = Math.min(total + 1, 60);
            save();
            PhantomAddons.LOGGER.info("[ChestTracker] Fail total={} s={} f={}", total, success, fail);
            return;
        }

        if (clean.contains(PAID_CHEST))  { handlePaidChest();  return; }
        if (clean.contains(FREE_CHEST))  { handleFreeChest(); }
    }

    private static void handlePaidChest() {
        ChestValueOverlay.commitOnPaidChest();
        if (total <= 0) return;
        lastChestOpenMs = System.currentTimeMillis();
        total--;
        if (success > 0) success--;

        if (total == 0) {
            success = 0;
            fail    = 0;
        }

        save();
        PhantomAddons.LOGGER.info("[ChestTracker] Paid chest → total={} s={} f={}", total, success, fail);
    }

    private static void handleFreeChest() {
        if (total <= 0) return;
        lastChestOpenMs = System.currentTimeMillis();
        total--;

        if (total == 0) {
            success = 0;
            fail    = 0;
            save();
        }

        if (fail > 0) {
            fail--;
        } else {
            if (success > 0) success--;
        }
        save();
        PhantomAddons.LOGGER.info("[ChestTracker] Free chest → total={} s={} f={}", total, success, fail);
    }

    public static void syncFromTabList(int tabTotal) {
        if (tabTotal == total) return;
        if (System.currentTimeMillis() - lastChestOpenMs < CHEST_SYNC_COOLDOWN_MS) return;

        PhantomAddons.LOGGER.info(
                "[ChestTracker] Tab sync: tab={} tracker={}", tabTotal, total);

        if (tabTotal < total) {
            int diff = total - tabTotal;
            total = tabTotal;
            success = Math.max(0, success - diff);
        } else {
            total = tabTotal;
        }
        if (total == 0) { success = 0; fail = 0; }
        save();
    }

    private static void registerTickListener() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KuudraTierDetector.isInKuudraHollow()) {
                lastKuudraHollowMs = System.currentTimeMillis();
            }
        });
    }

    private static void registerHud() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "chest_tracker"),
                ChestTracker::renderHud);
    }

    private static void renderHud(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        if (!shouldShowHud()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) return;

        Font tr = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int x = screenW / 2 + 91 + 6;
        int y = screenH - 22 + (22 - tr.lineHeight) / 2;

        drawLine(ctx, tr, x, y);
    }

    private static void drawLine(GuiGraphicsExtractor ctx, Font tr, int x, int y) {
        int cx = x;
        cx = seg(ctx, tr, cx, y, String.valueOf(total),   0xFFFFFF);
        cx = seg(ctx, tr, cx, y, " (",                    0xAAAAAA);
        cx = seg(ctx, tr, cx, y, String.valueOf(success),  0x55FF55);
        cx = seg(ctx, tr, cx, y, " / ",                   0xAAAAAA);
        cx = seg(ctx, tr, cx, y, String.valueOf(fail),     0xFF5555);
        seg(ctx, tr, cx, y, ")",                      0xAAAAAA);
    }

    private static int seg(GuiGraphicsExtractor ctx, Font tr,
                           int x, int y, String text, int color) {
        ctx.text(tr, text, x, y, color | 0xFF000000, true);
        return x + tr.width(text);
    }

    private static boolean shouldShowHud() {
        if (!PhantomConfig.isChestTrackerVisible()) return false;
        if (total == 0) return false; // nothing to show yet

        return KuudraTierDetector.isInKuudraHollow()
                || KuudraTierDetector.isInDungeonHub()
                || KuudraTierDetector.isInForgottenSkull()
                || (lastKuudraHollowMs > 0
                && System.currentTimeMillis() - lastKuudraHollowMs < TEN_MINUTES_MS);
    }

    private static void save() {
        PhantomConfig.setChestCounts(total, success, fail);
    }

    public static int  getTotal()           { return total; }
    public static int  getSuccess()         { return success; }
    public static int  getFail()            { return fail; }
    public static boolean isPendingPc()     { return pendingPcMessage; }
    public static void clearPendingPc()     { pendingPcMessage = false; }

    public static void reset() {
        total   = 0;
        success = 0;
        fail    = 0;
        pendingPcMessage = false;
        lastKuudraHollowMs = 0L;
        save();
    }
}
