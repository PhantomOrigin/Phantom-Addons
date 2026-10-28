package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import static com.phantomaddons.gui.GuiDraw.roundedFill;
import static com.phantomaddons.gui.Theme.*;

final class PvSacksPage {

    private PvSacksPage() {}

    private static final Map<PvProfileView, Set<String>> HELD =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static Set<String> heldSacks(PvProfileView v, Map<String, List<String>> defs) {
        Set<String> cached = HELD.get(v);
        if (cached != null) return cached;
        Set<String> out = new LinkedHashSet<>();
        List<ItemStack> sources = new ArrayList<>(v.inventory);
        sources.addAll(PvItems.decode(PvJson.path(v.member, "inventory", "bag_contents", "sacks_bag")));
        for (ItemStack st : sources) {
            if (st == null || st.isEmpty()) continue;
            String id = PvGear.id(st);
            if (id != null && defs.containsKey(id)) out.add(id);
        }
        HELD.put(v, out);
        return out;
    }

    private static String prettify(String id) {
        StringBuilder sb = new StringBuilder();
        for (String part : id.toLowerCase(Locale.ROOT).split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static String repoId(String id) { return id.replace(':', '-'); }

    private static long countOf(JsonObject counts, String id) {
        if (counts == null) return 0;
        JsonElement e = counts.get(id);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsLong() : 0;
    }

    static int render(PvScreen s, GuiGraphicsExtractor ctx, PvProfileView v, int key,
                      int x, int y, int w, int h, int mx, int my, int scroll) {
        int pad = PvPages.PAD;
        int y0 = y + pad - scroll;
        float ts = 0.85f * s.es;

        Map<String, List<String>> defs = PvSacks.definitions();
        if (defs == null) {
            s.text(ctx, PvSacks.failed() ? "Could not load the sack list." : "Loading sacks...", x + pad, y0, PvTheme.dim(), ts);
            return s.lineH(s.es);
        }
        JsonObject counts = PvJson.obj(v.member, "inventory", "sacks_counts");
        if (counts == null && !v.inventoryApi) {
            s.text(ctx, "Inventory API disabled.", x + pad, y0, PvTheme.dim(), ts);
            return s.lineH(s.es);
        }

        Set<String> held = heldSacks(v, defs);
        List<String> owned = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : defs.entrySet()) {
            boolean has = held.contains(e.getKey());
            if (!has) for (String item : e.getValue()) if (countOf(counts, item) > 0) { has = true; break; }
            if (has) owned.add(e.getKey());
        }
        if (owned.isEmpty()) {
            s.text(ctx, "No sacks.", x + pad, y0, PvTheme.dim(), ts);
            return s.lineH(s.es);
        }

        int page = Math.min(s.innerTab(key), owned.size() - 1);
        int navH = PvStoragePage.pageButtons(s, ctx, key, owned.size(), page,
                i -> s.repoIcon(owned.get(i), Items.CHEST),
                i -> "",
                i -> sackName(owned.get(i)),
                x, y0, w, mx, my);

        String sackId = owned.get(page);
        List<String> items = defs.get(sackId);
        long total = 0;
        for (String id : items) total += countOf(counts, id);

        float ls = Math.min(s.es, 1.2f);
        int ly = y0 + navH + 5;
        String label = stripColour(sackName(sackId));
        s.text(ctx, label, x + (w - s.textW(label, ls)) / 2, ly, C_TEXT, ls);
        String sub = "§7Items: §e" + PvScreen.fmtFull(total);
        s.text(ctx, sub, x + (w - s.textW(sub, ts)) / 2, ly + s.lineH(ls) + 1, C_TEXT, ts);

        int rows = Math.max(1, (items.size() + 8) / 9);
        int gy = ly + s.lineH(ls) + s.lineH(ts) + 8;
        int sz = s.fitSlot((w - 2 * pad) / 9.12, (h - (gy - y0) - 2 * pad) / (rows * 1.12));
        int g = PvScreen.gapFor(sz);
        int gridW = 9 * (sz + g) - g;
        int gx = x + (w - gridW) / 2;

        long[] amounts = new long[items.size()];
        for (int i = 0; i < items.size(); i++) {
            amounts[i] = countOf(counts, items.get(i));
            int sx = gx + (i % 9) * (sz + g), sy = gy + (i / 9) * (sz + g);
            roundedFill(ctx, sx, sy, sx + sz, sy + sz, PvScreen.slotBg(), 3);
            s.item(ctx, stackFor(items.get(i)), sx + 1, sy + 1, sz - 2);
        }
        ctx.nextStratum();
        float cs = 0.6f * s.es;
        for (int i = 0; i < items.size(); i++) {
            int sx = gx + (i % 9) * (sz + g), sy = gy + (i / 9) * (sz + g);
            if (amounts[i] <= 0) roundedFill(ctx, sx + 1, sy + 1, sx + sz - 1, sy + sz - 1, 0x88000000, 2);
            String c = PvScreen.fmt(amounts[i]);
            s.text(ctx, c, sx + sz - 1 - s.textW(c, cs), sy + sz - s.lineH(cs), amounts[i] > 0 ? 0xFFFFFFFF : C_TEXT, cs);
            if (s.hovered(mx, my, sx, sy, sz, sz)) {
                roundedFill(ctx, sx, sy, sx + sz, sy + sz, overlay(0x28), 3);
                s.tooltip(itemName(items.get(i)), "§7Amount: §e" + PvScreen.fmtFull(amounts[i]));
            }
        }
        return gy - y0 + rows * (sz + g);
    }

    private static String sackName(String id) {
        PvItems.RepoItem r = PvItems.repoItem(id);
        return r != null ? r.displayName() : prettify(id);
    }

    private static String itemName(String id) {
        PvItems.RepoItem r = PvItems.repoItem(repoId(id));
        return r != null ? r.displayName() : "§f" + prettify(id.contains(":") ? id.substring(0, id.indexOf(':')) : id);
    }

    private static ItemStack stackFor(String id) {
        String rid = repoId(id);
        PvItems.RepoItem r = PvItems.repoItem(rid);
        if (r != null) return r.stack();
        return new ItemStack(PvItems.repoMissing(rid) ? Items.BARRIER : Items.PAPER);
    }

    private static String stripColour(String s) {
        return s.replaceAll("§.", "");
    }
}
