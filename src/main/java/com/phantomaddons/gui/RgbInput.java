package com.phantomaddons.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.phantomaddons.gui.Theme.*;
import static com.phantomaddons.gui.GuiDraw.*;

public class RgbInput extends Feature {
    final Supplier<Integer> get; final Consumer<Integer> set;
    int    focus = 0;   // 0 none, 1 R, 2 G, 3 B
    String draft = null;
    static final int FW = 30;
    public RgbInput(String n, Supplier<Integer> get, Consumer<Integer> set) {
        super(n); this.get = get; this.set = set;
    }

    private int comp(int idx) {
        int c = get.get() & 0xFFFFFF;
        return idx == 0 ? (c >> 16) & 0xFF : idx == 1 ? (c >> 8) & 0xFF : c & 0xFF;
    }
    private int fx(int x, int w, int idx) {
        int b = x + w - 8 - FW;
        int g = b - 4 - FW;
        int r = g - 4 - FW;
        return idx == 0 ? r : idx == 1 ? g : b;
    }

    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        drawLabel(ctx, s, x, y, C_TEXT);
        int swX = fx(x, w, 0) - 16, swY = y + (ROW_H - 12) / 2;
        ctx.fill(swX, swY, swX + 12, swY + 12, 0xFF000000 | (get.get() & 0xFFFFFF));
        for (int i = 0; i < 3; i++) {
            int fxv = fx(x, w, i), fy = y + 3, fh = ROW_H - 6;
            boolean foc = focus == i + 1;
            insetFill(ctx, fxv, fy, fxv + FW, fy + fh, foc ? C_FIELD_FOCUS : C_FIELD_BG, CONTROL_RADIUS);
            String disp = foc ? (draft == null ? "" : draft) : String.valueOf(comp(i));
            drawCenteredText(ctx, s.font(), Component.literal(disp),
                    fxv + FW / 2, fy + (fh - s.font().lineHeight) / 2 + 1, C_TEXT);
        }
    }

    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        int fy = y + 3, fh = ROW_H - 6;
        for (int i = 0; i < 3; i++) {
            int fxv = fx(x, w, i);
            if (mx >= fxv && mx <= fxv + FW && my >= fy && my <= fy + fh) {
                commit(); focus = i + 1; draft = String.valueOf(comp(i)); return true;
            }
        }
        commit();
        return false;
    }

    @Override void onKey(int key) {
        if (focus == 0) return;
        if (key == InputConstants.KEY_ESCAPE) { commit(); }                         // Esc saves current value
        else if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) { commit(); }
        else if (key == InputConstants.KEY_TAB) { int next = focus % 3 + 1; commit(); focus = next; draft = String.valueOf(comp(next - 1)); }
        else if (key == InputConstants.KEY_BACKSPACE && draft != null && !draft.isEmpty())
            draft = draft.substring(0, draft.length() - 1);
    }

    @Override void onChar(char c) {
        if (focus != 0 && Character.isDigit(c) && (draft == null || draft.length() < 3)) {
            if (draft == null) draft = ""; draft += c;
        }
    }
    @Override boolean isCapturing() { return focus != 0; }
    @Override void cancel() { commit(); }

    private void commit() {
        if (focus != 0 && draft != null && !draft.isEmpty()) {
            try {
                int v = Mth.clamp(Integer.parseInt(draft), 0, 255);
                int c = get.get() & 0xFFFFFF;
                int shift = (focus - 1) == 0 ? 16 : (focus - 1) == 1 ? 8 : 0;
                c = (c & ~(0xFF << shift)) | (v << shift);
                set.accept(c);
            } catch (NumberFormatException ignored) {}
        }
        draft = null; focus = 0;
    }
}
