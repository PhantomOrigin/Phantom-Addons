package com.phantomaddons.features.abilities;

import com.google.gson.Gson;
import com.phantomaddons.PhantomAddons;
import com.phantomaddons.PhantomConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class AbilityData {

    public static final String WEAPON = "WEAPON", ARMOR = "ARMOR", PET = "PET", ITEM = "ITEM";

    public static final String DETECT_USE = "use", DETECT_CHAT = "chat", DETECT_COLOR = "color";

    public static final class Ability {
        public String id = "", name = "", type = WEAPON, item = "", itemName = "", icon = "minecraft:paper";
        public String detect = DETECT_USE, chat;
        public String activeStart, activeCancel;
        public double cooldown, active;
        public List<String> items;
        transient Pattern chatPattern;
        transient boolean chatBroken;
        transient Pattern startPattern, cancelPattern;

        public String typeLabel() {
            return switch (type) {
                case ARMOR -> "Armor";
                case PET -> "Pet";
                case ITEM -> "Item";
                default -> "Weapon";
            };
        }

        public String itemsText() {
            if (items == null || items.size() < 2) return itemName;
            return String.join(", ", items);
        }

        Pattern activeStartPattern() {
            if (activeStart == null) return null;
            if (startPattern == null) {
                try { startPattern = Pattern.compile(activeStart); }
                catch (RuntimeException e) { activeStart = null; }
            }
            return startPattern;
        }

        Pattern activeCancelPattern() {
            if (activeCancel == null) return null;
            if (cancelPattern == null) {
                try { cancelPattern = Pattern.compile(activeCancel); }
                catch (RuntimeException e) { activeCancel = null; }
            }
            return cancelPattern;
        }

        Pattern chatPattern() {
            if (chat == null || chatBroken) return null;
            if (chatPattern == null) {
                try { chatPattern = Pattern.compile(chat); }
                catch (RuntimeException e) { chatBroken = true; }
            }
            return chatPattern;
        }
    }

    private static final class File { List<Ability> abilities = new ArrayList<>(); }

    private static List<Ability> bundled;
    private static List<Ability> merged;
    private static Map<String, Ability> byId;
    private static Map<String, List<Ability>> byName;

    private AbilityData() {}

    private static synchronized void load() {
        if (merged != null) return;
        List<Ability> out = new ArrayList<>();
        if (bundled == null) {
            bundled = new ArrayList<>();
            try (InputStream in = AbilityData.class.getResourceAsStream("/assets/phantomaddons/ability_cooldowns.json")) {
                if (in != null) {
                    File f = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), File.class);
                    if (f != null && f.abilities != null) bundled.addAll(f.abilities);
                }
            } catch (Exception e) {
                PhantomAddons.LOGGER.warn("[PhantomAddons] Could not read the ability list: {}", e.toString());
            }
        }
        out.addAll(bundled);
        for (var e : PhantomConfig.abilities().learned.entrySet()) {
            var l = e.getValue();
            Ability a = new Ability();
            a.id = e.getKey();
            a.name = l.name; a.type = l.type; a.item = l.item; a.itemName = l.itemName; a.icon = l.icon;
            a.cooldown = l.cooldown; a.active = l.active;
            a.detect = DETECT_USE;
            out.add(a);
        }
        Map<String, Ability> ids = new HashMap<>();
        Map<String, List<Ability>> names = new HashMap<>();
        for (Ability a : out) {
            ids.put(a.id, a);
            names.computeIfAbsent(a.name.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(a);
        }
        byId = ids;
        byName = names;
        merged = out;
    }

    public static synchronized void invalidate() { merged = null; }

    public static List<Ability> all() { load(); return merged; }

    public static Ability byId(String id) { load(); return byId.get(id); }

    public static List<Ability> byName(String name) {
        load();
        return byName.getOrDefault(name.toLowerCase(Locale.ROOT), List.of());
    }

    public static ItemStack icon(Ability a) {
        String id = a.icon == null ? "" : a.icon;
        String path = id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
        path = switch (path) { // the item repo still uses the old names
            case "skull" -> "player_head";
            case "yellow_flower" -> "dandelion";
            case "double_plant" -> "sunflower";
            case "fireworks" -> "firework_rocket";
            case "golden_horse_armor" -> "golden_horse_armor";
            default -> path;
        };
        try {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path.toLowerCase(Locale.ROOT)));
            if (item != null && item != Items.AIR) return new ItemStack(item);
        } catch (RuntimeException ignored) {}
        return new ItemStack(Items.PAPER);
    }
}
