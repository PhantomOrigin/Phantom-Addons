package com.phantomaddons.features.pv;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.phantomaddons.PhantomAddons;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.utils.PhantomPaths;
import com.phantomaddons.utils.TextUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PvNameBook {

    private static final String NAME = "([A-Za-z0-9_]{3,16})";
    private static final String RANK = "(?:\\[[A-Za-z+]{2,12}\\] )?";

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final Pattern DASHES = Pattern.compile("[-=▬]{20,}");
    private static final Pattern PAGE = Pattern.compile("(?:<< )?Friends \\(Page (\\d+) of (\\d+)\\)(?: >>)?");
    private static final Pattern LIST_ENTRY = Pattern.compile(RANK + NAME + " (?:is|was) .+");
    private static final Pattern FRIEND_ONLINE = Pattern.compile("Friend > " + RANK + NAME + " (?:joined|left)\\.?");
    private static final Pattern FRIEND_ADDED = Pattern.compile("You are now friends with " + RANK + NAME + "[.!]?");
    private static final Pattern FRIEND_REMOVED = Pattern.compile("You removed " + RANK + NAME + " from your friends list!");

    private static final long RECENT_MS = 5 * 60_000L;
    private static final int PREVIOUS_LOBBIES = 2;
    private static final int MAX_BLOCK_LINES = 80;

    private record Recent(String name, long at) {}

    private static final Map<String, Recent> RECENT = new LinkedHashMap<>();
    private static final ArrayDeque<List<String>> PREVIOUS = new ArrayDeque<>();
    private static volatile List<String> current = List.of();
    private static volatile List<String> friends = List.of();
    private static volatile ClientLevel lastLevel;
    private static int ticks;

    private static boolean inBlock;
    private static final List<String> BLOCK_LINES = new ArrayList<>();

    private PvNameBook() {}

    public static void register() {
        loadFriends();
        ClientTickEvents.END_CLIENT_TICK.register(PvNameBook::tick);
        ClientReceiveMessageEvents.ALLOW_GAME.register((text, overlay) -> {
            if (!overlay) onMessage(text);
            return true;
        });
    }

    // ── Suggestions ───────────────────────────────────────────────────────────

    public static void noteViewed(String name) {
        if (name != null) noteViewed(List.of(name));
    }

    public static void noteViewed(Collection<String> names) {
        if (!PhantomConfig.isPvNameSuggestions()) return;
        long now = System.currentTimeMillis();
        synchronized (RECENT) {
            for (String n : names) {
                if (n == null || !VALID.matcher(n).matches()) continue;
                String key = n.toLowerCase(Locale.ROOT);
                RECENT.remove(key);
                RECENT.put(key, new Recent(n, now));
            }
        }
    }

    public static List<String> suggestions() {
        Map<String, String> out = new LinkedHashMap<>();
        if (PhantomConfig.isPvNameSuggestions()) {
            long cutoff = System.currentTimeMillis() - RECENT_MS;
            List<Recent> recent;
            synchronized (RECENT) {
                RECENT.values().removeIf(r -> r.at() < cutoff);
                recent = new ArrayList<>(RECENT.values());
            }
            Collections.reverse(recent);
            for (Recent r : recent) add(out, r.name());
        }
        for (String n : liveNames()) add(out, n);
        if (PhantomConfig.isPvNameSuggestions()) {
            List<List<String>> previous;
            synchronized (PREVIOUS) { previous = new ArrayList<>(PREVIOUS); }
            for (List<String> lobby : previous) for (String n : lobby) add(out, n);
            for (String n : friends) add(out, n);
        }
        return new ArrayList<>(out.values());
    }

    private static void add(Map<String, String> out, String name) {
        out.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
    }

    private static List<String> liveNames() {
        Minecraft mc = Minecraft.getInstance();
        List<String> names = new ArrayList<>();
        if (mc.getConnection() == null) return names;
        String own = mc.player != null ? mc.player.getGameProfile().name() : null;
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            String n = info.getProfile().name();
            if (n != null && VALID.matcher(n).matches() && !n.equalsIgnoreCase(own)) names.add(n);
        }
        return names;
    }

    // ── Lobbies ───────────────────────────────────────────────────────────────

    private static void tick(Minecraft mc) {
        if (++ticks % 20 != 0) return;
        ClientLevel level = mc.level;
        if (level != lastLevel) {
            rollLobby();
            lastLevel = level;
        }
        current = level == null || mc.getConnection() == null || mc.player == null ? List.of() : liveNames();
    }

    private static void rollLobby() {
        List<String> left = current;
        current = List.of();
        if (left.isEmpty()) return;
        synchronized (PREVIOUS) {
            PREVIOUS.addFirst(left);
            while (PREVIOUS.size() > PREVIOUS_LOBBIES) PREVIOUS.removeLast();
        }
    }

    // ── Friends, learned only from what the server sends ──────────────────────

    private static boolean onHypixel(Minecraft mc) {
        var server = mc.getCurrentServer();
        return server != null && server.ip != null && server.ip.toLowerCase(Locale.ROOT).contains("hypixel.net");
    }

    private static void onMessage(Component text) {
        if (!PhantomConfig.isPvNameSuggestions() || !onHypixel(Minecraft.getInstance())) return;
        String clean = TextUtil.stripColor(text.getString());

        Matcher m = FRIEND_ONLINE.matcher(clean);
        if (m.matches()) { merge(List.of(m.group(1))); return; }
        m = FRIEND_ADDED.matcher(clean);
        if (m.matches()) { merge(List.of(m.group(1))); return; }
        m = FRIEND_REMOVED.matcher(clean);
        if (m.matches()) { remove(m.group(1)); return; }

        trackListPage(clean);
    }

    private static void trackListPage(String clean) {
        boolean dash = DASHES.matcher(clean).matches();
        if (!inBlock) {
            if (dash) {
                inBlock = true;
                BLOCK_LINES.clear();
            }
            return;
        }
        if (!dash) {
            if (BLOCK_LINES.size() < MAX_BLOCK_LINES) BLOCK_LINES.add(clean.strip());
            return;
        }
        inBlock = false;
        boolean friendPage = false;
        List<String> names = new ArrayList<>();
        for (String line : BLOCK_LINES) {
            if (PAGE.matcher(line).matches()) { friendPage = true; continue; }
            Matcher e = LIST_ENTRY.matcher(line);
            if (e.matches()) names.add(e.group(1));
        }
        if (friendPage && !names.isEmpty()) merge(names);
    }

    private static void merge(Collection<String> names) {
        List<String> all = new ArrayList<>(friends);
        all.addAll(names);
        friends = unique(all);
        saveFriends();
    }

    private static void remove(String name) {
        List<String> kept = new ArrayList<>();
        for (String n : friends) if (!n.equalsIgnoreCase(name)) kept.add(n);
        if (kept.size() == friends.size()) return;
        friends = List.copyOf(kept);
        saveFriends();
    }

    private static List<String> unique(Collection<String> names) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String n : names) add(out, n);
        return List.copyOf(out.values());
    }

    // ── Storage ───────────────────────────────────────────────────────────────

    private static Path file() {
        return PhantomPaths.resolve("friends.json");
    }

    private static void loadFriends() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            List<String> loaded = new ArrayList<>();
            if (o.has("friends")) for (JsonElement e : o.getAsJsonArray("friends")) loaded.add(e.getAsString());
            friends = unique(loaded);
        } catch (IOException | RuntimeException e) {
            PhantomAddons.LOGGER.warn("[PV] couldn't read the saved friend list: {}", e.toString());
        }
    }

    private static void saveFriends() {
        try {
            JsonObject o = new JsonObject();
            JsonArray a = new JsonArray();
            for (String n : friends) a.add(n);
            o.add("friends", a);
            Files.writeString(file(), o.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            PhantomAddons.LOGGER.warn("[PV] couldn't save the friend list: {}", e.toString());
        }
    }
}
