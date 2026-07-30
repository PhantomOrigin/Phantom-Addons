package com.phantomaddons.mixin;

import com.phantomaddons.utils.TextUtil;
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

    private static final Pattern BOSS_HP_TITLE = Pattern.compile(
            "[\\d.,]+[a-zA-Z]?\\s*/\\s*[\\d.,]+[a-zA-Z]?\\s*❤|INVULNERABLE", Pattern.CASE_INSENSITIVE);

    private static boolean isDamageTitle(String text) {
        String stripped = TextUtil.stripColor(text);
        return DAMAGE_TITLE.matcher(stripped).find() || BOSS_HP_TITLE.matcher(stripped).find();
    }

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
            if (PhantomConfig.isHideDamageTitleEnabled() && isDamageTitle(text)) {
                ci.cancel();
                return;
            }
        }
        if (subtitle != null) {
            String subText = subtitle.getString();
            if (PearlTitleListener.isMatchingTitle(subText)) {
                PearlTitleListener.setActiveComponent(subtitle);
                ci.cancel();
                return;
            }
            if (PhantomConfig.isHideDamageTitleEnabled() && isDamageTitle(subText)) {
                ci.cancel();
            }
        }
    }
}
