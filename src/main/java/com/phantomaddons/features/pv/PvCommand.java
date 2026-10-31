package com.phantomaddons.features.pv;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.phantomaddons.PhantomConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.loader.api.FabricLoader;
import com.phantomaddons.features.misckuudra.profile.RemoteFeatureGate;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public final class PvCommand {

    private static final String PREFIX = "§6[Phantom] ";

    private PvCommand() {}

    private static final String[] OTHER_VIEWERS = { "skyblocker", "skyblockpv" };

    private static boolean otherViewerLoaded() {
        for (String id : OTHER_VIEWERS) if (FabricLoader.getInstance().isModLoaded(id)) return true;
        return false;
    }

    public static void register() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (PhantomConfig.isPvEnabledSetting()) PvItems.warmUp();
        });
        if (otherViewerLoaded()) return;
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("pv")
                        .executes(ctx -> { if (!intercept("pv")) open(null); return 1; })
                        .then(ClientCommands.argument("player", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(PvNameBook.suggestions(), builder))
                                .executes(ctx -> {
                                    String player = StringArgumentType.getString(ctx, "player");
                                    if (!intercept("pv " + player)) open(player);
                                    return 1;
                                }))));
    }

    public static boolean intercept(String command) {
        if (command == null) return false;
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0 || parts.length > 2 || !parts[0].equals("pv")) return false;
        String name = parts.length == 2 ? parts[1] : null;
        Minecraft mc = Minecraft.getInstance();
        if (!RemoteFeatureGate.isEnabled()) return false;
        if (needsPrompt()) {
            String cmd = command.trim();
            mc.execute(() -> mc.gui.setScreen(new PvPromptScreen(cmd)));
            return true;
        }
        if (PhantomConfig.isPvEnabled()) { open(name); return true; }
        return false;
    }

    private static boolean needsPrompt() {
        if (PhantomConfig.isPvPromptAnswered()) return false;
        boolean usedBefore = PhantomConfig.hasOpenedPv()
                || java.nio.file.Files.isDirectory(com.phantomaddons.utils.PhantomPaths.resolve("pv_cache"));
        return !(PhantomConfig.isPvEnabledSetting() && usedBefore);
    }

    private static void open(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        boolean testMode = java.nio.file.Files.exists(PvFetcher.testFile());
        if (!PhantomConfig.isPvEnabled()) {
            mc.player.sendSystemMessage(Component.literal(PREFIX + "§cThe Profile Viewer is disabled (Profile Viewer tab in /pa)."));
            return;
        }
        String ownName = mc.player.getGameProfile().name();
        UUID uuid = null;
        if (name == null || name.equalsIgnoreCase(ownName)) {
            name = ownName;
            uuid = mc.player.getUUID();
        }
        String target = name;
        UUID targetUuid = uuid;
        PvNameBook.noteViewed(target);
        PhantomConfig.markPvOpened();
        mc.execute(() -> mc.gui.setScreen(new PvScreen(target, targetUuid)));
    }
}
