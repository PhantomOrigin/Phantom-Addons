package com.phantomaddons.gui.tabs;

import com.phantomaddons.PhantomAddons;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.PingWarning;
import com.phantomaddons.gui.Group;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.RangeSlider;
import com.phantomaddons.gui.RgbInput;
import com.phantomaddons.gui.Toggle;

public final class BossTab {

    private BossTab() {}

    public static void build(MenuBuilder m) {
        m.heading("Information");
        Group solo = m.group("Solo Detector", PhantomConfig::isSoloDetectorEnabled, PhantomConfig::setSoloDetectorEnabled)
                .withTooltip("Tells you when the rest of your team has died.");
        solo.add(new Toggle("Solo Notification",
                PhantomConfig::isSoloNotifyEnabled, PhantomConfig::setSoloNotifyEnabled)
                .withTooltip("Shows a notification when you are solo."));
        m.sound(solo, PhantomConfig.SOUND_SOLO);
        m.add(solo);

        Group hp = m.group("Kuudra HP HUD", PhantomConfig::isKuudraHpHudEnabled, PhantomConfig::setKuudraHpHudEnabled)
                .withTooltip("Shows Kuudra's health on screen.");
        hp.add(new Toggle("Show Raw HP",
                PhantomConfig::isKuudraHpShowRaw, PhantomConfig::setKuudraHpShowRaw)
                .withTooltip("Shows Kuudra's exact health number."));
        hp.add(new Toggle("Hide Health Bar",
                PhantomConfig::isKuudraHpHideBar, PhantomConfig::setKuudraHpHideBar)
                .withTooltip("Hides the health bar."));
        m.add(hp);

        m.add(new Toggle("Kuudra Direction",
                PhantomConfig::isKuudraDirectionEnabled, PhantomConfig::setKuudraDirectionEnabled)
                .withTooltip("Shows which direction Kuudra is from the middle."));
        m.heading("Rend");
        Group rendDamage = m.group("Rend Damage", PhantomConfig::isRendDamageEnabled, PhantomConfig::setRendDamageEnabled)
                .withTooltip("Shows how much damage each Rend pull did.");
        rendDamage.add(new Toggle("Pull Attribution (BETA)",
                PhantomConfig::isRendPullAttributionEnabled, PhantomConfig::setRendPullAttributionEnabled)
                .withTooltip("Tries to work out which player did each pull."));
        rendDamage.withWarning(PingWarning.when(PhantomConfig::isRendDamageEnabled), PingWarning.FEATURE_TEXT);
        m.add(rendDamage);
        m.add(new Toggle("Rend Tracker",
                PhantomConfig::isRendTrackerEnabled, PhantomConfig::setRendTrackerEnabled)
                .withTooltip("Tracks Rend pulls during the fight.")
                .withWarning(PingWarning.when(PhantomConfig::isRendTrackerEnabled), PingWarning.FEATURE_TEXT));
        Group boneTiming = m.group("Bone Timing Assist", PhantomConfig::isBoneTimingAssistEnabled, PhantomConfig::setBoneTimingAssistEnabled)
                .withTooltip("Helps you time your bone throw.");
        boneTiming.add(new RangeSlider("Kuudra Timing Offset", -3.0f, 1.0f, "%.2f",
                PhantomConfig::getBoneTimingAssistOffset, PhantomConfig::setBoneTimingAssistOffset)
                .withTooltip("Moves the throw timing earlier or later."));
        m.sound(boneTiming, PhantomConfig.SOUND_BONE_THROW_NOW);
        m.add(boneTiming);
        Group backbone = m.group("Backbone Progress Bar", PhantomConfig::isBackboneProgressBarEnabled, PhantomConfig::setBackboneProgressBarEnabled)
                .withTooltip("Shows how far along the backbone is.");
        backbone.add(new Toggle("Work Outside Kuudra",
                PhantomConfig::isBackboneProgressBarOutsideKuudraEnabled, PhantomConfig::setBackboneProgressBarOutsideKuudraEnabled)
                .withTooltip("Lets this work outside Kuudra too."));
        m.sound(backbone, PhantomConfig.SOUND_BACKBONE_DONE);
        m.add(backbone);

        m.heading("Boss phase");
        m.add(new Toggle("Block Jump on Boss Start",
                PhantomConfig::isBlockBossPhaseJumpEnabled, PhantomConfig::setBlockBossPhaseJumpEnabled)
                .withTooltip("Stops you jumping for the first 5 seconds of the boss phase.")
                .withVisible(PhantomAddons::isBossJumpBlockerPresent));
        Group hitboxOutline = m.group("Predicted Hitbox Location", PhantomConfig::isBoneTimingHitboxOutlineEnabled, PhantomConfig::setBoneTimingHitboxOutlineEnabled)
                .withTooltip("Draws where Kuudra's hitbox will be.");
        hitboxOutline.add(new Toggle("Filled",
                PhantomConfig::isBoneTimingHitboxOutlineFilled, PhantomConfig::setBoneTimingHitboxOutlineFilled)
                .withTooltip("Fills the box in instead of just drawing the outline."));
        hitboxOutline.add(new RgbInput("Colour",
                PhantomConfig::getBoneTimingHitboxOutlineColor, PhantomConfig::setBoneTimingHitboxOutlineColor));
        hitboxOutline.add(new Toggle("Only Kuudra Direction",
                PhantomConfig::isBoneTimingHitboxOutlineOnlyCurrentDirection, PhantomConfig::setBoneTimingHitboxOutlineOnlyCurrentDirection)
                .withTooltip("Only shows the hitbox in the direction Kuudra is from the middle."));
        hitboxOutline.add(new Toggle("Highlight In Range",
                PhantomConfig::isBoneTimingHitboxHighlightInRangeEnabled, PhantomConfig::setBoneTimingHitboxHighlightInRange)
                .withTooltip("Changes the highlight when you are in range."));
        m.add(hitboxOutline);

        m.add(new Toggle("Right Peak Block Highlight",
                PhantomConfig::isRightPeakBlockHighlightEnabled, PhantomConfig::setRightPeakBlockHighlightEnabled)
                .withTooltip("Highlights the block at Kuudra's right peak."));

        m.heading("Highlights and blockers");
        Group hl = m.group("Kuudra Highlight", PhantomConfig::isKuudraHighlightEnabled, PhantomConfig::setKuudraHighlightEnabled)
                .withTooltip("Highlights Kuudra so he is easier to see.");
        hl.add(new Toggle("Filled Highlight",
                PhantomConfig::isKuudraHighlightFilled, PhantomConfig::setKuudraHighlightFilled)
                .withTooltip("Fills the highlight in."));
        hl.add(new RgbInput("Colour",
                PhantomConfig::getKuudraHighlightColor, PhantomConfig::setKuudraHighlightColor));
        m.add(hl);

        m.add(new Toggle("Block Atomsplit",
                PhantomConfig::isBlockAtomsplitEnabled, PhantomConfig::setBlockAtomsplitEnabled)
                .withTooltip("Stops you using Atomsplit during the boss phase."));
        m.add(new Toggle("Show Ichor Radius",
                PhantomConfig::isIchorRadiusEnabled, PhantomConfig::setIchorRadiusEnabled)
                .withTooltip("Shows the area an Ichor Pool covers."));
    }
}
