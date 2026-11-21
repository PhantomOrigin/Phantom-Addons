package com.phantomaddons.features.abilities;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AbilitySettings {

    public static final int FORMAT_TICKS = 0, FORMAT_SECONDS = 1, FORMAT_MS = 2;
    public static final int LABEL_ICON = 0, LABEL_ABILITY = 1, LABEL_ITEM = 2, LABEL_NONE = 3;

    public boolean enabled = false;
    public boolean smooth = true;
    public int     format = FORMAT_SECONDS;
    public boolean showActive = true;
    public boolean bar = true;
    public int     cooldownColor = 0x3F8CFF;
    public int     activeColor = 0xFF9F1A;
    public int     label = LABEL_ICON;
    public LinkedHashMap<Integer, Style> styles = new LinkedHashMap<>();

    public float listX = 0.01f, listY = 0.62f, listScale = 1.0f;
    public LinkedHashMap<String, String> groups = new LinkedHashMap<>();
    public LinkedHashMap<Integer, float[]> groupPos = new LinkedHashMap<>();
    public static final int MAX_GROUP = 99;
    public LinkedHashMap<String, float[]> iconPos = new LinkedHashMap<>();

    public LinkedHashSet<String> selected = new LinkedHashSet<>();
    public LinkedHashMap<String, LinkedHashSet<String>> disabledIslands = new LinkedHashMap<>();
    public LinkedHashMap<String, Learned> learned = new LinkedHashMap<>();

    public static final class Learned {
        public String name = "", type = "WEAPON", item = "", itemName = "", icon = "minecraft:paper";
        public double cooldown, active;
    }

    public void sanitize() {
        format = Math.max(0, Math.min(2, format));
        label  = Math.max(0, Math.min(3, label));
        if (styles == null) styles = new LinkedHashMap<>();
        styles.values().removeIf(v -> v == null);
        styles.keySet().removeIf(g -> g == null || g < 0 || g > MAX_GROUP);
        for (Style st : styles.values()) {
            st.format = Math.max(0, Math.min(2, st.format));
            st.label = Math.max(0, Math.min(3, st.label));
            st.cooldownColor &= 0xFFFFFF;
            st.activeColor &= 0xFFFFFF;
        }
        cooldownColor &= 0xFFFFFF;
        activeColor   &= 0xFFFFFF;
        listX = clamp01(listX);
        listY = clamp01(listY);
        listScale = Math.max(0.2f, Math.min(3f, listScale <= 0f ? 1f : listScale));
        if (iconPos == null) iconPos = new LinkedHashMap<>();
        if (selected == null) selected = new LinkedHashSet<>();
        if (disabledIslands == null) disabledIslands = new LinkedHashMap<>();
        if (learned == null) learned = new LinkedHashMap<>();
        iconPos.values().removeIf(p -> p == null || p.length != 3);
        if (groups == null) groups = new LinkedHashMap<>();
        if (groupPos == null) groupPos = new LinkedHashMap<>();
        groups.values().removeIf(g -> g == null || g.isBlank());
        groups.replaceAll((id, text) -> normalizeGroups(text));
        groups.values().removeIf(g -> g.equals("1"));
        groupPos.values().removeIf(p -> p == null || p.length != 3);
        groupPos.keySet().removeIf(g -> g == null || g <= 1 || g > MAX_GROUP);
    }

    public boolean isSelected(String id) { return selected.contains(id); }

    public void setSelected(String id, boolean on) {
        if (on) selected.add(id); else selected.remove(id);
    }

    public boolean worksOn(String id, String island) {
        Set<String> off = disabledIslands.get(id);
        return off == null || !off.contains(island);
    }

    public void setWorksOn(String id, String island, boolean on) {
        LinkedHashSet<String> off = disabledIslands.computeIfAbsent(id, k -> new LinkedHashSet<>());
        if (on) off.remove(island); else off.add(island);
        if (off.isEmpty()) disabledIslands.remove(id);
    }

    public void setWorksEverywhere(String id, boolean on) {
        if (on) { disabledIslands.remove(id); return; }
        LinkedHashSet<String> off = new LinkedHashSet<>();
        for (Island i : Island.selectable()) off.add(i.name());
        disabledIslands.put(id, off);
    }

    public boolean worksNowhere(String id) {
        Set<String> off = disabledIslands.get(id);
        return off != null && off.size() >= Island.selectable().size();
    }

    public static String normalizeGroups(String text) {
        java.util.TreeSet<Integer> out = new java.util.TreeSet<>();
        if (text != null) {
            for (String part : text.split("[,;\\s]+")) {
                if (part.isEmpty()) continue;
                try {
                    int g = Integer.parseInt(part);
                    if (g >= 0 && g <= MAX_GROUP) out.add(g);
                } catch (NumberFormatException ignored) {}
            }
        }
        if (out.isEmpty()) return "1";
        StringBuilder sb = new StringBuilder();
        for (int g : out) { if (sb.length() > 0) sb.append(','); sb.append(g); }
        return sb.toString();
    }

    public String groupText(String id) {
        String g = groups.get(id);
        return g == null ? "1" : g;
    }

    public void setGroupText(String id, String text) {
        String clean = normalizeGroups(text);
        if (clean.equals("1")) groups.remove(id); else groups.put(id, clean);
    }

    public static final class Style {
        public boolean enabled = true;
        public int format = FORMAT_SECONDS;
        public boolean showActive = true;
        public boolean bar = true;
        public transient boolean smooth = true;
        public int cooldownColor = 0x3F8CFF;
        public int activeColor = 0xFF9F1A;
        public int label = LABEL_ICON;
        public transient int group;
    }

    private Style defaultStyle() {
        Style st = new Style();
        st.format = format; st.showActive = showActive; st.bar = bar;
        st.cooldownColor = cooldownColor; st.activeColor = activeColor; st.label = label;
        return st;
    }

    public Style style(int group) {
        Style st = styles.get(group);
        if (st == null) st = defaultStyle();
        st.group = group;
        st.smooth = smooth;
        return st;
    }

    public Style editStyle(int group) {
        Style st = styles.get(group);
        if (st == null) { st = defaultStyle(); styles.put(group, st); }
        st.group = group;
        st.smooth = smooth;
        return st;
    }

    public int[] groupsOf(String id) {
        String[] parts = groupText(id).split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }

    public boolean inGroup(String id, int group) {
        for (int g : groupsOf(id)) if (g == group) return true;
        return false;
    }

    public static String ruleKey(int group, String abilityId) { return group == 0 ? abilityId : "#" + group; }

    public boolean shownOn(String id, String island) {
        for (int g : groupsOf(id)) if (style(g).enabled && worksOn(ruleKey(g, id), island)) return true;
        return false;
    }

    public java.util.List<Integer> activeGroups() {
        java.util.TreeSet<Integer> out = new java.util.TreeSet<>();
        for (String id : selected) {
            if (AbilityData.byId(id) == null) continue;
            for (int g : groupsOf(id)) if (g > 0) out.add(g);
        }
        return new java.util.ArrayList<>(out);
    }

    public java.util.List<String> individualAbilities() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String id : selected) if (AbilityData.byId(id) != null && inGroup(id, 0)) out.add(id);
        return out;
    }

    public float[] defaultListPos(int group) {
        if (group <= 1) return new float[]{ 0.01f, 0.62f, 1.0f };
        return new float[]{ clamp01(0.01f + 0.2f * (group - 1)), 0.62f, 1.0f };
    }

    public float[] listPos(int group) {
        if (group <= 1) return new float[]{ listX, listY, listScale };
        float[] p = groupPos.get(group);
        return p != null ? p : defaultListPos(group);
    }

    public void setListPos(int group, float x, float y, float scale) {
        float sc = Math.max(0.2f, Math.min(3f, scale));
        if (group <= 1) { listX = clamp01(x); listY = clamp01(y); listScale = sc; }
        else groupPos.put(group, new float[]{ clamp01(x), clamp01(y), sc });
    }

    public float[] iconPos(String id, int index) {
        float[] p = iconPos.get(id);
        if (p != null) return p;
        return new float[]{ 0.4f, Math.min(0.9f, 0.5f + 0.05f * index), 1.0f };
    }

    public void setIconPos(String id, float x, float y, float scale) {
        iconPos.put(id, new float[]{ clamp01(x), clamp01(y), Math.max(0.2f, Math.min(3f, scale)) });
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
}
