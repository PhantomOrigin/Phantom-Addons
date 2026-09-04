package com.phantomaddons.mixin;

import com.phantomaddons.features.misckuudra.EtherwarpPredictor;
import com.phantomaddons.features.miscskyblock.preventplacing.PreventPlacingPlayerHeads;
import com.phantomaddons.features.miscskyblock.preventplacing.PreventPlacingWeapons;
import com.phantomaddons.features.supplies.SlotBlocker;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.misckuudra.PickoblockManager;
import com.phantomaddons.features.supplies.etherwarp.EtherwarpWaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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

        if (com.phantomaddons.features.boss.AtomsplitBlocker.shouldBlock(stack)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        if (stack.is(net.minecraft.world.item.Items.FISHING_ROD) && hand == InteractionHand.MAIN_HAND
                && !hasAbilityFlay(stack) && !isLegacyNonFunctionalRod(stack)) {
            if (com.phantomaddons.features.miscskyblock.PredictedBobber.isActive()) {
                com.phantomaddons.features.miscskyblock.PredictedBobber.onRetrieve();
            } else {
                com.phantomaddons.features.miscskyblock.PredictedBobber.onCast(player);
            }
        }

        if (stack.is(net.minecraft.world.item.Items.ENDER_PEARL)) {
            if (PhantomConfig.isEtherwarpWaypointsEnabled()) {
                EtherwarpWaypointManager.onPearlThrow(client, player);
            }
            com.phantomaddons.features.supplies.waypointlines.WaypointLines.onPearlThrown();
            com.phantomaddons.data.supply.PearlCancelLogger.onPearlThrow(player);
        }

        if (hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
            String itemName = stack.getHoverName().getString().toLowerCase();
            if (itemName.contains("bonemerang")) {
                com.phantomaddons.features.boss.rend.RendTracker.onBonemerangThrow();
                com.phantomaddons.features.boss.backbone.BackboneProgressBar.onBonemerangThrow();
                com.phantomaddons.features.boss.bonetiming.BoneTimingAssist.onBonemerangThrow();
                Vec3 eye = player.getEyePosition();
                com.phantomaddons.features.boss.rend.BonemerangHitTracker.onThrow(eye.x, eye.y, eye.z);
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

        if (!PhantomConfig.isPickoblockEnabled()) return;
        if (!PickoblockManager.isPickobulusItem(stack)) return;
        if (!PickoblockManager.isPickobulusAllowedThisTick()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    private static boolean hasAbilityFlay(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var lore = stack.get(net.minecraft.core.component.DataComponents.LORE);
        if (lore == null) return false;
        for (var line : lore.lines()) {
            if (line.getString().contains("Ability: Flay")) return true;
        }
        return false;
    }

    private static final java.util.Set<String> LEGACY_NON_FUNCTIONAL_RODS = java.util.Set.of(
            "Prismarine Rod", "Sponge Rod", "Ice Rod", "Speedster Rod", "Farmer's Rod",
            "Chum Rod", "Winter Rod", "Yeti Rod", "Auger Rod", "Shredder", "Phantom Rod"
    );

    private static boolean isLegacyNonFunctionalRod(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String name = stack.getHoverName().getString();
        for (String rod : LEGACY_NON_FUNCTIONAL_RODS) {
            if (name.contains(rod)) return true;
        }
        return false;
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