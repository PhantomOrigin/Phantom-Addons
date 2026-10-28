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
                .withTooltip("Click to see what's new in your version, or everything you've missed if an update is available."));

        if (Edition.CURRENT.autoDownloadCapable) {
            m.add(new Toggle("Auto Updates",
                    PhantomConfig::isAutoUpdatesEnabled, PhantomConfig::setAutoUpdatesEnabled));
        }
        m.add(new Button("Updates", "Update", UpdateChecker::downloadManually)
                .withVisible(UpdateChecker::hasUpdate));

        m.heading("Menu");
        m.add(new KeyCapture("Menu Open Key",
                PhantomConfig::getOpenGuiKey, PhantomConfig::setOpenGuiKey)
                .withLabelColor(0xFFFFFFFF));

        m.add(new IntInput("Ping (ms) - Lowest normal ping",
                PhantomConfig::getLowPing, PhantomConfig::setLowPing)
                .withTooltip("Should be set to the lowest ping that you would get under normal conditions")
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
                }));
        m.add(new Toggle("Transparency Effects",
                PhantomConfig::isUiTransparencyEnabled,
                v -> { PhantomConfig.setUiTransparencyEnabled(v); m.screen().onThemeChanged(); }));
        m.add(new RangeSlider("GUI Scale", 0.7f, 1.6f, "%.2fx",
                PhantomConfig::getUiGuiScale,
                v -> { PhantomConfig.setUiGuiScale(v); m.screen().onGuiScaleChanged(); }));
        m.heading("Information and data");
        m.add(new Toggle("Show Changelog on Update",
                PhantomConfig::isChangelogEnabled, PhantomConfig::setChangelogEnabled));
        m.add(new RefreshToggle("Donator Names",
                PhantomConfig::isDonatorNamesEnabled, PhantomConfig::setDonatorNamesEnabled,
                com.phantomaddons.features.customisation.DonatorNames::refreshNow,
                com.phantomaddons.features.customisation.DonatorNames::cooldownLeftMs,
                ""));

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
                .withTooltip("Sends anonymous information about tentacles and cancel attempts to the PhantomAddons server for an upcoming feature"));
        m.heading("Advanced");
        m.add(new DisableAllButton(() -> m.screen().disableAllFeatures())
                .withTooltip("Turns off every feature toggle in the mod (not About-tab settings). Click, wait for the 3 second countdown, then click again to confirm."));
        m.add(new Toggle("Developer Features",
                PhantomConfig::isDeveloperFeaturesEnabled, PhantomConfig::setDeveloperFeaturesEnabled));

        if (PhantomConfig.isDeveloperFeaturesEnabled() && DataFeatures.isPresent()) {
            if (DataFeatures.hasHighlight()) {
                m.add(new Toggle("Highlight Tentacles",
                        PhantomConfig::isTentacleHighlightEnabled, PhantomConfig::setTentacleHighlightEnabled)
                        .withTooltip("Debug visual for the tentacle data-gathering tool — outlines the invisible cube at the top of each tracked tentacle: yellow while rising, green once it has stopped, red during a grab attempt. Purely visual, no gameplay effect."));

                m.add(new Toggle("Tentacle Spot Beacons",
                        PhantomConfig::isTentacleSpotBeaconsEnabled, PhantomConfig::setTentacleSpotBeaconsEnabled)
                        .withTooltip("Debug visual: draws a beacon beam from the bottom to the top of the world at every x/z position where a tentacle's invisible cube has come to rest. While it is on, new spots are saved as you play (phantomaddons_tentacle_spots.jsonl in the config folder) and drawn along with the ones saved by earlier developer builds. Purely visual, no gameplay effect."));
            }
        }
    }
}
