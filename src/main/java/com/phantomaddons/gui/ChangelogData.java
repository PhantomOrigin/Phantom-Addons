package com.phantomaddons.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.phantomaddons.PhantomAddons;

import com.phantomaddons.utils.PhantomPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChangelogData {

    public record Section(String name, List<String> items) {}

    public record Entry(String version, String date, String title, List<Section> sections) {}

    private static final String RESOURCE = "/assets/phantomaddons/changelog.json";
    private static final String URL = "https://raw.githubusercontent.com/PhantomOrigin/Phantom-Addons/main/changelog.json";
    private static final String CACHE_FILE = "phantomaddons_changelog.json";
    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private static List<Entry> bundled;
    private static volatile List<Entry> remote = List.of();

    private ChangelogData() {}

    public static List<Entry> all() {
        List<Entry> r = remote;
        return r.isEmpty() ? bundled() : r;
    }

    private static synchronized List<Entry> bundled() {
        if (bundled == null) {
            try (InputStream in = ChangelogData.class.getResourceAsStream(RESOURCE)) {
                bundled = in == null ? List.of() : parse(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (Exception e) {
                PhantomAddons.LOGGER.warn("[Changelog] couldn't read the bundled changelog: {}", e.toString());
                bundled = List.of();
            }
        }
        return bundled;
    }

    public static void loadSaved() {
        try {
            Path p = PhantomPaths.resolve(CACHE_FILE);
            if (!Files.exists(p)) return;
            List<Entry> entries = parse(new java.io.StringReader(Files.readString(p, StandardCharsets.UTF_8)));
            if (!entries.isEmpty()) remote = entries;
        } catch (Exception e) {
            PhantomAddons.LOGGER.warn("[Changelog] couldn't read the saved changelog: {}", e.toString());
        }
    }

    public static void fetch() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(URL).toURL().openConnection();
        conn.setRequestProperty("User-Agent", "PhantomAddons-Changelog");
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(6_000);
        if (conn.getResponseCode() != 200) throw new IOException("HTTP " + conn.getResponseCode());
        String body;
        try (InputStream in = conn.getInputStream()) { body = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
        List<Entry> entries = parse(new java.io.StringReader(body));
        if (entries.isEmpty()) throw new IOException("the file has no entries");
        remote = entries;
        Files.writeString(PhantomPaths.resolve(CACHE_FILE), body, StandardCharsets.UTF_8);
    }

    public static List<Entry> latest(int count) {
        List<Entry> all = all();
        return all.subList(0, Math.min(count, all.size()));
    }

    public static List<Entry> forVersion(String version) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : all()) if (compare(e.version(), version) == 0) out.add(e);
        return out.isEmpty() ? latest(1) : out;
    }

    public static List<Entry> unseen(String lastSeen, String current, int max) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : all()) {
            if (compare(e.version(), current) > 0) continue;       // written for a later version than this one
            if (lastSeen == null || lastSeen.isBlank()) {
                if (compare(e.version(), current) == 0) out.add(e);
            } else if (compare(e.version(), lastSeen) > 0) {
                out.add(e);
            }
            if (out.size() >= max) break;
        }
        return out;
    }

    public static List<Entry> newer(String current, String latest, int max) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : all()) {
            if (compare(e.version(), current) > 0 && compare(e.version(), latest) <= 0) out.add(e);
            if (out.size() >= max) break;
        }
        return out;
    }

    public static int compare(String a, String b) {
        List<Integer> x = numbers(a), y = numbers(b);
        for (int i = 0; i < Math.max(x.size(), y.size()); i++) {
            int p = i < x.size() ? x.get(i) : 0, q = i < y.size() ? y.get(i) : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }

    private static List<Integer> numbers(String version) {
        List<Integer> out = new ArrayList<>();
        Matcher m = NUMBER.matcher(version == null ? "" : version);
        while (m.find()) {
            try { out.add(Integer.parseInt(m.group())); } catch (NumberFormatException ignored) { out.add(0); }
        }
        return out;
    }

    private static List<Entry> parse(Reader reader) {
        List<Entry> out = new ArrayList<>();
        JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
        for (JsonElement el : array) {
            JsonObject o = el.getAsJsonObject();
            List<Section> sections = new ArrayList<>();
            if (o.has("sections")) {
                for (JsonElement se : o.getAsJsonArray("sections")) {
                    JsonObject so = se.getAsJsonObject();
                    List<String> items = new ArrayList<>();
                    if (so.has("items")) for (JsonElement i : so.getAsJsonArray("items")) items.add(i.getAsString());
                    sections.add(new Section(so.has("name") ? so.get("name").getAsString() : "", items));
                }
            }
            out.add(new Entry(o.get("version").getAsString(),
                    o.has("date") ? o.get("date").getAsString() : "",
                    o.has("title") ? o.get("title").getAsString() : "",
                    sections));
        }
        return out;
    }
}
