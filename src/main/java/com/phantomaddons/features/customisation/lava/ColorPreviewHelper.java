package com.phantomaddons.features.customisation.lava;

import com.phantomaddons.PhantomConfig;

public final class ColorPreviewHelper {

    private ColorPreviewHelper() {}

    public static int computePreviewColor() {
        float opacity = PhantomConfig.getLavaOpacity();
        int alpha = Math.max(0, Math.min(255, Math.round(opacity * 255f)));

        if (PhantomConfig.isLavaColorOverride()) {
            int rgb = PhantomConfig.getLavaColor() & 0x00FFFFFF;
            return (alpha << 24) | rgb;
        }

        boolean asWater = PhantomConfig.isLavaAsWater();
        int vanillaRgb = asWater ? 0x3F76E4 : 0xFF5A01;
        return (alpha << 24) | vanillaRgb;
    }
}