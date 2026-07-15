package com.kuudrahelper.features.misckuudra.profittracker;

import com.kuudrahelper.KuudraConfig;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class KuudraLootTables {

    private KuudraLootTables() {}

    private enum PriceType { BAZAAR_SELL, AH_BIN, ARMOR, NONE }

    private record Entry(String name, double chancePercent, PriceType priceType, String priceId, double quantity) {
        Entry(String name, double chancePercent, PriceType priceType, String priceId) {
            this(name, chancePercent, priceType, priceId, 1.0);
        }
    }

    private record Guaranteed(PriceType priceType, String priceId, double quantity) {}

    private record Tier(List<Guaranteed> guaranteed, List<Entry> slot1, List<Entry> slot2) {}

    private static final PriceType S = PriceType.BAZAAR_SELL;
    private static final PriceType B = PriceType.AH_BIN;
    private static final PriceType A = PriceType.ARMOR;
    private static final PriceType NONE = PriceType.NONE;

    private static final String[] ARMOR_SETS  = {"AURORA", "CRIMSON", "FERVOR", "HOLLOW", "TERROR"};
    private static final String[] ARMOR_SLOTS = {"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"};

    private static List<Entry> armorEntries(double chancePerPiece) {
        List<Entry> out = new ArrayList<>();
        for (String set : ARMOR_SETS) {
            for (String slot : ARMOR_SLOTS) {
                out.add(new Entry(set + " " + slot, chancePerPiece, A, set + "_" + slot));
            }
        }
        return out;
    }

    private static final String FATAL_TEMPO_I = "ENCHANTMENT_ULTIMATE_FATAL_TEMPO_1";
    private static final String INFERNO_I     = "ENCHANTMENT_ULTIMATE_INFERNO_1";

    private static List<Entry> manaBooks(double chanceEach, int level) {
        List<Entry> out = new ArrayList<>();
        out.add(new Entry("Enchanted Book (Strong Mana)", chanceEach, S, "ENCHANTMENT_STRONG_MANA_" + level));
        out.add(new Entry("Enchanted Book (Hardened Mana)", chanceEach, S, "ENCHANTMENT_HARDENED_MANA_" + level));
        out.add(new Entry("Enchanted Book (Ferocious Mana)", chanceEach, S, "ENCHANTMENT_FEROCIOUS_MANA_" + level));
        out.add(new Entry("Enchanted Book (Mana Vampire)", chanceEach, S, "ENCHANTMENT_MANA_VAMPIRE_" + level));
        return out;
    }

    private static final java.util.Map<Integer, Tier> TIERS = new java.util.HashMap<>();
    static {
        // ── Basic (T1) ──────────────────────────────────────────────────────
        {
            List<Guaranteed> g = List.of(
                new Guaranteed(S, KuudraDrops.CRIMSON_ESSENCE, 80),
                new Guaranteed(S, KuudraDrops.KUUDRA_TEETH, 1),
                new Guaranteed(S, "SHARD_KRAKEN", 1)
            );
            List<Entry> s1 = new ArrayList<>(armorEntries(4.31));
            s1.add(new Entry("Aurora Staff", 1.05, A, "AURORA_STAFF"));
            s1.add(new Entry("Hollow Wand", 1.05, B, "HOLLOW_WAND"));
            s1.add(new Entry("Molten Necklace", 1.20, A, "MOLTEN_NECKLACE"));
            s1.add(new Entry("Molten Cloak", 1.20, A, "MOLTEN_CLOAK"));
            s1.add(new Entry("Molten Belt", 1.20, A, "MOLTEN_BELT"));
            s1.add(new Entry("Molten Bracelet", 1.20, A, "MOLTEN_BRACELET"));
            s1.add(new Entry("Tentacle Dye", 0.001, B, KuudraDrops.TENTACLE_DYE));
            s1.add(new Entry("Bezal Shard", 6.76, S, "SHARD_BEZAL"));

            List<Entry> s2 = new ArrayList<>();
            s2.add(new Entry("Wheel of Fate", 0.53, B, KuudraDrops.WHEEL_OF_FATE));
            s2.add(new Entry("Enchanted Book (Fatal Tempo I)", 0.03, S, FATAL_TEMPO_I));
            s2.add(new Entry("Enchanted Book (Inferno I)", 0.03, S, INFERNO_I));
            s2.addAll(manaBooks(21.87, 1));
            s2.add(new Entry("Bezal Shard", 11.93, S, "SHARD_BEZAL"));
            TIERS.put(1, new Tier(g, s1, s2));
        }

        // ── Hot (T2) ────────────────────────────────────────────────────────
        {
            List<Guaranteed> g = List.of(
                new Guaranteed(S, KuudraDrops.CRIMSON_ESSENCE, 200),
                new Guaranteed(S, KuudraDrops.KUUDRA_TEETH, 1),
                new Guaranteed(S, "SHARD_KRAKEN", 1)
            );
            List<Entry> s1 = new ArrayList<>(armorEntries(4.06));
            s1.add(new Entry("Aurora Staff", 0.99, A, "AURORA_STAFF"));
            s1.add(new Entry("Hollow Wand", 0.99, B, "HOLLOW_WAND"));
            s1.add(new Entry("Molten Necklace", 1.27, A, "MOLTEN_NECKLACE"));
            s1.add(new Entry("Molten Cloak", 1.27, A, "MOLTEN_CLOAK"));
            s1.add(new Entry("Molten Belt", 1.27, A, "MOLTEN_BELT"));
            s1.add(new Entry("Molten Bracelet", 1.27, A, "MOLTEN_BRACELET"));
            s1.add(new Entry("Tentacle Dye", 0.00125, B, KuudraDrops.TENTACLE_DYE));
            s1.add(new Entry("Bezal Shard", 6.36, S, "SHARD_BEZAL"));
            s1.add(new Entry("Magma Slug Shard", 5.30, S, "SHARD_MAGMA_SLUG"));

            List<Entry> s2 = new ArrayList<>();
            s2.add(new Entry("Wheel of Fate", 1.19, B, KuudraDrops.WHEEL_OF_FATE));
            s2.add(new Entry("Enchanted Book (Fatal Tempo I)", 0.13, S, FATAL_TEMPO_I));
            s2.add(new Entry("Enchanted Book (Inferno I)", 0.13, S, INFERNO_I));
            s2.addAll(manaBooks(19.71, 2));
            s2.add(new Entry("Bezal Shard", 10.75, S, "SHARD_BEZAL"));
            s2.add(new Entry("Magma Slug Shard", 8.96, S, "SHARD_MAGMA_SLUG"));
            TIERS.put(2, new Tier(g, s1, s2));
        }

        // ── Burning (T3) ────────────────────────────────────────────────────
        {
            List<Guaranteed> g = List.of(
                new Guaranteed(S, KuudraDrops.CRIMSON_ESSENCE, 400),
                new Guaranteed(S, KuudraDrops.KUUDRA_TEETH, 2),
                new Guaranteed(S, "SHARD_KRAKEN", 1)
            );
            List<Entry> s1 = new ArrayList<>(armorEntries(3.43));
            s1.add(new Entry("Aurora Staff", 0.84, A, "AURORA_STAFF"));
            s1.add(new Entry("Burning Kuudra Core", 0.12, B, "BURNING_KUUDRA_CORE"));
            s1.add(new Entry("Hollow Wand", 0.84, B, "HOLLOW_WAND"));
            s1.add(new Entry("Mandraa", 0.96, S, "MANDRAA"));
            s1.add(new Entry("Molten Necklace", 1.19, A, "MOLTEN_NECKLACE"));
            s1.add(new Entry("Molten Cloak", 1.19, A, "MOLTEN_CLOAK"));
            s1.add(new Entry("Molten Belt", 1.19, A, "MOLTEN_BELT"));
            s1.add(new Entry("Molten Bracelet", 1.19, A, "MOLTEN_BRACELET"));
            s1.add(new Entry("Tentacle Dye", 0.00167, B, KuudraDrops.TENTACLE_DYE));
            s1.add(new Entry("Bezal Shard", 5.38, S, "SHARD_BEZAL"));
            s1.add(new Entry("Magma Slug Shard", 4.48, S, "SHARD_MAGMA_SLUG"));
            s1.add(new Entry("Kada Knight Shard", 3.64, S, "SHARD_KADA_KNIGHT"));
            s1.add(new Entry("Wither Specter Shard", 3.64, S, "SHARD_WITHER_SPECTER"));
            s1.add(new Entry("Matcho Shard", 3.64, S, "SHARD_MATCHO"));
            s1.add(new Entry("Lava Flame Shard", 2.99, S, "SHARD_LAVA_FLAME"));

            List<Entry> s2 = new ArrayList<>();
            s2.add(new Entry("Crimson Essence", 7.82, S, KuudraDrops.CRIMSON_ESSENCE, 500));
            s2.add(new Entry("Kuudra Teeth", 7.82, S, KuudraDrops.KUUDRA_TEETH, 5));
            s2.add(new Entry("Wheel of Fate", 1.25, B, KuudraDrops.WHEEL_OF_FATE));
            s2.add(new Entry("Enchanted Book (Fatal Tempo I)", 0.16, S, FATAL_TEMPO_I));
            s2.add(new Entry("Enchanted Book (Inferno I)", 0.16, S, INFERNO_I));
            s2.addAll(manaBooks(12.81, 3));
            s2.add(new Entry("Bezal Shard", 7.04, S, "SHARD_BEZAL"));
            s2.add(new Entry("Magma Slug Shard", 5.87, S, "SHARD_MAGMA_SLUG"));
            s2.add(new Entry("Kada Knight Shard", 4.77, S, "SHARD_KADA_KNIGHT"));
            s2.add(new Entry("Wither Specter Shard", 4.77, S, "SHARD_WITHER_SPECTER"));
            s2.add(new Entry("Matcho Shard", 4.77, S, "SHARD_MATCHO"));
            s2.add(new Entry("Lava Flame Shard", 3.91, S, "SHARD_LAVA_FLAME"));
            TIERS.put(3, new Tier(g, s1, s2));
        }

        // ── Fiery (T4) ──────────────────────────────────────────────────────
        {
            List<Guaranteed> g = List.of(
                new Guaranteed(S, KuudraDrops.CRIMSON_ESSENCE, 1000),
                new Guaranteed(S, KuudraDrops.KUUDRA_TEETH, 2),
                new Guaranteed(S, "SHARD_KRAKEN", 1.25) // 75% 1x, 25% 2x
            );
            List<Entry> s1 = new ArrayList<>(armorEntries(3.09));
            s1.add(new Entry("Aurora Staff", 0.75, A, "AURORA_STAFF"));
            s1.add(new Entry("Burning Kuudra Core", 0.32, B, "BURNING_KUUDRA_CORE"));
            s1.add(new Entry("Hollow Wand", 0.75, B, "HOLLOW_WAND"));
            s1.add(new Entry("Mandraa", 1.08, S, "MANDRAA"));
            s1.add(new Entry("Molten Necklace", 1.18, A, "MOLTEN_NECKLACE"));
            s1.add(new Entry("Molten Cloak", 1.18, A, "MOLTEN_CLOAK"));
            s1.add(new Entry("Molten Belt", 1.18, A, "MOLTEN_BELT"));
            s1.add(new Entry("Molten Bracelet", 1.18, A, "MOLTEN_BRACELET"));
            s1.add(new Entry("Tentacle Dye", 0.0025, B, KuudraDrops.TENTACLE_DYE));
            s1.add(new Entry("Bezal Shard", 4.84, S, "SHARD_BEZAL"));
            s1.add(new Entry("Magma Slug Shard", 4.03, S, "SHARD_MAGMA_SLUG"));
            s1.add(new Entry("Kada Knight Shard", 3.28, S, "SHARD_KADA_KNIGHT"));
            s1.add(new Entry("Wither Specter Shard", 3.28, S, "SHARD_WITHER_SPECTER"));
            s1.add(new Entry("Matcho Shard", 3.28, S, "SHARD_MATCHO"));
            s1.add(new Entry("Lava Flame Shard", 2.69, S, "SHARD_LAVA_FLAME"));
            s1.add(new Entry("Fire Eel Shard", 2.04, S, "SHARD_FIRE_EEL"));
            s1.add(new Entry("Flare Shard", 2.04, S, "SHARD_FLARE"));
            s1.add(new Entry("Barbarian Duke X Shard", 2.04, S, "SHARD_BARBARIAN_DUKE_X"));
            s1.add(new Entry("Hellwisp Shard", 1.67, S, "SHARD_HELLWISP"));
            s1.add(new Entry("XYZ Shard", 1.34, S, "SHARD_XYZ"));

            List<Entry> s2 = new ArrayList<>();
            s2.add(new Entry("Crimson Essence", 6.35, S, KuudraDrops.CRIMSON_ESSENCE, 500));
            s2.add(new Entry("Crimson Essence", 0.63, S, KuudraDrops.CRIMSON_ESSENCE, 2500));
            s2.add(new Entry("Heavy Pearl", 6.35, S, KuudraDrops.HEAVY_PEARL, 3));
            s2.add(new Entry("Kuudra Teeth", 6.35, S, KuudraDrops.KUUDRA_TEETH, 5));
            s2.add(new Entry("Kuudra Teeth", 0.63, S, KuudraDrops.KUUDRA_TEETH, 25));
            s2.add(new Entry("Wheel of Fate", 1.40, B, KuudraDrops.WHEEL_OF_FATE));
            s2.add(new Entry("Enchanted Book (Fatal Tempo I)", 0.18, S, FATAL_TEMPO_I));
            s2.add(new Entry("Enchanted Book (Inferno I)", 0.18, S, INFERNO_I));
            s2.addAll(manaBooks(10.47, 4));
            s2.add(new Entry("Bezal Shard", 5.71, S, "SHARD_BEZAL"));
            s2.add(new Entry("Magma Slug Shard", 4.76, S, "SHARD_MAGMA_SLUG"));
            s2.add(new Entry("Kada Knight Shard", 3.87, S, "SHARD_KADA_KNIGHT"));
            s2.add(new Entry("Wither Specter Shard", 3.87, S, "SHARD_WITHER_SPECTER"));
            s2.add(new Entry("Matcho Shard", 3.87, S, "SHARD_MATCHO"));
            s2.add(new Entry("Lava Flame Shard", 3.17, S, "SHARD_LAVA_FLAME"));
            s2.add(new Entry("Fire Eel Shard", 2.41, S, "SHARD_FIRE_EEL"));
            s2.add(new Entry("Flare Shard", 2.41, S, "SHARD_FLARE"));
            s2.add(new Entry("Barbarian Duke X Shard", 2.41, S, "SHARD_BARBARIAN_DUKE_X"));
            s2.add(new Entry("Hellwisp Shard", 1.97, S, "SHARD_HELLWISP"));
            s2.add(new Entry("XYZ Shard", 1.59, S, "SHARD_XYZ"));
            TIERS.put(4, new Tier(g, s1, s2));
        }

        // ── Infernal (T5) ───────────────────────────────────────────────────
        {
            List<Guaranteed> g = List.of(
                new Guaranteed(S, KuudraDrops.CRIMSON_ESSENCE, 2000),
                new Guaranteed(S, KuudraDrops.KUUDRA_TEETH, 3.5), // "3-4x", assumed even split
                new Guaranteed(S, "SHARD_KRAKEN", 1.25)           // 75% 1x, 25% 2x
            );
            List<Entry> s1 = new ArrayList<>();
            s1.add(new Entry("Ananke Shard", 0.05, S, "SHARD_ANANKE"));
            s1.add(new Entry("Hellstorm Wand", 0.1, B, "HELLSTORM_WAND"));
            s1.add(new Entry("Tormentor", 0.1, B, "TORMENTOR"));
            s1.add(new Entry("Ananke Feather", 0.43, S, "ANANKE_FEATHER"));
            s1.add(new Entry("Burning Kuudra Core", 0.51, B, "BURNING_KUUDRA_CORE"));
            s1.add(new Entry("Daemon Shard", 0.51, S, "SHARD_DAEMON"));
            s1.add(new Entry("Lord Jawbus Shard", 0.51, S, "SHARD_LORD_JAWBUS"));
            s1.add(new Entry("Moltenfish Shard", 0.51, S, "SHARD_MOLTENFISH"));
            s1.add(new Entry("Cinderbat Shard", 0.67, S, "SHARD_CINDERBAT"));
            s1.add(new Entry("Taurus Shard", 0.67, S, "SHARD_TAURUS"));
            s1.add(new Entry("Hollow Wand", 0.72, B, "HOLLOW_WAND"));
            s1.add(new Entry("Aurora Staff", 0.72, A, "AURORA_STAFF"));
            s1.add(new Entry("Mandraa", 1.23, S, "MANDRAA"));
            s1.add(new Entry("Molten Belt", 1.23, A, "MOLTEN_BELT"));
            s1.add(new Entry("Molten Bracelet", 1.23, A, "MOLTEN_BRACELET"));
            s1.add(new Entry("Molten Cloak", 1.23, A, "MOLTEN_CLOAK"));
            s1.add(new Entry("Molten Necklace", 1.23, A, "MOLTEN_NECKLACE"));
            s1.add(new Entry("XYZ Shard", 1.29, S, "SHARD_XYZ"));
            s1.add(new Entry("Hellwisp Shard", 1.59, S, "SHARD_HELLWISP"));
            s1.add(new Entry("Barbarian Duke X Shard", 1.95, S, "SHARD_BARBARIAN_DUKE_X"));
            s1.add(new Entry("Fire Eel Shard", 1.95, S, "SHARD_FIRE_EEL"));
            s1.add(new Entry("Flare Shard", 1.95, S, "SHARD_FLARE"));
            s1.add(new Entry("Lava Flame Shard", 2.57, S, "SHARD_LAVA_FLAME"));
            s1.addAll(armorEntries(2.96));
            s1.add(new Entry("Kada Knight Shard", 3.14, S, "SHARD_KADA_KNIGHT"));
            s1.add(new Entry("Matcho Shard", 3.14, S, "SHARD_MATCHO"));
            s1.add(new Entry("Wither Specter Shard", 3.14, S, "SHARD_WITHER_SPECTER"));
            s1.add(new Entry("Magma Slug Shard", 3.86, S, "SHARD_MAGMA_SLUG"));
            s1.add(new Entry("Bezal Shard", 4.63, S, "SHARD_BEZAL"));

            List<Entry> s2 = new ArrayList<>();
            s2.add(new Entry("Kuudra Teeth", 0.06, S, KuudraDrops.KUUDRA_TEETH, 100));
            s2.add(new Entry("Crimson Essence", 0.06, S, KuudraDrops.CRIMSON_ESSENCE, 10000));
            s2.add(new Entry("Ananke Shard", 0.06, S, "SHARD_ANANKE"));
            s2.add(new Entry("Enchanted Book (Inferno I)", 0.22, S, INFERNO_I));
            s2.add(new Entry("Enchanted Book (Fatal Tempo I)", 0.22, S, FATAL_TEMPO_I));
            s2.add(new Entry("Kuudra Teeth", 0.58, S, KuudraDrops.KUUDRA_TEETH, 25));
            s2.add(new Entry("Crimson Essence", 0.58, S, KuudraDrops.CRIMSON_ESSENCE, 2500));
            s2.add(new Entry("Heavy Pearl", 0.58, S, KuudraDrops.HEAVY_PEARL, 10));
            s2.add(new Entry("Lord Jawbus Shard", 0.58, S, "SHARD_LORD_JAWBUS"));
            s2.add(new Entry("Daemon Shard", 0.58, S, "SHARD_DAEMON"));
            s2.add(new Entry("Moltenfish Shard", 0.58, S, "SHARD_MOLTENFISH"));
            s2.add(new Entry("Taurus Shard", 0.76, S, "SHARD_TAURUS"));
            s2.add(new Entry("Cinderbat Shard", 0.76, S, "SHARD_CINDERBAT"));
            s2.add(new Entry("Dusty Travel Scroll", 0.87, B, KuudraDrops.DUSTY_TRAVEL_SCROLL));
            s2.add(new Entry("XYZ Shard", 1.46, S, "SHARD_XYZ"));
            s2.add(new Entry("Kuudra Mandible", 1.75, S, "KUUDRA_MANDIBLE"));
            s2.add(new Entry("Hellwisp Shard", 1.81, S, "SHARD_HELLWISP"));
            s2.add(new Entry("Fire Eel Shard", 2.21, S, "SHARD_FIRE_EEL"));
            s2.add(new Entry("Flare Shard", 2.21, S, "SHARD_FLARE"));
            s2.add(new Entry("Barbarian Duke X Shard", 2.21, S, "SHARD_BARBARIAN_DUKE_X"));
            s2.add(new Entry("Wheel of Fate", 2.68, B, KuudraDrops.WHEEL_OF_FATE));
            s2.add(new Entry("Lava Flame Shard", 2.91, S, "SHARD_LAVA_FLAME"));
            s2.add(new Entry("Kada Knight Shard", 3.56, S, "SHARD_KADA_KNIGHT"));
            s2.add(new Entry("Wither Specter Shard", 3.56, S, "SHARD_WITHER_SPECTER"));
            s2.add(new Entry("Matcho Shard", 3.56, S, "SHARD_MATCHO"));
            s2.add(new Entry("Magma Slug Shard", 4.37, S, "SHARD_MAGMA_SLUG"));
            s2.add(new Entry("Bezal Shard", 5.25, S, "SHARD_BEZAL"));
            s2.add(new Entry("Kuudra Teeth", 5.83, S, KuudraDrops.KUUDRA_TEETH, 5));
            s2.add(new Entry("Crimson Essence", 5.83, S, KuudraDrops.CRIMSON_ESSENCE, 500));
            s2.add(new Entry("Heavy Pearl", 5.83, S, KuudraDrops.HEAVY_PEARL, 3));
            s2.addAll(manaBooks(9.62, 5));
            TIERS.put(5, new Tier(g, s1, s2));
        }
    }

    private static final Set<String> ATTRIBUTE_SHARD_IDS = new LinkedHashSet<>(List.of(
        "SHARD_BEZAL", "SHARD_MAGMA_SLUG", "SHARD_KADA_KNIGHT", "SHARD_WITHER_SPECTER",
        "SHARD_MATCHO", "SHARD_LAVA_FLAME", "SHARD_FIRE_EEL", "SHARD_FLARE",
        "SHARD_BARBARIAN_DUKE_X", "SHARD_HELLWISP", "SHARD_XYZ", "SHARD_DAEMON",
        "SHARD_LORD_JAWBUS", "SHARD_MOLTENFISH", "SHARD_CINDERBAT", "SHARD_TAURUS", "SHARD_ANANKE"
    ));

    private static double priceOf(PriceType type, String id) {
        if (type == NONE || id == null) return -1;

        if (type == A) {
            if (KuudraConfig.isProfitArmorSalvage()) {
                int essence = KuudraDrops.salvageEssence(id, 0);
                double essencePrice = priceOf(S, KuudraDrops.CRIMSON_ESSENCE);
                return essencePrice > 0 ? essence * essencePrice : -1;
            }
            return PriceCache.getBin(id);
        }

        if (type == B) return PriceCache.getBin(id);

        double price = CroesusListener.bazaarSellPrice(id);
        if (price > 0 && KuudraDrops.CRIMSON_ESSENCE.equals(id)) {
            price *= KuudraConfig.getKuudraPetEssenceMultiplier();
        }
        return price;
    }

    public static double expectedChestValue(int tier) {
        Tier t = TIERS.get(tier);
        if (t == null) return -1;

        double total = 0;
        for (Guaranteed g : t.guaranteed()) {
            double p = priceOf(g.priceType(), g.priceId());
            if (p > 0) total += p * g.quantity();
        }
        total += slotExpectedValue(t.slot1());
        total += slotExpectedValue(t.slot2());
        return total;
    }

    public static double expectedAttributeSlotValue(int tier) {
        Tier t = TIERS.get(tier);
        if (t == null) return -1;
        double slot1Attr = slotExpectedAttributeValue(t.slot1());
        double slot2Attr = slotExpectedAttributeValue(t.slot2());
        return (slot1Attr + slot2Attr) / 2.0;
    }

    private static double slotExpectedValue(List<Entry> entries) {
        double total = 0;
        for (Entry e : entries) {
            double p = priceOf(e.priceType(), e.priceId());
            if (p > 0) total += (e.chancePercent() / 100.0) * p * e.quantity();
        }
        return total;
    }

    private static double slotExpectedAttributeValue(List<Entry> entries) {
        double total = 0;
        for (Entry e : entries) {
            if (e.priceId() == null || !ATTRIBUTE_SHARD_IDS.contains(e.priceId())) continue;
            double p = priceOf(e.priceType(), e.priceId());
            if (p > 0) total += (e.chancePercent() / 100.0) * p;
        }
        return total;
    }
}
