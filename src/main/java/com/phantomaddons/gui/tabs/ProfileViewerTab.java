package com.phantomaddons.gui.tabs;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.Cycle;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.RangeSlider;
import com.phantomaddons.gui.Toggle;

public final class ProfileViewerTab {

    private ProfileViewerTab() {}

    public static void build(MenuBuilder m) {
        m.add(new Toggle("Profile Viewer (/pv)",
                PhantomConfig::isPvEnabled, PhantomConfig::setPvEnabled)
                .withTooltip("/pv <player> opens a SkyBlock profile viewer. /pv on its own views your own profile."));

        m.add(new RangeSlider("GUI Size", 0.6f, 2.5f, "%.2fx",
                PhantomConfig::getPvGuiScale, PhantomConfig::setPvGuiScale)
                .withTooltip("Size of the profile viewer window. Everything inside it, text included, grows with it."));

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
                .withTooltip("How big the items and text are compared with the window. Small fits more on each page; large shows less, bigger. Pages rearrange to suit."));

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
                })));

        m.add(new Cycle("Tab Style",
                () -> PhantomConfig.isPvTabIcons() ? "Icons" : "Text",
                () -> PhantomConfig.setPvTabIcons(!PhantomConfig.isPvTabIcons()))
                .withTooltip("Text shows each tab's name along the top, scrolling when there are too many. Icons shows one small icon per tab, all on one screen, with the name when you hover it."));

        m.add(new Toggle("Replace Roman Numerals",
                PhantomConfig::isPvArabicNumerals, PhantomConfig::setPvArabicNumerals)
                .withTooltip("Shows numbers instead of Roman numerals in the profile viewer, e.g. Sharpness 7 instead of Sharpness VII."));

        m.add(new Toggle("Overflow Skill Levels",
                PhantomConfig::isPvOverflowSkills, PhantomConfig::setPvOverflowSkills)
                .withTooltip("Keeps counting skill levels and the Catacombs level past their max using the XP earned beyond it, the same way as SkyHanni's overflow levels."));
    }
}
