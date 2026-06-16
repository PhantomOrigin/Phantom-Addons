package com.kuudrahelper.mixin;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.KuudraScreen;
import com.kuudrahelper.features.ShopKeybinds;
import com.kuudrahelper.features.SlotBinds;
import com.kuudrahelper.features.WardrobeKeybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
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

        if (SlotBinds.handleKeyPress(self, key, hoveredSlot)) { cir.setReturnValue(true); return; }
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

    // Intercept shift-click on bound inventory slots and send SWAP instead
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void kuudrahelper$slotBindsClick(Slot slot, int slotId, int mouseButton,
                                              ContainerInput clickType, CallbackInfo ci) {
        if (clickType != ContainerInput.QUICK_MOVE) return;
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (SlotBinds.handleShiftClick(self, slot)) ci.cancel();
    }

    // Clear pending bind when inventory is closed
    @Inject(method = "removed", at = @At("HEAD"))
    private void kuudrahelper$onRemoved(CallbackInfo ci) {
        SlotBinds.clearPending();
    }

    // Draw lines between bound slot pairs when show-binds key is held
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void kuudrahelper$drawBindLines(GuiGraphicsExtractor ctx, int mx, int my, float delta,
                                             CallbackInfo ci) {
        if (!KuudraConfig.isSlotBindsEnabled()) return;

        int showKey = KuudraConfig.getSlotBindShowKey();
        if (showKey <= 0) return;

        long handle = GLFW.glfwGetCurrentContext();
        if (handle == 0) return;

        boolean held = showKey >= KuudraScreen.MOUSE_OFFSET
                ? GLFW.glfwGetMouseButton(handle, showKey - KuudraScreen.MOUSE_OFFSET) == GLFW.GLFW_PRESS
                : GLFW.glfwGetKey(handle, showKey) == GLFW.GLFW_PRESS;
        if (!held) return;

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        AbstractContainerMenu menu = self.getMenu();
        Map<Integer, Integer> bindings = KuudraConfig.getSlotBindings();
        if (bindings.isEmpty()) return;

        for (Map.Entry<Integer, Integer> entry : bindings.entrySet()) {
            int invSlotId   = entry.getKey();
            int hotbarIndex = entry.getValue();
            int hotSlotId   = 36 + hotbarIndex; // HOT_MIN + hotbarIndex

            if (invSlotId >= menu.slots.size() || hotSlotId >= menu.slots.size()) continue;
            Slot invSlot = menu.slots.get(invSlotId);
            Slot hotSlot = menu.slots.get(hotSlotId);

            int x1 = leftPos + invSlot.x + 8;
            int y1 = topPos  + invSlot.y + 8;
            int x2 = leftPos + hotSlot.x + 8;
            int y2 = topPos  + hotSlot.y + 8;

            drawLine(ctx, x1, y1, x2, y2, 0xFF4488FF);
        }
    }

    // Bresenham line — fill() uses raw screen coords, not the pose matrix
    private static void drawLine(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1), sx = x1 < x2 ? 1 : -1;
        int dy = -Math.abs(y2 - y1), sy = y1 < y2 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            ctx.fill(x1 - 1, y1 - 1, x1 + 1, y1 + 1, color); // 2×2 dot per step
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x1 += sx; }
            if (e2 <= dx) { err += dx; y1 += sy; }
        }
    }
}
