package com.kuudrahelper.utils;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.KuudraHelperMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class RoleManager {

    private static KuudraConfig.RoleMode resolvedRole = null;

    public static void resolveAutoRole(Minecraft client) {
        if (resolvedRole != null) return;
        if (client.player == null) return;

        boolean wearingHollow = isWearingHollowArmor(client.player);
        resolvedRole = wearingHollow ? KuudraConfig.RoleMode.STUN : KuudraConfig.RoleMode.DPS;
        KuudraHelperMod.LOGGER.info("[PhantomAddons] AUTO resolved role = {}", resolvedRole);
    }

    public static void reset() {
        resolvedRole = null;
    }

    public static KuudraConfig.RoleMode getActiveRole() {
        if (KuudraConfig.getRoleMode() == KuudraConfig.RoleMode.AUTO) {
            return resolvedRole != null ? resolvedRole : KuudraConfig.RoleMode.DPS;
        }
        return KuudraConfig.getRoleMode();
    }

    public static void setManualRole(KuudraConfig.RoleMode mode) {
        resolvedRole = mode;
    }

    public static void requestAutoRoleResolution() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) client.execute(() -> resolveAutoRole(client));
    }

    private static boolean isWearingHollowArmor(Player player) {
        int count = 0;
        if (isHollow(player.getItemBySlot(EquipmentSlot.FEET)))  count++;
        if (isHollow(player.getItemBySlot(EquipmentSlot.LEGS)))  count++;
        if (isHollow(player.getItemBySlot(EquipmentSlot.CHEST))) count++;
        if (isHollow(player.getItemBySlot(EquipmentSlot.HEAD)))  count++;
        return count >= 3;
    }

    private static boolean isHollow(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getHoverName().getString().toLowerCase().contains("hollow");
    }
}