package com.phantomaddons.mixin;

import com.phantomaddons.features.build.AnnounceFresh;
import com.phantomaddons.features.build.BuildBeaconRenderer;
import com.phantomaddons.features.build.ElleHighlightRenderer;
import com.phantomaddons.features.boss.KuudraHighlightRenderer;
import com.phantomaddons.features.boss.bonetiming.BoneTimingHitboxOutline;
import com.phantomaddons.features.supplies.etherwarp.EtherwarpWaypointRenderer;
import com.phantomaddons.features.supplies.SupplyBeaconRenderer;
import com.phantomaddons.features.supplies.giant.GiantHitboxOutline;
import com.phantomaddons.features.supplies.giant.SupplyGiantHitbox;
import com.phantomaddons.features.supplies.SupplyRenderHelper;
import com.phantomaddons.features.supplies.pearlwaypoints.PearlWaypointRenderer;
import com.phantomaddons.features.supplies.waypointlines.WaypointLinesRenderer;
import com.phantomaddons.features.stundps.StunPreviewRenderer;
import com.phantomaddons.features.miscskyblock.PredictedBobber;
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
        ElleHighlightRenderer.render(matrices, camera, tickDelta);
        SupplyBeaconRenderer.render(matrices, camera, tickDelta);
        SupplyRenderHelper.render(matrices, camera, tickDelta);
        EtherwarpWaypointRenderer.render(matrices, camera, tickDelta);
        AnnounceFresh.renderTimers(matrices, camera, tickDelta);
        SupplyGiantHitbox.render(matrices, camera, tickDelta);
        GiantHitboxOutline.render(matrices, camera, tickDelta);
        WaypointLinesRenderer.render(matrices, camera, tickDelta);
        BoneTimingHitboxOutline.render(matrices, camera, tickDelta);
        PredictedBobber.tick();
    }
}