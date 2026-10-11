package com.phantomaddons.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import static com.phantomaddons.gui.Theme.*;
import static com.phantomaddons.gui.GuiDraw.*;

public class DisableAllButton extends Feature {
    private static final long COUNTDOWN_MS = 3000, ARMED_MS = 10000, DONE_MS = 2500;
    private static final int  BTN_W = 96;
    private long pressedAt = 0, armedAt = 0, doneAt = 0;

    private final Runnable onConfirm;

    public DisableAllButton(Runnable onConfirm) { super("Disable All Features"); this.onConfirm = onConfirm; }

    private String label() {
        long now = System.currentTimeMillis();
        if (doneAt > 0 && now - doneAt < DONE_MS) return "Disabled!";
        if (armedAt > 0) {
            if (now - armedAt < ARMED_MS) return "Confirm";
            armedAt = 0;
        }
        if (pressedAt > 0) {
            long left = COUNTDOWN_MS - (now - pressedAt);
            if (left > 0) return "Wait " + ((left + 999) / 1000) + "...";
            pressedAt = 0;
            armedAt = now;
            return "Confirm";
        }
        return "Disable All";
    }

    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        drawLabel(ctx, s, x, y, C_TEXT);
        String label = label();
        int bh = ROW_H - 6, bx = x + w - BTN_W - 8, by = y + 3;
        boolean hov = mx >= bx && mx <= bx + BTN_W && my >= by && my <= by + bh;
        roundedFill(ctx, bx, by, bx + BTN_W, by + bh, hov ? C_CYCLE_HOVER : C_CYCLE_BG, CONTROL_RADIUS);
        int colour = label.equals("Confirm") ? C_ERROR : label.equals("Disabled!") ? C_ON : C_ACCENT;
        drawCenteredText(ctx, s.font(), Component.literal(label),
                bx + BTN_W / 2, by + (bh - s.font().lineHeight) / 2, colour);
    }

    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        int bh = ROW_H - 6, bx = x + w - BTN_W - 8, by = y + 3;
        if (mx < bx || mx > bx + BTN_W || my < by || my > by + bh) return false;
        String label = label();
        if (label.equals("Disable All")) {
            pressedAt = System.currentTimeMillis();
        } else if (label.equals("Confirm")) {
            onConfirm.run();
            armedAt = 0;
            doneAt = System.currentTimeMillis();
        }
        return true;
    }
}
