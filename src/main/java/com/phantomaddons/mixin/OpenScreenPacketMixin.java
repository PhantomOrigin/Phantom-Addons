package com.phantomaddons.mixin;

import com.phantomaddons.features.loadouts.WardrobeKeybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class OpenScreenPacketMixin {

    @Inject(method = "handleOpenScreen", at = @At("TAIL"))
    private void phantomaddons$openScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        WardrobeKeybinds.onOpenScreenPacket(Minecraft.getInstance(), packet.getTitle().getString());
    }
}
