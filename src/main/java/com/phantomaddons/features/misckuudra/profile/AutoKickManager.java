package com.phantomaddons.features.misckuudra.profile;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.misckuudra.AutoKickCoordinator;
import com.phantomaddons.features.misckuudra.PartyCommands;
import com.phantomaddons.features.supplies.PartyChatQueue;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AutoKickManager {

    private static final long KICK_DELAY_MS = 200;

    private static final Map<String, Long> pendingKicks = new LinkedHashMap<>();

    private AutoKickManager() {}

    public static void evaluate(String name, KuudraProfileData data) {
        if (!PhantomConfig.isAutoKickEnabled()) return;
        if (data == null || !data.loaded) return;

        List<String> failReasons = missingRequirements(data);
        if (failReasons.isEmpty()) return;
        if (!PartyCommands.isPartyLeader()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        String msg = "[Phantom] Kicking " + name + " - missing: " + String.join(", ", failReasons);

        mc.execute(() -> {
            if (!AutoKickCoordinator.tryClaim(name)) return;
            PartyChatQueue.send(msg);
            pendingKicks.put(name, System.currentTimeMillis() + KICK_DELAY_MS);
        });
    }

    public static void tick(Minecraft mc) {
        if (pendingKicks.isEmpty()) return;
        if (mc.getConnection() == null) return;

        long now = System.currentTimeMillis();
        var it = pendingKicks.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (now >= entry.getValue()) {
                PartyChatQueue.sendCommand("p kick " + entry.getKey());
                it.remove();
            }
        }
    }

    public static void reset() {
        pendingKicks.clear();
    }

    private static List<String> missingRequirements(KuudraProfileData d) {
        List<String> reasons = new ArrayList<>();

        checkAtLeast(reasons, "Catacombs", PhantomConfig.getAkMinCatacombs(), d.catacombsLevel);
        checkAtLeast(reasons, "Foraging", PhantomConfig.getAkMinForaging(), d.foragingLevel);
        checkAtLeast(reasons, "Magical Power", PhantomConfig.getAkMinMagicalPower(), d.magicalPower);
        checkAtLeast(reasons, "T5 Completions", PhantomConfig.getAkMinInfernal(),
                d.getKuudraCompletions(KuudraProfileData.KuudraTier.INFERNAL));
        checkAtLeast(reasons, "T4 Completions", PhantomConfig.getAkMinFiery(),
                d.getKuudraCompletions(KuudraProfileData.KuudraTier.FIERY));
        checkAtLeast(reasons, "T3 Completions", PhantomConfig.getAkMinBurning(),
                d.getKuudraCompletions(KuudraProfileData.KuudraTier.BURNING));
        checkAtLeast(reasons, "T2 Completions", PhantomConfig.getAkMinHot(),
                d.getKuudraCompletions(KuudraProfileData.KuudraTier.HOT));
        checkAtLeast(reasons, "T1 Completions", PhantomConfig.getAkMinBasic(),
                d.getKuudraCompletions(KuudraProfileData.KuudraTier.BASIC));

        if (PhantomConfig.isAkRequireRend() && !hasRendBuild(d)) {
            reasons.add("Rend Build");
        }

        int gdragLevel = Math.max(
                d.dpsGoldenDragonPet != null ? d.dpsGoldenDragonPet.level() : 0,
                d.rendGoldenDragonPet != null ? d.rendGoldenDragonPet.level() : 0);
        checkAtLeast(reasons, "Golden Dragon Lvl", PhantomConfig.getAkMinGdragLevel(), gdragLevel);

        return reasons;
    }

    private static void checkAtLeast(List<String> reasons, String label, int threshold, int actual) {
        if (threshold < 0) return;
        if (actual >= threshold) return;
        reasons.add(label + " " + threshold + " (has " + Math.max(actual, 0) + ")");
    }

    private static boolean hasRendBuild(KuudraProfileData d) {
        boolean hasElegant = d.armorSets.stream().anyMatch(s -> s.label().equals("Elegant Tuxedo"));
        boolean hasReaper = d.armorSets.stream().anyMatch(s -> s.label().equals("Reaper Set"));
        boolean hasBonemerang = d.hasWeapon(KuudraProfileData.Weapon.REND_BONEMERANG);
        return hasElegant && hasReaper && hasBonemerang;
    }
}
