package com.phantomaddons.gui;

public final class Links {

    public static final String DISCORD = "https://discord.gg/6MquvmrXNP";
    public static final String GITHUB = "https://github.com/PhantomOrigin/Phantom-Addons";

    private Links() {}

    public static void open(String url) {
        //? if <26.3 {
        net.minecraft.util.Util.getPlatform().openUri(url);
        //?} else {
        /*com.mojang.blaze3d.Blaze3D.openUri(java.net.URI.create(url));
        *///?}
    }
}
