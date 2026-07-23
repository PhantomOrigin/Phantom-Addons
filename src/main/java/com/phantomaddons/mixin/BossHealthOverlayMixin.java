package com.phantomaddons.mixin;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.utils.KuudraTierDetector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void phantomaddons$hideBossBar(GuiGraphicsExtractor ctx, CallbackInfo ci) {
        if (!PhantomConfig.isHideBossBarEnabled()) return;
        if (!KuudraTierDetector.isInKuudraHollow()) return;
        if (PhantomConfig.isHideBossBarOnlyInKuudra() && KuudraTierDetector.getTier() <= 0) return;
        ci.cancel();
    }
}
