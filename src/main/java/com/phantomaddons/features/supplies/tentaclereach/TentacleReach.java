package com.phantomaddons.features.supplies.tentaclereach;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.phase.KuudraPhaseTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
//? if <26.2 {
/*import net.minecraft.world.entity.monster.Slime;
*///?} else {
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
//?}

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TentacleReach {

    public static final double RADIUS = 20.0;
    public static final double CENTER_Y = 67.0;

    private static final int    KUUDRA_SIZE = 30;
    private static final double TRACK_DISTANCE = 60.0;
    private static final long   STALE_TICKS = 40;
    private static final double MOVE_EPS = 1.0e-3;
    private static final double LUNGE_EPS = 0.02;
    private static final int    DOWN_TICKS_TO_HIDE = 5;

    private static final class Cube {
        final double x, z;
        double lastX, lastY, lastZ;
        long lastSeenTick;
        boolean hidden;
        int downTicks;

        Cube(double x, double y, double z, long tick) {
            this.x = x; this.z = z;
            this.lastX = x; this.lastY = y; this.lastZ = z;
            this.lastSeenTick = tick;
        }
    }

    private static final Map<UUID, Cube> cubes = new HashMap<>();
    private static volatile List<double[]> centres = List.of();
    private static long tickCount;
    private static volatile boolean inside;

    private TentacleReach() {}

    public static List<double[]> getCentres() { return centres; }

    public static boolean isInside() { return inside; }

    public static void reset() {
        cubes.clear();
        centres = List.of();
        inside = false;
    }

    public static void tick(Minecraft client) {
        tickCount++;
        if (!PhantomConfig.isTentacleReachEnabled() || client.player == null || client.level == null
                || !KuudraPhaseTracker.isSupplyCollectionPhase()) {
            reset();
            return;
        }

        double playerX = client.player.getX(), playerZ = client.player.getZ();
        for (Entity e : client.level.entitiesForRendering()) {
            if (!(e instanceof AbstractCubeMob mob)) continue;
            if (mob.getSize() == KUUDRA_SIZE || !mob.isInvisible() || hasVisibleName(mob)) continue;
            double hx = mob.getX() - playerX, hz = mob.getZ() - playerZ;
            if (hx * hx + hz * hz > TRACK_DISTANCE * TRACK_DISTANCE) continue;
            update(mob);
        }

        Iterator<Cube> it = cubes.values().iterator();
        while (it.hasNext()) if (tickCount - it.next().lastSeenTick > STALE_TICKS) it.remove();

        List<double[]> out = new ArrayList<>();
        for (Cube c : cubes.values()) if (!c.hidden) out.add(new double[] { c.x, c.z });
        centres = out;

        double up = client.player.getY() - CENTER_Y;
        boolean in = false;
        for (double[] c : out) {
            double flat = Math.hypot(playerX - c[0], playerZ - c[1]);
            if (flat * flat + up * up <= RADIUS * RADIUS) { in = true; break; }
        }
        inside = in;
    }

    private static void update(AbstractCubeMob mob) {
        Cube c = cubes.get(mob.getUUID());
        if (c == null) {
            if (isBridgeAnimation(mob.getX(), mob.getZ())) return;
            cubes.put(mob.getUUID(), new Cube(mob.getX(), mob.getY(), mob.getZ(), tickCount));
            return;
        }
        c.lastSeenTick = tickCount;
        double horizontal = Math.hypot(mob.getX() - c.lastX, mob.getZ() - c.lastZ), dy = mob.getY() - c.lastY;
        if (horizontal > LUNGE_EPS) c.downTicks = 0;
        else if (dy > MOVE_EPS) { c.downTicks = 0; c.hidden = false; }
        else if (dy < -MOVE_EPS && ++c.downTicks >= DOWN_TICKS_TO_HIDE) c.hidden = true;
        c.lastX = mob.getX(); c.lastY = mob.getY(); c.lastZ = mob.getZ();
    }

    public static boolean isBridgeAnimation(double x, double z) {
        if (x < -93.0 || x > -83.0) return false;
        return (z >= -143.0 && z <= -140.0) || (z >= -160.0 && z <= -157.0);
    }

    private static boolean hasVisibleName(Entity e) {
        return e.hasCustomName() && e.getCustomName() != null && !e.getCustomName().getString().isBlank();
    }
}
