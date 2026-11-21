package com.phantomaddons.features.abilities;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.abilities.AbilityData.Ability;
import com.phantomaddons.utils.TextUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AbilityCooldowns {

    public static final class Timer {
        public final Ability ability;
        public final ItemStack icon;
        public final long start, cooldownMs, activeMs;
        volatile long activeFrom;
        Timer(Ability ability, ItemStack icon, long start, long cooldownMs, long activeMs) {
            this.ability = ability; this.icon = icon; this.start = start;
            this.cooldownMs = cooldownMs; this.activeMs = activeMs;
            this.activeFrom = ability.activeStart != null ? -1 : start;
        }
        public long cooldownLeft(long now) { return Math.max(0, start + cooldownMs - now); }
        public long activeLeft(long now)   { return activeFrom < 0 ? 0 : Math.max(0, activeFrom + activeMs - now); }
    }

    private static final Pattern MANA_USE = Pattern.compile("-[\\d,.]+\\s*Mana\\s*\\(([^()]+)\\)");
    private static final long SAME_USE_MS = 2500;
    private static boolean fromActionBar;

    private static final Map<String, Timer> timers = new LinkedHashMap<>();
    private static final Map<String, Long> lastStart = new LinkedHashMap<>();

    private AbilityCooldowns() {}

    public static void register() {
        IslandTracker.register();
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!active()) return;
            String clean = TextUtil.stripColor(message.getString());
            if (overlay) onActionBar(clean); else onChat(clean);
        });
        ClientTickEvents.END_CLIENT_TICK.register(AbilityCooldowns::tick);
    }

    private static boolean active() {
        var s = PhantomConfig.abilities();
        return s.enabled && !s.selected.isEmpty();
    }

    // ── What is running ───────────────────────────────────────────────────────

    public static List<Timer> running() {
        List<Timer> out = new ArrayList<>();
        var s = PhantomConfig.abilities();
        if (!s.enabled) return out;
        long now = System.currentTimeMillis();
        synchronized (timers) {
            timers.values().removeIf(t -> t.cooldownLeft(now) <= 0 && t.activeLeft(now) <= 0);
            for (Timer t : timers.values())
                if (s.isSelected(t.ability.id) && IslandTracker.allowedAnywhere(s, t.ability.id)) out.add(t);
        }
        return out;
    }

    public static void clear() {
        synchronized (timers) { timers.clear(); lastStart.clear(); }
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) { clear(); return; }
        if (PhantomConfig.abilities().enabled && ++scanTick >= 40) {
            scanTick = 0;
            learnFromInventory(mc);
        }
        if (!active()) return;
        watchArmorColor(mc);
    }

    private static int scanTick = 35;

    private static void learnFromInventory(Minecraft mc) {
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            for (LoreAbilities.Parsed p : LoreAbilities.parse(stack)) {
                if (p.cooldown() <= 0 || !(p.rightClick() || p.leftClick())) continue;
                String name = ALIASES.getOrDefault(p.name().toLowerCase(Locale.ROOT), p.name());
                if (AbilityData.byName(name).isEmpty()) learn(name, stack, p);
            }
        }
    }

    private static final java.util.Set<String> reported = new java.util.HashSet<>();

    public static void onUse(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if (hand != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        clicked(player, false);
    }

    public static void onAttack(net.minecraft.world.entity.player.Player player) {
        clicked(player, true);
    }

    private static void clicked(net.minecraft.world.entity.player.Player player, boolean left) {
        if (!active()) return;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return;
        List<LoreAbilities.Parsed> found = LoreAbilities.parse(held);
        String id = skyblockId(held);
        if (!id.isEmpty() && reported.add(id)) {
            StringBuilder sb = new StringBuilder();
            for (LoreAbilities.Parsed p : found)
                sb.append(p.name()).append(p.rightClick() ? " (right click, " : p.leftClick() ? " (left click, " : " (other, ")
                        .append(p.cooldown() > 0 ? p.cooldown() + "s" : "no cooldown line").append(") ");
            com.phantomaddons.PhantomAddons.LOGGER.info("[PhantomAddons] Click with {}: {}", id,
                    sb.length() == 0 ? "no ability found in its tooltip" : sb.toString().trim());
        }
        for (LoreAbilities.Parsed p : found) {
            if (!(left ? p.leftClick() : p.rightClick())) continue;
            if (p.sneak() && !player.isShiftKeyDown()) continue;
            start(p.name(), held, p);
        }
    }

    // ── Armor that changes colour (Reaper) ────────────────────────────────────

    private static final EquipmentSlot[] ARMOR_SLOTS =
            { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private static final Map<EquipmentSlot, Object[]> worn = new java.util.EnumMap<>(EquipmentSlot.class);

    private static boolean redder(int rgb) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        return r >= 100 && r > g * 2 && r > b * 2;
    }

    private static void watchArmorColor(Minecraft mc) {
        var s = PhantomConfig.abilities();
        List<Ability> watching = new ArrayList<>();
        for (String id : s.selected) {
            Ability a = AbilityData.byId(id);
            if (a != null && AbilityData.DETECT_COLOR.equals(a.detect)) watching.add(a);
        }
        if (watching.isEmpty()) { worn.clear(); return; }

        boolean fired = false;
        String firedItem = "";
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack piece = mc.player.getItemBySlot(slot);
            var dye = piece.isEmpty() ? null : piece.get(DataComponents.DYED_COLOR);
            if (dye == null) { worn.remove(slot); continue; }
            String id = skyblockId(piece);
            String key = id + "|" + skyblockUuid(piece);
            int rgb = dye.rgb() & 0xFFFFFF;
            Object[] before = worn.get(slot);
            boolean same = before != null && before[0].equals(key);
            if (same && (Integer) before[1] != rgb)
                com.phantomaddons.PhantomAddons.LOGGER.info("[PhantomAddons] {} changed colour {} -> {}", id,
                        String.format("#%06X", (Integer) before[1]), String.format("#%06X", rgb));
            if (same && !redder((Integer) before[1]) && redder(rgb)) {
                fired = true;
                firedItem = id;
            }
            worn.put(slot, new Object[]{ key, rgb });
        }
        if (!fired) return;
        for (Ability a : watching) {
            int cut = a.item.lastIndexOf('_');
            String set = cut > 0 ? a.item.substring(0, cut + 1) : a.item;
            if (!firedItem.startsWith(set)) continue;
            Found f = locate(mc, a.name);
            startAbility(a, f != null ? f.stack() : wornPiece(mc, set), f != null ? f.lore() : null);
        }
    }

    private static ItemStack wornPiece(Minecraft mc, String set) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack piece = mc.player.getItemBySlot(slot);
            if (skyblockId(piece).startsWith(set)) return piece;
        }
        return null;
    }

    private record Found(ItemStack stack, LoreAbilities.Parsed lore) {}

    private static Found locate(Minecraft mc, String name) {
        if (mc.player == null) return null;
        for (ItemStack candidate : carried(mc))
            for (LoreAbilities.Parsed p : LoreAbilities.parse(candidate))
                if (p.name().equalsIgnoreCase(name)) return new Found(candidate, p);
        return null;
    }

    // ── Ways of noticing a use ────────────────────────────────────────────────

    private static void onActionBar(String clean) {
        watchActiveStart(clean);
        Matcher m = MANA_USE.matcher(clean);
        fromActionBar = true;
        try {
            while (m.find()) start(m.group(1).trim(), null, null);
        } finally {
            fromActionBar = false;
        }
    }

    private static void watchActiveStart(String clean) {
        long now = System.currentTimeMillis();
        synchronized (timers) {
            for (Timer t : timers.values()) {
                Pattern begin = t.ability.activeStartPattern();
                if (begin == null || t.cooldownLeft(now) <= 0) continue;
                Pattern cancel = t.ability.activeCancelPattern();
                if (cancel != null && cancel.matcher(clean).find()) t.activeFrom = 0; // over: nothing left to show
                else if (t.activeFrom < 0 && begin.matcher(clean).find()) t.activeFrom = now;
            }
        }
    }

    private static void onChat(String clean) {
        var s = PhantomConfig.abilities();
        for (String id : new ArrayList<>(s.selected)) {
            Ability a = AbilityData.byId(id);
            if (a == null) continue;
            Pattern p = a.chatPattern();
            if (p == null || !p.matcher(clean).find()) continue;
            Found f = locate(Minecraft.getInstance(), a.name);
            startAbility(a, f != null ? f.stack() : null, f != null ? f.lore() : null);
        }
    }

    // ── Starting a timer ──────────────────────────────────────────────────────

    private static final Map<String, String> ALIASES = Map.of("wither impact", "Wither Shield");
    private static final Map<String, Double> ALIAS_COOLDOWN = Map.of("wither impact", 5.0);

    private static void start(String name, ItemStack held, LoreAbilities.Parsed fromLore) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var s = PhantomConfig.abilities();

        ItemStack source = held;
        LoreAbilities.Parsed lore = fromLore;
        if (lore == null) {
            Found f = locate(mc, name);
            if (f != null) { lore = f.lore(); source = f.stack(); }
        }

        double cooldownOverride = ALIAS_COOLDOWN.getOrDefault(name.toLowerCase(Locale.ROOT), 0.0);
        name = ALIASES.getOrDefault(name.toLowerCase(Locale.ROOT), name);
        List<Ability> candidates = new ArrayList<>();
        for (Ability a : AbilityData.byName(name)) if (s.isSelected(a.id)) candidates.add(a);
        if (candidates.isEmpty()) {
            if (AbilityData.byName(name).isEmpty() && lore != null && lore.cooldown() > 0) learn(name, source, lore);
            return;
        }
        startAbility(pick(candidates, source, lore), source, lore, cooldownOverride);
    }

    private static Ability pick(List<Ability> candidates, ItemStack source, LoreAbilities.Parsed lore) {
        if (candidates.size() == 1) return candidates.get(0);
        String id = skyblockId(source);
        for (Ability a : candidates) if (!id.isEmpty() && a.item.equals(id)) return a;
        boolean armor = source != null && !source.isEmpty() && isArmorSlot(source);
        for (Ability a : candidates) {
            boolean isArmor = AbilityData.ARMOR.equals(a.type) || AbilityData.PET.equals(a.type);
            if (isArmor == armor) return a;
        }
        return candidates.get(0);
    }

    private static void startAbility(Ability a, ItemStack source, LoreAbilities.Parsed lore) {
        startAbility(a, source, lore, 0);
    }

    private static void startAbility(Ability a, ItemStack source, LoreAbilities.Parsed lore, double cooldownOverride) {
        var s = PhantomConfig.abilities();
        if (!IslandTracker.allowedAnywhere(s, a.id)) return;
        long now = System.currentTimeMillis();
        synchronized (timers) {
            Timer running = timers.get(a.id);
            if (running != null && running.cooldownLeft(now) > 0) return;
            Long last = lastStart.get(a.id);
            if (fromActionBar && last != null && now - last < SAME_USE_MS) return;
            double cd = cooldownOverride > 0 ? cooldownOverride : lore != null && lore.cooldown() > 0 ? lore.cooldown() : a.cooldown;
            double act = lore != null && lore.active() > 0 ? lore.active() : a.active;
            if (cd <= 0) return;
            ItemStack icon = source != null && !source.isEmpty() ? source.copyWithCount(1) : AbilityData.icon(a);
            timers.put(a.id, new Timer(a, icon, now, Math.round(cd * 1000), Math.round(act * 1000)));
            lastStart.put(a.id, now);
            com.phantomaddons.PhantomAddons.LOGGER.info("[PhantomAddons] Ability cooldown started: {} ({}s)", a.name, cd);
        }
    }

    private static void learn(String name, ItemStack source, LoreAbilities.Parsed lore) {
        var s = PhantomConfig.abilities();
        String id = "learned|" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        if (s.learned.containsKey(id)) return;
        var l = new AbilitySettings.Learned();
        l.name = name;
        l.type = source != null && isArmorSlot(source) ? AbilityData.ARMOR : AbilityData.WEAPON;
        l.item = skyblockId(source);
        l.itemName = TextUtil.stripColor(source.getHoverName().getString()).replaceAll("[✪⚚\\uE000-\\uF8FF\\u2776-\\u2793]", "").trim();
        if (AbilityData.ARMOR.equals(l.type)) l.itemName = l.itemName.replaceAll("\\s+(Helmet|Chestplate|Leggings|Boots|Pants|Cap|Tunic|Hat|Hood)$", "");
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(source.getItem());
        l.icon = key != null ? key.toString() : "minecraft:paper";
        l.cooldown = lore.cooldown();
        l.active = lore.active();
        s.learned.put(id, l);
        AbilityData.invalidate();
        PhantomConfig.save();
    }

    // ── Items the player has on ───────────────────────────────────────────────

    private static List<ItemStack> carried(Minecraft mc) {
        List<ItemStack> out = new ArrayList<>();
        out.add(mc.player.getMainHandItem());
        for (EquipmentSlot slot : new EquipmentSlot[]{ EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET })
            out.add(mc.player.getItemBySlot(slot));
        return out;
    }

    private static boolean isArmorSlot(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        for (EquipmentSlot slot : new EquipmentSlot[]{ EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET })
            if (mc.player.getItemBySlot(slot) == stack) return true;
        return false;
    }

    static String skyblockUuid(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getStringOr("uuid", "");
    }

    static String skyblockId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getStringOr("id", "");
    }
}
