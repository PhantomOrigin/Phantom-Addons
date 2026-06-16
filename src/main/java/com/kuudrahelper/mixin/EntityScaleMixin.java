package com.kuudrahelper.mixin;

import com.kuudrahelper.KuudraConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class EntityScaleMixin {

    @Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("HEAD"))
    private void phantomaddons$scalePlayer(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        float scale;
        if (mc.player != null && state.id == mc.player.getId()) {
            scale = KuudraConfig.getSelfPlayerScale() / 100.0f;
        } else {
            scale = KuudraConfig.getOtherPlayerScale() / 100.0f;
        }
        if (Math.abs(scale - 1.0f) >= 0.001f) {
            poseStack.scale(scale, scale, scale);
        }
    }
}
