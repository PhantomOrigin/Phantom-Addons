package com.phantomaddons.features.pv;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PvGear {

    private PvGear() {}

    private static final Pattern STAT_LINE = Pattern.compile("^([A-Za-z' ]+): ([+-]?[\\d,]+(?:\\.\\d+)?)");
    private static final String[] TYPES = {
            "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "NECKLACE", "CLOAK", "BELT", "GLOVES", "BRACELET",
            "FISHING ROD", "FISHING WEAPON", "DRILL", "PICKAXE", "GAUNTLET", "AXE", "HOE", "SWORD", "BOW",
            "DUNGEON SWORD", "DUNGEON BOW", "LONGSWORD", "WAND", "ACCESSORY", "HATCESSORY", "SHOVEL", "SHEARS" };

    public static CompoundTag data(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d != null ? d.copyTag() : new CompoundTag();
    }

    public static String id(ItemStack stack) {
        return stack.isEmpty() ? "" : data(stack).getStringOr("id", "");
    }

    public static String reforge(ItemStack stack) {
        return data(stack).getStringOr("modifier", "").toLowerCase(Locale.ROOT);
    }

    public static List<String> lore(ItemStack stack) {
        List<String> out = new ArrayList<>();
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return out;
        for (Component c : lore.lines()) {
            String s = ChatFormatting.stripFormatting(c.getString());
            if (s != null) out.add(s.trim());
        }
        return out;
    }

    private static final Map<ItemStack, Map<String, Double>> STATS =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public static double stat(ItemStack stack, String name) {
        return STATS.computeIfAbsent(stack, PvGear::parseStats).getOrDefault(name.toLowerCase(Locale.ROOT), 0.0);
    }

    private static Map<String, Double> parseStats(ItemStack stack) {
        Map<String, Double> out = new java.util.HashMap<>();
        for (String line : lore(stack)) {
            Matcher m = STAT_LINE.matcher(line);
            if (!m.find()) continue;
            try {
                out.putIfAbsent(m.group(1).trim().toLowerCase(Locale.ROOT), Double.parseDouble(m.group(2).replace(",", "")));
            } catch (NumberFormatException ignored) {}
        }
        return out;
    }

    private static final Pattern RARITY_LINE = Pattern.compile(
            "\\b(?:COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL|ULTIMATE|ADMIN)\\b([A-Z ]*)");

    public static String type(ItemStack stack) {
        List<String> lore = lore(stack);
        for (int i = lore.size() - 1; i >= 0; i--) {
            Matcher m = RARITY_LINE.matcher(lore.get(i));
            if (!m.find()) continue;
            String rest = m.group(1);
            String best = "";
            for (String t : TYPES) if (rest.contains(t) && t.length() > best.length()) best = t;
            if (!best.isEmpty()) return best;
        }
        return "";
    }

    public static int rarityRgb(ItemStack stack) {
        return PvItems.rarityColor(stack);
    }

    public static int armorSlot(ItemStack stack) {
        return switch (type(stack)) {
            case "HELMET" -> 0;
            case "CHESTPLATE" -> 1;
            case "LEGGINGS" -> 2;
            case "BOOTS" -> 3;
            default -> -1;
        };
    }

    public static int equipSlot(ItemStack stack) {
        return switch (type(stack)) {
            case "NECKLACE" -> 0;
            case "CLOAK" -> 1;
            case "BELT" -> 2;
            case "GLOVES", "BRACELET" -> 3;
            default -> -1;
        };
    }

    public static int tierIndex(ItemStack stack, List<String> tiersBestFirst) {
        String id = id(stack);
        for (int i = 0; i < tiersBestFirst.size(); i++) if (id.startsWith(tiersBestFirst.get(i))) return i;
        return -1;
    }

    public static ItemStack[] bestArmor(List<ItemStack> items, ToDoubleFunction<ItemStack> score) {
        ItemStack[] best = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
        double[] bestScore = new double[4];
        for (ItemStack s : items) {
            int slot = armorSlot(s);
            if (slot < 0) continue;
            double sc = score.applyAsDouble(s);
            if (sc > bestScore[slot]) { bestScore[slot] = sc; best[slot] = s; }
        }
        return best;
    }

    public static ItemStack[] bestEquipment(List<ItemStack> items, ToDoubleFunction<ItemStack> score) {
        ItemStack[] best = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
        double[] bestScore = new double[4];
        for (ItemStack s : items) {
            int slot = equipSlot(s);
            if (slot < 0) continue;
            double sc = score.applyAsDouble(s);
            if (sc > bestScore[slot]) { bestScore[slot] = sc; best[slot] = s; }
        }
        return best;
    }

    public static List<ItemStack> bestByTier(List<ItemStack> items, List<String> tiersBestFirst, int max) {
        List<ItemStack> matches = new ArrayList<>();
        for (ItemStack s : items) if (tierIndex(s, tiersBestFirst) >= 0) matches.add(s);
        matches.sort(Comparator.comparingInt(s -> tierIndex(s, tiersBestFirst)));
        if (matches.isEmpty()) return matches;
        int top = tierIndex(matches.get(0), tiersBestFirst);
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : matches) {
            if (tierIndex(s, tiersBestFirst) != top || out.size() >= max) break;
            out.add(s);
        }
        return out;
    }

    public static ItemStack best(List<ItemStack> items, ToDoubleFunction<ItemStack> score) {
        ItemStack best = ItemStack.EMPTY;
        double bestScore = 0;
        for (ItemStack s : items) {
            double sc = score.applyAsDouble(s);
            if (sc > bestScore) { bestScore = sc; best = s; }
        }
        return best;
    }

    public static double statSum(ItemStack s, Map<String, Double> weights) {
        double total = 0;
        for (var e : weights.entrySet()) total += stat(s, e.getKey()) * e.getValue();
        return total;
    }
}
