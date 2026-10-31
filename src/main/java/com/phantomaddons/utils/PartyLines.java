package com.phantomaddons.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PartyLines {

    public enum Kind { NONE, YOU_JOINED, YOU_LEFT, JOINED, FINDER_JOINED, LEFT, TRANSFER, INVITE, LIST, DISBAND, CHAT }

    public record Event(Kind kind, String name, List<String> names, boolean leaderSection, String body) {
        static final Event NONE = new Event(Kind.NONE, null, List.of(), false, null);
        static Event of(Kind kind, String name) { return new Event(kind, name, List.of(), false, null); }
    }

    private static final String NAME = "([A-Za-z0-9_]{3,16})";
    private static final String RANK = "(?:\\[[^\\]]{1,30}\\] )?";

    private static final Pattern YOU_JOINED = Pattern.compile(
            "^You have joined " + RANK + NAME + "'s? party!|^You accepted " + RANK + NAME + "'s? party invitation!");
    private static final Pattern YOU_LEFT = Pattern.compile(
            "^You left the party\\.$|^You are not currently in a party\\.$|^You have been kicked from the party by " + RANK + "[A-Za-z0-9_]{3,16}$");
    private static final Pattern JOINED = Pattern.compile("^" + RANK + NAME + " joined the party\\.");
    private static final Pattern FINDER_JOINED = Pattern.compile(
            "^Party Finder > " + RANK + NAME + " joined the group! \\(Combat Level \\d+\\)$");
    private static final Pattern LEFT = Pattern.compile(
            "^" + RANK + NAME + " (?:left|was removed from|has been removed from) the party\\.");
    private static final Pattern TRANSFER = Pattern.compile(
            "^The party was transferred to " + RANK + NAME + " (?:by|because) " + RANK + "[A-Za-z0-9_]{3,16}(?: left)?$");
    private static final Pattern INVITE = Pattern.compile(
            "^" + RANK + NAME + " invited " + RANK + "[A-Za-z0-9_]{3,16} to the party! They have 60 seconds to accept\\.$");
    private static final Pattern DISBAND = Pattern.compile("^The party was disbanded");
    private static final Pattern LIST_SECTION = Pattern.compile("^Party (Leader|Moderators|Members): (.+)$");
    private static final Pattern LIST_NAME = Pattern.compile("^" + RANK + NAME + "$");
    private static final String LIST_DELIM = " ●";

    private PartyLines() {}

    public static Event parse(String raw) {
        if (raw == null) return Event.NONE;
        String s = raw.strip();

        ChatLine.Party chat = ChatLine.party(s);
        if (chat != null) return new Event(Kind.CHAT, chat.sender(), List.of(), false, chat.body());

        Matcher list = LIST_SECTION.matcher(s);
        if (list.matches()) {
            List<String> names = new ArrayList<>();
            for (String token : list.group(2).split(LIST_DELIM)) {
                Matcher n = LIST_NAME.matcher(token.strip());
                if (n.matches()) names.add(n.group(1));
            }
            return new Event(Kind.LIST, null, names, list.group(1).equals("Leader"), null);
        }

        if (!ChatLine.isServer(s)) return Event.NONE;

        Matcher m = YOU_JOINED.matcher(s);
        if (m.lookingAt()) return Event.of(Kind.YOU_JOINED, m.group(1) != null ? m.group(1) : m.group(2));
        if (YOU_LEFT.matcher(s).matches()) return Event.of(Kind.YOU_LEFT, null);
        m = JOINED.matcher(s);
        if (m.lookingAt()) return Event.of(Kind.JOINED, m.group(1));
        m = FINDER_JOINED.matcher(s);
        if (m.matches()) return Event.of(Kind.FINDER_JOINED, m.group(1));
        m = LEFT.matcher(s);
        if (m.lookingAt()) return Event.of(Kind.LEFT, m.group(1));
        m = TRANSFER.matcher(s);
        if (m.matches()) return Event.of(Kind.TRANSFER, m.group(1));
        m = INVITE.matcher(s);
        if (m.matches()) return Event.of(Kind.INVITE, m.group(1));
        if (DISBAND.matcher(s).find()) return Event.of(Kind.DISBAND, null);
        return Event.NONE;
    }
}
