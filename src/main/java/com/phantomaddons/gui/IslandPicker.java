package com.phantomaddons.gui;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.abilities.Island;

import java.util.List;

public class IslandPicker extends ChecklistPicker<Island> {
    private final String ruleKey;

    public IslandPicker(String name, String ruleKey) {
        super(name);
        this.ruleKey = ruleKey;
    }

    @Override List<Island> entries() { return Island.selectable(); }

    @Override String searchText(Island i) { return i.label; }

    @Override boolean isOn(Island i) { return PhantomConfig.abilities().worksOn(ruleKey, i.name()); }

    @Override void toggle(Island i) {
        var s = PhantomConfig.abilities();
        s.setWorksOn(ruleKey, i.name(), !s.worksOn(ruleKey, i.name()));
        PhantomConfig.save();
    }

    @Override String label(Island i) { return i.label; }

    @Override String summary() {
        var s = PhantomConfig.abilities();
        if (s.worksNowhere(ruleKey)) return "None";
        int total = Island.selectable().size(), on = 0;
        for (Island i : Island.selectable()) if (s.worksOn(ruleKey, i.name())) on++;
        return on == total ? "All places" : on + " of " + total;
    }

    @Override boolean summaryDim() { return PhantomConfig.abilities().worksNowhere(ruleKey); }

    @Override boolean hasBulk() { return true; }

    @Override void setAll(boolean on) {
        PhantomConfig.abilities().setWorksEverywhere(ruleKey, on);
        PhantomConfig.save();
    }

    @Override String searchHint() { return "Search islands..."; }

    @Override String emptyText() { return "No places found"; }
}
