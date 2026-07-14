package com.kuudrahelper.features.build;

import com.kuudrahelper.features.supplies.PearlLocation;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BuildProgressTracker {

    private static final Pattern PERCENT  = Pattern.compile("(\\d{1,3})%");
    private static final double  RADIUS_SQ = 16.0;

    private static final Map<PearlLocation, Integer> progress =
            new EnumMap<>(PearlLocation.class);

    private static volatile boolean active = false;

    private BuildProgressTracker() {}

    public static void start() {
        progress.clear();
        active = true;
    }

    public static void stop() {
        active = false;
        progress.clear();
    }

    public static int getProgress(PearlLocation loc) {
        return progress.getOrDefault(loc, -1);
    }

    public static boolean isComplete(PearlLocation loc) {
        int p = getProgress(loc);
        return p >= 100;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!active || client.level == null) return;

            for (PearlLocation loc : PearlLocation.values()) {
                double cx = loc.landingPos.x;
                double cy = loc.landingPos.y;
                double cz = loc.landingPos.z;
                int bestPct = -1;

                for (Entity entity : client.level.entitiesForRendering()) {
                    if (!(entity instanceof ArmorStand stand)) continue;
                    double dx = stand.getX() - cx;
                    double dy = stand.getY() - cy;
                    double dz = stand.getZ() - cz;
                    if (dx*dx + dy*dy + dz*dz > RADIUS_SQ) continue;

                    Component name = stand.getCustomName();
                    if (name == null) continue;

                    String raw = name.getString().replaceAll("§[0-9a-fk-orA-FK-OR]", "");
                    if (raw.toLowerCase().contains("complete")) {
                        bestPct = 100;
                        break;
                    }
                    Matcher m  = PERCENT.matcher(raw);
                    if (m.find()) {
                        int pct = Math.min(99, Integer.parseInt(m.group(1)));
                        if (pct > bestPct) bestPct = pct;
                    }
                }

                progress.put(loc, bestPct);
            }
        });
    }
}