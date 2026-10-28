package com.phantomaddons.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

import static com.phantomaddons.gui.Theme.*;
import static com.phantomaddons.gui.GuiDraw.*;

public class Cycle extends Feature {
    final Supplier<String> label; final Runnable cycle;
    public Cycle(String n, Supplier<String> l, Runnable c) {
        super(n); this.label = l; this.cycle = c;
    }
    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        drawLabel(ctx, s, x, y, C_TEXT);
        String val = label.get();
        int bw = s.font().width(val) + 18, bh = ROW_H - 6;
        int bx = x + w - bw - 8, by = y + 3;
        boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        raisedFill(ctx, bx, by, bx + bw, by + bh, hov ? C_CYCLE_HOVER : C_CYCLE_BG, CONTROL_RADIUS);
        drawCenteredText(ctx, s.font(), Component.literal(val),
                bx + bw / 2, by + (bh - s.font().lineHeight) / 2 + 1, C_ACCENT);
    }
    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        if (mx >= x && mx <= x + w && my >= y && my <= y + ROW_H) {
            cycle.run(); return true;
        }
        return false;
    }
}
