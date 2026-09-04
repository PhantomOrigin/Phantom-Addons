package com.phantomaddons.features.boss.rend;

import com.phantomaddons.PhantomAddons;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.phase.KuudraPhaseTracker;
import com.phantomaddons.phase.KuudraPhaseTracker.Phase;
import com.phantomaddons.utils.KuudraTierDetector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
//? if <26.2 {
/*import net.minecraft.world.entity.monster.Slime;
*///?} else {
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
//?}

import java.util.ArrayList;
import java.util.List;

public final class RendDamage {

    public  static final int  KUUDRA_SLIME_MAX_HP = 25_000;
    private static final int  KUUDRA_MAX_HP       = KUUDRA_SLIME_MAX_HP;
    private static final int  MIN_PULL_DIFF  = 1_666;
    private static final int  HP_MULTIPLIER  = 9_600;

    private static final long GROUP_WINDOW_MS = 50;

    private static final long RESOLVE_WINDOW_MS = 200;

    private static int  lastHp       = -1;
    private static long phaseStartMs = -1L;

    private record DiffInfo(int diff, long damage, long elapsed) {}

    private static Evaluation activeEval = null;

    private static final class Evaluation {
        final DiffInfo first;
        final long t0;
        final long resolveGroupAt;
        final long resolveFinalAt;
        boolean groupResolved = false;
        List<RendPullAttribution.Puller> groupA = null;
        DiffInfo second = null;
        long secondAtMs = -1L;

        Evaluation(DiffInfo first, long t0) {
            this.first = first;
            this.t0 = t0;
            this.resolveGroupAt = t0 + GROUP_WINDOW_MS;
            this.resolveFinalAt = t0 + RESOLVE_WINDOW_MS;
        }
    }

    private RendDamage() {}

    public static void onKillPhaseStart() {
        if (phaseStartMs >= 0) return;
        lastHp       = -1;
        phaseStartMs = System.currentTimeMillis();
    }

    public static void onBossPhaseStart() {
        lastHp       = -1;
        phaseStartMs = System.currentTimeMillis();
    }

    public static void reset() {
        lastHp       = -1;
        phaseStartMs = -1L;
        activeEval   = null;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!PhantomConfig.isRendDamageEnabled()) return;
            if (client.level == null || client.player == null) return;
            if (KuudraTierDetector.getTier() != 5) return;

            long now = System.currentTimeMillis();
            DiffInfo detected = detectDiff(client, now);
            if (detected != null) onDiffDetected(detected, now);

            processEvaluation(client, now);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());

        ClientReceiveMessageEvents.ALLOW_GAME.register((text, overlay) -> { onKuudraDownMessage(text.getString()); return true; });
        ClientReceiveMessageEvents.CHAT.register((text, signed, sender, params, ts) -> onKuudraDownMessage(text.getString()));
    }

    private static void onDiffDetected(DiffInfo detected, long now) {
        RendPullAttribution.logPendingAtDamage("diff detected (diff=" + detected.diff() + ")", now);
        if (activeEval == null) {
            PhantomAddons.LOGGER.info("[RendPull] diff detected (diff={}, damage={}) -> starting evaluation at t0={}",
                    detected.diff(), detected.damage(), now);
            activeEval = new Evaluation(detected, now);
        } else if (activeEval.second == null) {
            PhantomAddons.LOGGER.info("[RendPull] second diff detected (diff={}, damage={}) mid-evaluation",
                    detected.diff(), detected.damage());
            activeEval.second = detected;
            activeEval.secondAtMs = now;
        }
    }

    private static void onKuudraDownMessage(String raw) {
        if (!PhantomConfig.isRendDamageEnabled()) return;
        if (KuudraTierDetector.getTier() != 5) return;
        if (lastHp <= 0) return; // nothing left unaccounted for, or already finalized

        String msg = KuudraPhaseTracker.stripFormatting(raw);
        if (!(msg.contains("KUUDRA DOWN") || msg.contains("DEFEAT KUUDRA")
                || msg.contains("KUUDRA HAS BEEN DEFEATED"))) return;

        int diff = lastHp;
        lastHp = -1;
        if (diff < MIN_PULL_DIFF) return;

        long now = System.currentTimeMillis();
        long damage = (long) diff * HP_MULTIPLIER;
        long pingAdjustMs = PhantomConfig.getLowPing();
        long elapsed = phaseStartMs >= 0 ? Math.max(0L, now - phaseStartMs - pingAdjustMs) : 0;

        PhantomAddons.LOGGER.info("[RendPull] Kuudra defeated — synthesizing final diff from lastHp={} (chat: \"{}\")", diff, msg);
        onDiffDetected(new DiffInfo(diff, damage, elapsed), now);
    }

    private static DiffInfo detectDiff(Minecraft mc, long now) {
        Phase phase = KuudraPhaseTracker.getPhase();
        if (phase == Phase.SUPPLIES || phase == Phase.BUILD
                || phase == Phase.EATEN || phase == Phase.SKIP
                || phase == Phase.END || phase == Phase.NONE) return null;

        AbstractCubeMob kuudra = findKuudra(mc);
        if (kuudra == null) return null;

        int hp = Math.min(Math.max((int) kuudra.getHealth(), 0), KUUDRA_MAX_HP);

        if (lastHp < 0) { lastHp = hp; return null; }

        int diff = lastHp - hp;
        lastHp = hp;

        if (hp <= 0) {
            lastHp = -1;
        }
        if (diff < MIN_PULL_DIFF) return null;

        long damage = (long) diff * HP_MULTIPLIER;
        long pingAdjustMs = PhantomConfig.getLowPing();
        long elapsed = phaseStartMs >= 0
                ? Math.max(0L, now - phaseStartMs - pingAdjustMs)
                : 0;
        return new DiffInfo(diff, damage, elapsed);
    }

    private static void processEvaluation(Minecraft mc, long now) {
        if (activeEval == null || mc.player == null) return;
        Evaluation eval = activeEval;

        if (!eval.groupResolved && now >= eval.resolveGroupAt) {
            eval.groupA = RendPullAttribution.collectReady(eval.resolveGroupAt);
            eval.groupResolved = true;
            PhantomAddons.LOGGER.info("[RendPull] group window closed (t0={}, window=[{},{}]) -> {} candidate(s): {}",
                    eval.t0, eval.t0, eval.resolveGroupAt, eval.groupA.size(),
                    eval.groupA.stream().map(RendPullAttribution.Puller::name).toList());

            if (eval.groupA.size() == 1) {
                PhantomAddons.LOGGER.info("[RendPull] resolving via IMMEDIATE single match: {}", eval.groupA.get(0).name());
                sendMessage(mc, eval.first, List.of(eval.groupA.get(0).name()));
                activeEval = null;
                return;
            }
        }

        if (eval.groupResolved && now >= eval.resolveFinalAt) {
            List<RendPullAttribution.Puller> late =
                    RendPullAttribution.collectReady(eval.resolveFinalAt);
            PhantomAddons.LOGGER.info("[RendPull] final window closed (window=[{},{}]) -> {} late candidate(s): {}, second diff present={}",
                    eval.resolveGroupAt + 1, eval.resolveFinalAt, late.size(),
                    late.stream().map(RendPullAttribution.Puller::name).toList(), eval.second != null);

            if (eval.second != null && late.isEmpty()) {
                long midpoint = (eval.t0 + eval.secondAtMs) / 2;
                List<String> before = new ArrayList<>();
                List<String> after  = new ArrayList<>();
                for (RendPullAttribution.Puller p : eval.groupA) {
                    (p.effectiveTimeMs() > midpoint ? after : before).add(p.name());
                }
                if (!before.isEmpty() && !after.isEmpty()) {
                    PhantomAddons.LOGGER.info("[RendPull] resolving via SPLIT: before-t0={} -> first pull, after-t0={} -> second pull",
                            before, after);
                    sendMessage(mc, eval.first, before);
                    sendMessage(mc, eval.second, after);
                    activeEval = null;
                    return;
                }

                List<RendPullAttribution.Puller> all = new ArrayList<>(eval.groupA);
                all.addAll(late);
                String who = all.stream()
                        .max((a, b) -> Long.compare(a.effectiveTimeMs(), b.effectiveTimeMs()))
                        .map(RendPullAttribution.Puller::name)
                        .orElse(null);
                PhantomAddons.LOGGER.info("[RendPull] resolving via FALLBACK (degenerate split, last swinger of {}): {}", all.size(), who);
                sendMessage(mc, eval.first, who != null ? List.of(who) : List.of());
                activeEval = eval.second != null ? new Evaluation(eval.second, now) : null;
                return;
            }

            List<RendPullAttribution.Puller> all = new ArrayList<>(eval.groupA);
            all.addAll(late);
            List<String> names = all.stream().map(RendPullAttribution.Puller::name).toList();
            PhantomAddons.LOGGER.info("[RendPull] resolving via JOINT credit (no second pull, {} candidate(s)): {}", all.size(), names);
            sendMessage(mc, eval.first, names);

            activeEval = eval.second != null ? new Evaluation(eval.second, now) : null;
        }
    }

    private static void sendMessage(Minecraft mc, DiffInfo info, List<String> names) {
        String who = formatNames(names);
        mc.player.sendSystemMessage(Component.literal(
                String.format("§f[PhantomAddons]§r §f%s pulled for %s%s §fdamage at §a%s§f.",
                        who,
                        pullColor(info.diff()),
                        formatDamage(info.damage()),
                        formatElapsed(info.elapsed()))
        ));
    }

    private static AbstractCubeMob findKuudra(Minecraft mc) {
        if (mc.level == null) return null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof AbstractCubeMob slime && slime.getSize() == 30)
                return slime;
        }
        return null;
    }

    private static String formatNames(List<String> names) {
        if (names.isEmpty()) return "Someone";
        if (names.size() == 1) return names.get(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append(i == names.size() - 1 ? " and " : ", ");
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    private static String pullColor(int diff) {
        if (diff <= 4_166) return "§c";   // < ~40M
        if (diff <= 7_291) return "§e";   // 40–70M
        return "§a";                       // > 70M
    }

    private static String formatDamage(long n) {
        if (n >= 1_000_000_000L) return String.format("%.1fB", n / 1_000_000_000.0);
        if (n >= 1_000_000L)     return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000L)         return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    private static String formatElapsed(long ms) {
        long secs   = ms / 1_000;
        long millis = ms % 1_000;
        return String.format("%d.%03ds", secs, millis);
    }
}
