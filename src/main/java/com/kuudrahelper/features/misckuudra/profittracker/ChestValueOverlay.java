package com.kuudrahelper.features.misckuudra.profittracker;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.utils.KuudraTierDetector;
import net.minecraft.client.Minecraft;
import com.kuudrahelper.features.misckuudra.profittracker.ProfitRun;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;


public final class ChestValueOverlay {

    private static final int LINE_H = 10;
    private static final int INDENT = 8;

    private static boolean wheelUsed       = false;
    private static int     cachedTier      = 0;
    private static boolean hasCommitted    = false;
    private static boolean chestIsOpen     = false;
    private static boolean freeChest       = false;
    private static CroesusListener.ChestAnalysis pendingAnalysis = null;

    public static boolean isChestOpen()   { return chestIsOpen; }
    public static boolean isFreeChest()   { return freeChest; }
    public static boolean hasCommitted()  { return hasCommitted; }

    public static void onChestOpen(AbstractContainerScreen<?> screen) {
        wheelUsed    = false;
        hasCommitted = false;
        chestIsOpen  = true;
        freeChest    = CroesusListener.isFreeChest(screen);
        cachedTier   = parseTier(screen.getTitle().getString());
    }

    public static void reset() {
        wheelUsed       = false;
        hasCommitted    = false;
        chestIsOpen     = false;
        freeChest       = false;
        cachedTier      = 0;
        pendingAnalysis = null;
    }

    public static void updatePending(CroesusListener.ChestAnalysis a) {
        pendingAnalysis = a;
    }

    public static void commitOnPaidChest() {
        if (pendingAnalysis != null && !hasCommitted) {
            tryCommitRun(pendingAnalysis);
        }
        pendingAnalysis = null;
    }

    public static int  getCachedTier() { return cachedTier; }

    public static boolean areSlotsReady(AbstractContainerScreen<?> screen) {
        var slots = screen.getMenu().slots;
        if (slots.size() < 90) return false;
        int nonEmpty = 0;
        for (int i = CroesusListener.REWARD_SLOT_START;
             i <= CroesusListener.REWARD_SLOT_END && i < slots.size(); i++) {
            String name = CroesusListener.stripColorStatic(slots.get(i).getItem().getDisplayName().getString()).toLowerCase().trim();
            if (!name.isEmpty() && !name.equals("[]")) nonEmpty++;
        }
        return freeChest ? nonEmpty >= 1 : nonEmpty >= 5;
    }

    public static int parseTierFrom(AbstractContainerScreen<?> screen) {
        return parseTier(screen.getTitle().getString());
    }

    public static void onSlotClicked(int slotId, CroesusListener.ChestAnalysis analysis) {
        if (analysis == null) return;
        if (slotId == analysis.wheelSlotIndex()) wheelUsed = true;
    }

    public static void tryCommitRun(CroesusListener.ChestAnalysis a) {
        if (hasCommitted || a == null) return;

        if (a.detectedTier() > 0) cachedTier = a.detectedTier();

        int freeEssAmt = (cachedTier >= 1 && cachedTier <= 5) ? KuudraDrops.FREE_CHEST_ESSENCE[cachedTier] : 0;
        double petMult  = KuudraConfig.getKuudraPetEssenceMultiplier();
        double essPrice = CroesusListener.bazaarSellPrice(KuudraDrops.CRIMSON_ESSENCE);
        long freeEssValue = (long)(freeEssAmt * petMult * essPrice);

        ProfitRun run = new ProfitRun();
        run.itemsValue     = a.itemsValue();
        run.attributeValue = a.attributeValue();
        run.essenceValue   = a.essenceValue() + freeEssValue;
        run.keyCost        = freeChest ? 0 : CroesusListener.calculateKeyCost(cachedTier);
        run.kismetCost     = a.kismetAlreadyUsed()
                ? (long) CroesusListener.bazaarBuyPrice(KuudraDrops.KISMET_FEATHER) : 0;
        run.wheelCost      = wheelUsed
                ? (long) CroesusListener.bazaarBuyPrice(KuudraDrops.WHEEL_OF_FATE) : 0;
        run.durationMs     = 0;
        run.timestamp      = System.currentTimeMillis();

        ProfitStore.addRun(run);
        ProfitStore.setLastCommittedTier(cachedTier);

        hasCommitted = true;
    }

    public static void render(GuiGraphicsExtractor ctx, CroesusListener.ChestAnalysis a) {
        if (!KuudraConfig.isProfitTrackerEnabled()) return;
        if (!KuudraConfig.isChestValueGuiEnabled()) return;
        if (a == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int renderTier = a.detectedTier() > 0 ? a.detectedTier() : cachedTier;

        long itemValue    = a.itemsValue() + a.attributeValue();
        int freeEssAmt = (renderTier >= 1 && renderTier <= 5) ? KuudraDrops.FREE_CHEST_ESSENCE[renderTier] : 0;
        double petMult  = KuudraConfig.getKuudraPetEssenceMultiplier();
        double essPrice = CroesusListener.bazaarSellPrice(KuudraDrops.CRIMSON_ESSENCE);
        long freeEssValue = (long)(freeEssAmt * petMult * essPrice);
        long essenceValue = a.essenceValue() + freeEssValue;
        long chestValue   = itemValue + essenceValue;

        long keyCost     = freeChest ? 0 : CroesusListener.calculateKeyCost(renderTier);
        boolean kismetAny = a.kismetAlreadyUsed();
        long kismetCost  = kismetAny ? (long) CroesusListener.bazaarBuyPrice(KuudraDrops.KISMET_FEATHER) : 0;
        long wheelCost   = wheelUsed  ? (long) PriceCache.getBin(KuudraDrops.WHEEL_OF_FATE) : 0;
        long totalExp    = keyCost + kismetCost + wheelCost;
        long profit      = chestValue - totalExp;

        var m = ctx.pose();
        m.pushMatrix();
        m.translate((int)(KuudraConfig.getChestValueHudX() * screenW),
                    (int)(KuudraConfig.getChestValueHudY() * screenH));
        m.scale(KuudraConfig.getChestValueHudScale(), KuudraConfig.getChestValueHudScale());

        int y = 0;
        String tierStr = renderTier > 0 ? " §7(T" + renderTier + ")" : "";
        draw(ctx, mc, 0, y, "§6§lChest Value" + tierStr);
        y += LINE_H;

        draw(ctx, mc, 0, y, "§bChest Value:§r " + fmt(chestValue));
        y += LINE_H;
        draw(ctx, mc, INDENT, y, "§7Items: " + fmt(itemValue));
        y += LINE_H;
        draw(ctx, mc, INDENT, y, "§7Essence: " + fmt(essenceValue));
        y += LINE_H;

        draw(ctx, mc, 0, y, "§bExpenses:§r " + fmt(-totalExp));
        y += LINE_H;
        draw(ctx, mc, INDENT, y, "§7Key: " + fmt(-keyCost));
        y += LINE_H;
        if (kismetAny) { draw(ctx, mc, INDENT, y, "§7Kismet: " + fmt(-kismetCost)); y += LINE_H; }
        if (wheelUsed)  { draw(ctx, mc, INDENT, y, "§7WoF: "    + fmt(-wheelCost));  y += LINE_H; }

        String pColor = profit >= 0 ? "§a" : "§c";
        draw(ctx, mc, 0, y, "§bChest Profit:§r " + pColor + fmt(profit));

        m.popMatrix();
    }

    private static void draw(GuiGraphicsExtractor ctx, Minecraft mc, int x, int y, String text) {
        ctx.text(mc.font, text, x, y, 0xFFFFFFFF, true);
    }

    private static int parseTier(String rawTitle) {
        String t = KuudraDrops.stripColor(rawTitle).toLowerCase();
        if (t.contains("infernal")) return 5;
        if (t.contains("fiery"))    return 4;
        if (t.contains("burning"))  return 3;
        if (t.contains("hot"))      return 2;
        if (t.contains("basic"))    return 1;
        int kt = KuudraTierDetector.getTier();
        if (kt >= 1) return kt;
        int last = ProfitStore.getLastCommittedTier();
        return last >= 1 ? last : 0;
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

    private ChestValueOverlay() {}
}
