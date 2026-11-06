package com.phantomaddons.gui.tabs;

import com.phantomaddons.DataFeatures;
import com.phantomaddons.Edition;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.PingWarning;
import com.phantomaddons.UpdateChecker;
import com.phantomaddons.gui.Button;
import com.phantomaddons.gui.Cycle;
import com.phantomaddons.gui.DisableAllButton;
import com.phantomaddons.gui.UpdateRow;
import com.phantomaddons.gui.IntInput;
import com.phantomaddons.gui.KeyCapture;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.RefreshToggle;
import com.phantomaddons.gui.RangeSlider;
import com.phantomaddons.gui.Toggle;
import net.minecraft.client.Minecraft;

import static com.phantomaddons.gui.Theme.*;

public final class AboutTab {

    private AboutTab() {}

    public static void build(MenuBuilder m) {
        m.heading("Version");
        m.add(new UpdateRow("Version", com.phantomaddons.gui.ChangelogManager::openFromAbout)
                .withTooltip("Opens the list of changes in your version."));

        if (Edition.CURRENT.autoDownloadCapable) {
            m.add(new Toggle("Auto Updates",
                    PhantomConfig::isAutoUpdatesEnabled, PhantomConfig::setAutoUpdatesEnabled)
                    .withTooltip("Downloads new versions of the mod for you."));
        }
        m.add(new Button("Updates", "Update", UpdateChecker::downloadManually)
                .withTooltip("Downloads the newest version of the mod.")
                .withVisible(UpdateChecker::hasUpdate));

        m.heading("Menu");
        m.add(new KeyCapture("Menu Open Key",
                PhantomConfig::getOpenGuiKey, PhantomConfig::setOpenGuiKey)
                .withLabelColor(0xFFFFFFFF)
                .withTooltip("The key that opens this menu."));

        m.add(new IntInput("Ping (ms) - Lowest normal ping",
                PhantomConfig::getLowPing, PhantomConfig::setLowPing)
                .withTooltip("Set this to the lowest ping you normally get.")
                .withWarning(PingWarning::active, PingWarning.SETTING_TEXT));

        m.heading("Appearance");
        m.add(new Cycle("Theme",
                () -> switch (PhantomConfig.getUiTheme()) {
                    case DARK -> "Dark";
                    case LIGHT -> "Light";
                },
                () -> {
                    PhantomConfig.setUiTheme(switch (PhantomConfig.getUiTheme()) {
                        case DARK -> PhantomConfig.UiTheme.LIGHT;
                        case LIGHT -> PhantomConfig.UiTheme.DARK;
                    });
                    m.screen().onThemeChanged();
                })
                .withTooltip("Switches the menu between dark and light."));
        m.add(new Toggle("Transparency Effects",
                PhantomConfig::isUiTransparencyEnabled,
                v -> { PhantomConfig.setUiTransparencyEnabled(v); m.screen().onThemeChanged(); })
                .withTooltip("Makes parts of the menu see-through."));
        m.add(new RangeSlider("GUI Scale", 0.7f, 1.6f, "%.2fx",
                PhantomConfig::getUiGuiScale,
                v -> { PhantomConfig.setUiGuiScale(v); m.screen().onGuiScaleChanged(); })
                .withTooltip("Changes how big this menu is."));
        m.heading("Information and data");
        m.add(new Toggle("Show Changelog on Update",
                PhantomConfig::isChangelogEnabled, PhantomConfig::setChangelogEnabled)
                .withTooltip("Shows what's new when the mod updates."));
        m.add(new RefreshToggle("Donator Names",
                PhantomConfig::isDonatorNamesEnabled, PhantomConfig::setDonatorNamesEnabled,
                com.phantomaddons.features.customisation.DonatorNames::refreshNow,
                com.phantomaddons.features.customisation.DonatorNames::cooldownLeftMs,
                "Shows special names for people who supported the mod."));

        m.add(new Toggle("Share Pearl Cancel Data",
                PhantomConfig::isShareSupplyData,
                v -> {
                    PhantomConfig.setShareSupplyData(v);
                    com.phantomaddons.data.SupplyShare.onToggle(v);
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(v
                                ? "§aPearl cancel data sharing is on. §7During Kuudra's Supplies phase the mod will send anonymous tentacle and pearl cancel measurements. Turn it off any time in /pa > About."
                                : "§ePearl cancel data sharing is off. §7Anything not yet sent has been discarded."));
                    }
                })
                .withTooltip("Sends anonymous pearl and tentacle data to help build a future feature."));
        m.heading("Advanced");
        m.add(new DisableAllButton(() -> m.screen().disableAllFeatures())
                .withTooltip("Turns off every feature in the mod."));
        m.add(new Toggle("Developer Features",
                PhantomConfig::isDeveloperFeaturesEnabled, PhantomConfig::setDeveloperFeaturesEnabled)
                .withTooltip("Shows extra tools meant for mod developers."));

        if (PhantomConfig.isDeveloperFeaturesEnabled() && DataFeatures.isPresent()) {
            if (DataFeatures.hasHighlight()) {
                m.add(new Toggle("Highlight Tentacles",
                        PhantomConfig::isTentacleHighlightEnabled, PhantomConfig::setTentacleHighlightEnabled)
                        .withTooltip("Outlines tracked tentacles, for testing."));

                m.add(new Toggle("Tentacle Spot Beacons",
                        PhantomConfig::isTentacleSpotBeaconsEnabled, PhantomConfig::setTentacleSpotBeaconsEnabled)
                        .withTooltip("Shows a beam where tentacles stop, for testing."));

            }
        }
    }
}
