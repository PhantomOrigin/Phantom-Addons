package com.kuudrahelper.mixin;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.customisation.items.ItemCustomization;
import com.kuudrahelper.features.customisation.items.ItemRenderState;
import com.kuudrahelper.features.customisation.items.ItemTransformSettings;
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

    // Control equip animation height each tick.
    // When the renderer item matches the player item (stable) and noEquipAnimation is on,
    // keep height at 1 (fully raised). While transitioning (renderer hasn't swapped yet),
    // force height to 0 so vanilla's swap condition (< 0.1) fires on the next tick,
    // giving an instant item switch instead of a multi-tick fade.
    @Inject(method = "tick", at = @At("RETURN"))
    private void kuudrahelper$noEquipAnimTick(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !KuudraConfig.isItemCustomizationEnabled()) return;

        ItemStack playerItem  = mc.player.getMainHandItem();
        ItemTransformSettings sPlayer   = ItemCustomization.resolveSettings(playerItem);
        ItemTransformSettings sRenderer = ItemCustomization.resolveSettings(mainHandItem);

        boolean playerNoEquip   = sPlayer   != null && sPlayer.noEquipAnimation;
        boolean rendererNoEquip = sRenderer != null && sRenderer.noEquipAnimation;
        if (!playerNoEquip && !rendererNoEquip) return;

        if (mainHandItem != playerItem) {
            // Renderer hasn't swapped yet — force to 0 so vanilla can swap next tick
            mainHandHeight  = 0.0f;
            oMainHandHeight = 0.0f;
        } else if (playerNoEquip) {
            // Renderer is showing the correct item — keep it fully raised
            mainHandHeight  = 1.0f;
            oMainHandHeight = 1.0f;
        }
    }

    // Capture partialTick so applyPosRot can compute the bob counter-rotation
    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float kuudrahelper$capturePartialTick(float partialTick) {
        kuudrahelper$capturedPartialTick = partialTick;
        return partialTick;
    }

    // Capture swing progress (passed through unchanged; in-place handled by counter-translate)
    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float kuudrahelper$captureSwing(float swingProgress) {
        kuudrahelper$capturedSwing = swingProgress;
        return swingProgress;
    }

    // equipProgress 0 = fully raised/normal position (vanilla lerp gives 0 at steady state)
    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private float kuudrahelper$noEquipAnim(float equipProgress) {
        if (!KuudraConfig.isItemCustomizationEnabled()) return equipProgress;
        ItemTransformSettings s = ItemCustomization.resolveSettings(mainHandItem);
        if (s == null || !s.noEquipAnimation) return equipProgress;
        return 0f;
    }

    @ModifyVariable(method = "renderArmWithItem",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private PoseStack kuudrahelper$applyPosRot(PoseStack matrices) {
        if (!KuudraConfig.isItemCustomizationEnabled()) return matrices;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return matrices;

        // Use mainHandItem (what the renderer is actually drawing), not player's current item.
        // These differ during the swap transition, so resolving from mainHandItem is correct.
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

        // In-place swing: vanilla rotations play normally, but the arm's position drift is
        // cancelled by pre-applying the inverse of swingArm's translation. Translations
        // commute, so this correctly zeroes out the positional movement while leaving all
        // applyItemArmAttackTransform rotations intact (Devonian-style).
        if (s.inPlaceSwing && kuudrahelper$capturedSwing > 0f) {
            float t = kuudrahelper$capturedSwing;
            int armSign = mc.player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
            float f5 = -0.4f * (float) Math.sin(Math.sqrt(t) * Math.PI);
            float f6 =  0.2f * (float) Math.sin(Math.sqrt(t) * 2.0 * Math.PI);
            float f7 = -0.2f * (float) Math.sin(t * Math.PI);
            matrices.translate(-armSign * f5, -f6, -f7);
        }

        // Static position: undo the (viewRot - bobRot)*0.1 rotations from renderHandsWithItems
        if (s.staticPosition) {
            LocalPlayer p = mc.player;
            float pt = kuudrahelper$capturedPartialTick;
            float xBob = Mth.lerp(pt, p.xBobO, p.xBob);
            float yBob = Mth.lerp(pt, p.yBobO, p.yBob);
            float viewX = p.getViewXRot(pt);
            float viewY = p.getViewYRot(pt);
            // Undo in reverse application order (Y was applied after X, so undo Y first)
            matrices.mulPose(Axis.YP.rotationDegrees(-((viewY - yBob) * 0.1f)));
            matrices.mulPose(Axis.XP.rotationDegrees(-((viewX - xBob) * 0.1f)));
        }

        return matrices;
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void kuudrahelper$exposeSettings(CallbackInfo ci) {
        if (!KuudraConfig.isItemCustomizationEnabled()) {
            ItemRenderState.currentFirstPerson = null;
            return;
        }
        ItemRenderState.currentFirstPerson = ItemCustomization.resolveSettings(mainHandItem);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void kuudrahelper$clearSettings(CallbackInfo ci) {
        ItemRenderState.currentFirstPerson = null;
    }

}
