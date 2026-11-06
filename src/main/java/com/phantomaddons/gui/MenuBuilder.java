package com.phantomaddons.gui;

import com.phantomaddons.PhantomConfig;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MenuBuilder {

    private final PhantomScreen screen;
    private final List<Node> roots;
    private Tab tab;

    MenuBuilder(PhantomScreen screen, List<Node> roots) {
        this.screen = screen;
        this.roots  = roots;
    }

    void setTab(Tab tab) { this.tab = tab; }

    public PhantomScreen screen() { return screen; }

    public void add(Feature f) { roots.add(new Leaf(f, tab)); }
    public void add(Group g)   { roots.add(g); }
    public void heading(String name) { roots.add(new Heading(name, tab)); }

    public Group group(String name, Supplier<Boolean> get, Consumer<Boolean> set) {
        return new Group(name, tab, tab.name() + "/" + name, get, set);
    }

    public Group group(String name, Group parent, Supplier<Boolean> get, Consumer<Boolean> set) {
        return new Group(name, tab, parent.key + "/" + name, get, set);
    }

    public Group section(String name) { return group(name, null, null); }

    public Group section(String name, Group parent) { return group(name, parent, null, null); }

    public void sound(Group parent, String soundKey) {
        Group g = group("Sound", parent,
                () -> PhantomConfig.isNotificationSoundEnabled(soundKey),
                v  -> PhantomConfig.setNotificationSoundEnabled(soundKey, v));
        g.add(new SoundPicker("Selected Sound",
                () -> PhantomConfig.getNotificationSoundId(soundKey),
                v  -> PhantomConfig.setNotificationSoundId(soundKey, v)));
        g.add(new RangeSlider("Volume", 0f, 100f, "%.0f%%",
                () -> Math.min(1f, PhantomConfig.getNotificationSoundVolume(soundKey)) * 100f,
                v  -> PhantomConfig.setNotificationSoundVolume(soundKey, v / 100f)));
        g.add(new RangeSlider("Pitch", 0.1f, 4f, "%.2f",
                () -> PhantomConfig.getNotificationSoundPitch(soundKey),
                v  -> PhantomConfig.setNotificationSoundPitch(soundKey, v)));
        g.add(new RangeSlider("Repeat Count", 1f, 10f, "%.0f",
                () -> (float) PhantomConfig.getNotificationSoundRepeats(soundKey),
                v  -> PhantomConfig.setNotificationSoundRepeats(soundKey, Math.round(v)))
                .withTooltip("How many times the sound plays."));
        g.add(new RangeSlider("Repeat Delay (ticks)", 1f, 20f, "%.0f",
                () -> (float) PhantomConfig.getNotificationSoundRepeatTicks(soundKey),
                v  -> PhantomConfig.setNotificationSoundRepeatTicks(soundKey, Math.round(v)))
                .withTooltip("How many ticks to wait between each repeat."));
        g.add(new Button("Preview", "Play", () -> PhantomConfig.previewNotificationSound(soundKey))
                .withTooltip("Plays the sound with your current settings."));
        parent.add(g);
    }
}
