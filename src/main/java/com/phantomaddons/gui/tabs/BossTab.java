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
        Group solo = m.group("Solo Detector", PhantomConfig::isSoloDetectorEnabled, PhantomConfig::setSoloDetectorEnabled);
        solo.add(new Toggle("Solo Notification",
                PhantomConfig::isSoloNotifyEnabled, PhantomConfig::setSoloNotifyEnabled));
        m.sound(solo, PhantomConfig.SOUND_SOLO);
        m.add(solo);

        Group hp = m.group("Kuudra HP HUD", PhantomConfig::isKuudraHpHudEnabled, PhantomConfig::setKuudraHpHudEnabled);
        hp.add(new Toggle("Show Raw HP",
                PhantomConfig::isKuudraHpShowRaw, PhantomConfig::setKuudraHpShowRaw));
        hp.add(new Toggle("Hide Health Bar",
                PhantomConfig::isKuudraHpHideBar, PhantomConfig::setKuudraHpHideBar));
        m.add(hp);

        m.add(new Toggle("Kuudra Direction",
                PhantomConfig::isKuudraDirectionEnabled, PhantomConfig::setKuudraDirectionEnabled));
        m.heading("Rend");
        Group rendDamage = m.group("Rend Damage", PhantomConfig::isRendDamageEnabled, PhantomConfig::setRendDamageEnabled);
        rendDamage.add(new Toggle("Pull Attribution (BETA)",
                PhantomConfig::isRendPullAttributionEnabled, PhantomConfig::setRendPullAttributionEnabled)
                .withTooltip("Tries to figure out who dealt each pull's damage. Disabling this makes every pull just show \"Someone\" instead of attempting to attribute it to a player."));
        rendDamage.withWarning(PingWarning.when(PhantomConfig::isRendDamageEnabled), PingWarning.FEATURE_TEXT);
        m.add(rendDamage);
        m.add(new Toggle("Rend Tracker",
                PhantomConfig::isRendTrackerEnabled, PhantomConfig::setRendTrackerEnabled)
                .withWarning(PingWarning.when(PhantomConfig::isRendTrackerEnabled), PingWarning.FEATURE_TEXT));
        Group boneTiming = m.group("Bone Timing Assist", PhantomConfig::isBoneTimingAssistEnabled, PhantomConfig::setBoneTimingAssistEnabled);
        boneTiming.add(new RangeSlider("Kuudra Timing Offset", -3.0f, 1.0f, "%.2f",
                PhantomConfig::getBoneTimingAssistOffset, PhantomConfig::setBoneTimingAssistOffset));
        m.sound(boneTiming, PhantomConfig.SOUND_BONE_THROW_NOW);
        m.add(boneTiming);
        Group backbone = m.group("Backbone Progress Bar", PhantomConfig::isBackboneProgressBarEnabled, PhantomConfig::setBackboneProgressBarEnabled);
        backbone.add(new Toggle("Work Outside Kuudra",
                PhantomConfig::isBackboneProgressBarOutsideKuudraEnabled, PhantomConfig::setBackboneProgressBarOutsideKuudraEnabled));
        m.sound(backbone, PhantomConfig.SOUND_BACKBONE_DONE);
        m.add(backbone);

        m.heading("Boss phase");
        m.add(new Toggle("Block Jump on Boss Start",
                PhantomConfig::isBlockBossPhaseJumpEnabled, PhantomConfig::setBlockBossPhaseJumpEnabled)
                .withTooltip("Disables jumping for the first 5 seconds of the Boss phase")
                .withVisible(PhantomAddons::isBossJumpBlockerPresent));
        Group hitboxOutline = m.group("Predicted Hitbox Location", PhantomConfig::isBoneTimingHitboxOutlineEnabled, PhantomConfig::setBoneTimingHitboxOutlineEnabled);
        hitboxOutline.add(new Toggle("Filled",
                PhantomConfig::isBoneTimingHitboxOutlineFilled, PhantomConfig::setBoneTimingHitboxOutlineFilled));
        hitboxOutline.add(new RgbInput("Colour",
                PhantomConfig::getBoneTimingHitboxOutlineColor, PhantomConfig::setBoneTimingHitboxOutlineColor));
        hitboxOutline.add(new Toggle("Only Kuudra Direction",
                PhantomConfig::isBoneTimingHitboxOutlineOnlyCurrentDirection, PhantomConfig::setBoneTimingHitboxOutlineOnlyCurrentDirection));
        hitboxOutline.add(new Toggle("Highlight In Range",
                PhantomConfig::isBoneTimingHitboxHighlightInRangeEnabled, PhantomConfig::setBoneTimingHitboxHighlightInRange));
        m.add(hitboxOutline);

        m.add(new Toggle("Right Peak Block Highlight",
                PhantomConfig::isRightPeakBlockHighlightEnabled, PhantomConfig::setRightPeakBlockHighlightEnabled));

        m.heading("Highlights and blockers");
        Group hl = m.group("Kuudra Highlight", PhantomConfig::isKuudraHighlightEnabled, PhantomConfig::setKuudraHighlightEnabled);
        hl.add(new Toggle("Filled Highlight",
                PhantomConfig::isKuudraHighlightFilled, PhantomConfig::setKuudraHighlightFilled));
        m.add(hl);

        m.add(new Toggle("Block Atomsplit",
                PhantomConfig::isBlockAtomsplitEnabled, PhantomConfig::setBlockAtomsplitEnabled)
                .withTooltip("Prevents using any item with \"Atomsplit\" in its lore during the Boss phase"));
        m.add(new Toggle("Show Ichor Radius",
                PhantomConfig::isIchorRadiusEnabled, PhantomConfig::setIchorRadiusEnabled));
    }
}
