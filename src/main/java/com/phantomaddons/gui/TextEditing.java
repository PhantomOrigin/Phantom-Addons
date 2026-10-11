package com.phantomaddons.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

final class TextEditing {

    private TextEditing() {}

    static boolean fieldHovered(int fx, int fy, int fw, int fh, double mx, double my) {
        return mx >= fx && mx <= fx + fw && my >= fy && my <= fy + fh;
    }

    static int clickToCursor(Font font, String text, int textStartX, double clickX) {
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

    static void drawCaret(GuiGraphicsExtractor ctx, int x, int y, int h, int color) {
        if (caretVisible()) ctx.fill(x, y, x + 1, y + h, color);
    }

    static String insertAt(String s, int pos, char c) {
        pos = Math.max(0, Math.min(s.length(), pos));
        return s.substring(0, pos) + c + s.substring(pos);
    }

    static String deleteBefore(String s, int pos) {
        if (pos <= 0) return s;
        pos = Math.min(s.length(), pos);
        return s.substring(0, pos - 1) + s.substring(pos);
    }

    static String deleteAfter(String s, int pos) {
        if (pos >= s.length()) return s;
        pos = Math.max(0, pos);
        return s.substring(0, pos) + s.substring(pos + 1);
    }

    static String deleteRange(String s, int a, int b) {
        int lo = Math.max(0, Math.min(a, b)), hi = Math.min(s.length(), Math.max(a, b));
        if (lo >= hi) return s;
        return s.substring(0, lo) + s.substring(hi);
    }

    static String insertRange(String s, int pos, String insert) {
        pos = Math.max(0, Math.min(s.length(), pos));
        return s.substring(0, pos) + insert + s.substring(pos);
    }

    static boolean isCtrlDown() {
        Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    static boolean isShiftDown() {
        Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    static void copyToClipboard(String s) {
        if (s == null || s.isEmpty()) return;
        Minecraft.getInstance().keyboardHandler.setClipboard(s);
    }

    static String pasteFromClipboard() {
        String s = Minecraft.getInstance().keyboardHandler.getClipboard();
        return s == null ? "" : s.replace("\n", "").replace("\r", "");
    }

    static void drawSelection(GuiGraphicsExtractor ctx, Font font,
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
}
