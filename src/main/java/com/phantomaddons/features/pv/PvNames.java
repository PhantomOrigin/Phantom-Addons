package com.phantomaddons.features.pv;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

final class PvNames {

    private static final Map<String, CompletableFuture<String>> NAMES = new ConcurrentHashMap<>();
    private static final Map<String, Long> FAILED_AT = new ConcurrentHashMap<>();
    private static final long RETRY_MS = 120_000L;

    private PvNames() {}

    static String name(String uuid) {
        CompletableFuture<String> f = NAMES.get(uuid);
        boolean retry = f != null && f.isCompletedExceptionally()
                && System.currentTimeMillis() - FAILED_AT.getOrDefault(uuid, 0L) > RETRY_MS;
        if (f == null || retry) {
            f = CompletableFuture.supplyAsync(() -> lookup(uuid), PvItems.EXECUTOR);
            f.whenComplete((r, e) -> { if (e != null) FAILED_AT.put(uuid, System.currentTimeMillis()); });
            NAMES.put(uuid, f);
        }
        return f.isDone() && !f.isCompletedExceptionally() ? f.join() : null;
    }

    private static boolean failed(String uuid) {
        CompletableFuture<String> f = NAMES.get(uuid);
        return f != null && f.isCompletedExceptionally();
    }

    private static String lookup(String uuid) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(
                    "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.replace("-", "")).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "PhantomAddons-ProfileViewer");
            conn.setConnectTimeout(6_000);
            conn.setReadTimeout(8_000);
            if (conn.getResponseCode() != 200) throw new IOException("HTTP " + conn.getResponseCode());
            try (InputStream in = conn.getInputStream()) {
                JsonObject o = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                return o.get("name").getAsString();
            }
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    static String[] coopTooltip(List<String> coop) {
        if (coop.isEmpty()) return new String[] { "§7Co-op members: §f0" };
        List<String> lines = new ArrayList<>();
        lines.add("§7Co-op members §8(" + coop.size() + ")");
        for (String id : coop) {
            String n = name(id);
            if (n != null) lines.add("§f" + n);
            else if (failed(id)) lines.add("§8" + id.substring(0, Math.min(8, id.length())) + "...");
            else lines.add("§8Loading...");
        }
        return lines.toArray(new String[0]);
    }
}
