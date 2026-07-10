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

    // 0 = neither, 1 = lava, 2 = water
    @Unique
    private static final ThreadLocal<Integer> kuudra$implFluidKind =
            ThreadLocal.withInitial(() -> 0);

    // ── Capture lava/water flag at render entry ──────────────────────────────

    @ModifyVariable(
            method = "render",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private FluidState kuudra$captureFluid(FluidState state) {
        if (state != null && (state.getType() == Fluids.LAVA || state.getType() == Fluids.FLOWING_LAVA)) {
            kuudra$implFluidKind.set(1);
        } else if (state != null && (state.getType() == Fluids.WATER || state.getType() == Fluids.FLOWING_WATER)) {
            kuudra$implFluidKind.set(2);
        } else {
            kuudra$implFluidKind.set(0);
        }
        return state;
    }


    @ModifyVariable(
            method = "render",
            at = @At("STORE"),
            ordinal = 0,
            remap = false
    )
    private Material kuudra$swapFluidMaterialInImpl(Material material) {
        int kind = kuudra$implFluidKind.get();
        if (kind == 1 && (KuudraConfig.getLavaOpacity() < 0.999f || KuudraConfig.isLavaAsWater())) {
            return DefaultMaterials.TRANSLUCENT;
        }
        if (kind == 2 && (KuudraConfig.getWaterOpacity() < 0.999f || KuudraConfig.isWaterAsLava())) {
            return DefaultMaterials.TRANSLUCENT;
        }
        return material;
    }
}
