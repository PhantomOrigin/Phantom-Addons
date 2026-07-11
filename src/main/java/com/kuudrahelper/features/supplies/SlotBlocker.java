package com.kuudrahelper.features.supplies;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class SlotBlocker {

    private static final int BLOCKED_SLOT = 8;

    private SlotBlocker() {}

    public static void register() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (shouldBlock(player)) return InteractionResult.FAIL;
            return InteractionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (shouldBlock(player)) return InteractionResult.FAIL;
            return InteractionResult.PASS;
        });
    }

    public static boolean shouldBlock(Player player) {
        if (!KuudraConfig.isBlockSlot9Enabled()) return false;
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.SUPPLIES) return false;
        ItemStack slotStack = player.getInventory().getItem(BLOCKED_SLOT);
        if (slotStack.isEmpty()) return false;
        return slotStack == player.getItemInHand(InteractionHand.MAIN_HAND);
    }
}
