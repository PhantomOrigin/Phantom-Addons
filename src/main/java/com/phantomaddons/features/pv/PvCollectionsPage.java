package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.phantomaddons.gui.GuiDraw;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class PvCollectionsPage {

    private PvCollectionsPage() {}

    static int render(PvPages.Ctx c) {
        return switch (c.sub()) {
            case 1 -> minions(c);
            case 2 -> bestiary(c);
            default -> collections(c);
        };
    }

    // ── Shared tile grid ──────────────────────────────────────────────────────

    private record Tile(ItemStack icon, String name, String right, String sub, boolean maxed, boolean dim, List<String> tooltip,
                        double progress) {
        Tile(ItemStack icon, String name, String right, String sub, boolean maxed, boolean dim, List<String> tooltip) {
            this(icon, name, right, sub, maxed, dim, tooltip, -1);
        }
    }

    private static int tiles(PvPages.Ctx c, List<Tile> tiles, int x, int y, int w) {
        PvScreen s = c.s();
        float es = c.es(), ns = 0.8f * es, is = 0.65f * es;
        int slot = Math.max(12, Math.round(18 * es));
        int cols = Math.max(1, w / Math.max(105, Math.round(140 * es)));
        int gap = 4;
        int cw = (w - (cols - 1) * gap) / cols;
        boolean bars = false;
        for (Tile t : tiles) bars |= t.progress >= 0;
        int ch = slot + (bars ? 11 : 8);
        for (int i = 0; i < tiles.size(); i++) {
            Tile t = tiles.get(i);
            int cx = x + (i % cols) * (cw + gap), cy = y + (i / cols) * (ch + gap);
            boolean hov = s.hovered(c.mx(), c.my(), cx, cy, cw, ch);
            GuiDraw.roundedFill(c.g(), cx, cy, cx + cw, cy + ch, PvTheme.overlay(hov ? 0x1C : t.dim ? 0x06 : 0x10), 5);
            if (t.maxed) GuiDraw.roundedFill(c.g(), cx, cy + 3, cx + 2, cy + ch - 3, PvTheme.accent(), 1);
            s.slotNoTooltip(c.g(), cx + 4, cy + 4, slot, t.icon);
            int tx = cx + slot + 9;
            int rightW = s.textW(t.right, ns);
            String name = t.name;
            while (name.length() > 3 && s.textW(name, ns) > cw - slot - 16 - rightW) name = name.substring(0, name.length() - 2);
            if (!name.equals(t.name)) name = name + "…";
            s.text(c.g(), (t.dim ? "§7" : "") + name, tx, cy + 4, PvTheme.text(), ns);
            s.text(c.g(), t.right, cx + cw - 5 - rightW, cy + 4, t.maxed ? PvTheme.accent() : PvTheme.text(), ns);
            s.text(c.g(), t.sub, tx, cy + 6 + c.lh(ns), PvTheme.dim(), is);
            if (t.progress >= 0) {
                int bx = cx + 5, bw = cw - 10, by = cy + ch - 5;
                GuiDraw.roundedFill(c.g(), bx, by, bx + bw, by + 2, PvTheme.barBg(), 1);
                int fw = (int) Math.round(bw * Math.min(1.0, t.progress));
                if (fw > 0) GuiDraw.roundedFill(c.g(), bx, by, bx + fw, by + 2, t.maxed ? PvTheme.accent() : PvTheme.barFg(), 1);
            }
            if (hov && t.tooltip != null) s.tooltipLines(t.tooltip);
        }
        int rows = (tiles.size() + cols - 1) / cols;
        return rows * (ch + gap);
    }

    // ── Collections ───────────────────────────────────────────────────────────

    private static final String[] CATEGORIES = { "FARMING", "MINING", "COMBAT", "FORAGING", "FISHING", "RIFT", "BOSS" };
    private static final String[] CATEGORY_LABELS = { "Farming", "Mining", "Combat", "Foraging", "Fishing", "Rift", "Boss" };

    private record Boss(String name, int floor, long[] tiers) {}
    private static final long[] SMALL = { 25, 50, 100, 150, 250, 1000 };
    private static final long[] MID = { 50, 100, 150, 250, 400, 1000 };
    private static final long[] BIG = { 50, 100, 150, 250, 500, 750, 1000 };
    private static final Boss[] BOSSES = {
            new Boss("Bonzo", 1, SMALL), new Boss("Scarf", 2, SMALL), new Boss("The Professor", 3, SMALL),
            new Boss("Thorn", 4, MID), new Boss("Livid", 5, BIG), new Boss("Sadan", 6, BIG), new Boss("Necron", 7, BIG) };
    private static final long[] KUUDRA_TIERS = { 10, 100, 500, 2000, 5000 };

    private static int collections(PvPages.Ctx c) {
        PvScreen s = c.s();
        float es = c.es();
        int pad = PvPages.PAD;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;
        int yy = y0 + s.innerTabsRow(c.g(), c.key(), CATEGORY_LABELS, x0, y0, avail, c.mx(), c.my()) + 8;
        String cat = CATEGORIES[Math.min(s.innerTab(c.key()), CATEGORIES.length - 1)];
        JsonObject mine = PvJson.obj(c.m(), "collection");

        List<Tile> tiles = new ArrayList<>();
        if (cat.equals("BOSS")) {
            JsonObject dungeons = PvJson.obj(c.m(), "dungeons", "dungeon_types");
            for (Boss b : BOSSES) {
                String f = String.valueOf(b.floor);
                double amount = PvJson.num(dungeons, 0, "catacombs", "tier_completions", f)
                        + 2 * PvJson.num(dungeons, 0, "master_catacombs", "tier_completions", f);
                tiles.add(tierTile(new ItemStack(Items.WITHER_SKELETON_SKULL), b.name, amount, b.tiers,
                        "§8Floor " + b.floor + " + 2× Master Mode " + b.floor));
            }
            JsonObject k = PvJson.obj(c.m(), "nether_island_player_data", "kuudra_completed_tiers");
            String[] keys = { "none", "hot", "burning", "fiery", "infernal" };
            double weighted = 0;
            for (int i = 0; i < keys.length; i++) weighted += (i + 1) * PvJson.num(k, 0, keys[i]);
            tiles.add(tierTile(s.repoIcon("KUUDRA_TIER_KEY", Items.MAGMA_CREAM), "Kuudra", weighted, KUUDRA_TIERS,
                    "§8Basic ×1, Hot ×2, Burning ×3, Fiery ×4, Infernal ×5"));
        } else {
            JsonObject all = PvConstants.collections();
            JsonObject items = PvJson.obj(all, "collections", cat, "items");
            if (items == null) {
                s.text(c.g(), all == null ? "Loading collections..." : "No collections in this category.", x0, yy, PvTheme.dim(), 0.85f * es);
                return yy - y0 + c.lh(es);
            }
            for (var e : items.entrySet()) {
                JsonObject it = e.getValue().getAsJsonObject();
                List<Long> req = new ArrayList<>();
                for (JsonElement t : it.getAsJsonArray("tiers")) req.add(PvJson.num(t, 0, "amountRequired") > 0 ? (long) PvJson.num(t, 0, "amountRequired") : 0L);
                long[] tiers = req.stream().mapToLong(Long::longValue).toArray();
                double amount = PvJson.num(mine, 0, e.getKey());
                tiles.add(tierTile(s.repoIcon(e.getKey().replace(':', '-'), Items.CHEST),
                        PvJson.str(it, e.getKey(), "name"), amount, tiers, null));
            }
        }
        yy += tiles(c, tiles, x0, yy, avail);
        return yy - y0;
    }

    private static Tile tierTile(ItemStack icon, String name, double amount, long[] tiers, String note) {
        int tier = PvPages.cumulativeLevel(amount, tiers);
        boolean maxed = tier >= tiers.length;
        List<String> tip = new ArrayList<>();
        tip.add("§e" + name + " " + PvPages.roman(tier));
        tip.add("§7Collected: §e" + PvPages.num(amount));
        tip.add(maxed ? "§6MAXED" : "§7Next tier: §e" + PvPages.num(tiers[tier]) + " §8(" + PvPages.num(tiers[tier] - amount) + " to go)");
        if (note != null) tip.add(note);
        return new Tile(icon, name, PvPages.roman(tier) + "/" + PvPages.roman(tiers.length), PvPages.shortNum(amount),
                maxed, amount <= 0, tip);
    }

    // ── Minions ───────────────────────────────────────────────────────────────

    private static final Map<String, Set<String>> MINION_TYPES = new LinkedHashMap<>();
    static {
        MINION_TYPES.put("Farming", Set.of("WHEAT", "CARROT", "POTATO", "PUMPKIN", "MELON", "MUSHROOM", "COCOA", "CACTUS",
                "SUGAR_CANE", "NETHER_WARTS", "FLOWER", "CHICKEN", "COW", "PIG", "SHEEP", "RABBIT", "SUNFLOWER", "MOONFLOWER", "WILD_ROSE"));
        MINION_TYPES.put("Mining", Set.of("COBBLESTONE", "OBSIDIAN", "GLOWSTONE", "GRAVEL", "SAND", "RED_SAND", "ICE", "SNOW",
                "COAL", "IRON", "GOLD", "DIAMOND", "LAPIS", "REDSTONE", "EMERALD", "QUARTZ", "ENDER_STONE", "MITHRIL",
                "HARD_STONE", "MYCELIUM", "TUNGSTEN", "UMBER"));
        MINION_TYPES.put("Combat", Set.of("ZOMBIE", "REVENANT", "VOIDLING", "SKELETON", "CREEPER", "SPIDER", "TARANTULA",
                "CAVESPIDER", "BLAZE", "MAGMA_CUBE", "ENDERMAN", "GHAST", "SLIME", "INFERNO", "VAMPIRE"));
        MINION_TYPES.put("Foraging", Set.of("OAK", "SPRUCE", "BIRCH", "DARK_OAK", "ACACIA", "JUNGLE", "FIG", "MANGROVE"));
        MINION_TYPES.put("Fishing", Set.of("FISHING", "CLAY"));
    }
    private static final int[] SLOT_THRESHOLDS = { 5, 15, 30, 50, 75, 100, 125, 150, 175, 200, 225, 250, 275, 300, 350, 400, 450, 500, 550, 600, 650 };

    private static int minions(PvPages.Ctx c) {
        PvScreen s = c.s();
        float es = c.es(), ss = 0.85f * es;
        int pad = PvPages.PAD;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;

        Map<String, Integer> highest = new HashMap<>();
        int unique = 0;
        JsonElement crafted = PvJson.path(c.m(), "player_data", "crafted_generators");
        if (crafted != null && crafted.isJsonArray()) {
            for (JsonElement e : crafted.getAsJsonArray()) {
                String g = e.getAsString();
                int us = g.lastIndexOf('_');
                if (us < 0) continue;
                try {
                    int tier = Integer.parseInt(g.substring(us + 1));
                    highest.merge(g.substring(0, us), tier, Math::max);
                    unique++;
                } catch (NumberFormatException ignored) {}
            }
        }
        JsonObject maxTiers = PvJson.obj(PvConstants.misc(), "minions");
        Map<String, Integer> all = new LinkedHashMap<>();
        if (maxTiers != null) for (var e : maxTiers.entrySet()) all.put(e.getKey().replace("_GENERATOR", ""), e.getValue().getAsInt());
        for (String k : highest.keySet()) all.putIfAbsent(k, 11);

        int maxed = 0;
        for (var e : all.entrySet()) if (highest.getOrDefault(e.getKey(), 0) >= e.getValue()) maxed++;
        int slots = 5;
        for (int t : SLOT_THRESHOLDS) if (unique >= t) slots++;

        int yy = y0;
        int third = avail / 3;
        statBox(c, "Unique Minions", String.valueOf(unique), x0, yy, third - 4);
        statBox(c, "Minion Slots", slots + " §8+ upgrades", x0 + third, yy, third - 4);
        statBox(c, "Maxed Minions", maxed + "/" + all.size(), x0 + 2 * third, yy, third - 4);
        yy += c.lh(0.75f * es) + c.lh(es) + 18;

        Map<String, List<Tile>> byType = new LinkedHashMap<>();
        for (String t : MINION_TYPES.keySet()) byType.put(t, new ArrayList<>());
        byType.put("Other", new ArrayList<>());
        for (var e : all.entrySet()) {
            String type = "Other";
            for (var t : MINION_TYPES.entrySet()) if (t.getValue().contains(e.getKey())) { type = t.getKey(); break; }
            int have = highest.getOrDefault(e.getKey(), 0);
            int max = e.getValue();
            String name = PvProfileView.prettify(e.getKey()) + " Minion";
            ItemStack icon = s.repoIcon(e.getKey() + "_GENERATOR_" + Math.max(1, have), Items.ARMOR_STAND);
            byType.get(type).add(new Tile(icon, name, have == 0 ? "—" : PvPages.roman(have) + "/" + PvPages.roman(max),
                    have == 0 ? "Not crafted" : have >= max ? "Maxed" : (max - have) + " tier" + (max - have == 1 ? "" : "s") + " left",
                    have >= max, have == 0, List.of("§e" + name, "§7Highest crafted: §e" + (have == 0 ? "None" : "Tier " + PvPages.roman(have)),
                    "§7Max tier: §e" + PvPages.roman(max))));
        }
        for (var e : byType.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            e.getValue().sort((a, b) -> a.name.compareTo(b.name));
            yy += s.title(c.g(), e.getKey() + " §7(" + e.getValue().stream().filter(t -> !t.dim).count() + "/" + e.getValue().size() + ")", x0, yy);
            yy += tiles(c, e.getValue(), x0, yy, avail) + 6;
        }
        return yy - y0;
    }

    private static void statBox(PvPages.Ctx c, String label, String value, int x, int y, int w) {
        PvScreen s = c.s();
        float es = c.es();
        int h = c.lh(0.75f * es) + c.lh(es) + 12;
        s.card(c.g(), x, y, w, h);
        s.text(c.g(), label, x + 6, y + 4, PvTheme.dim(), 0.75f * es);
        s.text(c.g(), value, x + 6, y + 6 + c.lh(0.75f * es), PvTheme.text(), es);
    }

    // ── Bestiary ──────────────────────────────────────────────────────────────

    private static final Map<String, ItemStack> HEADS = new HashMap<>();

    private static ItemStack head(String texture) {
        if (texture == null || texture.isEmpty()) return new ItemStack(Items.SKELETON_SKULL);
        return HEADS.computeIfAbsent(texture, PvItems::head);
    }

    private static int bestiary(PvPages.Ctx c) {
        PvScreen s = c.s();
        float es = c.es();
        int pad = PvPages.PAD;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;
        JsonObject consts = PvConstants.bestiary();
        if (consts == null) {
            s.text(c.g(), "Loading bestiary data...", x0, y0, PvTheme.dim(), 0.85f * es);
            return c.lh(es);
        }
        JsonObject kills = PvJson.obj(c.m(), "bestiary", "kills");
        JsonObject brackets = PvJson.obj(consts, "brackets");

        List<String> islandKeys = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (var e : consts.entrySet()) {
            if (!e.getValue().isJsonObject() || !e.getValue().getAsJsonObject().has("mobs")) continue;
            islandKeys.add(e.getKey());
            labels.add(PvJson.str(e.getValue(), PvProfileView.prettify(e.getKey()), "name").replaceAll("§.", ""));
        }

        int totalTiers = 0, unlocked = 0, completed = 0;
        List<Family> fams = new ArrayList<>();
        for (int i = 0; i < islandKeys.size(); i++) {
            String key = islandKeys.get(i), location = labels.get(i);
            for (JsonElement mob : consts.getAsJsonObject(key).getAsJsonArray("mobs")) {
                JsonObject o = mob.getAsJsonObject();
                double k = 0;
                for (JsonElement id : o.getAsJsonArray("mobs")) k += PvJson.num(kills, 0, id.getAsString());
                long cap = (long) PvJson.num(o, 0, "cap");
                long[] th = PvConstants.table(brackets, String.valueOf((int) PvJson.num(o, 1, "bracket")));
                int maxTier = 0;
                for (long t : th) { if (t <= cap) maxTier++; else break; }
                int tier = Math.min(maxTier, PvPages.cumulativeLevel(k, th));
                long prevAt = tier > 0 ? th[tier - 1] : 0;
                long nextAt = tier < maxTier ? th[tier] : prevAt;
                Family f = new Family(i, location, PvJson.str(o, "?", "name").replaceAll("§.", ""),
                        head(PvJson.str(o, "", "texture")), k, tier, maxTier, prevAt, nextAt);
                fams.add(f);
                totalTiers += tier;
                if (f.started()) unlocked++;
                if (f.maxed()) completed++;
            }
        }
        int families = fams.size();

        int yy = y0;
        int third = avail / 3;
        statBox(c, "Bestiary Level", String.format(Locale.ROOT, "%.1f", totalTiers / 10.0), x0, yy, third - 4);
        statBox(c, "Families Unlocked", unlocked + "/" + families, x0 + third, yy, third - 4);
        statBox(c, "Families Completed", completed + "/" + families, x0 + 2 * third, yy, third - 4);
        yy += c.lh(0.75f * es) + c.lh(es) + 14;
        s.text(c.g(), "§7Family tiers: §r" + totalTiers, x0, yy, PvTheme.text(), 0.85f * es);
        yy += c.lh(0.85f * es) + 8;

        yy += s.searchBox(c.g(), c.key(), "Search bestiary families or locations", x0, yy, avail, c.mx(), c.my()) + 6;
        String query = s.search(c.key()).trim().toLowerCase(Locale.ROOT);

        int inProgress = 0, notStarted = 0;
        for (Family f : fams) {
            if (!f.started()) notStarted++;
            else if (!f.maxed()) inProgress++;
        }
        String[] views = { "All §7(" + families + ")", "In Progress §7(" + inProgress + ")", "Maxed §7(" + completed + ")",
                "Not Started §7(" + notStarted + ")", "By Location" };
        yy += s.innerTabsRow(c.g(), c.key(), views, x0, yy, avail, c.mx(), c.my()) + 8;
        int view = Math.min(s.innerTab(c.key()), views.length - 1);

        List<Family> shown = new ArrayList<>();
        for (Family f : fams) {
            if (!query.isEmpty()) {
                if (f.name.toLowerCase(Locale.ROOT).contains(query) || f.location.toLowerCase(Locale.ROOT).contains(query)) shown.add(f);
                continue;
            }
            boolean in = switch (view) {
                case 1 -> f.started() && !f.maxed();
                case 2 -> f.maxed();
                case 3 -> !f.started();
                default -> true;
            };
            if (in) shown.add(f);
        }
        Comparator<Family> byLevel = Comparator.comparingInt(Family::tier).reversed()
                .thenComparing(Comparator.comparingDouble(Family::kills).reversed()).thenComparing(Family::name);
        boolean grouped = query.isEmpty() && view == 4;
        if (query.isEmpty() && view == 1) {
            shown.sort(Comparator.comparingDouble(Family::progress).reversed().thenComparing(byLevel));
        } else if (query.isEmpty() && view == 3) {
            shown.sort(Comparator.comparingInt(Family::island).thenComparing(Family::name));
        } else {
            shown.sort(byLevel);
        }

        if (shown.isEmpty()) {
            s.text(c.g(), query.isEmpty() ? "Nothing here yet." : "No bestiary families match \"" + query + "\".",
                    x0, yy, PvTheme.dim(), 0.85f * es);
            return yy - y0 + c.lh(0.85f * es);
        }
        if (!grouped) {
            List<Tile> tiles = new ArrayList<>();
            for (Family f : shown) tiles.add(tileOf(f));
            yy += tiles(c, tiles, x0, yy, avail);
            return yy - y0;
        }
        for (int i = 0; i < islandKeys.size(); i++) {
            final int island = i;
            List<Family> here = new ArrayList<>();
            for (Family f : shown) if (f.island == island) here.add(f);
            if (here.isEmpty()) continue;
            int done = 0;
            for (Family f : here) if (f.maxed()) done++;
            s.text(c.g(), labels.get(i) + " §7" + done + "/" + here.size() + " maxed", x0, yy, PvTheme.accent(), 0.9f * es);
            yy += c.lh(0.9f * es) + 4;
            List<Tile> tiles = new ArrayList<>();
            for (Family f : here) tiles.add(tileOf(f));
            yy += tiles(c, tiles, x0, yy, avail) + 6;
        }
        return yy - y0;
    }

    private record Family(int island, String location, String name, ItemStack icon, double kills, int tier, int maxTier,
                          long prevAt, long nextAt) {
        boolean started() { return kills > 0; }
        boolean maxed() { return maxTier > 0 && tier >= maxTier; }

        double progress() {
            if (maxed()) return 1;
            return nextAt > prevAt ? Math.max(0, Math.min(1, (kills - prevAt) / (double) (nextAt - prevAt))) : 0;
        }
    }

    private static Tile tileOf(Family f) {
        List<String> tip = new ArrayList<>();
        tip.add("§a" + f.name);
        tip.add("§7Location: §f" + f.location);
        tip.add("§7Level: §f" + PvPages.roman(f.tier) + " §7/ §f" + PvPages.roman(f.maxTier));
        tip.add("§7Kills: §e" + PvPages.num(f.kills));
        if (f.maxed()) {
            tip.add("§6MAXED");
        } else {
            tip.add("§7Next level at: §e" + PvPages.num(f.nextAt) + " kills §8(" + PvPages.num(Math.max(0, f.nextAt - f.kills)) + " to go)");
        }
        String sub = f.maxed() ? PvPages.shortNum(f.kills) + " kills"
                : PvPages.shortNum(f.kills) + " / " + PvPages.shortNum(f.nextAt) + " kills";
        return new Tile(f.icon, f.name, PvPages.roman(f.tier) + "/" + PvPages.roman(f.maxTier), sub,
                f.maxed(), !f.started(), tip, f.progress());
    }
}
