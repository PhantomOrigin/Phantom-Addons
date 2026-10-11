package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class PvMiningPage {

    private PvMiningPage() {}

    static final long[] HOTM = { 0, 3000, 9000, 25000, 60000, 100000, 150000, 210000, 290000, 400000 };
    private static final String[][] PETS = { { "SCATHA", "Scatha" }, { "BAL", "Bal" }, { "GLACITE_GOLEM", "Glacite Golem" }, { "GOBLIN", "Goblin" } };
    private static final String[][] POWDERS = { { "mithril", "§2Mithril" }, { "gemstone", "§dGemstone" }, { "glacite", "§bGlacite" } };

    static int render(PvPages.Ctx c) {
        return switch (c.sub()) {
            case 1 -> hotm(c);
            case 2 -> commissions(c);
            default -> gear(c);
        };
    }

    // ── Gear ──────────────────────────────────────────────────────────────────

    private static int gear(PvPages.Ctx c) {
        PvScreen s = c.s();
        PvAnalysis a = c.v().analysis;
        int pad = PvPages.PAD, gap = 14;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;
        int colW = (avail - gap) / 2;
        float ls = 0.75f * c.es();

        List<String> labels = new ArrayList<>();
        List<ItemStack[]> rows = new ArrayList<>();
        for (PvAnalysis.MiningSet set : a.miningSets) { labels.add(set.name()); rows.add(set.pieces()); }
        if (rows.isEmpty()) { labels.add("Mining Armor"); rows.add(new ItemStack[4]); }
        labels.add("Mining Equipment");
        rows.add(a.miningEquipment);
        labels.add("Drills & Pickaxes");
        rows.add(a.miningTools.toArray(new ItemStack[0]));
        labels.add("Pets");
        rows.add(null);

        int gridRows = (rows.size() + 1) / 2;
        int rowGap = 10, labelH = c.lh(ls) + 3;
        int slot = s.fitSlot(colW / 4.6, (c.h() - 2 * pad + rowGap) / (double) gridRows - labelH - rowGap);
        int g = PvScreen.gapFor(slot);
        int rowH = labelH + slot;
        for (int i = 0; i < rows.size(); i++) {
            int x = x0 + (i % 2) * (colW + gap), y = y0 + (i / 2) * (rowH + rowGap);
            ItemStack[] items = rows.get(i);
            if (items == null) {
                s.text(c.g(), labels.get(i), x, y, PvTheme.dim(), ls);
                int shown = 0;
                for (String[] p : PETS) {
                    PvProfileView.Pet pet = PvPages.bestPet(c.v(), p[0], null);
                    if (pet == null) continue;
                    s.petSlot(c.g(), x + shown * (slot + g), y + labelH, slot, pet, c.mx(), c.my());
                    shown++;
                }
                if (shown == 0) s.text(c.g(), "None", x, y + labelH + (slot - c.lh(ls)) / 2, PvTheme.dim(), ls);
                continue;
            }
            for (int k = 0; k < items.length; k++) if (items[k] == null) items[k] = ItemStack.EMPTY;
            if (items.length == 0) {
                s.text(c.g(), labels.get(i), x, y, PvTheme.dim(), ls);
                s.text(c.g(), "None", x, y + labelH + (slot - c.lh(ls)) / 2, PvTheme.dim(), ls);
            } else {
                PvPages.slotRow(c, labels.get(i), items, x, y, slot);
            }
        }
        return gridRows * (rowH + rowGap) - rowGap;
    }

    // ── HotM ──────────────────────────────────────────────────────────────────

    private static int hotm(PvPages.Ctx c) {
        PvScreen s = c.s();
        float es = c.es(), ss = 0.85f * es;
        int pad = PvPages.PAD;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;
        JsonObject m = c.m();
        JsonObject tree = PvJson.obj(m, "skill_tree");
        JsonObject core = PvJson.obj(m, "mining_core");
        int preset = PvSkillGearPages.equippedPreset(tree, "mountain");
        JsonObject nodes = PvJson.obj(tree, "nodes", preset == 1 ? "mining" : "mining_" + preset);

        int infoW = Math.max(Math.round(130 * es), avail * 2 / 5);
        int yy = y0;
        double xp = PvJson.num(tree, 0, "experience", "mining");
        int level = Math.max(1, PvPages.tierFromIncrements(xp, HOTM));
        int potm = (int) PvJson.num(nodes, 0, "core_of_the_mountain");
        yy += s.title(c.g(), "Heart of the Mountain", x0, yy);
        yy += s.statRow(c.g(), "HotM Level", level + (level >= HOTM.length ? " §6(MAX)" : ""), x0, yy, infoW, ss);
        if (level < HOTM.length)
            yy += s.statRow(c.g(), "Next Level", PvPages.shortNum(PvPages.xpToNextTier(xp, HOTM, level)) + " XP", x0, yy, infoW, ss);
        yy += s.statRow(c.g(), "Peak of the Mountain", potm + "/10", x0, yy, infoW, ss);
        yy += s.statRow(c.g(), "Tokens Spent", PvPages.num(PvJson.num(tree, 0, "tokens_spent", "mountain")), x0, yy, infoW, ss);
        String ability = PvSkillGearPages.selectedAbility(tree, "mining", preset);
        yy += s.statRow(c.g(), "Ability", ability != null ? PvProfileView.prettify(ability) : "None", x0, yy, infoW, ss);
        yy += 6;
        yy += s.title(c.g(), "Powder", x0, yy);
        for (String[] p : POWDERS) {
            double cur = PvJson.num(core, 0, "powder_" + p[0]);
            double spent = PvJson.num(core, 0, "powder_spent_" + p[0]);
            int rowY = yy;
            yy += s.statRow(c.g(), p[1], PvPages.shortNum(cur) + " §7/ " + PvPages.shortNum(cur + spent), x0, yy, infoW, ss);
            if (s.hovered(c.mx(), c.my(), x0, rowY, infoW, c.lh(ss) + 3))
                s.tooltip(p[1] + " Powder", "§7Available: §e" + PvPages.num(cur), "§7Spent: §e" + PvPages.num(spent),
                        "§7Total: §e" + PvPages.num(cur + spent));
        }

        int tx = x0 + infoW + 12;
        int treeH = PvTree.render(c, PvConstants.hotmLayout(), "hotm", nodes, ability, potm, level,
                tx, y0, avail - infoW - 12, c.h() - 2 * pad);
        return Math.max(yy - y0, treeH);
    }

    // ── Commissions ───────────────────────────────────────────────────────────

    private static int commissions(PvPages.Ctx c) {
        PvScreen s = c.s();
        float es = c.es(), ss = 0.85f * es;
        int pad = PvPages.PAD, gap = 14;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;
        int colW = (avail - gap) / 2;
        JsonObject m = c.m();

        int milestone = 0;
        JsonElement tut = PvJson.path(m, "objectives", "tutorial");
        if (tut != null && tut.isJsonArray()) {
            for (JsonElement e : tut.getAsJsonArray()) {
                String t = e.getAsString();
                if (t.startsWith("commission_milestone_reward_mining_xp_tier_")) {
                    try { milestone = Math.max(milestone, Integer.parseInt(t.substring(t.lastIndexOf('_') + 1))); }
                    catch (NumberFormatException ignored) {}
                }
            }
        }

        int yy = y0;
        yy += s.title(c.g(), "Commissions", x0, yy);
        yy += s.statRow(c.g(), "Commission Milestone", milestone + "/6", x0, yy, colW, ss);
        yy += s.statRow(c.g(), "Crystal Nucleus Runs", PvPages.num(PvJson.num(m, 0, "leveling", "completions", "NUCLEUS_RUNS")), x0, yy, colW, ss);
        yy += 6;
        yy += s.title(c.g(), "Crystals", x0, yy);
        JsonObject crystals = PvJson.obj(m, "mining_core", "crystals");
        if (crystals != null) {
            for (var e : crystals.entrySet()) {
                String name = PvProfileView.prettify(e.getKey().replace("_crystal", "")) + " Crystal";
                double found = PvJson.num(e.getValue(), 0, "total_found");
                String state = PvJson.str(e.getValue(), "", "state");
                yy += s.statRow(c.g(), name, PvPages.num(found) + (state.equals("PLACED") ? " §a✔" : ""), x0, yy, colW, ss);
            }
        }

        int rx = x0 + colW + gap;
        int ry = y0;
        JsonObject glacite = PvJson.obj(m, "glacite_player_data");
        ry += s.title(c.g(), "Glacite Mineshafts", rx, ry);
        ry += s.statRow(c.g(), "Mineshafts Entered", PvPages.num(PvJson.num(glacite, 0, "mineshafts_entered")), rx, ry, colW, ss);
        ry += 4;
        ry += s.title(c.g(), "Corpses Looted", rx, ry);
        JsonObject corpses = PvJson.obj(glacite, "corpses_looted");
        List<String> order = new ArrayList<>(List.of("lapis", "tungsten", "umber", "vanguard"));
        if (corpses != null) for (String k : corpses.keySet()) if (!order.contains(k)) order.add(k);
        for (String k : order) {
            ry += s.statRow(c.g(), PvProfileView.prettify(k), PvPages.num(PvJson.num(corpses, 0, k)), rx, ry, colW, ss);
        }
        ry += 4;
        ry += s.statRow(c.g(), "Fossil Dust", PvPages.num(PvJson.num(glacite, 0, "fossil_dust")), rx, ry, colW, ss);
        JsonElement fossils = PvJson.path(glacite, "fossils_donated");
        ry += s.statRow(c.g(), "Fossils Donated", String.valueOf(fossils != null && fossils.isJsonArray() ? fossils.getAsJsonArray().size() : 0), rx, ry, colW, ss);
        return Math.max(yy, ry) - y0;
    }

}
