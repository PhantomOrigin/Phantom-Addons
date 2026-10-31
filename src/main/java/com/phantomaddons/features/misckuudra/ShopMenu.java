package com.phantomaddons.features.misckuudra;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.GuiDraw;
import com.phantomaddons.gui.KeyCapture;
import com.phantomaddons.utils.TextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShopMenu {

    public enum Kind { LARGE, SMALL, HEAD, ROUTE, BALLISTA }

    public record Entry(int slotIndex, ItemStack stack, int x, int y, int w, int h, Kind kind, int keyCode, String label) {
        public boolean clickable() { return kind == Kind.LARGE || kind == Kind.SMALL || kind == Kind.HEAD; }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    public record Panel(int x1, int y1, int x2, int y2) {
        boolean exists() { return x2 > x1 && y2 > y1; }
    }

    public record Layout(List<Entry> entries, Panel strip, Panel info) {
        public Entry at(double mx, double my) {
            for (Entry e : entries) if (e.contains(mx, my)) return e;
            return null;
        }
    }

    private record Perks(String route, String ballista) {}

    private static final int PLAYER_INVENTORY_SLOTS = 36;
    private static final int HUD_CLEARANCE = 68;
    private static final int MAX_HEADS = 4;

    private static final int DIM         = 0xA0000000;
    private static final int PANEL       = 0xE01E2228;
    private static final int PANEL_HOVER = 0xF0364252;
    private static final int STRIP       = 0xB00E1115;
    private static final int BADGE       = 0xFF2B323C;
    private static final int KEY_HINT    = 0xFFAAAAAA;
    private static final int LEVEL_TEXT  = 0xFFFFD34D;

    private static final long CONFIRM_WINDOW_MS = 10_000L;
    private static long perkMenuSeenMs = 0L;

    private ShopMenu() {}

    public static boolean active(AbstractContainerScreen<?> screen) {
        if (!PhantomConfig.isShopMenuGuiEnabled()) return false;
        if (ShopKeybinds.isPerkMenu(screen)) {
            perkMenuSeenMs = System.currentTimeMillis();
            return true;
        }
        return ShopKeybinds.isConfirmMenu(screen)
                && System.currentTimeMillis() - perkMenuSeenMs <= CONFIRM_WINDOW_MS;
    }

    public static Layout layout(AbstractContainerScreen<?> screen) {
        if (!active(screen)) return null;

        AbstractContainerMenu menu = screen.getMenu();
        boolean confirm = ShopKeybinds.isConfirmMenu(screen);
        int main = confirm ? ShopKeybinds.confirmSlot(menu) : ShopKeybinds.mainSlot(menu);
        int cannon = confirm ? -1 : ShopKeybinds.cannonSlot(menu);
        if (main < 0 && cannon < 0) return null;

        int w = screen.width, h = screen.height;
        List<Entry> entries = new ArrayList<>();
        List<Entry> small = new ArrayList<>();

        List<Integer> heads = new ArrayList<>();
        List<Integer> others = new ArrayList<>();
        int containerSlots = menu.slots.size() > PLAYER_INVENTORY_SLOTS ? menu.slots.size() - PLAYER_INVENTORY_SLOTS : menu.slots.size();
        for (int i = 0; i < containerSlots; i++) {
            if (i == main || i == cannon) continue;
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty() || TextUtil.stripColor(stack.getHoverName().getString()).isBlank()) continue;
            if (heads.size() < MAX_HEADS && isInfoHead(stack)) heads.add(i);
            else others.add(i);
        }

        Panel strip = new Panel(0, 0, 0, 0);
        if (!others.isEmpty()) {
            int pad = 4;
            int maxRowW = (int) (w * 0.9);
            int size = 26;
            int n = others.size();
            while (size > 14 && n * (size + pad) - pad > maxRowW) size--;
            int perRow = Math.max(1, (maxRowW + pad) / (size + pad));
            int rows = (n + perRow - 1) / perRow;

            int bottom = h - HUD_CLEARANCE;
            int top = bottom - rows * (size + pad) + pad;
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            for (int r = 0; r < rows; r++) {
                int inRow = Math.min(perRow, n - r * perRow);
                int rowX = (w - (inRow * (size + pad) - pad)) / 2;
                int rowY = top + r * (size + pad);
                for (int c = 0; c < inRow; c++) {
                    int slot = others.get(r * perRow + c);
                    small.add(new Entry(slot, menu.slots.get(slot).getItem(), rowX + c * (size + pad), rowY, size, size, Kind.SMALL, 0, null));
                }
                minX = Math.min(minX, rowX);
                maxX = Math.max(maxX, rowX + inRow * (size + pad) - pad);
            }
            strip = new Panel(minX - 8, top - 6, maxX + 8, bottom + 6);
        }

        int limit = (others.isEmpty() ? h - HUD_CLEARANCE + 6 : strip.y1()) - 10;
        int big = Mth.clamp((int) Math.min(Math.min(w * 0.24, h * 0.40), limit - 8), 60, 170);
        int gap = big / 4;
        int x = (w - (2 * big + gap)) / 2;
        int y = Mth.clamp((int) (h * 0.5 - big * 0.6), 8, Math.max(8, limit - big));
        if (main >= 0) {
            entries.add(new Entry(main, menu.slots.get(main).getItem(), x, y, big, big, Kind.LARGE, PhantomConfig.getShopMainKey(), null));
        }
        if (cannon >= 0) {
            entries.add(new Entry(cannon, menu.slots.get(cannon).getItem(), x + big + gap, y, big, big, Kind.LARGE, PhantomConfig.getShopCannonKey(), null));
        }

        Panel info = new Panel(0, 0, 0, 0);
        if (!heads.isEmpty()) {
            int head = Mth.clamp((int) (h * 0.075), 20, 34);
            int sub = head * 4 / 5;
            int rowGap = Math.max(6, head / 2);
            int margin = 14;
            int widthNeeded = head + 6 + sub + 4 + sub;
            int roomLeft = x - 12 - margin - 8;
            if (widthNeeded > roomLeft && roomLeft > 0) {
                float f = Math.max(0.5f, (float) roomLeft / widthNeeded);
                head = Math.max(14, (int) (head * f));
                sub = head * 4 / 5;
                widthNeeded = head + 6 + sub + 4 + sub;
            }
            int colH = heads.size() * head + (heads.size() - 1) * rowGap;
            int top = Mth.clamp((h - colH) / 2, 14, Math.max(14, limit - colH));

            for (int r = 0; r < heads.size(); r++) {
                int slot = heads.get(r);
                ItemStack headStack = menu.slots.get(slot).getItem();
                int rowY = top + r * (head + rowGap);
                entries.add(new Entry(slot, headStack, margin, rowY, head, head, Kind.HEAD, 0, null));

                Perks perks = perks(headStack);
                int ix = margin + head + 6;
                int iy = rowY + (head - sub) / 2;
                if (perks.route() != null) {
                    ItemStack icon = findByName(menu, containerSlots, heads, perks.route().toLowerCase(Locale.ROOT));
                    entries.add(new Entry(slot, icon, ix, iy, sub, sub, Kind.ROUTE, 0, perks.route()));
                }
                if (perks.ballista() != null) {
                    ItemStack icon = findByName(menu, containerSlots, heads, "ballista mechanic");
                    entries.add(new Entry(slot, icon, ix + sub + 4, iy, sub, sub, Kind.BALLISTA, 0, perks.ballista()));
                }
            }
            info = new Panel(margin - 6, top - 6, margin + widthNeeded + 6, top + colH + 6);
        }

        entries.addAll(small);
        return new Layout(entries, strip, info);
    }

    public static Entry render(GuiGraphicsExtractor ctx, AbstractContainerScreen<?> screen, Layout layout, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        ctx.fill(0, 0, screen.width, screen.height, DIM);

        if (layout.strip().exists()) {
            Panel p = layout.strip();
            GuiDraw.roundedFill(ctx, p.x1(), p.y1(), p.x2(), p.y2(), STRIP, 6);
        }
        if (layout.info().exists()) {
            Panel p = layout.info();
            GuiDraw.roundedFill(ctx, p.x1(), p.y1(), p.x2(), p.y2(), STRIP, 6);
        }

        Entry hovered = layout.at(mx, my);
        for (Entry e : layout.entries()) {
            boolean hover = e == hovered;
            switch (e.kind()) {
                case LARGE -> {
                    GuiDraw.raisedFill(ctx, e.x(), e.y(), e.x() + e.w(), e.y() + e.h(), hover ? PANEL_HOVER : PANEL, 8);
                    int icon = (int) (e.w() * 0.5);
                    int cx = e.x() + e.w() / 2;
                    drawItem(ctx, font, e.stack(), cx - icon / 2, e.y() + (int) (e.h() * 0.12), icon);

                    int textY = e.y() + (int) (e.h() * 0.12) + icon + 8;
                    drawFitted(ctx, font, e.stack().getHoverName(), cx, textY, e.w() - 12, 0xFFFFFFFF);
                    if (e.keyCode() > 0) {
                        drawFitted(ctx, font, Component.literal("[" + KeyCapture.nameForCode(e.keyCode()) + "]"),
                                cx, textY + font.lineHeight + 4, e.w() - 12, KEY_HINT);
                    }
                }
                case SMALL, HEAD -> {
                    if (hover) GuiDraw.roundedFill(ctx, e.x() - 2, e.y() - 2, e.x() + e.w() + 2, e.y() + e.h() + 2, 0x55FFFFFF, 4);
                    drawItem(ctx, font, e.stack(), e.x(), e.y(), e.w());
                }
                case ROUTE, BALLISTA -> {
                    if (hover) GuiDraw.roundedFill(ctx, e.x() - 2, e.y() - 2, e.x() + e.w() + 2, e.y() + e.h() + 2, 0x55FFFFFF, 4);
                    boolean ballista = e.kind() == Kind.BALLISTA;
                    if (e.stack().isEmpty()) {
                        GuiDraw.roundedFill(ctx, e.x(), e.y(), e.x() + e.w(), e.y() + e.h(), BADGE, 4);
                        drawFitted(ctx, font, Component.literal(ballista ? "Ballista" : e.label()),
                                e.x() + e.w() / 2, e.y() + (e.h() - font.lineHeight) / 2, e.w() - 2, 0xFFFFFFFF);
                    } else {
                        drawItem(ctx, font, e.stack(), e.x(), e.y(), e.w());
                    }
                    if (ballista) {
                        drawFitted(ctx, font, Component.literal(e.label()),
                                e.x() + e.w() - 2, e.y() + e.h() - font.lineHeight + 1, e.w() - 2, LEVEL_TEXT, true);
                    }
                }
            }
        }
        return hovered;
    }

    // ── Reading the party member heads ────────────────────────────────────────

    private static boolean isInfoHead(ItemStack stack) {
        if (TextUtil.stripColor(stack.getHoverName().getString()).contains("'s Upgrades")) return true;
        for (String line : lore(stack)) {
            if (line.startsWith("Selected Route:") || line.startsWith("Current Tokens:") || line.startsWith("Perk Levels:")) return true;
        }
        return false;
    }

    private static Perks perks(ItemStack stack) {
        List<String> lines = lore(stack);
        String route = null, ballista = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("Selected Route:")) {
                String value = line.substring("Selected Route:".length()).strip();
                if (value.isEmpty() && i + 1 < lines.size()) value = lines.get(i + 1);
                route = value(value);
                continue;
            }
            int at = line.indexOf("Ballista Mechanic:");
            if (at >= 0) ballista = value(line.substring(at + "Ballista Mechanic:".length()));
        }
        return new Perks(route, ballista);
    }

    private static String value(String text) {
        String v = text.strip();
        if (v.isEmpty() || v.matches("[-–—−]+") || v.equalsIgnoreCase("none")) return null;
        return v;
    }

    private static List<String> lore(ItemStack stack) {
        List<String> out = new ArrayList<>();
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return out;
        for (FormattedText line : lore.lines()) out.add(TextUtil.stripColor(line.getString()).strip());
        return out;
    }

    private static ItemStack findByName(AbstractContainerMenu menu, int containerSlots, List<Integer> skip, String fragment) {
        for (int i = 0; i < containerSlots; i++) {
            if (skip.contains(i)) continue;
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            if (TextUtil.stripColor(stack.getHoverName().getString()).toLowerCase(Locale.ROOT).contains(fragment)) return stack;
        }
        return ItemStack.EMPTY;
    }

    // ── Drawing helpers ───────────────────────────────────────────────────────

    private static void drawItem(GuiGraphicsExtractor ctx, Font font, ItemStack stack, int x, int y, int size) {
        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(size / 16f, size / 16f);
        ctx.item(stack, 0, 0);
        ctx.itemDecorations(font, stack, 0, 0);
        pose.popMatrix();
    }

    private static void drawFitted(GuiGraphicsExtractor ctx, Font font, Component text, int cx, int y, int maxWidth, int color) {
        drawFitted(ctx, font, text, cx, y, maxWidth, color, false);
    }

    private static void drawFitted(GuiGraphicsExtractor ctx, Font font, Component text, int x, int y, int maxWidth, int color, boolean rightAligned) {
        int width = font.width(text);
        float scale = width > maxWidth ? (float) maxWidth / width : 1f;
        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        ctx.text(font, text, rightAligned ? -width : -width / 2, 0, color, true);
        pose.popMatrix();
    }
}
