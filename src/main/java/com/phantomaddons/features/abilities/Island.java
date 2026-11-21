package com.phantomaddons.features.abilities;

import java.util.Arrays;
import java.util.List;

public enum Island {
    PRIVATE_ISLAND("dynamic", "Private Island"),
    GARDEN("garden", "Garden"),
    HUB("hub", "Hub"),
    FARMING_ISLANDS("farming_1", "The Farming Islands"),
    PARK("foraging_1", "The Park"),
    GALATEA("foraging_2", "Galatea"),
    TORRHUS_CANYON("foraging_3", "Torrhus Canyon"),
    LOTUS_ATOLL("lotus_atoll", "Lotus Atoll"),
    SPIDERS_DEN("combat_1", "Spider's Den"),
    BLAZING_FORTRESS("combat_2", "Blazing Fortress"),
    THE_END("combat_3", "The End"),
    CRIMSON_ISLE("crimson_isle", "Crimson Isle"),
    GOLD_MINE("mining_1", "Gold Mine"),
    DEEP_CAVERNS("mining_2", "Deep Caverns"),
    DWARVEN_MINES("mining_3", "Dwarven Mines"),
    CRYSTAL_HOLLOWS("crystal_hollows", "Crystal Hollows"),
    BACKWATER_BAYOU("fishing_1", "Backwater Bayou"),
    JERRYS_WORKSHOP("winter", "Jerry's Workshop"),
    RIFT("rift", "The Rift"),
    DARK_AUCTION("dark_auction", "Dark Auction"),
    DUNGEON_HUB("dungeon_hub", "Dungeon Hub"),
    DUNGEON("dungeon", "Dungeons"),
    KUUDRA("kuudra", "Kuudra's Hollow"),
    MINESHAFT("mineshaft", "Glacite Mineshafts"),
    UNKNOWN("", "Unknown");

    private static final List<Island> SELECTABLE =
            Arrays.stream(values()).filter(i -> i != UNKNOWN).toList();

    public final String mode;
    public final String label;

    Island(String mode, String label) {
        this.mode = mode;
        this.label = label;
    }

    public static List<Island> selectable() { return SELECTABLE; }

    public static Island fromMode(String mode) {
        if (mode == null) return UNKNOWN;
        for (Island i : SELECTABLE) if (i.mode.equals(mode)) return i;
        return UNKNOWN;
    }
}
