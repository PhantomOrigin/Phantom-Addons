package com.phantomaddons.features.miscskyblock.preventplacing;

import com.phantomaddons.utils.TextUtil;
import com.phantomaddons.PhantomConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PreventPlacingPlayerHeads {

    private static final Pattern AREA_PATTERN = Pattern.compile("^(?:Area|Dungeon): ([\\w ']+)$");

    private PreventPlacingPlayerHeads() {}

    public static boolean shouldCancel(Player player, ItemStack stack) {
        if (!PhantomConfig.isPreventPlacingPlayerHeadsEnabled()) return false;
        if (!stack.is(Items.PLAYER_HEAD)) return false;
        if (PhantomConfig.isPreventPlacingPlayerHeadsExceptGarden() && isInGarden()) return false;
        return hasRightClickLore(stack);
    }

    private static boolean hasRightClickLore(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        for (var line : lore.lines()) {
            String text = line.getString();
            if (text.contains("RIGHT CLICK") || text.contains("Right-click")) return true;
        }
        return false;
    }

    private static boolean isInGarden() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        Scoreboard sb = mc.level.getScoreboard();
        Objective sidebar = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return false;
        for (var entry : sb.listPlayerScores(sidebar)) {
            String owner = entry.owner();
            PlayerTeam team = sb.getPlayerTeam(owner);
            String raw;
            if (team != null) {
                raw = team.getPlayerPrefix().getString() + owner + team.getPlayerSuffix().getString();
            } else {
                raw = owner;
            }
            String clean = TextUtil.stripColor(raw).trim();
            Matcher m = AREA_PATTERN.matcher(clean);
            if (m.matches() && m.group(1).toLowerCase().equals("garden")) return true;
        }
        return false;
    }
}
