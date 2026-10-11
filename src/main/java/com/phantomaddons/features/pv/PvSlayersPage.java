package com.phantomaddons.features.pv;

import com.google.gson.JsonObject;
import com.phantomaddons.gui.GuiDraw;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class PvSlayersPage {

    private PvSlayersPage() {}

    private record Slayer(String key, String name, String color, int tiers, long[] xp) {}

    private static final long[] STANDARD_XP = { 5, 15, 200, 1000, 5000, 20000, 100000, 400000, 1000000 };
    private static final long[] WOLF_XP = { 10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000 };
    private static final long[] VAMPIRE_XP = { 20, 75, 240, 840, 2400 };

    private static final Slayer[] SLAYERS = {
            new Slayer("zombie", "Revenant Horror", "§2", 5, STANDARD_XP),
            new Slayer("spider", "Tarantula Broodfather", "§4", 5, STANDARD_XP),
            new Slayer("wolf", "Sven Packmaster", "§f", 4, WOLF_XP),
            new Slayer("enderman", "Voidgloom Seraph", "§5", 4, WOLF_XP),
            new Slayer("blaze", "Inferno Demonlord", "§6", 4, WOLF_XP),
            new Slayer("vampire", "Riftstalker Bloodfiend", "§c", 5, VAMPIRE_XP),
    };

    static int render(PvPages.Ctx c) {
        int pad = PvPages.PAD;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll();
        int avail = c.w() - 2 * pad;
        JsonObject bosses = PvJson.obj(c.m(), "slayer", "slayer_bosses");
        if (bosses == null) {
            c.s().text(c.g(), "No slayer data.", x0, y0, PvTheme.dim(), 0.85f * c.es());
            return c.lh(c.es());
        }

        int cols = PvPages.columnsFor(avail, Math.round(150 * c.es()));
        int gap = 6;
        int cardW = (avail - (cols - 1) * gap) / cols;
        int rowH = 0, cy = y0;
        for (int i = 0; i < SLAYERS.length; i++) {
            int col = i % cols;
            int ch = card(c, SLAYERS[i], PvJson.obj(bosses, SLAYERS[i].key), x0 + col * (cardW + gap), cy, cardW);
            rowH = Math.max(rowH, ch);
            if (col == cols - 1 || i == SLAYERS.length - 1) { cy += rowH + gap; rowH = 0; }
        }
        return cy - y0;
    }

    private static int card(PvPages.Ctx c, Slayer sl, JsonObject data, int x, int y, int w) {
        PvScreen s = c.s();
        float es = c.es(), ss = 0.8f * es;
        int ip = Math.round(6 * es);
        double xp = PvJson.num(data, 0, "xp");
        int maxLevel = sl.xp.length;
        int level = PvPages.cumulativeLevel(xp, sl.xp);
        int lh = c.lh(ss) + 2;

        List<PvAnalysis.WeaponLine> lines = PvAnalysis.SLAYER_WEAPONS.get(sl.key);
        PvAnalysis.SlayerWeapons found = c.v().analysis.slayerWeapons.get(sl.key);
        int slot = Math.max(12, Math.round(18 * es));
        int tierRows = (sl.tiers + 1) / 2;
        int labelRows = 0;
        for (PvAnalysis.WeaponLine l : lines) if (!l.label().isEmpty()) labelRows++;
        boolean sideBySide = lines.size() > 1;
        int weaponsH = sideBySide
                ? slot + 2 + (labelRows > 0 ? c.lh(0.7f * es) + 2 : 0)
                : lines.size() * (slot + 2) + labelRows * (c.lh(0.7f * es) + 2);
        int h = ip + c.lh(es) + 4 + lh + 8 + tierRows * lh + lh + 6 + weaponsH + ip;
        s.card(c.g(), x, y, w, h);

        int yy = y + ip;
        s.text(c.g(), sl.color + sl.name, x + ip, yy, PvTheme.text(), es);
        String lv = "Lvl " + level;
        s.text(c.g(), lv, x + w - ip - s.textW(lv, es), yy, level >= maxLevel ? PvTheme.accent() : PvTheme.text(), es);
        yy += c.lh(es) + 4;

        String xpLine = "§7XP: §r" + PvPages.num(xp);
        String next = level >= maxLevel ? "§6MAX" : "§7Next: §r" + PvPages.num(sl.xp[level] - xp);
        s.text(c.g(), xpLine, x + ip, yy, PvTheme.text(), ss);
        s.text(c.g(), next, x + w - ip - s.textW(next, ss), yy, PvTheme.text(), ss);
        yy += lh;
        double prevT = level == 0 ? 0 : sl.xp[level - 1];
        double prog = level >= maxLevel ? 1 : (xp - prevT) / (sl.xp[level] - prevT);
        int bw = w - 2 * ip, bh = Math.max(3, Math.round(4 * es));
        GuiDraw.roundedFill(c.g(), x + ip, yy, x + ip + bw, yy + bh, PvTheme.barBg(), bh / 2);
        int fw = (int) Math.round(bw * Math.max(0, Math.min(1, prog)));
        if (fw > 0) GuiDraw.roundedFill(c.g(), x + ip, yy, x + ip + fw, yy + bh, level >= maxLevel ? PvTheme.accent() : PvTheme.barFg(), bh / 2);
        yy += bh + 5;

        long total = 0;
        int colW = (w - 2 * ip) / 2;
        for (int t = 0; t < sl.tiers; t++) {
            long kills = (long) PvJson.num(data, 0, "boss_kills_tier_" + t);
            total += kills;
            int tx = x + ip + (t % 2) * colW, ty = yy + (t / 2) * lh;
            s.text(c.g(), "§7Tier " + PvPages.roman(t + 1) + ": §r" + PvPages.num(kills), tx, ty, PvTheme.text(), ss);
            long attempts = (long) PvJson.num(data, -1, "boss_attempts_tier_" + t);
            if (attempts >= 0 && s.hovered(c.mx(), c.my(), tx, ty, colW, lh))
                s.tooltip("§e" + sl.name + " Tier " + PvPages.roman(t + 1), "§7Kills: §a" + PvPages.num(kills),
                        "§7Attempts: §e" + PvPages.num(attempts));
        }
        yy += tierRows * lh;
        s.text(c.g(), "§7Total Kills: §r" + PvPages.num(total), x + ip, yy, PvTheme.text(), ss);
        yy += lh + 6;

        float ls = 0.7f * es;
        int lineW = (w - 2 * ip) / lines.size();
        int rowTop = yy;
        for (int i = 0; i < lines.size(); i++) {
            int lx = sideBySide ? x + ip + i * lineW : x + ip;
            if (sideBySide) yy = rowTop;
            if (!lines.get(i).label().isEmpty()) {
                s.text(c.g(), lines.get(i).label(), lx, yy, PvTheme.dim(), ls);
                yy += c.lh(ls) + 2;
            }
            List<ItemStack> got = found != null ? found.found().get(i) : List.of();
            for (int k = 0; k < Math.max(1, got.size()); k++) {
                s.slot(c.g(), lx + k * (slot + 3), yy, slot, k < got.size() ? got.get(k) : ItemStack.EMPTY, c.mx(), c.my());
            }
            if (got.isEmpty()) {
                String none = "None";
                s.text(c.g(), none, lx + slot + 3 + 4, yy + (slot - c.lh(ls)) / 2, PvTheme.dim(), ls);
            }
            yy += slot + 2;
        }
        return h;
    }
}
