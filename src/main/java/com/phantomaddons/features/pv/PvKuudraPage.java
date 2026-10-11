package com.phantomaddons.features.pv;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class PvKuudraPage {

    private PvKuudraPage() {}

    private static final String[][] TIERS = {
            { "none", "Basic" }, { "hot", "Hot" }, { "burning", "Burning" }, { "fiery", "Fiery" }, { "infernal", "Infernal" } };

    private record Entry(PvProfileView.Loadout loadout, String setLabel) {}

    static int render(PvPages.Ctx c) {
        float es = c.es();
        int pad = 8, gap = 6;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll();
        int availW = c.w() - 2 * pad, availH = c.h() - 2 * pad;

        int leftW = Math.max(Math.round(120 * es), (int) (availW * 0.36f));
        int leftH = leftColumn(c, x0, y0, leftW, availH);

        int rx = x0 + leftW + gap, rw = availW - leftW - gap;
        int rightH = loadouts(c, rx, y0, rw, availH);
        return Math.max(leftH, rightH);
    }

    // ── Left: Stats + Weapons ─────────────────────────────────────────────────

    private static int leftColumn(PvPages.Ctx c, int x, int y, int w, int availH) {
        PvScreen s = c.s();
        PvProfileView v = c.v();
        float es = c.es();
        int rows = 4 + TIERS.length + v.analysis.kuudraWeapons.size() + 2;
        int ip = 5, cardGap = 6;
        int head = c.lh(0.9f * es) + 4;
        float ss = 0.8f * es * (float) Math.sqrt(c.room());
        for (int i = 0; i < 6; i++) {
            int need = 2 * (ip * 2 + head) + cardGap + rows * (c.lh(ss) + 3);
            if (need <= availH || ss <= 0.55f) break;
            ss -= 0.05f;
        }
        int base = 2 * (ip * 2 + head) + cardGap + rows * (c.lh(ss) + 3);
        int extra = Math.max(0, Math.min(Math.round(12 * c.room()), (availH - base) / rows));
        int lh = c.lh(ss) + 3 + extra;
        int textOff = (lh - c.lh(ss)) / 2;
        int rw = w - 2 * ip;

        // ── Stats ──
        JsonObject tiers = PvJson.obj(c.m(), "nether_island_player_data", "kuudra_completed_tiers");
        int statsH = ip + head + (4 + TIERS.length) * lh + ip;
        s.card(c.g(), x, y, w, statsH);
        int yy = y + ip;
        s.text(c.g(), "§eStats", x + ip, yy, PvTheme.text(), 0.9f * es);
        yy += head;
        s.statRow(c.g(), "Bank", v.bank >= 0 ? PvPages.shortNum(v.bank) : "?", x + ip, yy + textOff, rw, ss);
        yy += lh;
        s.statRow(c.g(), "Magical Power", v.magicalPower >= 0 ? String.valueOf(v.magicalPower) : "?", x + ip, yy + textOff, rw, ss);
        yy += lh;
        s.statRow(c.g(), "Catacombs",
                String.valueOf(PvDungeonsPage.level(PvJson.num(c.m(), 0, "dungeons", "dungeon_types", "catacombs", "experience")).level()),
                x + ip, yy + textOff, rw, ss);
        yy += lh;
        int foraging = 0;
        for (PvProfileView.Skill sk : v.skills) if (sk.name().equals("Foraging")) foraging = sk.level().level();
        s.statRow(c.g(), "Foraging", String.valueOf(foraging), x + ip, yy + textOff, rw, ss);
        yy += lh;
        for (String[] t : TIERS) {
            double comps = PvJson.num(tiers, 0, t[0]);
            s.statRow(c.g(), t[1], PvPages.num(comps), x + ip, yy + textOff, rw, ss);
            double wave = PvJson.num(tiers, -1, "highest_wave_" + t[0]);
            if (wave >= 0 && s.hovered(c.mx(), c.my(), x + ip, yy, rw, lh))
                s.tooltip("§6" + t[1] + " Kuudra", "§7Completions: §e" + PvPages.num(comps), "§7Highest Wave: §e" + (int) wave);
            yy += lh;
        }

        // ── Weapons ──
        int wy = y + statsH + cardGap;
        int wrows = v.analysis.kuudraWeapons.size() + 2;
        int spaceH = availH - statsH - cardGap - 2 * ip - head;
        int wlh = Math.max(lh, Math.min(spaceH / wrows, Math.round(30 * es * c.room()) + 4));
        int slot = Math.max(10, Math.min(wlh - 2, Math.round(28 * es * c.room())));
        int wTextOff = (wlh - c.lh(ss)) / 2;
        int weaponsH = ip + head + wrows * wlh + ip;
        s.card(c.g(), x, wy, w, weaponsH);
        int iy = wy + ip;
        s.text(c.g(), "§eWeapons", x + ip, iy, PvTheme.text(), 0.9f * es);
        iy += head;
        int slotOff = (wlh - slot) / 2;
        for (PvAnalysis.KuudraWeapon wp : v.analysis.kuudraWeapons) {
            boolean has = !wp.stack().isEmpty();
            s.slot(c.g(), x + ip, iy + slotOff, slot, wp.stack(), c.mx(), c.my());
            String label = (wp.partial() ? "§6– §r" : has ? "§a✔ §r" : "§c✖ §r") + wp.label();
            s.text(c.g(), label, x + ip + slot + 4, iy + wTextOff, PvTheme.text(), ss);
            iy += wlh;
        }
        dragon(c, "DPS", v.analysis.dpsDragon, x + ip, iy + slotOff, slot, ss, iy + wTextOff);
        iy += wlh;
        dragon(c, "Rend", v.analysis.rendDragon, x + ip, iy + slotOff, slot, ss, iy + wTextOff);
        return statsH + cardGap + weaponsH;
    }

    private static void dragon(PvPages.Ctx c, String role, PvProfileView.Pet pet, int x, int slotY, int slot, float ss, int textY) {
        PvScreen s = c.s();
        if (pet == null) {
            s.slot(c.g(), x, slotY, slot, ItemStack.EMPTY, c.mx(), c.my());
            s.text(c.g(), "§8" + role + " Gdrag: None", x + slot + 4, textY, PvTheme.text(), ss);
            return;
        }
        s.petSlot(c.g(), x, slotY, slot, pet, c.mx(), c.my());
        s.text(c.g(), "§7[" + pet.level().level() + "] §r" + role + " Gdrag", x + slot + 4, textY, PvTheme.text(), ss);
    }

    // ── Right: vertical loadout cards ─────────────────────────────────────────

    private static int loadouts(PvPages.Ctx c, int x, int y, int w, int availH) {
        PvScreen s = c.s();
        PvProfileView v = c.v();
        float es = c.es();
        List<Entry> entries = new ArrayList<>();
        for (PvAnalysis.KuudraSet ks : v.analysis.kuudraSets)
            for (PvProfileView.Loadout lo : v.loadouts)
                if (lo.armorSet() == ks.slotNumber()) entries.add(new Entry(lo, ks.label()));

        int titleH = s.title(c.g(), "Kuudra Loadouts §7(" + entries.size() + ")", x, y);
        if (entries.isEmpty()) {
            String msg = v.analysis.kuudraSets.isEmpty()
                    ? "No Kuudra armor sets found in the wardrobe."
                    : "No saved loadouts use the detected sets: " + v.analysis.kuudraSets.stream()
                            .map(k -> k.label() + " (#" + k.slotNumber() + ")").collect(Collectors.joining(", "));
            float ms = 0.85f * es;
            var lines = s.pvFont().split(net.minecraft.network.chat.Component.literal(msg), (int) (w / ms));
            int ly = y + titleH;
            for (var l : lines) {
                var pose = c.g().pose();
                pose.pushMatrix();
                pose.translate(x, ly);
                pose.scale(ms, ms);
                c.g().text(s.pvFont(), l, 0, 0, PvTheme.dim(), com.phantomaddons.gui.Theme.darkMode);
                pose.popMatrix();
                ly += c.lh(ms) + 2;
            }
            return ly - y;
        }

        int gap = 5;
        int n = entries.size();
        int cols = Math.min(n, Math.max(1, (w + gap) / Math.max(52, Math.round(58 * es))));
        int rowsOfCards = (n + cols - 1) / cols;
        int cardW = (w - (cols - 1) * gap) / cols;
        int cardAreaH = (availH - titleH - (rowsOfCards - 1) * gap) / rowsOfCards;

        float grow = (float) Math.sqrt(c.room());
        float ts = 0.75f * es * grow, is = 0.62f * es * grow;
        int ip = 4;
        int textH = 2 * (c.lh(ts) + 2) + 2 * (c.lh(is) + 2) + 8;
        int slot = s.fitSlot((cardW - 2 * ip) / 2.25, (cardAreaH - textH - 2 * ip) / 5.5);
        int g = PvScreen.gapFor(slot);
        int cardH = 2 * ip + textH + 5 * slot + 4 * g + 6;

        int cy = y + titleH;
        for (int i = 0; i < n; i++) {
            int col = i % cols;
            if (col == 0 && i > 0) cy += cardH + gap;
            card(c, entries.get(i), v, x + col * (cardW + gap), cy, cardW, cardH, slot, g, ip, ts, is);
        }
        return cy + cardH - y;
    }

    private static void card(PvPages.Ctx c, Entry e, PvProfileView v, int x, int y, int w, int h,
                             int slot, int g, int ip, float ts, float is) {
        PvScreen s = c.s();
        PvProfileView.Loadout lo = e.loadout();
        com.phantomaddons.gui.GuiDraw.roundedFill(c.g(), x, y, x + w, y + h, PvTheme.overlay(lo.active() ? 0x18 : 0x10), 5);
        if (lo.active()) com.phantomaddons.gui.GuiDraw.roundedFill(c.g(), x + 3, y, x + w - 3, y + 2, PvTheme.on(), 1);

        int yy = y + ip;
        centered(c, fit(s, lo.name(), w - 2 * ip, ts), x, w, yy, lo.active() ? PvTheme.on() : PvTheme.text(), ts);
        yy += c.lh(ts) + 2;
        centered(c, fit(s, "§e" + e.setLabel(), w - 2 * ip, ts), x, w, yy, PvTheme.text(), ts);
        yy += c.lh(ts) + 4;

        PvProfileView.WardrobeSet armor = v.armorSet(lo.armorSet());
        PvProfileView.WardrobeSet equip = v.equipmentSet(lo.equipmentSet());
        int pairW = 2 * slot + g;
        int sx = x + (w - pairW) / 2;
        for (int r = 0; r < 4; r++) {
            ItemStack a = armor != null && r < armor.items().length ? armor.items()[r] : ItemStack.EMPTY;
            ItemStack q = equip != null && r < equip.items().length ? equip.items()[r] : ItemStack.EMPTY;
            s.slot(c.g(), sx, yy + r * (slot + g), slot, a, c.mx(), c.my());
            s.slot(c.g(), sx + slot + g, yy + r * (slot + g), slot, q, c.mx(), c.my());
        }
        yy += 4 * (slot + g) + 2;
        int px = x + (w - slot) / 2;
        if (lo.pet() != null) s.petSlot(c.g(), px, yy, slot, lo.pet(), c.mx(), c.my());
        else s.slot(c.g(), px, yy, slot, ItemStack.EMPTY, c.mx(), c.my());
        yy += slot + 4;

        String power = lo.powerStone() != null ? PvProfileView.prettify(lo.powerStone()) : "No Power";
        String tuning = lo.tuning().isEmpty() ? "§8No Tuning" : PvScreen.tuningString(lo.tuning());
        int infoY = yy;
        centered(c, fit(s, "§7" + power, w - 2 * ip, is), x, w, yy, PvTheme.text(), is);
        yy += c.lh(is) + 2;
        centered(c, fit(s, tuning, w - 2 * ip, is), x, w, yy, PvTheme.text(), is);
        if (s.hovered(c.mx(), c.my(), x, infoY, w, 2 * (c.lh(is) + 2)))
            s.tooltip("§e" + lo.name(), "§7Set: §r" + e.setLabel(), "§7Power Stone: §r" + power,
                    "§7Tuning: §r" + (lo.tuning().isEmpty() ? "None" : PvScreen.tuningString(lo.tuning())));
    }

    private static void centered(PvPages.Ctx c, String text, int x, int w, int y, int color, float scale) {
        c.s().text(c.g(), text, x + (w - c.s().textW(text, scale)) / 2, y, color, scale);
    }

    private static String fit(PvScreen s, String text, int w, float scale) {
        if (s.textW(text, scale) <= w) return text;
        String t = text;
        while (t.length() > 2 && s.textW(t + "…", scale) > w) t = t.substring(0, t.length() - 1);
        return t + "…";
    }
}
