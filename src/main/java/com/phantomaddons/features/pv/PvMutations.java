package com.phantomaddons.features.pv;

import com.google.gson.JsonArray;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class PvMutations {

    record Mutation(String id, String name, String rarity, boolean analyzable) {}

    private static final String URL = "https://phantomorigin.github.io/phantomaddons-data/mutations.min.json";
    private static final long MAX_AGE_MS = 24L * 60 * 60 * 1000;
    private static final long RETRY_MS = 60_000L;

    private static volatile CompletableFuture<List<Mutation>> future;
    private static volatile long failedAt;

    private PvMutations() {}

    static List<Mutation> list() {
        CompletableFuture<List<Mutation>> f = future;
        if (f == null || (f.isCompletedExceptionally() && System.currentTimeMillis() - failedAt > RETRY_MS)) {
            f = CompletableFuture.supplyAsync(PvMutations::load, PvItems.EXECUTOR);
            f.whenComplete((r, e) -> { if (e != null) failedAt = System.currentTimeMillis(); });
            future = f;
        }
        return f.isDone() && !f.isCompletedExceptionally() ? f.join() : null;
    }

    static boolean failed() {
        CompletableFuture<List<Mutation>> f = future;
        return f != null && f.isCompletedExceptionally();
    }

    private static List<Mutation> load() {
        Path file = PhantomPaths.resolve("pv_cache").resolve("mutations.min.json");
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
            List<Mutation> parsed = parse(body);
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, body, StandardCharsets.UTF_8);
            } catch (IOException ignored) {}
            return parsed;
        } catch (IOException | RuntimeException e) {
            PhantomAddons.LOGGER.warn("[PV] couldn't download the greenhouse crop list, trying the saved copy: {}", e.toString());
            try {
                if (Files.exists(file)) return parse(Files.readString(file, StandardCharsets.UTF_8));
            } catch (IOException | RuntimeException ignored) {}
            throw new RuntimeException("Could not load the greenhouse crop list");
        }
    }

    private static List<Mutation> parse(String json) {
        List<Mutation> out = new ArrayList<>();
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement e : array) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            if (!o.has("id") || !o.has("name")) continue;
            String rarity = o.has("rarity") ? o.get("rarity").getAsString() : "COMMON";
            boolean analyzable = !o.has("analyzable") || o.get("analyzable").getAsBoolean();
            out.add(new Mutation(o.get("id").getAsString(), o.get("name").getAsString(), rarity, analyzable));
        }
        if (out.isEmpty()) throw new IllegalStateException("no crops in the list");
        return out;
    }
}
