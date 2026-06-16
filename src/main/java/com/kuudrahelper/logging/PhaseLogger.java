package com.kuudrahelper.logging;

import com.kuudrahelper.KuudraHelperMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class PhaseLogger {

    private static boolean enabled  = false;
    private static boolean active   = false;
    private static int     runId    = 0;
    private static int     tickCounter = 0;
    private static BufferedWriter writer = null;

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled()            { return enabled;  }
    public static boolean isActive()             { return active;   }

    public static void begin() {
        if (!enabled || active) return;
        try {
            Files.createDirectories(Paths.get("logs/kuudra-phases"));
            String path = "logs/kuudra-phases/run-" + (++runId) + ".log";
            writer = Files.newBufferedWriter(Paths.get(path));
            active = true;
            writer.write("=== Phase 3 START ===\n");
            KuudraHelperMod.LOGGER.info("[PhantomAddons] Phase log started: {}", path);
        } catch (IOException e) {
            KuudraHelperMod.LOGGER.error("Failed to start phase log", e);
        }
    }

    public static void end() {
        try {
            if (writer != null) {
                writer.write("=== Phase 6 END ===\n");
                writer.close();
            }
        } catch (IOException e) {
            KuudraHelperMod.LOGGER.error("Failed to close phase log", e);
        } finally {
            writer  = null;
            active  = false;
        }
    }

    public static void resetTick() {
        tickCounter = 0;
    }

    public static void tick(Minecraft client) {
        if (!active || writer == null || client.player == null) return;
        tickCounter++;
        writePlayerState(client);
    }

    private static void writePlayerState(Minecraft client) {
        try {
            var player = client.player;
            double x = player.getX(), y = player.getY(), z = player.getZ();
            Vec3 vel = player.getDeltaMovement();
            String vehicleType = player.isPassenger() && player.getVehicle() != null
                    ? player.getVehicle().getType().toString() : "none";

            writer.write(String.format(
                    "TICK: %d | POS: %.3f,%.3f,%.3f | VEL: %.4f,%.4f,%.4f" +
                            " | YAW: %.2f | PITCH: %.2f | RIDING: %b (%s)%n",
                    tickCounter, x, y, z,
                    vel.x, vel.y, vel.z,
                    player.getYRot(), player.getXRot(),
                    player.isPassenger(), vehicleType));
        } catch (IOException e) {
            KuudraHelperMod.LOGGER.error("Failed writing player state", e);
        }
    }
}