package com.phantomaddons.features.misckuudra;

import com.phantomaddons.PhantomConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;


public final class EtherwarpPredictor {

    private static boolean etherwarpBlockedByLava = false;

    public static void predictEtherwarpIntoLava(Minecraft client) {
        etherwarpBlockedByLava = false;

        if (!PhantomConfig.isEtherwarpLavaBlockEnabled()) return;
        if (PhantomConfig.isEtherwarpLavaBlockOnlyInKuudra() && com.phantomaddons.utils.KuudraTierDetector.getTier() <= 0) return;
        if (client.player == null || client.level == null) return;

        Player player = client.player;
        ClientLevel world = client.level;

        BlockHitResult hit = castRay(world, player, player.getEyePosition(), 60.0, false);
        if (hit.getType() != HitResult.Type.BLOCK) return;

        Vec3 teleportPos = Vec3.atCenterOf(hit.getBlockPos()).add(0, 0.05, 0);
        BlockPos basePos = BlockPos.containing(teleportPos);
        BlockPos headPos = basePos.above();

        if (isLava(world.getFluidState(basePos))
                || isLava(world.getFluidState(headPos))) {
            etherwarpBlockedByLava = true;
        }
    }

    public static void predictEtherwarpIntoPickobulus(Minecraft client) {
        if (client.player == null || client.level == null) return;

        Player player = client.player;
        ClientLevel world = client.level;

        Vec3 eatenPos = getPredictedEatenPosition();
        if (eatenPos == null) return;

        Vec3 eatenEye = eatenPos.add(0, player.getEyeHeight(), 0);
        Vec3 look     = player.getViewVector(1.0F);

        Vec3 etherEnd = eatenEye.add(look.scale(60.0));
        BlockHitResult etherHit = world.clip(new ClipContext(
                eatenEye, etherEnd,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player));

        if (etherHit.getType() != HitResult.Type.BLOCK) return;

        BlockPos etherBlock = etherHit.getBlockPos();
        if (!isEtherwarpLandingValid(world, etherBlock)) return;

        Vec3 teleportEye = Vec3.atCenterOf(etherBlock)
                .add(0, 0.55 + player.getEyeHeight(), 0);
        Vec3 pickoEnd = teleportEye.add(look.scale(6.0));

        BlockHitResult pickoHit = world.clip(new ClipContext(
                teleportEye, pickoEnd,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player));

        if (pickoHit.getType() != HitResult.Type.BLOCK) return;

        if (PickoblockManager.VALID_BLOCKS.contains(pickoHit.getBlockPos())) {
            PickoblockManager.predictedPickobulusGraceTicks = 5;
        }
    }

    public static boolean isEtherwarpBlockedThisTick() {
        return etherwarpBlockedByLava;
    }

    public static boolean isEtherTransmissionItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        for (var line : lore.lines()) {
            if (line.getString().contains("Ability: Ether Transmission")) return true;
        }
        return false;
    }

    private static BlockHitResult castRay(ClientLevel world, Player player,
                                          Vec3 from, double range,
                                          boolean includeFluids) {
        Vec3 to = from.add(player.getViewVector(1.0F).scale(range));
        return world.clip(new ClipContext(
                from, to,
                ClipContext.Block.OUTLINE,
                includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE,
                player));
    }

    private static boolean isEtherwarpLandingValid(ClientLevel world, BlockPos pos) {
        return world.getBlockState(pos.above()).isAir()
                && world.getBlockState(pos.above(2)).isAir();
    }

    private static Vec3 getPredictedEatenPosition() {
        return new Vec3(-161, 49, -186);
    }

    public static boolean isInstantTransmissionItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        for (var line : lore.lines()) {
            if (line.getString().contains("Ability: Instant Transmission")) return true;
        }
        return false;
    }

    public static void predictInstantTransmissionIntoLava(Minecraft client) {
        etherwarpBlockedByLava = false;

        if (!PhantomConfig.isEtherwarpLavaBlockEnabled()) return;
        if (PhantomConfig.isEtherwarpLavaBlockOnlyInKuudra() && com.phantomaddons.utils.KuudraTierDetector.getTier() <= 0) return;
        if (client.player == null || client.level == null) return;

        Player player = client.player;
        ClientLevel world = client.level;

        BlockHitResult hit = castRay(world, player, player.getEyePosition(), 12.0, false);

        BlockPos headPos;
        if (hit.getType() == HitResult.Type.BLOCK) {
            headPos = hit.getBlockPos().relative(hit.getDirection());
        } else {
            Vec3 dest = player.getEyePosition().add(player.getViewVector(1.0F).scale(12.0));
            headPos = BlockPos.containing(dest);
        }

        BlockPos feetPos = headPos.below();

        if (isLava(world.getFluidState(feetPos))
                || isLava(world.getFluidState(headPos))) {
            etherwarpBlockedByLava = true;
        }
    }

    private static boolean isLava(net.minecraft.world.level.material.FluidState state) {
        return state.getType().isSame(Fluids.LAVA);
    }
}
