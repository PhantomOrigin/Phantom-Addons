package com.kuudrahelper;

import com.kuudrahelper.features.items.ItemCustomization;
import com.kuudrahelper.features.items.ItemTransformSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.client.input.MouseButtonEvent;
import java.util.ArrayList;
import java.util.List;
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

    // ── Colours ───────────────────────────────────────────────────────────────

    private static final int C_BG         = 0xFF0D0F14;
    private static final int C_SIDEBAR    = 0xFF090B0F;
    private static final int C_HEADER     = 0xFF111520;
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

    // ── Tabs ──────────────────────────────────────────────────────────────────

    private enum Tab {
        ABOUT("About"), SUPPLIES("Supplies"), BUILD("Build"), STUN_DPS("Stun/DPS"),
        BOSS("Boss"), LAVA_TWEAKS("Lava Tweaks"), MISC("Misc"), ITEMS("Items");
        final String label;
        Tab(String l) { this.label = l; }
    }

    private Tab   currentTab = Tab.ABOUT;
    private float tabAnim    = 1f;
    private static final float ANIM_SPEED = 9f;

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
            if (key == 256) { draft = null; focused = false; }
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
            if (key == 256) { capturing = false; return; } // Escape = cancel
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
        boolean drag = false;
        Slider(String n, Tab t, Supplier<Float> get, Consumer<Float> set, String unit) {
            super(n, t); this.get = get; this.set = set; this.unit = unit;
        }
        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            String valStr = (int)(get.get() * 100) + unit;
            ctx.text(s.font, Component.literal(valStr),
                    x + w - s.font.width(valStr) - 68,
                    y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
            drawTrack(ctx, x, y, w, get.get(), C_SLIDER_FG);
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
            int sw = 52, sx = x + w - sw - 8, sy = y + ROW_H / 2 - 5;
            if (mx >= sx - 4 && mx <= sx + sw + 4 && my >= sy && my <= sy + 10) {
                drag = true; apply(mx, sx, sw); return true;
            }
            return false;
        }
        @Override boolean onDrag(double mx, double my, int x, int y, int w) {
            if (!drag) return false; apply(mx, x + w - 52 - 8, 52); return true;
        }
        @Override void onUp() { drag = false; }
        void apply(double mx, int sx, int sw) {
            set.accept((float) Mth.clamp((mx - sx) / sw, 0.0, 1.0));
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
        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            float raw = get.get() * (max - min) + min;
            String disp = String.format(fmt, raw);
            ctx.text(s.font, Component.literal(disp),
                    x + w - s.font.width(disp) - 68,
                    y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
            drawTrack(ctx, x, y, w, get.get(), C_SLIDER_FG);
        }
    }

    // ── ColorSlider ───────────────────────────────────────────────────────────

    private static class ColorSlider extends Slider {
        final int shift, tint;
        ColorSlider(String n, Tab t, int shift, int tint) {
            super(n, t,
                    () -> ((KuudraConfig.getLavaColor() >> shift) & 0xFF) / 255f,
                    v -> {
                        int c = KuudraConfig.getLavaColor() & ~(0xFF << shift);
                        KuudraConfig.setLavaColor(c | (Mth.clamp((int)(v * 255), 0, 255) << shift));
                    }, "");
            this.shift = shift; this.tint = tint;
        }
        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal(name),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, tint | 0xFF000000);
            String valStr = String.valueOf((int)(get.get() * 255));
            ctx.text(s.font, Component.literal(valStr),
                    x + w - s.font.width(valStr) - 68,
                    y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
            drawTrack(ctx, x, y, w, get.get(), tint | 0xFF000000);
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
        AddCategoryFeature() { super("Add category", Tab.ITEMS); }

        @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
            ctx.text(s.font, Component.literal("Match:"),
                    x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
            int bw = 46, bx = x + w - bw - 12;
            int fy = y + 3, fh = ROW_H - 6;
            int fx = x + 60, fw = bx - fx - 4;
            ctx.fill(fx, fy, fx + fw, fy + fh, focused ? 0xFF1A2A44 : 0xFF0F1218);
            ctx.fill(fx, fy + fh - 1, fx + fw, fy + fh, focused ? C_ACCENT : C_BORDER);
            String disp = draft.isEmpty() && !focused ? "\u00a77type to filter..." : draft;
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
            if (key == 256) { focused = false; draft = ""; }
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

    // ── Feature lists ─────────────────────────────────────────────────────────

    private final List<Feature>            allFeatures     = new ArrayList<>();
    private final List<IntInput>           intInputs       = new ArrayList<>();
    private final List<KeyCapture>         captureFeatures = new ArrayList<>();
    private final List<Slider>             allSliders      = new ArrayList<>();
    private final List<AddCategoryFeature> categoryInputs  = new ArrayList<>();

    private void buildFeatures() {
        allFeatures.clear();
        intInputs.clear();
        captureFeatures.clear();
        allSliders.clear();
        categoryInputs.clear();

        // ── AUTO GFS ──────────────────────────────────────────────────────────
        allFeatures.add(new Toggle("Auto GFS", Tab.STUN_DPS,
                KuudraConfig::isAutoGfsEnabled, KuudraConfig::setAutoGfsEnabled));
        allFeatures.add(new Cycle("Role Mode", Tab.STUN_DPS,
                () -> KuudraConfig.getRoleMode().name(),
                () -> KuudraConfig.setRoleMode(switch (KuudraConfig.getRoleMode()) {
                    case DPS  -> KuudraConfig.RoleMode.STUN;
                    case STUN -> KuudraConfig.RoleMode.AUTO;
                    case AUTO -> KuudraConfig.RoleMode.DPS;
                })));
        IntInput dps  = new IntInput("DPS Amount",  Tab.STUN_DPS, KuudraConfig::getDpsValue,  KuudraConfig::setDpsValue);
        IntInput stun = new IntInput("Stun Amount", Tab.STUN_DPS, KuudraConfig::getStunValue, KuudraConfig::setStunValue);
        allFeatures.add(dps);  intInputs.add(dps);
        allFeatures.add(stun); intInputs.add(stun);

        // ── STUN ──────────────────────────────────────────────────────────────
        allFeatures.add(new Toggle("Pickobulus Blocker", Tab.STUN_DPS,
                KuudraConfig::isPickoblockEnabled, KuudraConfig::setPickoblockEnabled));
        allFeatures.add(new Toggle("Eaten Timer", Tab.STUN_DPS,
                KuudraConfig::isEatenTimerEnabled, KuudraConfig::setEatenTimerEnabled));
        allFeatures.add(new Toggle("Cannon Auto Close", Tab.STUN_DPS,
                KuudraConfig::isCannonAutoCloseEnabled, KuudraConfig::setCannonAutoCloseEnabled));
        allFeatures.add(new Toggle("Build Progress Tracker", Tab.BUILD,
                KuudraConfig::isBuildProgressHudEnabled, KuudraConfig::setBuildProgressHudEnabled));
        allFeatures.add(new Toggle("Announce Fresh", Tab.BUILD,
                KuudraConfig::isAnnounceFreshEnabled, KuudraConfig::setAnnounceFreshEnabled));
        allFeatures.add(new Toggle("Fresh Notification", Tab.BUILD,
                KuudraConfig::isFreshNotifyEnabled, KuudraConfig::setFreshNotifyEnabled));
        allFeatures.add(new Toggle("Build Started Notification", Tab.BUILD,
                KuudraConfig::isBuildStartedNotifyEnabled, KuudraConfig::setBuildStartedNotifyEnabled));
        allFeatures.add(new Toggle("Stun Preview", Tab.STUN_DPS,
                KuudraConfig::isStunPreviewEnabled, KuudraConfig::setStunPreviewEnabled));
        allFeatures.add(new Toggle("Build Beacons", Tab.BUILD,
                KuudraConfig::isBuildBeaconsEnabled, KuudraConfig::setBuildBeaconsEnabled));

        // ── LAVA ──────────────────────────────────────────────────────────────
        allFeatures.add(new Toggle("Replace with Water", Tab.LAVA_TWEAKS,
                KuudraConfig::isLavaAsWater, KuudraConfig::setLavaAsWater));
        Slider opacity = new Slider("Opacity", Tab.LAVA_TWEAKS,
                KuudraConfig::getLavaOpacity, KuudraConfig::setLavaOpacity, "%");
        allFeatures.add(opacity); allSliders.add(opacity);
        allFeatures.add(new Toggle("Colour Override", Tab.LAVA_TWEAKS,
                KuudraConfig::isLavaColorOverride, KuudraConfig::setLavaColorOverride));
        ColorSlider cr = new ColorSlider("Red",   Tab.LAVA_TWEAKS, 16, 0xFF4444);
        ColorSlider cg = new ColorSlider("Green", Tab.LAVA_TWEAKS,  8, 0x44FF88);
        ColorSlider cb = new ColorSlider("Blue",  Tab.LAVA_TWEAKS,  0, 0x4488FF);
        allFeatures.add(cr); allSliders.add(cr);
        allFeatures.add(cg); allSliders.add(cg);
        allFeatures.add(cb); allSliders.add(cb);

        allFeatures.add(new Toggle("Supply Beacons", Tab.SUPPLIES,
                KuudraConfig::isSupplyBeaconsEnabled, KuudraConfig::setSupplyBeaconsEnabled));
        allFeatures.add(new Toggle("No Pre Announce", Tab.SUPPLIES,
                KuudraConfig::isNoPreAnnounceEnabled, KuudraConfig::setNoPreAnnounceEnabled));
        allFeatures.add(new Toggle("No Pre Notification", Tab.SUPPLIES,
                KuudraConfig::isNoPreNotifyEnabled, KuudraConfig::setNoPreNotifyEnabled));
        allFeatures.add(new Toggle("Crate Priority", Tab.SUPPLIES,
                KuudraConfig::isCratePriorityEnabled, KuudraConfig::setCratePriorityEnabled));
        allFeatures.add(new Toggle("Supply Recovery Message", Tab.SUPPLIES,
                KuudraConfig::isSupplyRecoveryMsgEnabled, KuudraConfig::setSupplyRecoveryMsgEnabled));
        allFeatures.add(new Toggle("Supply Location Announce", Tab.SUPPLIES,
                KuudraConfig::isSupplyLocationAnnounceEnabled, KuudraConfig::setSupplyLocationAnnounceEnabled));
        allFeatures.add(new Toggle("Supply Hitbox", Tab.SUPPLIES,
                KuudraConfig::isSupplyHitboxEnabled, KuudraConfig::setSupplyHitboxEnabled));
        allFeatures.add(new Toggle("Supply Rod Radius", Tab.SUPPLIES,
                KuudraConfig::isSupplyRodRadiusEnabled, KuudraConfig::setSupplyRodRadiusEnabled));
        allFeatures.add(new Toggle("Supply Pearl Hitbox", Tab.SUPPLIES,
                KuudraConfig::isSupplyPearlHitboxEnabled, KuudraConfig::setSupplyPearlHitboxEnabled));
        allFeatures.add(new Toggle("Supply Giant Hitbox Alert", Tab.SUPPLIES,
                KuudraConfig::isSupplyGiantHitboxEnabled, KuudraConfig::setSupplyGiantHitboxEnabled));
        allFeatures.add(new Toggle("Lava Bobber Fix", Tab.SUPPLIES,
                KuudraConfig::isLavaBobberFixEnabled, KuudraConfig::setLavaBobberFixEnabled));
        allFeatures.add(new Toggle("Etherwarp Waypoints", Tab.SUPPLIES,
                KuudraConfig::isEtherwarpWaypointsEnabled, KuudraConfig::setEtherwarpWaypointsEnabled));
        allFeatures.add(new Toggle("Pearl Refill", Tab.MISC,
                KuudraConfig::isPearlRefillEnabled, KuudraConfig::setPearlRefillEnabled));

        // ── PEARLS ────────────────────────────────────────────────────────────
        allFeatures.add(new Toggle("Block Slot 9", Tab.SUPPLIES,
                KuudraConfig::isBlockSlot9Enabled, KuudraConfig::setBlockSlot9Enabled));
        allFeatures.add(new Toggle("Dynamic Waypoints", Tab.SUPPLIES,
                KuudraConfig::isPearlWaypointsEnabled, KuudraConfig::setPearlWaypointsEnabled));
        allFeatures.add(new Toggle("Show All Waypoints", Tab.SUPPLIES,
                KuudraConfig::isShowAllWaypoints, KuudraConfig::setShowAllWaypoints));
        allFeatures.add(new Toggle("Flat Pearls", Tab.SUPPLIES,
                KuudraConfig::isPearlFlatEnabled, KuudraConfig::setPearlFlatEnabled));
        allFeatures.add(new Toggle("Sky Pearls", Tab.SUPPLIES,
                KuudraConfig::isPearlSkyEnabled, KuudraConfig::setPearlSkyEnabled));
        allFeatures.add(new Toggle("Double Pearls", Tab.SUPPLIES,
                KuudraConfig::isPearlDoubleEnabled, KuudraConfig::setPearlDoubleEnabled));

        Slider doublePearlDelay = new Slider("Double Pearl Delay", Tab.SUPPLIES,
                () -> (KuudraConfig.getDoublePearlDelayS() - 0.05f) / 0.5f,
                v  ->  KuudraConfig.setDoublePearlDelayS(v * 0.5f + 0.05f), "") {
            @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
                ctx.text(s.font, Component.literal(name),
                        x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
                String valStr = String.format("%.2fs", KuudraConfig.getDoublePearlDelayS());
                ctx.text(s.font, Component.literal(valStr),
                        x + w - s.font.width(valStr) - 68,
                        y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
                drawTrack(ctx, x, y, w, get.get(), C_SLIDER_FG);
            }
        };
        allFeatures.add(doublePearlDelay); allSliders.add(doublePearlDelay);

        allFeatures.add(new Cycle("Waypoint Type", Tab.SUPPLIES,
                () -> KuudraConfig.getWaypointType().name().charAt(0)
                        + KuudraConfig.getWaypointType().name().substring(1).toLowerCase(),
                () -> KuudraConfig.setWaypointType(
                        KuudraConfig.getWaypointType() == KuudraConfig.WaypointType.CIRCLE
                                ? KuudraConfig.WaypointType.SQUARE
                                : KuudraConfig.WaypointType.CIRCLE)));
        allFeatures.add(new Toggle("Waypoint Fill", Tab.SUPPLIES,
                KuudraConfig::isWaypointFillEnabled, KuudraConfig::setWaypointFillEnabled));
        allFeatures.add(new Cycle("Update Frequency", Tab.SUPPLIES,
                () -> KuudraConfig.isPearlTickUpdate() ? "Per Tick" : "Per Frame",
                () -> KuudraConfig.setPearlTickUpdate(!KuudraConfig.isPearlTickUpdate())));
        allFeatures.add(new Toggle("Drop Locations", Tab.SUPPLIES,
                KuudraConfig::isDropLocationsEnabled, KuudraConfig::setDropLocationsEnabled));
        allFeatures.add(new Toggle("Pearl Timer", Tab.SUPPLIES,
                KuudraConfig::isPearlTimerEnabled, KuudraConfig::setPearlTimerEnabled));

        Slider timerHeight = new Slider("Timer Height",   Tab.SUPPLIES, KuudraConfig::getPearlTimerHeight, KuudraConfig::setPearlTimerHeight, ""); allFeatures.add(timerHeight); allSliders.add(timerHeight);
        Slider timerSize   = new Slider("Timer Size",     Tab.SUPPLIES, KuudraConfig::getPearlTimerSize,   KuudraConfig::setPearlTimerSize,   ""); allFeatures.add(timerSize);   allSliders.add(timerSize);
        Slider wpSize      = new Slider("Waypoint Size",  Tab.SUPPLIES, KuudraConfig::getPearlCircleSize,  KuudraConfig::setPearlCircleSize,  ""); allFeatures.add(wpSize);      allSliders.add(wpSize);
        Slider fillOp      = new Slider("Fill Opacity",   Tab.SUPPLIES, KuudraConfig::getWaypointFillAlpha,KuudraConfig::setWaypointFillAlpha,"%"); allFeatures.add(fillOp);      allSliders.add(fillOp);
        Slider beamOp      = new Slider("Beacon Opacity", Tab.SUPPLIES, KuudraConfig::getBeaconAlpha,       KuudraConfig::setBeaconAlpha,      "%"); allFeatures.add(beamOp);      allSliders.add(beamOp);

        allFeatures.add(new Cycle("Kuudra Talisman", Tab.SUPPLIES,
                () -> switch (KuudraConfig.getKuudraTalisman()) {
                    case NONE   -> "None";   case KIDNEY -> "Kidney";
                    case LUNG   -> "Lung";   case HEART  -> "Heart"; },
                () -> KuudraConfig.setKuudraTalisman(switch (KuudraConfig.getKuudraTalisman()) {
                    case NONE   -> KuudraConfig.KuudraTalisman.KIDNEY;
                    case KIDNEY -> KuudraConfig.KuudraTalisman.LUNG;
                    case LUNG   -> KuudraConfig.KuudraTalisman.HEART;
                    case HEART  -> KuudraConfig.KuudraTalisman.NONE; })));

        // ── DUNGEONS ──────────────────────────────────────────────────────────
        allFeatures.add(new Toggle("M7 Auto GFS Toxic",    Tab.MISC, KuudraConfig::isAutoGfsToxicEnabled,    KuudraConfig::setAutoGfsToxic));
        allFeatures.add(new Toggle("M7 Auto GFS Twilight", Tab.MISC, KuudraConfig::isAutoGfsTwilightEnabled, KuudraConfig::setAutoGfsTwilight));
        IntInput toxic    = new IntInput("Toxic Amount",    Tab.MISC, KuudraConfig::getToxicAmount,    KuudraConfig::setToxicAmount);
        IntInput twilight = new IntInput("Twilight Amount", Tab.MISC, KuudraConfig::getTwilightAmount, KuudraConfig::setTwilightAmount);
        allFeatures.add(toxic);    intInputs.add(toxic);
        allFeatures.add(twilight); intInputs.add(twilight);

        // ── MISC ──────────────────────────────────────────────────────────────
        allFeatures.add(new Toggle("Auto Requeue", Tab.MISC,
                KuudraConfig::isAutoRequeueEnabled, KuudraConfig::setAutoRequeueEnabled));
        allFeatures.add(new Toggle("Hide Falling Blocks", Tab.MISC,
                KuudraConfig::isHideFallingBlocksEnabled, KuudraConfig::setHideFallingBlocksEnabled));
        allFeatures.add(new Toggle("Hide Entity Fire", Tab.MISC,
                KuudraConfig::isHideEntityFireEnabled, KuudraConfig::setHideEntityFireEnabled));
        allFeatures.add(new Toggle("Hide Damage Title", Tab.MISC,
                KuudraConfig::isHideDamageTitleEnabled, KuudraConfig::setHideDamageTitleEnabled));
        allFeatures.add(new Toggle("Hide Dead Enemies", Tab.MISC,
                KuudraConfig::isHideDeadEntitiesEnabled, KuudraConfig::setHideDeadEntitiesEnabled));
        addRS(Tab.MISC, "Self Player Scale", 1.0f, 300.0f, "%.0f%%",
                KuudraConfig::getSelfPlayerScale, KuudraConfig::setSelfPlayerScale);
        addRS(Tab.MISC, "Other Player Scale", 1.0f, 300.0f, "%.0f%%",
                KuudraConfig::getOtherPlayerScale, KuudraConfig::setOtherPlayerScale);
        addRS(Tab.MISC, "Kuudra Mob Size", 1.0f, 200.0f, "%.0f%%",
                KuudraConfig::getKuudraSizeScale, KuudraConfig::setKuudraSizeScale);
        allFeatures.add(new Toggle("Auto Sprint", Tab.MISC,
                KuudraConfig::isAutoSprintEnabled, KuudraConfig::setAutoSprintEnabled));
        allFeatures.add(new Toggle("Hollow Wand Announcer", Tab.MISC,
                KuudraConfig::isHollowWandEnabled, KuudraConfig::setHollowWandEnabled));
        allFeatures.add(new Toggle("Hide Boss Bar", Tab.MISC,
                KuudraConfig::isHideBossBarEnabled, KuudraConfig::setHideBossBarEnabled));
        allFeatures.add(new Toggle("Hide Irrelevant Armor Stands", Tab.MISC,
                KuudraConfig::isHideArmorStandsEnabled, KuudraConfig::setHideArmorStandsEnabled));
        allFeatures.add(new Toggle("  Build Area", Tab.MISC,
                KuudraConfig::isHideArmorStandsBuild, KuudraConfig::setHideArmorStandsBuild));
        allFeatures.add(new Toggle("  Right Cannon", Tab.MISC,
                KuudraConfig::isHideArmorStandsRightCannon, KuudraConfig::setHideArmorStandsRightCannon));
        allFeatures.add(new Toggle("  Left Cannon", Tab.MISC,
                KuudraConfig::isHideArmorStandsLeftCannon, KuudraConfig::setHideArmorStandsLeftCannon));
        allFeatures.add(new Toggle("  Shop", Tab.MISC,
                KuudraConfig::isHideArmorStandsShop, KuudraConfig::setHideArmorStandsShop));
        allFeatures.add(new Toggle("  Others", Tab.MISC,
                KuudraConfig::isHideArmorStandsOthers, KuudraConfig::setHideArmorStandsOthers));
        allFeatures.add(new Toggle("Slot Binds", Tab.MISC,
                KuudraConfig::isSlotBindsEnabled, KuudraConfig::setSlotBindsEnabled));
        KeyCapture slotBindKey = new KeyCapture("  Bind Key", Tab.MISC,
                KuudraConfig::getSlotBindSetKey, KuudraConfig::setSlotBindSetKey);
        allFeatures.add(slotBindKey); captureFeatures.add(slotBindKey);
        KeyCapture slotShowKey = new KeyCapture("  Show Binds Key", Tab.MISC,
                KuudraConfig::getSlotBindShowKey, KuudraConfig::setSlotBindShowKey);
        allFeatures.add(slotShowKey); captureFeatures.add(slotShowKey);
        allFeatures.add(new Toggle("Hide Selfie Cam", Tab.MISC,
                KuudraConfig::isHideSelfieEnabled, KuudraConfig::setHideSelfieEnabled));
        allFeatures.add(new Toggle("Prevent Placing Player Heads", Tab.MISC,
                KuudraConfig::isPreventPlacingPlayerHeadsEnabled, KuudraConfig::setPreventPlacingPlayerHeadsEnabled));
        allFeatures.add(new Toggle("  Except Garden", Tab.MISC,
                KuudraConfig::isPreventPlacingPlayerHeadsExceptGarden, KuudraConfig::setPreventPlacingPlayerHeadsExceptGarden));
        allFeatures.add(new Toggle("Prevent Placing Weapons", Tab.MISC,
                KuudraConfig::isPreventPlacingWeaponsEnabled, KuudraConfig::setPreventPlacingWeaponsEnabled));
        allFeatures.add(new Toggle("Hide Elle Dialogue", Tab.MISC,
                KuudraConfig::isHideElleDialogueEnabled, KuudraConfig::setHideElleDialogue));
        allFeatures.add(new Toggle("Etherwarp Lava Block", Tab.MISC,
                KuudraConfig::isEtherwarpLavaBlockEnabled,
                v -> { if (v != KuudraConfig.isEtherwarpLavaBlockEnabled()) KuudraConfig.toggleEtherwarpLavaBlock(); }));
        allFeatures.add(new Toggle("Fast DPS Warning", Tab.STUN_DPS,
                KuudraConfig::isFastDpsWarningEnabled, KuudraConfig::setFastDpsWarningEnabled));
        allFeatures.add(new Toggle("Fast DPS Notification", Tab.STUN_DPS,
                KuudraConfig::isFastDpsNotifyEnabled, KuudraConfig::setFastDpsNotifyEnabled));
        allFeatures.add(new Toggle("Chest Tracker HUD", Tab.MISC,
                KuudraConfig::isChestTrackerVisible, KuudraConfig::setChestTrackerVisible));
        allFeatures.add(new Toggle("Solo Detector", Tab.BOSS,
                KuudraConfig::isSoloDetectorEnabled, KuudraConfig::setSoloDetectorEnabled));
        allFeatures.add(new Toggle("Solo Notification", Tab.BOSS,
                KuudraConfig::isSoloNotifyEnabled, KuudraConfig::setSoloNotifyEnabled));
        allFeatures.add(new Toggle("Kuudra HP HUD", Tab.BOSS,
                KuudraConfig::isKuudraHpHudEnabled, KuudraConfig::setKuudraHpHudEnabled));
        allFeatures.add(new Toggle("  Show Raw HP", Tab.BOSS,
                KuudraConfig::isKuudraHpShowRaw, KuudraConfig::setKuudraHpShowRaw));
        allFeatures.add(new Toggle("Mana Drain Announcer", Tab.BOSS,
                KuudraConfig::isManaDrainAnnouncerEnabled, KuudraConfig::setManaDrainAnnouncerEnabled));
        allFeatures.add(new Toggle("Shop Keybinds", Tab.MISC,
                KuudraConfig::isShopKeybindsEnabled, KuudraConfig::setShopKeybindsEnabled));
        KeyCapture mainKey   = new KeyCapture("  Main Key",   Tab.MISC, KuudraConfig::getShopMainKey,   KuudraConfig::setShopMainKey);
        KeyCapture cannonKey = new KeyCapture("  Cannon Key", Tab.MISC, KuudraConfig::getShopCannonKey, KuudraConfig::setShopCannonKey);
        allFeatures.add(mainKey);   captureFeatures.add(mainKey);
        allFeatures.add(cannonKey); captureFeatures.add(cannonKey);
        allFeatures.add(new Toggle("Wardrobe Keybinds", Tab.MISC,
                KuudraConfig::isWardrobeEnabled, KuudraConfig::setWardrobeEnabled));
        addWardrobeKeys();
        allFeatures.add(new Toggle("Exploison Hider", Tab.MISC,
                KuudraConfig::isExplosionFilterEnabled, KuudraConfig::setExplosionFilterEnabled));
        Slider expRadius = new Slider("  Hide Radius", Tab.MISC,
                KuudraConfig::getExplosionHideRadiusRaw, KuudraConfig::setExplosionHideRadius, "");
        allFeatures.add(expRadius); allSliders.add(expRadius);
        allFeatures.add(new Toggle("Party Commands", Tab.MISC,
                KuudraConfig::isPartyCmdsEnabled, KuudraConfig::setPartyCmdsEnabled));
        allFeatures.add(new Toggle("Split Timer", Tab.MISC,
                KuudraConfig::isSplitTimerEnabled, KuudraConfig::setSplitTimerEnabled));
        allFeatures.add(new Toggle("  Supply Times", Tab.MISC,
                KuudraConfig::isSupplyTimesEnabled, KuudraConfig::setSupplyTimesEnabled));
        allFeatures.add(new Toggle("Kuudra Direction", Tab.BOSS,
                KuudraConfig::isKuudraDirectionEnabled, KuudraConfig::setKuudraDirectionEnabled));
        allFeatures.add(new Toggle("Rend Damage", Tab.BOSS,
                KuudraConfig::isRendDamageEnabled, KuudraConfig::setRendDamageEnabled));
        allFeatures.add(new Toggle("Kuudra Highlight", Tab.BOSS,
                KuudraConfig::isKuudraHighlightEnabled, KuudraConfig::setKuudraHighlightEnabled));
        allFeatures.add(new Toggle("  Filled Highlight", Tab.BOSS,
                KuudraConfig::isKuudraHighlightFilled, KuudraConfig::setKuudraHighlightFilled));


        // ── ITEMS / ABOUT ─────────────────────────────────────────────────────
        buildItemsTab();
        buildAboutTab();
    }

    // ── About tab ─────────────────────────────────────────────────────────────

    private void addWardrobeKeys() {
        String[] slotLabels = {"  Slot 1","  Slot 2","  Slot 3","  Slot 4","  Slot 5",
                               "  Slot 6","  Slot 7","  Slot 8","  Slot 9"};
        for (int i = 0; i < 9; i++) {
            final int idx = i;
            KeyCapture kc = new KeyCapture(slotLabels[i], Tab.MISC,
                    () -> KuudraConfig.getWardrobeSlotKeys()[idx],
                    v  -> KuudraConfig.setWardrobeSlotKey(idx, v));
            allFeatures.add(kc); captureFeatures.add(kc);
        }
        KeyCapture openWardrobe  = new KeyCapture("  Open Wardrobe",  Tab.MISC, KuudraConfig::getWardrobeOpenKey,     KuudraConfig::setWardrobeOpenKey);
        KeyCapture openEquipment = new KeyCapture("  Open Equipment", Tab.MISC, KuudraConfig::getEquipmentOpenKey,    KuudraConfig::setEquipmentOpenKey);
        KeyCapture openPets      = new KeyCapture("  Open Pets",      Tab.MISC, KuudraConfig::getPetsOpenKey,         KuudraConfig::setPetsOpenKey);
        KeyCapture nextPage      = new KeyCapture("  Next Page",      Tab.MISC, KuudraConfig::getWardrobeNextPageKey, KuudraConfig::setWardrobeNextPageKey);
        KeyCapture prevPage      = new KeyCapture("  Prev Page",      Tab.MISC, KuudraConfig::getWardrobePrevPageKey, KuudraConfig::setWardrobePrevPageKey);
        KeyCapture unequip       = new KeyCapture("  Unequip",        Tab.MISC, KuudraConfig::getWardrobeUnequipKey,  KuudraConfig::setWardrobeUnequipKey);
        allFeatures.add(openWardrobe);  captureFeatures.add(openWardrobe);
        allFeatures.add(openEquipment); captureFeatures.add(openEquipment);
        allFeatures.add(openPets);      captureFeatures.add(openPets);
        allFeatures.add(nextPage);      captureFeatures.add(nextPage);
        allFeatures.add(prevPage);      captureFeatures.add(prevPage);
        allFeatures.add(unequip);       captureFeatures.add(unequip);
    }

    private void buildAboutTab() {
        allFeatures.add(new Feature("Current Version", Tab.ABOUT) {
            @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
                ctx.text(s.font, Component.literal("Current Version"),
                        x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
                String ver = UpdateChecker.currentVersion();
                ctx.text(s.font, Component.literal("§a" + ver),
                        x + w - s.font.width(ver) - 8,
                        y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
            }
        });

        allFeatures.add(new Feature("Latest Version", Tab.ABOUT) {
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
        });

        allFeatures.add(new Feature("Status", Tab.ABOUT) {
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
        });

        IntInput ping = new IntInput("Ping (ms)", Tab.ABOUT,
                KuudraConfig::getLowPing, KuudraConfig::setLowPing);
        allFeatures.add(ping); intInputs.add(ping);

        allFeatures.add(new Toggle("Auto Updates", Tab.ABOUT,
                KuudraConfig::isAutoUpdatesEnabled, KuudraConfig::setAutoUpdatesEnabled));

        allFeatures.add(new Button("HUD Layout", Tab.ABOUT,
                "Edit Layout",
                () -> Minecraft.getInstance().setScreen(new HudEditorScreen(KuudraScreen.this))));

        allFeatures.add(new Button("Updates", Tab.ABOUT,
                UpdateChecker.isDownloaded() ? "Restart to Install" : "Download Now",
                UpdateChecker::downloadManually));
    }

    // ── Items tab ─────────────────────────────────────────────────────────────

    private void buildItemsTab() {
        allFeatures.add(new Toggle("Item Customisation", Tab.ITEMS,
                KuudraConfig::isItemCustomizationEnabled,
                KuudraConfig::setItemCustomizationEnabled));

        allFeatures.add(labelRow("\u00a7e\u00a7lBuilt-in Types", Tab.ITEMS));

        for (ItemCustomization.ItemCategory cat : ItemCustomization.ItemCategory.values()) {
            ItemTransformSettings s = ItemCustomization.getBuiltinSettings(cat);
            String prefix = cat == ItemCustomization.ItemCategory.GLOBAL ? "Global" : cat.displayName();

            allFeatures.add(new Toggle(prefix + " enabled", Tab.ITEMS,
                    () -> s.enabled, v -> { s.enabled = v; KuudraConfig.save(); }));

            addRangeSliders(Tab.ITEMS, "  " + prefix, s);
        }

        allFeatures.add(labelRow("\u00a7a\u00a7lCustom Categories", Tab.ITEMS));

        AddCategoryFeature acf = new AddCategoryFeature();
        allFeatures.add(acf);
        categoryInputs.add(acf);

        List<ItemCustomization.CustomCategory> cats = ItemCustomization.getCustomCategories();
        for (int i = 0; i < cats.size(); i++) {
            final int idx = i;
            final ItemCustomization.CustomCategory cc = cats.get(i);

            allFeatures.add(new Feature("  \"" + cc.matchString + "\"", Tab.ITEMS) {
                @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
                    ctx.text(s.font,
                            Component.literal("\u00a7f  \"" + cc.matchString + "\""),
                            x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT);
                    int bw = 58, bh = ROW_H - 6, bx = x + w - bw - 18, by = y + 3;
                    boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
                    ctx.fill(bx, by, bx + bw, by + bh, hov ? 0xFF4A1515 : 0xFF2A0F0F);
                    ctx.fill(bx, by, bx + bw, by + 1, 0xFFCC3333);
                    ctx.centeredText(s.font, Component.literal("Remove"),
                            bx + bw / 2, by + (bh - s.font.lineHeight) / 2, 0xFFCC3333);
                }
                @Override boolean onDown(double mx, double my, int x, int y, int w) {
                    int bw = 58, bh = ROW_H - 6, bx = x + w - bw - 18, by = y + 3;
                    if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                        ItemCustomization.removeCustomCategory(idx);
                        KuudraConfig.save();
                        buildFeatures();
                        return true;
                    }
                    return false;
                }
            });

            addRangeSliders(Tab.ITEMS, "    " + cc.matchString, cc.settings);
        }
    }

    private static Feature labelRow(String text, Tab tab) {
        return new Feature(text, tab) {
            @Override void render(GuiGraphicsExtractor ctx, KuudraScreen s, int x, int y, int w, int mx, int my) {
                ctx.text(s.font, Component.literal(text),
                        x + 8, y + (ROW_H - s.font.lineHeight) / 2, C_TEXT_DIM);
            }
        };
    }
    
    private void addRangeSliders(Tab tab, String prefix, ItemTransformSettings s) {
        addRS(tab, prefix + " pos X",     -0.5f,  0.5f,  "%.3f",       () -> s.posX,       v -> { s.posX       = v; KuudraConfig.save(); });
        addRS(tab, prefix + " pos Y",     -0.5f,  0.5f,  "%.3f",       () -> s.posY,       v -> { s.posY       = v; KuudraConfig.save(); });
        addRS(tab, prefix + " pos Z",     -0.5f,  0.5f,  "%.3f",       () -> s.posZ,       v -> { s.posZ       = v; KuudraConfig.save(); });
        addRS(tab, prefix + " rot X",     -180f,  180f,  "%.0f\u00b0", () -> s.rotX,       v -> { s.rotX       = v; KuudraConfig.save(); });
        addRS(tab, prefix + " rot Y",     -180f,  180f,  "%.0f\u00b0", () -> s.rotY,       v -> { s.rotY       = v; KuudraConfig.save(); });
        addRS(tab, prefix + " rot Z",     -180f,  180f,  "%.0f\u00b0", () -> s.rotZ,       v -> { s.rotZ       = v; KuudraConfig.save(); });
        addRS(tab, prefix + " scale",     0.25f,  3.0f,  "%.2f\u00d7", () -> s.scale,      v -> { s.scale      = v; KuudraConfig.save(); });
        addRS(tab, prefix + " swing spd", 0.25f,  3.0f,  "%.2f\u00d7", () -> s.swingSpeed, v -> { s.swingSpeed = v; KuudraConfig.save(); });
        addRS(tab, prefix + " proximity", -1.0f,  1.0f,  "%.3f",       () -> s.proximity,  v -> { s.proximity  = v; KuudraConfig.save(); });
    }

    private void addRS(Tab tab, String name, float min, float max, String fmt,
                       Supplier<Float> get, Consumer<Float> set) {
        RangeSlider rs = new RangeSlider(name, tab, min, max, fmt, get, set);
        allFeatures.add(rs);
        allSliders.add(rs);
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private EditBox searchField;
    private String          query       = "";
    private int             scroll      = 0;
    private Feature         dragFeature = null;
    private int             dragX, dragY, dragW;

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

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isDoubleClick) {
        // If a KeyCapture is waiting for input, bind this mouse button to it
        for (KeyCapture kc : captureFeatures) {
            if (kc.capturing) { kc.onMouseButton(click.button()); return true; }
        }

        if (super.mouseClicked(click, isDoubleClick)) return true;
        if (click.button() != 0) return false;

        double mx = click.x(), my = click.y();

        Tab[] tabs = Tab.values();
        int tabH = 28, startY = py() + HEADER_H + 6;
        for (int i = 0; i < tabs.length; i++) {
            int ty = startY + i * (tabH + 2);
            if (mx >= px() && mx <= px() + SIDEBAR_W - 1 && my >= ty && my <= ty + tabH) {
                if (tabs[i] != currentTab) {
                    intInputs.forEach(Feature::cancel);
                    categoryInputs.forEach(Feature::cancel);
                    currentTab = tabs[i];
                    tabAnim = 0f;
                    scroll = 0;
                }
                return true;
            }
        }

        intInputs.forEach(Feature::cancel);
        categoryInputs.forEach(f -> { if (f.focused) f.focused = false; });

        Feature hit = featureAt(mx, my);
        if (hit != null) {
            int[] row = rowOf(hit);
            if (hit.onDown(mx, my, row[0], row[1], row[2])) {
                dragFeature = hit;
                dragX = row[0]; dragY = row[1]; dragW = row[2];
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (dragFeature != null) { dragFeature.onDrag(click.x(), click.y(), dragX, dragY, dragW); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragFeature != null) { dragFeature.onUp(); dragFeature = null; }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
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

        intInputs.forEach(f -> f.onKey(key));

        if (key == 256) {
            if (intInputs.stream().anyMatch(Feature::isCapturing)) return true;
            this.onClose(); return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent input) {
        if (searchField != null && searchField.isFocused()) return super.charTyped(input);
        for (AddCategoryFeature af : categoryInputs) {
            if (af.focused) { af.onChar((char) input.codepoint()); return true; }
        }
        intInputs.forEach(f -> f.onChar((char) input.codepoint()));
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

        int px = px(), py = py();
        ctx.fill(px - 1, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, C_BORDER);
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, C_BG);

        ctx.fill(px, py, px + PANEL_W, py + HEADER_H, C_HEADER);
        ctx.fill(px, py + HEADER_H - 1, px + PANEL_W, py + HEADER_H, C_ACCENT);
        ctx.text(font, Component.literal("\u2726 PhantomAddons"),
                px + 10, py + (HEADER_H - font.lineHeight) / 2, C_ACCENT);

        int sfX = px + PANEL_W - 148, sfY = py + (HEADER_H - 16) / 2;
        ctx.fill(sfX - 3, sfY - 1, sfX + 141, sfY + 15, 0xFF090B0E);
        ctx.fill(sfX - 3, sfY + 14, sfX + 141, sfY + 15, C_BORDER);

        ctx.fill(px, py + HEADER_H, px + SIDEBAR_W, py + PANEL_H, C_SIDEBAR);
        ctx.fill(px + SIDEBAR_W - 1, py + HEADER_H, px + SIDEBAR_W, py + PANEL_H, C_BORDER);
        renderTabs(ctx, px, py, mx, my);
        renderContent(ctx, mx, my);
        super.extractRenderState(ctx, mx, my, delta);
    }

    private void renderTabs(GuiGraphicsExtractor ctx, int px, int py, int mx, int my) {
        Tab[] tabs = Tab.values();
        int tabH = 28, startY = py + HEADER_H + 6;
        for (int i = 0; i < tabs.length; i++) {
            Tab t = tabs[i];
            int tx = px, ty = startY + i * (tabH + 2);
            boolean active = t == currentTab && query.isEmpty();
            boolean hover  = !active && mx >= tx && mx <= tx + SIDEBAR_W - 1 && my >= ty && my <= ty + tabH;
            if (active) {
                ctx.fill(tx, ty, tx + SIDEBAR_W - 1, ty + tabH, C_TAB_ACTIVE);
                ctx.fill(tx, ty, tx + 2, ty + tabH, C_ACCENT);
            } else if (hover) {
                ctx.fill(tx, ty, tx + SIDEBAR_W - 1, ty + tabH, C_TAB_HOVER);
            }
            ctx.text(font, Component.literal(t.label),
                    tx + 12, ty + (tabH - font.lineHeight) / 2,
                    active ? C_ACCENT : hover ? C_TEXT : C_TEXT_DIM);
        }
    }

    private void renderContent(GuiGraphicsExtractor ctx, int mx, int my) {
        int cx = cx(), cy = cy(), cw = cw(), ch = ch();
        List<Feature> visible = visibleFeatures();

        if (visible.isEmpty()) {
            ctx.centeredText(font,
                    Component.literal(query.isEmpty() ? "No features" : "No results"),
                    cx + cw / 2, cy + ch / 2, C_TEXT_DIM);
            return;
        }

        int totalH    = visible.size() * (ROW_H + ROW_GAP) - ROW_GAP;
        int maxScroll = Math.max(0, totalH - ch + PAD * 2);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        float ease   = tabAnim * tabAnim * (3f - 2f * tabAnim);
        int   slideX = (int)((1f - ease) * 16);

        int previewH = (currentTab == Tab.LAVA_TWEAKS && query.isEmpty()) ? 22 : 0;
        ctx.enableScissor(cx, cy, cx + cw, cy + ch - previewH);

        int y = cy + PAD - scroll;
        for (Feature f : visible) {
            if (y + ROW_H > cy && y < cy + ch - previewH) {
                ctx.fill(cx + PAD, y, cx + cw - PAD, y + ROW_H, 0x0AFFFFFF);
                f.render(ctx, this, cx + PAD + slideX, y, cw - PAD * 2 - slideX, mx, my);
            }
            y += ROW_H + ROW_GAP;
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            int sbX = cx + cw - 5, sbH = ch - PAD * 2;
            int thumbH = Math.max(18, sbH * sbH / (totalH + PAD * 2));
            int thumbY = cy + PAD + (int)((float)scroll / maxScroll * (sbH - thumbH));
            ctx.fill(sbX, cy + PAD, sbX + 3, cy + PAD + sbH, 0x1AFFFFFF);
            ctx.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, 0x77AABBDD);
        }

        if (currentTab == Tab.LAVA_TWEAKS && query.isEmpty()) {
            int sw = cw - PAD * 2, swY = cy + ch - 22, swX = cx + PAD;
            int previewArgb = com.kuudrahelper.features.lava.ColorPreviewHelper.computePreviewColor();
            ctx.fill(swX, swY, swX + sw, swY + 14, previewArgb);
            ctx.fill(swX, swY, swX + sw, swY + 1, 0x22FFFFFF);
            ctx.centeredText(font, Component.literal("Colour Preview"),
                    swX + sw / 2, swY + (14 - font.lineHeight) / 2, 0xCCFFFFFF);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Feature> visibleFeatures() {
        List<Feature> out = new ArrayList<>();
        for (Feature f : allFeatures)
            if (query.isEmpty() ? f.tab == currentTab : f.name.toLowerCase().contains(query))
                out.add(f);
        return out;
    }

    private Feature featureAt(double mx, double my) {
        int cx = cx(), cy = cy(), cw = cw(), ch = ch();
        if (mx < cx || mx > cx + cw || my < cy || my > cy + ch) return null;
        int y = cy + PAD - scroll;
        for (Feature f : visibleFeatures()) {
            if (mx >= cx + PAD && mx <= cx + cw - PAD && my >= y && my <= y + ROW_H) return f;
            y += ROW_H + ROW_GAP;
        }
        return null;
    }

    private int[] rowOf(Feature target) {
        int y = cy() + PAD - scroll;
        for (Feature f : visibleFeatures()) {
            if (f == target) return new int[]{cx() + PAD, y, cw() - PAD * 2};
            y += ROW_H + ROW_GAP;
        }
        return new int[]{cx() + PAD, cy() + PAD, cw() - PAD * 2};
    }

    @Override public void removed()        { KuudraConfig.save(); }
    @Override public boolean isPauseScreen() { return false; }
}