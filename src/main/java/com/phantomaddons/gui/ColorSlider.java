package com.phantomaddons.gui;

import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorSlider extends Slider {
    final int shift, tint;
    public ColorSlider(String n, int shift, int tint,
                Supplier<Integer> colorGet, Consumer<Integer> colorSet) {
        super(n,
                () -> ((colorGet.get() >> shift) & 0xFF) / 255f,
                v -> {
                    int c = colorGet.get() & ~(0xFF << shift);
                    colorSet.accept(c | (Mth.clamp((int)(v * 255), 0, 255) << shift));
                }, "");
        this.shift = shift; this.tint = tint;
    }
    @Override int labelColor() { return tint | 0xFF000000; }
    @Override int trackColor() { return tint | 0xFF000000; }
    @Override String displayValue() { return String.valueOf((int)(get.get() * 255)); }
    @Override String editableText()  { return displayValue(); }
    @Override void applyEdited(String text) {
        try {
            int v = Mth.clamp(Integer.parseInt(text), 0, 255);
            set.accept(v / 255f);
        } catch (NumberFormatException ignored) {}
    }
}
