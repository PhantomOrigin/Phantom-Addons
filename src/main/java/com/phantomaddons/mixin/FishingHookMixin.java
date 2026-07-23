package com.phantomaddons.mixin;

import com.phantomaddons.PhantomConfig;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    @Redirect(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean phantomaddons$floatOnLava(FluidState fluidState, TagKey<Fluid> tag) {
        if (fluidState.is(tag)) return true;
        return PhantomConfig.isLavaBobberFixEnabled() && fluidState.is(FluidTags.LAVA);
    }

    @Redirect(method = "onSyncedDataUpdated",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntity(I)Lnet/minecraft/world/entity/Entity;"))
    private Entity phantomaddons$blockFakeHookEntity(Level level, int id) {
        Entity entity = level.getEntity(id);
        if (entity == null || !PhantomConfig.isLavaBobberFixEnabled()) return entity;
        if (!(entity instanceof Player)) return entity;
        if (entity.getId() != ((Entity)(Object)this).getId() + 1) return entity;
        return null;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void phantomaddons$lavaBobberInterpolation(CallbackInfo ci) {
        if (!PhantomConfig.isLavaBobberFixEnabled()) return;
        FishingHook self = (FishingHook) (Object) this;
        boolean inLava = self.level().getFluidState(self.blockPosition()).is(FluidTags.LAVA);
        self.getInterpolation().setInterpolationLength(inLava ? 0 : InterpolationHandler.DEFAULT_INTERPOLATION_STEPS);
    }
}
