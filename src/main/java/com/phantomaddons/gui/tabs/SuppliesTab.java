package com.phantomaddons.gui.tabs;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.Cycle;
import com.phantomaddons.gui.Group;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.RangeSlider;
import com.phantomaddons.gui.RgbInput;
import com.phantomaddons.gui.Slider;
import com.phantomaddons.gui.Toggle;

public final class SuppliesTab {

    private SuppliesTab() {}

    public static void build(MenuBuilder m) {
        m.add(new Toggle("Supply Spawn Countdown",
                PhantomConfig::isSupplySpawnCountdownEnabled, PhantomConfig::setSupplySpawnCountdownEnabled)
                .withTooltip("Shows the countdown until the supplies spawn at the start of the Supplies phase"));

        m.add(new Toggle("Supply Beacons",
                PhantomConfig::isSupplyBeaconsEnabled, PhantomConfig::setSupplyBeaconsEnabled));

        Group noPre = m.group("No Pre Announce", PhantomConfig::isNoPreAnnounceEnabled, PhantomConfig::setNoPreAnnounceEnabled);
        noPre.add(new Toggle("No Pre Notification",
                PhantomConfig::isNoPreNotifyEnabled, PhantomConfig::setNoPreNotifyEnabled));
        m.sound(noPre, PhantomConfig.SOUND_NO_PRE);
        m.add(noPre);

        Group supplyGrabbed = m.group("Supply Grabbed Notification", PhantomConfig::isSupplyGrabbedNotifyEnabled, PhantomConfig::setSupplyGrabbedNotifyEnabled);
        m.sound(supplyGrabbed, PhantomConfig.SOUND_SUPPLY_GRABBED);
        m.add(supplyGrabbed);

        Group supplyDropped = m.group("Supply Dropped Notification", PhantomConfig::isSupplyDroppedNotifyEnabled, PhantomConfig::setSupplyDroppedNotifyEnabled);
        m.sound(supplyDropped, PhantomConfig.SOUND_SUPPLY_DROPPED);
        m.add(supplyDropped);

        m.add(new Toggle("Crate Priority",
                PhantomConfig::isCratePriorityEnabled, PhantomConfig::setCratePriorityEnabled));
        m.add(new Toggle("Supply Recovery Message",
                PhantomConfig::isSupplyRecoveryMsgEnabled, PhantomConfig::setSupplyRecoveryMsgEnabled));
        m.add(new Toggle("Supply Location Announce",
                PhantomConfig::isSupplyLocationAnnounceEnabled, PhantomConfig::setSupplyLocationAnnounceEnabled));
        m.add(new Toggle("Supply Hitbox",
                PhantomConfig::isSupplyHitboxEnabled, PhantomConfig::setSupplyHitboxEnabled));
        m.add(new Toggle("Supply Rod Radius",
                PhantomConfig::isSupplyRodRadiusEnabled, PhantomConfig::setSupplyRodRadiusEnabled));
        m.add(new Toggle("Supply Pearl Hitbox",
                PhantomConfig::isSupplyPearlHitboxEnabled, PhantomConfig::setSupplyPearlHitboxEnabled));
        m.add(new Toggle("Supply Giant Hitbox Alert",
                PhantomConfig::isSupplyGiantHitboxEnabled, PhantomConfig::setSupplyGiantHitboxEnabled));

        Group giantHitbox = m.group("Giant Hitbox", PhantomConfig::isGiantHitboxEnabled, PhantomConfig::setGiantHitboxEnabled);
        giantHitbox.add(new Toggle("Filled",
                PhantomConfig::isGiantHitboxFilled, PhantomConfig::setGiantHitboxFilled));
        giantHitbox.add(new Slider("Fill Opacity",
                PhantomConfig::getGiantHitboxFillOpacity, PhantomConfig::setGiantHitboxFillOpacity, ""));
        giantHitbox.add(new RgbInput("Colour",
                PhantomConfig::getGiantHitboxColor, PhantomConfig::setGiantHitboxColor));
        m.add(giantHitbox);

        m.add(new Toggle("Lava Bobber Fix",
                PhantomConfig::isLavaBobberFixEnabled, PhantomConfig::setLavaBobberFixEnabled));
        m.add(new Toggle("Legacy Rod Physics",
                PhantomConfig::isLegacyRodPhysicsEnabled, PhantomConfig::setLegacyRodPhysicsEnabled));
        m.add(new Toggle("Etherwarp Waypoints",
                PhantomConfig::isEtherwarpWaypointsEnabled, PhantomConfig::setEtherwarpWaypointsEnabled));
        m.add(new Toggle("Block Slot 9",
                PhantomConfig::isBlockSlot9Enabled, PhantomConfig::setBlockSlot9Enabled)
                .withTooltip("Prevents you from accidentally using the skyblock menu during supplies phase"));

        // Pearl waypoints — everything related lives under this dropdown
        Group wp = m.group("Dynamic Waypoints", PhantomConfig::isPearlWaypointsEnabled, PhantomConfig::setPearlWaypointsEnabled)
                .withTooltip("Optimal usage - Aim at the centre of the waypoints for the most consistency, aiming lower doesnt make the pearl land earlier.");
        wp.add(new Toggle("Show All Waypoints",
                PhantomConfig::isShowAllWaypoints, PhantomConfig::setShowAllWaypoints));
        wp.add(new Toggle("Flat Pearls",
                PhantomConfig::isPearlFlatEnabled, PhantomConfig::setPearlFlatEnabled));
        wp.add(new Toggle("Sky Pearls",
                PhantomConfig::isPearlSkyEnabled, PhantomConfig::setPearlSkyEnabled));
        wp.add(new Toggle("Double Pearls",
                PhantomConfig::isPearlDoubleEnabled, PhantomConfig::setPearlDoubleEnabled));
        wp.add(new RangeSlider("Double Pearl Delay", 0.05f, 0.55f, "%.2fs",
                PhantomConfig::getDoublePearlDelayS, PhantomConfig::setDoublePearlDelayS));
        wp.add(new Cycle("Waypoint Type",
                () -> PhantomConfig.getWaypointType().name().charAt(0)
                        + PhantomConfig.getWaypointType().name().substring(1).toLowerCase(),
                () -> PhantomConfig.setWaypointType(
                        PhantomConfig.getWaypointType() == PhantomConfig.WaypointType.CIRCLE
                                ? PhantomConfig.WaypointType.SQUARE
                                : PhantomConfig.WaypointType.CIRCLE)));
        wp.add(new Toggle("Waypoint Fill",
                PhantomConfig::isWaypointFillEnabled, PhantomConfig::setWaypointFillEnabled));
        wp.add(new Cycle("Update Frequency",
                () -> PhantomConfig.isPearlTickUpdate() ? "Per Tick" : "Per Frame",
                () -> PhantomConfig.setPearlTickUpdate(!PhantomConfig.isPearlTickUpdate())));
        wp.add(new Toggle("Drop Locations",
                PhantomConfig::isDropLocationsEnabled, PhantomConfig::setDropLocationsEnabled));
        wp.add(new Toggle("Pearl Timer",
                PhantomConfig::isPearlTimerEnabled, PhantomConfig::setPearlTimerEnabled));
        wp.add(new Slider("Timer Height", PhantomConfig::getPearlTimerHeight, PhantomConfig::setPearlTimerHeight, ""));
        wp.add(new Slider("Timer Size", PhantomConfig::getPearlTimerSize,   PhantomConfig::setPearlTimerSize,   ""));
        wp.add(new Slider("Waypoint Size", PhantomConfig::getPearlCircleSize,  PhantomConfig::setPearlCircleSize,  ""));
        wp.add(new Slider("Fill Opacity", PhantomConfig::getWaypointFillAlpha,PhantomConfig::setWaypointFillAlpha,"%"));
        wp.add(new Slider("Beacon Opacity", PhantomConfig::getBeaconAlpha,      PhantomConfig::setBeaconAlpha,      "%"));

        Group wpCol = m.section("Waypoint Colours", wp);
        wpCol.add(new RgbInput("Normal target", PhantomConfig::getWpColNormal,  PhantomConfig::setWpColNormal));
        wpCol.add(new RgbInput("Correct target", PhantomConfig::getWpColCorrect, PhantomConfig::setWpColCorrect));
        wpCol.add(new RgbInput("Hovered target", PhantomConfig::getWpColHovered, PhantomConfig::setWpColHovered));
        wpCol.add(new RgbInput("Ready target", PhantomConfig::getWpColReady,   PhantomConfig::setWpColReady));
        wp.add(wpCol);

        Group bcnCol = m.section("Beacon Colours", wp);
        bcnCol.add(new RgbInput("Normal target", PhantomConfig::getBeaconColNormal,  PhantomConfig::setBeaconColNormal));
        bcnCol.add(new RgbInput("Correct target", PhantomConfig::getBeaconColCorrect, PhantomConfig::setBeaconColCorrect));
        wp.add(bcnCol);
        m.sound(wp, PhantomConfig.SOUND_PEARL_NOW);

        m.add(wp);

        Group wpLines = m.group("Waypoint Lines", PhantomConfig::isWaypointLinesEnabled, PhantomConfig::setWaypointLinesEnabled)
                .withTooltip("Draws a line from the crosshair to the next waypoint you have to aim at");
        wpLines.add(new Toggle("Flat Pearls",
                PhantomConfig::isWaypointLinesFlatPearlsEnabled, PhantomConfig::setWaypointLinesFlatPearlsEnabled));
        wpLines.add(new Toggle("Supplies",
                PhantomConfig::isWaypointLinesSuppliesEnabled, PhantomConfig::setWaypointLinesSuppliesEnabled));
        wpLines.add(new Cycle("Second Supply Preference",
                () -> PhantomConfig.getSecondSupplyPreference() == PhantomConfig.SecondSupplyPreference.DOUBLE_PEARL
                        ? "Pearl" : "Etherwarp",
                () -> PhantomConfig.setSecondSupplyPreference(
                        PhantomConfig.getSecondSupplyPreference() == PhantomConfig.SecondSupplyPreference.DOUBLE_PEARL
                                ? PhantomConfig.SecondSupplyPreference.ETHERWARP
                                : PhantomConfig.SecondSupplyPreference.DOUBLE_PEARL)));
        m.add(wpLines);

        m.add(new Toggle("Smooth Crate Pickup",
                PhantomConfig::isSmoothCratePickupEnabled, PhantomConfig::setSmoothCratePickupEnabled));

        m.add(new Cycle("Kuudra Talisman",
                () -> switch (PhantomConfig.getKuudraTalisman()) {
                    case NONE   -> "None";   case KIDNEY -> "Kidney";
                    case LUNG   -> "Lung";   case HEART  -> "Heart"; },
                () -> PhantomConfig.setKuudraTalisman(switch (PhantomConfig.getKuudraTalisman()) {
                    case NONE   -> PhantomConfig.KuudraTalisman.KIDNEY;
                    case KIDNEY -> PhantomConfig.KuudraTalisman.LUNG;
                    case LUNG   -> PhantomConfig.KuudraTalisman.HEART;
                    case HEART  -> PhantomConfig.KuudraTalisman.NONE; })));
    }
}
