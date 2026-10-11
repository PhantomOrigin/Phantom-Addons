package com.phantomaddons.gui;

import com.phantomaddons.PhantomConfig;

public final class Theme {

    public static int C_BG, C_SIDEBAR, C_HEADER, C_BORDER, C_ACCENT, C_TAB_ACTIVE, C_TAB_HOVER,
            C_TEXT, C_TEXT_DIM, C_ON, C_OFF, C_SLIDER_BG, C_SLIDER_FG, C_SLIDER_GR, C_GROUP_BG,
            C_GROUP_BAR, C_CYCLE_BG, C_CYCLE_HOVER, C_FIELD_BG, C_FIELD_HOVER, C_FIELD_FOCUS,
            C_ERROR, C_INFO;

    public static boolean darkMode = true;

    static final int CONTENT_ALPHA_TRANS = 130; // ~51% opaque
    static final int SIDEBAR_ALPHA_TRANS = 130; // ~51% opaque
    static final int HEADER_ALPHA_TRANS  = 165; // ~65% opaque

    private Theme() {}

    public static void apply(PhantomConfig.UiTheme theme) {
        darkMode = theme == PhantomConfig.UiTheme.DARK;
        switch (theme) {
            case DARK -> {
                C_BG          = 0xFF15151A;
                C_SIDEBAR     = 0xFF101014;
                C_HEADER      = 0xFF1B1B1F;
                C_BORDER      = 0xFF1E2233;
                C_ACCENT      = 0xFFFFAA00;
                C_TAB_ACTIVE  = 0xFF1A1F2E;
                C_TAB_HOVER   = 0xFF131720;
                C_TEXT        = 0xFFD4D8E8;
                C_TEXT_DIM    = 0xFF6B7399;
                C_ON          = 0xFF44BB77;
                C_OFF         = 0xFF2A3345;
                C_SLIDER_BG   = 0xFF1A1F2E;
                C_SLIDER_FG   = 0xFF2244AA;
                C_SLIDER_GR   = 0xFF4477DD;
                C_GROUP_BG    = 0x14FFFFFF;
                C_GROUP_BAR   = 0x55FFAA00;
                C_CYCLE_BG    = 0xFF151E33;
                C_CYCLE_HOVER = 0xFF223366;
                C_FIELD_BG    = 0xFF0F1218;
                C_FIELD_HOVER = 0xFF17202E;
                C_FIELD_FOCUS = 0xFF1A2A44;
                C_ERROR       = 0xFFCC4444;
                C_INFO        = 0xFF44AAFF;
            }
            case LIGHT -> {
                C_BG          = 0xFFF7F2E5;
                C_SIDEBAR     = 0xFFEEE6D0;
                C_HEADER      = 0xFFE1D3A4;
                C_BORDER      = 0xFFC7B98A;
                C_ACCENT      = 0xFFB87700;
                C_TAB_ACTIVE  = 0xFFEEDCA0;
                C_TAB_HOVER   = 0xFFE2D5AA;
                C_TEXT        = 0xFF201B0F;
                C_TEXT_DIM    = 0xFF6C5F3C;
                C_ON          = 0xFF357F49;
                C_OFF         = 0xFFC7B98A;
                C_SLIDER_BG   = 0xFFDBD0A8;
                C_SLIDER_FG   = 0xFFB87700;
                C_SLIDER_GR   = 0xFFCB9A33;
                C_GROUP_BG    = 0x18000000;
                C_GROUP_BAR   = 0x66B87700;
                C_CYCLE_BG    = 0xFFDBD0A8;
                C_CYCLE_HOVER = 0xFFC9BB86;
                C_FIELD_BG    = 0xFFE3D8B4;
                C_FIELD_HOVER = 0xFFEAD3A0;
                C_FIELD_FOCUS = 0xFFF2E3AC;
                C_ERROR       = 0xFF992222;
                C_INFO        = 0xFF1F5FA8;
            }
        }

        boolean transparent = PhantomConfig.isUiTransparencyEnabled();
        C_BG      = withAlpha(C_BG, transparent ? CONTENT_ALPHA_TRANS : 255);
        C_SIDEBAR = withAlpha(C_SIDEBAR, transparent ? SIDEBAR_ALPHA_TRANS : 255);
        C_HEADER  = withAlpha(C_HEADER, transparent ? HEADER_ALPHA_TRANS : 255);
    }

    public static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    public static int overlay(int alpha) {
        return (alpha << 24) | (darkMode ? 0xFFFFFF : 0x000000);
    }

    public static int dangerBg(boolean hover) {
        if (darkMode) return hover ? 0xFF4A1515 : 0xFF2A0F0F;
        return hover ? 0xFFE8B4B4 : 0xFFF2D6D6;
    }

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int ra = Math.round(aa + (ba - aa) * t);
        int rr = Math.round(ar + (br - ar) * t);
        int rg = Math.round(ag + (bg - ag) * t);
        int rb = Math.round(ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    public static int shade(int argb, int delta) {
        int a = (argb >>> 24) & 0xFF;
        int r = clampByte(((argb >> 16) & 0xFF) + delta);
        int g = clampByte(((argb >> 8) & 0xFF) + delta);
        int b = clampByte((argb & 0xFF) + delta);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clampByte(int v) { return Math.max(0, Math.min(255, v)); }
}
