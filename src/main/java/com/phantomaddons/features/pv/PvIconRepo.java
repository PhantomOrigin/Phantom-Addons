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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class PvIconRepo {

    private static final String BASE = "https://phantomorigin.github.io/phantomaddons-data/";
    private static final String[] FILES = { "items", "pets" };

    public record Item(String name, String itemId, int damage, String texture, int color, boolean glint, String model) {}
    public record Pet(String name, String skill, Map<Integer, String> textures) {}

    private static volatile Map<String, Item> items;
    private static volatile Map<String, Pet> pets;
    private static volatile boolean failed;
    private static CompletableFuture<Void> loading;

    private PvIconRepo() {}

    public static synchronized void load() {
        if (loading != null) return;
        loading = CompletableFuture.runAsync(PvIconRepo::loadNow, PvItems.EXECUTOR);
    }

    public static boolean ready()  { return items != null && pets != null; }
    public static boolean failed() { return failed; }

    public static Item item(String id) { Map<String, Item> m = items; return m != null ? m.get(id) : null; }
    public static Pet pet(String type) { Map<String, Pet> m = pets; return m != null ? m.get(type) : null; }

    private static void loadNow() {
        Path dir = PhantomPaths.resolve("pv_cache").resolve("icons");
        try {
            Files.createDirectories(dir);
            JsonObject local = readJson(dir.resolve("shas.json"));
            JsonObject remote = null;
            try {
                remote = JsonParser.parseString(get(BASE + "shas.json")).getAsJsonObject();
            } catch (IOException | RuntimeException e) {
                PhantomAddons.LOGGER.warn("[PV] icon repo unreachable, using cached copy if there is one: {}", e.getMessage());
            }
            if (remote != null) {
                for (String f : FILES) {
                    String want = PvJson.str(remote, null, "files", f);
                    String have = PvJson.str(local, null, "files", f);
                    Path file = dir.resolve(f + ".min.json");
                    if (want == null || (want.equals(have) && Files.exists(file))) continue;
                    String body = get(BASE + f + ".min.json");
                    if (!want.equals(sha1(body))) throw new IOException(f + ".min.json hash mismatch");
                    Files.writeString(file, body);
                }
                Files.writeString(dir.resolve("shas.json"), remote.toString());
            }
            items = parseItems(readJson(dir.resolve("items.min.json")));
            pets = parsePets(readJson(dir.resolve("pets.min.json")));
            if (items.isEmpty()) throw new IOException("no cached icon data");
            PhantomAddons.LOGGER.info("[PV] icon repo loaded: {} items, {} pets", items.size(), pets.size());
        } catch (IOException | RuntimeException e) {
            failed = true;
            items = null;
            pets = null;
            PhantomAddons.LOGGER.warn("[PV] icon repo unavailable, falling back to per-item downloads", e);
        }
    }

    private static Map<String, Item> parseItems(JsonObject root) {
        Map<String, Item> out = new HashMap<>();
        if (root == null) return out;
        for (var e : root.entrySet()) {
            JsonObject o = e.getValue().getAsJsonObject();
            out.put(e.getKey(), new Item(
                    PvJson.str(o, e.getKey(), "n"),
                    PvJson.str(o, "minecraft:barrier", "i"),
                    (int) PvJson.num(o, 0, "d"),
                    PvJson.str(o, null, "t"),
                    (int) PvJson.num(o, -1, "c"),
                    o.has("g"),
                    PvJson.str(o, null, "m")));
        }
        return out;
    }

    private static Map<String, Pet> parsePets(JsonObject root) {
        Map<String, Pet> out = new HashMap<>();
        if (root == null) return out;
        for (var e : root.entrySet()) {
            JsonObject o = e.getValue().getAsJsonObject();
            Map<Integer, String> tex = new HashMap<>();
            JsonObject t = PvJson.obj(o, "t");
            if (t != null) for (var te : t.entrySet()) tex.put(Integer.parseInt(te.getKey()), te.getValue().getAsString());
            out.put(e.getKey(), new Pet(PvJson.str(o, e.getKey(), "n"), PvJson.str(o, "", "s"), tex));
        }
        return out;
    }

    private static JsonObject readJson(Path file) {
        try {
            if (!Files.exists(file)) return null;
            JsonElement e = JsonParser.parseString(Files.readString(file));
            return e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static String get(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestProperty("User-Agent", "PhantomAddons-ProfileViewer");
        conn.setConnectTimeout(8_000);
        conn.setReadTimeout(30_000);
        if (conn.getResponseCode() != 200) throw new IOException("HTTP " + conn.getResponseCode() + " for " + url);
        try (InputStream is = conn.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String sha1(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
