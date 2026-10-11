package com.phantomaddons.gui.tabs;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.Group;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.RgbInput;
import com.phantomaddons.gui.Slider;
import com.phantomaddons.gui.Toggle;

public final class BuildTab {

    private BuildTab() {}

    public static void build(MenuBuilder m) {
        m.add(new Toggle("Build Progress Tracker",
                PhantomConfig::isBuildProgressHudEnabled, PhantomConfig::setBuildProgressHudEnabled));

        Group announce = m.group("Announce Fresh", PhantomConfig::isAnnounceFreshEnabled, PhantomConfig::setAnnounceFreshEnabled);
        announce.add(new Toggle("Fresh Notification",
                PhantomConfig::isFreshNotifyEnabled, PhantomConfig::setFreshNotifyEnabled));
        m.sound(announce, PhantomConfig.SOUND_FRESH);
        m.add(announce);

        Group buildStarted = m.group("Build Started Notification", PhantomConfig::isBuildStartedNotifyEnabled, PhantomConfig::setBuildStartedNotifyEnabled);
        m.sound(buildStarted, PhantomConfig.SOUND_BUILD_STARTED);
        m.add(buildStarted);
        Group buildBeacons = m.group("Build Beacons", PhantomConfig::isBuildBeaconsEnabled, PhantomConfig::setBuildBeaconsEnabled);
        buildBeacons.add(new Slider("Opacity",
                PhantomConfig::getBuildBeaconAlpha, PhantomConfig::setBuildBeaconAlpha, "%"));
        m.add(buildBeacons);

        Group elleHighlight = m.group("Elle Highlight", PhantomConfig::isElleHighlightEnabled, PhantomConfig::setElleHighlightEnabled);
        elleHighlight.add(new RgbInput("Outline Colour",
                PhantomConfig::getElleOutlineColor, PhantomConfig::setElleOutlineColor));
        elleHighlight.add(new Toggle("Show Beacon",
                PhantomConfig::isElleHighlightBeaconEnabled, PhantomConfig::setElleHighlightBeaconEnabled));
        elleHighlight.add(new RgbInput("Beacon Colour",
                PhantomConfig::getElleBeaconColor, PhantomConfig::setElleBeaconColor));
        m.add(elleHighlight);
    }
}
