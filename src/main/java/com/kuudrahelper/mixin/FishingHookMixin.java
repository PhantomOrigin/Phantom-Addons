package com.kuudrahelper.mixin;

import com.kuudrahelper.KuudraConfig;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
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
        return KuudraConfig.isLavaBobberFixEnabled() && fluidState.is(FluidTags.LAVA);
    }

    @Redirect(method = "onSyncedDataUpdated",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntity(I)Lnet/minecraft/world/entity/Entity;"))
    private Entity phantomaddons$blockFakeHookEntity(Level level, int id) {
        Entity entity = level.getEntity(id);
        if (entity == null || !KuudraConfig.isLavaBobberFixEnabled()) return entity;
        if (!(entity instanceof Player)) return entity;
        if (entity.getId() != ((Entity)(Object)this).getId() + 1) return entity;
        return null;
    }

    @Inject(method = "recreateFromPacket", at = @At("RETURN"))
    private void phantomaddons$snapFishingHookInterpolation(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        if (!KuudraConfig.isLavaBobberFixEnabled() && !KuudraConfig.isLegacyRodPhysicsEnabled()) return;
        ((FishingHook)(Object)this).getInterpolation().setInterpolationLength(1);
    }
}
