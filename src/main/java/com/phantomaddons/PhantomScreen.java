package com.phantomaddons;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.phantomaddons.features.customisation.items.ItemCustomization;
import com.phantomaddons.features.customisation.items.ItemTransformSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.client.input.MouseButtonEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class PhantomScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────

    private static final int SIDEBAR_W = 108;
    private static final int HEADER_H  = 34;
    private static final int BASE_PANEL_W = 400;
    private static final int BASE_PANEL_H = 280;
    private static int PANEL_W = BASE_PANEL_W;
    private static int PANEL_H = BASE_PANEL_H;
    private static final int ROW_H     = 26;
    private static final int ROW_GAP   = 3;
    private static final int PAD       = 12;
    private static final int INDENT    = 12;
    private static final int PANEL_RADIUS = 6;
    private static final int GROUP_PAD_B = 6;

    // ── Colours ───────────────────────────────────────────────────────────────

    private static final Identifier LOGO =
            Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/logo.png");
    private static final Identifier LOGO_LIGHT =
            Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/logo_light.png");

    private static int C_BG          = 0xFF15151A;
    private static int C_SIDEBAR     = 0xFF101014;
    private static int C_HEADER      = 0xFF1B1B1F;
    private static int C_BORDER      = 0xFF1E2233;
    private static int C_ACCENT      = 0xFFFFAA00;
    private static int C_TAB_ACTIVE  = 0xFF1A1F2E;
    private static int C_TAB_HOVER   = 0xFF131720;
    private static int C_TEXT        = 0xFFD4D8E8;
    private static int C_TEXT_DIM    = 0xFF6B7399;
    private static int C_ON          = 0xFF44BB77;
    private static int C_OFF         = 0xFF2A3345;
    private static int C_SLIDER_BG   = 0xFF1A1F2E;
    private static int C_SLIDER_FG   = 0xFF2244AA;
    private static int C_SLIDER_GR   = 0xFF4477DD;
    private static int C_GROUP_BG    = 0x14FFFFFF;
    private static int C_GROUP_BAR   = 0x55FFAA00;
    private static int C_CYCLE_BG    = 0xFF151E33;
    private static int C_CYCLE_HOVER = 0xFF223366;
    private static int C_FIELD_BG    = 0xFF0F1218;
    private static int C_FIELD_HOVER = 0xFF17202E;
    private static int C_FIELD_FOCUS = 0xFF1A2A44;
    private static int C_ERROR       = 0xFFCC4444;
    private static int C_INFO        = 0xFF44AAFF;

    private static final int C_DIM_OVERLAY_ALPHA       = 0xBB; // ~73% opaque — transparency off
    private static final int C_DIM_OVERLAY_ALPHA_TRANS = 0x7A; // ~48% opaque — transparency on
    private static final int C_DIM_OVERLAY_RGB         = 0x06080C;

    private static boolean darkMode = true;
    private static PhantomConfig.UiTheme uiTheme = PhantomConfig.UiTheme.DARK;

    private static void applyTheme(PhantomConfig.UiTheme theme) {
        uiTheme = theme;
        darkMode = theme == PhantomConfig.UiTheme.DARK;
        switch (theme) {
            case DARK -> {
                C_BG          = 0xFF15151A;
                C_SIDEBAR     = 0xFF101014;
                C_HEADER      = 0xFF1B1B1F;
                C_BORDER      = 0xFF1E2233;
                C_ACCENT      = 0xFFFFAA00;
                C_TAB_ACTIVE  = 0xFF1A1F2E;
                C_TAB_HOVER   = 0xFF131720;
                C_TEXT        = 0xFFD4D8E8;
                C_TEXT_DIM    = 0xFF6B7399;
                C_ON          = 0xFF44BB77;
                C_OFF         = 0xFF2A3345;
                C_SLIDER_BG   = 0xFF1A1F2E;
                C_SLIDER_FG   = 0xFF2244AA;
                C_SLIDER_GR   = 0xFF4477DD;
                C_GROUP_BG    = 0x14FFFFFF;
                C_GROUP_BAR   = 0x55FFAA00;
                C_CYCLE_BG    = 0xFF151E33;
                C_CYCLE_HOVER = 0xFF223366;
                C_FIELD_BG    = 0xFF0F1218;
                C_FIELD_HOVER = 0xFF17202E;
                C_FIELD_FOCUS = 0xFF1A2A44;
                C_ERROR       = 0xFFCC4444;
                C_INFO        = 0xFF44AAFF;
            }
            case LIGHT -> {
                C_BG          = 0xFFF7F2E5;
                C_SIDEBAR     = 0xFFEEE6D0;
                C_HEADER      = 0xFFE1D3A4;
                C_BORDER      = 0xFFC7B98A;
                C_ACCENT      = 0xFFB87700;
                C_TAB_ACTIVE  = 0xFFEEDCA0;
                C_TAB_HOVER   = 0xFFE2D5AA;
                C_TEXT        = 0xFF201B0F;
                C_TEXT_DIM    = 0xFF6C5F3C;
                C_ON          = 0xFF357F49;
                C_OFF         = 0xFFC7B98A;
                C_SLIDER_BG   = 0xFFDBD0A8;
                C_SLIDER_FG   = 0xFFB87700;
                C_SLIDER_GR   = 0xFFCB9A33;
                C_GROUP_BG    = 0x18000000;
                C_GROUP_BAR   = 0x66B87700;
                C_CYCLE_BG    = 0xFFDBD0A8;
                C_CYCLE_HOVER = 0xFFC9BB86;
                C_FIELD_BG    = 0xFFE3D8B4;
                C_FIELD_HOVER = 0xFFEAD3A0;
                C_FIELD_FOCUS = 0xFFF2E3AC;
                C_ERROR       = 0xFF992222;
                C_INFO        = 0xFF1F5FA8;
            }
        }

        boolean transparent = PhantomConfig.isUiTransparencyEnabled();
        C_BG      = withAlpha(C_BG, transparent ? CONTENT_ALPHA_TRANS : 255);
        C_SIDEBAR = withAlpha(C_SIDEBAR, transparent ? SIDEBAR_ALPHA_TRANS : 255);
        C_HEADER  = withAlpha(C_HEADER, transparent ? HEADER_ALPHA_TRANS : 255);
    }

    private static final int CONTENT_ALPHA_TRANS = 130; // ~51% opaque
    private static final int SIDEBAR_ALPHA_TRANS = 130; // ~51% opaque
    private static final int HEADER_ALPHA_TRANS  = 165; // ~65% opaque

    private static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private static int overlay(int alpha) {
        return (alpha << 24) | (darkMode ? 0xFFFFFF : 0x000000);
    }

    private static void drawText(GuiGraphicsExtractor ctx, net.minecraft.client.gui.Font font,
                                  Component c, int x, int y, int color) {
        ctx.text(font, c, x, y, color, darkMode);
    }

    private static void drawText(GuiGraphicsExtractor ctx, net.minecraft.client.gui.Font font,
                                  Component c, int x, int y, int color, boolean shadow) {
        ctx.text(font, c, x, y, color, shadow);
    }

    private static void drawCenteredText(GuiGraphicsExtractor ctx, net.minecraft.client.gui.Font font,
                                          Component c, int cx, int y, int color) {
        if (darkMode) { ctx.centeredText(font, c, cx, y, color); return; }
        drawText(ctx, font, c, cx - font.width(c) / 2, y, color, false);
    }

    private static int dangerBg(boolean hover) {
        if (darkMode) return hover ? 0xFF4A1515 : 0xFF2A0F0F;
        return hover ? 0xFFE8B4B4 : 0xFFF2D6D6;
    }

    // ── Shared text-field editing helpers ────────────────────────────────────

    private static boolean fieldHovered(int fx, int fy, int fw, int fh, double mx, double my) {
        return mx >= fx && mx <= fx + fw && my >= fy && my <= fy + fh;
    }

    private static int clickToCursor(net.minecraft.client.gui.Font font, String text, int textStartX, double clickX) {
        int best = 0, bestDist = Integer.MAX_VALUE;
        for (int i = 0; i <= text.length(); i++) {
            int cx = textStartX + font.width(text.substring(0, i));
            int dist = (int) Math.abs(clickX - cx);
            if (dist < bestDist) { bestDist = dist; best = i; }
        }
        return best;
    }

    private static boolean caretVisible() {
        return (System.currentTimeMillis() / 530L) % 2 == 0;
    }

    private static void drawCaret(GuiGraphicsExtractor ctx, int x, int y, int h, int color) {
        if (caretVisible()) ctx.fill(x, y, x + 1, y + h, color);
    }

    private static String insertAt(String s, int pos, char c) {
        pos = Math.max(0, Math.min(s.length(), pos));
        return s.substring(0, pos) + c + s.substring(pos);
    }

    private static String deleteBefore(String s, int pos) {
        if (pos <= 0) return s;
        pos = Math.min(s.length(), pos);
        return s.substring(0, pos - 1) + s.substring(pos);
    }

    private static String deleteAfter(String s, int pos) {
        if (pos >= s.length()) return s;
        pos = Math.max(0, pos);
        return s.substring(0, pos) + s.substring(pos + 1);
    }

    private static String deleteRange(String s, int a, int b) {
        int lo = Math.max(0, Math.min(a, b)), hi = Math.min(s.length(), Math.max(a, b));
        if (lo >= hi) return s;
        return s.substring(0, lo) + s.substring(hi);
    }

    private static String insertRange(String s, int pos, String insert) {
        pos = Math.max(0, Math.min(s.length(), pos));
        return s.substring(0, pos) + insert + s.substring(pos);
    }

    private static boolean isCtrlDown() {
        com.mojang.blaze3d.platform.Window window = Minecraft.getInstance().getWindow();
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private static boolean isShiftDown() {
        com.mojang.blaze3d.platform.Window window = Minecraft.getInstance().getWindow();
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static void copyToClipboard(String s) {
        if (s == null || s.isEmpty()) return;
        Minecraft.getInstance().keyboardHandler.setClipboard(s);
    }

    private static String pasteFromClipboard() {
        String s = Minecraft.getInstance().keyboardHandler.getClipboard();
        return s == null ? "" : s.replace("\n", "").replace("\r", "");
    }

    private static void drawSelection(GuiGraphicsExtractor ctx, net.minecraft.client.gui.Font font,
                                      String shown, int windowStart, int anchor, int cursor,
                                      int textX, int fy, int fh, int color) {
        if (anchor == cursor) return;
        int lo = Math.max(windowStart, Math.min(anchor, cursor));
        int hi = Math.min(windowStart + shown.length(), Math.max(anchor, cursor));
        if (lo >= hi) return;
        int x1 = textX + font.width(shown.substring(0, lo - windowStart));
        int x2 = textX + font.width(shown.substring(0, hi - windowStart));
        ctx.fill(x1, fy + 2, x2, fy + fh - 2, color);
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    private enum Tab {
        ABOUT(null, "About"),

        LOADOUTS(  "Skyblock", "Loadouts"),
        RENDER(    "Skyblock", "Render"),
        MISC_SKYBLOCK("Skyblock", "Misc"),

        SUPPLIES(  "Kuudra", "Supplies"),
        BUILD(     "Kuudra", "Build"),
        STUN_DPS(  "Kuudra", "Stun/DPS"),
        BOSS(      "Kuudra", "Boss"),
        MISC_KUUDRA("Kuudra", "Misc"),

        DUNGEONS_M7("Dungeons", "M7"),

        FLUID_CUSTOM( "Customisation", "Lava Customisation"),
        ITEM_CUSTOM(  "Customisation", "Item Customisation"),
        VISUAL_WORDS( "Customisation", "Visual Words");

        final String category;
        final String label;
        Tab(String category, String l) { this.category = category; this.label = l; }
    }

    private Tab   currentTab = Tab.ABOUT;
    private static final float GROUP_SPEED = 10f;

    // ── Tab switch animation ─────────────────────────────────────────────────
    private List<RenderRow> outgoingRows = null;
    private float tabOutT       = 1f;
    private float tabInT        = 1f;
    private float tabInElapsed  = 0f;
    private static final float TAB_OUT_SPEED          = 3f;
    private static final float TAB_IN_SPEED           = 0.2f;
    private static final float TAB_IN_STAGGER         = 1f; // extra delay per row index
    private static final int   TAB_IN_MAX_STAGGER_ROWS = 14;    // cap stagger growth for long tabs
    private static final int   TAB_OUT_DIST = 90;
    private static final int   TAB_IN_DIST  = 500;

    // ── Feature base ──────────────────────────────────────────────────────────

    private abstract static class Feature {
        final String name;
        final Tab    tab;
        String tooltip;
        Supplier<Boolean> visible = () -> true;
        Feature(String n, Tab t) { name = n; tab = t; }
        <T extends Feature> T withTooltip(String t) { this.tooltip = t; return (T) this; }
        <T extends Feature> T withVisible(Supplier<Boolean> v) { this.visible = v; return (T) this; }
        abstract void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my);
        boolean onDown(double mx, double my, int x, int y, int w) { return false; }
        boolean onDrag(double mx, double my, int x, int y, int w) { return false; }
        void onUp()         {}
        void onKey(int k)   {}
        void onChar(char c) {}
        boolean isCapturing() { return false; }
        void cancel()         {}
    }

    // ── Toggle ────────────────────────────────────────────────────────────────

    private static final float TOGGLE_ANIM_SPEED = 5f;

    private static class Toggle extends Feature {
        final Supplier<Boolean> get; final Consumer<Boolean> set;
        private float knobAnim = -1f; // -1 = not yet initialised, snaps instead of animating in
        Toggle(String n, Tab t, Supplier<Boolean> get, Consumer<Boolean> set) {
            super(n, t); this.get = get; this.set = set;
        }
        @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
            drawText(ctx, s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            boolean on = get.get();
            float target = on ? 1f : 0f;
            if (knobAnim < 0f) knobAnim = target;
            else knobAnim += (target - knobAnim) * Math.min(1f, s.frameDelta * TOGGLE_ANIM_SPEED);

            int pw = 34, ph = 12, px = x + w - pw - 8, py = y + (ROW_H - ph) / 2;
            roundedFill(ctx, px, py, px + pw, py + ph, lerpColor(C_OFF, C_ON, knobAnim), ph / 2);
            int kxOff = px + 1, kxOn = px + pw - 12;
            int kx = Math.round(kxOff + (kxOn - kxOff) * knobAnim);
            roundedFill(ctx, kx, py + 1, kx + 10, py + ph - 1, 0xFFFFFFFF, (ph - 2) / 2);
        }
        @Override boolean onDown(double mx, double my, int x, int y, int w) {
            int pw = 34, px = x + w - pw - 8, py = y + (ROW_H - 12) / 2;
            if (mx >= px && mx <= px + pw && my >= py && my <= py + 12) {
                set.accept(!get.get()); return true;
            }
            return false;
        }
    }

    // ── Cycle ─────────────────────────────────────────────────────────────────

    private static class Cycle extends Feature {
        final Supplier<String> label; final Runnable cycle;
        Cycle(String n, Tab t, Supplier<String> l, Runnable c) {
            super(n, t); this.label = l; this.cycle = c;
        }
        @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
            drawText(ctx, s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            String val = label.get();
            int bw = s.font.width(val) + 18, bh = ROW_H - 6;
            int bx = x + w - bw - 8, by = y + 3;
            boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
            roundedFill(ctx, bx, by, bx + bw, by + bh, hov ? C_CYCLE_HOVER : C_CYCLE_BG, CONTROL_RADIUS);
            drawCenteredText(ctx, s.font, Component.literal(val),
                    bx + bw / 2, by + (bh - s.font.lineHeight) / 2, C_ACCENT);
        }
        @Override boolean onDown(double mx, double my, int x, int y, int w) {
            if (mx >= x && mx <= x + w && my >= y && my <= y + ROW_H) {
                cycle.run(); return true;
            }
            return false;
        }
    }

    // ── IntInput ──────────────────────────────────────────────────────────────

    private static class IntInput extends Feature {
        final Supplier<Integer> get; final Consumer<Integer> set;
        String  draft   = null;
        boolean focused = false;
        int     cursorPos = 0;
        int     fieldW = 50;
        IntInput(String n, Tab t, Supplier<Integer> get, Consumer<Integer> set) {
            super(n, t); this.get = get; this.set = set;
        }
        String displayValue() { return draft != null ? draft : String.valueOf(get.get()); }
        @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
            drawText(ctx, s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            String display = displayValue();
            int fw = fieldW, fx = x + w - fw - 8, fy = y + 3, fh = ROW_H - 6;
            boolean hov = fieldHovered(fx, fy, fw, fh, mx, my);
            int bg = focused ? C_FIELD_FOCUS : hov ? C_FIELD_HOVER : C_FIELD_BG;
            roundedFill(ctx, fx, fy, fx + fw, fy + fh, bg, CONTROL_RADIUS);
            int textX = fx + (fw - s.font.width(display)) / 2;
            drawText(ctx, s.font, Component.literal(display), textX, fy + (fh - s.font.lineHeight) / 2, C_TEXT);
            if (focused) {
                cursorPos = Math.max(0, Math.min(cursorPos, display.length()));
                int caretX = textX + s.font.width(display.substring(0, cursorPos));
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
            if (key == 256 || key == 257 || key == 335) { commit(); focused = false; } // Esc/Enter save
            else if (key == 259 && draft != null) { draft = deleteBefore(draft, cursorPos); cursorPos = Math.max(0, cursorPos - 1); }
            else if (key == 261 && draft != null) { draft = deleteAfter(draft, cursorPos); }
            else if (key == 263) cursorPos = Math.max(0, cursorPos - 1);       // Left
            else if (key == 262) cursorPos = Math.min(draft == null ? 0 : draft.length(), cursorPos + 1); // Right
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

    // ── OptionalIntInput ──────────────────────────────────────────────────────

    private static class OptionalIntInput extends IntInput {
        OptionalIntInput(String n, Tab t, Supplier<Integer> get, Consumer<Integer> set) {
            super(n, t, get, set);
        }
        @Override String displayValue() {
            if (draft != null) return draft;
            int val = get.get();
            return val < 0 ? "" : String.valueOf(val);
        }
        @Override void commit() {
            if (draft == null) return;
            if (draft.isEmpty()) set.accept(-1);
            else { try { set.accept(Integer.parseInt(draft)); } catch (NumberFormatException ignored) {} }
            draft = null;
        }
    }

    // ── SignedIntInput ────────────────────────────────────────────────────────

    private static class SignedIntInput extends IntInput {
        SignedIntInput(String n, Tab t, Supplier<Integer> get, Consumer<Integer> set) {
            super(n, t, get, set);
            fieldW = 64;
        }
        @Override void onChar(char c) {
            if (!focused) return;
            if (c == '-' && (draft == null || draft.isEmpty())) { draft = "-"; cursorPos = 1; }
            else if (Character.isDigit(c)) {
                if (draft == null) draft = "";
                draft = insertAt(draft, cursorPos, c);
                cursorPos++;
            }
        }
        @Override void commit() {
            if (draft != null && !draft.isEmpty() && !draft.equals("-")) {
                try { set.accept(Integer.parseInt(draft)); } catch (NumberFormatException ignored) {}
            }
            draft = null;
        }
    }

    // ── TextInput ─────────────────────────────────────────────────────────────

    private static class TextInput extends Feature {
        final Supplier<String> get; final Consumer<String> set;
        String  draft   = null;
        boolean focused = false;
        int     cursorPos = 0;
        int     selAnchor = 0;
        // cached from last render so onDown uses the same geometry
        private int lastFx, lastFy, lastFw, lastFh, lastStart;
        private String lastShown = "";
        TextInput(String n, Tab t, Supplier<String> get, Consumer<String> set) {
            super(n, t); this.get = get; this.set = set;
        }
        @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
            drawText(ctx, s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            String display = draft != null ? draft : stripNamespace(get.get());
            if (display == null) display = "";
            // Give the field most of the row width — just leave the label and a small gap
            int labelW = s.font.width(name);
            int fx = x + 8 + labelW + 8, fy = y + 3, fw = w - fx + x - 8, fh = ROW_H - 6;
            if (fw < 30) fw = 30;
            lastFx = fx; lastFy = fy; lastFw = fw; lastFh = fh;
            boolean hov = fieldHovered(fx, fy, fw, fh, mx, my);
            int bg = focused ? C_FIELD_FOCUS : hov ? C_FIELD_HOVER : C_FIELD_BG;
            roundedFill(ctx, fx, fy, fx + fw, fy + fh, bg, CONTROL_RADIUS);
            cursorPos = Math.max(0, Math.min(cursorPos, display.length()));
            selAnchor = Math.max(0, Math.min(selAnchor, display.length()));
            int maxChars = (fw - 8) / Math.max(1, s.font.width("a"));
            int start = display.length() <= maxChars ? 0
                    : focused ? Math.max(0, Math.min(display.length() - maxChars, cursorPos - maxChars / 2))
                              : display.length() - maxChars;
            String shown = display.substring(start, Math.min(display.length(), start + maxChars));
            lastStart = start; lastShown = shown;
            if (focused) drawSelection(ctx, s.font, shown, start, selAnchor, cursorPos, fx + 4, fy, fh, withAlpha(C_ACCENT, 90));
            drawText(ctx, s.font, Component.literal(shown), fx + 4, fy + (fh - s.font.lineHeight) / 2, C_TEXT);
            if (focused) {
                int visibleCursor = Math.max(start, Math.min(cursorPos, start + shown.length()));
                int caretX = fx + 4 + s.font.width(display.substring(start, visibleCursor));
                drawCaret(ctx, caretX, fy + 2, fh - 4, C_TEXT);
            }
        }
        @Override boolean onDown(double mx, double my, int x, int y, int w) {
            if (fieldHovered(lastFx, lastFy, lastFw, lastFh, mx, my)) {
                focused = true;
                String raw = get.get();
                draft = raw != null ? stripNamespace(raw) : "";
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
            if (key == 256 || key == 257 || key == 335) { commit(); focused = false; return; }
            if (ctrl && key == 65) { selAnchor = 0; cursorPos = draft == null ? 0 : draft.length(); return; } // Ctrl+A
            if (ctrl && key == 67) { copySelection(); return; } // Ctrl+C
            if (ctrl && key == 88) { copySelection(); deleteSelection(); return; } // Ctrl+X
            if (ctrl && key == 86) { pasteAtCursor(); return; } // Ctrl+V
            if (key == 259) { // Backspace
                if (selAnchor != cursorPos) deleteSelection();
                else if (draft != null) { draft = deleteBefore(draft, cursorPos); cursorPos = Math.max(0, cursorPos - 1); selAnchor = cursorPos; }
                return;
            }
            if (key == 261) { // Delete
                if (selAnchor != cursorPos) deleteSelection();
                else if (draft != null) draft = deleteAfter(draft, cursorPos);
                selAnchor = cursorPos;
                return;
            }
            if (key == 263) { // Left
                if (!shift && selAnchor != cursorPos) cursorPos = Math.min(selAnchor, cursorPos);
                else cursorPos = Math.max(0, cursorPos - 1);
                if (!shift) selAnchor = cursorPos;
                return;
            }
            if (key == 262) { // Right
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
            if (draft != null) set.accept(addNamespace(draft));
            draft = null;
        }
        // Strip "minecraft:" prefix for display; restore it on commit if no namespace typed
        private static String stripNamespace(String s) {
            if (s == null) return "";
            return s.startsWith("minecraft:") ? s.substring("minecraft:".length()) : s;
        }
        private static String addNamespace(String s) {
            if (s == null || s.isBlank()) return s;
            return s.contains(":") ? s : "minecraft:" + s;
        }
    }

    // ── RawTextInput ──────────────────────────────────────────────────────────

    private static class RawTextInput extends Feature {
        final Supplier<String> get; final Consumer<String> set;
        String  draft   = null;
        boolean focused = false;
        int     cursorPos = 0;
        int     selAnchor = 0;
        boolean masked  = false; // shown as asterisks while not focused
        private int lastFx, lastFy, lastFw, lastFh, lastStart;
        private String lastShown = "";
        RawTextInput(String n, Tab t, Supplier<String> get, Consumer<String> set) {
            super(n, t); this.get = get; this.set = set;
        }
        RawTextInput withMasked() { this.masked = true; return this; }
        @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
            drawText(ctx, s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            String display = draft != null ? draft : get.get();
            if (display == null) display = "";
            int labelW = s.font.width(name);
            int fx = x + 8 + labelW + 8, fy = y + 3, fw = w - fx + x - 8, fh = ROW_H - 6;
            if (fw < 30) fw = 30;
            lastFx = fx; lastFy = fy; lastFw = fw; lastFh = fh;
            boolean hov = fieldHovered(fx, fy, fw, fh, mx, my);
            int bg = focused ? C_FIELD_FOCUS : hov ? C_FIELD_HOVER : C_FIELD_BG;
            roundedFill(ctx, fx, fy, fx + fw, fy + fh, bg, CONTROL_RADIUS);
            cursorPos = Math.max(0, Math.min(cursorPos, display.length()));
            selAnchor = Math.max(0, Math.min(selAnchor, display.length()));
            int maxChars = (fw - 8) / Math.max(1, s.font.width("a"));
            int start = display.length() <= maxChars ? 0
                    : focused ? Math.max(0, Math.min(display.length() - maxChars, cursorPos - maxChars / 2))
                              : display.length() - maxChars;
            String shown = display.substring(start, Math.min(display.length(), start + maxChars));
            lastStart = start; lastShown = shown;
            if (focused) drawSelection(ctx, s.font, shown, start, selAnchor, cursorPos, fx + 4, fy, fh, withAlpha(C_ACCENT, 90));
            String shownForDraw = (masked && !focused) ? "*".repeat(shown.length()) : shown;
            drawText(ctx, s.font, Component.literal(shownForDraw), fx + 4, fy + (fh - s.font.lineHeight) / 2, C_TEXT);
            if (focused) {
                int visibleCursor = Math.max(start, Math.min(cursorPos, start + shown.length()));
                int caretX = fx + 4 + s.font.width(display.substring(start, visibleCursor));
                drawCaret(ctx, caretX, fy + 2, fh - 4, C_TEXT);
            }
        }
        @Override boolean onDown(double mx, double my, int x, int y, int w) {
            if (fieldHovered(lastFx, lastFy, lastFw, lastFh, mx, my)) {
                focused = true;
                draft = get.get() != null ? get.get() : "";
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
            if (key == 256 || key == 257 || key == 335) { commit(); focused = false; return; }
            if (ctrl && key == 65) { selAnchor = 0; cursorPos = draft == null ? 0 : draft.length(); return; } // Ctrl+A
            if (ctrl && key == 67) { copySelection(); return; } // Ctrl+C
            if (ctrl && key == 88) { copySelection(); deleteSelection(); return; } // Ctrl+X
            if (ctrl && key == 86) { pasteAtCursor(); return; } // Ctrl+V
            if (key == 259) { // Backspace
                if (selAnchor != cursorPos) deleteSelection();
                else if (draft != null) { draft = deleteBefore(draft, cursorPos); cursorPos = Math.max(0, cursorPos - 1); selAnchor = cursorPos; }
                return;
            }
            if (key == 261) { // Delete
                if (selAnchor != cursorPos) deleteSelection();
                else if (draft != null) draft = deleteAfter(draft, cursorPos);
                selAnchor = cursorPos;
                return;
            }
            if (key == 263) { // Left
                if (!shift && selAnchor != cursorPos) cursorPos = Math.min(selAnchor, cursorPos);
                else cursorPos = Math.max(0, cursorPos - 1);
                if (!shift) selAnchor = cursorPos;
                return;
            }
            if (key == 262) { // Right
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
            if (draft != null) set.accept(draft);
            draft = null;
        }
    }

    // ── KeyCapture ────────────────────────────────────────────────────────────

    public static final int MOUSE_OFFSET = 2000;

    private static class KeyCapture extends Feature {
        final IntSupplier getKey;
        final IntConsumer setKey;
        boolean capturing = false;

        KeyCapture(String n, Tab t, IntSupplier getKey, IntConsumer setKey) {
            super(n, t); this.getKey = getKey; this.setKey = setKey;
        }

        static String nameForCode(int code) {
            if (code <= 0) return "None";
            if (code >= MOUSE_OFFSET) return "Mouse " + (code - MOUSE_OFFSET + 1);
            if (code == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) return "None";
            try {
                String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(code, -1);
                return name != null ? name.toUpperCase() : "Key " + code;
            } catch (Exception e) { return "Key " + code; }
        }

        private String keyName() { return nameForCode(getKey.getAsInt()); }

        @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
            drawText(ctx, s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
            String label = capturing ? "[ Press key or mouse... ]" : "[ " + keyName() + " ]";
            int lw = s.font.width(label);
            int bx = x + w - lw - 16, by = y + 3, bh = ROW_H - 6;
            boolean hov = !capturing && mx >= bx && mx <= bx + lw + 8 && my >= by && my <= by + bh;
            roundedFill(ctx, bx, by, bx + lw + 8, by + bh, capturing ? 0xFF1A3344 : hov ? C_CYCLE_HOVER : C_CYCLE_BG, CONTROL_RADIUS);
            drawText(ctx, s.font, Component.literal(label),
                    bx + 4, by + (bh - s.font.lineHeight) / 2,
                    capturing ? 0xFF88CCFF : C_ACCENT);
        }
        @Override boolean onDown(double mx, double my, int x, int y, int w) {
            int by = y + 3, bh = ROW_H - 6, bx = x + w / 2;
            if (mx >= bx && mx <= x + w && my >= by && my <= by + bh) { capturing = true; return true; }
            return false;
        }
        @Override void onKey(int key) {
            if (!capturing) return;
            if (key == 256) { setKey.accept(-1); capturing = false; return; } // Escape = unbind
            setKey.accept(key); capturing = false;
        }
        void onMouseButton(int button) {
            if (!capturing) return;
            setKey.accept(MOUSE_OFFSET + button); capturing = false;
        }
        @Override boolean isCapturing() { return capturing; }
        @Override void cancel()         { capturing = false; }
    }

    // ── Slider ────────────────────────────────────────────────────────────────

    private static class Slider extends Feature {
        final Supplier<Float> get; final Consumer<Float> set;
        final String unit;
        boolean drag    = false;
        boolean editing = false;
        String  draft   = null;
        int boxX, boxY, boxW, boxH;
        Slider(String n, Tab t, Supplier<Float> get, Consumer<Float> set, String unit) {
            super(n, t); this.get = get; this.set = set; this.unit = unit;
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
            drawText(ctx, s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, labelColor());
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
            drawText(ctx, s.font, Component.literal(shown),
                    bx + bw - s.font.width(shown) - 3,
                    y + (ROW_H - s.font.lineHeight) / 2, editing ? C_TEXT : C_TEXT_DIM);
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

    // ── RangeSlider ───────────────────────────────────────────────────────────

    private static class RangeSlider extends Slider {
        final float min, max;
        final String fmt;
        RangeSlider(String n, Tab t, float min, float max, String fmt,
                    Supplier<Float> getRaw, Consumer<Float> setRaw) {
            super(n, t,
                    () -> (getRaw.get() - min) / (max - min),
                    v  -> setRaw.accept(v * (max - min) + min),
                    "");
            this.min = min; this.max = max; this.fmt = fmt;
        }
        @Override String displayValue() {
            float raw = get.get() * (max - min) + min;
            return String.format(fmt, raw);
        }
        @Override String editableText() { return stripToNumber(displayValue()); }
        @Override void applyEdited(String text) {
            try {
                float raw = Mth.clamp(Float.parseFloat(text), min, max);
                set.accept((raw - min) / (max - min));
            } catch (NumberFormatException ignored) {}
        }
        private static String stripToNumber(String formatted) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < formatted.length(); i++) {
                char c = formatted.charAt(i);
                if (Character.isDigit(c) || c == '.' || c == '-') sb.append(c);
            }
            return sb.toString();
        }
    }

    // ── ColorSlider ───────────────────────────────────────────────────────────

    private static class ColorSlider extends Slider {
        final int shift, tint;
        ColorSlider(String n, Tab t, int shift, int tint,
                    Supplier<Integer> colorGet, Consumer<Integer> colorSet) {
            super(n, t,
                    () -> ((colorGet.get() >> shift) & 0xFF) / 255f,
                    v -> {
                        int c = colorGet.get() & ~(0xFF << shift);
                        colorSet.accept(c | (Mth.clamp((int)(v * 255), 0, 255) << shift));
                    }, "");
            this.shift = shift; this.tint = tint;
        }
        @Override int labelColor() { return tint | 0xFF000000; }
        @Override int trackColor() { return tint | 0xFF000000; }
        @Override String displayValue() { return String.valueOf((int)(get.get() * 255)); }
        @Override String editableText()  { return displayValue(); }
        @Override void applyEdited(String text) {
            try {
                int v = Mth.clamp(Integer.parseInt(text), 0, 255);
                set.accept(v / 255f);
            } catch (NumberFormatException ignored) {}
        }
    }

    // ── LavaPreview ─────────────────────────────────────────────────────────

    private static class LavaPreview extends Feature {
        LavaPreview(Tab t) { super("Colour Preview", t); }
        @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
            int previewArgb = com.phantomaddons.features.customisation.lava.ColorPreviewHelper.computePreviewColor();
            int sw = w - 16, swX = x + 8, swY = y + 4, sh = ROW_H - 8;
            ctx.fill(swX, swY, swX + sw, swY + sh, previewArgb);
            ctx.fill(swX, swY, swX + sw, swY + 1, 0x22FFFFFF);
            drawCenteredText(ctx, s.font, Component.literal("Colour Preview"),
                    swX + sw / 2, swY + (sh - s.font.lineHeight) / 2, 0xCCFFFFFF);
        }
    }

    // ── Button ────────────────────────────────────────────────────────────────

    private static class Button extends Feature {
        final String label; final Runnable action;
        Button(String n, Tab t, String label, Runnable action) {
            super(n, t); this.label = label; this.action = action;
        }
        @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
            drawText(ctx, s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            int lw = s.font.width(label);
            int bw = lw + 18, bh = ROW_H - 6, bx = x + w - bw - 8, by = y + 3;
            boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
            roundedFill(ctx, bx, by, bx + bw, by + bh, hov ? C_CYCLE_HOVER : C_CYCLE_BG, CONTROL_RADIUS);
            drawCenteredText(ctx, s.font, Component.literal(label),
                    bx + bw / 2, by + (bh - s.font.lineHeight) / 2, C_ACCENT);
        }
        @Override boolean onDown(double mx, double my, int x, int y, int w) {
            int bw = label.length() * 6 + 18, bh = ROW_H - 6;
            int bx = x + w - bw - 8, by = y + 3;
            if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) { action.run(); return true; }
            return false;
        }
    }

    // ── AddCategoryFeature ────────────────────────────────────────────────────

    private class AddCategoryFeature extends Feature {
        boolean focused = false;
        String  draft   = "";
        AddCategoryFeature() { super("Add category", Tab.ITEM_CUSTOM); }

        @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
            drawText(ctx, s.font, Component.literal("Match:"),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
            int bw = 46, bx = x + w - bw - 12;
            int fy = y + 3, fh = ROW_H - 6;
            int fx = x + 60, fw = bx - fx - 4;
            roundedFill(ctx, fx, fy, fx + fw, fy + fh, focused ? C_FIELD_FOCUS : C_FIELD_BG, CONTROL_RADIUS);
            boolean placeholder = draft.isEmpty() && !focused;
            String disp = placeholder ? "type to filter..." : draft;
            drawText(ctx, s.font, Component.literal(disp),
                    fx + 4, fy + (fh - s.font.lineHeight) / 2, placeholder ? C_TEXT_DIM : C_TEXT);
            boolean hov = mx >= bx && mx <= bx + bw && my >= fy && my <= fy + fh;
            roundedFill(ctx, bx, fy, bx + bw, fy + fh, hov ? C_CYCLE_HOVER : C_CYCLE_BG, CONTROL_RADIUS);
            drawCenteredText(ctx, s.font, Component.literal("+ Add"),
                    bx + bw / 2, fy + (fh - s.font.lineHeight) / 2, 0xFF44BB77);
        }

        @Override boolean onDown(double mx, double my, int x, int y, int w) {
            int bw = 46, bx = x + w - bw - 12;
            int fy = y + 3, fh = ROW_H - 6;
            int fx = x + 60, fw = bx - fx - 4;
            if (mx >= fx && mx <= fx + fw && my >= fy && my <= fy + fh) {
                focused = true; return true;
            }
            if (mx >= bx && mx <= bx + bw && my >= fy && my <= fy + fh) {
                addCategory(); return true;
            }
            if (focused) focused = false;
            return false;
        }

        @Override void onKey(int key) {
            if (!focused) return;
            if (key == 256) { focused = false; }   // Esc keeps the typed value, just deselects
            else if (key == 257 || key == 335) { addCategory(); focused = false; }
            else if (key == 259 && !draft.isEmpty())
                draft = draft.substring(0, draft.length() - 1);
        }

        @Override void onChar(char c) { if (focused && c >= 32) draft += c; }
        @Override boolean isCapturing() { return focused; }
        @Override void cancel()         { focused = false; }

        private void addCategory() {
            if (!draft.isBlank()) {
                ItemCustomization.addCustomCategory(draft.trim());
                PhantomConfig.save();
                draft = "";
                buildFeatures();
            }
        }
    }

    // ── VisualWordEntry ──────────────────────────────────────────────────────

    private class VisualWordEntry extends Feature {
        final int idx;
        int     focus = 0;       // 0 none, 1 input, 2 replacement
        String  draft = null;
        VisualWordEntry(int idx) { super("Visual Word", Tab.VISUAL_WORDS); this.idx = idx; }

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
            drawCenteredText(ctx, s.font, Component.literal("→"),
                    arrowX(x, w) + 4, fy + (fh - s.font.lineHeight) / 2, C_TEXT_DIM);
            drawField(ctx, s, replX(x, w), fy, fw, fh, repText, focus == 2, "replace");

            int rx = removeX(x, w);
            boolean hov = mx >= rx && mx <= rx + 14 && my >= fy && my <= fy + fh;
            roundedFill(ctx, rx, fy, rx + 14, fy + fh, dangerBg(hov), CONTROL_RADIUS);
            drawCenteredText(ctx, s.font, Component.literal("✕"),
                    rx + 7, fy + (fh - s.font.lineHeight) / 2, C_ERROR);
        }

        private void drawField(GuiGraphicsExtractor ctx, PhantomScreen s, int fx, int fy, int fw, int fh,
                               String text, boolean foc, String hint) {
            roundedFill(ctx, fx, fy, fx + fw, fy + fh, foc ? C_FIELD_FOCUS : C_FIELD_BG, CONTROL_RADIUS);
            boolean placeholder = (text == null || text.isEmpty()) && !foc;
            String disp = placeholder ? hint : (text == null ? "" : text);
            drawText(ctx, s.font, Component.literal(disp),
                    fx + 4, fy + (fh - s.font.lineHeight) / 2, placeholder ? C_TEXT_DIM : C_TEXT);
        }

        @Override boolean onDown(double mx, double my, int x, int y, int w) {
            var r = rule();
            if (r == null) return false;
            int fy = y + 4, fh = ROW_H - 8, fw = fieldW(x, w);
            int rx = removeX(x, w);
            if (mx >= rx && mx <= rx + 14 && my >= fy && my <= fy + fh) {
                com.phantomaddons.features.customisation.VisualWords.removeRule(idx);
                buildFeatures();
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
            if (key == 256) { commit(); }                         // Esc saves current value
            else if (key == 257 || key == 335) { commit(); }
            else if (key == 258) { // Tab → jump to the other field
                var r = rule();
                if (r != null) { int next = focus == 1 ? 2 : 1; commit(); focusOn(next, next == 1 ? r.input : r.replacement); }
            }
            else if (key == 259 && draft != null && !draft.isEmpty())
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

    // ── RgbInput ───────────────────────────────────────────────────────────────

    private static class RgbInput extends Feature {
        final Supplier<Integer> get; final Consumer<Integer> set;
        int    focus = 0;   // 0 none, 1 R, 2 G, 3 B
        String draft = null;
        static final int FW = 30;
        RgbInput(String n, Tab t, Supplier<Integer> get, Consumer<Integer> set) {
            super(n, t); this.get = get; this.set = set;
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
            drawText(ctx, s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            int swX = fx(x, w, 0) - 16, swY = y + (ROW_H - 12) / 2;
            ctx.fill(swX, swY, swX + 12, swY + 12, 0xFF000000 | (get.get() & 0xFFFFFF));
            for (int i = 0; i < 3; i++) {
                int fxv = fx(x, w, i), fy = y + 3, fh = ROW_H - 6;
                boolean foc = focus == i + 1;
                roundedFill(ctx, fxv, fy, fxv + FW, fy + fh, foc ? C_FIELD_FOCUS : C_FIELD_BG, CONTROL_RADIUS);
                String disp = foc ? (draft == null ? "" : draft) : String.valueOf(comp(i));
                drawCenteredText(ctx, s.font, Component.literal(disp),
                        fxv + FW / 2, fy + (fh - s.font.lineHeight) / 2, C_TEXT);
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
            if (key == 256) { commit(); }                         // Esc saves current value
            else if (key == 257 || key == 335) { commit(); }
            else if (key == 258) { int next = focus % 3 + 1; commit(); focus = next; draft = String.valueOf(comp(next - 1)); }
            else if (key == 259 && draft != null && !draft.isEmpty())
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

    // ── Tree model ──────────────────────────────────────────────────────────────

    private abstract static class Node {
        final Tab tab;
        Node(Tab t) { this.tab = t; }
    }

    private static class Leaf extends Node {
        final Feature f;
        Leaf(Feature f) { super(f.tab); this.f = f; }
    }

    private static class Group extends Node {
        final String name;
        final String key;
        final Supplier<Boolean> get;
        final Consumer<Boolean> set;
        final List<Node> children = new ArrayList<>();
        String tooltip;
        Group(String name, Tab tab, String key, Supplier<Boolean> get, Consumer<Boolean> set) {
            super(tab); this.name = name; this.key = key; this.get = get; this.set = set;
        }
        Group withTooltip(String t) { this.tooltip = t; return this; }
        Group add(Node n) { children.add(n); return this; }
    }

    private static class RenderRow {
        Node node; int x, y, w, h, contentH, depth;
    }

    private static final Map<String, Boolean> EXPANDED = new HashMap<>();
    private static final Map<String, Float>   ANIM     = new HashMap<>();
    private static final Map<String, Float>   GROUP_TOGGLE_ANIM = new HashMap<>();

    // ── Feature lists ─────────────────────────────────────────────────────────

    private final List<Node>               roots           = new ArrayList<>();
    private final List<Feature>            allLeaves       = new ArrayList<>();
    private final List<Group>              allGroups       = new ArrayList<>();
    private final List<IntInput>           intInputs       = new ArrayList<>();
    private final List<KeyCapture>         captureFeatures = new ArrayList<>();
    private final List<Slider>             allSliders      = new ArrayList<>();
    private final List<AddCategoryFeature> categoryInputs  = new ArrayList<>();
    private final List<VisualWordEntry>    vwInputs        = new ArrayList<>();
    private final List<RgbInput>           rgbInputs       = new ArrayList<>();
    private final List<TextInput>          textInputs      = new ArrayList<>();
    private final List<RawTextInput>       rawTextInputs   = new ArrayList<>();

    // ── Build helpers ───────────────────────────────────────────────────────────

    private Leaf leaf(Feature f) {
        if (f instanceof IntInput ii)          intInputs.add(ii);
        if (f instanceof KeyCapture kc)        captureFeatures.add(kc);
        if (f instanceof Slider sl)            allSliders.add(sl);
        if (f instanceof AddCategoryFeature a) categoryInputs.add(a);
        if (f instanceof VisualWordEntry v)    vwInputs.add(v);
        if (f instanceof RgbInput rgb)         rgbInputs.add(rgb);
        if (f instanceof TextInput ti)         textInputs.add(ti);
        if (f instanceof RawTextInput rti)     rawTextInputs.add(rti);
        allLeaves.add(f);
        return new Leaf(f);
    }

    private Group group(String name, Tab tab, String parentKey,
                        Supplier<Boolean> get, Consumer<Boolean> set) {
        String key = (parentKey == null ? tab.name() : parentKey) + "/" + name;
        Group g = new Group(name, tab, key, get, set);
        allGroups.add(g);
        return g;
    }

    private void buildFeatures() {
        roots.clear();
        allLeaves.clear();
        allGroups.clear();
        intInputs.clear();
        captureFeatures.clear();
        allSliders.clear();
        categoryInputs.clear();
        vwInputs.clear();
        rgbInputs.clear();
        textInputs.clear();
        rawTextInputs.clear();

        buildAboutTab();
        buildLoadoutsTab();
        buildRenderTab();
        buildMiscSkyblockTab();
        buildSuppliesTab();
        buildBuildTab();
        buildStunDpsTab();
        buildBossTab();
        buildMiscTab();
        if (com.phantomaddons.Edition.CURRENT.fullFeatureSet) buildDungeonsTab();
        buildFluidCustomTab();
        buildItemCustomTab();
        buildVisualWordsTab();
    }

    // ── Stun / DPS tab ──────────────────────────────────────────────────────────

    private void buildStunDpsTab() {
        Tab T = Tab.STUN_DPS;

        if (com.phantomaddons.Edition.CURRENT.fullFeatureSet) {
            Group gfs = group("Auto GFS", T, null,
                    PhantomConfig::isAutoGfsEnabled, PhantomConfig::setAutoGfsEnabled);
            gfs.add(leaf(new Cycle("Role Mode", T,
                    () -> PhantomConfig.getRoleMode().name(),
                    () -> PhantomConfig.setRoleMode(switch (PhantomConfig.getRoleMode()) {
                        case DPS  -> PhantomConfig.RoleMode.STUN;
                        case STUN -> PhantomConfig.RoleMode.AUTO;
                        case AUTO -> PhantomConfig.RoleMode.DPS;
                    }))));
            gfs.add(leaf(new IntInput("DPS Amount",  T, PhantomConfig::getDpsValue,  PhantomConfig::setDpsValue)));
            gfs.add(leaf(new IntInput("Stun Amount", T, PhantomConfig::getStunValue, PhantomConfig::setStunValue)));
            gfs.add(leaf(new IntInput("Refill Amount", T, PhantomConfig::getDpsRefillAmount, PhantomConfig::setDpsRefillAmount)));
            gfs.add(leaf(new RangeSlider("Disable Refill Below HP", T, 25, 100, "%.0f%%",
                    () -> (float) PhantomConfig.getAutoGfsDisableHpPercent(),
                    v  -> PhantomConfig.setAutoGfsDisableHpPercent(Math.round(v)))));
            roots.add(gfs);
        }

        roots.add(leaf(new Toggle("Pickobulus Blocker", T,
                PhantomConfig::isPickoblockEnabled, PhantomConfig::setPickoblockEnabled)
                .withTooltip("Prevents early use of pickobulus in most situations")));

        Group eaten = group("Eaten Timer", T, null,
                PhantomConfig::isEatenTimerEnabled, PhantomConfig::setEatenTimerEnabled);
        eaten.add(leaf(new Toggle("Subtract Ping", T,
                PhantomConfig::isEatenTimerSubtractPingEnabled, PhantomConfig::setEatenTimerSubtractPingEnabled)));
        roots.add(eaten);

        if (com.phantomaddons.Edition.CURRENT.fullFeatureSet) {
            roots.add(leaf(new Toggle("Cannon Auto Close", T,
                    PhantomConfig::isCannonAutoCloseEnabled, PhantomConfig::setCannonAutoCloseEnabled)));
        }
        Group stunPreview = group("Stun Preview", T, null,
                PhantomConfig::isStunPreviewEnabled, PhantomConfig::setStunPreviewEnabled);
        stunPreview.add(leaf(new Toggle("Left", T,
                PhantomConfig::isStunPreviewLeftEnabled, PhantomConfig::setStunPreviewLeftEnabled)));
        stunPreview.add(leaf(new Toggle("Right", T,
                PhantomConfig::isStunPreviewRightEnabled, PhantomConfig::setStunPreviewRightEnabled)));
        stunPreview.add(leaf(new Toggle("Back", T,
                PhantomConfig::isStunPreviewBackEnabled, PhantomConfig::setStunPreviewBackEnabled)));
        roots.add(stunPreview);

        Group fastDps = group("Fast DPS Warning", T, null,
                PhantomConfig::isFastDpsWarningEnabled, PhantomConfig::setFastDpsWarningEnabled)
                .withTooltip("If DPS is faster than 3.3s, kuudra can appear in weird places");
        fastDps.add(leaf(new Toggle("Fast DPS Notification", T,
                PhantomConfig::isFastDpsNotifyEnabled, PhantomConfig::setFastDpsNotifyEnabled)));
        fastDps.add(soundGroup(T, PhantomConfig.SOUND_FAST_DPS));
        roots.add(fastDps);
    }

    // ── Build tab ───────────────────────────────────────────────────────────────

    private void buildBuildTab() {
        Tab T = Tab.BUILD;
        roots.add(leaf(new Toggle("Build Progress Tracker", T,
                PhantomConfig::isBuildProgressHudEnabled, PhantomConfig::setBuildProgressHudEnabled)));

        Group announce = group("Announce Fresh", T, null,
                PhantomConfig::isAnnounceFreshEnabled, PhantomConfig::setAnnounceFreshEnabled);
        announce.add(leaf(new Toggle("Fresh Notification", T,
                PhantomConfig::isFreshNotifyEnabled, PhantomConfig::setFreshNotifyEnabled)));
        announce.add(soundGroup(T, announce.key, PhantomConfig.SOUND_FRESH));
        roots.add(announce);

        Group buildStarted = group("Build Started Notification", T, null,
                PhantomConfig::isBuildStartedNotifyEnabled, PhantomConfig::setBuildStartedNotifyEnabled);
        buildStarted.add(soundGroup(T, buildStarted.key, PhantomConfig.SOUND_BUILD_STARTED));
        roots.add(buildStarted);
        Group buildBeacons = group("Build Beacons", T, null,
                PhantomConfig::isBuildBeaconsEnabled, PhantomConfig::setBuildBeaconsEnabled);
        buildBeacons.add(leaf(new Slider("Opacity", T,
                PhantomConfig::getBuildBeaconAlpha, PhantomConfig::setBuildBeaconAlpha, "%")));
        roots.add(buildBeacons);

        roots.add(leaf(new Toggle("Elle Highlight", T,
                PhantomConfig::isElleHighlightEnabled, PhantomConfig::setElleHighlightEnabled)));
    }

    // ── Supplies tab ──────────────────────────────────────────────────────────

    private void buildSuppliesTab() {
        Tab T = Tab.SUPPLIES;
        roots.add(leaf(new Toggle("Supply Beacons", T,
                PhantomConfig::isSupplyBeaconsEnabled, PhantomConfig::setSupplyBeaconsEnabled)));

        Group noPre = group("No Pre Announce", T, null,
                PhantomConfig::isNoPreAnnounceEnabled, PhantomConfig::setNoPreAnnounceEnabled);
        noPre.add(leaf(new Toggle("No Pre Notification", T,
                PhantomConfig::isNoPreNotifyEnabled, PhantomConfig::setNoPreNotifyEnabled)));
        noPre.add(soundGroup(T, noPre.key, PhantomConfig.SOUND_NO_PRE));
        roots.add(noPre);

        Group supplyGrabbed = group("Supply Grabbed Notification", T, null,
                PhantomConfig::isSupplyGrabbedNotifyEnabled, PhantomConfig::setSupplyGrabbedNotifyEnabled);
        supplyGrabbed.add(soundGroup(T, supplyGrabbed.key, PhantomConfig.SOUND_SUPPLY_GRABBED));
        roots.add(supplyGrabbed);

        Group supplyDropped = group("Supply Dropped Notification", T, null,
                PhantomConfig::isSupplyDroppedNotifyEnabled, PhantomConfig::setSupplyDroppedNotifyEnabled);
        supplyDropped.add(soundGroup(T, supplyDropped.key, PhantomConfig.SOUND_SUPPLY_DROPPED));
        roots.add(supplyDropped);

        roots.add(leaf(new Toggle("Crate Priority", T,
                PhantomConfig::isCratePriorityEnabled, PhantomConfig::setCratePriorityEnabled)));
        roots.add(leaf(new Toggle("Supply Recovery Message", T,
                PhantomConfig::isSupplyRecoveryMsgEnabled, PhantomConfig::setSupplyRecoveryMsgEnabled)));
        roots.add(leaf(new Toggle("Supply Location Announce", T,
                PhantomConfig::isSupplyLocationAnnounceEnabled, PhantomConfig::setSupplyLocationAnnounceEnabled)));
        roots.add(leaf(new Toggle("Supply Hitbox", T,
                PhantomConfig::isSupplyHitboxEnabled, PhantomConfig::setSupplyHitboxEnabled)));
        roots.add(leaf(new Toggle("Supply Rod Radius", T,
                PhantomConfig::isSupplyRodRadiusEnabled, PhantomConfig::setSupplyRodRadiusEnabled)));
        roots.add(leaf(new Toggle("Supply Pearl Hitbox", T,
                PhantomConfig::isSupplyPearlHitboxEnabled, PhantomConfig::setSupplyPearlHitboxEnabled)));
        roots.add(leaf(new Toggle("Supply Giant Hitbox Alert", T,
                PhantomConfig::isSupplyGiantHitboxEnabled, PhantomConfig::setSupplyGiantHitboxEnabled)));

        Group giantHitbox = group("Giant Hitbox", T, null,
                PhantomConfig::isGiantHitboxEnabled, PhantomConfig::setGiantHitboxEnabled);
        giantHitbox.add(leaf(new Toggle("Filled", T,
                PhantomConfig::isGiantHitboxFilled, PhantomConfig::setGiantHitboxFilled)));
        giantHitbox.add(leaf(new Slider("Fill Opacity", T,
                PhantomConfig::getGiantHitboxFillOpacity, PhantomConfig::setGiantHitboxFillOpacity, "")));
        giantHitbox.add(leaf(new RgbInput("Colour", T,
                PhantomConfig::getGiantHitboxColor, PhantomConfig::setGiantHitboxColor)));
        roots.add(giantHitbox);

        roots.add(leaf(new Toggle("Lava Bobber Fix", T,
                PhantomConfig::isLavaBobberFixEnabled, PhantomConfig::setLavaBobberFixEnabled)));
        roots.add(leaf(new Toggle("Legacy Rod Physics", T,
                PhantomConfig::isLegacyRodPhysicsEnabled, PhantomConfig::setLegacyRodPhysicsEnabled)));
        roots.add(leaf(new Toggle("Etherwarp Waypoints", T,
                PhantomConfig::isEtherwarpWaypointsEnabled, PhantomConfig::setEtherwarpWaypointsEnabled)));
        roots.add(leaf(new Toggle("Block Slot 9", T,
                PhantomConfig::isBlockSlot9Enabled, PhantomConfig::setBlockSlot9Enabled)
                .withTooltip("Prevents you from accidentally using the skyblock menu during supplies phase")));

        // Pearl waypoints — everything related lives under this dropdown
        Group wp = group("Dynamic Waypoints", T, null,
                PhantomConfig::isPearlWaypointsEnabled, PhantomConfig::setPearlWaypointsEnabled)
                .withTooltip("Optimal usage - Aim at the centre of the waypoints for the most consistency, aiming lower doesnt make the pearl land earlier.");
        wp.add(leaf(new Toggle("Show All Waypoints", T,
                PhantomConfig::isShowAllWaypoints, PhantomConfig::setShowAllWaypoints)));
        wp.add(leaf(new Toggle("Flat Pearls", T,
                PhantomConfig::isPearlFlatEnabled, PhantomConfig::setPearlFlatEnabled)));
        wp.add(leaf(new Toggle("Sky Pearls", T,
                PhantomConfig::isPearlSkyEnabled, PhantomConfig::setPearlSkyEnabled)));
        wp.add(leaf(new Toggle("Double Pearls", T,
                PhantomConfig::isPearlDoubleEnabled, PhantomConfig::setPearlDoubleEnabled)));
        wp.add(leaf(new RangeSlider("Double Pearl Delay", T, 0.05f, 0.55f, "%.2fs",
                PhantomConfig::getDoublePearlDelayS, PhantomConfig::setDoublePearlDelayS)));
        wp.add(leaf(new Cycle("Waypoint Type", T,
                () -> PhantomConfig.getWaypointType().name().charAt(0)
                        + PhantomConfig.getWaypointType().name().substring(1).toLowerCase(),
                () -> PhantomConfig.setWaypointType(
                        PhantomConfig.getWaypointType() == PhantomConfig.WaypointType.CIRCLE
                                ? PhantomConfig.WaypointType.SQUARE
                                : PhantomConfig.WaypointType.CIRCLE))));
        wp.add(leaf(new Toggle("Waypoint Fill", T,
                PhantomConfig::isWaypointFillEnabled, PhantomConfig::setWaypointFillEnabled)));
        wp.add(leaf(new Cycle("Update Frequency", T,
                () -> PhantomConfig.isPearlTickUpdate() ? "Per Tick" : "Per Frame",
                () -> PhantomConfig.setPearlTickUpdate(!PhantomConfig.isPearlTickUpdate()))));
        wp.add(leaf(new Toggle("Drop Locations", T,
                PhantomConfig::isDropLocationsEnabled, PhantomConfig::setDropLocationsEnabled)));
        wp.add(leaf(new Toggle("Pearl Timer", T,
                PhantomConfig::isPearlTimerEnabled, PhantomConfig::setPearlTimerEnabled)));
        wp.add(leaf(new Slider("Timer Height",   T, PhantomConfig::getPearlTimerHeight, PhantomConfig::setPearlTimerHeight, "")));
        wp.add(leaf(new Slider("Timer Size",     T, PhantomConfig::getPearlTimerSize,   PhantomConfig::setPearlTimerSize,   "")));
        wp.add(leaf(new Slider("Waypoint Size",  T, PhantomConfig::getPearlCircleSize,  PhantomConfig::setPearlCircleSize,  "")));
        wp.add(leaf(new Slider("Fill Opacity",   T, PhantomConfig::getWaypointFillAlpha,PhantomConfig::setWaypointFillAlpha,"%")));
        wp.add(leaf(new Slider("Beacon Opacity", T, PhantomConfig::getBeaconAlpha,      PhantomConfig::setBeaconAlpha,      "%")));

        Group wpCol = group("Waypoint Colours", T, wp.key, null, null);
        wpCol.add(leaf(new RgbInput("Normal target",  T, PhantomConfig::getWpColNormal,  PhantomConfig::setWpColNormal)));
        wpCol.add(leaf(new RgbInput("Correct target", T, PhantomConfig::getWpColCorrect, PhantomConfig::setWpColCorrect)));
        wpCol.add(leaf(new RgbInput("Hovered target", T, PhantomConfig::getWpColHovered, PhantomConfig::setWpColHovered)));
        wpCol.add(leaf(new RgbInput("Ready target",   T, PhantomConfig::getWpColReady,   PhantomConfig::setWpColReady)));
        wp.add(wpCol);

        Group bcnCol = group("Beacon Colours", T, wp.key, null, null);
        bcnCol.add(leaf(new RgbInput("Normal target",  T, PhantomConfig::getBeaconColNormal,  PhantomConfig::setBeaconColNormal)));
        bcnCol.add(leaf(new RgbInput("Correct target", T, PhantomConfig::getBeaconColCorrect, PhantomConfig::setBeaconColCorrect)));
        wp.add(bcnCol);
        wp.add(soundGroup(T, wp.key, PhantomConfig.SOUND_PEARL_NOW));

        roots.add(wp);

        Group wpLines = group("Waypoint Lines", T, null,
                PhantomConfig::isWaypointLinesEnabled, PhantomConfig::setWaypointLinesEnabled)
                .withTooltip("Draws a line from the crosshair to the next waypoint you have to aim at");
        wpLines.add(leaf(new Toggle("Flat Pearls", T,
                PhantomConfig::isWaypointLinesFlatPearlsEnabled, PhantomConfig::setWaypointLinesFlatPearlsEnabled)));
        wpLines.add(leaf(new Toggle("Supplies", T,
                PhantomConfig::isWaypointLinesSuppliesEnabled, PhantomConfig::setWaypointLinesSuppliesEnabled)));
        wpLines.add(leaf(new Cycle("Second Supply Preference", T,
                () -> PhantomConfig.getSecondSupplyPreference() == PhantomConfig.SecondSupplyPreference.DOUBLE_PEARL
                        ? "Pearl" : "Etherwarp",
                () -> PhantomConfig.setSecondSupplyPreference(
                        PhantomConfig.getSecondSupplyPreference() == PhantomConfig.SecondSupplyPreference.DOUBLE_PEARL
                                ? PhantomConfig.SecondSupplyPreference.ETHERWARP
                                : PhantomConfig.SecondSupplyPreference.DOUBLE_PEARL))));
        roots.add(wpLines);

        roots.add(leaf(new Toggle("Smooth Crate Pickup", T,
                PhantomConfig::isSmoothCratePickupEnabled, PhantomConfig::setSmoothCratePickupEnabled)));

        roots.add(leaf(new Cycle("Kuudra Talisman", T,
                () -> switch (PhantomConfig.getKuudraTalisman()) {
                    case NONE   -> "None";   case KIDNEY -> "Kidney";
                    case LUNG   -> "Lung";   case HEART  -> "Heart"; },
                () -> PhantomConfig.setKuudraTalisman(switch (PhantomConfig.getKuudraTalisman()) {
                    case NONE   -> PhantomConfig.KuudraTalisman.KIDNEY;
                    case KIDNEY -> PhantomConfig.KuudraTalisman.LUNG;
                    case LUNG   -> PhantomConfig.KuudraTalisman.HEART;
                    case HEART  -> PhantomConfig.KuudraTalisman.NONE; }))));
    }

    // ── Boss tab ────────────────────────────────────────────────────────────────

    private void buildBossTab() {
        Tab T = Tab.BOSS;

        Group solo = group("Solo Detector", T, null,
                PhantomConfig::isSoloDetectorEnabled, PhantomConfig::setSoloDetectorEnabled);
        solo.add(leaf(new Toggle("Solo Notification", T,
                PhantomConfig::isSoloNotifyEnabled, PhantomConfig::setSoloNotifyEnabled)));
        solo.add(soundGroup(T, PhantomConfig.SOUND_SOLO));
        roots.add(solo);

        Group hp = group("Kuudra HP HUD", T, null,
                PhantomConfig::isKuudraHpHudEnabled, PhantomConfig::setKuudraHpHudEnabled);
        hp.add(leaf(new Toggle("Show Raw HP", T,
                PhantomConfig::isKuudraHpShowRaw, PhantomConfig::setKuudraHpShowRaw)));
        hp.add(leaf(new Toggle("Hide Health Bar", T,
                PhantomConfig::isKuudraHpHideBar, PhantomConfig::setKuudraHpHideBar)));
        roots.add(hp);

        roots.add(leaf(new Toggle("Mana Drain Announcer", T,
                PhantomConfig::isManaDrainAnnouncerEnabled, PhantomConfig::setManaDrainAnnouncerEnabled)));
        roots.add(leaf(new Toggle("Kuudra Direction", T,
                PhantomConfig::isKuudraDirectionEnabled, PhantomConfig::setKuudraDirectionEnabled)));
        roots.add(leaf(new Toggle("Rend Damage", T,
                PhantomConfig::isRendDamageEnabled, PhantomConfig::setRendDamageEnabled)));
        roots.add(leaf(new Toggle("Rend Tracker", T,
                PhantomConfig::isRendTrackerEnabled, PhantomConfig::setRendTrackerEnabled)));
        Group boneTiming = group("Bone Timing Assist", T, null,
                PhantomConfig::isBoneTimingAssistEnabled, PhantomConfig::setBoneTimingAssistEnabled);
        boneTiming.add(leaf(new RangeSlider("Kuudra Timing Offset", T, -3.0f, 1.0f, "%.2f",
                PhantomConfig::getBoneTimingAssistOffset, PhantomConfig::setBoneTimingAssistOffset)));
        boneTiming.add(soundGroup(T, boneTiming.key, PhantomConfig.SOUND_BONE_THROW_NOW));
        roots.add(boneTiming);
        Group hitboxOutline = group("Predicted Hitbox Location", T, null,
                PhantomConfig::isBoneTimingHitboxOutlineEnabled, PhantomConfig::setBoneTimingHitboxOutlineEnabled);
        hitboxOutline.add(leaf(new Toggle("Filled", T,
                PhantomConfig::isBoneTimingHitboxOutlineFilled, PhantomConfig::setBoneTimingHitboxOutlineFilled)));
        hitboxOutline.add(leaf(new RgbInput("Colour", T,
                PhantomConfig::getBoneTimingHitboxOutlineColor, PhantomConfig::setBoneTimingHitboxOutlineColor)));
        hitboxOutline.add(leaf(new Toggle("Only Kuudra Direction", T,
                PhantomConfig::isBoneTimingHitboxOutlineOnlyCurrentDirection, PhantomConfig::setBoneTimingHitboxOutlineOnlyCurrentDirection)));
        hitboxOutline.add(leaf(new Toggle("Highlight In Range", T,
                PhantomConfig::isBoneTimingHitboxHighlightInRangeEnabled, PhantomConfig::setBoneTimingHitboxHighlightInRange)));
        roots.add(hitboxOutline);
        Group backbone = group("Backbone Progress Bar", T, null,
                PhantomConfig::isBackboneProgressBarEnabled, PhantomConfig::setBackboneProgressBarEnabled);
        backbone.add(leaf(new Toggle("Work Outside Kuudra", T,
                PhantomConfig::isBackboneProgressBarOutsideKuudraEnabled, PhantomConfig::setBackboneProgressBarOutsideKuudraEnabled)));
        backbone.add(soundGroup(T, backbone.key, PhantomConfig.SOUND_BACKBONE_DONE));
        roots.add(backbone);

        Group hl = group("Kuudra Highlight", T, null,
                PhantomConfig::isKuudraHighlightEnabled, PhantomConfig::setKuudraHighlightEnabled);
        hl.add(leaf(new Toggle("Filled Highlight", T,
                PhantomConfig::isKuudraHighlightFilled, PhantomConfig::setKuudraHighlightFilled)));
        roots.add(hl);

        roots.add(leaf(new Toggle("Block Atomsplit", T,
                PhantomConfig::isBlockAtomsplitEnabled, PhantomConfig::setBlockAtomsplitEnabled)
                .withTooltip("Prevents using any item with \"Atomsplit\" in its lore during the Boss phase")));
    }

    // ── Loadouts tab (Skyblock) ────────────────────────────────────────────────

    private void buildLoadoutsTab() {
        Tab T = Tab.LOADOUTS;

        Group wardrobe = group("Wardrobe Keybinds", T, null,
                PhantomConfig::isWardrobeEnabled, PhantomConfig::setWardrobeEnabled);
        String[] slotLabels = {"Slot 1","Slot 2","Slot 3","Slot 4","Slot 5",
                               "Slot 6","Slot 7","Slot 8","Slot 9"};
        for (int i = 0; i < 9; i++) {
            final int idx = i;
            wardrobe.add(leaf(new KeyCapture(slotLabels[i], T,
                    () -> PhantomConfig.getWardrobeSlotKeys()[idx],
                    v  -> PhantomConfig.setWardrobeSlotKey(idx, v))));
        }
        wardrobe.add(leaf(new KeyCapture("Open Wardrobe",  T, PhantomConfig::getWardrobeOpenKey,     PhantomConfig::setWardrobeOpenKey)));
        wardrobe.add(leaf(new KeyCapture("Stats Keybind",  T, PhantomConfig::getStatsOpenKey,        PhantomConfig::setStatsOpenKey)));
        wardrobe.add(leaf(new KeyCapture("Open Pets",      T, PhantomConfig::getPetsOpenKey,         PhantomConfig::setPetsOpenKey)));
        wardrobe.add(leaf(new KeyCapture("Equipment Wardrobe Keybind", T, PhantomConfig::getEqWardrobeOpenKey, PhantomConfig::setEqWardrobeOpenKey)));
        wardrobe.add(leaf(new KeyCapture("Next Page",      T, PhantomConfig::getWardrobeNextPageKey, PhantomConfig::setWardrobeNextPageKey)));
        wardrobe.add(leaf(new KeyCapture("Prev Page",      T, PhantomConfig::getWardrobePrevPageKey, PhantomConfig::setWardrobePrevPageKey)));
        wardrobe.add(leaf(new KeyCapture("Unequip",        T, PhantomConfig::getWardrobeUnequipKey,  PhantomConfig::setWardrobeUnequipKey)));
        wardrobe.add(leaf(new Toggle("Disable Unequip", T,
                PhantomConfig::isWardrobeDisableUnequipEnabled, PhantomConfig::setWardrobeDisableUnequipEnabled)));
        if (com.phantomaddons.Edition.CURRENT.fullFeatureSet) {
            wardrobe.add(leaf(new Toggle("Auto Close Wardrobe", T,
                    PhantomConfig::isWardrobeAutoCloseEnabled, PhantomConfig::setWardrobeAutoCloseEnabled)));
        }
        wardrobe.add(soundGroup(T, wardrobe.key, PhantomConfig.SOUND_WARDROBE_SWAP));
        roots.add(wardrobe);

        Group loadouts = group("Loadout Keybinds", T, null,
                PhantomConfig::isLoadoutsEnabled, PhantomConfig::setLoadoutsEnabled);
        loadouts.add(leaf(new KeyCapture("Loadouts Keybind", T, PhantomConfig::getLoadoutsOpenKey, PhantomConfig::setLoadoutsOpenKey)));
        for (int i = 0; i < 12; i++) {
            final int idx = i;
            loadouts.add(leaf(new KeyCapture("Loadout Slot " + (idx + 1), T,
                    () -> PhantomConfig.getLoadoutSlotKeys()[idx],
                    v  -> PhantomConfig.setLoadoutSlotKey(idx, v))));
        }
        loadouts.add(leaf(new KeyCapture("Next Page", T, PhantomConfig::getWardrobeNextPageKey, PhantomConfig::setWardrobeNextPageKey)));
        loadouts.add(leaf(new KeyCapture("Prev Page", T, PhantomConfig::getWardrobePrevPageKey, PhantomConfig::setWardrobePrevPageKey)));
        if (com.phantomaddons.Edition.CURRENT.fullFeatureSet) {
            loadouts.add(leaf(new Toggle("Auto Close Loadouts", T,
                    PhantomConfig::isWardrobeAutoCloseEnabled, PhantomConfig::setWardrobeAutoCloseEnabled)));
            loadouts.add(leaf(new IntInput("Extra Auto Close Ms", T,
                    PhantomConfig::getWardrobeExtraAutoCloseMs, PhantomConfig::setWardrobeExtraAutoCloseMs)
                    .withTooltip("Add time here if you are experiencing a reopening with auto close")));
        }
        loadouts.add(soundGroup(T, loadouts.key, PhantomConfig.SOUND_WARDROBE_SWAP));
        roots.add(loadouts);
    }

    // ── Render tab (Skyblock) ───────────────────────────────────────────────────

    private void buildRenderTab() {
        Tab T = Tab.RENDER;

        roots.add(leaf(new Toggle("Hide Falling Blocks", T,
                PhantomConfig::isHideFallingBlocksEnabled, PhantomConfig::setHideFallingBlocksEnabled)));
        roots.add(leaf(new Toggle("Hide Entity Fire", T,
                PhantomConfig::isHideEntityFireEnabled, PhantomConfig::setHideEntityFireEnabled)));
        roots.add(leaf(new Toggle("Hide Dead Enemies", T,
                PhantomConfig::isHideDeadEntitiesEnabled, PhantomConfig::setHideDeadEntitiesEnabled)));
        roots.add(leaf(new Toggle("Hide Selfie Cam", T,
                PhantomConfig::isHideSelfieEnabled, PhantomConfig::setHideSelfieEnabled)));

        Group as = group("Hide Irrelevant Armor Stands", T, null,
                PhantomConfig::isHideArmorStandsEnabled, PhantomConfig::setHideArmorStandsEnabled);
        as.add(leaf(new Toggle("Build Area", T,
                PhantomConfig::isHideArmorStandsBuild, PhantomConfig::setHideArmorStandsBuild)));
        as.add(leaf(new Toggle("Right Cannon", T,
                PhantomConfig::isHideArmorStandsRightCannon, PhantomConfig::setHideArmorStandsRightCannon)));
        as.add(leaf(new Toggle("Left Cannon", T,
                PhantomConfig::isHideArmorStandsLeftCannon, PhantomConfig::setHideArmorStandsLeftCannon)));
        as.add(leaf(new Toggle("Shop", T,
                PhantomConfig::isHideArmorStandsShop, PhantomConfig::setHideArmorStandsShop)));
        as.add(leaf(new Toggle("Others", T,
                PhantomConfig::isHideArmorStandsOthers, PhantomConfig::setHideArmorStandsOthers)
                .withTooltip("Warning: this hides rat pets")));
        roots.add(as);
    }

    // ── Misc tab (Skyblock) ─────────────────────────────────────────────────────

    private void buildMiscSkyblockTab() {
        Tab T = Tab.MISC_SKYBLOCK;

        Group scaling = group("Scaling", T, null, null, null);
        scaling.add(leaf(rs(T, "Self Player Scale", 1.0f, 300.0f, "%.0f%%",
                PhantomConfig::getSelfPlayerScale, PhantomConfig::setSelfPlayerScale)));
        scaling.add(leaf(rs(T, "Other Player Scale", 1.0f, 300.0f, "%.0f%%",
                PhantomConfig::getOtherPlayerScale, PhantomConfig::setOtherPlayerScale)));
        roots.add(scaling);

        if (com.phantomaddons.Edition.CURRENT.fullFeatureSet) {
            Group pearlRefill = group("Pearl Refill", T, null,
                    PhantomConfig::isPearlRefillEnabled, PhantomConfig::setPearlRefillEnabled);
            pearlRefill.add(leaf(new Toggle("Work Outside Kuudra", T,
                    PhantomConfig::isPearlRefillOutsideKuudraEnabled, PhantomConfig::setPearlRefillOutsideKuudraEnabled)));
            roots.add(pearlRefill);
        }

        roots.add(leaf(new Toggle("Auto Sprint", T,
                PhantomConfig::isAutoSprintEnabled, PhantomConfig::setAutoSprintEnabled)));

        Group heads = group("Prevent Placing Player Heads", T, null,
                PhantomConfig::isPreventPlacingPlayerHeadsEnabled, PhantomConfig::setPreventPlacingPlayerHeadsEnabled);
        heads.add(leaf(new Toggle("Except Garden", T,
                PhantomConfig::isPreventPlacingPlayerHeadsExceptGarden, PhantomConfig::setPreventPlacingPlayerHeadsExceptGarden)));
        roots.add(heads);

        roots.add(leaf(new Toggle("Prevent Placing Weapons", T,
                PhantomConfig::isPreventPlacingWeaponsEnabled, PhantomConfig::setPreventPlacingWeaponsEnabled)));

        Group slot = group("Slot Binds", T, null,
                PhantomConfig::isSlotBindsEnabled, PhantomConfig::setSlotBindsEnabled);
        slot.add(leaf(new KeyCapture("Bind Key", T,
                PhantomConfig::getSlotBindSetKey, PhantomConfig::setSlotBindSetKey)));
        slot.add(leaf(new KeyCapture("Show Binds Key", T,
                PhantomConfig::getSlotBindShowKey, PhantomConfig::setSlotBindShowKey)));
        roots.add(slot);
    }

    // ── Misc tab (Kuudra) ───────────────────────────────────────────────────────

    private void buildMiscTab() {
        Tab T = Tab.MISC_KUUDRA;

        roots.add(leaf(rs(T, "Kuudra Mob Size", 1.0f, 200.0f, "%.0f%%",
                PhantomConfig::getKuudraSizeScale, PhantomConfig::setKuudraSizeScale)));
        roots.add(leaf(new Toggle("Tuxedo Warning", T,
                PhantomConfig::isTuxedoWarningEnabled, PhantomConfig::setTuxedoWarningEnabled)));

        Group shop = group("Shop Keybinds", T, null,
                PhantomConfig::isShopKeybindsEnabled, PhantomConfig::setShopKeybindsEnabled);
        shop.add(leaf(new KeyCapture("Main Key",   T, PhantomConfig::getShopMainKey,   PhantomConfig::setShopMainKey)));
        shop.add(leaf(new KeyCapture("Cannon Key", T, PhantomConfig::getShopCannonKey, PhantomConfig::setShopCannonKey)));
        roots.add(shop);

        roots.add(leaf(new Toggle("Middle Click Perk Menu", T,
                PhantomConfig::isMiddleClickShopGuiEnabled, PhantomConfig::setMiddleClickShopGuiEnabled)));

        Group profit = group("Profit Tracker", T, null,
                PhantomConfig::isProfitTrackerEnabled, PhantomConfig::setProfitTrackerEnabled);
        profit.add(leaf(new Toggle("Show During Run", T,
                PhantomConfig::isProfitShowDuringRun, PhantomConfig::setProfitShowDuringRun)));
        profit.add(leaf(new Cycle("Armor", T,
                () -> PhantomConfig.isProfitArmorSalvage() ? "Salvage" : "Sell",
                () -> PhantomConfig.setProfitArmorSalvage(!PhantomConfig.isProfitArmorSalvage()))));
        profit.add(leaf(new Cycle("Faction", T,
                () -> PhantomConfig.isProfitFactionMage() ? "Mage" : "Barbarian",
                () -> PhantomConfig.setProfitFactionMage(!PhantomConfig.isProfitFactionMage()))));
        profit.add(leaf(new Toggle("Highlight Unopened Chests", T,
                PhantomConfig::isProfitHighlightChests, PhantomConfig::setProfitHighlightChests)));
        profit.add(leaf(new Toggle("Reroll Calculator", T,
                PhantomConfig::isProfitRerollCalc, PhantomConfig::setProfitRerollCalc)));
        profit.add(leaf(new Toggle("Block Reroll On Expensive Items", T,
                PhantomConfig::isBlockExpensiveRerollEnabled, PhantomConfig::setBlockExpensiveRerollEnabled)));
        profit.add(leaf(new Cycle("Bazaar Sell", T,
                () -> PhantomConfig.isProfitBazaarInstaSell() ? "Instasell" : "Sell Order",
                () -> PhantomConfig.setProfitBazaarInstaSell(!PhantomConfig.isProfitBazaarInstaSell()))));
        profit.add(leaf(new Cycle("Bazaar Buy", T,
                () -> PhantomConfig.isProfitBazaarInstaBuy() ? "Instabuy" : "Buy Order",
                () -> PhantomConfig.setProfitBazaarInstaBuy(!PhantomConfig.isProfitBazaarInstaBuy()))));
        profit.add(leaf(new Toggle("Chest Value GUI", T,
                PhantomConfig::isChestValueGuiEnabled, PhantomConfig::setChestValueGuiEnabled)));
        profit.add(leaf(new Cycle("Kuudra Pet", T,
                () -> {
                    PhantomConfig.KuudraPetRarity r = PhantomConfig.getKuudraPetRarity();
                    String name = r.name().charAt(0) + r.name().substring(1).toLowerCase();
                    return name;
                },
                () -> {
                    PhantomConfig.KuudraPetRarity[] vals = PhantomConfig.KuudraPetRarity.values();
                    int next = (PhantomConfig.getKuudraPetRarity().ordinal() + 1) % vals.length;
                    PhantomConfig.setKuudraPetRarity(vals[next]);
                })));
        profit.add(leaf(new RangeSlider("Pet Level", T, 1, 100, "%.0f",
                () -> (float) PhantomConfig.getKuudraPetLevel(),
                v  -> PhantomConfig.setKuudraPetLevel(Math.round(v)))));
        roots.add(profit);

        Group kicked = group("Kicked Notification", T, null,
                PhantomConfig::isKickedNotificationEnabled, PhantomConfig::setKickedNotificationEnabled);
        kicked.add(soundGroup(T, PhantomConfig.SOUND_KICKED));
        roots.add(kicked);
        Group autoRequeue = group("Auto Requeue", T, null,
                PhantomConfig::isAutoRequeueEnabled, PhantomConfig::setAutoRequeueEnabled);
        autoRequeue.add(leaf(new Toggle("Party Chat Message", T,
                PhantomConfig::isAutoRequeueMessageEnabled, PhantomConfig::setAutoRequeueMessageEnabled)));
        roots.add(autoRequeue);
        roots.add(leaf(new Toggle("Hide Damage Title", T,
                PhantomConfig::isHideDamageTitleEnabled, PhantomConfig::setHideDamageTitleEnabled)));
        roots.add(leaf(new Toggle("Hollow Wand Announcer", T,
                PhantomConfig::isHollowWandEnabled, PhantomConfig::setHollowWandEnabled)));
        Group hideBossBar = group("Hide Boss Bar", T, null,
                PhantomConfig::isHideBossBarEnabled, PhantomConfig::setHideBossBarEnabled);
        hideBossBar.add(leaf(new Toggle("Only In Kuudra", T,
                PhantomConfig::isHideBossBarOnlyInKuudra, PhantomConfig::setHideBossBarOnlyInKuudra)));
        roots.add(hideBossBar);

        roots.add(leaf(new Toggle("Hide Elle Dialogue", T,
                PhantomConfig::isHideElleDialogueEnabled, PhantomConfig::setHideElleDialogue)));
        roots.add(leaf(new Toggle("Etherwarp Lava Block", T,
                PhantomConfig::isEtherwarpLavaBlockEnabled,
                v -> { if (v != PhantomConfig.isEtherwarpLavaBlockEnabled()) PhantomConfig.toggleEtherwarpLavaBlock(); })));
        roots.add(leaf(new Toggle("Chest Tracker HUD", T,
                PhantomConfig::isChestTrackerVisible, PhantomConfig::setChestTrackerVisible)));

        Group explosion = group("Exploison Hider", T, null,
                PhantomConfig::isExplosionFilterEnabled, PhantomConfig::setExplosionFilterEnabled);
        explosion.add(leaf(new Slider("Hide Radius", T,
                PhantomConfig::getExplosionHideRadiusRaw, PhantomConfig::setExplosionHideRadius, "")));
        roots.add(explosion);

        roots.add(leaf(new Toggle("Party Commands", T,
                PhantomConfig::isPartyCmdsEnabled, PhantomConfig::setPartyCmdsEnabled)));

        Group split = group("Split Timer", T, null,
                PhantomConfig::isSplitTimerEnabled, PhantomConfig::setSplitTimerEnabled);
        split.add(leaf(new Toggle("Supply Times", T,
                PhantomConfig::isSupplyTimesEnabled, PhantomConfig::setSupplyTimesEnabled)));
        roots.add(split);

        if (com.phantomaddons.features.misckuudra.profile.RemoteFeatureGate.isEnabled()) {
            Group autoKick = group("Auto Kick", T, null,
                    PhantomConfig::isAutoKickEnabled, PhantomConfig::setAutoKickEnabled);
            autoKick.add(leaf(new OptionalIntInput("Catacombs Level", T, PhantomConfig::getAkMinCatacombs, PhantomConfig::setAkMinCatacombs)));
            autoKick.add(leaf(new OptionalIntInput("Foraging Level", T, PhantomConfig::getAkMinForaging, PhantomConfig::setAkMinForaging)));
            autoKick.add(leaf(new OptionalIntInput("Magical Power", T, PhantomConfig::getAkMinMagicalPower, PhantomConfig::setAkMinMagicalPower)));
            autoKick.add(leaf(new OptionalIntInput("Infernal Comps", T, PhantomConfig::getAkMinInfernal, PhantomConfig::setAkMinInfernal)));
            autoKick.add(leaf(new OptionalIntInput("Fiery Comps", T, PhantomConfig::getAkMinFiery, PhantomConfig::setAkMinFiery)));
            autoKick.add(leaf(new OptionalIntInput("Burning Comps", T, PhantomConfig::getAkMinBurning, PhantomConfig::setAkMinBurning)));
            autoKick.add(leaf(new OptionalIntInput("Hot Comps", T, PhantomConfig::getAkMinHot, PhantomConfig::setAkMinHot)));
            autoKick.add(leaf(new OptionalIntInput("Basic Comps", T, PhantomConfig::getAkMinBasic, PhantomConfig::setAkMinBasic)));
            autoKick.add(leaf(new Toggle("Rend", T,
                    PhantomConfig::isAkRequireRend, PhantomConfig::setAkRequireRend)));
            autoKick.add(leaf(new OptionalIntInput("Gdrag Level", T, PhantomConfig::getAkMinGdragLevel, PhantomConfig::setAkMinGdragLevel)));
            autoKick.add(leaf(new Toggle("Auto Kick Shitters", T,
                    com.phantomaddons.features.misckuudra.ShitterList::isAutoKickEnabled,
                    com.phantomaddons.features.misckuudra.ShitterList::setAutoKickEnabled)));
            roots.add(autoKick);

            roots.add(leaf(new Toggle("Profile Viewer", T,
                    PhantomConfig::isProfileViewerEnabled, PhantomConfig::setProfileViewerEnabled)));
        }
    }

    // ── Dungeons tab ────────────────────────────────────────────────────────────

    private void buildDungeonsTab() {
        Tab T = Tab.DUNGEONS_M7;

        Group m7toxic = group("M7 Auto GFS Toxic", T, null,
                PhantomConfig::isAutoGfsToxicEnabled, PhantomConfig::setAutoGfsToxic);
        m7toxic.add(leaf(new IntInput("Toxic Amount", T, PhantomConfig::getToxicAmount, PhantomConfig::setToxicAmount)));
        roots.add(m7toxic);

        Group m7twi = group("M7 Auto GFS Twilight", T, null,
                PhantomConfig::isAutoGfsTwilightEnabled, PhantomConfig::setAutoGfsTwilight);
        m7twi.add(leaf(new IntInput("Twilight Amount", T, PhantomConfig::getTwilightAmount, PhantomConfig::setTwilightAmount)));
        roots.add(m7twi);
    }

    // ── Lava Customisation tab ─────────────────────────────────────────────────

    private void buildFluidCustomTab() {
        Tab T = Tab.FLUID_CUSTOM;

        Group lava = group("Lava Tweaks", T, null,
                PhantomConfig::isLavaTweaksEnabled, PhantomConfig::setLavaTweaksEnabled);
        lava.add(leaf(new Toggle("Replace with Water", T,
                PhantomConfig::isLavaAsWater, PhantomConfig::setLavaAsWater)));
        lava.add(leaf(new Slider("Opacity", T,
                PhantomConfig::getLavaOpacity, PhantomConfig::setLavaOpacity, "%")));
        lava.add(leaf(new Toggle("Colour Override", T,
                PhantomConfig::isLavaColorOverride, PhantomConfig::setLavaColorOverride)));
        lava.add(leaf(new ColorSlider("Red",   T, 16, 0xFF4444, PhantomConfig::getLavaColor, PhantomConfig::setLavaColor)));
        lava.add(leaf(new ColorSlider("Green", T,  8, 0x44FF88, PhantomConfig::getLavaColor, PhantomConfig::setLavaColor)));
        lava.add(leaf(new ColorSlider("Blue",  T,  0, 0x4488FF, PhantomConfig::getLavaColor, PhantomConfig::setLavaColor)));
        lava.add(leaf(new LavaPreview(T)));
        roots.add(lava);
    }

    // ── Item Customisation tab ─────────────────────────────────────────────────

    private void buildItemCustomTab() {
        Tab T = Tab.ITEM_CUSTOM;

        Group ic = group("Item Customisation", T, null,
                PhantomConfig::isItemCustomizationEnabled, PhantomConfig::setItemCustomizationEnabled);

        for (ItemCustomization.ItemCategory cat : ItemCustomization.ItemCategory.values()) {
            ItemTransformSettings st = ItemCustomization.getBuiltinSettings(cat);
            String label = cat == ItemCustomization.ItemCategory.GLOBAL ? "Global" : cat.displayName();
            Group typeGroup = group(label, T, ic.key,
                    () -> st.enabled, v -> { st.enabled = v; PhantomConfig.save(); });
            addRangeSliders(typeGroup, T, st);
            ic.add(typeGroup);
        }

        Group custom = group("Custom Categories", T, ic.key, null, null);
        custom.add(leaf(new AddCategoryFeature()));
        List<ItemCustomization.CustomCategory> cats = ItemCustomization.getCustomCategories();
        for (int i = 0; i < cats.size(); i++) {
            final int idx = i;
            final ItemCustomization.CustomCategory cc = cats.get(i);
            Group ccGroup = group("\"" + cc.matchString + "\"", T, custom.key, null, null);
            ccGroup.add(leaf(new Feature("Remove", T) {
                @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
                    String prefix = "match: ";
                    drawText(ctx, s.font, Component.literal(prefix),
                            x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
                    drawText(ctx, s.font, Component.literal("\"" + cc.matchString + "\""),
                            x + 8 + s.font.width(prefix), y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
                    int bw = 58, bh = ROW_H - 6, bx = x + w - bw - 8, by = y + 3;
                    boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
                    roundedFill(ctx, bx, by, bx + bw, by + bh, dangerBg(hov), CONTROL_RADIUS);
                    drawCenteredText(ctx, s.font, Component.literal("Remove"),
                            bx + bw / 2, by + (bh - s.font.lineHeight) / 2, C_ERROR);
                }
                @Override boolean onDown(double mx, double my, int x, int y, int w) {
                    int bw = 58, bh = ROW_H - 6, bx = x + w - bw - 8, by = y + 3;
                    if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                        ItemCustomization.removeCustomCategory(idx);
                        PhantomConfig.save();
                        buildFeatures();
                        return true;
                    }
                    return false;
                }
            }));
            addRangeSliders(ccGroup, T, cc.settings);
            custom.add(ccGroup);
        }
        ic.add(custom);
        roots.add(ic);
    }

    // ── Visual Words tab ────────────────────────────────────────────────────────

    private void buildVisualWordsTab() {
        Tab T = Tab.VISUAL_WORDS;

        roots.add(leaf(new Toggle("Visual Words", T,
                com.phantomaddons.features.customisation.VisualWords::isEnabled,
                com.phantomaddons.features.customisation.VisualWords::setEnabled)));
        roots.add(leaf(new Button("Add Word", T, "+ Add",
                () -> { com.phantomaddons.features.customisation.VisualWords.addRule(); buildFeatures(); })));
        int vwCount = com.phantomaddons.features.customisation.VisualWords.getRules().size();
        for (int i = 0; i < vwCount; i++) roots.add(leaf(new VisualWordEntry(i)));
    }

    // ── About tab ─────────────────────────────────────────────────────────────

    private void buildAboutTab() {
        Tab T = Tab.ABOUT;

        roots.add(leaf(new Feature("Current Version", T) {
            @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
                drawText(ctx, s.font, Component.literal("Current Version"),
                        x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
                String ver = UpdateChecker.currentVersion();
                drawText(ctx, s.font, Component.literal(ver),
                        x + w - s.font.width(ver) - 8,
                        y + (ROW_H - s.font.lineHeight) / 2, C_ON);
            }
        }));

        roots.add(leaf(new Feature("Latest Version", T) {
            @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
                drawText(ctx, s.font, Component.literal("Latest Version"),
                        x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
                String latest = UpdateChecker.getLatestVersion();
                String display;
                int colour;
                switch (UpdateChecker.getState()) {
                    case CHECKING, DOWNLOADING -> { display = "Checking..."; colour = C_TEXT_DIM; }
                    case ERROR                 -> { display = "Error";        colour = C_ERROR; }
                    case UP_TO_DATE            -> { display = latest != null ? latest : "Up to date"; colour = C_ON; }
                    case UPDATE_AVAILABLE      -> { display = latest;         colour = C_ACCENT; }
                    case DOWNLOADED            -> { display = latest;         colour = C_INFO; }
                    default                    -> { display = "—";            colour = C_TEXT_DIM; }
                }
                drawText(ctx, s.font, Component.literal(display),
                        x + w - s.font.width(display) - 8,
                        y + (ROW_H - s.font.lineHeight) / 2, colour);
            }
        }));

        roots.add(leaf(new Feature("Status", T) {
            @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
                drawText(ctx, s.font, Component.literal("Status"),
                        x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
                String status = switch (UpdateChecker.getState()) {
                    case IDLE              -> "Idle";
                    case CHECKING          -> "Checking...";
                    case UP_TO_DATE        -> "Up to date";
                    case UPDATE_AVAILABLE  -> "Update available";
                    case DOWNLOADING       -> "Downloading...";
                    case DOWNLOADED        -> "Ready to install";
                    case ERROR             -> "Check failed";
                };
                int colour = switch (UpdateChecker.getState()) {
                    case UP_TO_DATE   -> C_ON;
                    case DOWNLOADED   -> C_INFO;
                    case UPDATE_AVAILABLE -> C_ACCENT;
                    case ERROR        -> C_ERROR;
                    default           -> C_TEXT_DIM;
                };
                drawText(ctx, s.font, Component.literal(status),
                        x + w - s.font.width(status) - 8,
                        y + (ROW_H - s.font.lineHeight) / 2, colour);
            }
        }));

        if (com.phantomaddons.Edition.CURRENT.autoDownloadCapable) {
            roots.add(leaf(new Toggle("Auto Updates", T,
                    PhantomConfig::isAutoUpdatesEnabled, PhantomConfig::setAutoUpdatesEnabled)));
        }
        roots.add(leaf(new Button("Updates", T, "Update", UpdateChecker::downloadManually)
                .withVisible(UpdateChecker::hasUpdate)));

        // ── Everything else (kept in their existing relative order, moved below) ──
        roots.add(leaf(new IntInput("Ping (ms) - Lowest normal ping", T,
                PhantomConfig::getLowPing, PhantomConfig::setLowPing)
                .withTooltip("Should be set to the lowest ping that you would get under normal conditions")));
        roots.add(leaf(new Button("HUD Layout", T,
                "Edit Layout",
                () -> Minecraft.getInstance().setScreen(new HudEditorScreen(PhantomScreen.this)))));
        roots.add(leaf(new Cycle("Theme", T,
                () -> switch (PhantomConfig.getUiTheme()) {
                    case DARK -> "Dark";
                    case LIGHT -> "Light";
                },
                () -> {
                    PhantomConfig.UiTheme next = switch (PhantomConfig.getUiTheme()) {
                        case DARK -> PhantomConfig.UiTheme.LIGHT;
                        case LIGHT -> PhantomConfig.UiTheme.DARK;
                    };
                    PhantomConfig.setUiTheme(next);
                    applyTheme(next);
                    if (searchField != null) {
                        searchField.setTextColor(C_TEXT);
                        searchField.setTextShadow(darkMode);
                    }
                })));
        roots.add(leaf(new Toggle("Transparency Effects", T,
                PhantomConfig::isUiTransparencyEnabled,
                v -> { PhantomConfig.setUiTransparencyEnabled(v); applyTheme(PhantomConfig.getUiTheme()); })
                .withTooltip("Makes the GUI and its dim backdrop see-through so the game world shows behind it")));
        roots.add(leaf(new RangeSlider("GUI Scale", T, 0.7f, 1.6f, "%.2fx",
                PhantomConfig::getUiGuiScale,
                v -> {
                    PhantomConfig.setUiGuiScale(v);
                    applyGuiScale(PhantomConfig.getUiGuiScale());
                    resize(width, height);
                })
                .withTooltip("Resizes the mod's own GUI panel — everything inside stays the same size, this just fits more (or less) on screen at once")));
        roots.add(leaf(new Toggle("Developer Features", T,
                PhantomConfig::isDeveloperFeaturesEnabled, PhantomConfig::setDeveloperFeaturesEnabled)));

        roots.add(leaf(new Button("PhantomAddons Discord", T, "Copy Link",
                () -> copyToClipboard("https://discord.gg/6MquvmrXNP"))));

        if (com.phantomaddons.features.misckuudra.profile.RemoteFeatureGate.isEnabled()) {
            roots.add(leaf(new RawTextInput("API Key", T,
                    PhantomConfig::getKuudraApiKey, PhantomConfig::setKuudraApiKey)
                    .withMasked()
                    .withTooltip("Get a key with /apikey in the PhantomAddons Discord server. Required for Auto Kick and Profile Viewer.")));
        }
    }

    // ── Sound group helper ────────────────────────────────────────────────────

    private Group soundGroup(Tab tab, String soundKey) {
        return soundGroup(tab, null, soundKey);
    }

    private Group soundGroup(Tab tab, String parentKey, String soundKey) {
        Group g = group("Sound", tab, parentKey,
                () -> PhantomConfig.isNotificationSoundEnabled(soundKey),
                v  -> PhantomConfig.setNotificationSoundEnabled(soundKey, v));
        g.add(leaf(new TextInput("Sound ID", tab,
                () -> PhantomConfig.getNotificationSoundId(soundKey),
                v  -> PhantomConfig.setNotificationSoundId(soundKey, v))));
        g.add(leaf(new RangeSlider("Volume", tab, 0f, 2f, "%.2f",
                () -> PhantomConfig.getNotificationSoundVolume(soundKey),
                v  -> PhantomConfig.setNotificationSoundVolume(soundKey, v))));
        g.add(leaf(new RangeSlider("Pitch", tab, 0.1f, 4f, "%.2f",
                () -> PhantomConfig.getNotificationSoundPitch(soundKey),
                v  -> PhantomConfig.setNotificationSoundPitch(soundKey, v))));
        return g;
    }

    // ── Range slider helpers ────────────────────────────────────────────────────

    private void addRangeSliders(Group g, Tab tab, ItemTransformSettings s) {
        g.add(leaf(rs(tab, "pos X",     -0.5f,  0.5f,  "%.3f",       () -> s.posX,       v -> { s.posX       = v; PhantomConfig.save(); })));
        g.add(leaf(rs(tab, "pos Y",     -0.5f,  0.5f,  "%.3f",       () -> s.posY,       v -> { s.posY       = v; PhantomConfig.save(); })));
        g.add(leaf(rs(tab, "pos Z",     -0.5f,  0.5f,  "%.3f",       () -> s.posZ,       v -> { s.posZ       = v; PhantomConfig.save(); })));
        g.add(leaf(rs(tab, "rot X",     -180f,  180f,  "%.0f°", () -> s.rotX,       v -> { s.rotX       = v; PhantomConfig.save(); })));
        g.add(leaf(rs(tab, "rot Y",     -180f,  180f,  "%.0f°", () -> s.rotY,       v -> { s.rotY       = v; PhantomConfig.save(); })));
        g.add(leaf(rs(tab, "rot Z",     -180f,  180f,  "%.0f°", () -> s.rotZ,       v -> { s.rotZ       = v; PhantomConfig.save(); })));
        g.add(leaf(rs(tab, "scale",     0.25f,  3.0f,  "%.2f×", () -> s.scale,      v -> { s.scale      = v; PhantomConfig.save(); })));
        g.add(leaf(rs(tab, "swing spd", 0.25f,  3.0f,  "%.2f×", () -> s.swingSpeed, v -> { s.swingSpeed = v; PhantomConfig.save(); })));
        g.add(leaf(rs(tab, "proximity", -1.0f,  1.0f,  "%.3f",       () -> s.proximity,  v -> { s.proximity  = v; PhantomConfig.save(); })));
        g.add(leaf(new Toggle("No Equip Animation", tab, () -> s.noEquipAnimation, v -> { s.noEquipAnimation = v; PhantomConfig.save(); })));
        g.add(leaf(new Toggle("In Place Swing",     tab, () -> s.inPlaceSwing,     v -> { s.inPlaceSwing     = v; PhantomConfig.save(); })));
        g.add(leaf(new Toggle("Static Position",    tab, () -> s.staticPosition,   v -> { s.staticPosition   = v; PhantomConfig.save(); })));
    }

    private RangeSlider rs(Tab tab, String name, float min, float max, String fmt,
                           Supplier<Float> get, Consumer<Float> set) {
        return new RangeSlider(name, tab, min, max, fmt, get, set);
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private EditBox searchField;
    private String          query       = "";
    private int             scroll      = 0;
    private Feature         dragFeature = null;
    private int             dragX, dragY, dragW;

    private boolean draggingScroll = false;
    private int     sbThumbY, sbThumbH, sbTrackTop, sbTrackH, sbMaxScroll;

    private int tabScroll = 0;
    private final List<TabRow> tabRows = new ArrayList<>();

    private record TabRow(Tab tab, boolean isHeader, String label, int y, int h) {}

    // ── Open/close animation ─────────────────────────────────────────────────

    private static final float OPEN_ANIM_SPEED  = 3.0f; // ~1/3s to fully open
    private static final float CLOSE_ANIM_SPEED = 4.0f; // slightly snappier close

    private float   openAnim = 0f;
    private boolean closing  = false;
    float frameDelta = 0f;

    private float openProgress() {
        float t = openAnim;
        return t * t * (3f - 2f * t); // smoothstep
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    public PhantomScreen() {
        super(Component.literal("PhantomAddons"));
        applyTheme(PhantomConfig.getUiTheme());
        applyGuiScale(PhantomConfig.getUiGuiScale());
        buildFeatures();
    }

    private static void applyGuiScale(float scale) {
        PANEL_W = Math.round(BASE_PANEL_W * scale);
        PANEL_H = Math.round(BASE_PANEL_H * scale);
    }

    @Override
    public void onClose() {
        if (!closing) {
            closing = true;
            return;
        }
        super.onClose();
    }

    // ── Geometry ──────────────────────────────────────────────────────────────

    private int px() { return width  / 2 - PANEL_W / 2; }
    private int py() { return height / 2 - PANEL_H / 2; }
    private int cx() { return px() + SIDEBAR_W; }
    private int cy() { return py() + HEADER_H; }
    private int cw() { return PANEL_W - SIDEBAR_W; }
    private int ch() { return PANEL_H - HEADER_H; }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        int sfX = px() + PANEL_W - 148;
        int sfY = py() + (HEADER_H - 14) / 2;
        searchField = new EditBox(font, sfX, sfY, 138, 14, Component.empty());
        searchField.setMaxLength(64);
        searchField.setBordered(false);
        searchField.setTextColor(C_TEXT);
        searchField.setTextShadow(darkMode);
        searchField.setResponder(s -> { query = s.toLowerCase(); scroll = 0; });
        addRenderableWidget(searchField);
    }

    // ── Anim state ──────────────────────────────────────────────────────────────

    private boolean isExpanded(Group g) { return EXPANDED.getOrDefault(g.key, false); }
    private float   animOf(Group g)      { return ANIM.getOrDefault(g.key, 0f); }
    private float   catAnimOf(String category) { return ANIM.getOrDefault("cat:" + category, 0f); }

    private static final List<String> CATEGORIES = new ArrayList<>();
    static {
        for (Tab t : Tab.values())
            if (t.category != null && !CATEGORIES.contains(t.category)) CATEGORIES.add(t.category);
    }

    private void stepAnims(float delta) {
        for (Group g : allGroups) {
            float cur = ANIM.getOrDefault(g.key, 0f);
            float tgt = isExpanded(g) ? 1f : 0f;
            if (cur != tgt) {
                float step = delta * GROUP_SPEED * 0.05f; // delta is ~ticks; tune
                if (cur < tgt) cur = Math.min(tgt, cur + step);
                else           cur = Math.max(tgt, cur - step);
                ANIM.put(g.key, cur);
            }

            if (g.get != null) {
                float tCur = GROUP_TOGGLE_ANIM.getOrDefault(g.key, g.get.get() ? 1f : 0f);
                float tTgt = g.get.get() ? 1f : 0f;
                if (tCur != tTgt) {
                    tCur += (tTgt - tCur) * Math.min(1f, delta * TOGGLE_ANIM_SPEED);
                    GROUP_TOGGLE_ANIM.put(g.key, tCur);
                }
            }
        }

        for (String cat : CATEGORIES) {
            String key = "cat:" + cat;
            float cur = ANIM.getOrDefault(key, 0f);
            float tgt = isCategoryExpanded(cat) ? 1f : 0f;
            if (cur == tgt) continue;
            float step = delta * GROUP_SPEED * 0.05f;
            if (cur < tgt) cur = Math.min(tgt, cur + step);
            else           cur = Math.max(tgt, cur - step);
            ANIM.put(key, cur);
        }
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private List<RenderRow> layoutRows() {
        if (!query.isEmpty()) {
            List<RenderRow> out = new ArrayList<>();
            int baseX = cx() + PAD;
            int baseW = cw() - PAD * 2;
            int[] y = { cy() + PAD - scroll };
            for (Node n : roots) {
                Node f = filterNode(n, query);
                if (f != null) layoutNode(f, 0, baseX, baseW, 1f, y, out);
            }
            return out;
        }
        return layoutRowsForTab(currentTab);
    }

    private List<RenderRow> layoutRowsForTab(Tab tab) {
        List<RenderRow> out = new ArrayList<>();
        int baseX = cx() + PAD;
        int baseW = cw() - PAD * 2;
        int[] y = { cy() + PAD - scroll };
        for (Node n : roots) {
            if (n.tab != tab) continue;
            layoutNode(n, 0, baseX, baseW, 1f, y, out);
        }
        return out;
    }

    private Node filterNode(Node n, String q) {
        if (n instanceof Leaf lf) {
            return lf.f.name.toLowerCase().contains(q) ? lf : null;
        }
        Group g = (Group) n;
        if (g.name.toLowerCase().contains(q)) return g; // title match → whole dropdown
        Group copy = null;
        for (Node c : g.children) {
            Node fc = filterNode(c, q);
            if (fc != null) {
                if (copy == null) copy = new Group(g.name, g.tab, g.key, g.get, g.set);
                copy.children.add(fc);
            }
        }
        return copy;
    }

    private void layoutNode(Node n, int depth, int baseX, int baseW,
                            float ancestorAnim, int[] y, List<RenderRow> out) {
        if (n instanceof Leaf lf && !lf.f.visible.get()) {
            RenderRow r = new RenderRow();
            r.node = n; r.depth = depth; r.contentH = 0; r.h = 0;
            r.x = baseX + depth * INDENT;
            r.w = baseW - depth * INDENT;
            r.y = y[0];
            out.add(r);
            return;
        }

        int naturalH = ROW_H;
        int slotH = Math.round(naturalH * ancestorAnim);
        int gap   = Math.round(ROW_GAP * ancestorAnim);

        RenderRow r = new RenderRow();
        r.node = n; r.depth = depth; r.contentH = naturalH; r.h = slotH;
        r.x = baseX + depth * INDENT;
        r.w = baseW - depth * INDENT;
        r.y = y[0];
        out.add(r);
        y[0] += slotH + gap;

        if (n instanceof Group g) {
            float childAnim = ancestorAnim * animOf(g);
            if (childAnim > 0.002f) {
                y[0] += Math.round(GROUP_PAD_B * childAnim); // top margin inside the block
                for (Node c : g.children)
                    layoutNode(c, depth + 1, baseX, baseW, childAnim, y, out);
                // bottom margin (matches top) + one ROW_GAP of external space before the next item
                y[0] += Math.round((GROUP_PAD_B + ROW_GAP) * childAnim);
            }
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isDoubleClick) {
        for (KeyCapture kc : captureFeatures) {
            if (kc.capturing) { kc.onMouseButton(click.button()); return true; }
        }

        if (super.mouseClicked(click, isDoubleClick)) return true;
        if (searchField != null) searchField.setFocused(false);
        this.setFocused(null);
        if (click.button() != 0) return false;

        double mx = click.x(), my = click.y();

        if (sbMaxScroll > 0) {
            int sbX = cx() + cw() - 5;
            if (mx >= sbX - 1 && mx <= sbX + 4 && my >= sbTrackTop && my <= sbTrackTop + sbTrackH) {
                if (my >= sbThumbY && my <= sbThumbY + sbThumbH) {
                    draggingScroll = true;
                } else {
                    setScrollFromThumb(my - sbThumbH / 2.0);
                    draggingScroll = true;
                }
                return true;
            }
        }

        int startY = py() + HEADER_H + 6;
        for (TabRow r : tabRows) {
            int ty = startY + r.y() - tabScroll;
            if (ty < startY || ty + r.h() > py() + PANEL_H) continue;
            if (!(mx >= px() && mx <= px() + SIDEBAR_W - 1 && my >= ty && my <= ty + r.h())) continue;

            if (r.isHeader()) {
                ANIM.putIfAbsent("cat:" + r.label(), isCategoryExpanded(r.label()) ? 1f : 0f);
                EXPANDED.put("cat:" + r.label(), !isCategoryExpanded(r.label()));
                return true;
            }
            if (r.h() < TAB_ROW_H - 1) continue; // skip rows still animating in/out

            {
                if (r.tab() != currentTab) {
                    intInputs.forEach(Feature::cancel);
                    categoryInputs.forEach(Feature::cancel);
                    vwInputs.forEach(Feature::cancel);
                    rgbInputs.forEach(Feature::cancel);
                    textInputs.forEach(Feature::cancel);
                    rawTextInputs.forEach(Feature::cancel);
                    allSliders.forEach(Feature::cancel);
                    outgoingRows = layoutRowsForTab(currentTab);
                    tabOutT = 0f;
                    currentTab = r.tab();
                    tabInT = 0f;
                    tabInElapsed = 0f;
                    scroll = 0;
                }
                return true;
            }
        }

        intInputs.forEach(Feature::cancel);
        categoryInputs.forEach(f -> { if (f.focused) f.focused = false; });
        vwInputs.forEach(Feature::cancel);
        rgbInputs.forEach(Feature::cancel);
        textInputs.forEach(Feature::cancel);
        rawTextInputs.forEach(Feature::cancel);
        allSliders.forEach(Feature::cancel);

        if (tabOutT >= 1f && mx >= cx() && mx <= cx() + cw() && my >= cy() && my <= cy() + ch()) {
            for (RenderRow r : layoutRows()) {
                if (r.h < r.contentH - 1) continue; // skip rows mid-animation
                if (my < r.y || my > r.y + r.contentH) continue;
                if (mx < r.x || mx > r.x + r.w) continue;

                if (r.node instanceof Group g) {
                    if (groupHeaderDown(g, mx, my, r.x, r.y, r.w)) return true;
                    return true;
                } else if (r.node instanceof Leaf lf) {
                    if (lf.f.onDown(mx, my, r.x, r.y, r.w)) {
                        dragFeature = lf.f;
                        dragX = r.x; dragY = r.y; dragW = r.w;
                        return true;
                    }
                    return false;
                }
            }
        }
        return false;
    }

    private boolean groupHeaderDown(Group g, double mx, double my, int x, int y, int w) {
        if (g.get != null && g.set != null) {
            int pw = 34, pxa = x + w - pw - 8, pya = y + (ROW_H - 12) / 2;
            if (mx >= pxa && mx <= pxa + pw && my >= pya && my <= pya + 12) {
                GROUP_TOGGLE_ANIM.putIfAbsent(g.key, g.get.get() ? 1f : 0f);
                g.set.accept(!g.get.get());
                return true;
            }
        }
        EXPANDED.put(g.key, !isExpanded(g));
        return true;
    }

    private void setScrollFromThumb(double thumbTop) {
        if (sbTrackH - sbThumbH <= 0) { scroll = 0; return; }
        double frac = (thumbTop - sbTrackTop) / (sbTrackH - sbThumbH);
        scroll = Mth.clamp((int) Math.round(frac * sbMaxScroll), 0, sbMaxScroll);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (draggingScroll) {
            setScrollFromThumb(click.y() - sbThumbH / 2.0);
            return true;
        }
        if (dragFeature != null) { dragFeature.onDrag(click.x(), click.y(), dragX, dragY, dragW); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        draggingScroll = false;
        if (dragFeature != null) { dragFeature.onUp(); dragFeature = null; }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        if (mx >= px() && mx <= px() + SIDEBAR_W && my >= py() + HEADER_H && my <= py() + PANEL_H) {
            tabScroll -= (int)(vScroll * 12); return true;
        }
        if (mx >= cx() && mx <= cx() + cw() && my >= cy() && my <= cy() + ch()) {
            scroll -= (int)(vScroll * 12); return true;
        }
        return super.mouseScrolled(mx, my, hScroll, vScroll);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = event.key();

        for (KeyCapture kc : captureFeatures) {
            if (kc.capturing) { kc.onKey(key); return true; }
        }
        for (AddCategoryFeature af : categoryInputs) {
            if (af.focused) { af.onKey(key); return true; }
        }
        for (VisualWordEntry ve : vwInputs) {
            if (ve.isCapturing()) { ve.onKey(key); return true; }
        }
        for (RgbInput ri : rgbInputs) {
            if (ri.isCapturing()) { ri.onKey(key); return true; }
        }
        if (intInputs.stream().anyMatch(Feature::isCapturing)) {
            intInputs.forEach(f -> f.onKey(key));
            return true;
        }
        if (textInputs.stream().anyMatch(Feature::isCapturing)) {
            textInputs.forEach(f -> f.onKey(key));
            return true;
        }
        if (rawTextInputs.stream().anyMatch(Feature::isCapturing)) {
            rawTextInputs.forEach(f -> f.onKey(key));
            return true;
        }
        if (allSliders.stream().anyMatch(Feature::isCapturing)) {
            allSliders.forEach(f -> f.onKey(key));
            return true;
        }
        if (key == 256 && searchField != null && searchField.isFocused()) {
            searchField.setFocused(false);
            this.setFocused(null);
            return true;
        }

        if (key == 256) { this.onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent input) {
        for (AddCategoryFeature af : categoryInputs) {
            if (af.focused) { af.onChar((char) input.codepoint()); return true; }
        }
        for (VisualWordEntry ve : vwInputs) {
            if (ve.isCapturing()) { ve.onChar((char) input.codepoint()); return true; }
        }
        for (RgbInput ri : rgbInputs) {
            if (ri.isCapturing()) { ri.onChar((char) input.codepoint()); return true; }
        }
        if (intInputs.stream().anyMatch(Feature::isCapturing)) {
            intInputs.forEach(f -> f.onChar((char) input.codepoint()));
            return true;
        }
        if (textInputs.stream().anyMatch(Feature::isCapturing)) {
            textInputs.forEach(f -> f.onChar((char) input.codepoint()));
            return true;
        }
        if (rawTextInputs.stream().anyMatch(Feature::isCapturing)) {
            rawTextInputs.forEach(f -> f.onChar((char) input.codepoint()));
            return true;
        }
        if (allSliders.stream().anyMatch(Feature::isCapturing)) {
            allSliders.forEach(f -> f.onChar((char) input.codepoint()));
            return true;
        }
        if (searchField != null && searchField.isFocused()) return super.charTyped(input);
        return false;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        int baseAlpha = PhantomConfig.isUiTransparencyEnabled() ? C_DIM_OVERLAY_ALPHA_TRANS : C_DIM_OVERLAY_ALPHA;
        int overlayA = (int) (baseAlpha * openProgress()) << 24;
        ctx.fill(0, 0, width, height, C_DIM_OVERLAY_RGB | overlayA);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        this.frameDelta = delta;
        extractBackground(ctx, mx, my, delta);

        if (closing) {
            openAnim = Math.max(0f, openAnim - delta * CLOSE_ANIM_SPEED);
            if (openAnim <= 0f) { super.onClose(); return; }
        } else if (openAnim < 1f) {
            openAnim = Math.min(1f, openAnim + delta * OPEN_ANIM_SPEED);
        }

        if (tabOutT < 1f) {
            tabOutT = Math.min(1f, tabOutT + delta * TAB_OUT_SPEED);
            if (tabOutT >= 1f) outgoingRows = null;
        } else if (tabInT < 1f) {
            tabInElapsed += delta;
            float maxNeeded = TAB_IN_MAX_STAGGER_ROWS * TAB_IN_STAGGER + 1f / TAB_IN_SPEED;
            tabInT = Math.min(1f, tabInElapsed / maxNeeded);
        }
        stepAnims(delta);

        int px = px(), py = py();
        float prog  = openProgress();
        float scale = 0.82f + 0.18f * prog;
        float pcx = px + PANEL_W / 2f, pcy = py + PANEL_H / 2f;

        var m = ctx.pose();
        m.pushMatrix();
        m.translate(pcx, pcy);
        m.scale(scale, scale);
        m.translate(-pcx, -pcy);

        int outlineColor = PhantomConfig.isUiTransparencyEnabled() ? withAlpha(C_BORDER, CONTENT_ALPHA_TRANS) : C_BORDER;
        roundedFill(ctx, px - 1, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, outlineColor, PANEL_RADIUS + 1);
        // Circular gradient stemming from near the top-left of the content area (the panel's
        // largest single section) — noticeably stronger in transparency mode, since it's the
        // one surface where a flat colour would otherwise look the most washed-out/plain.
        boolean transparentBg = PhantomConfig.isUiTransparencyEnabled();
        int bgShade = transparentBg ? 26 : 10;
        double gradCenterX = px + SIDEBAR_W + 24;
        double gradCenterY = py + HEADER_H + 20;
        double gradRadius  = Math.hypot(PANEL_W, PANEL_H) * 0.85;
        roundedFillRadial(ctx, px, py, px + PANEL_W, py + PANEL_H,
                gradCenterX, gradCenterY, gradRadius,
                shade(C_BG, bgShade), shade(C_BG, -bgShade), PANEL_RADIUS, true, true, true, true);

        ctx.enableScissor(px, py, px + PANEL_W, py + PANEL_H);
        roundedFill(ctx, px, py, px + PANEL_W, py + HEADER_H, C_HEADER, PANEL_RADIUS, true, true, false, false);
        ctx.fill(px, py + HEADER_H - 1, px + PANEL_W, py + HEADER_H, C_ACCENT);
        int logoS = HEADER_H - 8, logoX = px + 6, logoY = py + 4;
        ctx.blit(darkMode ? LOGO : LOGO_LIGHT, logoX, logoY, logoX + logoS, logoY + logoS, 0f, 1f, 0f, 1f);
        drawText(ctx, font, Component.literal("PhantomAddons"),
                logoX + logoS + 6, py + (HEADER_H - font.lineHeight) / 2, C_ACCENT);

        int sfX = px + PANEL_W - 148, sfY = py + (HEADER_H - 16) / 2;
        roundedFill(ctx, sfX - 3, sfY - 1, sfX + 141, sfY + 15, darkMode ? 0xFF090B0E : 0xFFFFFBEF, CONTROL_RADIUS);
        if (searchField != null && query.isEmpty() && !searchField.isFocused()) {
            drawText(ctx, font, Component.literal("Search..."), sfX, sfY + (16 - font.lineHeight) / 2, C_TEXT_DIM, false);
        }

        roundedFill(ctx, px, py + HEADER_H, px + SIDEBAR_W, py + PANEL_H, C_SIDEBAR, PANEL_RADIUS, false, false, true, false);
        ctx.fill(px + SIDEBAR_W - 1, py + HEADER_H, px + SIDEBAR_W, py + PANEL_H, C_BORDER);
        renderTabs(ctx, px, py, mx, my);
        renderContent(ctx, mx, my);
        ctx.disableScissor();

        m.popMatrix();
        super.extractRenderState(ctx, mx, my, delta);
    }

    private static final int TAB_ROW_H    = 22;
    private static final int TAB_HEADER_H = 16;
    private static final int TAB_GAP      = 2;

    private static boolean isCategoryExpanded(String category) {
        return EXPANDED.getOrDefault("cat:" + category, false);
    }

    private void layoutTabRows() {
        tabRows.clear();
        int y = 0;
        String lastCategory = null;
        for (Tab t : Tab.values()) {
            if (t.category != null && !t.category.equals(lastCategory)) {
                tabRows.add(new TabRow(null, true, t.category, y, TAB_HEADER_H));
                y += TAB_HEADER_H + TAB_GAP;
            }
            lastCategory = t.category;
            if (t.category != null) {
                float catAnim = catAnimOf(t.category);
                if (catAnim <= 0.002f) continue;
                int h   = Math.round(TAB_ROW_H * catAnim);
                int gap = Math.round(TAB_GAP * catAnim);
                tabRows.add(new TabRow(t, false, t.label, y, h));
                y += h + gap;
                continue;
            }
            tabRows.add(new TabRow(t, false, t.label, y, TAB_ROW_H));
            y += TAB_ROW_H + TAB_GAP;
        }
    }

    private int tabListVisibleH() { return PANEL_H - HEADER_H - 6 - 4; }

    private void renderTabs(GuiGraphicsExtractor ctx, int px, int py, int mx, int my) {
        layoutTabRows();
        int startY = py + HEADER_H + 6;

        int totalH = tabRows.isEmpty() ? 0 : tabRows.get(tabRows.size() - 1).y() + tabRows.get(tabRows.size() - 1).h();
        int maxScroll = Math.max(0, totalH - tabListVisibleH());
        tabScroll = Mth.clamp(tabScroll, 0, maxScroll);

        ctx.enableScissor(px, startY, px + SIDEBAR_W, py + PANEL_H);
        for (TabRow r : tabRows) {
            int tx = px, ty = startY + r.y() - tabScroll;
            if (ty + r.h() < startY || ty > py + PANEL_H) continue;

            if (r.isHeader()) {
                boolean expanded = isCategoryExpanded(r.label());
                boolean headerHover = mx >= tx && mx <= tx + SIDEBAR_W - 1 && my >= ty && my <= ty + r.h()
                        && my >= startY && my <= py + PANEL_H;
                String arrow = expanded ? "▼" : "▶";
                drawText(ctx, font, Component.literal(arrow),
                        tx + 10, ty + (r.h() - font.lineHeight) / 2, headerHover ? C_TEXT : C_TEXT_DIM);
                drawText(ctx, font, Component.literal(r.label()),
                        tx + 20, ty + (r.h() - font.lineHeight) / 2, headerHover ? C_TEXT : C_TEXT_DIM);
                continue;
            }

            boolean active = r.tab() == currentTab && query.isEmpty();
            boolean hover  = !active && mx >= tx && mx <= tx + SIDEBAR_W - 1 && my >= ty && my <= ty + r.h()
                    && my >= startY && my <= py + PANEL_H;
            if (active) {
                ctx.fill(tx, ty, tx + SIDEBAR_W - 1, ty + r.h(), C_TAB_ACTIVE);
                ctx.fill(tx, ty, tx + 2, ty + r.h(), C_ACCENT);
            } else if (hover) {
                ctx.fill(tx, ty, tx + SIDEBAR_W - 1, ty + r.h(), C_TAB_HOVER);
            }
            drawText(ctx, font, Component.literal(r.label()),
                    tx + 14, ty + (r.h() - font.lineHeight) / 2,
                    active ? C_ACCENT : hover ? C_TEXT : C_TEXT_DIM);
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            int barX = px + SIDEBAR_W - 3;
            int barH = Math.max(16, (int) ((float) tabListVisibleH() / totalH * (py + PANEL_H - startY)));
            int barY = startY + (int) ((float) tabScroll / maxScroll * (py + PANEL_H - startY - barH));
            ctx.fill(barX, startY, barX + 2, py + PANEL_H, overlay(0x22));
            ctx.fill(barX, barY, barX + 2, barY + barH, overlay(0x88));
        }
    }

    private void renderContent(GuiGraphicsExtractor ctx, int mx, int my) {
        int cx = cx(), cy = cy(), cw = cw(), ch = ch();

        if (outgoingRows != null && tabOutT < 1f) {
            renderOutgoingRows(ctx, cx, cy, cw, ch);
            sbMaxScroll = 0;
            return;
        }

        List<RenderRow> rows = layoutRows();

        if (rows.isEmpty()) {
            drawCenteredText(ctx, font,
                    Component.literal(query.isEmpty() ? "No features" : "No results"),
                    cx + cw / 2, cy + ch / 2, C_TEXT_DIM);
            sbMaxScroll = 0;
            return;
        }

        int totalH = 0;
        for (RenderRow r : rows) totalH = Math.max(totalH, (r.y + scroll) + r.h);
        totalH -= (cy + PAD);
        int maxScroll = Math.max(0, totalH - ch + PAD * 2);
        if (scroll > maxScroll) { scroll = maxScroll; rows = layoutRows(); }
        if (scroll < 0)         { scroll = 0;         rows = layoutRows(); }

        boolean flyingIn = tabInT < 1f;

        ctx.enableScissor(cx, cy, cx + cw, cy + ch);

        for (int i = 0; i < rows.size(); i++) {
            RenderRow r = rows.get(i);
            if (!(r.node instanceof Group g)) continue;
            if (animOf(g) <= 0.002f) continue;
            int blockTop = r.y;
            int blockBot = r.y + r.h;
            for (int j = i + 1; j < rows.size(); j++) {
                if (rows.get(j).depth <= r.depth) break;
                blockBot = rows.get(j).y + rows.get(j).h;
            }
            int bx = r.x, bRight = cx + cw - PAD;
            int bb = blockBot + Math.round((GROUP_PAD_B + ROW_GAP) * animOf(g));
            ctx.fill(bx, blockTop, bRight, bb, C_GROUP_BG);
            ctx.fill(bx, blockTop, bx + 2, bb, C_GROUP_BAR);   // left bar (amber)
            ctx.fill(bx, bb - 1, bRight, bb, overlay(0x22));   // subtle bottom border
        }

        String hoverTooltip = null;

        for (int i = 0; i < rows.size(); i++) {
            RenderRow r = rows.get(i);
            if (r.h <= 0) continue;
            int top = Math.max(cy, r.y);
            int bot = Math.min(cy + ch, r.y + r.h);
            if (bot <= top) continue;

            ctx.enableScissor(cx, top, cx + cw, bot);

            int rowOffsetX = 0;
            int rowFadeA   = 0;
            if (flyingIn) {
                int   stagger  = Math.min(i, TAB_IN_MAX_STAGGER_ROWS);
                float rowT     = Mth.clamp((tabInElapsed - stagger * TAB_IN_STAGGER) * TAB_IN_SPEED, 0f, 1f);
                float rowEase  = rowT * rowT * (3f - 2f * rowT);
                rowOffsetX = Math.round((1f - rowEase) * -TAB_IN_DIST);
                rowFadeA   = (int) ((1f - rowEase) * 235) << 24;
            }

            int rx = r.x + rowOffsetX;
            int rw = r.w;

            boolean rowHovered = !flyingIn && mx >= rx && mx < cx + cw - PAD && my >= top && my < bot;

            if (r.node instanceof Group g) {
                renderGroupHeader(ctx, g, rx, r.y, rw, mx, my);
                if (rowHovered && g.tooltip != null) hoverTooltip = g.tooltip;
            } else if (r.node instanceof Leaf lf) {
                ctx.fill(rx, r.y, cx + cw - PAD, r.y + r.contentH, overlay(0x0A));
                lf.f.render(ctx, this, rx, r.y, rw, mx, my);
                if (rowHovered && lf.f.tooltip != null) hoverTooltip = lf.f.tooltip;
            }

            if (rowFadeA != 0) {
                ctx.fill(cx, top, cx + cw, bot, (C_BG & 0xFFFFFF) | rowFadeA);
            }

            ctx.disableScissor();
        }
        ctx.disableScissor();

        if (hoverTooltip != null) {
            drawTooltip(ctx, hoverTooltip, mx, my);
        }

        sbMaxScroll = maxScroll;
        if (maxScroll > 0) {
            int sbX = cx + cw - 5;
            sbTrackTop = cy + PAD;
            sbTrackH   = ch - PAD * 2;
            sbThumbH   = Math.max(18, (int)((long) sbTrackH * sbTrackH / (totalH + PAD * 2)));
            sbThumbY   = sbTrackTop + (int)((float) scroll / maxScroll * (sbTrackH - sbThumbH));
            boolean hov = mx >= sbX - 1 && mx <= sbX + 4 && my >= sbTrackTop && my <= sbTrackTop + sbTrackH;
            ctx.fill(sbX, sbTrackTop, sbX + 3, sbTrackTop + sbTrackH, overlay(0x1A));
            ctx.fill(sbX, sbThumbY, sbX + 3, sbThumbY + sbThumbH,
                    (draggingScroll || hov) ? 0xCCFFAA00 : 0x77AABBDD);
        }
    }

    private void renderOutgoingRows(GuiGraphicsExtractor ctx, int cx, int cy, int cw, int ch) {
        float ease     = tabOutT * tabOutT * (3f - 2f * tabOutT);
        int   offsetX  = Math.round(ease * TAB_OUT_DIST);
        int   fadeA    = (int) (ease * 235) << 24;

        ctx.enableScissor(cx, cy, cx + cw, cy + ch);
        for (RenderRow r : outgoingRows) {
            if (r.h <= 0) continue;
            int top = Math.max(cy, r.y);
            int bot = Math.min(cy + ch, r.y + r.h);
            if (bot <= top) continue;

            ctx.enableScissor(cx, top, cx + cw, bot);
            int rx = r.x + offsetX;
            if (r.node instanceof Group g) {
                renderGroupHeader(ctx, g, rx, r.y, r.w, -1, -1);
            } else if (r.node instanceof Leaf lf) {
                lf.f.render(ctx, this, rx, r.y, r.w, -1, -1);
            }
            ctx.fill(cx, top, cx + cw, bot, (C_BG & 0xFFFFFF) | fadeA);
            ctx.disableScissor();
        }
        ctx.disableScissor();
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int ra = Math.round(aa + (ba - aa) * t);
        int rr = Math.round(ar + (br - ar) * t);
        int rg = Math.round(ag + (bg - ag) * t);
        int rb = Math.round(ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    // ── Rounded rectangles ───────────────────────────────────────────────────────
    // Corners are a real anti-aliased circle baked into a texture (generated once offline with
    // Graphics2D, not drawn per-pixel), blitted with the fill colour as a GPU tint. This gives a
    // genuinely smooth curve at any size instead of a stepped pixel approximation — the straight
    // edges/centre are still plain flat fills since there's nothing to smooth there.

    private static final Identifier CORNER_TL = Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/corner_tl.png");
    private static final Identifier CORNER_TR = Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/corner_tr.png");
    private static final Identifier CORNER_BL = Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/corner_bl.png");
    private static final Identifier CORNER_BR = Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/corner_br.png");
    private static final int CORNER_TEX_SIZE = 64;

    private static void roundedFill(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color, int radius) {
        roundedFill(ctx, x1, y1, x2, y2, color, radius, true, true, true, true);
    }

    private static void roundedFill(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color, int radius,
                                      boolean roundTL, boolean roundTR, boolean roundBL, boolean roundBR) {
        int r = Math.max(0, Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
        if (r <= 0 || !(roundTL || roundTR || roundBL || roundBR)) { ctx.fill(x1, y1, x2, y2, color); return; }

        ctx.fill(x1 + r, y1, x2 - r, y2, color);
        ctx.fill(x1, y1 + r, x1 + r, y2 - r, color);
        ctx.fill(x2 - r, y1 + r, x2, y2 - r, color);

        cornerTile(ctx, x1,     y1,     r, roundTL, CORNER_TL, color);
        cornerTile(ctx, x2 - r, y1,     r, roundTR, CORNER_TR, color);
        cornerTile(ctx, x1,     y2 - r, r, roundBL, CORNER_BL, color);
        cornerTile(ctx, x2 - r, y2 - r, r, roundBR, CORNER_BR, color);
    }

    private static void cornerTile(GuiGraphicsExtractor ctx, int x, int y, int r, boolean rounded, Identifier tex, int color) {
        if (!rounded) { ctx.fill(x, y, x + r, y + r, color); return; }
        ctx.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, r, r,
                CORNER_TEX_SIZE, CORNER_TEX_SIZE, CORNER_TEX_SIZE, CORNER_TEX_SIZE, color);
    }

    private static void roundedFillRadial(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2,
                                           double centerX, double centerY, double radius,
                                           int colorNear, int colorFar, int radiusCorner,
                                           boolean roundTL, boolean roundTR, boolean roundBL, boolean roundBR) {
        int r = Math.max(0, Math.min(radiusCorner, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
        if (r <= 0 || !(roundTL || roundTR || roundBL || roundBR)) {
            radialCellFill(ctx, x1, y1, x2, y2, centerX, centerY, radius, colorNear, colorFar);
            return;
        }

        radialCellFill(ctx, x1 + r, y1,     x2 - r, y2,     centerX, centerY, radius, colorNear, colorFar);
        radialCellFill(ctx, x1,     y1 + r, x1 + r, y2 - r, centerX, centerY, radius, colorNear, colorFar);
        radialCellFill(ctx, x2 - r, y1 + r, x2,     y2 - r, centerX, centerY, radius, colorNear, colorFar);

        cornerTile(ctx, x1,     y1,     r, roundTL, CORNER_TL, radialSample(x1 + r / 2.0,     y1 + r / 2.0,     centerX, centerY, radius, colorNear, colorFar));
        cornerTile(ctx, x2 - r, y1,     r, roundTR, CORNER_TR, radialSample(x2 - r / 2.0,     y1 + r / 2.0,     centerX, centerY, radius, colorNear, colorFar));
        cornerTile(ctx, x1,     y2 - r, r, roundBL, CORNER_BL, radialSample(x1 + r / 2.0,     y2 - r / 2.0,     centerX, centerY, radius, colorNear, colorFar));
        cornerTile(ctx, x2 - r, y2 - r, r, roundBR, CORNER_BR, radialSample(x2 - r / 2.0,     y2 - r / 2.0,     centerX, centerY, radius, colorNear, colorFar));
    }

    private static final int RADIAL_CELL = 14;

    private static void radialCellFill(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2,
                                        double centerX, double centerY, double radius, int colorNear, int colorFar) {
        for (int gy = y1; gy < y2; gy += RADIAL_CELL) {
            int cellH = Math.min(RADIAL_CELL, y2 - gy);
            for (int gx = x1; gx < x2; gx += RADIAL_CELL) {
                int cellW = Math.min(RADIAL_CELL, x2 - gx);
                int color = radialSample(gx + cellW / 2.0, gy + cellH / 2.0, centerX, centerY, radius, colorNear, colorFar);
                ctx.fill(gx, gy, gx + cellW, gy + cellH, color);
            }
        }
    }

    private static int radialSample(double x, double y, double centerX, double centerY, double radius, int colorNear, int colorFar) {
        double dist = Math.hypot(x - centerX, y - centerY);
        float t = (float) Math.max(0.0, Math.min(1.0, dist / radius));
        return lerpColor(colorNear, colorFar, t);
    }

    private static int shade(int argb, int delta) {
        int a = (argb >>> 24) & 0xFF;
        int r = clampByte(((argb >> 16) & 0xFF) + delta);
        int g = clampByte(((argb >> 8) & 0xFF) + delta);
        int b = clampByte((argb & 0xFF) + delta);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clampByte(int v) { return Math.max(0, Math.min(255, v)); }

    private static final int CONTROL_RADIUS = 5;

    private static final int TOOLTIP_MAX_W = 200;

    private void drawTooltip(GuiGraphicsExtractor ctx, String text, int mx, int my) {
        List<String> lines = wrapTooltip(text, TOOLTIP_MAX_W);
        int lineH = font.lineHeight + 2;
        int tw = 0;
        for (String l : lines) tw = Math.max(tw, font.width(l));

        int boxW = tw + 8;
        int boxH = lines.size() * lineH + 4;
        int tx = mx < px() + PANEL_W / 2 ? mx + 12 : mx - boxW - 12;
        tx = Mth.clamp(tx, px() + 4, px() + PANEL_W - boxW - 4);
        int ty = my - boxH - 6;
        if (ty < 0) ty = my + 16;

        roundedFill(ctx, tx - 4, ty - 3, tx + boxW + 4, ty + boxH + 3,
                darkMode ? 0xEC0A0A0E : 0xF0FFFBEF, 4);
        ctx.fill(tx - 4, ty - 3, tx + boxW + 4, ty - 2, C_GROUP_BAR);

        int ly = ty;
        for (String l : lines) {
            drawText(ctx, font, Component.literal(l), tx, ly, C_TEXT, false);
            ly += lineH;
        }
    }

    private List<String> wrapTooltip(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = cur.isEmpty() ? word : cur + " " + word;
            if (font.width(test) > maxWidth && !cur.isEmpty()) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(test);
            }
        }
        if (!cur.isEmpty()) lines.add(cur.toString());
        return lines;
    }

    private void renderGroupHeader(GuiGraphicsExtractor ctx, Group g,
                                   int x, int y, int w, int mx, int my) {
        boolean expanded = isExpanded(g);
        float a = animOf(g);
        boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + ROW_H;

        ctx.fill(x, y, x + w, y + ROW_H, hov ? overlay(0x14) : overlay(0x0C));
        ctx.fill(x, y, x + w, y + 1, overlay(0x22));

        String arrow = a > 0.5f ? "▾" : "▸";
        drawText(ctx, font, Component.literal(arrow),
                x + 6, y + (ROW_H - font.lineHeight) / 2, expanded ? C_ACCENT : C_TEXT_DIM);

        drawText(ctx, font, Component.literal(g.name),
                x + 18, y + (ROW_H - font.lineHeight) / 2,
                expanded ? C_ACCENT : C_TEXT);

        if (g.get != null && g.set != null) {
            boolean on = g.get.get();
            float knobAnim = GROUP_TOGGLE_ANIM.getOrDefault(g.key, on ? 1f : 0f);
            int pw = 34, ph = 12, px = x + w - pw - 8, py = y + (ROW_H - ph) / 2;
            roundedFill(ctx, px, py, px + pw, py + ph, lerpColor(C_OFF, C_ON, knobAnim), ph / 2);
            int kxOff = px + 1, kxOn = px + pw - 12;
            int kx = Math.round(kxOff + (kxOn - kxOff) * knobAnim);
            roundedFill(ctx, kx, py + 1, kx + 10, py + ph - 1, 0xFFFFFFFF, (ph - 2) / 2);
        }
    }

    @Override public void removed()          { PhantomConfig.save(); }
    @Override public boolean isPauseScreen() { return false; }
}
