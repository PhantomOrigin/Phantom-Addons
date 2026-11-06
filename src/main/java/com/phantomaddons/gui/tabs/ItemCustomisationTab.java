package com.phantomaddons.gui.tabs;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.customisation.items.ItemCustomization;
import com.phantomaddons.features.customisation.items.ItemTransformSettings;
import com.phantomaddons.gui.AddCategoryFeature;
import com.phantomaddons.gui.CustomCategoryRow;
import com.phantomaddons.gui.Group;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.RangeSlider;
import com.phantomaddons.gui.Toggle;

import java.util.List;

public final class ItemCustomisationTab {

    private ItemCustomisationTab() {}

    public static void build(MenuBuilder m) {
        Group ic = m.group("Item Customisation", PhantomConfig::isItemCustomizationEnabled, PhantomConfig::setItemCustomizationEnabled)
                .withTooltip("Changes how items look in your hand.");

        for (ItemCustomization.ItemCategory cat : ItemCustomization.ItemCategory.values()) {
            ItemTransformSettings st = ItemCustomization.getBuiltinSettings(cat);
            String label = cat == ItemCustomization.ItemCategory.GLOBAL ? "Global" : cat.displayName();
            Group typeGroup = m.group(label, ic, () -> st.enabled, v -> { st.enabled = v; PhantomConfig.save(); })
                    .withTooltip(cat == ItemCustomization.ItemCategory.GLOBAL
                            ? "Changes how every held item looks."
                            : "Changes how held items of this type look.");
            addTransformSettings(typeGroup, st);
            ic.add(typeGroup);
        }

        Group custom = m.section("Custom Categories", ic)
                .withTooltip("Lets you add your own item types to change.");
        custom.add(new AddCategoryFeature(() -> m.screen().rebuild())
                .withTooltip("Adds a category for items with a name you choose."));
        List<ItemCustomization.CustomCategory> cats = ItemCustomization.getCustomCategories();
        for (int i = 0; i < cats.size(); i++) {
            final int idx = i;
            final ItemCustomization.CustomCategory cc = cats.get(i);
            Group ccGroup = m.section("\"" + cc.matchString + "\"", custom)
                    .withTooltip("Changes how items with this name look.");
            ccGroup.add(new CustomCategoryRow(cc.matchString, () -> {
                ItemCustomization.removeCustomCategory(idx);
                PhantomConfig.save();
                m.screen().rebuild();
            })
                    .withTooltip("Shows this category's name and lets you remove it."));
            addTransformSettings(ccGroup, cc.settings);
            custom.add(ccGroup);
        }
        ic.add(custom);
        m.add(ic);
    }

    private static void addTransformSettings(Group g, ItemTransformSettings s) {
        g.add(new RangeSlider("pos X",     -0.5f,  0.5f,  "%.3f",  () -> s.posX,       v -> { s.posX       = v; PhantomConfig.save(); })
                .withTooltip("Moves the item left or right."));
        g.add(new RangeSlider("pos Y",     -0.5f,  0.5f,  "%.3f",  () -> s.posY,       v -> { s.posY       = v; PhantomConfig.save(); })
                .withTooltip("Moves the item up or down."));
        g.add(new RangeSlider("pos Z",     -0.5f,  0.5f,  "%.3f",  () -> s.posZ,       v -> { s.posZ       = v; PhantomConfig.save(); })
                .withTooltip("Moves the item closer or further away."));
        g.add(new RangeSlider("rot X",     -180f,  180f,  "%.0f°", () -> s.rotX,       v -> { s.rotX       = v; PhantomConfig.save(); })
                .withTooltip("Tilts the item forwards or backwards."));
        g.add(new RangeSlider("rot Y",     -180f,  180f,  "%.0f°", () -> s.rotY,       v -> { s.rotY       = v; PhantomConfig.save(); })
                .withTooltip("Turns the item left or right."));
        g.add(new RangeSlider("rot Z",     -180f,  180f,  "%.0f°", () -> s.rotZ,       v -> { s.rotZ       = v; PhantomConfig.save(); })
                .withTooltip("Rolls the item sideways."));
        g.add(new RangeSlider("scale",     0.25f,  3.0f,  "%.2f×", () -> s.scale,      v -> { s.scale      = v; PhantomConfig.save(); })
                .withTooltip("Makes the item bigger or smaller."));
        g.add(new RangeSlider("swing spd", 0.25f,  3.0f,  "%.2f×", () -> s.swingSpeed, v -> { s.swingSpeed = v; PhantomConfig.save(); })
                .withTooltip("Changes how fast the item swings."));
        g.add(new RangeSlider("proximity", -1.0f,  1.0f,  "%.3f",  () -> s.proximity,  v -> { s.proximity  = v; PhantomConfig.save(); })
                .withTooltip("Changes how close the item is to the screen."));
        g.add(new Toggle("No Equip Animation", () -> s.noEquipAnimation, v -> { s.noEquipAnimation = v; PhantomConfig.save(); })
                .withTooltip("Removes the animation when you switch items."));
        g.add(new Toggle("In Place Swing",     () -> s.inPlaceSwing,     v -> { s.inPlaceSwing     = v; PhantomConfig.save(); })
                .withTooltip("Makes the item swing without moving around."));
        g.add(new Toggle("Static Position",    () -> s.staticPosition,   v -> { s.staticPosition   = v; PhantomConfig.save(); })
                .withTooltip("Keeps the item still while you move."));
    }
}
