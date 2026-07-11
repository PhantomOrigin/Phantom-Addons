package com.kuudrahelper.features.customisation.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SkullBlock;

import java.util.*;

public final class ItemCustomization {

    public enum ItemCategory {
        GLOBAL, HEAD, TOOL, WEAPON, BOW, FOOD, ARMOR, OTHER;
        public String displayName() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    public static class CustomCategory {
        public String               matchString = "";
        public ItemTransformSettings settings   = new ItemTransformSettings();
        public CustomCategory() {}
        public CustomCategory(String match) { matchString = match; settings.enabled = true; }
    }

    private static final Map<ItemCategory, ItemTransformSettings> builtinSettings =
            new EnumMap<>(ItemCategory.class);
    private static final List<CustomCategory> customCategories = new ArrayList<>();

    static {
        for (ItemCategory cat : ItemCategory.values())
            builtinSettings.put(cat, new ItemTransformSettings());
        builtinSettings.get(ItemCategory.GLOBAL).enabled = true;
    }

    private ItemCustomization() {}

    public static ItemTransformSettings getBuiltinSettings(ItemCategory cat) {
        return builtinSettings.get(cat);
    }

    public static List<CustomCategory> getCustomCategories() { return customCategories; }

    public static void addCustomCategory(String match) {
        if (match == null || match.isBlank()) return;
        customCategories.add(new CustomCategory(match.trim()));
    }

    public static void removeCustomCategory(int index) {
        if (index >= 0 && index < customCategories.size()) customCategories.remove(index);
    }

    public static ItemTransformSettings resolveSettings(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        for (CustomCategory cc : customCategories)
            if (cc.settings.enabled && !cc.matchString.isBlank()
                    && name.contains(cc.matchString.toLowerCase(Locale.ROOT)))
                return cc.settings;
        ItemCategory type = getItemCategory(stack);
        if (type != ItemCategory.GLOBAL) {
            ItemTransformSettings ts = builtinSettings.get(type);
            if (ts.enabled) return ts;
        }
        ItemTransformSettings global = builtinSettings.get(ItemCategory.GLOBAL);
        return global.enabled ? global : null;
    }

    public static ItemCategory getItemCategory(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemCategory.OTHER;
        Item item = stack.getItem();
        if (item instanceof BlockItem bi && bi.getBlock() instanceof SkullBlock)
            return ItemCategory.HEAD;
        if (item instanceof BowItem || item instanceof CrossbowItem)
            return ItemCategory.BOW;
        String id = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (id.endsWith("_sword") || id.endsWith("_axe"))
            return ItemCategory.WEAPON;
        if (id.endsWith("_pickaxe") || id.endsWith("_shovel") || id.endsWith("_hoe"))
            return ItemCategory.TOOL;
        if (stack.get(DataComponents.TOOL) != null)
            return ItemCategory.TOOL;
        if (stack.get(DataComponents.EQUIPPABLE) != null)
            return ItemCategory.ARMOR;
        if (id.endsWith("_helmet") || id.endsWith("_chestplate")
                || id.endsWith("_leggings") || id.endsWith("_boots"))
            return ItemCategory.ARMOR;
        if (stack.get(DataComponents.FOOD) != null)
            return ItemCategory.FOOD;
        return ItemCategory.OTHER;
    }

    public static void loadFrom(Map<String, ItemTransformSettings> cats,
                                List<CustomCategory> custom) {
        if (cats != null)
            for (ItemCategory cat : ItemCategory.values()) {
                ItemTransformSettings s = cats.get(cat.name());
                if (s != null) builtinSettings.put(cat, s);
            }
        customCategories.clear();
        if (custom != null) customCategories.addAll(custom);
    }

    public static Map<String, ItemTransformSettings> serialiseBuiltin() {
        Map<String, ItemTransformSettings> map = new LinkedHashMap<>();
        for (ItemCategory cat : ItemCategory.values())
            map.put(cat.name(), builtinSettings.get(cat));
        return map;
    }

    public static List<CustomCategory> serialiseCustom() {
        return Collections.unmodifiableList(customCategories);
    }
}
