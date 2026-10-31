package com.phantomaddons.features.misckuudra;

import com.phantomaddons.utils.TextUtil;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.misckuudra.chesttracking.ChestTracker;
import com.phantomaddons.features.misckuudra.profile.AutoKickManager;
import com.phantomaddons.features.misckuudra.profile.KuudraProfileFetcher;
import com.phantomaddons.utils.PartyLines;
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

    private static long lastLeaderQueryMs = 0L;
    private static final long LEADER_QUERY_COOLDOWN_MS = 3_000L;

    private static void queryLeaderIfUnknown() {
        if (partyLeader != null) return;
        long now = System.currentTimeMillis();
        if (now - lastLeaderQueryMs < LEADER_QUERY_COOLDOWN_MS) return;
        lastLeaderQueryMs = now;
        com.phantomaddons.features.supplies.PartyChatQueue.sendCommand("party list");
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            handle(TextUtil.stripColor(message.getString()).trim());
        });
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, ts) -> {
            handle(TextUtil.stripColor(message.getString()).trim());
        });
    }

    private static void handle(String raw) {
        PartyLines.Event event = PartyLines.parse(raw);
        switch (event.kind()) {
            case YOU_JOINED -> {
                partyMembers.clear();
                partyLeader = event.name();
                com.phantomaddons.features.misckuudra.splits.KuudraSplitTimer.resetPartySession();
                return;
            }
            case YOU_LEFT -> {
                partyMembers.clear();
                partyLeader = null;
                return;
            }
            case JOINED -> {
                partyMembers.add(event.name());
                queryLeaderIfUnknown();
                ShitterList.checkAutoKick(event.name());
                return;
            }
            case FINDER_JOINED -> {
                partyMembers.add(event.name());
                queryLeaderIfUnknown();
                ShitterList.checkAutoKick(event.name());
                onPartyFinderJoin(event.name());
                return;
            }
            case LEFT -> {
                partyMembers.remove(event.name());
                return;
            }
            case TRANSFER -> {
                partyLeader = event.name();
                return;
            }
            case INVITE -> {
                partyMembers.add(event.name());
                if (partyLeader == null) partyLeader = event.name();
                return;
            }
            case LIST -> {
                for (String name : event.names()) {
                    partyMembers.add(name);
                    if (event.leaderSection()) partyLeader = name;
                }
                return;
            }
            case DISBAND -> {
                partyMembers.clear();
                partyLeader = null;
                com.phantomaddons.features.misckuudra.splits.KuudraSplitTimer.resetPartySession();
                return;
            }
            case NONE -> {
                return;
            }
            case CHAT -> { }
        }

        String sender = event.name();
        String body   = event.body();

        partyMembers.add(sender);

        if (!PhantomConfig.isPartyCmdsEnabled()) return;

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
            AutoRequeue.setTempDisabled(true);
            if (PhantomConfig.isAutoRequeueMessageEnabled() && isPartyLeader()) {
                send("pc [Phantom] Auto Requeue: OFF");
            }
            return;
        }
        if (cmd.equals("!undt")) {
            AutoRequeue.setTempDisabled(false);
            if (PhantomConfig.isAutoRequeueMessageEnabled() && isPartyLeader()) {
                send("pc [Phantom] Auto Requeue: ON");
            }
            return;
        }

        if (cmd.equals("!pb")) {
            if (!canAnnounce()) return;
            int    tier = PhantomConfig.getHighestTierPlayed();
            double pb   = PhantomConfig.getTotalRunPb(tier);
            if (pb >= 9999) return;
            send("pc T" + tier + " Kuudra PB: " + PhantomConfig.formatTime(pb));
            return;
        }

        if (cmd.equals("!avg") || cmd.equals("!average")) {
            if (!canAnnounce()) return;
            String scope = word2.toLowerCase();

            String label;
            int tier;
            double avg;
            int runs;
            if (scope.equals("session")) {
                label = "Session";
                tier  = com.phantomaddons.features.misckuudra.profittracker.ProfitStore.getSessionHighestTier();
                avg   = tier < 1 ? -1 : com.phantomaddons.features.misckuudra.profittracker.ProfitStore.getSessionRunTimeAverage(tier);
                runs  = tier < 1 ? 0 : com.phantomaddons.features.misckuudra.profittracker.ProfitStore.getSessionRunTimeCount(tier);
            } else if (scope.equals("total")) {
                label = "Total";
                tier  = PhantomConfig.getTotalAverageHighestTier();
                avg   = tier < 1 ? -1 : PhantomConfig.getTotalAverage(tier);
                runs  = tier < 1 ? 0 : PhantomConfig.getTotalRunCount(tier);
            } else {
                label = "Party";
                tier  = com.phantomaddons.features.misckuudra.splits.KuudraSplitTimer.getSessionHighestTier();
                avg   = tier < 1 ? -1 : com.phantomaddons.features.misckuudra.splits.KuudraSplitTimer.getSessionAverage(tier);
                runs  = tier < 1 ? 0 : com.phantomaddons.features.misckuudra.splits.KuudraSplitTimer.getSessionRunCount(tier);
            }
            if (avg < 0) return;
            send("pc [Phantom] " + label + " Average: " + formatAverage(avg) + " (" + runs + " runs)");
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

    private static void onPartyFinderJoin(String name) {
        boolean profileViewerOn = PhantomConfig.isProfileViewerEnabled();
        boolean autoKickOn = PhantomConfig.isAutoKickEnabled() && isPartyLeader();
        if (!profileViewerOn && !autoKickOn) return;

        if (autoKickOn) KuudraProfileFetcher.fetchAsync(name, data -> AutoKickManager.evaluate(name, data));
        if (profileViewerOn) announceProfileLink(name);
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
