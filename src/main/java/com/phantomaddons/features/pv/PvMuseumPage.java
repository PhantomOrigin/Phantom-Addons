package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.phantomaddons.gui.GuiDraw;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

final class PvMuseumPage {

    private PvMuseumPage() {}

    private static final String[] CATEGORIES = { "combat", "farming", "mining", "fishing", "foraging", "dungeoneering", "hunting", "special" };
    private static final DateTimeFormatter DONATED =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    enum State { DONATED, COVERED, MISSING }

    private static final int RED = 0xFFE5484D, GREEN = 0xFF46D160, DARK_GREEN = 0xFF0B7A2A, OWNED = 0xFFFF9A1F;
    private static final int UPGRADES = CATEGORIES.length;

    private static final Map<JsonObject, java.util.concurrent.CompletableFuture<Map<String, List<ItemStack>>>> DECODED =
            Collections.synchronizedMap(new WeakHashMap<>());

    static int render(PvPages.Ctx c) {
        PvScreen s = c.s();
        float es = c.es(), ss = 0.85f * es;
        int pad = PvPages.PAD;
        int x0 = c.x() + pad, y0 = c.y() + pad - c.scroll(), avail = c.w() - 2 * pad;

        JsonObject museum = s.museum(c.v());
        JsonObject consts = PvConstants.museum();
        if (museum == null || consts == null) {
            String err = s.museumError(c.v());
            s.text(c.g(), err != null ? "§cCouldn't load the museum: " + err : "Loading museum...", x0, y0, PvTheme.dim(), ss);
            return c.lh(es);
        }
        JsonObject items = PvJson.obj(museum, "items");
        if (items == null) items = new JsonObject();
        boolean upgrades = c.sub() >= UPGRADES;
        String cat = upgrades ? "upgrades" : CATEGORIES[Math.max(0, c.sub())];

        int yy = y0;
        double value = PvJson.num(museum, 0, "value");
        int totalDonated = items.size();
        int totalMax = (int) PvJson.num(consts, 0, "max_values", "total");
        String left = PvProfileView.prettify(cat);
        String right = "§7Value: §6" + PvPages.shortNum(value) + "   §7Donated: §f" + totalDonated + (totalMax > 0 ? "§7/" + totalMax : "");
        s.title(c.g(), left, x0, yy);
        s.text(c.g(), right, x0 + avail - s.textW(right, ss), yy + (c.lh(es) - c.lh(ss)) / 2, PvTheme.text(), ss);
        yy += c.lh(es) + 6;

        if (upgrades) return yy - y0 + upgrades(c, museum, items, consts, x0, yy, avail);
        if (cat.equals("special")) return yy - y0 + special(c, museum, x0, yy, avail);

        // ── Category grid ──
        List<String> ids = new ArrayList<>();
        JsonElement list = PvJson.path(consts, "items", cat);
        if (list != null && list.isJsonArray()) for (JsonElement e : list.getAsJsonArray()) ids.add(e.getAsString());
        Set<String> known = new HashSet<>();
        JsonObject allLists = PvJson.obj(consts, "items");
        if (allLists != null) for (var e : allLists.entrySet())
            if (e.getValue().isJsonArray()) for (JsonElement id : e.getValue().getAsJsonArray()) known.add(id.getAsString());
        for (String id : items.keySet()) if (!known.contains(id) && guessCategory(id).equals(cat)) ids.add(id);

        Map<String, State> states = states(ids, items, PvJson.obj(consts, "children"));
        int donated = 0, covered = 0, borrowed = 0;
        for (String id : ids) {
            State st = states.get(id);
            if (st == State.DONATED) donated++;
            else if (st == State.COVERED) covered++;
            if (st == State.DONATED && borrowed(items, id)) borrowed++;
        }
        int max = (int) PvJson.num(consts, ids.size(), "max_values", cat);
        String progress = "§7Donated §f" + (donated + covered) + "§7/" + max
                + (covered > 0 ? "  §8(" + covered + " by higher tiers)" : "")
                + (borrowed > 0 ? "  §2" + borrowed + " borrowed" : "");
        s.text(c.g(), progress, x0, yy, PvTheme.text(), ss);
        yy += c.lh(ss) + 6;

        Map<String, List<ItemStack>> decoded = decoded(museum);
        if (decoded == null) {
            s.text(c.g(), "Loading items...", x0, yy, PvTheme.dim(), ss);
            return yy - y0 + c.lh(ss);
        }
        JsonObject armorToId = PvJson.obj(consts, "armor_to_id");
        JsonObject xp = PvJson.obj(consts, "itemToXp");
        int slot = Math.max(14, Math.round(20 * es)), g = PvScreen.gapFor(slot);
        int cols = Math.max(1, (avail + g) / (slot + g));
        int gx = x0 + (avail - (cols * (slot + g) - g)) / 2;
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            State st = states.get(id);
            int sx = gx + (i % cols) * (slot + g), sy = yy + (i / cols) * (slot + g);
            String pieceId = armorToId != null && armorToId.has(id) ? armorToId.get(id).getAsString() : id;
            if (st == State.DONATED) {
                ItemStack shown = pick(decoded.get(id), pieceId);
                GuiDraw.roundedFill(c.g(), sx - 1, sy - 1, sx + slot + 1, sy + slot + 1, borrowed(items, id) ? DARK_GREEN : GREEN, 4);
                s.slotNoTooltip(c.g(), sx, sy, slot, shown.isEmpty() ? s.repoIcon(pieceId, Items.BARRIER) : shown);
                if (s.hovered(c.mx(), c.my(), sx, sy, slot, slot)) {
                    List<String> tip = new ArrayList<>();
                    tip.add(nameOf(shown, pieceId, id));
                    long when = (long) PvJson.num(items, 0, id, "donated_time");
                    if (when > 0) tip.add("§7Donated " + DONATED.format(Instant.ofEpochMilli(when)));
                    if (borrowed(items, id)) tip.add("§2Currently borrowed");
                    List<ItemStack> pieces = decoded.get(id);
                    if (pieces != null && pieces.size() > 1) {
                        tip.add("");
                        for (ItemStack p : pieces) if (!p.isEmpty()) tip.add("§8• §r" + p.getHoverName().getString());
                    }
                    tip.add("§7Museum XP: §b" + (int) PvJson.num(xp, 0, id));
                    s.tooltipLines(tip);
                }
            } else {
                GuiDraw.roundedFill(c.g(), sx - 1, sy - 1, sx + slot + 1, sy + slot + 1, st == State.COVERED ? GREEN : RED, 4);
                s.slotNoTooltip(c.g(), sx, sy, slot, s.repoIcon(pieceId, Items.BARRIER));
                c.g().nextStratum();
                GuiDraw.roundedFill(c.g(), sx, sy, sx + slot, sy + slot, st == State.COVERED ? 0x55000000 : 0x66000000, 3);
                if (s.hovered(c.mx(), c.my(), sx, sy, slot, slot)) {
                    s.tooltip(nameOf(ItemStack.EMPTY, pieceId, id),
                            st == State.COVERED ? "§aDonated via a higher tier" : "§cNot donated",
                            "§7Museum XP: §b" + (int) PvJson.num(xp, 0, id));
                }
            }
        }
        yy += ((ids.size() + cols - 1) / cols) * (slot + g);
        return yy - y0;
    }

    private static int upgrades(PvPages.Ctx c, JsonObject museum, JsonObject items, JsonObject consts, int x, int y, int w) {
        PvScreen s = c.s();
        float es = c.es(), ss = 0.85f * es;
        PvMuseumUpgrades.Result res = PvMuseumUpgrades.get(c.v(), museum, items, consts);
        int yy = y;
        String summary = "§7Museum XP still available: §b" + res.missingXp() + "   §6Orange: already owned";
        s.text(c.g(), summary, x, yy, PvTheme.text(), ss);
        if (!res.pricesLoaded()) {
            String loading = "§8loading prices...";
            s.text(c.g(), loading, x + w - s.textW(loading, ss), yy, PvTheme.dim(), ss);
        }
        yy += c.lh(ss) + 6;

        JsonObject armorToId = PvJson.obj(consts, "armor_to_id");
        int slot = Math.max(14, Math.round(20 * es)), g = PvScreen.gapFor(slot);
        int cols = Math.max(1, (w + g) / (slot + g));
        int gx = x + (w - (cols * (slot + g) - g)) / 2;
        yy += upgradeGrid(c, "Purchasable §7(" + res.buyable().size() + ")  §8cheapest per XP first", res.buyable(), armorToId, x, gx, cols, slot, g, yy);
        yy += 6;
        yy += upgradeGrid(c, "Not purchasable §7(" + res.unbuyable().size() + ")  §8no price found", res.unbuyable(), armorToId, x, gx, cols, slot, g, yy);
        return yy - y;
    }

    private static int upgradeGrid(PvPages.Ctx c, String label, List<PvMuseumUpgrades.Entry> entries, JsonObject armorToId,
                                   int x, int gx, int cols, int slot, int g, int y) {
        PvScreen s = c.s();
        float ls = 0.8f * c.es();
        s.text(c.g(), label, x, y, PvTheme.text(), ls);
        int gy = y + c.lh(ls) + 4;
        if (entries.isEmpty()) {
            s.text(c.g(), "§8None", x, gy, PvTheme.text(), ls);
            return gy - y + c.lh(ls);
        }
        for (int i = 0; i < entries.size(); i++) {
            PvMuseumUpgrades.Entry e = entries.get(i);
            int sx = gx + (i % cols) * (slot + g), sy = gy + (i / cols) * (slot + g);
            if (s.offscreen(sy, slot)) continue;
            String pieceId = armorToId != null && armorToId.has(e.id()) ? armorToId.get(e.id()).getAsString() : e.id();
            PvItems.RepoItem repo = PvItems.repoItem(pieceId);
            ItemStack icon = s.repoIcon(pieceId, Items.BARRIER);
            if (e.owned()) GuiDraw.roundedFill(c.g(), sx - 1, sy - 1, sx + slot + 1, sy + slot + 1, OWNED, 4);
            PvScreen.rarityBg(c.g(), sx, sy, slot, repo != null ? PvItems.rarityColor(repo.stack()) : -1);
            s.item(c.g(), icon, sx + 1, sy + 1, slot - 2);
            if (s.hovered(c.mx(), c.my(), sx, sy, slot, slot)) {
                c.g().nextStratum();
                GuiDraw.roundedFill(c.g(), sx, sy, sx + slot, sy + slot, PvTheme.overlay(0x28), 3);
                List<String> tip = new ArrayList<>();
                tip.add(nameOf(ItemStack.EMPTY, pieceId, e.id()));
                tip.add("§7" + PvProfileView.prettify(e.category()));
                if (e.owned()) tip.add("§6You already own this");
                tip.add("");
                tip.add("§7Museum XP: §b" + e.xp());
                if (e.buyable()) {
                    tip.add("§7Price: §6" + PvPages.shortNum(e.price()));
                    tip.add("§7Per XP: §6" + PvPages.shortNum(e.perXp()));
                    if (e.pieces().size() > 1) tip.add("§8Total of " + e.pieces().size() + " pieces");
                } else {
                    tip.add("§8No price data");
                }
                s.tooltipLines(tip);
            }
        }
        return gy - y + ((entries.size() + cols - 1) / cols) * (slot + g);
    }

    private static int special(PvPages.Ctx c, JsonObject museum, int x, int y, int w) {
        PvScreen s = c.s();
        float es = c.es(), ss = 0.85f * es;
        JsonElement special = museum.get("special");
        List<JsonObject> entries = new ArrayList<>();
        if (special != null && special.isJsonArray()) for (JsonElement e : special.getAsJsonArray()) if (e.isJsonObject()) entries.add(e.getAsJsonObject());
        int yy = y;
        s.text(c.g(), "§7Special items donated: §f" + entries.size(), x, yy, PvTheme.text(), ss);
        yy += c.lh(ss) + 6;
        if (entries.isEmpty()) return yy - y;
        int slot = Math.max(14, Math.round(20 * es)), g = PvScreen.gapFor(slot);
        int cols = Math.max(1, (w + g) / (slot + g));
        int gx = x + (w - (cols * (slot + g) - g)) / 2;
        for (int i = 0; i < entries.size(); i++) {
            JsonObject e = entries.get(i);
            Map<String, List<ItemStack>> one = decodedSpecial(e);
            List<ItemStack> stacks = one != null ? one.get("") : List.of();
            ItemStack shown = stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0);
            s.slot(c.g(), gx + (i % cols) * (slot + g), yy + (i / cols) * (slot + g), slot, shown, c.mx(), c.my());
        }
        return yy - y + ((entries.size() + cols - 1) / cols) * (slot + g);
    }

    static Map<String, State> states(List<String> ids, JsonObject items, JsonObject children) {
        Map<String, State> out = new HashMap<>();
        for (String id : ids) out.put(id, items.has(id) ? State.DONATED : State.MISSING);
        if (children == null) return out;
        Set<String> donatedOrCovered = new HashSet<>(items.keySet());
        boolean changed = true;
        while (changed) {
            changed = false;
            for (var e : children.entrySet()) {
                if (!donatedOrCovered.contains(e.getKey()) || !e.getValue().isJsonPrimitive()) continue;
                if (donatedOrCovered.add(e.getValue().getAsString())) changed = true;
            }
        }
        for (String id : ids) if (out.get(id) == State.MISSING && donatedOrCovered.contains(id)) out.put(id, State.COVERED);
        return out;
    }

    private static Map<String, List<ItemStack>> decoded(JsonObject museum) {
        var f = DECODED.computeIfAbsent(museum, m -> java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            Map<String, List<ItemStack>> out = new HashMap<>();
            JsonObject items = PvJson.obj(m, "items");
            if (items != null) for (var e : items.entrySet()) out.put(e.getKey(), PvItems.decode(PvJson.path(e.getValue(), "items")));
            return out;
        }, PvItems.EXECUTOR));
        return f.isDone() && !f.isCompletedExceptionally() ? f.join() : null;
    }

    private static Map<String, List<ItemStack>> decodedSpecial(JsonObject entry) {
        var f = DECODED.computeIfAbsent(entry, k -> java.util.concurrent.CompletableFuture.supplyAsync(
                () -> Map.of("", PvItems.decode(k.get("items"))), PvItems.EXECUTOR));
        return f.isDone() && !f.isCompletedExceptionally() ? f.join() : null;
    }

    private static boolean borrowed(JsonObject items, String id) {
        JsonElement b = PvJson.path(items, id, "borrowing");
        return b != null && b.isJsonPrimitive() && b.getAsBoolean();
    }

    private static ItemStack pick(List<ItemStack> stacks, String pieceId) {
        if (stacks == null || stacks.isEmpty()) return ItemStack.EMPTY;
        for (ItemStack st : stacks) if (PvGear.id(st).equals(pieceId)) return st;
        for (ItemStack st : stacks) if (!st.isEmpty()) return st;
        return ItemStack.EMPTY;
    }

    static String nameOf(ItemStack stack, String pieceId, String id) {
        if (!stack.isEmpty()) return stack.getHoverName().getString();
        PvItems.RepoItem repo = PvItems.repoItem(pieceId);
        String name = repo != null ? repo.displayName() : PvProfileView.prettify(id);
        if (!pieceId.equals(id)) name = name.replaceAll("(?i) (helmet|hat|hood|cap|crown|mask|head)$", "") + " Set";
        return name;
    }

    private static String guessCategory(String id) {
        if (id.contains("PESTHUNTER") || id.contains("HOE") || id.contains("FARM")) return "farming";
        if (id.contains("ROD") || id.contains("FISH")) return "fishing";
        if (id.contains("DRILL") || id.contains("PICKAXE") || id.contains("MINING")) return "mining";
        return "combat";
    }
}
