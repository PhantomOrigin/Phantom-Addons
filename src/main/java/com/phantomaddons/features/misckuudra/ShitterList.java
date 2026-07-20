package com.phantomaddons.features.misckuudra;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.phantomaddons.HudEditorScreen;
import com.phantomaddons.PhantomScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ShitterList {

    private static final class Data {
        boolean      enabled         = false;
        boolean      autoKickEnabled = false;
        List<String> names           = new ArrayList<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("phantomaddons_shitterlist.json");

    private static final int DARK_RED = 0x8B0000;

    private static boolean enabled         = false;
    private static boolean autoKickEnabled = false;
    private static final List<String> NAMES       = new ArrayList<>();
    private static final Set<String>  NAMES_LOWER = new LinkedHashSet<>();

    private ShitterList() {}

    public static void load() {
        Path p = PATH;
        if (!p.toFile().exists()) return;
        try (Reader r = new FileReader(p.toFile())) {
            Data d = GSON.fromJson(r, Data.class);
            if (d != null) {
                enabled = d.enabled;
                autoKickEnabled = d.autoKickEnabled;
                NAMES.clear();
                NAMES_LOWER.clear();
                if (d.names != null) {
                    for (String n : d.names) {
                        if (n == null || n.isBlank()) continue;
                        NAMES.add(n);
                        NAMES_LOWER.add(n.toLowerCase());
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        Data d = new Data();
        d.enabled = enabled;
        d.autoKickEnabled = autoKickEnabled;
        d.names = NAMES;
        try (Writer w = new FileWriter(PATH.toFile())) {
            GSON.toJson(d, w);
        } catch (Exception ignored) {}
    }

    public static boolean isEnabled()             { return enabled; }
    public static void    setEnabled(boolean v)   { enabled = v; save(); }
    public static boolean isAutoKickEnabled()           { return autoKickEnabled; }
    public static void    setAutoKickEnabled(boolean v) { autoKickEnabled = v; save(); }

    public static List<String> getNames() { return new ArrayList<>(NAMES); }

    public static boolean add(String name) {
        if (name == null || name.isBlank()) return false;
        String lower = name.toLowerCase();
        if (!NAMES_LOWER.add(lower)) return false;
        NAMES.add(name);
        save();
        return true;
    }

    public static boolean remove(String name) {
        if (name == null || name.isBlank()) return false;
        String lower = name.toLowerCase();
        if (!NAMES_LOWER.remove(lower)) return false;
        NAMES.removeIf(n -> n.equalsIgnoreCase(name));
        save();
        return true;
    }

    public static boolean contains(String name) {
        return name != null && NAMES_LOWER.contains(name.toLowerCase());
    }

    public static void checkAutoKick(String name) {
        if (!enabled || !autoKickEnabled) return;
        if (!contains(name)) return;
        if (!PartyCommands.isPartyLeader()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        mc.execute(() -> mc.getConnection().sendCommand("p kick " + name));
    }

    private static boolean active() {
        if (!enabled || NAMES.isEmpty()) return false;
        var screen = Minecraft.getInstance().screen;
        if (screen instanceof PhantomScreen || screen instanceof HudEditorScreen) return false;
        return true;
    }

    private static boolean matchesAny(String lower) {
        for (String n : NAMES_LOWER) {
            if (!n.isEmpty() && lower.contains(n)) return true;
        }
        return false;
    }

    public static FormattedCharSequence apply(FormattedCharSequence in) {
        if (in == null || !active()) return in;
        try {
            List<Style>         styles = new ArrayList<>();
            List<StringBuilder> runs   = new ArrayList<>();
            StringBuilder full = new StringBuilder();

            in.accept((pos, style, codePoint) -> {
                if (runs.isEmpty() || !styles.get(styles.size() - 1).equals(style)) {
                    styles.add(style);
                    runs.add(new StringBuilder());
                }
                runs.get(runs.size() - 1).appendCodePoint(codePoint);
                full.appendCodePoint(codePoint);
                return true;
            });

            if (full.length() == 0 || !matchesAny(full.toString().toLowerCase())) return in;

            boolean changed = false;
            MutableComponent root = Component.empty();
            for (int i = 0; i < runs.size(); i++) {
                String orig  = runs.get(i).toString();
                Style  style = styles.get(i);
                if (matchesAny(orig.toLowerCase())) {
                    style = style.withColor(DARK_RED);
                    changed = true;
                }
                root.append(Component.literal(orig).setStyle(style));
            }
            return changed ? root.getVisualOrderText() : in;
        } catch (Throwable t) {
            return in;
        }
    }
}
