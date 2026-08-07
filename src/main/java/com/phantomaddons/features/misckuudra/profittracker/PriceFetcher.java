package com.phantomaddons.features.misckuudra.profittracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.phantomaddons.PhantomAddons;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


public final class PriceFetcher {

    private static final String BAZAAR_URL   = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final String COFLNET_URL  = "https://sky.coflnet.com/api/item/price/";

    private static final int  MAX_ATTEMPTS   = 10;
    private static final long RETRY_DELAY_MS = 5_000L;

    private static final AtomicBoolean bazaarFetching = new AtomicBoolean(false);
    private static final AtomicBoolean binsFetching   = new AtomicBoolean(false);

    public static void fetchBazaarIfStale() {
        if (!PriceCache.bazaarNeedsFetch()) return;
        if (!bazaarFetching.compareAndSet(false, true)) return;
        CompletableFuture.runAsync(() -> {
            try {
                int count = fetchBazaarWithRetry();
                if (count > 0) PriceCache.markBazaarFetched();
                PhantomAddons.LOGGER.info("[PriceFetcher] Bazaar loaded: {} products", count);
            } finally {
                bazaarFetching.set(false);
            }
        });
    }

    private static int fetchBazaarWithRetry() {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String json = get(BAZAAR_URL);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonObject products = root.getAsJsonObject("products");
                if (products != null) {
                    int count = 0;
                    for (java.util.Map.Entry<String, JsonElement> entry : products.entrySet()) {
                        String id = entry.getKey().toUpperCase();
                        JsonObject qs = entry.getValue().getAsJsonObject()
                                .getAsJsonObject("quick_status");
                        if (qs == null) continue;
                        double sellPrice = qs.has("sellPrice") ? qs.get("sellPrice").getAsDouble() : -1;
                        double buyPrice  = qs.has("buyPrice")  ? qs.get("buyPrice").getAsDouble()  : -1;
                        if (sellPrice > 0) { PriceCache.putBazaarSell(id, sellPrice); count++; }
                        if (buyPrice  > 0) PriceCache.putBazaarBuy(id,  buyPrice);
                    }
                    if (count > 0) return count;
                }
            } catch (Exception e) {
                PhantomAddons.LOGGER.warn("[PriceFetcher] Bazaar fetch attempt {}/{} failed: {}",
                        attempt, MAX_ATTEMPTS, e.getMessage());
            }
            if (attempt < MAX_ATTEMPTS && !sleepBeforeRetry()) return 0;
        }
        return 0;
    }

    public static void fetchBinsIfStale() {
        if (!PriceCache.binsNeedFetch()) return;
        if (!binsFetching.compareAndSet(false, true)) return;
        CompletableFuture.runAsync(() -> {
            try {
                int ok = 0, fail = 0;
                for (String id : KuudraDrops.AH_ITEM_IDS) {
                    double price = fetchBinPriceWithRetry(COFLNET_URL + id, "min");
                    if (price > 0) { PriceCache.putBin(id, price); ok++; } else fail++;
                }
                for (String id : KuudraDrops.AH_LOWEST_BIN_ITEM_IDS) {
                    double price = fetchBinPriceWithRetry(COFLNET_URL + id + "/bin", "lowest");
                    if (price > 0) { PriceCache.putBin(id, price); ok++; } else fail++;
                }
                if (ok > 0) PriceCache.markBinsFetched();
                PhantomAddons.LOGGER.info("[PriceFetcher] BINs loaded: {}/{} items", ok, ok + fail);
            } finally {
                binsFetching.set(false);
            }
        });
    }

    private static double fetchBinPriceWithRetry(String url, String priceField) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String json = get(url);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                double price = obj.has(priceField) ? obj.get(priceField).getAsDouble() : -1;
                if (price > 0) return price;
            } catch (Exception e) {
                PhantomAddons.LOGGER.warn("[PriceFetcher] BIN fetch attempt {}/{} failed for {}: {}",
                        attempt, MAX_ATTEMPTS, url, e.getMessage());
            }
            if (attempt < MAX_ATTEMPTS && !sleepBeforeRetry()) return -1;
        }
        return -1;
    }

    private static boolean sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String get(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "PhantomAddons-ProfitTracker");
        conn.setConnectTimeout(8_000);
        conn.setReadTimeout(15_000);
        try (InputStream is = conn.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining());
        }
    }

    private PriceFetcher() {}
}
