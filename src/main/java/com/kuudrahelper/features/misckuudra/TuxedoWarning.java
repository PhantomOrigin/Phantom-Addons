package com.kuudrahelper.features.misckuudra;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.utils.KuudraTierDetector;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class TuxedoWarning {

    private TuxedoWarning() {}

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("phantomaddons", "tuxedo_warning"),
                (ctx, tickCounter) -> render(ctx));
    }

    private static void render(net.minecraft.client.gui.GuiGraphicsExtractor ctx) {
        if (!KuudraConfig.isTuxedoWarningEnabled()) return;
        if (!KuudraTierDetector.isInKuudraHollow()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.font == null) return;
        if (!isWearingTuxedo(mc)) return;

        String text = "§eWEARING TUXEDO!";
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int tw = mc.font.width(text);
        ctx.text(mc.font, Component.literal(text), (screenW - tw) / 2, screenH / 4, 0xFFFFFFFF, true);
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
