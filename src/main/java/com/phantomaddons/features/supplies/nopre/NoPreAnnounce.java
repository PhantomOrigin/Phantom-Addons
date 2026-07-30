package com.phantomaddons.features.supplies.nopre;

import com.phantomaddons.utils.TextUtil;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.PhantomAddons;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.phys.Vec3;

public final class NoPreAnnounce {

    private static final Vec3 POS_SHOP     = new Vec3( -81.0,  76.0, -143.0);
    private static final Vec3 POS_X        = new Vec3(-142.5,  77.0, -148.0);
    private static final Vec3 POS_X_CANNON = new Vec3(-143.0,  76.0, -125.0);
    private static final Vec3 POS_EQUALS   = new Vec3( -65.5,  76.0,  -87.5);
    private static final Vec3 POS_SLASH    = new Vec3(-113.5,  77.0,  -68.5);
    private static final Vec3 POS_TRIANGLE = new Vec3( -67.5,  77.0, -122.5);
    private static final Vec3 POS_SQUARE   = new Vec3(-143.0,  76.0,  -80.0);

    private static final String ELLE_NOT_AGAIN = "[NPC] Elle: Not again!";

    private static final int    DETECT_TICK       = 170; // 8.5 seconds
    private static final int    MAIN_DELAY        = 1;    // ticks after first detecting a spawned crate
    private static final int    SECONDARY_DELAY   = 4;    // ticks after the main message
    private static final double MAIN_RANGE      = 18.0;
    private static final double X_SEC_RANGE     = 16.0;
    private static final double SLASH_SEC_RANGE = 20.0;

    private static final double X_XCANNON_Z_BOUNDARY = -136.0;

    private static int     ticksSinceStart   = -1;
    private static boolean detected          = false;
    private static Vec3    prePos            = null;
    private static String  preName           = null;
    private static Vec3    secondaryPos      = null;
    private static String  secondaryName     = null;
    private static double  secondaryRange    = MAIN_RANGE;

    private static boolean triggered         = false;
    private static boolean mainSent          = false;
    private static boolean secondarySent     = false;
    private static int     mainDelay         = -1;
    private static int     secondaryDelay    = -1;

    private NoPreAnnounce() {}

    public static void onSuppliesStart() {
        ticksSinceStart = 0;
        detected        = false;
        prePos          = null;
        preName         = null;
        secondaryPos    = null;
        secondaryName   = null;
        secondaryRange  = MAIN_RANGE;
        triggered       = false;
        mainSent        = false;
        secondarySent   = false;
        mainDelay       = -1;
        secondaryDelay  = -1;
    }

    public static void reset() {
        ticksSinceStart = -1;
        detected        = false;
        prePos          = null;
        preName         = null;
        secondaryPos    = null;
        secondaryName   = null;
        secondaryRange  = MAIN_RANGE;
        triggered       = false;
        mainSent        = false;
        secondarySent   = false;
        mainDelay       = -1;
        secondaryDelay  = -1;
    }

    public static void tick(Minecraft mc) {
        if (!PhantomConfig.isNoPreAnnounceEnabled()) return;
        if (ticksSinceStart < 0) return;
        if (mc.player == null || mc.level == null) return;

        ticksSinceStart++;

        if (!detected && ticksSinceStart == DETECT_TICK) {
            detected = true;
            Vec3 pos = mc.player.position();

            if (pos.distanceTo(POS_TRIANGLE) < 15.0) {
                set(POS_TRIANGLE, "Triangle", POS_SHOP,     "Shop",    MAIN_RANGE);
            } else if (pos.distanceTo(POS_X) < 30.0) {
                set(POS_X,        "X",        POS_X_CANNON, "X Cannon", X_SEC_RANGE);
            } else if (pos.distanceTo(POS_EQUALS) < 15.0) {
                set(POS_EQUALS,   "Equals",   null,         null,      MAIN_RANGE);
            } else if (pos.distanceTo(POS_SLASH) < 10.0) {
                set(POS_SLASH,    "Slash",    POS_SQUARE,   "Square",  SLASH_SEC_RANGE);
            }
            if (preName != null)
                PhantomAddons.LOGGER.info("[NoPreAnnounce] Detected pre-spot at tick {}: {}", ticksSinceStart, preName);
        }

        if (prePos != null && !triggered && anyCrateSpawned(mc)) {
            triggered = true;
            mainDelay = MAIN_DELAY;
            PhantomAddons.LOGGER.info("[NoPreAnnounce] Crate spawn detected at tick {} — checking main spot in {} tick(s)", ticksSinceStart, mainDelay);
        }

        if (mainDelay > 0) {
            mainDelay--;
        } else if (mainDelay == 0) {
            mainDelay = -1;
            checkMain(mc);
            if (secondaryPos != null) secondaryDelay = SECONDARY_DELAY;
            else secondarySent = true;
        }

        if (secondaryDelay > 0) {
            secondaryDelay--;
        } else if (secondaryDelay == 0) {
            secondaryDelay = -1;
            checkSecondary(mc);
        }
    }

    public static void onChat(String raw) {
        if (!PhantomConfig.isNoPreAnnounceEnabled()) return;
        String clean = TextUtil.stripColor(raw);

        if (clean.contains(ELLE_NOT_AGAIN)) {
            if (prePos == null) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.getConnection() == null) return;

            if (!mainSent) checkMain(mc);
            if (!secondarySent) checkSecondary(mc);
        }
    }

    private static void checkMain(Minecraft mc) {
        if (mainSent || mc.getConnection() == null) return;
        mainSent = true;

        boolean isXPair = "X".equals(preName) && "X Cannon".equals(secondaryName);
        boolean hasMain = isXPair
                ? spotFound(mc, prePos, MAIN_RANGE, null, X_XCANNON_Z_BOUNDARY) // X: z < boundary only
                : spotFound(mc, prePos, MAIN_RANGE, null, null);

        PhantomAddons.LOGGER.info("[NoPreAnnounce] Main check — {} present={}", preName, hasMain);
        if (!hasMain) {
            com.phantomaddons.features.supplies.PartyChatQueue.send("No " + preName + "!");
            PhantomAddons.LOGGER.info("[NoPreAnnounce] Queued: No {}!", preName);
        }
    }

    private static void checkSecondary(Minecraft mc) {
        if (secondarySent || secondaryPos == null || mc.getConnection() == null) return;
        secondarySent = true;

        boolean isXPair = "X".equals(preName) && "X Cannon".equals(secondaryName);
        boolean hasSecondary = isXPair
                ? spotFound(mc, secondaryPos, secondaryRange, X_XCANNON_Z_BOUNDARY, null) // X Cannon: z >= boundary only
                : spotFound(mc, secondaryPos, secondaryRange, null, null);

        PhantomAddons.LOGGER.info("[NoPreAnnounce] Secondary check — {} present={}", secondaryName, hasSecondary);
        if (!hasSecondary) {
            com.phantomaddons.features.supplies.PartyChatQueue.send("No " + secondaryName + "!");
            PhantomAddons.LOGGER.info("[NoPreAnnounce] Queued: No {}!", secondaryName);
        }
    }

    private static boolean anyCrateSpawned(Minecraft mc) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Giant giant && giant.getY() < 67.0) return true;
        }
        return false;
    }

    private static boolean spotFound(Minecraft mc, Vec3 target, double range, Double minZ, Double maxZ) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Giant giant)) continue;
            if (giant.getY() >= 67.0) continue;
            double angle = (giant.getYRot() + 130.0) * Math.PI / 180.0;
            Vec3 crate = new Vec3(
                    giant.getX() + 0.5 + 3.7 * Math.cos(angle),
                    75.0,
                    giant.getZ() + 0.5 + 3.7 * Math.sin(angle)
            );
            if (minZ != null && crate.z < minZ) continue;
            if (maxZ != null && crate.z > maxZ) continue;
            if (crate.distanceTo(target) < range) return true;
        }
        return false;
    }

    private static void set(Vec3 pre, String prN, Vec3 sec, String secN, double secRange) {
        prePos         = pre;
        preName        = prN;
        secondaryPos   = sec;
        secondaryName  = secN;
        secondaryRange = secRange;
    }
}
