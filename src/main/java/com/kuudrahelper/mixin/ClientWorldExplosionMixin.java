package com.kuudrahelper.mixin;

import com.kuudrahelper.KuudraConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ClientWorldExplosionMixin {

    @Inject(
            method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"),
            cancellable = true
    )
    private <T extends ParticleOptions> void phantomaddons$filterExplosion(
            T parameters,
            double x, double y, double z,
            double vx, double vy, double vz,
            CallbackInfoReturnable<Particle> cir) {

        if (!KuudraConfig.isExplosionFilterEnabled()) return;
        if (parameters != ParticleTypes.EXPLOSION
                && parameters != ParticleTypes.EXPLOSION_EMITTER) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float radius = KuudraConfig.getExplosionHideRadius();
        if (mc.player.distanceToSqr(x, y, z) <= (double)(radius * radius)) {
            cir.setReturnValue(null);
        }
    }
}
