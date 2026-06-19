package com.kuudrahelper.mixin;

import com.kuudrahelper.features.AnnounceFresh;
import com.kuudrahelper.features.BuildBeaconRenderer;
import com.kuudrahelper.features.kuudra.KuudraHighlightRenderer;
import com.kuudrahelper.features.supplies.EtherwarpWaypointRenderer;
import com.kuudrahelper.features.supplies.SupplyBeaconRenderer;
import com.kuudrahelper.features.supplies.SupplyGiantHitbox;
import com.kuudrahelper.features.supplies.SupplyRenderHelper;
import com.kuudrahelper.features.pearls.PearlWaypointRenderer;
import com.kuudrahelper.features.StunPreviewRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class WorldRendererMixin {

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void kuudrahelper$onRender(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        if (camera == null) return;

        float tickDelta = mc.getDeltaTracker().getRealtimeDeltaTicks();

        PoseStack matrices = new PoseStack();
        matrices.mulPose(new Quaternionf().rotationX(Mth.DEG_TO_RAD * camera.xRot()));
        matrices.mulPose(new Quaternionf().rotationY(Mth.DEG_TO_RAD * (camera.yRot() + 180.0f)));

        PearlWaypointRenderer.renderWorld(matrices, camera, tickDelta);
        KuudraHighlightRenderer.render(matrices, camera, tickDelta);
        StunPreviewRenderer.render(matrices, camera, tickDelta);
        BuildBeaconRenderer.render(matrices, camera, tickDelta);
        SupplyBeaconRenderer.render(matrices, camera, tickDelta);
        SupplyRenderHelper.render(matrices, camera, tickDelta);
        EtherwarpWaypointRenderer.render(matrices, camera, tickDelta);
        AnnounceFresh.renderTimers(matrices, camera, tickDelta);
        SupplyGiantHitbox.render(matrices, camera, tickDelta);
    }
}