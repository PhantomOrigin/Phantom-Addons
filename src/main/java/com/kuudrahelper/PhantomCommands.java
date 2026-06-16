package com.kuudrahelper;

import com.kuudrahelper.phase.KuudraPhaseTracker;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class PhantomCommands {

    private static final String PREFIX = "§f[PhantomAddons]§r ";
    private static final String[] TIER_NAMES  = {"", "Normal", "Hot", "Burning", "Fiery", "Infernal"};
    private static final String[] SPLIT_NAMES = {"Supplies", "Build", "Eaten", "Stun", "DPS", "Skip", "Boss"};

    private static final KuudraPhaseTracker.Phase[] PHASE_BY_NUMBER = {
            KuudraPhaseTracker.Phase.NONE,      // 0
            KuudraPhaseTracker.Phase.SUPPLIES,  // 1
            KuudraPhaseTracker.Phase.BUILD,     // 2
            KuudraPhaseTracker.Phase.EATEN,     // 3
            KuudraPhaseTracker.Phase.STUN,      // 4
            KuudraPhaseTracker.Phase.DPS,       // 5
            KuudraPhaseTracker.Phase.SKIP,      // 6
            KuudraPhaseTracker.Phase.BOSS,      // 7
            KuudraPhaseTracker.Phase.END,       // 8
    };
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private PhantomCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(
                        ClientCommands.literal("phantom")
                                .then(ClientCommands.literal("phase")
                                        .then(ClientCommands.argument("number",
                                                        IntegerArgumentType.integer(0, 8))
                                                .executes(ctx -> {
                                                    int n = IntegerArgumentType.getInteger(ctx, "number");
                                                    forcePhase(PHASE_BY_NUMBER[n]);
                                                    return 1;
                                                })
                                        )
                                        .then(ClientCommands.literal("reset")
                                                .executes(ctx -> {
                                                    forcePhase(KuudraPhaseTracker.Phase.NONE);
                                                    return 1;
                                                })
                                        )
                                )
                                .then(ClientCommands.literal("pb")
                                        .executes(ctx -> { showAllPbs(); return 1; })
                                )
                                .then(ClientCommands.literal("setpb")
                                        .then(ClientCommands.argument("tier",
                                                        IntegerArgumentType.integer(1, 5))
                                                .then(ClientCommands.argument("data",
                                                                StringArgumentType.greedyString())
                                                        .executes(ctx -> {
                                                            int tier = IntegerArgumentType.getInteger(ctx, "tier");
                                                            String data = StringArgumentType.getString(ctx, "data");
                                                            parsePbCommand(tier, data);
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                )
        );
    }

    private static void showAllPbs() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.sendSystemMessage(Component.literal(PREFIX + "§eKuudra PBs §7(hover for details)"));

        for (int tier = 5; tier >= 1; tier--) {
            KuudraConfig.PbRecord record = KuudraConfig.getPbRecord(tier);

            String tierLabel = "§b T" + tier + " §7(" + TIER_NAMES[tier] + ")§r: ";
            String timeLabel = (record != null && record.totalTime < 9999)
                    ? "§a" + KuudraConfig.formatTime(record.totalTime)
                    : "§8No PB";

            MutableComponent line = Component.literal(PREFIX + tierLabel + timeLabel);

            if (record != null && record.totalTime < 9999) {
                final KuudraConfig.PbRecord r = record;
                final int t = tier;
                line = line.withStyle(style ->
                        style.withHoverEvent(new HoverEvent.ShowText(buildTooltip(t, r))));
            }

            mc.player.sendSystemMessage(line);
        }
    }

    private static Component buildTooltip(int tier, KuudraConfig.PbRecord record) {
        MutableComponent t = Component.literal("§e" + TIER_NAMES[tier] + " Tier PB:\n");
        t.append(Component.literal("§fTime: §a" + KuudraConfig.formatTime(record.totalTime) + "\n"));

        if (record.dateMs > 0) {
            LocalDateTime date = LocalDateTime.ofEpochSecond(
                    record.dateMs / 1000, 0, ZoneOffset.UTC);
            t.append(Component.literal("§fDate: §7" + date.format(DATE_FMT) + "\n"));
        }

        if (record.splits != null && record.splits.length == 7) {
            t.append(Component.literal("\n§ePhases:\n"));
            for (int i = 0; i < 7; i++) {
                String val = record.splits[i] < 9999
                        ? KuudraConfig.formatTime(record.splits[i]) : "—";
                t.append(Component.literal("§7 - " + SPLIT_NAMES[i] + ": §f" + val + "\n"));
            }
        }

        if (record.supplies != null && !record.supplies.isEmpty()) {
            t.append(Component.literal("\n§eSupplies:\n"));
            List<KuudraConfig.PlayerTime> sorted = new ArrayList<>(record.supplies);
            sorted.sort((a, b) -> Double.compare(a.time, b.time));
            for (int i = 0; i < sorted.size(); i++) {
                KuudraConfig.PlayerTime pt = sorted.get(i);
                t.append(Component.literal("§7 #" + (i + 1) + " §f" + pt.player
                        + " §7" + KuudraConfig.formatTime(pt.time) + "\n"));
            }
        }

        if (record.freshes != null && !record.freshes.isEmpty()) {
            t.append(Component.literal("\n§eFresh:\n"));
            List<KuudraConfig.PlayerTime> sorted = new ArrayList<>(record.freshes);
            sorted.sort((a, b) -> Double.compare(a.time, b.time));
            for (KuudraConfig.PlayerTime pt : sorted) {
                t.append(Component.literal("§7 - §f" + pt.player
                        + " §7" + KuudraConfig.formatTime(pt.time) + "\n"));
            }
        }

        return t;
    }

    private static void forcePhase(KuudraPhaseTracker.Phase phase) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        KuudraPhaseTracker.forcePhase(phase);
        mc.player.sendSystemMessage(Component.literal(
                PREFIX + "§e[DEV] Phase forced to §b" + phase.name()));
    }

    private static void parsePbCommand(int tier, String raw) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        try {
            String[] parts = raw.trim().split("\\s+");
            int i = 0;

            double total = Double.parseDouble(parts[i++]);

            double[] splits = new double[7];
            for (int s = 0; s < 7; s++) splits[s] = Double.parseDouble(parts[i++]);

            long dateMs = 0;
            int dateStart = parts.length - 6;
            if (dateStart >= i) {
                try {
                    int year  = Integer.parseInt(parts[dateStart]);
                    int month = Integer.parseInt(parts[dateStart + 1]);
                    int day   = Integer.parseInt(parts[dateStart + 2]);
                    int hour  = Integer.parseInt(parts[dateStart + 3]);
                    int min   = Integer.parseInt(parts[dateStart + 4]);
                    int sec   = Integer.parseInt(parts[dateStart + 5]);
                    dateMs = LocalDateTime.of(year, month, day, hour, min, sec)
                            .toEpochSecond(ZoneOffset.UTC) * 1000L;
                } catch (Exception ex) {
                    dateStart = parts.length;
                }
            } else {
                dateStart = parts.length;
            }

            List<KuudraConfig.PlayerTime> supplies = new ArrayList<>();
            List<KuudraConfig.PlayerTime> freshes  = new ArrayList<>();
            int supplyCount = 0;
            while (i < dateStart - 1) {
                String player = parts[i++];
                double time   = Double.parseDouble(parts[i++]);
                if (supplyCount < 6) { supplies.add(new KuudraConfig.PlayerTime(player, time)); supplyCount++; }
                else                 { freshes.add(new KuudraConfig.PlayerTime(player, time)); }
            }

            KuudraConfig.PbRecord record = new KuudraConfig.PbRecord();
            record.totalTime = total;
            record.splits    = splits;
            record.dateMs    = dateMs > 0 ? dateMs : System.currentTimeMillis();
            record.supplies  = supplies;
            record.freshes   = freshes;

            KuudraConfig.setPbRecord(tier, record);

            mc.player.sendSystemMessage(Component.literal(
                    PREFIX + "§aT" + tier + " PB set: "
                            + KuudraConfig.formatTime(total)));

        } catch (Exception e) {
            mc.player.sendSystemMessage(Component.literal(
                    PREFIX + "§cError: " + e.getMessage() + "\n"
                            + "§7Usage: /phantom setpb <tier> <total> <s1>..<s7> "
                            + "[player time]... <yyyy MM dd HH mm ss>"));
        }
    }
}