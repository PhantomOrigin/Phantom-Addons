package com.phantomaddons.mixin;

import com.phantomaddons.PhantomConfig;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityFireMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void phantomaddons$hideEntityFire(T entity, S state, float partialTick, CallbackInfo ci) {
        if (PhantomConfig.isHideEntityFireEnabled()) {
            state.displayFireAnimation = false;
        }
    }
}
