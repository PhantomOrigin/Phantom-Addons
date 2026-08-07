package com.phantomaddons.features.loadouts;

import com.phantomaddons.utils.TextUtil;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.PhantomScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
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

    private static volatile int  closeInTicks       = -1;
    private static volatile int  pendingCloseKeyCode = -1;
    private static volatile int  pendingCloseTimeoutTicks = -1;
    private static volatile long nextAllowedAtMs    = 0;
    private static volatile long suppressReopenUntilMs = 0;

    private static final long    ACTION_COOLDOWN_MS = 200;
    private static final int     CLOSE_DELAY_TICKS  = 1;

    private static final int     CLOSE_TIMEOUT_TICKS = 20;
    private static final long    SUPPRESS_REOPEN_MS  = 350;

    private static final Pattern WARDROBE_TITLE_PATTERN =
            Pattern.compile("wardrobe|armor sets|equipment sets", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOADOUTS_TITLE_PATTERN =
            Pattern.compile("loadouts", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAGE_PATTERN = Pattern.compile("\\((\\d+)/(\\d+)\\)");
    private static final Pattern EQUIPPED_PATTERN =
            Pattern.compile("slot \\d+: equipped", Pattern.CASE_INSENSITIVE);
    private static final String  UNEQUIP_HINT      = "click to unequip!";
    private static final String  UNEQUIP_HINT_OLD  = "click to unequip this armor set";

    private static final int[] LOADOUT_SLOTS = {14,15,16, 23,24,25, 32,33,34, 41,42,43};

    private static final boolean[] prevDown = new boolean[5];

    private WardrobeKeybinds() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (closeInTicks == 1) {
                closeInTicks = 0;
                closeContainerNow(client);
            } else if (closeInTicks > 1) {
                closeInTicks--;
            }

            if (pendingCloseKeyCode >= 0) {
                boolean stillDown = isKeyDown(GLFW.glfwGetCurrentContext(), pendingCloseKeyCode);
                pendingCloseTimeoutTicks--;
                if (!stillDown || pendingCloseTimeoutTicks <= 0) {
                    pendingCloseKeyCode = -1;
                    pendingCloseTimeoutTicks = -1;
                    closeInTicks = CLOSE_DELAY_TICKS;
                }
            }

            boolean wardrobeEligible = PhantomConfig.isWardrobeEnabled() && client.gui.screen() == null;
            boolean loadoutsEligible = PhantomConfig.isLoadoutsEnabled() && client.gui.screen() == null;

            long handle = GLFW.glfwGetCurrentContext();
            checkOpenKey(client, handle, 0, PhantomConfig.getWardrobeOpenKey(),    "wardrobe", wardrobeEligible);
            checkOpenKey(client, handle, 1, PhantomConfig.getStatsOpenKey(),       "stats", wardrobeEligible);
            checkOpenKey(client, handle, 2, PhantomConfig.getPetsOpenKey(),        "pets", wardrobeEligible);
            checkOpenKey(client, handle, 3, PhantomConfig.getEqWardrobeOpenKey(),  "eq", wardrobeEligible);
            checkOpenKey(client, handle, 4, PhantomConfig.getLoadoutsOpenKey(),    "loadouts", loadoutsEligible);
        });

        ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> {
            if (System.currentTimeMillis() >= suppressReopenUntilMs) return;
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;
            String title = strip(containerScreen.getTitle().getString());
            if (WARDROBE_TITLE_PATTERN.matcher(title).find()
                    || LOADOUTS_TITLE_PATTERN.matcher(title).find()) {
                client.execute(() -> {
                    if (client.player != null) client.player.closeContainer();
                });
            }
        });
    }

    private static void checkOpenKey(Minecraft mc, long handle, int idx, int keyCode, String command, boolean eligible) {
        if (keyCode <= 0) { prevDown[idx] = false; return; }
        boolean down = isKeyDown(handle, keyCode);
        if (eligible && down && !prevDown[idx] && mc.player != null) {
            mc.player.connection.sendCommand(command);
        }
        prevDown[idx] = down;
    }

    private static boolean isKeyDown(long handle, int keyCode) {
        if (handle == 0) return false;
        if (keyCode >= PhantomScreen.MOUSE_OFFSET) {
            return GLFW.glfwGetMouseButton(handle, keyCode - PhantomScreen.MOUSE_OFFSET) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;
    }

    private static void closeContainerNow(Minecraft client) {
        if (client.gui.screen() instanceof AbstractContainerScreen && client.player != null) {
            client.player.closeContainer();
        }
    }

    private static void scheduleCloseAfterRelease(int keyCode) {
        if (!com.phantomaddons.Edition.CURRENT.fullFeatureSet) return; // not included in this edition
        if (!PhantomConfig.isWardrobeAutoCloseEnabled()) return;
        suppressReopenUntilMs = System.currentTimeMillis() + SUPPRESS_REOPEN_MS + PhantomConfig.getWardrobeExtraAutoCloseMs();
        if (keyCode < 0) { closeInTicks = CLOSE_DELAY_TICKS; return; }
        pendingCloseKeyCode = keyCode;
        pendingCloseTimeoutTicks = CLOSE_TIMEOUT_TICKS;
    }

    public static boolean handleMouseButton(AbstractContainerScreen<?> screen, int button, Minecraft mc) {
        return handleKey(screen, PhantomScreen.MOUSE_OFFSET + button, mc);
    }

    public static boolean handleKey(AbstractContainerScreen<?> screen, int keyCode, Minecraft mc) {
        if (System.currentTimeMillis() < nextAllowedAtMs) return false;

        String title = strip(screen.getTitle().getString());

        if (WARDROBE_TITLE_PATTERN.matcher(title).find()) {
            if (!PhantomConfig.isWardrobeEnabled()) return false;
            return handleWardrobeKey(screen, title, keyCode, mc);
        }
        if (LOADOUTS_TITLE_PATTERN.matcher(title).find()) {
            if (!PhantomConfig.isLoadoutsEnabled()) return false;
            return handleLoadoutsKey(screen, title, keyCode, mc);
        }
        return false;
    }

    private static boolean handleWardrobeKey(AbstractContainerScreen<?> screen, String title, int keyCode, Minecraft mc) {
        Matcher pm = PAGE_PATTERN.matcher(title);
        if (!pm.find()) return false;

        int currentPage = Integer.parseInt(pm.group(1));
        int totalPages  = Integer.parseInt(pm.group(2));

        AbstractContainerMenu handler = screen.getMenu();
        int equippedSlot = findEquippedSlot(handler);

        int[] slotKeys = PhantomConfig.getWardrobeSlotKeys();
        for (int i = 0; i < 9; i++) {
            if (slotKeys[i] > 0 && keyCode == slotKeys[i]) {
                int containerSlot = 36 + i;
                if (containerSlot >= handler.slots.size()) return true;
                if (containerSlot == equippedSlot && PhantomConfig.isWardrobeDisableUnequipEnabled()) {
                    scheduleCloseAfterRelease(keyCode);
                    return true;
                }
                doClick(mc, handler, containerSlot, true, keyCode);
                PhantomConfig.playNotificationSound(PhantomConfig.SOUND_WARDROBE_SWAP);
                return true;
            }
        }

        int nextKey    = PhantomConfig.getWardrobeNextPageKey();
        int prevKey    = PhantomConfig.getWardrobePrevPageKey();
        int unequipKey = PhantomConfig.getWardrobeUnequipKey();

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
            doClick(mc, handler, equippedSlot, false, keyCode);
            return true;
        }

        return false;
    }

    private static boolean handleLoadoutsKey(AbstractContainerScreen<?> screen, String title, int keyCode, Minecraft mc) {
        Matcher pm = PAGE_PATTERN.matcher(title);
        if (!pm.find()) return false;

        int currentPage = Integer.parseInt(pm.group(1));
        int totalPages  = Integer.parseInt(pm.group(2));

        AbstractContainerMenu handler = screen.getMenu();

        int[] slotKeys = PhantomConfig.getLoadoutSlotKeys();
        for (int i = 0; i < LOADOUT_SLOTS.length; i++) {
            if (slotKeys[i] > 0 && keyCode == slotKeys[i]) {
                int containerSlot = LOADOUT_SLOTS[i];
                if (containerSlot >= handler.slots.size()) return true;
                doClick(mc, handler, containerSlot, true, keyCode);
                PhantomConfig.playNotificationSound(PhantomConfig.SOUND_WARDROBE_SWAP);
                return true;
            }
        }

        int nextKey = PhantomConfig.getWardrobeNextPageKey();
        int prevKey = PhantomConfig.getWardrobePrevPageKey();

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

        return false;
    }

    private static void doClick(Minecraft mc, AbstractContainerMenu handler, int slot, boolean scheduleClose) {
        doClick(mc, handler, slot, scheduleClose, -1);
    }

    private static void doClick(Minecraft mc, AbstractContainerMenu handler, int slot, boolean scheduleClose, int triggeringKeyCode) {
        if (mc.gameMode == null || mc.player == null) return;
        nextAllowedAtMs = System.currentTimeMillis() + ACTION_COOLDOWN_MS;
        mc.gameMode.handleContainerInput(handler.containerId, slot, 0, ContainerInput.PICKUP, mc.player);
        if (scheduleClose) scheduleCloseAfterRelease(triggeringKeyCode);
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
            for (var line : lore.lines()) {
                String stripped = strip(line.getString()).toLowerCase();
                if (EQUIPPED_PATTERN.matcher(stripped).find()
                        || stripped.contains(UNEQUIP_HINT)
                        || stripped.contains(UNEQUIP_HINT_OLD)) {
                    PhantomConfig.setLastEquippedWardrobeSlot(i);
                    return i;
                }
            }
        }
        return PhantomConfig.getLastEquippedWardrobeSlot();
    }

    public static void reset() {
        closeInTicks             = -1;
        pendingCloseKeyCode      = -1;
        pendingCloseTimeoutTicks = -1;
        nextAllowedAtMs          = 0;
        for (int i = 0; i < prevDown.length; i++) prevDown[i] = false;
    }

    private static String strip(String s) {
        return TextUtil.stripColor(s);
    }
}
