package com.phantomaddons.features.pv;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

final class PvDungeonGear {

    record Group(String label, ItemStack[] items, List<PvProfileView.Pet> pets, ItemStack[] equipment, String source) {
        static Group items(String label, ItemStack... items) { return new Group(label, items, List.of(), null, null); }
        static Group pets(String label, List<PvProfileView.Pet> pets) { return new Group(label, new ItemStack[0], pets, null, null); }
        boolean isArmorSet() { return equipment != null; }
    }

    private record ArmorRule(String label, Predicate<ItemStack[]> set, List<ToDoubleFunction<ItemStack>> pieces) {}

    static final String[] CLASSES = { "healer", "mage", "berserk", "archer", "tank" };

    final ItemStack bonzoMask, spiritMask;
    final PvProfileView.Pet phoenix;
    final ItemStack[] bestDungeonEquipment;
    final Map<String, List<Group>> setups = new LinkedHashMap<>();

    private static final List<String> LOWER_TIER = List.of(
            "JUJU_SHORTBOW", "MOSQUITO_BOW", "DARK_CLAYMORE", "GIANTS_SWORD", "ITEM_SPIRIT_BOW", "BOUQUET_OF_LIES",
            "SHADOW_FURY", "FELTHORN_REAPER", "GIANT_CLEAVER", "HYPER_CLEAVER", "SUPER_CLEAVER", "CLEAVER",
            "FLOWER_OF_TRUTH", "ASPECT_OF_THE_DRAGON");
    private static final List<String> WITHER_BLADES = List.of("HYPERION", "ASTRAEA", "SCYLLA", "VALKYRIE", "NECRON_BLADE");

    private static final List<String> MAGE_FALLBACK = new ArrayList<>(WITHER_BLADES);
    static { MAGE_FALLBACK.addAll(List.of("BAT_WAND", "GLACIAL_SCYTHE", "FROZEN_SCYTHE")); }

    private static int bladeRank(ItemStack s) {
        return WITHER_BLADES.indexOf(PvGear.id(s).replaceFirst("^STARRED_", ""));
    }
    static { for (String id : LOWER_TIER) if (!id.contains("BOW")) MAGE_FALLBACK.add(id); }

    private static final String[] GDRAG_ITEMS = { "HEPHAESTUS_REMEDIES", "ANTIQUE_REMEDIES", "HEPHAESTUS_RELIC", "MINOS_RELIC" };
    private static final List<String> MONSTER_RAIDER = List.of("SKELETON_HELMET", "GUARDIAN_CHESTPLATE", "CREEPER_LEGGINGS", "TARANTULA_BOOTS");
    private static final Map<String, Double> DUNGEON_STATS = Map.of(
            "Strength", 1.0, "Crit Damage", 1.0, "Crit Chance", 1.0, "Intelligence", 0.5,
            "Health", 0.1, "Defense", 0.1, "Ferocity", 1.0, "Attack Speed", 1.0);

    // ── Armor set rules ───────────────────────────────────────────────────────

    private static ToDoubleFunction<ItemStack> id(String part) { return s -> PvGear.id(s).contains(part) ? 1 : 0; }
    private static ToDoubleFunction<ItemStack> sc(Predicate<ItemStack> p) { return s -> p.test(s) ? 1 : 0; }

    private static ToDoubleFunction<ItemStack> ranked(String... parts) {
        return s -> {
            String id = PvGear.id(s);
            for (int i = 0; i < parts.length; i++) if (id.contains(parts[i])) return parts.length - i;
            return 0;
        };
    }

    private static boolean mythic(ItemStack s) { return PvItems.rarityColor(s) == 0xFF55FF; }

    private static final ArmorRule RCM = new ArmorRule("RCM Set",
            items -> PvGear.id(items[0]).contains("WITHER_GOGGLES") && count(items, "WISE_WITHER") >= 2,
            List.of(id("WITHER_GOGGLES"), id("WISE_WITHER"), id("WISE_WITHER"), id("WISE_WITHER")));
    private static final Predicate<ItemStack> ANCIENT_STORM = s -> PvGear.id(s).contains("WISE_WITHER") && PvGear.reforge(s).equals("ancient");
    private static final ArmorRule LCM = new ArmorRule("LCM Set",
            items -> { int n = 0; for (ItemStack s : items) if (ANCIENT_STORM.test(s)) n++; return n >= 4; },
            List.of(sc(ANCIENT_STORM), sc(ANCIENT_STORM), sc(ANCIENT_STORM), sc(ANCIENT_STORM)));
    private static final ArmorRule NECRON = new ArmorRule("Necron / Maxor Set",
            items -> {
                int n = count(items, "POWER_WITHER");
                boolean maxorBoots = PvGear.id(items[3]).contains("SPEED_WITHER");
                return n >= 2 || (n >= 1 && maxorBoots);
            },
            List.of(ranked("DIAMOND_NECRON_HEAD", "GOLD_NECRON_HEAD", "POWER_WITHER", "PRIMORDIAL_HELMET", "TARANTULA_HELMET"),
                    s -> PvGear.id(s).contains("POWER_WITHER") ? 2
                            : PvGear.id(s).contains("SKELETON_MASTER_CHESTPLATE") && mythic(s) ? 1 : 0,
                    ranked("POWER_WITHER"),
                    ranked("POWER_WITHER", "SPEED_WITHER")));
    private static final Predicate<ItemStack> FB_PIECE = s -> PvGear.id(s).contains("FROZEN_BLAZE") && dungeonised(s);
    private static final ArmorRule FROZEN_BLAZE = new ArmorRule("Frozen Blaze (Dungeon)",
            items -> { int n = 0; for (ItemStack s : items) if (FB_PIECE.test(s)) n++; return n >= 3; },
            List.of(sc(FB_PIECE), sc(FB_PIECE), sc(FB_PIECE), sc(FB_PIECE)));
    private static final ArmorRule RAIDER = new ArmorRule("Monster Raider",
            items -> { int n = 0; for (ItemStack s : items) for (String r : MONSTER_RAIDER) if (PvGear.id(s).endsWith(r)) { n++; break; } return n >= 3; },
            List.of(sc(s -> PvGear.id(s).endsWith(MONSTER_RAIDER.get(0))), sc(s -> PvGear.id(s).endsWith(MONSTER_RAIDER.get(1))),
                    sc(s -> PvGear.id(s).endsWith(MONSTER_RAIDER.get(2))), sc(s -> PvGear.id(s).endsWith(MONSTER_RAIDER.get(3)))));
    private static final ArmorRule THERMO = new ArmorRule("Thermodynamic Set",
            items -> count(items, "THERMODYNAMIC") >= 3,
            List.of(id("THERMODYNAMIC"), id("THERMODYNAMIC"), id("THERMODYNAMIC"), id("THERMODYNAMIC")));
    private static final ArmorRule GOLDOR = new ArmorRule("Goldor Set",
            items -> count(items, "TANK_WITHER") >= 3,
            List.of(id("TANK_WITHER"), id("TANK_WITHER"), id("TANK_WITHER"), id("TANK_WITHER")));

    private static final List<ArmorRule> FALLBACKS = new ArrayList<>();
    static {
        FALLBACKS.add(fullSet("Shadow Assassin", "SHADOW_ASSASSIN", false));
        FALLBACKS.add(fullSet("Necromancer Lord", "NECROMANCER_LORD", false));
        for (String dragon : List.of("SUPERIOR", "STRONG", "UNSTABLE", "WISE", "YOUNG", "OLD", "PROTECTOR", "HOLY"))
            FALLBACKS.add(fullSet(PvProfileView.prettify(dragon) + " Dragon", dragon + "_DRAGON", true));
    }

    private static ArmorRule fullSet(String label, String idPart, boolean mustBeDungeonised) {
        Predicate<ItemStack> piece = s -> PvGear.id(s).contains(idPart) && (!mustBeDungeonised || dungeonised(s));
        return new ArmorRule(label,
                items -> { int n = 0; for (ItemStack s : items) if (piece.test(s)) n++; return n >= 3; },
                List.of(sc(piece), sc(piece), sc(piece), sc(piece)));
    }

    PvDungeonGear(PvProfileView v) {
        List<ItemStack> all = v.allItems;
        bonzoMask = first(all, s -> PvGear.id(s).contains("BONZO_MASK"));
        spiritMask = first(all, s -> PvGear.id(s).contains("SPIRIT_MASK"));
        phoenix = PvPages.bestPet(v, "PHOENIX", null);
        bestDungeonEquipment = PvGear.bestEquipment(all, s -> {
            double sc = PvGear.statSum(s, DUNGEON_STATS);
            return sc > 0 ? sc + (dungeonised(s) ? 50 : 0) : 0;
        });

        // ── Shared pieces ──
        Group necronSet = armorOrFallback(v, NECRON);
        ItemStack soulEater = first(all, s -> PvGear.id(s).contains("TERMINATOR") && ench(s).contains("ultimate_soul_eater"));
        ItemStack duplex = first(all, s -> PvGear.id(s).contains("TERMINATOR") && ench(s).contains("ultimate_reiterate"));
        ItemStack ragnarock = ragnarock(all);
        boolean hasTerminator = !first(all, s -> PvGear.id(s).contains("TERMINATOR")).isEmpty();
        ItemStack meleeOrBow = hasTerminator ? ItemStack.EMPTY : bestOf(all, LOWER_TIER, s -> true);
        ItemStack bowOnly = hasTerminator ? ItemStack.EMPTY : bestOf(all, LOWER_TIER, PvDungeonGear::isBow);
        PvProfileView.Pet gdrag = bestGoldenDragon(v);
        PvProfileView.Pet edrag = PvPages.bestPet(v, "ENDER_DRAGON", null);

        // ── Mage ──
        List<Group> mage = new ArrayList<>();
        mage.add(armorOrFallback(v, RCM));
        mage.add(armor(v, LCM));
        List<ItemStack> lcmHypes = lcmSuited(all, s -> bladeRank(s) >= 0, 2);
        List<ItemStack> claymores = lcmSuited(all, s -> PvGear.id(s).contains("DARK_CLAYMORE"), 2);
        ItemStack rcmHype = bestOf(all, WITHER_BLADES, s -> PvGear.reforge(s).equals("heroic") && hasWitherImpact(s)
                && (ench(s).contains("ultimate_wise") || ench(s).contains("ultimate_swarm") || ench(s).contains("ultimate_combo")));
        if (rcmHype.isEmpty()) {
            List<ItemStack> used = new ArrayList<>(lcmHypes);
            used.addAll(claymores);
            rcmHype = bestOf(all, MAGE_FALLBACK, s -> !containsSame(used, s));
        }
        mage.add(Group.items("RCM Wither Blade", rcmHype));
        mage.add(Group.items("LCM Wither Blade", orOne(lcmHypes)));
        mage.add(Group.items("Dark Claymore", orOne(claymores)));
        mage.add(Group.items("Ragnarock", ragnarock));
        mage.add(Group.pets("Pet", firstNonNull(gdrag, edrag,
                PvPages.bestPet(v, "CROW", null), PvPages.bestPet(v, "SHEEP", null))));
        setups.put("mage", mage);

        // ── Archer (and the base for the other classes) ──
        List<PvProfileView.Pet> mainPets = mainDamagePets(v, gdrag, edrag, true);
        List<Group> archer = base(necronSet, hasTerminator, bowOnly, soulEater, duplex, ragnarock, mainPets);
        archer.add(armor(v, FROZEN_BLAZE));
        archer.add(armor(v, RAIDER));
        archer.add(Group.items("Bonemerang", first(all, s -> PvGear.id(s).contains("BONE_BOOMERANG"))));
        archer.add(Group.items("Death Bow", first(all, s -> PvGear.id(s).contains("DEATH_BOW"))));
        archer.add(Group.items("Halberd / Atomsplit", halberdOrAtomsplit(all)));
        setups.put("archer", archer);

        List<Group> berserk = base(necronSet, hasTerminator, meleeOrBow, soulEater, duplex, ragnarock, mainPets);
        berserk.add(armor(v, THERMO));
        setups.put("berserk", berserk);

        List<PvProfileView.Pet> noEdragPets = mainDamagePets(v, gdrag, edrag, false);
        List<Group> tank = base(necronSet, hasTerminator, meleeOrBow, soulEater, duplex, ragnarock, noEdragPets);
        tank.add(armor(v, GOLDOR));
        setups.put("tank", tank);

        setups.put("healer", base(necronSet, hasTerminator, meleeOrBow, soulEater, duplex, ragnarock, noEdragPets));
    }

    private Group armor(PvProfileView v, ArmorRule rule) {
        Group saved = savedArmor(v, rule);
        return saved != null ? saved : storageArmor(v, rule);
    }

    private Group savedArmor(PvProfileView v, ArmorRule rule) {
        for (PvProfileView.Loadout lo : v.loadouts) {
            PvProfileView.WardrobeSet ws = v.armorSet(lo.armorSet());
            if (ws == null || !rule.set().test(ws.items())) continue;
            PvProfileView.WardrobeSet eq = v.equipmentSet(lo.equipmentSet());
            return new Group(rule.label(), ws.items(), List.of(), eq != null ? eq.items() : bestDungeonEquipment, lo.name());
        }
        for (PvProfileView.WardrobeSet ws : v.wardrobe) {
            if (rule.set().test(ws.items()))
                return new Group(rule.label(), ws.items(), List.of(), bestDungeonEquipment, "Wardrobe #" + ws.slotNumber());
        }
        return null;
    }

    private Group storageArmor(PvProfileView v, ArmorRule rule) {
        List<ItemStack> pool = new ArrayList<>(v.mainInventoryItems);
        for (ItemStack s : v.armor) if (!s.isEmpty()) pool.add(s);
        for (PvProfileView.Backpack b : v.backpacks) for (ItemStack s : b.items()) if (!s.isEmpty()) pool.add(s);
        for (ItemStack s : v.enderChest) if (!s.isEmpty()) pool.add(s);
        for (ItemStack s : v.vault) if (!s.isEmpty()) pool.add(s);

        ItemStack[] built = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
        for (int slot = 0; slot < 4; slot++) {
            final int sl = slot;
            ToDoubleFunction<ItemStack> piece = rule.pieces().get(slot);
            built[slot] = PvGear.best(pool, s -> {
                if (PvGear.armorSlot(s) != sl) return 0;
                double rank = piece.applyAsDouble(s);
                return rank <= 0 ? 0 : rank * 1000 + (dungeonised(s) ? 10 : 0) + PvGear.statSum(s, DUNGEON_STATS) * 0.01;
            });
        }
        if (rule.set().test(built)) return new Group(rule.label(), built, List.of(), bestDungeonEquipment, "Storage");
        return new Group(rule.label(), orEmpty(null), List.of(), orEmpty(null), null);
    }

    private Group armorOrFallback(PvProfileView v, ArmorRule rule) {
        List<ArmorRule> tiers = new ArrayList<>();
        tiers.add(rule);
        tiers.addAll(FALLBACKS);
        for (ArmorRule r : tiers) {
            Group g = armor(v, r);
            if (g.source() != null) return g;
        }
        return new Group(rule.label(), orEmpty(null), List.of(), orEmpty(null), null);
    }

    private static List<Group> base(Group necronSet, boolean hasTerminator, ItemStack lowerTier,
                                    ItemStack soulEater, ItemStack duplex, ItemStack ragnarock,
                                    List<PvProfileView.Pet> pets) {
        List<Group> g = new ArrayList<>();
        g.add(necronSet);
        if (hasTerminator) {
            g.add(Group.items("Soul Eater Terminator", soulEater));
            g.add(Group.items("Duplex Terminator", duplex));
        } else {
            g.add(Group.items("Main Weapon", lowerTier));
        }
        g.add(Group.items("Ragnarock", ragnarock));
        g.add(Group.pets("Main Damage Pet", pets));
        return g;
    }

    private static List<PvProfileView.Pet> mainDamagePets(PvProfileView v, PvProfileView.Pet gdrag, PvProfileView.Pet edrag, boolean allowEdrag) {
        List<PvProfileView.Pet> out = new ArrayList<>();
        PvProfileView.Pet main = firstNonNullOne(gdrag, allowEdrag ? edrag : null,
                PvPages.bestPet(v, "SKELETON", null), PvPages.bestPet(v, "WITHER_SKELETON", null));
        if (main != null) out.add(main);
        if (allowEdrag && main == gdrag && gdrag != null && edrag != null) out.add(edrag);
        return out;
    }

    // ── Golden dragon priority (held item, then level) ────────────────────────

    static PvProfileView.Pet bestGoldenDragon(PvProfileView v) {
        return bestGoldenDragon(v, null);
    }

    static PvProfileView.Pet bestGoldenDragon(PvProfileView v, PvProfileView.Pet exclude) {
        PvProfileView.Pet best = null;
        for (PvProfileView.Pet p : v.pets) {
            if (!p.type().equals("GOLDEN_DRAGON") || p == exclude) continue;
            if (best == null || gdragRank(p) < gdragRank(best)
                    || (gdragRank(p) == gdragRank(best) && p.exp() > best.exp())) best = p;
        }
        return best;
    }

    private static int gdragRank(PvProfileView.Pet p) {
        String held = p.heldItem() == null ? "" : p.heldItem().toUpperCase(Locale.ROOT);
        for (int i = 0; i < GDRAG_ITEMS.length; i++) if (held.contains(GDRAG_ITEMS[i])) return i;
        return GDRAG_ITEMS.length;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ItemStack[] orEmpty(ItemStack[] set) {
        return set != null ? set : new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
    }

    private static int count(ItemStack[] items, String idPart) {
        int n = 0;
        for (ItemStack s : items) if (s != null && PvGear.id(s).contains(idPart)) n++;
        return n;
    }

    private static ItemStack first(List<ItemStack> items, Predicate<ItemStack> test) {
        for (ItemStack s : items) if (test.test(s)) return s;
        return ItemStack.EMPTY;
    }

    private static CompoundTag ench(ItemStack s) {
        return PvGear.data(s).getCompoundOrEmpty("enchantments");
    }

    private static boolean dungeonised(ItemStack s) {
        CompoundTag d = PvGear.data(s);
        return d.contains("dungeon_item") || d.contains("dungeon_item_level") || d.contains("upgrade_level");
    }

    private static boolean hasWitherImpact(ItemStack s) {
        ListTag scrolls = PvGear.data(s).getListOrEmpty("ability_scroll");
        int found = 0;
        for (int i = 0; i < scrolls.size(); i++) {
            String sc = scrolls.getStringOr(i, "");
            if (sc.equals("IMPLOSION_SCROLL") || sc.equals("SHADOW_WARP_SCROLL") || sc.equals("WITHER_SHIELD_SCROLL")) found++;
        }
        return found >= 3;
    }

    private static List<ItemStack> lcmSuited(List<ItemStack> all, Predicate<ItemStack> match, int max) {
        List<ItemStack> suited = new ArrayList<>(), chim = new ArrayList<>(), any = new ArrayList<>();
        List<ItemStack> ordered = new ArrayList<>(all);
        ordered.sort(java.util.Comparator.comparingInt(s -> bladeRank(s) < 0 ? Integer.MAX_VALUE : bladeRank(s)));
        for (ItemStack s : ordered) {
            if (!match.test(s) || containsSame(any, s)) continue;
            any.add(s);
            if (!ench(s).contains("ultimate_chimera")) continue;
            String r = PvGear.reforge(s);
            if (r.equals("fabled") || r.equals("withered")) suited.add(s); else chim.add(s);
        }
        if (!suited.isEmpty()) return suited.subList(0, Math.min(max, suited.size()));
        if (!chim.isEmpty()) return List.of(chim.get(0));
        return any.isEmpty() ? List.of() : List.of(any.get(0));
    }

    private static ItemStack[] orOne(List<ItemStack> items) {
        return items.isEmpty() ? new ItemStack[] { ItemStack.EMPTY } : items.toArray(new ItemStack[0]);
    }

    private static boolean containsSame(List<ItemStack> list, ItemStack s) {
        for (ItemStack o : list) if (o == s) return true;
        return false;
    }

    private static boolean isBow(ItemStack s) {
        return PvGear.type(s).contains("BOW");
    }

    private static ItemStack bestOf(List<ItemStack> all, List<String> order, Predicate<ItemStack> filter) {
        ItemStack best = ItemStack.EMPTY;
        int bestRank = Integer.MAX_VALUE;
        for (ItemStack s : all) {
            if (!filter.test(s)) continue;
            String id = PvGear.id(s).replaceFirst("^STARRED_", "");
            int rank = order.indexOf(id);
            if (rank >= 0 && rank < bestRank) { bestRank = rank; best = s; }
        }
        return best;
    }

    static ItemStack ragnarock(List<ItemStack> all) {
        ItemStack best = ItemStack.EMPTY;
        int bestLevel = -1;
        for (ItemStack s : all) {
            if (!PvGear.id(s).contains("RAGNAROCK")) continue;
            int level = ench(s).getIntOr("ultimate_chimera", 0);
            if (level > bestLevel) { bestLevel = level; best = s; }
        }
        return best;
    }

    static ItemStack halberdOrAtomsplit(List<ItemStack> all) {
        ItemStack halberd = first(all, s -> PvGear.id(s).contains("AXE_OF_THE_SHREDDED"));
        if (!halberd.isEmpty()) return halberd;
        ItemStack swarm = first(all, s -> PvGear.id(s).contains("ATOMSPLIT")
                && ench(s).contains("ultimate_swarm") && ench(s).contains("prosecute"));
        return !swarm.isEmpty() ? swarm : first(all, s -> PvGear.id(s).contains("ATOMSPLIT"));
    }

    private static List<PvProfileView.Pet> firstNonNull(PvProfileView.Pet... pets) {
        PvProfileView.Pet p = firstNonNullOne(pets);
        return p != null ? List.of(p) : List.of();
    }

    private static PvProfileView.Pet firstNonNullOne(PvProfileView.Pet... pets) {
        for (PvProfileView.Pet p : pets) if (p != null) return p;
        return null;
    }
}
