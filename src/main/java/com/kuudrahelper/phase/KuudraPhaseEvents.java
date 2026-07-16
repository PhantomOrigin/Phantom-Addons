package com.kuudrahelper.phase;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.build.AnnounceFresh;
import com.kuudrahelper.features.stundps.AutoGFS;
import com.kuudrahelper.features.build.BuildProgressHud;
import com.kuudrahelper.features.build.BuildProgressTracker;
import com.kuudrahelper.features.supplies.EtherwarpWaypointManager;
import com.kuudrahelper.features.supplies.NoPreAnnounce;
import com.kuudrahelper.features.supplies.SupplyProgressHud;
import com.kuudrahelper.features.supplies.SupplyWaypointTracker;
import com.kuudrahelper.features.stundps.FastDpsWarning;
import com.kuudrahelper.features.boss.SoloDetector;
import com.kuudrahelper.features.misckuudra.splits.KuudraSplitTimer;
import com.kuudrahelper.features.supplies.PearlWaypointManager;
import com.kuudrahelper.logging.PhaseLogger;
import com.kuudrahelper.utils.Phase2BuildTracker;
import com.kuudrahelper.features.render.HideArmorStands;
import com.kuudrahelper.features.boss.RendDamage;
import com.kuudrahelper.features.boss.RendTracker;
import com.kuudrahelper.utils.KuudraTierDetector;
import com.kuudrahelper.utils.RoleManager;
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
                    if (KuudraConfig.isAutoMode()) {
                        RoleManager.resolveAutoRole(client);
                    } else {
                        RoleManager.setManualRole(KuudraConfig.getRoleMode());
                    }
                    AutoGFS.queueCommand();
                    AutoGFS.start(KuudraConfig.getDpsRefillAmount());
                    PhaseLogger.begin();
                    KuudraSplitTimer.onEatenStart();
                }

                case STUN -> {
                    RendDamage.onKillPhaseStart();
                    RendTracker.onKillPhaseStart();
                    com.kuudrahelper.features.boss.BackboneProgressBar.reset();
                    KuudraSplitTimer.onStunStart();
                }

                case DPS -> {
                    RendDamage.onKillPhaseStart();
                    RendTracker.onKillPhaseStart();
                    com.kuudrahelper.features.boss.BackboneProgressBar.reset();
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
                    com.kuudrahelper.features.boss.BackboneProgressBar.reset();
                    KuudraSplitTimer.onBossStart();
                }

                case KILL, DEATH -> {
                    if (KuudraSplitTimer.getActiveSplit() != KuudraSplitTimer.Split.BOSS) {
                        KuudraSplitTimer.onBossStart();
                    }
                }

                case END -> {
                    com.kuudrahelper.features.misckuudra.AutoRequeue.trigger();
                    RendDamage.reset();
                    com.kuudrahelper.features.boss.RendTracker.reset();
                    com.kuudrahelper.features.boss.BackboneProgressBar.reset();
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
                }

                default -> {}
            }
        });
    }
}
