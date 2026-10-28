package com.phantomaddons.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static com.phantomaddons.gui.GuiDraw.*;

public class RefreshToggle extends Toggle {

    private static final Identifier ICON =
            Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/icon_refresh.png");
    private static final Identifier ICON_DIM =
            Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/icon_refresh_dim.png");
    private static final int ICON_SIZE = 14;

    private final Runnable onRefresh;
    private final LongSupplier cooldownLeftMs;
    private final String baseTooltip;

    public RefreshToggle(String name, Supplier<Boolean> get, Consumer<Boolean> set,
                         Runnable onRefresh, LongSupplier cooldownLeftMs, String baseTooltip) {
        super(name, get, set);
        this.onRefresh = onRefresh;
        this.cooldownLeftMs = cooldownLeftMs;
        this.baseTooltip = baseTooltip;
        this.tooltip = baseTooltip;
    }

    private int iconX(int x, int w) { return x + w - SWITCH_W - 8 - 8 - ICON_SIZE; }
    private int iconY(int y) { return y + (ROW_H - ICON_SIZE) / 2; }

    private boolean iconHit(double mx, double my, int x, int y, int w) {
        int ix = iconX(x, w), iy = iconY(y);
        return mx >= ix - 2 && mx <= ix + ICON_SIZE + 2 && my >= iy - 2 && my <= iy + ICON_SIZE + 2;
    }

    @Override void render(GuiGraphicsExtractor ctx, PhantomScreen s, int x, int y, int w, int mx, int my) {
        super.render(ctx, s, x, y, w, mx, my);
        long left = cooldownLeftMs.getAsLong();
        int ix = iconX(x, w), iy = iconY(y);
        ctx.blit(left > 0 ? ICON_DIM : ICON, ix, iy, ix + ICON_SIZE, iy + ICON_SIZE, 0f, 1f, 0f, 1f);
        if (left > 0) {
            long minutes = (left + 59_999) / 60_000;
            tooltip = (baseTooltip + " You can refresh again in " + minutes + (minutes == 1 ? " minute." : " minutes.")).trim();
        } else {
            tooltip = (baseTooltip + " Click the arrow to refresh the list.").trim();
        }
    }

    @Override boolean onDown(double mx, double my, int x, int y, int w) {
        if (iconHit(mx, my, x, y, w)) {
            onRefresh.run();
            return true;
        }
        return super.onDown(mx, my, x, y, w);
    }
}
