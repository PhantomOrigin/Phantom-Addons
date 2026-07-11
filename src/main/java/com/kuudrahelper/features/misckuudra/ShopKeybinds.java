package com.kuudrahelper.features.misckuudra;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.KuudraHelperMod;
import com.kuudrahelper.KuudraScreen;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ShopKeybinds {

    private static final String TITLE_CONFIRM   = "Are you sure?";
    private static final String TITLE_PERK_MENU = "Perk Menu";

    private static final String ITEM_SPECIALIST = "Specialist Route";
    private static final String ITEM_CONFIRM    = "Confirm";
    private static final String ITEM_BALLISTA   = "Ballista Mechanic";
    private static final String ITEM_CANNONBALL = "Human Cannonball";

    private static final String PURCHASE_MSG    = "You purchased Human Cannonball!";

    private static final long COOLDOWN_MS = 200L;
    private static long nextActionMs = 0L;

    private static volatile boolean pendingClose = false;

    private ShopKeybinds() {}

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !KuudraConfig.isShopKeybindsEnabled()) return;
            String text = message.getString().replaceAll("§[0-9a-fk-orA-FK-OR]", "");
            if (text.contains(PURCHASE_MSG)) {
                pendingClose = true;
                KuudraHelperMod.LOGGER.info("[ShopKeybinds] Human Cannonball purchased — cannon key will close next GUI");
            }
        });
    }

    public static void reset() {
        pendingClose = false;
        nextActionMs = 0L;
    }

    public static boolean handleMouseButton(AbstractContainerScreen<?> screen, int button, Minecraft mc) {
        return handleKey(screen, KuudraScreen.MOUSE_OFFSET + button, mc);
    }

    public static boolean handleKey(AbstractContainerScreen<?> screen, int keyCode, Minecraft mc) {
        if (!KuudraConfig.isShopKeybindsEnabled()) return false;

        boolean isCannon = (keyCode == KuudraConfig.getShopCannonKey());

        if (pendingClose && isCannon) {
            pendingClose = false;
            mc.player.closeContainer();
            KuudraHelperMod.LOGGER.info("[ShopKeybinds] Closed container after cannonball purchase");
            return true;
        }

        long now = System.currentTimeMillis();
        if (now < nextActionMs) return false;

        boolean isMain = (keyCode == KuudraConfig.getShopMainKey());
        if (!isMain && !isCannon) return false;

        String title = strip(screen.getTitle().getString());
        boolean acted = false;

        if (isMain) {
            if (title.contains(TITLE_CONFIRM)) {
                acted = clickSlot(mc, screen, ITEM_CONFIRM, "Confirm");
            } else if (title.contains(TITLE_PERK_MENU)) {
                acted = clickSlot(mc, screen, ITEM_SPECIALIST, "Specialist Route")
                        || clickSlot(mc, screen, ITEM_BALLISTA,   "Ballista Mechanic");
            }
        } else { // cannon key — normal perk-menu click while not yet purchased
            if (title.contains(TITLE_PERK_MENU)) {
                acted = clickSlot(mc, screen, ITEM_CANNONBALL, "Human Cannonball");
            }
        }

        if (acted) {
            nextActionMs = now + COOLDOWN_MS;
        }
        return acted;
    }

    private static boolean clickSlot(Minecraft mc, AbstractContainerScreen<?> screen,
                                     String fragment, String logName) {
        AbstractContainerMenu handler = screen.getMenu();
        List<Slot> slots = handler.slots;
        String target = fragment.toLowerCase();

        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            if (!strip(stack.getHoverName().getString()).toLowerCase().contains(target)) continue;

            if (mc.gameMode == null || mc.player == null) return false;
            mc.gameMode.handleContainerInput(
                    handler.containerId, i, 0, ContainerInput.PICKUP, mc.player);
            KuudraHelperMod.LOGGER.info("[ShopKeybinds] Clicked {}", logName);
            return true;
        }
        return false;
    }

    private static String strip(String s) {
        return s.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}
