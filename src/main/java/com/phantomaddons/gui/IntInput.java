package com.phantomaddons.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.phantomaddons.gui.Theme.*;
import static com.phantomaddons.gui.GuiDraw.*;
import static com.phantomaddons.gui.TextEditing.*;

public class IntInput extends Feature {
    final Supplier<Integer> get; final Consumer<Integer> set;
    String  draft   = null;
    boolean focused = false;
    int     cursorPos = 0;
    int     fieldW = 50;
    public IntInput(String n, Supplier<Integer> get, Consumer<Integer> set) {
        super(n); this.get = get; this.set = set;
    }
    String displayValue() { return draft != null ? draft : String.valueOf(get.get()); }
    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        drawLabel(ctx, s, x, y, C_TEXT);
        String display = displayValue();
        int fw = fieldW, fx = x + w - fw - 8, fy = y + 3, fh = ROW_H - 6;
        boolean hov = fieldHovered(fx, fy, fw, fh, mx, my);
        int bg = focused ? C_FIELD_FOCUS : hov ? C_FIELD_HOVER : C_FIELD_BG;
        insetFill(ctx, fx, fy, fx + fw, fy + fh, bg, CONTROL_RADIUS);
        int textX = fx + (fw - s.font().width(display)) / 2;
        drawText(ctx, s.font(), Component.literal(display), textX, fy + (fh - s.font().lineHeight) / 2 + 1, C_TEXT);
        if (focused) {
            cursorPos = Math.max(0, Math.min(cursorPos, display.length()));
            int caretX = textX + s.font().width(display.substring(0, cursorPos));
            drawCaret(ctx, caretX, fy + 2, fh - 4, C_TEXT);
        }
    }
    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        int fw = fieldW, fx = x + w - fw - 8, fy = y + 3, fh = ROW_H - 6;
        if (fieldHovered(fx, fy, fw, fh, mx, my)) {
            String display = displayValue();
            var font = Minecraft.getInstance().font;
            int textX = fx + (fw - font.width(display)) / 2;
            focused = true;
            draft = display;
            cursorPos = clickToCursor(font, display, textX, mx);
            return true;
        }
        if (focused) { commit(); focused = false; }
        return false;
    }
    @Override void onKey(int key) {
        if (!focused) return;
        if (key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) { commit(); focused = false; } // Esc/Enter save
        else if (key == InputConstants.KEY_BACKSPACE && draft != null) { draft = deleteBefore(draft, cursorPos); cursorPos = Math.max(0, cursorPos - 1); }
        else if (key == InputConstants.KEY_DELETE && draft != null) { draft = deleteAfter(draft, cursorPos); }
        else if (key == InputConstants.KEY_LEFT) cursorPos = Math.max(0, cursorPos - 1);       // Left
        else if (key == InputConstants.KEY_RIGHT) cursorPos = Math.min(draft == null ? 0 : draft.length(), cursorPos + 1); // Right
    }
    @Override void onChar(char c) {
        if (focused && Character.isDigit(c)) {
            if (draft == null) draft = "";
            draft = insertAt(draft, cursorPos, c);
            cursorPos++;
        }
    }
    @Override boolean isCapturing() { return focused; }
    @Override void cancel() { if (focused) { commit(); focused = false; } }
    void commit() {
        if (draft != null && !draft.isEmpty()) {
            try { set.accept(Integer.parseInt(draft)); } catch (NumberFormatException ignored) {}
        }
        draft = null;
    }
}
