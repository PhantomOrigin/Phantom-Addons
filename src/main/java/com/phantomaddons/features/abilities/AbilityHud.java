package com.phantomaddons.features.abilities;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.abilities.AbilityCooldowns.Timer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import static com.phantomaddons.gui.GuiDraw.roundedFill;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AbilityHud {

    public record Row(String id, ItemStack icon, String ability, String item,
                      long cooldownLeft, long cooldownTotal, long activeLeft, long activeTotal) {}

    private static final int ICON = 16, GAP = 5, ROW_GAP = 3, BAR_W = 84, BAR_H = 17, INNER_H = BAR_H - 2, ACTIVE_ROWS = 4, SEPARATOR = 1, BAR_RADIUS = 5;

    private AbilityHud() {}

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "ability_cooldowns"),
                (ctx, tickDelta) -> render(ctx));
    }

    // ── Rows ──────────────────────────────────────────────────────────────────

    public static Row rowOf(Timer t, long now) {
        return new Row(t.ability.id, t.icon, t.ability.name, t.ability.itemName,
                t.cooldownLeft(now), t.cooldownMs, t.activeLeft(now), t.activeMs);
    }

    public static List<Row> previewRows(AbilitySettings s) {
        List<Row> out = new ArrayList<>();
        for (String id : s.selected) {
            AbilityData.Ability a = AbilityData.byId(id);
            if (a == null) continue;
            long total = Math.round(a.cooldown * 1000), active = Math.round(a.active * 1000);
            out.add(new Row(id, AbilityData.icon(a), a.name, a.itemName, total * 6 / 10, total, active * 4 / 10, active));
        }
        return out;
    }

    public static List<Row> previewRows(AbilitySettings s, int group) {
        List<Row> out = new ArrayList<>();
        for (Row r : previewRows(s)) if (s.inGroup(r.id(), group)) out.add(r);
        return out;
    }

    public static Row sampleRow() {
        return new Row("sample", new ItemStack(net.minecraft.world.item.Items.IRON_SWORD),
                "Wither Impact", "Hyperion", 3200, 5000, 1200, 3000);
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    private static Row stepped(AbilitySettings.Style s, Row r) {
        if (s.smooth) return r;
        return new Row(r.id(), r.icon(), r.ability(), r.item(), tick(r.cooldownLeft()), r.cooldownTotal(),
                tick(r.activeLeft()), r.activeTotal());
    }

    private static long tick(long ms) { return ms <= 0 ? 0 : (ms + 49) / 50 * 50; }

    private static boolean showsActive(AbilitySettings.Style s, Row r) {
        return s.showActive && r.activeTotal() > 0 && r.activeLeft() > 0;
    }

    private static String label(int lab, Row r) {
        return lab == AbilitySettings.LABEL_ITEM ? r.item() : r.ability();
    }

    private static int labelWidth(Font font, int lab, Row r) {
        return switch (lab) {
            case AbilitySettings.LABEL_NONE -> 0;
            case AbilitySettings.LABEL_ICON -> ICON;
            default -> font.width(label(lab, r));
        };
    }

    public static String format(long ms, int format) {
        return switch (format) {
            case AbilitySettings.FORMAT_TICKS -> (long) Math.ceil(ms / 50.0) + "t";
            case AbilitySettings.FORMAT_MS -> ms + "ms";
            default -> String.format(Locale.ROOT, "%.1fs", ms / 1000.0);
        };
    }

    private static int valueWidth(Font font, AbilitySettings.Style s, Row r) {
        if (s.bar) return BAR_W;
        int w = font.width(format(r.cooldownLeft(), s.format));
        if (showsActive(s, r)) w += font.width(format(r.activeLeft(), s.format)) + 5;
        return w;
    }

    private static int valueHeight(Font font, AbilitySettings.Style s, Row r) {
        if (s.bar) return BAR_H;
        return font.lineHeight;
    }

    public static int rowWidth(Font font, AbilitySettings.Style s, Row r) {
        r = stepped(s, r);
        int label = labelWidth(font, s.label, r);
        return label + (label > 0 ? GAP : 0) + valueWidth(font, s, r);
    }

    public static int rowHeight(Font font, AbilitySettings.Style s, Row r) {
        int label = s.label == AbilitySettings.LABEL_NONE ? 0 : s.label == AbilitySettings.LABEL_ICON ? ICON : font.lineHeight;
        return Math.max(label, valueHeight(font, s, r));
    }

    public static int rowStep(Font font, AbilitySettings.Style s, Row r) { return rowHeight(font, s, r) + ROW_GAP; }

    public static void draw(GuiGraphicsExtractor ctx, Font font, AbilitySettings.Style s, Row r, int x, int y) {
        r = stepped(s, r);
        int h = rowHeight(font, s, r);
        int lw = labelWidth(font, s.label, r);
        if (s.label == AbilitySettings.LABEL_ICON) {
            ctx.item(r.icon(), x, y + (h - ICON) / 2);
        } else if (s.label != AbilitySettings.LABEL_NONE) {
            ctx.text(font, label(s.label, r), x, y + (h - font.lineHeight) / 2, 0xFFFFFFFF, true);
        }
        int vx = x + lw + (lw > 0 ? GAP : 0);
        int cd = 0xFF000000 | s.cooldownColor, act = 0xFF000000 | s.activeColor;
        boolean active = showsActive(s, r);

        if (s.bar) {
            bar(ctx, font, s, r, vx, y + (h - valueHeight(font, s, r)) / 2);
            return;
        }
        int ty = y + (h - font.lineHeight) / 2;
        if (active) {
            String a = format(r.activeLeft(), s.format);
            ctx.text(font, a, vx, ty, act, true);
            vx += font.width(a) + 5;
        }
        ctx.text(font, format(r.cooldownLeft(), s.format), vx, ty, cd, true);
    }

    private static float fraction(long left, long total) {
        return total <= 0 ? 0f : Math.max(0f, Math.min(1f, left / (float) total));
    }

    // ── The bar and how it changes shape ──────────────────────────────────────

    private static final class Anim { float activeRows; long at; }
    private static final java.util.Map<String, Anim> anims = new java.util.HashMap<>();
    private static final double SHIFT_SECONDS = 0.04;

    private static Anim anim(AbilitySettings.Style s, Row r) {
        boolean showActive = showsActive(s, r);
        boolean both = showActive && r.cooldownLeft() > 0;
        float target = !showActive ? 0 : both ? ACTIVE_ROWS : INNER_H;

        long now = System.nanoTime();
        String key = r.id() + "@" + s.group;
        Anim a = anims.get(key);
        if (a == null) {
            a = new Anim();
            a.activeRows = target;
            a.at = now;
            anims.put(key, a);
            return a;
        }
        float k = 1f - (float) Math.exp(-((now - a.at) / 1e9) / SHIFT_SECONDS);
        a.at = now;
        a.activeRows += (target - a.activeRows) * k;
        if (Math.abs(target - a.activeRows) < 0.05f) a.activeRows = target;
        return a;
    }

    private static final int TRACK = 0xFF1A1C24;

    private static void bar(GuiGraphicsExtractor ctx, Font font, AbilitySettings.Style s, Row r, int x, int y) {
        Anim a = anim(s, r);
        var pose = ctx.pose();
        double gui = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        float k = (float) (gui * pose.m00());
        if (k <= 0f) return;
        int ox = Math.round((float) (gui * (pose.m00() * x + pose.m20())));
        int oy = Math.round((float) (gui * (pose.m11() * y + pose.m21())));

        int w = Math.round(BAR_W * k), h = Math.round(BAR_H * k);
        int edge = Math.max(1, Math.round(k));
        int radius = Math.min(Math.round(BAR_RADIUS * k), h / 2);
        int ix = ox + edge, iy = oy + edge, iw = w - 2 * edge, ih = h - 2 * edge, ir = Math.max(0, radius - edge);
        if (iw <= 0 || ih <= 0) return;

        int activeRows = Math.max(0, Math.min(ih, Math.round(a.activeRows * k)));
        int line = Math.max(1, Math.round(SEPARATOR * k));
        int divider = 0, cooldownRows = ih - activeRows;
        if (activeRows > 0 && cooldownRows > line) { divider = line; cooldownRows -= divider; }
        else if (activeRows > 0) { activeRows = ih; cooldownRows = 0; }

        int cooldownFill = Math.round(iw * fraction(r.cooldownLeft(), r.cooldownTotal()));
        int activeFill = Math.round(iw * fraction(r.activeLeft(), r.activeTotal()));

        pose.pushMatrix();
        pose.identity();
        pose.scale((float) (1.0 / gui), (float) (1.0 / gui));
        roundedFill(ctx, ox, oy, ox + w, oy + h, TRACK, radius);
        if (activeRows > 0) fillRows(ctx, ix, iy, iw, ih, activeFill, 0xFF000000 | s.activeColor, ir, ih - activeRows, ih);
        if (cooldownRows > 0) fillRows(ctx, ix, iy, iw, ih, cooldownFill, 0xFF000000 | s.cooldownColor, ir, 0, cooldownRows);
        pose.popMatrix();

        boolean activeBigger = activeRows > cooldownRows;
        String time = format(activeBigger ? r.activeLeft() : r.cooldownLeft(), s.format);
        int regionTop = activeBigger ? cooldownRows + divider : 0;
        int regionH = activeBigger ? activeRows : cooldownRows;
        float regionUnits = regionH / k;
        if (regionUnits >= font.lineHeight)
            ctx.text(font, time, x + (BAR_W - font.width(time)) / 2,
                    Math.round(y + (edge + regionTop) / k + (regionUnits - font.lineHeight) / 2f) + 1, 0xFFFFFFFF, true);
    }

    private static final Identifier CORNER_TL = Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/corner_tl.png");
    private static final Identifier CORNER_TR = Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/corner_tr.png");
    private static final Identifier CORNER_BL = Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/corner_bl.png");
    private static final Identifier CORNER_BR = Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/corner_br.png");
    private static final int CORNER_TEX = 64;

    private static void fillRows(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int fillAt, int color, int radius,
                                 int top, int bottom) {
        if (fillAt <= 0) return;
        shapeRows(ctx, x, y, w, h, radius, top, bottom, color, true, true, true, true, fillAt);
    }

    private static void shapeRows(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int radius, int top, int bottom,
                                  int color, boolean tl, boolean tr, boolean bl, boolean br, int cut) {
        top = Math.max(0, top);
        bottom = Math.min(h, bottom);
        cut = Math.min(cut, w);
        if (bottom <= top || cut <= 0) return;
        int r = Math.max(0, Math.min(radius, h / 2));
        boolean left = tl || bl, right = tr || br;
        int rl = left ? r : 0, rr = right ? r : 0;
        if (left && right) rl = rr = Math.min(r, w / 2);
        else if (left) rl = Math.min(r, w);
        else if (right) rr = Math.min(r, w);
        if (rl == 0 && rr == 0) { ctx.fill(x, y + top, x + cut, y + bottom, color); return; }

        int midEnd = w - rr;
        if (Math.min(cut, midEnd) > rl) ctx.fill(x + rl, y + top, x + Math.min(cut, midEnd), y + bottom, color); // between the ends
        endColumns(ctx, x, y, h, rl, Math.min(rl, cut), top, bottom, color, tl, bl, CORNER_TL, CORNER_BL);
        if (cut > midEnd) endColumns(ctx, x + midEnd, y, h, rr, cut - midEnd, top, bottom, color, tr, br, CORNER_TR, CORNER_BR);
    }

    private static void endColumns(GuiGraphicsExtractor ctx, int x, int y, int h, int r, int visible, int top, int bottom,
                                   int color, boolean roundTop, boolean roundBottom, Identifier topTex, Identifier bottomTex) {
        if (r <= 0 || visible <= 0) return;
        int midTop = Math.max(top, r), midBottom = Math.min(bottom, h - r);
        if (midBottom > midTop) ctx.fill(x, y + midTop, x + visible, y + midBottom, color); // the straight side
        if (top < r) cornerRows(ctx, roundTop, topTex, x, y, r, visible, top, Math.min(bottom, r), color);
        if (bottom > h - r) {
            int from = Math.max(top, h - r);
            cornerRows(ctx, roundBottom, bottomTex, x, y + h - r, r, visible, from - (h - r), bottom - (h - r), color);
        }
    }

    private static void cornerRows(GuiGraphicsExtractor ctx, boolean rounded, Identifier tex, int x, int y, int r, int visible,
                                   int from, int to, int color) {
        if (to <= from || visible <= 0) return;
        if (!rounded) { ctx.fill(x, y + from, x + visible, y + to, color); return; }
        float perPixel = (float) CORNER_TEX / r;
        ctx.blit(RenderPipelines.GUI_TEXTURED, tex, x, y + from, 0f, from * perPixel,
                visible, to - from, Math.max(1, Math.round(visible * perPixel)), Math.max(1, Math.round((to - from) * perPixel)),
                CORNER_TEX, CORNER_TEX, color);
    }

    public static int[] listSize(Font font, AbilitySettings.Style s, List<Row> rows) {
        int w = 0, h = 0;
        for (Row r : rows) {
            w = Math.max(w, rowWidth(font, s, r));
            h += rowStep(font, s, r);
        }
        return new int[]{ w, Math.max(0, h - ROW_GAP) };
    }

    public static void drawList(GuiGraphicsExtractor ctx, Font font, AbilitySettings.Style s, List<Row> rows) {
        int y = 0;
        for (Row r : rows) {
            draw(ctx, font, s, r, 0, y);
            y += rowStep(font, s, r);
        }
    }

    public static int indexOf(AbilitySettings s, String id) {
        int i = 0;
        for (String k : s.selected) { if (k.equals(id)) return i; i++; }
        return 0;
    }

    // ── On screen ─────────────────────────────────────────────────────────────

    private static void render(GuiGraphicsExtractor ctx) {
        AbilitySettings s = PhantomConfig.abilities();
        if (!s.enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.font == null) return;
        List<Timer> running = AbilityCooldowns.running();
        if (running.isEmpty()) return;

        long now = System.currentTimeMillis();
        List<Row> rows = new ArrayList<>();
        for (Timer t : running) rows.add(rowOf(t, now));
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        var pose = ctx.pose();

        java.util.TreeMap<Integer, List<Row>> byGroup = new java.util.TreeMap<>();
        for (Row r : rows)
            for (int g : s.groupsOf(r.id()))
                if (g > 0 && s.style(g).enabled && IslandTracker.allowed(s, AbilitySettings.ruleKey(g, r.id()))) byGroup.computeIfAbsent(g, k -> new ArrayList<>()).add(r);
        for (var group : byGroup.entrySet()) {
            float[] p = s.listPos(group.getKey());
            pose.pushMatrix();
            pose.translate((int) (p[0] * sw), (int) (p[1] * sh));
            pose.scale(p[2], p[2]);
            drawList(ctx, mc.font, s.style(group.getKey()), group.getValue());
            pose.popMatrix();
        }
        for (Row r : rows) {
            if (!s.inGroup(r.id(), 0) || !s.style(0).enabled || !IslandTracker.allowed(s, r.id())) continue;
            float[] p = s.iconPos(r.id(), indexOf(s, r.id()));
            pose.pushMatrix();
            pose.translate((int) (p[0] * sw), (int) (p[1] * sh));
            pose.scale(p[2], p[2]);
            draw(ctx, mc.font, s.style(0), r, 0, 0);
            pose.popMatrix();
        }
    }
}
