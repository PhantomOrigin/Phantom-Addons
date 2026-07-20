package com.phantomaddons.mixin;

import com.phantomaddons.features.render.HideArmorStands;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class ArmorStandRendererMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void phantomaddons$hideArmorStand(
            E entity, Frustum frustum, double camX, double camY, double camZ,
            CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof ArmorStand stand)) return;
        if (HideArmorStands.shouldHide(stand.getX(), stand.getZ(),
                stand.getCustomName() != null)) {
            cir.setReturnValue(false);
        }
    }
}
