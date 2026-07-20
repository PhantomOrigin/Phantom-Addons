package com.phantomaddons;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class UpdateChecker {

    private static final String API_URL    =
            "https://api.github.com/repos/PhantomOrigin/Phantom-Addons/releases/latest";
    private static final String GITHUB_URL =
            "https://github.com/PhantomOrigin/Phantom-Addons/releases/latest";

    private static final String ARCHIVE_BASE_NAME = "PhantomAddons";
    private static final String SUFFIX_FULL     = "full";
    private static final String SUFFIX_NOAUTO   = "full-noautoupdate";
    private static final String SUFFIX_STANDARD = ""; // Standard ships with no suffix: PhantomAddons-<version>.jar

    public enum State { IDLE, CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, DOWNLOADING, DOWNLOADED, ERROR }

    private static volatile State   state               = State.IDLE;
    private static volatile String  latestVersion       = null;
    private static volatile String  downloadUrl         = null;
    private static          boolean notifiedThisSession = false;

    private static final Map<String, String> releaseAssets = new HashMap<>();

    private UpdateChecker() {}

    public static State   getState()         { return state; }
    public static String  getLatestVersion() { return latestVersion; }
    public static boolean hasUpdate()        { return state == State.UPDATE_AVAILABLE || state == State.DOWNLOADED; }
    public static boolean isDownloaded()     { return state == State.DOWNLOADED; }

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (notifiedThisSession) return;
            if (state != State.UPDATE_AVAILABLE && state != State.DOWNLOADED) return;
            notifiedThisSession = true;

            client.execute(() -> {
                if (client.player == null) return;
                if (state == State.DOWNLOADED) {
                    client.player.sendSystemMessage(Component.literal(
                            "§f[PhantomAddons]§r §eVersion §a" + latestVersion
                                    + " §ehas been downloaded and will install on next restart."));
                } else {
                    client.player.sendSystemMessage(Component.literal(
                            "§f[PhantomAddons]§r §eNew version §a" + latestVersion
                                    + " §eavailable at §b" + GITHUB_URL));
                }
            });
        });
    }

    public static void checkOnStartup() {
        if (state == State.CHECKING || state == State.DOWNLOADING) return;
        state = State.CHECKING;

        CompletableFuture.runAsync(() -> {
            try {
                String current = currentVersion();
                fetchReleaseInfo();

                if (latestVersion == null || latestVersion.equals(current)) {
                    state = State.UP_TO_DATE;
                    PhantomAddons.LOGGER.info("[PhantomAddons] Up to date ({})", current);
                    return;
                }

                PhantomAddons.LOGGER.info("[PhantomAddons] Update available: {} → {}",
                        current, latestVersion);

                if (canAutoDownload()) {
                    state = State.DOWNLOADING;
                    invokeInstaller("downloadAndScheduleSwap", downloadUrl, latestVersion);
                    state = State.DOWNLOADED;
                    PhantomAddons.LOGGER.info("[PhantomAddons] {} downloaded — will install on restart.",
                            latestVersion);
                } else {
                    state = State.UPDATE_AVAILABLE;
                }
            } catch (Exception e) {
                state = State.ERROR;
                PhantomAddons.LOGGER.warn("[PhantomAddons] Update check failed: {}", e.getMessage());
            }
        });
    }

    public static void downloadManually() {
        if (state == State.DOWNLOADING || state == State.DOWNLOADED) return;
        if (!Edition.CURRENT.autoDownloadCapable) {
            openReleasePage();
            return;
        }
        state = State.CHECKING;
        CompletableFuture.runAsync(() -> {
            try {
                latestVersion = null;
                downloadUrl   = null;
                releaseAssets.clear();
                fetchReleaseInfo();

                if (latestVersion == null) throw new Exception("Could not fetch release info");

                String current = currentVersion();
                if (latestVersion.equals(current)) {
                    state = State.UP_TO_DATE;
                    notifyChat("§a Already on the latest version (" + current + ")");
                    return;
                }

                if (!PhantomConfig.isAutoUpdatesEnabled() || !allEditionAssetsPresent()) {
                    state = State.UPDATE_AVAILABLE;
                    notifyChat("§eCan't auto-download right now — opening the release page instead.");
                    openReleasePage();
                    return;
                }

                state = State.DOWNLOADING;
                invokeInstaller("downloadAndScheduleSwap", downloadUrl, latestVersion);
                state = State.DOWNLOADED;
                notifyChat("§a Version §e" + latestVersion
                        + "§a downloaded — restart the game to install it.");
            } catch (Exception e) {
                state = State.ERROR;
                notifyChat("§c Download failed: " + e.getMessage());
                PhantomAddons.LOGGER.warn("[PhantomAddons] Manual download failed: {}", e.getMessage());
            }
        });
    }

    private static boolean canAutoDownload() {
        if (!Edition.CURRENT.autoDownloadCapable) return false;
        if (!PhantomConfig.isAutoUpdatesEnabled()) return false;
        if (!allEditionAssetsPresent()) {
            PhantomAddons.LOGGER.warn(
                    "[PhantomAddons] Release {} is missing one or more edition jars — "
                            + "falling back to notify-only until all three are uploaded.",
                    latestVersion);
            return false;
        }
        return true;
    }

    private static boolean allEditionAssetsPresent() {
        return releaseAssets.containsKey(expectedAssetName(SUFFIX_FULL))
                && releaseAssets.containsKey(expectedAssetName(SUFFIX_NOAUTO))
                && releaseAssets.containsKey(expectedAssetName(SUFFIX_STANDARD));
    }

    private static String expectedAssetName(String suffix) {
        return suffix.isEmpty()
                ? ARCHIVE_BASE_NAME + "-" + latestVersion + ".jar"
                : ARCHIVE_BASE_NAME + "-" + latestVersion + "-" + suffix + ".jar";
    }

    private static String ownEditionSuffix() {
        return switch (Edition.CURRENT) {
            case FULL                 -> SUFFIX_FULL;
            case FULL_NO_AUTO_UPDATE  -> SUFFIX_NOAUTO;
            case STANDARD             -> SUFFIX_STANDARD;
        };
    }

    private static void openReleasePage() {
        try {
            net.minecraft.util.Util.getPlatform().openUri(GITHUB_URL);
        } catch (Exception ignored) {}
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static void fetchReleaseInfo() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "PhantomAddons-UpdateChecker");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        if (conn.getResponseCode() != 200)
            throw new Exception("GitHub API returned HTTP " + conn.getResponseCode());

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        String json = sb.toString();

        int idx = json.indexOf("\"tag_name\"");
        if (idx >= 0) {
            int s = json.indexOf('"', idx + 10) + 1;
            String tag = json.substring(s, json.indexOf('"', s));
            latestVersion = tag.startsWith("v") ? tag.substring(1) : tag;
        }

        releaseAssets.clear();
        int pos = 0;
        while ((pos = json.indexOf("browser_download_url", pos)) >= 0) {
            int s = json.indexOf('"', pos + 22) + 1;
            int e = json.indexOf('"', s);
            String url = json.substring(s, e);
            if (url.endsWith(".jar")) {
                int nameStart = url.lastIndexOf('/') + 1;
                releaseAssets.put(url.substring(nameStart), url);
            }
            pos = e;
        }

        if (latestVersion != null) {
            downloadUrl = releaseAssets.get(expectedAssetName(ownEditionSuffix()));
        }
    }

    private static void invokeInstaller(String methodName, String... args) throws Exception {
        Class<?>[] types = new Class<?>[args.length];
        java.util.Arrays.fill(types, String.class);
        try {
            Class<?> cls = Class.forName("com.phantomaddons.UpdateInstaller");
            cls.getMethod(methodName, types).invoke(null, (Object[]) args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) throw ex;
            throw e;
        } catch (ReflectiveOperationException e) {
            throw new Exception("Auto-download isn't available in this edition", e);
        }
    }

    public static void cleanupLeftoverJars() {
        if (!Edition.CURRENT.autoDownloadCapable) return;
        try {
            invokeInstaller("cleanupLeftoverJars");
        } catch (Exception e) {
            PhantomAddons.LOGGER.error("[PhantomAddons] Cleanup failed: {}", e.getMessage());
        }
    }

    public static String currentVersion() {
        return FabricLoader.getInstance()
                .getModContainer("phantomaddons")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static void notifyChat(String message) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null)
                mc.player.sendSystemMessage(Component.literal("§f[PhantomAddons]§r " + message));
        });
    }
}