package com.kuudrahelper.mixin;

import com.kuudrahelper.KuudraConfig;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(FishingHookRenderer.class)
public abstract class FishingHookRendererMixin {

    @Inject(method = "getPlayerHandPos", at = @At("HEAD"), cancellable = true)
    private void phantomaddons$legacyHandPos(Player player, float bobArg, float partialTicks,
                                             CallbackInfoReturnable<Vec3> cir) {
        if (!KuudraConfig.isLegacyRodPhysicsEnabled()) return;

        int arm = FishingHookRenderer.getHoldingArm(player) == HumanoidArm.RIGHT ? 1 : -1;
        float yBodyRot = Mth.lerp(partialTicks, player.yBodyRotO, player.yBodyRot) * 0.017453292f;
        double sin = Mth.sin(yBodyRot);
        double cos = Mth.cos(yBodyRot);
        float scale = player.getScale();
        double d0 = arm * 0.35 * scale;
        double d1 = 0.8 * scale;
        float crouchOffset = player.isCrouching() ? -0.1875f : 0f;

        Vec3 result = player.getEyePosition(partialTicks).add(
                -cos * d0 - sin * d1,
                crouchOffset - 0.45 * scale,
                -sin * d0 + cos * d1);
        cir.setReturnValue(result);
    }
}
