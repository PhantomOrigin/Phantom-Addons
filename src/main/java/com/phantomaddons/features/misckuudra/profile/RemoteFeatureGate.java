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

/**
 * Formerly a remote kill switch for the Hypixel-API-key-backed features (Profile Viewer,
 * Auto Kick), driven by a KV flag on the worker ({@code feature:apiKeyFeaturesEnabled} —
 * see kuudra-profile-worker-v2/src/index.js). Those features are now permanently enabled
 * client-side and no longer consult that flag — {@link #isEnabled()} always returns true.
 * <p>
 * The worker's {@code /status} endpoint and its {@code feature:apiKeyFeaturesEnabled} KV
 * flag are left in place (not removed) so older mod versions that still check it keep
 * working. This class still hits {@code /status} on startup purely for the separate
 * {@link #isManaDrainTrackingEnabled()} flag it also returns.
 */
public final class RemoteFeatureGate {

    private static final String STATUS_URL =
            "https://kuudra-profile-worker-v2.phantomaddons.workers.dev/status";

    private static final boolean enabled = true;
    private static volatile boolean checked = true;

    // Separate concern from the API-key gate above: whether mana-drain tracking/display
    // in the Rend Tracker report should still run. Endstone Sword (the mana-drain source)
    // is due to be nerfed out of relevance, at which point this can be flipped off from
    // the worker without shipping a new build. Unlike the API-key gate, this fails OPEN
    // (defaults true) — it's an accuracy toggle, not a security/legal one, so a failed
    // check shouldn't silently break an otherwise-working display.
    private static volatile boolean manaDrainTrackingEnabled = true;

    private RemoteFeatureGate() {}

    public static boolean isEnabled() { return enabled; }
    /** Whether the startup check has completed (success or failure) — for UI "checking..." states. */
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
