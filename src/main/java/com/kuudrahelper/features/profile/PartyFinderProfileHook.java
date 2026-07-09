package com.kuudrahelper.features.profile;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class PartyFinderProfileHook {

    private static final Pattern MEMBERS_HEADER = Pattern.compile("members:", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEMBER_LINE = Pattern.compile("^(\\S+)\\s*\\(\\d+\\)$");

    private static boolean wasTriggered = false;

    private PartyFinderProfileHook() {}

    public static void reset() {
        wasTriggered = false;
    }

    public static void checkShiftHover(AbstractContainerScreen<?> screen, Slot hoveredSlot, Minecraft mc) {
        String title = strip(screen.getTitle().getString());
        if (!title.equalsIgnoreCase("Party Finder") || hoveredSlot == null || !hoveredSlot.hasItem()) {
            wasTriggered = false;
            return;
        }

        if (!isShiftDown(mc)) { wasTriggered = false; return; }
        if (wasTriggered) return;

        List<String> members = extractMembers(hoveredSlot.getItem(), mc);
        if (members.isEmpty()) return;

        wasTriggered = true;
        mc.execute(() -> mc.setScreen(new KuudraProfileScreen(members)));
    }

    private static boolean isShiftDown(Minecraft mc) {
        var window = mc.getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static List<String> extractMembers(ItemStack stack, Minecraft mc) {
        List<String> result = new ArrayList<>();
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return result;

        List<String> lines = new ArrayList<>();
        for (FormattedText line : lore.lines()) lines.add(strip(line.getString()).trim());

        int membersIdx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (MEMBERS_HEADER.matcher(lines.get(i)).find()) { membersIdx = i; break; }
        }
        if (membersIdx < 0) return result;

        String selfName = mc.player != null ? mc.player.getScoreboardName() : null;

        for (int i = membersIdx + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.equalsIgnoreCase("Empty")) continue;

            var matcher = MEMBER_LINE.matcher(line);
            if (!matcher.matches()) break;

            String name = matcher.group(1);
            if (selfName != null && name.equalsIgnoreCase(selfName)) continue;
            result.add(name);
        }
        return result;
    }

    private static String strip(String s) {
        return s.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}
