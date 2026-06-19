package com.kuudrahelper.mixin;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class KuudraScaleMixin<T extends LivingEntity, S extends LivingEntityRenderState, M> {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void phantomaddons$scaleAndFilter(T entity, S state, float partialTick, CallbackInfo ci) {
        if (KuudraConfig.isHideDeadEntitiesEnabled()
                && !(entity instanceof Player)
                && entity instanceof Monster
                && entity.deathTime > 0) {
            state.scale = 0f;
            return;
        }

        if (KuudraPhaseTracker.getPhase() == KuudraPhaseTracker.Phase.NONE) return;
        if (!entity.isAlive()) return;
        if (!(entity instanceof Mob mob)) return;
        if (!hasArmor(mob) && !(mob instanceof ZombifiedPiglin)) return;

        float scale = KuudraConfig.getKuudraSizeScale() / 100.0f;
        state.scale *= scale;
    }

    private static boolean hasArmor(Mob mob) {
        return !mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
            || !mob.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
            || !mob.getItemBySlot(EquipmentSlot.LEGS).isEmpty();
    }
}
