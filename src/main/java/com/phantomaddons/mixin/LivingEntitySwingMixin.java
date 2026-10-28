package com.phantomaddons.mixin;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.customisation.items.ItemCustomization;
import com.phantomaddons.features.customisation.items.ItemTransformSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if >=26.3 {
/*import net.minecraft.world.item.component.SwingAnimation;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntitySwingMixin {

    //? if <26.3 {
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;)V", at = @At("HEAD"))
    private void kuudrahelper$onSwing(InteractionHand hand, CallbackInfo ci) {
        kuudrahelper$handleSwing(hand);
    }
    //?} else {
    /*@Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z", at = @At("HEAD"))
    private void kuudrahelper$onSwing(InteractionHand hand, SwingAnimation animation, boolean updateSelf, CallbackInfoReturnable<Boolean> cir) {
        kuudrahelper$handleSwing(hand);
    }
    *///?}

    private void kuudrahelper$handleSwing(InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return;

        if ((Object) this instanceof Player swinger) {
            com.phantomaddons.features.boss.rend.RendPullAttribution.onPlayerSwing(swinger, swinger.getMainHandItem());
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player != (Object) this) return;
        com.phantomaddons.features.boss.rend.RendTracker.onLeftClick(mc.player.getMainHandItem());
        com.phantomaddons.features.boss.bonetiming.BoneTimingAssist.onLeftClick(mc.player.getMainHandItem());
    }

    //? if <26.3 {
    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
    private void kuudrahelper$modifySwingDuration(CallbackInfoReturnable<Integer> cir) {
        kuudrahelper$changeSwingDuration(cir);
    }
    //?} else {
    /*@Inject(method = "getModifiedSwingDuration", at = @At("RETURN"), cancellable = true)
    private void kuudrahelper$modifySwingDuration(SwingAnimation animation, CallbackInfoReturnable<Integer> cir) {
        kuudrahelper$changeSwingDuration(cir);
    }
    *///?}

    private void kuudrahelper$changeSwingDuration(CallbackInfoReturnable<Integer> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player != (Object) this) return;
        if (!PhantomConfig.isItemCustomizationEnabled()) return;

        ItemStack stack = mc.player.getMainHandItem();
        ItemTransformSettings s = ItemCustomization.resolveSettings(stack);
        if (s == null) return;

        float speed = (s.swingSpeed > 0.001f) ? s.swingSpeed : 1f;
        if (Math.abs(speed - 1f) < 0.01f) return;

        cir.setReturnValue(Math.max(1, Math.round(6f / speed)));
    }
}