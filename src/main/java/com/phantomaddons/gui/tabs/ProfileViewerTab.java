package com.phantomaddons.gui.tabs;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.Cycle;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.RangeSlider;
import com.phantomaddons.gui.Toggle;

public final class ProfileViewerTab {

    private ProfileViewerTab() {}

    public static void build(MenuBuilder m) {
        m.heading("General");
        m.add(new Toggle("Profile Viewer (/pv)",
                PhantomConfig::isPvEnabled, PhantomConfig::setPvEnabled)
                .withTooltip("Turns on the /pv profile viewer."));

        m.add(new Toggle("Click Names To Open Profile",
                PhantomConfig::isPvClickNames, PhantomConfig::setPvClickNames)
                .withTooltip("Clicking a name in chat opens that player's profile."));

        m.add(new Toggle("Party Finder Shift Preview",
                PhantomConfig::isPvPartyFinderPreview, PhantomConfig::setPvPartyFinderPreview)
                .withTooltip("Hold Shift over a party to preview its members' profiles."));

        m.add(new Toggle("Preserve Last Open Page",
                PhantomConfig::isPvRememberPage, PhantomConfig::setPvRememberPage)
                .withTooltip("Reopens the profile viewer on the page you left it on."));

        m.add(new Toggle("Player Name Suggestions",
                PhantomConfig::isPvNameSuggestions, PhantomConfig::setPvNameSuggestions)
                .withTooltip("Suggests player names when you type /pv or /kuudra."));

        m.heading("Window");
        m.add(new RangeSlider("GUI Size", 0.6f, 2.5f, "%.2fx",
                PhantomConfig::getPvGuiScale, PhantomConfig::setPvGuiScale)
                .withTooltip("Changes how big the profile viewer is."));

        m.add(new Cycle("Content Size",
                () -> switch (PhantomConfig.getPvContent()) {
                    case SMALL  -> "Small";
                    case MEDIUM -> "Medium";
                    case LARGE  -> "Large";
                },
                () -> PhantomConfig.setPvContent(switch (PhantomConfig.getPvContent()) {
                    case SMALL  -> PhantomConfig.PvContent.MEDIUM;
                    case MEDIUM -> PhantomConfig.PvContent.LARGE;
                    case LARGE  -> PhantomConfig.PvContent.SMALL;
                }))
                .withTooltip("Changes how big the text and items inside it are."));

        m.add(new Cycle("Theme",
                () -> switch (PhantomConfig.getPvTheme()) {
                    case MATCH -> "Match Menu";
                    case DARK  -> "Dark";
                    case LIGHT -> "Light";
                },
                () -> PhantomConfig.setPvTheme(switch (PhantomConfig.getPvTheme()) {
                    case MATCH -> PhantomConfig.PvTheme.DARK;
                    case DARK  -> PhantomConfig.PvTheme.LIGHT;
                    case LIGHT -> PhantomConfig.PvTheme.MATCH;
                }))
                .withTooltip("Chooses a dark, light or menu-matching look."));

        m.add(new Cycle("Tab Style",
                () -> PhantomConfig.isPvTabIcons() ? "Icons" : "Text",
                () -> PhantomConfig.setPvTabIcons(!PhantomConfig.isPvTabIcons()))
                .withTooltip("Shows the tabs as icons or as text."));

        m.heading("Numbers");
        m.add(new Toggle("Replace Roman Numerals",
                PhantomConfig::isPvArabicNumerals, PhantomConfig::setPvArabicNumerals)
                .withTooltip("Shows normal numbers instead of Roman numerals."));

        m.add(new Toggle("Overflow Skill Levels",
                PhantomConfig::isPvOverflowSkills, PhantomConfig::setPvOverflowSkills)
                .withTooltip("Keeps counting levels past the maximum."));
    }
}
