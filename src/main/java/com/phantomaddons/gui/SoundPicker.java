package com.phantomaddons.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.phantomaddons.gui.Theme.*;
import static com.phantomaddons.gui.GuiDraw.*;
import static com.phantomaddons.gui.TextEditing.*;

public class SoundPicker extends Feature {
    private static final int FIELD_MAX_W = 150, POPUP_W = 220;
    private static final int SEARCH_H = 16, ITEM_H = 11, MAX_ROWS = 9, MIN_ROWS = 3, PAD = 3;

    private static List<String> allSounds;

    private final Supplier<String> get;
    private final Consumer<String> set;

    private boolean open;
    private String query = "";
    private List<String> shown = List.of();
    private int listScroll, highlight;

    private int fieldX, fieldY, fieldW, fieldH;
    private int popX, popY, popW, popH, rows;
    private int areaX, areaY, areaW, areaH;

    public SoundPicker(String n, Supplier<String> get, Consumer<String> set) {
        super(n);
        this.get = get;
        this.set = set;
    }

    private static List<String> sounds() {
        if (allSounds == null) {
            List<String> out = new ArrayList<>();
            for (Identifier id : BuiltInRegistries.SOUND_EVENT.keySet()) out.add(toDisplay(id.toString()));
            out.sort(String.CASE_INSENSITIVE_ORDER);
            allSounds = out;
        }
        return allSounds;
    }

    private static String toDisplay(String stored) {
        if (stored == null) return "";
        return stored.startsWith("minecraft:") ? stored.substring("minecraft:".length()) : stored;
    }

    private static String toStored(String shown) {
        return shown.contains(":") ? shown : "minecraft:" + shown;
    }

    private static String fit(Font font, String text, int maxW) {
        if (font.width(text) <= maxW) return text;
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + "...") > maxW) end--;
        return text.substring(0, end) + "...";
    }

    // ── Row ───────────────────────────────────────────────────────────────────

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
        drawText(ctx, s.font(), Component.literal(fit(s.font(), toDisplay(get.get()), fieldW - 20)),
                fieldX + 4, fieldY + (fieldH - s.font().lineHeight) / 2 + 1, C_TEXT);
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
        String current = toDisplay(get.get());
        int idx = shown.indexOf(current);
        highlight = Math.max(idx, 0);
        listScroll = Math.max(0, highlight - MAX_ROWS / 2);
        return true;
    }

    // ── Popup ─────────────────────────────────────────────────────────────────

    boolean hasPopup() { return open; }

    /** Called by the screen so the popup can stay inside the scrolling content area. */
    void setArea(int x, int y, int w, int h) { areaX = x; areaY = y; areaW = w; areaH = h; }

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

    void renderPopup(GuiGraphicsExtractor ctx, PhantomScreen s, int mx, int my) {
        if (!open) return;
        Font font = s.font();
        layoutPopup();
        int maxScroll = Math.max(0, shown.size() - rows);
        listScroll = Math.max(0, Math.min(listScroll, maxScroll));

        roundedFill(ctx, popX - 1, popY - 1, popX + popW + 1, popY + popH + 1, C_BORDER, 5);
        roundedFill(ctx, popX, popY, popX + popW, popY + popH, darkMode ? 0xFF14161C : 0xFFFFFBEF, 5);

        int sx = popX + PAD, sy = popY + PAD, sw = popW - PAD * 2;
        insetFill(ctx, sx, sy, sx + sw, sy + SEARCH_H, C_FIELD_FOCUS, CONTROL_RADIUS);
        int ty = sy + (SEARCH_H - font.lineHeight) / 2 + 1;
        if (query.isEmpty()) {
            drawText(ctx, font, Component.literal("Search sounds..."), sx + 4, ty, C_TEXT_DIM, false);
        } else {
            String q = fit(font, query, sw - 12);
            drawText(ctx, font, Component.literal(q), sx + 4, ty, C_TEXT);
        }
        int caretX = sx + 4 + font.width(query.isEmpty() ? "" : fit(font, query, sw - 12));
        drawCaret(ctx, Math.min(caretX, sx + sw - 4), sy + 2, SEARCH_H - 4, C_TEXT);

        int ly = sy + SEARCH_H + PAD;
        int listBottom = ly + rows * ITEM_H;
        String current = toDisplay(get.get());
        if (shown.isEmpty()) {
            drawCenteredText(ctx, font, Component.literal("No sounds found"), popX + popW / 2,
                    ly + (rows * ITEM_H - font.lineHeight) / 2, C_TEXT_DIM);
        }
        ctx.enableScissor(popX, ly, popX + popW - 5, listBottom);
        for (int i = 0; i < rows; i++) {
            int idx = listScroll + i;
            if (idx >= shown.size()) break;
            int iy = ly + i * ITEM_H;
            String entry = shown.get(idx);
            boolean hov = mx >= popX && mx < popX + popW - 5 && my >= iy && my < iy + ITEM_H;
            if (hov || idx == highlight) ctx.fill(popX + 2, iy, popX + popW - 6, iy + ITEM_H, hov ? C_CYCLE_HOVER : C_CYCLE_BG);
            boolean selected = entry.equals(current);
            drawText(ctx, font, Component.literal(fit(font, entry, popW - 16)),
                    popX + PAD + 2, iy + (ITEM_H - font.lineHeight) / 2 + 1, selected ? C_ACCENT : C_TEXT, false);
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

    /** Returns true when the click was consumed; a click outside closes the popup. */
    boolean popupClick(double mx, double my) {
        if (!open) return false;
        boolean inside = mx >= popX && mx <= popX + popW && my >= popY && my <= popY + popH;
        if (!inside) { open = false; return true; }
        int ly = popY + PAD + SEARCH_H + PAD;
        if (my >= ly && my < ly + rows * ITEM_H && mx < popX + popW - 5) {
            int idx = listScroll + (int) ((my - ly) / ITEM_H);
            if (idx >= 0 && idx < shown.size()) choose(shown.get(idx));
        }
        return true;
    }

    boolean popupScroll(double mx, double my, double amount) {
        if (!open) return false;
        if (!(mx >= popX && mx <= popX + popW && my >= popY && my <= popY + popH)) { open = false; return false; }
        listScroll -= (int) Math.signum(amount) * 3;
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, shown.size() - rows)));
        return true;
    }

    private void choose(String display) {
        set.accept(toStored(display));
        open = false;
    }

    private void refilter() {
        String[] terms = query.toLowerCase().trim().split("\\s+");
        List<String> out = new ArrayList<>();
        for (String sound : sounds()) {
            String lower = sound.toLowerCase();
            boolean ok = true;
            for (String t : terms) {
                if (!t.isEmpty() && !lower.contains(t)) { ok = false; break; }
            }
            if (ok) out.add(sound);
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
            if (highlight < shown.size()) choose(shown.get(highlight));
            else open = false;
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
