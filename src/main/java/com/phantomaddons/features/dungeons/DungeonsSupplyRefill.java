package com.phantomaddons.features.dungeons;

import com.phantomaddons.Edition;
import com.phantomaddons.PhantomConfig;
import com.phantomaddons.utils.KuudraTierDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.function.BooleanSupplier;

/*
This feature is excluded from the standard version of the mod
 */
public final class DungeonsSupplyRefill {

    private static final long CHECK_INTERVAL_MS = 5000L;
    private static final long SEND_SPACING_MS   = 1500L;
    private static final int  THRESHOLD         = 20;
    private static final int  TARGET            = 64;

    private record Supply(String skyblockId, String gfsId, BooleanSupplier enabled) {}

    private static final Supply[] SUPPLIES = {
            new Supply("INFLATABLE_JERRY", "inflatable_jerry", PhantomConfig::isAutoGfsJerryEnabled),
            new Supply("SPIRIT_LEAP",      "spirit_leap",      PhantomConfig::isAutoGfsSpiritLeapEnabled),
            new Supply("SUPERBOOM_TNT",    "superboom_tnt",    PhantomConfig::isAutoGfsSuperboomEnabled),
    };

    private static final ArrayDeque<String> pending = new ArrayDeque<>();
    private static long nextCheckMs = 0L;
    private static long nextSendMs  = 0L;

    private DungeonsSupplyRefill() {}

    public static void reset() {
        pending.clear();
        nextCheckMs = 0L;
        nextSendMs  = 0L;
    }

    public static void tick(Minecraft mc) {
        if (!Edition.CURRENT.fullFeatureSet) return; // not included in this edition
        if (mc.player == null || mc.getConnection() == null) return;
        if (!KuudraTierDetector.isInDungeon()) { pending.clear(); return; }

        long now = System.currentTimeMillis();

        if (!pending.isEmpty()) {
            if (now >= nextSendMs) {
                mc.getConnection().sendCommand(pending.poll());
                nextSendMs = now + SEND_SPACING_MS;
            }
            return;
        }

        if (now < nextCheckMs) return;
        nextCheckMs = now + CHECK_INTERVAL_MS;

        for (Supply s : SUPPLIES) {
            if (!s.enabled().getAsBoolean()) continue;
            int count = countOf(mc, s.skyblockId());
            if (count <= 0 || count >= THRESHOLD) continue;
            pending.add("gfs " + s.gfsId() + " " + (TARGET - count));
        }
    }

    private static int countOf(Minecraft mc, String skyblockId) {
        var inv = mc.player.getInventory();
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            var data = stack.get(DataComponents.CUSTOM_DATA);
            if (data == null) continue;
            if (skyblockId.equals(data.copyTag().getStringOr("id", ""))) total += stack.getCount();
        }
        return total;
    }
}
