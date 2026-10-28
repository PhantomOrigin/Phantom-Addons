package com.phantomaddons.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.phantomaddons.gui.Theme.*;
import static com.phantomaddons.gui.GuiDraw.*;

public class Slider extends Feature {
    final Supplier<Float> get; final Consumer<Float> set;
    final String unit;
    boolean drag    = false;
    boolean editing = false;
    String  draft   = null;
    int boxX, boxY, boxW, boxH;
    public Slider(String n, Supplier<Float> get, Consumer<Float> set, String unit) {
        super(n); this.get = get; this.set = set; this.unit = unit;
    }
    String displayValue()  { return (int)(get.get() * 100) + unit; }
    String editableText()  { return String.valueOf((int)(get.get() * 100)); }
    void   applyEdited(String text) {
        try {
            float pct = Float.parseFloat(text);
            set.accept(Mth.clamp(pct / 100f, 0f, 1f));
        } catch (NumberFormatException ignored) {}
    }
    int labelColor() { return C_TEXT; }
    int trackColor() { return C_ON; }
    private static final int TRACK_W_MAX = 78, TRACK_W_MIN = 34, BOX_W = 44, KNOB_R = 5, INNER_R = 3;
    int trackX, trackW = TRACK_W_MAX;
    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        drawLabel(ctx, s, x, y, labelColor());
        int labelW = s.font().width(nameLabel);
        trackW = Mth.clamp(w - BOX_W - labelW - 40, TRACK_W_MIN, TRACK_W_MAX);
        trackX = x + w - trackW - 8;
        renderValueBox(ctx, s, x, y, w);
        drawTrack(ctx, x, y, w, get.get(), trackColor());
    }
    void renderValueBox(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w) {
        String shown = editing ? (draft == null ? "" : draft) : displayValue();
        int bw = BOX_W, bh = 12;
        int bx = trackX - 8 - bw, by = y + (ROW_H - bh) / 2;
        boxX = bx; boxY = by; boxW = bw; boxH = bh;
        if (editing) {
            insetFill(ctx, bx, by, bx + bw, by + bh, C_FIELD_FOCUS, CONTROL_RADIUS);
        }
        drawText(ctx, s.font(), Component.literal(shown),
                bx + bw - s.font().width(shown) - 3,
                y + (ROW_H - s.font().lineHeight) / 2 + 1, editing ? C_TEXT : C_TEXT_DIM);
    }
    void drawTrack(GuiGraphicsExtractor ctx, int x, int y, int w, float v, int fill) {
        int sw = trackW, sx = trackX, cy = y + ROW_H / 2;
        int trackBg = darkMode ? 0xFF4A4E57 : 0xFFB9B3A2;
        roundedFill(ctx, sx, cy - 2, sx + sw, cy + 2, trackBg, 2);
        int kx = sx + KNOB_R + Math.round(Mth.clamp(v, 0f, 1f) * (sw - 2 * KNOB_R));
        roundedFill(ctx, sx, cy - 2, kx, cy + 2, fill, 2);
        roundedFill(ctx, kx - KNOB_R, cy - KNOB_R, kx + KNOB_R, cy + KNOB_R, fill, KNOB_R);
        roundedFill(ctx, kx - INNER_R, cy - INNER_R, kx + INNER_R, cy + INNER_R, 0xFFFFFFFF, INNER_R);
    }
    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        if (mx >= boxX && mx <= boxX + boxW && my >= boxY && my <= boxY + boxH) {
            if (!editing) { editing = true; draft = editableText(); }
            return true;
        }
        int sw = trackW, sx = trackX, sy = y + ROW_H / 2 - 7;
        if (mx >= sx - 4 && mx <= sx + sw + 4 && my >= sy && my <= sy + 14) {
            if (editing) { commit(); editing = false; }
            drag = true; apply(mx, sx, sw); return true;
        }
        if (editing) { commit(); editing = false; }
        return false;
    }
    @Override boolean onDrag(double mx, double my, int x, int y, int w) {
        if (!drag) return false; apply(mx, trackX, trackW); return true;
    }
    @Override void onUp() { drag = false; }
    void apply(double mx, int sx, int sw) {
        set.accept((float) Mth.clamp((mx - sx - KNOB_R) / (double) (sw - 2 * KNOB_R), 0.0, 1.0));
    }
    @Override void onKey(int key) {
        if (!editing) return;
        if (key == InputConstants.KEY_ESCAPE) { commit(); editing = false; }        // Esc saves current value
        else if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) { commit(); editing = false; }
        else if (key == InputConstants.KEY_BACKSPACE && draft != null && !draft.isEmpty())
            draft = draft.substring(0, draft.length() - 1);
    }
    @Override void onChar(char c) {
        if (!editing) return;
        if (Character.isDigit(c) || c == '.' || c == '-') {
            if (draft == null) draft = "";
            draft += c;
        }
    }
    @Override boolean isCapturing() { return editing; }
    @Override void cancel() { if (editing) { commit(); editing = false; } }
    void commit() {
        if (draft != null && !draft.isEmpty() && !draft.equals("-")) applyEdited(draft);
        draft = null;
    }
}
