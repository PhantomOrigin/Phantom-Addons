package com.phantomaddons.features.customisation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.phantomaddons.HudEditorScreen;
import com.phantomaddons.gui.PhantomScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import com.phantomaddons.utils.TextUtil;

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
        public String gradient          = "";
        public int    gradientLength    = 0;
        public String gradientDirection = "right";
        public float  gradientSpeed     = NameGradient.DEFAULT_SPEED;
        transient StyledText styled;
        transient Pattern pattern;
        transient String  replQuoted;
        transient String  inputLower;
    }

    private static final class Data {
        boolean    enabled = false;
        List<Rule> rules   = new ArrayList<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = com.phantomaddons.utils.PhantomPaths.resolve("phantomaddons_visualwords.json");

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

    private static boolean gradientsAvailable() { return com.phantomaddons.DataFeatures.hasHighlight(); }

    private static boolean anyGradient = false;

    private static void recompile() {
        anyGradient = false;
        for (Rule r : RULES) {
            int[] cols = gradientsAvailable() && r.gradient != null ? NameGradient.parseColors(r.gradient) : new int[0];
            boolean codes = r.replacement != null && r.replacement.matches("(?s).*&([0-9a-fA-Fk-oK-OrR]|#[0-9a-fA-F]{6}).*");
            r.styled = cols.length == 0 && !codes ? null
                    : new StyledText(r.replacement, cols.length == 0 ? null
                            : new NameGradient(cols, r.gradientLength, r.gradientDirection, r.gradientSpeed));
            anyGradient |= r.styled != null;
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
        var screen = Minecraft.getInstance().gui.screen();
        if (screen instanceof PhantomScreen || screen instanceof HudEditorScreen) return false;
        return true;
    }

    private static boolean matchesAny(CharSequence text) {
        for (Rule r : RULES) {
            if (r.pattern != null && !r.input.isEmpty()
                    && TextUtil.containsIgnoreCase(text, r.input)) return true;
        }
        return false;
    }

    private static String applyRules(String s) {
        for (Rule r : RULES) {
            if (r.pattern == null || r.styled != null) continue;
            s = r.pattern.matcher(s).replaceAll(r.replQuoted);
        }
        return s;
    }

    public static String applyString(String s) {
        if (s == null || !active()) return s;
        if (!matchesAny(s)) return s;
        return applyRules(s);
    }

    public static float centerShift(net.minecraft.client.gui.Font font, FormattedCharSequence original) {
        if (original == null || !active()) return 0f;
        FormattedCharSequence replaced = apply(original);
        if (replaced == original) return 0f; // unchanged
        return (font.width(original) - font.width(DonatorNames.steady(replaced))) / 2f;
    }

    private static final ThreadLocal<StringBuilder> SCRATCH = ThreadLocal.withInitial(StringBuilder::new);

    private record Seg(String text, StyledText styled) {}

    private static boolean appendWithGradients(MutableComponent root, String orig, Style style, float y, double seconds) {
        List<Seg> segs = new ArrayList<>();
        segs.add(new Seg(orig, null));
        for (Rule r : RULES) {
            if (r.styled == null || r.pattern == null) continue;
            List<Seg> next = new ArrayList<>();
            for (Seg s : segs) {
                if (s.styled != null) { next.add(s); continue; }
                Matcher m = r.pattern.matcher(s.text);
                int last = 0;
                while (m.find()) {
                    if (m.start() > last) next.add(new Seg(s.text.substring(last, m.start()), null));
                    next.add(new Seg(m.group(), r.styled));
                    last = m.end();
                }
                if (last < s.text.length()) next.add(new Seg(s.text.substring(last), null));
            }
            segs = next;
        }
        boolean changed = false;
        for (Seg s : segs) {
            if (s.styled == null) {
                String rep = applyRules(s.text);
                if (!rep.equals(s.text)) changed = true;
                root.append(Component.literal(rep).setStyle(style));
                continue;
            }
            changed = true;
            s.styled.emit(s.text, style, y, seconds,
                    (cp, st) -> root.append(Component.literal(new String(Character.toChars(cp))).setStyle(st)));
        }
        return changed;
    }

    public static FormattedCharSequence apply(FormattedCharSequence in) { return apply(in, 0f); }

    public static FormattedCharSequence apply(FormattedCharSequence in, float y) {
        if (in == null || !active()) return in;
        try {
            StringBuilder scratch = SCRATCH.get();
            scratch.setLength(0);
            in.accept((pos, style, codePoint) -> {
                scratch.appendCodePoint(codePoint);
                return true;
            });
            if (scratch.length() == 0 || !matchesAny(scratch)) return in;

            List<Style>         styles = new ArrayList<>();
            List<StringBuilder> runs   = new ArrayList<>();

            in.accept((pos, style, codePoint) -> {
                if (runs.isEmpty() || !styles.get(styles.size() - 1).equals(style)) {
                    styles.add(style);
                    runs.add(new StringBuilder());
                }
                runs.get(runs.size() - 1).appendCodePoint(codePoint);
                return true;
            });

            boolean changed = false;
            MutableComponent root = Component.empty();
            double seconds = System.nanoTime() / 1e9;
            for (int i = 0; i < runs.size(); i++) {
                String orig = runs.get(i).toString();
                if (anyGradient) { changed |= appendWithGradients(root, orig, styles.get(i), y, seconds); continue; }
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
