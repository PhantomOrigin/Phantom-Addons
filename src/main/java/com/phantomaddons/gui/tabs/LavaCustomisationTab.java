package com.phantomaddons.gui.tabs;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.gui.ColorSlider;
import com.phantomaddons.gui.Group;
import com.phantomaddons.gui.LavaPreview;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.Slider;
import com.phantomaddons.gui.Toggle;

public final class LavaCustomisationTab {

    private LavaCustomisationTab() {}

    public static void build(MenuBuilder m) {
        Group lava = m.group("Lava Tweaks", PhantomConfig::isLavaTweaksEnabled, PhantomConfig::setLavaTweaksEnabled)
                .withTooltip("Changes how lava looks.");
        lava.add(new Toggle("Replace with Water",
                PhantomConfig::isLavaAsWater, PhantomConfig::setLavaAsWater)
                .withTooltip("Makes lava look like water."));
        lava.add(new Slider("Opacity",
                PhantomConfig::getLavaOpacity, PhantomConfig::setLavaOpacity, "%")
                .withTooltip("How see-through lava is."));
        lava.add(new Toggle("Colour Override",
                PhantomConfig::isLavaColorOverride, PhantomConfig::setLavaColorOverride)
                .withTooltip("Lets you choose the colour of lava."));
        lava.add(new ColorSlider("Red", 16, 0xFF4444, PhantomConfig::getLavaColor, PhantomConfig::setLavaColor));
        lava.add(new ColorSlider("Green",  8, 0x44FF88, PhantomConfig::getLavaColor, PhantomConfig::setLavaColor));
        lava.add(new ColorSlider("Blue",  0, 0x4488FF, PhantomConfig::getLavaColor, PhantomConfig::setLavaColor));
        lava.add(new LavaPreview());
        m.add(lava);
    }
}
