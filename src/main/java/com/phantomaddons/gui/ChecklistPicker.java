package com.phantomaddons.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.phantomaddons.gui.GuiDraw.*;
import static com.phantomaddons.gui.TextEditing.*;
import static com.phantomaddons.gui.Theme.*;

abstract class ChecklistPicker<T> extends Popup {
    private static final int FIELD_MAX_W = 150, POPUP_W = 300;
    private static final int SEARCH_H = 16, ITEM_H = 12, MAX_ROWS = 10, MIN_ROWS = 3, PAD = 3, BOX = 7, BULK_W = 28;

    private boolean open;
    private String query = "";
    private List<T> shown = List.of();
    private int listScroll, highlight;

    private int fieldX, fieldY, fieldW, fieldH;
    private int popX, popY, popW, popH, rows;
    private int areaX, areaY, areaW, areaH;
    private int allX, noneX, bulkY;

    ChecklistPicker(String name) { super(name); }

    // ── What a subclass provides ──────────────────────────────────────────────

    abstract List<T> entries();
    abstract String searchText(T entry);
    abstract boolean isOn(T entry);
    abstract void toggle(T entry);
    abstract String label(T entry);
    abstract String summary();
    boolean summaryDim() { return false; }
    String detail(T entry) { return ""; }
    String trailing(T entry) { return ""; }
    String searchHint() { return "Search..."; }
    String emptyText() { return "Nothing found"; }
    boolean hasBulk() { return false; }
    void setAll(boolean on) {}
    // ── Row ───────────────────────────────────────────────────────────────────

    static String fit(Font font, String text, int maxW) {
        if (font.width(text) <= maxW) return text;
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + "...") > maxW) end--;
        return text.substring(0, end) + "...";
    }

    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        drawLabel(ctx, s, x, y, C_TEXT);
        int labelW = s.font().width(nameLabel);
        fieldW = Math.max(60, Math.min(FIELD_MAX_W, w - labelW - 32));
        fieldX = x + w - fieldW - 8;
        fieldY = y + 3;
        fieldH = ROW_H - 6;
        boolean hov = fieldHovered(fieldX, fieldY, fieldW, fieldH, mx, my);
        insetFill(ctx, fieldX, fieldY, fieldX + fieldW, fieldY + fieldH,
                open ? C_FIELD_FOCUS : hov ? C_FIELD_HOVER : C_FIELD_BG, CONTROL_RADIUS);
        drawText(ctx, s.font(), Component.literal(fit(s.font(), summary(), fieldW - 20)),
                fieldX + 4, fieldY + (fieldH - s.font().lineHeight) / 2 + 1, summaryDim() ? C_TEXT_DIM : C_TEXT);
        chevron(ctx, fieldX + fieldW - 11, fieldY + fieldH / 2, open, open || hov ? C_ACCENT : C_TEXT_DIM);
    }

    private static void chevron(GuiGraphicsExtractor ctx, int x, int cy, boolean up, int color) {
        for (int i = 0; i < 4; i++) {
            if (up) ctx.fill(x + i, cy + 1 - i, x + 7 - i, cy + 2 - i, color);
            else    ctx.fill(x + i, cy - 2 + i, x + 7 - i, cy - 1 + i, color);
        }
    }

    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        if (!fieldHovered(fieldX, fieldY, fieldW, fieldH, mx, my)) return false;
        open = true;
        query = "";
        refilter();
        return true;
    }

    // ── Popup ─────────────────────────────────────────────────────────────────

    @Override boolean hasPopup() { return open; }

    @Override void setArea(int x, int y, int w, int h) { areaX = x; areaY = y; areaW = w; areaH = h; }

    private void layoutPopup() {
        popW = Math.min(POPUP_W, areaW - 12);
        popX = Math.max(areaX + 6, Math.min(fieldX + fieldW - popW, areaX + areaW - popW - 6));
        int below = areaY + areaH - (fieldY + fieldH + 2) - 4;
        int above = fieldY - 2 - areaY - 4;
        int wanted = SEARCH_H + PAD * 3 + MAX_ROWS * ITEM_H;
        boolean useBelow = below >= wanted || below >= above;
        int avail = Math.max(useBelow ? below : above, SEARCH_H + PAD * 3 + MIN_ROWS * ITEM_H);
        rows = Math.max(MIN_ROWS, Math.min(MAX_ROWS, (avail - SEARCH_H - PAD * 3) / ITEM_H));
        popH = SEARCH_H + PAD * 3 + rows * ITEM_H;
        popY = useBelow ? fieldY + fieldH + 2 : fieldY - 2 - popH;
        popY = Math.max(areaY + 2, Math.min(popY, areaY + areaH - popH - 2));
    }

    private void bulkButton(GuiGraphicsExtractor ctx, Font font, int x, String text, int mx, int my) {
        boolean hov = mx >= x && mx < x + BULK_W && my >= bulkY && my < bulkY + SEARCH_H;
        insetFill(ctx, x, bulkY, x + BULK_W, bulkY + SEARCH_H, hov ? C_FIELD_HOVER : C_FIELD_BG, CONTROL_RADIUS);
        drawCenteredText(ctx, font, Component.literal(text), x + BULK_W / 2,
                bulkY + (SEARCH_H - font.lineHeight) / 2 + 1, hov ? C_ACCENT : C_TEXT);
    }

    @Override void renderPopup(GuiGraphicsExtractor ctx, PhantomScreen s, int mx, int my) {
        if (!open) return;
        Font font = s.font();
        layoutPopup();
        int maxScroll = Math.max(0, shown.size() - rows);
        listScroll = Math.max(0, Math.min(listScroll, maxScroll));

        roundedFill(ctx, popX - 1, popY - 1, popX + popW + 1, popY + popH + 1, C_BORDER, 5);
        roundedFill(ctx, popX, popY, popX + popW, popY + popH, darkMode ? 0xFF14161C : 0xFFFFFBEF, 5);

        int sx = popX + PAD, sy = popY + PAD, sw = popW - PAD * 2;
        bulkY = sy;
        if (hasBulk()) {
            noneX = sx + sw - BULK_W;
            allX = noneX - 2 - BULK_W;
            sw = allX - 2 - sx;
            bulkButton(ctx, font, allX, "All", mx, my);
            bulkButton(ctx, font, noneX, "None", mx, my);
        }
        insetFill(ctx, sx, sy, sx + sw, sy + SEARCH_H, C_FIELD_FOCUS, CONTROL_RADIUS);
        int ty = sy + (SEARCH_H - font.lineHeight) / 2 + 1;
        if (query.isEmpty()) {
            drawText(ctx, font, Component.literal(fit(font, searchHint(), sw - 8)), sx + 4, ty, C_TEXT_DIM, false);
        } else {
            drawText(ctx, font, Component.literal(fit(font, query, sw - 12)), sx + 4, ty, C_TEXT);
        }
        int caretX = sx + 4 + font.width(query.isEmpty() ? "" : fit(font, query, sw - 12));
        drawCaret(ctx, Math.min(caretX, sx + sw - 4), sy + 2, SEARCH_H - 4, C_TEXT);

        int ly = sy + SEARCH_H + PAD;
        int listBottom = ly + rows * ITEM_H;
        if (shown.isEmpty()) {
            drawCenteredText(ctx, font, Component.literal(emptyText()), popX + popW / 2,
                    ly + (rows * ITEM_H - font.lineHeight) / 2, C_TEXT_DIM);
        }
        int right = popX + popW - 5;
        ctx.enableScissor(popX, ly, right, listBottom);
        for (int i = 0; i < rows; i++) {
            int idx = listScroll + i;
            if (idx >= shown.size()) break;
            int iy = ly + i * ITEM_H;
            T entry = shown.get(idx);
            boolean hov = mx >= popX && mx < right && my >= iy && my < iy + ITEM_H;
            if (hov || idx == highlight) ctx.fill(popX + 2, iy, popX + popW - 6, iy + ITEM_H, hov ? C_CYCLE_HOVER : C_CYCLE_BG);
            boolean on = isOn(entry);

            int bx = popX + PAD + 2, by = iy + (ITEM_H - BOX) / 2;
            ctx.fill(bx, by, bx + BOX, by + BOX, on ? C_ACCENT : C_BORDER);
            if (!on) ctx.fill(bx + 1, by + 1, bx + BOX - 1, by + BOX - 1, C_FIELD_BG);

            String trailing = trailing(entry);
            int trailW = trailing.isEmpty() ? 0 : font.width(trailing) + 6;
            int textX = bx + BOX + 5, textY = iy + (ITEM_H - font.lineHeight) / 2 + 1;
            int room = right - 4 - trailW - textX;
            String name = fit(font, label(entry), room);
            drawText(ctx, font, Component.literal(name), textX, textY, on ? C_ACCENT : C_TEXT, false);
            int nameW = font.width(name);
            String detail = detail(entry);
            int detailRoom = room - nameW - 8;
            if (!detail.isEmpty() && detailRoom > 24) {
                drawText(ctx, font, Component.literal(fit(font, detail, detailRoom)), textX + nameW + 8, textY, C_TEXT_DIM, false);
            }
            if (!trailing.isEmpty()) {
                drawText(ctx, font, Component.literal(trailing), right - 4 - font.width(trailing), textY, C_TEXT_DIM, false);
            }
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            int trackH = rows * ITEM_H;
            int thumbH = Math.max(10, trackH * rows / shown.size());
            int thumbY = ly + (int) ((long) (trackH - thumbH) * listScroll / maxScroll);
            ctx.fill(popX + popW - 5, ly, popX + popW - 2, listBottom, overlay(0x1A));
            ctx.fill(popX + popW - 5, thumbY, popX + popW - 2, thumbY + thumbH, 0x77AABBDD);
        }
    }

    @Override boolean popupClick(double mx, double my) {
        if (!open) return false;
        boolean inside = mx >= popX && mx <= popX + popW && my >= popY && my <= popY + popH;
        if (!inside) { open = false; return true; }
        if (hasBulk() && my >= bulkY && my < bulkY + SEARCH_H) {
            if (mx >= allX && mx < allX + BULK_W) setAll(true);
            else if (mx >= noneX && mx < noneX + BULK_W) setAll(false);
            return true;
        }
        int ly = popY + PAD + SEARCH_H + PAD;
        if (my >= ly && my < ly + rows * ITEM_H && mx < popX + popW - 5) {
            int idx = listScroll + (int) ((my - ly) / ITEM_H);
            if (idx >= 0 && idx < shown.size()) toggle(shown.get(idx));
        }
        return true;
    }

    @Override boolean popupScroll(double mx, double my, double amount) {
        if (!open) return false;
        if (!(mx >= popX && mx <= popX + popW && my >= popY && my <= popY + popH)) { open = false; return false; }
        listScroll -= (int) Math.signum(amount) * 3;
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, shown.size() - rows)));
        return true;
    }

    private void refilter() {
        String[] terms = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        List<T> out = new ArrayList<>();
        for (T entry : entries()) {
            String hay = searchText(entry).toLowerCase(Locale.ROOT);
            boolean ok = true;
            for (String t : terms) {
                if (!t.isEmpty() && !hay.contains(t)) { ok = false; break; }
            }
            if (ok) out.add(entry);
        }
        shown = out;
        highlight = 0;
        listScroll = 0;
    }

    private void moveHighlight(int delta) {
        if (shown.isEmpty()) return;
        highlight = Math.max(0, Math.min(shown.size() - 1, highlight + delta));
        if (highlight < listScroll) listScroll = highlight;
        else if (highlight >= listScroll + rows) listScroll = highlight - rows + 1;
    }

    // ── Keyboard (the screen routes these here while the popup is open) ───────

    @Override void onKey(int key) {
        if (!open) return;
        if (key == InputConstants.KEY_ESCAPE) { open = false; return; }
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            if (highlight < shown.size()) toggle(shown.get(highlight));
            return;
        }
        if (key == InputConstants.KEY_DOWN)     { moveHighlight(1); return; }
        if (key == InputConstants.KEY_UP)       { moveHighlight(-1); return; }
        if (key == InputConstants.KEY_PAGEDOWN) { moveHighlight(rows); return; }
        if (key == InputConstants.KEY_PAGEUP)   { moveHighlight(-rows); return; }
        if (isCtrlDown() && key == InputConstants.KEY_V) { query += pasteFromClipboard(); refilter(); return; }
        if (isCtrlDown() && key == InputConstants.KEY_A) { return; }
        if (key == InputConstants.KEY_BACKSPACE && !query.isEmpty()) {
            query = isCtrlDown() ? "" : query.substring(0, query.length() - 1);
            refilter();
        }
    }

    @Override void onChar(char c) {
        if (!open || c < ' ' || c == 127) return;
        query += c;
        refilter();
    }

    @Override boolean isCapturing() { return open; }
    @Override void cancel() { open = false; }
}
