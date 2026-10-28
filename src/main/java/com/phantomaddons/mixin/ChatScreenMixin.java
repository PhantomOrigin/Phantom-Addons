package com.phantomaddons.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.phantomaddons.features.pv.ChatCards;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Inject(method = "handleComponentClicked(Lnet/minecraft/network/chat/Style;Z)Z", at = @At("HEAD"), cancellable = true)
    private void phantomaddons$nameClick(net.minecraft.network.chat.Style style, boolean insertion, CallbackInfoReturnable<Boolean> cir) {
        if (insertion || !com.phantomaddons.PhantomConfig.isPvClickNames() || !com.phantomaddons.PhantomConfig.isPvEnabled()) return;
        String name = com.phantomaddons.features.pv.ChatNameLinks.nameFor(style);
        if (name != null && com.phantomaddons.features.pv.PvCommand.intercept("pv " + name)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true)
    private void phantomaddons$cardClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && ChatCards.click(event.x(), event.y(), doubleClick)) cir.setReturnValue(true);
    }
}
