package com.kuudrahelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {

    private static final int N = 9;

    // Labels shown in tooltip on hover
    private static final String[] LABELS = {
        "Mount Timer", "Kuudra Direction", "Split Timer", "Pearl Title",
        "Supply Progress", "Build Progress", "Notifications", "Crate Priority", "Kuudra HP"
    };

    // Approximate box size (px) at configScale = 1.0, before ps multiplier.
    // Sized to contain the preview content below.
    private static final int[] BASE_W = { 60, 130, 135, 130, 160, 155, 90, 110, 165 };
    private static final int[] BASE_H = { 30,  36,  95,  28,  28,  28,  18,  18,  22 };

    // Preview render scale for each element at ps = 1.0
    private static final float[] PREVIEW_SCALE = { 3.0f, 3.5f, 1.0f, 1.5f, 1.5f, 1.5f, 1.0f, 1.0f, 1.0f };

    // True if the element's stored position is its visual CENTER (false = top-left corner)
    private static final boolean[] IS_CENTERED = { true, true, false, true, true, true, true, true, true };

    // Working copies committed to config on close
    private final float[] px = new float[N];
    private final float[] py = new float[N];
    private final float[] ps = new float[N];

    // Drag state
    private int    draggingIdx = -1;
    private double dragStartMX, dragStartMY;
    private float  dragStartPx, dragStartPy;

    private final Screen parent;

    // Split timer preview lines
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

        px[4] = KuudraConfig.getSupplyProgressHudX();
        py[4] = KuudraConfig.getSupplyProgressHudY();
        ps[4] = KuudraConfig.getSupplyProgressHudScale();

        px[5] = KuudraConfig.getBuildProgressHudX();
        py[5] = KuudraConfig.getBuildProgressHudY();
        ps[5] = KuudraConfig.getBuildProgressHudScale();

        px[6] = KuudraConfig.getNotificationHudX();
        py[6] = KuudraConfig.getNotificationHudY();
        ps[6] = KuudraConfig.getNotificationHudScale();

        px[7] = KuudraConfig.getCratePriorityHudX();
        py[7] = KuudraConfig.getCratePriorityHudY();
        ps[7] = KuudraConfig.getCratePriorityHudScale();

        px[8] = KuudraConfig.getKuudraHpHudX();
        py[8] = KuudraConfig.getKuudraHpHudY();
        ps[8] = KuudraConfig.getKuudraHpHudScale();
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

        KuudraConfig.setSupplyProgressHudX(px[4]);
        KuudraConfig.setSupplyProgressHudY(py[4]);
        KuudraConfig.setSupplyProgressHudScale(ps[4]);

        KuudraConfig.setBuildProgressHudX(px[5]);
        KuudraConfig.setBuildProgressHudY(py[5]);
        KuudraConfig.setBuildProgressHudScale(ps[5]);

        KuudraConfig.setNotificationHudX(px[6]);
        KuudraConfig.setNotificationHudY(py[6]);
        KuudraConfig.setNotificationHudScale(ps[6]);

        KuudraConfig.setCratePriorityHudX(px[7]);
        KuudraConfig.setCratePriorityHudY(py[7]);
        KuudraConfig.setCratePriorityHudScale(ps[7]);

        KuudraConfig.setKuudraHpHudX(px[8]);
        KuudraConfig.setKuudraHpHudY(py[8]);
        KuudraConfig.setKuudraHpHudScale(ps[8]);

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

        // Draw all boxes + previews
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

            // Scale indicator in bottom-left corner
            String scaleStr = String.format("%.1f×", ps[i]);
            ctx.text(font, Component.literal("§7" + scaleStr), bx + 2, by + bh - font.lineHeight - 1, 0xFFFFFFFF);
        }

        // Tooltip for hovered element
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

        // Coordinate display for hovered or dragged element (quadrant coords, 0,0 = screen centre)
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

        // Reset button
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
            case 0 -> { // Mount Timer: "182" centered
                m.translate(bx + bw / 2f, by + bh / 2f);
                m.scale(s, s);
                String txt = "182";
                ctx.text(font, txt, -font.width(txt) / 2, -font.lineHeight / 2, 0xFFFFFFFF, true);
            }
            case 1 -> { // Kuudra Direction: "RIGHT!" centered
                m.translate(bx + bw / 2f, by + bh / 2f);
                m.scale(s, s);
                String txt = "RIGHT!";
                ctx.text(font, txt, -font.width(txt) / 2, -font.lineHeight / 2, 0xFFFFFFFF, true);
            }
            case 2 -> { // Split Timer: lines from top-left
                m.translate(bx + 2, by + 2);
                m.scale(s, s);
                int y = 0;
                for (String line : SPLIT_LINES) {
                    ctx.text(font, line, 0, y, 0xFFFFFFFF, true);
                    y += 10;
                }
            }
            case 3 -> { // Pearl Title: bar centered
                m.translate(bx + bw / 2f, by + bh / 2f);
                m.scale(s, s);
                String txt = "§6[||||||||||||||||||||] §e75%";
                ctx.text(font, txt, -font.width(txt) / 2, -font.lineHeight / 2, 0xFFFFFFFF, true);
            }
            case 4 -> { // Supply Progress: two lines centered
                m.translate(bx + bw / 2f, by + bh / 2f - font.lineHeight / 2f);
                m.scale(s, s);
                String s4h = "§e§lSupplies Progress:";
                String s4b = "Supplies Gathered: 3/6";
                ctx.text(font, s4h, -font.width(s4h) / 2, 0, 0xFFFFFF, true);
                ctx.text(font, s4b, -font.width(s4b) / 2, font.lineHeight + 2, 0xFFFFFF, true);
            }
            case 5 -> { // Build Progress: two lines centered
                m.translate(bx + bw / 2f, by + bh / 2f - font.lineHeight / 2f);
                m.scale(s, s);
                String s5h = "§e§lBuild Progress:";
                String s5b = "Progress: 67%";
                ctx.text(font, s5h, -font.width(s5h) / 2, 0, 0xFFFFFF, true);
                ctx.text(font, s5b, -font.width(s5b) / 2, font.lineHeight + 2, 0xFFFFFF, true);
            }
            case 6 -> { // Notifications: example notification centered
                m.translate(bx + bw / 2f, by + bh / 2f);
                m.scale(s, s);
                String s6 = "§cFAST DPS!";
                ctx.text(font, s6, -font.width(s6) / 2, -font.lineHeight / 2, 0xFFFFFF, true);
            }
            case 7 -> { // Crate Priority: example destination centered
                m.translate(bx + bw / 2f, by + bh / 2f);
                m.scale(s, s);
                String s7 = "§fNo Square!";
                ctx.text(font, s7, -font.width(s7) / 2, -font.lineHeight / 2, 0xFFFFFF, true);
            }
            case 8 -> { // Kuudra HP: bar + text preview
                m.translate(bx + bw / 2f, by + bh / 2f);
                m.scale(s, s);
                String s8 = "§cKuudra §f75.0%";
                ctx.text(font, s8, -font.width(s8) / 2, -font.lineHeight - 2, 0xFFFFFF, true);
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

        // Reset button
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
        if (event.key() == 256) { // ESC
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

    // Centered elements: anchor point = center of box. Top-left elements: anchor = top-left.
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
        px[4] = 0.5f;   py[4] = 0.35f; ps[4] = 1.0f;
        px[5] = 0.5f;   py[5] = 0.45f; ps[5] = 1.0f;
        px[6] = 0.5f;   py[6] = 0.15f; ps[6] = 1.5f;
        px[7] = 0.5f;   py[7] = 0.6f;  ps[7] = 2.0f;
        px[8] = 0.5f;   py[8] = 0.07f; ps[8] = 1.0f;
    }

    private static float clamp01(float v)    { return Math.max(0f, Math.min(1f, v)); }
    private static float clampScale(float v) { return Math.max(0.2f, Math.min(3f, v)); }
}
