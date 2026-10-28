package com.phantomaddons.mixin;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.customisation.items.ItemCustomization;
import com.phantomaddons.features.customisation.items.ItemTransformSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItems.class)
public class FirstPersonHandsStateMixin {

    @Shadow private float mainHandHeight;
    @Shadow private float oMainHandHeight;
    @Shadow private ItemStack mainHandItem;

    @Inject(method = "tick", at = @At("RETURN"))
    private void kuudrahelper$noEquipAnimTick(LocalPlayer player, CallbackInfo ci) {
        if (!PhantomConfig.isItemCustomizationEnabled()) return;

        ItemStack playerItem = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getMainHandItem() : player.getMainHandItem();
        ItemTransformSettings sPlayer   = ItemCustomization.resolveSettings(playerItem);
        ItemTransformSettings sRenderer = ItemCustomization.resolveSettings(mainHandItem);

        boolean playerNoEquip   = sPlayer   != null && sPlayer.noEquipAnimation;
        boolean rendererNoEquip = sRenderer != null && sRenderer.noEquipAnimation;
        if (!playerNoEquip && !rendererNoEquip) return;

        if (mainHandItem != playerItem) {
            mainHandHeight  = 0.0f;
            oMainHandHeight = 0.0f;
        } else if (playerNoEquip) {
            mainHandHeight  = 1.0f;
            oMainHandHeight = 1.0f;
        }
    }
}
