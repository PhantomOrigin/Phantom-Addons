package com.phantomaddons.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.customisation.items.ItemCustomization;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import static com.phantomaddons.gui.Theme.*;
import static com.phantomaddons.gui.GuiDraw.*;

public class AddCategoryFeature extends Feature {
    boolean focused = false;
    String  draft   = "";
    private final Runnable onAdded;
    public AddCategoryFeature(Runnable onAdded) { super("Add category"); this.onAdded = onAdded; }

    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        drawText(ctx, s.font(), Component.literal("Match:"),
                x + 8, y + (ROW_H - s.font().lineHeight) / 2 + 1, C_TEXT_DIM);
        int bw = 46, bx = x + w - bw - 12;
        int fy = y + 3, fh = ROW_H - 6;
        int fx = x + 60, fw = bx - fx - 4;
        insetFill(ctx, fx, fy, fx + fw, fy + fh, focused ? C_FIELD_FOCUS : C_FIELD_BG, CONTROL_RADIUS);
        boolean placeholder = draft.isEmpty() && !focused;
        String disp = placeholder ? "type to filter..." : draft;
        drawText(ctx, s.font(), Component.literal(disp),
                fx + 4, fy + (fh - s.font().lineHeight) / 2 + 1, placeholder ? C_TEXT_DIM : C_TEXT);
        boolean hov = mx >= bx && mx <= bx + bw && my >= fy && my <= fy + fh;
        raisedFill(ctx, bx, fy, bx + bw, fy + fh, hov ? C_CYCLE_HOVER : C_CYCLE_BG, CONTROL_RADIUS);
        drawCenteredText(ctx, s.font(), Component.literal("+ Add"),
                bx + bw / 2, fy + (fh - s.font().lineHeight) / 2 + 1, 0xFF44BB77);
    }

    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        int bw = 46, bx = x + w - bw - 12;
        int fy = y + 3, fh = ROW_H - 6;
        int fx = x + 60, fw = bx - fx - 4;
        if (mx >= fx && mx <= fx + fw && my >= fy && my <= fy + fh) {
            focused = true; return true;
        }
        if (mx >= bx && mx <= bx + bw && my >= fy && my <= fy + fh) {
            addCategory(); return true;
        }
        if (focused) focused = false;
        return false;
    }

    @Override void onKey(int key) {
        if (!focused) return;
        if (key == InputConstants.KEY_ESCAPE) { focused = false; }   // Esc keeps the typed value, just deselects
        else if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) { addCategory(); focused = false; }
        else if (key == InputConstants.KEY_BACKSPACE && !draft.isEmpty())
            draft = draft.substring(0, draft.length() - 1);
    }

    @Override void onChar(char c) { if (focused && c >= 32) draft += c; }
    @Override boolean isCapturing() { return focused; }
    @Override void cancel()         { focused = false; }

    private void addCategory() {
        if (!draft.isBlank()) {
            ItemCustomization.addCustomCategory(draft.trim());
            PhantomConfig.save();
            draft = "";
            onAdded.run();
        }
    }
}
