package com.phantomaddons.features.miscskyblock;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;

public final class FishingHookDebugTracker {

    private static boolean enabled = false;
    private static FishingHook tracked = null;
    private static Vec3 lastPos = null;
    private static int tickIndex = 0;

    private static Vec3 entryPos  = null;
    private static Vec3 lowestPos = null;
    private static Vec3 restPos   = null;

    private FishingHookDebugTracker() {}

    public static boolean isEnabled() { return enabled; }
    public static Vec3 getEntryPos()  { return entryPos; }
    public static Vec3 getLowestPos() { return lowestPos; }
    public static Vec3 getRestPos()   { return restPos; }

    public static boolean toggle() {
        enabled = !enabled;
        tracked = null;
        lastPos = null;
        tickIndex = 0;
        return enabled;
    }

    public static void tick(Minecraft mc) {
        if (!enabled || mc.player == null) return;

        FishingHook real = mc.player.fishing;
        if (real == null) {
            if (tracked != null) {
                mc.player.sendSystemMessage(Component.literal(
                        "§f[PhantomAddons]§r §7Fish hook debug: gone after " + tickIndex + " ticks."));
                printComparison(mc);
                tracked = null;
            }
            return;
        }

        if (real != tracked) {
            tracked = real;
            lastPos = real.position();
            tickIndex = 0;
            entryPos = null;
            lowestPos = null;
            restPos = null;
            mc.player.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,
                    "§f[PhantomAddons]§r §7Fish hook debug: tick 0 pos=(%.3f, %.3f, %.3f)",
                    lastPos.x, lastPos.y, lastPos.z)));
            return;
        }

        tickIndex++;
        Vec3 pos = real.position();
        Vec3 delta = pos.subtract(lastPos);
        lastPos = pos;

        if (mc.level != null && !mc.level.getFluidState(real.blockPosition()).isEmpty()) {
            if (entryPos == null) { entryPos = pos; lowestPos = pos; }
            else if (pos.y < lowestPos.y) { lowestPos = pos; }
        }
        restPos = pos;

        mc.player.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,
                "§f[PhantomAddons]§r §7tick %d pos=(%.3f, %.3f, %.3f) delta=(%.4f, %.4f, %.4f) |delta|=%.4f",
                tickIndex, pos.x, pos.y, pos.z, delta.x, delta.y, delta.z, delta.length())));
    }

    private static void printComparison(Minecraft mc) {
        printPoint(mc, "Entry",   entryPos,  PredictedBobber.getDebugEntryPos());
        printPoint(mc, "Lowest",  lowestPos, PredictedBobber.getDebugLowestPos());
        printPoint(mc, "Rest",    restPos,   PredictedBobber.getDebugRestPos());
    }

    private static void printPoint(Minecraft mc, String label, Vec3 real, Vec3 ghost) {
        if (real == null && ghost == null) return;
        String realStr = real == null ? "n/a" : String.format(java.util.Locale.ROOT,
                "(%.3f, %.3f, %.3f)", real.x, real.y, real.z);
        String ghostStr = ghost == null ? "n/a" : String.format(java.util.Locale.ROOT,
                "(%.3f, %.3f, %.3f)", ghost.x, ghost.y, ghost.z);
        String distStr = (real != null && ghost != null)
                ? String.format(java.util.Locale.ROOT, " §7|off %.3f|", real.distanceTo(ghost))
                : "";
        mc.player.sendSystemMessage(Component.literal(
                "§f[PhantomAddons]§r §7" + label + ": real=" + realStr + " ghost=" + ghostStr + distStr));
    }
}
