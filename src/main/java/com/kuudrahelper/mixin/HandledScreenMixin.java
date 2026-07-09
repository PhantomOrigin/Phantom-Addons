package com.kuudrahelper.mixin;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.ShopKeybinds;
import com.kuudrahelper.features.SlotBinds;
import com.kuudrahelper.features.WardrobeKeybinds;
import com.kuudrahelper.features.profile.PartyFinderProfileHook;
import com.kuudrahelper.features.profittracker.ChestValueOverlay;
import com.kuudrahelper.features.profittracker.CroesusListener;
import com.kuudrahelper.features.profittracker.ProfitHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(AbstractContainerScreen.class)
public class HandledScreenMixin {

    @Shadow protected Slot hoveredSlot;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Final @Shadow protected int imageWidth;

    private int kuudrahelper$getMenuSlotId(AbstractContainerScreen<?> screen, Slot slot) {
        return slot == null ? -1 : screen.getMenu().slots.indexOf(slot);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void kuudrahelper$containerKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        int key = event.key();

        if (self instanceof InventoryScreen && SlotBinds.handleKeyPress(self, key, hoveredSlot)) {
            cir.setReturnValue(true);
            return;
        }
        if (WardrobeKeybinds.handleKey(self, key, mc)) {
            cir.setReturnValue(true);
            return;
        }
        if (ShopKeybinds.handleKey(self, key, mc)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void kuudrahelper$containerMouse(MouseButtonEvent event, boolean isDoubleClick,
                                             CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        int button = event.button();

        if (WardrobeKeybinds.handleMouseButton(self, button, mc)) {
            cir.setReturnValue(true);
            return;
        }
        if (ShopKeybinds.handleMouseButton(self, button, mc)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void kuudrahelper$profitButtonClick(MouseButtonEvent event, boolean isDouble,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) return;
        if (!ProfitHud.shouldShow()) return;
        int mx = (int) event.x(), my = (int) event.y();
        if (ProfitHud.handleClick(mx, my)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void kuudrahelper$slotBindsClick(Slot slot, int slotId, int mouseButton,
                                             ContainerInput clickType, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;

        // Track kismet / wheel-of-fate usage in chest overlay
        if (KuudraConfig.isProfitTrackerEnabled() && CroesusListener.isKuudraChest(self)) {
            ChestValueOverlay.onSlotClicked(slotId, kuudrahelper$cachedAnalysis);
            // Invalidate cached analysis so we re-read the new slot contents next frame
            kuudrahelper$cachedAnalysis = null;
        }

        if (clickType != ContainerInput.QUICK_MOVE) return;
        if (self instanceof InventoryScreen && SlotBinds.handleShiftClick(self, slot)) {
            ci.cancel();
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void kuudrahelper$onRemoved(CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        com.kuudrahelper.KuudraHelperMod.LOGGER.info("[WardrobeDebug] t={} screen removed: \"{}\"",
                System.currentTimeMillis(), self.getTitle().getString());
        SlotBinds.clearPending();
        kuudrahelper$cachedAnalysis = null;
        ChestValueOverlay.reset();
        PartyFinderProfileHook.reset();
    }

    // ── Croesus + Profit Tracker ──────────────────────────────────────────────────

    private CroesusListener.ChestAnalysis kuudrahelper$cachedAnalysis = null;
    private boolean kuudrahelper$loggedShown = false;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void kuudrahelper$inventoryOverlay(GuiGraphicsExtractor ctx, int mx, int my, float delta,
                                               CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        boolean profitOn = KuudraConfig.isProfitTrackerEnabled();

        if (!kuudrahelper$loggedShown) {
            kuudrahelper$loggedShown = true;
            com.kuudrahelper.KuudraHelperMod.LOGGER.info("[WardrobeDebug] t={} screen shown: \"{}\"",
                    System.currentTimeMillis(), self.getTitle().getString());
        }

        PartyFinderProfileHook.checkShiftHover(self, hoveredSlot, Minecraft.getInstance());

        // ── Croesus main menu: highlight unopened chests ──────────────────────────
        if (profitOn && KuudraConfig.isProfitHighlightChests() && CroesusListener.isCroesusMain(self)) {
            ctx.nextStratum();
            AbstractContainerMenu menu = self.getMenu();
            for (Slot slot : menu.slots) {
                ItemStack stack = slot.getItem();
                if (CroesusListener.isUnopenedChest(stack)) {
                    int sx = leftPos + slot.x - 1;
                    int sy = topPos  + slot.y - 1;
                    ctx.fill(sx, sy, sx + 18, sy + 18, 0x6600FFFF); // aqua highlight
                }
            }
            return;
        }

        // ── Kuudra reward chest: value overlay + highlight reroll/wheel slots ─────
        if (profitOn && CroesusListener.isKuudraChest(self)) {
            if (!ChestValueOverlay.isChestOpen()) {
                ChestValueOverlay.onChestOpen(self);
            }
            // Re-analyse every frame so prices update as the bazaar/BIN cache populates.
            if (ChestValueOverlay.areSlotsReady(self)) {
                kuudrahelper$cachedAnalysis = CroesusListener.analyseChest(self);
                ChestValueOverlay.updatePending(kuudrahelper$cachedAnalysis);
            }
            CroesusListener.ChestAnalysis a = kuudrahelper$cachedAnalysis;
            AbstractContainerMenu menu = self.getMenu();
            ctx.nextStratum();

            ChestValueOverlay.render(ctx, a);

            if (a != null && KuudraConfig.isProfitRerollCalc()) {
                if (a.rerollSlotIndex() >= 0 && a.rerollSlotIndex() < menu.slots.size()) {
                    Slot rs = menu.slots.get(a.rerollSlotIndex());
                    int color = a.rerollProfit() ? 0x6600FF44 : 0x66FF4444;
                    ctx.fill(leftPos + rs.x - 1, topPos + rs.y - 1,
                             leftPos + rs.x + 17, topPos + rs.y + 17, color);
                }
                if (a.wheelOfFate() && a.wheelSlotIndex() >= 0 && a.wheelSlotIndex() < menu.slots.size()) {
                    Slot ws = menu.slots.get(a.wheelSlotIndex());
                    ctx.fill(leftPos + ws.x - 1, topPos + ws.y - 1,
                             leftPos + ws.x + 17, topPos + ws.y + 17, 0x6600FF44);
                }
            }
            return;
        }

        // ── Slot bind lines (inventory screen only) ───────────────────────────────
        if (!((Object) self instanceof InventoryScreen)) return;
        if (!KuudraConfig.isSlotBindsEnabled()) return;

        AbstractContainerMenu menu = self.getMenu();
        Map<Integer, Integer> bindings = KuudraConfig.getSlotBindings();
        if (bindings.isEmpty()) return;

        int hoveredId = kuudrahelper$getMenuSlotId(self, hoveredSlot);
        boolean showAll = isShowBindsKeyHeld();
        boolean hoveredHasBind = false;
        int relatedCount = 0;
        int tooltipHotSlot = -1;

        if (hoveredId >= 0) {
            for (Map.Entry<Integer, Integer> entry : bindings.entrySet()) {
                int invSlotId = entry.getKey();
                int hotSlotId = 36 + entry.getValue();
                if (invSlotId == hoveredId || hotSlotId == hoveredId) {
                    hoveredHasBind = true;
                    relatedCount++;
                    tooltipHotSlot = hotSlotId;
                }
            }
        }

        if (!showAll && !hoveredHasBind) return;

        ctx.nextStratum();

        for (Map.Entry<Integer, Integer> entry : bindings.entrySet()) {
            int invSlotId = entry.getKey();
            int hotSlotId = 36 + entry.getValue();
            boolean relatedToHovered = invSlotId == hoveredId || hotSlotId == hoveredId;

            if (!showAll && !relatedToHovered) continue;
            if (invSlotId < 0 || hotSlotId < 0) continue;
            if (invSlotId >= menu.slots.size() || hotSlotId >= menu.slots.size()) continue;

            Slot invSlot = menu.slots.get(invSlotId);
            Slot hotSlot = menu.slots.get(hotSlotId);

            int x1 = leftPos + invSlot.x + 8;
            int y1 = topPos + invSlot.y + 8;
            int x2 = leftPos + hotSlot.x + 8;
            int y2 = topPos + hotSlot.y + 8;

            drawGuiLine(ctx, x1, y1, x2, y2, 0x55FFFF);
        }

        if (hoveredHasBind && hoveredSlot != null) {
            Minecraft mc = Minecraft.getInstance();
            String label = relatedCount > 1
                    ? "§e" + relatedCount + " Slot Binds"
                    : "§eBound -> Hotbar " + (tooltipHotSlot - 36 + 1);

            int lx = leftPos + hoveredSlot.x + 8 - mc.font.width(label) / 2;
            int ly = topPos + hoveredSlot.y - 12;

            ctx.fill(lx - 2, ly - 1, lx + mc.font.width(label) + 2, ly + mc.font.lineHeight + 1, 0xBB000000);
            ctx.text(mc.font, label, lx, ly, 0xFFFFFFFF, true);
        }
    }

    private static boolean isShowBindsKeyHeld() {
        int showKey = KuudraConfig.getSlotBindShowKey();
        if (showKey <= 0) return false;

        com.mojang.blaze3d.platform.Window window = Minecraft.getInstance().getWindow();
        if (showKey >= com.kuudrahelper.KuudraScreen.MOUSE_OFFSET) {
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                    window.handle(),
                    showKey - com.kuudrahelper.KuudraScreen.MOUSE_OFFSET
            ) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        }

        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, showKey);
    }

    private static void drawGuiLine(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1), sx = x1 < x2 ? 1 : -1;
        int dy = -Math.abs(y2 - y1), sy = y1 < y2 ? 1 : -1;
        int err = dx + dy;
        int c = color | 0xFF000000;
        while (true) {
            ctx.fill(x1 - 1, y1 - 1, x1 + 2, y1 + 2, c);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x1 += sx; }
            if (e2 <= dx) { err += dx; y1 += sy; }
        }
    }
}
