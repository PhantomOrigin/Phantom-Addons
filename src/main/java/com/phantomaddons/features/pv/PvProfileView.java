package com.phantomaddons.features.pv;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.phantomaddons.PhantomAddons;
import com.phantomaddons.PhantomConfig;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PvProfileView {

    public record Skill(String name, double xp, PvLeveling.Level level, PvLeveling.Level overflow, int max, boolean capBlocked) {
        public boolean atMax() { return level.level() >= max; }

        public PvLeveling.Level shown() {
            return overflow != null && atMax() && com.phantomaddons.PhantomConfig.isPvOverflowSkills()
                    && overflow.level() > level.level() ? overflow : level;
        }
    }

    static int maxLevel(String skill) {
        return switch (skill) {
            case "Fishing", "Alchemy", "Carpentry", "Hunting" -> 50;
            case "Foraging" -> 57;
            case "Runecrafting", "Social" -> 25;
            default -> 60;
        };
    }

    public record Pet(String uniqueId, String type, String name, String rarity, PvLeveling.Level level, double exp,
                      String heldItem, int candyUsed, String skin, boolean active) {}

    public record WardrobeSet(int slotNumber, ItemStack[] items) {}

    public record Loadout(int id, String name, int armorSet, int equipmentSet, String powerStone,
                          int tuningSlot, Map<String, Integer> tuning, Pet pet, boolean active,
                          int hotmPreset, int hotfPreset) {}

    private static int treePreset(JsonObject loadout, String... words) {
        for (var e : loadout.entrySet()) {
            if (!e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isNumber()) continue;
            String k = e.getKey().toLowerCase(Locale.ROOT);
            for (String w : words) {
                if (!k.contains(w)) continue;
                int n = e.getValue().getAsInt();
                return n >= 1 && n <= 5 ? n : -1; // presets are numbered 1-5
            }
        }
        return -1;
    }

    public record Backpack(int index, ItemStack icon, List<ItemStack> items) {}

    public final PvData.Profile profile;
    public final JsonObject member;

    // ── Loading stages ────────────────────────────────────────────────────────

    private final java.util.concurrent.CompletableFuture<Void> basicStage, fullStage;

    public boolean basicReady() { return basicStage.isDone(); }
    public boolean itemsReady() { return fullStage.isDone(); }
    public java.util.concurrent.CompletableFuture<Void> loaded() { return fullStage.handle((ok, err) -> null); }

    // ── Storage (full stage) ──────────────────────────────────────────────────
    public volatile List<ItemStack> enderChest = List.of();
    public volatile List<Backpack> backpacks = List.of();
    public volatile List<ItemStack> vault = List.of();
    public volatile ItemStack[] riftArmor = empty(4);
    public volatile ItemStack[] riftEquipment = empty(4);
    public volatile List<ItemStack> riftItems = List.of();
    public volatile List<ItemStack> riftInventory = List.of();
    public volatile List<ItemStack> riftEnderChest = List.of();
    public final boolean riftInventoryApi;
    public volatile Map<String, net.minecraft.nbt.CompoundTag> farmingToolkit = Map.of();
    public volatile List<ItemStack> allItems = List.of();
    public volatile List<ItemStack> mainInventoryItems = List.of();

    public volatile PvAnalysis analysis;

    // ── Stats ─────────────────────────────────────────────────────────────────
    public final double purse;
    public final long bank;
    public final double personalBank;
    public final int fairySouls;
    public final long firstJoin;
    public final int skyblockLevel;
    public final boolean skillsApi;
    public final List<Skill> skills = new ArrayList<>();
    public final double skillAverage;

    // ── Gear (basic stage, except the equipped-set numbers) ───────────────────
    public final boolean inventoryApi;
    public volatile List<ItemStack> inventory = List.of();
    public volatile ItemStack[] armor = empty(4);
    public volatile ItemStack[] equipment = empty(4);
    public volatile List<ItemStack> accessories = List.of();
    public volatile List<Integer> accessoryColors = List.of();
    public volatile List<WardrobeSet> wardrobe = List.of();
    public final int wardrobeEquipped;
    public volatile List<WardrobeSet> equipmentWardrobe = List.of();
    public final int equipmentWardrobeEquipped;
    public final List<Pet> pets = new ArrayList<>();
    public final Pet activePet;
    public final List<Loadout> loadouts = new ArrayList<>();
    public final int magicalPower;
    public final String selectedPower;
    public final Map<String, Integer> tuning;

    private static final String[][] SKILLS = {
            { "Farming", "SKILL_FARMING" }, { "Mining", "SKILL_MINING" }, { "Combat", "SKILL_COMBAT" },
            { "Foraging", "SKILL_FORAGING" }, { "Fishing", "SKILL_FISHING" }, { "Enchanting", "SKILL_ENCHANTING" },
            { "Alchemy", "SKILL_ALCHEMY" }, { "Taming", "SKILL_TAMING" }, { "Carpentry", "SKILL_CARPENTRY" },
            { "Hunting", "SKILL_HUNTING" }, { "Runecrafting", "SKILL_RUNECRAFTING" }, { "Social", "SKILL_SOCIAL" },
    };
    private static final List<String> COSMETIC_SKILLS = List.of("Hunting", "Runecrafting", "Social");
    private static final String[] RARITIES = { "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC" };
    private static final String[] ARMOR_KEYS = { "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS" };
    private static final String[] EQUIPMENT_KEYS = { "EQUIPMENT_SLOT_1", "EQUIPMENT_SLOT_2", "EQUIPMENT_SLOT_3", "EQUIPMENT_SLOT_4" };

    PvProfileView(PvData.Profile profile) {
        this.profile = profile;
        JsonObject m = profile.member() != null ? profile.member() : new JsonObject();
        this.member = m;

        purse         = PvJson.num(m, -1, "currencies", "coin_purse");
        bank          = profile.bank();
        personalBank  = PvJson.num(m, -1, "profile", "bank_account");
        fairySouls    = (int) PvJson.num(m, -1, "fairy_soul", "total_collected");
        firstJoin     = (long) PvJson.num(m, -1, "profile", "first_join");
        skyblockLevel = (int) (PvJson.num(m, 0, "leveling", "experience") / 100);

        // ── Skills ──
        JsonObject xp = PvJson.obj(m, "player_data", "experience");
        skillsApi = xp != null;
        double avgSum = 0;
        int avgCount = 0;
        for (String[] s : SKILLS) {
            JsonElement v = xp != null ? xp.get(s[1]) : null;
            if (v == null && s[0].equals("Hunting")) continue;
            double amount = v != null ? v.getAsDouble() : 0;
            PvLeveling.Level lvl = switch (s[0]) {
                case "Runecrafting" -> PvLeveling.skill(amount, PvLeveling.RUNECRAFTING, 25);
                case "Social"       -> PvLeveling.skill(amount, PvLeveling.SOCIAL, 25);
                default             -> PvLeveling.skill(amount, PvLeveling.SKILL, skillCap(s[0], m));
            };
            boolean ownTable = s[0].equals("Runecrafting") || s[0].equals("Social");
            int max = maxLevel(s[0]);
            boolean blocked = !ownTable && lvl.level() < max
                    && PvLeveling.skill(amount, PvLeveling.SKILL, max).level() > lvl.level();
            skills.add(new Skill(s[0], amount, lvl, ownTable ? null : PvLeveling.overflow(amount), max, blocked));
            if (!COSMETIC_SKILLS.contains(s[0])) { avgSum += lvl.level(); avgCount++; }
        }
        skillAverage = avgCount > 0 ? avgSum / avgCount : 0;

        JsonObject inv = PvJson.obj(m, "inventory");
        inventoryApi = inv != null && inv.has("inv_contents");
        riftInventoryApi = PvJson.obj(m, "rift", "inventory") != null;
        JsonObject loadout = PvJson.obj(m, "loadout");
        JsonObject armorSets = PvJson.obj(loadout, "armor");
        JsonObject equipSets = PvJson.obj(loadout, "equipment");
        wardrobeEquipped = armorSets != null ? (int) PvJson.num(armorSets, -1, "equipped_set")
                : (int) PvJson.num(inv, -1, "wardrobe_equipped_slot");
        equipmentWardrobeEquipped = (int) PvJson.num(equipSets, -1, "equipped_set");

        // ── Accessory bag / power / tuning ──
        magicalPower  = (int) PvJson.num(m, -1, "accessory_bag_storage", "highest_magical_power");
        selectedPower = PvJson.str(m, null, "accessory_bag_storage", "selected_power");
        JsonObject tuningRoot = PvJson.obj(m, "accessory_bag_storage", "tuning");
        tuning = readTuning(PvJson.obj(tuningRoot, "slot_0"));

        // ── Pets ──
        Pet active = null;
        JsonElement petList = PvJson.path(m, "pets_data", "pets");
        if (petList != null && petList.isJsonArray()) {
            for (JsonElement e : petList.getAsJsonArray()) {
                Pet p = parsePet(e.getAsJsonObject());
                pets.add(p);
                if (p.active()) active = p;
            }
        }
        pets.sort(Comparator.comparing(Pet::active).reversed()
                .thenComparing(p -> -rarityIndex(p.rarity()))
                .thenComparing(p -> -p.exp()));
        activePet = active;

        // ── Loadouts ──
        JsonObject saved = PvJson.obj(loadout, "loadouts");
        if (saved != null) {
            List<String> keys = new ArrayList<>(saved.keySet());
            keys.sort(Comparator.comparingInt(PvProfileView::parseIntSafe));
            for (String key : keys) {
                JsonObject o = PvJson.obj(saved, key);
                if (o == null) continue;
                int armorSet = (int) PvJson.num(o, -1, "armor_set_id");
                int equipSet = (int) PvJson.num(o, -1, "equipment_set_id");
                int tuneSlot = (int) PvJson.num(o, -1, "tuning_points_slot");
                String petId = PvJson.str(o, null, "pet");
                Pet pet = null;
                for (Pet p : pets) if (p.uniqueId() != null && p.uniqueId().equals(petId)) { pet = p; break; }
                loadouts.add(new Loadout(
                        (int) PvJson.num(o, parseIntSafe(key), "id"),
                        PvJson.str(o, "Loadout " + key, "name"),
                        armorSet, equipSet,
                        PvJson.str(o, null, "power_stone"),
                        tuneSlot,
                        tuneSlot >= 0 ? readTuning(PvJson.obj(tuningRoot, "slot_" + tuneSlot)) : Map.of(),
                        pet,
                        armorSet == wardrobeEquipped && equipSet == equipmentWardrobeEquipped,
                        treePreset(o, "mining_core_selected_slot", "mountain", "hotm", "mining"),
                        treePreset(o, "foraging_core_selected_slot", "forest", "hotf", "foraging")));
            }
        }

        basicStage = java.util.concurrent.CompletableFuture.runAsync(() -> loadBasic(m), PvItems.EXECUTOR);
        fullStage = basicStage.thenRunAsync(() -> loadStorage(m), PvItems.EXECUTOR);
        fullStage.whenComplete((ok, err) -> {
            if (err != null) PhantomAddons.LOGGER.error("[PV] failed to load items for {}", profile.cuteName(), err);
        });

        if (PhantomConfig.isDeveloperFeaturesEnabled()) {
            PhantomAddons.LOGGER.info("[PV] {} loadout shape: {}", profile.cuteName(), PvJson.shape(loadout, 2));
        }
    }

    private static <T> List<T> parallel(List<java.util.function.Supplier<T>> jobs) {
        List<java.util.concurrent.CompletableFuture<T>> fs = new ArrayList<>();
        for (var job : jobs) fs.add(java.util.concurrent.CompletableFuture.supplyAsync(job));
        List<T> out = new ArrayList<>();
        for (var fut : fs) out.add(fut.join());
        return out;
    }

    private void loadBasic(JsonObject m) {
        JsonObject inv = PvJson.obj(m, "inventory");
        JsonObject loadout = PvJson.obj(m, "loadout");
        JsonObject armorSets = PvJson.obj(loadout, "armor");
        JsonObject equipSets = PvJson.obj(loadout, "equipment");
        List<java.util.function.Supplier<List<ItemStack>>> jobs = List.of(
                () -> inv != null ? PvItems.decode(inv.get("inv_contents")) : List.of(),
                () -> inv != null ? PvItems.decode(inv.get("inv_armor")) : List.of(),
                () -> inv != null ? PvItems.decode(inv.get("equipment_contents")) : List.of(),
                () -> inv != null ? PvItems.decode(PvJson.path(inv, "bag_contents", "talisman_bag")) : List.of());
        List<List<ItemStack>> got = parallel(jobs);

        ItemStack[] newArmor = empty(4), newEquip = empty(4);
        List<ItemStack> worn = got.get(1), eq = got.get(2);
        for (int i = 0; i < Math.min(4, worn.size()); i++) newArmor[3 - i] = worn.get(i); // API order is boots first
        for (int i = 0; i < Math.min(4, eq.size()); i++) newEquip[i] = eq.get(i);
        List<ItemStack> acc = new ArrayList<>();
        List<Integer> accColors = new ArrayList<>();
        for (ItemStack s : got.get(3)) {
            if (s.isEmpty()) continue;
            acc.add(s);
            accColors.add(PvItems.rarityColor(s));
        }

        List<WardrobeSet> sets = new ArrayList<>(), equipSetList = new ArrayList<>();
        if (armorSets != null) readSets(armorSets, ARMOR_KEYS, sets);
        else if (inv != null && inv.has("wardrobe_contents")) readLegacyWardrobe(PvItems.decode(inv.get("wardrobe_contents")), sets);
        fillEquipped(sets, wardrobeEquipped, newArmor);
        if (equipSets != null) readSets(equipSets, EQUIPMENT_KEYS, equipSetList);
        fillEquipped(equipSetList, equipmentWardrobeEquipped, newEquip);

        inventory = got.get(0);
        armor = newArmor;
        equipment = newEquip;
        accessoryColors = accColors;
        accessories = acc;
        wardrobe = sets;
        equipmentWardrobe = equipSetList;
    }

    private void loadStorage(JsonObject m) {
        JsonObject inv = PvJson.obj(m, "inventory");
        List<java.util.function.Supplier<List<ItemStack>>> jobs = new ArrayList<>();
        jobs.add(() -> inv != null ? PvItems.decode(inv.get("ender_chest_contents")) : List.of());
        jobs.add(() -> inv != null ? PvItems.decode(inv.get("personal_vault_contents")) : List.of());
        JsonObject packs = PvJson.obj(inv, "backpack_contents");
        JsonObject icons = PvJson.obj(inv, "backpack_icons");
        List<String> packKeys = new ArrayList<>(packs != null ? packs.keySet() : java.util.Set.of());
        packKeys.sort(Comparator.comparingInt(PvProfileView::parseIntSafe));
        for (String k : packKeys) jobs.add(() -> PvItems.decode(packs.get(k)));
        JsonObject riftInv = PvJson.obj(m, "rift", "inventory");
        for (String key : new String[] { "inv_armor", "equipment_contents", "inv_contents", "ender_chest_contents" })
            jobs.add(() -> riftInv != null ? PvItems.decode(riftInv.get(key)) : List.of());
        List<List<ItemStack>> got = parallel(jobs);

        List<ItemStack> ec = got.get(0), vlt = got.get(1);
        List<Backpack> bps = new ArrayList<>();
        for (int i = 0; i < packKeys.size(); i++) {
            String k = packKeys.get(i);
            ItemStack icon = icons != null && icons.has(k) ? PvItems.decodeSingle(icons.get(k)) : ItemStack.EMPTY;
            bps.add(new Backpack(parseIntSafe(k), icon, got.get(2 + i)));
        }
        int r0 = 2 + packKeys.size();
        ItemStack[] rArmor = empty(4), rEquip = empty(4);
        List<ItemStack> worn = got.get(r0), eq = got.get(r0 + 1);
        for (int i = 0; i < Math.min(4, worn.size()); i++) rArmor[3 - i] = worn.get(i);
        for (int i = 0; i < Math.min(4, eq.size()); i++) rEquip[i] = eq.get(i);
        List<ItemStack> rift = new ArrayList<>();
        addNonEmpty(rift, got.get(r0 + 2));
        addNonEmpty(rift, got.get(r0 + 3));
        addNonEmpty(rift, List.of(rArmor));
        addNonEmpty(rift, List.of(rEquip));

        Map<String, net.minecraft.nbt.CompoundTag> kit = new LinkedHashMap<>();
        JsonObject toolkit = PvJson.obj(m, "garden_player_data", "farming_toolkit");
        if (toolkit != null) {
            for (var e : toolkit.entrySet()) {
                if (!e.getValue().isJsonArray() || e.getValue().getAsJsonArray().isEmpty()) continue;
                net.minecraft.nbt.CompoundTag attrs = PvItems.decodeCompound(e.getValue().getAsJsonArray().get(0));
                if (attrs != null && !attrs.getStringOr("id", "").isEmpty()) kit.put(e.getKey(), attrs);
            }
        }

        List<ItemStack> main = new ArrayList<>(), all = new ArrayList<>();
        addNonEmpty(main, inventory);
        addNonEmpty(all, inventory);
        addNonEmpty(all, List.of(armor));
        addNonEmpty(all, List.of(equipment));
        addNonEmpty(all, ec);
        addNonEmpty(all, vlt);
        for (Backpack b : bps) addNonEmpty(all, b.items());
        for (WardrobeSet s : wardrobe) addNonEmpty(all, List.of(s.items()));
        for (WardrobeSet s : equipmentWardrobe) addNonEmpty(all, List.of(s.items()));
        JsonElement hunting = PvJson.path(m, "foraging", "hunting_toolkit");
        if (hunting != null && hunting.isJsonObject()) {
            for (var e : hunting.getAsJsonObject().entrySet()) {
                if (!e.getValue().isJsonArray()) continue;
                for (JsonElement blob : e.getValue().getAsJsonArray()) addNonEmpty(all, PvItems.decode(blob));
            }
        }

        enderChest = ec;
        vault = vlt;
        backpacks = bps;
        riftArmor = rArmor;
        riftEquipment = rEquip;
        riftItems = rift;
        riftInventory = got.get(r0 + 2);
        riftEnderChest = got.get(r0 + 3);
        farmingToolkit = kit;
        mainInventoryItems = main;
        allItems = all;
        analysis = new PvAnalysis(this);
    }

    private static void addNonEmpty(List<ItemStack> out, java.util.Collection<ItemStack> items) {
        for (ItemStack s : items) if (s != null && !s.isEmpty()) out.add(s);
    }

    public WardrobeSet armorSet(int number)     { return findSet(wardrobe, number); }
    public WardrobeSet equipmentSet(int number) { return findSet(equipmentWardrobe, number); }

    private static WardrobeSet findSet(List<WardrobeSet> sets, int number) {
        for (WardrobeSet s : sets) if (s.slotNumber() == number) return s;
        return null;
    }

    private static ItemStack[] empty(int n) {
        ItemStack[] a = new ItemStack[n];
        java.util.Arrays.fill(a, ItemStack.EMPTY);
        return a;
    }

    private static void fillEquipped(List<WardrobeSet> sets, int equipped, ItemStack[] worn) {
        WardrobeSet set = findSet(sets, equipped);
        if (set == null) return;
        for (int i = 0; i < Math.min(worn.length, set.items().length); i++)
            if (set.items()[i].isEmpty()) set.items()[i] = worn[i];
    }

    private static Map<String, Integer> readTuning(JsonObject slot) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (slot == null) return out;
        for (var e : slot.entrySet()) {
            if (e.getKey().equals("purchase_ts") || !e.getValue().isJsonPrimitive()) continue;
            int v = e.getValue().getAsInt();
            if (v > 0) out.put(e.getKey(), v);
        }
        return out;
    }

    private static int skillCap(String skill, JsonObject m) {
        return switch (skill) {
            case "Farming" -> 50 + (int) PvJson.num(m, 0, "jacobs_contest", "perks", "farming_level_cap");
            case "Taming" -> {
                JsonElement sac = PvJson.path(m, "pets_data", "pet_care", "pet_types_sacrificed");
                yield 50 + (sac != null && sac.isJsonArray() ? Math.min(10, sac.getAsJsonArray().size()) : 0);
            }
            case "Foraging" -> 50 + (int) PvJson.num(m, 0, "player_data", "experience", "SKILL_FORAGING_extra_level_cap");
            case "Fishing", "Alchemy", "Carpentry", "Hunting" -> 50;
            default -> 60;
        };
    }

    private static void readSets(JsonObject sets, String[] order, List<WardrobeSet> out) {
        List<String> keys = new ArrayList<>(sets.keySet());
        keys.sort(Comparator.comparingInt(PvProfileView::parseIntSafe));
        for (String key : keys) {
            JsonObject slot = PvJson.obj(sets, key);
            if (slot == null) continue;
            ItemStack[] items = empty(order.length);
            for (int i = 0; i < order.length; i++) {
                if (slot.has(order[i])) items[i] = PvItems.decodeSingle(slot.get(order[i]));
            }
            out.add(new WardrobeSet((int) PvJson.num(slot, parseIntSafe(key), "id"), items));
        }
    }

    private static void readLegacyWardrobe(List<ItemStack> items, List<WardrobeSet> wardrobe) {
        int pages = items.size() / 36;
        for (int page = 0; page < pages; page++) {
            for (int col = 0; col < 9; col++) {
                ItemStack[] set = new ItemStack[4];
                for (int row = 0; row < 4; row++) set[row] = items.get(page * 36 + row * 9 + col);
                wardrobe.add(new WardrobeSet(page * 9 + col + 1, set));
            }
        }
    }

    private static Pet parsePet(JsonObject p) {
        String type = PvJson.str(p, "UNKNOWN", "type");
        String rarity = PvJson.str(p, "COMMON", "tier");
        String held = PvJson.str(p, null, "heldItem");
        if ("PET_ITEM_TIER_BOOST".equals(held)) {
            int idx = rarityIndex(rarity);
            if (idx < RARITIES.length - 1) rarity = RARITIES[idx + 1];
        }
        double exp = PvJson.num(p, 0, "exp");
        int maxLevel = maxPetLevel(type);
        return new Pet(PvJson.str(p, null, "uniqueId"), type, prettify(type), rarity,
                PvLeveling.pet(exp, rarity, maxLevel), exp, held,
                (int) PvJson.num(p, 0, "candyUsed"), PvJson.str(p, null, "skin"),
                p.has("active") && p.get("active").getAsBoolean());
    }

    static int maxPetLevel(String type) {
        return type.equals("GOLDEN_DRAGON") || type.equals("JADE_DRAGON") || type.equals("ROSE_DRAGON") ? 200 : 100;
    }

    static int rarityIndex(String rarity) {
        for (int i = 0; i < RARITIES.length; i++) if (RARITIES[i].equals(rarity)) return i;
        return 0;
    }

    public static String rarityColor(String rarity) {
        return switch (rarity) {
            case "UNCOMMON" -> "§a";
            case "RARE" -> "§9";
            case "EPIC" -> "§5";
            case "LEGENDARY" -> "§6";
            case "MYTHIC" -> "§d";
            case "DIVINE" -> "§b";
            case "SPECIAL", "VERY_SPECIAL" -> "§c";
            default -> "§f";
        };
    }

    public static String prettify(String id) {
        StringBuilder sb = new StringBuilder();
        for (String w : id.toLowerCase(Locale.ROOT).split("_")) {
            if (w.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return Integer.MAX_VALUE; }
    }
}
