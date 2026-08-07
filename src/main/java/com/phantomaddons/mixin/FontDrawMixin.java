package com.phantomaddons.mixin;

import com.phantomaddons.features.misckuudra.ShitterList;
import com.phantomaddons.features.customisation.VisualWords;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(Font.class)
public class FontDrawMixin {

    @ModifyVariable(
            method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence phantomaddons$visualWords(FormattedCharSequence seq) {
        return VisualWords.apply(seq);
    }

    @ModifyVariable(
            method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence phantomaddons$shitterList(FormattedCharSequence seq) {
        return ShitterList.apply(seq);
    }

    // drawInBatch no longer exists in 26.2 (Font lost all rendering methods, only prepareText remains) —
    // prepareText is now the single choke point all text (2D GUI and 3D world) funnels through before
    // being submitted via SubmitNodeCollector.submitText, so the centering shift moves here too. This
    // reads the FormattedCharSequence local after the two content-substitution mixins above have already
    // run (since they're declared first), meaning the shift is computed against the actual substituted
    // text rather than the pre-substitution original — centers what's really drawn, which is the more
    // correct behavior anyway.
    @ModifyVariable(
            method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float phantomaddons$recenter(float x, @Local(argsOnly = true) FormattedCharSequence text) {
        return x + VisualWords.centerShift((Font) (Object) this, text);
    }
}
