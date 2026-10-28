package com.phantomaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.customisation.items.ItemCustomization;
import com.phantomaddons.features.customisation.items.ItemTransformSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class FirstPersonHandsMixin {

    @ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private float kuudrahelper$noEquipAnim(float equipProgress,
                                           @Local(argsOnly = true) FirstPersonHandsAndItemsRenderState state) {
        if (!PhantomConfig.isItemCustomizationEnabled()) return equipProgress;
        ItemTransformSettings s = ItemCustomization.resolveSettings(state.mainHandItem);
        if (s == null || !s.noEquipAnimation) return equipProgress;
        return 0f;
    }

    @ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private PoseStack kuudrahelper$applyPosRot(PoseStack matrices,
                                               @Local(argsOnly = true) FirstPersonHandsAndItemsRenderState state,
                                               @Local(argsOnly = true, ordinal = 0) float partialTick,
                                               @Local(argsOnly = true, ordinal = 2) float swingProgress) {
        if (!PhantomConfig.isItemCustomizationEnabled()) return matrices;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return matrices;

        ItemTransformSettings s = ItemCustomization.resolveSettings(state.mainHandItem);
        if (s == null) return matrices;

        if (s.posX != 0 || s.posY != 0 || s.posZ != 0)
            matrices.translate(s.posX, s.posY, s.posZ);
        if (s.proximity != 0)
            matrices.translate(0, 0, s.proximity);
        if (s.rotX != 0) matrices.rotate(Axis.XP.rotationDegrees(s.rotX));
        if (s.rotY != 0) matrices.rotate(Axis.YP.rotationDegrees(s.rotY));
        if (s.rotZ != 0) matrices.rotate(Axis.ZP.rotationDegrees(s.rotZ));

        float sc = (s.scale > 0.001f) ? s.scale : 1f;
        if (Math.abs(sc - 1f) >= 0.01f)
            matrices.scale(sc, sc, sc);

        if (s.inPlaceSwing && swingProgress > 0f) {
            float t = swingProgress;
            int armSign = mc.player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
            float f5 = -0.4f * (float) Math.sin(Math.sqrt(t) * Math.PI);
            float f6 =  0.2f * (float) Math.sin(Math.sqrt(t) * 2.0 * Math.PI);
            float f7 = -0.2f * (float) Math.sin(t * Math.PI);
            matrices.translate(-armSign * f5, -f6, -f7);
        }

        if (s.staticPosition) {
            LocalPlayer p = mc.player;
            float xBob = Mth.lerp(partialTick, p.xBobO, p.xBob);
            float yBob = Mth.lerp(partialTick, p.yBobO, p.yBob);
            float viewX = p.getViewXRot(partialTick);
            float viewY = p.getViewYRot(partialTick);
            matrices.rotate(Axis.YP.rotationDegrees(-((viewY - yBob) * 0.1f)));
            matrices.rotate(Axis.XP.rotationDegrees(-((viewX - xBob) * 0.1f)));
        }

        return matrices;
    }

    @WrapOperation(method = "submitArmWithItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void kuudrahelper$scaleItem(ItemStackRenderState itemState, PoseStack matrices, SubmitNodeCollector collector,
                                        int light, int overlay, int outlineColor, Operation<Void> original,
                                        @Local(argsOnly = true) FirstPersonHandsAndItemsRenderState state) {
        if (PhantomConfig.isItemCustomizationEnabled()) {
            ItemTransformSettings s = ItemCustomization.resolveSettings(state.mainHandItem);
            if (s != null) {
                float sc = (s.scale > 0.001f) ? s.scale : 1f;
                if (Math.abs(sc - 1f) >= 0.01f) matrices.scale(sc, sc, sc);
            }
        }
        original.call(itemState, matrices, collector, light, overlay, outlineColor);
    }
}
