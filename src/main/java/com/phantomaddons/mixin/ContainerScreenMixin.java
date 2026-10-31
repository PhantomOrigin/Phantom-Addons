package com.phantomaddons.mixin;

import com.phantomaddons.features.misckuudra.ShopMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerScreen.class)
public abstract class ContainerScreenMixin {

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void phantomaddons$hideShopFrame(GuiGraphicsExtractor ctx, int mx, int my, float delta, CallbackInfo ci) {
        if (ShopMenu.layout((AbstractContainerScreen<?>) (Object) this) != null) ci.cancel();
    }
}
