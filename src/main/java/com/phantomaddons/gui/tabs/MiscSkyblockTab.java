package com.phantomaddons.gui.tabs;

import com.phantomaddons.Edition;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.PingWarning;
import com.phantomaddons.gui.Group;
import com.phantomaddons.gui.KeyCapture;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.RangeSlider;
import com.phantomaddons.gui.Toggle;

public final class MiscSkyblockTab {

    private MiscSkyblockTab() {}

    public static void build(MenuBuilder m) {
        m.heading("Scaling");
        m.add(new RangeSlider("Self Player Scale", 1.0f, 300.0f, "%.0f%%",
                PhantomConfig::getSelfPlayerScale, PhantomConfig::setSelfPlayerScale));
        m.add(new RangeSlider("Other Player Scale", 1.0f, 300.0f, "%.0f%%",
                PhantomConfig::getOtherPlayerScale, PhantomConfig::setOtherPlayerScale));

        m.heading("Gameplay");
        if (Edition.CURRENT.fullFeatureSet) {
            Group pearlRefill = m.group("Pearl Refill", PhantomConfig::isPearlRefillEnabled, PhantomConfig::setPearlRefillEnabled);
            pearlRefill.add(new Toggle("Work Outside Kuudra",
                    PhantomConfig::isPearlRefillOutsideKuudraEnabled, PhantomConfig::setPearlRefillOutsideKuudraEnabled));
            pearlRefill.add(new Toggle("Work In Dungeons",
                    PhantomConfig::isPearlRefillDungeonsEnabled, PhantomConfig::setPearlRefillDungeonsEnabled));
            pearlRefill.withWarning(PingWarning.when(PhantomConfig::isPearlRefillEnabled), PingWarning.FEATURE_TEXT);
            m.add(pearlRefill);
        }

        m.add(new Toggle("Auto Sprint",
                PhantomConfig::isAutoSprintEnabled, PhantomConfig::setAutoSprintEnabled));

        m.heading("Inventory");
        Group heads = m.group("Prevent Placing Player Heads", PhantomConfig::isPreventPlacingPlayerHeadsEnabled, PhantomConfig::setPreventPlacingPlayerHeadsEnabled);
        heads.add(new Toggle("Except Garden",
                PhantomConfig::isPreventPlacingPlayerHeadsExceptGarden, PhantomConfig::setPreventPlacingPlayerHeadsExceptGarden));
        m.add(heads);

        m.add(new Toggle("Prevent Placing Weapons",
                PhantomConfig::isPreventPlacingWeaponsEnabled, PhantomConfig::setPreventPlacingWeaponsEnabled));

        m.add(new Toggle("Block Close Item",
                PhantomConfig::isBlockCloseItemEnabled, PhantomConfig::setBlockCloseItemEnabled));

        Group slot = m.group("Slot Binds", PhantomConfig::isSlotBindsEnabled, PhantomConfig::setSlotBindsEnabled);
        slot.add(new KeyCapture("Bind Key",
                PhantomConfig::getSlotBindSetKey, PhantomConfig::setSlotBindSetKey));
        slot.add(new Toggle("Show Binds on Hover",
                PhantomConfig::isSlotBindsShowOnHover, PhantomConfig::setSlotBindsShowOnHover)
                .withTooltip("Draws a line to the bound hotbar/inventory slot while hovering either end of the pair"));
        m.add(slot);

        m.heading("Commands");
        m.add(new Toggle("Case-Insensitive Commands",
                PhantomConfig::isCaseInsensitiveCommands, PhantomConfig::setCaseInsensitiveCommands));
    }
}
