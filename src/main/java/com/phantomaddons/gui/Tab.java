package com.phantomaddons.gui;

import com.phantomaddons.gui.tabs.*;

import java.util.function.Consumer;

enum Tab {
    ABOUT(null, "About", AboutTab::build),

    LOADOUTS(     "Skyblock", "Loadouts", LoadoutsTab::build),
    PROFILE_VIEWER("Skyblock", "Profile Viewer", ProfileViewerTab::build),
    RENDER(       "Skyblock", "Render",   RenderTab::build),
    MISC_SKYBLOCK("Skyblock", "Misc",     MiscSkyblockTab::build),

    SUPPLIES(   "Kuudra", "Supplies", SuppliesTab::build),
    BUILD(      "Kuudra", "Build",    BuildTab::build),
    STUN_DPS(   "Kuudra", "Stun/DPS", StunDpsTab::build),
    BOSS(       "Kuudra", "Boss",     BossTab::build),
    MISC_KUUDRA("Kuudra", "Misc",     MiscKuudraTab::build),

    DUNGEONS_M7("Dungeons", "M7", DungeonsTab::build),

    FLUID_CUSTOM("Customisation", "Lava Customisation", LavaCustomisationTab::build),
    ITEM_CUSTOM( "Customisation", "Item Customisation", ItemCustomisationTab::build),
    VISUAL_WORDS("Customisation", "Visual Words",       VisualWordsTab::build);

    final String category;
    final String label;
    private final Consumer<MenuBuilder> builder;

    Tab(String category, String label, Consumer<MenuBuilder> builder) {
        this.category = category;
        this.label    = label;
        this.builder  = builder;
    }

    void build(MenuBuilder m) { builder.accept(m); }
}
