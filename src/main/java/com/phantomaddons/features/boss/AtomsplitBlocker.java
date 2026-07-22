package com.phantomaddons.features.boss;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.phase.KuudraPhaseTracker;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

public final class AtomsplitBlocker {

    private AtomsplitBlocker() {}

    public static void register() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (shouldBlock(player.getItemInHand(hand))) return InteractionResult.FAIL;
            return InteractionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (shouldBlock(player.getItemInHand(hand))) return InteractionResult.FAIL;
            return InteractionResult.PASS;
        });
    }

    public static boolean shouldBlock(ItemStack stack) {
        if (!PhantomConfig.isBlockAtomsplitEnabled()) return false;
        if (!KuudraPhaseTracker.isRunActive()) return false;
        if (stack == null || stack.isEmpty()) return false;

        return stack.getHoverName().getString().toLowerCase().contains("atomsplit");
    }
}
