package com.kuudrahelper.features;

import com.kuudrahelper.utils.Phase2BuildTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashSet;
import java.util.Set;

public final class PickoblockManager {

    public static final Set<BlockPos> VALID_BLOCKS = new HashSet<>();

    public static int predictedPickobulusGraceTicks = 0;

    private static boolean allowedThisTick = true;

    public static void init() {
        // ── Z = -167 column ────────────────────────────────────────────────
        add(-169, 28, -167); add(-169, 27, -167); add(-169, 26, -167); add(-169, 25, -167);
        add(-168, 30, -167); add(-168, 29, -167); add(-168, 28, -167); add(-168, 27, -167);
        add(-168, 26, -167); add(-168, 25, -167); add(-168, 24, -167);
        add(-167, 28, -167); add(-167, 27, -167); add(-167, 26, -167);
        add(-167, 25, -167); add(-167, 24, -167);

        // ── Z = -166 column ────────────────────────────────────────────────
        add(-169, 29, -166); add(-169, 28, -166); add(-169, 27, -166);
        add(-169, 26, -166); add(-169, 25, -166); add(-169, 24, -166);
        add(-168, 35, -166); add(-168, 34, -166); add(-168, 33, -166); add(-168, 32, -166);
        add(-168, 31, -166); add(-168, 30, -166); add(-168, 29, -166); add(-168, 28, -166);
        add(-168, 27, -166); add(-168, 26, -166); add(-168, 25, -166); add(-168, 24, -166);
        add(-167, 30, -166); add(-167, 29, -166); add(-167, 28, -166); add(-167, 27, -166);
        add(-167, 26, -166); add(-167, 25, -166); add(-167, 24, -166);

        // ── Z = -165 column ────────────────────────────────────────────────
        add(-169, 29, -165); add(-169, 28, -165); add(-169, 27, -165);
        add(-169, 26, -165); add(-169, 25, -165); add(-169, 24, -165);
        add(-168, 30, -165); add(-168, 29, -165); add(-168, 28, -165); add(-168, 27, -165);
        add(-168, 26, -165); add(-168, 25, -165); add(-168, 24, -165);
        add(-167, 29, -165); add(-167, 28, -165); add(-167, 27, -165);
        add(-167, 26, -165); add(-167, 25, -165);

        // ── Z = -168 column ────────────────────────────────────────────────
        add(-168, 29, -168); add(-168, 28, -168); add(-168, 27, -168);
        add(-167, 27, -168);
    }

    public static void tick(Minecraft client) {
        if (predictedPickobulusGraceTicks > 0) predictedPickobulusGraceTicks--;
        updatePermission(client);
    }

    public static boolean isPickobulusAllowedThisTick() {
        return allowedThisTick;
    }

    public static boolean isPickobulusItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        for (var line : lore.lines()) {
            if (line.getString().contains("Ability: Pickobulus")) return true;
        }
        return false;
    }

    public static boolean isLookingAtAllowedBlock(Player player) {
        Minecraft client = Minecraft.getInstance();
        if (!(client.hitResult instanceof BlockHitResult hit)) return false;
        return VALID_BLOCKS.contains(hit.getBlockPos());
    }

    private static void updatePermission(Minecraft client) {
        Player player = client.player;

        if (player == null || !Phase2BuildTracker.isActive()) {
            allowedThisTick = true;
            return;
        }

        if (predictedPickobulusGraceTicks > 0) {
            allowedThisTick = true;
            return;
        }

        allowedThisTick = isLookingAtAllowedBlock(player);
    }

    private static void add(int x, int y, int z) {
        VALID_BLOCKS.add(new BlockPos(x, y, z));
    }
}
