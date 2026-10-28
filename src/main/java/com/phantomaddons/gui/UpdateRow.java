package com.phantomaddons.gui;

import com.phantomaddons.UpdateChecker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import static com.phantomaddons.gui.GuiDraw.*;
import static com.phantomaddons.gui.Theme.*;

public class UpdateRow extends Feature {

    private final Runnable onClick;

    public UpdateRow(String n, Runnable onClick) {
        super(n);
        this.onClick = onClick;
    }

    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        drawLabel(ctx, s, x, y, C_TEXT);
        String current = UpdateChecker.currentVersion();
        String latest = UpdateChecker.getLatestVersion();
        String text;
        int colour;
        switch (UpdateChecker.getState()) {
            case CHECKING -> { text = current + " · Checking..."; colour = C_TEXT_DIM; }
            case UP_TO_DATE -> { text = current + " · Up to date"; colour = C_ON; }
            case UPDATE_AVAILABLE -> { text = current + " · " + latest + " available"; colour = C_ACCENT; }
            case DOWNLOADING -> { text = current + " · Downloading..."; colour = C_TEXT_DIM; }
            case DOWNLOADED -> { text = latest + " ready to install"; colour = C_INFO; }
            case ERROR -> { text = current + " · Check failed"; colour = C_ERROR; }
            default -> { text = current; colour = C_TEXT_DIM; }
        }
        drawText(ctx, s.font(), Component.literal(text),
                x + w - s.font().width(text) - 8,
                y + (ROW_H - s.font().lineHeight) / 2 + 1, colour);
    }

    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        onClick.run();
        return true;
    }
}
