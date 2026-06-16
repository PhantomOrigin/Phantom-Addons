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

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void kuudrahelper$containerKeys(KeyEvent event,
                                            CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        int key = event.key();

        if (self instanceof InventoryScreen && SlotBinds.handleKeyPress(self, key, hoveredSlot)) { cir.setReturnValue(true); return; }
        if (WardrobeKeybinds.handleKey(self, key, mc))        { cir.setReturnValue(true); return; }
        if (ShopKeybinds.handleKey(self, key, mc))             { cir.setReturnValue(true); }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void kuudrahelper$containerMouse(MouseButtonEvent event, boolean isDoubleClick,
                                             CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        int button = event.button();

        if (WardrobeKeybinds.handleMouseButton(self, button, mc)) { cir.setReturnValue(true); return; }
        if (ShopKeybinds.handleMouseButton(self, button, mc))      { cir.setReturnValue(true); }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void kuudrahelper$slotBindsClick(Slot slot, int slotId, int mouseButton,
                                              ContainerInput clickType, CallbackInfo ci) {
        if (clickType != ContainerInput.QUICK_MOVE) return;
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (self instanceof InventoryScreen && SlotBinds.handleShiftClick(self, slot)) ci.cancel();
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

        int hoveredId = hoveredSlot != null ? hoveredSlot.index : -1;

        // Find which binding involves the hovered slot (if any)
        int hoveredInv = -1, hoveredHot = -1;
        if (hoveredId >= 0) {
            if (bindings.containsKey(hoveredId)) {
                hoveredInv = hoveredId;
                hoveredHot = 36 + bindings.get(hoveredId);
            } else {
                for (Map.Entry<Integer, Integer> e : bindings.entrySet()) {
                    if (36 + e.getValue() == hoveredId) {
                        hoveredInv = e.getKey();
                        hoveredHot = hoveredId;
                        break;
                    }
                }
            }
        }

        for (Map.Entry<Integer, Integer> entry : bindings.entrySet()) {
            int invSlotId   = entry.getKey();
            int hotSlotId   = 36 + entry.getValue();

            if (invSlotId >= menu.slots.size() || hotSlotId >= menu.slots.size()) continue;
            Slot invSlot = menu.slots.get(invSlotId);
            Slot hotSlot = menu.slots.get(hotSlotId);

            int x1 = leftPos + invSlot.x + 8;
            int y1 = topPos  + invSlot.y + 8;
            int x2 = leftPos + hotSlot.x + 8;
            int y2 = topPos  + hotSlot.y + 8;

            boolean hovered = invSlotId == hoveredInv;
            int color = hovered ? 0xFF55BBFF : 0x884488FF;
            drawLine(ctx, x1, y1, x2, y2, color);
        }

        // Tooltip label when hovering a bound slot
        if (hoveredInv >= 0 && hoveredSlot != null) {
            Minecraft mc = Minecraft.getInstance();
            int hotbarNum = hoveredHot - 36 + 1;
            String label = "§eBound ↔ Hotbar " + hotbarNum;
            int lx = leftPos + hoveredSlot.x + 8 - mc.font.width(label) / 2;
            int ly = topPos  + hoveredSlot.y - 12;
            ctx.fill(lx - 2, ly - 1, lx + mc.font.width(label) + 2, ly + mc.font.lineHeight + 1, 0xBB000000);
            ctx.text(mc.font, label, lx, ly, 0xFFFFFFFF, true);
        }
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
}
