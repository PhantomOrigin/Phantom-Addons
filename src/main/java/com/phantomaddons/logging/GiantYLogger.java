package com.phantomaddons.logging;

import com.phantomaddons.PhantomAddons;
import com.phantomaddons.phase.KuudraPhaseTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class GiantYLogger {

    private static boolean enabled = false;
    private static boolean active  = false;
    private static int     runId   = 0;
    private static int     tickCounter = 0;
    private static BufferedWriter writer = null;

    private GiantYLogger() {}

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled()            { return enabled; }

    public static void tick(Minecraft client) {
        if (!enabled || client.player == null || client.level == null) {
            end();
            return;
        }
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.SUPPLIES) {
            end();
            return;
        }
        if (!active) begin();
        if (!active) return;

        Giant nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (Entity e : client.level.entitiesForRendering()) {
            if (!(e instanceof Giant g)) continue;
            if (!isCarryingSupply(g)) continue;
            double distSq = g.distanceToSqr(client.player);
            if (distSq < nearestDistSq) { nearestDistSq = distSq; nearest = g; }
        }

        tickCounter++;
        writeLine(nearest, nearestDistSq);
    }

    private static boolean isCarryingSupply(Giant g) {
        ItemStack hand = g.getMainHandItem();
        if (hand.isEmpty()) return false;
        return hand.is(Items.PLAYER_HEAD) || hand.is(Items.CARVED_PUMPKIN) || hand.is(Items.PUMPKIN);
    }

    private static void begin() {
        try {
            Files.createDirectories(Paths.get("logs/kuudra-giant-y"));
            String path = "logs/kuudra-giant-y/run-" + (++runId) + ".log";
            writer = Files.newBufferedWriter(Paths.get(path));
            active = true;
            tickCounter = 0;
            PhantomAddons.LOGGER.info("[PhantomAddons] Giant Y log started: {}", path);
        } catch (IOException e) {
            PhantomAddons.LOGGER.error("Failed to start giant Y log", e);
        }
    }

    private static void end() {
        if (!active) return;
        try {
            if (writer != null) writer.close();
        } catch (IOException e) {
            PhantomAddons.LOGGER.error("Failed to close giant Y log", e);
        } finally {
            writer = null;
            active = false;
        }
    }

    private static void writeLine(Giant nearest, double distSq) {
        try {
            if (nearest == null) {
                writer.write(String.format("TICK: %d | NO SUPPLY GIANT%n", tickCounter));
            } else {
                writer.write(String.format(
                        "TICK: %d | Y: %.5f | EYE_Y: %.5f | DIST: %.3f | ID: %d%n",
                        tickCounter, nearest.getY(), nearest.getEyeY(),
                        Math.sqrt(distSq), nearest.getId()));
            }
        } catch (IOException e) {
            PhantomAddons.LOGGER.error("Failed writing giant Y log", e);
        }
    }
}
