package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PvMissingAccessories {

    private PvMissingAccessories() {}

    private static final Set<String> UNRELEASED = Set.of("ARGOFAY_TRINKET");

    private static final List<List<String>> CHAINS = List.of(
            List.of("POWDER_OF_ONE", "POWDER_OF_THE_FEW", "POWDER_OF_THE_MANY", "POWDER_OF_THE_PEOPLE"));

    private static final List<List<String>> EXCLUSIVE = List.of(List.of("DANTE_TALISMAN", "DANTE_RING"));

    record Entry(String id, String name, String tier, int mpGain, String from, double price, boolean recomb, int count) {
        double perMp() { return price > 0 && mpGain > 0 ? price / mpGain : Double.MAX_VALUE; }
    }
    record Result(List<Entry> upgrades, List<Entry> missing, int upgradeMp, int missingMp, boolean pricesLoaded) {}

    private static final Map<PvProfileView, Result> CACHE = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    static Result get(PvProfileView v) {
        Result r = CACHE.get(v);
        boolean settled = PvConstants.pricesSettled();
        if (r != null && (r.pricesLoaded || !settled)) return r;
        r = compute(v, settled);
        if (r != null) CACHE.put(v, r);
        return r;
    }

    static int mp(String tier) {
        return switch (tier) {
            case "COMMON", "SPECIAL" -> 3;
            case "UNCOMMON", "VERY_SPECIAL" -> 5;
            case "RARE" -> 8;
            case "EPIC" -> 12;
            case "LEGENDARY" -> 16;
            case "MYTHIC", "DIVINE" -> 22;
            default -> 0;
        };
    }

    private static volatile JsonObject familiesFor;
    private static volatile Map<String, List<String>> familiesCache;

    static Map<String, List<String>> families(JsonObject misc) {
        if (misc == familiesFor && familiesCache != null) return familiesCache;
        Map<String, List<String>> family = new HashMap<>();
        JsonObject upgrades = PvJson.obj(misc, "talisman_upgrades");
        if (upgrades != null) {
            for (var e : upgrades.entrySet()) {
                if (!e.getValue().isJsonArray()) continue;
                List<String> fam = new ArrayList<>();
                fam.add(e.getKey());
                for (JsonElement u : e.getValue().getAsJsonArray()) fam.add(u.getAsString());
                for (String member : fam) {
                    List<String> cur = family.get(member);
                    if (cur == null || fam.size() > cur.size()) family.put(member, fam);
                }
            }
        }
        for (List<String> chain : CHAINS) for (String member : chain) family.put(member, chain);
        for (List<String> group : EXCLUSIVE) for (String member : group) family.remove(member);
        familiesCache = family;
        familiesFor = misc;
        return family;
    }

    private static Result compute(PvProfileView v, boolean pricesLoaded) {
        JsonObject list = PvConstants.accessories();
        JsonObject misc = PvConstants.misc();
        if (list == null || misc == null) return null;

        Map<String, JsonObject> all = new HashMap<>();
        for (JsonElement e : list.getAsJsonArray("items")) all.put(PvJson.str(e, "", "id"), e.getAsJsonObject());

        Set<String> ignored = new HashSet<>();
        JsonElement ig = misc.get("ignored_talisman");
        if (ig != null && ig.isJsonArray()) for (JsonElement e : ig.getAsJsonArray()) ignored.add(e.getAsString());

        Map<String, List<String>> family = families(misc);
        ignored.addAll(UNRELEASED);

        Set<String> owned = new HashSet<>();
        Map<String, Integer> ownedMp = new HashMap<>();
        Set<String> ownedRecomb = new HashSet<>();
        for (int i = 0; i < v.accessories.size(); i++) {
            ItemStack s = v.accessories.get(i);
            owned.add(PvGear.id(s));
            if (!ownedMp.containsKey(PvGear.id(s)) && PvGear.data(s).getIntOr("rarity_upgrades", 0) > 0) ownedRecomb.add(PvGear.id(s));
            ownedMp.putIfAbsent(PvGear.id(s), PvInactiveAccessories.magicalPower(s, v.accessoryColors.get(i)));
        }
        com.google.gson.JsonElement consumed = PvJson.path(v.member, "rift", "access", "consumed_prism");
        if (consumed != null && consumed.isJsonPrimitive() && consumed.getAsBoolean()) owned.add("RIFT_PRISM");

        boolean ownsHat = owned.stream().anyMatch(PvMissingAccessories::isHat);
        Entry bestHat = null;
        Map<List<String>, Entry> bestAlternative = new HashMap<>();

        List<Entry> ups = new ArrayList<>(), miss = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (var e : all.entrySet()) {
            String id = e.getKey();
            if (owned.contains(id) || ignored.contains(id) || !seen.add(id)) continue;
            List<String> group = exclusiveGroup(id);
            if (group != null) {
                if (group.stream().anyMatch(owned::contains)) continue;
                String groupTier = PvJson.str(e.getValue(), "COMMON", "tier");
                Entry alt = new Entry(id, PvJson.str(e.getValue(), id, "name"), groupTier, mp(groupTier), null, PvConstants.price(id), false, 0);
                bestAlternative.merge(group, alt, (a, b) -> b.perMp() < a.perMp() ? b : a);
                continue;
            }
            if (isHat(id)) {
                if (ownsHat) continue;
                Entry hat = new Entry(id, PvJson.str(e.getValue(), id, "name"), PvJson.str(e.getValue(), "COMMON", "tier"),
                        mp(PvJson.str(e.getValue(), "COMMON", "tier")), null, PvConstants.price(id), false, 0);
                if (bestHat == null || hat.perMp() < bestHat.perMp()) bestHat = hat;
                continue;
            }
            List<String> fam = family.getOrDefault(id, List.of(id));
            int bestOwned = -1;
            for (int i = 0; i < fam.size(); i++) if (owned.contains(fam.get(i))) bestOwned = i;
            String tier = PvJson.str(e.getValue(), "COMMON", "tier");
            String name = PvJson.str(e.getValue(), id, "name");
            if (bestOwned >= 0) {
                if (fam.indexOf(id) <= bestOwned) continue;
                String from = fam.get(bestOwned);
                JsonObject fromItem = all.get(from);
                int current = ownedMp.getOrDefault(from, fromItem != null ? mp(PvJson.str(fromItem, "COMMON", "tier")) : 0);
                boolean recomb = ownedRecomb.contains(from) && recombobulatedTier(tier) != null
                        && !PvConstants.noRecombobulator().contains(id);
                int gain = mp(recomb ? recombobulatedTier(tier) : tier) - current;
                ups.add(new Entry(id, name, tier, gain, fromItem != null ? PvJson.str(fromItem, from, "name") : from,
                        PvConstants.price(id), recomb, 0));
            } else if (id.equals(topListed(fam, all))) {
                miss.add(new Entry(id, name, tier, mp(tier), null, PvConstants.price(id), false, 0));
            }
        }
        ups.addAll(recombobulators(v));
        if (bestHat != null) miss.add(bestHat);
        miss.addAll(bestAlternative.values());
        Comparator<Entry> order = Comparator.comparingDouble((Entry en) -> en.perMp() == Double.MAX_VALUE ? en.perMp() : Math.round(en.perMp()))
                .thenComparingInt(en -> en.count > 0 ? -rarityRank(en.tier) : 0)
                .thenComparingInt(en -> -en.mpGain).thenComparing(Entry::name);
        ups.sort(order);
        miss.sort(order);
        int upMp = 0, missMp = 0;
        Map<String, Integer> bestGain = new HashMap<>();
        for (Entry en : ups) {
            if (en.count > 0) { upMp += en.mpGain; continue; }
            List<String> fam = family.getOrDefault(en.id, List.of(en.id));
            bestGain.merge(fam.get(0), en.mpGain, Math::max);
        }
        for (int g : bestGain.values()) upMp += Math.max(0, g);
        for (Entry en : miss) missMp += en.mpGain;
        return new Result(ups, miss, upMp, missMp, pricesLoaded);
    }

    private static List<Entry> recombobulators(PvProfileView v) {
        java.util.Set<ItemStack> inactive = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        List<PvInactiveAccessories.Entry> idle = PvInactiveAccessories.get(v);
        if (idle != null) for (PvInactiveAccessories.Entry e : idle) inactive.add(e.stack());

        Map<String, Integer> needed = new java.util.LinkedHashMap<>();
        for (int i = 0; i < v.accessories.size(); i++) {
            ItemStack stack = v.accessories.get(i);
            int rgb = v.accessoryColors.get(i);
            if (inactive.contains(stack) || !PvScreen.canRecombobulate(stack, rgb)) continue;
            String tier = switch (rgb) {
                case 0xFFFFFF -> "COMMON";
                case 0x55FF55 -> "UNCOMMON";
                case 0x5555FF -> "RARE";
                case 0xAA00AA -> "EPIC";
                case 0xFFAA00 -> "LEGENDARY";
                case 0xFF5555 -> "SPECIAL";
                default -> null;
            };
            if (tier != null) needed.merge(tier, 1, Integer::sum);
        }
        double price = PvConstants.price("RECOMBOBULATOR_3000");
        List<Entry> out = new ArrayList<>();
        for (var e : needed.entrySet()) {
            String next = recombobulatedTier(e.getKey());
            int gain = next != null ? mp(next) - mp(e.getKey()) : 0;
            if (gain <= 0) continue;
            out.add(new Entry("RECOMBOBULATOR_3000", "Recombobulator 3000", e.getKey(), gain * e.getValue(), null,
                    price > 0 ? price * e.getValue() : -1, false, e.getValue()));
        }
        return out;
    }

    private static int rarityRank(String tier) {
        return switch (tier) {
            case "COMMON" -> 0;
            case "UNCOMMON" -> 1;
            case "RARE" -> 2;
            case "EPIC" -> 3;
            case "LEGENDARY" -> 4;
            default -> -1;
        };
    }

    private static String recombobulatedTier(String tier) {
        return switch (tier) {
            case "COMMON" -> "UNCOMMON";
            case "UNCOMMON" -> "RARE";
            case "RARE" -> "EPIC";
            case "EPIC" -> "LEGENDARY";
            case "LEGENDARY" -> "MYTHIC";
            case "SPECIAL" -> "VERY_SPECIAL";
            default -> null;
        };
    }

    static List<String> exclusiveGroup(String id) {
        for (List<String> group : EXCLUSIVE) if (group.contains(id)) return group;
        return null;
    }

    static boolean isHat(String id) {
        return id.startsWith("PARTY_HAT_") || id.startsWith("BALLOON_HAT_");
    }

    private static String topListed(List<String> fam, Map<String, JsonObject> all) {
        for (int i = fam.size() - 1; i >= 0; i--) if (all.containsKey(fam.get(i))) return fam.get(i);
        return fam.get(fam.size() - 1);
    }
}
