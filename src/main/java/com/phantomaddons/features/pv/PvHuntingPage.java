package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.phantomaddons.gui.GuiDraw;
import net.minecraft.world.item.Items;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class PvHuntingPage {

    private PvHuntingPage() {}

    private static final String[] RARITIES = { "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY" };
    private static final String[] LABELS = { "§fCommon", "§aUncommon", "§9Rare", "§5Epic", "§6Legendary" };
    private static final DateTimeFormatter CAPTURED =
            DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private record Shard(String name, String ability, String icon, String rarity, List<String> family,
                         int syphoned, int maxSyphon, int owned, long captured, int level, boolean consumable) {
        boolean obtained() { return syphoned > 0 || owned > 0 || captured > 0; }
    }

    static int render(PvPages.Ctx c) {
        PvScreen s = c.s();
        float es = c.es(), ss = 0.85f * es;
        int pad = PvPages.PAD;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;
        JsonObject consts = PvConstants.attributeShards();
        if (consts == null) {
            s.text(c.g(), "Loading shard data...", x0, y0, PvTheme.dim(), ss);
            return c.lh(es);
        }
        List<Shard> all = shards(c.m(), consts);

        int yy = y0;
        yy += s.searchBox(c.g(), c.key(), "Search shards, abilities, or families", x0, yy, avail, c.mx(), c.my()) + 6;
        String query = s.search(c.key()).trim().toLowerCase(Locale.ROOT);
        yy += s.innerTabsRow(c.g(), c.key(), LABELS, x0, yy, avail, c.mx(), c.my()) + 4;

        int attributes = 0, unlocked = 0, maxed = 0;
        long syphoned = 0, maxTotal = 0;
        for (Shard sh : all) {
            if (!sh.consumable) continue;
            attributes++;
            if (sh.syphoned > 0) unlocked++;
            if (sh.maxSyphon > 0 && sh.syphoned >= sh.maxSyphon) maxed++;
            syphoned += Math.min(sh.syphoned, sh.maxSyphon);
            maxTotal += sh.maxSyphon;
        }
        String summary = "§7Unlocked: §f" + unlocked + " / " + attributes
                + "   §7Syphoned: §f" + PvPages.num(syphoned) + " / " + PvPages.num(maxTotal)
                + "   §7Maxed Shards: §f" + maxed;
        s.text(c.g(), summary, x0 + (avail - s.textW(summary, 0.75f * es)) / 2, yy, PvTheme.text(), 0.75f * es);
        yy += c.lh(0.75f * es) + 6;

        String rarity = RARITIES[Math.min(s.innerTab(c.key()), RARITIES.length - 1)];
        List<Shard> shown = new ArrayList<>();
        for (Shard sh : all) {
            if (query.isEmpty() ? sh.rarity.equals(rarity) : matches(sh, query)) shown.add(sh);
        }
        shown.sort(Comparator.comparing(Shard::name));
        if (shown.isEmpty()) {
            s.text(c.g(), "No shards match \"" + query + "\".", x0, yy, PvTheme.dim(), ss);
            return yy - y0 + c.lh(ss);
        }
        yy += grid(c, shown, x0, yy, avail);
        return yy - y0;
    }

    private static boolean matches(Shard sh, String q) {
        if (sh.name.toLowerCase(Locale.ROOT).contains(q) || sh.ability.toLowerCase(Locale.ROOT).contains(q)) return true;
        for (String f : sh.family) if (f.toLowerCase(Locale.ROOT).contains(q)) return true;
        return false;
    }

    private static List<Shard> shards(JsonObject m, JsonObject consts) {
        JsonObject stacks = PvJson.obj(m, "attributes", "stacks");
        Map<String, JsonObject> owned = new HashMap<>();
        JsonElement ownedList = PvJson.path(m, "shards", "owned");
        if (ownedList != null && ownedList.isJsonArray())
            for (JsonElement e : ownedList.getAsJsonArray())
                if (e.isJsonObject()) owned.put(PvJson.str(e, "", "type"), e.getAsJsonObject());
        JsonObject levelling = PvJson.obj(consts, "attribute_levelling");
        java.util.Set<String> unconsumable = new java.util.HashSet<>();
        JsonElement un = consts.get("unconsumable_attributes");
        if (un != null && un.isJsonArray()) for (JsonElement e : un.getAsJsonArray()) unconsumable.add(e.getAsString());

        List<JsonObject> defs = new ArrayList<>();
        JsonElement attrs = consts.get("attributes");
        if (attrs != null && attrs.isJsonArray()) {
            for (JsonElement e : attrs.getAsJsonArray()) if (e.isJsonObject()) defs.add(e.getAsJsonObject());
        } else if (attrs != null && attrs.isJsonObject()) {
            for (var e : attrs.getAsJsonObject().entrySet()) if (e.getValue().isJsonObject()) defs.add(e.getValue().getAsJsonObject());
        }

        List<Shard> out = new ArrayList<>();
        for (JsonObject a : defs) {
            String internal = PvJson.str(a, "", "internalName");
            String key = internal.replaceFirst("^ATTRIBUTE_SHARD_", "").replaceFirst(";\\d+$", "").toLowerCase(Locale.ROOT);
            String rarity = PvJson.str(a, "COMMON", "rarity");
            String type = PvJson.str(a, "", "bazaarName").replaceFirst("^SHARD_", "");
            int syph = (int) PvJson.num(stacks, 0, key);
            JsonObject own = owned.get(type);
            List<String> family = new ArrayList<>();
            JsonElement fam = a.get("family");
            if (fam != null && fam.isJsonArray()) for (JsonElement f : fam.getAsJsonArray()) family.add(f.getAsString());
            out.add(new Shard(PvJson.str(a, PvProfileView.prettify(type), "displayName"), PvJson.str(a, "", "abilityName"),
                    internal, rarity, family, syph, maxSyphon(levelling, rarity),
                    own != null ? (int) PvJson.num(own, 0, "amount_owned") : 0,
                    own != null ? (long) PvJson.num(own, 0, "captured") : 0,
                    level(syph, levelling, rarity), !unconsumable.contains(PvJson.str(a, "", "bazaarName"))));
        }
        return out;
    }

    private static int maxSyphon(JsonObject levelling, String rarity) {
        JsonElement arr = levelling != null ? levelling.get(rarity) : null;
        int total = 0;
        if (arr != null && arr.isJsonArray()) for (JsonElement e : arr.getAsJsonArray()) total += e.getAsInt();
        return total;
    }

    private static int level(int syphoned, JsonObject levelling, String rarity) {
        JsonElement arr = levelling != null ? levelling.get(rarity) : null;
        if (arr == null || !arr.isJsonArray()) return Math.min(10, syphoned / 10);
        int need = 0, level = 0;
        for (JsonElement e : arr.getAsJsonArray()) {
            need += e.getAsInt();
            if (syphoned >= need) level++; else break;
        }
        return level;
    }

    private static int grid(PvPages.Ctx c, List<Shard> shards, int x, int y, int w) {
        PvScreen s = c.s();
        float es = c.es(), ns = 0.78f * es, ls = 0.66f * es;
        int gap = 5, ip = 5;
        int cols = Math.max(1, Math.min(3, (w + gap) / (Math.round(150 * es) + gap)));
        int cw = (w - (cols - 1) * gap) / cols;
        int lineH = c.lh(ls) + 2;
        int ch = ip + c.lh(ns) + 2 + 5 * lineH + ip - 2;
        int icon = Math.max(14, Math.min(Math.round(24 * es), ch - 2 * ip));

        for (int i = 0; i < shards.size(); i++) {
            Shard sh = shards.get(i);
            int cx = x + (i % cols) * (cw + gap), cy = y + (i / cols) * (ch + gap);
            boolean hov = s.hovered(c.mx(), c.my(), cx, cy, cw, ch);
            GuiDraw.roundedFill(c.g(), cx, cy, cx + cw, cy + ch, PvTheme.overlay(hov ? 0x1C : sh.obtained() ? 0x12 : 0x07), 5);
            s.slotNoTooltip(c.g(), cx + ip, cy + (ch - icon) / 2, icon, s.repoIcon(sh.icon, Items.PRISMARINE_SHARD));

            int tx = cx + ip + icon + 6, tw = cx + cw - ip - tx;
            String color = PvProfileView.rarityColor(sh.rarity);
            List<String> lines = lines(sh);
            int ty = cy + ip;
            s.text(c.g(), fit(s, (sh.obtained() ? color : "§7") + sh.name, tw, ns), tx, ty, PvTheme.text(), ns);
            ty += c.lh(ns) + 2;
            for (String line : lines) {
                s.text(c.g(), fit(s, line, tw, ls), tx, ty, PvTheme.text(), ls);
                ty += lineH;
            }
            if (hov) {
                List<String> tip = new ArrayList<>();
                tip.add(color + sh.name);
                tip.addAll(lines);
                s.tooltipLines(tip);
            }
        }
        int rows = (shards.size() + cols - 1) / cols;
        return rows * (ch + gap) - gap;
    }

    private static List<String> lines(Shard sh) {
        List<String> out = new ArrayList<>();
        boolean maxed = sh.maxSyphon > 0 && sh.syphoned >= sh.maxSyphon;
        out.add(!sh.consumable ? "§7Syphoned: §8Can't be syphoned"
                : (maxed ? "§6Syphoned: " : "§7Syphoned: §f") + sh.syphoned + " / " + sh.maxSyphon);
        out.add("§7Owned: §f" + PvPages.num(sh.owned));
        if (!sh.ability.isEmpty()) out.add("§7Ability: §f" + sh.ability + " §7(" + sh.level + "/10)");
        if (!sh.family.isEmpty()) out.add("§7Family: §f" + String.join(", ", sh.family));
        if (sh.captured > 0) out.add("§7Captured: §f" + CAPTURED.format(Instant.ofEpochMilli(sh.captured)));
        return out;
    }

    private static String fit(PvScreen s, String text, int w, float scale) {
        if (s.textW(text, scale) <= w) return text;
        String t = text;
        while (t.length() > 2 && s.textW(t + "…", scale) > w) t = t.substring(0, t.length() - 1);
        return t + "…";
    }
}
