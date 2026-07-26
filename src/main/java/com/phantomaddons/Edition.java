package com.phantomaddons;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;


public enum Edition {
    FULL(true, true),
    FULL_NO_AUTO_UPDATE(true, false),
    STANDARD(false, false);

    public final boolean fullFeatureSet;
    public final boolean autoDownloadCapable;

    Edition(boolean fullFeatureSet, boolean autoDownloadCapable) {
        this.fullFeatureSet     = fullFeatureSet;
        this.autoDownloadCapable = autoDownloadCapable;
    }

    public static final Edition CURRENT = load();

    private static Edition load() {
        try {
            Path path = FabricLoader.getInstance()
                    .getModContainer("phantomaddons")
                    .flatMap(c -> c.findPath("edition.properties"))
                    .orElse(null);
            if (path != null) {
                try (InputStream in = Files.newInputStream(path)) {
                    Properties p = new Properties();
                    p.load(in);
                    return Edition.valueOf(p.getProperty("edition", "FULL"));
                }
            }
        } catch (IOException | IllegalArgumentException ignored) {
        }

        try (InputStream in = Edition.class.getClassLoader().getResourceAsStream("edition.properties")) {
            if (in == null) return FULL;
            Properties p = new Properties();
            p.load(in);
            return Edition.valueOf(p.getProperty("edition", "FULL"));
        } catch (IOException | IllegalArgumentException e) {
            return FULL;
        }
    }
}
