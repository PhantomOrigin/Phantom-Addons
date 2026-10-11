package com.phantomaddons.gui;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TextInput extends RawTextInput {
    public TextInput(String n, Supplier<String> get, Consumer<String> set) { super(n, get, set); }
    @Override String toDisplay(String s) {
        if (s == null) return "";
        return s.startsWith("minecraft:") ? s.substring("minecraft:".length()) : s;
    }
    @Override String toStored(String s) {
        if (s == null || s.isBlank()) return s;
        return s.contains(":") ? s : "minecraft:" + s;
    }
}
