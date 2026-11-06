package com.phantomaddons.gui.tabs;

import com.phantomaddons.Edition;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.Group;
import com.phantomaddons.gui.IntInput;
import com.phantomaddons.gui.KeyCapture;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.Toggle;

public final class LoadoutsTab {

    private LoadoutsTab() {}

    public static void build(MenuBuilder m) {
        Group wardrobe = m.group("Wardrobe Keybinds", PhantomConfig::isWardrobeEnabled, PhantomConfig::setWardrobeEnabled)
                .withTooltip("Lets you pick wardrobe slots with keys.");
        String[] slotLabels = {"Slot 1","Slot 2","Slot 3","Slot 4","Slot 5",
                               "Slot 6","Slot 7","Slot 8","Slot 9"};
        for (int i = 0; i < 9; i++) {
            final int idx = i;
            wardrobe.add(new KeyCapture(slotLabels[i],
                    () -> PhantomConfig.getWardrobeSlotKeys()[idx],
                    v  -> PhantomConfig.setWardrobeSlotKey(idx, v))
                    .withTooltip("Equips this wardrobe slot."));
        }
        wardrobe.add(new KeyCapture("Open Wardrobe", PhantomConfig::getWardrobeOpenKey,     PhantomConfig::setWardrobeOpenKey)
                .withTooltip("Opens the wardrobe."));
        wardrobe.add(new KeyCapture("Stats Keybind", PhantomConfig::getStatsOpenKey,        PhantomConfig::setStatsOpenKey)
                .withTooltip("Opens your Equipment and Stats menu."));
        wardrobe.add(new KeyCapture("Open Pets", PhantomConfig::getPetsOpenKey,         PhantomConfig::setPetsOpenKey)
                .withTooltip("Opens your pets menu."));
        wardrobe.add(new KeyCapture("Equipment Wardrobe Keybind", PhantomConfig::getEqWardrobeOpenKey, PhantomConfig::setEqWardrobeOpenKey)
                .withTooltip("Opens the equipment wardrobe."));
        wardrobe.add(new KeyCapture("Next Page", PhantomConfig::getWardrobeNextPageKey, PhantomConfig::setWardrobeNextPageKey)
                .withTooltip("Goes to the next wardrobe page."));
        wardrobe.add(new KeyCapture("Prev Page", PhantomConfig::getWardrobePrevPageKey, PhantomConfig::setWardrobePrevPageKey)
                .withTooltip("Goes to the previous wardrobe page."));
        wardrobe.add(new KeyCapture("Unequip", PhantomConfig::getWardrobeUnequipKey,  PhantomConfig::setWardrobeUnequipKey)
                .withTooltip("Takes off your current armor set."));
        wardrobe.add(new Toggle("Disable Unequip",
                PhantomConfig::isWardrobeDisableUnequipEnabled, PhantomConfig::setWardrobeDisableUnequipEnabled)
                .withTooltip("Stops you taking your armor off by accident."));
        if (Edition.CURRENT.fullFeatureSet) {
            wardrobe.add(new Toggle("Auto Close Wardrobe",
                    PhantomConfig::isWardrobeAutoCloseEnabled, PhantomConfig::setWardrobeAutoCloseEnabled)
                    .withTooltip("Closes the wardrobe after you pick a set."));
        }
        m.sound(wardrobe, PhantomConfig.SOUND_WARDROBE_SWAP);
        m.add(wardrobe);

        Group loadouts = m.group("Loadout Keybinds", PhantomConfig::isLoadoutsEnabled, PhantomConfig::setLoadoutsEnabled)
                .withTooltip("Lets you pick loadouts with keys.");
        loadouts.add(new KeyCapture("Loadouts Keybind", PhantomConfig::getLoadoutsOpenKey, PhantomConfig::setLoadoutsOpenKey)
                .withTooltip("Opens the loadouts menu."));
        for (int i = 0; i < 12; i++) {
            final int idx = i;
            loadouts.add(new KeyCapture("Loadout Slot " + (idx + 1),
                    () -> PhantomConfig.getLoadoutSlotKeys()[idx],
                    v  -> PhantomConfig.setLoadoutSlotKey(idx, v))
                    .withTooltip("Equips this loadout."));
        }
        loadouts.add(new KeyCapture("Next Page", PhantomConfig::getWardrobeNextPageKey, PhantomConfig::setWardrobeNextPageKey)
                .withTooltip("Goes to the next loadout page."));
        loadouts.add(new KeyCapture("Prev Page", PhantomConfig::getWardrobePrevPageKey, PhantomConfig::setWardrobePrevPageKey)
                .withTooltip("Goes to the previous loadout page."));
        if (Edition.CURRENT.fullFeatureSet) {
            loadouts.add(new Toggle("Auto Close Loadouts",
                    PhantomConfig::isWardrobeAutoCloseEnabled, PhantomConfig::setWardrobeAutoCloseEnabled)
                    .withTooltip("Closes the menu after you pick a loadout."));
            loadouts.add(new IntInput("Extra Auto Close Ms",
                    PhantomConfig::getWardrobeExtraAutoCloseMs, PhantomConfig::setWardrobeExtraAutoCloseMs)
                    .withTooltip("Increases the menu suppression time after closing loadout menu. Increase this value if you see the menu reopen after closing"));
        }
        m.sound(loadouts, PhantomConfig.SOUND_WARDROBE_SWAP);
        m.add(loadouts);
    }
}
