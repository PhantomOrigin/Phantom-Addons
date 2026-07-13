package com.kuudrahelper.logging;

import com.kuudrahelper.KuudraHelperMod;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.Vec3;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public final class KuudraStationaryLogger {

    private static final int    KUUDRA_SIZE      = 30;
    private static final int    STATIONARY_TICKS = 5;
    private static final double MOVE_EPSILON_SQ  = 0.0001; // ~0.01 blocks

    private static boolean enabled = false;
    private static boolean active  = false;
    private static int     runId   = 0;
    private static int     tickCounter = 0;
    private static BufferedWriter writer = null;

    private static Vec3 lastPos          = null;
    private static int  stationaryStreak = 0;
    private static final Set<BlockPos> loggedPositions = new HashSet<>();

    private KuudraStationaryLogger() {}

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled()            { return enabled; }

    public static void tick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null) {
            end();
            return;
        }
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.BOSS) {
            end();
            return;
        }
        if (!active) begin();
        if (!active) return;

        tickCounter++;

        Slime kuudra = findKuudra(client);
        if (kuudra == null) {
            lastPos          = null;
            stationaryStreak = 0;
            return;
        }

        Vec3 pos = kuudra.position();
        if (lastPos != null && pos.distanceToSqr(lastPos) < MOVE_EPSILON_SQ) {
            stationaryStreak++;
        } else {
            stationaryStreak = 0;
        }
        lastPos = pos;

        if (stationaryStreak > STATIONARY_TICKS) {
            BlockPos key = BlockPos.containing(pos);
            if (loggedPositions.add(key)) {
                logStationaryPoint(kuudra, pos);
            }
        }
    }

    private static Slime findKuudra(Minecraft mc) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Slime s)) continue;
            if (s.getSize() != KUUDRA_SIZE || s.getHealth() <= 0) continue;
            return s;
        }
        return null;
    }

    private static void logStationaryPoint(Slime kuudra, Vec3 pos) {
        String line = String.format(
                "STATIONARY | x: %.5f  y: %.5f  z: %.5f  |  bb: %s",
                pos.x, pos.y, pos.z, kuudra.getBoundingBox());

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§f[PhantomAddons]§r §e" + line));
        }
        KuudraHelperMod.LOGGER.info("[BoneTimingAssist] {}", line);

        try {
            if (writer != null) {
                writer.write(String.format("TICK: %d | %s%n", tickCounter, line));
                writer.flush();
            }
        } catch (IOException e) {
            KuudraHelperMod.LOGGER.error("Failed writing stationary log", e);
        }
    }

    private static void begin() {
        try {
            Files.createDirectories(Paths.get("logs/kuudra-stationary"));
            String path = "logs/kuudra-stationary/run-" + (++runId) + ".log";
            writer = Files.newBufferedWriter(Paths.get(path));
            active = true;
            tickCounter      = 0;
            lastPos          = null;
            stationaryStreak = 0;
            loggedPositions.clear();
            KuudraHelperMod.LOGGER.info("[PhantomAddons] Kuudra stationary log started: {}", path);
        } catch (IOException e) {
            KuudraHelperMod.LOGGER.error("Failed to start stationary log", e);
        }
    }

    private static void end() {
        if (!active) return;
        try {
            if (writer != null) writer.close();
        } catch (IOException e) {
            KuudraHelperMod.LOGGER.error("Failed to close stationary log", e);
        } finally {
            writer = null;
            active = false;
        }
    }
}
