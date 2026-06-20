package com.kuudrahelper.features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kuudrahelper.HudEditorScreen;
import com.kuudrahelper.KuudraScreen;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VisualWords {

    public static final class Rule {
        public String input       = "";
        public String replacement = "";
        transient Pattern pattern;
        transient String  replQuoted;
        transient String  inputLower;
    }

    private static final class Data {
        boolean    enabled = false;
        List<Rule> rules   = new ArrayList<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("phantomaddons_visualwords.json");

    private static boolean enabled = false;
    private static final List<Rule> RULES = new ArrayList<>();

    private VisualWords() {}

    public static void load() {
        Path p = PATH;
        if (!p.toFile().exists()) { recompile(); return; }
        try (Reader r = new FileReader(p.toFile())) {
            Data d = GSON.fromJson(r, Data.class);
            if (d != null) {
                enabled = d.enabled;
                RULES.clear();
                if (d.rules != null) RULES.addAll(d.rules);
            }
        } catch (Exception ignored) {}
        recompile();
    }

    public static void save() {
        recompile();
        Data d = new Data();
        d.enabled = enabled;
        d.rules   = RULES;
        try (Writer w = new FileWriter(PATH.toFile())) {
            GSON.toJson(d, w);
        } catch (Exception ignored) {}
    }

    private static void recompile() {
        for (Rule r : RULES) {
            if (r.input == null) r.input = "";
            if (r.replacement == null) r.replacement = "";
            r.inputLower = r.input.toLowerCase();
            r.replQuoted = Matcher.quoteReplacement(r.replacement);
            r.pattern = r.input.isEmpty() ? null
                    : Pattern.compile(Pattern.quote(r.input), Pattern.CASE_INSENSITIVE);
        }
    }

    public static boolean isEnabled()        { return enabled; }
    public static void    setEnabled(boolean v) { enabled = v; save(); }
    public static List<Rule> getRules()      { return RULES; }
    public static void addRule()             { RULES.add(new Rule()); save(); }
    public static void removeRule(int idx)   { if (idx >= 0 && idx < RULES.size()) { RULES.remove(idx); save(); } }

    private static boolean active() {
        if (!enabled || RULES.isEmpty()) return false;
        var screen = Minecraft.getInstance().screen;
        if (screen instanceof KuudraScreen || screen instanceof HudEditorScreen) return false;
        return true;
    }

    private static boolean matchesAny(String lower) {
        for (Rule r : RULES) {
            if (r.pattern != null && !r.inputLower.isEmpty() && lower.contains(r.inputLower)) return true;
        }
        return false;
    }

    private static String applyRules(String s) {
        for (Rule r : RULES) {
            if (r.pattern == null) continue;
            s = r.pattern.matcher(s).replaceAll(r.replQuoted);
        }
        return s;
    }

    public static String applyString(String s) {
        if (s == null || !active()) return s;
        if (!matchesAny(s.toLowerCase())) return s;
        return applyRules(s);
    }

    public static float centerShift(net.minecraft.client.gui.Font font, FormattedCharSequence original) {
        if (original == null || !active()) return 0f;
        FormattedCharSequence replaced = apply(original);
        if (replaced == original) return 0f; // unchanged
        return (font.width(original) - font.width(replaced)) / 2f;
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
                String orig = runs.get(i).toString();
                String rep  = applyRules(orig);
                if (!rep.equals(orig)) changed = true;
                root.append(Component.literal(rep).setStyle(styles.get(i)));
            }
            return changed ? root.getVisualOrderText() : in;
        } catch (Throwable t) {
            return in;
        }
    }
}
