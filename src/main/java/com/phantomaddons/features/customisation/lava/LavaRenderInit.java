package com.phantomaddons.features.customisation.lava;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;

public class LavaRenderInit {

    public static void init(FluidRenderHandler lavaParent, FluidRenderHandler waterParent) {
        var lavaModel = new FluidModel.Unbaked(
                new Material(Identifier.fromNamespaceAndPath("minecraft", "block/lava_still")),
                new Material(Identifier.fromNamespaceAndPath("minecraft", "block/lava_flow")),
                new Material(Identifier.fromNamespaceAndPath("minecraft", "block/lava_still")),
                BlockTintSources.constant(0xFFFFFF)
        );

        FluidRenderingRegistry.register(
                Fluids.LAVA, Fluids.FLOWING_LAVA,
                lavaModel,
                new LavaRenderHandler(lavaParent, waterParent)
        );
    }
}
