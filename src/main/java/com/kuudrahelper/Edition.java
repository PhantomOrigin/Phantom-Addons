package com.kuudrahelper;

import java.io.IOException;
import java.io.InputStream;
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
