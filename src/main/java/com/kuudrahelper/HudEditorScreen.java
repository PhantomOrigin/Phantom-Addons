package com.kuudrahelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {

    private static final int N = 8;

    private static final String[] LABELS = {
        "Mount Timer", "Kuudra Direction", "Split Timer", "Pearl Title",
        "Build Progress", "Notifications", "Crate Priority", "Kuudra HP"
    };

    private static final int[] BASE_W = { 60, 130, 135, 130, 155, 160, 110, 165 };
    private static final int[] BASE_H = { 30,  36,  95,  28,  28,   28,  18,  22 };

    private static final float[] PREVIEW_SCALE = { 3.0f, 3.5f, 1.0f, 1.5f, 1.5f, 1.0f, 1.0f, 1.0f };

    private static final boolean[] IS_CENTERED = { true, true, false, true, true, true, true, true };

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
        px[0] = KuudraConfig.getMountTimerHudX();
        py[0] = KuudraConfig.getMountTimerHudY();
        ps[0] = KuudraConfig.getMountTimerHudScale();

        px[1] = KuudraConfig.getDirectionHudX();
        py[1] = KuudraConfig.getDirectionHudY();
        ps[1] = KuudraConfig.getDirectionHudScale();

        px[2] = KuudraConfig.getSplitHudX();
        py[2] = KuudraConfig.getSplitHudY();
        ps[2] = KuudraConfig.getSplitHudScale();

        px[3] = KuudraConfig.getPearlTitleHudX();
        py[3] = KuudraConfig.getPearlTitleHudY();
        ps[3] = KuudraConfig.getPearlTitleHudScale();

        px[4] = KuudraConfig.getBuildProgressHudX();
        py[4] = KuudraConfig.getBuildProgressHudY();
        ps[4] = KuudraConfig.getBuildProgressHudScale();

        px[5] = KuudraConfig.getNotificationHudX();
        py[5] = KuudraConfig.getNotificationHudY();
        ps[5] = KuudraConfig.getNotificationHudScale();

        px[6] = KuudraConfig.getCratePriorityHudX();
        py[6] = KuudraConfig.getCratePriorityHudY();
        ps[6] = KuudraConfig.getCratePriorityHudScale();

        px[7] = KuudraConfig.getKuudraHpHudX();
        py[7] = KuudraConfig.getKuudraHpHudY();
        ps[7] = KuudraConfig.getKuudraHpHudScale();
    }

    @Override
    public void onClose() {
        KuudraConfig.setMountTimerHudX(px[0]);
        KuudraConfig.setMountTimerHudY(py[0]);
        KuudraConfig.setMountTimerHudScale(ps[0]);

        KuudraConfig.setDirectionHudX(px[1]);
        KuudraConfig.setDirectionHudY(py[1]);
        KuudraConfig.setDirectionHudScale(ps[1]);

        KuudraConfig.setSplitHudX(px[2]);
        KuudraConfig.setSplitHudY(py[2]);
        KuudraConfig.setSplitHudScale(ps[2]);

        KuudraConfig.setPearlTitleHudX(px[3]);
        KuudraConfig.setPearlTitleHudY(py[3]);
        KuudraConfig.setPearlTitleHudScale(ps[3]);

        KuudraConfig.setBuildProgressHudX(px[4]);
        KuudraConfig.setBuildProgressHudY(py[4]);
        KuudraConfig.setBuildProgressHudScale(ps[4]);

        KuudraConfig.setNotificationHudX(px[5]);
        KuudraConfig.setNotificationHudY(py[5]);
        KuudraConfig.setNotificationHudScale(ps[5]);

        KuudraConfig.setCratePriorityHudX(px[6]);
        KuudraConfig.setCratePriorityHudY(py[6]);
        KuudraConfig.setCratePriorityHudScale(ps[6]);

        KuudraConfig.setKuudraHpHudX(px[7]);
        KuudraConfig.setKuudraHpHudY(py[7]);
        KuudraConfig.setKuudraHpHudScale(ps[7]);

        KuudraConfig.save();
        minecraft.setScreen(parent);
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
            case 0 -> { // Mount Timer
                m.translate(bx + bw / 2f, by + bh / 2f);
                m.scale(s, s);
                String txt = "182";
                ctx.text(font, txt, -font.width(txt) / 2, -font.lineHeight / 2, 0xFFFFFFFF, true);
            }
            case 1 -> { // Kuudra Direction
                m.translate(bx + bw / 2f, by + bh / 2f);
                m.scale(s, s);
                String txt = "RIGHT!";
                ctx.text(font, txt, -font.width(txt) / 2, -font.lineHeight / 2, 0xFFFFFFFF, true);
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
            case 3 -> { // Pearl Title
                m.translate(bx + bw / 2f, by + bh / 2f);
                m.scale(s, s);
                String txt = "§6[||||||||||||||||||||] §e75%";
                ctx.text(font, txt, -font.width(txt) / 2, -font.lineHeight / 2, 0xFFFFFFFF, true);
            }
            case 4 -> { // Build Progress
                m.translate(bx + bw / 2f, by + bh / 2f - font.lineHeight / 2f);
                m.scale(s, s);
                String h = "§e§lBuild Progress:";
                String b = "Progress: 67%";
                ctx.text(font, h, -font.width(h) / 2, 0, 0xFFFFFFFF, true);
                ctx.text(font, b, -font.width(b) / 2, font.lineHeight + 2, 0xFFFFFFFF, true);
            }
            case 5 -> { // Notifications (shows both countdown and notification example)
                m.translate(bx + bw / 2f, by + bh / 2f - font.lineHeight / 2f);
                m.scale(s, s);
                String cd = "§eSupplies Spawn: §f6.43s";
                String n  = "§cFAST DPS!";
                ctx.text(font, cd, -font.width(cd) / 2, 0, 0xFFFFFFFF, true);
                ctx.text(font, n,  -font.width(n)  / 2, font.lineHeight + 2, 0xFFFFFFFF, true);
            }
            case 6 -> { // Crate Priority
                m.translate(bx + bw / 2f, by + bh / 2f);
                m.scale(s, s);
                String txt = "§eGo Square!";
                ctx.text(font, txt, -font.width(txt) / 2, -font.lineHeight / 2, 0xFFFFFFFF, true);
            }
            case 7 -> { // Kuudra HP
                m.translate(bx + bw / 2f, by + bh / 2f);
                m.scale(s, s);
                String txt = "§cKuudra §f75.0%";
                ctx.text(font, txt, -font.width(txt) / 2, -font.lineHeight - 2, 0xFFFFFFFF, true);
                int barW = 160;
                ctx.fill(-barW / 2, 0, barW / 2, 8, 0xAA000000);
                ctx.fill(-barW / 2, 0, barW / 4, 8, 0xFF44AA44);
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
        return IS_CENTERED[i] ? (int)(px[i] * width)  - bw / 2 : (int)(px[i] * width);
    }
    private int boxY(int i) {
        int bh = boxH(i);
        return IS_CENTERED[i] ? (int)(py[i] * height) - bh / 2 : (int)(py[i] * height);
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
    }

    private static float clamp01(float v)    { return Math.max(0f, Math.min(1f, v)); }
    private static float clampScale(float v) { return Math.max(0.2f, Math.min(3f, v)); }
}
