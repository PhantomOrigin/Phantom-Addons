package com.phantomaddons.features.pv;

import com.mojang.blaze3d.platform.InputConstants;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

import static com.phantomaddons.gui.GuiDraw.roundedFill;
import static com.phantomaddons.gui.Theme.*;

final class PvPromptScreen extends Screen {

    private static final int BOX_W = 260, PAD = 12, BTN_H = 20, RADIUS = 6;
    private static final long OPEN_MS = 200;

    private final String command;
    private final long openStart = System.currentTimeMillis();
    private int[] enableBtn, keepBtn;

    PvPromptScreen(String command) {
        super(Component.literal("PhantomAddons Profile Viewer"));
        this.command = command;
    }

    @Override
    protected void init() {
        Theme.apply(PhantomConfig.getEffectivePvTheme());
    }

    @Override
    public void removed() {
        Theme.apply(PhantomConfig.getUiTheme());
    }

    @Override public boolean isPauseScreen() { return false; }

    private List<String> body(int width) {
        List<String> out = new ArrayList<>();
        String[] paragraphs = {
                "PhantomAddons now has its own SkyBlock profile viewer, opened with §e/pv§r.",
                "Enabling it replaces the /pv profile viewers from Skyblocker and SkyBlock PV.",
                "§7You can change this later in the Profile Viewer tab of /pa."
        };
        for (int p = 0; p < paragraphs.length; p++) {
            if (p > 0) out.add("");
            StringBuilder line = new StringBuilder();
            String carry = "";
            for (String word : paragraphs[p].split(" ")) {
                String next = line.isEmpty() ? carry + word : line + " " + word;
                if (!line.isEmpty() && font.width(next) > width) {
                    out.add(line.toString());
                    carry = lastFormat(line.toString());
                    line = new StringBuilder(carry + word);
                } else {
                    line = new StringBuilder(next);
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    private static String lastFormat(String s) {
        int i = s.lastIndexOf('§');
        return i >= 0 && i + 1 < s.length() && s.charAt(i + 1) != 'r' ? s.substring(i, i + 2) : "";
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        float a = PvScreen.ease((System.currentTimeMillis() - openStart) / (float) OPEN_MS);
        ctx.fill(0, 0, width, height, ((int) (0xAA * a) << 24) | 0x06080C);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        float a = PvScreen.ease((System.currentTimeMillis() - openStart) / (float) OPEN_MS);
        int bw = Math.min(BOX_W, width - 24);
        List<String> lines = body(bw - 2 * PAD);
        int headH = 22, lh = font.lineHeight + 2;
        int bh = headH + PAD + lines.size() * lh + PAD + BTN_H + PAD;
        int bx = (width - bw) / 2, by = (height - bh) / 2;

        var pose = ctx.pose();
        pose.pushMatrix();
        float sc = 0.9f + 0.1f * a;
        pose.translate(bx + bw / 2f, by + bh / 2f);
        pose.scale(sc, sc);
        pose.translate(-(bx + bw / 2f), -(by + bh / 2f));

        roundedFill(ctx, bx - 1, by - 1, bx + bw + 1, by + bh + 1, C_BORDER, RADIUS + 1);
        roundedFill(ctx, bx, by, bx + bw, by + bh, withAlpha(C_BG, 250), RADIUS);
        roundedFill(ctx, bx, by, bx + bw, by + headH, C_HEADER, RADIUS, true, true, false, false);
        ctx.fill(bx, by + headH - 1, bx + bw, by + headH, C_ACCENT);
        ctx.text(font, "New Profile Viewer", bx + PAD, by + (headH - font.lineHeight) / 2, C_ACCENT, darkMode);

        int yy = by + headH + PAD;
        for (String line : lines) {
            ctx.text(font, line, bx + PAD, yy, C_TEXT, darkMode);
            yy += lh;
        }

        int gap = 8;
        int btnW = (bw - 2 * PAD - gap) / 2;
        int btnY = by + bh - PAD - BTN_H;
        enableBtn = new int[] { bx + PAD, btnY, bx + PAD + btnW, btnY + BTN_H };
        keepBtn = new int[] { bx + PAD + btnW + gap, btnY, bx + bw - PAD, btnY + BTN_H };
        button(ctx, enableBtn, "Enable", true, mx, my);
        button(ctx, keepBtn, "Keep Disabled", false, mx, my);
        pose.popMatrix();
    }

    private void button(GuiGraphicsExtractor ctx, int[] r, String label, boolean primary, int mx, int my) {
        boolean hov = inside(r, mx, my);
        int bg = primary ? (hov ? withAlpha(C_ACCENT, 0xFF) : withAlpha(C_ACCENT, 0xD0))
                : (hov ? C_TAB_HOVER : C_HEADER);
        if (!primary) roundedFill(ctx, r[0] - 1, r[1] - 1, r[2] + 1, r[3] + 1, C_BORDER, 5);
        roundedFill(ctx, r[0], r[1], r[2], r[3], bg, 4);
        int color = primary ? 0xFF111111 : C_TEXT;
        ctx.text(font, label, (r[0] + r[2] - font.width(label)) / 2, (r[1] + r[3] - font.lineHeight) / 2 + 1, color, !primary && darkMode);
    }

    private static boolean inside(int[] r, double x, double y) {
        return r != null && x >= r[0] && x < r[2] && y >= r[1] && y < r[3];
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isDoubleClick) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
        if (inside(enableBtn, click.x(), click.y())) choose(true);
        else if (inside(keepBtn, click.x(), click.y())) choose(false);
        return true;
    }

    private void choose(boolean enable) {
        PhantomConfig.setPvEnabled(enable);
        PhantomConfig.setPvPromptAnswered(true);
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(null);
        if (mc.getConnection() != null) mc.getConnection().sendCommand(command);
    }
}
