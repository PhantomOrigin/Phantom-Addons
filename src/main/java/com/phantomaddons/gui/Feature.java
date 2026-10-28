package com.phantomaddons.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static com.phantomaddons.gui.GuiDraw.*;

public abstract class Feature {
    final String name;
    final String searchName;
    final Component nameLabel;
    String tooltip;
    BooleanSupplier warnWhen;
    String warnText;
    Supplier<Boolean> visible = () -> true;
    Feature(String n) { name = n; searchName = n.toLowerCase(); nameLabel = Component.literal(n); }
    public <T extends Feature> T withTooltip(String t) { this.tooltip = t; return (T) this; }
    public <T extends Feature> T withWarning(BooleanSupplier when, String text) {
        this.warnWhen = when;
        this.warnText = text;
        return (T) this;
    }
    public <T extends Feature> T withVisible(Supplier<Boolean> v) { this.visible = v; return (T) this; }
    boolean warning() { return warnWhen != null && warnWhen.getAsBoolean(); }
    void drawLabel(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int color) {
        drawText(ctx, s.font(), nameLabel, x + 8, y + (ROW_H - s.font().lineHeight) / 2 + 1, color);
        if (warning()) drawWarningMark(ctx, s.font(), x + 8 + s.font().width(nameLabel) + 4, y);
    }
    abstract void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my);
    boolean onDown(double mx, double my, int x, int y, int w) { return false; }
    boolean onDrag(double mx, double my, int x, int y, int w) { return false; }
    void onUp()         {}
    void onKey(int k)   {}
    void onChar(char c) {}
    boolean isCapturing() { return false; }
    void cancel()         {}
}
