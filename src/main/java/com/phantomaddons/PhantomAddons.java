package com.phantomaddons;

import com.phantomaddons.features.build.AnnounceFresh;
import com.phantomaddons.features.stundps.AutoGFS;
import com.phantomaddons.features.misckuudra.AutoRequeue;
import com.phantomaddons.features.build.buildprogress.BuildProgressHud;
import com.phantomaddons.features.build.buildprogress.BuildProgressTracker;
import com.phantomaddons.features.misckuudra.EtherwarpPredictor;
import com.phantomaddons.features.stundps.FastDpsWarning;
import com.phantomaddons.features.misckuudra.HollowWandAnnouncer;
import com.phantomaddons.features.stundps.MountTimerHud;
import com.phantomaddons.features.misckuudra.PartyCommands;
import com.phantomaddons.features.miscskyblock.PearlRefill;
import com.phantomaddons.features.misckuudra.PickoblockManager;
import com.phantomaddons.features.misckuudra.ShopKeybinds;
import com.phantomaddons.features.supplies.SlotBlocker;
import com.phantomaddons.features.misckuudra.NotificationHud;
import com.phantomaddons.features.misckuudra.splits.KuudraSplitTimer;
import com.phantomaddons.features.supplies.CratePriority;
import com.phantomaddons.features.supplies.nopre.NoPreAnnounce;
import com.phantomaddons.features.supplies.SupplyProgressHud;
import com.phantomaddons.features.supplies.SupplyWaypointTracker;
import com.phantomaddons.features.supplies.SupplyTracker;
import com.phantomaddons.features.supplies.pearlwaypoints.PearlTitleHud;
import com.phantomaddons.features.supplies.pearlwaypoints.PearlTitleListener;
import com.phantomaddons.logging.GiantYLogger;
import com.phantomaddons.logging.KuudraStationaryLogger;
import com.phantomaddons.logging.PhaseLogAppender;
import com.phantomaddons.logging.PhaseLogger;
import com.phantomaddons.phase.KuudraPhaseTracker;
import com.phantomaddons.utils.Phase2BuildTracker;
import com.phantomaddons.utils.RoleManager;
import com.phantomaddons.features.customisation.lava.LavaRenderInit;
import com.phantomaddons.features.supplies.doublepearl.DoublePearlCoords;
import com.phantomaddons.features.supplies.nopre.NoPre;
import com.phantomaddons.features.supplies.pearlwaypoints.PearlWaypointManager;
import com.phantomaddons.features.boss.SoloDetector;
import com.phantomaddons.features.boss.KuudraDirectionHud;
import com.phantomaddons.features.boss.rend.RendDamage;
import com.phantomaddons.features.stundps.CannonAutoClose;
import com.phantomaddons.features.loadouts.WardrobeKeybinds;
import com.phantomaddons.utils.KuudraTierDetector;
import com.phantomaddons.features.misckuudra.chesttracking.ChestTracker;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import com.phantomaddons.features.misckuudra.chesttracking.TabListChestSync;
import com.phantomaddons.features.dungeons.DungeonsGfs;
import com.phantomaddons.features.misckuudra.splits.SplitHud;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhantomAddons implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("phantomaddons");

    private static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.phantomaddons.opengui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            KeyMapping.Category.MISC);

    private static boolean openGuiNextTick = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("PhantomAddons initialised");

        org.apache.logging.log4j.core.Logger rootLogger =
                (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        PhaseLogAppender appender = new PhaseLogAppender();
        appender.start();
        rootLogger.addAppender(appender);

        UpdateChecker.cleanupLeftoverJars();
        PhantomConfig.load();
        com.phantomaddons.features.customisation.VisualWords.load();
        com.phantomaddons.features.misckuudra.ShitterList.load();
        PickoblockManager.init();
        MountTimerHud.register();

        KuudraPhaseTracker.init();

        registerElleFilter();
        registerChatEvents();
        registerCommands();
        registerConnectionEvents();
        registerTickEvents();
        {
            var nativeLava  = net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry.get(net.minecraft.world.level.material.Fluids.LAVA);
            var nativeWater = net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry.get(net.minecraft.world.level.material.Fluids.WATER);
            LavaRenderInit.init(nativeLava, nativeWater);
        }
        ChestTracker.init();
        TabListChestSync.init();
        PearlRefill.register();
        CannonAutoClose.register();
        KuudraTierDetector.init();
        ShopKeybinds.register();
        WardrobeKeybinds.register();
        FastDpsWarning.register();
        PartyCommands.register();
        AutoRequeue.register();
        KuudraSplitTimer.register();
        SplitHud.register();
        PhantomCommands.register();
        AutoGFS.register();
        UpdateChecker.register();
        UpdateChecker.checkOnStartup();
        com.phantomaddons.features.misckuudra.profile.RemoteFeatureGate.checkOnStartup();
        SlotBlocker.register();
        com.phantomaddons.features.boss.AtomsplitBlocker.register();
        KuudraDirectionHud.register();
        com.phantomaddons.features.boss.bonetiming.BoneTimingAssist.register();
        PearlTitleHud.register();
        com.phantomaddons.features.supplies.smoothcrate.SmoothCratePickupHud.register();
        com.phantomaddons.features.boss.backbone.BackboneProgressBarHud.register();
        com.phantomaddons.features.supplies.doublepearl.DoublePearlWarningHud.register();
        BuildProgressTracker.register();
        RendDamage.register();
        SupplyProgressHud.register();
        BuildProgressHud.register();
        NotificationHud.register();
        com.phantomaddons.features.misckuudra.TuxedoWarning.register();
        com.phantomaddons.features.misckuudra.KickedTimerHud.register();
        CratePriority.register();
        com.phantomaddons.features.boss.KuudraHpHud.register();

        com.phantomaddons.features.misckuudra.profittracker.ProfitStore.load();
        com.phantomaddons.features.misckuudra.profittracker.ProfitHud.register();
        registerProfitTrackerPhaseListener();
    }

    private void registerProfitTrackerPhaseListener() {
        // Track run start (used for duration) and fire price fetches on run end
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!PhantomConfig.isProfitTrackerEnabled()) return;
            com.phantomaddons.features.misckuudra.profittracker.PriceFetcher.fetchBazaarIfStale();
            com.phantomaddons.features.misckuudra.profittracker.PriceFetcher.fetchBinsIfStale();
        });
    }

    private static void registerChatEvents() {
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            String raw = message.getString();
            String clean = raw.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
            if (isInSuppliesPhase()) NoPre.onChat(raw);
            NoPreAnnounce.onChat(raw);
            SupplyWaypointTracker.onChat(raw);
            DoublePearlCoords.onChat(raw);
            DungeonsGfs.onChat(raw);
            SoloDetector.onChat(raw);
            AnnounceFresh.onChat(raw);
            CratePriority.onChat(clean);
            handleSupplyNotifications(clean);
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String raw = message.getString();
            String clean = raw.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
            if (!overlay && isInSuppliesPhase()) NoPre.onChat(raw);
            if (!overlay) NoPreAnnounce.onChat(raw);
            if (!overlay) DoublePearlCoords.onChat(raw);
            DungeonsGfs.onChat(raw);
            SoloDetector.onChat(raw);
            if (!overlay) AnnounceFresh.onChat(raw);
            if (!overlay) CratePriority.onChat(clean);
            if (!overlay) handleSupplyNotifications(clean);
            if (!overlay) com.phantomaddons.features.boss.mana.ManaDrainAnnouncer.onChat(clean);
            // Action bar (overlay) text carries the live "current/max✎" mana readout.
            if (overlay) com.phantomaddons.features.boss.mana.ManaTracker.onChat(clean);
            if (clean.contains("Used Extreme Focus!")) com.phantomaddons.features.boss.rend.RendTracker.onManaDrain();
            com.phantomaddons.features.misckuudra.HollowWandAnnouncer.onChat(clean);
            if (!overlay && clean.contains("A kick occurred in your connection, so you were put in the SkyBlock lobby!")
                    && PhantomConfig.isKickedNotificationEnabled()) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                com.phantomaddons.features.misckuudra.KickedTimerHud.onKicked();
                PhantomConfig.playNotificationSound(PhantomConfig.SOUND_KICKED);
                if (mc.getConnection() != null)
                    mc.execute(() -> mc.getConnection().sendCommand("pc [Phantom] Kicked from Skyblock!"));
            }
        });
    }

    private static boolean isInSuppliesPhase() {
        return KuudraPhaseTracker.getPhase() == KuudraPhaseTracker.Phase.SUPPLIES;
    }

    private static boolean isNpreElleLine(String msg) {
        String lc = msg.toLowerCase();
        return lc.contains("head over to the main platform")
                || lc.contains("not again!");
    }

    private static String toLegacyString(net.minecraft.network.chat.Component comp) {
        StringBuilder sb = new StringBuilder();
        comp.visit((style, str) -> {
            net.minecraft.network.chat.TextColor tc = style.getColor();
            if (tc == null) {
                sb.append("§r");
            } else {
                int rgb = tc.getValue();
                for (net.minecraft.ChatFormatting cf : net.minecraft.ChatFormatting.values()) {
                    if (cf.isColor() && cf.getColor() != null && cf.getColor() == rgb) {
                        sb.append('§').append(cf.getChar());
                        break;
                    }
                }
            }
            if (Boolean.TRUE.equals(style.isBold())) sb.append("§l");
            sb.append(str);
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return sb.toString();
    }

    private static final java.util.regex.Pattern SUPPLY_RECOVERY_PATTERN =
            java.util.regex.Pattern.compile("(\\S+)\\s+recovered one of Elle's supplies! \\((\\d+/\\d+)\\)");
    private static final java.util.regex.Pattern RANK_NAME_PATTERN =
            java.util.regex.Pattern.compile("^(.*?)\\s+recovered one of Elle's supplies!");

    private static void registerElleFilter() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((text, overlay) -> {
            String raw   = toLegacyString(text);
            String clean = raw.replaceAll("§[0-9a-fk-orA-FK-OR]", "");

            if (isNpreElleLine(clean)) return true;

            if (PhantomConfig.isHideElleDialogueEnabled()
                    && clean.toLowerCase().contains("[npc] elle:")) return false;

            if (PhantomConfig.isSupplyRecoveryMsgEnabled()) {
                java.util.regex.Matcher m = SUPPLY_RECOVERY_PATTERN.matcher(clean);
                if (m.find()) {
                    String playerName = m.group(1);
                    String countStr   = m.group(2);

                    java.util.regex.Matcher rm = RANK_NAME_PATTERN.matcher(raw);
                    String rawPrefix = rm.find() ? rm.group(1) : playerName;
                    rawPrefix = rawPrefix.replaceAll("§r$", "").stripTrailing();

                    SupplyTracker.onChat(raw);
                    double elapsed = KuudraSplitTimer.recordSupplyRecovery(playerName);

                    String timeStr;
                    String timeColor;
                    if (elapsed >= 0) {
                        timeStr   = String.format("%.2fs", elapsed);
                        timeColor = elapsed < 19 ? "§f"
                                  : elapsed < 24 ? "§b"
                                  : elapsed < 28 ? "§a"
                                  : "§c";
                    } else {
                        timeStr   = "?.??s";
                        timeColor = "§7";
                    }
                    String replacement = "§7[Phantom] " + rawPrefix + " §7recovered a supply in "
                            + timeColor + timeStr + "§7! (" + countStr + ")";

                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    mc.execute(() -> {
                        if (mc.player != null)
                            mc.player.sendSystemMessage(Component.literal(replacement));
                    });
                    return false;
                }
            }

            return true;
        });
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, ts) -> {
            String msg = message.getString().replaceAll("§[0-9a-fk-orA-FK-OR]", "");
            if (isNpreElleLine(msg)) return true;
            return !(PhantomConfig.isHideElleDialogueEnabled()
                    && msg.toLowerCase().contains("[npc] elle:"));
        });
    }

    private void registerCommands() {
        // Intercept /pa and /phantom before they reach the server (e.g. Hypixel has its own /pa).
        // ALLOW_COMMAND fires with the raw command string (no leading slash).
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            if (command.trim().equalsIgnoreCase("pa")) {
                openGuiNextTick = true;
                return false; // cancel — don't send to server
            }
            return true;
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            for (String alias : new String[]{"pa", "phantom"}) {
                dispatcher.register(ClientCommands.literal(alias)
                        .executes(ctx -> { openGuiNextTick = true; return 1; }));
            }

            dispatcher.register(ClientCommands.literal("phaselog")
                    .executes(ctx -> {
                        if (!PhantomConfig.isDeveloperFeaturesEnabled()) {
                            ctx.getSource().sendFeedback(Component.literal("Developer Features is disabled (About tab)."));
                            return 0;
                        }
                        boolean next = !PhaseLogger.isEnabled();
                        PhaseLogger.setEnabled(next);
                        ctx.getSource().sendFeedback(Component.literal("Phase logging: " + next));
                        LOGGER.info("[PhantomAddons] Phase logging = {}", next);
                        return 1;
                    }));

            dispatcher.register(ClientCommands.literal("giantlog")
                    .executes(ctx -> {
                        if (!PhantomConfig.isDeveloperFeaturesEnabled()) {
                            ctx.getSource().sendFeedback(Component.literal("Developer Features is disabled (About tab)."));
                            return 0;
                        }
                        boolean next = !GiantYLogger.isEnabled();
                        GiantYLogger.setEnabled(next);
                        ctx.getSource().sendFeedback(Component.literal("Giant Y logging: " + next));
                        LOGGER.info("[PhantomAddons] Giant Y logging = {}", next);
                        return 1;
                    }));

            dispatcher.register(ClientCommands.literal("kuudrastationarylog")
                    .executes(ctx -> {
                        if (!PhantomConfig.isDeveloperFeaturesEnabled()) {
                            ctx.getSource().sendFeedback(Component.literal("Developer Features is disabled (About tab)."));
                            return 0;
                        }
                        boolean next = !KuudraStationaryLogger.isEnabled();
                        KuudraStationaryLogger.setEnabled(next);
                        ctx.getSource().sendFeedback(Component.literal("Kuudra stationary logging: " + next));
                        LOGGER.info("[PhantomAddons] Kuudra stationary logging = {}", next);
                        return 1;
                    }));

            dispatcher.register(ClientCommands.literal("phantomdebug")
                    .executes(ctx -> {
                        if (!PhantomConfig.isDeveloperFeaturesEnabled()) {
                            ctx.getSource().sendFeedback(Component.literal("Developer Features is disabled (About tab)."));
                            return 0;
                        }
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc.level == null || mc.player == null) return 0;
                        var clusters = com.phantomaddons.features.supplies.SupplyWaypointTracker.detectedClusters;
                        if (clusters.isEmpty()) {
                            ctx.getSource().sendFeedback(Component.literal("[Debug] No supply clusters detected."));
                            return 1;
                        }
                        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
                        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                            for (com.phantomaddons.features.supplies.SupplyCluster c : clusters) {
                                if (e.position().distanceTo(c.center) <= 15.0) {
                                    String type = e.getClass().getSimpleName();
                                    counts.merge(type, 1, Integer::sum);
                                    break;
                                }
                            }
                        }
                        ctx.getSource().sendFeedback(Component.literal("[Debug] Clusters: " + clusters.size()));
                        counts.forEach((type, count) ->
                            ctx.getSource().sendFeedback(Component.literal("  " + type + " x" + count)));
                        if (counts.isEmpty())
                            ctx.getSource().sendFeedback(Component.literal("  (no entities within 15 blocks of any cluster)"));
                        return 1;
                    }));
        });
    }

    private void registerConnectionEvents() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetAll());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            resetAll();
            if (ChestTracker.isPendingPc()) {
                ChestTracker.clearPendingPc();
                client.execute(() -> {
                    if (client.player != null && client.getConnection() != null)
                        client.getConnection().sendCommand("pc !dt Last Run! Chests 59/60");
                });
            }
        });
    }

    private static void resetAll() {
        KuudraPhaseTracker.reset();
        com.phantomaddons.features.render.HideArmorStands.deactivate();
        AutoGFS.stop();
        Phase2BuildTracker.stop();
        PhaseLogger.end();
        RoleManager.reset();
        PearlWaypointManager.reset();
        PearlTitleListener.reset();
        SoloDetector.onPhaseEnd();
        KuudraTierDetector.init();
        KuudraSplitTimer.reset();
        CannonAutoClose.reset();
        PartyCommands.reset();
        com.phantomaddons.features.misckuudra.profile.AutoKickManager.reset();
        com.phantomaddons.features.misckuudra.ShitterList.reset();
        com.phantomaddons.features.misckuudra.AutoKickCoordinator.reset();
        com.phantomaddons.features.miscskyblock.PredictedBobber.reset();
        AutoRequeue.resetSession();
        ShopKeybinds.reset();
        WardrobeKeybinds.reset();
        KuudraDirectionHud.reset();
        BuildProgressTracker.stop();
        BuildProgressHud.reset();
        SupplyProgressHud.reset();
        AnnounceFresh.reset();
        PearlRefill.reset();
        RendDamage.reset();
        com.phantomaddons.features.boss.rend.RendTracker.reset();
        com.phantomaddons.features.boss.backbone.BackboneProgressBar.reset();
        com.phantomaddons.features.boss.bonetiming.BoneTimingAssist.reset();
        com.phantomaddons.features.boss.KuudraHpHud.reset();
        SupplyWaypointTracker.reset();
        NoPreAnnounce.reset();
        com.phantomaddons.features.supplies.PartyChatQueue.reset();
        com.phantomaddons.features.supplies.etherwarp.EtherwarpWaypointManager.reset();
        NotificationHud.reset();
        CratePriority.reset();
    }

    private void registerTickEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (openGuiNextTick) {
                openGuiNextTick = false;
                if (client.player != null && client.level != null && client.screen == null) {
                    client.setScreen(new PhantomScreen());
                }
            }

            while (OPEN_GUI_KEY.consumeClick()) {
                if (client.player != null && client.level != null && client.screen == null) {
                    client.setScreen(new PhantomScreen());
                }
            }

            if (PhantomConfig.isPearlWaypointsEnabled()
                    && KuudraPhaseTracker.getPhase() == KuudraPhaseTracker.Phase.SUPPLIES) {
                PearlTitleListener.tick();
            }

            AutoGFS.flushCommands(client);
            AutoGFS.tick(client);
            PearlRefill.tick(client);
            if (PhantomConfig.isHideSelfieEnabled()
                    && client.options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_FRONT) {
                client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            }
            Phase2BuildTracker.tick(client);
            PickoblockManager.tick(client);
            EtherwarpPredictor.tick(client);
            PhaseLogger.tick(client);
            SupplyWaypointTracker.tick(client);
            com.phantomaddons.features.supplies.nopre.NoPreAnnounce.tick(client);
            com.phantomaddons.features.supplies.PartyChatQueue.tick(client);
            com.phantomaddons.features.misckuudra.profile.AutoKickManager.tick(client);
            com.phantomaddons.features.misckuudra.ShitterList.tick(client);
            com.phantomaddons.features.supplies.giant.SupplyGiantHitbox.tick(client);
            com.phantomaddons.features.supplies.giant.GiantHitboxOutline.tick(client);
            GiantYLogger.tick(client);
            KuudraStationaryLogger.tick(client);
            com.phantomaddons.features.boss.rend.RendTracker.tick();
            com.phantomaddons.features.boss.backbone.BackboneProgressBar.tick();

            if (PhantomConfig.isAutoSprintEnabled() && client.player != null
                    && client.options.keyUp.isDown()) {
                client.player.setSprinting(true);
            }
        });
    }

    private static void handleSupplyNotifications(String clean) {
        if (PhantomConfig.isSupplyGrabbedNotifyEnabled()
                && clean.contains("Someone else is currently trying to pick up these supplies")) {
            com.phantomaddons.features.misckuudra.NotificationHud.show("§cSupply already taken!", 3000);
            PhantomConfig.playNotificationSound(PhantomConfig.SOUND_SUPPLY_GRABBED);
        }

        if (PhantomConfig.isSupplyDroppedNotifyEnabled()
                && clean.contains("the Chest slipped out of your hands")) {
            com.phantomaddons.features.misckuudra.NotificationHud.show("§cYou dropped a supply!", 3000);
            PhantomConfig.playNotificationSound(PhantomConfig.SOUND_SUPPLY_DROPPED);
        }
    }
}