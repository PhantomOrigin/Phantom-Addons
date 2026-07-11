package com.kuudrahelper.features.misckuudra.profile;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class KuudraProfileData {

    public enum KuudraTier { INFERNAL, FIERY, BURNING, HOT, BASIC }

    public enum Weapon {
        DUPLEX_TERMINATOR("Duplex Terminator"),
        REND_TERMINATOR("Rend Terminator"),
        REND_BONEMERANG("Rend Bonemerang"),
        RAGNAROK("Ragnarok"),
        ATOMSPLIT("Atomsplit");

        public final String label;
        Weapon(String label) { this.label = label; }
    }

    public record ItemInfo(String displayName, List<String> lore) {}

    public record ArmorSetResult(String label, List<ItemInfo> pieces) {}

    public record GoldenDragonPet(int level, String item) {}

    public String name = "";
    public boolean loaded = false;
    public String errorMessage = null;

    public String nameTag = null;
    public int skyblockLevel = -1;

    public int magicalPower = -1;
    public int catacombsLevel = -1;
    public int foragingLevel = -1;

    public final Map<KuudraTier, Integer> kuudraCompletions = new EnumMap<>(KuudraTier.class);
    public final Map<Weapon, ItemInfo> ownedWeapons = new EnumMap<>(Weapon.class);

    public GoldenDragonPet dpsGoldenDragonPet = null;

    public GoldenDragonPet rendGoldenDragonPet = null;

    public final List<ArmorSetResult> armorSets = new ArrayList<>();

    public KuudraProfileData(String name) {
        this.name = name;
    }

    public int getKuudraCompletions(KuudraTier tier) {
        return kuudraCompletions.getOrDefault(tier, 0);
    }

    public boolean hasWeapon(Weapon w) {
        return ownedWeapons.containsKey(w);
    }

    public ItemInfo getWeapon(Weapon w) {
        return ownedWeapons.get(w);
    }
}
