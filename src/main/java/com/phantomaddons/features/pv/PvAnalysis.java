package com.phantomaddons.features.pv;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PvAnalysis {

    // ── Slayer weapons (best tier first) ──────────────────────────────────────
    public record WeaponLine(String label, List<String> tiers) {}
    public record SlayerWeapons(String slayer, List<WeaponLine> lines, List<List<ItemStack>> found) {}

    static final Map<String, List<WeaponLine>> SLAYER_WEAPONS = new LinkedHashMap<>();
    static {
        SLAYER_WEAPONS.put("zombie", List.of(new WeaponLine("",
                List.of("REAPER_SCYTHE", "AXE_OF_THE_SHREDDED", "REAPER_SWORD", "REVENANT_SWORD"))));
        SLAYER_WEAPONS.put("spider", List.of(new WeaponLine("", List.of("SCORPION_FOIL", "RECLUSE_FANG"))));
        SLAYER_WEAPONS.put("wolf", List.of(new WeaponLine("", List.of("POOCH_SWORD", "SHAMAN_SWORD"))));
        SLAYER_WEAPONS.put("enderman", List.of(new WeaponLine("",
                List.of("ATOMSPLIT_KATANA", "VORPAL_KATANA", "VOIDEDGE_KATANA", "VOIDWALKER_KATANA"))));
        SLAYER_WEAPONS.put("blaze", List.of(
                new WeaponLine("", List.of("HEARTFIRE_DAGGER", "BURSTFIRE_DAGGER", "FIREDUST_DAGGER")),
                new WeaponLine("", List.of("HEARTMAW_DAGGER", "BURSTMAW_DAGGER", "MAWDUST_DAGGER"))));
        SLAYER_WEAPONS.put("vampire", List.of(new WeaponLine("", List.of("SILVER_LACED_KARAMBIT", "STEAK_STAKE"))));
    }
    public final Map<String, SlayerWeapons> slayerWeapons = new LinkedHashMap<>();

    // ── Kuudra ────────────────────────────────────────────────────────────────
    public record KuudraWeapon(String label, ItemStack stack, boolean partial) {
        KuudraWeapon(String label, ItemStack stack) { this(label, stack, false); }
    }
    public final List<KuudraWeapon> kuudraWeapons = new ArrayList<>();
    public PvProfileView.Pet dpsDragon, rendDragon;
    public record KuudraSet(String label, int slotNumber) {}
    public final List<KuudraSet> kuudraSets = new ArrayList<>();

    // ── Dungeons ──────────────────────────────────────────────────────────────
    final PvDungeonGear dungeonGear;

    // ── Mining ────────────────────────────────────────────────────────────────
    private record MiningSetRule(String[] names, String... prefixes) {}
    public record MiningSet(String name, ItemStack[] pieces) {}

    private static final List<MiningSetRule> MINING_SETS = List.of(
            new MiningSetRule(new String[] { "Divan's Armor" }, "DIVAN_"),
            new MiningSetRule(new String[] { "Sorrow Armor" }, "SORROW_"),
            new MiningSetRule(new String[] { "Yog Armor" }, "ARMOR_OF_YOG_"),
            new MiningSetRule(new String[] { "Glossy Mineral Armor", "Mineral Armor" }, "GLOSSY_MINERAL_", "MINERAL_"),
            new MiningSetRule(new String[] { "Glacite Armor" }, "GLACITE_"),
            new MiningSetRule(new String[] { "Flamebreaker Armor" }, "FLAME_BREAKER_"),
            new MiningSetRule(new String[] { "Goblin Armor" }, "GOBLIN_"),
            new MiningSetRule(new String[] { "Miner Armor" }, "TANK_MINER_"),
            new MiningSetRule(new String[] { "Prospecting Armor" }, "MINER_OUTFIT_"),
            new MiningSetRule(new String[] { "Lapis Armor" }, "LAPIS_ARMOR_"));

    private static final List<String> MINING_TOOLS = List.of(
            "DIVAN_DRILL", "TITANIUM_DRILL_4", "GEMSTONE_DRILL_4", "TITANIUM_DRILL_3", "GEMSTONE_DRILL_3",
            "TITANIUM_DRILL_2", "TITANIUM_DRILL_1", "GEMSTONE_DRILL_2", "GEMSTONE_DRILL_1", "GEMSTONE_GAUNTLET",
            "MITHRIL_DRILL_2", "MITHRIL_DRILL_1", "PICKONIMBUS", "REFINED_TITANIUM_PICKAXE", "TITANIUM_PICKAXE",
            "REFINED_MITHRIL_PICKAXE", "MITHRIL_PICKAXE", "BANDAGED_MITHRIL_PICKAXE", "FRACTURED_MITHRIL_PICKAXE",
            "RUSTY_TITANIUM_PICKAXE", "DIAMOND_PICKAXE", "IRON_PICKAXE");

    public final List<MiningSet> miningSets = new ArrayList<>();
    public final List<ItemStack> miningTools = new ArrayList<>();
    public final ItemStack[] miningEquipment;

    // ── Farming ───────────────────────────────────────────────────────────────
    public record FarmingSet(String label, ItemStack[] armor) {}
    public final List<FarmingSet> farmingSets = new ArrayList<>();
    public ItemStack[] farmingEquipment;
    public ItemStack[] pesthunterEquipment;
    public final Map<String, ItemStack> farmingTools = new LinkedHashMap<>();
    public ItemStack vacuum = ItemStack.EMPTY, wateringCan = ItemStack.EMPTY;
    private static final List<String> VACUUMS = List.of(
            "INFINI_VACUUM_HOOVERIUS", "INFINI_VACUUM", "SKYMART_HYPER_VACUUM", "SKYMART_TURBO_VACUUM", "SKYMART_VACUUM");
    private static final List<String> WATERING_CANS = List.of(
            "AQUAMASTER_HYDROMAX", "AQUAMASTER_X", "HYDRO_CAN_ULTRA_3000", "HYDRO_CAN_TURBO_2000", "HYDRO_CAN_1000");
    private final Map<String, net.minecraft.nbt.CompoundTag> toolkit;

    public ItemStack farmingTool(String crop) {
        net.minecraft.nbt.CompoundTag attrs = toolkit.get(crop);
        if (attrs == null && crop.equals("MOONFLOWER")) attrs = toolkit.get("SUNFLOWER");
        if (attrs != null) {
            ItemStack built = PvItems.fromAttributes(attrs);
            return built != null ? built : ItemStack.EMPTY;
        }
        return farmingTools.getOrDefault(crop, ItemStack.EMPTY);
    }

    // ── Foraging / fishing ────────────────────────────────────────────────────
    public final ItemStack[] foragingArmor, foragingEquipment, fishingArmor, fishingEquipment;
    public final ItemStack foragingAxe, waterRod, lavaRod;
    public final ItemStack[] trophyHunterArmor;
    private static final List<String> HUNTER_TIERS = List.of("DIAMOND_HUNTER_", "GOLD_HUNTER_", "SILVER_HUNTER_", "BRONZE_HUNTER_");

    private static final List<String> FARMING_ARMOR = List.of(
            "HELIANTHUS_", "FERMENTO_", "CROPIE_", "SQUASH_", "MELON_", "FARM_ARMOR_", "FARM_SUIT_", "RANCHERS_");
    private static final List<String> LAVA_RODS = List.of("HELLFIRE_ROD", "INFERNO_ROD", "MAGMA_ROD");
    public static final Map<String, List<String>> CROP_TOOLS = new LinkedHashMap<>();
    static {
        CROP_TOOLS.put("WHEAT", List.of("HOE_WHEAT"));
        CROP_TOOLS.put("CARROT", List.of("HOE_CARROT"));
        CROP_TOOLS.put("POTATO", List.of("HOE_POTATO"));
        CROP_TOOLS.put("PUMPKIN", List.of("PUMPKIN_DICER"));
        CROP_TOOLS.put("MELON", List.of("MELON_DICER"));
        CROP_TOOLS.put("SUGAR_CANE", List.of("HOE_CANE"));
        CROP_TOOLS.put("CACTUS", List.of("CACTUS_KNIFE"));
        CROP_TOOLS.put("COCOA_BEANS", List.of("COCO_CHOPPER"));
        CROP_TOOLS.put("MUSHROOM", List.of("FUNGI_CUTTER"));
        CROP_TOOLS.put("NETHER_STALK", List.of("HOE_WARTS", "HOE_WART"));
        CROP_TOOLS.put("SUNFLOWER", List.of("HOE_SUNFLOWER"));
        CROP_TOOLS.put("MOONFLOWER", List.of("HOE_SUNFLOWER")); // the Eclipse Sickle farms both
        CROP_TOOLS.put("WILD_ROSE", List.of("HOE_WILD_ROSE"));
    }

    PvAnalysis(PvProfileView v) {
        List<ItemStack> all = v.allItems;
        toolkit = v.farmingToolkit;

        for (var e : SLAYER_WEAPONS.entrySet()) {
            List<ItemStack> pool = e.getKey().equals("vampire") ? v.riftItems : all;
            List<List<ItemStack>> found = new ArrayList<>();
            for (WeaponLine line : e.getValue()) found.add(PvGear.bestByTier(pool, line.tiers(), 2));
            slayerWeapons.put(e.getKey(), new SlayerWeapons(e.getKey(), e.getValue(), found));
        }

        analyseKuudra(v);
        dungeonGear = new PvDungeonGear(v);

        Map<String, Double> mining = Map.of("Mining Fortune", 1.0, "Mining Speed", 0.08, "Pristine", 20.0,
                "Ore Fortune", 1.0, "Gemstone Fortune", 1.0, "Dwarven Metal Fortune", 0.5, "Block Fortune", 0.5);
        for (MiningSetRule rule : MINING_SETS) {
            int[] bestTier = { rule.prefixes().length };
            ItemStack[] pieces = PvGear.bestArmor(all, s -> {
                String id = PvGear.id(s);
                for (int i = 0; i < rule.prefixes().length; i++) {
                    if (!id.startsWith(rule.prefixes()[i])) continue;
                    bestTier[0] = Math.min(bestTier[0], i);
                    return (rule.prefixes().length - i) * 10_000 + 1 + PvGear.statSum(s, mining);
                }
                return 0;
            });
            boolean any = false;
            for (ItemStack p : pieces) any |= !p.isEmpty();
            if (any) miningSets.add(new MiningSet(rule.names()[bestTier[0]], pieces));
        }
        miningEquipment = PvGear.bestEquipment(all, s -> {
            double sc = PvGear.statSum(s, mining);
            return PvGear.id(s).equals("DIVAN_PENDANT") ? 10_000 + sc : sc;
        });
        List<ItemStack> tools = new ArrayList<>();
        for (ItemStack s : all) if (MINING_TOOLS.contains(PvGear.id(s))) tools.add(s);
        tools.sort(java.util.Comparator.comparingInt((ItemStack s) -> MINING_TOOLS.indexOf(PvGear.id(s)))
                .thenComparingDouble(s -> -PvGear.statSum(s, mining)));
        for (ItemStack s : tools) if (miningTools.size() < 2) miningTools.add(s);

        analyseFarming(v);

        Map<String, Double> foraging = Map.of("Foraging Fortune", 1.0, "Sweep", 3.0, "Foraging Wisdom", 1.0);
        foragingArmor = PvGear.bestArmor(all, s -> PvGear.statSum(s, foraging));
        foragingEquipment = PvGear.bestEquipment(all, s -> PvGear.statSum(s, foraging));
        foragingAxe = PvGear.best(all, s -> PvGear.type(s).equals("AXE") ? 1 + PvGear.statSum(s, foraging) : 0);

        Map<String, Double> fishing = Map.of("Sea Creature Chance", 10.0, "Fishing Speed", 1.0,
                "Treasure Chance", 4.0, "Double Hook Chance", 4.0, "Trophy Fish Chance", 2.0);
        fishingArmor = PvGear.bestArmor(all, s -> PvGear.tierIndex(s, HUNTER_TIERS) >= 0 ? 0 : PvGear.statSum(s, fishing));
        fishingEquipment = PvGear.bestEquipment(all, s -> PvGear.statSum(s, fishing));
        List<ItemStack> lava = PvGear.bestByTier(all, LAVA_RODS, 1);
        lavaRod = lava.isEmpty() ? ItemStack.EMPTY : lava.get(0);
        ItemStack[] hunter = PvGear.bestArmor(all, s -> {
            int t = PvGear.tierIndex(s, HUNTER_TIERS);
            return t < 0 ? 0 : (HUNTER_TIERS.size() - t) * 1000 + 1 + PvGear.statSum(s, fishing);
        });
        boolean anyHunter = false;
        for (ItemStack s : hunter) anyHunter |= !s.isEmpty();
        trophyHunterArmor = anyHunter ? hunter : null;
        waterRod = PvGear.best(all, s -> PvGear.type(s).equals("FISHING ROD") && PvGear.tierIndex(s, LAVA_RODS) < 0
                ? 1 + PvGear.statSum(s, fishing) : 0);
    }

    // ── Kuudra (ported from the worker's weapon/pet matchers) ─────────────────

    private void analyseKuudra(PvProfileView v) {
        kuudraWeapons.add(new KuudraWeapon("Terminator (Rend)", firstWithUltimate(v, "TERMINATOR", "rend")));
        ItemStack duplex = firstWithUltimate(v, "TERMINATOR", "reiterate");
        if (!duplex.isEmpty()) {
            kuudraWeapons.add(new KuudraWeapon("Terminator (Duplex)", duplex));
        } else {
            ItemStack other = ItemStack.EMPTY;
            for (ItemStack s : weaponPool(v)) {
                if (PvGear.id(s).contains("TERMINATOR") && !enchants(s).contains("ultimate_rend")) { other = s; break; }
            }
            if (other.isEmpty()) kuudraWeapons.add(new KuudraWeapon("Terminator (Duplex)", ItemStack.EMPTY));
            else kuudraWeapons.add(new KuudraWeapon("Terminator (" + ultimateName(other) + ")", other, true));
        }
        kuudraWeapons.add(new KuudraWeapon("Bonemerang (Rend)", firstWithUltimate(v, "BONE_BOOMERANG", "rend")));
        kuudraWeapons.add(new KuudraWeapon("Ragnarock Axe", ragnarock(v)));
        ItemStack atom = atomsplit(v);
        kuudraWeapons.add(new KuudraWeapon(PvGear.id(atom).contains("AXE_OF_THE_SHREDDED") ? "Halberd" : "Atomsplit", atom));

        List<PvProfileView.Pet> dragons = new ArrayList<>();
        for (PvProfileView.Pet p : v.pets) if (p.type().equals("GOLDEN_DRAGON")) dragons.add(p);
        for (PvProfileView.Pet p : dragons) {
            if (p.heldItem() != null && p.heldItem().equals("UNALLOYED_SPEED")
                    && (rendDragon == null || p.exp() > rendDragon.exp())) rendDragon = p;
        }
        dpsDragon = PvDungeonGear.bestGoldenDragon(v, rendDragon);
        if (dpsDragon == null) dpsDragon = rendDragon;
        if (dragons.size() < 2) rendDragon = null;

        findKuudraSets(v.wardrobe);
    }

    private static final String[] RCM_TYPES = { "TERROR", "AURORA", "STORM" };

    private void findKuudraSets(List<PvProfileView.WardrobeSet> sets) {
        PvProfileView.WardrobeSet rcm = null;
        int rcmPriority = Integer.MAX_VALUE;
        String rcmLabel = null;
        for (PvProfileView.WardrobeSet set : sets) {
            int[] counts = new int[RCM_TYPES.length];
            int valid = 0;
            for (ItemStack it : set.items()) {
                if (it.isEmpty()) continue;
                CompoundTag ench = enchants(it);
                if (!ench.contains("ultimate_wisdom") && !ench.contains("ultimate_refrigerate")) continue;
                for (int t = 0; t < RCM_TYPES.length; t++) {
                    if (PvGear.id(it).contains(RCM_TYPES[t])) { counts[t]++; valid++; break; }
                }
            }
            if (valid < 2) continue;
            int dominant = 0;
            for (int t = 1; t < RCM_TYPES.length; t++) if (counts[t] > counts[dominant]) dominant = t;
            if (dominant < rcmPriority) {
                rcmPriority = dominant;
                rcm = set;
                rcmLabel = PvProfileView.prettify(RCM_TYPES[dominant]) + " Set";
            }
        }
        if (rcm != null) kuudraSets.add(new KuudraSet(rcmLabel, rcm.slotNumber()));

        addFirstMatching(sets, "Hollow Set", it -> PvGear.id(it).contains("INFERNAL_HOLLOW"));
        if (addFirstMatching(sets, "Elegant Tuxedo", it -> PvGear.id(it).contains("ELEGANT"))) {
            addFirstMatching(sets, "Reaper Set", it -> PvGear.id(it).contains("REAPER"));
        } else {
            addFirstMatching(sets, "DPS Terror", it -> PvGear.id(it).contains("TERROR")
                    && PvGear.reforge(it).equals("ancient")
                    && !enchants(it).contains("ultimate_wisdom") && !enchants(it).contains("ultimate_refrigerate"));
        }
    }

    private boolean addFirstMatching(List<PvProfileView.WardrobeSet> sets, String label,
                                     java.util.function.Predicate<ItemStack> piece) {
        for (PvProfileView.WardrobeSet set : sets) {
            int count = 0;
            for (ItemStack it : set.items()) if (!it.isEmpty() && piece.test(it)) count++;
            if (count >= 3) {
                kuudraSets.add(new KuudraSet(label, set.slotNumber()));
                return true;
            }
        }
        return false;
    }

    private static CompoundTag enchants(ItemStack s) {
        return PvGear.data(s).getCompoundOrEmpty("enchantments");
    }

    private static String ultimateName(ItemStack s) {
        for (String key : enchants(s).keySet()) {
            if (key.startsWith("ultimate_")) return PvProfileView.prettify(key.substring("ultimate_".length()));
        }
        return "No Ultimate";
    }

    private static ItemStack firstWithUltimate(PvProfileView v, String idPart, String ultimate) {
        for (ItemStack s : weaponPool(v)) {
            if (PvGear.id(s).contains(idPart) && enchants(s).contains("ultimate_" + ultimate)) return s;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack ragnarock(PvProfileView v) {
        ItemStack best = ItemStack.EMPTY;
        int bestLevel = -1;
        for (ItemStack s : preferInventory(v, "RAGNAROCK")) {
            int level = enchants(s).getIntOr("ultimate_chimera", 0);
            if (level > bestLevel) { bestLevel = level; best = s; }
        }
        return best;
    }

    private static ItemStack atomsplit(PvProfileView v) {
        List<ItemStack> halberds = preferInventory(v, "AXE_OF_THE_SHREDDED");
        if (!halberds.isEmpty()) return halberds.get(0);
        List<ItemStack> katanas = preferInventory(v, "ATOMSPLIT");
        for (ItemStack s : katanas)
            if (enchants(s).contains("ultimate_swarm") && enchants(s).contains("prosecute")) return s;
        return katanas.isEmpty() ? ItemStack.EMPTY : katanas.get(0);
    }

    private static List<ItemStack> weaponPool(PvProfileView v) {
        List<ItemStack> pool = new ArrayList<>(v.mainInventoryItems);
        for (PvProfileView.Backpack b : v.backpacks) for (ItemStack s : b.items()) if (!s.isEmpty()) pool.add(s);
        for (ItemStack s : v.enderChest) if (!s.isEmpty()) pool.add(s);
        for (ItemStack s : v.vault) if (!s.isEmpty()) pool.add(s);
        for (ItemStack s : v.equipment) if (!s.isEmpty()) pool.add(s);
        return pool;
    }

    private static List<ItemStack> preferInventory(PvProfileView v, String idPart) {
        List<ItemStack> inv = new ArrayList<>(), rest = new ArrayList<>();
        for (ItemStack s : weaponPool(v)) {
            if (!PvGear.id(s).contains(idPart)) continue;
            (v.mainInventoryItems.contains(s) ? inv : rest).add(s);
        }
        return inv.isEmpty() ? rest : inv;
    }

    // ── Farming ───────────────────────────────────────────────────────────────

    private void analyseFarming(PvProfileView v) {
        List<ItemStack> all = v.allItems;
        List<ItemStack> mantid = new ArrayList<>(), mossy = new ArrayList<>(), farmArmor = new ArrayList<>();
        for (ItemStack s : all) {
            if (PvGear.armorSlot(s) < 0 || PvGear.tierIndex(s, FARMING_ARMOR) < 0) continue;
            farmArmor.add(s);
            String r = PvGear.reforge(s);
            if (r.equals("mantid")) mantid.add(s);
            else if (r.equals("mossy")) mossy.add(s);
        }
        java.util.function.ToDoubleFunction<ItemStack> farmScore = s ->
                (FARMING_ARMOR.size() - PvGear.tierIndex(s, FARMING_ARMOR)) * 1000 + PvGear.stat(s, "Farming Fortune");
        if (mantid.size() >= 2 && mossy.size() >= 2) {
            farmingSets.add(new FarmingSet("Mantid Set", fill(PvGear.bestArmor(mantid, farmScore), farmArmor, farmScore)));
            farmingSets.add(new FarmingSet("Mossy Set", fill(PvGear.bestArmor(mossy, farmScore), farmArmor, farmScore)));
        } else {
            farmingSets.add(new FarmingSet("Farming Armor", PvGear.bestArmor(farmArmor, farmScore)));
        }

        Map<String, Double> fortune = Map.of("Farming Fortune", 1.0, "Pest Chance", 0.5);
        farmingEquipment = PvGear.bestEquipment(all, s ->
                PvGear.id(s).contains("PESTHUNTER") ? 0 : PvGear.stat(s, "Farming Fortune"));
        pesthunterEquipment = farmingSets.size() > 1
                ? PvGear.bestEquipment(all, s -> PvGear.id(s).contains("PESTHUNTER") ? 1 + PvGear.statSum(s, fortune) : 0)
                : null;

        List<ItemStack> vac = PvGear.bestByTier(all, VACUUMS, 1);
        if (!vac.isEmpty()) vacuum = vac.get(0);
        List<ItemStack> can = PvGear.bestByTier(all, WATERING_CANS, 1);
        if (!can.isEmpty()) wateringCan = can.get(0);

        for (var e : CROP_TOOLS.entrySet()) {
            if (v.farmingToolkit.containsKey(e.getKey())
                    || (e.getKey().equals("MOONFLOWER") && v.farmingToolkit.containsKey("SUNFLOWER"))) continue;
            List<ItemStack> candidates = new ArrayList<>();
            for (ItemStack s : all) {
                String id = PvGear.id(s);
                for (String frag : e.getValue()) {
                    if (id.contains(frag) && !PvGear.type(s).isEmpty() && PvGear.armorSlot(s) < 0 && PvGear.equipSlot(s) < 0) {
                        candidates.add(s);
                        break;
                    }
                }
            }
            ItemStack best = PvGear.best(candidates, s -> 1 + PvGear.stat(s, "Farming Fortune") + tierSuffix(PvGear.id(s)));
            if (!best.isEmpty()) farmingTools.put(e.getKey(), best);
        }
    }

    private static ItemStack[] fill(ItemStack[] set, List<ItemStack> pool, java.util.function.ToDoubleFunction<ItemStack> score) {
        ItemStack[] best = PvGear.bestArmor(pool, score);
        for (int i = 0; i < 4; i++) if (set[i].isEmpty()) set[i] = best[i];
        return set;
    }

    private static double tierSuffix(String id) {
        int us = id.lastIndexOf('_');
        if (us < 0) return 0;
        try { return Integer.parseInt(id.substring(us + 1)) * 0.5; } catch (NumberFormatException e) { return 0; }
    }
}
