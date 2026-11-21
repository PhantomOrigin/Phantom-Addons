package com.phantomaddons.gui.tabs;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.abilities.AbilityData;
import com.phantomaddons.features.abilities.AbilitySettings;
import com.phantomaddons.gui.AbilityPicker;
import com.phantomaddons.gui.Cycle;
import com.phantomaddons.gui.Group;
import com.phantomaddons.gui.InfoRow;
import com.phantomaddons.gui.IslandPicker;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.RawTextInput;
import com.phantomaddons.gui.RgbInput;
import com.phantomaddons.gui.Theme;
import com.phantomaddons.gui.Toggle;

import java.util.function.Consumer;

public final class AbilityCooldownsTab {

    private AbilityCooldownsTab() {}

    private static AbilitySettings cfg() { return PhantomConfig.abilities(); }

    private static void edit(Consumer<AbilitySettings> change) {
        change.accept(cfg());
        PhantomConfig.save();
    }

    public static void build(MenuBuilder m) {
        m.add(new Toggle("Ability Cooldowns",
                () -> cfg().enabled, v -> edit(s -> s.enabled = v))
                .withTooltip("Shows a timer when you use an ability you've picked."));
        m.add(new Toggle("Smooth Updates",
                () -> cfg().smooth, v -> edit(s -> s.smooth = v))
                .withTooltip("Off updates once per game tick."));

        m.heading("Abilities");
        m.add(new AbilityPicker("Abilities"));

        Group groups = m.section("Groups")
                .withTooltip("Abilities in the same group share a list.")
                .dynamic(() -> String.join("\n", cfg().selected), AbilityCooldownsTab::populateGroups);
        m.add(groups);

        Group rules = m.section("Group Rules")
                .withTooltip("Where and how each group shows.")
                .dynamic(AbilityCooldownsTab::rulesStamp, AbilityCooldownsTab::populateRules);
        m.add(rules);
    }

    private static String rulesStamp() {
        StringBuilder sb = new StringBuilder();
        for (String id : cfg().selected) sb.append(id).append('=').append(cfg().groupText(id)).append('\n');
        return sb.toString();
    }

    private static void populateGroups(Group section) {
        boolean any = false;
        for (String id : cfg().selected) {
            AbilityData.Ability a = AbilityData.byId(id);
            if (a == null) continue;
            any = true;
            section.add(new RawTextInput(a.name + " (" + a.itemName + ")",
                    () -> cfg().groupText(id),
                    v -> edit(s -> s.setGroupText(id, v)))
                    .withTooltip("Groups separated by commas, e.g. 1,2. 0 = its own icon."));
        }
        if (!any) {
            section.add(new InfoRow("Nothing switched on",
                    () -> new InfoRow.Value("Pick abilities above", Theme.C_TEXT_DIM)));
        }
    }

    // ── Group rules ───────────────────────────────────────────────────────────

    private static String labelName(int mode) {
        return switch (mode) {
            case AbilitySettings.LABEL_ABILITY -> "Ability Name";
            case AbilitySettings.LABEL_ITEM -> "Item Name";
            case AbilitySettings.LABEL_NONE -> "None";
            default -> "Item Icon";
        };
    }

    private static String formatName(int format) {
        return switch (format) {
            case AbilitySettings.FORMAT_TICKS -> "Ticks";
            case AbilitySettings.FORMAT_MS -> "Milliseconds";
            default -> "Seconds";
        };
    }

    /** Hides or shows a whole group's list (or the individual icons); its settings are kept. */
    private static Toggle showToggle(String name, int group) {
        return new Toggle(name,
                () -> cfg().style(group).enabled,
                v -> edit(s -> s.editStyle(group).enabled = v));
    }

    /** The look settings shared by every group, including the individual icons (group 0). */
    private static void addStyleRows(Group g, int group) {
        g.add(new Cycle("Label",
                () -> labelName(cfg().style(group).label),
                () -> edit(s -> { var st = s.editStyle(group); st.label = (st.label + 1) % 4; })));
        g.add(new Cycle("Time Format",
                () -> formatName(cfg().style(group).format),
                () -> edit(s -> { var st = s.editStyle(group); st.format = (st.format + 1) % 3; })));
        g.add(new Toggle("Show Active Time",
                () -> cfg().style(group).showActive,
                v -> edit(s -> s.editStyle(group).showActive = v)));
        g.add(new Toggle("Show As Bar",
                () -> cfg().style(group).bar,
                v -> edit(s -> s.editStyle(group).bar = v)));
        g.add(new RgbInput("Cooldown Colour",
                () -> cfg().style(group).cooldownColor,
                v -> edit(s -> s.editStyle(group).cooldownColor = v & 0xFFFFFF)));
        g.add(new RgbInput("Active Time Colour",
                () -> cfg().style(group).activeColor,
                v -> edit(s -> s.editStyle(group).activeColor = v & 0xFFFFFF)));
    }

    /** One dropdown per group in use, holding its islands and how it looks; the individual icons get one too. */
    private static void populateRules(Group section) {
        boolean any = false;
        for (int g : cfg().activeGroups()) {
            any = true;
            Group sub = section.sub("g" + g, "Group " + g);
            sub.add(showToggle("Show Group", g));
            sub.add(new IslandPicker("Islands", AbilitySettings.ruleKey(g, "")));
            addStyleRows(sub, g);
            section.add(sub);
        }
        var individual = cfg().individualAbilities();
        if (!individual.isEmpty()) {
            any = true;
            Group sub = section.sub("g0", "Individual Icons");
            sub.add(showToggle("Show Individual Icons", 0));
            addStyleRows(sub, 0);
            for (String id : individual) { // each icon has its own islands
                AbilityData.Ability a = AbilityData.byId(id);
                if (a != null) sub.add(new IslandPicker(a.name + " Islands", AbilitySettings.ruleKey(0, id)));
            }
            section.add(sub);
        }
        if (!any) {
            section.add(new InfoRow("No groups yet",
                    () -> new InfoRow.Value("Pick abilities above", Theme.C_TEXT_DIM)));
        }
    }
}
