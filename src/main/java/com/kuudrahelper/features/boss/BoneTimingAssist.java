package com.kuudrahelper.features.boss;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.phase.KuudraPhaseTracker;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class BoneTimingAssist {

    // TEMP: still waiting on KUUDRA_STATIONARY_HITBOXES data — force-disabled across all
    // editions so it doesn't ship half-finished. Flip back to false once that's filled in.
    private static final boolean TEMP_DISABLED = true;
    public static boolean isTempDisabled() { return TEMP_DISABLED; }

    private static final double RAYCAST_RANGE = 14.0;

    private static final int BRACKET_MAX_OFFSET_PX = 60;
    private static final int WHITE_ROW_Y_OFFSET     = 0;
    private static final int RED_ROW_Y_OFFSET        = 12;

    private static final AABB[] KUUDRA_STATIONARY_HITBOXES = {
        // TODO: new AABB(minX, minY, minZ, maxX, maxY, maxZ),
    };

    // No arena-bounds placeholder needed anymore — the wall check is a real block
    // raycast against the world (see wallHitDistance), since the room isn't a clean box.

    private static volatile boolean hidden = false;

    private BoneTimingAssist() {}

    public static void onBonemerangThrow() {
        hidden = true;
    }

    public static void onLeftClick(ItemStack item) {
        if (!hasRendInItem(item)) return;
        hidden = false;
    }

    public static void reset() {
        hidden = false;
    }

    private static boolean hasRendInItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getHoverName().getString().toLowerCase().contains("rend")) return true;
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        for (Component line : lore.lines()) {
            if (line.getString().toLowerCase().contains("rend")) return true;
        }
        return false;
    }

    public static void register() {
        if (TEMP_DISABLED) return;
        HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath("phantomaddons", "bone_timing_assist"),
                (ctx, tickCounter) -> render(ctx));
    }

    private static void render(GuiGraphicsExtractor ctx) {
        if (TEMP_DISABLED) return;
        if (!KuudraConfig.isBoneTimingAssistEnabled()) return;
        if (hidden) return;
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.BOSS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.font == null) return;

        Vec3 eye  = mc.player.getEyePosition(1.0f);
        Vec3 look = mc.player.getLookAngle();

        AABB kuudraBox = findAimedStationaryHitbox(eye, look);
        if (kuudraBox == null) return; // not looking at a known Kuudra spot (or no data logged yet)

        double distToKuudra = rayEntryDistance(eye, look, kuudraBox, RAYCAST_RANGE);
        double distToWall   = wallHitDistance(mc, eye, look, RAYCAST_RANGE);

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int cx = screenW / 2;
        int cy = screenH / 2;

        drawBracketPair(ctx, mc, cx, cy + WHITE_ROW_Y_OFFSET, distToKuudra, 0xFFFFFFFF);
        drawBracketPair(ctx, mc, cx, cy + RED_ROW_Y_OFFSET, distToWall, 0xFFFF3333);
    }

    /**
     * Distance along the ray (capped at {@code range}) to the first non-air/liquid block
     * it actually hits — a real raycast against the world rather than a single bounding
     * box, since the arena's walls aren't flat.
     */
    private static double wallHitDistance(Minecraft mc, Vec3 origin, Vec3 dir, double range) {
        if (mc.level == null) return range;
        Vec3 far = origin.add(dir.scale(range));
        BlockHitResult hit = mc.level.clip(new ClipContext(
                origin, far,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                mc.player));
        if (hit.getType() != HitResult.Type.BLOCK) return range;
        return Math.min(range, origin.distanceTo(hit.getLocation()));
    }

    private static AABB findAimedStationaryHitbox(Vec3 eye, Vec3 look) {
        for (AABB box : KUUDRA_STATIONARY_HITBOXES) {
            if (rayExitDistance(eye, look, box, RAYCAST_RANGE) != null) return box;
        }
        return null;
    }

    private static void drawBracketPair(GuiGraphicsExtractor ctx, Minecraft mc,
                                        int cx, int cy, double distance, int color) {
        double frac   = Math.max(0.0, Math.min(1.0, distance / RAYCAST_RANGE));
        int    offset = (int) Math.round(BRACKET_MAX_OFFSET_PX * frac);
        if (offset <= 0) return; // fully closed — hide entirely per spec

        String left  = "[";
        String right = "]";
        ctx.text(mc.font, Component.literal(left),  cx - offset - mc.font.width(left), cy, color, true);
        ctx.text(mc.font, Component.literal(right), cx + offset,                        cy, color, true);
    }

    private static double rayEntryDistance(Vec3 origin, Vec3 dir, AABB box, double range) {
        double[] t = rayBoxSlabs(origin, dir, box);
        if (t == null || t[0] > range || t[1] < 0) return range;
        return Math.max(0.0, t[0]);
    }

    private static Double rayExitDistance(Vec3 origin, Vec3 dir, AABB box, double range) {
        double[] t = rayBoxSlabs(origin, dir, box);
        if (t == null || t[0] > range || t[1] < 0) return null;
        return Math.min(range, Math.max(0.0, t[1]));
    }

    private static double[] rayBoxSlabs(Vec3 origin, Vec3 dir, AABB box) {
        double tMin = Double.NEGATIVE_INFINITY;
        double tMax = Double.POSITIVE_INFINITY;

        for (int axis = 0; axis < 3; axis++) {
            double o, d, min, max;
            switch (axis) {
                case 0 -> { o = origin.x; d = dir.x; min = box.minX; max = box.maxX; }
                case 1 -> { o = origin.y; d = dir.y; min = box.minY; max = box.maxY; }
                default -> { o = origin.z; d = dir.z; min = box.minZ; max = box.maxZ; }
            }

            if (Math.abs(d) < 1e-9) {
                if (o < min || o > max) return null; // parallel and outside the slab
                continue;
            }

            double t1 = (min - o) / d;
            double t2 = (max - o) / d;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return null;
        }

        return new double[]{tMin, tMax};
    }
}
