package com.kuudrahelper.features.misckuudra.profittracker;

import com.kuudrahelper.KuudraConfig;
import com.kuudrahelper.KuudraHelperMod;
import com.kuudrahelper.utils.KuudraTierDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CroesusListener {

    public static boolean isCroesusMain(AbstractContainerScreen<?> screen) {
        String title = stripColor(screen.getTitle().getString()).toLowerCase();
        return title.contains("croesus");
    }

    public static boolean isKuudraChest(AbstractContainerScreen<?> screen) {
        String title = stripColor(screen.getTitle().getString()).toLowerCase();
        return title.contains("kuudra -") || title.contains("paid chest") || title.contains("free chest");
    }

    public static boolean isFreeChest(AbstractContainerScreen<?> screen) {
        String title = stripColor(screen.getTitle().getString()).toLowerCase();
        return title.contains("free chest");
    }

    private static final int AQUA_HIGHLIGHT  = 0x6600FFFF;
    private static final int GREEN_HIGHLIGHT = 0x6600FF44;

    public static boolean isUnopenedChest(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        String name = stripColor(stack.getDisplayName().getString()).toLowerCase();
        if (!name.contains("chest")) return false;

        List<String> lore = getLore(stack);
        for (String line : lore) {
            String l = stripColor(line).toLowerCase();
            if (l.contains("already opened") || l.contains("not yet opened") && l.contains("false"))
                return false;
            if (l.contains("not opened") || l.contains("not yet opened")) return true;
        }
        return stack.getItem() == Items.CHEST || stack.getItem() == Items.TRAPPED_CHEST;
    }

    public record ChestAnalysis(
        long itemsValue,
        long attributeValue,
        long essenceValue,
        long totalValue,
        boolean canReroll,
        boolean rerollProfit,      // true if rerolling is estimated to be profitable
        boolean wheelOfFate,       // true if Wheel of Fate on an item is recommended
        int rerollSlotIndex,       // GUI slot index of the kismet feather (or -1)
        int wheelSlotIndex,        // GUI slot index of the wheel-of-fate item (or -1)
        boolean kismetAlreadyUsed, // chest lore says "already rerolled"
        int detectedTier           // tier read from "Cost: X Kuudra Key" lore (0 if not found)
    ) {}

    public static final int REWARD_SLOT_START = 11;
    public static final int REWARD_SLOT_END   = 15; // inclusive

    public static ChestAnalysis analyseChest(AbstractContainerScreen<?> screen) {
        var slots = screen.getMenu().slots;
        long itemsValue     = 0;
        long attributeValue = 0;
        long essenceValue   = 0;
        boolean canReroll          = false;
        boolean kismetAlreadyUsed  = false;
        int rerollSlot             = -1;
        int wheelSlot              = -1;
        int detectedTier           = 0;

        // Scan chest slots only (excluding player inventory) to find action buttons
        int chestSlots = Math.max(0, slots.size() - 36);
        for (int i = 0; i < chestSlots; i++) {
            ItemStack stack = slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            String name = stripColor(stack.getDisplayName().getString()).toLowerCase();
            List<String> lore = getLore(stack);

            if (name.contains("kismet") || name.contains("reroll")) {
                rerollSlot = i;
                canReroll = true;
                for (String l : lore) {
                    String sl = stripColor(l).toLowerCase();
                    if (sl.contains("already been rerolled") || sl.contains("cannot be rerolled")
                        || sl.contains("already rerolled")) {
                        canReroll = false;
                        kismetAlreadyUsed = true;
                        break;
                    }
                }
            } else if (name.contains("wheel of fate") || (name.contains("wheel") && name.contains("fate"))) {
                wheelSlot = i;
            }

            if (detectedTier == 0) {
                for (String l : lore) {
                    int t = tierFromKeyLore(stripColor(l).toLowerCase());
                    if (t > 0) { detectedTier = t; break; }
                }
            }
        }

        for (int i = REWARD_SLOT_START; i <= REWARD_SLOT_END && i < slots.size(); i++) {
            ItemStack stack = slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            if (i == rerollSlot || i == wheelSlot) continue;

            String rawName = stack.getDisplayName().getString();
            String name    = stripColor(rawName).toLowerCase().trim();
            List<String> lore = getLore(stack);

            String armorId = KuudraDrops.armorIdForName(name);
            if (armorId != null) {
                if (KuudraConfig.isProfitArmorSalvage()) {
                    int stars   = KuudraDrops.countStars(rawName);
                    int essence = KuudraDrops.salvageEssence(armorId, stars);
                    double essencePrice = bazaarSellPrice(KuudraDrops.CRIMSON_ESSENCE);
                    double petMult = KuudraConfig.getKuudraPetEssenceMultiplier();
                    long val = (long)(essence * essencePrice * petMult);
                    itemsValue += val;
                } else {
                    double price = PriceCache.getBin(armorId);
                    if (price > 0) itemsValue += (long)price;
                }
                continue;
            }

            String weaponId = KuudraDrops.weaponIdForName(name);
            if (weaponId != null) {
                double price;
                String priceSource;
                if (KuudraDrops.AH_WEAPON_IDS.contains(weaponId)) {
                    price = PriceCache.getBin(weaponId);
                    priceSource = "bin";
                } else {
                    price = bazaarSellPrice(weaponId);
                    priceSource = "bazaar";
                }
                if (price > 0) itemsValue += (long)price;
                continue;
            }

            if (name.contains("shard")) {
                String shardId = KuudraDrops.attributeShardId(rawName, lore);
                if (shardId != null) {
                    double price = bazaarSellPrice(shardId);
                    if (price > 0) attributeValue += (long)price;
                }
                continue;
            }

            if (name.contains("enchanted book")) {
                long val = enchantedBookValue(lore);
                if (val > 0) itemsValue += val;
                continue;
            }

            if (name.contains("kuudra teeth") || name.contains("kuudra tooth")) {
                int count = stack.getCount();
                if (count <= 0) count = parseQuantity(name);
                double price = bazaarSellPrice(KuudraDrops.KUUDRA_TEETH);
                long val = price > 0 ? (long)(count * price) : 0;
                if (price > 0) itemsValue += val;
                continue;
            }

            if (name.contains("kuudra tentacle")) {
                int count = stack.getCount();
                if (count <= 0) count = parseQuantity(name);
                double price = bazaarSellPrice(KuudraDrops.KUUDRA_TENTACLE);
                long val = price > 0 ? (long)(count * price) : 0;
                if (price > 0) itemsValue += val;
                continue;
            }

            if (name.contains("heavy pearl")) {
                int count = stack.getCount();
                if (count <= 0) count = parseQuantity(name);
                double price = bazaarSellPrice(KuudraDrops.HEAVY_PEARL);
                long val = price > 0 ? (long)(count * price) : 0;
                if (price > 0) itemsValue += val;
                continue;
            }

            if (name.contains("crimson essence") || (name.contains("essence") && !name.contains("shard"))) {
                int count = parseQuantity(name);
                if (count <= 1) count = stack.getCount();
                double price = bazaarSellPrice(KuudraDrops.CRIMSON_ESSENCE);
                double petMult = KuudraConfig.getKuudraPetEssenceMultiplier();
                long val = price > 0 ? (long)(count * price * petMult) : 0;
                if (price > 0) essenceValue += val;
                continue;
            }
        }

        long totalValue = itemsValue + attributeValue + essenceValue;

        boolean rerollProfit = false;
        if (canReroll) {
            double kismetCost = bazaarBuyPrice(KuudraDrops.KISMET_FEATHER);
            rerollProfit = kismetCost > 0 && totalValue < kismetCost * 0.5;
        }

        boolean useWheel = false;
        if (wheelSlot >= 0 && attributeValue > 0) {
            double wheelCost = bazaarBuyPrice(KuudraDrops.WHEEL_OF_FATE);
            useWheel = wheelCost > 0 && attributeValue < wheelCost * 2;
        }

        return new ChestAnalysis(
            itemsValue, attributeValue, essenceValue, totalValue,
            canReroll, rerollProfit, useWheel, rerollSlot, wheelSlot,
            kismetAlreadyUsed, detectedTier
        );
    }

    public static long calculateKeyCost(int tier) {
        if (tier < 1 || tier > 5) return 0;
        KuudraDrops.KeyRecipe recipe = KuudraDrops.KEY_RECIPES[tier];
        if (recipe == null) return 0;

        boolean mage = KuudraConfig.isProfitFactionMage();
        String mainItem = mage ? recipe.mageItem() : recipe.barbItem();
        int    mainAmt  = mage ? recipe.mageAmt()  : recipe.barbAmt();

        double mainPrice  = bazaarBuyPrice(mainItem);
        double extraPrice = recipe.extraItem() != null ? bazaarBuyPrice(recipe.extraItem()) : 0;

        return (long)(mainAmt * mainPrice + recipe.extraAmt() * extraPrice) + recipe.fixedCoinCost();
    }

    public static double bazaarSellPrice(String id) {
        double p = KuudraConfig.isProfitBazaarInstaSell()
                ? PriceCache.getBazaarSell(id) : PriceCache.getBazaarBuy(id);
        return p > 0 ? p : 0;
    }

    public static double bazaarBuyPrice(String id) {
        double p = KuudraConfig.isProfitBazaarInstaBuy()
                ? PriceCache.getBazaarBuy(id) : PriceCache.getBazaarSell(id);
        return p > 0 ? p : 0;
    }

    private static final Pattern QUANTITY_PATTERN = Pattern.compile("x(\\d+)", Pattern.CASE_INSENSITIVE);

    static int parseQuantity(String strippedName) {
        Matcher m = QUANTITY_PATTERN.matcher(strippedName);
        int qty = 0;
        while (m.find()) qty = Integer.parseInt(m.group(1));
        return qty > 0 ? qty : 1;
    }

    private static long enchantedBookValue(List<String> lore) {
        boolean isUltimate = false;
        for (String line : lore) {
            if (stripColor(line).toLowerCase().contains("only have 1 ultimate enchantment")) {
                isUltimate = true;
                break;
            }
        }

        for (String line : lore) {
            String stripped = stripColor(line).trim();
            if (stripped.isEmpty()) continue;
            int lastSpace = stripped.lastIndexOf(' ');
            if (lastSpace < 1) continue;
            String levelStr = stripped.substring(lastSpace + 1);
            String enchName = stripped.substring(0, lastSpace).trim();
            if (enchName.isEmpty()) continue;
            int level = romanToArabic(levelStr);
            if (level <= 0) {
                try { level = Integer.parseInt(levelStr); } catch (NumberFormatException ignored) {}
            }
            if (level <= 0) continue;
            String namePart = enchName.toUpperCase().replace(' ', '_');
            String apiId = isUltimate
                    ? "ENCHANTMENT_ULTIMATE_" + namePart + "_" + level
                    : "ENCHANTMENT_" + namePart + "_" + level;
            double price = bazaarSellPrice(apiId);
            if (price > 0) return (long) price;
        }
        return 0;
    }

    private static int romanToArabic(String s) {
        return switch (s.toUpperCase()) {
            case "I"    -> 1; case "II"   -> 2; case "III"  -> 3;
            case "IV"   -> 4; case "V"    -> 5; case "VI"   -> 6;
            case "VII"  -> 7; case "VIII" -> 8; case "IX"   -> 9; case "X" -> 10;
            default -> 0;
        };
    }

    static int tierFromKeyLore(String lowerStripped) {
        if (lowerStripped.contains("infernal kuudra key")) return 5;
        if (lowerStripped.contains("fiery kuudra key"))    return 4;
        if (lowerStripped.contains("burning kuudra key"))  return 3;
        if (lowerStripped.contains("hot kuudra key"))      return 2;
        if (lowerStripped.contains("basic kuudra key"))    return 1;
        return 0;
    }

    private static String stripColor(String s) { return KuudraDrops.stripColor(s); }
    public  static String stripColorStatic(String s) { return KuudraDrops.stripColor(s); }

    private static List<String> getLore(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        if (stack.isEmpty()) return lines;
        var tag = stack.getComponents().get(net.minecraft.core.component.DataComponents.LORE);
        if (tag != null) {
            tag.lines().forEach(c -> lines.add(c.getString()));
        }
        return lines;
    }

    private CroesusListener() {}
}
