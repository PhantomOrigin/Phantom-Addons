package com.kuudrahelper.mixin;

import com.kuudrahelper.features.HollowWandAnnouncer;
import com.kuudrahelper.features.pearls.PearlTitleListener;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class TitleMixin {

    @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
    private void phantomaddons$onSetTitle(Component title, CallbackInfo ci) {
        if (title == null) return;
        String text = title.getString();
        HollowWandAnnouncer.onTitle(text);
        PearlTitleListener.onTitleText(text);
        if (PearlTitleListener.isMatchingTitle(text)) {
            PearlTitleListener.setActiveComponent(title);
            ci.cancel();
        }
    }

    @Inject(method = "setSubtitle", at = @At("HEAD"), cancellable = true)
    private void phantomaddons$onSetSubtitle(Component subtitle, CallbackInfo ci) {
        if (subtitle == null) return;
        String text = subtitle.getString();
        PearlTitleListener.onTitleText(text);
        if (PearlTitleListener.isMatchingTitle(text)) {
            PearlTitleListener.setActiveComponent(subtitle);
            ci.cancel();
        }
    }
}
