package com.kuudrahelper.features.misckuudra.profile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kuudrahelper.KuudraHelperMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Remote kill switch for the Hypixel-API-key-backed features (Profile Viewer, Auto
 * Kick) — replaces the old build-time {@code API_KEY_FEATURES_UNLOCKED} constant.
 * <p>
 * On startup this checks {@code GET /status} on the same worker that serves profile
 * data; that endpoint reads a KV flag ({@code feature:apiKeyFeaturesEnabled}) which can
 * be flipped at any time without redeploying the worker or shipping a new mod build —
 * see kuudra-profile-worker-v2/src/index.js. The worker itself also refuses {@code
 * /profile} requests while the flag is off, so this is defense in depth, not the only
 * gate: even a modified client couldn't use these features while disabled remotely.
 * <p>
 * Defaults to disabled until the check succeeds — if the request fails (offline, worker
 * down, etc.) the features simply stay off rather than failing open.
 */
public final class RemoteFeatureGate {

    private static final String STATUS_URL =
            "https://kuudra-profile-worker-v2.phantomaddons.workers.dev/status";

    private static volatile boolean enabled = false;
    private static volatile boolean checked = false;

    private RemoteFeatureGate() {}

    public static boolean isEnabled() { return enabled; }
    /** Whether the startup check has completed (success or failure) — for UI "checking..." states. */
    public static boolean hasChecked() { return checked; }

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
                enabled = root.has("apiKeyFeaturesEnabled") && root.get("apiKeyFeaturesEnabled").getAsBoolean();
                KuudraHelperMod.LOGGER.info("[PhantomAddons] API-key features: {}", enabled ? "enabled" : "disabled");
            } catch (Exception e) {
                enabled = false;
                KuudraHelperMod.LOGGER.warn("[PhantomAddons] Could not check API-key feature status ({}) — leaving disabled",
                        e.getMessage());
            } finally {
                checked = true;
            }
        });
    }
}
