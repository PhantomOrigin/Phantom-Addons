package com.phantomaddons.gui.tabs;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.Group;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.Toggle;

public final class RenderTab {

    private RenderTab() {}

    public static void build(MenuBuilder m) {
        m.add(new Toggle("Hide Falling Blocks",
                PhantomConfig::isHideFallingBlocksEnabled, PhantomConfig::setHideFallingBlocksEnabled));
        m.add(new Toggle("Hide Entity Fire",
                PhantomConfig::isHideEntityFireEnabled, PhantomConfig::setHideEntityFireEnabled));
        m.add(new Toggle("Hide Dead Enemies",
                PhantomConfig::isHideDeadEntitiesEnabled, PhantomConfig::setHideDeadEntitiesEnabled));
        m.add(new Toggle("Hide Selfie Cam",
                PhantomConfig::isHideSelfieEnabled, PhantomConfig::setHideSelfieEnabled));

        Group as = m.group("Hide Irrelevant Armor Stands", PhantomConfig::isHideArmorStandsEnabled, PhantomConfig::setHideArmorStandsEnabled);
        as.add(new Toggle("Build Area",
                PhantomConfig::isHideArmorStandsBuild, PhantomConfig::setHideArmorStandsBuild));
        as.add(new Toggle("Right Cannon",
                PhantomConfig::isHideArmorStandsRightCannon, PhantomConfig::setHideArmorStandsRightCannon));
        as.add(new Toggle("Left Cannon",
                PhantomConfig::isHideArmorStandsLeftCannon, PhantomConfig::setHideArmorStandsLeftCannon));
        as.add(new Toggle("Shop",
                PhantomConfig::isHideArmorStandsShop, PhantomConfig::setHideArmorStandsShop));
        as.add(new Toggle("Others",
                PhantomConfig::isHideArmorStandsOthers, PhantomConfig::setHideArmorStandsOthers)
                .withTooltip("Warning: this hides rat pets"));
        m.add(as);

        Group ch = m.group("Show Cannon Hitboxes", PhantomConfig::isCannonHitboxesEnabled, PhantomConfig::setCannonHitboxesEnabled);
        ch.add(new Toggle("Left Cannon",
                PhantomConfig::isCannonHitboxesLeftEnabled, PhantomConfig::setCannonHitboxesLeftEnabled));
        ch.add(new Toggle("Right Cannon",
                PhantomConfig::isCannonHitboxesRightEnabled, PhantomConfig::setCannonHitboxesRightEnabled));
        m.add(ch);
    }
}
