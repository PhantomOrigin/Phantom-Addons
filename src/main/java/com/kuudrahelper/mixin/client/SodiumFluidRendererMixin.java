package com.kuudrahelper.mixin.client;

import com.kuudrahelper.KuudraConfig;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DefaultFluidRenderer.class, remap = false)
public class SodiumFluidRendererMixin {

    @Unique
    private static final boolean IRIS_LOADED =
            FabricLoader.getInstance().isModLoaded("iris");

    @Unique
    private static final ThreadLocal<Boolean> kuudra$isLava =
            ThreadLocal.withInitial(() -> false);

    // ── Detect lava ──────────────────────────────────────────────────────────

    @ModifyVariable(
            method = "render",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private FluidState kuudra$captureFluid(FluidState state) {
        kuudra$isLava.set(state != null
                && (state.getType() == Fluids.LAVA
                || state.getType() == Fluids.FLOWING_LAVA));
        return state;
    }

    @ModifyVariable(
            method = "writeQuad",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private Material kuudra$swapMaterial(Material material) {
        if (kuudra$isLava.get()
                && (KuudraConfig.getLavaOpacity() < 0.999f || KuudraConfig.isLavaAsWater())) {
            return DefaultMaterials.TRANSLUCENT;
        }
        return material;
    }

    // ── Apply color/opacity after updateQuad ─────────────────────────────────

    @Inject(
            method = "updateQuad",
            at = @At("RETURN"),
            remap = false
    )
    private void kuudra$applyLavaColor(CallbackInfo ci) {
        if (!kuudra$isLava.get()) return;

        if (IRIS_LOADED && kuudra$irisHasShadersActive()) return;

        int[] quadColors = ((DefaultFluidRendererAccessor) (Object) this).getQuadColors();
        float[] brightnessArr = ((DefaultFluidRendererAccessor) (Object) this).getBrightness();

        float opacity = KuudraConfig.getLavaOpacity();
        int alpha = Math.round(opacity * 255f) & 0xFF;

        for (int i = 0; i < 4; i++) {
            int abgr = quadColors[i];

            if (KuudraConfig.isLavaColorOverride()) {
                int userColor = KuudraConfig.getLavaColor();
                int ur = (userColor >> 16) & 0xFF;
                int ug = (userColor >>  8) & 0xFF;
                int ub =  userColor        & 0xFF;

                int finalR, finalG, finalB;

                if (!KuudraConfig.isLavaAsWater()) {
                    int sr =  abgr        & 0xFF;
                    int sg = (abgr >>  8) & 0xFF;
                    int sb = (abgr >> 16) & 0xFF;
                    int luma = Math.max(0, Math.min(255,
                            (int) Math.round(0.2126 * sr + 0.7152 * sg + 0.0722 * sb)));
                    finalR = (luma * ur) / 255;
                    finalG = (luma * ug) / 255;
                    finalB = (luma * ub) / 255;
                } else {
                    int sr =  abgr        & 0xFF;
                    int sg = (abgr >>  8) & 0xFF;
                    int sb = (abgr >> 16) & 0xFF;
                    finalR = (sr * ur) / 255;
                    finalG = (sg * ug) / 255;
                    finalB = (sb * ub) / 255;
                }

                quadColors[i] = (alpha << 24) | (finalB << 16) | (finalG << 8) | finalR;
                brightnessArr[i] = 1.0f;
            } else {
                quadColors[i] = (abgr & 0x00FFFFFF) | (alpha << 24);
            }
        }
    }

    @Unique
    private static boolean kuudra$irisHasShadersActive() {
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Object pipeline = iris.getMethod("getPipelineManager").invoke(null);
            if (pipeline == null) return false;
            Object activePipeline = pipeline.getClass()
                    .getMethod("getPipelineNow").invoke(pipeline);
            return activePipeline != null;
        } catch (Exception e) {
            return false;
        }
    }
}
