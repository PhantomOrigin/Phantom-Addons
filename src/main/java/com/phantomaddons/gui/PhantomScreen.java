package com.phantomaddons.gui;

import com.phantomaddons.HudEditorScreen;
import com.phantomaddons.PhantomConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.phantomaddons.gui.GuiDraw.*;
import static com.phantomaddons.gui.Theme.*;

public class PhantomScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────

    private static final int SIDEBAR_W = 108;
    private static final int HEADER_H  = 34;
    private static final int BASE_PANEL_W = 400;
    private static final int BASE_PANEL_H = 280;
    private static int PANEL_W = BASE_PANEL_W;
    private static int PANEL_H = BASE_PANEL_H;
    private static final int ROW_GAP   = 3;
    private static final int PAD       = 12;
    private static final int INDENT    = 12;
    private static final int PANEL_RADIUS = 6;
    private static final int GROUP_PAD_B = 6;

    private static final Identifier LOGO =
            Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/logo.png");
    private static final Identifier LOGO_LIGHT =
            Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/logo_light.png");

    private static final int C_DIM_OVERLAY_ALPHA       = 0xBB; // ~73% opaque — transparency off
    private static final int C_DIM_OVERLAY_ALPHA_TRANS = 0x7A; // ~48% opaque — transparency on
    private static final int C_DIM_OVERLAY_RGB         = 0x06080C;

    public static final int MOUSE_OFFSET = 2000;

    private Tab   currentTab = Tab.ABOUT;
    private static final float GROUP_SPEED = 10f;

    // ── Tab switch animation ─────────────────────────────────────────────────
    private List<RenderRow> outgoingRows = null;
    private float tabOutT       = 1f;
    private float tabInT        = 1f;
    private float tabInElapsed  = 0f;
    private static final float TAB_OUT_SPEED          = 3f;
    private static final float TAB_IN_SPEED           = 0.2f;
    private static final float TAB_IN_STAGGER         = 1f; // extra delay per row index
    private static final int   TAB_IN_MAX_STAGGER_ROWS = 14;    // cap stagger growth for long tabs
    private static final int   TAB_OUT_DIST = 90;
    private static final int   TAB_IN_DIST  = 500;

    // ── Menu tree ─────────────────────────────────────────────────────────────

    private static class RenderRow {
        Node node; int x, y, w, h, contentH, depth;
    }

    private static final Map<String, Boolean> EXPANDED = new HashMap<>();
    private static final Map<String, Float>   ANIM     = new HashMap<>();
    private static final Map<String, Float>   GROUP_TOGGLE_ANIM = new HashMap<>();

    // ── Feature lists ─────────────────────────────────────────────────────────

    private final List<Node>    roots     = new ArrayList<>();
    private final List<Feature> allLeaves = new ArrayList<>();
    private final List<Group>   allGroups = new ArrayList<>();

    private Feature capturingFeature(boolean includeKeyCaptures) {
        for (Feature f : allLeaves)
            if (f.isCapturing() && (includeKeyCaptures || !(f instanceof KeyCapture))) return f;
        return null;
    }

    private void cancelAllInputs() {
        for (Feature f : allLeaves) f.cancel();
    }

    private void buildFeatures() {
        roots.clear();
        allLeaves.clear();
        allGroups.clear();
        filteredQuery = null;

        MenuBuilder m = new MenuBuilder(this, roots);
        for (Tab t : Tab.values()) {
            m.setTab(t);
            t.build(m);
        }
        collect(roots);
    }

    private void collect(List<Node> nodes) {
        for (Node n : nodes) {
            if (n instanceof Leaf l) allLeaves.add(l.f);
            else if (n instanceof Group g) { allGroups.add(g); collect(g.children); }
        }
    }

    // ── Hooks for the tab files ───────────────────────────────────────────────

    public void rebuild() { buildFeatures(); }

    public void openHudEditor() {
        Minecraft.getInstance().gui.setScreen(new HudEditorScreen(this));
    }

    public void onThemeChanged() {
        Theme.apply(PhantomConfig.getUiTheme());
        if (searchField != null) {
            searchField.setTextColor(C_TEXT);
            searchField.setTextShadow(darkMode);
        }
    }

    public void onGuiScaleChanged() {
        applyGuiScale(PhantomConfig.getUiGuiScale());
        resize(width, height);
    }

    public void disableAllFeatures() {
        for (Node n : roots) {
            if (n.tab == Tab.ABOUT) continue;
            if (n instanceof Leaf l && l.f instanceof Toggle t) {
                if (t.get.get()) t.set.accept(false);
            } else if (n instanceof Group g && g.get != null && g.set != null) {
                if (g.get.get()) g.set.accept(false);
            }
        }
        PhantomConfig.save();
    }

    Font font() { return font; }


    // ── State ─────────────────────────────────────────────────────────────────

    private EditBox searchField;
    private String          query       = "";
    private String          filteredQuery = null;
    private final List<Node> filtered   = new ArrayList<>();
    private int             scroll      = 0;
    private Feature         dragFeature = null;
    private int             dragX, dragY, dragW;

    private boolean draggingScroll = false;
    private int     sbThumbY, sbThumbH, sbTrackTop, sbTrackH, sbMaxScroll;

    private int tabScroll = 0;
    private final List<TabRow> tabRows = new ArrayList<>();

    private record TabRow(Tab tab, boolean isHeader, String label, int y, int h) {}

    // ── Open/close animation ─────────────────────────────────────────────────

    private static final float OPEN_ANIM_SPEED  = 3.0f; // ~1/3s to fully open
    private static final float CLOSE_ANIM_SPEED = 4.0f; // slightly snappier close

    private float   openAnim = 0f;
    private boolean closing  = false;
    float frameDelta = 0f;

    private float openProgress() {
        float t = openAnim;
        return t * t * (3f - 2f * t); // smoothstep
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    public PhantomScreen() {
        super(Component.literal("PhantomAddons"));
        Theme.apply(PhantomConfig.getUiTheme());
        applyGuiScale(PhantomConfig.getUiGuiScale());
        buildFeatures();
    }

    private static void applyGuiScale(float scale) {
        PANEL_W = Math.round(BASE_PANEL_W * scale);
        PANEL_H = Math.round(BASE_PANEL_H * scale);
    }

    @Override
    public void onClose() {
        if (!closing) {
            closing = true;
            return;
        }
        PhantomConfig.saveNow();
        super.onClose();
    }

    // ── Geometry ──────────────────────────────────────────────────────────────

    private int px() { return width  / 2 - PANEL_W / 2; }
    private int py() { return height / 2 - PANEL_H / 2; }
    private int cx() { return px() + SIDEBAR_W; }
    private int cy() { return py() + HEADER_H; }
    private int cw() { return PANEL_W - SIDEBAR_W; }
    private int ch() { return PANEL_H - HEADER_H; }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        int sfX = px() + PANEL_W - 148;
        int sfY = py() + (HEADER_H - 14) / 2;
        searchField = new EditBox(font, sfX, sfY, 138, 14, Component.empty());
        searchField.setMaxLength(64);
        searchField.setBordered(false);
        searchField.setTextColor(C_TEXT);
        searchField.setTextShadow(darkMode);
        searchField.setResponder(s -> { query = s.toLowerCase(); scroll = 0; });
        addRenderableWidget(searchField);
    }

    // ── Anim state ──────────────────────────────────────────────────────────────

    private boolean isExpanded(Group g) { return EXPANDED.getOrDefault(g.key, false); }
    private float   animOf(Group g)      { return ANIM.getOrDefault(g.key, 0f); }
    private float   catAnimOf(String category) { return ANIM.getOrDefault("cat:" + category, 0f); }

    private static final List<String> CATEGORY_KEYS = new ArrayList<>();
    static {
        for (Tab t : Tab.values())
            if (t.category != null && !CATEGORY_KEYS.contains("cat:" + t.category)) CATEGORY_KEYS.add("cat:" + t.category);
    }

    private static void stepExpandAnim(String key, float step) {
        float cur = ANIM.getOrDefault(key, 0f);
        float tgt = EXPANDED.getOrDefault(key, false) ? 1f : 0f;
        if (cur == tgt) return;
        ANIM.put(key, cur < tgt ? Math.min(tgt, cur + step) : Math.max(tgt, cur - step));
    }

    private void stepAnims(float delta) {
        float step = delta * GROUP_SPEED * 0.05f; // delta is ~ticks; tune
        for (Group g : allGroups) {
            stepExpandAnim(g.key, step);

            if (g.get != null) {
                float tTgt = g.get.get() ? 1f : 0f;
                float tCur = GROUP_TOGGLE_ANIM.getOrDefault(g.key, tTgt);
                if (tCur != tTgt) {
                    tCur += (tTgt - tCur) * Math.min(1f, delta * TOGGLE_ANIM_SPEED);
                    GROUP_TOGGLE_ANIM.put(g.key, tCur);
                }
            }
        }

        for (String key : CATEGORY_KEYS) stepExpandAnim(key, step);
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private List<Node> filteredRoots() {
        if (!query.equals(filteredQuery)) {
            filtered.clear();
            for (Node n : roots) {
                Node f = filterNode(n, query);
                if (f != null) filtered.add(f);
            }
            filteredQuery = query;
        }
        return filtered;
    }

    private List<RenderRow> layoutRows() {
        if (!query.isEmpty()) {
            List<RenderRow> out = new ArrayList<>();
            int baseX = cx() + PAD;
            int baseW = cw() - PAD * 2;
            int[] y = { cy() + PAD - scroll };
            for (Node f : filteredRoots()) layoutNode(f, 0, baseX, baseW, 1f, y, out);
            return out;
        }
        return layoutRowsForTab(currentTab);
    }

    private List<RenderRow> layoutRowsForTab(Tab tab) {
        List<RenderRow> out = new ArrayList<>();
        int baseX = cx() + PAD;
        int baseW = cw() - PAD * 2;
        int[] y = { cy() + PAD - scroll };
        for (Node n : roots) {
            if (n.tab != tab) continue;
            layoutNode(n, 0, baseX, baseW, 1f, y, out);
        }
        return out;
    }

    private Node filterNode(Node n, String q) {
        if (n instanceof Leaf lf) {
            return lf.f.searchName.contains(q) ? lf : null;
        }
        Group g = (Group) n;
        if (g.searchName.contains(q)) return g; // title match → whole dropdown
        Group copy = null;
        for (Node c : g.children) {
            Node fc = filterNode(c, q);
            if (fc != null) {
                if (copy == null) copy = new Group(g.name, g.tab, g.key, g.get, g.set).withTooltip(g.tooltip);
                copy.children.add(fc);
            }
        }
        return copy;
    }

    private void layoutNode(Node n, int depth, int baseX, int baseW,
                            float ancestorAnim, int[] y, List<RenderRow> out) {
        if (n instanceof Leaf lf && !lf.f.visible.get()) {
            RenderRow r = new RenderRow();
            r.node = n; r.depth = depth; r.contentH = 0; r.h = 0;
            r.x = baseX + depth * INDENT;
            r.w = baseW - depth * INDENT;
            r.y = y[0];
            out.add(r);
            return;
        }

        int naturalH = ROW_H;
        int slotH = Math.round(naturalH * ancestorAnim);
        int gap   = Math.round(ROW_GAP * ancestorAnim);

        RenderRow r = new RenderRow();
        r.node = n; r.depth = depth; r.contentH = naturalH; r.h = slotH;
        r.x = baseX + depth * INDENT;
        r.w = baseW - depth * INDENT;
        r.y = y[0];
        out.add(r);
        y[0] += slotH + gap;

        if (n instanceof Group g) {
            float childAnim = ancestorAnim * animOf(g);
            if (childAnim > 0.002f) {
                y[0] += Math.round(GROUP_PAD_B * childAnim);
                for (Node c : g.children)
                    layoutNode(c, depth + 1, baseX, baseW, childAnim, y, out);
                y[0] += Math.round((GROUP_PAD_B + ROW_GAP) * childAnim);
            }
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isDoubleClick) {
        for (Feature f : allLeaves) {
            if (f instanceof KeyCapture kc && kc.capturing) { kc.onMouseButton(click.button()); return true; }
        }

        if (super.mouseClicked(click, isDoubleClick)) return true;
        if (searchField != null) searchField.setFocused(false);
        this.setFocused(null);
        if (click.button() != 0) return false;

        double mx = click.x(), my = click.y();

        if (sbMaxScroll > 0) {
            int sbX = cx() + cw() - 5;
            if (mx >= sbX - 1 && mx <= sbX + 4 && my >= sbTrackTop && my <= sbTrackTop + sbTrackH) {
                if (my >= sbThumbY && my <= sbThumbY + sbThumbH) {
                    draggingScroll = true;
                } else {
                    setScrollFromThumb(my - sbThumbH / 2.0);
                    draggingScroll = true;
                }
                return true;
            }
        }

        int startY = py() + HEADER_H + 6;
        for (TabRow r : tabRows) {
            int ty = startY + r.y() - tabScroll;
            if (ty < startY || ty + r.h() > py() + PANEL_H) continue;
            if (!(mx >= px() && mx <= px() + SIDEBAR_W - 1 && my >= ty && my <= ty + r.h())) continue;

            if (r.isHeader()) {
                ANIM.putIfAbsent("cat:" + r.label(), isCategoryExpanded(r.label()) ? 1f : 0f);
                EXPANDED.put("cat:" + r.label(), !isCategoryExpanded(r.label()));
                return true;
            }
            if (r.h() < TAB_ROW_H - 1) continue; // skip rows still animating in/out

            if (r.tab() != currentTab) {
                cancelAllInputs();
                outgoingRows = layoutRowsForTab(currentTab);
                tabOutT = 0f;
                currentTab = r.tab();
                tabInT = 0f;
                tabInElapsed = 0f;
                scroll = 0;
            }
            return true;
        }

        cancelAllInputs();

        if (tabOutT >= 1f && mx >= cx() && mx <= cx() + cw() && my >= cy() && my <= cy() + ch()) {
            for (RenderRow r : layoutRows()) {
                if (r.h < r.contentH - 1) continue; // skip rows mid-animation
                if (my < r.y || my > r.y + r.contentH) continue;
                if (mx < r.x || mx > r.x + r.w) continue;

                if (r.node instanceof Group g) {
                    if (groupHeaderDown(g, mx, my, r.x, r.y, r.w)) return true;
                    return true;
                } else if (r.node instanceof Leaf lf) {
                    if (lf.f.onDown(mx, my, r.x, r.y, r.w)) {
                        dragFeature = lf.f;
                        dragX = r.x; dragY = r.y; dragW = r.w;
                        return true;
                    }
                    return false;
                }
            }
        }
        return false;
    }

    private boolean groupHeaderDown(Group g, double mx, double my, int x, int y, int w) {
        if (g.get != null && g.set != null) {
            if (switchHit(mx, my, x, y, w)) {
                GROUP_TOGGLE_ANIM.putIfAbsent(g.key, g.get.get() ? 1f : 0f);
                g.set.accept(!g.get.get());
                return true;
            }
        }
        EXPANDED.put(g.key, !isExpanded(g));
        return true;
    }

    private void setScrollFromThumb(double thumbTop) {
        if (sbTrackH - sbThumbH <= 0) { scroll = 0; return; }
        double frac = (thumbTop - sbTrackTop) / (sbTrackH - sbThumbH);
        scroll = Mth.clamp((int) Math.round(frac * sbMaxScroll), 0, sbMaxScroll);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (draggingScroll) {
            setScrollFromThumb(click.y() - sbThumbH / 2.0);
            return true;
        }
        if (dragFeature != null) { dragFeature.onDrag(click.x(), click.y(), dragX, dragY, dragW); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        draggingScroll = false;
        if (dragFeature != null) { dragFeature.onUp(); dragFeature = null; }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        if (mx >= px() && mx <= px() + SIDEBAR_W && my >= py() + HEADER_H && my <= py() + PANEL_H) {
            tabScroll -= (int)(vScroll * 12); return true;
        }
        if (mx >= cx() && mx <= cx() + cw() && my >= cy() && my <= cy() + ch()) {
            scroll -= (int)(vScroll * 12); return true;
        }
        return super.mouseScrolled(mx, my, hScroll, vScroll);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = event.key();

        Feature capturing = capturingFeature(true);
        if (capturing != null) { capturing.onKey(key); return true; }
        if (key == 256 && searchField != null && searchField.isFocused()) {
            searchField.setFocused(false);
            this.setFocused(null);
            return true;
        }

        if (key == 256) { this.onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent input) {
        Feature capturing = capturingFeature(false);
        if (capturing != null) { capturing.onChar((char) input.codepoint()); return true; }
        if (searchField != null && searchField.isFocused()) return super.charTyped(input);
        return false;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        int baseAlpha = PhantomConfig.isUiTransparencyEnabled() ? C_DIM_OVERLAY_ALPHA_TRANS : C_DIM_OVERLAY_ALPHA;
        int overlayA = (int) (baseAlpha * openProgress()) << 24;
        ctx.fill(0, 0, width, height, C_DIM_OVERLAY_RGB | overlayA);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        this.frameDelta = delta;
        extractBackground(ctx, mx, my, delta);

        if (closing) {
            openAnim = Math.max(0f, openAnim - delta * CLOSE_ANIM_SPEED);
            if (openAnim <= 0f) { super.onClose(); return; }
        } else if (openAnim < 1f) {
            openAnim = Math.min(1f, openAnim + delta * OPEN_ANIM_SPEED);
        }

        if (tabOutT < 1f) {
            tabOutT = Math.min(1f, tabOutT + delta * TAB_OUT_SPEED);
            if (tabOutT >= 1f) outgoingRows = null;
        } else if (tabInT < 1f) {
            tabInElapsed += delta;
            float maxNeeded = TAB_IN_MAX_STAGGER_ROWS * TAB_IN_STAGGER + 1f / TAB_IN_SPEED;
            tabInT = Math.min(1f, tabInElapsed / maxNeeded);
        }
        stepAnims(delta);

        int px = px(), py = py();
        float prog  = openProgress();
        float scale = 0.82f + 0.18f * prog;
        float pcx = px + PANEL_W / 2f, pcy = py + PANEL_H / 2f;

        var m = ctx.pose();
        m.pushMatrix();
        m.translate(pcx, pcy);
        m.scale(scale, scale);
        m.translate(-pcx, -pcy);

        int outlineColor = PhantomConfig.isUiTransparencyEnabled() ? withAlpha(C_BORDER, CONTENT_ALPHA_TRANS) : C_BORDER;
        roundedFill(ctx, px - 1, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, outlineColor, PANEL_RADIUS + 1);
        boolean transparentBg = PhantomConfig.isUiTransparencyEnabled();
        int bgShade = transparentBg ? 26 : 10;
        double gradCenterX = px + SIDEBAR_W + 24;
        double gradCenterY = py + HEADER_H + 20;
        double gradRadius  = Math.hypot(PANEL_W, PANEL_H) * 0.85;
        roundedFillRadial(ctx, px, py, px + PANEL_W, py + PANEL_H,
                gradCenterX, gradCenterY, gradRadius,
                shade(C_BG, bgShade), shade(C_BG, -bgShade), PANEL_RADIUS, true, true, true, true);

        ctx.enableScissor(px, py, px + PANEL_W, py + PANEL_H);
        roundedFill(ctx, px, py, px + PANEL_W, py + HEADER_H, C_HEADER, PANEL_RADIUS, true, true, false, false);
        ctx.fill(px, py + HEADER_H - 1, px + PANEL_W, py + HEADER_H, C_ACCENT);
        int logoS = HEADER_H - 8, logoX = px + 6, logoY = py + 4;
        ctx.blit(darkMode ? LOGO : LOGO_LIGHT, logoX, logoY, logoX + logoS, logoY + logoS, 0f, 1f, 0f, 1f);
        drawText(ctx, font, Component.literal("PhantomAddons"),
                logoX + logoS + 6, py + (HEADER_H - font.lineHeight) / 2, C_ACCENT);

        int sfX = px + PANEL_W - 148, sfY = py + (HEADER_H - 16) / 2;
        roundedFill(ctx, sfX - 3, sfY - 1, sfX + 141, sfY + 15, darkMode ? 0xFF090B0E : 0xFFFFFBEF, CONTROL_RADIUS);
        if (searchField != null && query.isEmpty() && !searchField.isFocused()) {
            drawText(ctx, font, Component.literal("Search..."), sfX, sfY + (16 - font.lineHeight) / 2, C_TEXT_DIM, false);
        }

        roundedFill(ctx, px, py + HEADER_H, px + SIDEBAR_W, py + PANEL_H, C_SIDEBAR, PANEL_RADIUS, false, false, true, false);
        ctx.fill(px + SIDEBAR_W - 1, py + HEADER_H, px + SIDEBAR_W, py + PANEL_H, C_BORDER);
        renderTabs(ctx, px, py, mx, my);
        renderContent(ctx, mx, my);
        ctx.disableScissor();

        m.popMatrix();
        super.extractRenderState(ctx, mx, my, delta);
    }

    private static final int TAB_ROW_H    = 22;
    private static final int TAB_HEADER_H = 16;
    private static final int TAB_GAP      = 2;

    private static boolean isCategoryExpanded(String category) {
        return EXPANDED.getOrDefault("cat:" + category, false);
    }

    private void layoutTabRows() {
        tabRows.clear();
        int y = 0;
        String lastCategory = null;
        for (Tab t : Tab.values()) {
            if (t.category != null && !t.category.equals(lastCategory)) {
                tabRows.add(new TabRow(null, true, t.category, y, TAB_HEADER_H));
                y += TAB_HEADER_H + TAB_GAP;
            }
            lastCategory = t.category;
            if (t.category != null) {
                float catAnim = catAnimOf(t.category);
                if (catAnim <= 0.002f) continue;
                int h   = Math.round(TAB_ROW_H * catAnim);
                int gap = Math.round(TAB_GAP * catAnim);
                tabRows.add(new TabRow(t, false, t.label, y, h));
                y += h + gap;
                continue;
            }
            tabRows.add(new TabRow(t, false, t.label, y, TAB_ROW_H));
            y += TAB_ROW_H + TAB_GAP;
        }
    }

    private int tabListVisibleH() { return PANEL_H - HEADER_H - 6 - 4; }

    private void renderTabs(GuiGraphicsExtractor ctx, int px, int py, int mx, int my) {
        layoutTabRows();
        int startY = py + HEADER_H + 6;

        int totalH = tabRows.isEmpty() ? 0 : tabRows.get(tabRows.size() - 1).y() + tabRows.get(tabRows.size() - 1).h();
        int maxScroll = Math.max(0, totalH - tabListVisibleH());
        tabScroll = Mth.clamp(tabScroll, 0, maxScroll);

        ctx.enableScissor(px, startY, px + SIDEBAR_W, py + PANEL_H);
        for (TabRow r : tabRows) {
            int tx = px, ty = startY + r.y() - tabScroll;
            if (ty + r.h() < startY || ty > py + PANEL_H) continue;

            if (r.isHeader()) {
                boolean expanded = isCategoryExpanded(r.label());
                boolean headerHover = mx >= tx && mx <= tx + SIDEBAR_W - 1 && my >= ty && my <= ty + r.h()
                        && my >= startY && my <= py + PANEL_H;
                String arrow = expanded ? "▼" : "▶";
                drawText(ctx, font, Component.literal(arrow),
                        tx + 10, ty + (r.h() - font.lineHeight) / 2, headerHover ? C_TEXT : C_TEXT_DIM);
                drawText(ctx, font, Component.literal(r.label()),
                        tx + 20, ty + (r.h() - font.lineHeight) / 2, headerHover ? C_TEXT : C_TEXT_DIM);
                continue;
            }

            boolean active = r.tab() == currentTab && query.isEmpty();
            boolean hover  = !active && mx >= tx && mx <= tx + SIDEBAR_W - 1 && my >= ty && my <= ty + r.h()
                    && my >= startY && my <= py + PANEL_H;
            if (active) {
                ctx.fill(tx, ty, tx + SIDEBAR_W - 1, ty + r.h(), C_TAB_ACTIVE);
                ctx.fill(tx, ty, tx + 2, ty + r.h(), C_ACCENT);
            } else if (hover) {
                ctx.fill(tx, ty, tx + SIDEBAR_W - 1, ty + r.h(), C_TAB_HOVER);
            }
            drawText(ctx, font, Component.literal(r.label()),
                    tx + 14, ty + (r.h() - font.lineHeight) / 2,
                    active ? C_ACCENT : hover ? C_TEXT : C_TEXT_DIM);
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            int barX = px + SIDEBAR_W - 3;
            int barH = Math.max(16, (int) ((float) tabListVisibleH() / totalH * (py + PANEL_H - startY)));
            int barY = startY + (int) ((float) tabScroll / maxScroll * (py + PANEL_H - startY - barH));
            ctx.fill(barX, startY, barX + 2, py + PANEL_H, overlay(0x22));
            ctx.fill(barX, barY, barX + 2, barY + barH, overlay(0x88));
        }
    }

    private void renderContent(GuiGraphicsExtractor ctx, int mx, int my) {
        int cx = cx(), cy = cy(), cw = cw(), ch = ch();

        if (outgoingRows != null && tabOutT < 1f) {
            renderOutgoingRows(ctx, cx, cy, cw, ch);
            sbMaxScroll = 0;
            return;
        }

        List<RenderRow> rows = layoutRows();

        if (rows.isEmpty()) {
            drawCenteredText(ctx, font,
                    Component.literal(query.isEmpty() ? "No features" : "No results"),
                    cx + cw / 2, cy + ch / 2, C_TEXT_DIM);
            sbMaxScroll = 0;
            return;
        }

        int totalH = 0;
        for (RenderRow r : rows) totalH = Math.max(totalH, (r.y + scroll) + r.h);
        totalH -= (cy + PAD);
        int maxScroll = Math.max(0, totalH - ch + PAD * 2);
        int clamped = Mth.clamp(scroll, 0, maxScroll);
        if (clamped != scroll) {
            int shift = scroll - clamped;
            for (RenderRow r : rows) r.y += shift;
            scroll = clamped;
        }

        boolean flyingIn = tabInT < 1f;

        ctx.enableScissor(cx, cy, cx + cw, cy + ch);

        for (int i = 0; i < rows.size(); i++) {
            RenderRow r = rows.get(i);
            if (!(r.node instanceof Group g)) continue;
            if (animOf(g) <= 0.002f) continue;
            int blockTop = r.y;
            int blockBot = r.y + r.h;
            for (int j = i + 1; j < rows.size(); j++) {
                if (rows.get(j).depth <= r.depth) break;
                blockBot = rows.get(j).y + rows.get(j).h;
            }
            int bx = r.x, bRight = cx + cw - PAD;
            int bb = blockBot + Math.round((GROUP_PAD_B + ROW_GAP) * animOf(g));
            ctx.fill(bx, blockTop, bRight, bb, C_GROUP_BG);
            ctx.fill(bx, blockTop, bx + 2, bb, C_GROUP_BAR);   // left bar (amber)
            ctx.fill(bx, bb - 1, bRight, bb, overlay(0x22));   // subtle bottom border
        }

        String hoverTooltip = null;

        for (int i = 0; i < rows.size(); i++) {
            RenderRow r = rows.get(i);
            if (r.h <= 0) continue;
            int top = Math.max(cy, r.y);
            int bot = Math.min(cy + ch, r.y + r.h);
            if (bot <= top) continue;

            ctx.enableScissor(cx, top, cx + cw, bot);

            int rowOffsetX = 0;
            int rowFadeA   = 0;
            if (flyingIn) {
                int   stagger  = Math.min(i, TAB_IN_MAX_STAGGER_ROWS);
                float rowT     = Mth.clamp((tabInElapsed - stagger * TAB_IN_STAGGER) * TAB_IN_SPEED, 0f, 1f);
                float rowEase  = rowT * rowT * (3f - 2f * rowT);
                rowOffsetX = Math.round((1f - rowEase) * -TAB_IN_DIST);
                rowFadeA   = (int) ((1f - rowEase) * 235) << 24;
            }

            int rx = r.x + rowOffsetX;
            int rw = r.w;

            boolean rowHovered = !flyingIn && mx >= rx && mx < cx + cw - PAD && my >= top && my < bot;

            if (r.node instanceof Group g) {
                renderGroupHeader(ctx, g, rx, r.y, rw, mx, my);
                if (rowHovered && g.tooltip != null) hoverTooltip = g.tooltip;
            } else if (r.node instanceof Leaf lf) {
                ctx.fill(rx, r.y, cx + cw - PAD, r.y + r.contentH, overlay(0x0A));
                lf.f.render(ctx, this, rx, r.y, rw, mx, my);
                if (rowHovered && lf.f.tooltip != null) hoverTooltip = lf.f.tooltip;
            }

            if (rowFadeA != 0) {
                ctx.fill(cx, top, cx + cw, bot, (C_BG & 0xFFFFFF) | rowFadeA);
            }

            ctx.disableScissor();
        }
        ctx.disableScissor();

        if (hoverTooltip != null) {
            drawTooltip(ctx, hoverTooltip, mx, my);
        }

        sbMaxScroll = maxScroll;
        if (maxScroll > 0) {
            int sbX = cx + cw - 5;
            sbTrackTop = cy + PAD;
            sbTrackH   = ch - PAD * 2;
            sbThumbH   = Math.max(18, (int)((long) sbTrackH * sbTrackH / (totalH + PAD * 2)));
            sbThumbY   = sbTrackTop + (int)((float) scroll / maxScroll * (sbTrackH - sbThumbH));
            boolean hov = mx >= sbX - 1 && mx <= sbX + 4 && my >= sbTrackTop && my <= sbTrackTop + sbTrackH;
            ctx.fill(sbX, sbTrackTop, sbX + 3, sbTrackTop + sbTrackH, overlay(0x1A));
            ctx.fill(sbX, sbThumbY, sbX + 3, sbThumbY + sbThumbH,
                    (draggingScroll || hov) ? 0xCCFFAA00 : 0x77AABBDD);
        }
    }

    private void renderOutgoingRows(GuiGraphicsExtractor ctx, int cx, int cy, int cw, int ch) {
        float ease     = tabOutT * tabOutT * (3f - 2f * tabOutT);
        int   offsetX  = Math.round(ease * TAB_OUT_DIST);
        int   fadeA    = (int) (ease * 235) << 24;

        ctx.enableScissor(cx, cy, cx + cw, cy + ch);
        for (RenderRow r : outgoingRows) {
            if (r.h <= 0) continue;
            int top = Math.max(cy, r.y);
            int bot = Math.min(cy + ch, r.y + r.h);
            if (bot <= top) continue;

            ctx.enableScissor(cx, top, cx + cw, bot);
            int rx = r.x + offsetX;
            if (r.node instanceof Group g) {
                renderGroupHeader(ctx, g, rx, r.y, r.w, -1, -1);
            } else if (r.node instanceof Leaf lf) {
                lf.f.render(ctx, this, rx, r.y, r.w, -1, -1);
            }
            ctx.fill(cx, top, cx + cw, bot, (C_BG & 0xFFFFFF) | fadeA);
            ctx.disableScissor();
        }
        ctx.disableScissor();
    }

    private static final int TOOLTIP_MAX_W = 200;

    private void drawTooltip(GuiGraphicsExtractor ctx, String text, int mx, int my) {
        List<String> lines = wrapTooltip(text, TOOLTIP_MAX_W);
        int lineH = font.lineHeight + 2;
        int tw = 0;
        for (String l : lines) tw = Math.max(tw, font.width(l));

        int boxW = tw + 8;
        int boxH = lines.size() * lineH + 4;
        int tx = mx < px() + PANEL_W / 2 ? mx + 12 : mx - boxW - 12;
        tx = Mth.clamp(tx, px() + 4, px() + PANEL_W - boxW - 4);
        int ty = my - boxH - 6;
        if (ty < 0) ty = my + 16;

        roundedFill(ctx, tx - 4, ty - 3, tx + boxW + 4, ty + boxH + 3,
                darkMode ? 0xEC0A0A0E : 0xF0FFFBEF, 4);
        ctx.fill(tx - 4, ty - 3, tx + boxW + 4, ty - 2, C_GROUP_BAR);

        int ly = ty;
        for (String l : lines) {
            drawText(ctx, font, Component.literal(l), tx, ly, C_TEXT, false);
            ly += lineH;
        }
    }

    private String       wrappedTooltipText;
    private List<String> wrappedTooltipLines;

    private List<String> wrapTooltip(String text, int maxWidth) {
        if (!text.equals(wrappedTooltipText)) {
            wrappedTooltipLines = wrapWords(text, maxWidth);
            wrappedTooltipText  = text;
        }
        return wrappedTooltipLines;
    }

    private List<String> wrapWords(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = cur.isEmpty() ? word : cur + " " + word;
            if (font.width(test) > maxWidth && !cur.isEmpty()) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(test);
            }
        }
        if (!cur.isEmpty()) lines.add(cur.toString());
        return lines;
    }

    private void renderGroupHeader(GuiGraphicsExtractor ctx, Group g,
                                   int x, int y, int w, int mx, int my) {
        boolean expanded = isExpanded(g);
        float a = animOf(g);
        boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + ROW_H;

        ctx.fill(x, y, x + w, y + ROW_H, hov ? overlay(0x14) : overlay(0x0C));
        ctx.fill(x, y, x + w, y + 1, overlay(0x22));

        String arrow = a > 0.5f ? "▾" : "▸";
        drawText(ctx, font, Component.literal(arrow),
                x + 6, y + (ROW_H - font.lineHeight) / 2, expanded ? C_ACCENT : C_TEXT_DIM);

        drawText(ctx, font, g.nameLabel,
                x + 18, y + (ROW_H - font.lineHeight) / 2,
                expanded ? C_ACCENT : C_TEXT);

        if (g.get != null && g.set != null) {
            Float knobAnim = GROUP_TOGGLE_ANIM.get(g.key);
            drawSwitch(ctx, x, y, w, knobAnim != null ? knobAnim : g.get.get() ? 1f : 0f);
        }
    }

    @Override public void removed()          { PhantomConfig.save(); }
    @Override public boolean isPauseScreen() { return false; }
}
