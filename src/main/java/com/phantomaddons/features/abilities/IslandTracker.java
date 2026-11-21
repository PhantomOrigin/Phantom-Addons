package com.phantomaddons.features.abilities;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.phantomaddons.PhantomAddons;
import com.phantomaddons.utils.KuudraTierDetector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

public final class IslandTracker {

    private static final int FIRST_ASK_TICKS = 60, RETRY_TICKS = 100, MAX_ASKS = 3;

    private static volatile Island located = Island.UNKNOWN;
    private static int  askIn = -1, askCount;
    private static boolean awaiting;
    private static int  scoreboardTick;
    private static Island scoreboard = Island.UNKNOWN;

    private IslandTracker() {}

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientTickEvents.END_CLIENT_TICK.register(IslandTracker::tick);
        ClientReceiveMessageEvents.ALLOW_GAME.register((text, overlay) -> overlay || !consume(text.getString()));
    }

    public static Island current() {
        Island at = located;
        return at != Island.UNKNOWN ? at : scoreboard;
    }

    public static boolean allowed(AbilitySettings s, String ruleKey) {
        Island at = current();
        return at == Island.UNKNOWN || s.worksOn(ruleKey, at.name());
    }

    public static boolean allowedAnywhere(AbilitySettings s, String abilityId) {
        Island at = current();
        return at == Island.UNKNOWN || s.shownOn(abilityId, at.name());
    }

    private static void reset() {
        located = Island.UNKNOWN;
        scoreboard = Island.UNKNOWN;
        askIn = -1;
        awaiting = false;
        askCount = 0;
    }

    private static void onJoin() {
        located = Island.UNKNOWN;
        scoreboard = Island.UNKNOWN;
        awaiting = false;
        askCount = 0;
        askIn = FIRST_ASK_TICKS;
    }

    private static boolean wanted() {
        var s = com.phantomaddons.PhantomConfig.abilities();
        return s.enabled && !s.selected.isEmpty();
    }

    private static boolean onHypixel(Minecraft mc) {
        var server = mc.getCurrentServer();
        return server != null && server.ip != null && server.ip.toLowerCase().contains("hypixel");
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        if (!wanted() || !onHypixel(mc)) return;

        if (++scoreboardTick >= 20) {
            scoreboardTick = 0;
            scoreboard = fromScoreboard();
        }

        if (askIn < 0 || located != Island.UNKNOWN) return;
        if (--askIn > 0) return;
        if (askCount >= MAX_ASKS) { askIn = -1; return; }
        askCount++;
        awaiting = true;
        askIn = RETRY_TICKS;
        if (mc.getConnection() != null) mc.getConnection().sendCommand("locraw");
    }

    private static Island fromScoreboard() {
        if (KuudraTierDetector.isInKuudraHollow()) return Island.KUUDRA;
        if (KuudraTierDetector.isInDungeon()) return Island.DUNGEON;
        for (String line : KuudraTierDetector.sidebarText()) {
            if (line.contains("Glacite Mineshaft")) return Island.MINESHAFT;
        }
        return Island.UNKNOWN;
    }

    private static boolean consume(String message) {
        String line = message.trim();
        if (!line.startsWith("{\"server\"") || !line.endsWith("}")) return false;
        boolean ours = awaiting;
        try {
            JsonObject json = JsonParser.parseString(line).getAsJsonObject();
            String game = json.has("gametype") ? json.get("gametype").getAsString() : "";
            String mode = json.has("mode") ? json.get("mode").getAsString() : "";
            located = "SKYBLOCK".equalsIgnoreCase(game) ? Island.fromMode(mode) : Island.UNKNOWN;
            if (located == Island.UNKNOWN && "SKYBLOCK".equalsIgnoreCase(game) && !mode.isEmpty())
                PhantomAddons.LOGGER.info("[PhantomAddons] Unknown SkyBlock mode \"{}\"", mode);
            awaiting = false;
            askIn = -1;
        } catch (RuntimeException e) {
            return false;
        }
        return ours;
    }
}
