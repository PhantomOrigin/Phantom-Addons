package com.phantomaddons.features.boss.rend;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.phase.KuudraPhaseTracker;
import com.phantomaddons.utils.KuudraTierDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public final class RendTracker {

    private static long killPhaseStartMs      = -1;

    private static long backboneHitMs          = -1;
    private static ItemStack helmetAtBackbone  = null;
    private static ItemStack heldAtBackbone    = null;
    private static long manaDrainMs            = -1;

    private static long      pendingPullMs   = -1;
    private static ItemStack pendingPullItem = null;
    private static final long PULL_BUFFER_MS = 400;

    private RendTracker() {}

    static {
        BonemerangHitTracker.addOnBackHitListener(RendTracker::onRealBackboneHit);
    }

    public static void onKillPhaseStart() {
        if (killPhaseStartMs > 0) return;
        killPhaseStartMs = System.currentTimeMillis();
        clearCycle();
    }

    public static void onBossPhaseStart() {
        killPhaseStartMs = System.currentTimeMillis();
        clearCycle();
    }

    public static void reset() {
        killPhaseStartMs = -1;
        clearCycle();
    }

    private static void clearCycle() {
        backboneHitMs          = -1;
        helmetAtBackbone       = null;
        heldAtBackbone         = null;
        manaDrainMs            = -1;
        pendingPullMs          = -1;
        pendingPullItem        = null;
    }

    public static void onBonemerangThrow() {
        if (!PhantomConfig.isRendTrackerEnabled()) return;
        if (!isKillPhase()) return;
        if (killPhaseStartMs < 0) return;
        clearCycle();
    }

    private static void onRealBackboneHit() {
        if (!PhantomConfig.isRendTrackerEnabled() || !isKillPhase() || backboneHitMs >= 0) return;

        backboneHitMs = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            helmetAtBackbone = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).copy();
            heldAtBackbone   = mc.player.getMainHandItem().copy();
        }
        if (pendingPullMs >= 0) {
            printOutput(pendingPullItem, pendingPullMs);
            clearCycle();
        }
    }

    public static void onManaDrain() {
        if (!com.phantomaddons.features.misckuudra.profile.RemoteFeatureGate.isManaDrainTrackingEnabled()) return;
        if (!isKillPhase()) return;
        if (backboneHitMs < 0) return;
        if (manaDrainMs < 0) manaDrainMs = System.currentTimeMillis();
    }

    public static void onLeftClick(ItemStack item) {
        if (!PhantomConfig.isRendTrackerEnabled()) return;
        if (!isKillPhase()) return;
        if (!hasRendInItem(item)) return;
        long pullMs = System.currentTimeMillis();
        if (backboneHitMs >= 0) {
            printOutput(item.copy(), pullMs);
            clearCycle();
        } else {
            pendingPullMs   = pullMs;
            pendingPullItem = item.copy();
        }
    }

    private static boolean hasRendInItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getHoverName().getString().toLowerCase().contains("rend")) return true;
        net.minecraft.world.item.component.ItemLore lore =
                stack.get(net.minecraft.core.component.DataComponents.LORE);
        if (lore == null) return false;
        for (net.minecraft.network.chat.Component line : lore.lines()) {
            if (line.getString().toLowerCase().contains("rend")) return true;
        }
        return false;
    }

    public static void tick() {
        if (!isKillPhase()) return;

        if (pendingPullMs >= 0 && System.currentTimeMillis() - pendingPullMs > PULL_BUFFER_MS) {
            pendingPullMs   = -1;
            pendingPullItem = null;
        }
    }

    private static boolean isKillPhase() {
        if (KuudraTierDetector.getTier() != 5) return false;
        KuudraPhaseTracker.Phase p = KuudraPhaseTracker.getPhase();
        return p == KuudraPhaseTracker.Phase.STUN
            || p == KuudraPhaseTracker.Phase.DPS
            || p == KuudraPhaseTracker.Phase.BOSS;
    }

    private static void printOutput(ItemStack pullItem, long pullMs) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || killPhaseStartMs < 0 || backboneHitMs < 0) return;

        long pingAdjustMs = PhantomConfig.getLowPing() / 2L;
        double backboneTime = Math.max(0L, backboneHitMs - killPhaseStartMs - pingAdjustMs) / 1000.0;
        double pullTime     = Math.max(0L, pullMs         - killPhaseStartMs - pingAdjustMs) / 1000.0;

        mc.player.sendSystemMessage(Component.literal("§8-------------------------------------"));
        mc.player.sendSystemMessage(Component.literal(String.format("§fBackbone Hit at §a%.2fs", backboneTime)));
        mc.player.sendSystemMessage(buildItemLine("§fWearing: ", helmetAtBackbone));
        mc.player.sendSystemMessage(buildItemLine("§fHolding: ", heldAtBackbone));

        if (com.phantomaddons.features.misckuudra.profile.RemoteFeatureGate.isManaDrainTrackingEnabled()) {
            if (manaDrainMs > 0) {
                double drainTime = Math.max(0L, manaDrainMs - killPhaseStartMs - pingAdjustMs) / 1000.0;
                mc.player.sendSystemMessage(Component.literal(String.format("§fMana Drain at §b%.2fs", drainTime)));
            } else {
                mc.player.sendSystemMessage(Component.literal("§cNo Mana Drain!"));
            }
        }

        MutableComponent pullLine = Component.literal(String.format("§fPulled At §a%.2fs §fwith ", pullTime));
        pullLine.append(pullItem != null && !pullItem.isEmpty()
                ? buildHoverItem(pullItem)
                : Component.literal("§7Unknown"));
        mc.player.sendSystemMessage(pullLine);

        mc.player.sendSystemMessage(Component.literal("§8-------------------------------------"));
    }

    private static MutableComponent buildItemLine(String prefix, ItemStack stack) {
        MutableComponent line = Component.literal(prefix);
        line.append(stack != null && !stack.isEmpty()
                ? buildHoverItem(stack)
                : Component.literal("§7None"));
        return line;
    }

    private static MutableComponent buildHoverItem(ItemStack stack) {
        String coloredName = toLegacyString(stack.getHoverName());
        return Component.literal(coloredName).withStyle(
                style -> style.withHoverEvent(
                        new HoverEvent.ShowItem(ItemStackTemplate.fromNonEmptyStack(stack))));
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
                    net.minecraft.network.chat.TextColor legacy = net.minecraft.network.chat.TextColor.fromLegacyFormat(cf);
                    if (legacy != null && legacy.getValue() == rgb) {
                        sb.append(cf.toString());
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
}
