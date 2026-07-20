package com.phantomaddons.mixin;

import com.phantomaddons.features.boss.backbone.BackboneProgressBarHud;
import com.phantomaddons.features.misckuudra.profittracker.ProfitHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenHudMixin {

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void phantomaddons$drawOverlaysAboveScreen(GuiGraphicsExtractor ctx, int mouseX, int mouseY,
                                                        float partialTick, CallbackInfo ci) {
        BackboneProgressBarHud.render(ctx);

        if (ProfitHud.shouldShow()) {
            ctx.nextStratum();
            ProfitHud.renderInScreen(ctx);
        }
    }
}
