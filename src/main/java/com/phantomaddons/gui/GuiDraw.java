package com.phantomaddons.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static com.phantomaddons.gui.Theme.*;

public final class GuiDraw {

    public static final int   ROW_H             = 26;
    public static final int   CONTROL_RADIUS    = 5;
    static final float        TOGGLE_ANIM_SPEED = 5f;

    private GuiDraw() {}

    // ── Text ──────────────────────────────────────────────────────────────────

    public static void drawText(GuiGraphicsExtractor ctx, Font font, Component c, int x, int y, int color) {
        ctx.text(font, c, x, y, color, darkMode);
    }

    public static void drawText(GuiGraphicsExtractor ctx, Font font, Component c, int x, int y, int color, boolean shadow) {
        ctx.text(font, c, x, y, color, shadow);
    }

    public static void drawCenteredText(GuiGraphicsExtractor ctx, Font font, Component c, int cx, int y, int color) {
        if (darkMode) { ctx.centeredText(font, c, cx, y, color); return; }
        drawText(ctx, font, c, cx - font.width(c) / 2, y, color, false);
    }

    // ── On/off switch ─────────────────────────────────────────────────────────

    private static final int SWITCH_W = 34, SWITCH_H = 12;

    static void drawSwitch(GuiGraphicsExtractor ctx, int x, int y, int w, float anim) {
        int px = x + w - SWITCH_W - 8, py = y + (ROW_H - SWITCH_H) / 2;
        roundedFill(ctx, px, py, px + SWITCH_W, py + SWITCH_H, lerpColor(C_OFF, C_ON, anim), SWITCH_H / 2);
        int kxOff = px + 1, kxOn = px + SWITCH_W - 12;
        int kx = Math.round(kxOff + (kxOn - kxOff) * anim);
        roundedFill(ctx, kx, py + 1, kx + 10, py + SWITCH_H - 1, 0xFFFFFFFF, (SWITCH_H - 2) / 2);
    }

    static boolean switchHit(double mx, double my, int x, int y, int w) {
        int px = x + w - SWITCH_W - 8, py = y + (ROW_H - SWITCH_H) / 2;
        return mx >= px && mx <= px + SWITCH_W && my >= py && my <= py + SWITCH_H;
    }

    // ── Rounded rectangles ───────────────────────────────────────────────────────

    private static final Identifier CORNER_TL = Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/corner_tl.png");
    private static final Identifier CORNER_TR = Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/corner_tr.png");
    private static final Identifier CORNER_BL = Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/corner_bl.png");
    private static final Identifier CORNER_BR = Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/corner_br.png");
    private static final int CORNER_TEX_SIZE = 64;

    public static void roundedFill(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color, int radius) {
        roundedFill(ctx, x1, y1, x2, y2, color, radius, true, true, true, true);
    }

    public static void roundedFill(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color, int radius,
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

    public static void roundedFillRadial(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2,
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

    private static final Identifier GRADIENT_TEX =
            Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/radial_gradient.png");
    private static final int GRADIENT_TEX_SIZE = 128;

    private static void radialCellFill(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2,
                                       double centerX, double centerY, double radius, int colorNear, int colorFar) {
        ctx.fill(x1, y1, x2, y2, colorFar);

        double vx0 = centerX - radius, vy0 = centerY - radius;
        double vx1 = centerX + radius, vy1 = centerY + radius;

        double ix0 = Math.max(x1, vx0), iy0 = Math.max(y1, vy0);
        double ix1 = Math.min(x2, vx1), iy1 = Math.min(y2, vy1);
        if (ix1 <= ix0 || iy1 <= iy0) return;

        double span = 2.0 * radius;
        float u0 = (float) ((ix0 - vx0) / span);
        float v0 = (float) ((iy0 - vy0) / span);
        float u1 = (float) ((ix1 - vx0) / span);
        float v1 = (float) ((iy1 - vy0) / span);

        int targetX = (int) Math.round(ix0), targetY = (int) Math.round(iy0);
        int targetW = (int) Math.round(ix1 - ix0), targetH = (int) Math.round(iy1 - iy0);
        int regionW = Math.max(1, Math.round((u1 - u0) * GRADIENT_TEX_SIZE));
        int regionH = Math.max(1, Math.round((v1 - v0) * GRADIENT_TEX_SIZE));
        if (targetW <= 0 || targetH <= 0) return;

        ctx.blit(RenderPipelines.GUI_TEXTURED, GRADIENT_TEX, targetX, targetY,
                u0 * GRADIENT_TEX_SIZE, v0 * GRADIENT_TEX_SIZE, targetW, targetH,
                regionW, regionH, GRADIENT_TEX_SIZE, GRADIENT_TEX_SIZE, colorNear);
    }

    private static int radialSample(double x, double y, double centerX, double centerY, double radius, int colorNear, int colorFar) {
        double dist = Math.hypot(x - centerX, y - centerY);
        float t = (float) Math.max(0.0, Math.min(1.0, dist / radius));
        return lerpColor(colorNear, colorFar, t);
    }
}
