package com.phantomaddons.features.pv;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.phantomaddons.PhantomConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ChatCards {

    private static final int MARK = 0xA5;
    private static final char BLANK = '‌';
    private static final int MAX_CARDS = 8;

    private static final int VIRTUAL_W = 460, VIRTUAL_H = 280 - 24;
    private static final int PAD = 2;

    private static final class Card {
        private PvScreen host;
        final String name;
        final java.util.UUID uuid;
        final int id, key, width, fullHeight, lineHeight, lines;
        final float scale;
        final Component full, small;
        boolean collapsed;

        Card(int id, String name, java.util.UUID uuid, int key, float scale, int width, int lines, int lineHeight, Component full, Component small) {
            this.id = id; this.name = name; this.uuid = uuid; this.key = key; this.scale = scale; this.width = width;
            this.lines = lines; this.fullHeight = lines * lineHeight; this.lineHeight = lineHeight;
            this.full = full; this.small = small;
        }

        PvScreen host() {
            if (host == null) host = new PvScreen(name, uuid);
            return host;
        }

        int height() { return collapsed ? lineHeight : fullHeight; }
        String pageName() { return key == 300 ? "Dungeons" : "Kuudra"; }
    }

    private static net.minecraft.client.gui.components.ChatComponent chat;

    public static void setChat(net.minecraft.client.gui.components.ChatComponent c) { chat = c; }

    private static final Map<Integer, Card> CARDS = new LinkedHashMap<>();
    private static int nextId = 0;

    private ChatCards() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("pvcard")
                        .then(ClientCommands.argument("page", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("kuudra", "dungeons"), builder))
                                .executes(ctx -> { post(StringArgumentType.getString(ctx, "page"), null); return 1; })
                                .then(ClientCommands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> {
                                            post(StringArgumentType.getString(ctx, "page"), StringArgumentType.getString(ctx, "player"));
                                            return 1;
                                        })))));
    }

    private static void post(String page, String name) { post(page, name, false, false); }

    private static void post(String page, String name, boolean minimised, boolean automatic) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int key = page.equalsIgnoreCase("dungeons") ? 300 : page.equalsIgnoreCase("kuudra") ? 400 : -1;
        if (key < 0 || !PhantomConfig.isPvEnabled()) {
            mc.player.sendSystemMessage(Component.literal("§6[Phantom] §cUse /pvcard kuudra|dungeons [player] (the Profile Viewer must be on)."));
            return;
        }
        String own = mc.player.getGameProfile().name();
        boolean self = name == null || name.equalsIgnoreCase(own);

        var options = mc.options;
        double chatScale = Math.max(0.1, options.chatScale().get());
        int chatWidth = (int) (ChatComponent.getWidth(options.chatWidth().get()) / chatScale);
        int width = Math.max(120, Math.min(chatWidth - 2 * PAD - 8, 420));
        float scale = width / (float) VIRTUAL_W;
        int lineHeight = Math.max(1, (int) (9.0 * (options.chatLineSpacing().get() + 1.0)));
        int lines = Math.max(1, (int) Math.ceil(VIRTUAL_H * scale / lineHeight));

        int id = nextId++ & 0xFF;
        Component full = blankLines(id, lines), small = blankLines(id, 1);
        CARDS.remove(id);
        Card card = new Card(id, self ? own : name, self ? mc.player.getUUID() : null, key, scale, width, lines, lineHeight, full, small);
        card.collapsed = minimised;
        CARDS.put(id, card);
        while (CARDS.size() > MAX_CARDS) CARDS.remove(CARDS.keySet().iterator().next());
        card.host();                                    // start loading the profile now, either way
        if (minimised) {
            mc.player.sendSystemMessage(small);        // the bar is shown at once; opening it finds the data ready
        } else {
            // A full card is only posted once it has something to show, so it never appears as an empty box.
            PENDING.add(new Pending(card, System.currentTimeMillis() + PENDING_TIMEOUT_MS));
            if (!automatic) mc.player.sendSystemMessage(Component.literal("§7Loading " + card.name + "'s " + card.pageName() + " card..."));
        }
    }

    private record Pending(Card card, long deadline) {}
    private static final List<Pending> PENDING = new java.util.ArrayList<>();
    private static final long PENDING_TIMEOUT_MS = 20_000L;

    public static void tick() {
        if (PENDING.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { PENDING.clear(); return; }
        long now = System.currentTimeMillis();
        PENDING.removeIf(p -> {
            if (!p.card.host().cardReady(p.card.key) && now < p.deadline) return false;
            if (CARDS.get(p.card.id) == p.card) mc.player.sendSystemMessage(p.card.full);
            return true;
        });
    }

    private static Component blankLines(int id, int lines) {
        MutableComponent message = Component.empty();
        for (int i = 0; i < lines; i++) {
            int rgb = (MARK << 16) | (id << 8) | Math.min(i, 255);
            message.append(Component.literal(String.valueOf(BLANK)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
            if (i < lines - 1) message.append(Component.literal("\n"));
        }
        return message;
    }

    private static int cardIdOf(Component content) {
        int[] found = { -1 };
        content.visit((style, text) -> {
            TextColor c = style.getColor();
            if (c != null && text.indexOf(BLANK) >= 0 && (c.getValue() >>> 16) == MARK) {
                found[0] = (c.getValue() >> 8) & 0xFF;
                return java.util.Optional.of(Boolean.TRUE);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static void toggle(Card card) {
        if (chat == null) return;
        var access = (com.phantomaddons.mixin.ChatComponentAccessor) (Object) chat;
        Component to = card.collapsed ? card.full : card.small;
        var all = access.phantomaddons$allMessages();
        boolean swapped = false;
        for (int i = 0; i < all.size(); i++) {
            var m = all.get(i);
            if (cardIdOf(m.content()) == card.id) {
                all.set(i, new net.minecraft.client.multiplayer.chat.GuiMessage(m.addedTime(), to, m.signature(), m.source(), m.tag()));
                swapped = true;
                break;
            }
        }
        com.phantomaddons.PhantomAddons.LOGGER.info("[ChatCards] {} card {}: {}", card.collapsed ? "opening" : "collapsing", card.id,
                swapped ? "swapped its chat message" : "its chat message wasn't found in the history");
        if (!swapped) return;
        card.collapsed = !card.collapsed;
        int scroll = access.phantomaddons$scroll();
        access.phantomaddons$refresh();
        access.phantomaddons$setScroll(scroll);
        chat.scrollChat(0);
        VISIBLE.clear();
    }

    private static final class Visible {
        int top = Integer.MAX_VALUE, bottom = Integer.MIN_VALUE;   // the on-screen lines' span
        int firstLine = Integer.MAX_VALUE, lastLine = -1;          // which of the card's lines those are
        int cardTop;                                               // where the card's first line would be
    }

    private static final Map<Integer, Visible> VISIBLE = new LinkedHashMap<>();
    private static final java.util.Set<Integer> LOGGED = new java.util.HashSet<>();

    private record Placed(Card card, int cardTop, int top, int bottom) {}
    private static final List<Placed> PLACED = new java.util.ArrayList<>();
    private static final org.joml.Matrix3x2f CHAT_POSE = new org.joml.Matrix3x2f();
    private static boolean placedValid;

    public static void noteLine(int y, float opacity, FormattedCharSequence line) {
        if (CARDS.isEmpty() || opacity < 0.05f) return;
        int[] marker = { -1 };
        line.accept((pos, style, cp) -> {
            TextColor c = style.getColor();
            if (cp == BLANK && c != null && (c.getValue() >>> 16) == MARK) { marker[0] = c.getValue() & 0xFFFF; return false; }
            return true;
        });
        if (marker[0] < 0) return;
        int id = marker[0] >> 8, index = marker[0] & 0xFF;
        Card card = CARDS.get(id);
        if (card == null) return;
        Visible v = VISIBLE.computeIfAbsent(id, k -> new Visible());
        v.top = Math.min(v.top, y);
        v.bottom = Math.max(v.bottom, y + card.lineHeight);
        v.cardTop = y - index * card.lineHeight;
        v.firstLine = Math.min(v.firstLine, index);
        v.lastLine = Math.max(v.lastLine, index);
    }

    public static void discard() { VISIBLE.clear(); }

    public static void flush(ChatCardView view) {
        if (VISIBLE.isEmpty()) {
            PLACED.clear();
            placedValid = false;
            return;
        }
        GuiGraphicsExtractor g = view.phantomaddons$graphics();
        Minecraft mc = Minecraft.getInstance();
        boolean focused = mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen;
        PLACED.clear();
        CHAT_POSE.set(g.pose());
        placedValid = true;
        for (var e : VISIBLE.entrySet()) {
            Card card = CARDS.get(e.getKey());
            if (card == null) continue;
            Visible v = e.getValue();
            int mx = -10000, my = -10000;
            if (focused && !card.collapsed) {
                float lx = view.phantomaddons$localX(), ly = view.phantomaddons$localY();
                if (lx >= PAD && lx < PAD + card.width && ly >= v.top && ly < v.bottom) {
                    mx = Math.round((lx - PAD) / card.scale);
                    my = Math.round((ly - v.cardTop) / card.scale);
                }
            }
            if (v.firstLine > 0 && LOGGED.add(e.getKey()))
                com.phantomaddons.PhantomAddons.LOGGER.info("[ChatCards] card {} is cut at the top: showing lines {}-{} of {} (chat y {}..{}, card top {})",
                        e.getKey(), v.firstLine, v.lastLine, card.lines, v.top, v.bottom, v.cardTop);
            g.enableScissor(0, v.top, PAD + card.width + PAD, v.bottom);
            var pose = g.pose();
            pose.pushMatrix();
            try {
                pose.translate(PAD, v.cardTop);
                if (card.collapsed) {
                    PvScreen.renderCardBar(g, card.name, card.width, card.lineHeight, card.pageName());
                } else {
                    pose.scale(card.scale, card.scale);
                    card.host().renderCard(g, card.key, VIRTUAL_W, Math.round(card.fullHeight / card.scale), mx, my,
                            view.phantomaddons$globalX(), view.phantomaddons$globalY());
                }
            } catch (RuntimeException ex) {
                com.phantomaddons.PhantomAddons.LOGGER.error("[ChatCards] drawing card {} failed", e.getKey(), ex);
            } finally {
                pose.popMatrix();
                g.disableScissor();
            }
            PLACED.add(new Placed(card, v.cardTop, v.top, v.bottom));
        }
        VISIBLE.clear();
    }

    public static boolean click(double x, double y, boolean doubleClick) {
        if (!placedValid || PLACED.isEmpty()) return false;
        var inverse = new org.joml.Matrix3x2f(CHAT_POSE).invert();
        var local = inverse.transformPosition((float) x, (float) y, new org.joml.Vector2f());
        for (Placed p : PLACED) {
            if (local.x >= PAD && local.x < PAD + p.card.width && local.y >= p.top && local.y < p.bottom) {
                if (!p.card.collapsed
                        && p.card.host().cardClick(Math.round((local.x - PAD) / p.card.scale), Math.round((local.y - p.cardTop) / p.card.scale)))
                    return true;
                if (doubleClick) { toggle(p.card); return true; }
                return true;
            }
        }
        return false;
    }
}
