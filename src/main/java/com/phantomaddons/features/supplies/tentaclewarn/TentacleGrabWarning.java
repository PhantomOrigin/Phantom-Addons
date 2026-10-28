package com.phantomaddons.features.supplies.tentaclewarn;

import com.phantomaddons.PhantomAddons;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.supplies.pearlwaypoints.PearlWaypointManager;
import com.phantomaddons.phase.KuudraPhaseTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
//? if <26.2 {
/*import net.minecraft.world.entity.monster.Slime;
*///?} else {
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
//?}
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TentacleGrabWarning {

    public record Box(Vec3 center, double halfWidth, double halfHeight, double rotationDeg) {}

    public record Target(List<Box> boxes, Vec3 anchor, boolean primary, float confidence) {}

    private static final int    KUUDRA_SIZE = 30;
    private static final double TRACK_DISTANCE = 30.0;
    private static final double CHAIN_RADIUS = 1.5;
    private static final double MOVE_EPS = 1.0e-3;
    private static final long   LUNGE_HOLD_MS = 3000;
    private static final double CUBE_PAD = 0.06;
    private static final boolean DEBUG = true;
    private static final double LUNGE_EPS = 0.02;
    private static final long   STALE_TICKS = 40;
    private static final long   RISE_GRACE_MS = 250;

    private static final double RANGE_FULL_TO = 17.5, RANGE_END = 18.5;

    private static final double RISE_SPEED = 2.21;
    private static final int[] REST_HEIGHTS = {59, 65, 67, 93, 110, 114};
    private static final Map<Integer, Integer> REST_FOR_START = Map.of(41, 67, 20, 65, 67, 93, 65, 110, 59, 114, 36, 59, 22, 59);

    private static final Map<Integer, Integer> CHAIN_FOR_START = Map.of(67, 12, 65, 15, 59, 18);
    private static final double RISE_AFTER_CHAIN_TICKS = 3.0, TICKS_PER_CHAIN_CUBE = 3.5;
    private static final double START_HEIGHT_TOLERANCE = 0.3;

    private static final float[] GRAB_T = {0f, 5f, 10f, 20f, 40f, 80f, 130f};
    private static final float[] GRAB_P = {0f, 0.14f, 0.29f, 0.57f, 0.76f, 0.97f, 1f};

    private static final class Cube {
        Vec3 pos;
        long lastSeenTick, lastRiseMs = -1, lungeMs = -1;
        boolean lungeInRange;
        double startY;
        boolean rising, spent;
        int chainLive;
        final Set<UUID> members = new HashSet<>();
        Cube(Vec3 pos, long tick) { this.pos = pos; this.lastSeenTick = tick; this.startY = pos.y; }
    }

    private static final Map<UUID, Cube> cubes = new HashMap<>();
    private static final Map<UUID, AbstractCubeMob> live = new HashMap<>();
    private static volatile List<Target> targets = List.of();
    private static long tickCount;

    private TentacleGrabWarning() {}

    public static List<Target> getTargets() { return targets; }

    public static boolean isGrabLikely() { return !targets.isEmpty(); }

    public static void reset() {
        cubes.clear();
        live.clear();
        targets = List.of();
    }

    public static void tick(Minecraft client) {
        tickCount++;
        if (!PhantomConfig.isTentacleGrabWarningEnabled() || client.player == null || client.level == null
                || !KuudraPhaseTracker.isSupplyCollectionPhase()) {
            if (!cubes.isEmpty()) cubes.clear();
            targets = List.of();
            return;
        }

        Vec3 player = client.player.position();
        List<AbstractCubeMob> anchors = new ArrayList<>();
        List<AbstractCubeMob> chain = new ArrayList<>();
        live.clear();
        for (Entity e : client.level.entitiesForRendering()) {
            if (!(e instanceof AbstractCubeMob mob)) continue;
            if (mob.getSize() == KUUDRA_SIZE) continue;
            if (hasVisibleName(mob)) continue;
            double hx = mob.getX() - player.x, hz = mob.getZ() - player.z;   // horizontal only: tentacles are tall, so 3D distance would clip them
            if (hx * hx + hz * hz > TRACK_DISTANCE * TRACK_DISTANCE) continue;
            if (mob.isInvisible()) {
                anchors.add(mob);
            } else {
                chain.add(mob);
                live.put(mob.getUUID(), mob);
            }
        }

        for (AbstractCubeMob mob : anchors) {
            updateCube(mob, player);
            Cube c = cubes.get(mob.getUUID());
            if (c != null) measureChain(c, mob, chain);
        }
        Iterator<Cube> it = cubes.values().iterator();
        while (it.hasNext()) if (tickCount - it.next().lastSeenTick > STALE_TICKS) it.remove();

        long elapsedMs = PearlWaypointManager.getPickupElapsedMs();
        if (elapsedMs < 0) { targets = List.of(); return; }
        float elapsedTicks = elapsedMs / 50f;
        float remainingTicks = Math.max(0L, PhantomConfig.getPickupDurationMs() - elapsedMs) / 50f;
        float timing = timingFactor(elapsedTicks, remainingTicks);
        float min = PhantomConfig.getTentacleGrabWarningMinConfidence();
        long pickupStartMs = System.currentTimeMillis() - elapsedMs - RISE_GRACE_MS;

        record Scored(AbstractCubeMob mob, double dist, double readyTicks, float confidence) {}
        List<Scored> scored = new ArrayList<>();
        for (AbstractCubeMob mob : anchors) {
            Cube c = cubes.get(mob.getUUID());
            if (c == null) continue;
            double dist = Math.hypot(mob.getX() - player.x, mob.getZ() - player.z);
            double riseIn = ticksUntilRise(c, mob);
            if (riseIn >= 0) {
                float conf = (float) (rangeFactor(dist) * timingFactor(elapsedTicks, (float) Math.max(0.0, remainingTicks - riseIn)));
                if (DEBUG && tickCount % 4 == 0) {
                    PhantomAddons.LOGGER.info("[TentacleWarn]   {} pending dist={} conf={} chain={} riseInTicks={} anchorY={}",
                            mob.getUUID().toString().substring(0, 4), String.format("%.1f", dist), String.format("%.2f", conf),
                            c.chainLive, String.format("%.0f", riseIn), String.format("%.1f", mob.getY()));
                }
                if (conf >= min && conf > 0f) {
                    Integer rest = REST_FOR_START.get((int) Math.round(mob.getY()));
                    scored.add(new Scored(mob, dist, riseIn + (rest == null ? 0.0 : (rest - mob.getY()) / RISE_SPEED), conf));
                }
                continue;
            }
            if (c.lastRiseMs < pickupStartMs) continue;
            if (c.spent) {
                if (c.lungeMs >= 0 && c.lungeInRange && System.currentTimeMillis() - c.lungeMs <= LUNGE_HOLD_MS) {
                    scored.add(new Scored(mob, dist, -1.0, 1f));
                }
                continue;
            }
            float conf = (float) (rangeFactor(dist) * timing);
            if (DEBUG && tickCount % 4 == 0) {
                List<Box> bx = tentacleBoxes(mob, c);
                double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
                for (Box b : bx) { lo = Math.min(lo, b.center().y - b.halfHeight()); hi = Math.max(hi, b.center().y + b.halfHeight()); }
                PhantomAddons.LOGGER.info("[TentacleWarn]   {} dist={} conf={} min={} boxes={} boxY={}..{} anchorY={}",
                        mob.getUUID().toString().substring(0, 4), String.format("%.1f", dist), String.format("%.2f", conf), min,
                        bx.size(), String.format("%.1f", lo), String.format("%.1f", hi), String.format("%.1f", mob.getY()));
            }
            if (conf >= min && conf > 0f) scored.add(new Scored(mob, dist, ticksToRest(c, mob.getY()), conf));
        }
        scored.sort(Comparator.<Scored>comparingDouble(s -> -Math.round(s.confidence() * 100f))
                .thenComparingDouble(Scored::readyTicks)
                .thenComparingDouble(Scored::dist));

        List<Target> out = new ArrayList<>(scored.size());
        for (int i = 0; i < scored.size(); i++) {
            Scored s = scored.get(i);
            out.add(new Target(tentacleBoxes(s.mob(), cubes.get(s.mob().getUUID())), s.mob().position(), i == 0, s.confidence()));
        }
        targets = out;
    }

    private static void updateCube(AbstractCubeMob mob, Vec3 player) {
        Vec3 p = mob.position();
        Cube c = cubes.get(mob.getUUID());
        if (c == null) {
            cubes.put(mob.getUUID(), new Cube(p, tickCount));
            return;
        }
        c.lastSeenTick = tickCount;
        double horizontal = Math.hypot(p.x - c.pos.x, p.z - c.pos.z);
        double dy = p.y - c.pos.y;
        c.rising = false;
        if (horizontal > LUNGE_EPS) {
            c.spent = true;
            long now = System.currentTimeMillis();
            if (c.lungeMs < 0 || now - c.lungeMs > LUNGE_HOLD_MS) {
                c.lungeInRange = rangeFactor(Math.hypot(c.pos.x - player.x, c.pos.z - player.z)) > 0.0;
            }
            c.lungeMs = now;
        } else if (dy > MOVE_EPS) {
            if (c.spent) c.startY = c.pos.y;
            c.spent = false;
            c.lungeMs = -1;
            c.rising = true;
            c.lastRiseMs = System.currentTimeMillis();
        } else if (dy < -MOVE_EPS) {
            c.spent = true;
        }
        if (DEBUG && tickCount % 4 == 0 && PearlWaypointManager.getPickupElapsedMs() >= 0) {
            PhantomAddons.LOGGER.info("[TentacleWarn] {} y={} dy={} horiz={} rising={} spent={} riseAgeMs={}",
                    mob.getUUID().toString().substring(0, 4), String.format("%.2f", p.y), String.format("%.3f", dy),
                    String.format("%.4f", horizontal), c.rising, c.spent,
                    c.lastRiseMs < 0 ? -1 : System.currentTimeMillis() - c.lastRiseMs);
        }
        c.pos = p;
    }

    private static double ticksUntilRise(Cube c, AbstractCubeMob mob) {
        if (c.rising || c.chainLive < 1) return -1.0;
        if (c.lungeMs >= 0 && System.currentTimeMillis() - c.lungeMs <= LUNGE_HOLD_MS) return -1.0;
        double y = mob.getY();
        Integer expected = CHAIN_FOR_START.get((int) Math.round(y));
        if (expected == null || Math.abs(y - Math.round(y)) > START_HEIGHT_TOLERANCE) return -1.0;
        return RISE_AFTER_CHAIN_TICKS + TICKS_PER_CHAIN_CUBE * Math.max(0, expected - c.chainLive);
    }

    private static double ticksToRest(Cube c, double y) {
        if (!c.rising) return 0.0;
        Integer known = REST_FOR_START.get((int) Math.round(c.startY));
        double target = known != null && known >= y - 0.5 ? known : Double.NaN;
        if (Double.isNaN(target)) {
            for (int h : REST_HEIGHTS) if (h >= y - 0.5) { target = h; break; }
        }
        return Double.isNaN(target) ? 0.0 : Math.max(0.0, target - y) / RISE_SPEED;
    }

    private static void measureChain(Cube c, AbstractCubeMob anchor, List<AbstractCubeMob> chain) {
        double ax = anchor.getX(), ay = anchor.getY(), az = anchor.getZ();
        c.chainLive = 0;
        for (AbstractCubeMob m : chain) {
            double dx = m.getX() - ax, dz = m.getZ() - az;
            if (dx * dx + dz * dz > CHAIN_RADIUS * CHAIN_RADIUS) continue;
            c.chainLive++;
            c.members.add(m.getUUID());
        }
    }

    private static List<Box> tentacleBoxes(AbstractCubeMob anchor, Cube c) {
        List<Box> boxes = new ArrayList<>();
        AABB top = anchor.getBoundingBox();
        boxes.add(new Box(top.getCenter(), top.getXsize() / 2.0 + CUBE_PAD, top.getYsize() / 2.0 + CUBE_PAD, 0.0));
        if (c != null) {
            for (UUID id : c.members) {
                AbstractCubeMob m = live.get(id);
                if (m == null) continue;   // gone or out of range this tick
                AABB bb = m.getBoundingBox();
                boxes.add(new Box(bb.getCenter(), bb.getXsize() / 2.0 + CUBE_PAD, bb.getYsize() / 2.0 + CUBE_PAD, 180.0 - m.yBodyRot));
            }
        }
        return boxes;
    }

    static double rangeFactor(double d) {
        if (d >= RANGE_END) return 0.0;
        if (d > RANGE_FULL_TO) return (RANGE_END - d) / (RANGE_END - RANGE_FULL_TO);
        return 1.0;
    }

    static float timingFactor(float elapsedTicks, float remainingTicks) {
        float now = grabCdf(elapsedTicks), end = grabCdf(elapsedTicks + remainingTicks);
        float left = Math.max(0.08f, 1f - now);
        return Math.max(0f, Math.min(1f, (end - now) / left));
    }

    private static float grabCdf(float t) {
        if (t <= GRAB_T[0]) return 0f;
        for (int i = 1; i < GRAB_T.length; i++) {
            if (t <= GRAB_T[i]) return GRAB_P[i - 1] + (GRAB_P[i] - GRAB_P[i - 1]) * (t - GRAB_T[i - 1]) / (GRAB_T[i] - GRAB_T[i - 1]);
        }
        return 1f;
    }

    private static boolean hasVisibleName(Entity e) {
        return e.hasCustomName() && e.getCustomName() != null && !e.getCustomName().getString().isBlank();
    }
}
