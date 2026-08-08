package com.phantomaddons;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/*
This file is excluded from the standard and no autoupdate versions of the mod
 */
public final class UpdateInstaller {

    private static final String STAGING_PREFIX = ".phantomaddons_staging-";
    private static final String PENDING_REMOVAL_FILE = "phantomaddons_pending_jar_removal.txt";
    // Fabric Loader scans mods/<minecraft-version>/ in addition to the flat mods/ root, and some
    // launchers nest jars a level or two further (e.g. a modpack-managed subfolder). A depth of 3
    // (mods/, mods/*/, mods/*/*/) comfortably covers real-world layouts without risking a runaway
    // walk if a mod ships a deeply nested resource/cache folder inside mods/.
    private static final int MAX_SCAN_DEPTH = 3;

    private UpdateInstaller() {}

    public static void downloadAndScheduleSwap(String downloadUrl, String version) throws Exception {
        if (downloadUrl == null) throw new Exception("No .jar download URL found in release assets");
        String targetFileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
        if (targetFileName.isEmpty() || !targetFileName.endsWith(".jar")) {
            throw new Exception("Download URL doesn't point to a .jar file: " + downloadUrl);
        }

        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        Path staging = modsDir.resolve(STAGING_PREFIX + targetFileName);

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

        scheduleSwap(staging, targetFileName);
    }

    private static void scheduleSwap(Path staging, String targetFileName) {
        Path modsDir  = FabricLoader.getInstance().getGameDir().resolve("mods");
        Path finalJar = modsDir.resolve(targetFileName);

        Path currentJar = FabricLoader.getInstance()
                .getModContainer("phantomaddons")
                .map(c -> c.getOrigin().getPaths().get(0))
                .orElse(null);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.move(staging, finalJar, StandardCopyOption.REPLACE_EXISTING);
                PhantomAddons.LOGGER.info("[PhantomAddons] Installed {}.", finalJar.getFileName());
            } catch (Exception ex) {
                PhantomAddons.LOGGER.error("[PhantomAddons] Failed to install update: {}",
                        ex.getMessage());
                return;
            }
            if (currentJar != null && !currentJar.equals(finalJar)) {
                removeOldJar(currentJar);
            }
        }, "PhantomAddons-UpdateSwap"));
    }

    private static void removeOldJar(Path jar) {
        try {
            if (Files.deleteIfExists(jar)) {
                PhantomAddons.LOGGER.info("[PhantomAddons] Old jar deleted: {}", jar.getFileName());
                return;
            }
        } catch (Exception ignored) {}

        try {
            Path marker = FabricLoader.getInstance().getConfigDir().resolve(PENDING_REMOVAL_FILE);
            Files.writeString(marker, jar.toAbsolutePath() + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            PhantomAddons.LOGGER.info(
                    "[PhantomAddons] Old jar {} is still in use — scheduled for removal on next launch.",
                    jar.getFileName());
        } catch (Exception ex) {
            PhantomAddons.LOGGER.error("[PhantomAddons] Could not schedule old jar {} for removal: {}",
                    jar.getFileName(), ex.getMessage());
        }
    }

    public static void cleanupLeftoverJars() {
        try {
            Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
            removePendingJars();
            removeDuplicatePhantomAddonsJars(modsDir);

            // Recursive (not Files.list): Fabric Loader itself scans mods/<minecraft-version>/ in
            // addition to the flat mods/ root, and some launchers/modpacks nest jars in their own
            // subfolders under mods/ too. A flat listing here would silently miss leftover staging
            // files or .disabled jars sitting in any of those, leaving them stranded forever.
            try (var stream = Files.walk(modsDir, MAX_SCAN_DEPTH)) {
                stream.filter(Files::isRegularFile).forEach(p -> {
                    String name = p.getFileName().toString();
                    if (name.endsWith(".jar.disabled")) {
                        try {
                            Files.deleteIfExists(p);
                            PhantomAddons.LOGGER.info("[PhantomAddons] Cleaned up leftover: {}", p.getFileName());
                        } catch (Exception ignored) {}
                    } else if (name.startsWith(STAGING_PREFIX) && name.endsWith(".jar")) {
                        String targetFileName = name.substring(STAGING_PREFIX.length());
                        Path finalJar = modsDir.resolve(targetFileName);
                        if (Files.exists(finalJar)) {
                            try {
                                Files.deleteIfExists(p);
                            } catch (Exception ignored) {}
                        } else {
                            PhantomAddons.LOGGER.info(
                                    "[PhantomAddons] Resuming interrupted update to {} left over from a previous session.",
                                    targetFileName);
                            scheduleSwap(p, targetFileName);
                        }
                    }
                });
            }
        } catch (Exception ignored) {}
    }

    private static void removeDuplicatePhantomAddonsJars(Path modsDir) {
        var container = FabricLoader.getInstance().getModContainer("phantomaddons").orElse(null);
        Path currentJar = container == null ? null
                : container.getOrigin().getPaths().get(0).toAbsolutePath().normalize();
        String runningVersion = container == null ? null
                : container.getMetadata().getVersion().getFriendlyString();

        // Recursive for the same reason as cleanupLeftoverJars — stale jars can be sitting in
        // mods/<minecraft-version>/ or another launcher-specific subfolder, not just the flat root.
        try (var stream = Files.walk(modsDir, MAX_SCAN_DEPTH)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                String name = p.getFileName().toString();
                if (!name.startsWith("PhantomAddons-") || !name.endsWith(".jar")) return;
                if (currentJar != null && p.toAbsolutePath().normalize().equals(currentJar)) return;
                
                String candidateVersion = parseVersionFromJarName(name);
                if (runningVersion == null || candidateVersion == null
                        || !isOlderVersion(candidateVersion, runningVersion)) {
                    return;
                }

                try {
                    if (Files.deleteIfExists(p)) {
                        PhantomAddons.LOGGER.info(
                                "[PhantomAddons] Removed stale jar from a previous install: {}", p.getFileName());
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    private static String parseVersionFromJarName(String fileName) {
        if (!fileName.startsWith("PhantomAddons-") || !fileName.endsWith(".jar")) return null;
        String base = fileName.substring("PhantomAddons-".length(), fileName.length() - ".jar".length());
        if (base.endsWith("-full-noautoupdate")) {
            base = base.substring(0, base.length() - "-full-noautoupdate".length());
        } else if (base.endsWith("-full")) {
            base = base.substring(0, base.length() - "-full".length());
        }
        return base.isEmpty() ? null : base;
    }

    private static boolean isOlderVersion(String candidate, String running) {
        try {
            String[] a = candidate.split("[.\\-]");
            String[] b = running.split("[.\\-]");
            int len = Math.max(a.length, b.length);
            for (int i = 0; i < len; i++) {
                int av = i < a.length ? Integer.parseInt(a[i]) : 0;
                int bv = i < b.length ? Integer.parseInt(b[i]) : 0;
                if (av != bv) return av < bv;
            }
            return false; // equal versions — not older
        } catch (Exception e) {
            return false;
        }
    }

    private static void removePendingJars() {
        Path marker = FabricLoader.getInstance().getConfigDir().resolve(PENDING_REMOVAL_FILE);
        if (!Files.exists(marker)) return;

        try {
            for (String line : Files.readAllLines(marker)) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    Path jar = Path.of(line);
                    if (Files.deleteIfExists(jar)) {
                        PhantomAddons.LOGGER.info(
                                "[PhantomAddons] Removed old jar left over from a previous update: {}",
                                jar.getFileName());
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        try {
            Files.deleteIfExists(marker);
        } catch (Exception ignored) {}
    }
}
