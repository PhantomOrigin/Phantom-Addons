package com.kuudrahelper.features.supplies;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.KuudraHelperMod;
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

    private static final int    DETECT_TICK     = 170; // 8.5 seconds
    private static final double MAIN_RANGE      = 18.0;
    private static final double X_SEC_RANGE     = 16.0;
    private static final double SLASH_SEC_RANGE = 20.0;

    private static int     ticksSinceStart = -1;
    private static boolean detected        = false;
    private static Vec3    prePos          = null;
    private static String  preName         = null;
    private static Vec3    secondaryPos    = null;
    private static String  secondaryName   = null;
    private static double  secondaryRange  = MAIN_RANGE;

    private NoPreAnnounce() {}

    public static void onSuppliesStart() {
        ticksSinceStart = 0;
        detected        = false;
        prePos          = null;
        preName         = null;
        secondaryPos    = null;
        secondaryName   = null;
        secondaryRange  = MAIN_RANGE;
    }

    public static void reset() {
        ticksSinceStart = -1;
        detected        = false;
        prePos          = null;
        preName         = null;
        secondaryPos    = null;
        secondaryName   = null;
        secondaryRange  = MAIN_RANGE;
    }

    public static void tick(Minecraft mc) {
        if (!KuudraConfig.isNoPreAnnounceEnabled()) return;
        if (ticksSinceStart < 0 || detected) return;
        if (mc.player == null) return;

        ticksSinceStart++;

        if (ticksSinceStart == DETECT_TICK) {
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
                KuudraHelperMod.LOGGER.info("[NoPreAnnounce] Detected pre-spot at tick {}: {}", ticksSinceStart, preName);
        }
    }

    public static void onChat(String raw) {
        if (!KuudraConfig.isNoPreAnnounceEnabled()) return;
        String clean = raw.replaceAll("§[0-9a-fk-orA-FK-OR]", "");

        if (clean.contains(ELLE_NOT_AGAIN)) {
            if (prePos == null) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.getConnection() == null) return;

            boolean hasMain      = spotFound(mc, prePos, MAIN_RANGE);
            boolean hasSecondary = secondaryPos == null || spotFound(mc, secondaryPos, secondaryRange);

            KuudraHelperMod.LOGGER.info(
                "[NoPreAnnounce] Not again — main={} secondary={}", hasMain, hasSecondary);

            if (!hasMain) {
                mc.getConnection().sendCommand("pc No " + preName + "!");
                KuudraHelperMod.LOGGER.info("[NoPreAnnounce] Sent: No {}!", preName);
            }
            if (!hasSecondary) {
                mc.getConnection().sendCommand("pc No " + secondaryName + "!");
                KuudraHelperMod.LOGGER.info("[NoPreAnnounce] Sent: No {}!", secondaryName);
            }
        }
    }

    private static boolean spotFound(Minecraft mc, Vec3 target, double range) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Giant giant)) continue;
            if (giant.getY() >= 67.0) continue;
            double angle = (giant.getYRot() + 130.0) * Math.PI / 180.0;
            Vec3 crate = new Vec3(
                    giant.getX() + 0.5 + 3.7 * Math.cos(angle),
                    75.0,
                    giant.getZ() + 0.5 + 3.7 * Math.sin(angle)
            );
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
