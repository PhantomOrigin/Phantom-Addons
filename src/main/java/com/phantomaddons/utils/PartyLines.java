package com.phantomaddons.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PartyLines {

    public enum Kind {
        NONE, YOU_JOINED, YOU_LEFT, JOINED, FINDER_JOINED, LEFT, TRANSFER, TRANSFER_LEFT, INVITE, YOU_INVITED,
        SELF_LEADER, LEADER_NAMED, NOT_LEADER, MEMBERS, LIST, DISBAND, CHAT
    }

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
            "^" + RANK + NAME + " (?:has left|left|was removed from|has been removed from) the party\\.");
    private static final Pattern KICKED_OFFLINE = Pattern.compile(
            "^Kicked " + RANK + NAME + " because they were offline\\.$");
    private static final Pattern REMOVED_DISCONNECTED = Pattern.compile(
            "^" + RANK + NAME + " was removed from your party because they disconnected\\.$");
    private static final Pattern TRANSFER_BY = Pattern.compile(
            "^The party was transferred to " + RANK + NAME + " by " + RANK + "[A-Za-z0-9_]{3,16}$");
    private static final Pattern TRANSFER_LEFT = Pattern.compile(
            "^The party was transferred to " + RANK + NAME + " because " + RANK + NAME + " left$");
    private static final Pattern YOU_INVITED = Pattern.compile(
            "^You invited " + RANK + "[A-Za-z0-9_]{3,16} to your party! They have 60 seconds to accept\\.$");
    private static final Pattern REQUEUE_PROMPT = Pattern.compile("^Click HERE to re-queue into .+!$");
    private static final Pattern FINDER_QUEUED = Pattern.compile(
            "^Party Finder > Your party has been queued in the dungeon finder!$");
    private static final Pattern ENTERED = Pattern.compile(
            "^" + RANK + NAME + " entered Kuudra's Hollow, [A-Za-z]+ Tier!$");
    private static final Pattern LEADER_DISCONNECTED = Pattern.compile(
            "^The party leader, " + RANK + NAME + " has disconnected, they have 5 minutes to rejoin before the party is disbanded\\.$");
    private static final Pattern LEADER_REJOINED = Pattern.compile(
            "^The party leader " + RANK + NAME + " has rejoined\\.$");
    private static final Pattern NOT_LEADER = Pattern.compile("^You are not this party's leader[.!]?$");
    private static final Pattern PARTYING_WITH = Pattern.compile("^You'll be partying with: (.+)$");
    private static final Pattern DISBANDED_BY = Pattern.compile("^" + RANK + NAME + " has disbanded the party!$");
    private static final Pattern INVITE = Pattern.compile(
            "^" + RANK + NAME + " invited " + RANK + "[A-Za-z0-9_]{3,16} to the party! They have 60 seconds to accept\\.$");
    private static final Pattern DISBAND = Pattern.compile("^The party was disbanded");
    private static final Pattern LIST_SECTION = Pattern.compile("^Party (Leader|Moderators|Members): (.+)$");
    private static final Pattern LIST_NAME = Pattern.compile("^" + RANK + NAME + "$");
    private static final String LIST_DELIM = " ●";
    private static final String MEMBERS_DELIM = ", ";

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

        Matcher with = PARTYING_WITH.matcher(s);
        if (with.matches()) {
            List<String> names = new ArrayList<>();
            for (String token : with.group(1).split(MEMBERS_DELIM)) {
                Matcher n = LIST_NAME.matcher(token.strip());
                if (n.matches()) names.add(n.group(1));
            }
            return new Event(Kind.MEMBERS, null, names, false, null);
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
        m = KICKED_OFFLINE.matcher(s);
        if (m.matches()) return Event.of(Kind.LEFT, m.group(1));
        m = REMOVED_DISCONNECTED.matcher(s);
        if (m.matches()) return Event.of(Kind.LEFT, m.group(1));
        m = TRANSFER_BY.matcher(s);
        if (m.matches()) return Event.of(Kind.TRANSFER, m.group(1));
        m = TRANSFER_LEFT.matcher(s);
        if (m.matches()) return new Event(Kind.TRANSFER_LEFT, m.group(1), List.of(m.group(2)), false, null);
        m = INVITE.matcher(s);
        if (m.matches()) return Event.of(Kind.INVITE, m.group(1));
        if (YOU_INVITED.matcher(s).matches()) return Event.of(Kind.YOU_INVITED, null);
        if (REQUEUE_PROMPT.matcher(s).matches() || FINDER_QUEUED.matcher(s).matches()) return Event.of(Kind.SELF_LEADER, null);
        m = ENTERED.matcher(s);
        if (m.matches()) return Event.of(Kind.LEADER_NAMED, m.group(1));
        m = LEADER_DISCONNECTED.matcher(s);
        if (m.matches()) return Event.of(Kind.LEADER_NAMED, m.group(1));
        m = LEADER_REJOINED.matcher(s);
        if (m.matches()) return Event.of(Kind.LEADER_NAMED, m.group(1));
        if (NOT_LEADER.matcher(s).matches()) return Event.of(Kind.NOT_LEADER, null);
        if (DISBAND.matcher(s).find() || DISBANDED_BY.matcher(s).matches()) return Event.of(Kind.DISBAND, null);
        return Event.NONE;
    }
}
