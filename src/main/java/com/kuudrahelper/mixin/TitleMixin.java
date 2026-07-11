package com.kuudrahelper.mixin;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.misckuudra.HollowWandAnnouncer;
import com.kuudrahelper.features.supplies.PearlTitleListener;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Pattern;

@Mixin(Gui.class)
public abstract class TitleMixin {

    private static final Pattern DAMAGE_TITLE = Pattern.compile("[\\d.,]+[a-zA-Z]?/[\\d.,]+[a-zA-Z]", Pattern.CASE_INSENSITIVE);

    @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
    private void phantomaddons$onSetTitle(Component title, CallbackInfo ci) {
        if (title == null) return;
        String text = title.getString();
        HollowWandAnnouncer.onTitle(text);
        PearlTitleListener.onTitleText(text);
        if (PearlTitleListener.isMatchingTitle(text)) {
            PearlTitleListener.setActiveComponent(title);
            ci.cancel();
            return;
        }
        if (KuudraConfig.isHideDamageTitleEnabled() && DAMAGE_TITLE.matcher(text).find()) {
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
