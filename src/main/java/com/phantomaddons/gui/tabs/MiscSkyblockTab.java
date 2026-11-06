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
                PhantomConfig::getSelfPlayerScale, PhantomConfig::setSelfPlayerScale)
                .withTooltip("Changes how big you look."));
        m.add(new RangeSlider("Other Player Scale", 1.0f, 300.0f, "%.0f%%",
                PhantomConfig::getOtherPlayerScale, PhantomConfig::setOtherPlayerScale)
                .withTooltip("Changes how big other players look."));

        m.heading("Gameplay");
        if (Edition.CURRENT.fullFeatureSet) {
            Group pearlRefill = m.group("Pearl Refill", PhantomConfig::isPearlRefillEnabled, PhantomConfig::setPearlRefillEnabled)
                    .withTooltip("Refills your ender pearls from your sacks for you.");
            pearlRefill.add(new Toggle("Work Outside Kuudra",
                    PhantomConfig::isPearlRefillOutsideKuudraEnabled, PhantomConfig::setPearlRefillOutsideKuudraEnabled)
                    .withTooltip("Lets this work outside Kuudra too."));
            pearlRefill.add(new Toggle("Work In Dungeons",
                    PhantomConfig::isPearlRefillDungeonsEnabled, PhantomConfig::setPearlRefillDungeonsEnabled)
                    .withTooltip("Lets this work in dungeons too."));
            pearlRefill.withWarning(PingWarning.when(PhantomConfig::isPearlRefillEnabled), PingWarning.FEATURE_TEXT);
            m.add(pearlRefill);
        }

        m.add(new Toggle("Auto Sprint",
                PhantomConfig::isAutoSprintEnabled, PhantomConfig::setAutoSprintEnabled)
                .withTooltip("Keeps you sprinting automatically."));

        m.heading("Inventory");
        Group heads = m.group("Prevent Placing Player Heads", PhantomConfig::isPreventPlacingPlayerHeadsEnabled, PhantomConfig::setPreventPlacingPlayerHeadsEnabled)
                .withTooltip("Stops you placing player heads by accident.");
        heads.add(new Toggle("Except Garden",
                PhantomConfig::isPreventPlacingPlayerHeadsExceptGarden, PhantomConfig::setPreventPlacingPlayerHeadsExceptGarden)
                .withTooltip("Still lets you place them in the Garden."));
        m.add(heads);

        m.add(new Toggle("Prevent Placing Weapons",
                PhantomConfig::isPreventPlacingWeaponsEnabled, PhantomConfig::setPreventPlacingWeaponsEnabled)
                .withTooltip("Stops you placing weapons as blocks."));

        m.add(new Toggle("Block Close Item",
                PhantomConfig::isBlockCloseItemEnabled, PhantomConfig::setBlockCloseItemEnabled)
                .withTooltip("Stops you clicking a menu's Close button by accident."));

        Group slot = m.group("Slot Binds", PhantomConfig::isSlotBindsEnabled, PhantomConfig::setSlotBindsEnabled)
                .withTooltip("Lets you link two slots so items swap between them.");
        slot.add(new KeyCapture("Bind Key",
                PhantomConfig::getSlotBindSetKey, PhantomConfig::setSlotBindSetKey)
                .withTooltip("The key you use to bind slots."));
        slot.add(new Toggle("Show Binds on Hover",
                PhantomConfig::isSlotBindsShowOnHover, PhantomConfig::setSlotBindsShowOnHover)
                .withTooltip("Draws a line to the bound slot when you hover over one."));
        m.add(slot);

        m.heading("Commands");
        m.add(new Toggle("Case-Insensitive Commands",
                PhantomConfig::isCaseInsensitiveCommands, PhantomConfig::setCaseInsensitiveCommands)
                .withTooltip("Lets commands work whatever capital letters you type."));
    }
}
