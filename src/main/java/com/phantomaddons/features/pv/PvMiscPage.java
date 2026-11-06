package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class PvMiscPage {

    private PvMiscPage() {}

    private record Layout(int x0, int y0, int colW, int rx, float ss) {}

    private static Layout layout(PvPages.Ctx c) {
        int pad = PvPages.PAD, gap = 14;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;
        int colW = (avail - gap) / 2;
        return new Layout(x0, y0, colW, x0 + colW + gap, 0.85f * c.es());
    }

    static int render(PvPages.Ctx c) {
        return switch (c.sub()) {
            case 1 -> gifts(c);
            case 2 -> faction(c);
            case 3 -> mythological(c);
            case 4 -> auctions(c);
            case 5 -> dojo(c);
            case 6 -> hoppity(c);
            default -> consumables(c);
        };
    }

    // ── Consumables ───────────────────────────────────────────────────────────

    private static int consumables(PvPages.Ctx c) {
        PvScreen s = c.s();
        Layout l = layout(c);
        JsonObject m = c.m();
        int yy = l.y0;
        yy += s.title(c.g(), "Consumables", l.x0, yy);
        yy += row(c, "Reaper Peppers", PvJson.num(m, 0, "player_data", "reaper_peppers_eaten"), 5, l.x0, yy, l.colW, l.ss);
        yy += row(c, "Metaphysical Serums", PvJson.num(m, 0, "experimentation", "serums_drank"), 3, l.x0, yy, l.colW, l.ss);
        yy += row(c, "Wriggling Larva", PvJson.num(m, 0, "garden_player_data", "larva_consumed"), 0, l.x0, yy, l.colW, l.ss);
        yy += row(c, "McGrubber's Burgers", PvJson.num(m, 0, "rift", "castle", "grubber_stacks"), 5, l.x0, yy, l.colW, l.ss);
        yy += row(c, "Refined Dark Cacao Truffles", PvJson.num(m, 0, "events", "easter", "refined_dark_cacao_truffles"), 0, l.x0, yy, l.colW, l.ss);
        yy += row(c, "Refined Jyrre", PvJson.num(m, 0, "winter_player_data", "refined_jyrre_uses"), 0, l.x0, yy, l.colW, l.ss);
        yy += row(c, "Fairy Soul Exchanges", PvJson.num(m, 0, "fairy_soul", "fairy_exchanges"), 0, l.x0, yy, l.colW, l.ss);
        return yy - l.y0;
    }

    // ── Gifts ─────────────────────────────────────────────────────────────────

    private static int gifts(PvPages.Ctx c) {
        PvScreen s = c.s();
        Layout l = layout(c);
        JsonObject stats = PvJson.obj(c.m(), "player_stats");
        int yy = l.y0;
        yy += s.title(c.g(), "Gifts", l.x0, yy);
        yy += s.statRow(c.g(), "Gifts Given", PvPages.num(PvJson.num(stats, 0, "gifts", "total_given")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Gifts Received", PvPages.num(PvJson.num(stats, 0, "gifts", "total_received")), l.x0, yy, l.colW, l.ss);
        return yy - l.y0;
    }

    // ── Faction ───────────────────────────────────────────────────────────────

    private static int faction(PvPages.Ctx c) {
        PvScreen s = c.s();
        Layout l = layout(c);
        JsonObject nether = PvJson.obj(c.m(), "nether_island_player_data");
        int yy = l.y0;
        yy += s.title(c.g(), "Faction", l.x0, yy);
        String faction = PvJson.str(nether, "", "selected_faction");
        boolean mage = faction.equalsIgnoreCase("mages"), barb = faction.equalsIgnoreCase("barbarians");
        yy += s.statRow(c.g(), "Selected Faction", mage ? "§5Mages" : barb ? "§cBarbarians" : "§7None", l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Mage Reputation", PvPages.num(PvJson.num(nether, 0, "mages_reputation")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Barbarian Reputation", PvPages.num(PvJson.num(nether, 0, "barbarians_reputation")), l.x0, yy, l.colW, l.ss);
        return yy - l.y0;
    }

    // ── Mythological Event ────────────────────────────────────────────────────

    private static int mythological(PvPages.Ctx c) {
        PvScreen s = c.s();
        Layout l = layout(c);
        JsonObject mythos = PvJson.obj(PvJson.obj(c.m(), "player_stats"), "mythos");
        int yy = l.y0;
        yy += s.title(c.g(), "Mythological Event", l.x0, yy);
        yy += s.statRow(c.g(), "Mythological Kills", PvPages.num(PvJson.num(mythos, 0, "kills")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Burrows (Treasure)", PvPages.num(burrows(mythos, "burrows_dug_treasure")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Burrows (Combat)", PvPages.num(burrows(mythos, "burrows_dug_combat")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Burrows (Start)", PvPages.num(burrows(mythos, "burrows_dug_next")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Chains Completed", PvPages.num(burrows(mythos, "burrows_chains_complete")), l.x0, yy, l.colW, l.ss);
        return yy - l.y0;
    }

    // ── Auctions ──────────────────────────────────────────────────────────────

    private static int auctions(PvPages.Ctx c) {
        PvScreen s = c.s();
        Layout l = layout(c);
        JsonObject auctions = PvJson.obj(PvJson.obj(c.m(), "player_stats"), "auctions");
        int yy = l.y0, ry = l.y0;
        yy += s.title(c.g(), "Selling", l.x0, yy);
        yy += s.statRow(c.g(), "Auctions Created", PvPages.num(PvJson.num(auctions, 0, "created")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Auctions Completed", PvPages.num(PvJson.num(auctions, 0, "completed")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Gold Earned", PvPages.shortNum(PvJson.num(auctions, 0, "gold_earned")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Fees Paid", PvPages.shortNum(PvJson.num(auctions, 0, "fees")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "No-Bid Auctions", PvPages.num(PvJson.num(auctions, 0, "no_bids")), l.x0, yy, l.colW, l.ss);

        ry += s.title(c.g(), "Buying", l.rx, ry);
        ry += s.statRow(c.g(), "Bids Placed", PvPages.num(PvJson.num(auctions, 0, "bids")), l.rx, ry, l.colW, l.ss);
        ry += s.statRow(c.g(), "Auctions Won", PvPages.num(PvJson.num(auctions, 0, "won")), l.rx, ry, l.colW, l.ss);
        ry += s.statRow(c.g(), "Highest Bid", PvPages.shortNum(PvJson.num(auctions, 0, "highest_bid")), l.rx, ry, l.colW, l.ss);
        ry += s.statRow(c.g(), "Gold Spent", PvPages.shortNum(PvJson.num(auctions, 0, "gold_spent")), l.rx, ry, l.colW, l.ss);
        return Math.max(yy, ry) - l.y0;
    }

    // ── Dojo ──────────────────────────────────────────────────────────────────

    private static int dojo(PvPages.Ctx c) {
        PvScreen s = c.s();
        Layout l = layout(c);
        JsonObject dojo = PvJson.obj(PvJson.obj(c.m(), "nether_island_player_data"), "dojo");
        int dy = l.y0;
        dy += s.title(c.g(), "Dojo", l.x0, dy);
        double total = 0;
        int ly = dy, qy = dy;
        for (int i = 0; i < DOJO.length; i++) {
            String[] t = DOJO[i];
            double pts = PvJson.num(dojo, 0, "dojo_points_" + t[1]);
            double ms = PvJson.num(dojo, 0, "dojo_time_" + t[1]);
            total += pts;
            String value = pts > 0
                    ? grade(pts) + " §7· §f" + PvPages.num(pts) + (ms > 0 ? " §7(" + time(ms) + ")" : "")
                    : "§7Not attempted";
            if (i < 4) ly += s.statRow(c.g(), "Test of " + t[0], value, l.x0, ly, l.colW, l.ss);
            else qy += s.statRow(c.g(), "Test of " + t[0], value, l.rx, qy, l.colW, l.ss);
        }
        qy += s.statRow(c.g(), "Total Points", PvPages.num(total), l.rx, qy, l.colW, l.ss);
        qy += s.statRow(c.g(), "Belt", belt(total), l.rx, qy, l.colW, l.ss);
        return Math.max(ly, qy) - l.y0;
    }

    // ── Hoppity (Chocolate Factory) ───────────────────────────────────────────

    private static final String[][] EMPLOYEES = {
            { "rabbit_bro", "Rabbit Bro" }, { "rabbit_cousin", "Rabbit Cousin" }, { "rabbit_sis", "Rabbit Sis" },
            { "rabbit_father", "Rabbit Father" }, { "rabbit_grandma", "Rabbit Grandma" },
            { "rabbit_uncle", "Rabbit Uncle" }, { "rabbit_dog", "Rabbit Dog" },
    };

    private static int hoppity(PvPages.Ctx c) {
        PvScreen s = c.s();
        Layout l = layout(c);
        JsonObject easter = PvJson.obj(c.m(), "events", "easter");
        int yy = l.y0, ry = l.y0;

        // ── Left: factory + upgrades ──
        yy += s.title(c.g(), "Chocolate Factory", l.x0, yy);
        yy += s.statRow(c.g(), "Chocolate", PvPages.shortNum(PvJson.num(easter, 0, "chocolate")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Since Prestige", PvPages.shortNum(PvJson.num(easter, 0, "chocolate_since_prestige")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "All-Time Chocolate", PvPages.shortNum(PvJson.num(easter, 0, "total_chocolate")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Chocolate Spent", PvPages.shortNum(PvJson.num(easter, 0, "shop", "chocolate_spent")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Prestige", PvPages.num(PvJson.num(easter, 0, "chocolate_level")), l.x0, yy, l.colW, l.ss);
        yy += 6;
        yy += s.title(c.g(), "Upgrades", l.x0, yy);
        yy += s.statRow(c.g(), "Click Upgrades", PvPages.num(PvJson.num(easter, 0, "click_upgrades")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Chocolate Multiplier", PvPages.num(PvJson.num(easter, 0, "chocolate_multiplier_upgrades")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Rabbit Rarity", PvPages.num(PvJson.num(easter, 0, "rabbit_rarity_upgrades")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Rabbit Barn", PvPages.num(PvJson.num(easter, 0, "rabbit_barn_capacity_level")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Cocoa Fortune", PvPages.num(PvJson.num(easter, 0, "shop", "cocoa_fortune_upgrades")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Hitman Slots", PvPages.num(PvJson.num(easter, 0, "rabbit_hitmen", "rabbit_hitmen_slots")), l.x0, yy, l.colW, l.ss);
        yy += 6;
        yy += s.title(c.g(), "Time Tower", l.x0, yy);
        yy += s.statRow(c.g(), "Level", PvPages.num(PvJson.num(easter, 0, "time_tower", "level")), l.x0, yy, l.colW, l.ss);
        yy += s.statRow(c.g(), "Charges", PvPages.num(PvJson.num(easter, 0, "time_tower", "charges")), l.x0, yy, l.colW, l.ss);

        // ── Right: employees + rabbits ──
        ry += s.title(c.g(), "Employees", l.rx, ry);
        for (String[] e : EMPLOYEES) {
            ry += s.statRow(c.g(), e[1], "Lvl " + PvPages.num(PvJson.num(easter, 0, "employees", e[0])), l.rx, ry, l.colW, l.ss);
        }
        ry += 6;
        JsonObject rabbits = PvJson.obj(easter, "rabbits");
        int unique = 0;
        long found = 0;
        if (rabbits != null) {
            for (var en : rabbits.entrySet()) {
                JsonElement v = en.getValue();
                if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber()) continue;
                unique++;
                found += v.getAsLong();
            }
        }
        ry += s.title(c.g(), "Rabbits", l.rx, ry);
        ry += s.statRow(c.g(), "Unique Rabbits", PvPages.num(unique), l.rx, ry, l.colW, l.ss);
        ry += s.statRow(c.g(), "Total Found", PvPages.num(found), l.rx, ry, l.colW, l.ss);
        ry += s.statRow(c.g(), "Egg Locations", PvPages.num(locations(rabbits)), l.rx, ry, l.colW, l.ss);
        ry += s.statRow(c.g(), "Supreme Chocolate Bars", PvPages.num(PvJson.num(easter, 0, "supreme_chocolate_bars")), l.rx, ry, l.colW, l.ss);
        ry += s.statRow(c.g(), "Dark Cacao Truffles", PvPages.num(PvJson.num(easter, 0, "refined_dark_cacao_truffles")), l.rx, ry, l.colW, l.ss);
        return Math.max(yy, ry) - l.y0;
    }

    private static int locations(JsonObject rabbits) {
        JsonObject loc = PvJson.obj(rabbits, "collected_locations");
        if (loc == null) return 0;
        int n = 0;
        for (var en : loc.entrySet()) if (en.getValue().isJsonArray()) n += en.getValue().getAsJsonArray().size();
        return n;
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private static final String[][] DOJO = {
            { "Force", "mob_kb" }, { "Stamina", "wall_jump" }, { "Mastery", "archer" }, { "Discipline", "sword_swap" },
            { "Swiftness", "snake" }, { "Control", "lock_head" }, { "Tenacity", "fireball" },
    };

    private static String grade(double pts) {
        return pts >= 1000 ? "§6S" : pts >= 800 ? "§aA" : pts >= 600 ? "§bB" : pts >= 400 ? "§eC" : pts >= 200 ? "§cD" : "§7F";
    }

    private static String belt(double total) {
        return total >= 7000 ? "§8Black" : total >= 6000 ? "§6Brown" : total >= 4000 ? "§9Blue"
                : total >= 2000 ? "§aGreen" : total >= 1000 ? "§eYellow" : "§fWhite";
    }

    private static String time(double ms) {
        int tenths = (int) Math.round(ms / 100);
        return (tenths / 600) + ":" + String.format("%04.1f", (tenths % 600) / 10.0);
    }

    private static int row(PvPages.Ctx c, String label, double value, int max, int x, int y, int w, float ss) {
        return c.s().statRow(c.g(), label, (int) value + (max > 0 ? "/" + max : ""), x, y, w, ss);
    }

    private static double burrows(JsonObject mythos, String key) {
        JsonElement e = PvJson.path(mythos, key);
        if (e == null) return 0;
        if (e.isJsonPrimitive()) return e.getAsDouble();
        if (e.isJsonObject() && e.getAsJsonObject().has("total")) return e.getAsJsonObject().get("total").getAsDouble();
        double sum = 0;
        if (e.isJsonObject()) for (var en : e.getAsJsonObject().entrySet()) if (en.getValue().isJsonPrimitive()) sum += en.getValue().getAsDouble();
        return sum;
    }
}
