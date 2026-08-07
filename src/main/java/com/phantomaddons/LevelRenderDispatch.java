package com.phantomaddons;

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
import com.phantomaddons.features.miscskyblock.BobberComparisonRenderer;
import com.phantomaddons.features.boss.IchorRadiusRenderer;
import com.phantomaddons.features.render.CannonHitboxRenderer;
import com.phantomaddons.features.stundps.DpsWaypoint;
import com.phantomaddons.utils.WorldRenderCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

// Replaces the old WorldRendererMixin (which injected at the TAIL of LevelRenderer.renderLevel).
// 26.2 restructured world rendering around a deferred submit/frame-graph model — mods no longer get
// an immediate MultiBufferSource to draw into at an arbitrary point; instead Fabric API exposes
// LevelRenderEvents.COLLECT_SUBMITS, which hands over a SubmitNodeCollector valid only for the
// current frame. That collector is stashed in WorldRenderCollector so every renderer below can keep
// using the same "grab what I need and draw" shape they always have.
public final class LevelRenderDispatch {

    private LevelRenderDispatch() {}

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            Camera camera = mc.gameRenderer.mainCamera();
            if (camera == null) return;

            WorldRenderCollector.set(ctx.submitNodeCollector());

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
            IchorRadiusRenderer.render(matrices, camera, tickDelta);
            CannonHitboxRenderer.render(matrices, camera, tickDelta);
            DpsWaypoint.render(matrices, camera, tickDelta);
            BobberComparisonRenderer.render(matrices, camera);
            PredictedBobber.tick();

            WorldRenderCollector.set(null);
        });
    }
}
