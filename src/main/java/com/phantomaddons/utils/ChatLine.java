package com.phantomaddons.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatLine {

    private static final String NAME = "[A-Za-z0-9_]{3,16}";
    private static final String RANK = "(?:\\[[^\\]]{1,30}\\] )?";

    private static final Pattern PARTY = Pattern.compile("^Party > " + RANK + "(" + NAME + "): (.+)$");
    private static final Pattern RECOVERED = Pattern.compile(
            "^" + RANK + "(" + NAME + ") recovered (?:a supply|one of Elle's supplies)(?:!(?: \\((\\d+/\\d+)\\))?)?",
            Pattern.CASE_INSENSITIVE);

    public record Party(String sender, String body) {}

    public record Recovery(String name, String count) {}

    private ChatLine() {}

    public static boolean isServer(String clean) {
        return clean != null && clean.indexOf(':') < 0;
    }

    public static Party party(String clean) {
        if (clean == null) return null;
        Matcher m = PARTY.matcher(clean.strip());
        return m.matches() ? new Party(m.group(1), m.group(2).strip()) : null;
    }

    public static boolean isNpc(String clean, String npc) {
        return clean != null && clean.strip().startsWith("[NPC] " + npc + ": ");
    }

    public static Recovery recovered(String clean) {
        if (!isServer(clean)) return null;
        Matcher m = RECOVERED.matcher(clean.strip());
        return m.lookingAt() ? new Recovery(m.group(1), m.group(2)) : null;
    }
}
