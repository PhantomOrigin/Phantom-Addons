package com.phantomaddons.features.miscskyblock;

import com.phantomaddons.PhantomConfig;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

public final class BlockCloseItem {

    private BlockCloseItem() {}

    public static boolean shouldBlock(Slot slot) {
        if (!PhantomConfig.isBlockCloseItemEnabled()) return false;
        return slot != null && slot.getItem().is(Items.BARRIER);
    }
}
