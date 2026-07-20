package com.phantomaddons.features.misckuudra.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.phantomaddons.PhantomAddons;
import com.phantomaddons.features.misckuudra.profile.KuudraProfileData.ItemInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class KuudraProfileFetcher {

    private static final String WORKER_BASE_URL = "https://kuudra-profile-worker-v2.phantomaddons.workers.dev/profile";
    private static final long CACHE_MS = 5 * 60 * 1000L;

    private static final Map<String, CacheEntry> CACHE = new HashMap<>();

    private record CacheEntry(KuudraProfileData data, long timestamp) {}

    private KuudraProfileFetcher() {}

    public static void fetchAsync(String playerName, Consumer<KuudraProfileData> callback) {
        String key = playerName.toLowerCase();
        CacheEntry cached = CACHE.get(key);
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < CACHE_MS) {
            callback.accept(cached.data());
            return;
        }

        CompletableFuture.runAsync(() -> {
            KuudraProfileData data = new KuudraProfileData(playerName);
            try {
                String encoded = URLEncoder.encode(playerName, StandardCharsets.UTF_8);
                String json = get(WORKER_BASE_URL + "?name=" + encoded);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                if (root.has("error")) {
                    throw new IllegalStateException(root.get("error").getAsString());
                }
                parseInto(data, root);
                data.loaded = true;
            } catch (Exception e) {
                data.errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                PhantomAddons.LOGGER.warn("[KuudraProfile] Fetch failed for {}: {}", playerName, e.toString());
            }
            CACHE.put(key, new CacheEntry(data, System.currentTimeMillis()));
            callback.accept(data);
        });
    }

    private static void parseInto(KuudraProfileData data, JsonObject root) {
        data.nameTag = strOrNull(root, "nameTag");
        data.skyblockLevel = intOr(root, "skyblockLevel", -1);
        data.magicalPower = intOr(root, "magicalPower", -1);
        data.catacombsLevel = intOr(root, "catacombsLevel", -1);
        data.foragingLevel = intOr(root, "foragingLevel", -1);

        JsonObject tiers = objOrNull(root, "kuudraCompletions");
        if (tiers != null) {
            putTier(data, tiers, KuudraProfileData.KuudraTier.INFERNAL, "infernal");
            putTier(data, tiers, KuudraProfileData.KuudraTier.FIERY, "fiery");
            putTier(data, tiers, KuudraProfileData.KuudraTier.BURNING, "burning");
            putTier(data, tiers, KuudraProfileData.KuudraTier.HOT, "hot");
            putTier(data, tiers, KuudraProfileData.KuudraTier.BASIC, "basic");
        }

        data.dpsGoldenDragonPet = petOrNull(root, "dpsGoldenDragonPet");
        data.rendGoldenDragonPet = petOrNull(root, "rendGoldenDragonPet");

        if (root.has("armorSets") && root.get("armorSets").isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray("armorSets")) {
                JsonObject set = el.getAsJsonObject();
                String label = set.get("label").getAsString();
                List<ItemInfo> pieces = new ArrayList<>();
                for (JsonElement pieceEl : set.getAsJsonArray("pieces")) {
                    JsonObject piece = pieceEl.getAsJsonObject();
                    pieces.add(new ItemInfo(strOrNull(piece, "name"), stringList(piece, "lore")));
                }
                data.armorSets.add(new KuudraProfileData.ArmorSetResult(label, pieces));
            }
        }

        JsonObject weapons = objOrNull(root, "weapons");
        if (weapons != null) {
            putWeapon(data, weapons, KuudraProfileData.Weapon.REND_TERMINATOR, "rendTerminator");
            putWeapon(data, weapons, KuudraProfileData.Weapon.DUPLEX_TERMINATOR, "duplexTerminator");
            putWeapon(data, weapons, KuudraProfileData.Weapon.REND_BONEMERANG, "rendBonemerang");
            putWeapon(data, weapons, KuudraProfileData.Weapon.RAGNAROK, "ragnarok");
            putWeapon(data, weapons, KuudraProfileData.Weapon.ATOMSPLIT, "atomsplit");
        }
    }

    private static KuudraProfileData.GoldenDragonPet petOrNull(JsonObject root, String key) {
        JsonObject obj = objOrNull(root, key);
        if (obj == null) return null;
        return new KuudraProfileData.GoldenDragonPet(intOr(obj, "level", 0), strOrNull(obj, "item"));
    }

    private static void putWeapon(KuudraProfileData data, JsonObject weapons, KuudraProfileData.Weapon w, String key) {
        JsonObject obj = objOrNull(weapons, key);
        if (obj == null) return;
        data.ownedWeapons.put(w, new ItemInfo(strOrNull(obj, "displayName"), stringList(obj, "lore")));
    }

    private static void putTier(KuudraProfileData data, JsonObject tiers, KuudraProfileData.KuudraTier tier, String key) {
        if (tiers.has(key)) data.kuudraCompletions.put(tier, tiers.get(key).getAsInt());
    }

    private static List<String> stringList(JsonObject obj, String key) {
        List<String> list = new ArrayList<>();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray(key)) list.add(el.getAsString());
        }
        return list;
    }

    private static JsonObject objOrNull(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonObject() ? obj.getAsJsonObject(key) : null;
    }

    private static int intOr(JsonObject obj, String key, int fallback) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : fallback;
    }

    private static String strOrNull(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private static String get(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "PhantomAddons-KuudraProfile");
        conn.setConnectTimeout(8_000);
        conn.setReadTimeout(15_000);
        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String body = br.lines().collect(Collectors.joining());
            if (code >= 400) throw new IOException("HTTP " + code + ": " + body);
            return body;
        }
    }
}
