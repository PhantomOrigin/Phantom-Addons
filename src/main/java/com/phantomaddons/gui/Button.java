package com.phantomaddons.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import static com.phantomaddons.gui.Theme.*;
import static com.phantomaddons.gui.GuiDraw.*;

public class Button extends Feature {
    final String label; final Runnable action;
    public Button(String n, String label, Runnable action) {
        super(n); this.label = label; this.action = action;
    }
    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        drawLabel(ctx, s, x, y, C_TEXT);
        int lw = s.font().width(label);
        int bw = lw + 18, bh = ROW_H - 6, bx = x + w - bw - 8, by = y + 3;
        boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        roundedFill(ctx, bx, by, bx + bw, by + bh, hov ? C_CYCLE_HOVER : C_CYCLE_BG, CONTROL_RADIUS);
        drawCenteredText(ctx, s.font(), Component.literal(label),
                bx + bw / 2, by + (bh - s.font().lineHeight) / 2, C_ACCENT);
    }
    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        int bw = Minecraft.getInstance().font.width(label) + 18, bh = ROW_H - 6;
        int bx = x + w - bw - 8, by = y + 3;
        if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) { action.run(); return true; }
        return false;
    }
}
