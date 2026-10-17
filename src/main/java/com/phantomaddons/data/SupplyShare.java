package com.phantomaddons.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.phantomaddons.PhantomAddons;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.phase.KuudraPhaseTracker;
import com.phantomaddons.utils.PhantomApi;
import com.phantomaddons.utils.PhantomPaths;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SupplyShare {

    private static final int BATCH_SPAWNS = 20, BATCH_GRABS = 8, BATCH_CANCELS = 8;

    private static final int MAX_QUEUE = 1000;
    private static final long MIN_TICKS_BEFORE_RISE = 2;
    private static final long MIN_RISE_TICKS = 3;

    private static final long GATE_RECHECK_MS = 30 * 60_000L;
    private static final long GATE_RETRY_MS = 5 * 60_000L;
    private static final long UPLOAD_INTERVAL_MS = 60_000L;
    private static final long MORE_TO_SEND_MS = 15_000L;
    private static final long ERROR_BACKOFF_MS = 5 * 60_000L;

    private static final String QUEUE_FILE = "phantomaddons_share_queue.jsonl";
    private static final Gson GSON = new Gson();

    private static final java.util.Map<String, double[]> LIMITS = java.util.Map.ofEntries(
            java.util.Map.entry("averageCps", new double[] { 0, 60 }),
            java.util.Map.entry("pearls", new double[] { 0, 1000 }),
            java.util.Map.entry("pearlsAnyDirection", new double[] { 0, 1000 }),
            java.util.Map.entry("supplyPercentAtReach", new double[] { 0, 100 }),
            java.util.Map.entry("riseDistance", new double[] { -500, 500 }),
            java.util.Map.entry("riseTicks", new double[] { 0, 5000 }),
            java.util.Map.entry("riseVelocity", new double[] { -20, 20 }),
            java.util.Map.entry("grabVelocityX", new double[] { -20, 20 }),
            java.util.Map.entry("grabVelocityZ", new double[] { -20, 20 }),
            java.util.Map.entry("grabSpeed", new double[] { 0, 20 }),
            java.util.Map.entry("chainCubesAtFirstSight", new double[] { 0, 500 }),
            java.util.Map.entry("chainCubesMax", new double[] { 0, 500 }));

    private record Row(String type, String id, String grabId, JsonObject json) {}

    private static final List<Row> queue = new ArrayList<>();
    private static boolean queueLoaded;

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PhantomAddons-share");
        t.setDaemon(true);
        return t;
    });

    private static volatile boolean gateOpen;
    private static volatile boolean gateChecking;
    private static volatile long nextGateCheck;
    private static volatile boolean uploading;
    private static volatile long nextUpload;
    private static volatile boolean refusedThisSession;
    private static volatile long retryAfterSeconds;

    private SupplyShare() {}

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static boolean isActive() {
        return PhantomConfig.isShareSupplyData() && gateOpen && !refusedThisSession;
    }

    public static void onToggle(boolean on) {
        refusedThisSession = false;
        nextGateCheck = 0;
        if (!on) {
            gateOpen = false;
            clearQueue();
        }
    }

    // ── Every client tick ─────────────────────────────────────────────────────

    public static void tick(Minecraft client) {
        if (!PhantomConfig.isShareSupplyData() || client.player == null) return;
        long now = System.currentTimeMillis();
        KuudraPhaseTracker.Phase phase = KuudraPhaseTracker.getPhase();

        if (phase != KuudraPhaseTracker.Phase.NONE && !gateChecking && now >= nextGateCheck) {
            gateChecking = true;
            IO.execute(SupplyShare::checkGate);
        }

        if (gateOpen && !refusedThisSession && !uploading && now >= nextUpload
                && phase != KuudraPhaseTracker.Phase.SUPPLIES && hasRows()) {
            uploading = true;
            IO.execute(SupplyShare::upload);
        }
    }

    // ── Rows to share ─────────────────────────────────────────────────────────

    public static void addSpawn(SupplyDataStore.TentacleSpawn s) {
        if (!isActive() || s.clientId == null) return;
        if (s.ticksBeforeRise < MIN_TICKS_BEFORE_RISE || s.riseTicks < MIN_RISE_TICKS) return;
        JsonObject o = GSON.toJsonTree(s).getAsJsonObject();
        o.remove("recordedAt");
        o.remove("clientId");
        o.addProperty("id", s.clientId);
        add(new Row("spawn", s.clientId, null, o), o);
    }

    public static void addGrab(SupplyDataStore.GrabAttempt g) {
        if (!isActive() || g.clientId == null) return;
        JsonObject o = GSON.toJsonTree(g).getAsJsonObject();
        for (String k : new String[] { "recordedAt", "clientId", "tentacleId", "tentacleX", "tentacleY", "tentacleZ", "tentacleRiseVelocity" }) {
            o.remove(k);
        }
        o.addProperty("id", g.clientId);
        JsonObject t = new JsonObject();
        t.addProperty("x", g.tentacleX);
        t.addProperty("y", g.tentacleY);
        t.addProperty("z", g.tentacleZ);
        t.addProperty("riseVelocity", g.tentacleRiseVelocity);
        o.add("tentacle", t);
        add(new Row("grab", g.clientId, null, o), o);
    }

    public static void addCancel(SupplyDataStore.CancelAttempt c) {
        if (!isActive() || c.clientId == null || c.grabClientId == null) return;
        JsonObject o = GSON.toJsonTree(c).getAsJsonObject();
        for (String k : new String[] { "recordedAt", "clientId", "grabClientId", "grabAttemptId", "tentacleId" }) o.remove(k);
        o.addProperty("id", c.clientId);
        o.addProperty("grabId", c.grabClientId);
        o.addProperty("playerYaw", Math.round((((c.playerYaw % 360f) + 540f) % 360f - 180f) * 100f) / 100f);
        add(new Row("cancel", c.clientId, c.grabClientId, o), o);
    }

    private static void add(Row row, JsonObject json) {
        if (!plausible(json)) {
            PhantomAddons.LOGGER.info("[SupplyShare] left out a {} row with values outside what the server accepts", row.type);
            return;
        }
        synchronized (queue) {
            loadQueue();
            queue.add(row);
            while (queue.size() > MAX_QUEUE) queue.remove(0);
            appendToFile(row);
        }
    }

    private static boolean plausible(JsonObject o) {
        for (var e : o.entrySet()) {
            String k = e.getKey();
            if (!e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isNumber()) continue;
            double v = e.getValue().getAsDouble();
            if (!Double.isFinite(v)) return false;
            double[] limit = LIMITS.get(k);
            if (limit != null) {
                if (v < limit[0] || v > limit[1]) return false;
            } else if (k.endsWith("X") || k.endsWith("Z") || k.equals("x") || k.equals("z")) {
                if (v < -400 || v > 400) return false;
            } else if (k.endsWith("Y") || k.equals("y")) {
                if (v < -64 || v > 400) return false;
            } else if (Math.abs(v) > 10_000_000) {
                return false;
            }
        }
        JsonObject t = o.has("tentacle") ? o.getAsJsonObject("tentacle") : null;
        return t == null || plausible(t);
    }

    private static boolean hasRows() {
        synchronized (queue) {
            loadQueue();
            return !queue.isEmpty();
        }
    }

    // ── The queue on disk ─────────────────────────────────────────────────────

    private static Path queuePath() { return PhantomPaths.resolve(QUEUE_FILE); }

    private static void loadQueue() {
        if (queueLoaded) return;
        queueLoaded = true;
        Path p = queuePath();
        if (!Files.exists(p)) return;
        try {
            for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                try {
                    JsonObject l = JsonParser.parseString(line).getAsJsonObject();
                    queue.add(new Row(l.get("t").getAsString(), l.get("i").getAsString(),
                            l.has("g") ? l.get("g").getAsString() : null, l.getAsJsonObject("row")));
                } catch (RuntimeException e) {
                    PhantomAddons.LOGGER.warn("[SupplyShare] skipped a bad line in the queue file");
                }
            }
        } catch (IOException e) {
            PhantomAddons.LOGGER.warn("[SupplyShare] couldn't read the queue file: {}", e.toString());
        }
    }

    private static String line(Row r) {
        JsonObject l = new JsonObject();
        l.addProperty("t", r.type);
        l.addProperty("i", r.id);
        if (r.grabId != null) l.addProperty("g", r.grabId);
        l.add("row", r.json);
        return GSON.toJson(l) + "\n";
    }

    private static void appendToFile(Row r) {
        try {
            Files.writeString(queuePath(), line(r), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            PhantomAddons.LOGGER.warn("[SupplyShare] couldn't save the queue: {}", e.toString());
        }
    }

    private static void rewriteFile() {
        try {
            if (queue.isEmpty()) {
                Files.deleteIfExists(queuePath());
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (Row r : queue) sb.append(line(r));
            Files.writeString(queuePath(), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            PhantomAddons.LOGGER.warn("[SupplyShare] couldn't save the queue: {}", e.toString());
        }
    }

    private static void clearQueue() {
        synchronized (queue) {
            queueLoaded = true;
            queue.clear();
            rewriteFile();
        }
    }

    // ── The remote gate ───────────────────────────────────────────────────────

    private static void checkGate() {
        try {
            JsonObject status = PhantomApi.withFallback(base -> getJson(base + "/status"));
            boolean open = status.has("supplyDataEnabled") && status.get("supplyDataEnabled").getAsBoolean();
            gateOpen = open;
            nextGateCheck = System.currentTimeMillis() + GATE_RECHECK_MS;
            if (!open) {
                PhantomAddons.LOGGER.info("[SupplyShare] data collection is switched off by the server");
                clearQueue();
            }
        } catch (IOException | RuntimeException e) {
            nextGateCheck = System.currentTimeMillis() + GATE_RETRY_MS;
            PhantomAddons.LOGGER.info("[SupplyShare] couldn't check whether data collection is on: {}", e.toString());
        } finally {
            gateChecking = false;
        }
    }

    private static JsonObject getJson(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "PhantomAddons-Share");
        conn.setConnectTimeout(8_000);
        conn.setReadTimeout(15_000);
        int code = conn.getResponseCode();
        if (code != 200) throw new IOException("HTTP " + code);
        try (InputStream is = conn.getInputStream()) {
            return JsonParser.parseString(new String(is.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    // ── Sending ───────────────────────────────────────────────────────────────

    private static void upload() {
        List<Row> batch = null;
        try {
            batch = takeBatch();
            if (batch.isEmpty()) {
                nextUpload = System.currentTimeMillis() + UPLOAD_INTERVAL_MS;
                return;
            }
            String body = body(batch);
            retryAfterSeconds = 0;
            PhantomApi.authed(base -> { post(base + "/data/supply", body); return null; });
            synchronized (queue) {
                queue.removeAll(batch);
                rewriteFile();
                nextUpload = System.currentTimeMillis() + (queue.isEmpty() ? UPLOAD_INTERVAL_MS : MORE_TO_SEND_MS);
            }
            PhantomAddons.LOGGER.info("[SupplyShare] sent {} rows", batch.size());
        } catch (PhantomApi.HttpException e) {
            handleRefusal(e, batch);
        } catch (IOException | RuntimeException e) {
            nextUpload = System.currentTimeMillis() + ERROR_BACKOFF_MS;
            PhantomAddons.LOGGER.info("[SupplyShare] couldn't send data, will try again later: {}", e.toString());
        } finally {
            uploading = false;
        }
    }

    private static void handleRefusal(PhantomApi.HttpException e, List<Row> batch) {
        long now = System.currentTimeMillis();
        switch (e.status) {
            case 400, 413 -> {
                synchronized (queue) {
                    if (batch != null) queue.removeAll(batch);
                    rewriteFile();
                }
                nextUpload = now + MORE_TO_SEND_MS;
                PhantomAddons.LOGGER.warn("[SupplyShare] the server refused a batch and it was dropped: {}", e.getMessage());
            }
            case 403 -> {
                refusedThisSession = true;
                gateOpen = false;
                clearQueue();
                PhantomAddons.LOGGER.info("[SupplyShare] the server doesn't want data right now: {}", e.getMessage());
            }
            case 429, 503 -> {
                long wait = retryAfterSeconds > 0 ? retryAfterSeconds * 1000L : ERROR_BACKOFF_MS;
                nextUpload = now + Math.max(wait, 60_000L);
                PhantomAddons.LOGGER.info("[SupplyShare] the server asked for a wait: {}", e.getMessage());
            }
            default -> {
                nextUpload = now + ERROR_BACKOFF_MS;
                PhantomAddons.LOGGER.info("[SupplyShare] couldn't send data ({}): {}", e.status, e.getMessage());
            }
        }
    }

    private static List<Row> takeBatch() {
        synchronized (queue) {
            loadQueue();
            List<Row> out = new ArrayList<>();
            Set<String> queuedGrabs = new HashSet<>();
            for (Row r : queue) if (r.type.equals("grab")) queuedGrabs.add(r.id);

            int spawns = 0, grabs = 0, cancels = 0;
            Set<String> batchGrabs = new HashSet<>();
            for (Row r : queue) {
                switch (r.type) {
                    case "spawn" -> { if (spawns < BATCH_SPAWNS) { out.add(r); spawns++; } }
                    case "grab" -> { if (grabs < BATCH_GRABS) { out.add(r); batchGrabs.add(r.id); grabs++; } }
                    default -> {}
                }
            }
            for (Row r : queue) {
                if (!r.type.equals("cancel") || cancels >= BATCH_CANCELS) continue;
                if (!queuedGrabs.contains(r.grabId) || batchGrabs.contains(r.grabId)) { out.add(r); cancels++; }
            }
            return out;
        }
    }

    private static String body(List<Row> rows) {
        JsonObject o = new JsonObject();
        o.addProperty("v", 1);
        JsonArray spawns = new JsonArray(), grabs = new JsonArray(), cancels = new JsonArray();
        for (Row r : rows) {
            switch (r.type) {
                case "spawn" -> spawns.add(r.json);
                case "grab" -> grabs.add(r.json);
                default -> cancels.add(r.json);
            }
        }
        o.add("spawns", spawns);
        o.add("grabs", grabs);
        o.add("cancels", cancels);
        return GSON.toJson(o);
    }

    private static void post(String url, String json) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", "PhantomAddons-Share");
        conn.setRequestProperty("Content-Type", "application/json");
        PhantomApi.authorize(conn);
        conn.setConnectTimeout(8_000);
        conn.setReadTimeout(15_000);
        conn.setDoOutput(true);
        conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        int code = conn.getResponseCode();
        if (code == 200) return;

        String retry = conn.getHeaderField("Retry-After");
        if (retry != null) {
            try { retryAfterSeconds = Long.parseLong(retry.trim()); } catch (NumberFormatException ignored) {}
        }
        InputStream is = conn.getErrorStream();
        String text = is != null ? new String(is.readAllBytes(), StandardCharsets.UTF_8) : "";
        String message = "HTTP " + code;
        String errorCode = null;
        try {
            JsonObject parsed = JsonParser.parseString(text).getAsJsonObject();
            if (parsed.has("error")) message = parsed.get("error").getAsString();
            if (parsed.has("code")) errorCode = parsed.get("code").getAsString();
        } catch (RuntimeException ignored) {}
        throw new PhantomApi.HttpException(code, message, errorCode);
    }
}
