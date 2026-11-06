package com.phantomaddons.features.pv;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

final class PvDungeonXp {

    private PvDungeonXp() {}

    private static final double SCORE = 300;
    private static final int SHOWN_FLOORS = 4, MASTER_SHOWN = 3;
    private static final double MAX_XP = 569_809_640d;

    private static final double[] NORMAL_BASE = { 0, 110, 220, 560, 1420, 2400, 4880, 28000 };
    private static final int[] NORMAL_CAP = { 0, 75, 75, 75, 75, 75, 50, 25 };
    private static final double[] MASTER_BASE = { 0, 15000, 20000, 35000, 55000, 70000, 100000, 300000 };
    private static final int MASTER_CAP = 25;
    private static final double[] HECATOMB = { 0, 0.0028, 0.0036, 0.0044, 0.0052, 0.006, 0.0068, 0.0076, 0.0084, 0.0092, 0.01 };

    private static final double EXPLORER_PER_LEVEL = 0.01;
    private static final double GRADUATE_PER_LEVEL = 0.02;
    private static final double RING_BONUS = 0.1;
    private static final double CLASS_PERK_PER_LEVEL = 0.02;

    private static final Map<String, String> CLASS_PERK = Map.of(
            "archer", "toxophilite", "berserk", "unbridled_rage", "healer", "heart_of_gold",
            "mage", "cold_efficiency", "tank", "diamond_in_the_rough");

    record Boosts(int hecatomb, boolean ring, int bonzo, int scarf, double scarfAccessory, Map<String, Integer> perks) {}

    private record Floor(boolean master, int tier) {
        String label() { return (master ? "M" : "F") + tier; }
        double base() { return (master ? MASTER_BASE : NORMAL_BASE)[tier]; }
        int cap() { return master ? MASTER_CAP : NORMAL_CAP[tier]; }
    }

    private static final Map<PvProfileView, Boosts> BOOSTS = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    static Boosts boosts(PvProfileView v) {
        return BOOSTS.computeIfAbsent(v, PvDungeonXp::scan);
    }

    private static Boosts scan(PvProfileView v) {
        int hecatomb = 0;
        for (ItemStack it : v.allItems) {
            if (it.isEmpty() || PvGear.armorSlot(it) != 0) continue;
            hecatomb = Math.max(hecatomb, PvGear.data(it).getCompoundOrEmpty("enchantments").getIntOr("hecatomb", 0));
        }
        boolean ring = false;
        double scarfAccessory = 0;
        for (ItemStack it : v.accessories) {
            String id = PvGear.id(it);
            if (id.equals("CATACOMBS_EXPERT_RING")) ring = true;
            else if (id.equals("SCARF_GRIMOIRE")) scarfAccessory = Math.max(scarfAccessory, 0.06);
            else if (id.equals("SCARF_THESIS")) scarfAccessory = Math.max(scarfAccessory, 0.04);
            else if (id.equals("SCARF_STUDIES")) scarfAccessory = Math.max(scarfAccessory, 0.02);
        }
        Map<String, Integer> perks = new java.util.HashMap<>();
        for (var e : CLASS_PERK.entrySet()) perks.put(e.getKey(), (int) PvJson.num(v.member, 0, "player_data", "perks", e.getValue()));
        return new Boosts(Math.min(10, hecatomb), ring,
                PvHuntingPage.attributeLevel(v.member, "catacombs_explorer"),
                PvHuntingPage.attributeLevel(v.member, "catacombs_graduate"), scarfAccessory, perks);
    }

    private static double runXp(Floor f, Boosts b, String cls, double completions, int runIndex) {
        int count = (int) Math.min(f.cap(), completions + runIndex);
        double hecatomb = HECATOMB[b.hecatomb()] * (SCORE >= 300 ? 2 : 1);
        double firstFive = count < 5 ? 300 : 0;
        double score = (SCORE + firstFive) / 300;
        if (cls != null) {
            double perk = b.perks().getOrDefault(cls, 0) * CLASS_PERK_PER_LEVEL;
            return f.base() * (1 + 2 * hecatomb + perk + b.scarfAccessory() + b.scarf() * GRADUATE_PER_LEVEL) * score;
        }
        double step = (b.ring() ? 0.024 : 0.022) + hecatomb / 50;
        double additive = 0.95 + (b.ring() ? RING_BONUS : 0) + hecatomb + b.bonzo() * EXPLORER_PER_LEVEL + count * step;
        return f.base() * additive * score;
    }

    private static long runsFor(Floor f, Boosts b, String cls, double completions, double remaining) {
        if (remaining <= 0) return 0;
        long runs = 0;
        double xp = 0;
        long dynamic = Math.max(0, f.cap() - (long) Math.min(completions, f.cap()));
        while (xp < remaining && runs < dynamic) xp += runXp(f, b, cls, completions, (int) runs++);
        if (xp < remaining) {
            double stable = runXp(f, b, cls, completions, (int) runs);
            runs += (long) Math.ceil((remaining - xp) / stable);
        }
        return runs;
    }

    private static List<Floor> topFloors(JsonObject dungeons) {
        List<Floor> out = new ArrayList<>();
        for (boolean master : new boolean[] { true, false }) {
            JsonObject done = PvJson.obj(dungeons, "dungeon_types", master ? "master_catacombs" : "catacombs", "tier_completions");
            int limit = master ? MASTER_SHOWN : SHOWN_FLOORS;
            for (int tier = 7; tier >= 1 && out.size() < limit; tier--)
                if (PvJson.num(done, 0, String.valueOf(tier)) > 0) out.add(new Floor(master, tier));
        }
        return out;
    }

    static List<String> runLines(PvProfileView v, JsonObject dungeons, double xp, PvLeveling.Level level, String cls) {
        List<String> lines = new ArrayList<>();
        boolean overflow = level.level() > 50;
        double toNext = level.needed() > 0 ? level.needed() - level.into() : 0;
        double to50 = Math.max(0, MAX_XP - xp);
        if (level.maxed() && !overflow) return lines;
        List<Floor> floors = topFloors(dungeons);
        if (floors.isEmpty()) return lines;

        Boosts b = boosts(v);
        lines.add("");
        lines.add("§7Runs needed §8(S+ " + (cls != null ? "class " : "") + "estimate)");
        for (Floor f : floors) {
            double completions = PvJson.num(dungeons, 0, "dungeon_types", f.master() ? "master_catacombs" : "catacombs", "tier_completions", String.valueOf(f.tier()));
            double perRun = runXp(f, b, cls, completions, 0);
            String line = (f.master() ? "§c" : "§a") + f.label() + " §8(" + PvPages.shortNum(perRun) + "/run)§7:";
            if (toNext > 0) line += " §e" + PvPages.num(runsFor(f, b, cls, completions, toNext)) + " §7next";
            if (!overflow && to50 > 0) line += (toNext > 0 ? " §8|" : "") + " §e" + PvPages.num(runsFor(f, b, cls, completions, to50)) + " §7to 50";
            lines.add(line);
        }
        StringBuilder used = new StringBuilder("§8Hecatomb " + (b.hecatomb() > 0 ? PvPages.roman(b.hecatomb()) : "none"));
        if (cls != null) {
            used.append(" · Scarf shard ").append(b.scarf())
                .append(b.scarfAccessory() > 0 ? " · Scarf accessory +" + Math.round(b.scarfAccessory() * 100) + "%" : " · no Scarf accessory")
                .append(" · perk ").append(b.perks().getOrDefault(cls, 0));
        } else used.append(b.ring() ? " · Expert Ring" : " · no Expert Ring").append(" · Bonzo ").append(b.bonzo());
        lines.add(used.toString());
        return lines;
    }
}
