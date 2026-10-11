package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.phantomaddons.gui.GuiDraw;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

final class PvFarmingPage {

    private PvFarmingPage() {}

    private record Crop(String name, String contest, String[] garden, String milestone, String tool, String icon) {}

    private static final Crop[] CROPS = {
            new Crop("Wheat", "WHEAT", new String[] { "WHEAT" }, "WHEAT", "WHEAT", "WHEAT"),
            new Crop("Carrot", "CARROT_ITEM", new String[] { "CARROT_ITEM", "CARROT" }, "CARROT", "CARROT", "CARROT_ITEM"),
            new Crop("Potato", "POTATO_ITEM", new String[] { "POTATO_ITEM", "POTATO" }, "POTATO", "POTATO", "POTATO_ITEM"),
            new Crop("Pumpkin", "PUMPKIN", new String[] { "PUMPKIN" }, "PUMPKIN", "PUMPKIN", "PUMPKIN"),
            new Crop("Melon", "MELON", new String[] { "MELON", "MELON_SLICE" }, "MELON", "MELON", "MELON"),
            new Crop("Sugar Cane", "SUGAR_CANE", new String[] { "SUGAR_CANE" }, "SUGAR_CANE", "SUGAR_CANE", "SUGAR_CANE"),
            new Crop("Cactus", "CACTUS", new String[] { "CACTUS" }, "CACTUS", "CACTUS", "CACTUS"),
            new Crop("Cocoa Beans", "INK_SACK:3", new String[] { "INK_SACK:3", "COCOA_BEANS" }, "COCOA_BEANS", "COCOA_BEANS", "INK_SACK-3"),
            new Crop("Mushroom", "MUSHROOM_COLLECTION", new String[] { "MUSHROOM_COLLECTION", "MUSHROOM" }, "MUSHROOM", "MUSHROOM", "HUGE_MUSHROOM_1"),
            new Crop("Nether Wart", "NETHER_STALK", new String[] { "NETHER_STALK", "NETHER_WART" }, "NETHER_WART", "NETHER_STALK", "NETHER_STALK"),
            new Crop("Sunflower", "DOUBLE_PLANT", new String[] { "DOUBLE_PLANT", "SUNFLOWER" }, "SUNFLOWER", "SUNFLOWER", "DOUBLE_PLANT"),
            new Crop("Moonflower", "MOONFLOWER", new String[] { "MOONFLOWER" }, "MOONFLOWER", "MOONFLOWER", "MOONFLOWER"),
            new Crop("Wild Rose", "WILD_ROSE", new String[] { "WILD_ROSE" }, "WILD_ROSE", "WILD_ROSE", "WILD_ROSE"),
    };

    static int render(PvPages.Ctx c) {
        return switch (c.sub()) {
            case 1 -> contests(c);
            case 2 -> garden(c);
            case 3 -> milestones(c);
            default -> gear(c);
        };
    }

    // ── Gear ──────────────────────────────────────────────────────────────────

    private static int gear(PvPages.Ctx c) {
        PvScreen s = c.s();
        PvAnalysis a = c.v().analysis;
        float es = c.es(), ls = 0.75f * es;
        int pad = PvPages.PAD, gap = 14;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;
        int colW = (avail - gap) / 2;
        int slot = s.fitSlot(colW / 4.6, 9999);
        slot = Math.min(slot, Math.max(14, Math.round(24 * es)));
        int yy = y0;

        int rowH = 0;
        for (int i = 0; i < a.farmingSets.size(); i++) {
            PvAnalysis.FarmingSet set = a.farmingSets.get(i);
            rowH = Math.max(rowH, PvPages.slotRow(c, set.label(), set.armor(), x0 + i * (colW + gap), yy, slot));
        }
        yy += rowH + 10;
        rowH = PvPages.slotRow(c, "Farming Equipment", a.farmingEquipment, x0, yy, slot);
        if (a.pesthunterEquipment != null) PvPages.slotRow(c, "Pesthunter Equipment", a.pesthunterEquipment, x0 + colW + gap, yy, slot);
        yy += rowH + 10;

        PvProfileView.Pet normal = firstOwned(c.v(), "ROSE_DRAGON", "ELEPHANT", "RABBIT");
        PvProfileView.Pet pest = firstOwned(c.v(), "MOSQUITO", "SLUG");
        s.text(c.g(), "Farming Pet", x0, yy, PvTheme.dim(), ls);
        s.text(c.g(), "Pest Pet", x0 + colW + gap, yy, PvTheme.dim(), ls);
        int py = yy + c.lh(ls) + 3;
        petOrNone(c, normal, x0, py, slot);
        petOrNone(c, pest, x0 + colW + gap, py, slot);
        yy = py + slot + 12;

        yy += s.title(c.g(), "Farming Tools", x0, yy);
        int cols = Math.max(1, avail / Math.max(80, Math.round(110 * es)));
        int cw = avail / cols;
        int toolRow = slot + 4;
        float ns = 0.8f * es;
        // Every crop's tool, then the vacuum and watering can in the slots after them.
        int count = CROPS.length + 2;
        for (int i = 0; i < count; i++) {
            ItemStack tool = i < CROPS.length ? a.farmingTool(CROPS[i].tool) : i == CROPS.length ? a.vacuum : a.wateringCan;
            String label = i < CROPS.length ? CROPS[i].name : i == CROPS.length ? "Vacuum" : "Watering Can";
            int tx = x0 + (i % cols) * cw, ty = yy + (i / cols) * toolRow;
            s.slot(c.g(), tx, ty, slot, tool, c.mx(), c.my());
            s.text(c.g(), label, tx + slot + 4, ty + (slot - c.lh(ns)) / 2, tool.isEmpty() ? PvTheme.dim() : PvTheme.text(), ns);
        }
        yy += ((count + cols - 1) / cols) * toolRow;
        return yy - y0;
    }

    private static PvProfileView.Pet firstOwned(PvProfileView v, String... types) {
        for (String t : types) {
            PvProfileView.Pet p = PvPages.bestPet(v, t, null);
            if (p != null) return p;
        }
        return null;
    }

    private static void petOrNone(PvPages.Ctx c, PvProfileView.Pet pet, int x, int y, int slot) {
        if (pet != null) {
            c.s().petSlot(c.g(), x, y, slot, pet, c.mx(), c.my());
            c.s().text(c.g(), PvProfileView.rarityColor(pet.rarity()) + pet.name() + " §7Lvl " + pet.level().level(),
                    x + slot + 5, y + (slot - c.lh(0.8f * c.es())) / 2, PvTheme.text(), 0.8f * c.es());
        } else {
            c.s().slot(c.g(), x, y, slot, ItemStack.EMPTY, c.mx(), c.my());
            c.s().text(c.g(), "None", x + slot + 5, y + (slot - c.lh(0.8f * c.es())) / 2, PvTheme.dim(), 0.8f * c.es());
        }
    }

    // ── Contests ──────────────────────────────────────────────────────────────

    private static int contests(PvPages.Ctx c) {
        PvScreen s = c.s();
        float es = c.es(), ss = 0.85f * es;
        int pad = PvPages.PAD;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;
        JsonObject jacob = PvJson.obj(c.m(), "jacobs_contest");
        if (jacob == null) {
            s.text(c.g(), "No Jacob's contest data.", x0, y0, PvTheme.dim(), ss);
            return c.lh(es);
        }
        JsonObject pbs = PvJson.obj(jacob, "personal_bests");
        JsonObject contests = PvJson.obj(jacob, "contests");
        JsonObject brackets = PvJson.obj(jacob, "unique_brackets");
        int yy = y0;
        JsonObject medals = PvJson.obj(jacob, "medals_inv");
        String medalLine = "§6Gold §r" + (int) PvJson.num(medals, 0, "gold") + "  §fSilver §r" + (int) PvJson.num(medals, 0, "silver")
                + "  §cBronze §r" + (int) PvJson.num(medals, 0, "bronze");
        String medalText = "§7Medals: " + medalLine;
        s.title(c.g(), "Jacob's Contests", x0, yy);
        s.text(c.g(), medalText, x0 + avail - s.textW(medalText, ss), yy + (c.lh(es) - c.lh(ss)) / 2, PvTheme.text(), ss);
        yy += c.lh(es) + 8;

        int slot = Math.max(12, Math.round(18 * es));
        int rowH = slot + 6;
        int availH = c.h() - 2 * pad - (yy - y0);
        int cellW = slot + 5 + Math.max(s.textW("Nether Wart §6●", 0.8f * es), s.textW("PB 9,999,999  Contests 999", 0.7f * es)) + 8;
        int maxCols = Math.max(1, avail / cellW);
        int fitRows = Math.max(1, availH / rowH);
        int cols = Math.max(1, Math.min(maxCols, (CROPS.length + fitRows - 1) / fitRows));
        cols = Math.max(cols, Math.min(maxCols, 2));
        int rows = (CROPS.length + cols - 1) / cols;
        if (rows * rowH > availH) {
            rowH = Math.max(c.lh(0.8f * es) + c.lh(0.7f * es) + 3, availH / rows);
            slot = Math.max(10, Math.min(slot, rowH - 4));
        }
        int cw = avail / cols;
        for (int i = 0; i < CROPS.length; i++) {
            Crop crop = CROPS[i];
            int count = 0;
            if (contests != null) for (String k : contests.keySet()) if (k.endsWith(":" + crop.contest)) count++;
            int cx = x0 + (i / rows) * cw, cy = yy + (i % rows) * rowH;
            s.slot(c.g(), cx, cy, slot, s.repoIcon(crop.icon, Items.WHEAT), c.mx(), c.my());
            String medal = bestMedal(brackets, crop.contest);
            s.text(c.g(), crop.name + (medal.isEmpty() ? "" : " " + medal), cx + slot + 5, cy, PvTheme.text(), 0.8f * es);
            String info = "§7PB §r" + PvPages.num(PvJson.num(pbs, 0, crop.contest)) + "  §7Contests §r" + count;
            s.text(c.g(), info, cx + slot + 5, cy + c.lh(0.8f * es) + 1, PvTheme.text(), 0.7f * es);
        }
        yy += rows * rowH;
        return yy - y0;
    }

    private static String bestMedal(JsonObject brackets, String crop) {
        String[][] order = { { "diamond", "§b◆" }, { "platinum", "§3◆" }, { "gold", "§6◆" }, { "silver", "§f◆" }, { "bronze", "§c◆" } };
        if (brackets == null) return "";
        for (String[] o : order) {
            JsonElement list = brackets.get(o[0]);
            if (list != null && list.isJsonArray())
                for (JsonElement e : list.getAsJsonArray()) if (e.getAsString().equals(crop)) return o[1];
        }
        return "";
    }

    // ── Garden ────────────────────────────────────────────────────────────────

    private static int garden(PvPages.Ctx c) {
        PvScreen s = c.s();
        float es = c.es(), ss = 0.85f * es;
        int pad = PvPages.PAD, gap = 14;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;
        int colW = (avail - gap) / 2;
        JsonObject m = c.m();
        JsonObject garden = s.garden(c.v());
        JsonObject consts = PvConstants.garden();

        int yy = y0;
        yy += s.title(c.g(), "Garden", x0, yy);
        if (garden != null) {
            double xp = PvJson.num(garden, 0, "garden_experience");
            long[] costs = PvConstants.table(consts, "garden_exp");
            PvLeveling.Level lvl = costs.length > 1 ? PvLeveling.compute(xp, costs, 1, 1, costs.length) : null;
            yy += s.statRow(c.g(), "Garden Level", lvl != null ? lvl.level() + (lvl.maxed() ? " §6(MAX)" : "") : "?", x0, yy, colW, ss);
            yy += s.statRow(c.g(), "Garden XP", PvPages.num(xp), x0, yy, colW, ss);
        } else {
            String err = s.gardenError(c.v());
            s.text(c.g(), err != null ? "§cGarden data unavailable: " + err : "Loading garden data...", x0, yy, PvTheme.dim(), 0.8f * es);
            yy += c.lh(es) + 2;
        }
        yy += s.statRow(c.g(), "Copper", PvPages.num(PvJson.num(m, 0, "garden_player_data", "copper")), x0, yy, colW, ss);
        yy += s.statRow(c.g(), "Wriggling Larva", (int) PvJson.num(m, 0, "garden_player_data", "larva_consumed") + "", x0, yy, colW, ss);

        if (garden != null) {
            yy += 6;
            yy += s.title(c.g(), "Visitors", x0, yy);
            JsonObject comm = PvJson.obj(garden, "commission_data");
            yy += s.statRow(c.g(), "Visitors Served", PvPages.num(PvJson.num(comm, 0, "total_completed")), x0, yy, colW, ss);
            JsonObject visitorRarity = PvJson.obj(consts, "visitors");
            int total = visitorRarity != null ? visitorRarity.size() : 0;
            yy += s.statRow(c.g(), "Unique Visitors", (int) PvJson.num(comm, 0, "unique_npcs_served") + (total > 0 ? "/" + total : ""), x0, yy, colW, ss);
            JsonObject completed = PvJson.obj(comm, "completed");
            if (visitorRarity != null && completed != null) {
                for (String r : new String[] { "UNCOMMON", "RARE", "LEGENDARY", "MYTHIC", "SPECIAL" }) {
                    int have = 0, of = 0;
                    for (var e : visitorRarity.entrySet()) {
                        if (!e.getValue().getAsString().equals(r)) continue;
                        of++;
                        if (PvJson.num(completed, 0, e.getKey()) > 0) have++;
                    }
                    if (of > 0) yy += s.statRow(c.g(), PvProfileView.rarityColor(r) + PvProfileView.prettify(r), have + "/" + of, x0, yy, colW, ss);
                }
            }
        }

        int rx = x0 + colW + gap, ry = y0;
        ry += s.title(c.g(), "Garden Chips", rx, ry);
        JsonObject chips = PvJson.obj(m, "player_data", "garden_chips");
        if (chips == null || chips.isEmpty()) {
            s.text(c.g(), "None", rx, ry, PvTheme.dim(), ss);
            ry += c.lh(ss) + 3;
        } else {
            for (var e : chips.entrySet()) ry += s.statRow(c.g(), PvProfileView.prettify(e.getKey()), String.valueOf(e.getValue().getAsInt()), rx, ry, colW, ss);
        }
        if (garden != null) {
            ry += 6;
            ry += s.title(c.g(), "Crop Upgrades", rx, ry);
            JsonObject ups = PvJson.obj(garden, "crop_upgrade_levels");
            for (Crop crop : CROPS) {
                double lvl = gardenValue(ups, crop);
                ry += s.statRow(c.g(), crop.name, (int) lvl + "/9", rx, ry, colW, ss);
            }
        }
        return Math.max(yy, ry) - y0;
    }

    private static double gardenValue(JsonObject obj, Crop crop) {
        if (obj == null) return 0;
        for (String k : crop.garden) if (obj.has(k)) return obj.get(k).getAsDouble();
        return 0;
    }

    // ── Milestones ────────────────────────────────────────────────────────────

    private static int milestones(PvPages.Ctx c) {
        PvScreen s = c.s();
        float es = c.es();
        int pad = PvPages.PAD;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;
        JsonObject garden = s.garden(c.v());
        int yy = y0;
        yy += s.title(c.g(), "Crop Milestones", x0, yy);
        if (garden == null) {
            String err = s.gardenError(c.v());
            s.text(c.g(), err != null ? "§cGarden data unavailable: " + err : "Loading garden data...", x0, yy, PvTheme.dim(), 0.8f * es);
            return yy - y0 + c.lh(es);
        }
        JsonObject consts = PvConstants.garden();
        JsonObject collected = PvJson.obj(garden, "resources_collected");
        int slot = Math.max(12, Math.round(18 * es));
        int cols = Math.max(1, avail / Math.max(120, Math.round(170 * es)));
        int gap = 8;
        int cw = (avail - (cols - 1) * gap) / cols;
        int barH = Math.max(3, Math.round(4 * es));
        int rowH = slot + 8;
        for (int i = 0; i < CROPS.length; i++) {
            Crop crop = CROPS[i];
            double amount = gardenValue(collected, crop);
            long[] costs = PvConstants.table(PvJson.obj(consts, "crop_milestones"), crop.milestone);
            PvLeveling.Level lvl = costs.length > 0 ? PvLeveling.compute(amount, costs, 0, 0, costs.length) : null;
            int cx = x0 + (i % cols) * (cw + gap), cy = yy + (i / cols) * rowH;
            s.slot(c.g(), cx, cy, slot, s.repoIcon(crop.icon, Items.WHEAT), c.mx(), c.my());
            int tx = cx + slot + 5, tw = cw - slot - 5;
            String lv = lvl != null ? (lvl.maxed() ? "§6" : "") + PvPages.roman(lvl.level()) : "?";
            s.text(c.g(), crop.name, tx, cy, PvTheme.text(), 0.8f * es);
            s.text(c.g(), lv, tx + tw - s.textW(lv, 0.8f * es), cy, PvTheme.text(), 0.8f * es);
            int by = cy + c.lh(0.8f * es) + 3;
            double prog = lvl != null ? lvl.progress() : 0;
            GuiDraw.roundedFill(c.g(), tx, by, tx + tw, by + barH, PvTheme.barBg(), barH / 2);
            int fw = (int) Math.round(tw * Math.max(0, Math.min(1, prog)));
            if (fw > 0) GuiDraw.roundedFill(c.g(), tx, by, tx + fw, by + barH, lvl != null && lvl.maxed() ? PvTheme.accent() : PvTheme.barFg(), barH / 2);
            if (lvl != null && s.hovered(c.mx(), c.my(), cx, cy, cw, rowH - 2)) {
                s.tooltip("§a" + crop.name + " " + PvPages.roman(lvl.level()),
                        "§7Collected: §e" + PvPages.num(amount),
                        lvl.maxed() ? "§6MAX MILESTONE"
                                : "§7Next: §e" + PvPages.num(lvl.into()) + "§6/§e" + PvPages.num(lvl.needed())
                                + " §8(" + String.format(Locale.ROOT, "%.1f", lvl.progress() * 100) + "%)");
            }
        }
        yy += ((CROPS.length + cols - 1) / cols) * rowH;
        return yy - y0;
    }
}
