package com.phantomaddons.gui.tabs;

import com.phantomaddons.Edition;
import com.phantomaddons.PhantomConfig;
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
            Group gfs = m.group("Auto GFS", PhantomConfig::isAutoGfsEnabled, PhantomConfig::setAutoGfsEnabled);
            gfs.add(new Cycle("Role Mode",
                    () -> PhantomConfig.getRoleMode().name(),
                    () -> PhantomConfig.setRoleMode(switch (PhantomConfig.getRoleMode()) {
                        case DPS  -> PhantomConfig.RoleMode.STUN;
                        case STUN -> PhantomConfig.RoleMode.AUTO;
                        case AUTO -> PhantomConfig.RoleMode.DPS;
                    })));
            gfs.add(new IntInput("DPS Amount", PhantomConfig::getDpsValue,  PhantomConfig::setDpsValue));
            gfs.add(new IntInput("Stun Amount", PhantomConfig::getStunValue, PhantomConfig::setStunValue));
            gfs.add(new IntInput("Refill Amount", PhantomConfig::getDpsRefillAmount, PhantomConfig::setDpsRefillAmount));
            gfs.add(new RangeSlider("Disable Refill Below HP", 25, 100, "%.0f%%",
                    () -> (float) PhantomConfig.getAutoGfsDisableHpPercent(),
                    v  -> PhantomConfig.setAutoGfsDisableHpPercent(Math.round(v))));
            m.add(gfs);
        }

        m.add(new Toggle("Pickobulus Blocker",
                PhantomConfig::isPickoblockEnabled, PhantomConfig::setPickoblockEnabled)
                .withTooltip("Prevents early use of pickobulus in most situations"));

        Group eaten = m.group("Eaten Timer", PhantomConfig::isEatenTimerEnabled, PhantomConfig::setEatenTimerEnabled);
        eaten.add(new Toggle("Subtract Ping",
                PhantomConfig::isEatenTimerSubtractPingEnabled, PhantomConfig::setEatenTimerSubtractPingEnabled));
        m.add(eaten);

        if (Edition.CURRENT.fullFeatureSet) {
            m.add(new Toggle("Cannon Auto Close",
                    PhantomConfig::isCannonAutoCloseEnabled, PhantomConfig::setCannonAutoCloseEnabled));
        }
        Group stunPreview = m.group("Stun Preview", PhantomConfig::isStunPreviewEnabled, PhantomConfig::setStunPreviewEnabled);
        stunPreview.add(new Toggle("Left",
                PhantomConfig::isStunPreviewLeftEnabled, PhantomConfig::setStunPreviewLeftEnabled));
        stunPreview.add(new Toggle("Right",
                PhantomConfig::isStunPreviewRightEnabled, PhantomConfig::setStunPreviewRightEnabled));
        stunPreview.add(new Toggle("Back",
                PhantomConfig::isStunPreviewBackEnabled, PhantomConfig::setStunPreviewBackEnabled));
        m.add(stunPreview);

        Group fastDps = m.group("Fast DPS Warning", PhantomConfig::isFastDpsWarningEnabled, PhantomConfig::setFastDpsWarningEnabled)
                .withTooltip("If DPS is faster than 3.3s, kuudra can appear in weird places");
        fastDps.add(new Toggle("Fast DPS Notification",
                PhantomConfig::isFastDpsNotifyEnabled, PhantomConfig::setFastDpsNotifyEnabled));
        m.sound(fastDps, PhantomConfig.SOUND_FAST_DPS);
        m.add(fastDps);

        m.add(new Toggle("DPS Waypoint",
                PhantomConfig::isDpsWaypointEnabled, PhantomConfig::setDpsWaypointEnabled)
                .withTooltip("Shows a personal waypoint during eaten/stun based on alphabetical order among non-stun players"));
    }
}
