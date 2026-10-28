package com.phantomaddons.gui;

import com.mojang.blaze3d.platform.InputConstants;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private static final int ROW_GAP   = 1;
    private static final int PAD       = 12;
    private static final int INDENT    = 12;
    private static final int PANEL_RADIUS = 6;
    private static final int GROUP_PAD_B = 2;
    private static final int HEADING_H = 22;
    private static final int SEARCH_W = 144, SEARCH_H = 17, SEARCH_PAD = 5, SEARCH_RIGHT = 7;
    private static final int BAR_H = 28;
    private static final String[] BAR_LABELS = { "HUD Layout Editor", "Discord", "GitHub" };

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
        startTabIn(currentTab);
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
        int sbx = px() + PANEL_W - SEARCH_RIGHT - SEARCH_W;
        int sby = py() + (HEADER_H - SEARCH_H) / 2;
        final int hitX0 = sbx, hitY0 = sby, hitX1 = sbx + SEARCH_W, hitY1 = sby + SEARCH_H;
        searchField = new EditBox(font, sbx + SEARCH_PAD, sby + SEARCH_PAD,
                SEARCH_W - 2 * SEARCH_PAD, 9, Component.empty()) {
            @Override
            public boolean isMouseOver(double mouseX, double mouseY) {
                return mouseX >= hitX0 && mouseX < hitX1 && mouseY >= hitY0 && mouseY < hitY1;
            }
        };
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
        if (tabInT >= 1f && tabOutT >= 1f) flyingGroups.clear();
        for (Group g : allGroups) {
            if (!flyingGroups.contains(g.key)) stepExpandAnim(g.key, step);

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

    private final Set<String> flyingGroups = new HashSet<>();

    private void startTabIn(Tab t) {
        tabInT = 0f;
        tabInElapsed = 0f;
        flyingGroups.clear();
        for (Group g : allGroups) if (g.tab == t) ANIM.put(g.key, 0f);
        for (Node n : roots) if (n.tab == t && n instanceof Group g) flyingGroups.add(g.key);
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
            int[] y = { cy() + topPad(null) - scroll };
            for (Node f : filteredRoots()) layoutNode(f, 0, baseX, baseW, 1f, y, out);
            return out;
        }
        return layoutRowsForTab(currentTab);
    }

    private int topPad(Tab tab) {
        if (tab == null || !query.isEmpty()) return 5;
        for (Node n : roots) {
            if (n.tab != tab) continue;
            return n instanceof Heading ? 4 : 5;
        }
        return 5;
    }

    private List<RenderRow> layoutRowsForTab(Tab tab) {
        List<RenderRow> out = new ArrayList<>();
        int baseX = cx() + PAD;
        int baseW = cw() - PAD * 2;
        int[] y = { cy() + topPad(tab) - scroll };
        for (Node n : roots) {
            if (n.tab != tab) continue;
            layoutNode(n, 0, baseX, baseW, 1f, y, out);
        }
        return out;
    }

    private Node filterNode(Node n, String q) {
        if (n instanceof Heading) return null;
        if (n instanceof Leaf lf) {
            return lf.f.searchName.contains(q) ? lf : null;
        }
        Group g = (Group) n;
        if (g.searchName.contains(q)) return g; // title match → whole dropdown
        Group copy = null;
        for (Node c : g.children) {
            Node fc = filterNode(c, q);
            if (fc != null) {
                if (copy == null) {
                    copy = new Group(g.name, g.tab, g.key, g.get, g.set).withTooltip(g.tooltip);
                    copy.warnWhen = g.warnWhen;
                    copy.warnText = g.warnText;
                }
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

        int naturalH = n instanceof Heading ? HEADING_H : ROW_H;
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
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;

        double mx = click.x(), my = click.y();

        if (mx >= px() && mx < px() + SIDEBAR_W && my >= barTop() && my < py() + PANEL_H) {
            int[][] rects = barButtonRects();
            for (int i = 0; i < rects.length; i++) {
                int[] r = rects[i];
                if (mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3]) { barAction(i); break; }
            }
            return true;
        }

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
            if (ty < startY || ty + r.h() > barTop()) continue;
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
                startTabIn(currentTab);
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
        if (key == InputConstants.KEY_ESCAPE && searchField != null && searchField.isFocused()) {
            searchField.setFocused(false);
            this.setFocused(null);
            return true;
        }

        if (key == InputConstants.KEY_ESCAPE) { this.onClose(); return true; }
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
        updateTextInput();
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
                logoX + logoS + 6, py + (HEADER_H - font.lineHeight) / 2 + 1, C_ACCENT);

        int sbx = px + PANEL_W - SEARCH_RIGHT - SEARCH_W, sby = py + (HEADER_H - SEARCH_H) / 2;
        insetFill(ctx, sbx, sby, sbx + SEARCH_W, sby + SEARCH_H, darkMode ? 0xFF0F1218 : 0xFFFFFBEF, CONTROL_RADIUS);
        if (searchField != null && query.isEmpty() && !searchField.isFocused()) {
            drawText(ctx, font, Component.literal("Search..."), sbx + SEARCH_PAD, sby + SEARCH_PAD, C_TEXT_DIM, false);
        }

        roundedFill(ctx, px, py + HEADER_H, px + SIDEBAR_W, py + PANEL_H, C_SIDEBAR, PANEL_RADIUS, false, false, true, false);
        ctx.fill(px + SIDEBAR_W - 1, py + HEADER_H, px + SIDEBAR_W, py + PANEL_H, C_BORDER);
        renderTabs(ctx, px, py, mx, my);
        renderSidebarBar(ctx, px, py, mx, my);
        renderContent(ctx, mx, my);
        if (barHoverLabel != null) drawTooltip(ctx, barHoverLabel, mx, my);
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

    private static boolean hasVisibleContent(Node n) {
        if (n instanceof Leaf lf) return lf.f.visible.get();
        if (n instanceof Group g) {
            if (g.get != null) return true;
            for (Node c : g.children) if (hasVisibleContent(c)) return true;
        }
        return false;
    }

    private boolean tabHasContent(Tab t) {
        if (t == Tab.ABOUT) return true;
        for (Node n : roots) if (n.tab == t && hasVisibleContent(n)) return true;
        return false;
    }

    private void layoutTabRows() {
        tabRows.clear();
        if (!tabHasContent(currentTab)) {
            currentTab = Tab.ABOUT;
            scroll = 0;
        }
        int y = 0;
        String lastCategory = null;
        for (Tab t : Tab.values()) {
            if (!tabHasContent(t)) continue;
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

    private int tabListVisibleH() { return PANEL_H - HEADER_H - 6 - 4 - BAR_H; }

    private int barTop() { return py() + PANEL_H - BAR_H; }

    private int[][] barButtonRects() {
        int x0 = px() + 6, x1 = px() + SIDEBAR_W - 7;
        int gap = 4, h = 18, y0 = barTop() + 5;
        int w = (x1 - x0 - gap * 2) / 3;
        return new int[][] {
            { x0, y0, x0 + w, y0 + h },
            { x0 + w + gap, y0, x0 + 2 * w + gap, y0 + h },
            { x1 - w, y0, x1, y0 + h }
        };
    }

    private void barAction(int index) {
        switch (index) {
            case 0 -> openHudEditor();
            case 1 -> Links.open(Links.DISCORD);
            case 2 -> Links.open(Links.GITHUB);
            default -> {}
        }
    }

    private static final Identifier[] BAR_ICONS = {
        Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/icon_hud.png"),
        Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/icon_discord.png"),
        Identifier.fromNamespaceAndPath("phantomaddons", "textures/gui/icon_github.png")
    };
    private static final int BAR_ICON_SIZE = 12;
    private String barHoverLabel;

    private void renderSidebarBar(GuiGraphicsExtractor ctx, int px, int py, int mx, int my) {
        int top = barTop();
        roundedFill(ctx, px, top, px + SIDEBAR_W, py + PANEL_H, C_HEADER, PANEL_RADIUS, false, false, true, false);
        ctx.fill(px, top, px + SIDEBAR_W - 1, top + 1, C_BORDER);
        ctx.fill(px + SIDEBAR_W - 1, top, px + SIDEBAR_W, py + PANEL_H, C_BORDER);
        barHoverLabel = null;
        int[][] rects = barButtonRects();
        for (int i = 0; i < rects.length; i++) {
            int[] r = rects[i];
            boolean hov = mx >= r[0] && mx < r[2] && my >= r[1] && my < r[3];
            raisedFill(ctx, r[0], r[1], r[2], r[3], hov ? C_FIELD_HOVER : C_FIELD_BG, CONTROL_RADIUS);
            int ix = (r[0] + r[2] - BAR_ICON_SIZE) / 2, iy = (r[1] + r[3] - BAR_ICON_SIZE) / 2;
            ctx.blit(BAR_ICONS[i], ix, iy, ix + BAR_ICON_SIZE, iy + BAR_ICON_SIZE, 0f, 1f, 0f, 1f);
            if (hov) barHoverLabel = BAR_LABELS[i];
        }
    }

    private void renderTabs(GuiGraphicsExtractor ctx, int px, int py, int mx, int my) {
        layoutTabRows();
        int startY = py + HEADER_H + 6;

        int totalH = tabRows.isEmpty() ? 0 : tabRows.get(tabRows.size() - 1).y() + tabRows.get(tabRows.size() - 1).h();
        int maxScroll = Math.max(0, totalH - tabListVisibleH());
        tabScroll = Mth.clamp(tabScroll, 0, maxScroll);

        ctx.enableScissor(px, startY, px + SIDEBAR_W, barTop());
        for (TabRow r : tabRows) {
            int tx = px, ty = startY + r.y() - tabScroll;
            if (ty + r.h() < startY || ty > barTop()) continue;

            if (r.isHeader()) {
                boolean expanded = isCategoryExpanded(r.label());
                boolean headerHover = mx >= tx && mx <= tx + SIDEBAR_W - 1 && my >= ty && my <= ty + r.h()
                        && my >= startY && my <= barTop();
                String arrow = expanded ? "▼" : "▶";
                drawText(ctx, font, Component.literal(arrow),
                        tx + 10, ty + (r.h() - font.lineHeight) / 2 + 1, headerHover ? C_TEXT : C_TEXT_DIM);
                drawText(ctx, font, Component.literal(r.label()),
                        tx + 20, ty + (r.h() - font.lineHeight) / 2 + 1, headerHover ? C_TEXT : C_TEXT_DIM);
                continue;
            }

            boolean active = r.tab() == currentTab && query.isEmpty();
            boolean hover  = !active && mx >= tx && mx <= tx + SIDEBAR_W - 1 && my >= ty && my <= ty + r.h()
                    && my >= startY && my <= barTop();
            if (active) {
                ctx.fill(tx, ty, tx + SIDEBAR_W - 1, ty + r.h(), C_TAB_ACTIVE);
                ctx.fill(tx, ty, tx + 2, ty + r.h(), C_ACCENT);
            } else if (hover) {
                ctx.fill(tx, ty, tx + SIDEBAR_W - 1, ty + r.h(), C_TAB_HOVER);
            }
            drawText(ctx, font, Component.literal(r.label()),
                    tx + 14, ty + (r.h() - font.lineHeight) / 2 + 1,
                    active ? C_ACCENT : hover ? C_TEXT : C_TEXT_DIM);
            if (r.tab() == Tab.ABOUT && PingWarning.active())
                drawWarningMark(ctx, font, tx + 14 + font.width(r.label()) + 4, ty + (r.h() - ROW_H) / 2);
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            int barX = px + SIDEBAR_W - 3;
            int barH = Math.max(16, (int) ((float) tabListVisibleH() / totalH * (barTop() - startY)));
            int barY = startY + (int) ((float) tabScroll / maxScroll * (barTop() - startY - barH));
            ctx.fill(barX, startY, barX + 2, barTop(), overlay(0x22));
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
        int tp = topPad(currentTab);
        totalH -= (cy + tp);
        int maxScroll = Math.max(0, totalH - ch + tp + PAD);
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
            int blockBot = r.y + r.h;
            for (int j = i + 1; j < rows.size(); j++) {
                if (rows.get(j).depth <= r.depth) break;
                blockBot = rows.get(j).y + rows.get(j).h;
            }
            int guideTop = r.y + r.h;
            if (blockBot > guideTop) ctx.fill(r.x + 5, guideTop, r.x + 6, blockBot, C_GROUP_BAR);
        }

        String hoverTooltip = null;
        String hoverWarning = null;

        int topRows = -1;
        for (int i = 0; i < rows.size(); i++) {
            RenderRow r = rows.get(i);
            if (r.depth == 0 && r.contentH > 0) topRows++;
            if (r.h <= 0) continue;
            int top = Math.max(cy, r.y);
            int bot = Math.min(cy + ch, r.y + r.h);
            if (bot <= top) continue;

            ctx.enableScissor(cx, top, cx + cw, bot);

            int rowOffsetX = 0;
            if (flyingIn) {
                int   stagger  = Math.min(Math.max(topRows, 0), TAB_IN_MAX_STAGGER_ROWS);
                float rowT     = Mth.clamp((tabInElapsed - stagger * TAB_IN_STAGGER) * TAB_IN_SPEED, 0f, 1f);
                if (rowT >= 1f && r.depth == 0 && r.node instanceof Group landed) flyingGroups.remove(landed.key);
                float rowEase  = rowT * rowT * (3f - 2f * rowT);
                rowOffsetX = Math.round((1f - rowEase) * -TAB_IN_DIST);
            }

            int rx = r.x + rowOffsetX;
            int rw = r.w;

            boolean rowHovered = !flyingIn && mx >= rx && mx < cx + cw - PAD && my >= top && my < bot;

            if (r.node instanceof Group g) {
                renderGroupHeader(ctx, g, rx, r.y, rw, mx, my);
                if (rowHovered && g.tooltip != null) hoverTooltip = g.tooltip;
                if (rowHovered && g.warning()) hoverWarning = g.warnText;
            } else if (r.node instanceof Heading hd) {
                renderHeading(ctx, hd, rx, r.y, rw);
            } else if (r.node instanceof Leaf lf) {
                if (rowHovered) ctx.fill(rx, r.y, rx + rw, r.y + r.contentH, overlay(0x08));
                ctx.fill(rx, r.y + r.contentH - 1, rx + rw, r.y + r.contentH, overlay(0x12));
                lf.f.render(ctx, this, rx, r.y, rw, mx, my);
                if (rowHovered && lf.f.tooltip != null) hoverTooltip = lf.f.tooltip;
                if (rowHovered && lf.f.warning()) hoverWarning = lf.f.warnText;
            }

            ctx.disableScissor();
        }
        ctx.disableScissor();

        if (hoverTooltip != null || hoverWarning != null) {
            drawTooltip(ctx, hoverTooltip, hoverWarning, mx, my);
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
            } else if (r.node instanceof Heading hd) {
                renderHeading(ctx, hd, rx, r.y, r.w);
            } else if (r.node instanceof Leaf lf) {
                lf.f.render(ctx, this, rx, r.y, r.w, -1, -1);
            }
            ctx.disableScissor();
        }
        ctx.disableScissor();
    }

    private static final int TOOLTIP_MAX_W = 200;

    private void drawTooltip(GuiGraphicsExtractor ctx, String text, int mx, int my) {
        drawTooltip(ctx, text, null, mx, my);
    }

    private void drawTooltip(GuiGraphicsExtractor ctx, String text, String warning, int mx, int my) {
        List<String> lines = text != null ? wrapTooltip(text, TOOLTIP_MAX_W) : List.of();
        List<String> warn = warning != null ? wrapWords(warning, TOOLTIP_MAX_W) : List.of();
        int lineH = font.lineHeight + 2;
        int gap = !lines.isEmpty() && !warn.isEmpty() ? 3 : 0;
        int tw = 0;
        for (String l : lines) tw = Math.max(tw, font.width(l));
        for (String l : warn) tw = Math.max(tw, font.width(l));

        int padX = 5, padY = 4;
        int textH = (lines.size() + warn.size() - 1) * lineH + gap + 7;
        int boxW = tw + 2 * padX;
        int boxH = textH + 2 * padY;
        int bx = mx < px() + PANEL_W / 2 ? mx + 12 : mx - boxW - 12;
        bx = Mth.clamp(bx, px() + 4, px() + PANEL_W - boxW - 4);
        int by = my - boxH - 3;
        if (by < 0) by = my + 16;

        roundedFill(ctx, bx, by, bx + boxW, by + boxH, darkMode ? 0xEC0A0A0E : 0xF0FFFBEF, 4);

        int ly = by + padY - 1;
        for (String l : lines) {
            drawText(ctx, font, Component.literal(l), bx + padX, ly, C_TEXT, false);
            ly += lineH;
        }
        ly += gap;
        for (String l : warn) {
            drawText(ctx, font, Component.literal(l), bx + padX, ly, WARNING_RED, false);
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
        boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + ROW_H;
        boolean toggled = g.get != null && g.set != null;
        boolean open = isExpanded(g);

        if (hov) ctx.fill(x, y, x + w, y + ROW_H, overlay(0x08));
        ctx.fill(x, y + ROW_H - 1, x + w, y + ROW_H, overlay(0x12));

        drawText(ctx, font, g.nameLabel,
                x + 8, y + (ROW_H - font.lineHeight) / 2 + 1,
                open ? C_ACCENT : C_TEXT);
        if (g.warning()) drawWarningMark(ctx, font, x + 8 + font.width(g.nameLabel) + 4, y);

        int arrowX = toggled ? x + w - 34 - 8 - 16 : x + w - 18;
        drawChevron(ctx, arrowX, y + ROW_H / 2, animOf(g), open || hov ? C_ACCENT : C_TEXT_DIM);

        if (toggled) {
            Float knobAnim = GROUP_TOGGLE_ANIM.get(g.key);
            drawSwitch(ctx, x, y, w, knobAnim != null ? knobAnim : g.get.get() ? 1f : 0f);
        }
    }

    private void renderHeading(GuiGraphicsExtractor ctx, Heading h, int x, int y, int w) {
        drawText(ctx, font, h.label, x + 8, y + HEADING_H - font.lineHeight - 4, C_ACCENT);
        ctx.fill(x, y + HEADING_H - 1, x + w, y + HEADING_H, overlay(0x2A));
    }

    private static void drawChevron(GuiGraphicsExtractor ctx, int x, int cy, float open, int color) {
        if (open > 0.5f) {
            for (int i = 0; i < 4; i++) ctx.fill(x + i, cy - 2 + i, x + 7 - i, cy - 1 + i, color);
        } else {
            for (int i = 0; i < 4; i++) ctx.fill(x + 2 + i, cy - 3 + i, x + 3 + i, cy + 4 - i, color);
        }
    }

    private boolean textInputOn;

    private void updateTextInput() {
        boolean want = capturingFeature(false) != null;
        if (want == textInputOn) return;
        textInputOn = want;
        com.phantomaddons.utils.TextInputCompat.set(this, want);
    }

    @Override public void removed() {
        if (textInputOn) com.phantomaddons.utils.TextInputCompat.set(this, false);
        textInputOn = false;
        PhantomConfig.save();
    }
    @Override public boolean isPauseScreen() { return false; }
}
