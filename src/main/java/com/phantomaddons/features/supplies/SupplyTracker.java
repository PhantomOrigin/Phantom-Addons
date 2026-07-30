package com.phantomaddons.features.supplies;

import com.phantomaddons.utils.TextUtil;
import com.phantomaddons.features.supplies.pearlwaypoints.PearlLocation;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SupplyTracker {

    private static final double SCAN_RADIUS = 4.0;

    private static final Pattern RECOVERED =
            Pattern.compile("(\\S+)\\s+recovered (?:a supply|one of Elle's supplies)",
                    Pattern.CASE_INSENSITIVE);

    private static final Set<PearlLocation> completed =
            EnumSet.noneOf(PearlLocation.class);

    private SupplyTracker() {}

    public static void register() {
        ClientReceiveMessageEvents.GAME
                .register((message, overlay) -> { if (!overlay) onChat(message.getString()); });
    }

    public static void reset() { completed.clear(); }

    public static boolean isCompleted(PearlLocation loc) { return completed.contains(loc); }

    public static boolean onChat(String raw) {
        String clean = TextUtil.stripColor(raw).trim();
        Matcher m = RECOVERED.matcher(clean);
        if (!m.find()) return false;

        String name = m.group(1).trim();
        markByPlayerName(name);
        return true;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (PearlLocation loc : PearlLocation.values()) {
            if (completed.contains(loc)) continue;

            Vec3 pos = loc.landingPos;
            AABB box = new AABB(
                    pos.x - SCAN_RADIUS, pos.y - 1, pos.z - SCAN_RADIUS,
                    pos.x + SCAN_RADIUS, pos.y + 4, pos.z + SCAN_RADIUS);

            boolean found = mc.level.getEntitiesOfClass(
                    ArmorStand.class, box,
                    e -> e.getName() != null && e.getName().getString().contains("SUPPLIES RECEIVED")
            ).stream().findFirst().isPresent();

            if (found) completed.add(loc);
        }
    }

    private static void markByPlayerName(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        mc.level.players().stream()
                .filter(p -> p.getName().getString().equalsIgnoreCase(name)
                        || p.getName().getString().endsWith(name))
                .findFirst()
                .ifPresent(player -> {
                    Vec3 pos = new Vec3(player.getX(), player.getY(), player.getZ());
                    PearlLocation closest = closestPearlLocation(pos);
                    if (closest != null) completed.add(closest);
                });
    }

    private static PearlLocation closestPearlLocation(Vec3 pos) {
        PearlLocation best   = null;
        double        bestD2 = Double.MAX_VALUE;
        for (PearlLocation loc : PearlLocation.values()) {
            double d2 = loc.landingPos.distanceToSqr(pos);
            if (d2 < bestD2) { bestD2 = d2; best = loc; }
        }
        return best;
    }
}