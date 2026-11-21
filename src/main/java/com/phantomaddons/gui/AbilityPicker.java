package com.phantomaddons.gui;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.abilities.AbilityData;
import com.phantomaddons.features.abilities.AbilityData.Ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AbilityPicker extends ChecklistPicker<Ability> {

    public AbilityPicker(String name) { super(name); }

    private static int order(Ability a) {
        return switch (a.type) {
            case AbilityData.WEAPON -> 0;
            case AbilityData.ARMOR -> 1;
            case AbilityData.PET -> 2;
            default -> 3;
        };
    }

    private static String seconds(double s) {
        if (s >= 60 && s % 60 == 0) return (int) (s / 60) + "m";
        return s % 1 == 0 ? (int) s + "s" : String.format(Locale.ROOT, "%.1fs", s);
    }

    private static int selectedCount() {
        int n = 0;
        for (String id : PhantomConfig.abilities().selected) if (AbilityData.byId(id) != null) n++;
        return n;
    }

    @Override List<Ability> entries() {
        List<Ability> out = new ArrayList<>(AbilityData.all());
        out.sort((a, b) -> {
            int c = Integer.compare(order(a), order(b));
            return c != 0 ? c : a.name.compareToIgnoreCase(b.name);
        });
        return out;
    }

    @Override String searchText(Ability a) {
        return a.name + " " + a.itemsText() + " " + a.typeLabel() + " " + a.type;
    }

    @Override boolean isOn(Ability a) { return PhantomConfig.abilities().isSelected(a.id); }

    @Override void toggle(Ability a) {
        var s = PhantomConfig.abilities();
        s.setSelected(a.id, !s.isSelected(a.id));
        PhantomConfig.save();
    }

    @Override String label(Ability a) { return a.name; }

    @Override String detail(Ability a) {
        String more = a.items != null && a.items.size() > 1 ? " +" + (a.items.size() - 1) : "";
        return a.itemName + more + " · " + a.typeLabel();
    }

    @Override String trailing(Ability a) { return seconds(a.cooldown); }

    @Override String summary() {
        int n = selectedCount();
        return n == 0 ? "None selected" : n + " selected";
    }

    @Override boolean summaryDim() { return selectedCount() == 0; }

    @Override String searchHint() { return "Search abilities, items, weapon, armor, pet..."; }

    @Override String emptyText() { return "No abilities found"; }
}
