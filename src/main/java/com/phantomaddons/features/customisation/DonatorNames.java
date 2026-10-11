package com.phantomaddons.features.customisation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.phantomaddons.PhantomAddons;
import com.phantomaddons.utils.PhantomPaths;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class DonatorNames {

    private static final String URL = "https://raw.githubusercontent.com/PhantomOrigin/Phantom-Addons/main/donators.json";
    private static final Path CACHE = PhantomPaths.resolve("phantomaddons_donators.json");
    private static final long MAX_AGE_MS = 12L * 60 * 60 * 1000;

    private static volatile Map<String, StyledText> styles = Map.of();

    private DonatorNames() {}

    public static void init() {
        boolean fresh = false;
        try {
            if (Files.exists(CACHE)) {
                styles = parse(Files.readString(CACHE, StandardCharsets.UTF_8));
                fresh = System.currentTimeMillis() - Files.getLastModifiedTime(CACHE).toMillis() < MAX_AGE_MS;
            }
        } catch (Exception ignored) {}
        if (!fresh) CompletableFuture.runAsync(DonatorNames::refresh);
    }

    private static void refresh() {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(URL).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "PhantomAddons-Donators");
            conn.setConnectTimeout(8_000);
            conn.setReadTimeout(10_000);
            if (conn.getResponseCode() != 200) throw new java.io.IOException("HTTP " + conn.getResponseCode());
            String body;
            try (var in = conn.getInputStream()) { body = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
            styles = parse(body);
            Files.writeString(CACHE, body, StandardCharsets.UTF_8);
        } catch (Exception e) {
            PhantomAddons.LOGGER.warn("[PhantomAddons] Couldn't refresh donator names ({}), using the saved list", e.getMessage());
        }
    }

    private static Map<String, StyledText> parse(String json) {
        Map<String, StyledText> out = new HashMap<>();
        JsonElement root = JsonParser.parseString(json);
        JsonElement list = root.isJsonObject() ? root.getAsJsonObject().get("donators") : root;
        if (list == null || !list.isJsonArray()) return out;
        for (JsonElement e : list.getAsJsonArray()) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            String name = o.has("name") && o.get("name").isJsonPrimitive() ? o.get("name").getAsString().trim() : "";
            StyledText t = isUsername(name) ? StyledText.fromJson(o) : null;
            if (t != null) out.put(name.toLowerCase(), t);
        }
        return out;
    }

    private static boolean isUsername(String s) {
        if (s.isEmpty() || s.length() > 16) return false;
        for (int i = 0; i < s.length(); i++) if (!isNameChar(s.charAt(i))) return false;
        return true;
    }

    private static boolean isNameChar(int c) {
        return c == '_' || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    public static float centerShift(net.minecraft.client.gui.Font font, FormattedCharSequence original) {
        if (original == null || styles.isEmpty()) return 0f;
        FormattedCharSequence applied = apply(original, 0f);
        if (applied == original) return 0f;
        return (font.width(original) - font.width(applied)) / 2f;
    }

    private static final ThreadLocal<StringBuilder> TOKEN = ThreadLocal.withInitial(StringBuilder::new);

    public static FormattedCharSequence apply(FormattedCharSequence in, float y) {
        Map<String, StyledText> map = styles;
        if (in == null || map.isEmpty()) return in;
        try {
            StringBuilder token = TOKEN.get();
            token.setLength(0);
            boolean[] found = { false };
            in.accept((pos, style, cp) -> {
                if (isNameChar(cp)) { if (token.length() <= 16) token.appendCodePoint(cp); }
                else { if (map.containsKey(token.toString().toLowerCase())) found[0] = true; token.setLength(0); }
                return !found[0];
            });
            if (!found[0] && !map.containsKey(token.toString().toLowerCase())) return in;

            List<Style> styleOf = new ArrayList<>();
            StringBuilder text = new StringBuilder();
            in.accept((pos, style, cp) -> { styleOf.add(style); text.appendCodePoint(cp); return true; });

            int[] cps = text.codePoints().toArray();
            StringBuilder outText = new StringBuilder();
            List<Style> outStyle = new ArrayList<>();
            double seconds = System.nanoTime() / 1e9;
            for (int i = 0; i < cps.length; ) {
                if (!isNameChar(cps[i])) { outText.appendCodePoint(cps[i]); outStyle.add(styleOf.get(i)); i++; continue; }
                int j = i;
                while (j < cps.length && isNameChar(cps[j])) j++;
                String word = new String(cps, i, j - i);
                StyledText t = j - i <= 16 ? map.get(word.toLowerCase()) : null;
                if (t == null) {
                    for (int k = i; k < j; k++) { outText.appendCodePoint(cps[k]); outStyle.add(styleOf.get(k)); }
                } else {
                    t.emit(word, styleOf.get(i), y, seconds, (cp, s) -> { outText.appendCodePoint(cp); outStyle.add(s); });
                }
                i = j;
            }
            int[] outCps = outText.codePoints().toArray();
            return sink -> {
                for (int i = 0; i < outCps.length; i++) if (!sink.accept(i, outStyle.get(i), outCps[i])) return false;
                return true;
            };
        } catch (Throwable t) {
            return in;
        }
    }
}
