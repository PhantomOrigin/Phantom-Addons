package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class PvMiscPage {

    private PvMiscPage() {}

    static int render(PvPages.Ctx c) {
        PvScreen s = c.s();
        float es = c.es(), ss = 0.85f * es;
        int pad = PvPages.PAD, gap = 14;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;
        int colW = (avail - gap) / 2;
        int rx = x0 + colW + gap;
        JsonObject m = c.m();
        JsonObject stats = PvJson.obj(m, "player_stats");

        // ── Left: consumables + gifts ──
        int yy = y0;
        yy += s.title(c.g(), "Consumables", x0, yy);
        yy += row(c, "Reaper Peppers", PvJson.num(m, 0, "player_data", "reaper_peppers_eaten"), 5, x0, yy, colW, ss);
        yy += row(c, "Metaphysical Serums", PvJson.num(m, 0, "experimentation", "serums_drank"), 3, x0, yy, colW, ss);
        yy += row(c, "Wriggling Larva", PvJson.num(m, 0, "garden_player_data", "larva_consumed"), 0, x0, yy, colW, ss);
        yy += row(c, "McGrubber's Burgers", PvJson.num(m, 0, "rift", "castle", "grubber_stacks"), 5, x0, yy, colW, ss);
        yy += row(c, "Refined Dark Cacao Truffles", PvJson.num(m, 0, "events", "easter", "refined_dark_cacao_truffles"), 0, x0, yy, colW, ss);
        yy += row(c, "Refined Jyrre", PvJson.num(m, 0, "winter_player_data", "refined_jyrre_uses"), 0, x0, yy, colW, ss);
        yy += row(c, "Fairy Soul Exchanges", PvJson.num(m, 0, "fairy_soul", "fairy_exchanges"), 0, x0, yy, colW, ss);
        yy += 6;
        yy += s.title(c.g(), "Gifts", x0, yy);
        yy += s.statRow(c.g(), "Gifts Given", PvPages.num(PvJson.num(stats, 0, "gifts", "total_given")), x0, yy, colW, ss);
        yy += s.statRow(c.g(), "Gifts Received", PvPages.num(PvJson.num(stats, 0, "gifts", "total_received")), x0, yy, colW, ss);
        yy += 6;
        JsonObject nether = PvJson.obj(m, "nether_island_player_data");
        yy += s.title(c.g(), "Faction", x0, yy);
        String faction = PvJson.str(nether, "", "selected_faction");
        boolean mage = faction.equalsIgnoreCase("mages"), barb = faction.equalsIgnoreCase("barbarians");
        yy += s.statRow(c.g(), "Selected Faction", mage ? "§5Mages" : barb ? "§cBarbarians" : "§7None", x0, yy, colW, ss);
        yy += s.statRow(c.g(), "Mage Reputation", PvPages.num(PvJson.num(nether, 0, "mages_reputation")), x0, yy, colW, ss);
        yy += s.statRow(c.g(), "Barbarian Reputation", PvPages.num(PvJson.num(nether, 0, "barbarians_reputation")), x0, yy, colW, ss);

        // ── Right: mythological + auctions ──
        int ry = y0;
        JsonObject mythos = PvJson.obj(stats, "mythos");
        ry += s.title(c.g(), "Mythological Event", rx, ry);
        ry += s.statRow(c.g(), "Mythological Kills", PvPages.num(PvJson.num(mythos, 0, "kills")), rx, ry, colW, ss);
        ry += s.statRow(c.g(), "Burrows (Treasure)", PvPages.num(burrows(mythos, "burrows_dug_treasure")), rx, ry, colW, ss);
        ry += s.statRow(c.g(), "Burrows (Combat)", PvPages.num(burrows(mythos, "burrows_dug_combat")), rx, ry, colW, ss);
        ry += s.statRow(c.g(), "Burrows (Start)", PvPages.num(burrows(mythos, "burrows_dug_next")), rx, ry, colW, ss);
        ry += s.statRow(c.g(), "Chains Completed", PvPages.num(burrows(mythos, "burrows_chains_complete")), rx, ry, colW, ss);
        ry += 6;
        JsonObject auctions = PvJson.obj(stats, "auctions");
        ry += s.title(c.g(), "Auctions", rx, ry);
        ry += s.statRow(c.g(), "Auctions Created", PvPages.num(PvJson.num(auctions, 0, "created")), rx, ry, colW, ss);
        ry += s.statRow(c.g(), "Auctions Completed", PvPages.num(PvJson.num(auctions, 0, "completed")), rx, ry, colW, ss);
        ry += s.statRow(c.g(), "Gold Earned", PvPages.shortNum(PvJson.num(auctions, 0, "gold_earned")), rx, ry, colW, ss);
        ry += s.statRow(c.g(), "Fees Paid", PvPages.shortNum(PvJson.num(auctions, 0, "fees")), rx, ry, colW, ss);
        ry += s.statRow(c.g(), "Bids Placed", PvPages.num(PvJson.num(auctions, 0, "bids")), rx, ry, colW, ss);
        ry += s.statRow(c.g(), "Auctions Won", PvPages.num(PvJson.num(auctions, 0, "won")), rx, ry, colW, ss);
        ry += s.statRow(c.g(), "Highest Bid", PvPages.shortNum(PvJson.num(auctions, 0, "highest_bid")), rx, ry, colW, ss);
        ry += s.statRow(c.g(), "Gold Spent", PvPages.shortNum(PvJson.num(auctions, 0, "gold_spent")), rx, ry, colW, ss);
        ry += s.statRow(c.g(), "No-Bid Auctions", PvPages.num(PvJson.num(auctions, 0, "no_bids")), rx, ry, colW, ss);

        // ── Dojo: spans both columns ──
        int dy = Math.max(yy, ry) + 6;
        JsonObject dojo = PvJson.obj(nether, "dojo");
        dy += s.title(c.g(), "Dojo", x0, dy);
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
            if (i < 4) ly += s.statRow(c.g(), "Test of " + t[0], value, x0, ly, colW, ss);
            else qy += s.statRow(c.g(), "Test of " + t[0], value, rx, qy, colW, ss);
        }
        qy += s.statRow(c.g(), "Total Points", PvPages.num(total), rx, qy, colW, ss);
        qy += s.statRow(c.g(), "Belt", belt(total), rx, qy, colW, ss);
        return Math.max(ly, qy) - y0;
    }

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
