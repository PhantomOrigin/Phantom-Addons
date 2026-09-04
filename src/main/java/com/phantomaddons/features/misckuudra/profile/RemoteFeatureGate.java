package com.phantomaddons.features.misckuudra.profile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.phantomaddons.PhantomAddons;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class RemoteFeatureGate {

    private static final String STATUS_URL =
            "https://kuudra-profile-worker-v2.phantomaddons.workers.dev/status";

    private static final boolean enabled = true;
    private static volatile boolean checked = true;

    private static volatile boolean manaDrainTrackingEnabled = true;

    private RemoteFeatureGate() {}

    public static boolean isEnabled() { return enabled; }
    public static boolean hasChecked() { return checked; }
    public static boolean isManaDrainTrackingEnabled() { return manaDrainTrackingEnabled; }

    public static void checkOnStartup() {
        CompletableFuture.runAsync(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(STATUS_URL).toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "PhantomAddons-RemoteFeatureGate");
                conn.setConnectTimeout(8_000);
                conn.setReadTimeout(10_000);

                String body;
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    body = br.lines().collect(Collectors.joining());
                }

                JsonObject root = JsonParser.parseString(body).getAsJsonObject();
                manaDrainTrackingEnabled = !root.has("manaDrainTrackingEnabled")
                        || root.get("manaDrainTrackingEnabled").getAsBoolean();
                PhantomAddons.LOGGER.info("[PhantomAddons] mana drain tracking: {}",
                        manaDrainTrackingEnabled ? "enabled" : "disabled");
            } catch (Exception e) {
                PhantomAddons.LOGGER.warn("[PhantomAddons] Could not check mana drain tracking status ({}) — leaving enabled",
                        e.getMessage());
            } finally {
                checked = true;
            }
        });
    }
}
