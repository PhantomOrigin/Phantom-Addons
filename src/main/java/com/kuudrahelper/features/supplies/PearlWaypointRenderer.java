package com.kuudrahelper.features.supplies;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.KuudraConfig.WaypointType;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import com.kuudrahelper.phase.KuudraPhaseTracker.Phase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

public final class PearlWaypointRenderer {

    private static final float BEAM_WIDTH = 0.15f;
    private static final int   SEGS       = 32;

    private static boolean prevShouldThrow = false;

    private PearlWaypointRenderer() {}

    public static void renderWorld(PoseStack matrices, Camera camera, float tickDelta) {
        if (!KuudraConfig.isPearlWaypointsEnabled()) return;
        if (KuudraPhaseTracker.getPhase() != Phase.SUPPLIES) return;

        PearlWaypointManager.frameUpdate();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3  camPos    = camera.position();
        Vec3  spawn     = PearlWaypointManager.pearlSpawnPos(mc);
        float camPitch  = camera.xRot();
        float camYaw    = camera.yRot();
        float radius    = 0.3f + KuudraConfig.getPearlCircleSize() * 0.5f;
        boolean square  = KuudraConfig.getWaypointType() == WaypointType.SQUARE;
        boolean doFill  = KuudraConfig.isWaypointFillEnabled();
        int fillA       = Math.max(1, (int)(255f * KuudraConfig.getWaypointFillAlpha()));
        int beaconA     = Math.max(1, (int)(255f * KuudraConfig.getBeaconAlpha()));

        MultiBufferSource.BufferSource imm    = mc.renderBuffers().bufferSource();
        List<PearlWaypointState>       states = PearlWaypointManager.getSnapshot();

        PearlLocation myTargetLoc = null;
        for (PearlWaypointState s : states) {
            if (s.isMyTarget()) { myTargetLoc = s.target(); break; }
        }

        if (KuudraConfig.isDropLocationsEnabled()) {
            for (PearlLocation loc : PearlLocation.values()) {
                if (SupplyTracker.isCompleted(loc)) continue;
                boolean isTarget   = (loc == myTargetLoc);
                float   beamHeight = 320f - (float) loc.landingPos.y;
                drawBeaconBeam(matrices, imm, camPos, loc.landingPos, beaconA, isTarget, beamHeight);
            }
        }

        boolean anyAimed    = false;
        boolean aimedThrow  = false;
        boolean targetThrow = false;

        for (PearlWaypointState state : states) {
            Vec3 aim = state.centerAimDir();
            if (aim == null) continue;

            Vec3 landing = state.target().landingPos;
            if (sq(mc.player.getX() - landing.x) + sq(mc.player.getZ() - landing.z) < 25.0) continue;

            Vec3    wp      = spawn.add(aim.scale(50.0));
            boolean aimed   = crosshairHits(mc, wp, radius);
            long    throwIn = state.throwInMs();
            if (aimed) {
                Vec3 look = mc.player.getLookAngle();
                long curFl = TrajectorySolver.estimateFlightMs(spawn, look, state.target().targetPos);
                if (curFl > 0L) throwIn = PearlWaypointManager.computeThrowForFlight(curFl);
            }

            int argb = resolveColor(state.isMyTarget(), aimed, throwIn);
            if (aimed) { anyAimed = true; if (throwIn <= 0L) aimedThrow = true; }
            if (state.isMyTarget() && throwIn <= 0L) targetThrow = true;

            if (square) drawSquare(matrices, imm, camPos, wp, argb, radius, doFill, fillA);
            else        drawCircle(matrices, imm, camPos, wp, argb, radius, doFill, fillA);

            if (KuudraConfig.isPearlTimerEnabled())
                drawTimer(matrices, imm, camPos, mc.font, state, wp, radius, throwIn, aimed, camPitch, camYaw);
        }

        boolean frameShouldThrow = anyAimed ? aimedThrow : targetThrow;
        if (frameShouldThrow && !prevShouldThrow) {
            KuudraConfig.NotificationSound ns = KuudraConfig.getNotificationSound(KuudraConfig.SOUND_PEARL_NOW);
            if (ns.enabled) {
                KuudraConfig.playNotificationSound(KuudraConfig.SOUND_PEARL_NOW);
            } else {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 2.0f));
            }
        }
        prevShouldThrow = frameShouldThrow;

        imm.endBatch();

        org.lwjgl.opengl.GL11.glDepthFunc(519);

        for (PearlWaypointState state2 : states) {
            Vec3 aim2 = state2.centerAimDir();
            if (aim2 == null) continue;

            Vec3 landing2 = state2.target().landingPos;
            if (sq(mc.player.getX() - landing2.x) + sq(mc.player.getZ() - landing2.z) < 25.0) continue;

            Vec3    wp2      = spawn.add(aim2.scale(50.0));
            boolean aimed2   = crosshairHits(mc, wp2, radius);
            long    throwIn2 = state2.throwInMs();
            if (aimed2) {
                Vec3 look2 = mc.player.getLookAngle();
                long fl2 = TrajectorySolver.estimateFlightMs(spawn, look2, state2.target().targetPos);
                if (fl2 > 0L) throwIn2 = PearlWaypointManager.computeThrowForFlight(fl2);
            }

            int argb2 = resolveColor(state2.isMyTarget(), aimed2, throwIn2);

            if (square) drawSquare(matrices, imm, camPos, wp2, argb2, radius, doFill, fillA);
            else        drawCircle(matrices, imm, camPos, wp2, argb2, radius, doFill, fillA);

            if (KuudraConfig.isPearlTimerEnabled())
                drawTimer(matrices, imm, camPos, mc.font, state2, wp2, radius, throwIn2, aimed2, camPitch, camYaw);
        }

        imm.endBatch();
        org.lwjgl.opengl.GL11.glDepthFunc(515);
    }

    private static int resolveColor(boolean myTarget, boolean aimed, long throwIn) {
        int rgb;
        if (aimed) rgb = (myTarget && throwIn <= 0L) ? KuudraConfig.getWpColReady()
                                                      : KuudraConfig.getWpColHovered();
        else       rgb = myTarget ? KuudraConfig.getWpColCorrect()
                                  : KuudraConfig.getWpColNormal();
        return 0xFF000000 | rgb;
    }

    private static void applyBillboard(PoseStack matrices, Vec3 camPos, Vec3 wp) {
        matrices.translate(wp.x - camPos.x, wp.y - camPos.y, wp.z - camPos.z);
        Vec3  d     = camPos.subtract(wp).normalize();
        float yaw   = (float) Math.atan2(d.x, d.z);
        float pitch = (float) Math.asin(Math.max(-1.0, Math.min(1.0, d.y)));
        matrices.mulPose(new Quaternionf().rotationY(yaw));
        matrices.mulPose(new Quaternionf().rotationX(-pitch));
    }

    private static void drawCircle(PoseStack matrices, MultiBufferSource provider,
                                   Vec3 camPos, Vec3 wp,
                                   int argb, float r, boolean doFill, int fillA) {
        int a  = (argb >> 24) & 0xFF, rc = (argb >> 16) & 0xFF;
        int g  = (argb >>  8) & 0xFF, b  =  argb        & 0xFF;
        matrices.pushPose();
        applyBillboard(matrices, camPos, wp);
        Matrix4f mat = matrices.last().pose();

        if (!doFill) {
            VertexConsumer line = provider.getBuffer(RenderTypes.lines());
            for (int i = 0; i < SEGS; i++) {
                double a0 = 2 * Math.PI * i / SEGS, a1 = 2 * Math.PI * (i + 1) / SEGS;
                float x0 = (float)(Math.cos(a0) * r), y0 = (float)(Math.sin(a0) * r);
                float x1 = (float)(Math.cos(a1) * r), y1 = (float)(Math.sin(a1) * r);
                line.addVertex(mat, x0, y0, 0f).setColor(rc, g, b, a).setNormal(0f, 0f, 1f).setLineWidth(2.0f);
                line.addVertex(mat, x1, y1, 0f).setColor(rc, g, b, a).setNormal(0f, 0f, 1f).setLineWidth(2.0f);
            }
        }
        if (doFill) {
            VertexConsumer fv = provider.getBuffer(RenderTypes.debugQuads());
            for (int i = 0; i < SEGS; i++) {
                double a0 = 2 * Math.PI * i / SEGS, a1 = 2 * Math.PI * (i + 1) / SEGS;
                float x0 = (float)(Math.cos(a0) * r), y0 = (float)(Math.sin(a0) * r);
                float x1 = (float)(Math.cos(a1) * r), y1 = (float)(Math.sin(a1) * r);
                fv.addVertex(mat, 0f, 0f, 0f).setColor(rc, g, b, fillA).setNormal(0f, 0f, 1f);
                fv.addVertex(mat, x0, y0, 0f).setColor(rc, g, b, fillA).setNormal(0f, 0f, 1f);
                fv.addVertex(mat, x1, y1, 0f).setColor(rc, g, b, fillA).setNormal(0f, 0f, 1f);
                fv.addVertex(mat, 0f, 0f, 0f).setColor(rc, g, b, fillA).setNormal(0f, 0f, 1f);
            }
        }
        matrices.popPose();
    }

    private static void drawSquare(PoseStack matrices, MultiBufferSource provider,
                                   Vec3 camPos, Vec3 wp,
                                   int argb, float r, boolean doFill, int fillA) {
        int a  = (argb >> 24) & 0xFF, rc = (argb >> 16) & 0xFF;
        int g  = (argb >>  8) & 0xFF, b  =  argb        & 0xFF;
        float[][] C = {{-r,-r},{r,-r},{r,r},{-r,r}};
        matrices.pushPose();
        applyBillboard(matrices, camPos, wp);
        Matrix4f mat = matrices.last().pose();

        if (!doFill) {
            VertexConsumer line = provider.getBuffer(RenderTypes.lines());
            for (int i = 0; i < 4; i++) {
                float[] p0 = C[i], p1 = C[(i + 1) % 4];
                line.addVertex(mat, p0[0], p0[1], 0f).setColor(rc, g, b, a).setNormal(0f, 0f, 1f).setLineWidth(2.0f);
                line.addVertex(mat, p1[0], p1[1], 0f).setColor(rc, g, b, a).setNormal(0f, 0f, 1f).setLineWidth(2.0f);
            }
        }
        if (doFill) {
            VertexConsumer fv = provider.getBuffer(RenderTypes.debugQuads());
            fv.addVertex(mat, -r, -r, 0f).setColor(rc, g, b, fillA).setNormal(0f, 0f,  1f);
            fv.addVertex(mat,  r, -r, 0f).setColor(rc, g, b, fillA).setNormal(0f, 0f,  1f);
            fv.addVertex(mat,  r,  r, 0f).setColor(rc, g, b, fillA).setNormal(0f, 0f,  1f);
            fv.addVertex(mat, -r,  r, 0f).setColor(rc, g, b, fillA).setNormal(0f, 0f,  1f);
            fv.addVertex(mat, -r,  r, 0f).setColor(rc, g, b, fillA).setNormal(0f, 0f, -1f);
            fv.addVertex(mat,  r,  r, 0f).setColor(rc, g, b, fillA).setNormal(0f, 0f, -1f);
            fv.addVertex(mat,  r, -r, 0f).setColor(rc, g, b, fillA).setNormal(0f, 0f, -1f);
            fv.addVertex(mat, -r, -r, 0f).setColor(rc, g, b, fillA).setNormal(0f, 0f, -1f);
        }
        matrices.popPose();
    }

    private static void drawBeaconBeam(PoseStack matrices, MultiBufferSource provider,
                                       Vec3 camPos, Vec3 pos,
                                       int alpha, boolean isTarget, float beamHeight) {
        float hw = 0.075f;
        int col = isTarget ? KuudraConfig.getBeaconColCorrect() : KuudraConfig.getBeaconColNormal();
        int r = (col >> 16) & 0xFF;
        int g = (col >>  8) & 0xFF;
        int b =  col        & 0xFF;
        matrices.pushPose();
        matrices.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
        Matrix4f mat = matrices.last().pose();
        VertexConsumer vc = provider.getBuffer(
                RenderTypes.beaconBeam(Identifier.withDefaultNamespace("textures/entity/beacon/beacon_beam.png"), true));
        // East face
        vc.addVertex(mat,  hw, 0,          -hw).setColor(r,g,b,alpha).setUv(0,0).setLight(15728880).setNormal( 1,0,0);
        vc.addVertex(mat,  hw, beamHeight, -hw).setColor(r,g,b,alpha).setUv(0,1).setLight(15728880).setNormal( 1,0,0);
        vc.addVertex(mat,  hw, beamHeight,  hw).setColor(r,g,b,alpha).setUv(1,1).setLight(15728880).setNormal( 1,0,0);
        vc.addVertex(mat,  hw, 0,           hw).setColor(r,g,b,alpha).setUv(1,0).setLight(15728880).setNormal( 1,0,0);
        // West face
        vc.addVertex(mat, -hw, 0,           hw).setColor(r,g,b,alpha).setUv(0,0).setLight(15728880).setNormal(-1,0,0);
        vc.addVertex(mat, -hw, beamHeight,  hw).setColor(r,g,b,alpha).setUv(0,1).setLight(15728880).setNormal(-1,0,0);
        vc.addVertex(mat, -hw, beamHeight, -hw).setColor(r,g,b,alpha).setUv(1,1).setLight(15728880).setNormal(-1,0,0);
        vc.addVertex(mat, -hw, 0,          -hw).setColor(r,g,b,alpha).setUv(1,0).setLight(15728880).setNormal(-1,0,0);
        // South face
        vc.addVertex(mat,  hw, 0,           hw).setColor(r,g,b,alpha).setUv(0,0).setLight(15728880).setNormal(0,0, 1);
        vc.addVertex(mat,  hw, beamHeight,  hw).setColor(r,g,b,alpha).setUv(0,1).setLight(15728880).setNormal(0,0, 1);
        vc.addVertex(mat, -hw, beamHeight,  hw).setColor(r,g,b,alpha).setUv(1,1).setLight(15728880).setNormal(0,0, 1);
        vc.addVertex(mat, -hw, 0,           hw).setColor(r,g,b,alpha).setUv(1,0).setLight(15728880).setNormal(0,0, 1);
        // North face
        vc.addVertex(mat, -hw, 0,          -hw).setColor(r,g,b,alpha).setUv(0,0).setLight(15728880).setNormal(0,0,-1);
        vc.addVertex(mat, -hw, beamHeight, -hw).setColor(r,g,b,alpha).setUv(0,1).setLight(15728880).setNormal(0,0,-1);
        vc.addVertex(mat,  hw, beamHeight, -hw).setColor(r,g,b,alpha).setUv(1,1).setLight(15728880).setNormal(0,0,-1);
        vc.addVertex(mat,  hw, 0,          -hw).setColor(r,g,b,alpha).setUv(1,0).setLight(15728880).setNormal(0,0,-1);
        matrices.popPose();
    }

    private static void drawTimer(PoseStack matrices, MultiBufferSource provider,
                                  Vec3 camPos, Font font,
                                  PearlWaypointState state, Vec3 wp,
                                  float radius, long throwIn, boolean aimed,
                                  float camPitch, float camYaw) {
        boolean tracking = PearlWaypointManager.isTrackingPickup();
        String text;
        if (!tracking) {
            if      (state.optimalFlightMs() <= 0L) text = "---";
            else if (throwIn >= 0L)                 text = "+" + throwIn + "ms";
            else                                    text = Math.abs(throwIn) + "ms early";
        } else {
            text = throwIn > 0L ? throwIn + "ms" : "THROW";
        }

        int color;
        if      (!aimed)                              color = 0xFFFFFFFF;
        else if (state.isMyTarget() && throwIn <= 0L) color = 0xFF33FF33;
        else                                          color = 0xFFFFAA00;

        double heightOff = radius + 0.1 + KuudraConfig.getPearlTimerHeight() * 4.0;
        float  scale     = 0.02f + KuudraConfig.getPearlTimerSize() * 0.5f;
        Vec3   textPos   = wp.add(0, heightOff, 0);

        matrices.pushPose();
        matrices.translate(textPos.x - camPos.x, textPos.y - camPos.y, textPos.z - camPos.z);
        matrices.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-camYaw)));
        matrices.mulPose(new Quaternionf().rotationX((float) Math.toRadians( camPitch)));
        matrices.scale(-scale, -scale, scale);
        int tw = font.width(text);
        font.drawInBatch(Component.literal(text), -tw / 2f, 0f, color, false,
                matrices.last().pose(), provider,
                Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        matrices.popPose();
    }

    private static boolean crosshairHits(Minecraft mc, Vec3 wp, float radius) {
        Entity cam = mc.getCameraEntity();
        if (cam == null) return false;
        Vec3   start  = cam.getEyePosition();
        Vec3   look   = cam.getLookAngle();
        Vec3   toWp   = wp.subtract(start);
        double t      = toWp.dot(look);
        if (t < 0.5 || t > 70.0) return false;
        Vec3   closest = start.add(look.scale(t));
        double hitR    = radius * 1.1;
        return closest.distanceToSqr(wp) < hitR * hitR;
    }

    private static double sq(double v) { return v * v; }
}