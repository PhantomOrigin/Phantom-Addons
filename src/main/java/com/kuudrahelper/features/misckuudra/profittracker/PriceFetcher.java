package com.kuudrahelper.features.misckuudra.profittracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kuudrahelper.KuudraHelperMod;

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

    private static final AtomicBoolean bazaarFetching = new AtomicBoolean(false);
    private static final AtomicBoolean binsFetching   = new AtomicBoolean(false);

    public static void fetchBazaarIfStale() {
        if (!PriceCache.bazaarNeedsFetch()) return;
        if (!bazaarFetching.compareAndSet(false, true)) return;
        PriceCache.markBazaarFetched();
        CompletableFuture.runAsync(() -> {
            try {
                String json = get(BAZAAR_URL);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonObject products = root.getAsJsonObject("products");
                if (products == null) return;
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
                KuudraHelperMod.LOGGER.info("[PriceFetcher] Bazaar loaded: {} products", count);
            } catch (Exception e) {
                KuudraHelperMod.LOGGER.warn("[ProfitTracker] Bazaar fetch failed: {}", e.getMessage());
            } finally {
                bazaarFetching.set(false);
            }
        });
    }

    public static void fetchBinsIfStale() {
        if (!PriceCache.binsNeedFetch()) return;
        if (!binsFetching.compareAndSet(false, true)) return;
        PriceCache.markBinsFetched();
        CompletableFuture.runAsync(() -> {
            try {
                int ok = 0, fail = 0;
                for (String id : KuudraDrops.AH_ITEM_IDS) {
                    try {
                        String json  = get(COFLNET_URL + id);
                        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                        double price = obj.has("min") ? obj.get("min").getAsDouble() : -1;
                        if (price > 0) { PriceCache.putBin(id, price); ok++; }
                        else fail++;
                    } catch (Exception e) {
                        KuudraHelperMod.LOGGER.warn("[PriceFetcher] BIN fetch failed for {}: {}", id, e.getMessage());
                        fail++;
                    }
                }
                KuudraHelperMod.LOGGER.info("[PriceFetcher] BINs loaded: {}/{} items", ok, ok + fail);
            } finally {
                binsFetching.set(false);
            }
        });
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
