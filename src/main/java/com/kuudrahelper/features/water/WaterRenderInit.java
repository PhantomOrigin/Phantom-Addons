package com.kuudrahelper.features.water;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;

public class WaterRenderInit {

    public static void init(FluidRenderHandler waterParent, FluidRenderHandler lavaParent) {
        var waterModel = new FluidModel.Unbaked(
                new Material(Identifier.fromNamespaceAndPath("minecraft", "block/water_still")),
                new Material(Identifier.fromNamespaceAndPath("minecraft", "block/water_flow")),
                new Material(Identifier.fromNamespaceAndPath("minecraft", "block/water_overlay")),
                BlockTintSources.constant(0xFFFFFF)
        );

        FluidRenderingRegistry.register(
                Fluids.WATER, Fluids.FLOWING_WATER,
                waterModel,
                new WaterRenderHandler(waterParent, lavaParent)
        );
    }
}
