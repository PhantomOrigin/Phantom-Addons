package com.phantomaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {

    private static final int N = 11;

    private static final String[] LABELS = {
        "Mount Timer", "Kuudra Direction", "Split Timer", "Crate Pickup Progress",
        "Build Progress", "Notifications", "Crate Priority", "Kuudra HP",
        "Profit Tracker", "Chest Value", "Backbone Progress Bar"
    };

    // Sizes reflect each element's real rendered footprint at scale = 1.0, so the preview
    // box lines up with where the real HUD element actually appears in-game.
    // Note: the vanilla Minecraft font renders '|' at only ~2px wide (much narrower than a
    // letter/digit's ~6px), so bar-style strings like "[||||||||||||||||||||] 100%" are far
    // narrower than a naive average-character-width guess would suggest.
    private static final int[] BASE_W = { 60, 110, 180, 290, 100, 160, 110, 165, 158, 140, 235 };
    private static final int[] BASE_H = { 30,  32,  95,  36,  22,  38,  18,  22, 125,  90,  28 };

    // PREVIEW_SCALE must equal each element's real internal scale multiplier (the BASE_SCALE
    // constant in its own renderer) so the preview text visually fills the box that was sized
    // for that same real footprint — a mismatch here is why text used to look tiny inside an
    // otherwise correctly-sized box.
    private static final float[] PREVIEW_SCALE = { 3.0f, 3.5f, 1.0f, 4.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 3.0f };

    // Whether the real renderer centers its content on the X / Y axis at the configured
    // point, or draws from a top-left corner. Most elements only center horizontally and
    // draw downward from the top (Y not centered) — only Notifications and Crate Priority
    // truly center on both axes.
    private static final boolean[] IS_X_CENTERED = { true, true, false, true, true, true, true, true, false, false, true };
    private static final boolean[] IS_Y_CENTERED = { false, false, false, false, false, true, true, false, false, false, false };

    private final float[] px = new float[N];
    private final float[] py = new float[N];
    private final float[] ps = new float[N];

    private int    draggingIdx = -1;
    private double dragStartMX, dragStartMY;
    private float  dragStartPx, dragStartPy;

    private final Screen parent;

    private static final String[] SPLIT_LINES = {
        "§b§lKuudra Splits:",
        "§bSupplies:§r 23.45s  §a[-1.2]",
        "§bBuild:§r 45.67s  §c[+2.1]",
        "§bEaten:§r 12.34s",
        "§bStun:§r 5.67s",
        "§bDPS:§r 8.90s",
        "§bBoss:§r 34.56s",
        "§bOverall:§r 2:10s",
        "§bPredicted:§r 2:08s"
    };

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Layout Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        px[0] = PhantomConfig.getMountTimerHudX();
        py[0] = PhantomConfig.getMountTimerHudY();
        ps[0] = PhantomConfig.getMountTimerHudScale();

        px[1] = PhantomConfig.getDirectionHudX();
        py[1] = PhantomConfig.getDirectionHudY();
        ps[1] = PhantomConfig.getDirectionHudScale();

        px[2] = PhantomConfig.getSplitHudX();
        py[2] = PhantomConfig.getSplitHudY();
        ps[2] = PhantomConfig.getSplitHudScale();

        px[3] = PhantomConfig.getPearlTitleHudX();
        py[3] = PhantomConfig.getPearlTitleHudY();
        ps[3] = PhantomConfig.getPearlTitleHudScale();

        px[4] = PhantomConfig.getBuildProgressHudX();
        py[4] = PhantomConfig.getBuildProgressHudY();
        ps[4] = PhantomConfig.getBuildProgressHudScale();

        px[5] = PhantomConfig.getNotificationHudX();
        py[5] = PhantomConfig.getNotificationHudY();
        ps[5] = PhantomConfig.getNotificationHudScale();

        px[6] = PhantomConfig.getCratePriorityHudX();
        py[6] = PhantomConfig.getCratePriorityHudY();
        ps[6] = PhantomConfig.getCratePriorityHudScale();

        px[7] = PhantomConfig.getKuudraHpHudX();
        py[7] = PhantomConfig.getKuudraHpHudY();
        ps[7] = PhantomConfig.getKuudraHpHudScale();

        px[8] = PhantomConfig.getProfitHudX();
        py[8] = PhantomConfig.getProfitHudY();
        ps[8] = PhantomConfig.getProfitHudScale();

        px[9] = PhantomConfig.getChestValueHudX();
        py[9] = PhantomConfig.getChestValueHudY();
        ps[9] = PhantomConfig.getChestValueHudScale();

        px[10] = PhantomConfig.getBackboneProgressBarHudX();
        py[10] = PhantomConfig.getBackboneProgressBarHudY();
        ps[10] = PhantomConfig.getBackboneProgressBarHudScale();
    }

    @Override
    public void onClose() {
        PhantomConfig.setMountTimerHudX(px[0]);
        PhantomConfig.setMountTimerHudY(py[0]);
        PhantomConfig.setMountTimerHudScale(ps[0]);

        PhantomConfig.setDirectionHudX(px[1]);
        PhantomConfig.setDirectionHudY(py[1]);
        PhantomConfig.setDirectionHudScale(ps[1]);

        PhantomConfig.setSplitHudX(px[2]);
        PhantomConfig.setSplitHudY(py[2]);
        PhantomConfig.setSplitHudScale(ps[2]);

        PhantomConfig.setPearlTitleHudX(px[3]);
        PhantomConfig.setPearlTitleHudY(py[3]);
        PhantomConfig.setPearlTitleHudScale(ps[3]);

        PhantomConfig.setBuildProgressHudX(px[4]);
        PhantomConfig.setBuildProgressHudY(py[4]);
        PhantomConfig.setBuildProgressHudScale(ps[4]);

        PhantomConfig.setNotificationHudX(px[5]);
        PhantomConfig.setNotificationHudY(py[5]);
        PhantomConfig.setNotificationHudScale(ps[5]);

        PhantomConfig.setCratePriorityHudX(px[6]);
        PhantomConfig.setCratePriorityHudY(py[6]);
        PhantomConfig.setCratePriorityHudScale(ps[6]);

        PhantomConfig.setKuudraHpHudX(px[7]);
        PhantomConfig.setKuudraHpHudY(py[7]);
        PhantomConfig.setKuudraHpHudScale(ps[7]);

        PhantomConfig.setProfitHudX(px[8]);
        PhantomConfig.setProfitHudY(py[8]);
        PhantomConfig.setProfitHudScale(ps[8]);

        PhantomConfig.setChestValueHudX(px[9]);
        PhantomConfig.setChestValueHudY(py[9]);
        PhantomConfig.setChestValueHudScale(ps[9]);

        PhantomConfig.setBackboneProgressBarHudX(px[10]);
        PhantomConfig.setBackboneProgressBarHudY(py[10]);
        PhantomConfig.setBackboneProgressBarHudScale(ps[10]);

        PhantomConfig.save();
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xA8000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        extractBackground(ctx, mx, my, delta);

        ctx.text(font, Component.literal("§bHUD Layout Editor"), 8, 8, 0xFFFFFFFF);
        ctx.text(font, Component.literal("§7Drag to reposition  •  Scroll to resize  •  ESC to save & exit"), 8, 18, 0xFFAAAAAA);

        int hoveredIdx = -1;
        for (int i = 0; i < N; i++) {
            int bx = boxX(i), by = boxY(i), bw = boxW(i), bh = boxH(i);
            boolean hover    = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
            boolean dragging = (i == draggingIdx);
            if (hover) hoveredIdx = i;

            int bg     = dragging ? 0x900055FF : (hover ? 0x600055FF : 0x40003388);
            int border = (dragging || hover)    ? 0xFF00BBFF : 0xFF003E99;

            ctx.fill(bx, by, bx + bw, by + bh, bg);
            ctx.fill(bx,          by,          bx + bw,     by + 1,      border);
            ctx.fill(bx,          by + bh - 1, bx + bw,     by + bh,     border);
            ctx.fill(bx,          by,          bx + 1,      by + bh,     border);
            ctx.fill(bx + bw - 1, by,          bx + bw,     by + bh,     border);

            renderPreview(ctx, i, bx, by, bw, bh);

            String scaleStr = String.format("%.1f×", ps[i]);
            ctx.text(font, Component.literal("§7" + scaleStr), bx + 2, by + bh - font.lineHeight - 1, 0xFFFFFFFF);
        }

        if (hoveredIdx >= 0) {
            int i  = hoveredIdx;
            int bx = boxX(i), by = boxY(i), bw = boxW(i);
            String label = LABELS[i];
            int tw = font.width(label);
            int tx = bx + (bw - tw) / 2;
            int ty = by - font.lineHeight - 5;
            if (ty < 4) ty = boxY(i) + boxH(i) + 5;

            ctx.fill(tx - 3, ty - 2, tx + tw + 3, ty + font.lineHeight + 2, 0xFF111111);
            ctx.fill(tx - 3, ty + font.lineHeight + 2, tx + tw + 3, ty + font.lineHeight + 3, 0xFF00AAFF);
            ctx.text(font, Component.literal(label), tx, ty, 0xFFFFFFFF);
        }

        int coordIdx = draggingIdx >= 0 ? draggingIdx : hoveredIdx;
        if (coordIdx >= 0) {
            float cx = (px[coordIdx] - 0.5f) * width;
            float cy = (py[coordIdx] - 0.5f) * height;
            String coordStr = String.format("x: %+.0f  y: %+.0f", cx, cy);
            int cw = font.width(coordStr);
            int cx2 = (width - cw) / 2;
            int cy2 = height - 36;
            ctx.fill(cx2 - 4, cy2 - 2, cx2 + cw + 4, cy2 + font.lineHeight + 2, 0xCC111111);
            ctx.text(font, Component.literal("§e" + coordStr), cx2, cy2, 0xFFFFFFFF);
        }

        int rx = width - 82, ry = height - 22;
        boolean rHov = mx >= rx && mx <= rx + 72 && my >= ry && my <= ry + 14;
        ctx.fill(rx, ry, rx + 72, ry + 14, rHov ? 0xC0444444 : 0x80333333);
        ctx.fill(rx, ry, rx + 72, ry + 1, 0xFF666666);
        ctx.text(font, Component.literal("Reset Defaults"), rx + 4, ry + 3, 0xFFDDDDDD);

        super.extractRenderState(ctx, mx, my, delta);
    }

    private void renderPreview(GuiGraphicsExtractor ctx, int i, int bx, int by, int bw, int bh) {
        var m = ctx.pose();
        m.pushMatrix();

        float s = PREVIEW_SCALE[i] * ps[i];

        switch (i) {
            case 0 -> { // Mount Timer — real renderer only centers X, draws downward from the top
                m.translate(bx + bw / 2f, by);
                m.scale(s, s);
                String txt = "182";
                ctx.text(font, txt, -font.width(txt) / 2, 0, 0xFFFFFFFF, true);
            }
            case 1 -> { // Kuudra Direction — same top-anchored behaviour as Mount Timer
                m.translate(bx + bw / 2f, by);
                m.scale(s, s);
                String txt = "RIGHT!";
                ctx.text(font, txt, -font.width(txt) / 2, 0, 0xFFFFFFFF, true);
            }
            case 2 -> { // Split Timer
                m.translate(bx + 2, by + 2);
                m.scale(s, s);
                int y = 0;
                for (String line : SPLIT_LINES) {
                    ctx.text(font, line, 0, y, 0xFFFFFFFF, true);
                    y += 10;
                }
            }
            case 3 -> { // Crate Pickup Progress ("Pearl Title") — top-anchored like the others
                m.translate(bx + bw / 2f, by);
                m.scale(s, s);
                String txt = "§6[||||||||||||||||||||] §e75%";
                ctx.text(font, txt, -font.width(txt) / 2, 0, 0xFFFFFFFF, true);
            }
            case 4 -> { // Build Progress
                m.translate(bx + bw / 2f, by);
                m.scale(s, s);
                String h = "§e§lBuild Progress:";
                String b = "Progress: 67%";
                ctx.text(font, h, -font.width(h) / 2, 0, 0xFFFFFFFF, true);
                ctx.text(font, b, -font.width(b) / 2, font.lineHeight + 2, 0xFFFFFFFF, true);
            }
            case 5 -> { // Notifications — one shared location for every countdown/alert text
                m.translate(bx + bw / 2f, by + bh / 2f);
                m.scale(s, s);
                String txt = "§eNOTIFICATION";
                ctx.text(font, txt, -font.width(txt) / 2, -font.lineHeight / 2, 0xFFFFFFFF, true);
            }
            case 6 -> { // Crate Priority
                m.translate(bx + bw / 2f, by + bh / 2f);
                m.scale(s, s);
                String txt = "§eGo Square!";
                ctx.text(font, txt, -font.width(txt) / 2, -font.lineHeight / 2, 0xFFFFFFFF, true);
            }
            case 7 -> { // Kuudra HP — the real renderer's origin sits at the bar's top, with the
                        // label drawn ABOVE it (negative y); box top must offset for that label height.
                int labelH = font.lineHeight + 2;
                m.translate(bx + bw / 2f, by + labelH);
                m.scale(s, s);
                String txt = "§cKuudra §f75.0%";
                ctx.text(font, txt, -font.width(txt) / 2, -labelH, 0xFFFFFFFF, true);
                int barW = 160;
                ctx.fill(-barW / 2, 0, barW / 2, 8, 0xAA000000);
                ctx.fill(-barW / 2, 0, barW / 4, 8, 0xFF44AA44);
            }
            case 8 -> { // Profit Tracker
                m.translate(bx + 2, by + 2);
                m.scale(s, s);
                String[] lines = {
                    "§6§lProfit Tracker §7(Session)",
                    "§bTotal Gains:§r §f332M",
                    "§bTotal Expenses:§r §c-225M",
                    "§7  Items: §f125M",
                    "§7  Essence: §f172M",
                    "§7  Keys: §c-180M",
                    "§7  Kismets: §c-45M",
                    "§7  Wheel of Fate: §c-20M",
                    "§bAvg Time:§r §f1:00",
                    "§bTotal Profit:§r §a107M",
                    "§bTotal Runs:§r §f60",
                    "§bProfit/Hour:§r §f107M/H"
                };
                int y = 0;
                for (String line : lines) { ctx.text(font, line, 0, y, 0xFFFFFFFF, true); y += 10; }
            }
            case 9 -> { // Chest Value
                m.translate(bx + 2, by + 2);
                m.scale(s, s);
                String[] lines = {
                    "§6§lChest Value §7(T5)",
                    "§bChest Value:§r §f50.0M",
                    "§7  Items: §f35.0M",
                    "§7  Essence: §f15.0M",
                    "§bExpenses:§r §c-12.0M",
                    "§7  Key: §c-10.0M",
                    "§bChest Profit:§r §a38.0M"
                };
                int y = 0;
                for (String line : lines) { ctx.text(font, line, 0, y, 0xFFFFFFFF, true); y += 10; }
            }
            case 10 -> { // Backbone Progress Bar — top-anchored like the others
                m.translate(bx + bw / 2f, by);
                m.scale(s, s);
                String txt = "§8[§6||||||||||||§f||||||||§8] §b60%";
                ctx.text(font, txt, -font.width(txt) / 2, 0, 0xFFFFFFFF, true);
            }
        }

        m.popMatrix();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isDoubleClick) {
        if (click.button() != 0) return super.mouseClicked(click, isDoubleClick);

        double mx = click.x(), my = click.y();

        int rx = width - 82, ry = height - 22;
        if (mx >= rx && mx <= rx + 72 && my >= ry && my <= ry + 14) {
            resetDefaults();
            return true;
        }

        for (int i = 0; i < N; i++) {
            int bx = boxX(i), by = boxY(i), bw = boxW(i), bh = boxH(i);
            if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                draggingIdx = i;
                dragStartMX = mx;
                dragStartMY = my;
                dragStartPx = px[i];
                dragStartPy = py[i];
                return true;
            }
        }
        draggingIdx = -1;
        return super.mouseClicked(click, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (draggingIdx >= 0 && click.button() == 0) {
            px[draggingIdx] = clamp01(dragStartPx + (float)(click.x() - dragStartMX) / width);
            py[draggingIdx] = clamp01(dragStartPy + (float)(click.y() - dragStartMY) / height);
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.button() == 0) draggingIdx = -1;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        for (int i = 0; i < N; i++) {
            int bx = boxX(i), by = boxY(i), bw = boxW(i), bh = boxH(i);
            if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                ps[i] = clampScale(ps[i] + (float)vScroll * 0.1f);
                return true;
            }
        }
        return super.mouseScrolled(mx, my, hScroll, vScroll);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ── Box geometry ──────────────────────────────────────────────────────────

    private int boxW(int i) { return Math.max(10, (int)(BASE_W[i] * ps[i])); }
    private int boxH(int i) { return Math.max(10, (int)(BASE_H[i] * ps[i])); }

    private int boxX(int i) {
        int bw = boxW(i);
        return IS_X_CENTERED[i] ? (int)(px[i] * width)  - bw / 2 : (int)(px[i] * width);
    }
    private int boxY(int i) {
        int bh = boxH(i);
        if (i == 7) { // Kuudra HP: real origin is the bar's top, label sits above it
            int labelH = Math.round((font.lineHeight + 2) * ps[i]);
            return (int)(py[i] * height) - labelH;
        }
        return IS_Y_CENTERED[i] ? (int)(py[i] * height) - bh / 2 : (int)(py[i] * height);
    }

    private void resetDefaults() {
        px[0] = 0.5f;   py[0] = 0.56f; ps[0] = 1.0f;
        px[1] = 0.5f;   py[1] = 0.25f; ps[1] = 1.0f;
        px[2] = 0.005f; py[2] = 0.01f; ps[2] = 1.0f;
        px[3] = 0.5f;   py[3] = 0.5f;  ps[3] = 1.0f;
        px[4] = 0.5f;   py[4] = 0.45f; ps[4] = 1.0f;
        px[5] = 0.5f;   py[5] = 0.15f; ps[5] = 1.5f;
        px[6] = 0.5f;   py[6] = 0.6f;  ps[6] = 2.0f;
        px[7] = 0.5f;   py[7] = 0.07f; ps[7] = 1.0f;
        px[8] = 0.01f;  py[8] = 0.5f;  ps[8] = 1.0f;
        px[9] = 0.3f;   py[9] = 0.3f;  ps[9] = 1.0f;
        px[10] = 0.5f;  py[10] = 0.6f; ps[10] = 1.0f;
    }

    private static float clamp01(float v)    { return Math.max(0f, Math.min(1f, v)); }
    private static float clampScale(float v) { return Math.max(0.2f, Math.min(3f, v)); }
}
