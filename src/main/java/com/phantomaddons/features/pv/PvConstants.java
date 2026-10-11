package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.phantomaddons.PhantomAddons;
import com.phantomaddons.utils.PhantomPaths;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PvConstants {

    private static final String NEU = "https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/constants/";
    private static final String COLLECTIONS = "https://api.hypixel.net/v2/resources/skyblock/collections";
    private static final String ITEMS = "https://api.hypixel.net/v2/resources/skyblock/items";
    private static final String BAZAAR = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final String LOWEST_BIN = "https://lb.tricked.pro/lowestbins";
    private static final long PRICE_MAX_AGE_MS = 30L * 60 * 1000;
    private static final long MAX_AGE_MS = 24L * 60 * 60 * 1000;

    private static final Map<String, CompletableFuture<JsonObject>> CACHE = new ConcurrentHashMap<>();

    private PvConstants() {}

    public static JsonObject leveling()        { return get("leveling", NEU + "leveling.json"); }
    public static JsonObject hotmLayout()      { return get("hotmlayout", NEU + "hotmlayout.json"); }
    public static JsonObject hotfLayout()      { return get("hotflayout", NEU + "hotflayout.json"); }
    public static JsonObject attributeShards() { return get("attribute_shards", NEU + "attribute_shards.json"); }
    public static JsonObject bestiary()        { return get("bestiary", NEU + "bestiary.json"); }
    public static JsonObject garden()          { return get("garden", NEU + "garden.json"); }
    public static JsonObject misc()            { return get("misc", NEU + "misc.json"); }
    public static JsonObject collections()     { return get("collections", COLLECTIONS); }
    public static JsonObject museum()          { return get("museum", NEU + "museum.json"); }
    public static JsonObject lowestBins()      { return get("lowestbin", LOWEST_BIN, null, PRICE_MAX_AGE_MS); }
    public static JsonObject bazaar()          { return get("bazaar", BAZAAR, PvConstants::bazaarBuyPrices, PRICE_MAX_AGE_MS); }

    public static double price(String id) {
        JsonObject bin = lowestBins(), bz = bazaar();
        if (bin != null && bin.has(id) && bin.get(id).isJsonPrimitive()) return bin.get(id).getAsDouble();
        if (bz != null && bz.has(id) && bz.get(id).isJsonPrimitive()) return bz.get(id).getAsDouble();
        return -1;
    }

    public static boolean pricesSettled() {
        lowestBins(); bazaar();
        CompletableFuture<JsonObject> a = CACHE.get("lowestbin"), b = CACHE.get("bazaar");
        return a != null && a.isDone() && b != null && b.isDone();
    }

    private static JsonObject bazaarBuyPrices(JsonObject root) {
        JsonObject out = new JsonObject();
        JsonObject products = PvJson.obj(root, "products");
        if (products != null) {
            for (var e : products.entrySet()) {
                double buy = PvJson.num(e.getValue(), -1, "quick_status", "buyPrice");
                if (buy > 0) out.addProperty(e.getKey(), Math.round(buy * 10) / 10.0);
            }
        }
        return out;
    }

    public static JsonObject accessories()     { return get("items_slim3", ITEMS, PvConstants::slimItems); }

    public static JsonObject gemSlots() {
        JsonObject root = accessories();
        return root != null ? PvJson.obj(root, "gems") : null;
    }

    private static final java.util.Set<String> NO_RECOMB_KNOWN = java.util.Set.of("RUNEBOOK", "BEASTSLAYERS_CODEX", "SAFETY_BADGE",
            "PANDORAS_BOX", "BOOK_OF_PROGRESSION", "VOTER_BADGE", "VOTER_BADGE_VIP", "VOTER_BADGE_ELITE", "VOTER_BADGE_SUPREME", "RIFT_PRISM");
    private static volatile JsonObject noRecombFor;
    private static volatile java.util.Set<String> noRecombCache = NO_RECOMB_KNOWN;

    public static java.util.Set<String> noRecombobulator() {
        JsonObject root = accessories();
        if (root == null) return NO_RECOMB_KNOWN;
        if (root == noRecombFor) return noRecombCache;
        java.util.Set<String> out = new java.util.HashSet<>(NO_RECOMB_KNOWN);
        JsonElement list = root.get("no_recomb");
        if (list != null && list.isJsonArray()) for (JsonElement e : list.getAsJsonArray()) out.add(e.getAsString());
        noRecombCache = out;
        noRecombFor = root;
        return out;
    }

    public static JsonObject essenceCosts()    { return get("essence_costs", NEU + "essencecosts.json"); }
    public static JsonObject reforgeStones()   { return get("reforge_stones", NEU + "reforgestones.json"); }

    public static boolean networthDataSettled() {
        if (!pricesSettled()) return false;
        accessories(); essenceCosts(); reforgeStones();
        for (String name : new String[] { "items_slim3", "essence_costs", "reforge_stones" }) {
            CompletableFuture<JsonObject> f = CACHE.get(name);
            if (f == null || !f.isDone()) return false;
        }
        return true;
    }

    private static JsonObject slimItems(JsonObject all) {
        com.google.gson.JsonArray out = new com.google.gson.JsonArray();
        JsonObject gems = new JsonObject();
        com.google.gson.JsonArray cosmetic = new com.google.gson.JsonArray();
        com.google.gson.JsonArray noRecomb = new com.google.gson.JsonArray();
        JsonElement items = all.get("items");
        if (items != null && items.isJsonArray()) {
            for (JsonElement e : items.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                if ("ACCESSORY".equals(PvJson.str(e, "", "category"))) {
                    JsonObject a = new JsonObject();
                    a.addProperty("id", PvJson.str(e, "", "id"));
                    a.addProperty("name", PvJson.str(e, "", "name"));
                    a.addProperty("tier", PvJson.str(e, "COMMON", "tier"));
                    out.add(a);
                }
                if ("COSMETIC".equals(PvJson.str(e, "", "category"))) cosmetic.add(PvJson.str(e, "", "id"));
                JsonElement canRecomb = e.getAsJsonObject().get("can_recombobulate");
                if (canRecomb != null && canRecomb.isJsonPrimitive() && !canRecomb.getAsBoolean()) noRecomb.add(PvJson.str(e, "", "id"));
                JsonElement slots = e.getAsJsonObject().get("gemstone_slots");
                if (slots == null || !slots.isJsonArray()) continue;
                com.google.gson.JsonArray defs = new com.google.gson.JsonArray();
                for (JsonElement s : slots.getAsJsonArray()) {
                    JsonObject def = new JsonObject();
                    def.addProperty("t", PvJson.str(s, "", "slot_type"));
                    com.google.gson.JsonArray costs = new com.google.gson.JsonArray();
                    JsonElement list = PvJson.path(s, "costs");
                    if (list != null && list.isJsonArray()) {
                        for (JsonElement c : list.getAsJsonArray()) {
                            if (!"ITEM".equals(PvJson.str(c, "", "type"))) continue;
                            com.google.gson.JsonArray pair = new com.google.gson.JsonArray();
                            pair.add(PvJson.str(c, "", "item_id"));
                            pair.add(PvJson.num(c, 1, "amount"));
                            costs.add(pair);
                        }
                    }
                    def.add("c", costs);
                    defs.add(def);
                }
                gems.add(PvJson.str(e, "", "id"), defs);
            }
        }
        JsonObject root = new JsonObject();
        root.add("items", out);
        root.add("gems", gems);
        root.add("cosmetic", cosmetic);
        root.add("no_recomb", noRecomb);
        return root;
    }

    public static void preload() {
        leveling(); hotmLayout(); hotfLayout(); attributeShards(); bestiary(); garden(); misc(); collections();
        museum(); accessories(); essenceCosts(); reforgeStones();
    }

    private static JsonObject get(String name, String url) {
        return get(name, url, null);
    }

    private static JsonObject get(String name, String url, java.util.function.UnaryOperator<JsonObject> shrink) {
        return get(name, url, shrink, MAX_AGE_MS);
    }

    private static JsonObject get(String name, String url, java.util.function.UnaryOperator<JsonObject> shrink, long maxAge) {
        CompletableFuture<JsonObject> f = CACHE.computeIfAbsent(name,
                k -> CompletableFuture.supplyAsync(() -> load(name, url, shrink, maxAge), PvItems.EXECUTOR));
        if (maxAge < MAX_AGE_MS && f.isDone() && LOADED_AT.getOrDefault(name, 0L) + maxAge < System.currentTimeMillis()) {
            LOADED_AT.put(name, System.currentTimeMillis());
            CompletableFuture.supplyAsync(() -> load(name, url, shrink, maxAge), PvItems.EXECUTOR)
                    .thenAccept(fresh -> CACHE.put(name, CompletableFuture.completedFuture(fresh)));
        }
        return f.isDone() && !f.isCompletedExceptionally() ? f.join() : null;
    }

    private static final Map<String, Long> LOADED_AT = new ConcurrentHashMap<>();

    private static JsonObject load(String name, String url, java.util.function.UnaryOperator<JsonObject> shrink, long maxAge) {
        LOADED_AT.putIfAbsent(name, System.currentTimeMillis());
        Path file = PhantomPaths.resolve("pv_cache").resolve(name + ".json");
        try {
            if (Files.exists(file) && System.currentTimeMillis() - Files.getLastModifiedTime(file).toMillis() < maxAge) {
                return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            }
        } catch (IOException | RuntimeException ignored) {}

        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "PhantomAddons-ProfileViewer");
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setConnectTimeout(8_000);
            conn.setReadTimeout(30_000);
            if (conn.getResponseCode() != 200) throw new IOException("HTTP " + conn.getResponseCode());
            String body;
            InputStream raw = conn.getInputStream();
            try (InputStream is = "gzip".equalsIgnoreCase(conn.getContentEncoding()) ? new java.util.zip.GZIPInputStream(raw) : raw) {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (shrink != null) {
                json = shrink.apply(json);
                body = json.toString();
            }
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, body);
            } catch (IOException ignored) {}
            return json;
        } catch (IOException | RuntimeException e) {
            PhantomAddons.LOGGER.warn("[PV] couldn't download {} — falling back to cached copy", name, e);
            try {
                if (Files.exists(file)) return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            } catch (IOException | RuntimeException ignored) {}
            throw new RuntimeException("Couldn't load " + name);
        }
    }

    static long[] table(JsonObject root, String key) {
        if (root == null) return new long[0];
        JsonElement e = root.get(key);
        if (e == null || !e.isJsonArray()) return new long[0];
        long[] out = new long[e.getAsJsonArray().size()];
        for (int i = 0; i < out.length; i++) out[i] = e.getAsJsonArray().get(i).getAsLong();
        return out;
    }
}
