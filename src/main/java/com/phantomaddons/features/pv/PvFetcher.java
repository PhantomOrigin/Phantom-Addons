package com.phantomaddons.features.pv;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.utils.PhantomApi;
import com.phantomaddons.utils.PhantomPaths;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public final class PvFetcher {


    static final long CACHE_TTL_MS = 10 * 60 * 1000L;
    private static final long CACHE_PURGE_MS = 24 * 60 * 60 * 1000L;

    private record Cached(long savedAt, JsonObject body) {
        boolean fresh() { return System.currentTimeMillis() - savedAt < CACHE_TTL_MS; }
    }

    private static final Map<String, Cached> PROFILES = new ConcurrentHashMap<>(); // uuid (no dashes) → response
    private static final Map<String, Cached> GARDENS = new ConcurrentHashMap<>();  // profile id → garden
    private static final Map<String, Cached> MUSEUMS = new ConcurrentHashMap<>();  // profile id:uuid → museum
    private static final Map<String, String> NAMES = new ConcurrentHashMap<>();    // lowercase name → uuid

    private static final int MEMORY_PLAYERS = 5;
    private static final Map<String, PvData> LOADED = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<>(8, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, PvData> e) { return size() > MEMORY_PLAYERS; }
            });

    public record Recent(String name, UUID uuid, long viewedAt) {}
    private static final List<Recent> RECENT = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static volatile boolean recentLoaded;
    private static volatile boolean diskLoaded;

    private PvFetcher() {}

    public static Path testFile() {
        return PhantomPaths.resolve("pv_test.json");
    }

    private static Path cacheDir() {
        return PhantomPaths.resolve("pv_cache").resolve("profiles");
    }

    public static CompletableFuture<PvData> fetch(String name, UUID uuid) {
        String known = uuid != null ? uuid.toString().replace("-", "")
                : name != null ? NAMES.get(name.toLowerCase(Locale.ROOT)) : null;
        if (known != null && !Files.exists(testFile())) {
            PvData mem = LOADED.get(known);
            Cached c = PROFILES.get(known);
            if (mem != null && c != null && c.fresh() && mem.fetchedAt == c.savedAt()) return CompletableFuture.completedFuture(mem);
        }
        Path test = testFile();
        if (Files.exists(test)) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    JsonObject root = JsonParser.parseString(Files.readString(test)).getAsJsonObject();
                    return PvData.fromHypixel(root, name, uuid);
                } catch (IOException | RuntimeException e) {
                    throw new RuntimeException("pv_test.json: " + e.getMessage(), e);
                }
            }, PvItems.EXECUTOR);
        }
        return CompletableFuture.supplyAsync(() -> {
            loadDiskIndex();
            String key = uuid != null ? uuid.toString().replace("-", "")
                    : name != null ? NAMES.get(name.toLowerCase(Locale.ROOT)) : null;
            if (key != null) {
                Cached c = cachedProfile(key);
                if (c != null && c.fresh()) {
                    PvData mem = LOADED.get(key);
                    if (mem != null && mem.fetchedAt == c.savedAt()) return mem;
                    return remember(key, withTime(PvData.parse(c.body()), c.savedAt()));
                }
            }
            String query = uuid != null
                    ? "uuid=" + uuid.toString().replace("-", "")
                    : "name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
            String nameParam = uuid != null && name != null
                    ? "&name=" + URLEncoder.encode(name, StandardCharsets.UTF_8) : "";
            try {
                JsonObject body = PhantomApi.authed(base -> get(base + "/pv?" + query + nameParam));
                body.remove("cached");
                long now = System.currentTimeMillis();
                PvData data = withTime(PvData.parse(body), now);
                String id = data.uuid.toString().replace("-", "");
                remember(id, data);
                PROFILES.put(id, new Cached(now, body));
                NAMES.put(data.name.toLowerCase(Locale.ROOT), id);
                save(id, now, body);
                return data;
            } catch (IOException e) {
                Cached stale = key != null ? cachedProfile(key) : null;
                if (stale != null) {
                    PvData mem = LOADED.get(key);
                    if (mem != null && mem.fetchedAt == stale.savedAt()) return mem;
                    return remember(key, withTime(PvData.parse(stale.body()), stale.savedAt()));
                }
                throw new RuntimeException(e.getMessage(), e);
            }
        }, PvItems.EXECUTOR);
    }

    public static CompletableFuture<PvData> peek(String name, UUID uuid) {
        if (Files.exists(testFile())) return CompletableFuture.completedFuture(null);
        String known = uuid != null ? uuid.toString().replace("-", "")
                : name != null ? NAMES.get(name.toLowerCase(Locale.ROOT)) : null;
        if (known != null) {
            PvData mem = LOADED.get(known);
            if (mem != null) return CompletableFuture.completedFuture(mem);
        }
        return CompletableFuture.supplyAsync(() -> {
            loadDiskIndex();
            String key = uuid != null ? uuid.toString().replace("-", "")
                    : name != null ? NAMES.get(name.toLowerCase(Locale.ROOT)) : null;
            if (key == null) return null;
            PvData mem = LOADED.get(key);
            if (mem != null) return mem;
            Cached c = cachedProfile(key);
            return c != null ? remember(key, withTime(PvData.parse(c.body()), c.savedAt())) : null;
        }, PvItems.EXECUTOR);
    }

    private static PvData remember(String uuid, PvData d) {
        return LOADED.merge(uuid, d, (old, nw) -> nw.fetchedAt >= old.fetchedAt ? nw : old);
    }

    // ── Recently viewed ───────────────────────────────────────────────────────

    private static Path recentFile() {
        return PhantomPaths.resolve("pv_cache").resolve("recent.json");
    }

    public static List<Recent> recent() {
        loadRecent();
        return List.copyOf(RECENT);
    }

    public static void noteViewed(PvData d) {
        if (d == null || d.testData) return;
        loadRecent();
        RECENT.removeIf(r -> r.uuid().equals(d.uuid));
        RECENT.add(0, new Recent(d.name, d.uuid, System.currentTimeMillis()));
        while (RECENT.size() > MEMORY_PLAYERS) RECENT.remove(RECENT.size() - 1);
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (Recent r : RECENT) {
            JsonObject o = new JsonObject();
            o.addProperty("name", r.name());
            o.addProperty("uuid", r.uuid().toString());
            o.addProperty("viewedAt", r.viewedAt());
            arr.add(o);
        }
        CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(recentFile().getParent());
                Files.writeString(recentFile(), arr.toString());
            } catch (IOException ignored) {}
        }, PvItems.EXECUTOR);
    }

    private static synchronized void loadRecent() {
        if (recentLoaded) return;
        recentLoaded = true;
        try {
            if (!Files.exists(recentFile())) return;
            for (var e : JsonParser.parseString(Files.readString(recentFile())).getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                RECENT.add(new Recent(o.get("name").getAsString(), UUID.fromString(o.get("uuid").getAsString()),
                        o.get("viewedAt").getAsLong()));
            }
        } catch (IOException | RuntimeException ignored) {}
    }

    private static PvData withTime(PvData d, long savedAt) {
        d.fetchedAt = savedAt;
        return d;
    }

    public static CompletableFuture<JsonObject> fetchGarden(String profileId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (Files.exists(testFile())) {
                    Path test = PhantomPaths.resolve("pv_test_garden.json");
                    if (!Files.exists(test)) throw new IOException("save a garden response as pv_test_garden.json to test this");
                    JsonObject root = JsonParser.parseString(Files.readString(test)).getAsJsonObject();
                    return root.has("garden") ? root.getAsJsonObject("garden") : root;
                }
                Cached c = GARDENS.get(profileId);
                if (c != null && c.fresh()) return c.body();
                JsonObject root = PhantomApi.authed(base ->
                        get(base + "/pv/garden?profile=" + URLEncoder.encode(profileId, StandardCharsets.UTF_8)));
                JsonObject garden = root.has("garden") ? root.getAsJsonObject("garden") : root;
                GARDENS.put(profileId, new Cached(System.currentTimeMillis(), garden));
                return garden;
            } catch (IOException e) {
                Cached stale = GARDENS.get(profileId);
                if (stale != null) return stale.body();
                throw new RuntimeException(e.getMessage(), e);
            }
        }, PvItems.EXECUTOR);
    }

    public static CompletableFuture<JsonObject> fetchMuseum(String profileId, UUID uuid) {
        String member = uuid.toString().replace("-", "");
        String key = profileId + ":" + member;
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (Files.exists(testFile())) {
                    Path test = PhantomPaths.resolve("pv_test_museum.json");
                    if (!Files.exists(test)) throw new IOException("save a museum response as pv_test_museum.json to test this");
                    JsonObject root = JsonParser.parseString(Files.readString(test)).getAsJsonObject();
                    JsonObject members = PvJson.obj(root, "members");
                    if (members == null) return root.has("museum") ? root.getAsJsonObject("museum") : root;
                    if (members.has(member)) return members.getAsJsonObject(member);
                    return members.entrySet().iterator().next().getValue().getAsJsonObject();
                }
                Cached c = MUSEUMS.get(key);
                if (c != null && c.fresh()) return c.body();
                JsonObject root = PhantomApi.authed(base ->
                        get(base + "/pv/museum?profile=" + URLEncoder.encode(profileId, StandardCharsets.UTF_8) + "&uuid=" + member));
                JsonObject museum = root.has("museum") ? root.getAsJsonObject("museum") : root;
                MUSEUMS.put(key, new Cached(System.currentTimeMillis(), museum));
                return museum;
            } catch (IOException e) {
                Cached stale = MUSEUMS.get(key);
                if (stale != null) return stale.body();
                throw new RuntimeException(e.getMessage(), e);
            }
        }, PvItems.EXECUTOR);
    }

    // ── Disk cache ────────────────────────────────────────────────────────────

    private static Cached cachedProfile(String uuid) {
        Cached c = PROFILES.get(uuid);
        if (c != null) return c;
        Path file = cacheDir().resolve(uuid + ".json");
        try {
            if (!Files.exists(file)) return null;
            JsonObject wrapper = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            c = new Cached(wrapper.get("savedAt").getAsLong(), wrapper.getAsJsonObject("data"));
            PROFILES.put(uuid, c);
            return c;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static void save(String uuid, long savedAt, JsonObject body) {
        try {
            Path dir = cacheDir();
            Files.createDirectories(dir);
            JsonObject wrapper = new JsonObject();
            wrapper.addProperty("savedAt", savedAt);
            wrapper.add("data", body);
            Files.writeString(dir.resolve(uuid + ".json"), wrapper.toString());
        } catch (IOException ignored) {}
    }

    private static synchronized void loadDiskIndex() {
        if (diskLoaded) return;
        diskLoaded = true;
        Path dir = cacheDir();
        if (!Files.isDirectory(dir)) return;
        long now = System.currentTimeMillis();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                try {
                    if (now - Files.getLastModifiedTime(f).toMillis() > CACHE_PURGE_MS) { Files.deleteIfExists(f); continue; }
                    JsonObject wrapper = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                    JsonObject data = wrapper.getAsJsonObject("data");
                    String id = data.get("uuid").getAsString();
                    NAMES.put(data.get("name").getAsString().toLowerCase(Locale.ROOT), id);
                } catch (IOException | RuntimeException ignored) {}
            }
        } catch (IOException ignored) {}
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────

    private static JsonObject get(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "PhantomAddons-ProfileViewer");
        conn.setRequestProperty("Accept-Encoding", "gzip");
        PhantomApi.authorize(conn);
        conn.setConnectTimeout(8_000);
        conn.setReadTimeout(30_000);
        int code = conn.getResponseCode();
        InputStream raw = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (raw == null) throw new IOException("HTTP " + code);
        String body;
        try (InputStream is = "gzip".equalsIgnoreCase(conn.getContentEncoding()) ? new GZIPInputStream(raw) : raw) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("HTTP " + code + ": unreadable response");
        }
        if (json.has("error") || code >= 400) {
            throw new PhantomApi.HttpException(code,
                    json.has("error") ? json.get("error").getAsString() : "HTTP " + code,
                    json.has("code") ? json.get("code").getAsString() : null);
        }
        return json;
    }
}
