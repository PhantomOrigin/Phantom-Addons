package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

final class PvRiftPage {

    private PvRiftPage() {}

    static int render(PvPages.Ctx c) {
        PvScreen s = c.s();
        float es = c.es(), ss = 0.9f * es;
        int pad = PvPages.PAD, gap = 14;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;
        int colW = (avail - gap) / 2;
        int slot = Math.min(s.fitSlot(colW / 4.6, 9999), Math.max(14, Math.round(26 * es)));
        JsonObject m = c.m();
        JsonObject rift = PvJson.obj(m, "rift");

        int yy = y0;
        int rowH = PvPages.slotRow(c, "Rift Armor", c.v().riftArmor, x0, yy, slot);
        PvPages.slotRow(c, "Rift Equipment", c.v().riftEquipment, x0 + colW + gap, yy, slot);
        yy += rowH + 12;

        yy += s.title(c.g(), "Rift Stats", x0, yy);
        int rowY = yy;
        JsonElement charms = PvJson.path(rift, "gallery", "secured_trophies");
        int charmCount = charms != null && charms.isJsonArray() ? charms.getAsJsonArray().size() : 0;
        yy += s.statRow(c.g(), "Timecharms", charmCount + "/8", x0, yy, colW, ss);
        if (charms != null && charms.isJsonArray() && s.hovered(c.mx(), c.my(), x0, rowY, colW, c.lh(ss) + 3)) {
            List<String> lines = new ArrayList<>();
            lines.add("§aTimecharms");
            for (JsonElement e : charms.getAsJsonArray()) {
                String type = e.isJsonObject() ? PvJson.str(e, "?", "type") : e.getAsString();
                lines.add("§7- §f" + PvProfileView.prettify(type));
            }
            s.tooltipLines(lines);
        }
        JsonElement souls = PvJson.path(rift, "enigma", "found_souls");
        yy += s.statRow(c.g(), "Enigma Souls", String.valueOf(souls != null && souls.isJsonArray() ? souls.getAsJsonArray().size() : 0), x0, yy, colW, ss);
        yy += s.statRow(c.g(), "McGrubber's Burgers", (int) PvJson.num(rift, 0, "castle", "grubber_stacks") + "/5", x0, yy, colW, ss);

        int ry = y0 + rowH + 12;
        ry += s.title(c.g(), "Motes", x0 + colW + gap, ry);
        int rx = x0 + colW + gap;
        ry += s.statRow(c.g(), "Motes", PvPages.num(PvJson.num(m, 0, "currencies", "motes_purse")), rx, ry, colW, ss);
        ry += s.statRow(c.g(), "Lifetime Motes", PvPages.num(PvJson.num(m, 0, "player_stats", "rift", "lifetime_motes_earned")), rx, ry, colW, ss);
        ry += s.statRow(c.g(), "Rift Visits", PvPages.num(PvJson.num(m, 0, "player_stats", "rift", "visits")), rx, ry, colW, ss);
        return Math.max(yy, ry) - y0;
    }
}
