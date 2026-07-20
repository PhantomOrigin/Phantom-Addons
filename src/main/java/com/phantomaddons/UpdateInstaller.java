package com.phantomaddons;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/*
This file is excluded from the standard and no autoupdate versions of the mod
 */
public final class UpdateInstaller {

    private static final String STAGING_PREFIX = ".phantomaddons_staging-";
    private static final String STAGING_SUFFIX = ".jar";

    private UpdateInstaller() {}

    public static void downloadAndScheduleSwap(String downloadUrl, String version) throws Exception {
        if (downloadUrl == null) throw new Exception("No .jar download URL found in release assets");

        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        Path staging = modsDir.resolve(STAGING_PREFIX + version + STAGING_SUFFIX);

        PhantomAddons.LOGGER.info("[PhantomAddons] Downloading from {}", downloadUrl);
        URLConnection dl = new URL(downloadUrl).openConnection();
        dl.setRequestProperty("User-Agent", "PhantomAddons-UpdateChecker");
        dl.setConnectTimeout(10000);
        dl.setReadTimeout(120000);

        long expectedLength = dl.getContentLengthLong();
        long written;
        try (InputStream in = dl.getInputStream()) {
            written = Files.copy(in, staging, StandardCopyOption.REPLACE_EXISTING);
        }
        if (expectedLength > 0 && written != expectedLength) {
            Files.deleteIfExists(staging);
            throw new Exception("Download incomplete (" + written + "/" + expectedLength + " bytes)");
        }
        if (written < 1024) {
            Files.deleteIfExists(staging);
            throw new Exception("Downloaded file is too small to be a valid jar (" + written + " bytes)");
        }

        scheduleSwap(staging, version);
    }

    private static void scheduleSwap(Path staging, String version) {
        Path modsDir  = FabricLoader.getInstance().getGameDir().resolve("mods");
        Path finalJar = modsDir.resolve("phantomaddons-" + version + ".jar");

        Path currentJar = FabricLoader.getInstance()
                .getModContainer("phantomaddons")
                .map(c -> c.getOrigin().getPaths().get(0))
                .orElse(null);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.move(staging, finalJar, StandardCopyOption.REPLACE_EXISTING);
                if (currentJar != null && !currentJar.equals(finalJar)) {
                    removeOldJar(currentJar);
                }
                PhantomAddons.LOGGER.info("[PhantomAddons] Installed {}.", finalJar.getFileName());
            } catch (Exception ex) {
                PhantomAddons.LOGGER.error("[PhantomAddons] Failed to install update: {}",
                        ex.getMessage());
            }
        }, "PhantomAddons-UpdateSwap"));
    }

    private static void removeOldJar(Path jar) {
        try {
            Files.deleteIfExists(jar);
            if (!Files.exists(jar)) {
                PhantomAddons.LOGGER.info("[PhantomAddons] Old jar deleted: {}", jar.getFileName());
                return;
            }
        } catch (Exception ignored) {}

        try {
            Path disabled = jar.resolveSibling(jar.getFileName() + ".disabled");
            Files.move(jar, disabled, StandardCopyOption.REPLACE_EXISTING);
            PhantomAddons.LOGGER.info("[PhantomAddons] Old jar renamed to {} (will be removed on next startup).",
                    disabled.getFileName());
        } catch (Exception ex) {
            PhantomAddons.LOGGER.error("[PhantomAddons] Could not remove old jar {}: {}",
                    jar.getFileName(), ex.getMessage());
        }
    }

    public static void cleanupLeftoverJars() {
        try {
            Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
            try (var stream = Files.list(modsDir)) {
                stream.forEach(p -> {
                    String name = p.getFileName().toString();
                    if (name.endsWith(".jar.disabled")) {
                        try {
                            Files.deleteIfExists(p);
                            PhantomAddons.LOGGER.info("[PhantomAddons] Cleaned up leftover: {}", p.getFileName());
                        } catch (Exception ignored) {}
                    } else if (name.startsWith(STAGING_PREFIX) && name.endsWith(STAGING_SUFFIX)) {
                        String version = name.substring(STAGING_PREFIX.length(), name.length() - STAGING_SUFFIX.length());
                        Path finalJar = modsDir.resolve("phantomaddons-" + version + ".jar");
                        if (Files.exists(finalJar)) {
                            try {
                                Files.deleteIfExists(p);
                            } catch (Exception ignored) {}
                        } else {
                            PhantomAddons.LOGGER.info(
                                    "[PhantomAddons] Resuming interrupted update to {} left over from a previous session.",
                                    version);
                            scheduleSwap(p, version);
                        }
                    }
                });
            }
        } catch (Exception ignored) {}
    }
}
