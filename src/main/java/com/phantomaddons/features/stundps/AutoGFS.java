package com.phantomaddons.features.stundps;

import com.phantomaddons.utils.TextUtil;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.PhantomAddons;
import com.phantomaddons.features.boss.KuudraHpHud;
import com.phantomaddons.utils.RoleManager;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.Queue;

/*
This feature is excluded from the standard version of the mod
 */
public final class AutoGFS {

    private static boolean active     = false;
    private static int     amount     = 0;
    private static int     cooldown   = 0;
    private static int     startDelay = 0;

    static final Queue<String> commandQueue = new ArrayDeque<>();

    // ── Registration ─────────────────────────────────────────────────────────

    public static void register() {
        if (!com.phantomaddons.Edition.CURRENT.fullFeatureSet) return; // not included in this edition
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            if (overlay || !active) return;
            String text = TextUtil.stripColor(msg.getString())
                    .trim();

            if (text.contains("no Toxic Arrow Poison in your Sacks")) {
                PhantomAddons.LOGGER.warn("[PhantomAddons] Sacks empty — stopping DPS loop");
                stop();
                queuePartyWarning("No Toxic Arrow Poison!");
            } else if (text.contains("no Twilight Arrow Poison in your Sacks")) {
                PhantomAddons.LOGGER.warn("[PhantomAddons] Sacks empty — stopping loop");
                stop();
                queuePartyWarning("No Twilight Arrow Poison!");
            }
        });
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static void start(int arrowAmount) {
        if (!com.phantomaddons.Edition.CURRENT.fullFeatureSet) return; // not included in this edition
        if (!PhantomConfig.isAutoGfsEnabled()) {
            PhantomAddons.LOGGER.info("[PhantomAddons] AutoGFS disabled — skipping");
            return;
        }
        PhantomConfig.RoleMode role = RoleManager.getActiveRole();
        if (role != PhantomConfig.RoleMode.DPS) {
            PhantomAddons.LOGGER.info("[PhantomAddons] DPS loop blocked (role = {})", role);
            return;
        }
        active     = true;
        amount     = arrowAmount;
        startDelay = 20;
        PhantomAddons.LOGGER.info("[PhantomAddons] DPS loop starting in 1s ({})", arrowAmount);
    }

    public static void stop() {
        active   = false;
        cooldown = 0;
        PhantomAddons.LOGGER.info("[PhantomAddons] DPS loop stopped");
    }

    public static void queueCommand() {
        if (!com.phantomaddons.Edition.CURRENT.fullFeatureSet) return; // not included in this edition
        if (!PhantomConfig.isAutoGfsEnabled()) return;
        PhantomConfig.RoleMode role = RoleManager.getActiveRole();
        if (role != PhantomConfig.RoleMode.DPS && role != PhantomConfig.RoleMode.STUN) return;
        commandQueue.add(buildCommand());
    }

    public static void tick(Minecraft client) {
        if (!active) return;
        if (startDelay > 0) {
            startDelay--;
        } else if (cooldown > 0) {
            cooldown--;
        } else {
            tryApplyArrow(client);
        }
    }

    public static void flushCommands(Minecraft client) {
        while (!commandQueue.isEmpty()) {
            String cmd = commandQueue.poll();
            if (client.player != null) {
                client.player.connection.sendChat("/" + cmd);
                PhantomAddons.LOGGER.info("[PhantomAddons] Sent: /{}", cmd);
            }
        }
    }

    public static boolean isActive() { return active; }

    // ── Internal logic ────────────────────────────────────────────────────────

    private static void tryApplyArrow(Minecraft client) {
        if (client.player == null) return;

        float hpPercent = KuudraHpHud.getTrueHpPercent();
        if (hpPercent >= 0 && hpPercent <= PhantomConfig.getAutoGfsDisableHpPercent()) return;

        boolean found = false;

        for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
            var stack = client.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (!stack.getHoverName().getString().equalsIgnoreCase("toxic arrow poison")) continue;

            found = true;
            if (stack.getCount() < 4) {
                commandQueue.add("gfs toxic_arrow_poison " + amount);
                PhantomAddons.LOGGER.info("[PhantomAddons] Toxic Arrow low (<4) — requesting refill");
                cooldown = 40;
            }
            return;
        }

        if (!found) {
            commandQueue.add("gfs toxic_arrow_poison " + amount);
            PhantomAddons.LOGGER.info("[PhantomAddons] Toxic Arrow missing — requesting from sacks");
            cooldown = 60;
        }
    }

    private static void queuePartyWarning(String message) {
        commandQueue.add("pc !dt " + message);
    }

    private static String buildCommand() {
        PhantomConfig.RoleMode mode = PhantomConfig.getRoleMode();
        if (mode == PhantomConfig.RoleMode.DPS)
            return "gfs toxic_arrow_poison "    + PhantomConfig.getDpsValue();
        if (mode == PhantomConfig.RoleMode.STUN)
            return "gfs twilight_arrow_poison " + PhantomConfig.getStunValue();
        return switch (RoleManager.getActiveRole()) {
            case STUN -> "gfs twilight_arrow_poison " + PhantomConfig.getStunValue();
            default   -> "gfs toxic_arrow_poison "    + PhantomConfig.getDpsValue();
        };
    }
}