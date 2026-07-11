package com.kuudrahelper.features.miscskyblock;

import com.kuudrahelper.KuudraConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public final class PreventPlacingWeapons {

    private static final Set<String> BLOCKED_IDS = Set.of(
            "FLOWER_OF_TRUTH",
            "BOUQUET_OF_LIES",
            "MOODY_GRAPPLESHOT",
            "BAT_WAND",
            "STARRED_BAT_WAND",
            "WEIRD_TUBA",
            "WEIRDER_TUBA",
            "PUMPKIN_LAUNCHER",
            "FIRE_FREEZE_STAFF"
    );

    private PreventPlacingWeapons() {}

    public static boolean shouldCancel(Player player, ItemStack stack) {
        if (!KuudraConfig.isPreventPlacingWeaponsEnabled()) return false;
        String id = getSkyblockId(stack);
        return id != null && BLOCKED_IDS.contains(id);
    }

    private static String getSkyblockId(ItemStack stack) {
        if (stack.isEmpty()) return null;
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        String id = customData.copyTag().getStringOr("id", "");
        return id.isEmpty() ? null : id;
    }
}
