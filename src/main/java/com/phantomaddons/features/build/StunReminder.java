package com.phantomaddons.features.build;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.build.buildprogress.BuildProgressTracker;
import com.phantomaddons.features.supplies.pearlwaypoints.PearlLocation;
import com.phantomaddons.utils.KuudraTierDetector;
import com.phantomaddons.utils.RoleManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;

public final class StunReminder {

    private static final long   WINDOW_MS  = 2000;
    private static final float  BASE_SCALE = 3.0f;
    private static final String TEXT       = "LEAVE BUILD!";
    private static final int    MIN_TIER   = 3; // Basic and Hot have no stun

    private record Sample(long timeMs, int progress) {}

    private static final ArrayDeque<Sample> samples = new ArrayDeque<>();

    private static volatile boolean active    = false;
    private static volatile boolean triggered = false;

    private StunReminder() {}

    public static void onBuildStart() {
        samples.clear();
        triggered = false;
        active    = true;
    }

    public static void reset() {
        active    = false;
        triggered = false;
        samples.clear();
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!active || triggered) return;
            if (!PhantomConfig.isStunReminderEnabled() || client.player == null) return;
            if (KuudraTierDetector.getTier() < MIN_TIER || !isStun(client)) return;

            int progress = currentProgress();
            if (progress < 0) return;

            long now = System.currentTimeMillis();
            samples.addLast(new Sample(now, progress));
            while (!samples.isEmpty() && now - samples.peekFirst().timeMs() > WINDOW_MS) samples.removeFirst();

            if (progress >= PhantomConfig.getStunReminderMaxPercent()) {
                triggered = true;
                return;
            }

            // Wait for a full window of history so the first few ticks of build don't spike the rate
            Sample oldest = samples.peekFirst();
            long span = now - oldest.timeMs();
            if (span < WINDOW_MS - 100) return;

            double perMs = (progress - oldest.progress()) / (double) span;
            if (perMs <= 0) return;

            double etaMs = (100 - progress) / perMs;
            if (etaMs <= PhantomConfig.getStunReminderTravelMs()) triggered = true;
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "stun_reminder"),
                (ctx, tc) -> {
                    if (!active || !triggered || !PhantomConfig.isStunReminderEnabled()) return;

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.font == null) return;

                    int screenW = mc.getWindow().getGuiScaledWidth();
                    int screenH = mc.getWindow().getGuiScaledHeight();

                    float cx = PhantomConfig.getStunReminderHudX() * screenW;
                    float cy = PhantomConfig.getStunReminderHudY() * screenH;
                    float s  = BASE_SCALE * PhantomConfig.getStunReminderHudScale();

                    Component comp = Component.literal(TEXT);
                    var matrices = ctx.pose();
                    matrices.pushMatrix();
                    matrices.translate(cx, cy);
                    matrices.scale(s, s);

                    int textWidth = mc.font.width(comp);
                    ctx.text(mc.font, comp, -textWidth / 2, 0, 0xFFFF3333, true);

                    matrices.popMatrix();
                });
    }

    private static boolean isStun(Minecraft client) {
        return switch (PhantomConfig.getStunReminderRoleMode()) {
            case STUN -> true;
            case DPS  -> false;
            case AUTO -> RoleManager.isWearingHollowArmor(client.player);
        };
    }

    private static int currentProgress() {
        int total = 0, count = 0;
        boolean any = false;
        for (PearlLocation loc : PearlLocation.values()) {
            int p = BuildProgressTracker.getProgress(loc);
            if (p >= 0) any = true;
            total += Math.max(0, p);
            count++;
        }
        return any && count > 0 ? total / count : -1;
    }
}
