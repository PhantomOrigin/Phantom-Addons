package com.phantomaddons.features.miscskyblock;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.utils.TextUtil;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class BlockCloseItem {

    private BlockCloseItem() {}

    public static boolean shouldBlock(Slot slot) {
        if (!PhantomConfig.isBlockCloseItemEnabled() || slot == null) return false;
        ItemStack stack = slot.getItem();
        if (!stack.is(Items.BARRIER)) return false;
        return TextUtil.containsIgnoreCase(TextUtil.stripColor(stack.getHoverName().getString()), "close");
    }
}
