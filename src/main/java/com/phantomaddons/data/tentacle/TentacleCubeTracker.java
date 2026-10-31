package com.phantomaddons.data.tentacle;

import com.phantomaddons.PhantomAddons;
import com.phantomaddons.data.GameplayDataLogger;
import com.phantomaddons.data.SupplyDataStore;
import com.phantomaddons.data.SupplyShare;
import com.phantomaddons.features.supplies.tentaclewarn.TentacleGrabWarning;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
//? if <26.2 {
/*import net.minecraft.world.entity.monster.Slime;
*///?} else {
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TentacleCubeTracker {

    private static final double MOVE_EPS = 1.0e-3;
    private static final int STATIONARY_TICKS = 3;
    private static final double TRACK_DISTANCE = 60.0;
    private static final int KUUDRA_SIZE = 30;
    private static final double CHAIN_RADIUS = 1.5;
    private static final double REACH_DISTANCE = 1.0;
    private static final double NEAR_DISTANCE = 2.0;
    private static final int REACH_WINDOW_TICKS = 20;
    private static final int CANCEL_PEARLS = 4;
    private static final float DOWN_PITCH_MIN = 65.0f;
    private static final long GRAB_TIMEOUT_TICKS = 200;
    private static final long OUTCOME_TIMEOUT_TICKS = 200;
    private static final long STALE_CUBE_TICKS = 40;

    private static final class Cube {
        final UUID id;
        Vec3 pos, changePos;
        long changeTick, lastSeenTick, streakStart;
        long riseStartTick = -1;
        double riseSum;
        int riseSamples;
        boolean stationary, grabbed, pickupAtStationary;
        boolean lunged;
        Vec3 stationaryPos;
        long stationaryTick;
        double riseVelocity;
        final Vec3 spawnPos;
        final long firstSeenTick;
        int chainFirst = -1, chainMax;
        double chainMinY, chainMaxY;
        boolean spawnLogged;

        Cube(UUID id, Vec3 pos, long tick) {
            this.id = id; this.pos = pos; this.changePos = pos;
            this.changeTick = tick; this.streakStart = tick; this.lastSeenTick = tick;
            this.spawnPos = pos; this.firstSeenTick = tick;
        }
    }

    private static final class ActiveGrab {
        int grabId;
        String grabClientId;
        UUID cube;
        long grabTick;
        long reachTick = -1;
        float reachYaw, reachPitch;
        double reachX, reachZ;
        int reachPercent;
        boolean near;
        float nearYaw, nearPitch;
        double nearX, nearZ;
        int nearPercent;
        boolean windowClosed;
        SupplyDataStore.CancelAttempt pending;
        long pendingSince;
    }

    private static final Map<UUID, Cube> cubes = new HashMap<>();
    private static final List<ActiveGrab> grabs = new ArrayList<>();
    private static final Set<UUID> countedTentacles = new HashSet<>();
    private static int grabCounter;

    private record Throw(long tick, boolean down, float yaw, float pitch, double x, double z, int percent) {}
    private static final List<Throw> throwsList = new ArrayList<>();

    private static boolean pickupActive;
    private static long pickupStartTick = -1, pickupEndTick = -1;
    private static int pickupPercent = -1;

    private static boolean chestInSlot, chestSeen;
    private static long chestTick = -1;
    private static long droppedTick = -1;

    private static Vec3 playerPos = Vec3.ZERO;

    private TentacleCubeTracker() {}

    public static void reset() {
        cubes.clear();
        grabs.clear();
        countedTentacles.clear();
        throwsList.clear();
        pickupActive = false;
        pickupStartTick = pickupEndTick = -1;
        pickupPercent = -1;
        chestInSlot = chestSeen = false;
        chestTick = -1;
        droppedTick = -1;
    }

    public static void onSupplyDropped() {
        droppedTick = GameplayDataLogger.runTick();
        onPickupEnd(-1);
    }

    // ── Pickup and pearl events ───────────────────────────────────────────────

    public static void onPickupStart(int percent) {
        pickupActive = true;
        pickupStartTick = GameplayDataLogger.runTick();
        pickupEndTick = -1;
        pickupPercent = percent;
    }

    public static void onPickupPercent(int percent) {
        if (pickupActive) pickupPercent = percent;
    }

    public static void onPickupEnd(int finalPercent) {
        pickupActive = false;
        pickupEndTick = GameplayDataLogger.runTick();
        if (finalPercent >= 0) pickupPercent = finalPercent;
    }

    public static void onPearlThrow(Player player) {
        long tick = GameplayDataLogger.runTick();
        throwsList.add(new Throw(tick, player.getXRot() >= DOWN_PITCH_MIN, player.getYRot(), player.getXRot(),
                player.getX(), player.getZ(), Math.max(0, pickupPercent)));
        throwsList.removeIf(t -> tick - t.tick > 600);
    }

    private static boolean wasPickingUpAt(long tick) {
        return pickupStartTick >= 0 && pickupStartTick <= tick && (pickupActive || pickupEndTick >= tick);
    }

    // ── Per tick ──────────────────────────────────────────────────────────────

    public static void tick(Minecraft client, long tick) {
        if (client.level == null || client.player == null) return;
        playerPos = client.player.position();
        watchChest(client, tick);

        List<AbstractCubeMob> nearby = new ArrayList<>();
        for (Entity e : client.level.entitiesForRendering()) {
            if (!(e instanceof AbstractCubeMob mob)) continue;
            if (mob.getSize() == KUUDRA_SIZE) continue;
            if (mob.distanceToSqr(client.player) > TRACK_DISTANCE * TRACK_DISTANCE) continue;
            nearby.add(mob);
        }

        for (AbstractCubeMob mob : nearby) {
            if (!mob.isInvisible()) continue;
            if (mob.hasCustomName() && mob.getCustomName() != null && !mob.getCustomName().getString().isBlank()) continue;

            Cube cube = cubes.computeIfAbsent(mob.getUUID(), id -> new Cube(id, mob.position(), tick));
            cube.lastSeenTick = tick;
            if (!cube.spawnLogged && cube.riseStartTick < 0 && !cube.stationary) measureChain(cube, mob, nearby);
            update(cube, mob.position(), tick);
        }

        Iterator<Cube> it = cubes.values().iterator();
        while (it.hasNext()) {
            if (tick - it.next().lastSeenTick > STALE_CUBE_TICKS) it.remove();
        }

        updateGrabs(client, tick);
    }

    private static void watchChest(Minecraft client, long tick) {
        boolean chest = client.player.getInventory().getItem(8).is(Items.CHEST);
        if (chestSeen && chest && !chestInSlot) {
            chestTick = tick;
            PhantomAddons.LOGGER.info("[TentacleData] chest appeared in the ninth slot");
        }
        chestInSlot = chest;
        chestSeen = true;
    }

    private static void measureChain(Cube c, AbstractCubeMob top, List<AbstractCubeMob> nearby) {
        int count = 0;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        Vec3 tp = top.position();
        for (AbstractCubeMob other : nearby) {
            if (other == top) continue;
            double dx = other.getX() - tp.x, dz = other.getZ() - tp.z;
            if (dx * dx + dz * dz > CHAIN_RADIUS * CHAIN_RADIUS) continue;
            count++;
            minY = Math.min(minY, other.getY());
            maxY = Math.max(maxY, other.getY());
        }
        if (c.chainFirst < 0) c.chainFirst = count;
        if (count > c.chainMax) {
            c.chainMax = count;
            c.chainMinY = minY;
            c.chainMaxY = maxY;
        }
    }

    private static void recordSpawn(Cube c) {
        c.spawnLogged = true;
        SupplyDataStore.TentacleSpawn s = new SupplyDataStore.TentacleSpawn();
        s.clientId = SupplyShare.newId();
        s.spawnX = c.spawnPos.x; s.spawnY = c.spawnPos.y; s.spawnZ = c.spawnPos.z;
        s.chainCubesAtFirstSight = Math.max(0, c.chainFirst);
        s.chainCubesMax = c.chainMax;
        s.chainMinY = c.chainMax > 0 ? c.chainMinY : 0;
        s.chainMaxY = c.chainMax > 0 ? c.chainMaxY : 0;
        s.ticksBeforeRise = c.riseStartTick - c.firstSeenTick;
        s.restX = c.stationaryPos.x; s.restY = c.stationaryPos.y; s.restZ = c.stationaryPos.z;
        s.riseDistance = c.stationaryPos.y - c.spawnPos.y;
        s.riseTicks = c.stationaryTick - c.riseStartTick;
        s.riseVelocity = c.riseVelocity;
        SupplyDataStore.recordSpawn(s);
    }

    private static void update(Cube c, Vec3 p, long tick) {
        boolean moved = p.distanceToSqr(c.pos) > MOVE_EPS * MOVE_EPS;
        if (moved) {
            long dt = Math.min(2, Math.max(1, tick - c.changeTick));
            double dx = (p.x - c.changePos.x) / dt, dy = (p.y - c.changePos.y) / dt, dz = (p.z - c.changePos.z) / dt;
            double horizontal = Math.hypot(dx, dz);

            if (!c.stationary && horizontal >= 0.02 && !c.lunged) {
                c.lunged = true;
                double rest = restHeightOf(c);
                if (!Double.isNaN(rest)) {
                    if (Math.abs(c.pos.y - rest) > 0.5) {
                        PhantomAddons.LOGGER.info("[TentacleData] cube lunged at y {} but its predicted top is {}", c.pos.y, rest);
                    }
                    c.stationaryPos = new Vec3(c.pos.x, Math.round(rest), c.pos.z);
                    c.stationaryTick = c.streakStart;
                    c.pickupAtStationary = wasPickingUpAt(c.stationaryTick);
                    c.riseVelocity = c.riseSamples > 0 ? c.riseSum / c.riseSamples : 0;
                    if (!c.spawnLogged && c.riseStartTick >= 0) recordSpawn(c);
                    c.stationary = true;
                }
            }
            if (c.stationary) {
                if (horizontal > MOVE_EPS) c.lunged = true;
                if (horizontal > MOVE_EPS && !c.grabbed && pickupActive) {
                    c.grabbed = true;
                    onGrabAttempt(c, tick, dx, dz, horizontal);
                }
                c.stationary = false;
                c.grabbed = false;
                c.riseStartTick = -1;
                c.riseSum = 0;
                c.riseSamples = 0;
            } else if (dy > MOVE_EPS && horizontal < 0.02) {
                if (c.riseStartTick < 0) c.riseStartTick = tick;
                c.riseSum += dy;
                c.riseSamples++;
            }
            c.streakStart = tick;
            c.changePos = p;
            c.changeTick = tick;
        } else if (!c.stationary && c.riseStartTick >= 0 && tick - c.streakStart > STATIONARY_TICKS) {
            c.stationary = true;
            c.grabbed = false;
            c.lunged = false;
            c.stationaryPos = p;
            c.stationaryTick = c.streakStart;
            c.pickupAtStationary = wasPickingUpAt(c.stationaryTick);
            c.riseVelocity = c.riseSamples > 0 ? c.riseSum / c.riseSamples : 0;
            if (!c.spawnLogged) recordSpawn(c);
        }
        c.pos = p;
    }

    // ── A grab attempt ────────────────────────────────────────────────────────

    private static void onGrabAttempt(Cube c, long tick, double vx, double vz, double speed) {
        boolean first = countedTentacles.add(c.id);

        SupplyDataStore.GrabAttempt g = new SupplyDataStore.GrabAttempt();
        g.clientId = SupplyShare.newId();
        g.firstForEntity = first;
        g.tentacleX = c.stationaryPos.x;
        g.tentacleY = c.stationaryPos.y;
        g.tentacleZ = c.stationaryPos.z;
        g.tentacleRiseVelocity = c.riseVelocity;
        g.riseStartTick = c.riseStartTick;
        g.stationaryTick = c.stationaryTick;
        g.grabTick = tick;
        g.pickupActiveAtStationary = c.pickupAtStationary;
        g.ticksStationaryToGrab = tick - c.stationaryTick;
        g.ticksPickupStartToGrab = tick - pickupStartTick;
        g.grabVelocityX = vx;
        g.grabVelocityZ = vz;
        g.grabSpeed = speed;
        g.playerX = playerPos.x;
        g.playerY = playerPos.y;
        g.playerZ = playerPos.z;
        SupplyDataStore.recordGrab(g);

        ActiveGrab a = new ActiveGrab();
        a.grabId = ++grabCounter;
        a.grabClientId = g.clientId;
        a.cube = c.id;
        a.grabTick = tick;
        grabs.add(a);
    }

    private static void finishUnreached(ActiveGrab a, Minecraft client) {
        long end = chestTick;
        int down = 0, any = 0;
        Throw lastDown = null;
        for (Throw t : throwsList) {
            if (t.tick < a.grabTick || t.tick > end) continue;
            any++;
            if (t.down) { down++; lastDown = t; }
        }
        PhantomAddons.LOGGER.info("[TentacleData] grab {}: supply picked up {} ticks after the grab, before the cube reached the player ({} downward pearls, {} in any direction)",
                a.grabId, end - a.grabTick, down, any);
        if (down <= CANCEL_PEARLS) return;

        SupplyDataStore.CancelAttempt c = new SupplyDataStore.CancelAttempt();
        c.clientId = SupplyShare.newId();
        c.grabClientId = a.grabClientId;
        c.pearls = down;
        c.pearlsAnyDirection = any;
        c.averageCps = down / (Math.max(1, end - a.grabTick) / 20.0);
        if (a.near) {
            c.playerYaw = a.nearYaw;
            c.playerPitch = a.nearPitch;
            c.playerX = a.nearX;
            c.playerZ = a.nearZ;
            c.supplyPercentAtReach = a.nearPercent;
        } else {
            c.playerYaw = lastDown.yaw();
            c.playerPitch = lastDown.pitch();
            c.playerX = lastDown.x();
            c.playerZ = lastDown.z();
            c.supplyPercentAtReach = lastDown.percent();
        }
        PhantomAddons.LOGGER.info("[TentacleData] grab {}: view and position taken {}", a.grabId,
                a.near ? "when the cube came within 2 blocks" : "at the last downward pearl (the cube never came within 2 blocks)");
        c.reachedPlayer = false;
        c.pickedUp = true;
        SupplyDataStore.recordCancel(c);
        PhantomAddons.LOGGER.info("[TentacleData] grab {} cancel attempt recorded without the cube reaching the player, supply picked up: true", a.grabId);
    }

    private static void updateGrabs(Minecraft client, long tick) {
        Iterator<ActiveGrab> it = grabs.iterator();
        while (it.hasNext()) {
            ActiveGrab a = it.next();

            if (a.reachTick < 0) {
                Cube cube = cubes.get(a.cube);
                double dist = cube != null ? cube.pos.distanceTo(client.player.position()) : Double.MAX_VALUE;
                if (!a.near && dist <= NEAR_DISTANCE) {
                    a.near = true;
                    a.nearYaw = client.player.getYRot();
                    a.nearPitch = client.player.getXRot();
                    a.nearX = client.player.getX();
                    a.nearZ = client.player.getZ();
                    a.nearPercent = Math.max(0, pickupPercent);
                }
                if (chestTick >= a.grabTick) {
                    finishUnreached(a, client);     // they picked the supply up before the cube got to them
                    it.remove();
                    continue;
                }
                if (dist <= REACH_DISTANCE) {
                    PhantomAddons.LOGGER.info("[TentacleData] grab {} reached the player {} ticks after the grab", a.grabId, tick - a.grabTick);
                    a.reachTick = tick;
                    a.reachYaw = client.player.getYRot();
                    a.reachPitch = client.player.getXRot();
                    a.reachX = client.player.getX();
                    a.reachZ = client.player.getZ();
                    a.reachPercent = Math.max(0, pickupPercent);
                } else if (tick - a.grabTick > GRAB_TIMEOUT_TICKS) {
                    PhantomAddons.LOGGER.info("[TentacleData] grab {} dropped: the cube never came within 1 block of the player", a.grabId);
                    it.remove();
                }
                continue;
            }

            if (!a.windowClosed && tick >= a.reachTick + REACH_WINDOW_TICKS) {
                a.windowClosed = true;
                long end = a.reachTick + REACH_WINDOW_TICKS;
                int down = 0, any = 0;
                for (Throw t : throwsList) {
                    if (t.tick < a.grabTick || t.tick > end) continue;
                    any++;
                    if (t.down) down++;
                }
                PhantomAddons.LOGGER.info("[TentacleData] grab {} window closed: {} downward pearls, {} in any direction", a.grabId, down, any);
                if (down <= CANCEL_PEARLS) { it.remove(); continue; }

                SupplyDataStore.CancelAttempt c = new SupplyDataStore.CancelAttempt();
                c.clientId = SupplyShare.newId();
                c.grabClientId = a.grabClientId;
                c.pearls = down;
                c.pearlsAnyDirection = any;
                c.averageCps = down / ((end - a.grabTick) / 20.0);
                c.playerYaw = a.reachYaw;
                c.playerPitch = a.reachPitch;
                c.playerX = a.reachX;
                c.playerZ = a.reachZ;
                c.supplyPercentAtReach = a.reachPercent;
                c.reachedPlayer = true;
                a.pending = c;
                a.pendingSince = tick;
            }

            if (a.pending != null) {
                boolean got = chestTick >= a.grabTick;
                boolean slipped = droppedTick >= a.grabTick;
                if (got || slipped || tick - a.pendingSince > OUTCOME_TIMEOUT_TICKS) {
                    a.pending.pickedUp = got;
                    SupplyDataStore.recordCancel(a.pending);
                    PhantomAddons.LOGGER.info("[TentacleData] grab {} cancel attempt recorded, supply picked up: {}", a.grabId, got);
                    it.remove();
                }
            }
        }
    }

    // ── For the debug highlight ───────────────────────────────────────────────

    private static double restHeightOf(Cube c) {
        double rest = TentacleGrabWarning.restHeightFor(c.spawnPos.y);
        if (Double.isNaN(rest) && c.riseStartTick >= 0) rest = TentacleGrabWarning.restHeightAbove(c.pos.y);
        if (Double.isNaN(rest) && TentacleGrabWarning.isRestOnlyHeight(c.pos.y)) rest = c.pos.y;
        return rest;
    }

    public static int stateOf(UUID entity) {
        Cube c = cubes.get(entity);
        if (c == null) return -1;
        for (ActiveGrab a : grabs) if (a.cube.equals(entity)) return 2;
        return c.stationary ? 1 : 0;
    }
}
