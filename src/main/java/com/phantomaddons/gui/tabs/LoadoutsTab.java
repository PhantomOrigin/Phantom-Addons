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
        Group wardrobe = m.group("Wardrobe Keybinds", PhantomConfig::isWardrobeEnabled, PhantomConfig::setWardrobeEnabled);
        String[] slotLabels = {"Slot 1","Slot 2","Slot 3","Slot 4","Slot 5",
                               "Slot 6","Slot 7","Slot 8","Slot 9"};
        for (int i = 0; i < 9; i++) {
            final int idx = i;
            wardrobe.add(new KeyCapture(slotLabels[i],
                    () -> PhantomConfig.getWardrobeSlotKeys()[idx],
                    v  -> PhantomConfig.setWardrobeSlotKey(idx, v)));
        }
        wardrobe.add(new KeyCapture("Open Wardrobe", PhantomConfig::getWardrobeOpenKey,     PhantomConfig::setWardrobeOpenKey));
        wardrobe.add(new KeyCapture("Stats Keybind", PhantomConfig::getStatsOpenKey,        PhantomConfig::setStatsOpenKey));
        wardrobe.add(new KeyCapture("Open Pets", PhantomConfig::getPetsOpenKey,         PhantomConfig::setPetsOpenKey));
        wardrobe.add(new KeyCapture("Equipment Wardrobe Keybind", PhantomConfig::getEqWardrobeOpenKey, PhantomConfig::setEqWardrobeOpenKey));
        wardrobe.add(new KeyCapture("Next Page", PhantomConfig::getWardrobeNextPageKey, PhantomConfig::setWardrobeNextPageKey));
        wardrobe.add(new KeyCapture("Prev Page", PhantomConfig::getWardrobePrevPageKey, PhantomConfig::setWardrobePrevPageKey));
        wardrobe.add(new KeyCapture("Unequip", PhantomConfig::getWardrobeUnequipKey,  PhantomConfig::setWardrobeUnequipKey));
        wardrobe.add(new Toggle("Disable Unequip",
                PhantomConfig::isWardrobeDisableUnequipEnabled, PhantomConfig::setWardrobeDisableUnequipEnabled));
        if (Edition.CURRENT.fullFeatureSet) {
            wardrobe.add(new Toggle("Auto Close Wardrobe",
                    PhantomConfig::isWardrobeAutoCloseEnabled, PhantomConfig::setWardrobeAutoCloseEnabled));
        }
        m.sound(wardrobe, PhantomConfig.SOUND_WARDROBE_SWAP);
        m.add(wardrobe);

        Group loadouts = m.group("Loadout Keybinds", PhantomConfig::isLoadoutsEnabled, PhantomConfig::setLoadoutsEnabled);
        loadouts.add(new KeyCapture("Loadouts Keybind", PhantomConfig::getLoadoutsOpenKey, PhantomConfig::setLoadoutsOpenKey));
        for (int i = 0; i < 12; i++) {
            final int idx = i;
            loadouts.add(new KeyCapture("Loadout Slot " + (idx + 1),
                    () -> PhantomConfig.getLoadoutSlotKeys()[idx],
                    v  -> PhantomConfig.setLoadoutSlotKey(idx, v)));
        }
        loadouts.add(new KeyCapture("Next Page", PhantomConfig::getWardrobeNextPageKey, PhantomConfig::setWardrobeNextPageKey));
        loadouts.add(new KeyCapture("Prev Page", PhantomConfig::getWardrobePrevPageKey, PhantomConfig::setWardrobePrevPageKey));
        if (Edition.CURRENT.fullFeatureSet) {
            loadouts.add(new Toggle("Auto Close Loadouts",
                    PhantomConfig::isWardrobeAutoCloseEnabled, PhantomConfig::setWardrobeAutoCloseEnabled));
            loadouts.add(new IntInput("Extra Auto Close Ms",
                    PhantomConfig::getWardrobeExtraAutoCloseMs, PhantomConfig::setWardrobeExtraAutoCloseMs)
                    .withTooltip("Add time here if you are experiencing a reopening with auto close"));
        }
        m.sound(loadouts, PhantomConfig.SOUND_WARDROBE_SWAP);
        m.add(loadouts);
    }
}
