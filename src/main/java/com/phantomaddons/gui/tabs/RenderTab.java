package com.phantomaddons.gui.tabs;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.Group;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.Toggle;

public final class RenderTab {

    private RenderTab() {}

    public static void build(MenuBuilder m) {
        m.add(new Toggle("Hide Falling Blocks",
                PhantomConfig::isHideFallingBlocksEnabled, PhantomConfig::setHideFallingBlocksEnabled)
                .withTooltip("Hides falling blocks."));
        m.add(new Toggle("Hide Entity Fire",
                PhantomConfig::isHideEntityFireEnabled, PhantomConfig::setHideEntityFireEnabled)
                .withTooltip("Hides fire on entities."));
        m.add(new Toggle("Hide Dead Enemies",
                PhantomConfig::isHideDeadEntitiesEnabled, PhantomConfig::setHideDeadEntitiesEnabled)
                .withTooltip("Hides enemies once they die."));
        m.add(new Toggle("Hide Selfie Cam",
                PhantomConfig::isHideSelfieEnabled, PhantomConfig::setHideSelfieEnabled)
                .withTooltip("Skips the front camera view when you change camera."));

        Group as = m.group("Hide Irrelevant Armor Stands", PhantomConfig::isHideArmorStandsEnabled, PhantomConfig::setHideArmorStandsEnabled)
                .withTooltip("Hides armor stands you don't need to see.");
        as.add(new Toggle("Build Area",
                PhantomConfig::isHideArmorStandsBuild, PhantomConfig::setHideArmorStandsBuild)
                .withTooltip("Hides armor stands at the build area."));
        as.add(new Toggle("Right Cannon",
                PhantomConfig::isHideArmorStandsRightCannon, PhantomConfig::setHideArmorStandsRightCannon)
                .withTooltip("Hides armor stands at the right cannon."));
        as.add(new Toggle("Left Cannon",
                PhantomConfig::isHideArmorStandsLeftCannon, PhantomConfig::setHideArmorStandsLeftCannon)
                .withTooltip("Hides armor stands at the left cannon."));
        as.add(new Toggle("Shop",
                PhantomConfig::isHideArmorStandsShop, PhantomConfig::setHideArmorStandsShop)
                .withTooltip("Hides armor stands at the shop."));
        as.add(new Toggle("Others",
                PhantomConfig::isHideArmorStandsOthers, PhantomConfig::setHideArmorStandsOthers)
                .withTooltip("Hides all other armor stands, including rat pets."));
        m.add(as);

        Group ch = m.group("Show Cannon Hitboxes", PhantomConfig::isCannonHitboxesEnabled, PhantomConfig::setCannonHitboxesEnabled)
                .withTooltip("Shows boxes around the cannons.");
        ch.add(new Toggle("Left Cannon",
                PhantomConfig::isCannonHitboxesLeftEnabled, PhantomConfig::setCannonHitboxesLeftEnabled)
                .withTooltip("Shows the left cannon's hitbox."));
        ch.add(new Toggle("Right Cannon",
                PhantomConfig::isCannonHitboxesRightEnabled, PhantomConfig::setCannonHitboxesRightEnabled)
                .withTooltip("Shows the right cannon's hitbox."));
        m.add(ch);
    }
}
