package com.phantomaddons.features.pv;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

final class PvInactiveAccessories {

    private PvInactiveAccessories() {}

    record Entry(ItemStack stack, ItemStack active, boolean better, String reason) {}

    private record Ranked(ItemStack stack, int bag, int tier, int mp) {}

    private static final Map<PvProfileView, List<Entry>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    static List<Entry> get(PvProfileView v) {
        List<Entry> cached = CACHE.get(v);
        if (cached != null) return cached;
        JsonObject misc = PvConstants.misc();
        if (misc == null) return null;
        List<Entry> result = compute(v, PvMissingAccessories.families(misc));
        CACHE.put(v, result);
        return result;
    }

    private static List<Entry> compute(PvProfileView v, Map<String, List<String>> families) {
        Map<String, List<Ranked>> groups = new LinkedHashMap<>();
        for (int i = 0; i < v.accessories.size(); i++) {
            ItemStack stack = v.accessories.get(i);
            String id = PvGear.id(stack);
            if (id.isEmpty()) continue;

            String key = id;
            int tier = 0;
            List<String> alternatives = PvMissingAccessories.exclusiveGroup(id);
            List<String> chain = families.get(id);
            if (alternatives != null) key = "ALT:" + alternatives.get(0);
            else if (PvMissingAccessories.isHat(id)) key = "HAT";
            else if (chain != null) { key = "FAMILY:" + chain.get(0); tier = chain.indexOf(id); }
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(new Ranked(stack, i, tier, magicalPower(stack, v.accessoryColors.get(i))));
        }

        Comparator<Ranked> first = Comparator.comparingInt(Ranked::bag);
        List<Entry> out = new ArrayList<>();
        List<Ranked> inactive = new ArrayList<>();
        Map<Ranked, Ranked> winner = new java.util.IdentityHashMap<>();
        for (List<Ranked> group : groups.values()) {
            if (group.size() < 2) continue;
            Ranked top = group.stream().min(first).orElseThrow();
            for (Ranked r : group) {
                if (r == top) continue;
                inactive.add(r);
                winner.put(r, top);
            }
        }
        inactive.sort(Comparator.comparingInt(Ranked::bag));
        for (Ranked r : inactive) {
            Ranked top = winner.get(r);
            boolean better = r.tier() > top.tier() || (r.tier() == top.tier() && r.mp() > top.mp());
            String reason = better ? "It ranks higher than the one counting, but sits later in the bag."
                    : r.tier() == top.tier() && r.mp() == top.mp() ? "A copy earlier in the bag counts instead."
                    : "A better accessory earlier in the bag counts instead.";
            out.add(new Entry(r.stack(), top.stack(), better, reason));
        }
        return out;
    }

    static int magicalPower(ItemStack stack, int rarityRgb) {
        return switch (rarityRgb) {
            case 0xFFFFFF -> 3;
            case 0x55FF55 -> 5;
            case 0x5555FF -> 8;
            case 0xAA00AA -> 12;
            case 0xFFAA00 -> 16;
            case 0xFF55FF, 0x55FFFF -> 22;
            case 0xFF5555 -> PvGear.lore(stack).stream().anyMatch(l -> l.contains("VERY SPECIAL")) ? 5 : 3;
            default -> 0;
        };
    }
}
