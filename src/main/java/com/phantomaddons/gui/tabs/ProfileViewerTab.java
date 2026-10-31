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
                .withTooltip("/pv <player> opens a SkyBlock profile viewer. /pv on its own views your own profile."));

        m.add(new Toggle("Click Names To Open Profile",
                PhantomConfig::isPvClickNames, PhantomConfig::setPvClickNames)
                .withTooltip("Clicking a player's name in chat opens their profile viewer instead of starting a message to them. Holding Shift still inserts the name into the chat box."));

        m.add(new Toggle("Party Finder Shift Preview",
                PhantomConfig::isPvPartyFinderPreview, PhantomConfig::setPvPartyFinderPreview)
                .withTooltip("Hold Shift over a Kuudra party in the Party Finder to preview its members' profiles. This looks each member up on our server, and signs you in to it the first time you use it. Off by default."));

        m.add(new Toggle("Preserve Last Open Page",
                PhantomConfig::isPvRememberPage, PhantomConfig::setPvRememberPage)
                .withTooltip("Opening the profile viewer on someone you looked at in the last 10 minutes puts you back on the page and sub-tab you left it on, instead of the first page."));

        m.add(new Toggle("Player Name Suggestions",
                PhantomConfig::isPvNameSuggestions, PhantomConfig::setPvNameSuggestions)
                .withTooltip("Tab-completes /pv and /kuudra with your friends, players from your last 2 lobbies and anyone whose profile you opened in the last 5 minutes. Friends are learned from the messages Hypixel sends when you view your friend list, add or remove a friend, or a friend joins or leaves."));

        m.heading("Window");
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
                })));

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
                () -> PhantomConfig.setPvTabIcons(!PhantomConfig.isPvTabIcons())));

        m.heading("Numbers");
        m.add(new Toggle("Replace Roman Numerals",
                PhantomConfig::isPvArabicNumerals, PhantomConfig::setPvArabicNumerals)
                .withTooltip("Shows numbers instead of Roman numerals in the profile viewer, e.g. Sharpness 7 instead of Sharpness VII."));

        m.add(new Toggle("Overflow Skill Levels",
                PhantomConfig::isPvOverflowSkills, PhantomConfig::setPvOverflowSkills)
                .withTooltip("Keeps counting skill levels and the Catacombs level past their max using the XP earned beyond it, the same way as SkyHanni's overflow levels."));
    }
}
