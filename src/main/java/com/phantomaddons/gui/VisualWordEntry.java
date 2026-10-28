package com.phantomaddons.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import static com.phantomaddons.gui.Theme.*;
import static com.phantomaddons.gui.GuiDraw.*;

public class VisualWordEntry extends Feature {
    final int idx;
    int     focus = 0;       // 0 none, 1 input, 2 replacement
    String  draft = null;
    private final Runnable onRemoved;
    public VisualWordEntry(int idx, Runnable onRemoved) { super("Visual Word"); this.idx = idx; this.onRemoved = onRemoved; }

    private com.phantomaddons.features.customisation.VisualWords.Rule rule() {
        var rules = com.phantomaddons.features.customisation.VisualWords.getRules();
        return idx >= 0 && idx < rules.size() ? rules.get(idx) : null;
    }

    // geometry
    private int removeX(int x, int w) { return x + w - 18; }
    private int inputX(int x)         { return x + 6; }
    private int fieldW(int x, int w)  { return (removeX(x, w) - inputX(x) - 14) / 2; }
    private int arrowX(int x, int w)  { return inputX(x) + fieldW(x, w) + 2; }
    private int replX(int x, int w)   { return arrowX(x, w) + 10; }

    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        var r = rule();
        if (r == null) return;
        int fy = y + 4, fh = ROW_H - 8;
        int fw = fieldW(x, w);

        String inText  = focus == 1 ? draft : r.input;
        String repText = focus == 2 ? draft : r.replacement;

        drawField(ctx, s, inputX(x), fy, fw, fh, inText, focus == 1, "find");
        drawCenteredText(ctx, s.font(), Component.literal("→"),
                arrowX(x, w) + 4, fy + (fh - s.font().lineHeight) / 2 + 1, C_TEXT_DIM);
        drawField(ctx, s, replX(x, w), fy, fw, fh, repText, focus == 2, "replace");

        int rx = removeX(x, w);
        boolean hov = mx >= rx && mx <= rx + 14 && my >= fy && my <= fy + fh;
        raisedFill(ctx, rx, fy, rx + 14, fy + fh, dangerBg(hov), CONTROL_RADIUS);
        drawCenteredText(ctx, s.font(), Component.literal("✕"),
                rx + 7, fy + (fh - s.font().lineHeight) / 2 + 1, C_ERROR);
    }

    private void drawField(GuiGraphicsExtractor ctx, PhantomScreen s, int fx, int fy, int fw, int fh,
                           String text, boolean foc, String hint) {
        insetFill(ctx, fx, fy, fx + fw, fy + fh, foc ? C_FIELD_FOCUS : C_FIELD_BG, CONTROL_RADIUS);
        boolean placeholder = (text == null || text.isEmpty()) && !foc;
        String disp = placeholder ? hint : (text == null ? "" : text);
        drawText(ctx, s.font(), Component.literal(disp),
                fx + 4, fy + (fh - s.font().lineHeight) / 2 + 1, placeholder ? C_TEXT_DIM : C_TEXT);
    }

    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        var r = rule();
        if (r == null) return false;
        int fy = y + 4, fh = ROW_H - 8, fw = fieldW(x, w);
        int rx = removeX(x, w);
        if (mx >= rx && mx <= rx + 14 && my >= fy && my <= fy + fh) {
            com.phantomaddons.features.customisation.VisualWords.removeRule(idx);
            onRemoved.run();
            return true;
        }
        if (my >= fy && my <= fy + fh) {
            if (mx >= inputX(x) && mx <= inputX(x) + fw) { focusOn(1, r.input); return true; }
            if (mx >= replX(x, w) && mx <= replX(x, w) + fw) { focusOn(2, r.replacement); return true; }
        }
        commit();
        return false;
    }

    private void focusOn(int which, String current) { commit(); focus = which; draft = current == null ? "" : current; }

    @Override void onKey(int key) {
        if (focus == 0) return;
        if (key == InputConstants.KEY_ESCAPE) { commit(); }                         // Esc saves current value
        else if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) { commit(); }
        else if (key == InputConstants.KEY_TAB) { // Tab → jump to the other field
            var r = rule();
            if (r != null) { int next = focus == 1 ? 2 : 1; commit(); focusOn(next, next == 1 ? r.input : r.replacement); }
        }
        else if (key == InputConstants.KEY_BACKSPACE && draft != null && !draft.isEmpty())
            draft = draft.substring(0, draft.length() - 1);
    }

    @Override void onChar(char c) { if (focus != 0 && c >= 32) { if (draft == null) draft = ""; draft += c; } }
    @Override boolean isCapturing() { return focus != 0; }
    @Override void cancel() { commit(); }

    private void commit() {
        var r = rule();
        if (r != null && draft != null && focus != 0) {
            if (focus == 1) r.input = draft; else r.replacement = draft;
            com.phantomaddons.features.customisation.VisualWords.save();
        }
        draft = null; focus = 0;
    }
}
