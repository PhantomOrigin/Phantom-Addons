package com.kuudrahelper;

import com.kuudrahelper.features.*;
import com.kuudrahelper.features.NotificationHud;
import com.kuudrahelper.features.splits.KuudraSplitTimer;
import com.kuudrahelper.features.supplies.CratePriority;
import com.kuudrahelper.features.supplies.NoPreAnnounce;
import com.kuudrahelper.features.supplies.SupplyProgressHud;
import com.kuudrahelper.features.supplies.SupplyWaypointTracker;
import com.kuudrahelper.features.pearls.SupplyTracker;
import com.kuudrahelper.features.pearls.PearlTitleHud;
import com.kuudrahelper.features.pearls.PearlTitleListener;
import com.kuudrahelper.logging.GiantYLogger;
import com.kuudrahelper.logging.PhaseLogAppender;
import com.kuudrahelper.logging.PhaseLogger;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import com.kuudrahelper.utils.Phase2BuildTracker;
import com.kuudrahelper.utils.RoleManager;
import com.kuudrahelper.features.lava.LavaRenderInit;
import com.kuudrahelper.features.pearls.DoublePearlCoords;
import com.kuudrahelper.features.pearls.NoPre;
import com.kuudrahelper.features.pearls.PearlWaypointManager;
import com.kuudrahelper.features.SoloDetector;
import com.kuudrahelper.features.kuudra.KuudraDirectionHud;
import com.kuudrahelper.features.kuudra.RendDamage;
import com.kuudrahelper.features.CannonAutoClose;
import com.kuudrahelper.features.WardrobeKeybinds;
import com.kuudrahelper.utils.KuudraTierDetector;
import com.kuudrahelper.features.ChestTracker;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import com.kuudrahelper.features.TabListChestSync;
import com.kuudrahelper.features.dungeons.DungeonsGfs;
import com.kuudrahelper.features.splits.SplitHud;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KuudraHelperMod implements ClientModInitializer {

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
        KuudraConfig.load();
        com.kuudrahelper.features.VisualWords.load();
        com.kuudrahelper.features.ShitterList.load();
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
            com.kuudrahelper.features.water.WaterRenderInit.init(nativeWater, nativeLava);
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
        SlotBlocker.register();
        KuudraDirectionHud.register();
        PearlTitleHud.register();
        com.kuudrahelper.features.supplies.DoublePearlWarningHud.register();
        BuildProgressTracker.register();
        RendDamage.register();
        SupplyProgressHud.register();
        BuildProgressHud.register();
        NotificationHud.register();
        com.kuudrahelper.features.KickedTimerHud.register();
        CratePriority.register();
        com.kuudrahelper.features.kuudra.KuudraHpHud.register();

        com.kuudrahelper.features.profittracker.ProfitStore.load();
        com.kuudrahelper.features.profittracker.ProfitHud.register();
        registerProfitTrackerPhaseListener();
    }

    private void registerProfitTrackerPhaseListener() {
        // Track run start (used for duration) and fire price fetches on run end
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!KuudraConfig.isProfitTrackerEnabled()) return;
            com.kuudrahelper.features.profittracker.PriceFetcher.fetchBazaarIfStale();
            com.kuudrahelper.features.profittracker.PriceFetcher.fetchBinsIfStale();
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
            if (!overlay) com.kuudrahelper.features.kuudra.ManaDrainAnnouncer.onChat(clean);
            if (clean.contains("Used Extreme Focus!")) com.kuudrahelper.features.kuudra.RendTracker.onManaDrain();
            com.kuudrahelper.features.HollowWandAnnouncer.onChat(clean);
            if (!overlay && clean.contains("A kick occurred in your connection, so you were put in the SkyBlock lobby!")
                    && KuudraConfig.isKickedNotificationEnabled()) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                com.kuudrahelper.features.KickedTimerHud.onKicked();
                KuudraConfig.playNotificationSound(KuudraConfig.SOUND_KICKED);
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

            if (KuudraConfig.isHideElleDialogueEnabled()
                    && clean.toLowerCase().contains("[npc] elle:")) return false;

            if (KuudraConfig.isSupplyRecoveryMsgEnabled()) {
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
            return !(KuudraConfig.isHideElleDialogueEnabled()
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
                        boolean next = !PhaseLogger.isEnabled();
                        PhaseLogger.setEnabled(next);
                        ctx.getSource().sendFeedback(Component.literal("Phase logging: " + next));
                        LOGGER.info("[PhantomAddons] Phase logging = {}", next);
                        return 1;
                    }));

            dispatcher.register(ClientCommands.literal("giantlog")
                    .executes(ctx -> {
                        boolean next = !GiantYLogger.isEnabled();
                        GiantYLogger.setEnabled(next);
                        ctx.getSource().sendFeedback(Component.literal("Giant Y logging: " + next));
                        LOGGER.info("[PhantomAddons] Giant Y logging = {}", next);
                        return 1;
                    }));

            dispatcher.register(ClientCommands.literal("phantomdebug")
                    .executes(ctx -> {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc.level == null || mc.player == null) return 0;
                        var clusters = com.kuudrahelper.features.supplies.SupplyWaypointTracker.detectedClusters;
                        if (clusters.isEmpty()) {
                            ctx.getSource().sendFeedback(Component.literal("[Debug] No supply clusters detected."));
                            return 1;
                        }
                        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
                        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                            for (com.kuudrahelper.features.supplies.SupplyCluster c : clusters) {
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
            KuudraConfig.setAutoRequeueEnabled(true);
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
        com.kuudrahelper.features.HideArmorStands.deactivate();
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
        AutoRequeue.reset();
        ShopKeybinds.reset();
        WardrobeKeybinds.reset();
        KuudraDirectionHud.reset();
        BuildProgressTracker.stop();
        BuildProgressHud.reset();
        SupplyProgressHud.reset();
        AnnounceFresh.reset();
        PearlRefill.reset();
        RendDamage.reset();
        com.kuudrahelper.features.kuudra.RendTracker.reset();
        com.kuudrahelper.features.kuudra.KuudraHpHud.reset();
        SupplyWaypointTracker.reset();
        NoPreAnnounce.reset();
        com.kuudrahelper.features.supplies.EtherwarpWaypointManager.reset();
        NotificationHud.reset();
        CratePriority.reset();
    }

    private void registerTickEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (openGuiNextTick) {
                openGuiNextTick = false;
                if (client.player != null && client.level != null && client.screen == null) {
                    client.setScreen(new KuudraScreen());
                }
            }

            while (OPEN_GUI_KEY.consumeClick()) {
                if (client.player != null && client.level != null && client.screen == null) {
                    client.setScreen(new KuudraScreen());
                }
            }

            if (KuudraConfig.isPearlWaypointsEnabled()
                    && KuudraPhaseTracker.getPhase() == KuudraPhaseTracker.Phase.SUPPLIES) {
                PearlTitleListener.tick();
            }

            AutoGFS.flushCommands(client);
            AutoGFS.tick(client);
            PearlRefill.tick(client);
            if (KuudraConfig.isHideSelfieEnabled()
                    && client.options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_FRONT) {
                client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            }
            Phase2BuildTracker.tick(client);
            PickoblockManager.tick(client);
            EtherwarpPredictor.tick(client);
            PhaseLogger.tick(client);
            SupplyWaypointTracker.tick(client);
            com.kuudrahelper.features.supplies.NoPreAnnounce.tick(client);
            com.kuudrahelper.features.supplies.SupplyGiantHitbox.tick(client);
            com.kuudrahelper.features.supplies.GiantHitboxOutline.tick(client);
            GiantYLogger.tick(client);
            com.kuudrahelper.features.kuudra.RendTracker.tick();

            if (KuudraConfig.isAutoSprintEnabled() && client.player != null) {
                client.player.setSprinting(true);
            }
        });
    }

    private static void handleSupplyNotifications(String clean) {
        if (KuudraConfig.isSupplyGrabbedNotifyEnabled()
                && clean.contains("Someone else is currently trying to pick up these supplies")) {
            com.kuudrahelper.features.NotificationHud.show("§cSupply already taken!", 3000);
            KuudraConfig.playNotificationSound(KuudraConfig.SOUND_SUPPLY_GRABBED);
        }

        if (KuudraConfig.isSupplyDroppedNotifyEnabled()
                && clean.contains("the Chest slipped out of your hands")) {
            com.kuudrahelper.features.NotificationHud.show("§cYou dropped a supply!", 3000);
            KuudraConfig.playNotificationSound(KuudraConfig.SOUND_SUPPLY_DROPPED);
        }
    }
}