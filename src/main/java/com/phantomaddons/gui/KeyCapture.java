package com.phantomaddons.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import static com.phantomaddons.gui.Theme.*;
import static com.phantomaddons.gui.GuiDraw.*;

public class KeyCapture extends Feature {
    final IntSupplier getKey;
    final IntConsumer setKey;
    boolean capturing = false;
    Integer labelColor = null; // null = default (C_TEXT_DIM)

    public KeyCapture(String n, IntSupplier getKey, IntConsumer setKey) {
        super(n); this.getKey = getKey; this.setKey = setKey;
    }

    public KeyCapture withLabelColor(int color) { this.labelColor = color; return this; }

    public static String nameForCode(int code) {
        if (code <= 0) return "None";
        if (code >= PhantomScreen.MOUSE_OFFSET) return "Mouse " + com.phantomaddons.utils.InputCompat.mouseButtonNumber(code - PhantomScreen.MOUSE_OFFSET);
        return com.phantomaddons.utils.InputCompat.keyName(code);
    }

    private String keyName() { return nameForCode(getKey.getAsInt()); }

    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        drawLabel(ctx, s, x, y, labelColor != null ? labelColor : C_TEXT_DIM);
        String label = capturing ? "Press key or mouse..." : keyName();
        int lw = s.font().width(label);
        int bx = x + w - lw - 16, by = y + 3, bh = ROW_H - 6;
        boolean hov = !capturing && mx >= bx && mx <= bx + lw + 8 && my >= by && my <= by + bh;
        raisedFill(ctx, bx, by, bx + lw + 8, by + bh, capturing ? 0xFF1A3344 : hov ? C_CYCLE_HOVER : C_CYCLE_BG, CONTROL_RADIUS);
        drawText(ctx, s.font(), Component.literal(label),
                bx + 4, by + (bh - s.font().lineHeight) / 2 + 1,
                capturing ? 0xFF88CCFF : C_ACCENT);
    }
    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        int by = y + 3, bh = ROW_H - 6, bx = x + w / 2;
        if (mx >= bx && mx <= x + w && my >= by && my <= by + bh) { capturing = true; return true; }
        return false;
    }
    @Override void onKey(int key) {
        if (!capturing) return;
        if (key == InputConstants.KEY_ESCAPE) { setKey.accept(-1); capturing = false; return; } // Escape = unbind
        setKey.accept(key); capturing = false;
    }
    void onMouseButton(int button) {
        if (!capturing) return;
        setKey.accept(PhantomScreen.MOUSE_OFFSET + button); capturing = false;
    }
    @Override boolean isCapturing() { return capturing; }
    @Override void cancel()         { capturing = false; }
}
