package com.phantomaddons.gui;

import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class RangeSlider extends Slider {
    final float min, max;
    final String fmt;
    public RangeSlider(String n, float min, float max, String fmt,
                Supplier<Float> getRaw, Consumer<Float> setRaw) {
        super(n,
                () -> (getRaw.get() - min) / (max - min),
                v  -> setRaw.accept(v * (max - min) + min),
                "");
        this.min = min; this.max = max; this.fmt = fmt;
    }
    private float  lastRaw = Float.NaN;
    private String lastFormatted;
    @Override String displayValue() {
        float raw = get.get() * (max - min) + min;
        if (Float.floatToIntBits(raw) != Float.floatToIntBits(lastRaw)) {
            lastFormatted = String.format(fmt, raw);
            lastRaw = raw;
        }
        return lastFormatted;
    }
    @Override String editableText() { return stripToNumber(displayValue()); }
    @Override void applyEdited(String text) {
        try {
            float raw = Mth.clamp(Float.parseFloat(text), min, max);
            set.accept((raw - min) / (max - min));
        } catch (NumberFormatException ignored) {}
    }
    private static String stripToNumber(String formatted) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < formatted.length(); i++) {
            char c = formatted.charAt(i);
            if (Character.isDigit(c) || c == '.' || c == '-') sb.append(c);
        }
        return sb.toString();
    }
}
