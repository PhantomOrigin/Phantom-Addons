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
    private static final String[] LABELS = { "§fCommon", "§aUncommon", "§9Rare", "§5Epic", "§6Legendary", "§cMissing" };

    private static boolean pricesReady;
    private static final DateTimeFormatter CAPTURED =
            DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private record Shard(String name, String ability, String icon, String rarity, List<String> family,
                         int syphoned, int maxSyphon, int owned, long captured, int level, boolean consumable,
                         String code, int toNext, int toMax, double unit) {
        boolean obtained() { return syphoned > 0 || owned > 0 || captured > 0; }

        boolean missing() { return consumable && maxSyphon > 0 && syphoned < maxSyphon; }


        int toBuyNext() { return Math.max(0, toNext - owned); }
        int toBuyMax() { return Math.max(0, toMax - owned); }

        double costNext() { return cost(toBuyNext()); }

        double costMax() { return cost(toBuyMax()); }

        private double cost(int toBuy) { return toBuy == 0 ? 0 : unit > 0 ? toBuy * unit : -1; }
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
        pricesReady = PvConstants.pricesSettled();
        List<Shard> all = shards(c.m(), consts);

        int yy = y0;
        yy += s.searchBox(c.g(), c.key(), "Search shards, abilities, families, or descriptions", x0, yy, avail, c.mx(), c.my()) + 6;
        String query = s.search(c.key()).trim().toLowerCase(Locale.ROOT);
        List<String> descriptionIds = new ArrayList<>();
        boolean loadingDescriptions = false;
        if (!query.isEmpty()) {
            for (Shard sh : all) descriptionIds.add(sh.icon);
            PvShardText.loadAll(descriptionIds);
            loadingDescriptions = !PvShardText.allLoaded();
        }
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
        if (loadingDescriptions) {
            String note = "§8Loading descriptions to search... " + PvShardText.loaded(descriptionIds) + " / " + descriptionIds.size();
            s.text(c.g(), note, x0 + (avail - s.textW(note, 0.75f * es)) / 2, yy - 3, PvTheme.dim(), 0.75f * es);
            yy += c.lh(0.75f * es) + 3;
        }

        int tab = Math.min(s.innerTab(c.key()), LABELS.length - 1);
        boolean missingTab = tab == RARITIES.length;
        String rarity = RARITIES[Math.min(tab, RARITIES.length - 1)];

        if (missingTab) {
            int count = 0, unpriced = 0;
            double nextTotal = 0, maxTotalCost = 0;
            for (Shard sh : all) {
                if (!sh.missing()) continue;
                count++;
                if (sh.costNext() < 0 || sh.costMax() < 0) unpriced++;
                nextTotal += Math.max(0, sh.costNext());
                maxTotalCost += Math.max(0, sh.costMax());
            }
            String totals = "§7Not maxed: §f" + count + "   §7Next level for all: §6" + PvPages.shortNum(nextTotal)
                    + "   §7Max for all: §6" + PvPages.shortNum(maxTotalCost)
                    + (unpriced > 0 ? "   §8(" + unpriced + " without a price)" : "")
                    + (!pricesReady ? "   §8loading prices..." : "");
            s.text(c.g(), totals, x0 + (avail - s.textW(totals, 0.75f * es)) / 2, yy, PvTheme.text(), 0.75f * es);
            yy += c.lh(0.75f * es) + 6;
        }

        List<Shard> shown = new ArrayList<>();
        for (Shard sh : all) {
            if (query.isEmpty() ? (missingTab ? sh.missing() : sh.rarity.equals(rarity))
                    : matches(sh, query) && (!missingTab || sh.missing())) shown.add(sh);
        }
        if (missingTab) {
            shown.sort(Comparator.comparingDouble((Shard sh) -> sh.costNext() < 0 ? Double.MAX_VALUE : sh.costNext())
                    .thenComparing(Shard::name));
        } else {
            shown.sort(Comparator.comparing(Shard::name));
        }
        if (shown.isEmpty()) {
            s.text(c.g(), "No shards match \"" + query + "\"" + (loadingDescriptions ? " yet." : "."), x0, yy, PvTheme.dim(), ss);
            return yy - y0 + c.lh(ss);
        }
        yy += grid(c, shown, x0, yy, avail);
        return yy - y0;
    }

    private static boolean matches(Shard sh, String q) {
        if (sh.name.toLowerCase(Locale.ROOT).contains(q) || sh.ability.toLowerCase(Locale.ROOT).contains(q)) return true;
        for (String f : sh.family) if (f.toLowerCase(Locale.ROOT).contains(q)) return true;
        return PvShardText.searchText(sh.icon).contains(q);
    }

    private static final int[] EPIC_SYPHON_FALLBACK = { 1, 1, 2, 2, 3, 3, 4, 4, 5, 7 };

    static int attributeLevel(JsonObject m, String key) {
        int syphoned = (int) PvJson.num(m, 0, "attributes", "stacks", key);
        JsonObject consts = PvConstants.attributeShards();
        if (consts != null) {
            for (Shard sh : shards(m, consts)) {
                String k = sh.icon().replaceFirst("^ATTRIBUTE_SHARD_", "").replaceFirst(";\\d+$", "").toLowerCase(Locale.ROOT);
                if (k.equals(key)) return sh.level();
            }
        }
        int need = 0, level = 0;
        for (int step : EPIC_SYPHON_FALLBACK) {
            need += step;
            if (syphoned >= need) level++; else break;
        }
        return level;
    }

    record ShardLevel(String name, String ability, String rarity, int level, int syphoned, int maxSyphon) {
        boolean maxed() { return maxSyphon > 0 && syphoned >= maxSyphon; }
    }

    static final String[] DUNGEON_SHARDS = { "Murkbat", "Prince", "Folf" };

    private static JsonObject dungeonMember, dungeonConsts;
    private static ShardLevel[] dungeonCache = new ShardLevel[DUNGEON_SHARDS.length];

    static synchronized ShardLevel[] dungeonShards(JsonObject m) {
        JsonObject consts = PvConstants.attributeShards();
        if (consts == null) return new ShardLevel[DUNGEON_SHARDS.length];
        if (m != dungeonMember || consts != dungeonConsts) {
            ShardLevel[] out = new ShardLevel[DUNGEON_SHARDS.length];
            for (Shard sh : shards(m, consts)) {
                for (int i = 0; i < DUNGEON_SHARDS.length; i++) {
                    if (sh.name().equalsIgnoreCase(DUNGEON_SHARDS[i]))
                        out[i] = new ShardLevel(sh.name(), sh.ability(), sh.rarity(), sh.level(), sh.syphoned(), sh.maxSyphon());
                }
            }
            dungeonMember = m;
            dungeonConsts = consts;
            dungeonCache = out;
        }
        return dungeonCache;
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
            boolean consumable = !unconsumable.contains(PvJson.str(a, "", "bazaarName"));
            int maxSyph = maxSyphon(levelling, rarity), nextAt = nextLevelAt(syph, levelling, rarity);
            out.add(new Shard(PvJson.str(a, PvProfileView.prettify(type), "displayName"), PvJson.str(a, "", "abilityName"),
                    internal, rarity, family, syph, maxSyph,
                    own != null ? (int) PvJson.num(own, 0, "amount_owned") : 0,
                    own != null ? (long) PvJson.num(own, 0, "captured") : 0,
                    level(syph, levelling, rarity), consumable,
                    PvJson.str(a, "", "shardId"),
                    consumable && nextAt >= 0 ? nextAt - syph : 0,
                    consumable ? Math.max(0, maxSyph - syph) : 0,
                    consumable ? PvConstants.price(PvJson.str(a, "", "bazaarName")) : -1));
        }
        return out;
    }

    private static int maxSyphon(JsonObject levelling, String rarity) {
        JsonElement arr = levelling != null ? levelling.get(rarity) : null;
        int total = 0;
        if (arr != null && arr.isJsonArray()) for (JsonElement e : arr.getAsJsonArray()) total += e.getAsInt();
        return total;
    }

    private static int nextLevelAt(int syphoned, JsonObject levelling, String rarity) {
        JsonElement arr = levelling != null ? levelling.get(rarity) : null;
        if (arr == null || !arr.isJsonArray()) return -1;
        int need = 0;
        for (JsonElement e : arr.getAsJsonArray()) {
            need += e.getAsInt();
            if (syphoned < need) return need;
        }
        return -1;
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
        int ch = ip + c.lh(ns) + 2 + 6 * lineH + ip - 2; // up to 6 lines: syphoned, prices, owned, ability, family, captured
        int icon = Math.max(14, Math.min(Math.round(24 * es), ch - 2 * ip));

        boolean scrolling = scrolling(c.scroll());
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
                List<String> description = scrolling ? List.of() : PvShardText.description(sh.icon);
                if (description == null) {
                    tip.add("§8Loading description...");
                    tip.add("");
                } else if (!description.isEmpty()) {
                    List<String> withMax = PvShardText.withMaxLevel(sh.code, description);
                    tip.addAll(withMax);
                    tip.add(withMax != description ? "§8Brackets show the value at level 10" : "§8Values shown for level I");
                    tip.add("");
                }
                tip.addAll(lines);
                if (sh.missing()) {
                    tip.add("");
                    tip.add("§7Shards needed: §f" + sh.toNext + " §7for the next level, §f" + sh.toMax + " §7to max");
                    if (sh.owned > 0) tip.add("§7You already have §f" + sh.owned + " §7in your Hunting Box");
                    tip.add(sh.unit > 0 ? "§7Shard price: §6" + PvPages.shortNum(sh.unit) + " §8(bazaar insta-buy)"
                            : pricesReady ? "§8No bazaar price" : "§8Loading prices...");
                }
                s.tooltipLines(tip);
            }
        }
        int rows = (shards.size() + cols - 1) / cols;
        return rows * (ch + gap) - gap;
    }

    private static int lastScroll;
    private static long scrolledAt;

    private static boolean scrolling(int scroll) {
        long now = System.currentTimeMillis();
        if (scroll != lastScroll) { lastScroll = scroll; scrolledAt = now; }
        return now - scrolledAt < 250;
    }

    private static List<String> lines(Shard sh) {
        List<String> out = new ArrayList<>();
        boolean maxed = sh.maxSyphon > 0 && sh.syphoned >= sh.maxSyphon;
        out.add(!sh.consumable ? "§7Syphoned: §8Can't be syphoned"
                : (maxed ? "§6Syphoned: " : "§7Syphoned: §f") + sh.syphoned + " / " + sh.maxSyphon);
        if (sh.missing()) out.add("§7Next: " + price(sh.costNext()) + " §7Max: " + price(sh.costMax()));
        out.add("§7Owned: §f" + PvPages.num(sh.owned));
        if (!sh.ability.isEmpty()) out.add("§7Ability: §f" + sh.ability + " §7(" + sh.level + "/10)");
        if (!sh.family.isEmpty()) out.add("§7Family: §f" + String.join(", ", sh.family));
        if (sh.captured > 0) out.add("§7Captured: §f" + CAPTURED.format(Instant.ofEpochMilli(sh.captured)));
        return out;
    }

    private static String price(double cost) {
        if (cost < 0) return pricesReady ? "§8none" : "§8...";
        return cost == 0 ? "§aowned" : "§6" + PvPages.shortNum(cost);
    }

    private static String fit(PvScreen s, String text, int w, float scale) {
        if (s.textW(text, scale) <= w) return text;
        String t = text;
        while (t.length() > 2 && s.textW(t + "…", scale) > w) t = t.substring(0, t.length() - 1);
        return t + "…";
    }
}
