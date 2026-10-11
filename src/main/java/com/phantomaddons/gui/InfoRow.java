package com.phantomaddons.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

import static com.phantomaddons.gui.Theme.*;
import static com.phantomaddons.gui.GuiDraw.*;

public class InfoRow extends Feature {

    public record Value(String text, int colour) {}

    private final Supplier<Value> value;

    public InfoRow(String n, Supplier<Value> value) {
        super(n); this.value = value;
    }

    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        drawLabel(ctx, s, x, y, C_TEXT_DIM);
        Value v = value.get();
        drawText(ctx, s.font(), Component.literal(v.text()),
                x + w - s.font().width(v.text()) - 8,
                y + (ROW_H - s.font().lineHeight) / 2, v.colour());
    }
}
