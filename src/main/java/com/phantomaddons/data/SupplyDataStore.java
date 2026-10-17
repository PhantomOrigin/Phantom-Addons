package com.phantomaddons.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.phantomaddons.PhantomAddons;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.utils.PhantomPaths;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SupplyDataStore {

    public static final class Tentacle {
        public double x, y, z;
    }

    public static final class GrabAttempt {
        public long riseStartTick, stationaryTick, grabTick;
        public boolean pickupActiveAtStationary;
        public long ticksStationaryToGrab;
        public long ticksPickupStartToGrab;
        public double grabVelocityX, grabVelocityZ, grabSpeed;
        public double playerX, playerY, playerZ;
        public String clientId;
        public boolean firstForEntity;
        public double tentacleX, tentacleY, tentacleZ, tentacleRiseVelocity;
    }

    public static final class CancelAttempt {
        public int pearls;
        public int pearlsAnyDirection;
        public double averageCps;
        public float playerYaw, playerPitch;
        public double playerX, playerZ;
        public int supplyPercentAtReach;
        public boolean pickedUp;
        public boolean reachedPlayer = true;
        public String clientId, grabClientId;
    }

    public static final class TentacleSpawn {
        public String clientId;
        public double spawnX, spawnY, spawnZ;
        public int chainCubesAtFirstSight;
        public int chainCubesMax;
        public double chainMinY, chainMaxY;
        public long ticksBeforeRise;
        public double restX, restY, restZ;
        public double riseDistance;
        public long riseTicks;
        public double riseVelocity;
    }

    private static final Gson GSON = new Gson();
    private static final Type TENTACLES = new TypeToken<List<Tentacle>>() {}.getType();

    private static final String SPOTS_FILE = "phantomaddons_tentacle_spots.jsonl";

    private static final List<double[]> legacyColumns = new ArrayList<>();
    private static final Set<String> seenColumns = new HashSet<>();
    private static boolean legacyLoaded;

    private SupplyDataStore() {}

    private static double round(double v, int places) {
        double f = Math.pow(10, places);
        return Math.round(v * f) / f;
    }

    public static void recordSpawn(TentacleSpawn s) {
        s.spawnX = round(s.spawnX, 2); s.spawnY = round(s.spawnY, 2); s.spawnZ = round(s.spawnZ, 2);
        s.chainMinY = round(s.chainMinY, 2); s.chainMaxY = round(s.chainMaxY, 2);
        s.restX = round(s.restX, 2); s.restY = round(s.restY, 2); s.restZ = round(s.restZ, 2);
        s.riseDistance = round(s.riseDistance, 3);
        s.riseVelocity = round(s.riseVelocity, 3);
        noteSpot(s.restX, s.restZ);
        SupplyShare.addSpawn(s);
    }

    public static void recordGrab(GrabAttempt g) {
        g.playerX = round(g.playerX, 2);
        g.playerY = round(g.playerY, 2);
        g.playerZ = round(g.playerZ, 2);
        g.tentacleX = round(g.tentacleX, 2);
        g.tentacleY = round(g.tentacleY, 2);
        g.tentacleZ = round(g.tentacleZ, 2);
        g.tentacleRiseVelocity = round(g.tentacleRiseVelocity, 3);
        noteSpot(g.tentacleX, g.tentacleZ);
        SupplyShare.addGrab(g);
    }

    public static void recordCancel(CancelAttempt c) {
        c.playerX = round(c.playerX, 2);
        c.playerZ = round(c.playerZ, 2);
        c.averageCps = round(c.averageCps, 2);
        SupplyShare.addCancel(c);
    }

    public static synchronized List<double[]> restColumns() {
        if (!legacyLoaded) {
            legacyLoaded = true;
            Set<String> seen = seenColumns;
            try {
                Path tentacles = PhantomPaths.resolve("phantomaddons_tentacles.json");
                if (Files.exists(tentacles)) {
                    List<Tentacle> rows = GSON.fromJson(Files.readString(tentacles, StandardCharsets.UTF_8), TENTACLES);
                    if (rows != null) for (Tentacle t : rows) add(seen, t.x, t.z);
                }
                Path spots = PhantomPaths.resolve(SPOTS_FILE);
                if (Files.exists(spots)) {
                    for (String line : Files.readAllLines(spots, StandardCharsets.UTF_8)) {
                        if (line.isBlank()) continue;
                        try {
                            Tentacle t = GSON.fromJson(line, Tentacle.class);
                            if (t != null) add(seen, t.x, t.z);
                        } catch (RuntimeException ignored) {}
                    }
                }
                Path spawns = PhantomPaths.resolve("phantomaddons_tentacle_spawns.jsonl");
                if (Files.exists(spawns)) {
                    for (String line : Files.readAllLines(spawns, StandardCharsets.UTF_8)) {
                        if (line.isBlank()) continue;
                        try {
                            TentacleSpawn s = GSON.fromJson(line, TentacleSpawn.class);
                            if (s != null) add(seen, s.restX, s.restZ);
                        } catch (RuntimeException ignored) {}
                    }
                }
            } catch (IOException | RuntimeException e) {
                PhantomAddons.LOGGER.warn("[SupplyDataStore] couldn't read the saved tentacle positions: {}", e.toString());
            }
        }
        return legacyColumns;
    }

    private static synchronized void noteSpot(double x, double z) {
        if (!PhantomConfig.isTentacleSpotBeaconsEnabled()) return;
        restColumns();
        double rx = round(x, 2), rz = round(z, 2);
        if (!seenColumns.add(rx + "," + rz)) return;
        legacyColumns.add(new double[] { rx, rz });
        try {
            Tentacle t = new Tentacle();
            t.x = rx;
            t.z = rz;
            Files.writeString(PhantomPaths.resolve(SPOTS_FILE), GSON.toJson(t) + "\n", StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            PhantomAddons.LOGGER.warn("[SupplyDataStore] couldn't save a tentacle spot: {}", e.toString());
        }
    }

    private static void add(Set<String> seen, double x, double z) {
        if (seen.add(x + "," + z)) legacyColumns.add(new double[] { x, z });
    }
}
