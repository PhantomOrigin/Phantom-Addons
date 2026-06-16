package com.kuudrahelper.mixin.client;

import com.kuudrahelper.KuudraConfig;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(
        targets = "net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl",
        remap = false
)
public class SodiumFluidRendererImplMixin {

    @Unique
    private static final ThreadLocal<Boolean> kuudra$implIsLava =
            ThreadLocal.withInitial(() -> false);

    // ── Capture lava flag at render entry ────────────────────────────────────

    @ModifyVariable(
            method = "render",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private FluidState kuudra$captureFluid(FluidState state) {
        kuudra$implIsLava.set(state != null
                && (state.getType() == Fluids.LAVA
                || state.getType() == Fluids.FLOWING_LAVA));
        return state;
    }


    @ModifyVariable(
            method = "render",
            at = @At("STORE"),
            ordinal = 0,
            remap = false
    )
    private Material kuudra$swapLavaMaterialInImpl(Material material) {
        if (kuudra$implIsLava.get()
                && (KuudraConfig.getLavaOpacity() < 0.999f || KuudraConfig.isLavaAsWater())) {
            return DefaultMaterials.TRANSLUCENT;
        }
        return material;
    }
}
