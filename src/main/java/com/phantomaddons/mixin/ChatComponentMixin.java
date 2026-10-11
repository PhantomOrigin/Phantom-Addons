package com.phantomaddons.mixin;

import com.phantomaddons.features.pv.ChatCardView;
import com.phantomaddons.features.pv.ChatCards;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At("RETURN"))
    private void phantomaddons$cards(ChatComponent.ChatGraphicsAccess access, int a, int b, ChatComponent.DisplayMode mode, CallbackInfo ci) {
        ChatCards.setChat((ChatComponent) (Object) this);
        if (access instanceof ChatCardView view) ChatCards.flush(view);
        else ChatCards.discard();
    }
}
