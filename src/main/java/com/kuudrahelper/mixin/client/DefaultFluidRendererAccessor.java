// DefaultFluidRendererAccessor.java
package com.kuudrahelper.mixin.client;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = DefaultFluidRenderer.class, remap = false)
public interface DefaultFluidRendererAccessor {
    @Accessor("quadColors")
    int[] getQuadColors();

    @Accessor("brightness")
    float[] getBrightness();
}