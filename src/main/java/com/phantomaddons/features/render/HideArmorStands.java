package com.phantomaddons.features.render;

import com.phantomaddons.PhantomConfig;

public final class HideArmorStands {

    private static volatile boolean active = false;

    public static void activate()   { active = true;  }
    public static void deactivate() { active = false; }

    private static final double BUILD_X0 = -113, BUILD_X1 = -91, BUILD_Z0 = -116, BUILD_Z1 = -96;
    private static final double R_CANNON_X0 = -132, R_CANNON_X1 = -126, R_CANNON_Z0 = -115, R_CANNON_Z1 = -108;
    private static final double L_CANNON_X0 = -72,  L_CANNON_X1 = -67,  L_CANNON_Z0 = -105, L_CANNON_Z1 = -100;
    private static final double SHOP_X0 = -98, SHOP_X1 = -93, SHOP_Z0 = -132, SHOP_Z1 = -129;

    private HideArmorStands() {}

    public static boolean shouldHide(double x, double z, boolean hasCustomName) {
        if (!active) return false;
        if (hasCustomName) return false;
        if (!PhantomConfig.isHideArmorStandsEnabled()) return false;

        if (PhantomConfig.isHideArmorStandsBuild()
                && inBox(x, z, BUILD_X0, BUILD_X1, BUILD_Z0, BUILD_Z1))   return true;
        if (PhantomConfig.isHideArmorStandsRightCannon()
                && inBox(x, z, R_CANNON_X0, R_CANNON_X1, R_CANNON_Z0, R_CANNON_Z1)) return true;
        if (PhantomConfig.isHideArmorStandsLeftCannon()
                && inBox(x, z, L_CANNON_X0, L_CANNON_X1, L_CANNON_Z0, L_CANNON_Z1)) return true;
        if (PhantomConfig.isHideArmorStandsShop()
                && inBox(x, z, SHOP_X0, SHOP_X1, SHOP_Z0, SHOP_Z1))       return true;

        if (PhantomConfig.isHideArmorStandsOthers()) {
            boolean inAnyNamedArea =
                    inBox(x, z, BUILD_X0,     BUILD_X1,     BUILD_Z0,     BUILD_Z1)     ||
                    inBox(x, z, R_CANNON_X0,  R_CANNON_X1,  R_CANNON_Z0,  R_CANNON_Z1) ||
                    inBox(x, z, L_CANNON_X0,  L_CANNON_X1,  L_CANNON_Z0,  L_CANNON_Z1) ||
                    inBox(x, z, SHOP_X0,      SHOP_X1,      SHOP_Z0,      SHOP_Z1);
            if (!inAnyNamedArea) return true;
        }

        return false;
    }

    private static boolean inBox(double x, double z,
                                 double x0, double x1, double z0, double z1) {
        double minX = Math.min(x0, x1), maxX = Math.max(x0, x1);
        double minZ = Math.min(z0, z1), maxZ = Math.max(z0, z1);
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
}
