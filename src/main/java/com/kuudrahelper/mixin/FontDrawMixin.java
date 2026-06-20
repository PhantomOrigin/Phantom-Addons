package com.kuudrahelper.mixin;

import com.kuudrahelper.features.VisualWords;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Visual Words text hooks on Font.
 *  - prepareText: universal replacement chokepoint (2D UI + in-world / nametags).
 *  - drawInBatch x: re-centre nametags whose text length changed due to a replacement.
 *    Only shifts text that actually changed, so left-aligned text is left alone.
 */
@Mixin(Font.class)
public class FontDrawMixin {

    @ModifyVariable(
            method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence phantomaddons$visualWords(FormattedCharSequence seq) {
        return VisualWords.apply(seq);
    }

    @ModifyVariable(
            method = "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4fc;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
            at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float phantomaddons$recenterComponent(float x, @Local(argsOnly = true) Component text) {
        return x + VisualWords.centerShift((Font) (Object) this, text.getVisualOrderText());
    }

    @ModifyVariable(
            method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4fc;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
            at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float phantomaddons$recenterFcs(float x, @Local(argsOnly = true) FormattedCharSequence text) {
        return x + VisualWords.centerShift((Font) (Object) this, text);
    }
}
