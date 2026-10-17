package com.phantomaddons.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.phantomaddons.PhantomAddons;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


public final class PhantomAuth {

    private static final Object LOCK = new Object();

    private static volatile String token;
    private static volatile long expiresAt;
    private static volatile boolean disabled;
    private static volatile long lastAttempt;
    private static volatile int failures;

    private PhantomAuth() {}

    // ── State ────────────────────────────────────────────────────────────────

    public static String bearer() {
        String t = token;
        return t != null && expiresAt > System.currentTimeMillis() ? t : null;
    }

    public static boolean isDisabled() {
        return disabled;
    }

    public static void markDisabled() {
        disabled = true;
        forget();
    }

    public static void invalidate() {
        forget();
    }

    private static void forget() {
        token = null;
        expiresAt = 0;
    }

    private static long cooldownMs() {
        return switch (failures) {
            case 0 -> 0;
            case 1 -> 5_000;
            case 2 -> 15_000;
            default -> 60_000;
        };
    }

    // ── Sign in ──────────────────────────────────────────────────────────────

    public static boolean ensureLogin() {
        synchronized (LOCK) {
            if (disabled) return false;
            return bearer() != null || login();
        }
    }

    public static boolean relogin() {
        synchronized (LOCK) {
            return !disabled && login();
        }
    }

    private static boolean login() {
        long now = System.currentTimeMillis();
        if (lastAttempt != 0 && now - lastAttempt < cooldownMs()) return false;
        lastAttempt = now;

        Minecraft mc = Minecraft.getInstance();
        var user = mc.getUser();
        if (user == null || user.getName() == null || user.getAccessToken() == null) {
            failures++;
            return false;
        }

        String serverId = UUID.randomUUID().toString();
        try {
            mc.services().sessionService().joinServer(user.getProfileId(), user.getAccessToken(), serverId);
        } catch (Exception e) {
            PhantomAddons.LOGGER.warn("[PhantomAddons] Mojang join handshake failed: {}", e.toString());
            failures++;
            return false;
        }

        JsonObject body = new JsonObject();
        body.addProperty("username", user.getName());
        body.addProperty("serverId", serverId);
        try {
            JsonObject response = PhantomApi.withFallback(base -> postJson(base + "/auth/session", body.toString()));
            token = response.get("token").getAsString();
            expiresAt = response.get("expiresAt").getAsLong();
            failures = 0;
            PhantomAddons.LOGGER.info("[PhantomAddons] Signed in with your Minecraft account");
            return true;
        } catch (PhantomApi.HttpException e) {
            if ("session_disabled".equals(e.code) || "banned".equals(e.code)) disabled = true;
            failures++;
            PhantomAddons.LOGGER.warn("[PhantomAddons] Sign-in refused ({}): {}", e.code, e.getMessage());
            if ("not_joined".equals(e.code)) diagnoseJoin(mc, user.getName(), serverId);
            return false;
        } catch (IOException | RuntimeException e) {
            failures++;
            PhantomAddons.LOGGER.warn("[PhantomAddons] Sign-in failed: {}", e.toString());
            return false;
        }
    }

    private static void diagnoseJoin(Minecraft mc, String username, String serverId) {
        try {
            var result = mc.services().sessionService().hasJoinedServer(username, serverId, null);
            PhantomAddons.LOGGER.warn("[PhantomAddons] Mojang, asked from this PC, says the join {}",
                    result != null ? "DID register (so the server-side check is what fails)" : "did NOT register");
        } catch (Exception e) {
            PhantomAddons.LOGGER.warn("[PhantomAddons] Couldn't ask Mojang from this PC: {}", e.toString());
        }
    }

    private static JsonObject postJson(String url, String json) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", "PhantomAddons-Auth");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(8_000);
        conn.setReadTimeout(15_000);
        conn.setDoOutput(true);
        conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String text = is != null ? new String(is.readAllBytes(), StandardCharsets.UTF_8) : "";
        JsonObject parsed;
        try {
            parsed = JsonParser.parseString(text).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new PhantomApi.HttpException(code, "HTTP " + code + ": unreadable response", null);
        }
        if (code >= 400 || parsed.has("error")) {
            String message = parsed.has("error") ? parsed.get("error").getAsString() : "HTTP " + code;
            if (parsed.has("detail")) message += " " + parsed.get("detail");
            throw new PhantomApi.HttpException(code, message, parsed.has("code") ? parsed.get("code").getAsString() : null);
        }
        return parsed;
    }
}
