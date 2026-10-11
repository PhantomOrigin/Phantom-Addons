package com.phantomaddons.mixin;

import com.phantomaddons.features.pv.ChatCardView;
import com.phantomaddons.features.pv.ChatCards;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess")
public abstract class ChatCardMixin implements ChatCardView {

    @Shadow @Final private GuiGraphicsExtractor graphics;
    @Shadow @Final private Vector2f localMousePos;
    @Shadow @Final private int globalMouseX;
    @Shadow @Final private int globalMouseY;

    @Inject(method = "handleMessage(IFLnet/minecraft/util/FormattedCharSequence;)Z", at = @At("HEAD"))
    private void phantomaddons$card(int y, float opacity, FormattedCharSequence line, CallbackInfoReturnable<Boolean> cir) {
        ChatCards.noteLine(y, opacity, line);
    }

    @Override public GuiGraphicsExtractor phantomaddons$graphics() { return graphics; }
    @Override public float phantomaddons$localX() { return localMousePos.x; }
    @Override public float phantomaddons$localY() { return localMousePos.y; }
    @Override public int phantomaddons$globalX() { return globalMouseX; }
    @Override public int phantomaddons$globalY() { return globalMouseY; }
}
