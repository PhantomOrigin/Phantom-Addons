package com.phantomaddons.utils;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.PhantomAddons;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class RoleManager {

    private static PhantomConfig.RoleMode resolvedRole = null;

    public static void resolveAutoRole(Minecraft client) {
        if (resolvedRole != null) return;
        if (client.player == null) return;

        boolean wearingHollow = isWearingHollowArmor(client.player);
        resolvedRole = wearingHollow ? PhantomConfig.RoleMode.STUN : PhantomConfig.RoleMode.DPS;
        PhantomAddons.LOGGER.info("[PhantomAddons] AUTO resolved role = {}", resolvedRole);
    }

    public static void reset() {
        resolvedRole = null;
    }

    public static PhantomConfig.RoleMode getActiveRole() {
        if (PhantomConfig.getRoleMode() == PhantomConfig.RoleMode.AUTO) {
            return resolvedRole != null ? resolvedRole : PhantomConfig.RoleMode.DPS;
        }
        return PhantomConfig.getRoleMode();
    }

    public static void setManualRole(PhantomConfig.RoleMode mode) {
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