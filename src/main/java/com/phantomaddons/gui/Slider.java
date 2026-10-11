package com.phantomaddons.gui;

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
    int trackColor() { return C_SLIDER_FG; }
    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        drawLabel(ctx, s, x, y, labelColor());
        renderValueBox(ctx, s, x, y, w);
        drawTrack(ctx, x, y, w, get.get(), trackColor());
    }
    void renderValueBox(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w) {
        String shown = editing ? (draft == null ? "" : draft) : displayValue();
        int bw = 44, bh = 12;
        int bx = x + w - 68 - bw, by = y + (ROW_H - bh) / 2;
        boxX = bx; boxY = by; boxW = bw; boxH = bh;
        if (editing) {
            roundedFill(ctx, bx, by, bx + bw, by + bh, C_FIELD_FOCUS, CONTROL_RADIUS);
        }
        drawText(ctx, s.font(), Component.literal(shown),
                bx + bw - s.font().width(shown) - 3,
                y + (ROW_H - s.font().lineHeight) / 2, editing ? C_TEXT : C_TEXT_DIM);
    }
    void drawTrack(GuiGraphicsExtractor ctx, int x, int y, int w, float v, int fill) {
        int sw = 52, sx = x + w - sw - 8, sy = y + ROW_H / 2 - 3;
        ctx.fill(sx, sy, sx + sw, sy + 6, C_SLIDER_BG);
        int filled = (int)(v * sw);
        if (filled > 0) ctx.fill(sx, sy, sx + filled, sy + 6, fill);
        int gx = Mth.clamp(sx + filled - 4, sx, sx + sw - 8);
        ctx.fill(gx, sy - 2, gx + 8, sy + 8, C_SLIDER_GR);
    }
    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        if (mx >= boxX && mx <= boxX + boxW && my >= boxY && my <= boxY + boxH) {
            if (!editing) { editing = true; draft = editableText(); }
            return true;
        }
        int sw = 52, sx = x + w - sw - 8, sy = y + ROW_H / 2 - 5;
        if (mx >= sx - 4 && mx <= sx + sw + 4 && my >= sy && my <= sy + 10) {
            if (editing) { commit(); editing = false; }
            drag = true; apply(mx, sx, sw); return true;
        }
        if (editing) { commit(); editing = false; }
        return false;
    }
    @Override boolean onDrag(double mx, double my, int x, int y, int w) {
        if (!drag) return false; apply(mx, x + w - 52 - 8, 52); return true;
    }
    @Override void onUp() { drag = false; }
    void apply(double mx, int sx, int sw) {
        set.accept((float) Mth.clamp((mx - sx) / sw, 0.0, 1.0));
    }
    @Override void onKey(int key) {
        if (!editing) return;
        if (key == 256) { commit(); editing = false; }        // Esc saves current value
        else if (key == 257 || key == 335) { commit(); editing = false; }
        else if (key == 259 && draft != null && !draft.isEmpty())
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
