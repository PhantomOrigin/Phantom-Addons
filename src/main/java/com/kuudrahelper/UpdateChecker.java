package com.kuudrahelper;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

public final class UpdateChecker {

    private static final String API_URL    =
            "https://api.github.com/repos/PhantomOrigin/Phantom-Addons/releases/latest";
    private static final String GITHUB_URL =
            "https://github.com/PhantomOrigin/Phantom-Addons/releases/latest";

    public enum State { IDLE, CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, DOWNLOADING, DOWNLOADED, ERROR }

    private static volatile State   state               = State.IDLE;
    private static volatile String  latestVersion       = null;
    private static volatile String  downloadUrl         = null;
    private static          boolean notifiedThisSession = false;

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
                    KuudraHelperMod.LOGGER.info("[PhantomAddons] Up to date ({})", current);
                    return;
                }

                KuudraHelperMod.LOGGER.info("[PhantomAddons] Update available: {} → {}",
                        current, latestVersion);

                if (KuudraConfig.isAutoUpdatesEnabled()) {
                    state = State.DOWNLOADING;
                    downloadAndScheduleSwap();
                    state = State.DOWNLOADED;
                    KuudraHelperMod.LOGGER.info("[PhantomAddons] {} downloaded — will install on restart.",
                            latestVersion);
                } else {
                    state = State.UPDATE_AVAILABLE;
                }
            } catch (Exception e) {
                state = State.ERROR;
                KuudraHelperMod.LOGGER.warn("[PhantomAddons] Update check failed: {}", e.getMessage());
            }
        });
    }

    public static void downloadManually() {
        if (state == State.DOWNLOADING || state == State.DOWNLOADED) return;
        state = State.CHECKING;
        CompletableFuture.runAsync(() -> {
            try {
                latestVersion = null;
                downloadUrl   = null;
                fetchReleaseInfo();

                if (latestVersion == null) throw new Exception("Could not fetch release info");

                String current = currentVersion();
                if (latestVersion.equals(current)) {
                    state = State.UP_TO_DATE;
                    notifyChat("§a Already on the latest version (" + current + ")");
                    return;
                }

                state = State.DOWNLOADING;
                downloadAndScheduleSwap();
                state = State.DOWNLOADED;
                notifyChat("§a Version §e" + latestVersion
                        + "§a downloaded — restart the game to install it.");
            } catch (Exception e) {
                state = State.ERROR;
                notifyChat("§c Download failed: " + e.getMessage());
                KuudraHelperMod.LOGGER.warn("[PhantomAddons] Manual download failed: {}", e.getMessage());
            }
        });
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
        
        int pos = 0;
        while ((pos = json.indexOf("browser_download_url", pos)) >= 0) {
            int s = json.indexOf('"', pos + 22) + 1;
            int e = json.indexOf('"', s);
            String url = json.substring(s, e);
            if (url.endsWith(".jar")) { downloadUrl = url; break; }
            pos = e;
        }
    }

    private static void downloadAndScheduleSwap() throws Exception {
        if (downloadUrl == null) throw new Exception("No .jar download URL found in release assets");

        Path currentJar = FabricLoader.getInstance()
                .getModContainer("phantomaddons")
                .map(c -> c.getOrigin().getPaths().get(0))
                .orElseThrow(() -> new Exception("Cannot locate current phantomaddons jar"));

        Path modsDir  = FabricLoader.getInstance().getGameDir().resolve("mods");
        Path staging  = modsDir.resolve(".phantomaddons_staging.jar");
        Path finalJar = modsDir.resolve("phantomaddons-" + latestVersion + ".jar");

        KuudraHelperMod.LOGGER.info("[PhantomAddons] Downloading from {}", downloadUrl);
        URLConnection dl = new URL(downloadUrl).openConnection();
        dl.setRequestProperty("User-Agent", "PhantomAddons-UpdateChecker");
        dl.setConnectTimeout(10000);
        dl.setReadTimeout(120000);

        try (InputStream in = dl.getInputStream()) {
            Files.copy(in, staging, StandardCopyOption.REPLACE_EXISTING);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.move(staging, finalJar, StandardCopyOption.REPLACE_EXISTING);
                if (!currentJar.equals(finalJar)) {
                    removeOldJar(currentJar);
                }
                KuudraHelperMod.LOGGER.info("[PhantomAddons] Installed {}.", finalJar.getFileName());
            } catch (Exception ex) {
                KuudraHelperMod.LOGGER.error("[PhantomAddons] Failed to install update: {}",
                        ex.getMessage());
            }
        }, "PhantomAddons-UpdateSwap"));
    }

    /**
     * Deletes the old jar after an update. On Windows the JVM may still hold the file
     * open (via a memory-mapped class-file), so direct deletion often fails. We fall back
     * to renaming it with a ".disabled" suffix — renames succeed even on locked files
     * because Windows opens JARs with FILE_SHARE_DELETE. The renamed file is cleaned up
     * by cleanupLeftoverJars() on the next startup, when the old jar is no longer loaded.
     */
    private static void removeOldJar(Path jar) {
        // Try direct deletion first (works on Mac / Linux and sometimes on Windows).
        try {
            Files.deleteIfExists(jar);
            if (!Files.exists(jar)) {
                KuudraHelperMod.LOGGER.info("[PhantomAddons] Old jar deleted: {}", jar.getFileName());
                return;
            }
        } catch (Exception ignored) {}

        // Fallback: rename to .disabled so Fabric ignores it on next load,
        // and cleanupLeftoverJars() can delete it on the next startup.
        try {
            Path disabled = jar.resolveSibling(jar.getFileName() + ".disabled");
            Files.move(jar, disabled, StandardCopyOption.REPLACE_EXISTING);
            KuudraHelperMod.LOGGER.info("[PhantomAddons] Old jar renamed to {} (will be removed on next startup).",
                    disabled.getFileName());
        } catch (Exception ex) {
            KuudraHelperMod.LOGGER.error("[PhantomAddons] Could not remove old jar {}: {}",
                    jar.getFileName(), ex.getMessage());
        }
    }

    /**
     * Deletes any *.jar.disabled files left in the mods directory from a previous
     * Windows update cycle. Call this early in onInitializeClient().
     */
    public static void cleanupLeftoverJars() {
        try {
            Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
            try (var stream = Files.list(modsDir)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".jar.disabled"))
                      .forEach(p -> {
                          try {
                              Files.deleteIfExists(p);
                              KuudraHelperMod.LOGGER.info("[PhantomAddons] Cleaned up leftover: {}", p.getFileName());
                          } catch (Exception ignored) {}
                      });
            }
        } catch (Exception ignored) {}
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