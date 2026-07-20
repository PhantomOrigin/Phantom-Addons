package com.phantomaddons.mixin;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.misckuudra.HollowWandAnnouncer;
import com.phantomaddons.features.supplies.pearlwaypoints.PearlTitleListener;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Pattern;

@Mixin(Gui.class)
public abstract class TitleMixin {

    private static final Pattern DAMAGE_TITLE = Pattern.compile("[\\d.,]+[a-zA-Z]?/[\\d.,]+[a-zA-Z]", Pattern.CASE_INSENSITIVE);

    @Shadow private Component title;
    @Shadow private Component subtitle;

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void phantomaddons$onSetTitle(Component title, CallbackInfo ci) {
        if (title == null) return;
        String text = title.getString();
        HollowWandAnnouncer.onTitle(text);
        PearlTitleListener.onTitleText(text);
    }

    @Inject(method = "setSubtitle", at = @At("HEAD"))
    private void phantomaddons$onSetSubtitle(Component subtitle, CallbackInfo ci) {
        if (subtitle == null) return;
        PearlTitleListener.onTitleText(subtitle.getString());
    }

    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
    private void phantomaddons$onExtractTitle(GuiGraphicsExtractor ctx, DeltaTracker tracker, CallbackInfo ci) {
        if (title != null) {
            String text = title.getString();
            if (PearlTitleListener.isMatchingTitle(text)) {
                PearlTitleListener.setActiveComponent(title);
                ci.cancel();
                return;
            }
            if (PhantomConfig.isHideDamageTitleEnabled() && DAMAGE_TITLE.matcher(text).find()) {
                ci.cancel();
                return;
            }
        }
        if (subtitle != null && PearlTitleListener.isMatchingTitle(subtitle.getString())) {
            PearlTitleListener.setActiveComponent(subtitle);
            ci.cancel();
        }
    }
}
