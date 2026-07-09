package com.kuudrahelper.features.profittracker;

import java.util.*;

public final class KuudraDrops {

    public static final Map<String, String> ARMOR_NAME_TO_ID;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        for (String[] set : new String[][]{
                {"Aurora","AURORA"}, {"Crimson","CRIMSON"},
                {"Fervor","FERVOR"}, {"Hollow","HOLLOW"},
                {"Terror","TERROR"}}) {
            for (String[] slot : new String[][]{
                    {"Helmet","HELMET"}, {"Chestplate","CHESTPLATE"},
                    {"Leggings","LEGGINGS"}, {"Boots","BOOTS"}}) {
                m.put((set[0] + " " + slot[0]).toLowerCase(), set[1] + "_" + slot[1]);
            }
        }
        ARMOR_NAME_TO_ID = Collections.unmodifiableMap(m);
    }

    public static String armorIdForName(String displayName) {
        String stripped = stripColor(displayName).toLowerCase().trim();
        for (Map.Entry<String, String> e : ARMOR_NAME_TO_ID.entrySet())
            if (stripped.contains(e.getKey())) return e.getValue();
        return null;
    }

    public static final Map<String, String> WEAPON_NAME_TO_ID;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("aurora staff",        "AURORA_STAFF");
        m.put("hollow wand",         "HOLLOW_WAND");
        m.put("hellstorm wand",      "HELLSTORM_WAND");
        m.put("tormentor",           "TORMENTOR");
        m.put("mandraa",             "MANDRAA");
        m.put("ananke feather",      "ANANKE_FEATHER");
        m.put("kuudra mandible",     "KUUDRA_MANDIBLE");
        m.put("burning kuudra core", "BURNING_KUUDRA_CORE");
        m.put("molten necklace",     "MOLTEN_NECKLACE");
        m.put("molten cloak",        "MOLTEN_CLOAK");
        m.put("molten belt",         "MOLTEN_BELT");
        m.put("molten bracelet",     "MOLTEN_BRACELET");
        WEAPON_NAME_TO_ID = Collections.unmodifiableMap(m);
    }

    public static String weaponIdForName(String displayName) {
        String stripped = stripColor(displayName).toLowerCase().trim();
        for (Map.Entry<String, String> e : WEAPON_NAME_TO_ID.entrySet())
            if (stripped.contains(e.getKey())) return e.getValue();
        return null;
    }

    public static final Set<String> AH_WEAPON_IDS = Set.of(
        "HOLLOW_WAND", "HELLSTORM_WAND", "TORMENTOR", "BURNING_KUUDRA_CORE"
    );

    public static final Set<String> AH_ITEM_IDS;
    static {
        Set<String> ids = new java.util.LinkedHashSet<>(AH_WEAPON_IDS);
        ids.addAll(ARMOR_NAME_TO_ID.values());
        AH_ITEM_IDS = Collections.unmodifiableSet(ids);
    }

    public static final Map<String, String> SHARD_NAME_TO_ID;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("kraken shard",           "SHARD_KRAKEN");
        m.put("bezal shard",            "SHARD_BEZAL");
        m.put("magma slug shard",       "SHARD_MAGMA_SLUG");
        m.put("kada knight shard",      "SHARD_KADA_KNIGHT");
        m.put("wither specter shard",   "SHARD_WITHER_SPECTER");
        m.put("matcho shard",           "SHARD_MATCHO");
        m.put("lava flame shard",       "SHARD_LAVA_FLAME");
        m.put("fire eel shard",         "SHARD_FIRE_EEL");
        m.put("flare shard",            "SHARD_FLARE");
        m.put("barbarian duke x shard", "SHARD_BARBARIAN_DUKE_X");
        m.put("hellwisp shard",         "SHARD_HELLWISP");
        m.put("xyz shard",              "SHARD_XYZ");
        m.put("daemon shard",           "SHARD_DAEMON");
        m.put("lord jawbus shard",      "SHARD_LORD_JAWBUS");
        m.put("moltenfish shard",       "SHARD_MOLTENFISH");
        m.put("cinderbat shard",        "SHARD_CINDERBAT");
        m.put("taurus shard",           "SHARD_TAURUS");
        m.put("ananke shard",           "SHARD_ANANKE");
        SHARD_NAME_TO_ID = Collections.unmodifiableMap(m);
    }

    public static String attributeShardId(String displayName, List<String> loreLines) {
        String name = stripColor(displayName).toLowerCase().trim();
        if (!name.contains("shard")) return null;
        for (Map.Entry<String, String> e : SHARD_NAME_TO_ID.entrySet())
            if (name.contains(e.getKey())) return e.getValue();
        return null;
    }

    public static boolean isAttributeShard(String displayName) {
        String n = stripColor(displayName).toLowerCase().trim();
        if (!n.contains("shard")) return false;
        for (String key : SHARD_NAME_TO_ID.keySet())
            if (n.contains(key)) return true;
        return false;
    }

    public static final String CRIMSON_ESSENCE    = "ESSENCE_CRIMSON";
    public static final String KISMET_FEATHER     = "KISMET_FEATHER";
    public static final String WHEEL_OF_FATE      = "WHEEL_OF_FATE";
    public static final String ENCHANTED_MYCELIUM = "ENCHANTED_MYCELIUM";
    public static final String ENCHANTED_RED_SAND = "ENCHANTED_RED_SAND";
    public static final String NETHER_STAR        = "NETHER_STAR";
    public static final String CORRUPTED_FRAGMENT = "CORRUPTED_NETHER_STAR";
    public static final String KUUDRA_TEETH       = "KUUDRA_TEETH";
    public static final String KUUDRA_TENTACLE    = "KUUDRA_TENTACLE";
    public static final String HEAVY_PEARL        = "HEAVY_PEARL";

    public static final List<String> BAZAAR_ITEMS = List.of(
        CRIMSON_ESSENCE, KISMET_FEATHER, WHEEL_OF_FATE,
        ENCHANTED_MYCELIUM, ENCHANTED_RED_SAND, NETHER_STAR, CORRUPTED_FRAGMENT,
        KUUDRA_TEETH, KUUDRA_TENTACLE, HEAVY_PEARL
    );

    public static final int[] FREE_CHEST_ESSENCE = {0, 10, 30, 75, 125, 200}; // index = tier

    public record KeyRecipe(String mageItem, int mageAmt,
                            String barbItem, int barbAmt,
                            String extraItem, int extraAmt,
                            long fixedCoinCost) {}

    public static final KeyRecipe[] KEY_RECIPES = {
        null,                                                                                        // [0] unused
        new KeyRecipe(ENCHANTED_MYCELIUM,  2, ENCHANTED_RED_SAND,  2, null,        0,          0L), // T1
        new KeyRecipe(ENCHANTED_MYCELIUM,  6, ENCHANTED_RED_SAND,  6, null,        0,          0L), // T2
        new KeyRecipe(ENCHANTED_MYCELIUM, 20, ENCHANTED_RED_SAND, 20, NETHER_STAR, 2,          0L), // T3
        new KeyRecipe(ENCHANTED_MYCELIUM, 60, ENCHANTED_RED_SAND, 60, NETHER_STAR, 2,          0L), // T4
        new KeyRecipe(ENCHANTED_MYCELIUM, 80, ENCHANTED_RED_SAND, 80, NETHER_STAR, 2, 2_328_000L), // T5
    };
    
    private static final Map<String, Integer> BASE_SALVAGE = new HashMap<>();
    static {
        for (String slot : new String[]{"HELMET","CHESTPLATE","LEGGINGS","BOOTS"}) {
            BASE_SALVAGE.put("AURORA_"  + slot, 100);
            BASE_SALVAGE.put("CRIMSON_" + slot, 100);
            BASE_SALVAGE.put("FERVOR_"  + slot, 150);
            BASE_SALVAGE.put("HOLLOW_"  + slot, 100);
            BASE_SALVAGE.put("TERROR_"  + slot, 200);
        }
    }

    public static int salvageEssence(String itemId, int stars) {
        int base = BASE_SALVAGE.getOrDefault(itemId.toUpperCase(), 0);
        double mult = switch (stars) {
            case  1 -> 1.5;
            case  2 -> 2.0;
            case  3 -> 3.0;
            case  4 -> 4.0;
            case  5 -> 5.0;
            case  6 -> 6.0;
            case  7 -> 7.0;
            case  8 -> 8.0;
            case  9 -> 9.0;
            case 10 -> 10.0;
            default -> 1.0;
        };
        return (int)(base * mult);
    }

    // ── Misc ─────────────────────────────────────────────────────────────────────
    public static String stripColor(String s) {
        return s == null ? "" : s.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    public static int countStars(String displayName) {
        if (displayName == null) return 0;
        int stars = 0;
        char activeColor = '6'; // default orange
        for (int i = 0; i < displayName.length(); i++) {
            char c = displayName.charAt(i);
            if (c == '§' && i + 1 < displayName.length()) {
                activeColor = displayName.charAt(++i);
            } else if (c == '✪') {
                stars += (activeColor == 'd' || activeColor == 'D') ? 2 : 1;
            }
        }
        return stars;
    }

    private KuudraDrops() {}
}
