package com.phantomaddons.features.pv;

import com.phantomaddons.gui.Theme;

final class PvTheme {
    private PvTheme() {}
    static int text()   { return Theme.C_TEXT; }
    static int dim()    { return Theme.C_TEXT_DIM; }
    static int accent() { return Theme.C_ACCENT; }
    static int on()     { return Theme.C_ON; }
    static int error()  { return Theme.C_ERROR; }
    static int barBg()  { return Theme.C_SLIDER_BG; }
    static int barFg()  { return Theme.C_SLIDER_GR; }
    static int overlay(int a) { return Theme.overlay(a); }
}
