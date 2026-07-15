package com.kuudrahelper.features.misckuudra;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.utils.KuudraTierDetector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class TuxedoWarning {

    private static boolean wasInHollow = false;

    private TuxedoWarning() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean inHollow = KuudraTierDetector.isInKuudraHollow();
            if (inHollow && !wasInHollow) {
                onEnterHollow();
            }
            wasInHollow = inHollow;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> wasInHollow = false);
    }

    private static void onEnterHollow() {
        if (!KuudraConfig.isTuxedoWarningEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (isWearingTuxedo(mc)) {
            NotificationHud.show("§eWEARING TUXEDO!", 5000);
        }
    }

    private static boolean isWearingTuxedo(Minecraft mc) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            if (stack.getHoverName().getString().toLowerCase().contains("tuxedo")) return true;
        }
        return false;
    }
}
