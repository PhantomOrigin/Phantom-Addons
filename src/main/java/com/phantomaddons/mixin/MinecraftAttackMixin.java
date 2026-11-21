package com.phantomaddons.mixin;

import com.phantomaddons.features.abilities.AbilityCooldowns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {
    @Shadow public LocalPlayer player;

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void phantomaddons$abilityAttack(CallbackInfoReturnable<Boolean> cir) {
        if (player != null) AbilityCooldowns.onAttack(player);
    }
}
