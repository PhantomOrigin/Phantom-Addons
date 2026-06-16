package com.kuudrahelper.features.lava;

import com.kuudrahelper.KuudraConfig;

public final class ColorPreviewHelper {

    private ColorPreviewHelper() {}

    public static int computePreviewColor() {
        float opacity = KuudraConfig.getLavaOpacity();
        int alpha = Math.max(0, Math.min(255, Math.round(opacity * 255f)));

        if (KuudraConfig.isLavaColorOverride()) {
            int rgb = KuudraConfig.getLavaColor() & 0x00FFFFFF;
            return (alpha << 24) | rgb;
        }

        boolean asWater = KuudraConfig.isLavaAsWater();
        int vanillaRgb = asWater ? 0x3F76E4 : 0xFF5A01;
        return (alpha << 24) | vanillaRgb;
    }
}