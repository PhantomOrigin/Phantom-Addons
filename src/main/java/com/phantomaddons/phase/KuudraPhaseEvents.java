package com.phantomaddons.phase;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.build.AnnounceFresh;
import com.phantomaddons.features.stundps.AutoGFS;
import com.phantomaddons.features.build.buildprogress.BuildProgressHud;
import com.phantomaddons.features.build.buildprogress.BuildProgressTracker;
import com.phantomaddons.features.supplies.etherwarp.EtherwarpWaypointManager;
import com.phantomaddons.features.supplies.nopre.NoPreAnnounce;
import com.phantomaddons.features.supplies.SupplyProgressHud;
import com.phantomaddons.features.supplies.SupplyWaypointTracker;
import com.phantomaddons.features.stundps.FastDpsWarning;
import com.phantomaddons.features.boss.SoloDetector;
import com.phantomaddons.features.misckuudra.splits.KuudraSplitTimer;
import com.phantomaddons.features.supplies.pearlwaypoints.PearlWaypointManager;
import com.phantomaddons.logging.PhaseLogger;
import com.phantomaddons.utils.Phase2BuildTracker;
import com.phantomaddons.features.render.HideArmorStands;
import com.phantomaddons.features.boss.rend.RendDamage;
import com.phantomaddons.features.boss.rend.RendTracker;
import com.phantomaddons.utils.KuudraTierDetector;
import com.phantomaddons.utils.RoleManager;
import net.minecraft.client.Minecraft;

public final class KuudraPhaseEvents {

    public static void onPhaseChanged(KuudraPhaseTracker.Phase phase) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        client.execute(() -> {
            switch (phase) {

                case SUPPLIES -> {
                    HideArmorStands.activate();
                    SupplyWaypointTracker.onSuppliesStart();
                    NoPreAnnounce.onSuppliesStart();
                    EtherwarpWaypointManager.onSuppliesStart();
                    SupplyProgressHud.onSuppliesStart();
                    BuildProgressHud.reset();
                    AnnounceFresh.reset();
                    PearlWaypointManager.reset();
                    AutoGFS.stop();
                    Phase2BuildTracker.stop();
                    PhaseLogger.end();
                    FastDpsWarning.onDpsEnd();
                    SoloDetector.onPhaseEnd();
                    SoloDetector.onPhaseStart();
                    KuudraSplitTimer.onSuppliesStart();
                    com.phantomaddons.features.stundps.DpsWaypoint.reset();
                    com.phantomaddons.data.RunRecorder.onRunStart();
                }

                case BUILD -> {
                    SupplyWaypointTracker.reset();
                    EtherwarpWaypointManager.reset();
                    SupplyProgressHud.reset();
                    BuildProgressTracker.start();
                    BuildProgressHud.onBuildStart();
                    AnnounceFresh.onBuildStart();
                    Phase2BuildTracker.start();
                    PearlWaypointManager.reset();
                    KuudraSplitTimer.onBuildStart();
                }

                case EATEN -> {
                    BuildProgressTracker.stop();
                    BuildProgressHud.reset();
                    PhaseLogger.resetTick();
                    RoleManager.reset();
                    if (PhantomConfig.isAutoMode()) {
                        RoleManager.resolveAutoRole(client);
                    } else {
                        RoleManager.setManualRole(PhantomConfig.getRoleMode());
                    }
                    AutoGFS.queueCommand();
                    AutoGFS.start(PhantomConfig.getDpsRefillAmount());
                    PhaseLogger.begin();
                    KuudraSplitTimer.onEatenStart();
                }

                case STUN -> {
                    RendDamage.onKillPhaseStart();
                    RendTracker.onKillPhaseStart();
                    com.phantomaddons.features.boss.backbone.BackboneProgressBar.reset();
                    KuudraSplitTimer.onStunStart();
                }

                case DPS -> {
                    RendDamage.onKillPhaseStart();
                    RendTracker.onKillPhaseStart();
                    com.phantomaddons.features.boss.backbone.BackboneProgressBar.reset();
                    FastDpsWarning.onDpsStart();
                    KuudraSplitTimer.onDpsStart();
                }

                case SKIP -> {
                    BuildProgressTracker.stop();
                    BuildProgressHud.reset();
                    AnnounceFresh.reset();
                    AutoGFS.stop();
                    Phase2BuildTracker.stop();
                    PhaseLogger.end();
                    RoleManager.reset();
                    FastDpsWarning.onDpsEnd();
                    PearlWaypointManager.reset();
                    KuudraSplitTimer.onSkipStart();
                }

                case BOSS -> {
                    RendDamage.onBossPhaseStart();
                    RendTracker.onBossPhaseStart();
                    com.phantomaddons.features.boss.backbone.BackboneProgressBar.reset();
                    KuudraSplitTimer.onBossStart();
                }

                case KILL, DEATH -> {
                    if (KuudraSplitTimer.getActiveSplit() != KuudraSplitTimer.Split.BOSS) {
                        KuudraSplitTimer.onBossStart();
                    }
                }

                case END -> {
                    com.phantomaddons.features.misckuudra.AutoRequeue.trigger();
                    RendDamage.reset();
                    com.phantomaddons.features.boss.rend.RendTracker.reset();
                    com.phantomaddons.features.boss.rend.RendPullAttribution.reset();
                    com.phantomaddons.features.boss.backbone.BackboneProgressBar.reset();
                    HideArmorStands.deactivate();
                    BuildProgressTracker.stop();
                    BuildProgressHud.reset();
                    AnnounceFresh.reset();
                    SupplyProgressHud.reset();
                    AutoGFS.stop();
                    Phase2BuildTracker.stop();
                    PhaseLogger.end();
                    RoleManager.reset();
                    FastDpsWarning.onDpsEnd();
                    PearlWaypointManager.reset();
                    KuudraSplitTimer.onEndStart();
                    com.phantomaddons.data.RunRecorder.onRunEnd();
                }

                case NONE -> com.phantomaddons.data.RunRecorder.onRunAbandoned();

                default -> {}
            }
        });
    }
}
