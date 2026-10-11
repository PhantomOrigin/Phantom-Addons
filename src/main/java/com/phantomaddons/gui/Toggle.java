package com.phantomaddons.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.phantomaddons.gui.Theme.*;
import static com.phantomaddons.gui.GuiDraw.*;

public class Toggle extends Feature {
    final Supplier<Boolean> get; final Consumer<Boolean> set;
    private float knobAnim = -1f; // -1 = not yet initialised, snaps instead of animating in
    public Toggle(String n, Supplier<Boolean> get, Consumer<Boolean> set) {
        super(n); this.get = get; this.set = set;
    }
    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        drawLabel(ctx, s, x, y, C_TEXT);
        float target = get.get() ? 1f : 0f;
        if (knobAnim < 0f) knobAnim = target;
        else knobAnim += (target - knobAnim) * Math.min(1f, s.frameDelta * TOGGLE_ANIM_SPEED);
        drawSwitch(ctx, x, y, w, knobAnim);
    }
    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        if (switchHit(mx, my, x, y, w)) { set.accept(!get.get()); return true; }
        return false;
    }
}
