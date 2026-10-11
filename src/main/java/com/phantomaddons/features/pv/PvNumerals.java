package com.phantomaddons.features.pv;

import com.phantomaddons.PhantomConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PvNumerals {

    private PvNumerals() {}

    private static final Pattern ROMAN = Pattern.compile(
            "(?<![\\w'§])(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})(?![\\w'])");

    static boolean enabled() { return PhantomConfig.isPvArabicNumerals(); }

    static String convert(String text) {
        if (text == null || text.isEmpty()) return text;
        Matcher m = ROMAN.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String token = m.group();
            boolean pronoun = token.equals("I") && text.length() > m.end() + 1
                    && text.charAt(m.end()) == ' ' && Character.isLowerCase(text.charAt(m.end() + 1));
            m.appendReplacement(out, token.isEmpty() || pronoun ? token : String.valueOf(value(token)));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static int value(String roman) {
        int total = 0, prev = 0;
        for (int i = roman.length() - 1; i >= 0; i--) {
            int v = switch (roman.charAt(i)) { case 'I' -> 1; case 'V' -> 5; case 'X' -> 10; case 'L' -> 50; default -> 0; };
            total += v < prev ? -v : v;
            prev = Math.max(prev, v);
        }
        return total;
    }

    static List<Component> convert(List<Component> lines) {
        List<Component> out = new ArrayList<>(lines.size());
        for (Component line : lines) {
            MutableComponent rebuilt = Component.empty();
            line.visit((Style style, String text) -> {
                rebuilt.append(Component.literal(convert(text)).withStyle(style));
                return Optional.empty();
            }, Style.EMPTY);
            out.add(rebuilt);
        }
        return out;
    }
}
