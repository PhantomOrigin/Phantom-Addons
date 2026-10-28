package com.phantomaddons.gui;

import net.minecraft.network.chat.Component;

final class Heading extends Node {
    final String name;
    final Component label;

    Heading(String name, Tab tab) {
        super(tab);
        this.name = name;
        this.label = Component.literal(name);
    }
}
