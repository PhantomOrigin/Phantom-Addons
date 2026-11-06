package com.phantomaddons.features.pv;

import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.Map;

final class PvStatus {

    private PvStatus() {}

    private static final Map<String, String> SKYBLOCK_MODES = Map.ofEntries(
            Map.entry("dynamic", "Private Island"),
            Map.entry("hub", "Hub"),
            Map.entry("garden", "Garden"),
            Map.entry("farming_1", "The Barn"),
            Map.entry("combat_1", "Spider's Den"),
            Map.entry("combat_2", "Blazing Fortress"),
            Map.entry("combat_3", "The End"),
            Map.entry("foraging_1", "The Park"),
            Map.entry("mining_1", "Gold Mine"),
            Map.entry("mining_2", "Deep Caverns"),
            Map.entry("mining_3", "Dwarven Mines"),
            Map.entry("crystal_hollows", "Crystal Hollows"),
            Map.entry("crimson_isle", "Crimson Isle"),
            Map.entry("kuudra", "Kuudra's Hollow"),
            Map.entry("dungeon_hub", "Dungeon Hub"),
            Map.entry("dungeon", "In a Dungeon"),
            Map.entry("rift", "The Rift"),
            Map.entry("winter", "Jerry's Workshop"),
            Map.entry("dark_auction", "Dark Auction"),
            Map.entry("instanced", "Instanced Area")
    );

    static String line(JsonObject status) {
        if (status == null) return "§8Status: —";
        boolean online = status.has("online") && status.get("online").getAsBoolean();
        if (online) {
            String where = location(status);
            return "§7Status: §aOnline" + (where.isEmpty() ? "" : " §7- §f" + where);
        }
        long last = longOf(status, "lastLogout");
        if (last > 0) return "§7Status: §cOffline §7- last seen " + PvScreen.ago(last);
        return "§7Status: §cOffline";
    }

    private static String location(JsonObject s) {
        String type = str(s, "gameType");
        String mode = str(s, "mode");
        if ("SKYBLOCK".equalsIgnoreCase(type)) {
            if (mode.isEmpty()) return "SkyBlock";
            String name = SKYBLOCK_MODES.get(mode.toLowerCase(Locale.ROOT));
            return name != null ? name : title(mode);
        }
        if (type.isEmpty()) return "";
        if ("LOBBY".equalsIgnoreCase(mode) || mode.isEmpty()) return title(type);
        return title(type) + " (" + title(mode) + ")";
    }

    private static String title(String id) {
        StringBuilder sb = new StringBuilder();
        for (String w : id.toLowerCase(Locale.ROOT).split("[_\\s]+")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static long longOf(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsLong() : 0L;
    }
}
