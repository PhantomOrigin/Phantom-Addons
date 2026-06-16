package com.kuudrahelper.mixin;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.items.ItemCustomization;
import com.kuudrahelper.features.items.ItemRenderState;
import com.kuudrahelper.features.items.ItemTransformSettings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {

    @ModifyVariable(method = "renderArmWithItem",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private PoseStack kuudrahelper$applyPosRot(PoseStack matrices) {
        if (!KuudraConfig.isItemCustomizationEnabled()) return matrices;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return matrices;

        ItemStack stack = mc.player.getMainHandItem();
        ItemTransformSettings s = ItemCustomization.resolveSettings(stack);
        if (s == null) return matrices;

        if (s.posX != 0 || s.posY != 0 || s.posZ != 0)
            matrices.translate(s.posX, s.posY, s.posZ);
        if (s.proximity != 0)
            matrices.translate(0, 0, s.proximity);
        if (s.rotX != 0) matrices.mulPose(Axis.XP.rotationDegrees(s.rotX));
        if (s.rotY != 0) matrices.mulPose(Axis.YP.rotationDegrees(s.rotY));
        if (s.rotZ != 0) matrices.mulPose(Axis.ZP.rotationDegrees(s.rotZ));

        float sc = (s.scale > 0.001f) ? s.scale : 1f;
        if (Math.abs(sc - 1f) >= 0.01f)
            matrices.scale(sc, sc, sc);

        return matrices;
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void kuudrahelper$exposeSettings(CallbackInfo ci) {
        if (!KuudraConfig.isItemCustomizationEnabled()) {
            ItemRenderState.currentFirstPerson = null;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { ItemRenderState.currentFirstPerson = null; return; }

        ItemStack stack = mc.player.getMainHandItem();
        ItemRenderState.currentFirstPerson = ItemCustomization.resolveSettings(stack);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void kuudrahelper$clearSettings(CallbackInfo ci) {
        ItemRenderState.currentFirstPerson = null;
    }

}
