package com.phantomaddons.features.pv;

import com.google.gson.JsonObject;
import com.phantomaddons.gui.GuiDraw;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


final class PvDungeonsPage {

    private PvDungeonsPage() {}

    static final long[] CATACOMBS = {
            50, 75, 110, 160, 230, 330, 470, 670, 950, 1340,
            1890, 2665, 3760, 5260, 7380, 10300, 14400, 20000, 27600, 38000,
            52500, 71500, 97000, 132000, 180000, 243000, 328000, 445000, 600000, 800000,
            1065000, 1410000, 1900000, 2500000, 3300000, 4300000, 5600000, 7200000, 9200000, 12000000,
            15000000, 19000000, 24000000, 30000000, 38000000, 48000000, 60000000, 75000000, 93000000, 116250000,
    };

    private static final String[] CLASSES = { "healer", "mage", "berserk", "archer", "tank" };
    private static final String[] FLOOR_NAMES = { "Entrance", "Floor 1", "Floor 2", "Floor 3", "Floor 4", "Floor 5", "Floor 6", "Floor 7" };

    static PvLeveling.Level level(double xp) {
        return PvLeveling.compute(xp, CATACOMBS, 0, 0, 50);
    }

    private static final double CATACOMBS_MAX_XP = 569_809_640d;   // the sum of the table above
    private static final double OVERFLOW_LEVEL_XP = 200_000_000d;   // every Catacombs level past 50

    static PvLeveling.Level catacombs(double xp) {
        PvLeveling.Level base = level(xp);
        if (!com.phantomaddons.PhantomConfig.isPvOverflowSkills() || !base.maxed() || xp < CATACOMBS_MAX_XP) return base;
        double extra = xp - CATACOMBS_MAX_XP;
        int levels = (int) (extra / OVERFLOW_LEVEL_XP);
        double into = extra - levels * OVERFLOW_LEVEL_XP;
        return new PvLeveling.Level(50 + levels, Integer.MAX_VALUE, into / OVERFLOW_LEVEL_XP, into, OVERFLOW_LEVEL_XP);
    }

    static int render(PvPages.Ctx c) {
        PvScreen s = c.s();
        float es = c.es();
        int pad = 8, gap = 10;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll();
        int avail = c.w() - 2 * pad, availH = c.h() - 2 * pad;
        JsonObject d = PvJson.obj(c.m(), "dungeons");
        if (d == null) {
            s.text(c.g(), "No dungeon data.", x0, y0, PvTheme.dim(), 0.85f * es);
            return c.lh(es);
        }

        // ── Floor cards take the bottom two rows, the stats strip sits just above them ──
        float fs = 0.75f * es;
        int floorCardH = 2 * c.lh(fs) + 9;
        int floorsH = 2 * floorCardH + 4;
        float is = 0.75f * es;
        int statLine = c.lh(is) + 4;
        int immSlot = Math.max(12, Math.round(18 * es));
        int stripH = Math.max(2 * statLine, immSlot);
        int topH = Math.max(availH - floorsH - stripH - 2 * gap, Math.round(110 * es));

        // ── Left: level bars ──
        PvLeveling.Level cata = catacombs(PvJson.num(d, 0, "dungeon_types", "catacombs", "experience"));
        List<PvLeveling.Level> classes = new ArrayList<>();
        double classSum = 0;
        for (String cl : CLASSES) {
            PvLeveling.Level l = catacombs(PvJson.num(d, 0, "player_classes", cl, "experience"));
            classes.add(l);
            classSum += Math.min(50, l.level());   // the average stays out of overflow
        }
        String selected = PvJson.str(d, "", "selected_dungeon_class");
        int viewKey = c.key() + 50;
        int picked = s.innerTab(viewKey) - 1;
        int viewed = picked >= 0 ? picked : Math.max(0, List.of(CLASSES).indexOf(selected));

        int leftW = (int) (avail * 0.34f);
        float bs = 0.85f * es;
        int barH = Math.max(3, Math.round(4 * es));
        int minRow = c.lh(bs) + barH + 4;
        int rowH = Math.max(minRow, Math.min(Math.round((minRow + 10) * c.room()), topH / 6));
        int by = y0;
        bar(c, "Catacombs", cata, x0, by, leftW, barH, bs, PvTheme.text());
        by += rowH;
        for (int i = 0; i < CLASSES.length; i++) {
            boolean isSelected = CLASSES[i].equals(selected);
            String name = (i == viewed ? "▶ " : "") + PvProfileView.prettify(CLASSES[i]);
            bar(c, name, classes.get(i), x0, by, leftW, barH, bs, isSelected ? PvTheme.accent() : PvTheme.text());
            final int idx = i;
            s.addHit(x0, by, x0 + leftW, by + rowH - 2, () -> s.setInnerTab(viewKey, idx + 1));
            by += rowH;
        }
        int sectionH = Math.max(topH, 6 * rowH);

        // ── Right: class setup cards, full height of the top section ──
        int rx = x0 + leftW + gap, rw = avail - leftW - gap;
        List<PvDungeonGear.Group> groups = c.v().analysis.dungeonGear.setups.get(CLASSES[viewed]);
        // In a chat card the player's name sits on the first line at the top right, so the setup starts below it.
        int nameRoom = s.inCard() ? c.lh(es) + 4 : 0;
        setupCards(c, groups, rx, y0 + nameRoom, rw, sectionH - nameRoom);

        // ── Stats strip: full width under the class bars and setup, immunity slots on the right ──
        int iy = y0 + sectionH + gap;
        GuiDraw.roundedFill(c.g(), x0, iy - gap / 2 - 1, x0 + avail, iy - gap / 2, PvTheme.overlay(0x18), 0);
        int immW = 3 * (immSlot + 3);
        int statsW = avail - immW - 12;
        int statW = (statsW - 2 * 10) / 3;
        String[][] stats = {
                { "Class Avg", String.format(Locale.ROOT, "%.2f", classSum / CLASSES.length) },
                { "Secrets", PvPages.shortNum(PvJson.num(d, 0, "secrets")) },
                { "Magical Power", c.v().magicalPower >= 0 ? String.valueOf(c.v().magicalPower) : "?" },
                { "Best Floor", floorName(false, (int) PvJson.num(d, -1, "dungeon_types", "catacombs", "highest_tier_completed")) },
                { "Best Master", floorName(true, (int) PvJson.num(d, -1, "dungeon_types", "master_catacombs", "highest_tier_completed")) },
                { "Bank", c.v().bank >= 0 ? PvPages.shortNum(c.v().bank) : "API off" },
        };
        int textTop = iy + (stripH - 2 * statLine) / 2;
        for (int i = 0; i < stats.length; i++) {
            int sx = x0 + (i % 3) * (statW + 10), sy = textTop + (i / 3) * statLine;
            s.statRow(c.g(), stats[i][0], stats[i][1], sx, sy, statW, is);
        }
        PvDungeonGear gear = c.v().analysis.dungeonGear;
        int gx = x0 + avail - immW + 3;
        int gy = iy + (stripH - immSlot) / 2;
        immunity(c, gear.bonzoMask, "Bonzo Mask", gx, gy, immSlot);
        immunity(c, gear.spiritMask, "Spirit Mask", gx + immSlot + 3, gy, immSlot);
        int px = gx + 2 * (immSlot + 3);
        if (gear.phoenix != null) s.petSlot(c.g(), px, gy, immSlot, gear.phoenix, c.mx(), c.my());
        else immunity(c, ItemStack.EMPTY, "Phoenix Pet", px, gy, immSlot);

        // ── Bottom: floors ──
        int fy = iy + stripH + gap;
        floorRow(c, PvJson.obj(d, "dungeon_types", "catacombs"), false, x0, fy, avail, floorCardH, fs);
        floorRow(c, PvJson.obj(d, "dungeon_types", "master_catacombs"), true, x0, fy + floorCardH + 4, avail, floorCardH, fs);
        return fy + floorsH - y0;
    }

    private static void immunity(PvPages.Ctx c, ItemStack item, String what, int x, int y, int slot) {
        c.s().slot(c.g(), x, y, slot, item, c.mx(), c.my());
        if (item.isEmpty() && c.s().hovered(c.mx(), c.my(), x, y, slot, slot)) c.s().tooltip("§7" + what, "§8Not found");
    }

    private record SetupCard(String title, String subtitle, List<ItemStack> items, List<String> labels, List<PvProfileView.Pet> pets) {
        int count() { return items.size() + pets.size(); }
        int columns() { return Math.max(1, (count() + 3) / 4); }
    }

    private static String shortLabel(String label) {
        return switch (label) {
            case "RCM Set" -> "RCM";
            case "LCM Set" -> "LCM";
            case "Necron / Maxor Set" -> "Necron";
            case "Frozen Blaze (Dungeon)" -> "Frozen Blaze";
            case "Monster Raider" -> "Raider";
            case "Thermodynamic Set" -> "Thermo";
            case "Goldor Set" -> "Goldor";
            default -> label;
        };
    }

    private static List<SetupCard> cards(List<PvDungeonGear.Group> groups) {
        List<SetupCard> out = new ArrayList<>();
        List<ItemStack> weapons = new ArrayList<>();
        List<String> weaponLabels = new ArrayList<>();
        List<PvProfileView.Pet> pets = new ArrayList<>();
        if (groups == null) return out;
        for (PvDungeonGear.Group g : groups) {
            if (!g.pets().isEmpty() || g.items().length == 0) { pets.addAll(g.pets()); continue; }
            if (g.isArmorSet()) {
                List<ItemStack> slots = new ArrayList<>(List.of(g.items()));
                slots.addAll(List.of(g.equipment()));
                List<String> labels = new ArrayList<>();
                for (int i = 0; i < 4; i++) labels.add(g.label());
                for (int i = 0; i < 4; i++) labels.add(g.label() + " equipment");
                out.add(new SetupCard(shortLabel(g.label()), g.source() != null ? g.source() : "Not found", slots, labels, List.of()));
            } else {
                boolean owned = false;
                for (ItemStack it : g.items()) {
                    if (it.isEmpty()) continue;
                    owned = true;
                    if (weapons.stream().anyMatch(o -> o == it)) continue;
                    weapons.add(it);
                    weaponLabels.add(g.label());
                }
                if (!owned) { weapons.add(ItemStack.EMPTY); weaponLabels.add(g.label()); }
            }
        }
        if (!weapons.isEmpty()) {
            List<ItemStack> sorted = new ArrayList<>();
            List<String> sortedLabels = new ArrayList<>();
            for (int pass = 0; pass < 2; pass++) {
                for (int i = 0; i < weapons.size(); i++) {
                    if (weapons.get(i).isEmpty() == (pass == 1)) {
                        sorted.add(weapons.get(i));
                        sortedLabels.add(weaponLabels.get(i));
                    }
                }
            }
            out.add(new SetupCard("Weapons", null, sorted, sortedLabels, List.of()));
        }
        out.add(new SetupCard(pets.size() == 1 ? "Pet" : "Pets", null, List.of(), List.of(), pets));
        return out;
    }

    private static void setupCards(PvPages.Ctx c, List<PvDungeonGear.Group> groups, int x, int y, int w, int h) {
        PvScreen s = c.s();
        float es = c.es(), ts = 0.7f * es;
        List<SetupCard> cards = cards(groups);
        if (cards.isEmpty()) return;
        int gap = 5, ip = 4;
        float subS = 0.6f * es;
        int titleH = c.lh(ts) + c.lh(subS) + 6;
        int totalCols = 0;
        for (SetupCard card : cards) totalCols += card.columns();

        int byH = (int) ((h - titleH - 2 * ip) / 4.25);
        int byW = (int) ((w - (cards.size() - 1) * gap - cards.size() * 2 * ip) / (totalCols * 1.12));
        int slot = Math.max(10, Math.min(Math.min(byH, byW), Math.round(30 * es * c.room())));
        int g = PvScreen.gapFor(slot);

        int used = 0;
        for (SetupCard card : cards) used += card.columns() * (slot + g) - g + 2 * ip;
        int extra = Math.max(0, (w - used - (cards.size() - 1) * gap) / cards.size());
        int cardH = titleH + 2 * ip + 4 * slot + 3 * g;

        int cx = x;
        for (SetupCard card : cards) {
            int cw = card.columns() * (slot + g) - g + 2 * ip + extra;
            GuiDraw.roundedFill(c.g(), cx, y, cx + cw, y + cardH, PvTheme.overlay(0x10), 5);
            String t = card.title();
            s.text(c.g(), t, cx + (cw - s.textW(t, ts)) / 2, y + ip, PvTheme.text(), ts);
            if (card.subtitle() != null) {
                String sub = card.subtitle();
                while (sub.length() > 3 && s.textW(sub, subS) > cw - 4) sub = sub.substring(0, sub.length() - 2);
                if (!sub.equals(card.subtitle())) sub += "…";
                s.text(c.g(), "§8" + sub, cx + (cw - s.textW("§8" + sub, subS)) / 2, y + ip + c.lh(ts) + 2, PvTheme.dim(), subS);
            }
            int colsW = card.columns() * (slot + g) - g;
            int sx0 = cx + (cw - colsW) / 2, sy0 = y + ip + titleH;
            int i = 0;
            for (int k = 0; k < card.items().size(); k++, i++) {
                int sx = sx0 + (i / 4) * (slot + g), sy = sy0 + (i % 4) * (slot + g);
                ItemStack it = card.items().get(k);
                s.slot(c.g(), sx, sy, slot, it, c.mx(), c.my());
                if (it.isEmpty() && s.hovered(c.mx(), c.my(), sx, sy, slot, slot))
                    s.tooltip("§7" + card.labels().get(k), "§8Not found");
            }
            for (PvProfileView.Pet p : card.pets()) {
                int sx = sx0 + (i / 4) * (slot + g), sy = sy0 + (i % 4) * (slot + g);
                s.petSlot(c.g(), sx, sy, slot, p, c.mx(), c.my());
                i++;
            }
            if (card.count() == 0) {
                s.slot(c.g(), sx0, sy0, slot, ItemStack.EMPTY, c.mx(), c.my());
                if (s.hovered(c.mx(), c.my(), sx0, sy0, slot, slot)) s.tooltip("§7No matching pet", "§8Not found");
            }
            cx += cw + gap;
        }
    }

    private static String floorName(boolean master, int tier) {
        if (tier < 0) return "None";
        return master ? "M" + tier : tier == 0 ? "E" : "F" + tier;
    }

    private static void bar(PvPages.Ctx c, String name, PvLeveling.Level l, int x, int y, int w, int barH, float scale, int nameColor) {
        PvScreen s = c.s();
        s.text(c.g(), name, x, y, nameColor, scale);
        String lv = String.valueOf(l.level());
        boolean max = l.level() >= 50, over = l.level() > 50;
        s.text(c.g(), lv, x + w - s.textW(lv, scale), y, max ? PvTheme.accent() : PvTheme.text(), scale);
        int by = y + c.lh(scale) + 2;
        GuiDraw.roundedFill(c.g(), x, by, x + w, by + barH, PvTheme.barBg(), barH / 2);
        int fw = (int) Math.round(w * Math.max(0, Math.min(1, l.progress())));
        if (fw > 0) GuiDraw.roundedFill(c.g(), x, by, x + fw, by + barH, max ? PvTheme.accent() : PvTheme.barFg(), barH / 2);
        if (s.hovered(c.mx(), c.my(), x, y, w, by + barH - y)) {
            String clean = name.replace("▶ ", "");
            String progress = "§7Progress: §e" + PvPages.shortNum(l.into()) + "§6/§e" + PvPages.shortNum(l.needed())
                    + " §8(" + String.format(Locale.ROOT, "%.1f", l.progress() * 100) + "%)";
            s.tooltip("§a" + clean + " " + l.level(), over ? "§7Overflow Level: §6" + l.level() + "  " + progress
                    : max ? "§6MAX LEVEL (50)" : progress,
                    clean.equals("Catacombs") ? "" : "§eClick to view this class's setup");
        }
    }

    private static void floorRow(PvPages.Ctx c, JsonObject type, boolean master, int x, int y, int w, int ch, float fs) {
        PvScreen s = c.s();
        int gap = 4, slots = 8;
        int cw = (w - (slots - 1) * gap) / slots;
        int from = master ? 1 : 0;
        for (int tier = from; tier <= 7; tier++) {
            String k = String.valueOf(tier);
            int cx = x + (master ? tier : tier) * (cw + gap);
            double comps = PvJson.num(type, 0, "tier_completions", k);
            boolean hov = s.hovered(c.mx(), c.my(), cx, y, cw, ch);
            GuiDraw.roundedFill(c.g(), cx, y, cx + cw, y + ch, PvTheme.overlay(hov ? 0x1C : 0x10), 4);
            String name = master ? "M" + tier : tier == 0 ? "E" : "F" + tier;
            s.text(c.g(), (master ? "§c" : "§a") + name, cx + 4, y + 4, PvTheme.text(), fs);
            String comp = PvPages.shortNum(comps);
            s.text(c.g(), comp, cx + cw - 4 - s.textW(comp, fs), y + 4, PvTheme.text(), fs);
            s.text(c.g(), bestTime(type, k), cx + 4, y + 6 + c.lh(fs), PvTheme.dim(), fs);
            if (hov) s.tooltipLines(floorTooltip(type, master, tier, master ? "Floor " + tier : FLOOR_NAMES[tier]));
        }
    }

    private static String bestTime(JsonObject type, String k) {
        double sPlus = PvJson.num(type, -1, "fastest_time_s_plus", k);
        if (sPlus > 0) return "§6S+ §7" + PvPages.time(sPlus);
        double sRank = PvJson.num(type, -1, "fastest_time_s", k);
        if (sRank > 0) return "§eS §7" + PvPages.time(sRank);
        double any = PvJson.num(type, -1, "fastest_time", k);
        return any > 0 ? "§7" + PvPages.time(any) : "§8—";
    }

    private static List<String> floorTooltip(JsonObject type, boolean master, int tier, String name) {
        String k = String.valueOf(tier);
        List<String> lines = new ArrayList<>();
        lines.add((master ? "§cMaster Mode " : "§a") + name);
        lines.add("§7Completions: §e" + PvPages.num(PvJson.num(type, 0, "tier_completions", k)));
        double played = PvJson.num(type, -1, "times_played", k);
        if (played >= 0) lines.add("§7Times Played: §e" + PvPages.num(played));
        double score = PvJson.num(type, -1, "best_score", k);
        if (score >= 0) lines.add("§7Best Score: §e" + PvPages.num(score));
        double sPlus = PvJson.num(type, -1, "fastest_time_s_plus", k);
        double sRank = PvJson.num(type, -1, "fastest_time_s", k);
        if (sPlus > 0) lines.add("§7Fastest §6S+§7: §a" + PvPages.time(sPlus));
        if (sRank > 0) lines.add("§7Fastest §eS§7: §a" + PvPages.time(sRank));
        if (sPlus <= 0 && sRank <= 0) {
            double any = PvJson.num(type, -1, "fastest_time", k);
            if (any > 0) lines.add("§7Fastest Completion: §a" + PvPages.time(any));
        }
        return lines;
    }
}
