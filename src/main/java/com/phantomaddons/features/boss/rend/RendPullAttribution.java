package com.phantomaddons.features.boss.rend;

import com.phantomaddons.PhantomAddons;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.boss.KuudraHpHud;
import com.phantomaddons.phase.KuudraPhaseTracker;
import com.phantomaddons.phase.KuudraPhaseTracker.Phase;
import com.phantomaddons.utils.KuudraTierDetector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RendPullAttribution {

    private static final double MAX_PULL_RANGE = 16.0;

    private static final long STALE_DISCARD_MS = 5_000;

    private static final Set<UUID> armed = new HashSet<>();
    private static final Map<UUID, PulledEntry> pending = new HashMap<>();

    private record PulledEntry(String name, long swingAtMs, long effectiveTimeMs, boolean local) {}

    public record Puller(String name, long effectiveTimeMs) {}

    private RendPullAttribution() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;
            if (!isActiveKuudraFight()) return;

            for (Player p : client.level.players()) {
                if (isBonemerang(p.getMainHandItem()) && armed.add(p.getUUID())) {
                    PhantomAddons.LOGGER.info("[RendPull] armed {} (holding Bonemerang)", p.getName().getString());
                }
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    public static void onPlayerSwing(Player player, ItemStack mainHand) {
        if (!isActiveKuudraFight()) return;

        UUID uuid = player.getUUID();
        boolean isArmed = armed.contains(uuid);
        String itemId = mainHand.isEmpty() ? "<empty>" : BuiltInRegistries.ITEM.getKey(mainHand.getItem()).toString();

        if (!isRendWeaponType(mainHand)) {
            if (isArmed) {
                PhantomAddons.LOGGER.info("[RendPull] swing: {} (armed) holding \"{}\" ({}) — not bow-type, ignored",
                        player.getName().getString(), mainHand.isEmpty() ? "<empty>" : mainHand.getHoverName().getString(), itemId);
            }
            return;
        }

        if (!isArmed) {
            PhantomAddons.LOGGER.info("[RendPull] swing: {} swung a bow-type item ({}) but wasn't armed — ignored",
                    player.getName().getString(), itemId);
            return;
        }

        if (!isWithinPullRange(player)) {
            PhantomAddons.LOGGER.info("[RendPull] swing: {} (armed) swung a bow-type item ({}) but is too far from Kuudra — ignored",
                    player.getName().getString(), itemId);
            return;
        }

        armed.remove(uuid);

        Minecraft mc = Minecraft.getInstance();
        boolean local = mc.player != null && mc.player.getUUID().equals(uuid);
        long now = System.currentTimeMillis();
        long effectiveTimeMs = local ? now + PhantomConfig.getLowPing() : now;

        PhantomAddons.LOGGER.info("[RendPull] PULL RECORDED: {} (local={}, swingAt={}, effectiveAt={}, pingUsed={})",
                player.getName().getString(), local, now, effectiveTimeMs, local ? PhantomConfig.getLowPing() : 0);
        pending.put(uuid, new PulledEntry(player.getName().getString(), now, effectiveTimeMs, local));
    }

    public static void logPendingAtDamage(String context, long damageAtMs) {
        if (pending.isEmpty()) {
            PhantomAddons.LOGGER.info("[RendPull][DEBUG] {} at {} — pending is EMPTY (nobody armed+swung before this damage)",
                    context, damageAtMs);
            return;
        }
        for (PulledEntry e : pending.values()) {
            long sinceSwing = damageAtMs - e.swingAtMs();
            long sinceEffective = damageAtMs - e.effectiveTimeMs();
            PhantomAddons.LOGGER.info(
                    "[RendPull][DEBUG] {} at {} — candidate {} (local={}): swungAt={} ({}ms before damage), effectiveAt={} ({}ms {} damage)",
                    context, damageAtMs, e.name(), e.local(), e.swingAtMs(), sinceSwing,
                    e.effectiveTimeMs(), Math.abs(sinceEffective), sinceEffective >= 0 ? "before" : "AFTER");
        }
    }

    private static boolean isWithinPullRange(Player player) {
        LivingEntity kuudra = KuudraHpHud.getKuudra();
        if (kuudra == null) return false;
        return player.distanceToSqr(kuudra) <= MAX_PULL_RANGE * MAX_PULL_RANGE;
    }

    public static List<Puller> collectReady(long byMs) {
        long now = System.currentTimeMillis();
        List<Puller> result = new ArrayList<>();

        Iterator<Map.Entry<UUID, PulledEntry>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PulledEntry> mapEntry = it.next();
            PulledEntry entry = mapEntry.getValue();
            if (entry.effectiveTimeMs() <= byMs) {
                result.add(new Puller(entry.name(), entry.effectiveTimeMs()));
                it.remove();
            } else if (now - entry.effectiveTimeMs() > STALE_DISCARD_MS) {
                PhantomAddons.LOGGER.info("[RendPull] discarding stale entry {} (effectiveAt={}, now={})",
                        entry.name(), entry.effectiveTimeMs(), now);
                it.remove();
            }
        }
        return result;
    }

    public static void reset() {
        armed.clear();
        pending.clear();
    }

    private static boolean isActiveKuudraFight() {
        if (KuudraTierDetector.getTier() != 5) return false;
        Phase phase = KuudraPhaseTracker.getPhase();
        return phase != Phase.SUPPLIES && phase != Phase.BUILD && phase != Phase.EATEN
                && phase != Phase.SKIP && phase != Phase.END && phase != Phase.NONE;
    }

    private static boolean isBonemerang(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var item = stack.getItem();
        return item == Items.BONE || item == Items.GHAST_TEAR;
    }

    private static boolean isRendWeaponType(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.BOW;
    }
}
