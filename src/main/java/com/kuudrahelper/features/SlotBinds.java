package com.kuudrahelper.features;

import com.kuudrahelper.KuudraConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import java.util.Map;

public final class SlotBinds {

    private static final int INV_MIN = 9;
    private static final int INV_MAX = 35;
    private static final int HOT_MIN = 36;
    private static final int HOT_MAX = 44;

    private static Integer pendingSlot = null;

    private SlotBinds() {}

    private static int getMenuSlotId(AbstractContainerScreen<?> screen, Slot slot) {
        return slot == null ? -1 : screen.getMenu().slots.indexOf(slot);
    }

    public static boolean handleKeyPress(AbstractContainerScreen<?> screen, int keyCode, Slot hovered) {
        if (!KuudraConfig.isSlotBindsEnabled()) return false;

        int bindKey = KuudraConfig.getSlotBindSetKey();
        if (bindKey <= 0 || keyCode != bindKey) return false;

        if (hovered == null) {
            if (pendingSlot != null) {
                pendingSlot = null;
                sendMsg("\u00a77Slot bind cancelled.");
            }
            return true;
        }

        int slotId = getMenuSlotId(screen, hovered);
        if (!isValidSlot(slotId)) {
            sendMsg("\u00a7cThat slot can't be used for slot binds (armor/crafting slots excluded).");
            pendingSlot = null;
            return true;
        }

        if (pendingSlot == null) {
            pendingSlot = slotId;
            String type = isHotbarSlot(slotId)
                    ? "hotbar slot " + (slotId - HOT_MIN + 1)
                    : "inventory slot " + slotId;
            sendMsg("\u00a7eSelected " + type + ". Now hover the second slot and press the bind key.");
        } else {
            int first = pendingSlot;
            pendingSlot = null;

            if (first == slotId) {
                sendMsg("\u00a7cYou can't bind a slot to itself.");
                return true;
            }

            int invSlot, hotbarSlot;
            if (isInventorySlot(first) && isHotbarSlot(slotId)) {
                invSlot = first;
                hotbarSlot = slotId;
            } else if (isHotbarSlot(first) && isInventorySlot(slotId)) {
                hotbarSlot = first;
                invSlot = slotId;
            } else {
                sendMsg("\u00a7cOne slot must be in the main inventory and one must be in the hotbar (slots 1-9).");
                return true;
            }

            int hotbarIndex = hotbarSlot - HOT_MIN;
            Map<Integer, Integer> bindings = KuudraConfig.getSlotBindings();
            Integer existingHotbarIndex = bindings.get(invSlot);

            if (Integer.valueOf(hotbarIndex).equals(existingHotbarIndex)) {
                KuudraConfig.clearSlotBinding(invSlot);
                sendMsg("\u00a77Removed binding for inventory slot " + invSlot + ".");
            } else {
                KuudraConfig.putSlotBinding(invSlot, hotbarIndex);
                sendMsg("\u00a7aBound inventory slot " + invSlot + " -> hotbar " + (hotbarIndex + 1) + ".");
            }
        }

        return true;
    }

    public static boolean handleShiftClick(AbstractContainerScreen<?> screen, Slot slot) {
        if (!KuudraConfig.isSlotBindsEnabled()) return false;
        if (slot == null) return false;

        int slotId = getMenuSlotId(screen, slot);
        if (!isInventorySlot(slotId)) return false;

        Integer hotbarIndex = KuudraConfig.getSlotBindings().get(slotId);
        if (hotbarIndex == null) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || mc.player == null) return false;

        mc.gameMode.handleContainerInput(
                screen.getMenu().containerId,
                slotId,
                hotbarIndex,
                ContainerInput.SWAP,
                mc.player
        );

        return true;
    }

    public static void clearPending() {
        pendingSlot = null;
    }

    private static boolean isInventorySlot(int id) {
        return id >= INV_MIN && id <= INV_MAX;
    }

    private static boolean isHotbarSlot(int id) {
        return id >= HOT_MIN && id <= HOT_MAX;
    }

    private static boolean isValidSlot(int id) {
        return isInventorySlot(id) || isHotbarSlot(id);
    }

    private static void sendMsg(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(msg));
        }
    }
}