package com.kuudrahelper;

import com.kuudrahelper.features.customisation.items.ItemCustomization;
import com.kuudrahelper.features.customisation.items.ItemTransformSettings;
import net.minecraft.client.Minecraft;
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

public class KuudraScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────

    private static final int SIDEBAR_W = 108;
    private static final int HEADER_H  = 34;
    private static final int PANEL_W   = 400;
    private static final int PANEL_H   = 280;
    private static final int ROW_H     = 26;
    private static final int ROW_GAP   = 3;
    private static final int PAD       = 12;
    private static final int INDENT    = 12;
    private static final int GROUP_PAD_B = 6;

    // ── Colours ───────────────────────────────────────────────────────────────

    private static final Identifier LOGO =
            Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/logo.png");

    private static final int C_BG         = 0xFF15151A;
    private static final int C_SIDEBAR    = 0xFF101014;
    private static final int C_HEADER     = 0xFF1B1B1F;
    private static final int C_BORDER     = 0xFF1E2233;
    private static final int C_ACCENT     = 0xFFFFAA00;
    private static final int C_TAB_ACTIVE = 0xFF1A1F2E;
    private static final int C_TAB_HOVER  = 0xFF131720;
    private static final int C_TEXT       = 0xFFD4D8E8;
    private static final int C_TEXT_DIM   = 0xFF6B7399;
    private static final int C_ON         = 0xFF44BB77;
    private static final int C_OFF        = 0xFF2A3345;
    private static final int C_SLIDER_BG  = 0xFF1A1F2E;
    private static final int C_SLIDER_FG  = 0xFF2244AA;
    private static final int C_SLIDER_GR  = 0xFF4477DD;
    private static final int C_GROUP_BG   = 0x14FFFFFF;
    private static final int C_GROUP_BAR  = 0x55FFAA00;

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

        DUNGEONS_M7("Dungeons", "M7 AutoGFS"),

        FLUID_CUSTOM( "Customisation", "Lava & Water Customisation"),
        ITEM_CUSTOM(  "Customisation", "Item Customisation"),
        VISUAL_WORDS( "Customisation", "Visual Words");

        final String category;
        final String label;
        Tab(String category, String l) { this.category = category; this.label = l; }
    }

    private Tab   currentTab = Tab.ABOUT;
    private float tabAnim    = 1f;
    private static final float ANIM_SPEED  = 9f;
    private static final float GROUP_SPEED = 14f;

    // ── Feature base ──────────────────────────────────────────────────────────

    private abstract static class Feature {
        final String name;
        final Tab    tab;
        Feature(String n, Tab t) { name = n; tab = t; }
        abstract void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my);
        boolean onDown(double mx, double my, int x, int y, int w) { return false; }
        boolean onDrag(double mx, double my, int x, int y, int w) { return false; }
        void onUp()         {}
        void onKey(int k)   {}
        void onChar(char c) {}
        boolean isCapturing() { return false; }
        void cancel()         {}
    }

    // ── Toggle ────────────────────────────────────────────────────────────────

    private static class Toggle extends Feature {
        final Supplier<Boolean> get; final Consumer<Boolean> set;
        Toggle(String n, Tab t, Supplier<Boolean> get, Consumer<Boolean> set) {
            super(n, t); this.get = get; this.set = set;
        }
        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            boolean on = get.get();
            int pw = 34, ph = 12, px = x + w - pw - 8, py = y + (ROW_H - ph) / 2;
            ctx.fill(px, py, px + pw, py + ph, on ? C_ON : C_OFF);
            int kx = on ? px + pw - 12 : px + 1;
            ctx.fill(kx, py + 1, kx + 10, py + ph - 1, 0xFFFFFFFF);
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
        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            String val = label.get();
            int bw = s.font.width(val) + 18, bh = ROW_H - 6;
            int bx = x + w - bw - 8, by = y + 3;
            boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
            ctx.fill(bx, by, bx + bw, by + bh, hov ? 0xFF223366 : 0xFF151E33);
            ctx.fill(bx, by, bx + bw, by + 1, C_ACCENT);
            ctx.centeredText(s.font, Component.literal(val),
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
        IntInput(String n, Tab t, Supplier<Integer> get, Consumer<Integer> set) {
            super(n, t); this.get = get; this.set = set;
        }
        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            String display = draft != null ? draft : String.valueOf(get.get());
            int fw = 50, fx = x + w - fw - 8, fy = y + 3, fh = ROW_H - 6;
            ctx.fill(fx, fy, fx + fw, fy + fh, focused ? 0xFF1A2A44 : 0xFF0F1218);
            ctx.fill(fx, fy + fh - 1, fx + fw, fy + fh, focused ? C_ACCENT : C_BORDER);
            ctx.centeredText(s.font, Component.literal(display),
                    fx + fw / 2, fy + (fh - s.font.lineHeight) / 2, C_TEXT);
        }
        @Override boolean onDown(double mx, double my, int x, int y, int w) {
            int fw = 50, fx = x + w - fw - 8, fy = y + 3, fh = ROW_H - 6;
            if (mx >= fx && mx <= fx + fw && my >= fy && my <= fy + fh) {
                focused = true; draft = String.valueOf(get.get()); return true;
            }
            if (focused) { commit(); focused = false; }
            return false;
        }
        @Override void onKey(int key) {
            if (!focused) return;
            if (key == 256) { commit(); focused = false; }       // Esc saves current value
            else if (key == 257 || key == 335) { commit(); focused = false; }
            else if (key == 259 && draft != null && !draft.isEmpty())
                draft = draft.substring(0, draft.length() - 1);
        }
        @Override void onChar(char c) {
            if (focused && Character.isDigit(c)) {
                if (draft == null) draft = "";
                draft += c;
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
    // Like IntInput, but an empty field means "unset" (stored as -1) rather than being ignored —
    // used for Auto Kick thresholds where a blank field means that stat isn't checked.

    private static class OptionalIntInput extends IntInput {
        OptionalIntInput(String n, Tab t, Supplier<Integer> get, Consumer<Integer> set) {
            super(n, t, get, set);
        }
        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            int val = get.get();
            String display = draft != null ? draft : (val < 0 ? "" : String.valueOf(val));
            int fw = 50, fx = x + w - fw - 8, fy = y + 3, fh = ROW_H - 6;
            ctx.fill(fx, fy, fx + fw, fy + fh, focused ? 0xFF1A2A44 : 0xFF0F1218);
            ctx.fill(fx, fy + fh - 1, fx + fw, fy + fh, focused ? C_ACCENT : C_BORDER);
            ctx.centeredText(s.font, Component.literal(display),
                    fx + fw / 2, fy + (fh - s.font.lineHeight) / 2, C_TEXT);
        }
        @Override boolean onDown(double mx, double my, int x, int y, int w) {
            int fw = 50, fx = x + w - fw - 8, fy = y + 3, fh = ROW_H - 6;
            if (mx >= fx && mx <= fx + fw && my >= fy && my <= fy + fh) {
                focused = true;
                int val = get.get();
                draft = val < 0 ? "" : String.valueOf(val);
                return true;
            }
            if (focused) { commit(); focused = false; }
            return false;
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
        }
        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            String display = draft != null ? draft : String.valueOf(get.get());
            int fw = 64, fx = x + w - fw - 8, fy = y + 3, fh = ROW_H - 6;
            ctx.fill(fx, fy, fx + fw, fy + fh, focused ? 0xFF1A2A44 : 0xFF0F1218);
            ctx.fill(fx, fy + fh - 1, fx + fw, fy + fh, focused ? C_ACCENT : C_BORDER);
            ctx.centeredText(s.font, Component.literal(display),
                    fx + fw / 2, fy + (fh - s.font.lineHeight) / 2, C_TEXT);
        }
        @Override boolean onDown(double mx, double my, int x, int y, int w) {
            int fw = 64, fx = x + w - fw - 8, fy = y + 3, fh = ROW_H - 6;
            if (mx >= fx && mx <= fx + fw && my >= fy && my <= fy + fh) {
                focused = true; draft = String.valueOf(get.get()); return true;
            }
            if (focused) { commit(); focused = false; }
            return false;
        }
        @Override void onChar(char c) {
            if (!focused) return;
            if (c == '-' && (draft == null || draft.isEmpty())) { draft = "-"; }
            else if (Character.isDigit(c)) { if (draft == null) draft = ""; draft += c; }
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
        // cached from last render so onDown uses the same geometry
        private int lastFx, lastFy, lastFw, lastFh;
        TextInput(String n, Tab t, Supplier<String> get, Consumer<String> set) {
            super(n, t); this.get = get; this.set = set;
        }
        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            String display = draft != null ? draft : stripNamespace(get.get());
            if (display == null) display = "";
            // Give the field most of the row width — just leave the label and a small gap
            int labelW = s.font.width(name);
            int fx = x + 8 + labelW + 8, fy = y + 3, fw = w - fx + x - 8, fh = ROW_H - 6;
            if (fw < 30) fw = 30;
            lastFx = fx; lastFy = fy; lastFw = fw; lastFh = fh;
            ctx.fill(fx, fy, fx + fw, fy + fh, focused ? 0xFF1A2A44 : 0xFF0F1218);
            ctx.fill(fx, fy + fh - 1, fx + fw, fy + fh, focused ? C_ACCENT : C_BORDER);
            // Scroll to show the end of the string so the user sees what they're typing
            int maxChars = (fw - 8) / Math.max(1, s.font.width("a"));
            String shown = display.length() > maxChars
                    ? display.substring(display.length() - maxChars) : display;
            ctx.text(s.font, Component.literal(shown), fx + 4, fy + (fh - s.font.lineHeight) / 2, C_TEXT);
        }
        @Override boolean onDown(double mx, double my, int x, int y, int w) {
            if (mx >= lastFx && mx <= lastFx + lastFw && my >= lastFy && my <= lastFy + lastFh) {
                focused = true;
                String raw = get.get();
                draft = raw != null ? stripNamespace(raw) : "";
                return true;
            }
            if (focused) { commit(); focused = false; }
            return false;
        }
        @Override void onKey(int key) {
            if (!focused) return;
            if (key == 256 || key == 257 || key == 335) { commit(); focused = false; }
            else if (key == 259 && draft != null && !draft.isEmpty())
                draft = draft.substring(0, draft.length() - 1);
        }
        @Override void onChar(char c) {
            if (focused) { if (draft == null) draft = ""; draft += c; }
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
    // Same as TextInput but without the "minecraft:" namespace add/strip behaviour
    // (that's specific to item-ID fields; this is for plain strings like an API key).

    private static class RawTextInput extends Feature {
        final Supplier<String> get; final Consumer<String> set;
        String  draft   = null;
        boolean focused = false;
        private int lastFx, lastFy, lastFw, lastFh;
        RawTextInput(String n, Tab t, Supplier<String> get, Consumer<String> set) {
            super(n, t); this.get = get; this.set = set;
        }
        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            String display = draft != null ? draft : get.get();
            if (display == null) display = "";
            int labelW = s.font.width(name);
            int fx = x + 8 + labelW + 8, fy = y + 3, fw = w - fx + x - 8, fh = ROW_H - 6;
            if (fw < 30) fw = 30;
            lastFx = fx; lastFy = fy; lastFw = fw; lastFh = fh;
            ctx.fill(fx, fy, fx + fw, fy + fh, focused ? 0xFF1A2A44 : 0xFF0F1218);
            ctx.fill(fx, fy + fh - 1, fx + fw, fy + fh, focused ? C_ACCENT : C_BORDER);
            int maxChars = (fw - 8) / Math.max(1, s.font.width("a"));
            String shown = display.length() > maxChars
                    ? display.substring(display.length() - maxChars) : display;
            ctx.text(s.font, Component.literal(shown), fx + 4, fy + (fh - s.font.lineHeight) / 2, C_TEXT);
        }
        @Override boolean onDown(double mx, double my, int x, int y, int w) {
            if (mx >= lastFx && mx <= lastFx + lastFw && my >= lastFy && my <= lastFy + lastFh) {
                focused = true;
                draft = get.get() != null ? get.get() : "";
                return true;
            }
            if (focused) { commit(); focused = false; }
            return false;
        }
        @Override void onKey(int key) {
            if (!focused) return;
            if (key == 256 || key == 257 || key == 335) { commit(); focused = false; }
            else if (key == 259 && draft != null && !draft.isEmpty())
                draft = draft.substring(0, draft.length() - 1);
        }
        @Override void onChar(char c) {
            if (focused) { if (draft == null) draft = ""; draft += c; }
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

        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
            String label = capturing ? "[ Press key or mouse... ]" : "[ " + keyName() + " ]";
            int lw = s.font.width(label);
            int bx = x + w - lw - 16, by = y + 3, bh = ROW_H - 6;
            boolean hov = !capturing && mx >= bx && mx <= bx + lw + 8 && my >= by && my <= by + bh;
            ctx.fill(bx, by, bx + lw + 8, by + bh, capturing ? 0xFF1A3344 : hov ? 0xFF223366 : 0xFF151E33);
            ctx.fill(bx, by, bx + lw + 8, by + 1, capturing ? 0xFF55AAFF : C_ACCENT);
            ctx.text(s.font, Component.literal(label),
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
        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, labelColor());
            renderValueBox(ctx, s, x, y, w);
            drawTrack(ctx, x, y, w, get.get(), trackColor());
        }
        void renderValueBox(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w) {
            String shown = editing ? (draft == null ? "" : draft) : displayValue();
            int bw = 44, bh = 12;
            int bx = x + w - 68 - bw, by = y + (ROW_H - bh) / 2;
            boxX = bx; boxY = by; boxW = bw; boxH = bh;
            if (editing) {
                ctx.fill(bx, by, bx + bw, by + bh, 0xFF1A2A44);
                ctx.fill(bx, by + bh - 1, bx + bw, by + bh, C_ACCENT);
            }
            ctx.text(s.font, Component.literal(shown),
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

    // ── LavaPreview / WaterPreview ────────────────────────────────────────────

    private static class LavaPreview extends Feature {
        LavaPreview(Tab t) { super("Colour Preview", t); }
        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            int previewArgb = com.kuudrahelper.features.customisation.lava.ColorPreviewHelper.computePreviewColor();
            int sw = w - 16, swX = x + 8, swY = y + 4, sh = ROW_H - 8;
            ctx.fill(swX, swY, swX + sw, swY + sh, previewArgb);
            ctx.fill(swX, swY, swX + sw, swY + 1, 0x22FFFFFF);
            ctx.centeredText(s.font, Component.literal("Colour Preview"),
                    swX + sw / 2, swY + (sh - s.font.lineHeight) / 2, 0xCCFFFFFF);
        }
    }

    private static class WaterPreview extends Feature {
        WaterPreview(Tab t) { super("Colour Preview", t); }
        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            int previewArgb = com.kuudrahelper.features.customisation.water.WaterColorPreviewHelper.computePreviewColor();
            int sw = w - 16, swX = x + 8, swY = y + 4, sh = ROW_H - 8;
            ctx.fill(swX, swY, swX + sw, swY + sh, previewArgb);
            ctx.fill(swX, swY, swX + sw, swY + 1, 0x22FFFFFF);
            ctx.centeredText(s.font, Component.literal("Colour Preview"),
                    swX + sw / 2, swY + (sh - s.font.lineHeight) / 2, 0xCCFFFFFF);
        }
    }

    // ── Button ────────────────────────────────────────────────────────────────

    private static class Button extends Feature {
        final String label; final Runnable action;
        Button(String n, Tab t, String label, Runnable action) {
            super(n, t); this.label = label; this.action = action;
        }
        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            int lw = s.font.width(label);
            int bw = lw + 18, bh = ROW_H - 6, bx = x + w - bw - 8, by = y + 3;
            boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
            ctx.fill(bx, by, bx + bw, by + bh, hov ? 0xFF2A3350 : 0xFF151E33);
            ctx.fill(bx, by, bx + bw, by + 1, C_ACCENT);
            ctx.centeredText(s.font, Component.literal(label),
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

        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal("Match:"),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
            int bw = 46, bx = x + w - bw - 12;
            int fy = y + 3, fh = ROW_H - 6;
            int fx = x + 60, fw = bx - fx - 4;
            ctx.fill(fx, fy, fx + fw, fy + fh, focused ? 0xFF1A2A44 : 0xFF0F1218);
            ctx.fill(fx, fy + fh - 1, fx + fw, fy + fh, focused ? C_ACCENT : C_BORDER);
            String disp = draft.isEmpty() && !focused ? "§7type to filter..." : draft;
            ctx.text(s.font, Component.literal(disp),
                    fx + 4, fy + (fh - s.font.lineHeight) / 2, C_TEXT);
            boolean hov = mx >= bx && mx <= bx + bw && my >= fy && my <= fy + fh;
            ctx.fill(bx, fy, bx + bw, fy + fh, hov ? 0xFF1A3322 : 0xFF0F1E14);
            ctx.fill(bx, fy, bx + bw, fy + 1, 0xFF44BB77);
            ctx.centeredText(s.font, Component.literal("+ Add"),
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
                KuudraConfig.save();
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

        private com.kuudrahelper.features.customisation.VisualWords.Rule rule() {
            var rules = com.kuudrahelper.features.customisation.VisualWords.getRules();
            return idx >= 0 && idx < rules.size() ? rules.get(idx) : null;
        }

        // geometry
        private int removeX(int x, int w) { return x + w - 18; }
        private int inputX(int x)         { return x + 6; }
        private int fieldW(int x, int w)  { return (removeX(x, w) - inputX(x) - 14) / 2; }
        private int arrowX(int x, int w)  { return inputX(x) + fieldW(x, w) + 2; }
        private int replX(int x, int w)   { return arrowX(x, w) + 10; }

        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            var r = rule();
            if (r == null) return;
            int fy = y + 4, fh = ROW_H - 8;
            int fw = fieldW(x, w);

            String inText  = focus == 1 ? draft : r.input;
            String repText = focus == 2 ? draft : r.replacement;

            drawField(ctx, s, inputX(x), fy, fw, fh, inText, focus == 1, "find");
            ctx.centeredText(s.font, Component.literal("→"),
                    arrowX(x, w) + 4, fy + (fh - s.font.lineHeight) / 2, C_TEXT_DIM);
            drawField(ctx, s, replX(x, w), fy, fw, fh, repText, focus == 2, "replace");

            int rx = removeX(x, w);
            boolean hov = mx >= rx && mx <= rx + 14 && my >= fy && my <= fy + fh;
            ctx.fill(rx, fy, rx + 14, fy + fh, hov ? 0xFF4A1515 : 0xFF2A0F0F);
            ctx.centeredText(s.font, Component.literal("✕"),
                    rx + 7, fy + (fh - s.font.lineHeight) / 2, 0xFFCC3333);
        }

        private void drawField(GuiGraphicsExtractor ctx, KuudraScreen s, int fx, int fy, int fw, int fh,
                               String text, boolean foc, String hint) {
            ctx.fill(fx, fy, fx + fw, fy + fh, foc ? 0xFF1A2A44 : 0xFF0F1218);
            ctx.fill(fx, fy + fh - 1, fx + fw, fy + fh, foc ? C_ACCENT : C_BORDER);
            String disp = (text == null || text.isEmpty()) && !foc ? "§7" + hint : (text == null ? "" : text);
            ctx.text(s.font, Component.literal(disp),
                    fx + 4, fy + (fh - s.font.lineHeight) / 2, C_TEXT);
        }

        @Override boolean onDown(double mx, double my, int x, int y, int w) {
            var r = rule();
            if (r == null) return false;
            int fy = y + 4, fh = ROW_H - 8, fw = fieldW(x, w);
            int rx = removeX(x, w);
            if (mx >= rx && mx <= rx + 14 && my >= fy && my <= fy + fh) {
                com.kuudrahelper.features.customisation.VisualWords.removeRule(idx);
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
                com.kuudrahelper.features.customisation.VisualWords.save();
            }
            draft = null; focus = 0;
        }
    }

    // ── RgbInput ───────────────────────────────────────────────────────────────

    /** One row: label + colour swatch + three editable R/G/B fields (packed 0xRRGGBB). */
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

        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            int swX = fx(x, w, 0) - 16, swY = y + (ROW_H - 12) / 2;
            ctx.fill(swX, swY, swX + 12, swY + 12, 0xFF000000 | (get.get() & 0xFFFFFF));
            for (int i = 0; i < 3; i++) {
                int fxv = fx(x, w, i), fy = y + 3, fh = ROW_H - 6;
                boolean foc = focus == i + 1;
                ctx.fill(fxv, fy, fxv + FW, fy + fh, foc ? 0xFF1A2A44 : 0xFF0F1218);
                ctx.fill(fxv, fy + fh - 1, fxv + FW, fy + fh, foc ? C_ACCENT : C_BORDER);
                String disp = foc ? (draft == null ? "" : draft) : String.valueOf(comp(i));
                ctx.centeredText(s.font, Component.literal(disp),
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
        Group(String name, Tab tab, String key, Supplier<Boolean> get, Consumer<Boolean> set) {
            super(tab); this.name = name; this.key = key; this.get = get; this.set = set;
        }
        Group add(Node n) { children.add(n); return this; }
    }

    private static class RenderRow {
        Node node; int x, y, w, h, contentH, depth;
    }

    private static final Map<String, Boolean> EXPANDED = new HashMap<>();
    private static final Map<String, Float>   ANIM     = new HashMap<>();

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
        buildDungeonsTab();
        buildFluidCustomTab();
        buildItemCustomTab();
        buildVisualWordsTab();
    }

    // ── Stun / DPS tab ──────────────────────────────────────────────────────────

    private void buildStunDpsTab() {
        Tab T = Tab.STUN_DPS;

        Group gfs = group("Auto GFS", T, null,
                KuudraConfig::isAutoGfsEnabled, KuudraConfig::setAutoGfsEnabled);
        gfs.add(leaf(new Cycle("Role Mode", T,
                () -> KuudraConfig.getRoleMode().name(),
                () -> KuudraConfig.setRoleMode(switch (KuudraConfig.getRoleMode()) {
                    case DPS  -> KuudraConfig.RoleMode.STUN;
                    case STUN -> KuudraConfig.RoleMode.AUTO;
                    case AUTO -> KuudraConfig.RoleMode.DPS;
                }))));
        gfs.add(leaf(new IntInput("DPS Amount",  T, KuudraConfig::getDpsValue,  KuudraConfig::setDpsValue)));
        gfs.add(leaf(new IntInput("Stun Amount", T, KuudraConfig::getStunValue, KuudraConfig::setStunValue)));
        gfs.add(leaf(new IntInput("Refill Amount", T, KuudraConfig::getDpsRefillAmount, KuudraConfig::setDpsRefillAmount)));
        gfs.add(leaf(new RangeSlider("Disable Refill Below HP", T, 25, 100, "%.0f%%",
                () -> (float) KuudraConfig.getAutoGfsDisableHpPercent(),
                v  -> KuudraConfig.setAutoGfsDisableHpPercent(Math.round(v)))));
        roots.add(gfs);

        roots.add(leaf(new Toggle("Pickobulus Blocker", T,
                KuudraConfig::isPickoblockEnabled, KuudraConfig::setPickoblockEnabled)));

        Group eaten = group("Eaten Timer", T, null,
                KuudraConfig::isEatenTimerEnabled, KuudraConfig::setEatenTimerEnabled);
        eaten.add(leaf(new Toggle("Subtract Ping", T,
                KuudraConfig::isEatenTimerSubtractPingEnabled, KuudraConfig::setEatenTimerSubtractPingEnabled)));
        roots.add(eaten);

        roots.add(leaf(new Toggle("Cannon Auto Close", T,
                KuudraConfig::isCannonAutoCloseEnabled, KuudraConfig::setCannonAutoCloseEnabled)));
        roots.add(leaf(new Toggle("Stun Preview", T,
                KuudraConfig::isStunPreviewEnabled, KuudraConfig::setStunPreviewEnabled)));

        Group fastDps = group("Fast DPS Warning", T, null,
                KuudraConfig::isFastDpsWarningEnabled, KuudraConfig::setFastDpsWarningEnabled);
        fastDps.add(leaf(new Toggle("Fast DPS Notification", T,
                KuudraConfig::isFastDpsNotifyEnabled, KuudraConfig::setFastDpsNotifyEnabled)));
        fastDps.add(soundGroup(T, KuudraConfig.SOUND_FAST_DPS));
        roots.add(fastDps);
    }

    // ── Build tab ───────────────────────────────────────────────────────────────

    private void buildBuildTab() {
        Tab T = Tab.BUILD;
        roots.add(leaf(new Toggle("Build Progress Tracker", T,
                KuudraConfig::isBuildProgressHudEnabled, KuudraConfig::setBuildProgressHudEnabled)));

        Group announce = group("Announce Fresh", T, null,
                KuudraConfig::isAnnounceFreshEnabled, KuudraConfig::setAnnounceFreshEnabled);
        announce.add(leaf(new Toggle("Fresh Notification", T,
                KuudraConfig::isFreshNotifyEnabled, KuudraConfig::setFreshNotifyEnabled)));
        announce.add(soundGroup(T, KuudraConfig.SOUND_FRESH));
        roots.add(announce);

        Group buildStarted = group("Build Started Notification", T, null,
                KuudraConfig::isBuildStartedNotifyEnabled, KuudraConfig::setBuildStartedNotifyEnabled);
        buildStarted.add(soundGroup(T, KuudraConfig.SOUND_BUILD_STARTED));
        roots.add(buildStarted);
        Group buildBeacons = group("Build Beacons", T, null,
                KuudraConfig::isBuildBeaconsEnabled, KuudraConfig::setBuildBeaconsEnabled);
        buildBeacons.add(leaf(new Slider("Opacity", T,
                KuudraConfig::getBuildBeaconAlpha, KuudraConfig::setBuildBeaconAlpha, "%")));
        roots.add(buildBeacons);

        roots.add(leaf(new Toggle("Elle Highlight", T,
                KuudraConfig::isElleHighlightEnabled, KuudraConfig::setElleHighlightEnabled)));
    }

    // ── Supplies tab ──────────────────────────────────────────────────────────

    private void buildSuppliesTab() {
        Tab T = Tab.SUPPLIES;
        roots.add(leaf(new Toggle("Supply Beacons", T,
                KuudraConfig::isSupplyBeaconsEnabled, KuudraConfig::setSupplyBeaconsEnabled)));

        Group noPre = group("No Pre Announce", T, null,
                KuudraConfig::isNoPreAnnounceEnabled, KuudraConfig::setNoPreAnnounceEnabled);
        noPre.add(leaf(new Toggle("No Pre Notification", T,
                KuudraConfig::isNoPreNotifyEnabled, KuudraConfig::setNoPreNotifyEnabled)));
        noPre.add(soundGroup(T, KuudraConfig.SOUND_NO_PRE));
        roots.add(noPre);

        Group supplyGrabbed = group("Supply Grabbed Notification", T, null,
                KuudraConfig::isSupplyGrabbedNotifyEnabled, KuudraConfig::setSupplyGrabbedNotifyEnabled);
        supplyGrabbed.add(soundGroup(T, KuudraConfig.SOUND_SUPPLY_GRABBED));
        roots.add(supplyGrabbed);

        Group supplyDropped = group("Supply Dropped Notification", T, null,
                KuudraConfig::isSupplyDroppedNotifyEnabled, KuudraConfig::setSupplyDroppedNotifyEnabled);
        supplyDropped.add(soundGroup(T, KuudraConfig.SOUND_SUPPLY_DROPPED));
        roots.add(supplyDropped);

        roots.add(leaf(new Toggle("Crate Priority", T,
                KuudraConfig::isCratePriorityEnabled, KuudraConfig::setCratePriorityEnabled)));
        roots.add(leaf(new Toggle("Supply Recovery Message", T,
                KuudraConfig::isSupplyRecoveryMsgEnabled, KuudraConfig::setSupplyRecoveryMsgEnabled)));
        roots.add(leaf(new Toggle("Supply Location Announce", T,
                KuudraConfig::isSupplyLocationAnnounceEnabled, KuudraConfig::setSupplyLocationAnnounceEnabled)));
        roots.add(leaf(new Toggle("Supply Hitbox", T,
                KuudraConfig::isSupplyHitboxEnabled, KuudraConfig::setSupplyHitboxEnabled)));
        roots.add(leaf(new Toggle("Supply Rod Radius", T,
                KuudraConfig::isSupplyRodRadiusEnabled, KuudraConfig::setSupplyRodRadiusEnabled)));
        roots.add(leaf(new Toggle("Supply Pearl Hitbox", T,
                KuudraConfig::isSupplyPearlHitboxEnabled, KuudraConfig::setSupplyPearlHitboxEnabled)));
        roots.add(leaf(new Toggle("Supply Giant Hitbox Alert", T,
                KuudraConfig::isSupplyGiantHitboxEnabled, KuudraConfig::setSupplyGiantHitboxEnabled)));

        Group giantHitbox = group("Giant Hitbox", T, null,
                KuudraConfig::isGiantHitboxEnabled, KuudraConfig::setGiantHitboxEnabled);
        giantHitbox.add(leaf(new Toggle("Filled", T,
                KuudraConfig::isGiantHitboxFilled, KuudraConfig::setGiantHitboxFilled)));
        giantHitbox.add(leaf(new Slider("Fill Opacity", T,
                KuudraConfig::getGiantHitboxFillOpacity, KuudraConfig::setGiantHitboxFillOpacity, "")));
        giantHitbox.add(leaf(new RgbInput("Colour", T,
                KuudraConfig::getGiantHitboxColor, KuudraConfig::setGiantHitboxColor)));
        roots.add(giantHitbox);

        roots.add(leaf(new Toggle("Lava Bobber Fix", T,
                KuudraConfig::isLavaBobberFixEnabled, KuudraConfig::setLavaBobberFixEnabled)));
        roots.add(leaf(new Toggle("Legacy Rod Physics", T,
                KuudraConfig::isLegacyRodPhysicsEnabled, KuudraConfig::setLegacyRodPhysicsEnabled)));
        roots.add(leaf(new Toggle("Etherwarp Waypoints", T,
                KuudraConfig::isEtherwarpWaypointsEnabled, KuudraConfig::setEtherwarpWaypointsEnabled)));
        roots.add(leaf(new Toggle("Block Slot 9", T,
                KuudraConfig::isBlockSlot9Enabled, KuudraConfig::setBlockSlot9Enabled)));

        // Pearl waypoints — everything related lives under this dropdown
        Group wp = group("Dynamic Waypoints", T, null,
                KuudraConfig::isPearlWaypointsEnabled, KuudraConfig::setPearlWaypointsEnabled);
        wp.add(leaf(new Toggle("Show All Waypoints", T,
                KuudraConfig::isShowAllWaypoints, KuudraConfig::setShowAllWaypoints)));
        wp.add(leaf(new Toggle("Flat Pearls", T,
                KuudraConfig::isPearlFlatEnabled, KuudraConfig::setPearlFlatEnabled)));
        wp.add(leaf(new Toggle("Sky Pearls", T,
                KuudraConfig::isPearlSkyEnabled, KuudraConfig::setPearlSkyEnabled)));
        wp.add(leaf(new Toggle("Double Pearls", T,
                KuudraConfig::isPearlDoubleEnabled, KuudraConfig::setPearlDoubleEnabled)));
        wp.add(leaf(new RangeSlider("Double Pearl Delay", T, 0.05f, 0.55f, "%.2fs",
                KuudraConfig::getDoublePearlDelayS, KuudraConfig::setDoublePearlDelayS)));
        wp.add(leaf(new Cycle("Waypoint Type", T,
                () -> KuudraConfig.getWaypointType().name().charAt(0)
                        + KuudraConfig.getWaypointType().name().substring(1).toLowerCase(),
                () -> KuudraConfig.setWaypointType(
                        KuudraConfig.getWaypointType() == KuudraConfig.WaypointType.CIRCLE
                                ? KuudraConfig.WaypointType.SQUARE
                                : KuudraConfig.WaypointType.CIRCLE))));
        wp.add(leaf(new Toggle("Waypoint Fill", T,
                KuudraConfig::isWaypointFillEnabled, KuudraConfig::setWaypointFillEnabled)));
        wp.add(leaf(new Cycle("Update Frequency", T,
                () -> KuudraConfig.isPearlTickUpdate() ? "Per Tick" : "Per Frame",
                () -> KuudraConfig.setPearlTickUpdate(!KuudraConfig.isPearlTickUpdate()))));
        wp.add(leaf(new Toggle("Drop Locations", T,
                KuudraConfig::isDropLocationsEnabled, KuudraConfig::setDropLocationsEnabled)));
        wp.add(leaf(new Toggle("Pearl Timer", T,
                KuudraConfig::isPearlTimerEnabled, KuudraConfig::setPearlTimerEnabled)));
        wp.add(leaf(new Slider("Timer Height",   T, KuudraConfig::getPearlTimerHeight, KuudraConfig::setPearlTimerHeight, "")));
        wp.add(leaf(new Slider("Timer Size",     T, KuudraConfig::getPearlTimerSize,   KuudraConfig::setPearlTimerSize,   "")));
        wp.add(leaf(new Slider("Waypoint Size",  T, KuudraConfig::getPearlCircleSize,  KuudraConfig::setPearlCircleSize,  "")));
        wp.add(leaf(new Slider("Fill Opacity",   T, KuudraConfig::getWaypointFillAlpha,KuudraConfig::setWaypointFillAlpha,"%")));
        wp.add(leaf(new Slider("Beacon Opacity", T, KuudraConfig::getBeaconAlpha,      KuudraConfig::setBeaconAlpha,      "%")));

        Group wpCol = group("Waypoint Colours", T, wp.key, null, null);
        wpCol.add(leaf(new RgbInput("Normal target",  T, KuudraConfig::getWpColNormal,  KuudraConfig::setWpColNormal)));
        wpCol.add(leaf(new RgbInput("Correct target", T, KuudraConfig::getWpColCorrect, KuudraConfig::setWpColCorrect)));
        wpCol.add(leaf(new RgbInput("Hovered target", T, KuudraConfig::getWpColHovered, KuudraConfig::setWpColHovered)));
        wpCol.add(leaf(new RgbInput("Ready target",   T, KuudraConfig::getWpColReady,   KuudraConfig::setWpColReady)));
        wp.add(wpCol);

        Group bcnCol = group("Beacon Colours", T, wp.key, null, null);
        bcnCol.add(leaf(new RgbInput("Normal target",  T, KuudraConfig::getBeaconColNormal,  KuudraConfig::setBeaconColNormal)));
        bcnCol.add(leaf(new RgbInput("Correct target", T, KuudraConfig::getBeaconColCorrect, KuudraConfig::setBeaconColCorrect)));
        wp.add(bcnCol);
        wp.add(soundGroup(T, KuudraConfig.SOUND_PEARL_NOW));

        roots.add(wp);

        Group wpLines = group("Waypoint Lines", T, null,
                KuudraConfig::isWaypointLinesEnabled, KuudraConfig::setWaypointLinesEnabled);
        wpLines.add(leaf(new Toggle("Flat Pearls", T,
                KuudraConfig::isWaypointLinesFlatPearlsEnabled, KuudraConfig::setWaypointLinesFlatPearlsEnabled)));
        wpLines.add(leaf(new Toggle("Supplies", T,
                KuudraConfig::isWaypointLinesSuppliesEnabled, KuudraConfig::setWaypointLinesSuppliesEnabled)));
        wpLines.add(leaf(new Cycle("Second Supply Preference", T,
                () -> KuudraConfig.getSecondSupplyPreference() == KuudraConfig.SecondSupplyPreference.DOUBLE_PEARL
                        ? "Double Pearls" : "Etherwarp Waypoints",
                () -> KuudraConfig.setSecondSupplyPreference(
                        KuudraConfig.getSecondSupplyPreference() == KuudraConfig.SecondSupplyPreference.DOUBLE_PEARL
                                ? KuudraConfig.SecondSupplyPreference.ETHERWARP
                                : KuudraConfig.SecondSupplyPreference.DOUBLE_PEARL))));
        roots.add(wpLines);

        roots.add(leaf(new Toggle("Smooth Crate Pickup", T,
                KuudraConfig::isSmoothCratePickupEnabled, KuudraConfig::setSmoothCratePickupEnabled)));

        roots.add(leaf(new Cycle("Kuudra Talisman", T,
                () -> switch (KuudraConfig.getKuudraTalisman()) {
                    case NONE   -> "None";   case KIDNEY -> "Kidney";
                    case LUNG   -> "Lung";   case HEART  -> "Heart"; },
                () -> KuudraConfig.setKuudraTalisman(switch (KuudraConfig.getKuudraTalisman()) {
                    case NONE   -> KuudraConfig.KuudraTalisman.KIDNEY;
                    case KIDNEY -> KuudraConfig.KuudraTalisman.LUNG;
                    case LUNG   -> KuudraConfig.KuudraTalisman.HEART;
                    case HEART  -> KuudraConfig.KuudraTalisman.NONE; }))));
    }

    // ── Boss tab ────────────────────────────────────────────────────────────────

    private void buildBossTab() {
        Tab T = Tab.BOSS;

        Group solo = group("Solo Detector", T, null,
                KuudraConfig::isSoloDetectorEnabled, KuudraConfig::setSoloDetectorEnabled);
        solo.add(leaf(new Toggle("Solo Notification", T,
                KuudraConfig::isSoloNotifyEnabled, KuudraConfig::setSoloNotifyEnabled)));
        solo.add(soundGroup(T, KuudraConfig.SOUND_SOLO));
        roots.add(solo);

        Group hp = group("Kuudra HP HUD", T, null,
                KuudraConfig::isKuudraHpHudEnabled, KuudraConfig::setKuudraHpHudEnabled);
        hp.add(leaf(new Toggle("Show Raw HP", T,
                KuudraConfig::isKuudraHpShowRaw, KuudraConfig::setKuudraHpShowRaw)));
        hp.add(leaf(new Toggle("Hide Health Bar", T,
                KuudraConfig::isKuudraHpHideBar, KuudraConfig::setKuudraHpHideBar)));
        roots.add(hp);

        roots.add(leaf(new Toggle("Mana Drain Announcer", T,
                KuudraConfig::isManaDrainAnnouncerEnabled, KuudraConfig::setManaDrainAnnouncerEnabled)));
        roots.add(leaf(new Toggle("Kuudra Direction", T,
                KuudraConfig::isKuudraDirectionEnabled, KuudraConfig::setKuudraDirectionEnabled)));
        roots.add(leaf(new Toggle("Rend Damage", T,
                KuudraConfig::isRendDamageEnabled, KuudraConfig::setRendDamageEnabled)));
        roots.add(leaf(new Toggle("Rend Tracker", T,
                KuudraConfig::isRendTrackerEnabled, KuudraConfig::setRendTrackerEnabled)));
        Group backbone = group("Backbone Progress Bar", T, null,
                KuudraConfig::isBackboneProgressBarEnabled, KuudraConfig::setBackboneProgressBarEnabled);
        backbone.add(leaf(new Toggle("Work Outside Kuudra", T,
                KuudraConfig::isBackboneProgressBarOutsideKuudraEnabled, KuudraConfig::setBackboneProgressBarOutsideKuudraEnabled)));
        backbone.add(soundGroup(T, KuudraConfig.SOUND_BACKBONE_DONE));
        roots.add(backbone);

        Group hl = group("Kuudra Highlight", T, null,
                KuudraConfig::isKuudraHighlightEnabled, KuudraConfig::setKuudraHighlightEnabled);
        hl.add(leaf(new Toggle("Filled Highlight", T,
                KuudraConfig::isKuudraHighlightFilled, KuudraConfig::setKuudraHighlightFilled)));
        roots.add(hl);
    }

    // ── Loadouts tab (Skyblock) ────────────────────────────────────────────────

    private void buildLoadoutsTab() {
        Tab T = Tab.LOADOUTS;

        Group wardrobe = group("Wardrobe Keybinds", T, null,
                KuudraConfig::isWardrobeEnabled, KuudraConfig::setWardrobeEnabled);
        String[] slotLabels = {"Slot 1","Slot 2","Slot 3","Slot 4","Slot 5",
                               "Slot 6","Slot 7","Slot 8","Slot 9"};
        for (int i = 0; i < 9; i++) {
            final int idx = i;
            wardrobe.add(leaf(new KeyCapture(slotLabels[i], T,
                    () -> KuudraConfig.getWardrobeSlotKeys()[idx],
                    v  -> KuudraConfig.setWardrobeSlotKey(idx, v))));
        }
        wardrobe.add(leaf(new KeyCapture("Open Wardrobe",  T, KuudraConfig::getWardrobeOpenKey,     KuudraConfig::setWardrobeOpenKey)));
        wardrobe.add(leaf(new KeyCapture("Stats Keybind",  T, KuudraConfig::getStatsOpenKey,        KuudraConfig::setStatsOpenKey)));
        wardrobe.add(leaf(new KeyCapture("Open Pets",      T, KuudraConfig::getPetsOpenKey,         KuudraConfig::setPetsOpenKey)));
        wardrobe.add(leaf(new KeyCapture("Equipment Wardrobe Keybind", T, KuudraConfig::getEqWardrobeOpenKey, KuudraConfig::setEqWardrobeOpenKey)));
        wardrobe.add(leaf(new KeyCapture("Next Page",      T, KuudraConfig::getWardrobeNextPageKey, KuudraConfig::setWardrobeNextPageKey)));
        wardrobe.add(leaf(new KeyCapture("Prev Page",      T, KuudraConfig::getWardrobePrevPageKey, KuudraConfig::setWardrobePrevPageKey)));
        wardrobe.add(leaf(new KeyCapture("Unequip",        T, KuudraConfig::getWardrobeUnequipKey,  KuudraConfig::setWardrobeUnequipKey)));
        wardrobe.add(leaf(new Toggle("Disable Unequip", T,
                KuudraConfig::isWardrobeDisableUnequipEnabled, KuudraConfig::setWardrobeDisableUnequipEnabled)));
        wardrobe.add(leaf(new Toggle("Auto Close Wardrobe", T,
                KuudraConfig::isWardrobeAutoCloseEnabled, KuudraConfig::setWardrobeAutoCloseEnabled)));
        wardrobe.add(soundGroup(T, wardrobe.key, KuudraConfig.SOUND_WARDROBE_SWAP));
        roots.add(wardrobe);

        Group loadouts = group("Loadout Keybinds", T, null,
                KuudraConfig::isWardrobeEnabled, KuudraConfig::setWardrobeEnabled);
        loadouts.add(leaf(new KeyCapture("Loadouts Keybind", T, KuudraConfig::getLoadoutsOpenKey, KuudraConfig::setLoadoutsOpenKey)));
        for (int i = 0; i < 12; i++) {
            final int idx = i;
            loadouts.add(leaf(new KeyCapture("Loadout Slot " + (idx + 1), T,
                    () -> KuudraConfig.getLoadoutSlotKeys()[idx],
                    v  -> KuudraConfig.setLoadoutSlotKey(idx, v))));
        }
        loadouts.add(leaf(new KeyCapture("Next Page", T, KuudraConfig::getWardrobeNextPageKey, KuudraConfig::setWardrobeNextPageKey)));
        loadouts.add(leaf(new KeyCapture("Prev Page", T, KuudraConfig::getWardrobePrevPageKey, KuudraConfig::setWardrobePrevPageKey)));
        loadouts.add(leaf(new Toggle("Auto Close Loadouts", T,
                KuudraConfig::isWardrobeAutoCloseEnabled, KuudraConfig::setWardrobeAutoCloseEnabled)));
        loadouts.add(soundGroup(T, loadouts.key, KuudraConfig.SOUND_WARDROBE_SWAP));
        roots.add(loadouts);
    }

    // ── Render tab (Skyblock) ───────────────────────────────────────────────────

    private void buildRenderTab() {
        Tab T = Tab.RENDER;

        roots.add(leaf(new Toggle("Hide Falling Blocks", T,
                KuudraConfig::isHideFallingBlocksEnabled, KuudraConfig::setHideFallingBlocksEnabled)));
        roots.add(leaf(new Toggle("Hide Entity Fire", T,
                KuudraConfig::isHideEntityFireEnabled, KuudraConfig::setHideEntityFireEnabled)));
        roots.add(leaf(new Toggle("Hide Dead Enemies", T,
                KuudraConfig::isHideDeadEntitiesEnabled, KuudraConfig::setHideDeadEntitiesEnabled)));
        roots.add(leaf(new Toggle("Hide Selfie Cam", T,
                KuudraConfig::isHideSelfieEnabled, KuudraConfig::setHideSelfieEnabled)));

        Group as = group("Hide Irrelevant Armor Stands", T, null,
                KuudraConfig::isHideArmorStandsEnabled, KuudraConfig::setHideArmorStandsEnabled);
        as.add(leaf(new Toggle("Build Area", T,
                KuudraConfig::isHideArmorStandsBuild, KuudraConfig::setHideArmorStandsBuild)));
        as.add(leaf(new Toggle("Right Cannon", T,
                KuudraConfig::isHideArmorStandsRightCannon, KuudraConfig::setHideArmorStandsRightCannon)));
        as.add(leaf(new Toggle("Left Cannon", T,
                KuudraConfig::isHideArmorStandsLeftCannon, KuudraConfig::setHideArmorStandsLeftCannon)));
        as.add(leaf(new Toggle("Shop", T,
                KuudraConfig::isHideArmorStandsShop, KuudraConfig::setHideArmorStandsShop)));
        as.add(leaf(new Toggle("Others", T,
                KuudraConfig::isHideArmorStandsOthers, KuudraConfig::setHideArmorStandsOthers)));
        roots.add(as);
    }

    // ── Misc tab (Skyblock) ─────────────────────────────────────────────────────

    private void buildMiscSkyblockTab() {
        Tab T = Tab.MISC_SKYBLOCK;

        Group pearlRefill = group("Pearl Refill", T, null,
                KuudraConfig::isPearlRefillEnabled, KuudraConfig::setPearlRefillEnabled);
        pearlRefill.add(leaf(new Toggle("Work Outside Kuudra", T,
                KuudraConfig::isPearlRefillOutsideKuudraEnabled, KuudraConfig::setPearlRefillOutsideKuudraEnabled)));
        roots.add(pearlRefill);

        roots.add(leaf(new Toggle("Auto Sprint", T,
                KuudraConfig::isAutoSprintEnabled, KuudraConfig::setAutoSprintEnabled)));

        Group heads = group("Prevent Placing Player Heads", T, null,
                KuudraConfig::isPreventPlacingPlayerHeadsEnabled, KuudraConfig::setPreventPlacingPlayerHeadsEnabled);
        heads.add(leaf(new Toggle("Except Garden", T,
                KuudraConfig::isPreventPlacingPlayerHeadsExceptGarden, KuudraConfig::setPreventPlacingPlayerHeadsExceptGarden)));
        roots.add(heads);

        roots.add(leaf(new Toggle("Prevent Placing Weapons", T,
                KuudraConfig::isPreventPlacingWeaponsEnabled, KuudraConfig::setPreventPlacingWeaponsEnabled)));

        Group slot = group("Slot Binds", T, null,
                KuudraConfig::isSlotBindsEnabled, KuudraConfig::setSlotBindsEnabled);
        slot.add(leaf(new KeyCapture("Bind Key", T,
                KuudraConfig::getSlotBindSetKey, KuudraConfig::setSlotBindSetKey)));
        slot.add(leaf(new KeyCapture("Show Binds Key", T,
                KuudraConfig::getSlotBindShowKey, KuudraConfig::setSlotBindShowKey)));
        roots.add(slot);
    }

    // ── Misc tab (Kuudra) ───────────────────────────────────────────────────────

    private void buildMiscTab() {
        Tab T = Tab.MISC_KUUDRA;

        roots.add(leaf(rs(T, "Kuudra Mob Size", 1.0f, 200.0f, "%.0f%%",
                KuudraConfig::getKuudraSizeScale, KuudraConfig::setKuudraSizeScale)));

        Group shop = group("Shop Keybinds", T, null,
                KuudraConfig::isShopKeybindsEnabled, KuudraConfig::setShopKeybindsEnabled);
        shop.add(leaf(new KeyCapture("Main Key",   T, KuudraConfig::getShopMainKey,   KuudraConfig::setShopMainKey)));
        shop.add(leaf(new KeyCapture("Cannon Key", T, KuudraConfig::getShopCannonKey, KuudraConfig::setShopCannonKey)));
        roots.add(shop);

        Group profit = group("Profit Tracker", T, null,
                KuudraConfig::isProfitTrackerEnabled, KuudraConfig::setProfitTrackerEnabled);
        profit.add(leaf(new Toggle("Show During Run", T,
                KuudraConfig::isProfitShowDuringRun, KuudraConfig::setProfitShowDuringRun)));
        profit.add(leaf(new Cycle("Armor", T,
                () -> KuudraConfig.isProfitArmorSalvage() ? "Salvage" : "Sell",
                () -> KuudraConfig.setProfitArmorSalvage(!KuudraConfig.isProfitArmorSalvage()))));
        profit.add(leaf(new Cycle("Faction", T,
                () -> KuudraConfig.isProfitFactionMage() ? "Mage" : "Barbarian",
                () -> KuudraConfig.setProfitFactionMage(!KuudraConfig.isProfitFactionMage()))));
        profit.add(leaf(new Toggle("Highlight Unopened Chests", T,
                KuudraConfig::isProfitHighlightChests, KuudraConfig::setProfitHighlightChests)));
        profit.add(leaf(new Toggle("Reroll Calculator", T,
                KuudraConfig::isProfitRerollCalc, KuudraConfig::setProfitRerollCalc)));
        profit.add(leaf(new Cycle("Bazaar Sell", T,
                () -> KuudraConfig.isProfitBazaarInstaSell() ? "Instasell" : "Sell Order",
                () -> KuudraConfig.setProfitBazaarInstaSell(!KuudraConfig.isProfitBazaarInstaSell()))));
        profit.add(leaf(new Cycle("Bazaar Buy", T,
                () -> KuudraConfig.isProfitBazaarInstaBuy() ? "Instabuy" : "Buy Order",
                () -> KuudraConfig.setProfitBazaarInstaBuy(!KuudraConfig.isProfitBazaarInstaBuy()))));
        profit.add(leaf(new Toggle("Chest Value GUI", T,
                KuudraConfig::isChestValueGuiEnabled, KuudraConfig::setChestValueGuiEnabled)));
        profit.add(leaf(new Cycle("Kuudra Pet", T,
                () -> {
                    KuudraConfig.KuudraPetRarity r = KuudraConfig.getKuudraPetRarity();
                    String name = r.name().charAt(0) + r.name().substring(1).toLowerCase();
                    return name;
                },
                () -> {
                    KuudraConfig.KuudraPetRarity[] vals = KuudraConfig.KuudraPetRarity.values();
                    int next = (KuudraConfig.getKuudraPetRarity().ordinal() + 1) % vals.length;
                    KuudraConfig.setKuudraPetRarity(vals[next]);
                })));
        profit.add(leaf(new RangeSlider("Pet Level", T, 1, 100, "%.0f",
                () -> (float) KuudraConfig.getKuudraPetLevel(),
                v  -> KuudraConfig.setKuudraPetLevel(Math.round(v)))));
        roots.add(profit);

        Group kicked = group("Kicked Notification", T, null,
                KuudraConfig::isKickedNotificationEnabled, KuudraConfig::setKickedNotificationEnabled);
        kicked.add(soundGroup(T, KuudraConfig.SOUND_KICKED));
        roots.add(kicked);
        Group autoRequeue = group("Auto Requeue", T, null,
                KuudraConfig::isAutoRequeueEnabled, KuudraConfig::setAutoRequeueEnabled);
        autoRequeue.add(leaf(new Toggle("Party Chat Message", T,
                KuudraConfig::isAutoRequeueMessageEnabled, KuudraConfig::setAutoRequeueMessageEnabled)));
        roots.add(autoRequeue);
        roots.add(leaf(new Toggle("Hide Damage Title", T,
                KuudraConfig::isHideDamageTitleEnabled, KuudraConfig::setHideDamageTitleEnabled)));
        roots.add(leaf(new Toggle("Hollow Wand Announcer", T,
                KuudraConfig::isHollowWandEnabled, KuudraConfig::setHollowWandEnabled)));
        roots.add(leaf(new Toggle("Hide Boss Bar", T,
                KuudraConfig::isHideBossBarEnabled, KuudraConfig::setHideBossBarEnabled)));

        roots.add(leaf(new Toggle("Hide Elle Dialogue", T,
                KuudraConfig::isHideElleDialogueEnabled, KuudraConfig::setHideElleDialogue)));
        roots.add(leaf(new Toggle("Etherwarp Lava Block", T,
                KuudraConfig::isEtherwarpLavaBlockEnabled,
                v -> { if (v != KuudraConfig.isEtherwarpLavaBlockEnabled()) KuudraConfig.toggleEtherwarpLavaBlock(); })));
        roots.add(leaf(new Toggle("Chest Tracker HUD", T,
                KuudraConfig::isChestTrackerVisible, KuudraConfig::setChestTrackerVisible)));

        Group shitterList = group("Shitter List", T, null,
                com.kuudrahelper.features.misckuudra.ShitterList::isEnabled,
                com.kuudrahelper.features.misckuudra.ShitterList::setEnabled);
        shitterList.add(leaf(new Toggle("Auto Kick Shitters", T,
                com.kuudrahelper.features.misckuudra.ShitterList::isAutoKickEnabled,
                com.kuudrahelper.features.misckuudra.ShitterList::setAutoKickEnabled)));
        roots.add(shitterList);

        Group explosion = group("Exploison Hider", T, null,
                KuudraConfig::isExplosionFilterEnabled, KuudraConfig::setExplosionFilterEnabled);
        explosion.add(leaf(new Slider("Hide Radius", T,
                KuudraConfig::getExplosionHideRadiusRaw, KuudraConfig::setExplosionHideRadius, "")));
        roots.add(explosion);

        roots.add(leaf(new Toggle("Party Commands", T,
                KuudraConfig::isPartyCmdsEnabled, KuudraConfig::setPartyCmdsEnabled)));

        Group split = group("Split Timer", T, null,
                KuudraConfig::isSplitTimerEnabled, KuudraConfig::setSplitTimerEnabled);
        split.add(leaf(new Toggle("Supply Times", T,
                KuudraConfig::isSupplyTimesEnabled, KuudraConfig::setSupplyTimesEnabled)));
        roots.add(split);

        Group autoKick = group("Auto Kick" + (KuudraConfig.API_KEY_FEATURES_UNLOCKED ? "" : " (DISABLED)"), T, null,
                KuudraConfig::isAutoKickEnabled, KuudraConfig::setAutoKickEnabled);
        autoKick.add(leaf(new OptionalIntInput("Catacombs Level", T, KuudraConfig::getAkMinCatacombs, KuudraConfig::setAkMinCatacombs)));
        autoKick.add(leaf(new OptionalIntInput("Foraging Level", T, KuudraConfig::getAkMinForaging, KuudraConfig::setAkMinForaging)));
        autoKick.add(leaf(new OptionalIntInput("Magical Power", T, KuudraConfig::getAkMinMagicalPower, KuudraConfig::setAkMinMagicalPower)));
        autoKick.add(leaf(new OptionalIntInput("Infernal Comps", T, KuudraConfig::getAkMinInfernal, KuudraConfig::setAkMinInfernal)));
        autoKick.add(leaf(new OptionalIntInput("Fiery Comps", T, KuudraConfig::getAkMinFiery, KuudraConfig::setAkMinFiery)));
        autoKick.add(leaf(new OptionalIntInput("Burning Comps", T, KuudraConfig::getAkMinBurning, KuudraConfig::setAkMinBurning)));
        autoKick.add(leaf(new OptionalIntInput("Hot Comps", T, KuudraConfig::getAkMinHot, KuudraConfig::setAkMinHot)));
        autoKick.add(leaf(new OptionalIntInput("Basic Comps", T, KuudraConfig::getAkMinBasic, KuudraConfig::setAkMinBasic)));
        autoKick.add(leaf(new Toggle("Rend", T,
                KuudraConfig::isAkRequireRend, KuudraConfig::setAkRequireRend)));
        autoKick.add(leaf(new OptionalIntInput("Gdrag Level", T, KuudraConfig::getAkMinGdragLevel, KuudraConfig::setAkMinGdragLevel)));
        roots.add(autoKick);

        roots.add(leaf(new Toggle("Profile Viewer" + (KuudraConfig.API_KEY_FEATURES_UNLOCKED ? "" : " (DISABLED)"), T,
                KuudraConfig::isProfileViewerEnabled, KuudraConfig::setProfileViewerEnabled)));
    }

    // ── Dungeons tab ────────────────────────────────────────────────────────────

    private void buildDungeonsTab() {
        Tab T = Tab.DUNGEONS_M7;

        Group m7toxic = group("M7 Auto GFS Toxic", T, null,
                KuudraConfig::isAutoGfsToxicEnabled, KuudraConfig::setAutoGfsToxic);
        m7toxic.add(leaf(new IntInput("Toxic Amount", T, KuudraConfig::getToxicAmount, KuudraConfig::setToxicAmount)));
        roots.add(m7toxic);

        Group m7twi = group("M7 Auto GFS Twilight", T, null,
                KuudraConfig::isAutoGfsTwilightEnabled, KuudraConfig::setAutoGfsTwilight);
        m7twi.add(leaf(new IntInput("Twilight Amount", T, KuudraConfig::getTwilightAmount, KuudraConfig::setTwilightAmount)));
        roots.add(m7twi);
    }

    // ── Lava & Water Customisation tab ─────────────────────────────────────────

    private void buildFluidCustomTab() {
        Tab T = Tab.FLUID_CUSTOM;

        Group lava = group("Lava Tweaks", T, null, null, null);
        lava.add(leaf(new Toggle("Replace with Water", T,
                KuudraConfig::isLavaAsWater, KuudraConfig::setLavaAsWater)));
        lava.add(leaf(new Slider("Opacity", T,
                KuudraConfig::getLavaOpacity, KuudraConfig::setLavaOpacity, "%")));
        lava.add(leaf(new Toggle("Colour Override", T,
                KuudraConfig::isLavaColorOverride, KuudraConfig::setLavaColorOverride)));
        lava.add(leaf(new ColorSlider("Red",   T, 16, 0xFF4444, KuudraConfig::getLavaColor, KuudraConfig::setLavaColor)));
        lava.add(leaf(new ColorSlider("Green", T,  8, 0x44FF88, KuudraConfig::getLavaColor, KuudraConfig::setLavaColor)));
        lava.add(leaf(new ColorSlider("Blue",  T,  0, 0x4488FF, KuudraConfig::getLavaColor, KuudraConfig::setLavaColor)));
        lava.add(leaf(new LavaPreview(T)));
        roots.add(lava);

        Group water = group("Water Tweaks", T, null, null, null);
        water.add(leaf(new Toggle("Replace with Lava", T,
                KuudraConfig::isWaterAsLava, KuudraConfig::setWaterAsLava)));
        water.add(leaf(new Slider("Opacity", T,
                KuudraConfig::getWaterOpacity, KuudraConfig::setWaterOpacity, "%")));
        water.add(leaf(new Toggle("Colour Override", T,
                KuudraConfig::isWaterColorOverride, KuudraConfig::setWaterColorOverride)));
        water.add(leaf(new ColorSlider("Red",   T, 16, 0xFF4444, KuudraConfig::getWaterColor, KuudraConfig::setWaterColor)));
        water.add(leaf(new ColorSlider("Green", T,  8, 0x44FF88, KuudraConfig::getWaterColor, KuudraConfig::setWaterColor)));
        water.add(leaf(new ColorSlider("Blue",  T,  0, 0x4488FF, KuudraConfig::getWaterColor, KuudraConfig::setWaterColor)));
        water.add(leaf(new WaterPreview(T)));
        roots.add(water);
    }

    // ── Item Customisation tab ─────────────────────────────────────────────────

    private void buildItemCustomTab() {
        Tab T = Tab.ITEM_CUSTOM;

        // Scaling — player and mob size overrides
        Group scaling = group("Scaling", T, null, null, null);
        scaling.add(leaf(rs(T, "Self Player Scale", 1.0f, 300.0f, "%.0f%%",
                KuudraConfig::getSelfPlayerScale, KuudraConfig::setSelfPlayerScale)));
        scaling.add(leaf(rs(T, "Other Player Scale", 1.0f, 300.0f, "%.0f%%",
                KuudraConfig::getOtherPlayerScale, KuudraConfig::setOtherPlayerScale)));
        roots.add(scaling);

        // Item customisation — master dropdown, each type a nested dropdown
        Group ic = group("Item Customisation", T, null,
                KuudraConfig::isItemCustomizationEnabled, KuudraConfig::setItemCustomizationEnabled);

        for (ItemCustomization.ItemCategory cat : ItemCustomization.ItemCategory.values()) {
            ItemTransformSettings st = ItemCustomization.getBuiltinSettings(cat);
            String label = cat == ItemCustomization.ItemCategory.GLOBAL ? "Global" : cat.displayName();
            Group typeGroup = group(label, T, ic.key,
                    () -> st.enabled, v -> { st.enabled = v; KuudraConfig.save(); });
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
                @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
                    ctx.text(s.font, Component.literal("§7match: §f\"" + cc.matchString + "\""),
                            x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
                    int bw = 58, bh = ROW_H - 6, bx = x + w - bw - 8, by = y + 3;
                    boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
                    ctx.fill(bx, by, bx + bw, by + bh, hov ? 0xFF4A1515 : 0xFF2A0F0F);
                    ctx.fill(bx, by, bx + bw, by + 1, 0xFFCC3333);
                    ctx.centeredText(s.font, Component.literal("Remove"),
                            bx + bw / 2, by + (bh - s.font.lineHeight) / 2, 0xFFCC3333);
                }
                @Override boolean onDown(double mx, double my, int x, int y, int w) {
                    int bw = 58, bh = ROW_H - 6, bx = x + w - bw - 8, by = y + 3;
                    if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                        ItemCustomization.removeCustomCategory(idx);
                        KuudraConfig.save();
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
                com.kuudrahelper.features.customisation.VisualWords::isEnabled,
                com.kuudrahelper.features.customisation.VisualWords::setEnabled)));
        roots.add(leaf(new Button("Add Word", T, "+ Add",
                () -> { com.kuudrahelper.features.customisation.VisualWords.addRule(); buildFeatures(); })));
        int vwCount = com.kuudrahelper.features.customisation.VisualWords.getRules().size();
        for (int i = 0; i < vwCount; i++) roots.add(leaf(new VisualWordEntry(i)));
    }

    // ── About tab ─────────────────────────────────────────────────────────────

    private void buildAboutTab() {
        Tab T = Tab.ABOUT;
        roots.add(leaf(new Feature("Current Version", T) {
            @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
                ctx.text(s.font, Component.literal("Current Version"),
                        x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
                String ver = UpdateChecker.currentVersion();
                ctx.text(s.font, Component.literal("§a" + ver),
                        x + w - s.font.width(ver) - 8,
                        y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            }
        }));

        roots.add(leaf(new Feature("Latest Version", T) {
            @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
                ctx.text(s.font, Component.literal("Latest Version"),
                        x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
                String latest = UpdateChecker.getLatestVersion();
                String display;
                int colour;
                switch (UpdateChecker.getState()) {
                    case CHECKING, DOWNLOADING -> { display = "Checking..."; colour = C_TEXT_DIM; }
                    case ERROR                 -> { display = "Error";        colour = 0xFFCC4444; }
                    case UP_TO_DATE            -> { display = latest != null ? latest : "Up to date"; colour = C_ON; }
                    case UPDATE_AVAILABLE      -> { display = latest;         colour = 0xFFFFAA00; }
                    case DOWNLOADED            -> { display = latest;         colour = 0xFF44AAFF; }
                    default                    -> { display = "—";            colour = C_TEXT_DIM; }
                }
                ctx.text(s.font, Component.literal(display),
                        x + w - s.font.width(display) - 8,
                        y + (ROW_H - s.font.lineHeight) / 2, colour);
            }
        }));

        roots.add(leaf(new Feature("Status", T) {
            @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
                ctx.text(s.font, Component.literal("Status"),
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
                    case DOWNLOADED   -> 0xFF44AAFF;
                    case UPDATE_AVAILABLE -> C_ACCENT;
                    case ERROR        -> 0xFFCC4444;
                    default           -> C_TEXT_DIM;
                };
                ctx.text(s.font, Component.literal(status),
                        x + w - s.font.width(status) - 8,
                        y + (ROW_H - s.font.lineHeight) / 2, colour);
            }
        }));

        roots.add(leaf(new IntInput("Ping (ms)", T,
                KuudraConfig::getLowPing, KuudraConfig::setLowPing)));
        roots.add(leaf(new Toggle("Auto Updates", T,
                KuudraConfig::isAutoUpdatesEnabled, KuudraConfig::setAutoUpdatesEnabled)));
        roots.add(leaf(new Toggle("Developer Features", T,
                KuudraConfig::isDeveloperFeaturesEnabled, KuudraConfig::setDeveloperFeaturesEnabled)));
        roots.add(leaf(new Button("HUD Layout", T,
                "Edit Layout",
                () -> Minecraft.getInstance().setScreen(new HudEditorScreen(KuudraScreen.this)))));
        roots.add(leaf(new Button("Updates", T,
                UpdateChecker.isDownloaded() ? "Restart to Install" : "Download Now",
                UpdateChecker::downloadManually)));
    }

    // ── Sound group helper ────────────────────────────────────────────────────

    private Group soundGroup(Tab tab, String soundKey) {
        return soundGroup(tab, null, soundKey);
    }

    private Group soundGroup(Tab tab, String parentKey, String soundKey) {
        Group g = group("Sound", tab, parentKey,
                () -> KuudraConfig.isNotificationSoundEnabled(soundKey),
                v  -> KuudraConfig.setNotificationSoundEnabled(soundKey, v));
        g.add(leaf(new TextInput("Sound ID", tab,
                () -> KuudraConfig.getNotificationSoundId(soundKey),
                v  -> KuudraConfig.setNotificationSoundId(soundKey, v))));
        g.add(leaf(new RangeSlider("Volume", tab, 0f, 2f, "%.2f",
                () -> KuudraConfig.getNotificationSoundVolume(soundKey),
                v  -> KuudraConfig.setNotificationSoundVolume(soundKey, v))));
        g.add(leaf(new RangeSlider("Pitch", tab, 0.1f, 4f, "%.2f",
                () -> KuudraConfig.getNotificationSoundPitch(soundKey),
                v  -> KuudraConfig.setNotificationSoundPitch(soundKey, v))));
        return g;
    }

    // ── Range slider helpers ────────────────────────────────────────────────────

    private void addRangeSliders(Group g, Tab tab, ItemTransformSettings s) {
        g.add(leaf(rs(tab, "pos X",     -0.5f,  0.5f,  "%.3f",       () -> s.posX,       v -> { s.posX       = v; KuudraConfig.save(); })));
        g.add(leaf(rs(tab, "pos Y",     -0.5f,  0.5f,  "%.3f",       () -> s.posY,       v -> { s.posY       = v; KuudraConfig.save(); })));
        g.add(leaf(rs(tab, "pos Z",     -0.5f,  0.5f,  "%.3f",       () -> s.posZ,       v -> { s.posZ       = v; KuudraConfig.save(); })));
        g.add(leaf(rs(tab, "rot X",     -180f,  180f,  "%.0f°", () -> s.rotX,       v -> { s.rotX       = v; KuudraConfig.save(); })));
        g.add(leaf(rs(tab, "rot Y",     -180f,  180f,  "%.0f°", () -> s.rotY,       v -> { s.rotY       = v; KuudraConfig.save(); })));
        g.add(leaf(rs(tab, "rot Z",     -180f,  180f,  "%.0f°", () -> s.rotZ,       v -> { s.rotZ       = v; KuudraConfig.save(); })));
        g.add(leaf(rs(tab, "scale",     0.25f,  3.0f,  "%.2f×", () -> s.scale,      v -> { s.scale      = v; KuudraConfig.save(); })));
        g.add(leaf(rs(tab, "swing spd", 0.25f,  3.0f,  "%.2f×", () -> s.swingSpeed, v -> { s.swingSpeed = v; KuudraConfig.save(); })));
        g.add(leaf(rs(tab, "proximity", -1.0f,  1.0f,  "%.3f",       () -> s.proximity,  v -> { s.proximity  = v; KuudraConfig.save(); })));
        g.add(leaf(new Toggle("No Equip Animation", tab, () -> s.noEquipAnimation, v -> { s.noEquipAnimation = v; KuudraConfig.save(); })));
        g.add(leaf(new Toggle("In Place Swing",     tab, () -> s.inPlaceSwing,     v -> { s.inPlaceSwing     = v; KuudraConfig.save(); })));
        g.add(leaf(new Toggle("Static Position",    tab, () -> s.staticPosition,   v -> { s.staticPosition   = v; KuudraConfig.save(); })));
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

    // scrollbar drag
    private boolean draggingScroll = false;
    private int     sbThumbY, sbThumbH, sbTrackTop, sbTrackH, sbMaxScroll;

    // sidebar (tab list) scroll
    private int tabScroll = 0;
    private final List<TabRow> tabRows = new ArrayList<>();

    private record TabRow(Tab tab, boolean isHeader, String label, int y, int h) {}

    // ── Constructor ───────────────────────────────────────────────────────────

    public KuudraScreen() {
        super(Component.literal("PhantomAddons"));
        buildFeatures();
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
        searchField.setHint(Component.literal("Search..."));
        searchField.setBordered(false);
        searchField.setTextColor(C_TEXT);
        searchField.setResponder(s -> { query = s.toLowerCase(); scroll = 0; });
        addRenderableWidget(searchField);
    }

    // ── Anim state ──────────────────────────────────────────────────────────────

    private boolean isExpanded(Group g) { return EXPANDED.getOrDefault(g.key, false); }
    private float   animOf(Group g)      { return ANIM.getOrDefault(g.key, 0f); }

    private void stepAnims(float delta) {
        for (Group g : allGroups) {
            float cur = ANIM.getOrDefault(g.key, 0f);
            float tgt = isExpanded(g) ? 1f : 0f;
            if (cur == tgt) continue;
            float step = delta * GROUP_SPEED * 0.05f; // delta is ~ticks; tune
            if (cur < tgt) cur = Math.min(tgt, cur + step);
            else           cur = Math.max(tgt, cur - step);
            ANIM.put(g.key, cur);
        }
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private List<RenderRow> layoutRows() {
        List<RenderRow> out = new ArrayList<>();
        int baseX = cx() + PAD;
        int baseW = cw() - PAD * 2;
        int[] y = { cy() + PAD - scroll };

        if (!query.isEmpty()) {
            for (Node n : roots) {
                Node f = filterNode(n, query);
                if (f != null) layoutNode(f, 0, baseX, baseW, 1f, y, out);
            }
            return out;
        }

        for (Node n : roots) {
            if (n.tab != currentTab) continue;
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
                EXPANDED.put("cat:" + r.label(), !isCategoryExpanded(r.label()));
                return true;
            }

            {
                if (r.tab() != currentTab) {
                    intInputs.forEach(Feature::cancel);
                    categoryInputs.forEach(Feature::cancel);
                    vwInputs.forEach(Feature::cancel);
                    rgbInputs.forEach(Feature::cancel);
                    textInputs.forEach(Feature::cancel);
                    rawTextInputs.forEach(Feature::cancel);
                    allSliders.forEach(Feature::cancel);
                    currentTab = r.tab();
                    tabAnim = 0f;
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

        if (mx >= cx() && mx <= cx() + cw() && my >= cy() && my <= cy() + ch()) {
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
        ctx.fill(0, 0, width, height, 0xBB06080C);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        extractBackground(ctx, mx, my, delta);
        if (tabAnim < 1f) tabAnim = Math.min(1f, tabAnim + delta * ANIM_SPEED);
        stepAnims(delta);

        int px = px(), py = py();
        ctx.fill(px - 1, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, C_BORDER);
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, C_BG);

        ctx.fill(px, py, px + PANEL_W, py + HEADER_H, C_HEADER);
        ctx.fill(px, py + HEADER_H - 1, px + PANEL_W, py + HEADER_H, C_ACCENT);
        int logoS = HEADER_H - 8, logoX = px + 6, logoY = py + 4;
        ctx.blit(LOGO, logoX, logoY, logoX + logoS, logoY + logoS, 0f, 1f, 0f, 1f);
        ctx.text(font, Component.literal("PhantomAddons"),
                logoX + logoS + 6, py + (HEADER_H - font.lineHeight) / 2, C_ACCENT);

        int sfX = px + PANEL_W - 148, sfY = py + (HEADER_H - 16) / 2;
        ctx.fill(sfX - 3, sfY - 1, sfX + 141, sfY + 15, 0xFF090B0E);
        ctx.fill(sfX - 3, sfY + 14, sfX + 141, sfY + 15, C_BORDER);

        ctx.fill(px, py + HEADER_H, px + SIDEBAR_W, py + PANEL_H, C_SIDEBAR);
        ctx.fill(px + SIDEBAR_W - 1, py + HEADER_H, px + SIDEBAR_W, py + PANEL_H, C_BORDER);
        renderTabs(ctx, px, py, mx, my);
        renderContent(ctx, mx, my);
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
            if (t.category != null && !isCategoryExpanded(t.category)) continue;
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
                ctx.text(font, Component.literal(arrow),
                        tx + 10, ty + (r.h() - font.lineHeight) / 2, headerHover ? 0xFFAAB2C4 : 0xFF7A8296);
                ctx.text(font, Component.literal(r.label()),
                        tx + 20, ty + (r.h() - font.lineHeight) / 2, headerHover ? 0xFFAAB2C4 : 0xFF7A8296);
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
            ctx.text(font, Component.literal(r.label()),
                    tx + 14, ty + (r.h() - font.lineHeight) / 2,
                    active ? C_ACCENT : hover ? C_TEXT : C_TEXT_DIM);
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            int barX = px + SIDEBAR_W - 3;
            int barH = Math.max(16, (int) ((float) tabListVisibleH() / totalH * (py + PANEL_H - startY)));
            int barY = startY + (int) ((float) tabScroll / maxScroll * (py + PANEL_H - startY - barH));
            ctx.fill(barX, startY, barX + 2, py + PANEL_H, 0x22FFFFFF);
            ctx.fill(barX, barY, barX + 2, barY + barH, 0x88FFFFFF);
        }
    }

    private void renderContent(GuiGraphicsExtractor ctx, int mx, int my) {
        int cx = cx(), cy = cy(), cw = cw(), ch = ch();
        List<RenderRow> rows = layoutRows();

        if (rows.isEmpty()) {
            ctx.centeredText(font,
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

        float ease   = tabAnim * tabAnim * (3f - 2f * tabAnim);
        int   slideX = (int)((1f - ease) * 16);

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
            ctx.fill(bx, bb - 1, bRight, bb, 0x22FFFFFF);      // subtle bottom border
        }

        for (RenderRow r : rows) {
            if (r.h <= 0) continue;
            int top = Math.max(cy, r.y);
            int bot = Math.min(cy + ch, r.y + r.h);
            if (bot <= top) continue;

            ctx.enableScissor(cx, top, cx + cw, bot);

            int rx = r.x + slideX;
            int rw = r.w - slideX;

            if (r.node instanceof Group g) {
                renderGroupHeader(ctx, g, rx, r.y, rw, mx, my);
            } else if (r.node instanceof Leaf lf) {
                // subtle row hover stripe
                ctx.fill(rx, r.y, cx + cw - PAD, r.y + r.contentH, 0x0AFFFFFF);
                lf.f.render(ctx, this, rx, r.y, rw, mx, my);
            }

            ctx.disableScissor();
        }
        ctx.disableScissor();

        if (tabAnim < 1f) {
            int a = (int)((1f - ease) * 200) << 24;
            ctx.fill(cx, cy, cx + cw, cy + ch, (C_BG & 0xFFFFFF) | a);
        }

        sbMaxScroll = maxScroll;
        if (maxScroll > 0) {
            int sbX = cx + cw - 5;
            sbTrackTop = cy + PAD;
            sbTrackH   = ch - PAD * 2;
            sbThumbH   = Math.max(18, (int)((long) sbTrackH * sbTrackH / (totalH + PAD * 2)));
            sbThumbY   = sbTrackTop + (int)((float) scroll / maxScroll * (sbTrackH - sbThumbH));
            boolean hov = mx >= sbX - 1 && mx <= sbX + 4 && my >= sbTrackTop && my <= sbTrackTop + sbTrackH;
            ctx.fill(sbX, sbTrackTop, sbX + 3, sbTrackTop + sbTrackH, 0x1AFFFFFF);
            ctx.fill(sbX, sbThumbY, sbX + 3, sbThumbY + sbThumbH,
                    (draggingScroll || hov) ? 0xCCFFAA00 : 0x77AABBDD);
        }
    }

    private void renderGroupHeader(GuiGraphicsExtractor ctx, Group g,
                                   int x, int y, int w, int mx, int my) {
        boolean expanded = isExpanded(g);
        float a = animOf(g);
        boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + ROW_H;

        ctx.fill(x, y, x + w, y + ROW_H, hov ? 0x14FFFFFF : 0x0CFFFFFF);
        ctx.fill(x, y, x + w, y + 1, 0x22FFFFFF);

        String arrow = a > 0.5f ? "▾" : "▸";
        ctx.text(font, Component.literal(arrow),
                x + 6, y + (ROW_H - font.lineHeight) / 2, expanded ? C_ACCENT : C_TEXT_DIM);

        ctx.text(font, Component.literal(g.name),
                x + 18, y + (ROW_H - font.lineHeight) / 2,
                expanded ? C_ACCENT : C_TEXT);

        if (g.get != null && g.set != null) {
            boolean on = g.get.get();
            int pw = 34, ph = 12, px = x + w - pw - 8, py = y + (ROW_H - ph) / 2;
            ctx.fill(px, py, px + pw, py + ph, on ? C_ON : C_OFF);
            int kx = on ? px + pw - 12 : px + 1;
            ctx.fill(kx, py + 1, kx + 10, py + ph - 1, 0xFFFFFFFF);
        }
    }

    @Override public void removed()          { KuudraConfig.save(); }
    @Override public boolean isPauseScreen() { return false; }
}
