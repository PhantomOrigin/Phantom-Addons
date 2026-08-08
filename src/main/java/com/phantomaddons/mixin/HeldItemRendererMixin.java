package com.phantomaddons.mixin;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.customisation.items.ItemCustomization;
import com.phantomaddons.features.customisation.items.ItemRenderState;
import com.phantomaddons.features.customisation.items.ItemTransformSettings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {

    @Shadow private float mainHandHeight;
    @Shadow private float oMainHandHeight;
    @Shadow private ItemStack mainHandItem;

    @Unique private static float kuudrahelper$capturedSwing = 0f;
    @Unique private static float kuudrahelper$capturedPartialTick = 0f;

    @Inject(method = "tick", at = @At("RETURN"))
    private void kuudrahelper$noEquipAnimTick(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !PhantomConfig.isItemCustomizationEnabled()) return;

        ItemStack playerItem  = mc.player.getMainHandItem();
        ItemTransformSettings sPlayer   = ItemCustomization.resolveSettings(playerItem);
        ItemTransformSettings sRenderer = ItemCustomization.resolveSettings(mainHandItem);

        boolean playerNoEquip   = sPlayer   != null && sPlayer.noEquipAnimation;
        boolean rendererNoEquip = sRenderer != null && sRenderer.noEquipAnimation;
        if (!playerNoEquip && !rendererNoEquip) return;

        if (mainHandItem != playerItem) {
            mainHandHeight  = 0.0f;
            oMainHandHeight = 0.0f;
        } else if (playerNoEquip) {
            mainHandHeight  = 1.0f;
            oMainHandHeight = 1.0f;
        }
    }

    @ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float kuudrahelper$capturePartialTick(float partialTick) {
        kuudrahelper$capturedPartialTick = partialTick;
        return partialTick;
    }

    @ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float kuudrahelper$captureSwing(float swingProgress) {
        kuudrahelper$capturedSwing = swingProgress;
        return swingProgress;
    }

    @ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private float kuudrahelper$noEquipAnim(float equipProgress) {
        if (!PhantomConfig.isItemCustomizationEnabled()) return equipProgress;
        ItemTransformSettings s = ItemCustomization.resolveSettings(mainHandItem);
        if (s == null || !s.noEquipAnimation) return equipProgress;
        return 0f;
    }

    @ModifyVariable(method = "submitArmWithItem",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private PoseStack kuudrahelper$applyPosRot(PoseStack matrices) {
        if (!PhantomConfig.isItemCustomizationEnabled()) return matrices;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return matrices;

        ItemTransformSettings s = ItemCustomization.resolveSettings(mainHandItem);
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

        if (s.inPlaceSwing && kuudrahelper$capturedSwing > 0f) {
            float t = kuudrahelper$capturedSwing;
            int armSign = mc.player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
            float f5 = -0.4f * (float) Math.sin(Math.sqrt(t) * Math.PI);
            float f6 =  0.2f * (float) Math.sin(Math.sqrt(t) * 2.0 * Math.PI);
            float f7 = -0.2f * (float) Math.sin(t * Math.PI);
            matrices.translate(-armSign * f5, -f6, -f7);
        }

        if (s.staticPosition) {
            LocalPlayer p = mc.player;
            float pt = kuudrahelper$capturedPartialTick;
            float xBob = Mth.lerp(pt, p.xBobO, p.xBob);
            float yBob = Mth.lerp(pt, p.yBobO, p.yBob);
            float viewX = p.getViewXRot(pt);
            float viewY = p.getViewYRot(pt);
            matrices.mulPose(Axis.YP.rotationDegrees(-((viewY - yBob) * 0.1f)));
            matrices.mulPose(Axis.XP.rotationDegrees(-((viewX - xBob) * 0.1f)));
        }

        return matrices;
    }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void kuudrahelper$exposeSettings(CallbackInfo ci) {
        if (!PhantomConfig.isItemCustomizationEnabled()) {
            ItemRenderState.currentFirstPerson = null;
            return;
        }
        ItemRenderState.currentFirstPerson = ItemCustomization.resolveSettings(mainHandItem);
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"))
    private void kuudrahelper$clearSettings(CallbackInfo ci) {
        ItemRenderState.currentFirstPerson = null;
    }

}
