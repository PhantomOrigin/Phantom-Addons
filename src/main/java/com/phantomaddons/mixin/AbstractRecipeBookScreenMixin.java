package com.phantomaddons.mixin;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.miscskyblock.SlotBinds;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin<T extends RecipeBookMenu> extends AbstractContainerScreen<T> {

    protected AbstractRecipeBookScreenMixin(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;nextStratum()V", ordinal = 0, shift = At.Shift.AFTER))
    private void phantomaddons$drawSlotBindLines(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!PhantomConfig.isSlotBindsEnabled()) return;

        Map<Integer, Integer> bindings = PhantomConfig.getSlotBindings();
        if (bindings.isEmpty()) return;

        AbstractContainerMenu menu = this.menu;
        int hoveredId = this.hoveredSlot != null ? menu.slots.indexOf(this.hoveredSlot) : -1;
        boolean bindKeyHeld = SlotBinds.isBindKeyHeld();

        boolean showAll = bindKeyHeld && hoveredId < 0;
        boolean showHoveredOnly = hoveredId >= 0 && (bindKeyHeld || PhantomConfig.isSlotBindsShowOnHover());
        if (!showAll && !showHoveredOnly) return;

        for (Map.Entry<Integer, Integer> entry : bindings.entrySet()) {
            int invSlotId = entry.getKey();
            int hotSlotId = 36 + entry.getValue();
            if (showHoveredOnly && invSlotId != hoveredId && hotSlotId != hoveredId) continue;
            if (invSlotId >= menu.slots.size() || hotSlotId >= menu.slots.size()) continue;

            Slot invSlot = menu.slots.get(invSlotId);
            Slot hotSlot = menu.slots.get(hotSlotId);
            drawGuiLine(ctx, this.leftPos + invSlot.x + 8, this.topPos + invSlot.y + 8,
                             this.leftPos + hotSlot.x + 8, this.topPos + hotSlot.y + 8, 0xAA55FF);
        }
    }

    private static void drawGuiLine(GuiGraphicsExtractor ctx, float x1, float y1, float x2, float y2, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.01f) return;
        float angle = (float) Math.atan2(dy, dx);
        int c = color | 0xFF000000;

        var m = ctx.pose();
        m.pushMatrix();
        m.translate(x1, y1);
        m.rotate(angle);
        ctx.fill(0, 0, Math.round(length), 1, c);
        m.popMatrix();
    }
}
