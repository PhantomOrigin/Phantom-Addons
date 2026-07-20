package com.phantomaddons.utils;

import com.phantomaddons.PhantomAddons;
import com.phantomaddons.features.stundps.MountTimerHud;
import net.minecraft.client.Minecraft;


public final class Phase2BuildTracker {

    private static boolean phase2Active       = false;
    private static boolean mountTimerActive   = false;
    private static boolean wasRidingArmorStand = false;
    private static int     mountGraceTicks    = 0;
    private static int     postTimerTicks     = 0;

    public static int mountTimer = 0;

    public static void start() {
        phase2Active        = true;
        mountTimer          = -1;
        mountTimerActive    = false;
        wasRidingArmorStand = false;
        postTimerTicks      = 0;
        PhantomAddons.LOGGER.info("[PhantomAddons] Phase 2 build tracking started");
    }

    public static void stop() {
        phase2Active        = false;
        mountTimer          = -1;
        mountGraceTicks     = 0;
        wasRidingArmorStand = false;
        postTimerTicks      = 0;
        PhantomAddons.LOGGER.info("[PhantomAddons] Phase 2 build reset");
    }

    public static void tick(Minecraft client) {
        if (client.player == null || !phase2Active) return;

        boolean ridingArmorStand =
                client.player.getVehicle() != null &&
                        client.player.getVehicle().getType().toString().contains("armor_stand");

        if (ridingArmorStand && !wasRidingArmorStand && !mountTimerActive) {
            mountTimer       = 96;
            mountGraceTicks  = 39;
            mountTimerActive = true;
        }

        if (mountGraceTicks > 0) {
            mountGraceTicks--;
            if (!ridingArmorStand) {
                mountTimer       = 0;
                mountGraceTicks  = 0;
                mountTimerActive = false;
            }
        }

        wasRidingArmorStand = ridingArmorStand;

        // Countdown
        if (mountTimer > 0) {
            mountTimer--;
            if (mountTimer == 0) {
                postTimerTicks = 60;
            }
        }

        if (postTimerTicks > 0) {
            postTimerTicks--;
        }
    }

    public static boolean isActive()     { return phase2Active; }
    public static int     getMountTimer() { return mountTimer;   }
}