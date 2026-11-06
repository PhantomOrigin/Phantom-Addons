package com.phantomaddons.gui.tabs;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.PingWarning;
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
        m.heading("Waypoints");
        m.add(new Toggle("Etherwarp Waypoints",
                PhantomConfig::isEtherwarpWaypointsEnabled, PhantomConfig::setEtherwarpWaypointsEnabled)
                .withTooltip("Shows where to etherwarp during supplies."));

        // Pearl waypoints — everything related lives under this dropdown
        Group wp = m.group("Dynamic Waypoints", PhantomConfig::isPearlWaypointsEnabled, PhantomConfig::setPearlWaypointsEnabled)
                .withTooltip("Shows where to aim your pearls.");
        wp.add(new Toggle("Show All Waypoints",
                PhantomConfig::isShowAllWaypoints, PhantomConfig::setShowAllWaypoints)
                .withTooltip("Shows every waypoint instead of just the next one."));
        wp.add(new Toggle("Flat Pearls",
                PhantomConfig::isPearlFlatEnabled, PhantomConfig::setPearlFlatEnabled)
                .withTooltip("Shows waypoints for flat pearls."));
        wp.add(new Toggle("Sky Pearls",
                PhantomConfig::isPearlSkyEnabled, PhantomConfig::setPearlSkyEnabled)
                .withTooltip("Shows waypoints for sky pearls."));
        wp.add(new Toggle("Double Pearls",
                PhantomConfig::isPearlDoubleEnabled, PhantomConfig::setPearlDoubleEnabled)
                .withTooltip("Shows waypoints for throwing two pearls."));
        wp.add(new RangeSlider("Double Pearl Delay", 0.05f, 0.55f, "%.2fs",
                PhantomConfig::getDoublePearlDelayS, PhantomConfig::setDoublePearlDelayS)
                .withTooltip("The time between the two pearls."));
        wp.add(new Cycle("Waypoint Type",
                () -> PhantomConfig.getWaypointType().name().charAt(0)
                        + PhantomConfig.getWaypointType().name().substring(1).toLowerCase(),
                () -> PhantomConfig.setWaypointType(
                        PhantomConfig.getWaypointType() == PhantomConfig.WaypointType.CIRCLE
                                ? PhantomConfig.WaypointType.SQUARE
                                : PhantomConfig.WaypointType.CIRCLE))
                .withTooltip("Chooses circle or square waypoints."));
        wp.add(new Toggle("Waypoint Fill",
                PhantomConfig::isWaypointFillEnabled, PhantomConfig::setWaypointFillEnabled)
                .withTooltip("Fills the waypoints in."));
        wp.add(new Cycle("Update Frequency",
                () -> PhantomConfig.isPearlTickUpdate() ? "Per Tick" : "Per Frame",
                () -> PhantomConfig.setPearlTickUpdate(!PhantomConfig.isPearlTickUpdate()))
                .withTooltip("Updates the waypoints every tick or every frame."));
        wp.add(new Toggle("Drop Locations",
                PhantomConfig::isDropLocationsEnabled, PhantomConfig::setDropLocationsEnabled)
                .withTooltip("Shows where supplies will drop."));
        wp.add(new Toggle("Pearl Timer",
                PhantomConfig::isPearlTimerEnabled, PhantomConfig::setPearlTimerEnabled)
                .withTooltip("Shows a timer for your pearl."));
        wp.add(new Slider("Timer Height", PhantomConfig::getPearlTimerHeight, PhantomConfig::setPearlTimerHeight, "")
                .withTooltip("Moves the timer up or down."));
        wp.add(new Slider("Timer Size", PhantomConfig::getPearlTimerSize,   PhantomConfig::setPearlTimerSize,   "")
                .withTooltip("Changes how big the timer is."));
        wp.add(new Slider("Waypoint Size", PhantomConfig::getPearlCircleSize,  PhantomConfig::setPearlCircleSize,  "")
                .withTooltip("Changes how big the waypoints are."));
        wp.add(new Slider("Fill Opacity", PhantomConfig::getWaypointFillAlpha,PhantomConfig::setWaypointFillAlpha,"%")
                .withTooltip("How see-through the fill is."));
        wp.add(new Slider("Beacon Opacity", PhantomConfig::getBeaconAlpha,      PhantomConfig::setBeaconAlpha,      "%")
                .withTooltip("How see-through the beams are."));

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

        wp.withWarning(PingWarning.when(PhantomConfig::isPearlWaypointsEnabled), PingWarning.FEATURE_TEXT);
        m.add(wp);

        Group wpLines = m.group("Waypoint Lines", PhantomConfig::isWaypointLinesEnabled, PhantomConfig::setWaypointLinesEnabled)
                .withTooltip("Draws a line from your crosshair to the next waypoint.");
        wpLines.add(new Toggle("Flat Pearls",
                PhantomConfig::isWaypointLinesFlatPearlsEnabled, PhantomConfig::setWaypointLinesFlatPearlsEnabled)
                .withTooltip("Draws lines for flat pearls."));
        wpLines.add(new Toggle("Supplies",
                PhantomConfig::isWaypointLinesSuppliesEnabled, PhantomConfig::setWaypointLinesSuppliesEnabled)
                .withTooltip("Draws lines to supplies."));
        wpLines.add(new RgbInput("Line Colour",
                PhantomConfig::getWaypointLinesColor, PhantomConfig::setWaypointLinesColor));
        wpLines.add(new RangeSlider("Line Thickness", 1f, 10f, "%.1f",
                PhantomConfig::getWaypointLinesWidth, PhantomConfig::setWaypointLinesWidth)
                .withTooltip("How thick the line is."));
        wpLines.add(new Cycle("Second Supply Preference",
                () -> PhantomConfig.getSecondSupplyPreference() == PhantomConfig.SecondSupplyPreference.DOUBLE_PEARL
                        ? "Pearl" : "Etherwarp",
                () -> PhantomConfig.setSecondSupplyPreference(
                        PhantomConfig.getSecondSupplyPreference() == PhantomConfig.SecondSupplyPreference.DOUBLE_PEARL
                                ? PhantomConfig.SecondSupplyPreference.ETHERWARP
                                : PhantomConfig.SecondSupplyPreference.DOUBLE_PEARL))
                .withTooltip("Chooses pearl or etherwarp for the second supply."));
        m.add(wpLines);

        m.add(new Toggle("Crate Priority",
                PhantomConfig::isCratePriorityEnabled, PhantomConfig::setCratePriorityEnabled)
                .withTooltip("Tells you which crate to pick up first."));

        m.heading("Pickup");
        m.add(new Toggle("Block Slot 9",
                PhantomConfig::isBlockSlot9Enabled, PhantomConfig::setBlockSlot9Enabled)
                .withTooltip("Stops you opening the SkyBlock menu during the Supplies phase."));

        m.add(new Toggle("Smooth Crate Pickup",
                PhantomConfig::isSmoothCratePickupEnabled, PhantomConfig::setSmoothCratePickupEnabled)
                .withTooltip("Makes the crate pickup percentage count up smoothly instead of jumping."));

        Group grabWarning = m.group("Tentacle Grab Warning", PhantomConfig::isTentacleGrabWarningEnabled, PhantomConfig::setTentacleGrabWarningEnabled)
                .withTooltip("Warns you when a tentacle may grab you.");
        grabWarning.add(new Slider("Minimum Confidence",
                PhantomConfig::getTentacleGrabWarningMinConfidence, PhantomConfig::setTentacleGrabWarningMinConfidence, "%")
                .withTooltip("How sure the mod has to be before it warns you."));
        grabWarning.add(new Toggle("Line to Tentacle",
                PhantomConfig::isTentacleGrabLineEnabled, PhantomConfig::setTentacleGrabLineEnabled)
                .withTooltip("Draws a line to the tentacle."));
        grabWarning.add(new Toggle("On-Screen Warning",
                PhantomConfig::isTentacleGrabHudEnabled, PhantomConfig::setTentacleGrabHudEnabled)
                .withTooltip("Shows a warning on your screen."));
        m.sound(grabWarning, PhantomConfig.SOUND_TENTACLE_GRAB);
        m.add(grabWarning);

        Group reach = m.group("Tentacle Reach Highlight", PhantomConfig::isTentacleReachEnabled, PhantomConfig::setTentacleReachEnabled)
                .withTooltip("Shows how far a tentacle can reach.");
        reach.add(new Cycle("Highlight Type",
                () -> PhantomConfig.isTentacleReachGround() ? "Ground" : "Sphere",
                () -> PhantomConfig.setTentacleReachGround(!PhantomConfig.isTentacleReachGround()))
                .withTooltip("Shows the reach on the ground or as a sphere."));
        reach.add(new Toggle("Highlight Lava",
                PhantomConfig::isTentacleReachLava, PhantomConfig::setTentacleReachLava)
                .withTooltip("Also shows the reach over lava."));
        reach.add(new Slider("Opacity",
                PhantomConfig::getTentacleReachOpacity, PhantomConfig::setTentacleReachOpacity, "%")
                .withTooltip("How see-through the highlight is."));
        m.add(reach);

        m.add(new Cycle("Kuudra Talisman",
                () -> switch (PhantomConfig.getKuudraTalisman()) {
                    case NONE   -> "None";   case KIDNEY -> "Kidney";
                    case LUNG   -> "Lung";   case HEART  -> "Heart"; },
                () -> PhantomConfig.setKuudraTalisman(switch (PhantomConfig.getKuudraTalisman()) {
                    case NONE   -> PhantomConfig.KuudraTalisman.KIDNEY;
                    case KIDNEY -> PhantomConfig.KuudraTalisman.LUNG;
                    case LUNG   -> PhantomConfig.KuudraTalisman.HEART;
                    case HEART  -> PhantomConfig.KuudraTalisman.NONE; }))
                .withTooltip("Tells the mod which Kuudra talisman you have."));

        m.heading("Supplies");
        m.add(new Toggle("Supply Spawn Countdown",
                PhantomConfig::isSupplySpawnCountdownEnabled, PhantomConfig::setSupplySpawnCountdownEnabled)
                .withTooltip("Shows the countdown until the supplies spawn."));

        m.add(new Toggle("Supply Beacons",
                PhantomConfig::isSupplyBeaconsEnabled, PhantomConfig::setSupplyBeaconsEnabled)
                .withTooltip("Shows beams at supply locations."));

        m.heading("Announcements");
        Group noPre = m.group("No Pre Announce", PhantomConfig::isNoPreAnnounceEnabled, PhantomConfig::setNoPreAnnounceEnabled)
                .withTooltip("Tells your party when there is no pre supply.");
        noPre.add(new Toggle("No Pre Notification",
                PhantomConfig::isNoPreNotifyEnabled, PhantomConfig::setNoPreNotifyEnabled)
                .withTooltip("Shows a notification when there is no pre supply."));
        m.sound(noPre, PhantomConfig.SOUND_NO_PRE);
        m.add(noPre);

        Group supplyGrabbed = m.group("Supply Grabbed Notification", PhantomConfig::isSupplyGrabbedNotifyEnabled, PhantomConfig::setSupplyGrabbedNotifyEnabled)
                .withTooltip("Shows a notification when a supply is picked up.");
        m.sound(supplyGrabbed, PhantomConfig.SOUND_SUPPLY_GRABBED);
        m.add(supplyGrabbed);

        Group supplyDropped = m.group("Supply Dropped Notification", PhantomConfig::isSupplyDroppedNotifyEnabled, PhantomConfig::setSupplyDroppedNotifyEnabled)
                .withTooltip("Shows a notification when a supply is dropped.");
        m.sound(supplyDropped, PhantomConfig.SOUND_SUPPLY_DROPPED);
        m.add(supplyDropped);

        m.add(new Toggle("Supply Recovery Message",
                PhantomConfig::isSupplyRecoveryMsgEnabled, PhantomConfig::setSupplyRecoveryMsgEnabled)
                .withTooltip("Shows a message when a dropped supply is picked back up."));
        m.add(new Toggle("Supply Location Announce",
                PhantomConfig::isSupplyLocationAnnounceEnabled, PhantomConfig::setSupplyLocationAnnounceEnabled)
                .withTooltip("Tells your party where supplies are."));
        m.heading("Hitboxes");
        m.add(new Toggle("Supply Hitbox",
                PhantomConfig::isSupplyHitboxEnabled, PhantomConfig::setSupplyHitboxEnabled)
                .withTooltip("Shows the supplies' hitboxes."));
        m.add(new Toggle("Supply Rod Radius",
                PhantomConfig::isSupplyRodRadiusEnabled, PhantomConfig::setSupplyRodRadiusEnabled)
                .withTooltip("Shows how far your rod reaches around supplies."));
        m.add(new Toggle("Supply Pearl Hitbox",
                PhantomConfig::isSupplyPearlHitboxEnabled, PhantomConfig::setSupplyPearlHitboxEnabled)
                .withTooltip("Shows where a pearl will hit a supply."));
        m.add(new Toggle("Supply Giant Hitbox Alert",
                PhantomConfig::isSupplyGiantHitboxEnabled, PhantomConfig::setSupplyGiantHitboxEnabled)
                .withTooltip("Warns you about the giant's hitbox near supplies."));

        Group giantHitbox = m.group("Giant Hitbox", PhantomConfig::isGiantHitboxEnabled, PhantomConfig::setGiantHitboxEnabled)
                .withTooltip("Shows the giant's hitbox.");
        giantHitbox.add(new Toggle("Filled",
                PhantomConfig::isGiantHitboxFilled, PhantomConfig::setGiantHitboxFilled)
                .withTooltip("Fills the box in."));
        giantHitbox.add(new Slider("Fill Opacity",
                PhantomConfig::getGiantHitboxFillOpacity, PhantomConfig::setGiantHitboxFillOpacity, "")
                .withTooltip("How see-through the fill is."));
        giantHitbox.add(new RgbInput("Colour",
                PhantomConfig::getGiantHitboxColor, PhantomConfig::setGiantHitboxColor));
        m.add(giantHitbox);

        m.heading("Rod");
        m.add(new Toggle("Lava Bobber Fix",
                PhantomConfig::isLavaBobberFixEnabled, PhantomConfig::setLavaBobberFixEnabled)
                .withTooltip("Fixes the fishing bobber in lava."));
        m.add(new Toggle("Legacy Rod Physics",
                PhantomConfig::isLegacyRodPhysicsEnabled, PhantomConfig::setLegacyRodPhysicsEnabled)
                .withTooltip("Makes the rod move like it did in older versions."));
    }
}