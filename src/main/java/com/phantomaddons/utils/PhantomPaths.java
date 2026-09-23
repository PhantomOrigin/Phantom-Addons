package com.phantomaddons.utils;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class PhantomPaths {

    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("phantomaddons");

    private PhantomPaths() {}

    public static Path resolve(String filename) {
        try {
            Files.createDirectories(DIR);
        } catch (IOException ignored) {}

        Path target = DIR.resolve(filename);
        if (!Files.exists(target)) {
            Path legacy = FabricLoader.getInstance().getConfigDir().resolve(filename);
            if (Files.exists(legacy)) {
                try {
                    Files.move(legacy, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {}
            }
        }
        return target;
    }
}
