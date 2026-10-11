package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class PvJson {

    private PvJson() {}

    static JsonElement path(JsonElement root, String... keys) {
        JsonElement cur = root;
        for (String k : keys) {
            if (cur == null || !cur.isJsonObject()) return null;
            cur = cur.getAsJsonObject().get(k);
        }
        return cur == null || cur.isJsonNull() ? null : cur;
    }

    static JsonObject obj(JsonElement root, String... keys) {
        JsonElement e = path(root, keys);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    static double num(JsonElement root, double fallback, String... keys) {
        JsonElement e = path(root, keys);
        if (e == null || !e.isJsonPrimitive()) return fallback;
        try { return e.getAsDouble(); } catch (NumberFormatException ex) { return fallback; }
    }

    static String str(JsonElement root, String fallback, String... keys) {
        JsonElement e = path(root, keys);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
    }

    static String shape(JsonElement e, int depth) {
        if (e == null || e.isJsonNull()) return "null";
        if (e.isJsonPrimitive()) return e.getAsJsonPrimitive().isString() ? "str" : "num";
        if (e.isJsonArray()) return "[" + e.getAsJsonArray().size() + "]";
        JsonObject o = e.getAsJsonObject();
        if (o.has("type") && o.has("data")) return "<items>";
        if (depth <= 0) return "{" + o.size() + " keys}";
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (var en : o.entrySet()) {
            if (i++ > 0) sb.append(", ");
            if (i > 24) { sb.append("..."); break; }
            sb.append(en.getKey()).append(": ").append(shape(en.getValue(), depth - 1));
        }
        return sb.append('}').toString();
    }
}
