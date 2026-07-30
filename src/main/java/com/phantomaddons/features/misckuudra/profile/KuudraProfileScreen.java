package com.phantomaddons.features.misckuudra.profile;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KuudraProfileScreen extends Screen {

    private static final int C_BG      = 0xEE0B0C10;
    private static final int C_BORDER  = 0xFF2A3345;
    private static final int C_HEADER  = 0xFF15161C;
    private static final int C_TEXT    = 0xFFD4D8E8;
    private static final int C_TEXT_DIM= 0xFF8A90A8;
    private static final int C_COL_BG  = 0x22FFFFFF;
    private static final int C_TT_BG   = 0xF0100010;
    private static final int C_TT_BORDER = 0x505000FF;

    private static final int PANEL_MAX_W  = 780;
    private static final int PANEL_PAD    = 10;
    private static final int COL_GAP      = 8;
    private static final int COL_MIN_W    = 100;
    private static final int COL_PAD      = 4;
    private static final int LINE_H       = 11;
    private static final int GAP_BETWEEN_PANELS = 10;

    private final List<String> playerNames;
    private final Map<String, KuudraProfileData> data = new LinkedHashMap<>();

    private int scroll = 0;
    private KuudraProfileData.ItemInfo hoveredItem = null;
    private int hoverX;
    private int hoverY;

    public KuudraProfileScreen(List<String> playerNames) {
        super(Component.literal("Kuudra Profiles"));
        this.playerNames = new ArrayList<>(playerNames);
        for (String name : this.playerNames) {
            data.put(name, null);
            KuudraProfileFetcher.fetchAsync(name, result -> data.put(name, result));
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xAA05060A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        extractBackground(ctx, mx, my, delta);

        hoveredItem = null;

        int panelW = Math.min(PANEL_MAX_W, width - 40);
        int x = width / 2 - panelW / 2;
        int y = 20 - scroll;

        for (String name : playerNames) {
            KuudraProfileData d = data.get(name);
            Layout layout = buildLayout(d, panelW);
            int h = layout.height();
            if (y + h > 0 && y < height) {
                renderPanel(ctx, name, d, layout, x, y, panelW, mx, my);
            }
            y += h + GAP_BETWEEN_PANELS;
        }

        if (hoveredItem != null) {
            renderTooltip(ctx, hoveredItem, hoverX, hoverY);
        }

        super.extractRenderState(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        scroll = Math.max(0, scroll - (int) (vScroll * 14));
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == 256) { onClose(); return true; } // Escape
        return super.keyPressed(event);
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private record Row(List<String> lines, KuudraProfileData.ItemInfo item) {}

    private record Column(String title, List<Row> rows, int width) {}

    private record Layout(List<Column> columns, int gap, int height) {}

    private Layout buildLayout(KuudraProfileData d, int panelW) {
        int innerW = panelW - PANEL_PAD * 2;

        List<RawColumn> raw = buildRawColumns(d);
        int numCols = Math.max(1, raw.size());
        int gap = numCols > 1 ? COL_GAP : 0;
        int colWidth = Math.max(COL_MIN_W, (innerW - gap * (numCols - 1)) / numCols);

        List<Column> columns = new ArrayList<>();
        int maxLines = 1; // title line
        for (RawColumn rc : raw) {
            List<Row> rows = new ArrayList<>();
            int lineCount = 1;
            for (RawRow rr : rc.rawRows()) {
                List<String> lines = wrap(rr.text(), colWidth - COL_PAD * 2);
                rows.add(new Row(lines, rr.item()));
                lineCount += lines.size();
            }
            columns.add(new Column(rc.title(), rows, colWidth));
            maxLines = Math.max(maxLines, lineCount);
        }

        int header = 20;
        int height = header + maxLines * LINE_H + PANEL_PAD * 2;
        return new Layout(columns, gap, height);
    }

    private record RawRow(String text, KuudraProfileData.ItemInfo item) {}

    private record RawColumn(String title, List<RawRow> rawRows) {}

    private List<RawColumn> buildRawColumns(KuudraProfileData d) {
        List<RawColumn> cols = new ArrayList<>();
        if (d == null || !d.loaded) return cols;

        List<RawRow> statLines = new ArrayList<>();
        statLines.add(new RawRow("§bBank:§r " + (d.bankBalance >= 0 ? formatCoins(d.bankBalance) : "?"), null));
        statLines.add(new RawRow("§bMagical Power:§r " + (d.magicalPower >= 0 ? d.magicalPower : "?"), null));
        statLines.add(new RawRow("§bCatacombs:§r " + (d.catacombsLevel >= 0 ? d.catacombsLevel : "?"), null));
        statLines.add(new RawRow("§bForaging:§r " + (d.foragingLevel >= 0 ? d.foragingLevel : "?"), null));
        for (KuudraProfileData.KuudraTier tier : KuudraProfileData.KuudraTier.values()) {
            statLines.add(new RawRow("§7" + tierLabel(tier) + ":§r " + d.getKuudraCompletions(tier), null));
        }
        cols.add(new RawColumn("Stats", statLines));

        List<RawRow> weaponLines = new ArrayList<>();
        for (KuudraProfileData.Weapon w : KuudraProfileData.Weapon.values()) {
            KuudraProfileData.ItemInfo item = d.getWeapon(w);
            boolean has = item != null;
            String rowLabel = has && item.displayName() != null ? item.displayName() : w.label;
            weaponLines.add(new RawRow((has ? "§a✔ " : "§c✖ ") + rowLabel, item));
        }
        weaponLines.add(new RawRow(gdragLine("DPS", d.dpsGoldenDragonPet), null));
        weaponLines.add(new RawRow(gdragLine("Rend", d.rendGoldenDragonPet), null));
        cols.add(new RawColumn("Weapons", weaponLines));

        for (KuudraProfileData.ArmorSetResult set : d.armorSets) {
            List<RawRow> lines = new ArrayList<>();
            for (KuudraProfileData.ItemInfo piece : set.pieces()) {
                lines.add(new RawRow("§7- " + (piece.displayName() != null ? piece.displayName() : "?"), piece));
            }
            cols.add(new RawColumn(set.label(), lines));
        }

        return cols;
    }

    private static String formatCoins(long amount) {
        double value;
        String suffix;
        if (amount >= 1_000_000_000L) { value = amount / 1_000_000_000.0; suffix = "b"; }
        else if (amount >= 1_000_000L) { value = amount / 1_000_000.0; suffix = "m"; }
        else if (amount >= 1_000L) { value = amount / 1_000.0; suffix = "k"; }
        else return String.valueOf(amount);

        String formatted = String.format("%.1f", value);
        if (formatted.endsWith(".0")) formatted = formatted.substring(0, formatted.length() - 2);
        return formatted + suffix;
    }

    private static String gdragLine(String role, KuudraProfileData.GoldenDragonPet pet) {
        if (pet == null) return "§7" + role + " GDrag:§r §8None";
        return "§7" + role + " [" + pet.level() + "] GDrag:§r " + (pet.item() != null ? pet.item() : "?");
    }

    private void renderPanel(GuiGraphicsExtractor ctx, String name, KuudraProfileData d, Layout layout,
                              int x, int y, int panelW, int mx, int my) {
        int h = layout.height();
        ctx.fill(x - 1, y - 1, x + panelW + 1, y + h + 1, C_BORDER);
        ctx.fill(x, y, x + panelW, y + h, C_BG);
        ctx.fill(x, y, x + panelW, y + 20, C_HEADER);
        String header = (d != null && d.nameTag != null) ? d.nameTag : ("§e" + name);
        ctx.text(font, Component.literal(header), x + 8, y + (20 - font.lineHeight) / 2, C_TEXT);

        int contentY = y + 20 + PANEL_PAD;
        int contentH = h - 20 - PANEL_PAD * 2;

        if (d == null) {
            ctx.text(font, Component.literal("§7Loading..."), x + 8, contentY, C_TEXT_DIM);
            return;
        }
        if (!d.loaded) {
            String msg = d.errorMessage != null ? "§cFailed to load: " + d.errorMessage : "§7Loading...";
            ctx.text(font, Component.literal(msg), x + 8, contentY, C_TEXT_DIM);
            return;
        }

        int cx = x + PANEL_PAD;
        for (Column c : layout.columns()) {
            renderColumn(ctx, c, cx, contentY, contentH, mx, my);
            cx += c.width() + layout.gap();
        }
    }

    private void renderColumn(GuiGraphicsExtractor ctx, Column col, int x, int y, int h, int mx, int my) {
        ctx.fill(x, y, x + col.width(), y + h, C_COL_BG);
        ctx.text(font, Component.literal("§e" + col.title()), x + COL_PAD, y + 2, C_TEXT);
        int ly = y + 2 + LINE_H;
        for (Row row : col.rows()) {
            int rowTop = ly;
            for (String line : row.lines()) {
                ctx.text(font, Component.literal(line), x + COL_PAD, ly, C_TEXT);
                ly += LINE_H;
            }
            if (row.item() != null) {
                int rowH = ly - rowTop;
                if (mx >= x + COL_PAD && mx <= x + col.width() - COL_PAD && my >= rowTop && my < rowTop + rowH) {
                    hoveredItem = row.item();
                    hoverX = mx;
                    hoverY = my;
                }
            }
        }
    }

    private void renderTooltip(GuiGraphicsExtractor ctx, KuudraProfileData.ItemInfo item, int mx, int my) {
        List<String> lines = new ArrayList<>();
        lines.add(item.displayName() != null ? item.displayName() : "§7Unknown Item");
        if (item.lore() != null) lines.addAll(item.lore());

        int tw = 0;
        for (String line : lines) tw = Math.max(tw, font.width(line));
        int th = lines.size() * LINE_H + 4;

        int tx = mx + 12;
        int ty = my - 4;
        if (tx + tw + 8 > width) tx = width - tw - 8;
        if (ty + th > height) ty = height - th;
        if (ty < 0) ty = 0;

        ctx.fill(tx - 4, ty - 4, tx + tw + 4, ty + th, C_TT_BG);
        ctx.fill(tx - 4, ty - 4, tx + tw + 4, ty - 3, C_TT_BORDER);
        ctx.fill(tx - 4, ty + th - 1, tx + tw + 4, ty + th, C_TT_BORDER);
        ctx.fill(tx - 4, ty - 4, tx - 3, ty + th, C_TT_BORDER);
        ctx.fill(tx + tw + 3, ty - 4, tx + tw + 4, ty + th, C_TT_BORDER);

        int ly = ty;
        for (String line : lines) {
            ctx.text(font, Component.literal(line), tx, ly, C_TEXT);
            ly += LINE_H;
        }
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        if (maxWidth <= 0 || font.width(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        String activeFormat = "";
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.width(candidate) > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(activeFormat + word);
            } else {
                current = new StringBuilder(candidate);
            }
            activeFormat = lastFormat(word, activeFormat);
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines.isEmpty() ? List.of(text) : lines;
    }

    private static String lastFormat(String word, String previous) {
        String active = previous;
        for (int i = 0; i + 1 < word.length(); i++) {
            if (word.charAt(i) == '§') {
                char code = Character.toLowerCase(word.charAt(i + 1));
                active = (code == 'r') ? "" : "§" + code;
                i++;
            }
        }
        return active;
    }

    private static String tierLabel(KuudraProfileData.KuudraTier tier) {
        return switch (tier) {
            case INFERNAL -> "Infernal";
            case FIERY -> "Fiery";
            case BURNING -> "Burning";
            case HOT -> "Hot";
            case BASIC -> "Basic";
        };
    }
}
