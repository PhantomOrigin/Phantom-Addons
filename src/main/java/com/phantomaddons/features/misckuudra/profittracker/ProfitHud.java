package com.phantomaddons.features.misckuudra.profittracker;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.phase.KuudraPhaseTracker;
import com.phantomaddons.utils.KuudraTierDetector;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class ProfitHud {

    private static final int LINE_H = 10;
    private static final int INDENT = 8;

    private static float lastHudX  = 0;
    private static float lastHudY  = 0;
    private static float lastScale = 1;

    private static int row1Y;
    private static int sessionX, sessionW;
    private static int allTimeX, allTimeW;
    private static int resetX,   resetW;

    private ProfitHud() {}

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "profit_hud"),
                ProfitHud::render);
    }

    public static boolean shouldShow() {
        if (!PhantomConfig.isProfitTrackerEnabled()) return false;
        if (PhantomConfig.isProfitTrackerForced()) return true;
        boolean inValidArea = KuudraTierDetector.isInKuudraHollow()
                           || KuudraTierDetector.isInDungeonHub()
                           || KuudraTierDetector.isInForgottenSkull();
        if (!inValidArea) return false;
        if (KuudraPhaseTracker.isRunActive() && !PhantomConfig.isProfitShowDuringRun()) return false;
        return true;
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker dt) {
        if (Minecraft.getInstance().screen != null) return;
        renderInScreen(ctx);
    }

    public static void renderInScreen(GuiGraphicsExtractor ctx) {
        if (!shouldShow()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        renderCore(ctx, mc, sw, sh);
    }


    public static boolean handleClick(int mx, int my) {
        if (!shouldShow()) return false;
        int lx = (int)((mx - lastHudX) / lastScale);
        int ly = (int)((my - lastHudY) / lastScale);

        if (ly >= row1Y && ly < row1Y + LINE_H) {
            if (lx >= sessionX && lx < sessionX + sessionW) { ProfitStore.setShowSession(true);  return true; }
            if (lx >= allTimeX && lx < allTimeX + allTimeW) { ProfitStore.setShowSession(false); return true; }
            if (lx >= resetX   && lx < resetX   + resetW)   { ProfitStore.resetSession();        return true; }
        }

        return false;
    }

    private static void renderCore(GuiGraphicsExtractor ctx, Minecraft mc, int screenW, int screenH) {
        float hudX  = PhantomConfig.getProfitHudX()    * screenW;
        float hudY  = PhantomConfig.getProfitHudY()    * screenH;
        float scale = PhantomConfig.getProfitHudScale();

        lastHudX  = hudX;
        lastHudY  = hudY;
        lastScale = scale;

        var m = ctx.pose();
        m.pushMatrix();
        m.translate((int) hudX, (int) hudY);
        m.scale(scale, scale);

        ProfitStore.Stats s = ProfitStore.getActiveStats();
        boolean session = ProfitStore.isShowingSession();

        int y = 0;
        draw(ctx, mc, 0, y, "§6§lProfit Tracker §7(" + (session ? "Session" : "All Time") + ")");
        y += LINE_H;

        draw(ctx, mc, 0, y, "§bTotal Gains:§r " + fmt(s.totalGains()));
        y += LINE_H;
        draw(ctx, mc, 0, y, "§bTotal Expenses:§r " + fmt(-s.totalExpenses()));
        y += LINE_H;
        draw(ctx, mc, INDENT, y, "§7Items: " + fmt(s.itemsValue() + s.attributeValue()));
        y += LINE_H;
        draw(ctx, mc, INDENT, y, "§7Essence: " + fmt(s.essenceValue()));
        y += LINE_H;
        draw(ctx, mc, INDENT, y, "§7Keys: " + fmt(-s.keyCost()));
        y += LINE_H;
        draw(ctx, mc, INDENT, y, "§7Kismets: " + fmt(-s.kismetCost()));
        y += LINE_H;
        draw(ctx, mc, INDENT, y, "§7Wheel of Fate: " + fmt(-s.wheelCost()));
        y += LINE_H;

        long profit = s.profit();
        draw(ctx, mc, 0, y, "§bTotal Profit:§r " + (profit >= 0 ? "§a" : "§c") + fmt(profit));
        y += LINE_H;

        draw(ctx, mc, 0, y, "§bTotal Runs:§r " + s.runs());
        y += LINE_H;

        draw(ctx, mc, 0, y, "§bProfit/Run:§r " + fmt(s.profitPerRun()));
        y += LINE_H;

        y += 2;
        row1Y = y;

        String sesStr = "Session";
        String atStr  = "All Time";
        String rstStr = "Reset";
        int sesRaw = mc.font.width(sesStr);
        int atRaw  = mc.font.width(atStr);
        int rstRaw = mc.font.width(rstStr);
        int gap    = 8;

        sessionX = 0;             sessionW = sesRaw;
        allTimeX = sesRaw + gap;  allTimeW = atRaw;
        resetX   = sesRaw + gap + atRaw + gap;  resetW = rstRaw;

        draw(ctx, mc, sessionX, y, session  ? "§n§fSession§r" : "§7Session");
        draw(ctx, mc, allTimeX, y, !session ? "§n§fAll Time§r" : "§7All Time");
        draw(ctx, mc, resetX,   y, "§cReset§r");
        y += LINE_H;

        m.popMatrix();
    }

    private static void draw(GuiGraphicsExtractor ctx, Minecraft mc, int x, int y, String text) {
        ctx.text(mc.font, text, x, y, 0xFFFFFFFF, true);
    }

    static String fmt(long coins) {
        if (coins == 0) return "§70§r";
        boolean neg = coins < 0;
        long abs = Math.abs(coins);
        String s;
        if      (abs >= 1_000_000_000L) s = String.format(Locale.US, "%.1fB", abs / 1_000_000_000.0);
        else if (abs >= 1_000_000L)     s = String.format(Locale.US, "%.1fM", abs / 1_000_000.0);
        else if (abs >= 1_000L)         s = String.format(Locale.US, "%.1fK", abs / 1_000.0);
        else                            s = String.valueOf(abs);
        return neg ? "§c-" + s + "§r" : "§f" + s + "§r";
    }

}
