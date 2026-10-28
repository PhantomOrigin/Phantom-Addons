package com.phantomaddons.utils;

import com.phantomaddons.PhantomAddons;
import com.phantomaddons.features.stundps.MountTimerHud;
import com.phantomaddons.PhantomConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.decoration.ArmorStand;


public final class Phase2BuildTracker {

    private static boolean phase2Active       = false;
    private static boolean mountTimerActive   = false;
    private static boolean wasRidingArmorStand = false;
    private static int     mountGraceTicks    = 0;
    private static int     postTimerTicks     = 0;

    public static int mountTimer = 0;

    public static final int BLINDNESS_TICKS = 9;
    private static boolean waitingForAgro = false;
    private static boolean wasBlind       = false;
    private static boolean blindChecked   = false;
    private static boolean timerFinished  = false;

    public static int pingTicks() {
        if (!PhantomConfig.isEatenTimerSubtractPingEnabled()) return 0;
        return Math.max(0, PhantomConfig.getLowPing() / 50 - 1);
    }

    public static int fullPingTicks() {
        return Math.max(0, PhantomConfig.getLowPing() / 50);
    }

    public static boolean pickobulusTimeAllowed() {
        return mountTimer <= 0 || mountTimer - fullPingTicks() <= 1;
    }

    private static final double EATEN_X = -161, EATEN_Z = -186, EATEN_MAX_Y = 55, EATEN_RADIUS = 20;

    private static boolean atEatenPosition(Minecraft client) {
        var p = client.player.position();
        double dx = p.x - EATEN_X, dz = p.z - EATEN_Z;
        return p.y < EATEN_MAX_Y && dx * dx + dz * dz <= EATEN_RADIUS * EATEN_RADIUS;
    }

    private static int blindnessMark() {
        return BLINDNESS_TICKS;
    }

    public static void start() {
        phase2Active        = true;
        mountTimer          = -1;
        mountTimerActive    = false;
        wasRidingArmorStand = false;
        postTimerTicks      = 0;
        waitingForAgro      = false;
        blindChecked        = false;
        timerFinished       = false;
        PhantomAddons.LOGGER.info("[PhantomAddons] Phase 2 build tracking started");
    }

    public static void stop() {
        phase2Active        = false;
        mountTimer          = -1;
        mountGraceTicks     = 0;
        wasRidingArmorStand = false;
        postTimerTicks      = 0;
        waitingForAgro      = false;
        blindChecked        = false;
        timerFinished       = false;
        PhantomAddons.LOGGER.info("[PhantomAddons] Phase 2 build reset");
    }

    public static void tick(Minecraft client) {
        if (client.player == null || !phase2Active) return;

        boolean ridingArmorStand = client.player.getVehicle() instanceof ArmorStand;

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
                waitingForAgro   = false;
            }
        }

        wasRidingArmorStand = ridingArmorStand;

        boolean blind = client.player.hasEffect(MobEffects.BLINDNESS);
        if (!blindChecked) {
            wasBlind = blind;
            blindChecked = true;
        }
        boolean blindnessGained = blind && !wasBlind;
        wasBlind = blind;
        if (blindnessGained && !timerFinished && mountTimer <= 0) {
            mountTimer       = BLINDNESS_TICKS;
            mountGraceTicks  = 0;
            mountTimerActive = true;
            waitingForAgro   = false;
            PhantomAddons.LOGGER.info("[PhantomAddons] Eaten timer started by blindness, the mount was not seen");
        }

        if (mountTimer > 0 && atEatenPosition(client)) {
            mountTimer       = 0;
            mountGraceTicks  = 0;
            mountTimerActive = false;
            waitingForAgro   = false;
            timerFinished    = true;
        }

        if (mountTimer > 0) {
            if (mountTimer == blindnessMark() && !blind) waitingForAgro = true;
            if (waitingForAgro && blind) waitingForAgro = false;
            if (!waitingForAgro) {
                mountTimer--;
                if (mountTimer == 0) {
                    postTimerTicks = 60;
                    timerFinished  = true;
                }
            }
        }

        if (postTimerTicks > 0) {
            postTimerTicks--;
        }
    }

    public static boolean isActive()     { return phase2Active; }
    public static int     getMountTimer() { return mountTimer;   }
    public static boolean isWaitingForAgro() { return waitingForAgro; }
}