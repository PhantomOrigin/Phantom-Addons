package com.kuudrahelper.features.misckuudra.profile;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.misckuudra.PartyCommands;
import net.minecraft.client.Minecraft;

public final class AutoKickManager {

    private AutoKickManager() {}

    public static void evaluate(String name, KuudraProfileData data) {
        if (!KuudraConfig.isAutoKickEnabled()) return;
        if (data == null || !data.loaded) return;
        if (meetsRequirements(data)) return;
        if (!PartyCommands.isPartyLeader()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        mc.execute(() -> mc.getConnection().sendCommand("p kick " + name));
    }

    private static boolean meetsRequirements(KuudraProfileData d) {
        if (!atLeast(KuudraConfig.getAkMinCatacombs(), d.catacombsLevel)) return false;
        if (!atLeast(KuudraConfig.getAkMinForaging(), d.foragingLevel)) return false;
        if (!atLeast(KuudraConfig.getAkMinMagicalPower(), d.magicalPower)) return false;
        if (!atLeast(KuudraConfig.getAkMinInfernal(), d.getKuudraCompletions(KuudraProfileData.KuudraTier.INFERNAL))) return false;
        if (!atLeast(KuudraConfig.getAkMinFiery(), d.getKuudraCompletions(KuudraProfileData.KuudraTier.FIERY))) return false;
        if (!atLeast(KuudraConfig.getAkMinBurning(), d.getKuudraCompletions(KuudraProfileData.KuudraTier.BURNING))) return false;
        if (!atLeast(KuudraConfig.getAkMinHot(), d.getKuudraCompletions(KuudraProfileData.KuudraTier.HOT))) return false;
        if (!atLeast(KuudraConfig.getAkMinBasic(), d.getKuudraCompletions(KuudraProfileData.KuudraTier.BASIC))) return false;
        if (KuudraConfig.isAkRequireRend() && !hasRendBuild(d)) return false;

        int gdragLevel = Math.max(
                d.dpsGoldenDragonPet != null ? d.dpsGoldenDragonPet.level() : 0,
                d.rendGoldenDragonPet != null ? d.rendGoldenDragonPet.level() : 0);
        if (!atLeast(KuudraConfig.getAkMinGdragLevel(), gdragLevel)) return false;

        return true;
    }

    private static boolean atLeast(int threshold, int actual) {
        return threshold < 0 || actual >= threshold;
    }

    private static boolean hasRendBuild(KuudraProfileData d) {
        boolean hasElegant = d.armorSets.stream().anyMatch(s -> s.label().equals("Elegant Tuxedo"));
        boolean hasReaper = d.armorSets.stream().anyMatch(s -> s.label().equals("Reaper Set"));
        boolean hasBonemerang = d.hasWeapon(KuudraProfileData.Weapon.REND_BONEMERANG);
        return hasElegant && hasReaper && hasBonemerang;
    }
}
