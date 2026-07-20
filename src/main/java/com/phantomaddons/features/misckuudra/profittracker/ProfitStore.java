package com.phantomaddons.features.misckuudra.profittracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.phantomaddons.PhantomAddons;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ProfitStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("phantomaddons_profit.json");

    public record Stats(
        long itemsValue, long attributeValue, long essenceValue,
        long keyCost, long kismetCost, long wheelCost,
        int  runs,
        long totalDurationMs
    ) {
        public long totalGains()    { return itemsValue + attributeValue + essenceValue; }
        public long totalExpenses() { return keyCost + kismetCost + wheelCost; }
        public long profit()        { return totalGains() - totalExpenses(); }
        public long profitPerRun()  { return runs > 0 ? profit() / runs : 0; }
    }

    private static final List<ProfitRun> session = new ArrayList<>();
    private static final List<ProfitRun> allTime = new ArrayList<>();

    private static final java.util.Map<Integer, List<Double>> sessionRunTimes = new java.util.HashMap<>();

    // ── View toggle ──────────────────────────────────────────────────────────────
    private static boolean showingSession = true;
    public static boolean isShowingSession()        { return showingSession; }
    public static void    toggleView()              { showingSession = !showingSession; }
    public static void    setShowSession(boolean v) { showingSession = v; }

    // ── Last-committed run metadata ───────────────────────────────────────────────
    private static int lastCommittedTier = 0;

    public static void setLastCommittedTier(int tier) { lastCommittedTier = tier; }
    public static int  getLastCommittedTier()         { return lastCommittedTier; }

    public static void updateLastRunKismetCost(long additionalCost) {
        if (allTime.isEmpty()) return;
        allTime.get(allTime.size() - 1).kismetCost += additionalCost;
        save();
    }

    public static void addRun(ProfitRun run) {
        session.add(run);
        allTime.add(run);
        save();
    }

    public static void resetSession() {
        session.clear();
        sessionRunTimes.clear();
        save();
    }

    public static void resetAllTime() {
        allTime.clear();
        session.clear();
        sessionRunTimes.clear();
        save();
    }

    // ── Run-time averages (for !avg session) ──────────────────────────────────────

    public static void recordSessionRunTime(int tier, double seconds) {
        if (tier < 1 || tier > 5) return;
        sessionRunTimes.computeIfAbsent(tier, k -> new ArrayList<>()).add(seconds);
        save();
    }

    public static int getSessionHighestTier() {
        int best = -1;
        for (var entry : sessionRunTimes.entrySet()) {
            if (!entry.getValue().isEmpty() && entry.getKey() > best) best = entry.getKey();
        }
        return best;
    }

    public static double getSessionRunTimeAverage(int tier) {
        List<Double> times = sessionRunTimes.get(tier);
        if (times == null || times.isEmpty()) return -1;
        return times.stream().mapToDouble(Double::doubleValue).average().orElse(-1);
    }

    public static int getSessionRunTimeCount(int tier) {
        List<Double> times = sessionRunTimes.get(tier);
        return times == null ? 0 : times.size();
    }

    public static Stats getActiveStats() { return compute(showingSession ? session : allTime); }
    public static Stats getSessionStats() { return compute(session); }
    public static Stats getAllTimeStats()  { return compute(allTime); }

    private static Stats compute(List<ProfitRun> runs) {
        long items = 0, attr = 0, ess = 0, keys = 0, kismets = 0, wheels = 0, dur = 0;
        for (ProfitRun r : runs) {
            items   += r.itemsValue;
            attr    += r.attributeValue;
            ess     += r.essenceValue;
            keys    += r.keyCost;
            kismets += r.kismetCost;
            wheels  += r.wheelCost;
            dur     += r.durationMs;
        }
        return new Stats(items, attr, ess, keys, kismets, wheels, runs.size(), dur);
    }

    // ── Persistence ───────────────────────────────────────────────────────────────

    private static final Type RUN_LIST_TYPE = new TypeToken<List<ProfitRun>>(){}.getType();

    public static void load() {
        File f = DATA_PATH.toFile();
        if (!f.exists()) return;
        try (Reader r = new FileReader(f)) {
            JsonElement root = JsonParser.parseReader(r);
            if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("allTime")) {
                    List<ProfitRun> at = GSON.fromJson(obj.get("allTime"), RUN_LIST_TYPE);
                    if (at != null) allTime.addAll(at);
                }
                if (obj.has("session")) {
                    List<ProfitRun> s = GSON.fromJson(obj.get("session"), RUN_LIST_TYPE);
                    if (s != null) session.addAll(s);
                }
                if (obj.has("lastTier")) lastCommittedTier = obj.get("lastTier").getAsInt();
                if (obj.has("sessionRunTimes")) {
                    Type mapType = new TypeToken<java.util.Map<Integer, List<Double>>>(){}.getType();
                    java.util.Map<Integer, List<Double>> m = GSON.fromJson(obj.get("sessionRunTimes"), mapType);
                    if (m != null) sessionRunTimes.putAll(m);
                }
            } else if (root.isJsonArray()) {
                List<ProfitRun> at = GSON.fromJson(root, RUN_LIST_TYPE);
                if (at != null) allTime.addAll(at);
            }
        } catch (IOException e) {
            PhantomAddons.LOGGER.error("[ProfitTracker] Failed to load profit data", e);
        }
    }

    public static void save() {
        try (Writer w = new FileWriter(DATA_PATH.toFile())) {
            JsonObject obj = new JsonObject();
            obj.add("session", GSON.toJsonTree(session, RUN_LIST_TYPE));
            obj.add("allTime", GSON.toJsonTree(allTime, RUN_LIST_TYPE));
            obj.addProperty("lastTier", lastCommittedTier);
            obj.add("sessionRunTimes", GSON.toJsonTree(sessionRunTimes));
            GSON.toJson(obj, w);
        } catch (IOException e) {
            PhantomAddons.LOGGER.error("[ProfitTracker] Failed to save profit data", e);
        }
    }

    private ProfitStore() {}
}
