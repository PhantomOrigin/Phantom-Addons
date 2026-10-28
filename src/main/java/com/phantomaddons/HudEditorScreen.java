package com.phantomaddons;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class HudEditorScreen extends Screen {

    private static final int N = 14;

    private static final String[] LABELS = {
        "Mount Timer", "Kuudra Direction", "Split Timer", "Crate Pickup Progress",
        "Build Progress", "Notifications", "Crate Priority", "Kuudra HP",
        "Profit Tracker", "Chest Value", "Backbone Progress Bar", "Chest Tracker",
        "Stun Reminder", "Tentacle Warning"
    };
    private final float[] px = new float[N];
    private final float[] py = new float[N];
    private final float[] ps = new float[N];

    private int    draggingIdx = -1;
    private double dragStartMX, dragStartMY;
    private float  dragStartPx, dragStartPy;

    private final Screen parent;

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

        px[11] = PhantomConfig.getChestTrackerHudX();
        py[11] = PhantomConfig.getChestTrackerHudY();
        ps[11] = PhantomConfig.getChestTrackerHudScale();

        px[12] = PhantomConfig.getStunReminderHudX();
        py[12] = PhantomConfig.getStunReminderHudY();
        ps[12] = PhantomConfig.getStunReminderHudScale();

        px[13] = PhantomConfig.getTentacleWarnHudX();
        py[13] = PhantomConfig.getTentacleWarnHudY();
        ps[13] = PhantomConfig.getTentacleWarnHudScale();
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

        PhantomConfig.setChestTrackerHudX(px[11]);
        PhantomConfig.setChestTrackerHudY(py[11]);
        PhantomConfig.setChestTrackerHudScale(ps[11]);

        PhantomConfig.setStunReminderHudX(px[12]);
        PhantomConfig.setStunReminderHudY(py[12]);
        PhantomConfig.setStunReminderHudScale(ps[12]);

        PhantomConfig.setTentacleWarnHudX(px[13]);
        PhantomConfig.setTentacleWarnHudY(py[13]);
        PhantomConfig.setTentacleWarnHudScale(ps[13]);

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

        int hoveredIdx = draggingIdx >= 0 ? -1 : elementAt(mx, my);
        for (int i = 0; i < N; i++) {
            int[] b = box(i);
            int bx = b[0], by = b[1], bw = b[2], bh = b[3];
            boolean active = i == draggingIdx || i == hoveredIdx;

            int bg     = i == draggingIdx ? 0x900055FF : (i == hoveredIdx ? 0x600055FF : 0x40003388);
            int border = active ? 0xFF00BBFF : 0xFF003E99;

            ctx.fill(bx, by, bx + bw, by + bh, bg);
            ctx.fill(bx - 1,  by - 1,  bx + bw + 1, by,          border);
            ctx.fill(bx - 1,  by + bh, bx + bw + 1, by + bh + 1, border);
            ctx.fill(bx - 1,  by,      bx,          by + bh,     border);
            ctx.fill(bx + bw, by,      bx + bw + 1, by + bh,     border);

            renderPreview(ctx, i);
        }

        int labelIdx = draggingIdx >= 0 ? draggingIdx : hoveredIdx;
        if (labelIdx >= 0) {
            int i  = labelIdx;
            int[] b = box(i);
            String label = LABELS[i] + String.format("  §7%.1f×", ps[i]);
            int tw = font.width(label);
            int tx = b[0] + (b[2] - tw) / 2;
            int ty = b[1] - font.lineHeight - 6;
            if (ty < 4) ty = b[1] + b[3] + 6;

            ctx.fill(tx - 3, ty - 2, tx + tw + 3, ty + font.lineHeight + 2, 0xFF111111);
            ctx.fill(tx - 3, ty + font.lineHeight + 2, tx + tw + 3, ty + font.lineHeight + 3, 0xFF00AAFF);
            ctx.text(font, Component.literal(label), tx, ty, 0xFFFFFFFF);
        }

        if (labelIdx >= 0) {
            float cx = (px[labelIdx] - 0.5f) * width;
            float cy = (py[labelIdx] - 0.5f) * height;
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

    // ── Previews ──────────────────────────────────────────────────────────────

    private record Line(int x, int y, String text) {}

    private record Element(boolean centered, boolean intAnchor, float baseScale, List<Line> lines, boolean hpBar) {}

    private static final int HP_BAR_W = 160, HP_BAR_H = 8;

    private Element element(int i) {
        int lh = font.lineHeight;
        return switch (i) {
            case 0  -> centered(3.0f, new Line(0, 0, "182"));
            case 1  -> centered(3.5f, new Line(0, 0, "RIGHT!"));
            case 2  -> leftAligned(stacked(0,
                    "§b§lKuudra Splits:",
                    "§bSupplies:§r 23.45s   §a[-1.2]§r",
                    "§bBuild:§r 45.67s   §c[+2.1]§r",
                    "§bEaten:§r 12.34s",
                    "§bStun:§r 5.67s",
                    "§bDPS:§r 8.90s",
                    "§bBoss:§r 34.56s",
                    "§bOverall:§r 2:10s",
                    "§bPredicted:§r 2:08s"));
            case 3  -> centered(4.0f, new Line(0, 0, "§6[||||||||||||||||||||] §e75%"));
            case 4  -> centered(1.0f, new Line(0, 0, "§e§lBuild Progress:"), new Line(0, lh + 2, "Progress: 67%"));
            case 5  -> centered(1.0f, new Line(0, -(lh + 2) / 2, "§eNOTIFICATION"));
            case 6  -> centered(1.0f, new Line(0, -lh / 2, "§eGo Square!"));
            case 7  -> kuudraHp();
            case 8  -> profitTracker();
            case 9  -> leftAligned(List.of(
                    new Line(0,          0, "§6§lChest Value §7(T5)"),
                    new Line(0,         10, "§bChest Value:§r §f50.0M§r"),
                    new Line(INDENT,    20, "§7Items: §f35.0M§r"),
                    new Line(INDENT,    30, "§7Essence: §f15.0M§r"),
                    new Line(0,         40, "§bExpenses:§r §c-12.0M§r"),
                    new Line(INDENT,    50, "§7Key: §c-10.0M§r"),
                    new Line(0,         60, "§bChest Profit:§r §a§f38.0M§r")));
            case 10 -> centered(3.0f, new Line(0, 0, "§8[§6||||||||||||§f||||||||§8] §b60%"));
            case 11 -> leftAligned(List.of(new Line(0, 0, "§f12§7 (§a5§7 / §c2§7)")));
            case 12 -> centered(3.0f, new Line(0, 0, "§cLEAVE BUILD!"));
            case 13 -> centered(1.0f, new Line(0, -lh / 2, "§c§l!!!"));
            default -> throw new IllegalArgumentException("No HUD element " + i);
        };
    }

    private static final int INDENT = 8;

    private static Element centered(float baseScale, Line... lines) {
        return new Element(true, false, baseScale, List.of(lines), false);
    }

    private static Element leftAligned(List<Line> lines) {
        return new Element(false, true, 1.0f, lines, false);
    }

    private static List<Line> stacked(int x, String... texts) {
        List<Line> out = new ArrayList<>();
        for (int i = 0; i < texts.length; i++) out.add(new Line(x, i * 10, texts[i]));
        return out;
    }

    private Element kuudraHp() {
        boolean hideBar = PhantomConfig.isKuudraHpHideBar();
        String label = PhantomConfig.isKuudraHpShowRaw() ? "§f75,000 §7/ §f100,000" : "§f75.0%";
        int labelY = hideBar ? -font.lineHeight / 2 : -font.lineHeight - 2;
        return new Element(true, false, 1.0f, List.of(new Line(0, labelY, label)), !hideBar);
    }

    private Element profitTracker() {
        List<Line> lines = new ArrayList<>(List.of(
                new Line(0,       0, "§6§lProfit Tracker §7(Session)"),
                new Line(0,      10, "§bTotal Gains:§r §f332.0M§r"),
                new Line(INDENT, 20, "§7Items: §f125.0M§r"),
                new Line(INDENT, 30, "§7Essence: §f172.0M§r"),
                new Line(0,      40, "§bTotal Expenses:§r §c-225.0M§r"),
                new Line(INDENT, 50, "§7Keys: §c-180.0M§r"),
                new Line(INDENT, 60, "§7Kismets: §c-45.0M§r"),
                new Line(INDENT, 70, "§7Wheel of Fate: §c-20.0M§r"),
                new Line(0,      80, "§bTotal Profit:§r §a§f107.0M§r"),
                new Line(0,      90, "§bTotal Runs:§r 60"),
                new Line(0,     100, "§bProfit/Run:§r §f1.8M§r"),
                new Line(0,     110, "§bRuns/Hour:§r §f6.3§r"),
                new Line(0,     120, "§bProfit/Hour:§r §f11.3M§r")));
        int rowY = 132, gap = 8;
        int sesW = font.width("Session"), atW = font.width("All Time");
        lines.add(new Line(0,                          rowY, "§n§fSession§r"));
        lines.add(new Line(sesW + gap,                 rowY, "§7All Time"));
        lines.add(new Line(sesW + gap + atW + gap,     rowY, "§cReset§r"));
        return new Element(false, true, 1.0f, lines, false);
    }

    private int lineX(Element e, Line l) {
        return e.centered() ? -font.width(l.text()) / 2 : l.x();
    }

    private int[] localBounds(Element e) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Line l : e.lines()) {
            int x = lineX(e, l);
            minX = Math.min(minX, x);
            minY = Math.min(minY, l.y());
            maxX = Math.max(maxX, x + font.width(l.text()));
            maxY = Math.max(maxY, l.y() + font.lineHeight);
        }
        if (e.hpBar()) {
            int half = HP_BAR_W / 2;
            minX = Math.min(minX, -half - 1);
            minY = Math.min(minY, -1);
            maxX = Math.max(maxX, half + 1);
            maxY = Math.max(maxY, HP_BAR_H + 1);
        }
        return new int[]{ minX, minY, maxX, maxY };
    }

    private float anchorX(int i, Element e) { float v = px[i] * width;  return e.intAnchor() ? (int) v : v; }
    private float anchorY(int i, Element e) { float v = py[i] * height; return e.intAnchor() ? (int) v : v; }

    private int[] box(int i) {
        Element e = element(i);
        int[] b = localBounds(e);
        float s = e.baseScale() * ps[i];
        float ax = anchorX(i, e), ay = anchorY(i, e);
        int x0 = (int) Math.floor(ax + b[0] * s), y0 = (int) Math.floor(ay + b[1] * s);
        int x1 = (int) Math.ceil(ax + b[2] * s),  y1 = (int) Math.ceil(ay + b[3] * s);
        return new int[]{ x0, y0, x1 - x0, y1 - y0 };
    }

    private static final int MIN_HIT = 8;

    private boolean hit(int[] b, double mx, double my) {
        int padX = Math.max(0, (MIN_HIT - b[2]) / 2), padY = Math.max(0, (MIN_HIT - b[3]) / 2);
        return mx >= b[0] - padX && mx <= b[0] + b[2] + padX && my >= b[1] - padY && my <= b[1] + b[3] + padY;
    }

    private int elementAt(double mx, double my) {
        for (int i = 0; i < N; i++) if (hit(box(i), mx, my)) return i;
        return -1;
    }

    private void renderPreview(GuiGraphicsExtractor ctx, int i) {
        Element e = element(i);
        float s = e.baseScale() * ps[i];
        var m = ctx.pose();
        m.pushMatrix();
        m.translate(anchorX(i, e), anchorY(i, e));
        m.scale(s, s);
        if (e.hpBar()) {
            int half = HP_BAR_W / 2;
            ctx.fill(-half, 0, half, HP_BAR_H, 0xAA000000);
            ctx.fill(-half, 0, -half + Math.round(HP_BAR_W * 0.75f), HP_BAR_H, 0xFF44AA44);
            ctx.fill(-half - 1, -1,       half + 1, 0,            0xFF000000);
            ctx.fill(-half - 1, HP_BAR_H, half + 1, HP_BAR_H + 1, 0xFF000000);
            ctx.fill(-half - 1, -1,       -half,    HP_BAR_H + 1, 0xFF000000);
            ctx.fill( half,     -1,       half + 1, HP_BAR_H + 1, 0xFF000000);
        }
        for (Line l : e.lines()) ctx.text(font, l.text(), lineX(e, l), l.y(), 0xFFFFFFFF, true);
        m.popMatrix();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isDoubleClick) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(click, isDoubleClick);

        double mx = click.x(), my = click.y();

        int rx = width - 82, ry = height - 22;
        if (mx >= rx && mx <= rx + 72 && my >= ry && my <= ry + 14) {
            resetDefaults();
            return true;
        }

        int i = elementAt(mx, my);
        if (i >= 0) {
            draggingIdx = i;
            dragStartMX = mx;
            dragStartMY = my;
            dragStartPx = px[i];
            dragStartPy = py[i];
            return true;
        }
        draggingIdx = -1;
        return super.mouseClicked(click, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (draggingIdx >= 0 && click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            px[draggingIdx] = clamp01(dragStartPx + (float)(click.x() - dragStartMX) / width);
            py[draggingIdx] = clamp01(dragStartPy + (float)(click.y() - dragStartMY) / height);
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) draggingIdx = -1;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int i = elementAt(mx, my);
        if (i >= 0) {
            ps[i] = clampScale(ps[i] + (float)vScroll * 0.1f);
            return true;
        }
        return super.mouseScrolled(mx, my, hScroll, vScroll);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }

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
        px[11] = 0.556f; py[11] = 0.958f; ps[11] = 1.0f;
        px[12] = 0.5f;  py[12] = 0.25f; ps[12] = 1.0f;
        px[13] = 0.5f;  py[13] = 0.35f; ps[13] = 3.0f;
    }

    private static float clamp01(float v)    { return Math.max(0f, Math.min(1f, v)); }
    private static float clampScale(float v) { return Math.max(0.2f, Math.min(3f, v)); }
}
