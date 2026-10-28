package com.phantomaddons.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.phantomaddons.PhantomConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

import static com.phantomaddons.gui.GuiDraw.roundedFill;
import static com.phantomaddons.gui.Theme.*;

public final class ChangelogScreen extends Screen {

    private static final int BOX_W = 340, PAD = 12, HEADER_H = 30, FOOT_H = 36, BTN_H = 20, RADIUS = 6;
    private static final long OPEN_MS = 200;

    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/logo.png");
    private static final Identifier LOGO_LIGHT = Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/logo_light.png");

    private record Line(FormattedCharSequence text, String plain, int color, int indent, int height, boolean divider, String bullet) {}

    private final Screen parent;
    private final List<ChangelogData.Entry> entries;
    private final long openStart = System.currentTimeMillis();

    private List<Line> lines = List.of();
    private int linesWidth = -1;
    private int totalHeight;
    private float scroll;
    private int[] closeBtn, actionBtn, skipBtn;
    private final String updateVersion;

    public ChangelogScreen(Screen parent, List<ChangelogData.Entry> entries) {
        this(parent, entries, null);
    }

    public ChangelogScreen(Screen parent, List<ChangelogData.Entry> entries, String updateVersion) {
        super(Component.literal("PhantomAddons Changelog"));
        this.parent = parent;
        this.entries = entries;
        this.updateVersion = updateVersion;
    }

    @Override
    protected void init() {
        Theme.apply(PhantomConfig.getUiTheme());
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private int boxW() { return Math.min(BOX_W, width - 24); }
    private int boxH() { return Math.min(270, height - 24); }
    private int boxX() { return (width - boxW()) / 2; }
    private int boxY() { return (height - boxH()) / 2; }
    private int textW() { return boxW() - 2 * PAD - 6; }
    private int viewTop() { return boxY() + HEADER_H + 8; }
    private int footH() { return updateVersion == null ? FOOT_H : 66; }
    private int viewBottom() { return boxY() + boxH() - footH(); }

    private void layout() {
        int w = textW();
        if (w == linesWidth) return;
        linesWidth = w;
        List<Line> out = new ArrayList<>();
        int lh = font.lineHeight + 2;
        boolean first = true;
        if (entries.isEmpty()) {
            for (FormattedCharSequence l : font.split(Component.literal(
                    "No release notes have been published for this version yet."), w)) {
                out.add(new Line(l, "", C_TEXT_DIM, 0, lh, false, null));
            }
        }
        for (ChangelogData.Entry e : entries) {
            if (!first) {
                out.add(new Line(null, "", C_BORDER, 0, 8, false, null));
                out.add(new Line(null, "", C_BORDER, 0, 1, true, null));
                out.add(new Line(null, "", C_BORDER, 0, 8, false, null));
            }
            first = false;
            String head = "v" + e.version() + (e.date().isBlank() ? "" : "  ·  " + e.date());
            out.add(new Line(null, head, C_ACCENT, 0, lh + 2, false, null));
            if (!e.title().isBlank()) {
                for (FormattedCharSequence l : font.split(Component.literal(e.title()), w)) {
                    out.add(new Line(l, "", C_TEXT, 0, lh, false, null));
                }
            }
            for (ChangelogData.Section s : e.sections()) {
                out.add(new Line(null, "", C_TEXT, 0, 4, false, null));
                out.add(new Line(null, s.name(), sectionColor(s.name()), 0, lh, false, null));
                for (String item : s.items()) {
                    boolean firstLine = true;
                    for (FormattedCharSequence l : font.split(Component.literal(item), w - 10)) {
                        out.add(new Line(l, "", C_TEXT, 10, lh, false, firstLine ? "•" : null));
                        firstLine = false;
                    }
                    out.add(new Line(null, "", C_TEXT, 0, 2, false, null));
                }
            }
        }
        lines = out;
        totalHeight = 0;
        for (Line l : out) totalHeight += l.height();
    }

    private static int sectionColor(String name) {
        return switch (name.toLowerCase()) {
            case "new" -> C_ON;
            case "fixed" -> C_INFO;
            default -> C_ACCENT;
        };
    }

    private float maxScroll() { return Math.max(0, totalHeight - (viewBottom() - viewTop())); }

    // ── Render ────────────────────────────────────────────────────────────────

    private float openProgress() {
        float t = Mth.clamp((System.currentTimeMillis() - openStart) / (float) OPEN_MS, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, ((int) (0xAA * openProgress()) << 24) | 0x06080C);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        extractBackground(ctx, mx, my, delta);
        layout();
        scroll = Mth.clamp(scroll, 0, maxScroll());

        int bw = boxW(), bh = boxH(), bx = boxX(), by = boxY();
        float a = openProgress();

        var pose = ctx.pose();
        pose.pushMatrix();
        float sc = 0.9f + 0.1f * a;
        pose.translate(bx + bw / 2f, by + bh / 2f);
        pose.scale(sc, sc);
        pose.translate(-(bx + bw / 2f), -(by + bh / 2f));

        roundedFill(ctx, bx - 1, by - 1, bx + bw + 1, by + bh + 1, C_BORDER, RADIUS + 1);
        roundedFill(ctx, bx, by, bx + bw, by + bh, withAlpha(C_BG, 250), RADIUS);
        roundedFill(ctx, bx, by, bx + bw, by + HEADER_H, C_HEADER, RADIUS, true, true, false, false);
        ctx.fill(bx, by + HEADER_H - 1, bx + bw, by + HEADER_H, C_ACCENT);
        int logo = HEADER_H - 8, lx = bx + 8, ly = by + 4;
        ctx.blit(darkMode ? LOGO : LOGO_LIGHT, lx, ly, lx + logo, ly + logo, 0f, 1f, 0f, 1f);
        ctx.text(font, updateVersion == null ? "What's new" : "Update available", lx + logo + 6,
                by + (HEADER_H - font.lineHeight) / 2, C_ACCENT, darkMode);
        if (updateVersion != null) {
            String change = "v" + com.phantomaddons.UpdateChecker.currentVersion() + "  →  v" + updateVersion;
            ctx.text(font, change, bx + bw - PAD - font.width(change), by + (HEADER_H - font.lineHeight) / 2, C_TEXT_DIM, false);
        }

        int top = viewTop(), bottom = viewBottom(), x = bx + PAD;
        ctx.enableScissor(bx, top, bx + bw, bottom);
        float y = top - scroll;
        for (Line l : lines) {
            int iy = Math.round(y);
            if (iy + l.height() >= top && iy <= bottom) drawLine(ctx, l, x, iy, bx + bw - PAD - 6);
            y += l.height();
        }
        ctx.disableScissor();

        if (maxScroll() > 0) {
            int trackH = bottom - top;
            int thumbH = Math.max(18, Math.round(trackH * trackH / (float) totalHeight));
            int thumbY = top + Math.round((trackH - thumbH) * (scroll / maxScroll()));
            int sx = bx + bw - PAD / 2 - 3;
            roundedFill(ctx, sx, thumbY, sx + 3, thumbY + thumbH, withAlpha(C_TEXT_DIM, 180), 1);
        }

        int btnY = by + bh - FOOT_H + (FOOT_H - BTN_H) / 2;
        if (updateVersion == null) {
            skipBtn = null;
            int btnW = 90;
            closeBtn = new int[] { bx + (bw - btnW) / 2, btnY, bx + (bw + btnW) / 2, btnY + BTN_H };
            actionBtn = null;
            button(ctx, closeBtn, "Got it", true, true, mx, my);
            ctx.text(font, "/phantom changelog", bx + PAD, btnY + (BTN_H - font.lineHeight) / 2, C_TEXT_DIM, false);
        } else {
            // Two rows: the download (or link) button, then Later and Skip this version side by side.
            int row1 = by + bh - footH() + 8, row2 = row1 + BTN_H + 6;
            int actionW = Math.min(220, bw - 2 * PAD);
            actionBtn = new int[] { bx + (bw - actionW) / 2, row1, bx + (bw + actionW) / 2, row1 + BTN_H };
            int gap = 8, w2 = Math.min(110, (bw - 2 * PAD - gap) / 2);
            int left2 = bx + (bw - (2 * w2 + gap)) / 2;
            closeBtn = new int[] { left2, row2, left2 + w2, row2 + BTN_H };
            skipBtn = new int[] { left2 + w2 + gap, row2, left2 + 2 * w2 + gap, row2 + BTN_H };
            button(ctx, actionBtn, actionLabel(), true, actionEnabled(), mx, my);
            button(ctx, closeBtn, "Later", false, true, mx, my);
            button(ctx, skipBtn, "Skip this version", false, true, mx, my);
        }

        pose.popMatrix();
    }

    private String actionLabel() {
        var edition = com.phantomaddons.Edition.CURRENT;
        if (!edition.autoDownloadCapable) {
            return edition == com.phantomaddons.Edition.STANDARD ? "View on Modrinth" : "View on GitHub";
        }
        return switch (com.phantomaddons.UpdateChecker.getState()) {
            case DOWNLOADED -> "Downloaded - restart to install";
            case CHECKING, DOWNLOADING -> "Downloading...";
            default -> "Download v" + updateVersion;
        };
    }

    private boolean actionEnabled() {
        if (!com.phantomaddons.Edition.CURRENT.autoDownloadCapable) return true;
        return switch (com.phantomaddons.UpdateChecker.getState()) {
            case DOWNLOADED, CHECKING, DOWNLOADING -> false;
            default -> true;
        };
    }

    /** Stops this version being offered again; the window and the chat notice stay quiet until a newer one is out. */
    private void skipVersion() {
        PhantomConfig.setSkippedUpdateVersion(updateVersion);
        onClose();
    }

    private void runAction() {
        if (!actionEnabled()) return;
        if (com.phantomaddons.Edition.CURRENT.autoDownloadCapable) com.phantomaddons.UpdateChecker.downloadFromChangelog();
        else com.phantomaddons.UpdateChecker.openEditionPage();
    }

    private void button(GuiGraphicsExtractor ctx, int[] r, String label, boolean primary, boolean enabled, int mx, int my) {
        boolean hov = enabled && inside(r, mx, my);
        int bg = primary ? (enabled ? (hov ? withAlpha(C_ACCENT, 0xFF) : withAlpha(C_ACCENT, 0xD0)) : withAlpha(C_ON, 0xB0))
                : (hov ? C_TAB_HOVER : C_HEADER);
        if (!primary) roundedFill(ctx, r[0] - 1, r[1] - 1, r[2] + 1, r[3] + 1, C_BORDER, 5);
        roundedFill(ctx, r[0], r[1], r[2], r[3], bg, 4);
        int color = primary ? 0xFF111111 : C_TEXT;
        ctx.text(font, label, (r[0] + r[2] - font.width(label)) / 2, (r[1] + r[3] - font.lineHeight) / 2 + 1, color, !primary && darkMode);
    }

    private void drawLine(GuiGraphicsExtractor ctx, Line l, int x, int y, int right) {
        if (l.divider()) {
            ctx.fill(x, y, right, y + 1, C_BORDER);
        } else if (l.text() != null) {
            if (l.bullet() != null) ctx.text(font, l.bullet(), x + 2, y, C_TEXT_DIM, false);
            ctx.text(font, l.text(), x + l.indent(), y, l.color(), darkMode);
        } else if (!l.plain().isEmpty()) {
            ctx.text(font, l.plain(), x, y, l.color(), darkMode);
        }
    }

    private static boolean inside(int[] r, double x, double y) {
        return r != null && x >= r[0] && x < r[2] && y >= r[1] && y < r[3];
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isDoubleClick) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
        if (inside(actionBtn, click.x(), click.y())) runAction();
        else if (inside(skipBtn, click.x(), click.y())) skipVersion();
        else if (inside(closeBtn, click.x(), click.y())) onClose();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        scroll = Mth.clamp(scroll - (float) (vScroll * 14), 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER || key == InputConstants.KEY_SPACE) { onClose(); return true; }   // Enter, keypad Enter, Space
        if (key == InputConstants.KEY_UP) { scroll = Mth.clamp(scroll - 14, 0, maxScroll()); return true; }   // Up
        if (key == InputConstants.KEY_DOWN) { scroll = Mth.clamp(scroll + 14, 0, maxScroll()); return true; }   // Down
        return super.keyPressed(event);                                                      // Escape closes
    }
}
