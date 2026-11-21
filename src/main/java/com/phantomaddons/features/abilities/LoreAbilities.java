package com.phantomaddons.features.abilities;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LoreAbilities {

    record Parsed(String name, double cooldown, double active, boolean mana, boolean rightClick, boolean leftClick, boolean sneak) {}

    private static final Pattern HEADER = Pattern.compile(
            "^[^A-Za-z]*(?:Ability|Spirit Ability|Item Ability|Full Set Bonus|Piece Bonus|Tier Bonus):\\s*(.+)$"); // "⦾ Ability: ..." on newer items
    private static final Pattern TRIGGER = Pattern.compile("\\s+(?:RIGHT CLICK|LEFT CLICK|SNEAK|SHIFT|RIGHT|LEFT)(?:\\s.*)?$");
    private static final Pattern COOLDOWN = Pattern.compile("^Cooldown:\\s*([\\d.,]+)\\s*(minutes?|min|m)?(?=s|\\b|$).*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACTIVE = Pattern.compile("(?:for|Lasts)\\s+(\\d+(?:\\.\\d+)?)\\s*(?:s|sec|seconds?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNT = Pattern.compile("\\s*\\(\\d+/\\d+\\)\\s*$");

    private LoreAbilities() {}

    static List<String> lines(ItemStack stack) {
        List<String> out = new ArrayList<>();
        if (stack == null || stack.isEmpty()) return out;
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) lore.lines().forEach(c -> out.add(c.getString()));
        return out;
    }

    static List<Parsed> parse(ItemStack stack) {
        List<Parsed> out = new ArrayList<>();
        String name = null;
        boolean mana = false, right = false, left = false, sneak = false;
        StringBuilder text = new StringBuilder();
        for (String raw : lines(stack)) {
            String line = raw.trim();
            Matcher h = HEADER.matcher(line);
            if (h.matches()) {
                if (name != null) out.add(new Parsed(name, 0, 0, mana, right, left, sneak));
                String full = h.group(1);
                String bare = TRIGGER.matcher(full).replaceFirst("");
                String trigger = full.substring(bare.length());
                right = trigger.contains("RIGHT CLICK");
                left = trigger.contains("LEFT CLICK");
                sneak = trigger.contains("SNEAK") || trigger.contains("SHIFT");
                bare = COUNT.matcher(bare).replaceFirst("").trim();
                name = !bare.isEmpty() && Character.isLetter(bare.charAt(0)) ? bare : null;
                mana = false;
                text.setLength(0);
                continue;
            }
            if (name == null) continue;
            if (line.regionMatches(true, 0, "Mana Cost:", 0, 10)) mana = true;
            Matcher c = COOLDOWN.matcher(line);
            if (c.matches()) {
                double n;
                try { n = Double.parseDouble(c.group(1).replace(',', '.')); }
                catch (NumberFormatException e) { name = null; continue; }
                double seconds = c.group(2) != null ? n * 60 : n;
                if (seconds > 0) out.add(new Parsed(name, seconds, active(text.toString(), seconds), mana, right, left, sneak));
                name = null;
                continue;
            }
            if (!line.isEmpty()) text.append(line).append(' ');
        }
        if (name != null) out.add(new Parsed(name, 0, 0, mana, right, left, sneak));
        return out;
    }

    private static double active(String text, double cooldown) {
        double found = 0;
        Matcher m = ACTIVE.matcher(text);
        while (m.find()) found = Double.parseDouble(m.group(1));
        return found > 0 && found < cooldown ? found : 0;
    }
}
