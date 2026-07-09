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

    private static volatile int  closeInTicks       = -1;
    private static volatile int  pendingCloseKeyCode = -1;
    private static volatile int  pendingCloseTimeoutTicks = -1;
    private static volatile long nextAllowedAtMs    = 0;
    private static volatile long suppressReopenUntilMs = 0;

    private static final long    ACTION_COOLDOWN_MS = 200;
    private static final int     CLOSE_DELAY_TICKS  = 1;

    private static final int     CLOSE_TIMEOUT_TICKS = 20;
    private static final long    SUPPRESS_REOPEN_MS  = 1000;

    // Left-side "currently equipped" preview column in the Loadouts screen: the boots
    // slot (3rd column, 5th row -> container slot 38) is the last one to refresh when the
    // server pushes the post-equip menu update, so once it changes the whole screen is settled.
    private static final int     LOADOUT_BOOTS_PREVIEW_SLOT = 38;
    private static final int     BOOTS_WATCH_TIMEOUT_TICKS  = 40;

    private static volatile boolean  watchingBootsChange = false;
    private static volatile ItemStack watchedBootsStack   = ItemStack.EMPTY;
    private static volatile int      bootsWatchTimeoutTicks = -1;
    private static volatile int      closeAfterBootsChangeTicks = -1;

    private static final Pattern WARDROBE_TITLE_PATTERN =
            Pattern.compile("wardrobe|armor sets|equipment sets", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOADOUTS_TITLE_PATTERN =
            Pattern.compile("loadouts", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAGE_PATTERN = Pattern.compile("\\((\\d+)/(\\d+)\\)");
    private static final Pattern EQUIPPED_PATTERN =
            Pattern.compile("slot \\d+: equipped", Pattern.CASE_INSENSITIVE);
    private static final String  UNEQUIP_HINT      = "click to unequip!";
    private static final String  UNEQUIP_HINT_OLD  = "click to unequip this armor set";

    // (1/3) Loadouts screen: 3 columns x 4 rows of individual loadout slots
    // (container cols 5-7, rows 1-4 in a 9-wide/6-row menu), row-major 1-12.
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
                    closeContainerNow(client);
                }
            }

            // Hypixel briefly re-opens the wardrobe screen server-side after an equip is
            // actually processed (a "confirmation" push), then closes it itself a moment
            // later. We let it open normally (so the client's real close flow still fires
            // and a proper close packet is sent) and just react to it the tick it appears.
            if (System.currentTimeMillis() < suppressReopenUntilMs
                    && client.screen instanceof AbstractContainerScreen<?> reopened) {
                String reopenedTitle = strip(reopened.getTitle().getString());
                if (WARDROBE_TITLE_PATTERN.matcher(reopenedTitle).find()) {
                    client.player.closeContainer();
                }
            }

            if (watchingBootsChange) {
                tickBootsWatch(client);
            }

            boolean eligible = KuudraConfig.isWardrobeEnabled() && client.screen == null;

            long handle = GLFW.glfwGetCurrentContext();
            checkOpenKey(client, handle, 0, KuudraConfig.getWardrobeOpenKey(),    "wardrobe", eligible);
            checkOpenKey(client, handle, 1, KuudraConfig.getStatsOpenKey(),       "stats", eligible);
            checkOpenKey(client, handle, 2, KuudraConfig.getPetsOpenKey(),        "pets", eligible);
            checkOpenKey(client, handle, 3, KuudraConfig.getEqWardrobeOpenKey(),  "eq", eligible);
            checkOpenKey(client, handle, 4, KuudraConfig.getLoadoutsOpenKey(),    "loadouts", eligible);
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
        if (keyCode >= KuudraScreen.MOUSE_OFFSET) {
            return GLFW.glfwGetMouseButton(handle, keyCode - KuudraScreen.MOUSE_OFFSET) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;
    }

    private static void armBootsWatch(AbstractContainerMenu handler) {
        if (!KuudraConfig.isWardrobeAutoCloseEnabled()) return;
        if (LOADOUT_BOOTS_PREVIEW_SLOT >= handler.slots.size()) return;
        watchedBootsStack   = handler.slots.get(LOADOUT_BOOTS_PREVIEW_SLOT).getItem().copy();
        watchingBootsChange = true;
        bootsWatchTimeoutTicks = BOOTS_WATCH_TIMEOUT_TICKS;
        closeAfterBootsChangeTicks = -1;
    }

    private static void tickBootsWatch(Minecraft client) {
        if (!(client.screen instanceof AbstractContainerScreen<?> screen)) {
            watchingBootsChange = false;
            return;
        }

        if (closeAfterBootsChangeTicks == 1) {
            watchingBootsChange = false;
            closeAfterBootsChangeTicks = -1;
            closeContainerNow(client);
            return;
        } else if (closeAfterBootsChangeTicks > 1) {
            closeAfterBootsChangeTicks--;
            return;
        }

        AbstractContainerMenu menu = screen.getMenu();
        if (LOADOUT_BOOTS_PREVIEW_SLOT >= menu.slots.size()) {
            watchingBootsChange = false;
            closeContainerNow(client);
            return;
        }

        ItemStack current = menu.slots.get(LOADOUT_BOOTS_PREVIEW_SLOT).getItem();
        if (!ItemStack.matches(current, watchedBootsStack)) {
            closeAfterBootsChangeTicks = 1;
            return;
        }

        bootsWatchTimeoutTicks--;
        if (bootsWatchTimeoutTicks <= 0) {
            watchingBootsChange = false;
            closeContainerNow(client);
        }
    }

    private static void closeContainerNow(Minecraft client) {
        if (client.screen instanceof AbstractContainerScreen && client.player != null) {
            client.player.closeContainer();
        }
    }

    private static void scheduleCloseAfterRelease(int keyCode) {
        if (!KuudraConfig.isWardrobeAutoCloseEnabled()) return;
        suppressReopenUntilMs = System.currentTimeMillis() + SUPPRESS_REOPEN_MS;
        if (keyCode < 0) { closeInTicks = CLOSE_DELAY_TICKS; return; }
        pendingCloseKeyCode = keyCode;
        pendingCloseTimeoutTicks = CLOSE_TIMEOUT_TICKS;
    }

    public static boolean handleMouseButton(AbstractContainerScreen<?> screen, int button, Minecraft mc) {
        return handleKey(screen, KuudraScreen.MOUSE_OFFSET + button, mc);
    }

    public static boolean handleKey(AbstractContainerScreen<?> screen, int keyCode, Minecraft mc) {
        if (!KuudraConfig.isWardrobeEnabled()) return false;

        if (System.currentTimeMillis() < nextAllowedAtMs) return false;

        String title = strip(screen.getTitle().getString());

        if (WARDROBE_TITLE_PATTERN.matcher(title).find()) {
            return handleWardrobeKey(screen, title, keyCode, mc);
        }
        if (LOADOUTS_TITLE_PATTERN.matcher(title).find()) {
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

        int[] slotKeys = KuudraConfig.getWardrobeSlotKeys();
        for (int i = 0; i < 9; i++) {
            if (slotKeys[i] > 0 && keyCode == slotKeys[i]) {
                int containerSlot = 36 + i;
                if (containerSlot >= handler.slots.size()) return true;
                if (containerSlot == equippedSlot && KuudraConfig.isWardrobeDisableUnequipEnabled()) {
                    scheduleCloseAfterRelease(keyCode);
                    return true;
                }
                doClick(mc, handler, containerSlot, true, keyCode);
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

        int[] slotKeys = KuudraConfig.getLoadoutSlotKeys();
        for (int i = 0; i < LOADOUT_SLOTS.length; i++) {
            if (slotKeys[i] > 0 && keyCode == slotKeys[i]) {
                int containerSlot = LOADOUT_SLOTS[i];
                if (containerSlot >= handler.slots.size()) return true;
                doClick(mc, handler, containerSlot, false, keyCode);
                armBootsWatch(handler);
                return true;
            }
        }

        int nextKey = KuudraConfig.getWardrobeNextPageKey();
        int prevKey = KuudraConfig.getWardrobePrevPageKey();

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
                    KuudraConfig.setLastEquippedWardrobeSlot(i);
                    return i;
                }
            }
        }
        return KuudraConfig.getLastEquippedWardrobeSlot();
    }

    public static void reset() {
        closeInTicks             = -1;
        pendingCloseKeyCode      = -1;
        pendingCloseTimeoutTicks = -1;
        nextAllowedAtMs          = 0;
        for (int i = 0; i < prevDown.length; i++) prevDown[i] = false;
    }

    private static String strip(String s) {
        return s.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}
