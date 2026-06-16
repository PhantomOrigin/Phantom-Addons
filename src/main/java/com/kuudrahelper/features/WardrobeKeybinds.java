package com.kuudrahelper.features;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.KuudraScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WardrobeKeybinds {

    private static volatile int  closeInTicks    = -1;
    private static volatile long nextAllowedAtMs = 0;

    private static final long    ACTION_COOLDOWN_MS = 200;
    private static final int     CLOSE_DELAY_TICKS  = 1;

    private static final Pattern WARDROBE_PATTERN = Pattern.compile("Wardrobe \\((\\d+)/(\\d+)\\)");
    private static final Pattern EQUIPPED_PATTERN = Pattern.compile("Slot \\d+: Equipped");

    private static final boolean[] prevDown = new boolean[3];

    private WardrobeKeybinds() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Scheduled close
            if (closeInTicks == 1) {
                closeInTicks = 0;
                if (client.screen instanceof AbstractContainerScreen && client.player != null) {
                    client.player.closeContainer();
                }
            } else if (closeInTicks > 1) {
                closeInTicks--;
            }

            // Open-command keybinds — only active when no screen is open
            if (!KuudraConfig.isWardrobeEnabled() || client.screen != null) {
                prevDown[0] = prevDown[1] = prevDown[2] = false;
                return;
            }

            long handle = GLFW.glfwGetCurrentContext();
            checkOpenKey(client, handle, 0, KuudraConfig.getWardrobeOpenKey(),  "wardrobe");
            checkOpenKey(client, handle, 1, KuudraConfig.getEquipmentOpenKey(), "equipment");
            checkOpenKey(client, handle, 2, KuudraConfig.getPetsOpenKey(),      "pets");
        });
    }

    private static void checkOpenKey(Minecraft mc, long handle, int idx, int keyCode, String command) {
        if (keyCode <= 0) { prevDown[idx] = false; return; }
        boolean down;
        if (keyCode >= KuudraScreen.MOUSE_OFFSET) {
            int btn = keyCode - KuudraScreen.MOUSE_OFFSET;
            down = handle != 0 && GLFW.glfwGetMouseButton(handle, btn) == GLFW.GLFW_PRESS;
        } else {
            down = handle != 0 && GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;
        }
        if (down && !prevDown[idx] && mc.player != null) {
            mc.player.connection.sendCommand(command);
        }
        prevDown[idx] = down;
    }

    public static boolean handleMouseButton(AbstractContainerScreen<?> screen, int button, Minecraft mc) {
        return handleKey(screen, KuudraScreen.MOUSE_OFFSET + button, mc);
    }

    public static boolean handleKey(AbstractContainerScreen<?> screen, int keyCode, Minecraft mc) {
        if (!KuudraConfig.isWardrobeEnabled()) return false;

        if (System.currentTimeMillis() < nextAllowedAtMs) return false;

        String title = strip(screen.getTitle().getString());
        Matcher wm = WARDROBE_PATTERN.matcher(title);
        if (!wm.find()) return false;

        int currentPage = Integer.parseInt(wm.group(1));
        int totalPages  = Integer.parseInt(wm.group(2));

        AbstractContainerMenu handler = screen.getMenu();
        int equippedSlot = findEquippedSlot(handler);

        int[] slotKeys = KuudraConfig.getWardrobeSlotKeys();
        for (int i = 0; i < 9; i++) {
            if (slotKeys[i] > 0 && keyCode == slotKeys[i]) {
                int containerSlot = 36 + i;
                if (containerSlot >= handler.slots.size()) return true;
                if (containerSlot == equippedSlot) return true;
                doClick(mc, handler, containerSlot, true);
                return true;
            }
        }

        int nextKey    = KuudraConfig.getWardrobeNextPageKey();
        int prevKey    = KuudraConfig.getWardrobePrevPageKey();
        int unequipKey = KuudraConfig.getWardrobeUnequipKey();

        if (nextKey > 0 && keyCode == nextKey) {
            if (currentPage >= totalPages) return true;
            clickByName(mc, handler, "next page");
            return true;
        }
        if (prevKey > 0 && keyCode == prevKey) {
            if (currentPage <= 1) return true;
            clickByName(mc, handler, "previous page");
            return true;
        }
        if (unequipKey > 0 && keyCode == unequipKey) {
            if (equippedSlot == -1) return true;
            doClick(mc, handler, equippedSlot, false);
            return true;
        }

        return false;
    }

    private static void doClick(Minecraft mc, AbstractContainerMenu handler, int slot, boolean scheduleClose) {
        if (mc.gameMode == null || mc.player == null) return;
        nextAllowedAtMs = System.currentTimeMillis() + ACTION_COOLDOWN_MS;
        mc.gameMode.handleContainerInput(handler.containerId, slot, 0, ContainerInput.PICKUP, mc.player);
        if (scheduleClose) closeInTicks = CLOSE_DELAY_TICKS;
    }

    private static void clickByName(Minecraft mc, AbstractContainerMenu handler, String fragment) {
        for (int i = 0; i < handler.slots.size(); i++) {
            ItemStack stack = handler.slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            if (strip(stack.getHoverName().getString()).toLowerCase().contains(fragment)) {
                doClick(mc, handler, i, false);
                return;
            }
        }
    }

    private static int findEquippedSlot(AbstractContainerMenu handler) {
        for (int i = 0; i < handler.slots.size(); i++) {
            ItemStack stack = handler.slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) continue;
            boolean equipped = lore.lines().stream()
                    .anyMatch(line -> EQUIPPED_PATTERN.matcher(strip(line.getString())).find());
            if (equipped) return i;
        }
        return -1;
    }

    public static void reset() {
        closeInTicks    = -1;
        nextAllowedAtMs = 0;
        prevDown[0] = prevDown[1] = prevDown[2] = false;
    }

    private static String strip(String s) {
        return s.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}
