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

        Group m7toxic = m.group("M7 Auto GFS Toxic", PhantomConfig::isAutoGfsToxicEnabled, PhantomConfig::setAutoGfsToxic)
                .withTooltip("Gets Toxic Arrow Poison from your sacks for you in M7 dragons phase.");
        m7toxic.add(new IntInput("Toxic Amount", PhantomConfig::getToxicAmount, PhantomConfig::setToxicAmount)
                .withTooltip("How many Toxic Arrow Poison to get each time."));
        m.add(m7toxic);

        Group m7twi = m.group("M7 Auto GFS Twilight", PhantomConfig::isAutoGfsTwilightEnabled, PhantomConfig::setAutoGfsTwilight)
                .withTooltip("Gets Twilight Arrow Poison from your sacks for you in M7 dragons phase.");
        m7twi.add(new IntInput("Twilight Amount", PhantomConfig::getTwilightAmount, PhantomConfig::setTwilightAmount)
                .withTooltip("How many Twilight Arrow Poison to get each time."));
        m.add(m7twi);

        m.add(m.group("Auto GFS Inflatable Jerry", PhantomConfig::isAutoGfsJerryEnabled, PhantomConfig::setAutoGfsJerry)
                .withTooltip("Every 5 seconds, refills Inflatable Jerry to 64 from your sacks if you hold fewer than 20. Does nothing if you have none."));
        m.add(m.group("Auto GFS Spirit Leap", PhantomConfig::isAutoGfsSpiritLeapEnabled, PhantomConfig::setAutoGfsSpiritLeap)
                .withTooltip("Every 5 seconds, refills Spirit Leap to 64 from your sacks if you hold fewer than 20. Does nothing if you have none."));
        m.add(m.group("Auto GFS Superboom TNT", PhantomConfig::isAutoGfsSuperboomEnabled, PhantomConfig::setAutoGfsSuperboom)
                .withTooltip("Every 5 seconds, refills Superboom TNT to 64 from your sacks if you hold fewer than 20. Does nothing if you have none."));
    }
}
