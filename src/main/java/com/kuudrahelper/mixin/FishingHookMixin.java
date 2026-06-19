package com.kuudrahelper.mixin;

import com.kuudrahelper.KuudraConfig;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    @Redirect(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean phantomaddons$floatOnLava(FluidState fluidState, TagKey<Fluid> tag) {
        if (fluidState.is(tag)) return true;
        return KuudraConfig.isLavaBobberFixEnabled() && fluidState.is(FluidTags.LAVA);
    }
}
