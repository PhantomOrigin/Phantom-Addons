package com.phantomaddons.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import static com.phantomaddons.gui.Theme.*;
import static com.phantomaddons.gui.GuiDraw.*;

public class CustomCategoryRow extends Feature {

    private static final String PREFIX = "match: ";

    private final String   matchString;
    private final Runnable onRemove;

    public CustomCategoryRow(String matchString, Runnable onRemove) {
        super("Remove");
        this.matchString = matchString;
        this.onRemove    = onRemove;
    }

    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        drawText(ctx, s.font(), Component.literal(PREFIX),
                x + 8, y + (ROW_H - s.font().lineHeight) / 2, C_TEXT_DIM);
        drawText(ctx, s.font(), Component.literal("\"" + matchString + "\""),
                x + 8 + s.font().width(PREFIX), y + (ROW_H - s.font().lineHeight) / 2, C_TEXT);
        int bw = 58, bh = ROW_H - 6, bx = x + w - bw - 8, by = y + 3;
        boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        roundedFill(ctx, bx, by, bx + bw, by + bh, dangerBg(hov), CONTROL_RADIUS);
        drawCenteredText(ctx, s.font(), Component.literal("Remove"),
                bx + bw / 2, by + (bh - s.font().lineHeight) / 2, C_ERROR);
    }

    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        int bw = 58, bh = ROW_H - 6, bx = x + w - bw - 8, by = y + 3;
        if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
            onRemove.run();
            return true;
        }
        return false;
    }
}
