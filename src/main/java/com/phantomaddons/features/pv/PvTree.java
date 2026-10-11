package com.phantomaddons.features.pv;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class PvTree {

    private PvTree() {}

    private static final Set<String> RESERVED = Set.of("name", "x", "y", "maxLevel", "powder", "item", "cost", "lore", "isAbility");

    static int render(PvPages.Ctx c, JsonObject layout, String treeKey, JsonObject nodes, String selectedAbility,
                      int potm, int hotm, int x, int y, int w, int h) {
        if (layout == null) {
            c.s().text(c.g(), "Loading tree layout...", x, y, PvTheme.dim(), 0.85f * c.es());
            return c.lh(c.es());
        }
        JsonObject perks = PvJson.obj(layout, treeKey, "perks");
        if (perks == null) return 0;
        List<String> prelude = new ArrayList<>();
        JsonElement pre = layout.get("prelude");
        if (pre != null && pre.isJsonArray()) for (JsonElement e : pre.getAsJsonArray()) prelude.add(e.getAsString());
        PvLisp lisp = new PvLisp(prelude);

        int cols = 1, rows = 1;
        for (var e : perks.entrySet()) {
            JsonObject p = e.getValue().getAsJsonObject();
            cols = Math.max(cols, p.get("x").getAsInt() + 1);
            rows = Math.max(rows, p.get("y").getAsInt() + 1);
        }
        int s = c.s().fitSlot(w / (cols * 1.15), h / (rows * 1.12));
        int g = Math.max(2, s / 6);
        int gridW = cols * (s + g) - g, gridH = rows * (s + g) - g;
        int gx = x + (w - gridW) / 2;

        for (var e : perks.entrySet()) {
            JsonObject p = e.getValue().getAsJsonObject();
            int px = p.get("x").getAsInt(), py = p.get("y").getAsInt();
            int max = p.has("maxLevel") ? p.get("maxLevel").getAsInt() : 1;
            int level = nodes != null && nodes.has(e.getKey()) && nodes.get(e.getKey()).isJsonPrimitive()
                    ? nodes.get(e.getKey()).getAsInt() : 0;
            Map<String, Object> vars = new HashMap<>();
            vars.put("level", (double) Math.max(level, 1));
            vars.put("level0", (double) level);
            vars.put("maxLevel", (double) max);
            vars.put("potm", (double) potm);
            vars.put("hotm", (double) hotm);

            ItemStack icon = new ItemStack(item(lisp.eval(PvJson.str(p, "", "item"), vars)));
            boolean selected = e.getKey().equals(selectedAbility);
            if (selected) icon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            String name = PvJson.str(p, e.getKey(), "name");
            icon.set(DataComponents.CUSTOM_NAME, Component.literal((level > 0 ? "§a" : "§c") + name));

            int sx = gx + px * (s + g), sy = y + py * (s + g);
            if (selected) com.phantomaddons.gui.GuiDraw.roundedFill(c.g(), sx - 1, sy - 1, sx + s + 1, sy + s + 1, PvTheme.accent(), 4);
            c.s().slotNoTooltip(c.g(), sx, sy, s, icon);
            if (level > 0 && max > 1) {
                float ls = Math.max(0.5f, s / 32f);
                c.g().nextStratum();
                c.s().text(c.g(), String.valueOf(level), sx + 2, sy + s - c.lh(ls) - 1, 0xFFFFFFFF, ls);
            }
            if (c.s().hovered(c.mx(), c.my(), sx, sy, s, s)) {
                List<String> lines = new ArrayList<>();
                lines.add((level > 0 ? "§a" : "§c") + name);
                lines.add("§7Level " + (level >= max ? "§a" : "§e") + level + "§7/" + max + (selected ? "  §6§lSELECTED" : ""));
                JsonArray lore = p.has("lore") && p.get("lore").isJsonArray() ? p.getAsJsonArray("lore") : new JsonArray();
                if (!lore.isEmpty()) lines.add("");
                Map<String, Object> condVars = new HashMap<>(vars);
                condVars.put("level", (double) level);
                for (JsonElement l : lore) {
                    if (l.isJsonPrimitive()) {
                        lines.add(fill(l.getAsString(), p, lisp, vars));
                    } else if (l.isJsonObject()) {
                        String cond = PvJson.str(l, null, "onlyIf");
                        Object ok = cond != null ? lisp.eval(cond, condVars) : 1.0;
                        if (ok instanceof Double d && d != 0) lines.add(fill(PvJson.str(l, "", "text"), p, lisp, vars));
                    }
                }
                c.s().tooltipLines(lines);
            }
        }
        return gridH;
    }

    private static String fill(String line, JsonObject perk, PvLisp lisp, Map<String, Object> vars) {
        if (!line.contains("{")) return line;
        String out = line;
        for (var e : perk.entrySet()) {
            if (RESERVED.contains(e.getKey()) || !e.getValue().isJsonPrimitive()) continue;
            String token = "{" + e.getKey() + "}";
            if (!out.contains(token)) continue;
            Object v = lisp.eval(e.getValue().getAsString(), vars);
            if (v != null) out = out.replace(token, PvLisp.format(v));
        }
        return out;
    }

    private static Item item(Object keyword) {
        if (keyword instanceof String s && s.startsWith(":")) {
            Item i = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(s.substring(1).toLowerCase(Locale.ROOT)));
            if (i != null && i != Items.AIR) return i;
        }
        return Items.EMERALD;
    }
}
