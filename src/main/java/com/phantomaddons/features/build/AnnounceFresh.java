package com.phantomaddons.features.build;

import com.phantomaddons.utils.TextUtil;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.build.buildprogress.BuildProgressHud;
import com.phantomaddons.features.misckuudra.NotificationHud;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if <26.2 {
/*import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
*///?} else {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.phantomaddons.utils.WorldRenderCollector;
//?}
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AnnounceFresh {

    private static final String FRESH_SERVER_MSG =
            "Your Fresh Tools Perk bonus doubles your building speed for the next 10 seconds!";

    private static final Pattern FRESH_PARTY_PATTERN = Pattern.compile(
            "Party > (?:\\[.*?\\] )?([A-Za-z0-9_]+): FRESH(?:.*?\\((\\d+)%\\))?.*");

    private static final long FRESH_DURATION_MS = 10_000L;

    private static final Map<String, Long> activeTimers = new ConcurrentHashMap<>();

    private static volatile boolean buildActive = false;

    private AnnounceFresh() {}

    public static void onBuildStart() {
        buildActive = true;
    }

    public static void reset() {
        buildActive = false;
        activeTimers.clear();
    }

    public static void onChat(String raw) {
        if (!PhantomConfig.isAnnounceFreshEnabled()) return;

        String clean = TextUtil.stripColor(raw).trim();

        if (clean.equals(FRESH_SERVER_MSG)) {
            if (!buildActive) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() == null) return;
            int progress = BuildProgressHud.getCurrentProgress();
            String pctStr = progress >= 0 ? progress + "%" : "0%";
            if (PhantomConfig.isFreshNotifyEnabled()) {
                NotificationHud.show("§aFresh!", 3000);
                PhantomConfig.playNotificationSound(PhantomConfig.SOUND_FRESH);
            }
            mc.execute(() -> {
                if (mc.getConnection() != null)
                    mc.getConnection().sendCommand("pc FRESH! (" + pctStr + ")");
            });
            return;
        }

        Matcher m = FRESH_PARTY_PATTERN.matcher(clean);
        if (m.find()) {
            String playerName = m.group(1);
            activeTimers.put(playerName, System.currentTimeMillis() + FRESH_DURATION_MS);
        }
    }

    public static void renderTimers(PoseStack matrices, Camera camera, float tickDelta) {
        if (!PhantomConfig.isAnnounceFreshEnabled()) return;
        if (activeTimers.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        long now = System.currentTimeMillis();
        activeTimers.entrySet().removeIf(e -> now >= e.getValue());
        if (activeTimers.isEmpty()) return;

        Vec3 camPos = camera.position();
        float cameraXRot = camera.xRot();
        float cameraYRot = camera.yRot();
        //? if <26.2 {
        /*MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        *///?} else {
        SubmitNodeCollector collector = WorldRenderCollector.get();
        if (collector == null) return;
        //?}

        for (var entry : activeTimers.entrySet()) {
            String playerName = entry.getKey();
            long endMs = entry.getValue();
            if (now >= endMs) continue;

            double remaining = (endMs - now) / 1000.0;

            Player target = null;
            for (Player p : mc.level.players()) {
                if (p.getName().getString().equals(playerName)) { target = p; break; }
            }
            if (target == null) continue;

            double x = target.getX() - camPos.x;
            double y = target.getY() + target.getBbHeight() + 0.5 - camPos.y;
            double z = target.getZ() - camPos.z;

            String text = String.format("%.1fs", remaining);
            int color = timerColor(remaining);

            matrices.pushPose();
            matrices.translate(x, y, z);
            matrices.mulPose(new Quaternionf().rotationY(-Mth.DEG_TO_RAD * (cameraYRot + 180.0f)));
            matrices.mulPose(new Quaternionf().rotationX(-Mth.DEG_TO_RAD * cameraXRot));
            matrices.scale(-0.025f, -0.025f, 0.025f);

            float tw = mc.font.width(text);
            //? if <26.2 {
            /*Matrix4f matrix = matrices.last().pose();
            mc.font.drawInBatch(text, -tw / 2f, 0f, color, false,
                    matrix, buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            *///?} else {
            FormattedCharSequence seq = Component.literal(text).getVisualOrderText();
            collector.submitText(matrices, -tw / 2f, 0f, seq, false,
                    Font.DisplayMode.NORMAL, 0xF000F0, color, 0, 0);
            //?}

            matrices.popPose();
        }
        //? if <26.2 {
        /*buffers.endBatch();
        *///?}
    }

    private static int timerColor(double remaining) {
        if (remaining > 7.0) return 0xFF00FF00;  // green
        if (remaining > 3.0) return 0xFFFF8800;  // orange
        return 0xFFFF3300;                        // red
    }
}
