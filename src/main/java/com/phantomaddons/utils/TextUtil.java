package com.phantomaddons.utils;

import java.util.regex.Pattern;

// String.replaceAll compiles a fresh Pattern on every call, and colour-stripping happens in hot
// paths (per chat message, per rendered GUI frame, per scoreboard line). Compiling once here and
// reusing the Matcher keeps those paths allocation-light.
public final class TextUtil {

    private static final Pattern COLOR_CODES = Pattern.compile("§[0-9a-fk-orA-FK-OR]");

    private TextUtil() {}

    public static String stripColor(String s) {
        if (s == null || s.isEmpty()) return s == null ? "" : s;
        if (s.indexOf('§') < 0) return s; // overwhelmingly the common case — skip the regex entirely
        return COLOR_CODES.matcher(s).replaceAll("");
    }

    // Allocation-free case-insensitive search. Takes a CharSequence so callers can test a
    // StringBuilder directly without materialising it, and avoids the toLowerCase() copy that
    // `a.toLowerCase().contains(b)` would make on every call.
    public static boolean containsIgnoreCase(CharSequence haystack, String needle) {
        int needleLen = needle.length();
        if (needleLen == 0) return true;
        int limit = haystack.length() - needleLen;
        outer:
        for (int i = 0; i <= limit; i++) {
            for (int j = 0; j < needleLen; j++) {
                char a = haystack.charAt(i + j);
                char b = needle.charAt(j);
                if (a != b && Character.toLowerCase(a) != Character.toLowerCase(b)) continue outer;
            }
            return true;
        }
        return false;
    }
}
