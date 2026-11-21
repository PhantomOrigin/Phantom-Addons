package com.phantomaddons.features.pv;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.phantomaddons.utils.PhantomPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

final class PvShardText {

    private PvShardText() {}

    private static final Map<String, List<String>> TEXT = new ConcurrentHashMap<>();
    private static final Map<String, String> SEARCH = new ConcurrentHashMap<>();
    private static final Set<String> HOVER_REQUESTED = ConcurrentHashMap.newKeySet();

    private static final ExecutorService POOL = Executors.newFixedThreadPool(6, r -> {
        Thread t = new Thread(r, "PhantomAddons-ShardText");
        t.setDaemon(true);
        return t;
    });

    private static final long DISK_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000;
    private static final long RETRY_MS = 30_000L;

    private static final AtomicInteger pending = new AtomicInteger();
    private static volatile boolean complete, diskTried, fetchedAny;
    private static volatile long lastAttempt;

    static List<String> description(String id) {
        List<String> text = TEXT.get(id);
        if (text != null) return text;
        if (HOVER_REQUESTED.add(id)) {
            POOL.execute(() -> {
                List<String> fetched = PvItems.fetchDescription(id);
                if (fetched != null) TEXT.put(id, fetched);
                else HOVER_REQUESTED.remove(id); // try again the next time it is hovered
            });
        }
        return null;
    }

    // ── Values at max level ───────────────────────────────────────────────────

    private record Range(double first, double last, String unit) {}

    private static volatile Map<String, List<Range>> levels;

    private static Map<String, List<Range>> levels() {
        Map<String, List<Range>> loaded = levels;
        if (loaded != null) return loaded;
        synchronized (PvShardText.class) {
            if (levels != null) return levels;
            Map<String, List<Range>> out = new HashMap<>();
            try (InputStream in = PvShardText.class.getResourceAsStream("/assets/phantomaddons/shard_levels.json")) {
                if (in != null) {
                    JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    for (var e : root.entrySet()) {
                        List<Range> list = new ArrayList<>();
                        for (var r : e.getValue().getAsJsonArray()) {
                            JsonArray a = r.getAsJsonArray();
                            list.add(new Range(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsString()));
                        }
                        out.put(e.getKey(), list);
                    }
                }
            } catch (IOException | RuntimeException ignored) {
                // no level data: descriptions just show their level 1 numbers
            }
            levels = out;
            return out;
        }
    }

    static List<String> withMaxLevel(String code, List<String> lines) {
        List<Range> ranges = levels().get(code);
        if (ranges == null || lines.isEmpty()) return lines;
        List<String> out = new ArrayList<>(lines);
        int line = 0, from = 0;
        boolean added = false;
        for (Range r : ranges) {
            Pattern p = Pattern.compile("(§[0-9a-f])(\\+?)(?<![\\d.])" + Pattern.quote(number(r.first())) + Pattern.quote(r.unit()) + "(?![\\d.])");
            for (int i = line; i < out.size(); i++) {
                Matcher m = p.matcher(out.get(i));
                if (!m.find(i == line ? from : 0)) continue;
                String bracket = " §8(" + m.group(1) + number(r.last()) + r.unit() + "§8)" + m.group(1);
                out.set(i, out.get(i).substring(0, m.end()) + bracket + out.get(i).substring(m.end()));
                line = i;
                from = m.end() + bracket.length();
                added = true;
                break;
            }
        }
        return added ? out : lines;
    }

    private static String number(double d) {
        return new BigDecimal(d).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    static String searchText(String id) {
        List<String> text = TEXT.get(id);
        if (text == null || text.isEmpty()) return "";
        return SEARCH.computeIfAbsent(id, k ->
                String.join(" ", text).replaceAll("§.", "").replaceAll("\\s+", " ").toLowerCase(Locale.ROOT));
    }

    static int loaded(Collection<String> ids) {
        int n = 0;
        for (String id : ids) if (TEXT.containsKey(id)) n++;
        return n;
    }

    static boolean allLoaded() { return complete; }

    static void loadAll(Collection<String> idCollection) {
        if (complete || pending.get() > 0) return;
        long now = System.currentTimeMillis();
        if (now - lastAttempt < RETRY_MS) return;
        lastAttempt = now;
        List<String> ids = List.copyOf(idCollection);
        pending.set(1);
        POOL.execute(() -> {
            try {
                if (!diskTried) { diskTried = true; loadDisk(); }
                List<String> missing = new ArrayList<>();
                for (String id : ids) if (!TEXT.containsKey(id)) missing.add(id);
                pending.addAndGet(missing.size());
                for (String id : missing) {
                    POOL.execute(() -> {
                        List<String> fetched = PvItems.fetchDescription(id);
                        if (fetched != null) { TEXT.put(id, fetched); fetchedAny = true; }
                        if (pending.decrementAndGet() == 0) finish(ids);
                    });
                }
            } finally {
                if (pending.decrementAndGet() == 0) finish(ids);
            }
        });
    }

    private static void finish(List<String> ids) {
        for (String id : ids) if (!TEXT.containsKey(id)) return;
        complete = true;
        if (fetchedAny) saveDisk();
    }

    // ── Disk copy ─────────────────────────────────────────────────────────────

    private static Path file() {
        return PhantomPaths.resolve("pv_cache").resolve("shard_descriptions.json");
    }

    private static void loadDisk() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            JsonObject wrapper = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            if (System.currentTimeMillis() - wrapper.get("savedAt").getAsLong() > DISK_MAX_AGE_MS) return;
            for (var e : wrapper.getAsJsonObject("data").entrySet()) {
                List<String> lines = new ArrayList<>();
                for (var l : e.getValue().getAsJsonArray()) lines.add(l.getAsString());
                TEXT.putIfAbsent(e.getKey(), lines);
            }
        } catch (IOException | RuntimeException ignored) {
        }
    }

    private static void saveDisk() {
        try {
            JsonObject data = new JsonObject();
            TEXT.forEach((id, lines) -> {
                JsonArray a = new JsonArray();
                for (String l : lines) a.add(l);
                data.add(id, a);
            });
            JsonObject wrapper = new JsonObject();
            wrapper.addProperty("savedAt", System.currentTimeMillis());
            wrapper.add("data", data);
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, wrapper.toString());
        } catch (IOException ignored) {
            // just means it is fetched again next time
        }
    }
}
