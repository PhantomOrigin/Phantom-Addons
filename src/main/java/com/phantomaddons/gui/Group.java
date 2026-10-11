package com.phantomaddons.gui;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Group extends Node {
    final String name;
    final String searchName;
    final Component nameLabel;
    final String key;
    final Supplier<Boolean> get;
    final Consumer<Boolean> set;
    final List<Node> children = new ArrayList<>();
    String tooltip;

    Group(String name, Tab tab, String key, Supplier<Boolean> get, Consumer<Boolean> set) {
        super(tab); this.name = name; this.key = key; this.get = get; this.set = set;
        searchName = name.toLowerCase();
        nameLabel = Component.literal(name);
    }

    public Group withTooltip(String t) { this.tooltip = t; return this; }
    public Group add(Feature f)        { children.add(new Leaf(f, tab)); return this; }
    public Group add(Group g)          { children.add(g); return this; }
}
