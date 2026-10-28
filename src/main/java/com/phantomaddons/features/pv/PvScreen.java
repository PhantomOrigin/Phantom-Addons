package com.phantomaddons.features.pv;

import com.mojang.blaze3d.platform.InputConstants;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.phantomaddons.gui.GuiDraw.roundedFill;
import static com.phantomaddons.gui.GuiDraw.roundedFillRadial;
import static com.phantomaddons.gui.Theme.*;

public final class PvScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────

    private static final int BASE_W = 460, BASE_H = 280, BASE_TAB_H = 24, BASE_SIDEBAR_W = 108;
    private static final int RADIUS = 6, SUB_ROW_H = 20, DROP_ROW_H = 18, DROP_H = 18;
    private static final long OUT_MS = 110, IN_MS = 190, OPEN_MS = 280, CLOSE_MS = 200;
    private static final int SLIDE_DIST = 26;

    private static final String[] TABS = { "Main", "Gear", "Slayers", "Dungeons", "Kuudra", "Collections", "Mining",
            "Farming", "Foraging", "Fishing", "Hunting", "Rift", "Museum", "Misc" };
    private static final String[] TAB_ICON_IDS = {
            "SKYBLOCK_MENU", null, "AATROX_BATPHONE", null, "KUUDRA;4", "ENCHANTED_DIAMOND",
            "TITANIUM_PICKAXE", "THEORETICAL_HOE_WARTS_3", "TREECAPITATOR_AXE", "MAGMA_ROD", "STINGER_BOW", "RIFT_PRISM",
            null, "ABIPHONE_XII_MEGA" };
    private static final net.minecraft.world.item.Item[] TAB_ICONS = {
            Items.PLAYER_HEAD, Items.ENDER_CHEST, Items.DIAMOND_SWORD, Items.WITHER_SKELETON_SKULL,
            Items.MAGMA_CREAM, Items.BOOK, Items.DIAMOND_PICKAXE, Items.WHEAT, Items.IRON_AXE,
            Items.FISHING_ROD, Items.BOW, Items.ENDER_EYE, Items.PAINTING, Items.COMPASS };
    private static final int ICON_TAB_MAX_W = 28, ICON_TAB_MIN_W = 16;

    private static final String[][] SUBTABS = {
            {},
            { "Inventory", "Loadouts", "Wardrobe", "Equipment", "Accessories", "Pets", "Backpacks", "Ender Chest", "Sacks" },
            {}, {}, {},
            { "Collections", "Minions", "Bestiary" },
            { "Gear", "HotM", "Commissions" },
            { "Gear", "Contests", "Garden", "Milestones", "Greenhouse" },
            {}, {}, {},
            { "Overview", "Inventory", "Ender Chest" },
            { "Combat", "Farming", "Mining", "Fishing", "Foraging", "Dungeoneering", "Hunting", "Special", "Upgrades" },
            {},
    };
    private static final int PAGE_FARMING = 7, PAGE_MUSEUM = 12, FARMING_GARDEN = 2, FARMING_MILESTONES = 3;

    private boolean hidden(int pageIdx, int sub) {
        if (data == null || data.extras) return false;
        if (pageIdx == PAGE_MUSEUM) return true;
        return pageIdx == PAGE_FARMING && sub >= 0 && (sub == FARMING_GARDEN || sub == FARMING_MILESTONES);
    }

    private static final int GEAR_INVENTORY = 0, GEAR_LOADOUTS = 1, GEAR_WARDROBE = 2,
            GEAR_EQUIP_WARDROBE = 3, GEAR_ACCESSORIES = 4, GEAR_PETS = 5, GEAR_BACKPACKS = 6, GEAR_ENDER = 7, GEAR_SACKS = 8;

    // ── State ─────────────────────────────────────────────────────────────────

    private String requestedName;
    private PvData data;
    private String error;
    private int profileIdx;
    private PvPlayerModel model;
    private PvProfileView modelView;
    private ItemStack[] modelArmor;

    private int page = 0;
    private final int[] subOf = new int[TABS.length];
    private int outKey = -1;
    private long transStart;
    private int transDir = 1;
    private final Map<Integer, Integer> scrolls = new HashMap<>();
    private final Map<Integer, Integer> maxScrolls = new HashMap<>();

    private float sidebarAnim = 0f;
    private int tabScrollX = 0;
    private boolean dropOpen;
    private float dropAnim;
    private int[] dropListRect;
    private int overlay = -1; // 0 = Heart of the Mountain, 1 = Heart of the Forest
    private float overlayAnim;
    private int overlayScroll;
    private int overlayMaxScroll;
    private int[] overlayBox;
    private String overlayTitle = "";

    private boolean draggingModel;
    private int[] modelRect;

    private long openStart = -1;
    private boolean closing;
    private long closeStart;
    private long lastFrame;
    private float partialTick;

    private float gs;
    float es;
    private int panelW, panelH, tabH, sidebarW;

    private record Hit(int x0, int y0, int x1, int y1, Runnable action) {
        boolean contains(double x, double y) { return x >= x0 && x < x1 && y >= y0 && y < y1; }
    }
    private final List<Hit> hits = new ArrayList<>();

    ItemStack hoverStack;
    List<Component> hoverLines;
    private int clipX0, clipY0, clipX1, clipY1;
    private boolean interactive;

    public PvScreen(String name, UUID uuid) {
        this(name, uuid, 0);
    }

    public PvScreen(String name, UUID uuid, int startPage) {
        super(Component.literal("Profile Viewer"));
        PvConstants.preload();
        PvIconRepo.load();
        page = startPage;
        if (startPage == 0) restorePage(name);
        load(name, uuid);
    }

    private static final long REMEMBER_PAGE_MS = 10 * 60_000L;
    private static final int  REMEMBER_PAGE_MAX = 50;

    private record SavedPage(int page, int[] subs, long at) {}

    private static final Map<String, SavedPage> SAVED_PAGES = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SavedPage> eldest) {
            return size() > REMEMBER_PAGE_MAX;
        }
    };

    private static String pageKey(String name) {
        return name == null ? null : name.toLowerCase(Locale.ROOT);
    }

    private void restorePage(String name) {
        String key = pageKey(name);
        if (key == null || !PhantomConfig.isPvRememberPage()) return;
        SavedPage saved = SAVED_PAGES.get(key);
        if (saved == null) return;
        if (System.currentTimeMillis() - saved.at() > REMEMBER_PAGE_MS) {
            SAVED_PAGES.remove(key);
            return;
        }
        if (saved.page() < 0 || saved.page() >= TABS.length) return;
        page = saved.page();
        System.arraycopy(saved.subs(), 0, subOf, 0, Math.min(saved.subs().length, subOf.length));
    }

    private void rememberPage() {
        if (party != null || !PhantomConfig.isPvRememberPage()) return;
        String key = pageKey(data != null && data.name != null ? data.name : requestedName);
        if (key == null) return;
        SAVED_PAGES.put(key, new SavedPage(page, subOf.clone(), System.currentTimeMillis()));
    }

    public static final int PAGE_KUUDRA = 4;


    private List<String> party;
    private PvData[] partyData;
    private String[] partyErrors;

    public static PvScreen party(List<String> names) {
        PvScreen s = new PvScreen(names);
        return s;
    }

    private PvScreen(List<String> names) {
        super(Component.literal("Party Profiles"));
        PvConstants.preload();
        PvIconRepo.load();
        party = List.copyOf(names.subList(0, Math.min(3, names.size())));
        partyData = new PvData[party.size()];
        partyErrors = new String[party.size()];
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < party.size(); i++) {
            final int idx = i;
            String name = party.get(i);
            PvFetcher.peek(name, null).whenComplete((d, err) -> mc.execute(() -> {
                if (d != null && partyData[idx] == null) partyView(idx, d);
            }));
            PvFetcher.fetch(name, null).whenComplete((d, err) -> mc.execute(() -> {
                if (err != null) { if (partyData[idx] == null) partyErrors[idx] = rootMessage(err); return; }
                partyView(idx, d);
            }));
        }
    }

    private void partyView(int idx, PvData d) {
        partyData[idx] = d;
        partyErrors[idx] = null;
        PvData.Profile p = d.profiles.get(d.selectedIndex());
        d.views.computeIfAbsent(p.id(), k -> CompletableFuture.supplyAsync(() -> new PvProfileView(p), PvItems.EXECUTOR));
    }

    private PvProfileView partyViewOf(int idx) {
        PvData d = partyData[idx];
        if (d == null) return null;
        CompletableFuture<PvProfileView> f = d.views.get(d.profiles.get(d.selectedIndex()).id());
        return f != null && f.isDone() && !f.isCompletedExceptionally() ? f.join() : null;
    }

    private int loadSeq;

    private void load(String name, UUID uuid) {
        int seq = ++loadSeq;
        requestedName = name;
        Minecraft mc = Minecraft.getInstance();
        PvFetcher.peek(name, uuid).whenComplete((d, err) -> mc.execute(() -> {
            if (seq == loadSeq && d != null && data == null) adopt(d);
        }));
        PvFetcher.fetch(name, uuid).whenComplete((d, err) -> mc.execute(() -> {
            if (seq != loadSeq) return;
            if (err != null) {
                if (data == null) error = rootMessage(err);
                return;
            }
            if (d == data) return;
            if (data == null || !data.uuid.equals(d.uuid)) { adopt(d); return; }
            String showing = profileIdx < data.profiles.size() ? data.profiles.get(profileIdx).id() : null;
            int idx = d.selectedIndex();
            for (int i = 0; i < d.profiles.size(); i++) if (d.profiles.get(i).id().equals(showing)) idx = i;
            PvData.Profile p = d.profiles.get(idx);
            d.views.computeIfAbsent(p.id(), k -> CompletableFuture.supplyAsync(() -> new PvProfileView(p), PvItems.EXECUTOR))
                    .thenCompose(PvProfileView::loaded)
                    .whenComplete((ok, e2) -> mc.execute(() -> { if (seq == loadSeq && d != data) adopt(d); }));
        }));
    }

    private void adopt(PvData d) {
        String keepProfile = data != null && profileIdx < data.profiles.size() ? data.profiles.get(profileIdx).id() : null;
        boolean samePlayer = data != null && data.uuid.equals(d.uuid);
        data = d;
        error = null;
        profileIdx = d.selectedIndex();
        if (samePlayer && keepProfile != null)
            for (int i = 0; i < d.profiles.size(); i++) if (d.profiles.get(i).id().equals(keepProfile)) profileIdx = i;
        if (model == null || !samePlayer) model = new PvPlayerModel(d.uuid, d.name);
        modelView = null;
        petIcons.clear();
        view(profileIdx);
        PvFetcher.noteViewed(d);
    }

    private void openPlayer(PvFetcher.Recent r) {
        recentOpen = false;
        dropOpen = false;
        if (data != null && data.uuid.equals(r.uuid())) return;
        data = null;
        error = null;
        model = null;
        modelView = null;
        profileIdx = 0;
        scrolls.clear();
        maxScrolls.clear();
        brokenTabs.clear();
        load(r.name(), r.uuid());
    }

    private static String rootMessage(Throwable t) {
        while ((t instanceof CompletionException || t.getCause() != null && t instanceof RuntimeException) && t.getCause() != null)
            t = t.getCause();
        return t.getMessage() != null ? t.getMessage() : t.toString();
    }

    private CompletableFuture<PvProfileView> view(int idx) {
        PvData.Profile p = data.profiles.get(idx);
        return data.views.computeIfAbsent(p.id(), k -> CompletableFuture.supplyAsync(() -> new PvProfileView(p), PvItems.EXECUTOR));
    }

    private PvProfileView currentView() {
        if (data == null) return null;
        CompletableFuture<PvProfileView> f = view(profileIdx);
        return f.isDone() && !f.isCompletedExceptionally() ? f.join() : null;
    }

    private boolean currentViewFailed() {
        if (data == null) return false;
        return view(profileIdx).isCompletedExceptionally();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        Theme.apply(PhantomConfig.getEffectivePvTheme());
        if (openStart < 0) openStart = System.currentTimeMillis();
    }

    @Override
    public void removed() {
        if (textInputOn) com.phantomaddons.utils.TextInputCompat.set(this, false);
        textInputOn = false;
        rememberPage();
        Theme.apply(PhantomConfig.getUiTheme());
    }

    @Override
    public void onClose() {
        if (closing) { super.onClose(); return; }
        closing = true;
        closeStart = System.currentTimeMillis();
    }

    @Override
    public void tick() {
        if (model != null) model.tick();
    }

    @Override public boolean isPauseScreen() { return false; }

    private float openProgress(long now) {
        float t = closing
                ? 1f - Math.min(1f, (now - closeStart) / (float) CLOSE_MS)
                : Math.min(1f, (now - openStart) / (float) OPEN_MS);
        return ease(t);
    }

    static float ease(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private static float approach(float cur, float target, float step) {
        return cur < target ? Math.min(target, cur + step) : Math.max(target, cur - step);
    }

    // ── Geometry ──────────────────────────────────────────────────────────────

    private float uiScale = 1f;
    private int lw, lhgt;

    private static final int[][] CONTENT_ROOM = { { 580, 353 }, { 460, 280 }, { 360, 219 } }; // small, medium, large

    private void layout() {
        float size = PhantomConfig.getPvGuiScale();
        int[] room = CONTENT_ROOM[PhantomConfig.getPvContent().ordinal()];
        int wantW = Math.round(BASE_W * size), wantH = Math.round(BASE_H * size);
        // The window the screen can show, and the zoom that puts the content''s room onto it.
        float zoom0 = Math.min(wantW / (float) room[0], wantH / (float) room[1]);
        int physW = Math.min(wantW, width - 16);
        int physH = Math.min(wantH, Math.round(height - (DROP_H + 6) * zoom0 - 24));
        uiScale = Math.max(0.3f, Math.min(physW / (float) room[0], physH / (float) room[1]));
        gs = 1f;
        es = 1f;
        lw = Math.round(width / uiScale);
        lhgt = Math.round(height / uiScale);
        panelW = Math.max(150, Math.round(physW / uiScale));
        panelH = Math.max(100, Math.round(physH / uiScale));
        tabH = BASE_TAB_H;
        sidebarW = Math.max(72, Math.min(BASE_SIDEBAR_W, Math.round(panelW * 0.24f)));
    }
    private int phys(int v) { return Math.round(v * uiScale); }

    private int px() { return (lw - panelW) / 2; }
    private int py() { return (lhgt - panelH - DROP_H - 6) / 2; }
    private int key() { return page * 100 + subOf[page]; }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        if (openStart < 0) return;
        int base = PhantomConfig.isUiTransparencyEnabled() ? 0x7A : 0xBB;
        int a = (int) (base * openProgress(System.currentTimeMillis()));
        ctx.fill(0, 0, width, height, (a << 24) | 0x06080C);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        boolean wantText = searchFocus >= 0;
        if (wantText != textInputOn) {
            textInputOn = wantText;
            com.phantomaddons.utils.TextInputCompat.set(this, wantText);
        }
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        partialTick = Mth.clamp(delta, 0f, 1f);
        layout();
        final int tipX = mx, tipY = my;
        mx = Math.round(mx / uiScale);
        my = Math.round(my / uiScale);
        lastMouseX = mx;
        lastMouseY = my;

        if (closing && now - closeStart >= CLOSE_MS) { super.onClose(); return; }

        hits.clear();
        hoverStack = null;
        hoverLines = null;
        modelRect = null;
        if (hidden(page, -1)) page = 0;
        else if (SUBTABS[page].length > 0 && hidden(page, subOf[page])) subOf[page] = 0;

        boolean hasSub = SUBTABS[page].length > 0;
        sidebarAnim = approach(sidebarAnim, hasSub ? 1f : 0f, dt * 7f);
        dropAnim = approach(dropAnim, dropOpen ? 1f : 0f, dt * 9f);
        recentAnim = approach(recentAnim, recentOpen ? 1f : 0f, dt * 9f);
        overlayAnim = approach(overlayAnim, overlay >= 0 ? 1f : 0f, dt * 8f);
        if (overlayAnim <= 0f && overlay < 0) overlayBox = null;

        PvProfileView v = currentView();
        if (model != null && v != null && (v != modelView || v.armor != modelArmor)) {
            modelView = v;
            modelArmor = v.armor;
            ItemStack[] lobby = model.lobbyArmor();
            model.setArmor(lobby != null && v.profile.selected() ? lobby : v.armor);
        }

        int px = px(), py = py();
        float prog = openProgress(now);
        float scale = 0.85f + 0.15f * prog;
        var pose = ctx.pose();
        pose.pushMatrix();
        pose.scale(uiScale, uiScale);
        pose.pushMatrix();
        pose.translate(px + panelW / 2f, py + panelH / 2f);
        pose.scale(scale, scale);
        pose.translate(-(px + panelW / 2f), -(py + panelH / 2f));

        boolean transparent = PhantomConfig.isUiTransparencyEnabled();
        int outline = transparent ? withAlpha(C_BORDER, 130) : C_BORDER;
        roundedFill(ctx, px - 1, py - 1, px + panelW + 1, py + panelH + 1, outline, RADIUS + 1);
        int bgShade = transparent ? 26 : 10;
        roundedFillRadial(ctx, px, py, px + panelW, py + panelH,
                px + panelW * 0.3, py + tabH + 20, Math.hypot(panelW, panelH) * 0.85,
                shade(C_BG, bgShade), shade(C_BG, -bgShade), RADIUS, true, true, true, true);

        ctx.enableScissor(px, py, px + panelW, py + panelH);
        if (party != null) {
            renderParty(ctx, px, py, mx, my, now);
            ctx.disableScissor();
        } else {
            renderTabBar(ctx, px, py, mx, my);
            int sw = Math.round(sidebarW * ease(sidebarAnim));
            if (sw > 0) renderSidebar(ctx, px, py + tabH, sw, mx, my);
            renderContent(ctx, px + sw, py + tabH, panelW - sw, panelH - tabH, mx, my, now);
            ctx.disableScissor();

            renderDropdown(ctx, px, py, mx, my);
            renderOverlay(ctx, px, py, mx, my);
        }

        pose.popMatrix();
        pose.popMatrix();

        submitTooltip(ctx, tipX, tipY);
    }

    /** The hovered item's or entry's tooltip, drawn after everything else. */
    private void submitTooltip(GuiGraphicsExtractor ctx, int tipX, int tipY) {
        List<Component> lines;
        if (hoverStack != null) {
            lines = itemTooltip(hoverStack);
        } else if (hoverLines != null) {
            lines = PvNumerals.enabled() ? PvNumerals.convert(hoverLines) : hoverLines;
        } else {
            tooltipMaxScroll = 0;
            return;
        }

        // A tooltip taller than the screen can be scrolled with the mouse wheel; a new one starts at the top.
        String key = lines.isEmpty() ? "" : lines.get(0).getString();
        if (!key.equals(tooltipKey)) {
            tooltipKey = key;
            tooltipScroll = 0;
        }
        List<net.minecraft.util.FormattedCharSequence> drawn = new ArrayList<>(lines.size());
        for (Component line : lines) drawn.add(line.getVisualOrderText());
        ctx.setTooltipForNextFrame(font, drawn, Optional.empty(), tooltipPositioner, tipX, tipY, false, null);
    }

    /** Where the hovered tooltip sits, scroll position and how far it can scroll. */
    private float tooltipScroll;
    private int tooltipMaxScroll;
    private String tooltipKey = "";

    /** Like the normal positioning, except a tooltip too tall for the screen is moved up and down by the scroll. */
    private final net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner tooltipPositioner =
            (screenW, screenH, mouseX, mouseY, w, h) -> {
                int x = mouseX + 12, y = mouseY - 12;
                if (x + w > screenW) x = Math.max(x - 24 - w, 4);
                int needed = h + 8;
                if (needed <= screenH) {
                    tooltipMaxScroll = 0;
                    if (y + h + 3 > screenH) y = screenH - h - 3;
                    return new org.joml.Vector2i(x, y);
                }
                tooltipMaxScroll = needed - screenH;
                tooltipScroll = Mth.clamp(tooltipScroll, 0, tooltipMaxScroll);
                return new org.joml.Vector2i(x, 4 - Math.round(tooltipScroll));
            };

    /**
     * The lines of an item's tooltip, without trailing blank lines and without any extra tooltip image. Other mods
     * add lines and images to every item tooltip, which can leave a large empty gap under the text here.
     */
    private List<Component> itemTooltip(ItemStack stack) {
        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, stack));
        while (lines.size() > 1 && lines.get(lines.size() - 1).getString().replaceAll("§.", "").isBlank()) {
            lines.remove(lines.size() - 1);
        }
        return PvNumerals.enabled() ? PvNumerals.convert(lines) : lines;
    }

    // ── Party mode rendering ──────────────────────────────────────────────────

    private static final String[][] KUUDRA_TIERS = {
            { "infernal", "Infernal" }, { "fiery", "Fiery" }, { "burning", "Burning" }, { "hot", "Hot" }, { "none", "Basic" } };

    private void renderParty(GuiGraphicsExtractor ctx, int px, int py, int mx, int my, long now) {
        roundedFill(ctx, px, py, px + panelW, py + tabH, C_HEADER, RADIUS, true, true, false, false);
        ctx.fill(px, py + tabH - 1, px + panelW, py + tabH, C_ACCENT);
        String title = "Party Finder §7— Kuudra Profiles";
        drawLegacy(ctx, title, px + 10, py + (tabH - font.lineHeight) / 2, C_ACCENT);
        String hint = "Click a name for the full profile";
        ctx.text(font, hint, px + panelW - 10 - font.width(hint), py + (tabH - font.lineHeight) / 2, C_TEXT_DIM, darkMode);

        int pad = 6, gap = 6, n = party.size();
        int cx0 = px + pad, cy0 = py + tabH + pad, cw = (panelW - 2 * pad - (n - 1) * gap) / n, ch = panelH - tabH - 2 * pad;
        clipX0 = px; clipY0 = py + tabH; clipX1 = px + panelW; clipY1 = py + panelH;
        for (int i = 0; i < n; i++) {
            int x = cx0 + i * (cw + gap);
            roundedFill(ctx, x, cy0, x + cw, cy0 + ch, overlay(0x10), 5);
            ctx.enableScissor(x, cy0, x + cw, cy0 + ch);
            partyColumn(ctx, i, x, cy0, cw, ch, mx, my, now);
            ctx.disableScissor();
        }
    }

    private void partyColumn(GuiGraphicsExtractor ctx, int idx, int x, int y, int w, int h, int mx, int my, long now) {
        String name = party.get(idx);
        PvData d = partyData[idx];
        PvProfileView v = partyViewOf(idx);
        int ip = 5;
        int iw = w - 2 * ip;

        String nameLine = (v != null ? levelTag(v.skyblockLevel) + " " : "") + "§r" + (d != null ? d.name : name);
        int nh = lineH(es) + 4;
        boolean nameHov = hovered(mx, my, x, y, w, nh + 2);
        text(ctx, nameLine, x + ip, y + ip, nameHov ? C_ACCENT : C_TEXT, es);
        if (d != null) {
            String prof = d.profiles.get(d.selectedIndex()).symbol() + "§7" + d.profiles.get(d.selectedIndex()).cuteName();
            float ps = 0.7f * es;
            text(ctx, prof, x + w - ip - textW(prof, ps), y + ip + (lineH(es) - lineH(ps)) / 2, C_TEXT, ps);
        }
        final String target = d != null ? d.name : name;
        addHit(x, y, x + w, y + nh + 2, () -> minecraft.gui.setScreen(new PvScreen(target, null, PAGE_KUUDRA)));
        int yy = y + ip + nh;

        if (v == null) {
            String msg = partyErrors[idx] != null ? "§c" + partyErrors[idx] : "Loading" + ".".repeat((int) (now / 300 % 4));
            for (var l : font.split(Component.literal(msg), Math.max(40, (int) (iw / (0.8f * es))))) {
                var pose = ctx.pose();
                pose.pushMatrix();
                pose.translate(x + ip, yy);
                pose.scale(0.8f * es, 0.8f * es);
                ctx.text(font, l, 0, 0, C_TEXT_DIM, darkMode);
                pose.popMatrix();
                yy += lineH(0.8f * es) + 2;
            }
            return;
        }

        int sets = v.itemsReady() ? Math.max(1, v.analysis.kuudraSets.size()) : 1;
        int weaponRows = v.itemsReady() ? v.analysis.kuudraWeapons.size() + 2 : 1;
        int statRows = 2 + (KUUDRA_TIERS.length + 1) / 2;
        float ss = 0.75f * es;
        int heads = 3 * (lineH(0.85f * es) + 3);
        int lh = 0, slot = 0, setH = 0;
        for (int tries = 0; tries < 8; tries++) {
            lh = lineH(ss) + 3;
            slot = Math.max(9, Math.min(Math.round(18 * es), lh + 2));
            setH = lineH(ss) + 2 + slot + 3;
            int need = (yy - y) + heads + statRows * lh + weaponRows * Math.max(lh, slot + 2) + sets * setH + 8;
            if (need <= h || ss <= 0.5f) break;
            ss -= 0.04f;
        }
        int wlh = Math.max(lh, slot + 2);
        float hs = 0.85f * es;
        int half = (iw - 6) / 2;

        // ── Stats (two columns) ──
        text(ctx, "§eStats", x + ip, yy, C_TEXT, hs);
        yy += lineH(hs) + 3;
        com.google.gson.JsonObject tiers = PvJson.obj(v.member, "nether_island_player_data", "kuudra_completed_tiers");
        int foraging = 0;
        for (PvProfileView.Skill sk : v.skills) if (sk.name().equals("Foraging")) foraging = sk.level().level();
        String cata = String.valueOf(PvDungeonsPage.level(PvJson.num(v.member, 0, "dungeons", "dungeon_types", "catacombs", "experience")).level());
        String[][] stats = {
                { "Bank", v.bank >= 0 ? PvPages.shortNum(v.bank) : "?" },
                { "MP", v.magicalPower >= 0 ? String.valueOf(v.magicalPower) : "?" },
                { "Cata", cata },
                { "Foraging", String.valueOf(foraging) } };
        for (int i = 0; i < stats.length; i++) {
            int sx = x + ip + (i % 2) * (half + 6), sy = yy + (i / 2) * lh;
            statRow(ctx, stats[i][0], stats[i][1], sx, sy, half, ss);
        }
        yy += 2 * lh;
        for (int i = 0; i < KUUDRA_TIERS.length; i++) {
            int sx = x + ip + (i % 2) * (half + 6), sy = yy + (i / 2) * lh;
            double comps = PvJson.num(tiers, 0, KUUDRA_TIERS[i][0]);
            statRow(ctx, KUUDRA_TIERS[i][1], PvPages.num(comps), sx, sy, half, ss);
            double wave = PvJson.num(tiers, -1, "highest_wave_" + KUUDRA_TIERS[i][0]);
            if (wave >= 0 && hovered(mx, my, sx, sy, half, lh))
                tooltip("§6" + KUUDRA_TIERS[i][1] + " Kuudra", "§7Completions: §e" + PvPages.num(comps), "§7Highest Wave: §e" + (int) wave);
        }
        yy += ((KUUDRA_TIERS.length + 1) / 2) * lh + 3;

        // ── Weapons ──
        text(ctx, "§eWeapons", x + ip, yy, C_TEXT, hs);
        yy += lineH(hs) + 3;
        if (!v.itemsReady()) {
            text(ctx, "§8Loading items...", x + ip, yy, C_TEXT, ss);
            return;
        }
        int textOff = (wlh - lineH(ss)) / 2, slotOff = (wlh - slot) / 2;
        for (PvAnalysis.KuudraWeapon wp : v.analysis.kuudraWeapons) {
            boolean has = !wp.stack().isEmpty();
            slot(ctx, x + ip, yy + slotOff, slot, wp.stack(), mx, my);
            text(ctx, (wp.partial() ? "§6– §r" : has ? "§a✔ §r" : "§c✖ §r") + wp.label(), x + ip + slot + 4, yy + textOff, C_TEXT, ss);
            yy += wlh;
        }
        partyDragon(ctx, "DPS", v.analysis.dpsDragon, x + ip, yy, slot, slotOff, textOff, ss, mx, my);
        yy += wlh;
        partyDragon(ctx, "Rend", v.analysis.rendDragon, x + ip, yy, slot, slotOff, textOff, ss, mx, my);
        yy += wlh + 3;

        // ── Kuudra armor sets (with the equipment of a loadout that uses them) ──
        text(ctx, "§eKuudra Sets", x + ip, yy, C_TEXT, hs);
        yy += lineH(hs) + 3;
        if (v.analysis.kuudraSets.isEmpty()) {
            text(ctx, "§8None found in the wardrobe", x + ip, yy, C_TEXT, ss);
            return;
        }
        int g = gapFor(slot);
        for (PvAnalysis.KuudraSet ks : v.analysis.kuudraSets) {
            text(ctx, ks.label() + " §8#" + ks.slotNumber(), x + ip, yy, C_TEXT, ss);
            yy += lineH(ss) + 2;
            PvProfileView.WardrobeSet armor = v.armorSet(ks.slotNumber());
            PvProfileView.WardrobeSet equip = null;
            for (PvProfileView.Loadout lo : v.loadouts) {
                if (lo.armorSet() == ks.slotNumber()) { equip = v.equipmentSet(lo.equipmentSet()); break; }
            }
            for (int i = 0; i < 4; i++) {
                ItemStack a = armor != null && i < armor.items().length ? armor.items()[i] : ItemStack.EMPTY;
                slot(ctx, x + ip + i * (slot + g), yy, slot, a, mx, my);
            }
            if (equip != null) {
                int ex = x + ip + 4 * (slot + g) + 4;
                for (int i = 0; i < 4; i++) {
                    ItemStack q = i < equip.items().length ? equip.items()[i] : ItemStack.EMPTY;
                    slot(ctx, ex + i * (slot + g), yy, slot, q, mx, my);
                }
            }
            yy += slot + 3;
        }
    }

    private void partyDragon(GuiGraphicsExtractor ctx, String role, PvProfileView.Pet pet, int x, int y, int slot,
                             int slotOff, int textOff, float ss, int mx, int my) {
        if (pet == null) {
            slot(ctx, x, y + slotOff, slot, ItemStack.EMPTY, mx, my);
            text(ctx, "§8" + role + " Gdrag: None", x + slot + 4, y + textOff, C_TEXT, ss);
            return;
        }
        petSlot(ctx, x, y + slotOff, slot, pet, mx, my);
        text(ctx, "§7[" + pet.level().level() + "] §r" + role + " Gdrag", x + slot + 4, y + textOff, C_TEXT, ss);
    }

    // ── Chrome ────────────────────────────────────────────────────────────────

    private void renderTabBar(GuiGraphicsExtractor ctx, int px, int py, int mx, int my) {
        roundedFill(ctx, px, py, px + panelW, py + tabH, C_HEADER, RADIUS, true, true, false, false);
        ctx.fill(px, py + tabH - 1, px + panelW, py + tabH, C_ACCENT);

        int areaX0 = px, areaX1 = px + panelW, inset = 6;
        boolean icons = PhantomConfig.isPvTabIcons();
        int shown = 0;
        for (int i = 0; i < TABS.length; i++) if (!hidden(i, -1)) shown++;
        // Icon tabs share the bar equally, so they all fit on one screen; text tabs are as wide as their names.
        int iconW = Math.max(ICON_TAB_MIN_W, Math.min(ICON_TAB_MAX_W, (panelW - 2 * inset) / Math.max(1, shown)));
        int total = 0;
        for (int i = 0; i < TABS.length; i++) if (!hidden(i, -1)) total += icons ? iconW : font.width(TABS[i]) + 22;
        tabMaxScroll = Math.max(0, total + 2 * inset - panelW);
        tabScrollX = Mth.clamp(tabScrollX, 0, tabMaxScroll);
        fadeX0 = areaX0;
        fadeX1 = areaX1;
        fadeLeft = tabScrollX > 0;
        fadeRight = tabScrollX < tabMaxScroll;
        cornerY = py;

        ctx.enableScissor(areaX0, py, areaX1, py + tabH);
        int x = areaX0 + inset + (icons && tabMaxScroll == 0 ? (panelW - 2 * inset - total) / 2 : 0) - tabScrollX;
        for (int i = 0; i < TABS.length; i++) {
            if (hidden(i, -1)) continue;
            int w = icons ? iconW : font.width(TABS[i]) + 22;
            int y0 = py + 3, y1 = py + tabH - 1;
            boolean active = i == page;
            boolean hover = !active && data != null && mx >= x && mx < x + w && my >= y0 && my < y1 && mx >= areaX0 && mx < areaX1;
            if (active) {
                fadedFill(ctx, x, y0, x + w, y1, C_TAB_ACTIVE, true);
                fadedFill(ctx, x + 4, y1 - 2, x + w - 4, y1, C_ACCENT, false);
            } else if (hover) {
                fadedFill(ctx, x, y0, x + w, y1, C_TAB_HOVER, true);
            }
            if (icons) {
                int sz = Math.min(16, w - 4);
                item(ctx, TAB_ICON_IDS[i] != null ? repoIcon(TAB_ICON_IDS[i], TAB_ICONS[i]) : new ItemStack(TAB_ICONS[i]),
                        x + (w - sz) / 2, py + (tabH - sz) / 2, sz);
                boolean over = mx >= x && mx < x + w && my >= y0 && my < y1 && mx >= areaX0 && mx < areaX1;
                if (over) tooltip(TABS[i]);
            } else {
                fadedText(ctx, TABS[i], x + 11, py + (tabH - font.lineHeight) / 2,
                        active ? C_ACCENT : hover ? C_TEXT : C_TEXT_DIM);
            }
            if (data != null) {
                final int idx = i;
                addHit(Math.max(x, areaX0), y0, Math.min(x + w, areaX1), y1, () -> switchTo(idx, subOf[idx]));
            }
            x += w;
        }
        ctx.disableScissor();
        fadeLeft = fadeRight = false;
    }

    private static final int TAB_FADE_W = 26;
    private static final float TAB_FADE_MIN = 0.2f;
    private int fadeX0, fadeX1, cornerY;
    private boolean fadeLeft, fadeRight;

    private float fadeAlpha(float x) {
        float a = 1f;
        if (fadeLeft) a = Math.min(a, Mth.clamp((x - fadeX0) / TAB_FADE_W, 0f, 1f));
        if (fadeRight) a = Math.min(a, Mth.clamp((fadeX1 - x) / TAB_FADE_W, 0f, 1f));
        return TAB_FADE_MIN + (1f - TAB_FADE_MIN) * a;
    }

    private boolean inFade(int x0, int x1) {
        return (fadeLeft && x0 < fadeX0 + TAB_FADE_W) || (fadeRight && x1 > fadeX1 - TAB_FADE_W);
    }

    private static int scaleAlpha(int argb, float a) {
        return (Math.round(((argb >>> 24) & 0xFF) * a) << 24) | (argb & 0xFFFFFF);
    }

    private void fadedText(GuiGraphicsExtractor ctx, String s, int x, int y, int color) {
        if (!inFade(x, x + font.width(s))) { ctx.text(font, s, x, y, color, darkMode); return; }
        int cx = x;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            int w = font.width(ch);
            ctx.text(font, ch, cx, y, scaleAlpha(color, fadeAlpha(cx + w / 2f)), darkMode);
            cx += w;
            i += Character.charCount(cp);
        }
    }

    private boolean inCorner(int x0, int x1) {
        return x0 < fadeX0 + RADIUS || x1 > fadeX1 - RADIUS;
    }

    private int cornerTop(int x) {
        float xc = x + 0.5f, d = 0;
        if (xc < fadeX0 + RADIUS) d = fadeX0 + RADIUS - xc;
        else if (xc > fadeX1 - RADIUS) d = xc - (fadeX1 - RADIUS);
        if (d <= 0) return cornerY;
        float inset = RADIUS - (float) Math.sqrt(Math.max(0, RADIUS * RADIUS - d * d));
        return cornerY + (int) Math.ceil(inset);
    }

    private void fadedFill(GuiGraphicsExtractor ctx, int x0, int y0, int x1, int y1, int color, boolean roundedTop) {
        if (!inFade(x0, x1) && !inCorner(x0, x1)) {
            if (roundedTop) roundedFill(ctx, x0, y0, x1, y1, color, 4, true, true, false, false);
            else ctx.fill(x0, y0, x1, y1, color);
            return;
        }
        for (int x = Math.max(x0, fadeX0); x < Math.min(x1, fadeX1); x++) {
            int top = Math.max(y0, cornerTop(x));
            if (top < y1) ctx.fill(x, top, x + 1, y1, scaleAlpha(color, fadeAlpha(x + 0.5f)));
        }
    }

    private int tabMaxScroll;
    private boolean tabDragging, tabDragMoved;
    private double tabDragStartX;
    private int tabDragStartScroll;
    private Hit tabPressHit;

    private int sidebarScroll, sidebarMaxScroll;

    private void renderSidebar(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my) {
        int bottom = py() + panelH;
        roundedFill(ctx, x, y, x + w, bottom, C_SIDEBAR, RADIUS, false, false, true, false);
        ctx.fill(x + w - 1, y, x + w, bottom, C_BORDER);

        String[] subs = SUBTABS[page].length > 0 ? SUBTABS[page] : SUBTABS[1];
        List<Integer> shown = new ArrayList<>();
        for (int i = 0; i < subs.length; i++) if (SUBTABS[page].length == 0 || !hidden(page, i)) shown.add(i);
        int avail = bottom - y - 10;
        int pitch = Math.min(SUB_ROW_H + 2, Math.max(15, avail / Math.max(1, shown.size())));
        int rowH = pitch - 2;
        float ts = Math.min(1f, Math.max(0.75f, rowH / 18f));
        sidebarMaxScroll = Math.max(0, shown.size() * pitch - avail);
        sidebarScroll = Mth.clamp(sidebarScroll, 0, sidebarMaxScroll);

        ctx.enableScissor(x, y, x + w - 1, bottom);
        int ry = y + 6 - sidebarScroll;
        for (int i : shown) {
            boolean active = SUBTABS[page].length > 0 && i == subOf[page];
            boolean hover = !active && mx >= x && mx < x + w - 1 && my >= Math.max(ry, y) && my < Math.min(ry + rowH, bottom)
                    && my >= ry && my < ry + rowH;
            if (active) {
                ctx.fill(x, ry, x + w - 1, ry + rowH, C_TAB_ACTIVE);
                ctx.fill(x, ry, x + 2, ry + rowH, C_ACCENT);
            } else if (hover) {
                ctx.fill(x, ry, x + w - 1, ry + rowH, C_TAB_HOVER);
            }
            text(ctx, subs[i], x + 10, ry + (rowH - lineH(ts)) / 2, active ? C_ACCENT : hover ? C_TEXT : C_TEXT_DIM, ts);
            if (SUBTABS[page].length > 0 && data != null) {
                final int idx = i;
                addHit(x, Math.max(ry, y), x + w - 1, Math.min(ry + rowH, bottom), () -> switchTo(page, idx));
            }
            ry += pitch;
        }
        ctx.disableScissor();
    }

    boolean cardReady(int key) {
        if (error != null) return true;
        if (data == null) return false;
        PvProfileView v = currentView();
        return v == null ? currentViewFailed() : stageReady(key, v);
    }

    private static boolean stageReady(int key, PvProfileView v) {
        int pg = key / 100, sub = key % 100;
        return switch (pg) {
            case 1 -> sub == GEAR_BACKPACKS || sub == GEAR_ENDER || sub == GEAR_SACKS ? v.itemsReady() : v.basicReady();
            case 2, 3, 4, 8, 9, 11 -> v.itemsReady();
            case 6, 7 -> sub != 0 || v.itemsReady();
            default -> true;
        };
    }

    private void switchTo(int newPage, int newSub) {
        int oldKey = key();
        int newKey = newPage * 100 + newSub;
        dropOpen = false;
        if (oldKey == newKey) return;
        if (newPage != page) sidebarScroll = 0;
        transDir = newKey > oldKey ? 1 : -1;
        outKey = oldKey;
        transStart = System.currentTimeMillis();
        page = newPage;
        subOf[newPage] = newSub;
        rememberPage();
    }

    // ── Content + page transitions ────────────────────────────────────────────

    private void renderContent(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int mx, int my, long now) {
        if (data == null || currentView() == null || (model != null && !model.ready() && error == null)) {
            String msg;
            int color = C_TEXT_DIM;
            if (error != null) { msg = "Couldn't load profile: " + error; color = C_ERROR; }
            else if (currentViewFailed()) { msg = "Couldn't read this profile's data."; color = C_ERROR; }
            else msg = "Loading" + ".".repeat((int) (now / 300 % 4));
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(msg), Math.max(60, w - 30));
            int ly = y + h / 2 - lines.size() * (font.lineHeight + 2) / 2;
            for (var l : lines) {
                ctx.text(font, l, x + (w - font.width(l)) / 2, ly, color, darkMode);
                ly += font.lineHeight + 2;
            }
            return;
        }

        long t = now - transStart;
        if (outKey >= 0 && t < OUT_MS) {
            float e = ease(t / (float) OUT_MS);
            renderKey(ctx, outKey, x, y, w, h, Math.round(-transDir * e * SLIDE_DIST), e, -10000, -10000);
        } else if (outKey >= 0 && t < OUT_MS + IN_MS) {
            float e = ease((t - OUT_MS) / (float) IN_MS);
            renderKey(ctx, key(), x, y, w, h, Math.round(transDir * (1f - e) * SLIDE_DIST), 1f - e, -10000, -10000);
        } else {
            outKey = -1;
            boolean blocked = overlay >= 0 || (dropListRect != null && dropAnim > 0f
                    && mx >= dropListRect[0] && mx < dropListRect[2] && my >= dropListRect[1] && my < dropListRect[3])
                    || (recentListRect != null && recentAnim > 0f
                    && mx >= recentListRect[0] && mx < recentListRect[2] && my >= recentListRect[1] && my < recentListRect[3]);
            interactive = true;
            renderKey(ctx, key(), x, y, w, h, 0, 0f, blocked ? -10000 : mx, blocked ? -10000 : my);
            interactive = false;
        }
    }

    private void renderKey(GuiGraphicsExtractor ctx, int key, int x, int y, int w, int h,
                           int dx, float fade, int mx, int my) {
        PvProfileView v = currentView();
        clipX0 = x; clipY0 = y; clipX1 = x + w; clipY1 = y + h;
        ctx.enableScissor(x, y, x + w, y + h);
        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(dx, 0);

        int scroll = Mth.clamp(scrolls.getOrDefault(key, 0), 0, maxScrolls.getOrDefault(key, Integer.MAX_VALUE));
        int contentH = 0;
        int vx = x, vy = y, vw = w, vh = h, vmx = mx, vmy = my;
        float k = 1f, ox = x, oy = y;
        boolean scaledPage = MEDIUM_LOOK.contains(key / 100) && PhantomConfig.getPvContent() == PhantomConfig.PvContent.SMALL;
        if (scaledPage) {
            int mediumW = CONTENT_ROOM[1][0], mediumH = CONTENT_ROOM[1][1] - BASE_TAB_H;
            k = Math.min(w / (float) mediumW, h / (float) mediumH);
            if (k > 1.02f) {
                ox = x + (w - mediumW * k) / 2f;
                vx = 0; vy = 0; vw = mediumW; vh = mediumH;
                vmx = mx < -5000 ? mx : Math.round((mx - ox) / k);
                vmy = my < -5000 ? my : Math.round((my - oy) / k);
            } else {
                scaledPage = false;
                k = 1f;
            }
        }
        boolean entered = false;
        FitState fit = FIT_PAGES.contains(key / 100) ? fitState(key, vw, vh) : null;
        float savedEs = es;
        if (fit != null) es = savedEs * fit.scale;
        if (brokenTabs.contains(key)) {
            text(ctx, "§cThis tab hit an error reading this profile (see the log).", x + 10, y + 10, C_TEXT, es);
        } else if (!stageReady(key, v)) {
            String msg = "Loading items" + ".".repeat((int) (System.currentTimeMillis() / 300 % 4));
            ctx.text(font, msg, x + (w - font.width("Loading items...")) / 2, y + h / 2 - font.lineHeight / 2, C_TEXT_DIM, darkMode);
        } else try {
        int sc0 = clipX0, sc1 = clipY0, sc2 = clipX1, sc3 = clipY1;
        if (scaledPage) {
            entered = true;
            pose.pushMatrix();
            pose.translate(ox, oy);
            pose.scale(k, k);
            clipX0 = 0; clipY0 = 0; clipX1 = vw; clipY1 = vh;
            hitK = k; hitOx = ox; hitOy = oy;
        }
        contentH = switch (key) {
            case 0 -> renderMain(ctx, v, x, y, w, h, mx, my, scroll);
            case 100 + GEAR_INVENTORY -> renderInventory(ctx, v.armor, v.equipment, v.inventory, v.inventoryApi, x, y, w, h, mx, my, scroll);
            case 1101 -> renderInventory(ctx, v.riftArmor, v.riftEquipment, v.riftInventory, v.riftInventoryApi, x, y, w, h, mx, my, scroll);
            case 1102 -> PvStoragePage.render(this, ctx, v, key, PvStoragePage.Kind.RIFT_ENDER, x, y, w, h, mx, my, scroll);
            case 100 + GEAR_LOADOUTS -> renderLoadouts(ctx, v, x, y, w, h, mx, my, scroll);
            case 100 + GEAR_WARDROBE -> renderWardrobe(ctx, v.wardrobe, v.wardrobeEquipped, true, x, y, w, h, mx, my, scroll);
            case 100 + GEAR_EQUIP_WARDROBE -> renderWardrobe(ctx, v.equipmentWardrobe, v.equipmentWardrobeEquipped, false, x, y, w, h, mx, my, scroll);
            case 100 + GEAR_ACCESSORIES -> renderAccessories(ctx, v, x, y, w, h, mx, my, scroll);
            case 100 + GEAR_PETS -> renderPets(ctx, v, x, y, w, h, mx, my, scroll);
            case 100 + GEAR_BACKPACKS -> PvStoragePage.render(this, ctx, v, key, PvStoragePage.Kind.BACKPACKS, x, y, w, h, mx, my, scroll);
            case 100 + GEAR_ENDER -> PvStoragePage.render(this, ctx, v, key, PvStoragePage.Kind.ENDER, x, y, w, h, mx, my, scroll);
            case 100 + GEAR_SACKS -> PvSacksPage.render(this, ctx, v, key, x, y, w, h, mx, my, scroll);
            default -> {
                PvPages.Ctx c = new PvPages.Ctx(this, ctx, v, key, key % 100, vx, vy, vw, vh, vmx, vmy, scroll);
                yield switch (key / 100) {
                    case 2 -> PvSlayersPage.render(c);
                    case 3 -> PvDungeonsPage.render(c);
                    case 4 -> PvKuudraPage.render(c);
                    case 5 -> PvCollectionsPage.render(c);
                    case 6 -> PvMiningPage.render(c);
                    case 7 -> PvFarmingPage.render(c);
                    case 8 -> PvSkillGearPages.foraging(c);
                    case 9 -> PvSkillGearPages.fishing(c);
                    case 10 -> PvHuntingPage.render(c);
                    case 11 -> PvRiftPage.render(c);
                    case 12 -> PvMuseumPage.render(c);
                    case 13 -> PvMiscPage.render(c);
                    default -> 0;
                };
            }
        };
        if (entered) {
            pose.popMatrix();
            clipX0 = sc0; clipY0 = sc1; clipX1 = sc2; clipY1 = sc3;
            hitK = 1f; hitOx = 0f; hitOy = 0f;
            entered = false;
        }
        } catch (RuntimeException e) {
            if (entered) {
                pose.popMatrix();
                hitK = 1f; hitOx = 0f; hitOy = 0f;
                clipX0 = x; clipY0 = y; clipX1 = x + w; clipY1 = y + h;
            }
            brokenTabs.add(key);
            com.phantomaddons.PhantomAddons.LOGGER.error("[PV] tab {} failed to render", key, e);
        }
        if (fit != null) {
            es = savedEs;
            fit.update(contentH, vh - 16);
        }
        int max = Math.max(0, contentH - (vh - 16));
        maxScrolls.put(key, max);
        scrolls.put(key, Mth.clamp(scroll, 0, max));

        pose.popMatrix();
        ctx.disableScissor();

        if (fade > 0f) {
            ctx.nextStratum();
            ctx.fill(x, y, x + w, y + h, (C_BG & 0xFFFFFF) | ((int) (fade * 235) << 24));
        }
    }

    // ── Chat cards ────────────────────────────────────────────────────────────

    private boolean card;

    boolean inCard() { return card; }

    void renderCard(GuiGraphicsExtractor ctx, int key, int w, int h, int mx, int my, int tipX, int tipY) {
        Theme.apply(PhantomConfig.getEffectivePvTheme());
        try {
            card = true;
            es = 1f;
            gs = 1f;
            hits.clear();
            hoverStack = null;
            hoverLines = null;
            roundedFill(ctx, 0, 0, w, h, C_BG, 6);
            PvProfileView v = data == null ? null : currentView();
            if (v == null) {
                String msg = error != null ? "§cCouldn't load profile: " + error
                        : data != null && currentViewFailed() ? "§cCouldn't read this profile's data."
                        : "Loading " + requestedName + ".".repeat((int) (System.currentTimeMillis() / 300 % 4));
                ctx.text(font, msg, 8, h / 2 - font.lineHeight / 2, C_TEXT_DIM, darkMode);
                return;
            }
            interactive = mx > -5000;
            renderKey(ctx, key, 0, 0, w, h, 0, 0f, mx, my);
            interactive = false;
            String who = data.name != null ? data.name : requestedName;
            int nameW = font.width(com.phantomaddons.features.customisation.DonatorNames.apply(
                    Component.literal(who).getVisualOrderText(), 0f));
            text(ctx, who, w - 14 - nameW, 8, C_ACCENT, 1f);
            if (mx > -5000) submitTooltip(ctx, tipX, tipY);
        } finally {
            card = false;
            Theme.apply(PhantomConfig.getUiTheme());
        }
    }

    static void renderCardBar(GuiGraphicsExtractor ctx, String who, int w, int h, String page) {
        Theme.apply(PhantomConfig.getEffectivePvTheme());
        try {
            var font = Minecraft.getInstance().font;
            roundedFill(ctx, 0, 0, w, h, C_BG, 3);
            int y = Math.max(0, (h - font.lineHeight) / 2);
            ctx.text(font, "▶", 4, y, C_ACCENT, darkMode);
            ctx.text(font, who + " · " + page + " (double-click to open)", 4 + font.width("▶ "), y, C_TEXT, darkMode);
        } finally {
            Theme.apply(PhantomConfig.getUiTheme());
        }
    }
    
    boolean cardClick(int x, int y) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            if (hits.get(i).contains(x, y)) { hits.get(i).action().run(); return true; }
        }
        return false;
    }

    // ── Keeping one-screen pages on one screen ────────────────────────────────

    private static final java.util.Set<Integer> MEDIUM_LOOK = java.util.Set.of(3, 4);
    private float hitK = 1f, hitOx, hitOy;

    private static final java.util.Set<Integer> FIT_PAGES = java.util.Set.of(0, 2, 3, 4, 8, 9);

    private static final class FitState {
        float scale = 1f;
        int steps;

        void update(int contentH, int avail) {
            if (contentH > avail + 1 && steps < 8 && scale > 0.5f) {
                scale = Math.max(0.5f, scale * avail / (float) contentH * 0.98f);
                steps++;
            }
        }
    }

    private final Map<String, FitState> fitStates = new HashMap<>();

    private FitState fitState(int key, int w, int h) {
        return fitStates.computeIfAbsent(key + ":" + PhantomConfig.getPvContent() + ":" + w + "x" + h, k -> new FitState());
    }

    // ── Shared building blocks for the tab pages ─────────────────────────────

    net.minecraft.client.gui.Font pvFont() { return font; }

    private final Map<Integer, Integer> innerTabs = new HashMap<>();
    private final java.util.Set<Integer> brokenTabs = new java.util.HashSet<>();
    private final Map<String, CompletableFuture<com.google.gson.JsonObject>> gardens = new HashMap<>();

    int innerTab(int key) { return innerTabs.getOrDefault(key, 0); }

    // ── Search fields (one per page key) ──────────────────────────────────────

    private final Map<Integer, String> searches = new HashMap<>();
    private int searchFocus = -1;
    private boolean textInputOn;

    String search(int key) { return searches.getOrDefault(key, ""); }

    int searchBox(GuiGraphicsExtractor ctx, int key, String placeholder, int x, int y, int w, int mx, int my) {
        int h = font.lineHeight + 8;
        boolean focused = searchFocus == key;
        boolean hov = hovered(mx, my, x, y, w, h);
        roundedFill(ctx, x, y, x + w, y + h, overlay(focused ? 0x1C : hov ? 0x14 : 0x0C), h / 2);
        if (focused) roundedFill(ctx, x + 6, y + h - 1, x + w - 6, y + h, C_ACCENT, 1);
        String text = search(key);
        int tx = x + 8, ty = y + (h - font.lineHeight) / 2 + 1;
        if (text.isEmpty() && !focused) {
            ctx.text(font, placeholder, tx, ty, C_TEXT_DIM, darkMode);
        } else {
            String shown = text;
            while (font.width(shown) > w - 20 && shown.length() > 1) shown = shown.substring(1);
            ctx.text(font, shown, tx, ty, C_TEXT, darkMode);
            if (focused && (System.currentTimeMillis() / 500) % 2 == 0)
                ctx.fill(tx + font.width(shown) + 1, ty - 1, tx + font.width(shown) + 2, ty + font.lineHeight, C_TEXT);
        }
        addHit(x, y, x + w, y + h, () -> searchFocus = key);
        return h;
    }

    private void editSearch(String newText) {
        searches.put(searchFocus, newText);
        scrolls.put(searchFocus, 0);
    }
    void setInnerTab(int key, int value) { innerTabs.put(key, value); }

    private final Map<String, CompletableFuture<com.google.gson.JsonObject>> museums = new HashMap<>();

    com.google.gson.JsonObject museum(PvProfileView v) {
        if (data == null) return null;
        CompletableFuture<com.google.gson.JsonObject> f = museums.computeIfAbsent(v.profile.id(), id -> PvFetcher.fetchMuseum(id, data.uuid));
        return f.isDone() && !f.isCompletedExceptionally() ? f.join() : null;
    }

    private double museumValue(PvProfileView v) {
        if (data == null || !data.extras) return -1;
        com.google.gson.JsonObject m = museum(v);
        double value = m != null ? PvJson.num(m, 0, "value") : -1;
        return value > 0 ? value : -1;
    }

    private String museumNote(PvProfileView v) {
        if (data == null || !data.extras) return "The museum isn't included.";
        CompletableFuture<com.google.gson.JsonObject> f = museums.get(v.profile.id());
        if (f == null || !f.isDone()) return "The museum is still loading.";
        return f.isCompletedExceptionally() ? "The museum couldn't be loaded." : "This profile has no museum value.";
    }

    PvNetworth.Result networth(PvProfileView v) {
        return PvNetworth.get(v, v.itemsReady() ? museumValue(v) : -1);
    }

    String museumError(PvProfileView v) {
        CompletableFuture<com.google.gson.JsonObject> f = museums.get(v.profile.id());
        if (f == null || !f.isCompletedExceptionally()) return null;
        try { f.join(); } catch (RuntimeException e) { return rootMessage(e); }
        return null;
    }

    com.google.gson.JsonObject garden(PvProfileView v) {
        CompletableFuture<com.google.gson.JsonObject> f = gardens.computeIfAbsent(v.profile.id(), id -> PvFetcher.fetchGarden(id));
        return f.isDone() && !f.isCompletedExceptionally() ? f.join() : null;
    }

    boolean gardenFailed(PvProfileView v) {
        CompletableFuture<com.google.gson.JsonObject> f = gardens.get(v.profile.id());
        return f != null && f.isCompletedExceptionally();
    }

    String gardenError(PvProfileView v) {
        CompletableFuture<com.google.gson.JsonObject> f = gardens.get(v.profile.id());
        if (f == null || !f.isCompletedExceptionally()) return null;
        try { f.join(); } catch (RuntimeException e) { return rootMessage(e); }
        return null;
    }

    int innerTabsRow(GuiGraphicsExtractor ctx, int key, String[] labels, int x, int y, int w, int mx, int my) {
        int sel = Mth.clamp(innerTab(key), 0, labels.length - 1);
        float ts = 0.9f * Math.min(es, 1.2f);
        int h = lineH(ts) + 8;
        int gap = 4;
        List<int[]> rows = new ArrayList<>(); // {firstIndex, lastIndexExclusive, rowWidth}
        int start = 0, rowW = 0;
        for (int i = 0; i < labels.length; i++) {
            int tw = textW(labels[i], ts) + 14;
            if (i > start && rowW + gap + tw > w) { rows.add(new int[] { start, i, rowW }); start = i; rowW = 0; }
            rowW += (i > start ? gap : 0) + tw;
        }
        rows.add(new int[] { start, labels.length, rowW });

        int ry = y;
        for (int[] row : rows) {
            int cx = x + Math.max(0, (w - row[2]) / 2);
            for (int i = row[0]; i < row[1]; i++) {
                int tw = textW(labels[i], ts) + 14;
                boolean active = i == sel;
                boolean hov = !active && hovered(mx, my, cx, ry, tw, h);
                roundedFill(ctx, cx, ry, cx + tw, ry + h, active ? C_TAB_ACTIVE : hov ? C_TAB_HOVER : overlay(0x0C), 5);
                if (active) roundedFill(ctx, cx + 4, ry + h - 2, cx + tw - 4, ry + h, C_ACCENT, 1);
                text(ctx, labels[i], cx + 7, ry + 4, active ? C_ACCENT : hov ? C_TEXT : C_TEXT_DIM, ts);
                final int idx = i;
                addHit(cx, ry, cx + tw, ry + h, () -> { innerTabs.put(key, idx); scrolls.put(key, 0); });
                cx += tw + gap;
            }
            ry += h + gap;
        }
        return ry - y - gap;
    }

    int pager(GuiGraphicsExtractor ctx, int key, int count, String label, int x, int y, int w, int mx, int my) {
        int cur = Mth.clamp(innerTab(key), 0, Math.max(0, count - 1));
        float ts = Math.min(es, 1.2f);
        int lh = lineH(ts);
        int aw = textW("◀", ts) + 8;
        int lw = textW(label, ts);
        int cx = x + (w - (lw + 2 * aw + 12)) / 2;
        boolean hp = hovered(mx, my, cx, y - 2, aw, lh + 4), hn = hovered(mx, my, cx + aw + lw + 12, y - 2, aw, lh + 4);
        roundedFill(ctx, cx, y - 2, cx + aw, y + lh + 2, hp && cur > 0 ? C_CYCLE_HOVER : C_CYCLE_BG, 4);
        text(ctx, "◀", cx + 4, y, cur > 0 ? C_TEXT : C_TEXT_DIM, ts);
        text(ctx, label, cx + aw + 6, y, C_TEXT, ts);
        int nx = cx + aw + lw + 12;
        roundedFill(ctx, nx, y - 2, nx + aw, y + lh + 2, hn && cur < count - 1 ? C_CYCLE_HOVER : C_CYCLE_BG, 4);
        text(ctx, "▶", nx + 4, y, cur < count - 1 ? C_TEXT : C_TEXT_DIM, ts);
        addHit(cx, y - 2, cx + aw, y + lh + 2, () -> innerTabs.put(key, Math.max(0, cur - 1)));
        addHit(nx, y - 2, nx + aw, y + lh + 2, () -> innerTabs.put(key, Math.min(count - 1, cur + 1)));
        return lh + 8;
    }

    void card(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        roundedFill(ctx, x, y, x + w, y + h, overlay(0x10), 5);
    }

    int title(GuiGraphicsExtractor ctx, String t, int x, int y) {
        text(ctx, t, x, y, C_ACCENT, es);
        return lineH(es) + 6;
    }

    int statRow(GuiGraphicsExtractor ctx, String label, String value, int x, int y, int w, float scale) {
        text(ctx, label, x, y, C_TEXT_DIM, scale);
        text(ctx, value, x + w - textW(value, scale), y, C_TEXT, scale);
        return lineH(scale) + 3;
    }

    void tooltip(String... lines) {
        List<Component> out = new ArrayList<>();
        for (String l : lines) out.add(Component.literal(l));
        hoverLines = out;
    }

    void tooltipLines(List<String> lines) { tooltip(lines.toArray(new String[0])); }

    private static final Map<String, String> ICON_ALIASES = Map.of(
            "PARTY_HAT_SLOTH", "PARTY_HAT_SLOTH_CHEEKY",   // the newest of the sloth hat textures
            "ABICASE", "ABICASE_BLUE_BLUE");               // the blue Abicase

    ItemStack repoIcon(String id, net.minecraft.world.item.Item fallback) {
        PvItems.RepoItem r = PvItems.repoItem(ICON_ALIASES.getOrDefault(id, id));
        return r != null ? r.stack() : new ItemStack(fallback);
    }

    // ── Main page ─────────────────────────────────────────────────────────────

    private int renderMain(GuiGraphicsExtractor ctx, PvProfileView v, int x, int y, int w, int h, int mx, int my, int scroll) {
        int pad = 8;
        int mw = Mth.clamp((int) (w * 0.32f), 80, 180);
        int bx0 = x + pad, by0 = y + pad, bx1 = bx0 + mw, by1 = y + h - pad;
        roundedFill(ctx, bx0, by0, bx1, by1, overlay(0x10), 5);

        String lvl = levelTag(v.skyblockLevel) + " ";
        float ns = Math.min(es, 1.2f);
        int nw = textW(lvl, ns) + textW(data.name, ns);
        int nx = bx0 + (mw - nw) / 2;
        text(ctx, lvl, nx, by0 + 5, 0xFFFFFFFF, ns);
        text(ctx, data.name, nx + textW(lvl, ns), by0 + 5, C_TEXT, ns);
        PvData.Profile shownProfile = data.profiles.get(Math.min(profileIdx, data.profiles.size() - 1));
        if (hovered(mx, my, nx, by0 + 3, nw, lineH(ns) + 4))
            tooltip(PvNames.coopTooltip(shownProfile.coop()));

        float ws = 0.8f * Math.min(es, 1.2f);
        int worthY = by0 + 5 + lineH(ns) + 2;
        PvNetworth.Result worth = networth(v);
        String worthText = "§7Networth: " + (worth != null ? "§6" + fmt(worth.total()) : PvNetworth.status(v));
        text(ctx, worthText, bx0 + (mw - textW(worthText, ws)) / 2, worthY, C_TEXT, ws);
        if (worth != null && hovered(mx, my, bx0, worthY - 2, mw, lineH(ws) + 4)) tooltipLines(PvNetworth.tooltip(worth, museumNote(v)));

        float hs = 0.7f * Math.min(es, 1.2f);
        String hint = "Drag to rotate";
        text(ctx, hint, bx0 + (mw - textW(hint, hs)) / 2, by1 - Math.round(font.lineHeight * hs) - 4, C_TEXT_DIM, hs);

        int modelTop = worthY + lineH(ws) + 8;
        int modelBot = by1 - Math.round(font.lineHeight * hs) - 8;
        int drop = Math.round((modelBot - modelTop) * 0.08f);
        if (model != null && modelBot - modelTop > 20) model.render(ctx, phys(bx0 + 2), phys(modelTop + drop), phys(bx1 - 2), phys(modelBot + drop), partialTick);
        if (interactive) modelRect = new int[] { bx0, by0, bx1, by1 };

        // ── Right column ──
        int rx = bx1 + pad, rw = x + w - pad - rx;
        ctx.enableScissor(rx, y, x + w, y + h);
        int yy = y + pad - scroll;

        String bankStr = v.bank >= 0 ? fmt(v.bank) : "API off";
        String[][] cards = {
                { "Purse", v.purse >= 0 ? fmt(v.purse) : "—" },
                { "Bank", bankStr },
                { "Skill Average", v.skillsApi ? String.format(Locale.ROOT, "%.2f", v.skillAverage) : "—" },
                { "Fairy Souls", v.fairySouls >= 0 ? String.valueOf(v.fairySouls) : "—" },
                { "Joined", v.firstJoin > 0 ? ago(v.firstJoin) : "—" },
                { "SkyBlock Level", String.valueOf(v.skyblockLevel) },
        };
        int gap = 4, cols = 3;
        int cardW = (rw - gap * (cols - 1)) / cols;
        float ls = 0.75f * es;
        int cardH = Math.round(font.lineHeight * ls) + Math.round(font.lineHeight * es) + 12;
        for (int i = 0; i < cards.length; i++) {
            int cx = rx + (i % cols) * (cardW + gap), cy = yy + (i / cols) * (cardH + gap);
            roundedFill(ctx, cx, cy, cx + cardW, cy + cardH, overlay(0x10), 4);
            text(ctx, cards[i][0], cx + 6, cy + 4, C_TEXT_DIM, ls);
            text(ctx, cards[i][1], cx + 6, cy + 6 + Math.round(font.lineHeight * ls), C_TEXT, es);
            if (i == 1 && hovered(mx, my, cx, cy, cardW, cardH)) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal("§6Bank"));
                lines.add(Component.literal("§7Profile bank: §6" + (v.bank >= 0 ? fmtFull(v.bank) : "API off")));
                if (v.personalBank >= 0) lines.add(Component.literal("§7Personal bank: §6" + fmtFull(v.personalBank)));
                hoverLines = lines;
            }
        }
        yy += 2 * (cardH + gap) + 6;

        text(ctx, "Skills", rx, yy, C_ACCENT, es);
        yy += Math.round(font.lineHeight * es) + 5;
        if (!v.skillsApi) {
            text(ctx, "This player has their Skills API disabled.", rx, yy, C_TEXT_DIM, 0.85f * es);
            yy += Math.round(font.lineHeight * es) + 4;
        } else {
            int colW = (rw - gap) / 2;
            int barH = Math.max(3, Math.round(5 * es));
            int rowH = Math.round(font.lineHeight * es) + barH + 8;
            for (int i = 0; i < v.skills.size(); i++) {
                PvProfileView.Skill s = v.skills.get(i);
                int sx = rx + (i % 2) * (colW + gap), sy = yy + (i / 2) * rowH;
                PvLeveling.Level l = s.shown();
                boolean over = l != s.level();
                boolean max = s.atMax() || over;
                text(ctx, s.name(), sx, sy, C_TEXT, es);
                String lv = String.valueOf(l.level());
                text(ctx, lv, sx + colW - textW(lv, es), sy, max ? C_ACCENT : C_TEXT, es);
                int by = sy + Math.round(font.lineHeight * es) + 2;
                roundedFill(ctx, sx, by, sx + colW, by + barH, C_SLIDER_BG, barH / 2);
                // Gold at the skill's true max; a full blue bar when only the player's own cap holds them back.
                float prog = max ? (over ? (float) l.progress() : 1f) : s.capBlocked() ? 1f : (float) l.progress();
                int fw = Math.round(colW * Mth.clamp(prog, 0, 1));
                int barColor = max ? C_ACCENT : s.capBlocked() ? 0xFF4F9DFF : C_SLIDER_GR;
                if (fw > 0) roundedFill(ctx, sx, by, sx + fw, by + barH, barColor, barH / 2);
                if (hovered(mx, my, sx, sy, colW, rowH - 2)) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.literal("§a" + s.name() + " " + l.level() + " §8/ " + s.max()));
                    if (over) lines.add(Component.literal("§7Overflow Level: §6" + l.level()));
                    if (s.capBlocked()) {
                        lines.add(Component.literal("§9Level cap reached §8(" + s.level().cap() + ")"));
                        lines.add(Component.literal("§7Enough XP for the next level once more"));
                        lines.add(Component.literal("§7levels are unlocked."));
                    } else lines.add(Component.literal(s.atMax() && !over ? "§6MAX LEVEL"
                            : "§7Progress: §e" + fmt(l.into()) + "§6/§e" + fmt(l.needed())
                              + " §8(" + String.format(Locale.ROOT, "%.1f", l.progress() * 100) + "%)"));
                    lines.add(Component.literal("§7Total XP: §e" + fmtFull(s.xp())));
                    hoverLines = lines;
                }
            }
            yy += ((v.skills.size() + 1) / 2) * rowH;
        }
        ctx.disableScissor();
        return yy + scroll - (y + pad);
    }

    // ── Gear: sizing ──────────────────────────────────────────────────────────

    float room() {
        if (hitK != 1f || card) return 1f; // a page drawn at the Medium size has Medium room
        return Math.max(1f, Math.min(panelW / (float) BASE_W, panelH / (float) BASE_H));
    }

    int maxSlot() { return Math.max(14, Math.round(34 * es * room())); }
    static int gapFor(int s) { return Math.max(1, Math.round(s / 9f)); }
    int fitSlot(double byW, double byH) {
        return Mth.clamp((int) Math.floor(Math.min(byW, byH)), 10, maxSlot());
    }
    int lineH(float scale) { return Math.round(font.lineHeight * scale); }

    // ── Gear: inventory ───────────────────────────────────────────────────────

    private int renderInventory(GuiGraphicsExtractor ctx, ItemStack[] armor, ItemStack[] equipment, List<ItemStack> inv,
                                boolean apiOn, int x, int y, int w, int h, int mx, int my, int scroll) {
        int pad = 10;
        float ls = 0.75f * es;
        int labelH = lineH(ls) + 3;
        int wideS = fitSlot((w - 2 * pad) / 12.61, (h - 2 * pad - labelH) / 4.58);
        int stackS = fitSlot((w - 2 * pad) / 9.89, (h - 2 * pad - 2 * labelH - 6) / 5.7);
        boolean stacked = stackS > wideS;
        int s = stacked ? stackS : wideS;
        int g = gapFor(s), spacer = s / 2, hotGap = Math.max(2, s / 4);

        int totalW = stacked ? 9 * s + 8 * g : 11 * s + 10 * g + spacer;
        int totalH = stacked ? labelH + 4 * s + 3 * g + hotGap + 6 + labelH + s : labelH + 4 * s + 3 * g + hotGap;
        int x0 = x + Math.max(pad, (w - totalW) / 2);
        int y0 = y + Math.max(pad, (h - totalH) / 2) - scroll;

        int ix, iy;
        if (stacked) {
            ix = x0;
            iy = y0;
        } else {
            text(ctx, "Armor", x0, y0, C_TEXT_DIM, ls);
            text(ctx, "Equip", x0 + s + g, y0, C_TEXT_DIM, ls);
            for (int i = 0; i < 4; i++) {
                slot(ctx, x0, y0 + labelH + i * (s + g), s, armor[i], mx, my);
                slot(ctx, x0 + s + g, y0 + labelH + i * (s + g), s, equipment[i], mx, my);
            }
            ix = x0 + 2 * (s + g) + spacer;
            iy = y0;
        }

        text(ctx, "Inventory", ix, iy, C_TEXT_DIM, ls);
        if (!apiOn) {
            text(ctx, "Inventory API disabled", ix, iy + labelH + 4, C_TEXT_DIM, 0.85f * es);
            return totalH;
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = 9 + row * 9 + col;
                slot(ctx, ix + col * (s + g), iy + labelH + row * (s + g), s, idx < inv.size() ? inv.get(idx) : ItemStack.EMPTY, mx, my);
            }
        }
        int hotY = iy + labelH + 3 * (s + g) + hotGap - g;
        for (int col = 0; col < 9; col++) {
            slot(ctx, ix + col * (s + g), hotY, s, col < inv.size() ? inv.get(col) : ItemStack.EMPTY, mx, my);
        }

        if (stacked) {
            int ay = hotY + s + 6;
            text(ctx, "Armor", x0, ay, C_TEXT_DIM, ls);
            text(ctx, "Equip", x0 + 4 * (s + g) + spacer, ay, C_TEXT_DIM, ls);
            for (int i = 0; i < 4; i++) {
                slot(ctx, x0 + i * (s + g), ay + labelH, s, armor[i], mx, my);
                slot(ctx, x0 + 4 * (s + g) + spacer + i * (s + g), ay + labelH, s, equipment[i], mx, my);
            }
        }
        return totalH;
    }
    // ── Gear: loadouts ────────────────────────────────────────────────────────

    private int renderLoadouts(GuiGraphicsExtractor ctx, PvProfileView v, int x, int y, int w, int h,
                               int mx, int my, int scroll) {
        int pad = 10;
        int x0 = x + pad, y0 = y + pad - scroll;
        int lh = lineH(0.85f * es) + 3;

        text(ctx, "Loadouts §7(" + v.loadouts.size() + ")", x0, y0, C_ACCENT, es);
        String current = "§7Current power: §r" + powerName(v.selectedPower)
                + "  §7MP: §r" + (v.magicalPower >= 0 ? v.magicalPower : "—");
        float cs = 0.85f * es;
        text(ctx, current, x + w - pad - textW(current, cs), y0 + 1, C_TEXT, cs);
        int cy = y0 + lineH(es) + 8;

        int avail = w - 2 * pad;
        int cardGap = Math.round(5 * es);
        int cols = cardSlotSize(avail / 2 - cardGap) >= Math.round(14 * es) ? 2 : 1;
        int cardW = (avail - (cols - 1) * cardGap) / cols;

        if (v.loadouts.isEmpty()) {
            return cy + loadoutCard(ctx, x0, cy, cardW, "Current Setup", true, 0, 0, v.armor, v.equipment,
                    v.activePet, v.selectedPower, v.tuning, mx, my) - y0;
        }
        int rowH = 0;
        for (int i = 0; i < v.loadouts.size(); i++) {
            PvProfileView.Loadout lo = v.loadouts.get(i);
            PvProfileView.WardrobeSet armorSet = v.armorSet(lo.armorSet());
            PvProfileView.WardrobeSet equipSet = v.equipmentSet(lo.equipmentSet());
            int col = i % cols;
            int ch = loadoutCard(ctx, x0 + col * (cardW + cardGap), cy, cardW, lo.name(), lo.active(), lo.hotmPreset(), lo.hotfPreset(),
                    armorSet != null ? armorSet.items() : null,
                    equipSet != null ? equipSet.items() : null,
                    lo.pet(), lo.powerStone(), lo.tuning(), mx, my);
            rowH = Math.max(rowH, ch);
            if (col == cols - 1 || i == v.loadouts.size() - 1) { cy += rowH + cardGap; rowH = 0; }
        }
        return cy - y0;
    }

    int cardSlotSize(int cardW) {
        int ip = Math.round(6 * es);
        int groupGap = Math.round(6 * es);
        return Mth.clamp((int) ((cardW - 2 * ip - 3 * groupGap) / 12.1), 10, Math.max(14, Math.round(26 * es * room())));
    }

    int loadoutCard(GuiGraphicsExtractor ctx, int x0, int cy, int cw, String title, boolean active, int hotmPreset, int hotfPreset,
                            ItemStack[] armor, ItemStack[] equip, PvProfileView.Pet pet, String power,
                            Map<String, Integer> tuning, int mx, int my) {
        int s = cardSlotSize(cw), g = gapFor(s);
        int ip = Math.round(6 * es);
        int groupGap = Math.round(6 * es);
        float ts = 0.9f * es, is = 0.8f * es;
        int titleH = lineH(ts);
        int h = ip + titleH + 4 + s + 5 + lineH(is) + ip;

        roundedFill(ctx, x0, cy, x0 + cw, cy + h, overlay(active ? 0x18 : 0x0C), 5);
        if (active) ctx.fill(x0, cy + 3, x0 + 2, cy + h - 3, C_ON);

        text(ctx, title, x0 + ip, cy + ip, C_TEXT, ts);
        if (active) text(ctx, "Active", x0 + ip + textW(title, ts) + 6, cy + ip, C_ON, ts);

        int sy = cy + ip + titleH + 4;
        int sx = x0 + ip;
        for (int i = 0; i < 4; i++)
            slot(ctx, sx + i * (s + g), sy, s, armor != null && i < armor.length ? armor[i] : ItemStack.EMPTY, mx, my);
        sx += 4 * (s + g) + groupGap;
        for (int i = 0; i < 4; i++)
            slot(ctx, sx + i * (s + g), sy, s, equip != null && i < equip.length ? equip[i] : ItemStack.EMPTY, mx, my);
        sx += 4 * (s + g) + groupGap;
        if (pet != null) petSlot(ctx, sx, sy, s, pet, mx, my);
        else slot(ctx, sx, sy, s, ItemStack.EMPTY, mx, my);

        int bx = x0 + cw - ip - s;
        treeButton(ctx, bx, sy, s, 1, title, hotfPreset, mx, my);
        treeButton(ctx, bx - s - g - 2, sy, s, 0, title, hotmPreset, mx, my);

        String info = "§7Power: §r" + powerName(power) + "   §7Tuning: §r" + (tuning.isEmpty() ? "None" : tuningString(tuning));
        text(ctx, info, x0 + ip, sy + s + 5, C_TEXT, is);
        return h;
    }

    private int overlayPreset = 0;

    private void treeButton(GuiGraphicsExtractor ctx, int x, int y, int s, int which, String loadoutName, int preset, int mx, int my) {
        boolean hov = hovered(mx, my, x, y, s, s);
        String treeName = which == 0 ? "§5Heart of the Mountain" : "§2Heart of the Forest";
        if (preset < 0) {
            roundedFill(ctx, x, y, x + s, y + s, C_CYCLE_BG, 4);
            item(ctx, new ItemStack(which == 0 ? Items.DIAMOND_PICKAXE : Items.OAK_SAPLING), x + 1, y + 1, s - 2);
            ctx.nextStratum();
            roundedFill(ctx, x, y, x + s, y + s, darkMode ? 0xB0101014 : 0xB0E8E8E8, 4);
            if (hov) hoverLines = List.of(Component.literal(treeName), Component.literal("§7" + loadoutName),
                    Component.literal("§8No tree preset set for this loadout"));
            return;
        }
        roundedFill(ctx, x, y, x + s, y + s, hov ? C_CYCLE_HOVER : C_CYCLE_BG, 4);
        item(ctx, new ItemStack(which == 0 ? Items.DIAMOND_PICKAXE : Items.OAK_SAPLING), x + 1, y + 1, s - 2);
        if (preset > 0) {
            float ps = Math.max(0.5f, s / 30f);
            ctx.nextStratum();
            text(ctx, String.valueOf(preset), x + s - textW(String.valueOf(preset), ps) - 1, y + s - lineH(ps), 0xFFFFFFFF, ps);
        }
        addHit(x, y, x + s, y + s, () -> { overlay = which; overlayScroll = 0; overlayTitle = loadoutName; overlayPreset = preset; });
        if (hov) hoverLines = List.of(
                Component.literal(which == 0 ? "§5Heart of the Mountain" : "§2Heart of the Forest"),
                Component.literal("§7" + loadoutName + (preset > 0 ? " §8— Preset " + preset : "")),
                Component.literal("§eClick to view tree"));
    }

    static String tuningString(Map<String, Integer> tuning) {
        StringBuilder sb = new StringBuilder();
        for (var e : tuning.entrySet()) {
            String sym = switch (e.getKey()) {
                case "health" -> "§c❤";
                case "defense" -> "§a❈";
                case "walk_speed" -> "§f✦";
                case "strength" -> "§c❁";
                case "critical_damage" -> "§9☠";
                case "critical_chance" -> "§9☣";
                case "attack_speed" -> "§e⚔";
                case "intelligence" -> "§b✎";
                default -> "§7" + e.getKey();
            };
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(sym, 0, 2).append(e.getValue()).append(sym.substring(2));
        }
        return sb.toString();
    }

    static String powerName(String power) {
        return power != null ? PvProfileView.prettify(power) : "None";
    }

    // ── Gear: wardrobes ───────────────────────────────────────────────────────

    private int renderWardrobe(GuiGraphicsExtractor ctx, List<PvProfileView.WardrobeSet> sets, int equipped,
                               boolean armor, int x, int y, int w, int h, int mx, int my, int scroll) {
        int pad = 10;
        float ls = 0.75f * es;
        int y0 = y + pad - scroll;
        String title = armor ? "Wardrobe" : "Equipment Wardrobe";
        text(ctx, title + " §7(" + sets.size() + ")", x + pad, y0, C_ACCENT, es);
        if (sets.isEmpty()) {
            text(ctx, armor ? "No wardrobe data for this profile." : "No equipment wardrobe data for this profile.",
                    x + pad, y0 + lineH(es) + 8, C_TEXT_DIM, 0.85f * es);
            return lineH(es) * 2 + 8;
        }

        int headerH = lineH(es) + 8;
        int labelH = lineH(ls) + 3;
        int rowGap = Math.round(6 * es);
        int availW = w - 2 * pad, availH = h - 2 * pad - headerH;
        int n = sets.size();
        int bestCols = 1, bestS = 0;
        for (int c = 1; c <= n; c++) {
            int rows = (n + c - 1) / c;
            double byW = availW / (c * 1.25 - 0.25);
            double byH = (availH - rows * (labelH + rowGap)) / (rows * 4.34);
            int s = fitSlot(byW, byH);
            if (s > bestS || (s == bestS && c < bestCols)) { bestS = s; bestCols = c; }
        }
        int cols = bestCols, s = bestS, g = gapFor(s);
        int colW = s + Math.max(3, s / 4);
        int setH = labelH + 4 * s + 3 * g;
        int rows = (n + cols - 1) / cols;
        int gridW = cols * colW - (colW - s);
        int gridH = rows * (setH + rowGap) - rowGap;
        int gx = x + Math.max(pad, (w - gridW) / 2);
        int gy = y0 + headerH + Math.max(0, (availH - gridH) / 2);

        for (int i = 0; i < n; i++) {
            PvProfileView.WardrobeSet set = sets.get(i);
            int cx = gx + (i % cols) * colW;
            int ry = gy + (i / cols) * (setH + rowGap);
            String num = String.valueOf(set.slotNumber());
            boolean isEquipped = set.slotNumber() == equipped;
            text(ctx, num, cx + (s - textW(num, ls)) / 2, ry, isEquipped ? C_ON : C_TEXT_DIM, ls);
            if (isEquipped) {
                roundedFill(ctx, cx - 2, ry + labelH - 2, cx + s + 2, ry + labelH + 4 * s + 3 * g + 2, C_ON, 4);
            }
            for (int r = 0; r < Math.min(4, set.items().length); r++) {
                slot(ctx, cx, ry + labelH + r * (s + g), s, set.items()[r], mx, my);
            }
        }
        return headerH + gridH;
    }

    // ── Gear: accessories ─────────────────────────────────────────────────────

    private int renderAccessories(GuiGraphicsExtractor ctx, PvProfileView v, int x, int y, int w, int h,
                                  int mx, int my, int scroll) {
        int pad = 10;
        int y0 = y + pad - scroll;
        text(ctx, "Accessories §7(" + v.accessories.size() + ")", x + pad, y0, C_ACCENT, es);
        String info = "§7MP: §r" + (v.magicalPower >= 0 ? v.magicalPower : "—")
                + "  §7Power: §r" + powerName(v.selectedPower);
        float is = 0.85f * es;
        text(ctx, info, x + w - pad - textW(info, is), y0 + 1, C_TEXT, is);

        int gy = y0 + lineH(es) + 8;
        if (v.accessories.isEmpty()) {
            text(ctx, v.inventoryApi ? "No accessories." : "Inventory API disabled.", x + pad, gy, C_TEXT_DIM, 0.85f * es);
            return gy - y0 + lineH(es);
        }
        int[] grid = fillGrid(w - 2 * pad);
        int cols = grid[0], s = grid[1], g = gapFor(s);
        int x0 = x + (w - (cols * (s + g) - g)) / 2;
        java.util.Set<String> recombobulated = new java.util.HashSet<>();
        for (ItemStack a : v.accessories) if (PvGear.data(a).getIntOr("rarity_upgrades", 0) > 0) recombobulated.add(PvGear.id(a));
        for (int i = 0; i < v.accessories.size(); i++) {
            int rgb = v.accessoryColors.get(i);
            ItemStack acc = v.accessories.get(i);
            boolean dim = canRecombobulate(acc, rgb) && !recombobulated.contains(PvGear.id(acc));
            raritySlot(ctx, x0 + (i % cols) * (s + g), gy + (i / cols) * (s + g), s, acc, rgb, dim, mx, my);
        }
        int rows = (v.accessories.size() + cols - 1) / cols;
        int yy = gy + rows * (s + g) + 10;
        yy += renderInactiveAccessories(ctx, v, x + pad, x0, cols, yy, s, mx, my);
        yy += renderMissingAccessories(ctx, v, x + pad, x0, cols, yy, w - 2 * pad, s, mx, my);
        return yy - y0;
    }

    private int renderInactiveAccessories(GuiGraphicsExtractor ctx, PvProfileView v, int x, int gridX, int cols, int y,
                                          int s, int mx, int my) {
        List<PvInactiveAccessories.Entry> list = PvInactiveAccessories.get(v);
        if (list == null) return 0;
        text(ctx, "Not Active §7(" + list.size() + ")", x, y, C_ACCENT, es);
        float ls = 0.8f * es;
        int gy = y + lineH(es) + 4, g = gapFor(s);
        if (list.isEmpty()) {
            text(ctx, "§8None", x, gy, C_TEXT, ls);
            return gy - y + lineH(ls) + 10;
        }
        for (int i = 0; i < list.size(); i++) {
            PvInactiveAccessories.Entry e = list.get(i);
            int sx = gridX + (i % cols) * (s + g), sy = gy + (i / cols) * (s + g);
            if (offscreen(sy, s)) continue;
            // A gold edge marks one that outranks the accessory currently counting: it needs moving up the bag.
            if (e.better()) roundedFill(ctx, sx - 1, sy - 1, sx + s + 1, sy + s + 1, 0xFFFFAA00, 4);
            rarityBg(ctx, sx, sy, s, PvItems.rarityColor(e.stack()));
            item(ctx, e.stack(), sx + 1, sy + 1, s - 2);
            if (hovered(mx, my, sx, sy, s, s)) {
                ctx.nextStratum();
                roundedFill(ctx, sx, sy, sx + s, sy + s, overlay(0x28), 3);
                tooltip("§f" + e.stack().getHoverName().getString(), e.better() ? "§6Not active, but it should be" : "§cNot active",
                        "§7" + e.reason(), "§7Counting: §f" + e.active().getHoverName().getString(),
                        e.better() ? "§8Move it above the one counting." : "§8Safe to remove or leave where it is.");
            }
        }
        int rows = (list.size() + cols - 1) / cols;
        return gy - y + rows * (s + g) + 10;
    }

    private static final int[] NO_RECOMB = { 0xFF55FF, 0x55FFFF, 0xAA0000 };

    static boolean canRecombobulate(ItemStack stack, int rarityRgb) {
        if (rarityRgb < 0 || PvGear.data(stack).getIntOr("rarity_upgrades", 0) > 0) return false;
        for (int blocked : NO_RECOMB) if (rarityRgb == blocked) return false;
        if (PvConstants.noRecombobulator().contains(PvGear.id(stack))) return false;
        for (String line : PvGear.lore(stack)) if (line.contains("VERY SPECIAL")) return false;
        return true;
    }

    private int renderMissingAccessories(GuiGraphicsExtractor ctx, PvProfileView v, int x, int gridX, int cols, int y,
                                         int w, int s, int mx, int my) {
        int yy = y;
        PvMissingAccessories.Result res = PvMissingAccessories.get(v);
        text(ctx, "Missing Accessories", x, yy, C_ACCENT, es);
        float is = 0.85f * es;
        if (res == null) {
            text(ctx, "§8Loading accessory list...", x + w - textW("§8Loading accessory list...", is), yy + 1, C_TEXT, is);
            return lineH(es);
        }
        if (!res.pricesLoaded()) {
            String loading = "§8loading prices...";
            text(ctx, loading, x + w - textW(loading, is), yy + 1, C_TEXT, is);
        }
        yy += lineH(es) + 6;
        yy += missingGrid(ctx, "Upgrades §7(" + res.upgrades().size() + ")", res.upgrades(), x, gridX, cols, yy, s, mx, my);
        yy += 6;
        yy += missingGrid(ctx, "Missing §7(" + res.missing().size() + ")", res.missing(), x, gridX, cols, yy, s, mx, my);
        return yy - y;
    }

    private int missingGrid(GuiGraphicsExtractor ctx, String label, List<PvMissingAccessories.Entry> entries, int x, int gridX,
                            int cols, int y, int s, int mx, int my) {
        float ls = 0.8f * es;
        text(ctx, label, x, y, C_TEXT, ls);
        int gy = y + lineH(ls) + 4, g = gapFor(s);
        if (entries.isEmpty()) {
            text(ctx, "§8None", x, gy, C_TEXT, ls);
            return gy - y + lineH(ls);
        }
        for (int i = 0; i < entries.size(); i++) {
            PvMissingAccessories.Entry e = entries.get(i);
            int sx = gridX + (i % cols) * (s + g), sy = gy + (i / cols) * (s + g);
            if (offscreen(sy, s)) continue;
            rarityBg(ctx, sx, sy, s, rarityRgb(e.tier()));
            item(ctx, repoIcon(e.id(), Items.BARRIER), sx + 1, sy + 1, s - 2);
            if (e.count() > 0) {
                ctx.nextStratum();
                float cs = 0.75f * es;
                String n = String.valueOf(e.count());
                text(ctx, n, sx + s - textW(n, cs) - 1, sy + s - lineH(cs), C_TEXT, cs);
            }
            if (hovered(mx, my, sx, sy, s, s)) {
                ctx.nextStratum();
                roundedFill(ctx, sx, sy, sx + s, sy + s, overlay(0x28), 3);
                List<String> lines = new ArrayList<>();
                lines.add(PvProfileView.rarityColor(e.tier()) + e.name());
                if (e.count() > 0) {
                    lines.add("§7" + e.count() + " " + PvProfileView.rarityColor(e.tier()) + e.tier().replace('_', ' ')
                            + " §7accessor" + (e.count() == 1 ? "y" : "ies") + " to recombobulate");
                } else {
                    lines.add(PvProfileView.rarityColor(e.tier()) + "§l" + e.tier().replace('_', ' '));
                }
                lines.add("");
                if (e.from() != null) lines.add("§7Upgrades: §f" + e.from());
                lines.add("§7Magical Power: §a+" + e.mpGain() + (e.recomb() ? " §8(with a recombobulator)" : ""));
                if (e.price() > 0) {
                    lines.add("§7Price: §6" + PvPages.shortNum(e.price()));
                    if (e.mpGain() > 0) lines.add("§7Per MP: §6" + PvPages.shortNum(e.price() / e.mpGain()));
                } else {
                    lines.add("§8No price data");
                }
                tooltipLines(lines);
            }
        }
        return gy - y + ((entries.size() + cols - 1) / cols) * (s + g);
    }

    int[] fillGrid(int avail) {
        int target = Math.max(12, Math.round(24 * es));
        int tg = gapFor(target);
        int cols = Math.max(1, (avail + tg) / (target + tg));
        int s = target;
        for (int i = 0; i < 3; i++) {
            int g = gapFor(s);
            s = Math.max(10, (avail - (cols - 1) * g) / cols);
        }
        return new int[] { cols, s };
    }

    // ── Gear: pets ────────────────────────────────────────────────────────────

    private int renderPets(GuiGraphicsExtractor ctx, PvProfileView v, int x, int y, int w, int h,
                           int mx, int my, int scroll) {
        int pad = 10;
        int y0 = y + pad - scroll;
        text(ctx, "Pets §7(" + v.pets.size() + ")", x + pad, y0, C_ACCENT, es);
        if (v.activePet != null) {
            PvProfileView.Pet p = v.activePet;
            String a = "§7Active: " + PvProfileView.rarityColor(p.rarity()) + "[Lvl " + p.level().level() + "] " + p.name();
            float as = 0.85f * es;
            text(ctx, a, x + w - pad - textW(a, as), y0 + 1, C_TEXT, as);
        }
        int gy = y0 + lineH(es) + 8;
        if (v.pets.isEmpty()) {
            text(ctx, "No pets.", x + pad, gy, C_TEXT_DIM, 0.85f * es);
            return gy - y0 + lineH(es);
        }
        int[] grid = fillGrid(w - 2 * pad);
        int cols = grid[0], s = grid[1], g = gapFor(s);
        int x0 = x + (w - (cols * (s + g) - g)) / 2;
        for (int i = 0; i < v.pets.size(); i++) {
            petSlot(ctx, x0 + (i % cols) * (s + g), gy + (i / cols) * (s + g), s, v.pets.get(i), mx, my);
        }
        int rows = (v.pets.size() + cols - 1) / cols;
        return gy + rows * (s + g) - y0;
    }

    void petSlot(GuiGraphicsExtractor ctx, int x, int y, int s, PvProfileView.Pet pet, int mx, int my) {
        if (offscreen(y, s)) return;
        if (pet.active()) roundedFill(ctx, x - 1, y - 1, x + s + 1, y + s + 1, C_ON, 4);
        rarityBg(ctx, x, y, s, rarityRgb(pet.rarity()));
        item(ctx, petIcon(pet), x + 1, y + 1, s - 2);

        PvItems.RepoItem held = pet.heldItem() != null ? PvItems.repoItem(pet.heldItem()) : null;
        if (held != null) {
            int hs = Math.max(6, s / 2);
            ctx.nextStratum();
            item(ctx, held.stack(), x, y + s - hs, hs);
        }
        float ls = Math.max(0.5f, s / 30f);
        ctx.nextStratum();
        text(ctx, String.valueOf(pet.level().level()), x + 2, y + 1, 0xFFFFFFFF, ls);

        if (hovered(mx, my, x, y, s, s)) {
            roundedFill(ctx, x, y, x + s, y + s, overlay(0x28), 3);
            hoverLines = petTooltip(pet, held);
        }
    }

    private final Map<PvProfileView.Pet, ItemStack> petIcons = new java.util.IdentityHashMap<>();

    ItemStack petIcon(PvProfileView.Pet pet) {
        ItemStack cached = petIcons.get(pet);
        if (cached != null) return cached;
        int idx = PvProfileView.rarityIndex(pet.rarity());
        for (int r : new int[] { idx, 4, 3, 5, 2, 1, 0 }) {
            String id = pet.type() + ";" + r;
            PvItems.RepoItem item = PvItems.repoItem(id);
            if (item != null) {
                ItemStack icon = PvItems.withCustomData(item.stack(), petData(pet));
                icon.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(
                        "§7[Lvl " + pet.level().level() + "] " + PvProfileView.rarityColor(pet.rarity()) + pet.name()));
                petIcons.put(pet, icon);
                return icon;
            }
            if (!PvItems.repoMissing(id)) break;
        }
        return new ItemStack(Items.BONE);
    }

    private static net.minecraft.nbt.CompoundTag petData(PvProfileView.Pet pet) {
        com.google.gson.JsonObject info = new com.google.gson.JsonObject();
        info.addProperty("type", pet.type());
        info.addProperty("active", pet.active());
        info.addProperty("exp", pet.exp());
        info.addProperty("tier", pet.rarity());
        info.addProperty("hideInfo", false);
        if (pet.heldItem() != null) info.addProperty("heldItem", pet.heldItem());
        info.addProperty("candyUsed", pet.candyUsed());
        if (pet.skin() != null) info.addProperty("skin", pet.skin());
        if (pet.uniqueId() != null) info.addProperty("uuid", pet.uniqueId());
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("id", "PET");
        tag.putString("petInfo", info.toString());
        return tag;
    }

    private static List<Component> petTooltip(PvProfileView.Pet pet, PvItems.RepoItem held) {
        String c = PvProfileView.rarityColor(pet.rarity());
        PvLeveling.Level l = pet.level();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§7[Lvl " + l.level() + "] " + c + pet.name()));
        lines.add(Component.literal(c + "§l" + pet.rarity()));
        lines.add(Component.empty());
        lines.add(Component.literal(l.maxed() ? "§6MAX LEVEL"
                : "§7Progress to Level " + (l.level() + 1) + ": §e" + String.format(Locale.ROOT, "%.1f", l.progress() * 100) + "%"));
        if (!l.maxed()) lines.add(Component.literal("§e" + fmt(l.into()) + "§6/§e" + fmt(l.needed()) + " XP"));
        lines.add(Component.literal("§7Total XP: §e" + fmtFull(pet.exp())));
        if (pet.heldItem() != null) {
            lines.add(Component.literal("§7Held Item: " + (held != null ? held.displayName() : "§f" + PvProfileView.prettify(pet.heldItem()))));
        }
        if (pet.candyUsed() > 0) lines.add(Component.literal("§7Candy Used: §a" + pet.candyUsed() + "§7/10"));
        if (pet.skin() != null) lines.add(Component.literal("§7Skin: §d" + PvProfileView.prettify(pet.skin())));
        if (pet.active()) { lines.add(Component.empty()); lines.add(Component.literal("§aActive pet")); }
        return lines;
    }

    // ── Profile dropdown ──────────────────────────────────────────────────────

    private boolean recentOpen;
    private float recentAnim;
    private int[] recentListRect;

    private void renderRecentList(GuiGraphicsExtractor ctx, List<PvFetcher.Recent> recent, int px, int by, int tagW) {
        recentListRect = null;
        if (recentAnim <= 0f || recent.isEmpty()) return;
        int n = recent.size();
        int listW = tagW;
        for (PvFetcher.Recent r : recent) listW = Math.max(listW, font.width(r.name()) + font.width("00m ago") + 30);
        int listH = n * DROP_ROW_H + 4;
        boolean below = by + DROP_H + 3 + listH <= lhgt - 2;
        int ly = below ? by + DROP_H + 3 : by - 3 - listH;
        int shown = Math.round(listH * ease(recentAnim));
        int mx = lastMouseX, my = lastMouseY;

        ctx.nextStratum();
        int clipTop = below ? ly : ly + listH - shown;
        ctx.enableScissor(px - 1, clipTop - 1, px + listW + 1, clipTop + shown + 1);
        roundedFill(ctx, px - 1, ly - 1, px + listW + 1, ly + listH + 1, C_BORDER, 6);
        roundedFill(ctx, px, ly, px + listW, ly + listH, withAlpha(C_SIDEBAR, 250), 5);
        for (int i = 0; i < n; i++) {
            PvFetcher.Recent r = recent.get(i);
            int ry = ly + 2 + i * DROP_ROW_H;
            boolean current = data != null && data.uuid.equals(r.uuid());
            boolean rh = mx >= px && mx < px + listW && my >= ry && my < ry + DROP_ROW_H && my >= clipTop && my < clipTop + shown;
            if (current) ctx.fill(px + 2, ry, px + listW - 2, ry + DROP_ROW_H, C_TAB_ACTIVE);
            else if (rh) ctx.fill(px + 2, ry, px + listW - 2, ry + DROP_ROW_H, C_TAB_HOVER);
            int ty = ry + (DROP_ROW_H - font.lineHeight) / 2 + 1;
            ctx.text(font, r.name(), px + 8, ty, current ? C_ACCENT : C_TEXT, darkMode);
            String age = shortAgo(r.viewedAt());
            ctx.text(font, age, px + listW - 8 - font.width(age), ty, C_TEXT_DIM, darkMode);
            if (recentAnim >= 0.95f && overlay < 0) addHit(px, ry, px + listW, ry + DROP_ROW_H, () -> openPlayer(r));
        }
        ctx.disableScissor();
        recentListRect = new int[] { px, clipTop, px + listW, clipTop + shown };
    }

    private static String shortAgo(long epochMs) {
        long s = Math.max(0, (System.currentTimeMillis() - epochMs) / 1000);
        if (s < 60) return "now";
        if (s < 3600) return (s / 60) + "m ago";
        if (s < 86400) return (s / 3600) + "h ago";
        return (s / 86400) + "d ago";
    }

    private int lastMouseX, lastMouseY;

    private boolean nameTagHover(int x, int y, int w) {
        return lastMouseX >= x && lastMouseX < x + w && lastMouseY >= y && lastMouseY < y + DROP_H;
    }

    private void renderNameTag(GuiGraphicsExtractor ctx, int px, int py) {
        String nameLabel = data != null ? data.name : requestedName != null ? requestedName : "";
        PvProfileView v = currentView();
        String level = v != null ? levelTag(v.skyblockLevel) + " " : "";
        String test = data != null && data.testData ? "  §cTEST DATA" : "";
        String full = level + "§r" + nameLabel + test;
        List<PvFetcher.Recent> recent = PvFetcher.recent();
        boolean hasRecent = !recent.isEmpty();
        int w = font.width(full) + 16 + (hasRecent ? 12 : 0);
        int by = py + panelH + 5;
        boolean hov = overlay < 0 && nameTagHover(px, by, w);
        roundedFill(ctx, px - 1, by - 1, px + w + 1, by + DROP_H + 1, C_BORDER, 6);
        roundedFill(ctx, px, by, px + w, by + DROP_H, hasRecent && (hov || recentOpen) ? C_TAB_HOVER : C_HEADER, 5);
        drawLegacy(ctx, full, px + 8, by + (DROP_H - font.lineHeight) / 2 + 1, C_TEXT);
        if (hasRecent) {
            ctx.text(font, recentOpen ? "▴" : "▾", px + w - 12, by + (DROP_H - font.lineHeight) / 2 + 1, C_ACCENT, darkMode);
            if (overlay < 0) addHit(px, by, px + w, by + DROP_H, () -> { recentOpen = !recentOpen; dropOpen = false; });
        }
        renderRecentList(ctx, recent, px, by, w);
        if (!recentOpen && recentAnim <= 0f && data != null && data.fetchedAt > 0 && hov) {
            long age = (System.currentTimeMillis() - data.fetchedAt) / 1000;
            String when = age < 60 ? "just now" : age < 3600 ? (age / 60) + "m ago" : (age / 3600) + "h ago";
            tooltip("§7Profile data from §e" + when,
                    "§8Reopening within " + (PvFetcher.CACHE_TTL_MS / 60000) + " minutes uses this copy.");
        }
    }

    private void renderDropdown(GuiGraphicsExtractor ctx, int px, int py, int mx, int my) {
        dropListRect = null;
        renderNameTag(ctx, px, py);
        if (data == null || data.profiles.isEmpty()) return;
        PvData.Profile cur = data.profiles.get(profileIdx);
        String label = cur.symbol() + "§r" + cur.cuteName();
        int bw = Math.max(Math.round(110 * gs), font.width(label) + 28);
        int bx = px + panelW - bw, by = py + panelH + 5;
        boolean hov = overlay < 0 && mx >= bx && mx < bx + bw && my >= by && my < by + DROP_H;
        roundedFill(ctx, bx - 1, by - 1, bx + bw + 1, by + DROP_H + 1, C_BORDER, 6);
        roundedFill(ctx, bx, by, bx + bw, by + DROP_H, hov || dropOpen ? C_TAB_HOVER : C_HEADER, 5);
        drawLegacy(ctx, label, bx + 8, by + (DROP_H - font.lineHeight) / 2 + 1, C_TEXT);
        ctx.text(font, dropOpen ? "▴" : "▾", bx + bw - 12, by + (DROP_H - font.lineHeight) / 2 + 1, C_ACCENT, darkMode);
        if (overlay < 0) addHit(bx, by, bx + bw, by + DROP_H, () -> dropOpen = !dropOpen);

        if (dropAnim <= 0f) return;
        int n = data.profiles.size();
        int listW = Math.max(bw, widestProfile() + 30);
        int listH = n * DROP_ROW_H + 4;
        int lx = bx + bw - listW;
        boolean below = by + DROP_H + 3 + listH <= lhgt - 2;
        int ly = below ? by + DROP_H + 3 : by - 3 - listH;
        int shown = Math.round(listH * ease(dropAnim));

        ctx.nextStratum();
        int clipTop = below ? ly : ly + listH - shown;
        ctx.enableScissor(lx - 1, clipTop - 1, lx + listW + 1, clipTop + shown + 1);
        roundedFill(ctx, lx - 1, ly - 1, lx + listW + 1, ly + listH + 1, C_BORDER, 6);
        roundedFill(ctx, lx, ly, lx + listW, ly + listH, withAlpha(C_SIDEBAR, 250), 5);
        for (int i = 0; i < n; i++) {
            PvData.Profile p = data.profiles.get(i);
            int ry = ly + 2 + i * DROP_ROW_H;
            boolean rh = mx >= lx && mx < lx + listW && my >= ry && my < ry + DROP_ROW_H && my >= clipTop && my < clipTop + shown;
            if (i == profileIdx) ctx.fill(lx + 2, ry, lx + listW - 2, ry + DROP_ROW_H, C_TAB_ACTIVE);
            else if (rh) ctx.fill(lx + 2, ry, lx + listW - 2, ry + DROP_ROW_H, C_TAB_HOVER);
            ctx.text(font, p.symbol() + "§r" + p.cuteName(), lx + 8, ry + (DROP_ROW_H - font.lineHeight) / 2 + 1,
                    i == profileIdx ? C_ACCENT : C_TEXT, darkMode);
            if (p.selected()) ctx.text(font, "★", lx + listW - 14, ry + (DROP_ROW_H - font.lineHeight) / 2 + 1, C_TEXT_DIM, darkMode);
            if (dropAnim >= 0.95f && overlay < 0) {
                final int idx = i;
                addHit(lx, ry, lx + listW, ry + DROP_ROW_H, () -> selectProfile(idx));
            }
        }
        ctx.disableScissor();
        dropListRect = new int[] { lx, clipTop, lx + listW, clipTop + shown };
    }

    private int widestProfile() {
        int w = 0;
        for (PvData.Profile p : data.profiles) w = Math.max(w, font.width(p.symbol() + p.cuteName()));
        return w;
    }

    private void selectProfile(int idx) {
        dropOpen = false;
        if (idx == profileIdx) return;
        profileIdx = idx;
        view(idx);
        scrolls.clear();
        maxScrolls.clear();
        petIcons.clear();
        outKey = key();
        transDir = 1;
        transStart = System.currentTimeMillis();
    }

    // ── HotM / HotF overlay ───────────────────────────────────────────────────

    private void renderOverlay(GuiGraphicsExtractor ctx, int px, int py, int mx, int my) {
        if (overlayAnim <= 0f) return;
        int which = overlay >= 0 ? overlay : lastOverlay;
        if (overlay >= 0) lastOverlay = overlay;
        float a = ease(overlayAnim);

        ctx.nextStratum();
        roundedFill(ctx, px, py, px + panelW, py + panelH, (int) (0x99 * a) << 24, RADIUS);

        int bw = Math.round(panelW * 0.78f), bh = Math.round(panelH * 0.8f);
        int bx = px + (panelW - bw) / 2, by = py + (panelH - bh) / 2;
        overlayBox = new int[] { bx, by, bx + bw, by + bh };

        var pose = ctx.pose();
        pose.pushMatrix();
        float sc = 0.92f + 0.08f * a;
        pose.translate(bx + bw / 2f, by + bh / 2f);
        pose.scale(sc, sc);
        pose.translate(-(bx + bw / 2f), -(by + bh / 2f));

        roundedFill(ctx, bx - 1, by - 1, bx + bw + 1, by + bh + 1, C_BORDER, RADIUS + 1);
        roundedFill(ctx, bx, by, bx + bw, by + bh, withAlpha(C_BG, 250), RADIUS);
        int headH = 22;
        roundedFill(ctx, bx, by, bx + bw, by + headH, C_HEADER, RADIUS, true, true, false, false);
        ctx.fill(bx, by + headH - 1, bx + bw, by + headH, C_ACCENT);
        String title = (which == 0 ? "Heart of the Mountain" : "Heart of the Forest")
                + (overlayTitle.isEmpty() ? "" : " §7— " + overlayTitle);
        ctx.text(font, title, bx + 10, by + (headH - font.lineHeight) / 2, C_ACCENT, darkMode);
        String close = "Click outside to close";
        ctx.text(font, close, bx + bw - font.width(close) - 10, by + (headH - font.lineHeight) / 2, C_TEXT_DIM, darkMode);

        int ax0 = bx + 6, ay0 = by + headH + 4, ax1 = bx + bw - 6, ay1 = by + bh - 6;
        overlayMaxScroll = 0;
        overlayScroll = 0;
        PvProfileView v = currentView();
        if (v != null) {
            float ns = 0.7f * es;
            String note = overlayPreset > 0 ? "Tree preset " + overlayPreset : "Current tree";
            text(ctx, "§8" + note, ax0 + (ax1 - ax0 - textW(note, ns)) / 2, ay0, C_TEXT_DIM, ns);
            int ty = ay0 + lineH(ns) + 4;
            int ox0 = clipX0, oy0 = clipY0, ox1 = clipX1, oy1 = clipY1;
            clipX0 = ax0; clipY0 = ty; clipX1 = ax1; clipY1 = ay1;
            ctx.enableScissor(ax0, ty, ax1, ay1);
            PvPages.Ctx c = new PvPages.Ctx(this, ctx, v, key(), 0, ax0, ty, ax1 - ax0, ay1 - ty, mx, my, 0);
            renderCurrentTree(c, which == 0, overlayPreset, ax0, ty, ax1 - ax0, ay1 - ty);
            ctx.disableScissor();
            clipX0 = ox0; clipY0 = oy0; clipX1 = ox1; clipY1 = oy1;
        }
        pose.popMatrix();
    }

    private int lastOverlay = 0;

    private static void renderCurrentTree(PvPages.Ctx c, boolean mountain, int wanted, int x, int y, int w, int h) {
        com.google.gson.JsonObject tree = PvJson.obj(c.m(), "skill_tree");
        String kind = mountain ? "mining" : "foraging";
        int preset = wanted > 0 ? wanted : PvSkillGearPages.equippedPreset(tree, mountain ? "mountain" : "forest");
        com.google.gson.JsonObject nodes = PvJson.obj(tree, "nodes", preset == 1 ? kind : kind + "_" + preset);
        double xp = PvJson.num(tree, 0, "experience", kind);
        int level = Math.max(1, PvPages.tierFromIncrements(xp, mountain ? PvMiningPage.HOTM : PvSkillGearPages.HOTF));
        int core = (int) PvJson.num(nodes, 0, mountain ? "core_of_the_mountain" : "center_of_the_forest");
        PvTree.render(c, mountain ? PvConstants.hotmLayout() : PvConstants.hotfLayout(), mountain ? "hotm" : "hotf",
                nodes, PvSkillGearPages.selectedAbility(tree, kind, preset), mountain ? core : 0, level, x, y, w, h);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isDoubleClick) {
        double mx = click.x() / uiScale, my = click.y() / uiScale;
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;

        if (overlay >= 0) {
            if (overlayBox == null || !(mx >= overlayBox[0] && mx < overlayBox[2] && my >= overlayBox[1] && my < overlayBox[3]))
                overlay = -1;
            return true;
        }
        searchFocus = -1;
        int px = px(), py = py();
        if (tabMaxScroll > 0 && !dropOpen && my >= py && my < py + tabH && mx >= px && mx < px + panelW) {
            tabDragging = true;
            tabDragMoved = false;
            tabDragStartX = mx;
            tabDragStartScroll = tabScrollX;
            tabPressHit = null;
            for (int i = hits.size() - 1; i >= 0; i--) if (hits.get(i).contains(mx, my)) { tabPressHit = hits.get(i); break; }
            return true;
        }
        for (int i = hits.size() - 1; i >= 0; i--) {
            if (hits.get(i).contains(mx, my)) { hits.get(i).action().run(); return true; }
        }
        if (dropOpen || recentOpen) { dropOpen = false; recentOpen = false; return true; }
        if (modelRect != null && mx >= modelRect[0] && mx < modelRect[2] && my >= modelRect[1] && my < modelRect[3]) {
            draggingModel = true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (tabDragging) {
            double moved = click.x() / uiScale - tabDragStartX;
            if (Math.abs(moved) > 3) tabDragMoved = true;
            if (tabDragMoved) tabScrollX = Mth.clamp(tabDragStartScroll - (int) Math.round(moved), 0, tabMaxScroll);
            return true;
        }
        if (draggingModel && model != null) {
            model.yaw -= (float) dx * 2.2f;
            model.pitch = Mth.clamp(model.pitch - (float) dy * 0.8f, -25f, 25f);
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (tabDragging) {
            tabDragging = false;
            if (!tabDragMoved && tabPressHit != null && tabPressHit.contains(click.x() / uiScale, click.y() / uiScale)) tabPressHit.action().run();
            tabPressHit = null;
            return true;
        }
        draggingModel = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        if (tooltipMaxScroll > 0 && (hoverStack != null || hoverLines != null)) {
            tooltipScroll = Mth.clamp(tooltipScroll - (float) (vScroll * 24), 0, tooltipMaxScroll);
            return true;
        }
        mx /= uiScale;
        my /= uiScale;
        if (overlay >= 0) { overlayScroll = Mth.clamp(overlayScroll - (int) (vScroll * 20), 0, overlayMaxScroll); return true; }
        int px = px(), py = py();
        if (my >= py && my < py + tabH && mx >= px && mx < px + panelW) {
            tabScrollX -= (int) (vScroll * 20);
            return true;
        }
        if (sidebarMaxScroll > 0 && mx >= px && mx < px + sidebarW && my >= py + tabH && my < py + panelH) {
            sidebarScroll = Mth.clamp(sidebarScroll - (int) (vScroll * 20), 0, sidebarMaxScroll);
            return true;
        }
        if (mx >= px && mx < px + panelW && my >= py + tabH && my < py + panelH) {
            int k = key();
            scrolls.put(k, Mth.clamp(scrolls.getOrDefault(k, 0) - (int) (vScroll * 18), 0, maxScrolls.getOrDefault(k, 0)));
            return true;
        }
        return super.mouseScrolled(mx, my, hScroll, vScroll);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (searchFocus < 0) return false;
        String text = search(searchFocus);
        if (text.length() < 40 && event.codepoint() >= 32) editSearch(text + Character.toString(event.codepoint()));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (searchFocus >= 0) {
            String text = search(searchFocus);
            switch (event.key()) {
                case InputConstants.KEY_ESCAPE, InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> searchFocus = -1;                       // escape / enter
                case InputConstants.KEY_BACKSPACE -> { if (!text.isEmpty()) editSearch(text.substring(0, text.length() - 1)); } // backspace
                default -> { return true; }
            }
            return true;
        }
        if (event.key() == InputConstants.KEY_ESCAPE) {
            if (overlay >= 0) overlay = -1;
            else if (dropOpen) dropOpen = false;
            else onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    // ── Drawing helpers ───────────────────────────────────────────────────────

    void addHit(int x0, int y0, int x1, int y1, Runnable action) {
        if (hitK != 1f) { // inside a page that is drawn scaled
            x0 = Math.round(x0 * hitK + hitOx); x1 = Math.round(x1 * hitK + hitOx);
            y0 = Math.round(y0 * hitK + hitOy); y1 = Math.round(y1 * hitK + hitOy);
        }
        if (x1 > x0 && y1 > y0) hits.add(new Hit(x0, y0, x1, y1, action));
    }

    boolean hovered(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h
                && mx >= clipX0 && mx < clipX1 && my >= clipY0 && my < clipY1;
    }

    boolean offscreen(int y, int s) {
        return y + s < clipY0 - 32 || y > clipY1 + 32;
    }

    static int slotBg() { return darkMode ? 0x55000000 : 0x1E000000; }

    void slotNoTooltip(GuiGraphicsExtractor ctx, int x, int y, int s, ItemStack stack) {
        if (offscreen(y, s)) return;
        roundedFill(ctx, x, y, x + s, y + s, slotBg(), 3);
        if (!stack.isEmpty()) item(ctx, stack, x + 1, y + 1, s - 2);
    }

    void slot(GuiGraphicsExtractor ctx, int x, int y, int s, ItemStack stack, int mx, int my) {
        if (offscreen(y, s)) return;
        roundedFill(ctx, x, y, x + s, y + s, slotBg(), 3);
        if (!stack.isEmpty()) item(ctx, stack, x + 1, y + 1, s - 2);
        if (hovered(mx, my, x, y, s, s)) {
            ctx.nextStratum();
            roundedFill(ctx, x, y, x + s, y + s, overlay(0x28), 3);
            if (!stack.isEmpty()) hoverStack = stack.copy();   // a copy, so a mod that edits a hovered item can't change the cached one
        }
    }

    void raritySlot(GuiGraphicsExtractor ctx, int x, int y, int s, ItemStack stack, int rgb, int mx, int my) {
        raritySlot(ctx, x, y, s, stack, rgb, false, mx, my);
    }

    void raritySlot(GuiGraphicsExtractor ctx, int x, int y, int s, ItemStack stack, int rgb, boolean dim, int mx, int my) {
        if (offscreen(y, s)) return;
        rarityBg(ctx, x, y, s, rgb);
        if (dim) roundedFill(ctx, x + 1, y + 1, x + s - 1, y + s - 1, 0x55000000, 2);
        if (!stack.isEmpty()) item(ctx, stack, x + 1, y + 1, s - 2);
        if (hovered(mx, my, x, y, s, s)) {
            ctx.nextStratum();
            roundedFill(ctx, x, y, x + s, y + s, overlay(0x28), 3);
            if (!stack.isEmpty()) hoverStack = stack.copy();   // a copy, so a mod that edits a hovered item can't change the cached one
        }
    }

    private static final java.util.Map<Integer, Integer> INSIDE = new java.util.HashMap<>();

    private static int tintedInside(int rgb) {
        int key = rgb | (darkMode ? 0x01000000 : 0);
        Integer cached = INSIDE.get(key);
        if (cached != null) return cached;
        int base = darkMode ? 0xE6121216 : 0xE6F4EEDC;
        float a = 0x48 / 255f, ba = ((base >>> 24) & 0xFF) / 255f;
        int out = (Math.round((1 - (1 - ba) * (1 - a)) * 255) << 24);
        for (int shift = 16; shift >= 0; shift -= 8) {
            float c = ((base >> shift) & 0xFF) * (1 - a) + ((rgb >> shift) & 0xFF) * a;
            out |= Math.round(c) << shift;
        }
        INSIDE.put(key, out);
        return out;
    }

    static void rarityBg(GuiGraphicsExtractor ctx, int x, int y, int s, int rgb) {
        if (rgb < 0) { roundedFill(ctx, x, y, x + s, y + s, slotBg(), 3); return; }
        roundedFill(ctx, x, y, x + s, y + s, (0xD0 << 24) | rgb, 3);
        roundedFill(ctx, x + 1, y + 1, x + s - 1, y + s - 1, tintedInside(rgb), 2);
    }

    static int rarityRgb(String rarity) {
        return switch (rarity) {
            case "UNCOMMON" -> 0x55FF55;
            case "RARE" -> 0x5555FF;
            case "EPIC" -> 0xAA00AA;
            case "LEGENDARY" -> 0xFFAA00;
            case "MYTHIC" -> 0xFF55FF;
            case "DIVINE" -> 0x55FFFF;
            case "SPECIAL", "VERY_SPECIAL" -> 0xFF5555;
            default -> 0xFFFFFF;
        };
    }

    void item(GuiGraphicsExtractor ctx, ItemStack stack, int x, int y, int size) {
        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(size / 16f, size / 16f);
        ctx.item(stack, 0, 0);
        ctx.itemDecorations(font, stack, 0, 0);
        pose.popMatrix();
    }

    void text(GuiGraphicsExtractor ctx, String s, int x, int y, int color, float scale) {
        var pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        drawLegacy(ctx, s, 0, 0, color);
        pose.popMatrix();
    }

    void drawLegacy(GuiGraphicsExtractor ctx, String s, int x, int y, int color) {
        if (darkMode || s.indexOf('§') < 0) ctx.text(font, s, x, y, color, darkMode);
        else ctx.text(font, lightLegacy(s), x, y, color, darkMode);
    }

    private static final int[] LIGHT_CODES = { 0x000000, 0x0000AA, 0x007700, 0x007A7A, 0xAA0000, 0xAA00AA, 0xB87700, 0x666666,
            0x444444, 0x3030D0, 0x1E8A1E, 0x0A8A9A, 0xC62828, 0xB030B0, 0x9A7B00, -1 };
    private static final Map<String, net.minecraft.util.FormattedCharSequence> LIGHT_CACHE = new HashMap<>();

    private static final int[] VANILLA_CODES = { 0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF };
    private static final java.util.regex.Pattern LEVEL_TAG = java.util.regex.Pattern.compile("§8\\[§[0-9a-f]\\d+§8\\]");

    private static net.minecraft.util.FormattedCharSequence lightLegacy(String s) {
        net.minecraft.util.FormattedCharSequence cached = LIGHT_CACHE.get(s);
        if (cached != null) return cached;
        List<net.minecraft.util.FormattedCharSequence> parts = new ArrayList<>();
        java.util.regex.Matcher tag = LEVEL_TAG.matcher(s);
        int last = 0;
        while (tag.find()) {
            appendLegacy(parts, s.substring(last, tag.start()), LIGHT_CODES);
            appendLegacy(parts, tag.group(), VANILLA_CODES);
            last = tag.end();
        }
        appendLegacy(parts, s.substring(last), LIGHT_CODES);
        net.minecraft.util.FormattedCharSequence out = net.minecraft.util.FormattedCharSequence.composite(parts);
        if (LIGHT_CACHE.size() > 4000) LIGHT_CACHE.clear();
        LIGHT_CACHE.put(s, out);
        return out;
    }

    private static void appendLegacy(List<net.minecraft.util.FormattedCharSequence> parts, String s, int[] codes) {
        net.minecraft.network.chat.Style style = net.minecraft.network.chat.Style.EMPTY;
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '§' || i + 1 >= s.length()) { run.append(c); continue; }
            if (run.length() > 0) {
                parts.add(net.minecraft.util.FormattedCharSequence.forward(run.toString(), style));
                run.setLength(0);
            }
            char code = Character.toLowerCase(s.charAt(++i));
            int idx = "0123456789abcdef".indexOf(code);
            if (idx >= 0) {
                int rgb = codes[idx];
                style = rgb < 0 ? net.minecraft.network.chat.Style.EMPTY
                        : net.minecraft.network.chat.Style.EMPTY.withColor(net.minecraft.network.chat.TextColor.fromRgb(rgb));
            } else {
                switch (code) {
                    case 'l' -> style = style.withBold(true);
                    case 'o' -> style = style.withItalic(true);
                    case 'n' -> style = style.withUnderlined(true);
                    case 'm' -> style = style.withStrikethrough(true);
                    case 'k' -> style = style.withObfuscated(true);
                    case 'r' -> style = net.minecraft.network.chat.Style.EMPTY;
                    default -> { }
                }
            }
        }
        if (run.length() > 0) parts.add(net.minecraft.util.FormattedCharSequence.forward(run.toString(), style));
    }

    int textW(String s, float scale) {
        return Math.round(font.width(s) * scale);
    }

    private static final String LEVEL_COLORS = "7fea2b39d56c4";

    static String levelColor(int level) {
        return "§" + LEVEL_COLORS.charAt(Math.min(Math.max(0, level) / 40, LEVEL_COLORS.length() - 1));
    }

    static String levelTag(int level) {
        return "§8[" + levelColor(level) + level + "§8]";
    }

    static String fmt(double n) {
        double a = Math.abs(n);
        if (a >= 1e9) return String.format(Locale.ROOT, "%.2fB", n / 1e9);
        if (a >= 1e6) return String.format(Locale.ROOT, "%.2fM", n / 1e6);
        if (a >= 1e3) return String.format(Locale.ROOT, "%.1fk", n / 1e3);
        return String.format(Locale.ROOT, "%.0f", n);
    }

    static String fmtFull(double n) {
        return String.format(Locale.ROOT, "%,.0f", n);
    }

    static String ago(long epochMs) {
        long s = Math.max(0, (System.currentTimeMillis() - epochMs) / 1000);
        long[] units = { 365L * 86400, 30L * 86400, 86400, 3600, 60 };
        String[] names = { "year", "month", "day", "hour", "minute" };
        for (int i = 0; i < units.length; i++) {
            long n = s / units[i];
            if (n >= 1) return n + " " + names[i] + (n == 1 ? "" : "s") + " ago";
        }
        return "Just now";
    }
}
