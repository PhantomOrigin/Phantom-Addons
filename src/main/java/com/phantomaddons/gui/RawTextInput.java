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

public class RawTextInput extends Feature {
    final Supplier<String> get; final Consumer<String> set;
    String  draft   = null;
    boolean focused = false;
    int     cursorPos = 0;
    int     selAnchor = 0;
    boolean masked  = false;
    private int lastFx, lastFy, lastFw, lastFh, lastStart;
    private String lastShown = "";
    public RawTextInput(String n, Supplier<String> get, Consumer<String> set) {
        super(n); this.get = get; this.set = set;
    }
    public RawTextInput withMasked() { this.masked = true; return this; }
    String toDisplay(String stored) { return stored == null ? "" : stored; }
    String toStored(String edited)  { return edited; }
    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        drawLabel(ctx, s, x, y, C_TEXT);
        String display = draft != null ? draft : toDisplay(get.get());
        int labelW = s.font().width(name);
        int fx = x + 8 + labelW + 8, fy = y + 3, fw = w - fx + x - 8, fh = ROW_H - 6;
        if (fw < 30) fw = 30;
        lastFx = fx; lastFy = fy; lastFw = fw; lastFh = fh;
        boolean hov = fieldHovered(fx, fy, fw, fh, mx, my);
        int bg = focused ? C_FIELD_FOCUS : hov ? C_FIELD_HOVER : C_FIELD_BG;
        insetFill(ctx, fx, fy, fx + fw, fy + fh, bg, CONTROL_RADIUS);
        cursorPos = Math.max(0, Math.min(cursorPos, display.length()));
        selAnchor = Math.max(0, Math.min(selAnchor, display.length()));
        int maxChars = (fw - 8) / Math.max(1, s.font().width("a"));
        int start = display.length() <= maxChars ? 0
                : focused ? Math.max(0, Math.min(display.length() - maxChars, cursorPos - maxChars / 2))
                          : display.length() - maxChars;
        String shown = display.substring(start, Math.min(display.length(), start + maxChars));
        lastStart = start; lastShown = shown;
        if (focused) drawSelection(ctx, s.font(), shown, start, selAnchor, cursorPos, fx + 4, fy, fh, withAlpha(C_ACCENT, 90));
        String shownForDraw = (masked && !focused) ? "*".repeat(shown.length()) : shown;
        drawText(ctx, s.font(), Component.literal(shownForDraw), fx + 4, fy + (fh - s.font().lineHeight) / 2 + 1, C_TEXT);
        if (focused) {
            int visibleCursor = Math.max(start, Math.min(cursorPos, start + shown.length()));
            int caretX = fx + 4 + s.font().width(display.substring(start, visibleCursor));
            drawCaret(ctx, caretX, fy + 2, fh - 4, C_TEXT);
        }
    }
    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        if (fieldHovered(lastFx, lastFy, lastFw, lastFh, mx, my)) {
            focused = true;
            draft = toDisplay(get.get());
            cursorPos = lastStart + clickToCursor(Minecraft.getInstance().font, lastShown, lastFx + 4, mx);
            if (!isShiftDown()) selAnchor = cursorPos;
            return true;
        }
        if (focused) { commit(); focused = false; }
        return false;
    }
    @Override boolean onDrag(double mx, double my, int x, int y, int w) {
        if (!focused) return false;
        cursorPos = lastStart + clickToCursor(Minecraft.getInstance().font, lastShown, lastFx + 4, mx);
        return true;
    }
    @Override void onKey(int key) {
        if (!focused) return;
        boolean ctrl = isCtrlDown(), shift = isShiftDown();
        if (key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) { commit(); focused = false; return; }
        if (ctrl && key == InputConstants.KEY_A) { selAnchor = 0; cursorPos = draft == null ? 0 : draft.length(); return; } // Ctrl+A
        if (ctrl && key == InputConstants.KEY_C) { copySelection(); return; } // Ctrl+C
        if (ctrl && key == InputConstants.KEY_X) { copySelection(); deleteSelection(); return; } // Ctrl+X
        if (ctrl && key == InputConstants.KEY_V) { pasteAtCursor(); return; } // Ctrl+V
        if (key == InputConstants.KEY_BACKSPACE) { // Backspace
            if (selAnchor != cursorPos) deleteSelection();
            else if (draft != null) { draft = deleteBefore(draft, cursorPos); cursorPos = Math.max(0, cursorPos - 1); selAnchor = cursorPos; }
            return;
        }
        if (key == InputConstants.KEY_DELETE) { // Delete
            if (selAnchor != cursorPos) deleteSelection();
            else if (draft != null) draft = deleteAfter(draft, cursorPos);
            selAnchor = cursorPos;
            return;
        }
        if (key == InputConstants.KEY_LEFT) { // Left
            if (!shift && selAnchor != cursorPos) cursorPos = Math.min(selAnchor, cursorPos);
            else cursorPos = Math.max(0, cursorPos - 1);
            if (!shift) selAnchor = cursorPos;
            return;
        }
        if (key == InputConstants.KEY_RIGHT) { // Right
            int len = draft == null ? 0 : draft.length();
            if (!shift && selAnchor != cursorPos) cursorPos = Math.max(selAnchor, cursorPos);
            else cursorPos = Math.min(len, cursorPos + 1);
            if (!shift) selAnchor = cursorPos;
        }
    }
    @Override void onChar(char c) {
        if (focused) {
            if (draft == null) draft = "";
            if (selAnchor != cursorPos) deleteSelection();
            draft = insertAt(draft, cursorPos, c);
            cursorPos++;
            selAnchor = cursorPos;
        }
    }
    private void deleteSelection() {
        if (draft == null || selAnchor == cursorPos) return;
        draft = deleteRange(draft, selAnchor, cursorPos);
        cursorPos = Math.min(selAnchor, cursorPos);
        selAnchor = cursorPos;
    }
    private void copySelection() {
        if (draft == null || selAnchor == cursorPos) return;
        int lo = Math.min(selAnchor, cursorPos), hi = Math.max(selAnchor, cursorPos);
        copyToClipboard(draft.substring(lo, hi));
    }
    private void pasteAtCursor() {
        if (draft == null) draft = "";
        if (selAnchor != cursorPos) deleteSelection();
        String paste = pasteFromClipboard();
        draft = insertRange(draft, cursorPos, paste);
        cursorPos += paste.length();
        selAnchor = cursorPos;
    }
    @Override boolean isCapturing() { return focused; }
    @Override void cancel() { if (focused) { commit(); focused = false; } }
    void commit() {
        if (draft != null) set.accept(toStored(draft));
        draft = null;
    }
}
