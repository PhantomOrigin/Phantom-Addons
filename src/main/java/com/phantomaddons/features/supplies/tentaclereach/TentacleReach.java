package com.phantomaddons.features.supplies.tentaclereach;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.supplies.tentaclewarn.TentacleGrabWarning;
import com.phantomaddons.phase.KuudraPhaseTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
//? if <26.2 {
/*import net.minecraft.world.entity.monster.Slime;
*///?} else {
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
//?}
import net.minecraft.world.phys.Vec3;

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
    private static final double MOVE_EPS = 1.0e-3;
    private static final double LUNGE_EPS = 0.02;
    private static final int    DOWN_TICKS_TO_HIDE = 5;
    private static final long   STALE_TICKS = 40;

    private static final class Cube {
        Vec3 pos;
        long lastSeenTick;
        boolean anchored, lunged, hidden;
        double anchorX, anchorZ;
        int downTicks;

        Cube(Vec3 pos, long tick) { this.pos = pos; this.lastSeenTick = tick; }

        void anchor(Vec3 at) { anchored = true; anchorX = at.x; anchorZ = at.z; }
    }

    private static final Map<UUID, Cube> cubes = new HashMap<>();
    private static volatile List<double[]> centres = List.of();
    private static long tickCount;

    private TentacleReach() {}

    public static List<double[]> getCentres() { return centres; }

    public static void reset() {
        cubes.clear();
        centres = List.of();
    }

    public static void tick(Minecraft client) {
        tickCount++;
        if (!PhantomConfig.isTentacleReachEnabled() || client.player == null || client.level == null
                || !KuudraPhaseTracker.isSupplyCollectionPhase()) {
            reset();
            return;
        }

        Vec3 player = client.player.position();
        for (Entity e : client.level.entitiesForRendering()) {
            if (!(e instanceof AbstractCubeMob mob)) continue;
            if (mob.getSize() == KUUDRA_SIZE || !mob.isInvisible() || hasVisibleName(mob)) continue;
            double hx = mob.getX() - player.x, hz = mob.getZ() - player.z;
            if (hx * hx + hz * hz > TRACK_DISTANCE * TRACK_DISTANCE) continue;
            update(mob);
        }

        Iterator<Cube> it = cubes.values().iterator();
        while (it.hasNext()) if (tickCount - it.next().lastSeenTick > STALE_TICKS) it.remove();

        List<double[]> out = new ArrayList<>();
        for (Cube c : cubes.values()) {
            if (c.anchored && !c.hidden) out.add(new double[] { c.anchorX, c.anchorZ });
        }
        centres = out;
    }

    private static void update(AbstractCubeMob mob) {
        Vec3 p = mob.position();
        Cube c = cubes.get(mob.getUUID());
        if (c == null) {
            c = new Cube(p, tickCount);
            if (!Double.isNaN(TentacleGrabWarning.restHeightFor(p.y)) || TentacleGrabWarning.isRestOnlyHeight(p.y)) c.anchor(p);
            cubes.put(mob.getUUID(), c);
            return;
        }
        c.lastSeenTick = tickCount;
        double horizontal = Math.hypot(p.x - c.pos.x, p.z - c.pos.z), dy = p.y - c.pos.y;
        if (horizontal > LUNGE_EPS) {
            c.lunged = true;
            c.downTicks = 0;
        } else if (dy > MOVE_EPS) {
            c.downTicks = 0;
            c.hidden = false;
            if (!c.anchored && !c.lunged) c.anchor(p);
        } else if (dy < -MOVE_EPS) {
            if (++c.downTicks >= DOWN_TICKS_TO_HIDE) c.hidden = true;
        }
        c.pos = p;
    }

    private static boolean hasVisibleName(Entity e) {
        return e.hasCustomName() && e.getCustomName() != null && !e.getCustomName().getString().isBlank();
    }
}
