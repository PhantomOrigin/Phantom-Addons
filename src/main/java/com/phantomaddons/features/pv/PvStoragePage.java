package com.phantomaddons.features.pv;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.IntFunction;

import static com.phantomaddons.gui.GuiDraw.roundedFill;
import static com.phantomaddons.gui.Theme.*;

final class PvStoragePage {

    enum Kind { BACKPACKS, ENDER, RIFT_ENDER }

    private PvStoragePage() {}

    private static final int ENDER_PAGE = 45;

    static int render(PvScreen s, GuiGraphicsExtractor ctx, PvProfileView v, int key, Kind kind,
                      int x, int y, int w, int h, int mx, int my, int scroll) {
        int pad = PvPages.PAD;
        int y0 = y + pad - scroll;
        boolean backpacks = kind == Kind.BACKPACKS;
        List<ItemStack> chest = kind == Kind.RIFT_ENDER ? v.riftEnderChest : v.enderChest;
        boolean apiOn = kind == Kind.RIFT_ENDER ? v.riftInventoryApi : v.inventoryApi;
        int count = backpacks ? v.backpacks.size() : (chest.size() + ENDER_PAGE - 1) / ENDER_PAGE;
        if (count == 0) {
            s.text(ctx, backpacks ? "No backpacks." : (apiOn ? "Ender chest is empty." : "Inventory API disabled."),
                    x + pad, y0, PvTheme.dim(), 0.85f * s.es);
            return s.lineH(s.es);
        }
        String chestName = kind == Kind.RIFT_ENDER ? "Rift Ender Chest Page " : "Ender Chest Page ";
        int page = Math.min(s.innerTab(key), count - 1);

        List<ItemStack> items;
        String label;
        IntFunction<ItemStack> icon;
        IntFunction<String> number;
        IntFunction<String> name;
        if (backpacks) {
            PvProfileView.Backpack b = v.backpacks.get(page);
            items = b.items();
            label = "Backpack " + (b.index() + 1);
            icon = i -> v.backpacks.get(i).icon();
            number = i -> String.valueOf(v.backpacks.get(i).index() + 1);
            name = i -> "Backpack " + (v.backpacks.get(i).index() + 1);
        } else {
            int from = page * ENDER_PAGE;
            items = chest.subList(from, Math.min(chest.size(), from + ENDER_PAGE));
            label = chestName + (page + 1);
            ItemStack chestIcon = new ItemStack(Items.ENDER_CHEST);
            icon = i -> chestIcon;
            number = i -> String.valueOf(i + 1);
            name = i -> chestName + (i + 1);
        }

        int navH = pageButtons(s, ctx, key, count, page, icon, number, name, x, y0, w, mx, my);
        float ls = Math.min(s.es, 1.2f);
        int ly = y0 + navH + 5;
        s.text(ctx, label, x + (w - s.textW(label, ls)) / 2, ly, C_TEXT, ls);

        int rows = Math.max(1, (items.size() + 8) / 9);
        int gy = ly + s.lineH(ls) + 5;
        boolean showValue = kind != Kind.RIFT_ENDER;
        int valueH = showValue ? s.lineH(ls) + 6 : 0;
        // Arrows under the grid step through the pages, like the buttons above it.
        int arrowsH = count > 1 ? s.lineH(Math.min(s.es, 1.2f)) + 10 : 0;
        int sz = s.fitSlot((w - 2 * pad) / 9.12, (h - (gy - y0) - 2 * pad - valueH - arrowsH) / (rows * 1.12));
        int g = PvScreen.gapFor(sz);
        int gridW = 9 * (sz + g) - g;
        int gx = x + (w - gridW) / 2;
        for (int i = 0; i < rows * 9; i++) {
            ItemStack st = i < items.size() ? items.get(i) : ItemStack.EMPTY;
            s.slot(ctx, gx + (i % 9) * (sz + g), gy + (i / 9) * (sz + g), sz, st, mx, my);
        }
        if (showValue) {
            PvNetworth.Result worth = s.networth(v);
            double[] values = worth == null ? new double[0] : backpacks ? worth.backpacks() : worth.enderPages();
            String text = "§7Value: " + (worth != null ? "§6" + PvScreen.fmt(page < values.length ? values[page] : 0)
                    : PvNetworth.status(v));
            s.text(ctx, text, x + (w - s.textW(text, ls)) / 2, gy + rows * (sz + g) + 4, C_TEXT, ls);
        }
        if (count > 1) s.pager(ctx, key, count, "Page " + (page + 1) + " / " + count, x, gy + rows * (sz + g) + valueH + 4, w, mx, my);
        return gy - y0 + rows * (sz + g) + valueH + arrowsH;
    }

    private static int pageButtons(PvScreen s, GuiGraphicsExtractor ctx, int key, int count, int page,
                                   IntFunction<ItemStack> icon, IntFunction<String> number, IntFunction<String> name,
                                   int x, int y, int w, int mx, int my) {
        int avail = w - 2 * PvPages.PAD;
        int bs = Math.max(14, Math.round(20 * s.es)), g = 3;
        int perRow = Math.max(1, (avail + g) / (bs + g));
        int rows = (count + perRow - 1) / perRow;
        float ns = 0.55f * s.es;
        for (int i = 0; i < count; i++) {
            int row = i / perRow, col = i % perRow;
            int inRow = Math.min(perRow, count - row * perRow);
            int bx = x + (w - (inRow * (bs + g) - g)) / 2 + col * (bs + g), by = y + row * (bs + g);
            boolean active = i == page, hov = !active && s.hovered(mx, my, bx, by, bs, bs);
            roundedFill(ctx, bx, by, bx + bs, by + bs, active ? (C_ACCENT & 0xFFFFFF) | 0x99000000 : hov ? C_TAB_HOVER : PvScreen.slotBg(), 3);
            ItemStack st = icon.apply(i);
            if (!st.isEmpty()) s.item(ctx, st, bx + 2, by + 2, bs - 4);
            ctx.nextStratum();
            String n = number.apply(i);
            s.text(ctx, n, bx + bs - s.textW(n, ns) - 1, by + bs - s.lineH(ns), active ? 0xFFFFFFFF : C_TEXT, ns);
            if (hov) s.tooltip(name.apply(i));
            final int idx = i;
            s.addHit(bx, by, bx + bs, by + bs, () -> s.setInnerTab(key, idx));
        }
        return rows * (bs + g) - g;
    }
}
