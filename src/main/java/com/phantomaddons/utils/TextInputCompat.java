package com.phantomaddons.utils;

public final class TextInputCompat {

    private TextInputCompat() {}

    public static void set(Object owner, boolean active) {
        //? if >=26.3 {
        /*var manager = net.minecraft.client.Minecraft.getInstance().textInputManager();
        if (active) manager.startTextInput(owner);
        else manager.stopTextInput(owner);
        *///?}
    }
}
