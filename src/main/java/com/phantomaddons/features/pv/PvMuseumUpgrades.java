package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

final class PvMuseumUpgrades {

    private PvMuseumUpgrades() {}

    private static final String[] CATEGORIES = { "combat", "farming", "mining", "fishing", "foraging", "dungeoneering", "hunting" };

    record Entry(String id, String category, int xp, double price, List<String> pieces, boolean owned) {
        boolean buyable() { return price > 0; }
        double perXp() { return buyable() ? price / xp : Double.MAX_VALUE; }
    }
    record Result(List<Entry> buyable, List<Entry> unbuyable, int missingXp, boolean pricesLoaded) {}

    private static final Map<JsonObject, Result> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    static Result get(PvProfileView v, JsonObject museumData, JsonObject items, JsonObject consts) {
        Result r = CACHE.get(museumData);
        boolean settled = PvConstants.pricesSettled();
        if (r != null && (r.pricesLoaded || !settled)) return r;
        r = compute(v, items, consts, settled);
        CACHE.put(museumData, r);
        return r;
    }

    private static Set<String> ownedIds(PvProfileView v, JsonObject mapped) {
        Set<String> out = new HashSet<>();
        List<net.minecraft.world.item.ItemStack> stacks = new ArrayList<>(v.allItems);
        stacks.addAll(v.accessories);
        for (net.minecraft.world.item.ItemStack stack : stacks) {
            String id = PvGear.id(stack);
            if (id.isEmpty()) continue;
            out.add(id);
            if (mapped != null && mapped.has(id) && mapped.get(id).isJsonPrimitive()) out.add(mapped.get(id).getAsString());
        }
        return out;
    }

    private static Result compute(PvProfileView v, JsonObject items, JsonObject consts, boolean pricesLoaded) {
        JsonObject xp = PvJson.obj(consts, "itemToXp");
        JsonObject sets = PvJson.obj(consts, "sets_to_items");
        JsonObject lists = PvJson.obj(consts, "items");
        List<Entry> buyable = new ArrayList<>(), unbuyable = new ArrayList<>();
        int missingXp = 0;
        if (xp == null || lists == null) return new Result(buyable, unbuyable, 0, pricesLoaded);

        List<String> all = new ArrayList<>();
        Map<String, String> categoryOf = new java.util.HashMap<>();
        for (String cat : CATEGORIES) {
            JsonElement list = lists.get(cat);
            if (list == null || !list.isJsonArray()) continue;
            for (JsonElement e : list.getAsJsonArray()) {
                if (categoryOf.putIfAbsent(e.getAsString(), cat) == null) all.add(e.getAsString());
            }
        }
        Map<String, PvMuseumPage.State> states = PvMuseumPage.states(all, items, PvJson.obj(consts, "children"));

        Set<String> owned = ownedIds(v, PvJson.obj(consts, "mapped_ids"));
        Set<String> seen = new HashSet<>();
        for (String id : all) {
            if (states.get(id) != PvMuseumPage.State.MISSING || !seen.add(id)) continue;
            int gain = (int) PvJson.num(xp, 0, id);
            if (gain <= 0) continue;
            List<String> pieces = new ArrayList<>();
            JsonElement set = sets != null ? sets.get(id) : null;
            if (set != null && set.isJsonArray()) for (JsonElement p : set.getAsJsonArray()) pieces.add(p.getAsString());
            if (pieces.isEmpty()) pieces.add(id);
            double price = 0;
            for (String piece : pieces) {
                double p = PvConstants.price(piece);
                if (p <= 0) { price = -1; break; }
                price += p;
            }
            Entry e = new Entry(id, categoryOf.get(id), gain, price, pieces, owned.containsAll(pieces));
            missingXp += gain;
            (e.buyable() ? buyable : unbuyable).add(e);
        }
        buyable.sort(Comparator.comparingDouble(Entry::perXp).thenComparing(Comparator.comparingInt(Entry::xp).reversed()).thenComparing(Entry::id));
        unbuyable.sort(Comparator.comparingInt((Entry e) -> -e.xp).thenComparing(Entry::id));
        return new Result(buyable, unbuyable, missingXp, pricesLoaded);
    }
}
