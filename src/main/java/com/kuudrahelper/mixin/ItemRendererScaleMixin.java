package com.kuudrahelper.mixin;

import com.kuudrahelper.features.items.ItemRenderState;
import com.kuudrahelper.features.items.ItemTransformSettings;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemInHandRenderer.class)
public class ItemRendererScaleMixin {

    @ModifyVariable(method = "renderItem",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private PoseStack kuudrahelper$applyScale(PoseStack matrices) {
        ItemTransformSettings s = ItemRenderState.currentFirstPerson;
        if (s == null) return matrices;

        float sc = (s.scale > 0.001f) ? s.scale : 1f;
        if (Math.abs(sc - 1f) < 0.01f) return matrices;

        matrices.scale(sc, sc, sc);
        return matrices;
    }
}