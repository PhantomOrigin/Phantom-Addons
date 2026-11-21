package com.phantomaddons.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

abstract class Popup extends Feature {
    Popup(String name) { super(name); }

    abstract boolean hasPopup();

    abstract void setArea(int x, int y, int w, int h);

    abstract void renderPopup(GuiGraphicsExtractor ctx, PhantomScreen s, int mx, int my);

    abstract boolean popupClick(double mx, double my);

    abstract boolean popupScroll(double mx, double my, double amount);
}
