package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

final class PvSacks {

    private static final String URL = "https://phantomorigin.github.io/phantomaddons-data/sacks.min.json";
    private static final long MAX_AGE_MS = 24L * 60 * 60 * 1000;
    private static final long RETRY_MS = 60_000L;

    private static volatile CompletableFuture<Map<String, List<String>>> future;
    private static volatile long failedAt;

    private PvSacks() {}

    static Map<String, List<String>> definitions() {
        CompletableFuture<Map<String, List<String>>> f = future;
        if (f == null || (f.isCompletedExceptionally() && System.currentTimeMillis() - failedAt > RETRY_MS)) {
            f = CompletableFuture.supplyAsync(PvSacks::load, PvItems.EXECUTOR);
            f.whenComplete((r, e) -> { if (e != null) failedAt = System.currentTimeMillis(); });
            future = f;
        }
        return f.isDone() && !f.isCompletedExceptionally() ? f.join() : null;
    }

    static boolean failed() {
        CompletableFuture<Map<String, List<String>>> f = future;
        return f != null && f.isCompletedExceptionally();
    }

    private static Map<String, List<String>> load() {
        Path file = PhantomPaths.resolve("pv_cache").resolve("sacks.min.json");
        try {
            if (Files.exists(file) && System.currentTimeMillis() - Files.getLastModifiedTime(file).toMillis() < MAX_AGE_MS) {
                return parse(Files.readString(file, StandardCharsets.UTF_8));
            }
        } catch (IOException | RuntimeException ignored) {}
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(URL).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "PhantomAddons-ProfileViewer");
            conn.setConnectTimeout(8_000);
            conn.setReadTimeout(15_000);
            if (conn.getResponseCode() != 200) throw new IOException("HTTP " + conn.getResponseCode());
            String body;
            try (InputStream in = conn.getInputStream()) { body = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
            Map<String, List<String>> parsed = parse(body);
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, body, StandardCharsets.UTF_8);
            } catch (IOException ignored) {}
            return parsed;
        } catch (IOException | RuntimeException e) {
            PhantomAddons.LOGGER.warn("[PV] couldn't download the sack list, trying the saved copy: {}", e.toString());
            try {
                if (Files.exists(file)) return parse(Files.readString(file, StandardCharsets.UTF_8));
            } catch (IOException | RuntimeException ignored) {}
            throw new RuntimeException("Could not load the sack list");
        }
    }

    private static Map<String, List<String>> parse(String json) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : JsonParser.parseString(json).getAsJsonObject().entrySet()) {
            if (!e.getValue().isJsonArray()) continue;
            List<String> items = new ArrayList<>();
            for (JsonElement i : e.getValue().getAsJsonArray()) items.add(i.getAsString());
            out.put(e.getKey(), items);
        }
        if (out.isEmpty()) throw new IllegalStateException("no sacks in the list");
        return out;
    }
}
