package com.phantomaddons.mixin;

import com.phantomaddons.utils.TextUtil;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.misckuudra.ShopKeybinds;
import com.phantomaddons.features.miscskyblock.BlockCloseItem;
import com.phantomaddons.features.miscskyblock.SlotBinds;
import com.phantomaddons.features.loadouts.WardrobeKeybinds;
import com.phantomaddons.features.misckuudra.profile.PartyFinderProfileHook;
import com.phantomaddons.features.misckuudra.profittracker.ChestValueOverlay;
import com.phantomaddons.features.misckuudra.profittracker.CroesusListener;
import com.phantomaddons.features.misckuudra.profittracker.ProfitHud;
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

@Mixin(AbstractContainerScreen.class)
public class HandledScreenMixin {

    private static final int LIME_GREEN_HIGHLIGHT = 0x6632CD32;

    @Shadow protected Slot hoveredSlot;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Final @Shadow protected int imageWidth;

    @Shadow protected void slotClicked(Slot slot, int slotId, int mouseButton, ContainerInput type) {}

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
    private void kuudrahelper$middleClickShopGui(Slot slot, int slotId, int mouseButton,
                                                 ContainerInput clickType, CallbackInfo ci) {
        if (!PhantomConfig.isMiddleClickShopGuiEnabled()) return;
        if (mouseButton != 0 || clickType != ContainerInput.PICKUP) return;
        if (slot == null || slot.getItem().isEmpty()) return;

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        String title = TextUtil.stripColor(self.getTitle().getString());
        if (!title.contains("Perk Menu")) return;

        ci.cancel();
        this.slotClicked(slot, slotId, 2, ContainerInput.CLONE);
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void kuudrahelper$blockCloseItemClick(Slot slot, int slotId, int mouseButton,
                                                  ContainerInput clickType, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (!(self instanceof InventoryScreen) && BlockCloseItem.shouldBlock(slot)) {
            ci.cancel();
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void kuudrahelper$slotBindsClick(Slot slot, int slotId, int mouseButton,
                                             ContainerInput clickType, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        boolean isKuudraChest = PhantomConfig.isProfitTrackerEnabled()
                && kuudrahelper$kind(self) == KIND_KUUDRA_CHEST;

        if (isKuudraChest && PhantomConfig.isBlockExpensiveRerollEnabled()
                && kuudrahelper$cachedAnalysis != null
                && slotId == kuudrahelper$cachedAnalysis.rerollSlotIndex()
                && !isControlDown()
                && CroesusListener.hasExpensiveRerollBlockItem(self)) {
            ci.cancel();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cBlocked Reroll - Expensive Item Detected - Hold CTRL to override"));
            }
            return;
        }

        if (isKuudraChest) {
            kuudrahelper$cachedAnalysis = null;
        }

        if (clickType != ContainerInput.QUICK_MOVE) return;
        if (self instanceof InventoryScreen && SlotBinds.handleShiftClick(self, slot)) {
            ci.cancel();
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void kuudrahelper$onRemoved(CallbackInfo ci) {
        SlotBinds.clearPending();
        kuudrahelper$cachedAnalysis = null;
        kuudrahelper$lastChestSignature = 0;
        kuudrahelper$screenKind = KIND_UNRESOLVED;
        ChestValueOverlay.reset();
        PartyFinderProfileHook.reset();
    }

    // ── Croesus + Profit Tracker ──────────────────────────────────────────────────

    private CroesusListener.ChestAnalysis kuudrahelper$cachedAnalysis = null;

    private static final int KIND_UNRESOLVED = -1, KIND_OTHER = 0,
                             KIND_CROESUS = 1, KIND_KUUDRA_CHEST = 2, KIND_PARTY_FINDER = 3;
    private int kuudrahelper$screenKind = KIND_UNRESOLVED;

    private int kuudrahelper$kind(AbstractContainerScreen<?> self) {
        if (kuudrahelper$screenKind == KIND_UNRESOLVED) {
            if (CroesusListener.isCroesusMain(self)) {
                kuudrahelper$screenKind = KIND_CROESUS;
            } else if (CroesusListener.isKuudraChest(self)) {
                kuudrahelper$screenKind = KIND_KUUDRA_CHEST;
            } else if (TextUtil.stripColor(self.getTitle().getString())
                    .equalsIgnoreCase("Party Finder")) {
                kuudrahelper$screenKind = KIND_PARTY_FINDER;
            } else {
                kuudrahelper$screenKind = KIND_OTHER;
            }
        }
        return kuudrahelper$screenKind;
    }

    private int kuudrahelper$lastChestSignature = 0;

    private static int kuudrahelper$chestSignature(AbstractContainerScreen<?> self) {
        var slots = self.getMenu().slots;
        int chestSlots = Math.max(0, slots.size() - 36);
        int h = 1;
        for (int i = 0; i < chestSlots; i++) {
            ItemStack stack = slots.get(i).getItem();
            h = 31 * h + (stack.isEmpty() ? 0 : System.identityHashCode(stack) * 31 + stack.getCount());
        }
        return h;
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void kuudrahelper$inventoryOverlay(GuiGraphicsExtractor ctx, int mx, int my, float delta,
                                               CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        boolean profitOn = PhantomConfig.isProfitTrackerEnabled();
        int kind = kuudrahelper$kind(self);

        if (kind == KIND_PARTY_FINDER) {
            PartyFinderProfileHook.checkShiftHover(self, hoveredSlot, Minecraft.getInstance());
        }

        // ── Croesus main menu: highlight unopened chests ──────────────────────────
        if (PhantomConfig.isHighlightUnopenedChestsEnabled() && kind == KIND_CROESUS) {
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
        if (profitOn && kind == KIND_KUUDRA_CHEST) {
            if (!ChestValueOverlay.isChestOpen()) {
                ChestValueOverlay.onChestOpen(self);
            }
            int signature = kuudrahelper$chestSignature(self);
            if (signature != kuudrahelper$lastChestSignature || kuudrahelper$cachedAnalysis == null) {
                if (ChestValueOverlay.areSlotsReady(self)) {
                    kuudrahelper$lastChestSignature = signature;
                    kuudrahelper$cachedAnalysis = CroesusListener.analyseChest(self);
                    ChestValueOverlay.updatePending(kuudrahelper$cachedAnalysis);
                }
            }
            CroesusListener.ChestAnalysis a = kuudrahelper$cachedAnalysis;
            AbstractContainerMenu menu = self.getMenu();
            ctx.nextStratum();

            ChestValueOverlay.render(ctx, a);

            if (a != null && PhantomConfig.isProfitRerollCalc()) {
                if (a.rerollProfit() && a.rerollSlotIndex() >= 0 && a.rerollSlotIndex() < menu.slots.size()) {
                    Slot rs = menu.slots.get(a.rerollSlotIndex());
                    ctx.fill(leftPos + rs.x - 1, topPos + rs.y - 1,
                             leftPos + rs.x + 17, topPos + rs.y + 17, LIME_GREEN_HIGHLIGHT);
                }
                if (a.wheelOfFate() && a.wheelSlotIndex() >= 0 && a.wheelSlotIndex() < menu.slots.size()) {
                    Slot ws = menu.slots.get(a.wheelSlotIndex());
                    ctx.fill(leftPos + ws.x - 1, topPos + ws.y - 1,
                             leftPos + ws.x + 17, topPos + ws.y + 17, LIME_GREEN_HIGHLIGHT);
                }
            }
            return;
        }
    }

    private static boolean isControlDown() {
        com.mojang.blaze3d.platform.Window window = Minecraft.getInstance().getWindow();
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

}
