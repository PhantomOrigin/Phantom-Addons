package com.phantomaddons.gui.tabs;

import com.phantomaddons.DataFeatures;
import com.phantomaddons.Edition;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.UpdateChecker;
import com.phantomaddons.gui.Button;
import com.phantomaddons.gui.Cycle;
import com.phantomaddons.gui.DisableAllButton;
import com.phantomaddons.gui.InfoRow;
import com.phantomaddons.gui.IntInput;
import com.phantomaddons.gui.KeyCapture;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.RangeSlider;
import com.phantomaddons.gui.Toggle;
import net.minecraft.client.Minecraft;

import static com.phantomaddons.gui.Theme.*;

public final class AboutTab {

    private static final String DISCORD_URL = "https://discord.gg/6MquvmrXNP";

    private AboutTab() {}

    public static void build(MenuBuilder m) {
        m.add(new InfoRow("Current Version", () -> new InfoRow.Value(UpdateChecker.currentVersion(), C_ON)));

        m.add(new InfoRow("Latest Version", () -> {
            String latest = UpdateChecker.getLatestVersion();
            return switch (UpdateChecker.getState()) {
                case CHECKING, DOWNLOADING -> new InfoRow.Value("Checking...", C_TEXT_DIM);
                case ERROR                 -> new InfoRow.Value("Error", C_ERROR);
                case UP_TO_DATE            -> new InfoRow.Value(latest != null ? latest : "Up to date", C_ON);
                case UPDATE_AVAILABLE      -> new InfoRow.Value(latest, C_ACCENT);
                case DOWNLOADED            -> new InfoRow.Value(latest, C_INFO);
                default                    -> new InfoRow.Value("—", C_TEXT_DIM);
            };
        }));

        m.add(new InfoRow("Status", () -> switch (UpdateChecker.getState()) {
            case IDLE             -> new InfoRow.Value("Idle", C_TEXT_DIM);
            case CHECKING         -> new InfoRow.Value("Checking...", C_TEXT_DIM);
            case UP_TO_DATE       -> new InfoRow.Value("Up to date", C_ON);
            case UPDATE_AVAILABLE -> new InfoRow.Value("Update available", C_ACCENT);
            case DOWNLOADING      -> new InfoRow.Value("Downloading...", C_TEXT_DIM);
            case DOWNLOADED       -> new InfoRow.Value("Ready to install", C_INFO);
            case ERROR            -> new InfoRow.Value("Check failed", C_ERROR);
        }));

        if (Edition.CURRENT.autoDownloadCapable) {
            m.add(new Toggle("Auto Updates",
                    PhantomConfig::isAutoUpdatesEnabled, PhantomConfig::setAutoUpdatesEnabled));
        }
        m.add(new Button("Updates", "Update", UpdateChecker::downloadManually)
                .withVisible(UpdateChecker::hasUpdate));

        m.add(new KeyCapture("Menu Open Key",
                PhantomConfig::getOpenGuiKey, PhantomConfig::setOpenGuiKey)
                .withLabelColor(0xFFFFFFFF)
                .withTooltip("Off by default — press to set a key or mouse button, Escape to unbind"));
        m.add(new Button("HUD Layout", "Edit Layout", () -> m.screen().openHudEditor()));

        m.add(new IntInput("Ping (ms) - Lowest normal ping",
                PhantomConfig::getLowPing, PhantomConfig::setLowPing)
                .withTooltip("Should be set to the lowest ping that you would get under normal conditions"));

        m.add(new Button("PhantomAddons Discord", "Copy Link",
                () -> Minecraft.getInstance().keyboardHandler.setClipboard(DISCORD_URL)));

        // ── Theme and other related features ─────────────────────────────────────
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
                v -> { PhantomConfig.setUiTransparencyEnabled(v); m.screen().onThemeChanged(); })
                .withTooltip("Makes the GUI and its dim backdrop see-through so the game world shows behind it"));
        m.add(new RangeSlider("GUI Scale", 0.7f, 1.6f, "%.2fx",
                PhantomConfig::getUiGuiScale,
                v -> { PhantomConfig.setUiGuiScale(v); m.screen().onGuiScaleChanged(); })
                .withTooltip("Resizes the mod's own GUI panel — everything inside stays the same size, this just fits more (or less) on screen at once"));
        m.add(new DisableAllButton(() -> m.screen().disableAllFeatures())
                .withTooltip("Turns off every feature toggle in the mod (not About-tab settings). Click, wait for the 3 second countdown, then click again to confirm."));
        m.add(new Toggle("Developer Features",
                PhantomConfig::isDeveloperFeaturesEnabled, PhantomConfig::setDeveloperFeaturesEnabled));

        if (PhantomConfig.isDeveloperFeaturesEnabled() && DataFeatures.isPresent()) {
            m.add(new Toggle("Tentacle DB Logging",
                    PhantomConfig::isTentacleDbLoggingEnabled, PhantomConfig::setTentacleDbLoggingEnabled)
                    .withTooltip("Data-gathering tool for the upcoming Pearl Cancel Helper feature. While enabled, during the Supplies phase it tracks nearby tentacles (position, speed, chain size), when they grab you, and your position/orientation when pearl-cancelling — all written to a local SQLite database (phantomaddons_kuudra_data.sqlite in the config folder) for offline analysis. Purely observational, it doesn't change any gameplay behavior. Warning: this can log a large amount of data over many runs and grow the database file to hundreds of MB or more — leave off unless you're intentionally collecting data."));

            m.add(new Toggle("Highlight Tentacles",
                    PhantomConfig::isTentacleHighlightEnabled, PhantomConfig::setTentacleHighlightEnabled)
                    .withTooltip("Debug visual for the tentacle data-gathering tool — outlines each tracked tentacle segment in a distinct color so you can see the grouping/tracking working in real time. Colors are keyed to the tentacle's spawn spot, so the same physical tentacle gets the same color every run. Requires Tentacle DB Logging to also be enabled, since it highlights whatever that's currently tracking. Purely visual, no gameplay effect."));
        }
    }
}
