package com.phantomaddons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.phantomaddons.utils.ImmediateDraw;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin {

    @Inject(method = "submitCustomGeometry", at = @At("HEAD"), cancellable = true)
    private void phantomaddons$drawAfterTerrain(PoseStack stack, RenderType type,
                                                SubmitNodeCollector.CustomGeometryRenderer renderer,
                                                CallbackInfo ci) {
        if (!ImmediateDraw.isDrawingAfterTerrain()) return;
        ((SubmitNodeCollection) (Object) this).afterTerrain
                .submit(new CustomFeatureRenderer.Submit(stack.last().copy(), type, renderer));
        ci.cancel();
    }
}
