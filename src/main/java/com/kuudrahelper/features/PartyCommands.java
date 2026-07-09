package com.kuudrahelper.features;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.profile.AutoKickManager;
import com.kuudrahelper.features.profile.KuudraProfileFetcher;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

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
    private static final Pattern YOU_JOINED = Pattern.compile(
            "You have joined (?:\\[[^\\]]+\\] )?(\\w+)'s? party!|You accepted (?:\\[[^\\]]+\\] )?(\\w+)'s? party invitation!");
    private static final Pattern YOU_LEFT_OR_KICKED = Pattern.compile(
            "^You left the party\\.$|^You are not currently in a party\\.$|^You have been kicked from the party by (?:\\[[^\\]]+\\] )?\\w+$");
    private static final Pattern PARTY_TRANSFER = Pattern.compile(
            "^The party was transferred to (?:\\[[^\\]]+\\] )?(\\w+) (?:by|because) (?:\\[[^\\]]+\\] )?\\w+(?: left)?$");
    private static final Pattern PARTY_INVITE = Pattern.compile(
            "^(?:\\[[^\\]]+\\] )?(\\w+) invited (?:\\[[^\\]]+\\] )?\\w+ to the party! They have 60 seconds to accept\\.$");
    private static final Pattern PARTY_LIST_SECTION = Pattern.compile(
            "^Party (Leader|Moderators|Members): (.+)$");
    private static final Pattern PARTY_LIST_NAME = Pattern.compile(
            "^(?:\\[[^\\]]+\\] )?(\\w+)$");
    private static final String PARTY_LIST_DELIM = " ●";
    private static final Pattern PARTY_FINDER_JOIN = Pattern.compile(
            "^Party Finder > (?:\\[[^\\]]+\\] )?(\\w+) joined the group! \\(Combat Level \\d+\\)$");

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
    private static String partyLeader = null;

    private PartyCommands() {}

    public static void reset() {
        partyMembers.clear();
        partyLeader = null;
        lastAnnounceMs = 0L;
    }

    public static boolean isPartyLeader() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || partyLeader == null) return false;
        return partyLeader.equalsIgnoreCase(mc.player.getScoreboardName());
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
        Matcher youJoined = YOU_JOINED.matcher(raw);
        if (youJoined.find()) {
            partyMembers.clear();
            partyLeader = youJoined.group(1) != null ? youJoined.group(1) : youJoined.group(2);
            com.kuudrahelper.features.splits.KuudraSplitTimer.resetPartySession();
            return;
        }

        if (YOU_LEFT_OR_KICKED.matcher(raw).find()) {
            partyMembers.clear();
            partyLeader = null;
            return;
        }

        Matcher join = PARTY_JOIN.matcher(raw);
        if (join.find()) { partyMembers.add(join.group(1)); return; }

        Matcher finderJoin = PARTY_FINDER_JOIN.matcher(raw);
        if (finderJoin.find()) {
            String joined = finderJoin.group(1);
            partyMembers.add(joined);
            ShitterList.checkAutoKick(joined);
            onPartyFinderJoin(joined);
            return;
        }

        Matcher leave = PARTY_LEAVE.matcher(raw);
        if (leave.find()) { partyMembers.remove(leave.group(1)); return; }

        Matcher transfer = PARTY_TRANSFER.matcher(raw);
        if (transfer.find()) { partyLeader = transfer.group(1); return; }

        Matcher invite = PARTY_INVITE.matcher(raw);
        if (invite.find()) {
            String inviter = invite.group(1);
            partyMembers.add(inviter);
            if (partyLeader == null) partyLeader = inviter;
            return;
        }

        Matcher listSection = PARTY_LIST_SECTION.matcher(raw);
        if (listSection.find()) {
            boolean isLeaderSection = listSection.group(1).equals("Leader");
            for (String token : listSection.group(2).split(PARTY_LIST_DELIM)) {
                Matcher name = PARTY_LIST_NAME.matcher(token.trim());
                if (!name.find()) continue;
                partyMembers.add(name.group(1));
                if (isLeaderSection) partyLeader = name.group(1);
            }
            return;
        }

        if (PARTY_DISBAND.matcher(raw).find()) {
            partyMembers.clear();
            partyLeader = null;
            com.kuudrahelper.features.splits.KuudraSplitTimer.resetPartySession();
            return;
        }

        Matcher m = PARTY_MSG.matcher(raw);
        if (!m.find()) return;

        String sender = m.group(1);
        String body   = m.group(2).trim();

        partyMembers.add(sender);

        if (!KuudraConfig.isPartyCmdsEnabled()) return;

        String[] words = body.split("\\s+");
        String cmd   = words[0].toLowerCase();
        String word2 = words.length > 1 ? words[1] : "";

        if (cmd.equals("!w") || cmd.equals("!warp")) {
            send("p warp");
            return;
        }

        if (cmd.equals("!allinv") || cmd.equals("!allinvite")) {
            send("p settings allinvite");
            return;
        }

        if (cmd.equals("!inv") || cmd.equals("!invite")) {
            if (!word2.isEmpty()) send("p " + word2);
            return;
        }

        if (cmd.equals("!pt") || cmd.equals("!ptme")) {
            String target = resolveOrSelf(word2, sender);
            if (target != null) send("p transfer " + target);
            return;
        }

        for (Map.Entry<String, String> e : TIERS.entrySet()) {
            if (cmd.equals("!" + e.getKey())) {
                send("joininstance " + e.getValue());
                return;
            }
        }

        if (cmd.equals("!k") || cmd.equals("!kick")) {
            String target = resolve(word2);
            if (target != null) send("p kick " + target);
            return;
        }

        if (cmd.equals("!chests")) {
            if (!canAnnounce()) return;
            int total   = ChestTracker.getTotal();
            int success = ChestTracker.getSuccess();
            int fail    = ChestTracker.getFail();
            int left    = Math.max(0, 60 - total);
            send(String.format("pc Chests: %d/60 (%d:%d) | | %d runs left",
                    total, success, fail, left));
            return;
        }

        if (cmd.equals("!dt")) {
            KuudraConfig.setAutoRequeueEnabled(false);
            if (KuudraConfig.isAutoRequeueMessageEnabled() && isPartyLeader()) {
                send("pc [Phantom] Auto Requeue: OFF");
            }
            return;
        }
        if (cmd.equals("!undt")) {
            KuudraConfig.setAutoRequeueEnabled(true);
            if (KuudraConfig.isAutoRequeueMessageEnabled() && isPartyLeader()) {
                send("pc [Phantom] Auto Requeue: ON");
            }
            return;
        }

        if (cmd.equals("!pb")) {
            if (!canAnnounce()) return;
            int    tier = KuudraConfig.getHighestTierPlayed();
            double pb   = KuudraConfig.getTotalRunPb(tier);
            if (pb >= 9999) return;
            send("pc T" + tier + " Kuudra PB: " + KuudraConfig.formatTime(pb));
            return;
        }

        if (cmd.equals("!avg") || cmd.equals("!average")) {
            if (!canAnnounce()) return;
            int tier = com.kuudrahelper.features.splits.KuudraSplitTimer.getSessionHighestTier();
            if (tier < 1) return;
            double avg = com.kuudrahelper.features.splits.KuudraSplitTimer.getSessionAverage(tier);
            if (avg < 0) return;
            send("pc [Phantom] Party Average: " + formatAverage(avg));
        }
    }

    private static String formatAverage(double seconds) {
        if (seconds < 60.0) {
            return String.format("%.2fs", seconds);
        }
        long rounded = Math.round(seconds);
        long mins = rounded / 60;
        long secs = rounded % 60;
        return mins + ":" + String.format("%02d", secs);
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
        return null;
    }

    private static String resolveOrSelf(String arg, String sender) {
        if (arg.isEmpty()) return sender;
        return resolve(arg);
    }

    /** Prefetches the joiner's Kuudra profile (so a later /kuudra open is instant), evaluates
     *  the stat-based Auto Kick feature against it, and posts a clickable profile link. */
    private static void onPartyFinderJoin(String name) {
        if (!KuudraConfig.isProfileViewerEnabled()) return;
        KuudraProfileFetcher.fetchAsync(name, data -> AutoKickManager.evaluate(name, data));
        announceProfileLink(name);
    }

    private static void announceProfileLink(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        MutableComponent msg = Component.literal("§f[PhantomAddons]§r §bView Profile for §e" + name + "§b (click)")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.RunCommand("/kuudra " + name))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7Click to view §b" + name + "§7's Kuudra profile"))));
        mc.player.sendSystemMessage(msg);
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
