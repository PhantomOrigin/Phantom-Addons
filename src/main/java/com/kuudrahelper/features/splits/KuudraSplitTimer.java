package com.kuudrahelper.features.splits;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import com.kuudrahelper.utils.KuudraTierDetector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KuudraSplitTimer {

    public enum Split { SUPPLIES, BUILD, EATEN, STUN, DPS, SKIP, BOSS }
    public static final int N = Split.values().length;

    public static final double[] BEST_THEORETICAL = {19.80, 13.20, 4.20, 0.00, 3.60, 4.60, 1.80};
    private static final double LAG_THRESHOLD = 0.05;

    public record PhaseResult(double wallSec, double tickSec) {
        public double lagSec() { return wallSec - tickSec; }
    }

    public record SupplyTime(String player, double timeSec) {}

    private static long runStartMs = -1;
    private static long runEndMs = -1;
    private static long phaseStartMs = -1;
    private static int phaseStartTick = 0;
    private static int globalTick = 0;
    private static Split activeSplit = null;

    private static final EnumMap<Split, PhaseResult> results = new EnumMap<>(Split.class);
    private static final List<SupplyTime> supplyTimes = new ArrayList<>();

    private static Player stunPlayer = null;
    private static Vec3 lastStunPos = null;
    private static boolean stunPlayerLoaded = false;
    private static boolean inEatenPhase = false;

    private static final Map<UUID, Vec3> playerLastPositions = new HashMap<>();

    private static final List<KuudraConfig.PlayerTime> runSupplyTimes = new ArrayList<>();
    private static final List<KuudraConfig.PlayerTime> runFreshTimes = new ArrayList<>();

    private static final Map<Integer, List<Double>> sessionRunTimes = new HashMap<>();

    private static boolean pendingAnnounce = false;

    private static final Pattern FRESH_PAT =
            Pattern.compile("Party > (?:\\[.*?\\] )?([A-Za-z0-9_]+): FRESH!(?: \\((\\d+)%\\))?");
    private static final Pattern RECOVERED =
            Pattern.compile("(\\S+)\\s+recovered (?:a supply|one of Elle's supplies)", Pattern.CASE_INSENSITIVE);

    private KuudraSplitTimer() {}

    public static void register() {

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());

        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            if (overlay) return;

            com.kuudrahelper.features.pearls.SupplyTracker.onChat(msg.getString());

            if (runStartMs <= 0) return;

            String text = msg.getString().replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();

            if (activeSplit == Split.SUPPLIES && !com.kuudrahelper.KuudraConfig.isSupplyRecoveryMsgEnabled()) {
                Matcher rm = RECOVERED.matcher(text);
                if (rm.find()) {
                    String player = rm.group(1).trim();
                    double elapsed = (System.currentTimeMillis() - runStartMs) / 1000.0;
                    supplyTimes.add(new SupplyTime(player, elapsed));
                    runSupplyTimes.add(new KuudraConfig.PlayerTime(player, elapsed));
                }
            }

            if (text.contains("Party > ") && text.contains(": FRESH!")) {
                Matcher fm = FRESH_PAT.matcher(text);
                if (fm.find() && activeSplit == Split.BUILD && phaseStartMs >= 0) {
                    double elapsed = (System.currentTimeMillis() - phaseStartMs) / 1000.0;
                    int pct = fm.group(2) != null
                            ? Integer.parseInt(fm.group(2))
                            : com.kuudrahelper.features.BuildProgressHud.getCurrentProgress();
                    runFreshTimes.add(new KuudraConfig.PlayerTime(fm.group(1), elapsed, pct));
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            globalTick++;

            if (pendingAnnounce) {
                pendingAnnounce = false;
                printRunSummary();
            }

            if (client.level == null || client.player == null) return;

            tickPlayerDetection(client);
        });
    }

    public static void reset() {
        if (pendingAnnounce) {
            pendingAnnounce = false;
            printRunSummary();
        }
        activeSplit = null;
        inEatenPhase = false;
        stunPlayer = null;
        lastStunPos = null;
        runStartMs = -1;
        runEndMs = -1;
        phaseStartMs = -1;
        phaseStartTick = 0;

        results.clear();
        supplyTimes.clear();
        runSupplyTimes.clear();
        runFreshTimes.clear();
        playerLastPositions.clear();
    }

    public static void resetPartySession() {
        sessionRunTimes.clear();
    }

    public static int getSessionHighestTier() {
        int best = -1;
        for (int tier : sessionRunTimes.keySet()) {
            if (!sessionRunTimes.get(tier).isEmpty() && tier > best) best = tier;
        }
        return best;
    }

    public static double getSessionAverage(int tier) {
        List<Double> times = sessionRunTimes.get(tier);
        if (times == null || times.isEmpty()) return -1;
        return times.stream().mapToDouble(Double::doubleValue).average().orElse(-1);
    }

    public static void onSuppliesStart() {
        runSupplyTimes.clear();
        runFreshTimes.clear();
        results.clear();
        supplyTimes.clear();
        stunPlayer = null;
        lastStunPos = null;
        inEatenPhase = false;
        playerLastPositions.clear();

        runStartMs = System.currentTimeMillis();
        runEndMs = -1;

        beginSplit(Split.SUPPLIES);
    }

    public static void onBuildStart() {
        endSplit(Split.SUPPLIES);
        beginSplit(Split.BUILD);
    }

    public static void onEatenStart() {
        endSplit(Split.BUILD);
        beginSplit(Split.EATEN);
        inEatenPhase = true;
    }

    public static void onStunStart() {
        endEatenSplit();
    }

    public static void onDpsStart() {
        endSplit(Split.STUN);
        beginSplit(Split.DPS);
    }

    public static void onSkipStart() {
        endSplit(Split.DPS);
        beginSplit(Split.SKIP);
        playerLastPositions.clear();
    }

    public static void onBossStart() {
        int tier = KuudraTierDetector.getTier();
        if (tier == 1 || tier == 2) {
            endSplit(Split.BUILD);
        } else {
            endSplit(Split.SKIP);
        }
        beginSplit(Split.BOSS);
        playerLastPositions.clear();
    }

    public static void onEndStart() {
        if (activeSplit == Split.BOSS) endSplit(Split.BOSS);
        else if (activeSplit == Split.SKIP) endSplit(Split.SKIP);
        else if (activeSplit == Split.DPS) endSplit(Split.DPS);

        activeSplit = null;
        inEatenPhase = false;
        runEndMs = System.currentTimeMillis();
        playerLastPositions.clear();

        if (runStartMs > 0) {
            double total = (runEndMs - runStartMs) / 1000.0;

            int tier = KuudraTierDetector.getTier();
            if (tier < 1 || tier > 5) tier = 5;

            sessionRunTimes.computeIfAbsent(tier, k -> new ArrayList<>()).add(total);

            if (KuudraConfig.updateTotalRunPb(tier, total)) {
                double[] splitArr = new double[Split.values().length];
                java.util.Arrays.fill(splitArr, 9999.0);
                for (Split s : splitsForTier(tier)) {
                    PhaseResult r = results.get(s);
                    if (r != null) splitArr[s.ordinal()] = r.wallSec();
                }

                KuudraConfig.PbRecord record = new KuudraConfig.PbRecord();
                record.totalTime = total;
                record.splits = splitArr;
                record.dateMs = System.currentTimeMillis();
                record.supplies = new ArrayList<>();

                for (SupplyTime st : supplyTimes)
                    record.supplies.add(new KuudraConfig.PlayerTime(st.player(), st.timeSec()));

                record.freshes = new ArrayList<>(runFreshTimes);
                KuudraConfig.setPbRecord(tier, record);
            }
        }

        savePbIfBetter();
        pendingAnnounce = true;
    }

    private static void tickPlayerDetection(Minecraft client) {

        if (inEatenPhase) {
            if (stunPlayer == null) {
                for (Player p : client.level.players()) {
                    if (p.getVehicle() instanceof ArmorStand) {
                        stunPlayer = p;
                        lastStunPos = p.position();
                        stunPlayerLoaded = true;
                        break;
                    }
                }
                return;
            }

            boolean nowLoaded = client.level.players().contains(stunPlayer);

            if (!nowLoaded && stunPlayerLoaded) {
                triggerCannonFired();
                return;
            }

            stunPlayerLoaded = nowLoaded;

            if (nowLoaded && lastStunPos != null) {
                Vec3 cur = stunPlayer.position();

                if (cur.distanceTo(lastStunPos) > 30.0) {
                    triggerCannonFired();
                    return;
                }

                lastStunPos = cur;
            }

            return;
        }

        if (activeSplit != Split.SKIP) {
            playerLastPositions.clear();
        }
    }

    private static void triggerCannonFired() {
        inEatenPhase = false;
        endEatenSplit();
        stunPlayer = null;
        lastStunPos = null;
    }

    private static void endEatenSplit() {
        if (activeSplit == Split.EATEN) {
            endSplit(Split.EATEN);
            beginSplit(Split.STUN);
        }
        inEatenPhase = false;
    }

    private static void beginSplit(Split s) {
        activeSplit = s;
        phaseStartMs = System.currentTimeMillis();
        phaseStartTick = globalTick;
    }

    private static void endSplit(Split s) {
        if (activeSplit != s || phaseStartMs < 0) return;

        long endMs = System.currentTimeMillis();
        int endTick = globalTick;

        double wallSec = (endMs - phaseStartMs) / 1000.0;
        double tickSec = (endTick - phaseStartTick) * 0.05;

        results.put(s, new PhaseResult(wallSec, tickSec));
        activeSplit = null;
    }

    private static void savePbIfBetter() {
        int tier = KuudraTierDetector.getTier();
        if (tier < 1 || tier > 5) tier = 5;

        double[] pb = KuudraConfig.getSplitPb(tier);
        boolean improved = false;

        for (Split s : splitsForTier(tier)) {
            PhaseResult r = results.get(s);
            if (r == null) continue;
            if (r.wallSec() < pb[s.ordinal()]) {
                pb[s.ordinal()] = r.wallSec();
                improved = true;
            }
        }

        if (improved) KuudraConfig.setSplitPb(tier, pb);
    }

    private static void printRunSummary() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || runStartMs < 0 || runEndMs < 0) return;

        int tier = KuudraTierDetector.getTier();
        if (tier < 1 || tier > 5) tier = 5;

        send(mc, "§6§l--- T" + tier + " Run Complete ---");

        for (Split s : splitsForTier(tier)) {
            PhaseResult r = results.get(s);
            if (r != null)
                send(mc, "§7  " + splitLabel(s) + ": §a" + KuudraConfig.formatTime(r.wallSec()));
        }

        double total = (runEndMs - runStartMs) / 1000.0;
        send(mc, "§fTotal: §e" + KuudraConfig.formatTime(total));
        send(mc, "§6§l-----------------");
    }

    public static String splitLabel(Split s) {
        return switch (s) {
            case SUPPLIES -> "Supplies";
            case BUILD    -> "Build";
            case EATEN    -> "Eaten";
            case STUN     -> "Stun";
            case DPS      -> "DPS";
            case SKIP     -> "Skip";
            case BOSS     -> "Boss";
        };
    }

    private static void send(Minecraft mc, String msg) {
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal(msg));
    }

    public static Split                         getActiveSplit()    { return activeSplit; }
    public static PhaseResult                   getResult(Split s)  { return results.get(s); }
    public static List<SupplyTime>              getSupplyTimes()    { return Collections.unmodifiableList(supplyTimes); }
    public static List<KuudraConfig.PlayerTime> getFreshTimes()     { return Collections.unmodifiableList(runFreshTimes); }
    public static String                        getStunPlayerName() { return stunPlayer != null ? stunPlayer.getName().getString() : null; }

    public static double getActiveSplitElapsed() {
        if (activeSplit == null || phaseStartMs < 0) return 0;
        return (System.currentTimeMillis() - phaseStartMs) / 1000.0;
    }

    public static double recordSupplyRecovery(String player) {
        if (activeSplit != Split.SUPPLIES || runStartMs <= 0) return -1;
        double elapsed = (System.currentTimeMillis() - runStartMs) / 1000.0;
        supplyTimes.add(new SupplyTime(player, elapsed));
        runSupplyTimes.add(new KuudraConfig.PlayerTime(player, elapsed));
        return elapsed;
    }

    public static double getTotalRunTime() {
        if (runStartMs < 0 || runEndMs < 0) return 0;
        return (runEndMs - runStartMs) / 1000.0;
    }

    public static double getOverallElapsed() {
        if (runStartMs < 0) return 0;
        return (System.currentTimeMillis() - runStartMs) / 1000.0;
    }

    public static boolean hasData()       { return runStartMs >= 0; }
    public static boolean isRunActive()   { return activeSplit != null; }
    public static boolean isRunComplete() { return runEndMs > 0; }
    public static long    getRunEndMs()   { return runEndMs; }

    public static double getPredicted() {
        int tier = KuudraTierDetector.getTier();
        if (tier < 1 || tier > 5) tier = 5;
        Split[] relevant = splitsForTier(tier);

        double  total  = 0;
        boolean active = false;
        for (Split s : relevant) {
            int i = s.ordinal();
            PhaseResult r = results.get(s);
            if (r != null) {
                total += r.wallSec();
            } else if (s == activeSplit) {
                total += getActiveSplitElapsed();
                double rem = BEST_THEORETICAL[i] - getActiveSplitElapsed();
                if (rem > 0) total += rem;
                active = true;
            } else if (active) {
                total += BEST_THEORETICAL[i];
            }
        }
        if (!active && activeSplit == null) return getOverallElapsed();
        return total;
    }

    public static Split[] splitsForTier(int tier) {
        if (tier == 1 || tier == 2)
            return new Split[]{Split.SUPPLIES, Split.BUILD, Split.BOSS};
        if (tier == 3 || tier == 4)
            return new Split[]{Split.SUPPLIES, Split.BUILD, Split.EATEN, Split.STUN, Split.DPS};
        return Split.values();
    }

    public static Double getPbDiff(Split s) {
        PhaseResult r = results.get(s);
        if (r == null) return null;
        int tier = KuudraTierDetector.getTier();
        if (tier < 1 || tier > 5) tier = 5;
        double[] pb    = KuudraConfig.getSplitPb(tier);
        double   pbVal = pb[s.ordinal()];
        if (pbVal >= 9999) return null;
        return r.wallSec() - pbVal;
    }

    public static boolean showLag(Split s) {
        PhaseResult r = results.get(s);
        return r != null && Math.abs(r.lagSec()) >= LAG_THRESHOLD;
    }
}