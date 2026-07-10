package com.kuudrahelper.features.water;

import com.kuudrahelper.KuudraConfig;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class WaterRenderHandler implements FluidRenderHandler {

    private final FluidRenderHandler waterParent;
    private final FluidRenderHandler lavaParent;

    public WaterRenderHandler(FluidRenderHandler waterParent,
                              FluidRenderHandler lavaParent) {
        this.waterParent = waterParent;
        this.lavaParent  = lavaParent;
    }

    @Override
    public void renderFluid(FluidRenderer fluidRenderer,
                            BlockPos pos,
                            BlockAndTintGetter level,
                            FluidRenderer.Output output,
                            BlockState blockState,
                            FluidState fluidState) {
        if (KuudraConfig.isWaterAsLava()) {
            lavaParent.renderFluid(
                    fluidRenderer,
                    pos,
                    level,
                    output,
                    blockState,
                    toLavaState(fluidState)
            );
            return;
        }

        waterParent.renderFluid(fluidRenderer, pos, level, output, blockState, fluidState);
    }

    private static FluidState toLavaState(FluidState waterState) {
        boolean falling = waterState.getValueOrElse(FlowingFluid.FALLING, false);

        if (waterState.isSource()) {
            return Fluids.LAVA.getSource(falling);
        }

        return Fluids.LAVA.getFlowing(waterState.getAmount(), falling);
    }
}
