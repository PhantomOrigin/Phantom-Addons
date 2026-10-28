package com.phantomaddons.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import static com.phantomaddons.gui.GuiDraw.*;

public class LavaPreview extends Feature {
    public LavaPreview() { super("Colour Preview"); }
    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        int previewArgb = com.phantomaddons.features.customisation.lava.ColorPreviewHelper.computePreviewColor();
        int sw = w - 16, swX = x + 8, swY = y + 4, sh = ROW_H - 8;
        ctx.fill(swX, swY, swX + sw, swY + sh, previewArgb);
        ctx.fill(swX, swY, swX + sw, swY + 1, 0x22FFFFFF);
        drawCenteredText(ctx, s.font(), Component.literal("Colour Preview"),
                swX + sw / 2, swY + (sh - s.font().lineHeight) / 2 + 1, 0xCCFFFFFF);
    }
}
