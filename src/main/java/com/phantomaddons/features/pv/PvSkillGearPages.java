package com.phantomaddons.features.pv;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class PvSkillGearPages {

    private PvSkillGearPages() {}

    static final long[] HOTF = { 0, 3000, 9000, 25000, 60000, 100000, 150000, 200000 };

    private static final java.util.Map<String, String> SKILL_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    static String petSkill(PvProfileView.Pet pet) {
        String cached = SKILL_CACHE.get(pet.type());
        if (cached != null) return cached;
        PvIconRepo.Pet repoPet = PvIconRepo.pet(pet.type());
        if (repoPet != null && !repoPet.skill().isEmpty()) {
            SKILL_CACHE.put(pet.type(), repoPet.skill());
            return repoPet.skill();
        }
        int idx = PvProfileView.rarityIndex(pet.rarity());
        for (int r : new int[] { idx, 4, 3, 5, 2, 1, 0 }) {
            String id = pet.type() + ";" + r;
            PvItems.RepoItem item = PvItems.repoItem(id);
            if (item != null) {
                String skill = "";
                for (String line : PvGear.lore(item.stack())) {
                    if (line.endsWith(" Pet")) { skill = line.substring(0, line.length() - 4).trim(); break; }
                }
                SKILL_CACHE.put(pet.type(), skill);
                return skill;
            }
            if (!PvItems.repoMissing(id)) return null;
        }
        SKILL_CACHE.put(pet.type(), "");
        return "";
    }

    static String selectedAbility(JsonObject tree, String kind, int preset) {
        String own = PvJson.str(tree, null, "selected_ability", preset == 1 ? kind : kind + "_" + preset);
        return own;
    }

    private static final String[] FORAGING_PETS = { "JADE_DRAGON", "SLOTH", "FROG", "MONKEY", "OCELOT" };

    private static PvProfileView.Pet bestForagingPet(PvProfileView v) {
        for (String type : FORAGING_PETS) {
            PvProfileView.Pet p = bestByRarity(v, type);
            if (p != null) return p;
        }
        List<PvProfileView.Pet> any = petsOfSkill(v, "Foraging");
        return any.isEmpty() ? null : any.get(0);
    }

    static List<PvProfileView.Pet> petsOfSkill(PvProfileView v, String skill) {
        List<PvProfileView.Pet> out = new ArrayList<>();
        for (PvProfileView.Pet p : v.pets) if (skill.equals(petSkill(p))) out.add(p);
        out.sort(Comparator.comparingInt((PvProfileView.Pet p) -> -PvProfileView.rarityIndex(p.rarity()))
                .thenComparingInt(p -> -p.level().level())
                .thenComparingDouble(p -> -p.exp()));
        return out;
    }

    // ── Foraging ──────────────────────────────────────────────────────────────

    static int foraging(PvPages.Ctx c) {
        PvScreen s = c.s();
        PvAnalysis a = c.v().analysis;
        float es = c.es(), grow = (float) Math.sqrt(c.room()), ss = 0.85f * es * grow, ls = 0.75f * es * grow;
        int pad = PvPages.PAD, gap = 12;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;
        int leftW = Math.max(Math.round(150 * es), avail / 2);
        int half = (leftW - gap) / 2;
        int slot = Math.min(s.fitSlot(half / 4.6, 9999), Math.max(12, Math.round(20 * es * c.room())));

        // ── Gear ──
        int yy = y0;
        int rowH = PvPages.slotRow(c, "Armor", a.foragingArmor, x0, yy, slot);
        PvPages.slotRow(c, "Equipment", a.foragingEquipment, x0 + half + gap, yy, slot);
        yy += rowH + 6;
        PvPages.slotRow(c, "Axe", new ItemStack[] { a.foragingAxe }, x0, yy, slot);
        int px = x0 + half + gap;
        s.text(c.g(), "Best Foraging Pet", px, yy, PvTheme.dim(), ls);
        PvProfileView.Pet bestPet = bestForagingPet(c.v());
        int py = yy + c.lh(ls) + 3;
        if (bestPet != null) s.petSlot(c.g(), px, py, slot, bestPet, c.mx(), c.my());
        else s.text(c.g(), "None", px, py + (slot - c.lh(ls)) / 2, PvTheme.dim(), ls);
        yy = py + slot + 8;

        // ── Heart of the Forest ──
        JsonObject tree = PvJson.obj(c.m(), "skill_tree");
        double xp = PvJson.num(tree, 0, "experience", "foraging");
        int level = Math.max(1, PvPages.tierFromIncrements(xp, HOTF));
        int preset = equippedPreset(tree, "forest");
        JsonObject nodes = PvJson.obj(tree, "nodes", preset == 1 ? "foraging" : "foraging_" + preset);
        int center = (int) PvJson.num(nodes, 0, "center_of_the_forest");
        yy += s.title(c.g(), "Heart of the Forest", x0, yy);
        yy += s.statRow(c.g(), "HotF Tier", level + (level >= HOTF.length ? " §6(MAX)" : ""), x0, yy, leftW, ss);
        if (level < HOTF.length)
            yy += s.statRow(c.g(), "Next Tier", PvPages.shortNum(PvPages.xpToNextTier(xp, HOTF, level)) + " XP", x0, yy, leftW, ss);
        yy += s.statRow(c.g(), "Center of the Forest", center >= 5 ? "§6Level 5 (MAX)" : "Level " + center + "§7/5", x0, yy, leftW, ss);
        yy += s.statRow(c.g(), "Tokens Spent", PvPages.num(PvJson.num(tree, 0, "tokens_spent", preset == 1 ? "forest" : "forest_" + preset)), x0, yy, leftW, ss);

        // ── Whispers ──
        JsonObject whispers = PvJson.obj(c.m(), "foraging_core", "whispers");
        if (whispers != null) {
            yy += 4;
            for (var e : whispers.entrySet()) {
                if (!e.getValue().isJsonObject()) continue;
                JsonObject w = e.getValue().getAsJsonObject();
                double total = PvJson.num(w, 0, "total");
                double spent = 0;
                for (int i = 1; i <= 5; i++) spent += PvJson.num(w, 0, String.valueOf(i), "spent");
                int rowY = yy;
                String label = PvProfileView.prettify(e.getKey()) + " Whispers";
                yy += s.statRow(c.g(), label, PvPages.shortNum(total - spent) + " §7/ " + PvPages.shortNum(total), x0, yy, leftW, ss);
                if (s.hovered(c.mx(), c.my(), x0, rowY, leftW, c.lh(ss) + 3))
                    s.tooltip("§3" + label, "§7Available: §e" + PvPages.num(total - spent),
                            "§7Spent: §e" + PvPages.num(spent), "§7Total earned: §e" + PvPages.num(total));
            }
        }

        // ── Tree ──
        int tx = x0 + leftW + gap;
        String ability = selectedAbility(tree, "foraging", preset);
        int treeH = PvTree.render(c, PvConstants.hotfLayout(), "hotf", nodes, ability, 0, level,
                tx, y0, avail - leftW - gap, c.h() - 2 * pad);
        return Math.max(yy - y0, treeH);
    }


    static int equippedPreset(JsonObject tree, String tokenKey) {
        String kind = tokenKey.equals("mountain") ? "mining" : "foraging";
        int selected = (int) PvJson.num(tree, -1, "selected_skill_tree_slot", kind);
        if (selected >= 1 && selected <= 5) return selected;
        int best = 1;
        double most = PvJson.num(tree, 0, "tokens_spent", tokenKey);
        for (int i = 2; i <= 5; i++) {
            double t = PvJson.num(tree, 0, "tokens_spent", tokenKey + "_" + i);
            if (t > most) { most = t; best = i; }
        }
        return best;
    }

    // ── Fishing ───────────────────────────────────────────────────────────────

    private static final String[][] TROPHY_FISH = {
            { "sulphur_skitter", "Sulphur Skitter", "§f" }, { "obfuscated_fish_1", "Obfuscated 1", "§f" },
            { "steaming_hot_flounder", "Steaming-Hot Flounder", "§f" }, { "gusher", "Gusher", "§f" },
            { "blobfish", "Blobfish", "§f" }, { "obfuscated_fish_2", "Obfuscated 2", "§a" },
            { "slugfish", "Slugfish", "§a" }, { "flyfish", "Flyfish", "§a" },
            { "obfuscated_fish_3", "Obfuscated 3", "§9" }, { "lava_horse", "Lavahorse", "§9" },
            { "mana_ray", "Mana Ray", "§9" }, { "volcanic_stonefish", "Volcanic Stonefish", "§9" },
            { "vanille", "Vanille", "§9" }, { "skeleton_fish", "Skeleton Fish", "§5" },
            { "moldfin", "Moldfin", "§5" }, { "soul_fish", "Soul Fish", "§5" },
            { "karate_fish", "Karate Fish", "§5" }, { "golden_fish", "Golden Fish", "§6" },
    };
    private static final String[] TIERS = { "bronze", "silver", "gold", "diamond" };
    private static final String[] TIER_COLORS = { "§8", "§7", "§6", "§b" };
    private static final String[] TIER_NAMES = { "Bronze", "Silver", "Gold", "Diamond" };
    private static final String[][] FISHING_PETS = {
            { "FLYING_FISH", "Flying Fish" }, { "AMMONITE", "Ammonite" }, { "SQUID", "Squid" }, { "BABY_YETI", "Baby Yeti" } };

    static int fishing(PvPages.Ctx c) {
        PvScreen s = c.s();
        PvAnalysis a = c.v().analysis;
        float es = c.es(), ls = 0.75f * es;
        int pad = PvPages.PAD, gap = 14;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;
        int availH = c.h() - 2 * pad;
        int leftW = Math.max(Math.round(110 * es), avail * 2 / 5);
        int leftRows = a.trophyHunterArmor != null ? 5 : 4;
        int slot = Math.min(s.fitSlot(leftW / 4.6, (availH - leftRows * (c.lh(ls) + 9)) / (double) leftRows), Math.max(12, Math.round(22 * es * c.room())));

        // ── Left: gear and pets ──
        int yy = y0;
        yy += PvPages.slotRow(c, "Fishing Armor", a.fishingArmor, x0, yy, slot) + 8;
        yy += PvPages.slotRow(c, "Fishing Equipment", a.fishingEquipment, x0, yy, slot) + 8;
        if (a.trophyHunterArmor != null)
            yy += PvPages.slotRow(c, "Trophy Hunter Armor", a.trophyHunterArmor, x0, yy, slot) + 8;
        yy += PvPages.slotRow(c, "Water / Lava Rod", new ItemStack[] { a.waterRod, a.lavaRod }, x0, yy, slot) + 8;
        s.text(c.g(), "Pets", x0, yy, PvTheme.dim(), ls);
        int py = yy + c.lh(ls) + 3, g = PvScreen.gapFor(slot);
        for (int i = 0; i < FISHING_PETS.length; i++) {
            PvProfileView.Pet pet = bestByRarity(c.v(), FISHING_PETS[i][0]);
            int px = x0 + i * (slot + g);
            if (pet != null) {
                s.petSlot(c.g(), px, py, slot, pet, c.mx(), c.my());
            } else {
                s.slotNoTooltip(c.g(), px, py, slot, ItemStack.EMPTY);
                if (s.hovered(c.mx(), c.my(), px, py, slot, slot)) s.tooltip("§7" + FISHING_PETS[i][1], "§8Not owned");
            }
        }
        yy = py + slot;

        // ── Right: trophy fish ──
        JsonObject trophy = PvJson.obj(c.m(), "trophy_fish");
        int rx = x0 + leftW + gap, rw = avail - leftW - gap;
        int ry = y0;
        float ss = 0.8f * es;
        s.title(c.g(), "Trophy Fish", rx, ry);
        int rank = 0;
        com.google.gson.JsonElement rewards = PvJson.path(trophy, "rewards");
        if (rewards != null && rewards.isJsonArray()) {
            for (var e : rewards.getAsJsonArray()) {
                if (!e.isJsonPrimitive()) continue;
                int r = e.getAsInt();
                if (r >= 1 && r <= TIER_NAMES.length) rank = Math.max(rank, r);
            }
        }
        String rankText = rank > 0 ? TIER_COLORS[rank - 1] + TIER_NAMES[rank - 1] + " Hunter" : "§8No rank";
        s.text(c.g(), rankText, rx + rw - s.textW(rankText, ss), ry + (c.lh(es) - c.lh(ss)) / 2, PvTheme.text(), ss);
        ry += c.lh(es) + 6;
        s.text(c.g(), "§7Caught §r" + PvPages.num(PvJson.num(trophy, 0, "total_caught")), rx, ry, PvTheme.text(), ss);
        ry += c.lh(ss) + 6;

        int cols = 2, rows = (TROPHY_FISH.length + cols - 1) / cols;
        float grow = (float) Math.sqrt(c.room());
        float ns = 0.72f * es * grow, ts = 0.62f * es * grow;
        int rowH = Math.max(c.lh(ns) + c.lh(ts) + 3, Math.min(Math.round(24 * es * c.room()), (y0 + availH - ry) / rows));
        int icon = Math.max(10, Math.min(rowH - 3, Math.round(18 * es * c.room())));
        int cw = (rw - 6) / cols;

        long[][] allCounts = new long[TROPHY_FISH.length][4];
        long[] totals = new long[TROPHY_FISH.length];
        int textArea = cw - icon - 6;
        int nameNeed = 1, tiersStd = 1, tiersCompact = 1;
        for (int i = 0; i < TROPHY_FISH.length; i++) {
            for (int t = 0; t < 4; t++) allCounts[i][t] = (long) PvJson.num(trophy, 0, TROPHY_FISH[i][0] + "_" + TIERS[t]);
            totals[i] = (long) PvJson.num(trophy, 0, TROPHY_FISH[i][0]);
            nameNeed = Math.max(nameNeed, s.textW(TROPHY_FISH[i][1], 1f) + 6 + s.textW(PvPages.num(totals[i]), 1f));
            tiersStd = Math.max(tiersStd, s.textW(tiersLine(allCounts[i], false), 1f));
            tiersCompact = Math.max(tiersCompact, s.textW(tiersLine(allCounts[i], true), 1f));
        }
        ns = Math.min(ns, Math.max(0.35f, textArea / (float) nameNeed));
        boolean compact = textArea / (float) tiersStd < 0.8f * ts;
        ts = Math.min(ts, Math.max(0.35f, textArea / (float) (compact ? tiersCompact : tiersStd)));
        for (int i = 0; i < TROPHY_FISH.length; i++) {
            String[] fish = TROPHY_FISH[i];
            int cx = rx + (i / rows) * (cw + 6), cy = ry + (i % rows) * rowH;
            long[] counts = allCounts[i];
            int best = -1;
            for (int t = 0; t < 4; t++) if (counts[t] > 0) best = t;
            long total = totals[i];
            String iconId = fish[0].toUpperCase(java.util.Locale.ROOT) + "_" + TIERS[Math.max(0, best)].toUpperCase(java.util.Locale.ROOT);
            s.slotNoTooltip(c.g(), cx, cy + (rowH - icon) / 2 - 1, icon, s.repoIcon(iconId, net.minecraft.world.item.Items.COD));
            int tx = cx + icon + 4;
            String name = (total > 0 ? fish[2] : "§8") + fish[1];
            String tot = PvPages.num(total);
            s.text(c.g(), name, tx, cy, PvTheme.text(), ns);
            s.text(c.g(), "§7" + tot, cx + cw - s.textW(tot, ns) - 2, cy, PvTheme.text(), ns);
            s.text(c.g(), tiersLine(counts, compact), tx, cy + c.lh(ns) + 1, PvTheme.text(), ts);
            if (s.hovered(c.mx(), c.my(), cx, cy, cw, rowH)) {
                List<String> lines = new ArrayList<>();
                lines.add(fish[2] + fish[1]);
                lines.add("§7Caught: §e" + PvPages.num(total));
                for (int t = 0; t < 4; t++) lines.add(TIER_COLORS[t] + TIER_NAMES[t] + "§7: §e" + PvPages.num(counts[t]));
                s.tooltipLines(lines);
            }
        }
        ry += rows * rowH;
        return Math.max(yy, ry) - y0;
    }

    private static String tiersLine(long[] counts, boolean compact) {
        StringBuilder out = new StringBuilder();
        for (int t = 0; t < 4; t++) {
            if (t > 0) out.append(compact ? " " : "  ");
            out.append(counts[t] > 0 ? TIER_COLORS[t] : "§8").append(compact ? "●" : "● ")
                    .append(counts[t] > 0 ? "§r" : "§8").append(PvPages.num(counts[t]));
        }
        return out.toString();
    }

    private static PvProfileView.Pet bestByRarity(PvProfileView v, String type) {
        PvProfileView.Pet best = null;
        for (PvProfileView.Pet p : v.pets) {
            if (!p.type().equals(type)) continue;
            if (best == null) { best = p; continue; }
            int r = PvProfileView.rarityIndex(p.rarity()), br = PvProfileView.rarityIndex(best.rarity());
            if (r > br || (r == br && p.exp() > best.exp())) best = p;
        }
        return best;
    }
}
