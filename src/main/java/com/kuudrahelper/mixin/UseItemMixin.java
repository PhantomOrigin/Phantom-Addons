package com.kuudrahelper.mixin;

import com.kuudrahelper.features.EtherwarpPredictor;
import com.kuudrahelper.features.PreventPlacingPlayerHeads;
import com.kuudrahelper.features.PreventPlacingWeapons;
import com.kuudrahelper.features.SlotBlocker;
import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.PickoblockManager;
import com.kuudrahelper.features.supplies.EtherwarpWaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class UseItemMixin {
    @Inject(
            method = "useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"),
            cancellable = true)
    private void kuudrahelper$blockInteractions(
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {

        if (SlotBlocker.shouldBlock(player)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        ItemStack stack  = player.getItemInHand(hand);
        Minecraft client = Minecraft.getInstance();

        if (KuudraConfig.isEtherwarpWaypointsEnabled() && stack.is(net.minecraft.world.item.Items.ENDER_PEARL)) {
            EtherwarpWaypointManager.onPearlThrow(client, player);
        }

        // Rend Tracker: detect bonemerang throw
        if (hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
            String itemName = stack.getHoverName().getString().toLowerCase();
            if (itemName.contains("bonemerang")) {
                com.kuudrahelper.features.kuudra.RendTracker.onBonemerangThrow();
            }
        }

        if (EtherwarpPredictor.isEtherTransmissionItem(stack)) {
            EtherwarpPredictor.predictEtherwarpIntoPickobulus(client);
            boolean hasInstantTransmission = EtherwarpPredictor.isInstantTransmissionItem(stack);
            boolean sneaking = player.isShiftKeyDown();
            if (hasInstantTransmission && !sneaking) {
                EtherwarpPredictor.predictInstantTransmissionIntoLava(client);
            } else {
                EtherwarpPredictor.predictEtherwarpIntoLava(client);
            }
            if (EtherwarpPredictor.isEtherwarpBlockedThisTick()) {
                cir.setReturnValue(InteractionResult.FAIL);
            }
            return;
        }

        if (!KuudraConfig.isPickoblockEnabled()) return;
        if (!PickoblockManager.isPickobulusItem(stack)) return;
        if (!PickoblockManager.isPickobulusAllowedThisTick()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
            method = "useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"),
            cancellable = true)
    private void kuudrahelper$blockPlacing(
            net.minecraft.client.player.LocalPlayer player,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir) {

        ItemStack stack = player.getItemInHand(hand);

        if (PreventPlacingPlayerHeads.shouldCancel(player, stack)) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }

        if (PreventPlacingWeapons.shouldCancel(player, stack)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}