package com.phantomaddons;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

public final class DataFeatures {

    private static final boolean PRESENT;
    private static final boolean HIGHLIGHT;

    private static Method supplyAttemptTrackerOnPickupPercent;
    private static Method gameplayDataLoggerOnDisconnect;
    private static Method gameplayDataLoggerTick;
    private static Method supplyAttemptTrackerOnPickupStart;
    private static Method supplyAttemptTrackerOnPickupEnd;
    private static Method supplyAttemptTrackerOnSupplyDropped;
    private static Method pearlCancelLoggerOnPearlThrow;
    private static Method tentacleHighlightRendererRender;
    private static Method runRecorderOnRunStart;
    private static Method runRecorderOnRunEnd;
    private static Method runRecorderOnRunAbandoned;

    static {
        boolean present;
        try {
            Class<?> gameplayDataLogger = Class.forName("com.phantomaddons.data.GameplayDataLogger");
            gameplayDataLoggerOnDisconnect = gameplayDataLogger.getMethod("onDisconnect");
            gameplayDataLoggerTick = gameplayDataLogger.getMethod("tick", Minecraft.class);

            Class<?> supplyAttemptTracker = Class.forName("com.phantomaddons.data.supply.SupplyAttemptTracker");
            supplyAttemptTrackerOnPickupStart = supplyAttemptTracker.getMethod("onPickupStart", int.class);
            supplyAttemptTrackerOnPickupEnd = supplyAttemptTracker.getMethod("onPickupEnd", int.class);
            supplyAttemptTrackerOnPickupPercent = supplyAttemptTracker.getMethod("onPickupPercent", int.class);
            supplyAttemptTrackerOnSupplyDropped = supplyAttemptTracker.getMethod("onSupplyDropped");

            pearlCancelLoggerOnPearlThrow = Class.forName("com.phantomaddons.data.supply.PearlCancelLogger")
                    .getMethod("onPearlThrow", Player.class);

            Class<?> runRecorder = Class.forName("com.phantomaddons.data.RunRecorder");
            runRecorderOnRunStart = runRecorder.getMethod("onRunStart");
            runRecorderOnRunEnd = runRecorder.getMethod("onRunEnd");
            runRecorderOnRunAbandoned = runRecorder.getMethod("onRunAbandoned");

            present = true;
        } catch (ReflectiveOperationException e) {
            present = false;
        }
        PRESENT = present;

        boolean highlight;
        try {
            tentacleHighlightRendererRender = Class.forName("com.phantomaddons.data.tentacle.TentacleHighlightRenderer")
                    .getMethod("render", PoseStack.class, Camera.class, float.class);
            highlight = true;
        } catch (ReflectiveOperationException e) {
            highlight = false;
        }
        HIGHLIGHT = highlight;
    }

    private DataFeatures() {}

    public static boolean isPresent() { return PRESENT; }

    public static boolean hasHighlight() { return HIGHLIGHT; }

    public static void onDisconnect()                { invoke(gameplayDataLoggerOnDisconnect); }
    public static void tick(Minecraft client)         { invoke(gameplayDataLoggerTick, client); }
    public static void onSupplyPickupStart(int pct)   { invoke(supplyAttemptTrackerOnPickupStart, pct); }
    public static void onSupplyPickupEnd(int pct)     { invoke(supplyAttemptTrackerOnPickupEnd, pct); }
    public static void onSupplyPickupPercent(int pct) { invoke(supplyAttemptTrackerOnPickupPercent, pct); }
    public static void onSupplyDropped()              { invoke(supplyAttemptTrackerOnSupplyDropped); }
    public static void onPearlThrow(Player player)    { invoke(pearlCancelLoggerOnPearlThrow, player); }
    public static void onRunStart()                   { invoke(runRecorderOnRunStart); }
    public static void onRunEnd()                     { invoke(runRecorderOnRunEnd); }
    public static void onRunAbandoned()                { invoke(runRecorderOnRunAbandoned); }

    public static void renderTentacleHighlights(PoseStack matrices, Camera camera, float tickDelta) {
        if (!HIGHLIGHT) return;
        invoke(tentacleHighlightRendererRender, matrices, camera, tickDelta);
    }

    private static void invoke(Method m, Object... args) {
        if (!PRESENT || m == null) return;
        try {
            m.invoke(null, args);
        } catch (ReflectiveOperationException e) {
            PhantomAddons.LOGGER.warn("[DataFeatures] reflective call to {} failed", m, e);
        }
    }
}
