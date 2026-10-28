package com.phantomaddons.gui;

import com.phantomaddons.PhantomAddons;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.UpdateChecker;
import com.phantomaddons.phase.KuudraPhaseTracker;
import com.phantomaddons.utils.TextUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ChangelogManager {

    private static final int SETTLE_TICKS = 100;
    private static final int MAX_ENTRIES = 5;

    private static boolean started;
    private static int ticksInSkyblock;
    private static volatile boolean fetched;
    private static boolean markedSeen;

    private static volatile boolean updatePending;
    private static boolean updateStarted, updateShown;
    private static volatile boolean updateFetched;

    private ChangelogManager() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ChangelogManager::tick);
    }

    private static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;

        if (started && fetched && !markedSeen) {
            if (mc.gui.screen() == null && KuudraPhaseTracker.getPhase() == KuudraPhaseTracker.Phase.NONE) show(mc);
            return;
        }
        if (started) { updateTick(mc); return; }

        if (!inSkyblock(mc)) { ticksInSkyblock = 0; return; }
        if (++ticksInSkyblock < SETTLE_TICKS) return;
        if (mc.gui.screen() != null) return;
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.NONE) return;

        String current = UpdateChecker.currentVersion();
        if (current.equals("unknown")) { started = true; markedSeen = true; return; }

        String lastSeen = PhantomConfig.getLastChangelogVersion();
        if (PhantomConfig.isFirstRun() && lastSeen.isEmpty()) {
            PhantomConfig.setLastChangelogVersion(current);
            started = true; markedSeen = true;
            return;
        }
        if (lastSeen.equals(current)) { started = true; markedSeen = true; return; }
        if (!PhantomConfig.isChangelogEnabled()) {
            PhantomConfig.setLastChangelogVersion(current);
            started = true; markedSeen = true;
            return;
        }

        started = true;
        CompletableFuture.runAsync(() -> {
            ChangelogData.loadSaved();
            try {
                ChangelogData.fetch();
            } catch (Exception e) {
                PhantomAddons.LOGGER.info("[Changelog] couldn't read the changelog from GitHub ({}), using the saved copy", e.getMessage());
            }
            fetched = true;
        });
    }

    public static void onUpdateAvailable() {
        updatePending = true;
    }

    private static void updateTick(Minecraft mc) {
        if (!markedSeen || !updatePending || updateShown) return;
        if (!PhantomConfig.isChangelogEnabled() || !UpdateChecker.hasUpdate() || UpdateChecker.isSkipped()) { updateShown = true; return; }

        boolean quiet = mc.gui.screen() == null && KuudraPhaseTracker.getPhase() == KuudraPhaseTracker.Phase.NONE;
        if (!updateStarted) {
            if (!quiet) return;
            updateStarted = true;
            CompletableFuture.runAsync(() -> {
                ChangelogData.loadSaved();
                try {
                    ChangelogData.fetch();
                } catch (Exception e) {
                    PhantomAddons.LOGGER.info("[Changelog] couldn't read the changelog from GitHub ({}), using the saved copy", e.getMessage());
                }
                updateFetched = true;
            });
            return;
        }
        if (!updateFetched || !quiet) return;

        updateShown = true;
        String latest = UpdateChecker.getLatestVersion();
        if (latest == null) return;
        List<ChangelogData.Entry> entries = ChangelogData.newer(UpdateChecker.currentVersion(), latest, 3);
        PhantomAddons.LOGGER.info("[Changelog] showing the update window for {}", latest);
        mc.gui.setScreen(new ChangelogScreen(null, entries, latest));
    }

    private static void show(Minecraft mc) {
        markedSeen = true;
        String current = UpdateChecker.currentVersion();
        List<ChangelogData.Entry> entries = ChangelogData.unseen(PhantomConfig.getLastChangelogVersion(), current, MAX_ENTRIES);
        PhantomConfig.setLastChangelogVersion(current);          // seen: don't ask again for this version
        if (entries.isEmpty()) return;

        PhantomAddons.LOGGER.info("[Changelog] showing the changelog for {}", current);
        mc.gui.setScreen(new ChangelogScreen(null, entries));
    }

    public static void openNow() {
        Minecraft mc = Minecraft.getInstance();
        ChangelogData.loadSaved();
        mc.execute(() -> mc.gui.setScreen(new ChangelogScreen(mc.gui.screen(), ChangelogData.latest(MAX_ENTRIES))));
        CompletableFuture.runAsync(() -> {
            try { ChangelogData.fetch(); } catch (Exception ignored) {}
        });
    }

    public static void openFromAbout() {
        Minecraft mc = Minecraft.getInstance();
        CompletableFuture.runAsync(() -> {
            ChangelogData.loadSaved();
            try {
                ChangelogData.fetch();
            } catch (Exception e) {
                PhantomAddons.LOGGER.info("[Changelog] couldn't read the changelog from GitHub ({}), using the saved copy", e.getMessage());
            }
            String current = UpdateChecker.currentVersion();
            String latest = UpdateChecker.getLatestVersion();
            List<ChangelogData.Entry> entries = UpdateChecker.hasUpdate() && latest != null
                    ? ChangelogData.newer(current, latest, 10) : List.of();
            if (entries.isEmpty()) entries = ChangelogData.forVersion(current);
            List<ChangelogData.Entry> shown = entries;
            mc.execute(() -> mc.gui.setScreen(new ChangelogScreen(mc.gui.screen(), shown)));
        });
    }

    private static boolean inSkyblock(Minecraft mc) {
        Objective sidebar = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return false;
        return TextUtil.stripColor(sidebar.getDisplayName().getString()).toUpperCase().contains("SKYBLOCK");
    }
}
