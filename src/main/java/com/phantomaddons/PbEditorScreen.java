package com.phantomaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PbEditorScreen extends Screen {

    private static final String[] PHASE_NAMES = {"Supplies", "Build", "Eaten", "Stun", "DPS", "Skip", "Boss"};
    private static final int ROW_H = 22;
    private static final int PAD   = 12;
    private static final int LABEL_W = 90;

    private final Screen parent;
    private int tier;

    private EditBox totalTimeBox;
    private final EditBox[] phaseBoxes = new EditBox[7];
    private final EditBox[] playerNameBoxes = new EditBox[4];
    private final int[] crateOwner = new int[6]; // index into player names, 0-3
    private final EditBox[] crateTimeBoxes = new EditBox[6];
    private EditBox freshesBox;

    // scroll
    private int scroll = 0;
    private int maxScroll = 0;
    private int contentTop, contentBottom, contentH;

    // natural (unscrolled) y of each row, in the same order fields are laid out
    private final List<EditBox> orderedBoxes = new ArrayList<>();
    private final List<Integer> orderedNaturalY = new ArrayList<>();
    private int crateRowsNaturalY, crateRowH = ROW_H;

    private int panelX, panelY, panelW, panelH;

    public PbEditorScreen(Screen parent, int tier) {
        super(Component.literal("Set Personal Best"));
        this.parent = parent;
        this.tier = Math.max(1, Math.min(5, tier));
    }

    @Override
    protected void init() {
        panelW = Math.min(460, width - 40);
        panelH = Math.min(500, height - 40);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        orderedBoxes.clear();
        orderedNaturalY.clear();

        int y = 0;
        y += ROW_H; // total time
        totalTimeBox = addField(String.valueOf(existingRecord() != null ? existingRecord().totalTime : ""), y);
        orderedNaturalY.add(y); orderedBoxes.add(totalTimeBox);
        y += ROW_H;

        for (int i = 0; i < 7; i++) {
            double v = existingRecord() != null ? existingRecord().splits[i] : 9999.0;
            phaseBoxes[i] = addField(v >= 9999.0 ? "" : String.valueOf(v), y);
            orderedNaturalY.add(y); orderedBoxes.add(phaseBoxes[i]);
            y += ROW_H;
        }

        for (int i = 0; i < 4; i++) {
            String defaultName = "Player " + (i + 1);
            String existing = existingPlayerName(i, defaultName);
            playerNameBoxes[i] = addField(existing, y);
            orderedNaturalY.add(y); orderedBoxes.add(playerNameBoxes[i]);
            y += ROW_H;
        }

        crateRowsNaturalY = y;
        List<PhantomConfig.PlayerTime> existingSupplies = existingRecord() != null ? existingRecord().supplies : null;
        for (int i = 0; i < 6; i++) {
            crateOwner[i] = 0;
            String timeStr = "";
            if (existingSupplies != null && i < existingSupplies.size()) {
                PhantomConfig.PlayerTime pt = existingSupplies.get(i);
                timeStr = String.valueOf(pt.time);
                for (int p = 0; p < 4; p++) {
                    if (playerNameBoxes[p].getValue().equalsIgnoreCase(pt.player)) { crateOwner[i] = p; break; }
                }
            }
            crateTimeBoxes[i] = addField(timeStr, y);
            orderedNaturalY.add(y); orderedBoxes.add(crateTimeBoxes[i]);
            y += ROW_H;
        }

        y += ROW_H; // freshes label row
        String freshDefault = existingFreshesString();
        freshesBox = new EditBox(font, 0, 0, panelW - PAD * 2, 16, Component.empty());
        freshesBox.setMaxLength(512);
        freshesBox.setValue(freshDefault);
        freshesBox.setHint(Component.literal("name:time, name:time, ..."));
        addRenderableWidget(freshesBox);
        orderedNaturalY.add(y); orderedBoxes.add(freshesBox);
        y += ROW_H;

        contentTop = panelY + 34;
        contentBottom = panelY + panelH - 40;
        contentH = contentBottom - contentTop;
        int totalContentH = y;
        maxScroll = Math.max(0, totalContentH - contentH);
        scroll = 0;
        layoutFields();
    }

    private EditBox addField(String value, int naturalY) {
        EditBox box = new EditBox(font, 0, 0, panelW - LABEL_W - PAD * 2 - 10, 16, Component.empty());
        box.setMaxLength(64);
        box.setValue(value);
        addRenderableWidget(box);
        return box;
    }

    private void layoutFields() {
        for (int i = 0; i < orderedBoxes.size(); i++) {
            EditBox box = orderedBoxes.get(i);
            int naturalY = orderedNaturalY.get(i);
            int screenY = contentTop + naturalY - scroll;
            boolean visible = screenY + 16 >= contentTop && screenY <= contentBottom;
            box.setVisible(visible);
            box.setY(screenY);
            if (box == freshesBox) {
                box.setX(panelX + PAD);
            } else {
                box.setX(panelX + PAD + LABEL_W);
            }
        }
    }

    private PhantomConfig.PbRecord existingRecord() {
        return PhantomConfig.getPbRecord(tier);
    }

    private String existingPlayerName(int idx, String fallback) {
        PhantomConfig.PbRecord r = existingRecord();
        if (r == null || r.supplies == null) return fallback;
        List<String> distinct = new ArrayList<>();
        for (PhantomConfig.PlayerTime pt : r.supplies) {
            if (!distinct.contains(pt.player)) distinct.add(pt.player);
        }
        return idx < distinct.size() ? distinct.get(idx) : fallback;
    }

    private String existingFreshesString() {
        PhantomConfig.PbRecord r = existingRecord();
        if (r == null || r.freshes == null || r.freshes.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (PhantomConfig.PlayerTime pt : r.freshes) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(pt.player).append(':').append(pt.time);
        }
        return sb.toString();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xA8000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        extractBackground(ctx, mx, my, delta);

        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF0101014);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFFFFAA00);

        ctx.text(font, Component.literal("§bSet Personal Best §7- Tier:"), panelX + PAD, panelY + 10, 0xFFFFFFFF);
        String tierLabel = "T" + tier;
        int tierBoxW = 30, tierBoxX = panelX + panelW - PAD - tierBoxW, tierBoxY = panelY + 6;
        boolean tierHov = mx >= tierBoxX && mx <= tierBoxX + tierBoxW && my >= tierBoxY && my <= tierBoxY + 16;
        ctx.fill(tierBoxX, tierBoxY, tierBoxX + tierBoxW, tierBoxY + 16, tierHov ? 0xFF223366 : 0xFF151E33);
        ctx.text(font, Component.literal("§e" + tierLabel), tierBoxX + 6, tierBoxY + 4, 0xFFFFFFFF);

        ctx.enableScissor(panelX, contentTop, panelX + panelW, contentBottom);

        drawLabel(ctx, "Total Time", orderedNaturalY.get(0));
        for (int i = 0; i < 7; i++) drawLabel(ctx, PHASE_NAMES[i], orderedNaturalY.get(1 + i));
        for (int i = 0; i < 4; i++) drawLabel(ctx, "Player " + (i + 1), orderedNaturalY.get(8 + i));

        for (int i = 0; i < 6; i++) {
            int naturalY = crateRowsNaturalY + i * crateRowH;
            int screenY = contentTop + naturalY - scroll;
            if (screenY + ROW_H < contentTop || screenY > contentBottom) continue;

            drawLabel(ctx, "Crate " + (i + 1), naturalY);

            int cw = 70, cx = panelX + PAD + LABEL_W, cy = screenY + 2;
            boolean hov = mx >= cx && mx <= cx + cw && my >= cy && my <= cy + 16;
            ctx.fill(cx, cy, cx + cw, cy + 16, hov ? 0xFF223366 : 0xFF151E33);
            String pname = playerNameBoxes[crateOwner[i]].getValue();
            if (pname.isBlank()) pname = "Player " + (crateOwner[i] + 1);
            ctx.text(font, Component.literal("§e" + pname), cx + 4, cy + 4, 0xFFFFFFFF);
        }

        int freshLabelY = orderedNaturalY.get(orderedNaturalY.size() - 1) - ROW_H;
        int freshScreenY = contentTop + freshLabelY - scroll;
        if (freshScreenY >= contentTop - ROW_H && freshScreenY <= contentBottom) {
            ctx.text(font, Component.literal("§7Freshes (name:time, ...)"), panelX + PAD, freshScreenY + 6, 0xFFAAAAAA);
        }

        ctx.disableScissor();

        if (maxScroll > 0) {
            int barX = panelX + panelW - 5;
            int barH = Math.max(16, contentH * contentH / (contentH + maxScroll));
            int barY = contentTop + (int) ((float) scroll / maxScroll * (contentH - barH));
            ctx.fill(barX, contentTop, barX + 3, contentBottom, 0x33FFFFFF);
            ctx.fill(barX, barY, barX + 3, barY + barH, 0x88FFFFFF);
        }

        int saveW = 70, cancelW = 70, btnH = 18, btnY = panelY + panelH - 26;
        int saveX = panelX + panelW - PAD - saveW;
        int cancelX = saveX - 8 - cancelW;
        boolean saveHov = mx >= saveX && mx <= saveX + saveW && my >= btnY && my <= btnY + btnH;
        boolean cancelHov = mx >= cancelX && mx <= cancelX + cancelW && my >= btnY && my <= btnY + btnH;
        ctx.fill(saveX, btnY, saveX + saveW, btnY + btnH, saveHov ? 0xFF2A5A2A : 0xFF1A3A1A);
        ctx.text(font, Component.literal("§aSave"), saveX + saveW / 2 - 10, btnY + 5, 0xFFFFFFFF);
        ctx.fill(cancelX, btnY, cancelX + cancelW, btnY + btnH, cancelHov ? 0xFF4A2A2A : 0xFF3A1A1A);
        ctx.text(font, Component.literal("§cCancel"), cancelX + cancelW / 2 - 15, btnY + 5, 0xFFFFFFFF);

        super.extractRenderState(ctx, mx, my, delta);
    }

    private void drawLabel(GuiGraphicsExtractor ctx, String text, int naturalY) {
        int screenY = contentTop + naturalY - scroll;
        if (screenY + ROW_H < contentTop || screenY > contentBottom) return;
        ctx.text(font, Component.literal("§7" + text), panelX + PAD, screenY + 6, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isDoubleClick) {
        if (super.mouseClicked(click, isDoubleClick)) return true;
        double mx = click.x(), my = click.y();
        if (click.button() != 0) return false;

        int tierBoxW = 30, tierBoxX = panelX + panelW - PAD - tierBoxW, tierBoxY = panelY + 6;
        if (mx >= tierBoxX && mx <= tierBoxX + tierBoxW && my >= tierBoxY && my <= tierBoxY + 16) {
            tier = tier >= 5 ? 1 : tier + 1;
            return true;
        }

        if (my >= contentTop && my <= contentBottom) {
            for (int i = 0; i < 6; i++) {
                int naturalY = crateRowsNaturalY + i * crateRowH;
                int screenY = contentTop + naturalY - scroll;
                int cw = 70, cx = panelX + PAD + LABEL_W, cy = screenY + 2;
                if (mx >= cx && mx <= cx + cw && my >= cy && my <= cy + 16) {
                    crateOwner[i] = (crateOwner[i] + 1) % 4;
                    return true;
                }
            }
        }

        int saveW = 70, cancelW = 70, btnH = 18, btnY = panelY + panelH - 26;
        int saveX = panelX + panelW - PAD - saveW;
        int cancelX = saveX - 8 - cancelW;
        if (mx >= saveX && mx <= saveX + saveW && my >= btnY && my <= btnY + btnH) { save(); return true; }
        if (mx >= cancelX && mx <= cancelX + cancelW && my >= btnY && my <= btnY + btnH) { onClose(); return true; }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        if (maxScroll > 0 && mx >= panelX && mx <= panelX + panelW && my >= contentTop && my <= contentBottom) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (vScroll * ROW_H)));
            layoutFields();
            return true;
        }
        return super.mouseScrolled(mx, my, hScroll, vScroll);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) { onClose(); return true; } // Esc
        return super.keyPressed(event);
    }

    private void save() {
        PhantomConfig.PbRecord record = new PhantomConfig.PbRecord();
        record.totalTime = parseOr(totalTimeBox.getValue(), 9999.0);
        for (int i = 0; i < 7; i++) record.splits[i] = parseOr(phaseBoxes[i].getValue(), 9999.0);
        record.dateMs = System.currentTimeMillis();

        record.supplies = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String time = crateTimeBoxes[i].getValue();
            if (time.isBlank()) continue;
            String pname = playerNameBoxes[crateOwner[i]].getValue();
            if (pname.isBlank()) pname = "Player " + (crateOwner[i] + 1);
            record.supplies.add(new PhantomConfig.PlayerTime(pname, parseOr(time, 0)));
        }

        record.freshes = new ArrayList<>();
        String freshRaw = freshesBox.getValue();
        if (!freshRaw.isBlank()) {
            for (String part : freshRaw.split(",")) {
                String p = part.trim();
                if (p.isEmpty()) continue;
                int idx = p.lastIndexOf(':');
                if (idx <= 0) continue;
                String name = p.substring(0, idx).trim();
                double time = parseOr(p.substring(idx + 1).trim(), -1);
                if (time < 0 || name.isEmpty()) continue;
                record.freshes.add(new PhantomConfig.PlayerTime(name, time));
            }
        }

        PhantomConfig.setPbRecord(tier, record);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(
                    "§f[PhantomAddons]§r §aT" + tier + " PB set: " + PhantomConfig.formatTime(record.totalTime)));
        }
        onClose();
    }

    private static double parseOr(String s, double fallback) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return fallback; }
    }
}
