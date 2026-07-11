package com.kuudrahelper.features.customisation.lava;

import com.kuudrahelper.KuudraConfig;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class LavaRenderHandler implements FluidRenderHandler {

    private final FluidRenderHandler lavaParent;
    private final FluidRenderHandler waterParent;

    public LavaRenderHandler(FluidRenderHandler lavaParent,
                             FluidRenderHandler waterParent) {
        this.lavaParent  = lavaParent;
        this.waterParent = waterParent;
    }

    @Override
    public void renderFluid(FluidRenderer fluidRenderer,
                            BlockPos pos,
                            BlockAndTintGetter level,
                            FluidRenderer.Output output,
                            BlockState blockState,
                            FluidState fluidState) {
        if (KuudraConfig.isLavaAsWater()) {
            waterParent.renderFluid(
                    fluidRenderer,
                    pos,
                    level,
                    output,
                    blockState,
                    toWaterState(fluidState)
            );
            return;
        }

        lavaParent.renderFluid(fluidRenderer, pos, level, output, blockState, fluidState);
    }

    private static FluidState toWaterState(FluidState lavaState) {
        boolean falling = lavaState.getValueOrElse(FlowingFluid.FALLING, false);

        if (lavaState.isSource()) {
            return Fluids.WATER.getSource(falling);
        }

        return Fluids.WATER.getFlowing(lavaState.getAmount(), falling);
    }
}
