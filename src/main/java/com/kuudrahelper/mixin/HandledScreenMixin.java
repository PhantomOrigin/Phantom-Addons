package com.kuudrahelper.mixin;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.ShopKeybinds;
import com.kuudrahelper.features.SlotBinds;
import com.kuudrahelper.features.WardrobeKeybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
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

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void kuudrahelper$slotBindsClick(Slot slot, int slotId, int mouseButton,
                                             ContainerInput clickType, CallbackInfo ci) {
        if (clickType != ContainerInput.QUICK_MOVE) return;
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (self instanceof InventoryScreen && SlotBinds.handleShiftClick(self, slot)) {
            ci.cancel();
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void kuudrahelper$onRemoved(CallbackInfo ci) {
        SlotBinds.clearPending();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void kuudrahelper$drawBindLines(GuiGraphicsExtractor ctx, int mx, int my, float delta,
                                            CallbackInfo ci) {
        if (!((Object) this instanceof InventoryScreen)) return;
        if (!KuudraConfig.isSlotBindsEnabled()) return;

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
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
                    ? "\u00a7e" + relatedCount + " Slot Binds"
                    : "\u00a7eBound -> Hotbar " + (tooltipHotSlot - 36 + 1);

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

    private static void drawLine(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1), sx = x1 < x2 ? 1 : -1;
        int dy = -Math.abs(y2 - y1), sy = y1 < y2 ? 1 : -1;
        int err = dx + dy;

        while (true) {
            ctx.fill(x1 - 1, y1 - 1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x1 += sx; }
            if (e2 <= dx) { err += dx; y1 += sy; }
        }
    }

    private static void drawGuiLine(GuiGraphicsExtractor ctx,
                                    int x1, int y1,
                                    int x2, int y2,
                                    int color) {

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;

        int err = dx - dy;

        int opaqueColor = (color | 0xFF000000);

        while (true) {
            ctx.fill(x1, y1, x1 + 1, y1 + 1, opaqueColor);
            ctx.fill(x1 - 1, y1 - 1, x1 + 2, y1 + 2, opaqueColor);

            if (x1 == x2 && y1 == y2) break;

            int e2 = err * 2;

            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }

            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }
}