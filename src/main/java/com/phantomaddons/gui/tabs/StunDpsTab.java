package com.phantomaddons.gui.tabs;

import com.phantomaddons.Edition;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.PingWarning;
import com.phantomaddons.gui.Cycle;
import com.phantomaddons.gui.Group;
import com.phantomaddons.gui.IntInput;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.RangeSlider;
import com.phantomaddons.gui.Toggle;

public final class StunDpsTab {

    private StunDpsTab() {}

    public static void build(MenuBuilder m) {
        if (Edition.CURRENT.fullFeatureSet) {
            Group gfs = m.group("Auto GFS", PhantomConfig::isAutoGfsEnabled, PhantomConfig::setAutoGfsEnabled)
                    .withTooltip("Gets arrow poison from your sacks for you.");
            gfs.add(new Cycle("Role Mode",
                    () -> PhantomConfig.getRoleMode().name(),
                    () -> PhantomConfig.setRoleMode(switch (PhantomConfig.getRoleMode()) {
                        case DPS  -> PhantomConfig.RoleMode.STUN;
                        case STUN -> PhantomConfig.RoleMode.AUTO;
                        case AUTO -> PhantomConfig.RoleMode.DPS;
                    }))
                    .withTooltip("Chooses whether you count as DPS or Stun, or lets the mod decide."));
            gfs.add(new IntInput("DPS Amount", PhantomConfig::getDpsValue,  PhantomConfig::setDpsValue)
                    .withTooltip("How many Toxic Arrow Poison to get at the start."));
            gfs.add(new IntInput("Stun Amount", PhantomConfig::getStunValue, PhantomConfig::setStunValue)
                    .withTooltip("How many Twilight Arrow Poison to get at the start."));
            gfs.add(new IntInput("Refill Amount", PhantomConfig::getDpsRefillAmount, PhantomConfig::setDpsRefillAmount)
                    .withTooltip("How many Toxic Arrow Poison to get when you run low."));
            gfs.add(new RangeSlider("Disable Refill Below HP", 25, 100, "%.0f%%",
                    () -> (float) PhantomConfig.getAutoGfsDisableHpPercent(),
                    v  -> PhantomConfig.setAutoGfsDisableHpPercent(Math.round(v)))
                    .withTooltip("Stops refilling once Kuudra's health is below this percent."));
            m.add(gfs);
        }

        m.add(new Toggle("Pickobulus Blocker",
                PhantomConfig::isPickoblockEnabled, PhantomConfig::setPickoblockEnabled)
                .withTooltip("Stops you using Pickobulus too early.")
                .withWarning(PingWarning.when(PhantomConfig::isPickoblockEnabled), PingWarning.FEATURE_TEXT));

        Group eaten = m.group("Eaten Timer", PhantomConfig::isEatenTimerEnabled, PhantomConfig::setEatenTimerEnabled)
                .withTooltip("Shows a timer for the Eaten phase.");
        eaten.add(new Toggle("Subtract Ping",
                PhantomConfig::isEatenTimerSubtractPingEnabled, PhantomConfig::setEatenTimerSubtractPingEnabled)
                .withTooltip("Takes your ping off the timer."));
        eaten.withWarning(PingWarning.when(PingWarning::eatenTimerUsesPing), PingWarning.FEATURE_TEXT);
        m.add(eaten);

        if (Edition.CURRENT.fullFeatureSet) {
            m.add(new Toggle("Cannon Auto Close",
                    PhantomConfig::isCannonAutoCloseEnabled, PhantomConfig::setCannonAutoCloseEnabled)
                    .withTooltip("Closes the shop menu after you buy the cannon."));
        }
        Group stunPreview = m.group("Stun Preview", PhantomConfig::isStunPreviewEnabled, PhantomConfig::setStunPreviewEnabled)
                .withTooltip("Shows a preview of where the stun will land.");
        stunPreview.add(new Toggle("Left",
                PhantomConfig::isStunPreviewLeftEnabled, PhantomConfig::setStunPreviewLeftEnabled)
                .withTooltip("Shows the preview for the left side."));
        stunPreview.add(new Toggle("Right",
                PhantomConfig::isStunPreviewRightEnabled, PhantomConfig::setStunPreviewRightEnabled)
                .withTooltip("Shows the preview for the right side."));
        stunPreview.add(new Toggle("Back",
                PhantomConfig::isStunPreviewBackEnabled, PhantomConfig::setStunPreviewBackEnabled)
                .withTooltip("Shows the preview for the back side."));
        m.add(stunPreview);

        Group fastDps = m.group("Fast DPS Warning", PhantomConfig::isFastDpsWarningEnabled, PhantomConfig::setFastDpsWarningEnabled)
                .withTooltip("Warns you when DPS is fast enough to make Kuudra appear in odd places.");
        fastDps.add(new Toggle("Fast DPS Notification",
                PhantomConfig::isFastDpsNotifyEnabled, PhantomConfig::setFastDpsNotifyEnabled)
                .withTooltip("Shows a notification when DPS was too fast."));
        m.sound(fastDps, PhantomConfig.SOUND_FAST_DPS);
        m.add(fastDps);

        m.add(new Toggle("DPS Waypoint",
                PhantomConfig::isDpsWaypointEnabled, PhantomConfig::setDpsWaypointEnabled)
                .withTooltip("Shows your DPS spot during Eaten and Stun."));
    }
}
