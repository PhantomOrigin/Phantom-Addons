package com.kuudrahelper.features;

import com.kuudrahelper.KuudraConfig;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PartyCommands {

    private static final Pattern PARTY_MSG = Pattern.compile(
            "Party > (?:\\[[^\\]]+\\] )?(\\w+): (.+)");
    private static final Pattern PARTY_JOIN = Pattern.compile(
            "(?:\\[[^\\]]+\\] )?(\\w+) joined the party\\.");
    private static final Pattern PARTY_LEAVE = Pattern.compile(
            "(?:\\[[^\\]]+\\] )?(\\w+) (?:left|was removed from|has been removed from) the party\\.");
    private static final Pattern PARTY_DISBAND = Pattern.compile(
            "The party was disbanded");

    private static final Map<String, String> TIERS = Map.of(
            "t1", "KUUDRA_NORMAL",
            "t2", "KUUDRA_HOT",
            "t3", "KUUDRA_BURNING",
            "t4", "KUUDRA_FIERY",
            "t5", "KUUDRA_INFERNAL"
    );

    private static final long ANNOUNCE_COOLDOWN_MS = 5_000L;
    private static long lastAnnounceMs = 0L;

    private static final Set<String> partyMembers = new LinkedHashSet<>();

    private PartyCommands() {}

    public static void reset() {
        partyMembers.clear();
        lastAnnounceMs = 0L;
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            handle(message.getString().replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim());
        });
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, ts) -> {
            handle(message.getString().replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim());
        });
    }

    private static void handle(String raw) {
        Matcher join = PARTY_JOIN.matcher(raw);
        if (join.find()) { partyMembers.add(join.group(1)); return; }

        Matcher leave = PARTY_LEAVE.matcher(raw);
        if (leave.find()) { partyMembers.remove(leave.group(1)); return; }

        if (PARTY_DISBAND.matcher(raw).find()) { partyMembers.clear(); return; }

        Matcher m = PARTY_MSG.matcher(raw);
        if (!m.find()) return;

        String sender = m.group(1);
        String body   = m.group(2).trim();

        partyMembers.add(sender);

        if (!KuudraConfig.isPartyCmdsEnabled()) return;

        String lower = body.toLowerCase();

        if (lower.equals("!w") || lower.equals("!warp")) {
            send("p warp");
            return;
        }

        if (lower.equals("!pt") || lower.equals("!ptme")) {
            send("p transfer " + sender);
            return;
        }

        if (lower.startsWith("!pt ")) {
            String arg = body.substring(4).trim();
            String target = resolveOrSelf(arg, sender);
            if (target != null) send("p transfer " + target);
            return;
        }

        for (Map.Entry<String, String> e : TIERS.entrySet()) {
            if (lower.equals("!" + e.getKey())) {
                send("joininstance " + e.getValue());
                return;
            }
        }

        if (lower.startsWith("!k ") || lower.startsWith("!kick ")) {
            int sp = body.indexOf(' ');
            String arg = body.substring(sp + 1).trim();
            String target = resolve(arg);
            if (target != null) send("p kick " + target);
            return;
        }

        if (lower.equals("!chests")) {
            if (!canAnnounce()) return;
            int total   = ChestTracker.getTotal();
            int success = ChestTracker.getSuccess();
            int fail    = ChestTracker.getFail();
            int left    = Math.max(0, 60 - total);
            send(String.format("pc Chests: %d/60 (%d:%d) | | %d runs left",
                    total, success, fail, left));
            return;
        }

        if (lower.equals("!dt")) {
            KuudraConfig.setAutoRequeueEnabled(false);
            send("pc [Phantom] Auto Requeue: OFF");
            return;
        }
        if (lower.equals("!undt")) {
            KuudraConfig.setAutoRequeueEnabled(true);
            send("pc [Phantom] Auto Requeue: ON");
            return;
        }

        if (lower.equals("!pb")) {
            if (!canAnnounce()) return;
            int    tier = KuudraConfig.getHighestTierPlayed();
            double pb   = KuudraConfig.getTotalRunPb(tier);
            if (pb >= 9999) return;
            send("pc T" + tier + " Kuudra PB: " + KuudraConfig.formatTime(pb));
        }
    }

    private static String resolve(String input) {
        if (input.isEmpty()) return null;
        String lc = input.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String member : partyMembers) {
            if (member.toLowerCase().startsWith(lc)) matches.add(member);
        }
        if (matches.size() == 1) return matches.get(0);
        for (String member : partyMembers) {
            if (member.equalsIgnoreCase(input)) return member;
        }
        if (partyMembers.isEmpty()) return input;
        return null; // ambiguous
    }

    private static String resolveOrSelf(String arg, String sender) {
        if (arg.isEmpty()) return sender;
        return resolve(arg);
    }

    private static void send(String cmd) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        mc.execute(() -> mc.getConnection().sendCommand(cmd));
    }

    private static boolean canAnnounce() {
        long now = System.currentTimeMillis();
        if (now - lastAnnounceMs < ANNOUNCE_COOLDOWN_MS) return false;
        lastAnnounceMs = now;
        return true;
    }
}
