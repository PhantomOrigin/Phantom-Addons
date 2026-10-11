package com.phantomaddons.features.pv;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

final class PvNetworth {

    private PvNetworth() {}

    private static final int ENDER_PAGE = 45;
    private static final double ENCHANT_FACTOR = 0.85;
    private static final double ESSENCE_FACTOR = 0.75;
    private static final double MISC_FACTOR = 0.6;
    private static final double COLLECTED_COINS_FACTOR = 0.8;
    private static final String[] MASTER_STARS = { "FIRST_MASTER_STAR", "SECOND_MASTER_STAR", "THIRD_MASTER_STAR",
            "FOURTH_MASTER_STAR", "FIFTH_MASTER_STAR" };
    private static final String[] PARTS = { "drill_part_engine", "drill_part_fuel_tank", "drill_part_upgrade_module",
            "rod_part_line", "rod_part_hook", "rod_part_sinker" };
    private static final String[][] MISC = {
            { "art_of_war_count", "THE_ART_OF_WAR" }, { "artOfPeaceApplied", "THE_ART_OF_PEACE" },
            { "wood_singularity_count", "WOOD_SINGULARITY" }, { "tuned_transmission", "TRANSMISSION_TUNER" },
            { "mana_disintegrator_count", "MANA_DISINTEGRATOR" }, { "farming_for_dummies_count", "FARMING_FOR_DUMMIES" },
            { "jalapeno_count", "JALAPENO_BOOK" }, { "polarvoid_book_count", "POLARVOID_BOOK" },
            { "wet_book_count", "WET_BOOK" } };

    record Result(double total, double cosmetic, double museum, Map<String, Double> parts, double[] enderPages, double[] backpacks) {
        boolean museumIncluded() { return museum >= 0; }
        double nonCosmetic() { return total - cosmetic; }
    }

    private record Slot(double museum, CompletableFuture<Result> future, Result last) {}

    private static final Map<PvProfileView, Slot> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    static Result get(PvProfileView v, double museum) {
        if (v == null || !v.itemsReady() || !PvConstants.networthDataSettled()) return null;
        Slot slot = CACHE.get(v);
        if (slot == null || slot.museum() != museum) {
            Result previous = slot == null ? null : finished(slot);
            slot = new Slot(museum, CompletableFuture.supplyAsync(() -> compute(v, museum), PvItems.EXECUTOR), previous);
            CACHE.put(v, slot);
        }
        Result done = finished(slot);
        return done != null ? done : slot.last();
    }

    private static Result finished(Slot slot) {
        CompletableFuture<Result> f = slot.future();
        return f.isDone() && !f.isCompletedExceptionally() ? f.join() : slot.last();
    }

    static String status(PvProfileView v) {
        if (v == null || !v.itemsReady()) return "§8loading items...";
        if (!PvConstants.networthDataSettled()) return "§8loading prices...";
        if (PvConstants.lowestBins() == null && PvConstants.bazaar() == null) return "§8prices unavailable";
        Slot slot = CACHE.get(v);
        if (slot != null && slot.future().isCompletedExceptionally()) return "§8unavailable";
        return "§8calculating...";
    }

    // ── Calculation ───────────────────────────────────────────────────────────

    private static Result compute(PvProfileView v, double museum) {
        Prices p = new Prices();
        Map<String, Double> parts = new LinkedHashMap<>();

        parts.put("Coins", Math.max(0, v.purse) + Math.max(0, v.bank) + Math.max(0, v.personalBank));

        double wardrobe = wardrobe(p, v.wardrobe);
        if (v.armorSet(v.wardrobeEquipped) == null) wardrobe += p.sum(List.of(v.armor));
        double equipment = wardrobe(p, v.equipmentWardrobe);
        if (v.equipmentSet(v.equipmentWardrobeEquipped) == null) equipment += p.sum(List.of(v.equipment));
        parts.put("Wardrobe", wardrobe);
        parts.put("Equipment Wardrobe", equipment);
        parts.put("Inventory", p.sum(v.inventory));
        parts.put("Accessories", p.sum(v.accessories));

        List<ItemStack> chest = v.enderChest;
        int pages = (chest.size() + ENDER_PAGE - 1) / ENDER_PAGE;
        double[] enderPages = new double[pages];
        double ender = 0;
        for (int i = 0; i < pages; i++) {
            enderPages[i] = p.sum(chest.subList(i * ENDER_PAGE, Math.min(chest.size(), (i + 1) * ENDER_PAGE)));
            ender += enderPages[i];
        }
        parts.put("Ender Chest", ender);

        double[] backpacks = new double[v.backpacks.size()];
        double packs = 0;
        for (int i = 0; i < backpacks.length; i++) {
            backpacks[i] = p.sum(v.backpacks.get(i).items());
            packs += backpacks[i];
        }
        parts.put("Backpacks", packs);
        parts.put("Vault", p.sum(v.vault));

        double bags = 0;
        for (String key : new String[] { "fishing_bag", "potion_bag", "quiver", "sacks_bag" })
            bags += p.sum(PvItems.decode(PvJson.path(v.member, "inventory", "bag_contents", key)));
        JsonObject sacks = PvJson.obj(v.member, "inventory", "sacks_counts");
        if (sacks != null) {
            for (var e : sacks.entrySet()) {
                if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isNumber())
                    bags += p.unit(e.getKey()) * e.getValue().getAsDouble();
            }
        }
        parts.put("Bags & Sacks", bags);

        double pets = 0;
        for (PvProfileView.Pet pet : v.pets) pets += p.pet(pet.type(), pet.rarity(), pet.level().level(), pet.heldItem(), pet.skin());
        parts.put("Pets", pets);

        double essence = 0;
        JsonObject stock = PvJson.obj(v.member, "currencies", "essence");
        if (stock != null) {
            for (var e : stock.entrySet())
                essence += PvJson.num(e.getValue(), 0, "current") * p.unit("ESSENCE_" + e.getKey().toUpperCase(Locale.ROOT));
        }
        parts.put("Essence", essence);
        if (museum >= 0) parts.put("Museum", museum);

        double total = 0;
        for (double d : parts.values()) total += d;
        return new Result(total, p.cosmeticSum, museum, parts, enderPages, backpacks);
    }

    private static double wardrobe(Prices p, List<PvProfileView.WardrobeSet> sets) {
        double sum = 0;
        for (PvProfileView.WardrobeSet s : sets) sum += p.sum(List.of(s.items()));
        return sum;
    }

    private static final class Prices {
        private final JsonObject bins = orEmpty(PvConstants.lowestBins());
        private final JsonObject bazaar = orEmpty(PvConstants.bazaar());
        private final JsonObject essence = orEmpty(PvConstants.essenceCosts());
        private final JsonObject gemSlots = orEmpty(PvConstants.gemSlots());
        private final Map<String, String> stoneFor = reforgeStones(PvConstants.reforgeStones());
        private final java.util.Set<String> cosmetic = cosmeticIds(PvConstants.accessories());
        double cosmeticSum;

        private static JsonObject orEmpty(JsonObject o) { return o != null ? o : new JsonObject(); }

        private static java.util.Set<String> cosmeticIds(JsonObject slim) {
            java.util.Set<String> out = new java.util.HashSet<>();
            JsonElement list = slim != null ? slim.get("cosmetic") : null;
            if (list != null && list.isJsonArray()) for (JsonElement e : list.getAsJsonArray()) out.add(e.getAsString());
            out.remove("BOOK_OF_STATS"); // filed as cosmetic, but it's a real upgrade
            return out;
        }

        private static Map<String, String> reforgeStones(JsonObject stones) {
            Map<String, String> out = new HashMap<>();
            if (stones == null) return out;
            for (var e : stones.entrySet()) {
                String name = PvJson.str(e.getValue(), "", "reforgeName");
                if (!name.isEmpty()) out.put(name.toLowerCase(Locale.ROOT), e.getKey());
            }
            return out;
        }

        private static double num(JsonObject o, String key) {
            JsonElement e = o.get(key);
            return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsDouble() : -1;
        }

        double bin(String id) { return Math.max(0, num(bins, id)); }

        boolean onBazaar(String id) { return num(bazaar, id) > 0; }

        double unit(String id) {
            if (id == null || id.isEmpty()) return 0;
            double bz = num(bazaar, id);
            return bz > 0 ? bz : bin(id);
        }

        double sum(Iterable<ItemStack> items) {
            double sum = 0;
            for (ItemStack s : items) sum += item(s);
            return sum;
        }

        double item(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return 0;
            CompoundTag d = PvGear.data(stack);
            String id = d.getStringOr("id", "");
            if (id.isEmpty()) return 0;
            int count = Math.max(1, stack.getCount());
            if (cosmetic.contains(id) || id.startsWith("DYE_")) {
                double worth = unit(id) * count;
                cosmeticSum += worth;
                return worth;
            }

            if (id.equals("ENCHANTED_BOOK")) return books(d.getCompoundOrEmpty("enchantments")) * count;
            if (id.equals("PET")) return petItem(d);

            double base = unit(id);
            if (base <= 0 || onBazaar(id)) return base * count; // bazaar goods carry no upgrades
            double x = base;
            x += ENCHANT_FACTOR * books(d.getCompoundOrEmpty("enchantments"));
            int stars = Math.max(d.getIntOr("upgrade_level", 0), d.getIntOr("dungeon_item_level", 0));
            if (stars > 0) x += stars(id, stars);
            String stone = stoneFor.get(d.getStringOr("modifier", "").toLowerCase(Locale.ROOT));
            if (stone != null) x += unit(stone);
            x += gems(d, id);

            ListTag scrolls = d.getListOrEmpty("ability_scroll");
            for (int i = 0; i < scrolls.size(); i++) x += unit(scrolls.getStringOr(i, ""));
            x += unit(d.getStringOr("power_ability_scroll", ""));

            int potato = d.getIntOr("hot_potato_count", 0);
            if (potato > 0) x += Math.min(potato, 10) * unit("HOT_POTATO_BOOK") + Math.max(0, potato - 10) * unit("FUMING_POTATO_BOOK");
            if (d.getIntOr("rarity_upgrades", 0) > 0) x += unit("RECOMBOBULATOR_3000");
            String enrichment = d.getStringOr("talisman_enrichment", "");
            if (!enrichment.isEmpty()) x += unit("TALISMAN_ENRICHMENT_" + enrichment.toUpperCase(Locale.ROOT));
            if (d.getIntOr("stats_book", -1) >= 0) x += unit("BOOK_OF_STATS");
            double dye = unit(d.getStringOr("dye_item", ""));
            x += dye;
            cosmeticSum += dye;
            for (String part : PARTS) x += unit(d.getStringOr(part, "").toUpperCase(Locale.ROOT)); // drill and rod parts
            for (String[] m : MISC) {
                int n = d.getIntOr(m[0], 0);
                if (n > 0) x += unit(m[1]) * Math.min(n, 1000) * MISC_FACTOR;
            }
            double stored = d.getDoubleOr("collected_coins", 0);
            if (stored > 0) x += stored * COLLECTED_COINS_FACTOR;
            return x;
        }

        double books(CompoundTag enchants) {
            double sum = 0;
            for (String key : enchants.keySet()) {
                int level = enchants.getIntOr(key, 0);
                if (level <= 0) continue;
                String name = key.toUpperCase(Locale.ROOT);
                double price = unit("ENCHANTMENT_" + name + "_" + level);
                if (price <= 0) price = bin("ENCHANTED_BOOK-" + name + "-" + level);
                sum += price;
            }
            return sum;
        }

        double stars(String id, int level) {
            JsonObject table = PvJson.obj(essence, id);
            double sum = 0;
            for (int i = 1; i <= level; i++) {
                double amount = table != null ? PvJson.num(table, -1, String.valueOf(i)) : -1;
                if (amount > 0) {
                    sum += amount * unit("ESSENCE_" + PvJson.str(table, "", "type").toUpperCase(Locale.ROOT)) * ESSENCE_FACTOR;
                    JsonElement extra = PvJson.path(table, "items", String.valueOf(i));
                    if (extra != null && extra.isJsonArray()) {
                        for (JsonElement entry : extra.getAsJsonArray()) {
                            String[] part = entry.getAsString().split(":");
                            if (part[0].equals("SKYBLOCK_COIN")) continue;
                            sum += unit(part[0]) * (part.length > 1 ? parseCount(part[1]) : 1);
                        }
                    }
                } else if (i >= 6 && i <= 10) {
                    sum += unit(MASTER_STARS[i - 6]);
                }
            }
            return sum;
        }

        private static double parseCount(String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 1; }
        }

        double gems(CompoundTag d, String id) {
            CompoundTag g = d.getCompoundOrEmpty("gems");
            if (g.isEmpty()) return 0;
            double sum = 0;

            JsonElement defs = gemSlots.get(id);
            ListTag unlocked = g.getListOrEmpty("unlocked_slots");
            for (int i = 0; i < unlocked.size() && defs != null && defs.isJsonArray(); i++) {
                String key = unlocked.getStringOr(i, "");
                int us = key.lastIndexOf('_');
                if (us <= 0) continue;
                String type = key.substring(0, us);
                int wanted;
                try { wanted = Integer.parseInt(key.substring(us + 1)); } catch (NumberFormatException e) { continue; }
                int seen = 0;
                for (JsonElement def : defs.getAsJsonArray()) {
                    if (!PvJson.str(def, "", "t").equals(type) || seen++ != wanted) continue;
                    JsonElement costs = PvJson.path(def, "c");
                    if (costs == null || !costs.isJsonArray()) break;
                    for (JsonElement c : costs.getAsJsonArray()) {
                        JsonArray pair = c.getAsJsonArray();
                        sum += unit(pair.get(0).getAsString()) * pair.get(1).getAsDouble();
                    }
                    break;
                }
            }

            for (String key : g.keySet()) {
                if (key.equals("unlocked_slots") || key.endsWith("_gem")) continue;
                String quality = g.getStringOr(key, "");
                if (quality.isEmpty()) quality = g.getCompoundOrEmpty(key).getStringOr("quality", "");
                if (quality.isEmpty()) continue;
                String type = g.getStringOr(key + "_gem", "");
                if (type.isEmpty()) {
                    int us = key.lastIndexOf('_');
                    type = us > 0 ? key.substring(0, us) : key;
                }
                sum += unit(quality + "_" + type + "_GEM");
            }
            return sum;
        }

        double pet(String type, String rarity, int level, String held, String skin) {
            String base = "PET-" + type + "-" + rarity;
            double v = bin(base);
            if (level >= 200) v = firstPositive(bin(base + "-200"), bin(base + "-100"), v);
            else if (level >= 100) v = firstPositive(bin(base + "-100"), v);
            if (held != null && !held.equals("PET_ITEM_TIER_BOOST")) v += unit(held);
            if (skin != null && !skin.isEmpty()) {
                double worth = unit("PET_SKIN_" + skin);
                v += worth;
                cosmeticSum += worth;
            }
            return v;
        }

        double petItem(CompoundTag d) {
            try {
                JsonObject info = JsonParser.parseString(d.getStringOr("petInfo", "{}")).getAsJsonObject();
                String type = PvJson.str(info, "", "type");
                String tier = PvJson.str(info, "COMMON", "tier");
                String held = PvJson.str(info, null, "heldItem");
                if ("PET_ITEM_TIER_BOOST".equals(held)) {
                    int idx = PvProfileView.rarityIndex(tier);
                    String[] order = { "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC" };
                    if (idx < order.length - 1) tier = order[idx + 1];
                }
                int level = PvLeveling.pet(PvJson.num(info, 0, "exp"), tier, PvProfileView.maxPetLevel(type)).level();
                return type.isEmpty() ? 0 : pet(type, tier, level, held, PvJson.str(info, null, "skin"));
            } catch (RuntimeException e) {
                return 0;
            }
        }

        private static double firstPositive(double... values) {
            for (double v : values) if (v > 0) return v;
            return 0;
        }
    }

    static List<String> tooltip(Result r, String museumNote) {
        List<String> out = new ArrayList<>();
        out.add("§6Networth: §e" + PvScreen.fmtFull(Math.round(r.total())));
        out.add("§6Non-cosmetic networth: §e" + PvScreen.fmtFull(Math.round(r.nonCosmetic())));
        out.add("§6Cosmetic: §e" + PvScreen.fmtFull(Math.round(r.cosmetic())));
        out.add("");
        for (var e : r.parts().entrySet()) {
            if (e.getValue() <= 0) continue;
            out.add("§7" + e.getKey() + ": §6" + PvScreen.fmt(e.getValue()) + " §8(" + percent(e.getValue(), r.total()) + ")");
        }
        out.add("");
        out.add("§8Estimated from Bazaar and lowest BIN prices.");
        out.add("§8Non-cosmetic leaves out dyes, skins and runes.");
        if (!r.museumIncluded() && museumNote != null) out.add("§8" + museumNote);
        return out;
    }

    private static String percent(double part, double total) {
        return total > 0 ? String.format(Locale.ROOT, "%.0f%%", part * 100 / total) : "0%";
    }
}
