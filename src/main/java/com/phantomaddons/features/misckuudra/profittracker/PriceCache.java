package com.phantomaddons.features.misckuudra.profittracker;

import java.util.concurrent.ConcurrentHashMap;

public final class PriceCache {

    public static final long TTL_MS = 10L * 60 * 1000;

    private record Entry(double price, long ts) {}

    private static final ConcurrentHashMap<String, Entry> bazaarSell = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Entry> bazaarBuy  = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Entry> bins       = new ConcurrentHashMap<>();

    private static volatile long lastBazaarFetchMs = 0;
    private static volatile long lastBinsFetchMs   = 0;

    public static void putBazaarSell(String id, double price) { bazaarSell.put(id.toUpperCase(), new Entry(price, System.currentTimeMillis())); }
    public static void putBazaarBuy(String id, double price)  { bazaarBuy .put(id.toUpperCase(), new Entry(price, System.currentTimeMillis())); }
    public static void putBin(String id, double price)        { bins      .put(id.toUpperCase(), new Entry(price, System.currentTimeMillis())); }

    public static double getBazaarSell(String id) { return lookup(bazaarSell, id); }
    public static double getBazaarBuy(String id)  { return lookup(bazaarBuy,  id); }
    public static double getBin(String id)        { return lookup(bins,       id); }

    private static double lookup(ConcurrentHashMap<String, Entry> map, String id) {
        if (id == null) return -1;
        Entry e = map.get(id.toUpperCase());
        if (e == null) return -1;
        if (System.currentTimeMillis() - e.ts > TTL_MS) return -1;
        return e.price;
    }

    public static boolean bazaarNeedsFetch() { return System.currentTimeMillis() - lastBazaarFetchMs > TTL_MS; }
    public static boolean binsNeedFetch()    { return System.currentTimeMillis() - lastBinsFetchMs   > TTL_MS; }
    public static boolean areBinsLoaded()    { return !bins.isEmpty(); }

    public static void markBazaarFetched() { lastBazaarFetchMs = System.currentTimeMillis(); }
    public static void markBinsFetched()   { lastBinsFetchMs   = System.currentTimeMillis(); }

    public static void clear() {
        bazaarSell.clear(); bazaarBuy.clear(); bins.clear();
        lastBazaarFetchMs = 0; lastBinsFetchMs = 0;
    }

    private PriceCache() {}
}
