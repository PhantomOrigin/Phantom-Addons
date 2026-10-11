package com.phantomaddons.gui.tabs;

import com.phantomaddons.Edition;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.Group;
import com.phantomaddons.gui.IntInput;
import com.phantomaddons.gui.MenuBuilder;

public final class DungeonsTab {

    private DungeonsTab() {}

    public static void build(MenuBuilder m) {
        if (!Edition.CURRENT.fullFeatureSet) return;

        Group m7toxic = m.group("M7 Auto GFS Toxic", PhantomConfig::isAutoGfsToxicEnabled, PhantomConfig::setAutoGfsToxic);
        m7toxic.add(new IntInput("Toxic Amount", PhantomConfig::getToxicAmount, PhantomConfig::setToxicAmount));
        m.add(m7toxic);

        Group m7twi = m.group("M7 Auto GFS Twilight", PhantomConfig::isAutoGfsTwilightEnabled, PhantomConfig::setAutoGfsTwilight);
        m7twi.add(new IntInput("Twilight Amount", PhantomConfig::getTwilightAmount, PhantomConfig::setTwilightAmount));
        m.add(m7twi);
    }
}
