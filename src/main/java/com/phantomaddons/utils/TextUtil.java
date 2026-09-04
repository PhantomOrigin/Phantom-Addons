package com.phantomaddons.utils;

import java.util.regex.Pattern;

public final class TextUtil {

    private static final Pattern COLOR_CODES = Pattern.compile("§[0-9a-fk-orA-FK-OR]");

    private TextUtil() {}

    public static String stripColor(String s) {
        if (s == null || s.isEmpty()) return s == null ? "" : s;
        if (s.indexOf('§') < 0) return s; // overwhelmingly the common case — skip the regex entirely
        return COLOR_CODES.matcher(s).replaceAll("");
    }

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
