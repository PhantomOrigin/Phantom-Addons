package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PvData {

    public record Profile(String id, String cuteName, String gameMode, boolean selected, long bank,
                          List<String> coop, JsonObject member) {
        public String symbol() {
            return switch (gameMode) {
                case "ironman" -> "§7♲ ";
                case "island"  -> "§a☀ ";
                case "bingo"   -> "§6Ⓑ ";
                default        -> "";
            };
        }
    }

    public final UUID uuid;
    public final String name;
    public final List<Profile> profiles;
    public final boolean testData;
    public final boolean extras;
    public volatile long fetchedAt;
    final java.util.Map<String, java.util.concurrent.CompletableFuture<PvProfileView>> views =
            new java.util.concurrent.ConcurrentHashMap<>();

    private PvData(UUID uuid, String name, List<Profile> profiles, boolean testData, boolean extras) {
        this.uuid = uuid;
        this.name = name;
        this.profiles = profiles;
        this.testData = testData;
        this.extras = extras;
    }

    private static UUID dashed(String raw) {
        return UUID.fromString(raw.replace("-", "").replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }

    static PvData fromHypixel(JsonObject root, String name, UUID preferred) {
        List<JsonObject> raw = new ArrayList<>();
        if (root.has("profiles") && root.get("profiles").isJsonArray()) {
            for (JsonElement e : root.getAsJsonArray("profiles")) raw.add(e.getAsJsonObject());
        } else if (root.has("profile") && root.get("profile").isJsonObject()) {
            raw.add(root.getAsJsonObject("profile"));
        }
        if (raw.isEmpty()) throw new IllegalArgumentException("Test file has no 'profile' or 'profiles'");

        String key = preferred != null ? preferred.toString().replace("-", "") : null;
        JsonObject firstMembers = PvJson.obj(raw.get(0), "members");
        if ((key == null || firstMembers == null || !firstMembers.has(key)) && firstMembers != null && !firstMembers.isEmpty())
            key = firstMembers.keySet().iterator().next();
        if (key == null) throw new IllegalArgumentException("Test file has no members");

        List<Profile> profiles = new ArrayList<>();
        for (JsonObject p : raw) {
            JsonObject member = PvJson.obj(p, "members", key);
            if (member == null) continue;
            profiles.add(new Profile(
                    PvJson.str(p, "test", "profile_id"),
                    PvJson.str(p, "Test Profile", "cute_name"),
                    PvJson.str(p, "normal", "game_mode"),
                    !p.has("selected") || p.get("selected").getAsBoolean(),
                    (long) PvJson.num(p, -1, "banking", "balance"),
                    coopFromMembers(PvJson.obj(p, "members"), key),
                    member));
        }
        return new PvData(dashed(key), name != null ? name : key, profiles, true, true);
    }

    static PvData parse(JsonObject root) {
        String rawUuid = root.get("uuid").getAsString();
        UUID uuid = dashed(rawUuid);
        String name = PvJson.str(root, rawUuid, "name");

        List<Profile> profiles = new ArrayList<>();
        for (JsonElement e : root.getAsJsonArray("profiles")) {
            JsonObject p = e.getAsJsonObject();
            profiles.add(new Profile(
                    PvJson.str(p, "", "id"),
                    PvJson.str(p, "Unknown", "cuteName"),
                    PvJson.str(p, "normal", "gameMode"),
                    p.has("selected") && p.get("selected").getAsBoolean(),
                    (long) PvJson.num(p, -1, "bank"),
                    coopFromList(p),
                    PvJson.obj(p, "member")));
        }
        boolean extras = root.has("extras") && root.get("extras").isJsonPrimitive() && root.get("extras").getAsBoolean();
        return new PvData(uuid, name, profiles, false, extras);
    }

    private static List<String> coopFromMembers(JsonObject members, String self) {
        List<String> out = new ArrayList<>();
        if (members == null) return out;
        for (String id : members.keySet()) {
            if (id.equals(self)) continue;
            if (PvJson.obj(members, id, "profile", "deletion_notice") != null) continue;
            out.add(id);
        }
        return out;
    }

    private static List<String> coopFromList(JsonObject profile) {
        List<String> out = new ArrayList<>();
        JsonElement e = profile.get("coop");
        if (e != null && e.isJsonArray()) {
            for (JsonElement i : e.getAsJsonArray()) if (i.isJsonPrimitive()) out.add(i.getAsString());
        }
        return out;
    }

    public int selectedIndex() {
        for (int i = 0; i < profiles.size(); i++) if (profiles.get(i).selected()) return i;
        return 0;
    }
}
