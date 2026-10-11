package com.phantomaddons.features.customisation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class NameGradient {

    public static final String[] DIRECTIONS = { "none", "right", "left", "up", "down", "down-right", "down-left", "up-right", "up-left" };
    public static final float DEFAULT_SPEED = 4f;
    private static final float LINE_UNIT = 6f;

    private final int[] colors;
    private final float length;
    private final float dx, dy;
    private final float speed;

    public NameGradient(int[] colors, float length, String direction, float speed) {
        this.colors = colors.length == 0 ? new int[] { 0xFFFFFF } : colors;
        this.length = Math.max(0f, length);
        int[] d = vector(direction);
        float norm = (float) Math.sqrt(d[0] * d[0] + d[1] * d[1]);
        this.dx = d[0] / norm;
        this.dy = d[1] / norm;
        this.speed = "none".equals(normalise(direction)) ? 0f : Math.max(0f, speed);
    }

    public int colorAt(int index, int span, float y, double seconds) {
        int n = colors.length;
        if (n == 1) return colors[0];
        float len = length > 0f ? length : Math.max(1, span);
        double along = index * dx + (y / LINE_UNIT) * dy;
        double pos = (along - speed * seconds) % len;
        if (pos < 0) pos += len;
        double f = pos / len * n;            // the colour list loops back to its first entry
        int i0 = (int) f % n, i1 = (i0 + 1) % n;
        return mix(colors[i0], colors[i1], (float) (f - Math.floor(f)));
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    // ── Reading from text / JSON ──────────────────────────────────────────────

    public static int[] parseColors(String text) {
        List<Integer> out = new ArrayList<>();
        if (text != null) for (String part : text.split("[,\\s]+")) {
            Integer c = parseColor(part);
            if (c != null) out.add(c);
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    public static Integer parseColor(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() == 3) s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
        if (s.length() != 6) return null;
        try { return Integer.parseInt(s, 16); } catch (NumberFormatException e) { return null; }
    }

    public static String normalise(String s) {
        if (s == null) return "right";
        String t = s.trim().toLowerCase().replaceAll("[\\s_]+", "-");
        if (t.equals("static") || t.equals("still") || t.equals("off")) return "none";
        for (String d : DIRECTIONS) if (d.equals(t)) return d;
        return "right";
    }

    private static int[] vector(String direction) {
        return switch (normalise(direction)) {
            case "left" -> new int[] { -1, 0 };
            case "up" -> new int[] { 0, -1 };
            case "down" -> new int[] { 0, 1 };
            case "down-right" -> new int[] { 1, 1 };
            case "down-left" -> new int[] { -1, 1 };
            case "up-right" -> new int[] { 1, -1 };
            case "up-left" -> new int[] { -1, -1 };
            default -> new int[] { 1, 0 };   // right, and none (which just doesn't move)
        };
    }

    public static String nextDirection(String current) {
        String c = normalise(current);
        for (int i = 0; i < DIRECTIONS.length; i++) if (DIRECTIONS[i].equals(c)) return DIRECTIONS[(i + 1) % DIRECTIONS.length];
        return DIRECTIONS[0];
    }

    public static NameGradient fromJson(JsonObject o) {
        List<Integer> colors = new ArrayList<>();
        JsonElement c = o.get("colors");
        if (c != null && c.isJsonArray()) {
            for (JsonElement e : c.getAsJsonArray()) {
                Integer v = e.isJsonPrimitive() ? parseColor(e.getAsString()) : null;
                if (v != null) colors.add(v);
            }
        } else if (c != null && c.isJsonPrimitive()) {
            for (int v : parseColors(c.getAsString())) colors.add(v);
        }
        if (colors.isEmpty()) return null;
        float length = o.has("length") && o.get("length").isJsonPrimitive() ? o.get("length").getAsFloat() : 0f;
        float speed = o.has("speed") && o.get("speed").isJsonPrimitive() ? o.get("speed").getAsFloat() : DEFAULT_SPEED;
        String dir = o.has("direction") && o.get("direction").isJsonPrimitive() ? o.get("direction").getAsString() : "right";
        return new NameGradient(colors.stream().mapToInt(Integer::intValue).toArray(), length, dir, speed);
    }
}
