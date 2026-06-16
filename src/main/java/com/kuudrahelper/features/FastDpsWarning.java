package com.kuudrahelper.features;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.features.NotificationHud;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class FastDpsWarning {

    private static final int THRESHOLD_TICKS = 66; // 3.3 seconds

    private static boolean active   = false;
    private static int     dpsTicks = 0;

    private FastDpsWarning() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (active) dpsTicks++;
        });
    }

    public static void onDpsStart() {
        active   = true;
        dpsTicks = 0;
    }

    public static void onDpsEnd() {
        active = false;

        if (dpsTicks == 0) return;
        if (dpsTicks >= THRESHOLD_TICKS) return;

        if (KuudraConfig.isFastDpsNotifyEnabled()) {
            NotificationHud.show("§cFAST DPS!", 3000);
        }

        if (!KuudraConfig.isFastDpsWarningEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        String duration = String.format("%.2fs", dpsTicks / 20.0);
        mc.getConnection().sendCommand(
                "pc [Phantom] DPS FAST! Dps Took: " + duration);

        dpsTicks = 0;
    }
}
