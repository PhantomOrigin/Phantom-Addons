package com.phantomaddons.features.pv;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class PvPages {

    private PvPages() {}

    record Ctx(PvScreen s, GuiGraphicsExtractor g, PvProfileView v, int key, int sub,
               int x, int y, int w, int h, int mx, int my, int scroll) {
        float es() { return s.es; }
        float room() { return s.room(); }
        JsonObject m() { return v.member; }
        int lh(float scale) { return s.lineH(scale); }
    }

    static final int PAD = 10;

    static String num(double n) { return PvScreen.fmtFull(n); }
    static String shortNum(double n) { return PvScreen.fmt(n); }

    static String roman(int n) {
        if (n <= 0) return "0";
        if (PvNumerals.enabled()) return String.valueOf(n);
        int[] v = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
        String[] r = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) while (n >= v[i]) { n -= v[i]; sb.append(r[i]); }
        return sb.toString();
    }

    static String time(double ms) {
        long total = (long) Math.floor(ms / 1000);
        return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60);
    }

    static String timePrecise(double ms) {
        long total = (long) Math.floor(ms);
        return String.format(Locale.ROOT, "%d:%02d.%03d", total / 60000, total / 1000 % 60, total % 1000);
    }

    static int tierFromIncrements(double xp, long[] perTier) {
        double cum = 0;
        int level = 0;
        for (long t : perTier) { cum += t; if (xp >= cum) level++; else break; }
        return level;
    }

    static double xpToNextTier(double xp, long[] perTier, int level) {
        double cum = 0;
        for (int i = 0; i <= level && i < perTier.length; i++) cum += perTier[i];
        return cum - xp;
    }

    static int cumulativeLevel(double xp, long[] cumulative) {
        int level = 0;
        for (long t : cumulative) { if (xp >= t) level++; else break; }
        return level;
    }

    static PvProfileView.Pet bestPet(PvProfileView v, String type, java.util.function.Predicate<PvProfileView.Pet> filter) {
        PvProfileView.Pet best = null;
        for (PvProfileView.Pet p : v.pets) {
            if (!p.type().equals(type) || (filter != null && !filter.test(p))) continue;
            if (best == null || p.exp() > best.exp()
                    || PvProfileView.rarityIndex(p.rarity()) > PvProfileView.rarityIndex(best.rarity())) best = p;
        }
        return best;
    }

    static int slotRow(Ctx c, String label, ItemStack[] items, int x, int y, int s) {
        float ls = 0.75f * c.es();
        int labelH = c.lh(ls) + 3;
        c.s().text(c.g(), label, x, y, PvTheme.dim(), ls);
        int g = PvScreen.gapFor(s);
        for (int i = 0; i < items.length; i++) c.s().slot(c.g(), x + i * (s + g), y + labelH, s, items[i], c.mx(), c.my());
        return labelH + s;
    }

    static int columnsFor(int w, int minCardW) {
        return Math.max(1, Math.min(3, (w + 6) / (minCardW + 6)));
    }
}
