package com.phantomaddons.gui.tabs;

import com.phantomaddons.DataFeatures;
import com.phantomaddons.features.customisation.NameGradient;
import com.phantomaddons.features.customisation.VisualWords;
import com.phantomaddons.gui.Button;
import com.phantomaddons.gui.Cycle;
import com.phantomaddons.gui.IntInput;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.RawTextInput;
import com.phantomaddons.gui.Toggle;
import com.phantomaddons.gui.VisualWordEntry;

public final class VisualWordsTab {

    private VisualWordsTab() {}

    public static void build(MenuBuilder m) {
        m.add(new Toggle("Visual Words", VisualWords::isEnabled, VisualWords::setEnabled)
                .withTooltip("Replaces chosen words with your own text everywhere they appear."));
        m.add(new Button("Add Word", "+ Add", () -> { VisualWords.addRule(); m.screen().rebuild(); })
                .withTooltip("Adds a new word to replace."));
        int vwCount = VisualWords.getRules().size();
        for (int i = 0; i < vwCount; i++) {
            m.add(new VisualWordEntry(i, () -> m.screen().rebuild())
                    .withTooltip("Type the word to find and the text to show instead."));
            if (DataFeatures.hasHighlight()) addGradientRows(m, i);
        }
    }

    /** Dev Builds Only for testing*/
    private static void addGradientRows(MenuBuilder m, int idx) {
        m.add(new RawTextInput("  Gradient Colors",
                () -> rule(idx, r -> r.gradient, ""), v -> edit(idx, r -> r.gradient = v == null ? "" : v)));
        m.add(new IntInput("  Gradient Length",
                () -> rule(idx, r -> r.gradientLength, 0), v -> edit(idx, r -> r.gradientLength = Math.max(0, v)))
                .withTooltip("How long the gradient is."));
        m.add(new Cycle("  Gradient Direction",
                () -> NameGradient.normalise(rule(idx, r -> r.gradientDirection, "right")),
                () -> edit(idx, r -> r.gradientDirection = NameGradient.nextDirection(r.gradientDirection)))
                .withTooltip("Which way the gradient moves."));
        m.add(new IntInput("  Gradient Speed",
                () -> Math.round(rule(idx, r -> r.gradientSpeed, NameGradient.DEFAULT_SPEED)),
                v -> edit(idx, r -> r.gradientSpeed = Math.max(0, v)))
                .withTooltip("How fast the gradient moves."));
    }

    private static <T> T rule(int idx, java.util.function.Function<VisualWords.Rule, T> get, T fallback) {
        var rules = VisualWords.getRules();
        return idx >= 0 && idx < rules.size() ? get.apply(rules.get(idx)) : fallback;
    }

    private static void edit(int idx, java.util.function.Consumer<VisualWords.Rule> change) {
        var rules = VisualWords.getRules();
        if (idx < 0 || idx >= rules.size()) return;
        change.accept(rules.get(idx));
        VisualWords.save();
    }
}
