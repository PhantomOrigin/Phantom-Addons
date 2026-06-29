package com.kuudrahelper.features.kuudra;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public final class RendTracker {

    private static final int BACKBONE_BASE_TICKS = 22;

    private static long killPhaseStartMs      = -1;

    private static int  backboneTicksRemaining = -1;
    private static long backboneHitMs          = -1;
    private static ItemStack helmetAtBackbone  = null;
    private static ItemStack heldAtBackbone    = null;
    private static long manaDrainMs            = -1;

    // Buffered pull — stored when the player clicks before backbone hit registers
    private static long      pendingPullMs   = -1;
    private static ItemStack pendingPullItem = null;
    private static final long PULL_BUFFER_MS = 400;

    private RendTracker() {}

    public static void onKillPhaseStart() {
        if (killPhaseStartMs > 0) return;
        killPhaseStartMs = System.currentTimeMillis();
        clearCycle();
    }

    /** Called specifically when BOSS phase starts; always resets so timing is from BOSS, not STUN. */
    public static void onBossPhaseStart() {
        killPhaseStartMs = System.currentTimeMillis();
        clearCycle();
    }

    public static void reset() {
        killPhaseStartMs = -1;
        clearCycle();
    }

    private static void clearCycle() {
        backboneTicksRemaining = -1;
        backboneHitMs          = -1;
        helmetAtBackbone       = null;
        heldAtBackbone         = null;
        manaDrainMs            = -1;
        pendingPullMs          = -1;
        pendingPullItem        = null;
    }

    public static void onBonemerangThrow() {
        if (!KuudraConfig.isRendTrackerEnabled()) return;
        if (!isKillPhase()) return;
        if (killPhaseStartMs < 0) return;
        clearCycle();
        int pingTicks = (int) Math.round(KuudraConfig.getLowPing() / 50.0);
        backboneTicksRemaining = Math.max(1, BACKBONE_BASE_TICKS + pingTicks);
    }

    public static void onManaDrain() {
        if (!isKillPhase()) return;
        if (backboneHitMs < 0) return;
        if (manaDrainMs < 0) manaDrainMs = System.currentTimeMillis();
    }

    public static void onLeftClick(ItemStack item) {
        if (!KuudraConfig.isRendTrackerEnabled()) return;
        if (!isKillPhase()) return;
        if (!hasRendInItem(item)) return;
        long pullMs = System.currentTimeMillis();
        if (backboneHitMs >= 0) {
            printOutput(item.copy(), pullMs);
            clearCycle();
        } else {
            // Backbone hit not yet registered — buffer the pull and resolve in tick()
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

        // Expire a buffered pull that never got a backbone hit
        if (pendingPullMs >= 0 && System.currentTimeMillis() - pendingPullMs > PULL_BUFFER_MS) {
            pendingPullMs   = -1;
            pendingPullItem = null;
        }

        if (backboneTicksRemaining <= 0) return;
        backboneTicksRemaining--;
        if (backboneTicksRemaining == 0) {
            backboneHitMs = System.currentTimeMillis();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                helmetAtBackbone = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).copy();
                heldAtBackbone   = mc.player.getMainHandItem().copy();
            }
            // If the player already clicked the pull item before backbone hit was registered, fire now
            if (pendingPullMs >= 0) {
                printOutput(pendingPullItem, pendingPullMs);
                clearCycle();
            }
        }
    }

    private static boolean isKillPhase() {
        KuudraPhaseTracker.Phase p = KuudraPhaseTracker.getPhase();
        return p == KuudraPhaseTracker.Phase.STUN
            || p == KuudraPhaseTracker.Phase.DPS
            || p == KuudraPhaseTracker.Phase.BOSS;
    }

    private static void printOutput(ItemStack pullItem, long pullMs) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || killPhaseStartMs < 0 || backboneHitMs < 0) return;

        double backboneTime = (backboneHitMs - killPhaseStartMs) / 1000.0;
        double pullTime     = (pullMs         - killPhaseStartMs) / 1000.0;

        mc.player.sendSystemMessage(Component.literal("§8-------------------------------------"));
        mc.player.sendSystemMessage(Component.literal(String.format("§fBackbone Hit at §a%.2fs", backboneTime)));
        mc.player.sendSystemMessage(buildItemLine("§fWearing: ", helmetAtBackbone));
        mc.player.sendSystemMessage(buildItemLine("§fHolding: ", heldAtBackbone));

        if (manaDrainMs > 0) {
            double drainTime = (manaDrainMs - killPhaseStartMs) / 1000.0;
            mc.player.sendSystemMessage(Component.literal(String.format("§fMana Drain at §b%.2fs", drainTime)));
        } else {
            mc.player.sendSystemMessage(Component.literal("§cNo Mana Drain!"));
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
}
