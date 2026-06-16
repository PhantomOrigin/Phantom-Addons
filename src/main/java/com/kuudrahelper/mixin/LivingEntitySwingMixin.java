package com.kuudrahelper.mixin;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.items.ItemCustomization;
import com.kuudrahelper.features.items.ItemTransformSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntitySwingMixin {

    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
    private void kuudrahelper$modifySwingDuration(CallbackInfoReturnable<Integer> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player != (Object) this) return;
        if (!KuudraConfig.isItemCustomizationEnabled()) return;

        ItemStack stack = mc.player.getMainHandItem();
        ItemTransformSettings s = ItemCustomization.resolveSettings(stack);
        if (s == null) return;

        float speed = (s.swingSpeed > 0.001f) ? s.swingSpeed : 1f;
        if (Math.abs(speed - 1f) < 0.01f) return;

        int base = cir.getReturnValue();
        cir.setReturnValue(Math.max(1, Math.round(base / speed)));
    }
}