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

    private static final double THROW_RANGE   = 14.0;

    private static final double DISPLAY_RANGE = 18.0;

    private static final int BRACKET_MAX_OFFSET_PX = 60;

    static final AABB[] KUUDRA_LOGGED_HITBOXES = {
        new AABB(-112.7999997138977, 11.0, -84.7999997138977, -97.2000002861023, 26.59999942779541, -69.2000002861023),
        new AABB(-82.7999997138977, 12.0, -111.7999997138977, -67.2000002861023, 27.59999942779541, -96.2000002861023),
        new AABB(-105.7999997138977, 13.0, -143.7999997138977, -90.2000002861023, 28.59999942779541, -128.2000002861023),
        new AABB(-139.7999997138977, 9.0, -112.7999997138977, -124.2000002861023, 24.59999942779541, -97.2000002861023),
    };

    private static final double DETECTION_INFLATE = 3.0;

    private static final AABB[] KUUDRA_STATIONARY_HITBOXES = buildDetectionHitboxes();

    private static AABB[] buildDetectionHitboxes() {
        AABB[] result = new AABB[KUUDRA_LOGGED_HITBOXES.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = KUUDRA_LOGGED_HITBOXES[i].inflate(DETECTION_INFLATE);
        }
        return result;
    }

    private static volatile boolean hidden = false;

    private static volatile int     aimedHitboxIndex  = -1;
    private static volatile boolean aimedInThrowRange = false;
    private static volatile boolean aimedWallBlocking = false;

    private static volatile boolean throwNowSoundArmed = true;

    private BoneTimingAssist() {}

    public static int     getAimedHitboxIndex()  { return aimedHitboxIndex; }
    public static boolean isAimedInThrowRange()   { return aimedInThrowRange; }
    public static boolean isAimedWallBlocking()   { return aimedWallBlocking; }

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
        HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath("phantomaddons", "bone_timing_assist"),
                (ctx, tickCounter) -> render(ctx));
    }

    private static void render(GuiGraphicsExtractor ctx) {
        if (KuudraPhaseTracker.getPhase() != KuudraPhaseTracker.Phase.BOSS) { clearAimState(); return; }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.font == null) { clearAimState(); return; }

        Vec3 eye  = mc.player.getEyePosition(1.0f);
        Vec3 look = mc.player.getLookAngle();

        int aimedIndex = findAimedStationaryHitboxIndex(eye, look);
        if (aimedIndex < 0) { clearAimState(); return; }
        AABB kuudraBox = KUUDRA_LOGGED_HITBOXES[aimedIndex];

        double distToKuudra = rayEntryDistance(eye, look, kuudraBox, DISPLAY_RANGE)
                + KuudraConfig.getBoneTimingAssistOffset();
        Double distToWall = wallHitDistance(mc, eye, look, DISPLAY_RANGE);

        aimedHitboxIndex  = aimedIndex;
        aimedInThrowRange = distToKuudra <= THROW_RANGE;
        aimedWallBlocking = distToWall != null && distToWall <= THROW_RANGE;

        if (!KuudraConfig.isBoneTimingAssistEnabled() || hidden) { throwNowSoundArmed = true; return; }

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int cx = screenW / 2;
        int cy = screenH / 2;

        drawBracketPair(ctx, mc, cx, cy, distToKuudra, THROW_RANGE, 0xFFFFFFFF);
        if (distToWall != null) {
            drawBracketPair(ctx, mc, cx, cy, distToWall, THROW_RANGE, 0xFFFF3333);
        }

        if (aimedInThrowRange) {
            if (throwNowSoundArmed) {
                KuudraConfig.playNotificationSound(KuudraConfig.SOUND_BONE_THROW_NOW);
                throwNowSoundArmed = false;
            }
        } else {
            throwNowSoundArmed = true;
        }
    }

    private static void clearAimState() {
        aimedHitboxIndex  = -1;
        aimedInThrowRange = false;
        aimedWallBlocking = false;
        throwNowSoundArmed = true;
    }

    private static Double wallHitDistance(Minecraft mc, Vec3 origin, Vec3 dir, double range) {
        if (mc.level == null) return null;
        Vec3 far = origin.add(dir.scale(range));

        BlockHitResult hit = mc.level.clip(new ClipContext(
                origin, far,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player));
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return Math.min(range, origin.distanceTo(hit.getLocation()));
    }

    private static int findAimedStationaryHitboxIndex(Vec3 eye, Vec3 look) {
        for (int i = 0; i < KUUDRA_STATIONARY_HITBOXES.length; i++) {
            if (rayExitDistance(eye, look, KUUDRA_STATIONARY_HITBOXES[i], DISPLAY_RANGE) != null) {
                return i;
            }
        }
        return -1;
    }

    private static void drawBracketPair(GuiGraphicsExtractor ctx, Minecraft mc,
                                        int cx, int cy, double distance, double closeAt, int color) {
        double frac   = Math.max(0.0, Math.min(1.0, (distance - closeAt) / (DISPLAY_RANGE - closeAt)));
        int    offset = (int) Math.round(BRACKET_MAX_OFFSET_PX * frac);
        if (offset <= 0) return;

        String left  = "[";
        String right = "]";
        ctx.text(mc.font, Component.literal(left),  cx - offset - mc.font.width(left), cy, color, true);
        ctx.text(mc.font, Component.literal(right), cx + offset,                        cy, color, true);
    }

    private static double rayEntryDistance(Vec3 origin, Vec3 dir, AABB box, double range) {
        double[] t = rayBoxSlabs(origin, dir, box);
        if (t == null || t[0] > range || t[1] < 0) return range;
        return t[0];
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
