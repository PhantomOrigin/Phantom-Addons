package com.phantomaddons.features.customisation;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;

public final class StyledText {

    private record Cell(int cp, boolean bold, boolean italic, boolean underlined, boolean strike, boolean obf, int rgb) {}

    public interface Out { void accept(int codePoint, Style style); }

    private static final String LEGACY_COLORS = "0123456789abcdef";
    private static final int[] LEGACY_RGB = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF };

    private final String template;
    private final NameGradient gradient;

    public StyledText(String template, NameGradient gradient) {
        this.template = template == null || template.isEmpty() ? null : template;
        this.gradient = gradient;
    }

    public static StyledText fromJson(JsonObject o) {
        String replace = o.has("replace") && o.get("replace").isJsonPrimitive() ? o.get("replace").getAsString() : null;
        NameGradient g = NameGradient.fromJson(o);
        if (g == null && (replace == null || replace.isEmpty())) return null;
        return new StyledText(replace, g);
    }

    public void emit(String matched, Style base, float y, double seconds, Out out) {
        Cell[] cells = parse(template == null ? matched : template.replace("{name}", matched));
        int n = cells.length;
        for (int k = 0; k < n; k++) {
            Cell c = cells[k];
            Style s = base;
            if (c.rgb >= 0) s = s.withColor(TextColor.fromRgb(c.rgb));
            else if (gradient != null) s = s.withColor(TextColor.fromRgb(gradient.colorAt(k, n, y, seconds)));
            if (c.bold) s = s.withBold(true);
            if (c.italic) s = s.withItalic(true);
            if (c.underlined) s = s.withUnderlined(true);
            if (c.strike) s = s.withStrikethrough(true);
            if (c.obf) s = s.withObfuscated(true);
            out.accept(c.cp, s);
        }
    }

    private static Cell[] parse(String text) {
        List<Cell> out = new ArrayList<>();
        boolean bold = false, italic = false, underlined = false, strike = false, obf = false;
        int rgb = -1;
        int[] cps = text.codePoints().toArray();
        for (int i = 0; i < cps.length; i++) {
            int cp = cps[i];
            if ((cp == '&' || cp == '§') && i + 1 < cps.length) {
                int code = Character.toLowerCase(cps[i + 1]);
                if (code == '#' && i + 7 < cps.length + 0 && hex(cps, i + 2) >= 0) {
                    rgb = hex(cps, i + 2);
                    bold = italic = underlined = strike = obf = false;
                    i += 7;
                    continue;
                }
                int colour = LEGACY_COLORS.indexOf(code);
                if (colour >= 0) {
                    rgb = LEGACY_RGB[colour];
                    bold = italic = underlined = strike = obf = false;   // a colour code clears formatting, as in the game
                    i++;
                    continue;
                }
                boolean known = true;
                switch (code) {
                    case 'l' -> bold = true;
                    case 'o' -> italic = true;
                    case 'n' -> underlined = true;
                    case 'm' -> strike = true;
                    case 'k' -> obf = true;
                    case 'r' -> { rgb = -1; bold = italic = underlined = strike = obf = false; }
                    default -> known = false;
                }
                if (known) { i++; continue; }
            }
            out.add(new Cell(cp, bold, italic, underlined, strike, obf, rgb));
        }
        return out.toArray(new Cell[0]);
    }

    private static int hex(int[] cps, int from) {
        if (from + 6 > cps.length) return -1;
        int v = 0;
        for (int i = from; i < from + 6; i++) {
            int d = Character.digit(cps[i], 16);
            if (d < 0) return -1;
            v = v * 16 + d;
        }
        return v;
    }
}
