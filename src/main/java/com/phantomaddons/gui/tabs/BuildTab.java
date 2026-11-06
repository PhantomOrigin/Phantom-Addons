package com.phantomaddons.gui.tabs;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.Cycle;
import com.phantomaddons.gui.Group;
import com.phantomaddons.gui.IntInput;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.RangeSlider;
import com.phantomaddons.gui.RgbInput;
import com.phantomaddons.gui.Slider;
import com.phantomaddons.gui.Toggle;

public final class BuildTab {

    private BuildTab() {}

    public static void build(MenuBuilder m) {
        m.add(new Toggle("Build Progress Tracker",
                PhantomConfig::isBuildProgressHudEnabled, PhantomConfig::setBuildProgressHudEnabled)
                .withTooltip("Shows how far the build is."));

        Group announce = m.group("Announce Fresh", PhantomConfig::isAnnounceFreshEnabled, PhantomConfig::setAnnounceFreshEnabled)
                .withTooltip("Tells your party when you get Fresh.");
        announce.add(new Toggle("Fresh Notification",
                PhantomConfig::isFreshNotifyEnabled, PhantomConfig::setFreshNotifyEnabled)
                .withTooltip("Shows a notification when you get Fresh."));
        m.sound(announce, PhantomConfig.SOUND_FRESH);
        m.add(announce);

        m.add(new Toggle("Build Start Timer",
                PhantomConfig::isBuildStartTimerEnabled, PhantomConfig::setBuildStartTimerEnabled)
                .withTooltip("Shows the countdown until the build starts."));

        Group buildStarted = m.group("Build Started Notification", PhantomConfig::isBuildStartedNotifyEnabled, PhantomConfig::setBuildStartedNotifyEnabled)
                .withTooltip("Shows a notification when the build starts.");
        m.sound(buildStarted, PhantomConfig.SOUND_BUILD_STARTED);
        m.add(buildStarted);

        Group stunReminder = m.group("Stun Reminder", PhantomConfig::isStunReminderEnabled, PhantomConfig::setStunReminderEnabled)
                .withTooltip("Warns you to leave the build before it finishes without you at the cannon.");
        stunReminder.add(new Cycle("Role Mode",
                () -> PhantomConfig.getStunReminderRoleMode().name(),
                () -> PhantomConfig.setStunReminderRoleMode(switch (PhantomConfig.getStunReminderRoleMode()) {
                    case DPS  -> PhantomConfig.RoleMode.STUN;
                    case STUN -> PhantomConfig.RoleMode.AUTO;
                    case AUTO -> PhantomConfig.RoleMode.DPS;
                }))
                .withTooltip("Chooses whether you count as DPS or Stun, or lets the mod decide."));
        stunReminder.add(new IntInput("Time To Cannon (ms)",
                PhantomConfig::getStunReminderTravelMs, PhantomConfig::setStunReminderTravelMs)
                .withTooltip("How long it takes you to reach the cannon."));
        stunReminder.add(new RangeSlider("Leave At Progress", 1, 100, "%.0f%%",
                () -> (float) PhantomConfig.getStunReminderMaxPercent(),
                v  -> PhantomConfig.setStunReminderMaxPercent(Math.round(v)))
                .withTooltip("The build percentage where the warning appears."));
        m.sound(stunReminder, PhantomConfig.SOUND_STUN_REMINDER);
        m.add(stunReminder);

        Group buildBeacons = m.group("Build Beacons", PhantomConfig::isBuildBeaconsEnabled, PhantomConfig::setBuildBeaconsEnabled)
                .withTooltip("Shows beams at the build piles.");
        buildBeacons.add(new Slider("Opacity",
                PhantomConfig::getBuildBeaconAlpha, PhantomConfig::setBuildBeaconAlpha, "%")
                .withTooltip("How see-through the beams are."));
        m.add(buildBeacons);

        Group elleHighlight = m.group("Elle Highlight", PhantomConfig::isElleHighlightEnabled, PhantomConfig::setElleHighlightEnabled)
                .withTooltip("Highlights Elle so she is easier to see.");
        elleHighlight.add(new RgbInput("Outline Colour",
                PhantomConfig::getElleOutlineColor, PhantomConfig::setElleOutlineColor));
        elleHighlight.add(new Toggle("Show Beacon",
                PhantomConfig::isElleHighlightBeaconEnabled, PhantomConfig::setElleHighlightBeaconEnabled)
                .withTooltip("Adds a beam above Elle."));
        elleHighlight.add(new RgbInput("Beacon Colour",
                PhantomConfig::getElleBeaconColor, PhantomConfig::setElleBeaconColor));
        m.add(elleHighlight);
    }
}
