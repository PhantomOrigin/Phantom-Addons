package com.kuudrahelper.features.customisation.water;

import com.kuudrahelper.KuudraConfig;

public final class WaterColorPreviewHelper {

    private WaterColorPreviewHelper() {}

    public static int computePreviewColor() {
        float opacity = KuudraConfig.getWaterOpacity();
        int alpha = Math.max(0, Math.min(255, Math.round(opacity * 255f)));

        if (KuudraConfig.isWaterColorOverride()) {
            int rgb = KuudraConfig.getWaterColor() & 0x00FFFFFF;
            return (alpha << 24) | rgb;
        }

        boolean asLava = KuudraConfig.isWaterAsLava();
        int vanillaRgb = asLava ? 0xFF5A01 : 0x3F76E4;
        return (alpha << 24) | vanillaRgb;
    }
}
