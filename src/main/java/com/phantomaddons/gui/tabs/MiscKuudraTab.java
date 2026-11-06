package com.phantomaddons.gui.tabs;

import com.phantomaddons.PhantomConfig;
import com.phantomaddons.features.misckuudra.ShitterList;
import com.phantomaddons.features.misckuudra.profile.RemoteFeatureGate;
import com.phantomaddons.gui.Cycle;
import com.phantomaddons.gui.Group;
import com.phantomaddons.gui.KeyCapture;
import com.phantomaddons.gui.MenuBuilder;
import com.phantomaddons.gui.OptionalIntInput;
import com.phantomaddons.gui.RangeSlider;
import com.phantomaddons.gui.Slider;
import com.phantomaddons.gui.Toggle;

public final class MiscKuudraTab {

    private MiscKuudraTab() {}

    public static void build(MenuBuilder m) {
        m.heading("Mobs and warnings");
        m.add(new RangeSlider("Kuudra Mob Size", 1.0f, 200.0f, "%.0f%%",
                PhantomConfig::getKuudraSizeScale, PhantomConfig::setKuudraSizeScale)
                .withTooltip("Makes Kuudra's mobs bigger or smaller."));
        m.add(new Toggle("Tuxedo Warning",
                PhantomConfig::isTuxedoWarningEnabled, PhantomConfig::setTuxedoWarningEnabled)
                .withTooltip("Warns you if you are wearing the Tuxedo."));

        m.heading("Shop and chests");
        Group shop = m.group("Shop Keybinds", PhantomConfig::isShopKeybindsEnabled, PhantomConfig::setShopKeybindsEnabled)
                .withTooltip("Lets you use keys in Kuudra's shop.");
        shop.add(new KeyCapture("Main Key", PhantomConfig::getShopMainKey,   PhantomConfig::setShopMainKey)
                .withTooltip("Opens the main shop menu."));
        shop.add(new KeyCapture("Cannon Key", PhantomConfig::getShopCannonKey, PhantomConfig::setShopCannonKey)
                .withTooltip("Buys the Human Cannonball from the shop."));
        m.add(shop);

        m.add(new Toggle("Custom Shop Menu",
                PhantomConfig::isShopMenuGuiEnabled, PhantomConfig::setShopMenuGuiEnabled)
                .withTooltip("Replaces the shop with a custom menu."));

        m.add(new Toggle("Middle Click Perk Menu",
                PhantomConfig::isMiddleClickShopGuiEnabled, PhantomConfig::setMiddleClickShopGuiEnabled)
                .withTooltip("Turns your normal clicks in the shop menu into middle clicks."));

        Group profit = m.group("Profit Tracker", PhantomConfig::isProfitTrackerEnabled, PhantomConfig::setProfitTrackerEnabled)
                .withTooltip("Tracks the coins you earn from Kuudra runs.");
        profit.add(new Toggle("Show During Run",
                PhantomConfig::isProfitShowDuringRun, PhantomConfig::setProfitShowDuringRun)
                .withTooltip("Shows the tracker while a run is going."));
        profit.add(new Cycle("Armor",
                () -> PhantomConfig.isProfitArmorSalvage() ? "Salvage" : "Sell",
                () -> PhantomConfig.setProfitArmorSalvage(!PhantomConfig.isProfitArmorSalvage()))
                .withTooltip("Chooses whether armor drops count as sold or salvaged for essence."));
        profit.add(new Cycle("Faction",
                () -> PhantomConfig.isProfitFactionMage() ? "Mage" : "Barbarian",
                () -> PhantomConfig.setProfitFactionMage(!PhantomConfig.isProfitFactionMage()))
                .withTooltip("Your faction, used to work out key prices."));
        profit.add(new Toggle("Reroll Calculator",
                PhantomConfig::isProfitRerollCalc, PhantomConfig::setProfitRerollCalc)
                .withTooltip("Tells you if rerolling a chest is worth it."));
        profit.add(new Toggle("Block Reroll On Expensive Items",
                PhantomConfig::isBlockExpensiveRerollEnabled, PhantomConfig::setBlockExpensiveRerollEnabled)
                .withTooltip("Stops you rerolling a chest that has an expensive item."));
        profit.add(new Cycle("Bazaar Sell",
                () -> PhantomConfig.isProfitBazaarInstaSell() ? "Instasell" : "Sell Order",
                () -> PhantomConfig.setProfitBazaarInstaSell(!PhantomConfig.isProfitBazaarInstaSell()))
                .withTooltip("Prices items using instant sell or sell order prices."));
        profit.add(new Cycle("Bazaar Buy",
                () -> PhantomConfig.isProfitBazaarInstaBuy() ? "Instabuy" : "Buy Order",
                () -> PhantomConfig.setProfitBazaarInstaBuy(!PhantomConfig.isProfitBazaarInstaBuy()))
                .withTooltip("Prices items using instant buy or buy order prices."));
        profit.add(new Toggle("Chest Value GUI",
                PhantomConfig::isChestValueGuiEnabled, PhantomConfig::setChestValueGuiEnabled)
                .withTooltip("Shows a chest's value in its menu."));
        profit.add(new Cycle("Kuudra Pet",
                () -> {
                    PhantomConfig.KuudraPetRarity r = PhantomConfig.getKuudraPetRarity();
                    String name = r.name().charAt(0) + r.name().substring(1).toLowerCase();
                    return name;
                },
                () -> {
                    PhantomConfig.KuudraPetRarity[] vals = PhantomConfig.KuudraPetRarity.values();
                    int next = (PhantomConfig.getKuudraPetRarity().ordinal() + 1) % vals.length;
                    PhantomConfig.setKuudraPetRarity(vals[next]);
                })
                .withTooltip("The rarity of your Kuudra pet."));
        profit.add(new RangeSlider("Pet Level", 1, 100, "%.0f",
                () -> (float) PhantomConfig.getKuudraPetLevel(),
                v  -> PhantomConfig.setKuudraPetLevel(Math.round(v)))
                .withTooltip("The level of your Kuudra pet."));
        profit.add(new RangeSlider("Essence of Crimson", 0, 10, "%.0f",
                () -> (float) PhantomConfig.getEssenceOfCrimson(),
                v  -> PhantomConfig.setEssenceOfCrimson(Math.round(v)))
                .withTooltip("Each level adds 1% more Crimson Essence."));
        profit.add(new RangeSlider("Echo of Essence", 0, 10, "%.0f",
                () -> (float) PhantomConfig.getEchoOfEssence(),
                v  -> PhantomConfig.setEchoOfEssence(Math.round(v)))
                .withTooltip("Each level adds 3% to the bonus from Essence of Crimson."));
        m.add(profit);

        m.add(new Toggle("Highlight Unopened Chests",
                PhantomConfig::isHighlightUnopenedChestsEnabled, PhantomConfig::setHighlightUnopenedChestsEnabled)
                .withTooltip("Highlights chests you haven't opened yet."));

        m.heading("Runs");
        Group kicked = m.group("Kicked Notification", PhantomConfig::isKickedNotificationEnabled, PhantomConfig::setKickedNotificationEnabled)
                .withTooltip("Tells you when you get kicked from a run.");
        m.sound(kicked, PhantomConfig.SOUND_KICKED);
        m.add(kicked);
        Group autoRequeue = m.group("Auto Requeue", PhantomConfig::isAutoRequeueEnabled, PhantomConfig::setAutoRequeueEnabled)
                .withTooltip("Joins a new run automatically when one ends.");
        autoRequeue.add(new Toggle("Party Chat Message",
                PhantomConfig::isAutoRequeueMessageEnabled, PhantomConfig::setAutoRequeueMessageEnabled)
                .withTooltip("Sends a message in party chat when it requeues."));
        m.add(autoRequeue);
        m.heading("Hiding and extras");
        m.add(new Toggle("Hide Damage Title",
                PhantomConfig::isHideDamageTitleEnabled, PhantomConfig::setHideDamageTitleEnabled)
                .withTooltip("Hides the damage titles on your screen."));
        m.add(new Toggle("Hollow Wand Announcer",
                PhantomConfig::isHollowWandEnabled, PhantomConfig::setHollowWandEnabled)
                .withTooltip("Tells your party when you use a Hollow Wand."));
        Group hideBossBar = m.group("Hide Boss Bar", PhantomConfig::isHideBossBarEnabled, PhantomConfig::setHideBossBarEnabled)
                .withTooltip("Hides the boss health bar at the top of the screen.");
        hideBossBar.add(new Toggle("Only In Kuudra",
                PhantomConfig::isHideBossBarOnlyInKuudra, PhantomConfig::setHideBossBarOnlyInKuudra)
                .withTooltip("Only hides it inside Kuudra."));
        m.add(hideBossBar);

        m.add(new Toggle("Hide Elle Dialogue",
                PhantomConfig::isHideElleDialogueEnabled, PhantomConfig::setHideElleDialogue)
                .withTooltip("Hides Elle's chat messages."));
        Group etherwarpLavaBlock = m.group("Etherwarp Lava Block", PhantomConfig::isEtherwarpLavaBlockEnabled, v -> { if (v != PhantomConfig.isEtherwarpLavaBlockEnabled()) PhantomConfig.toggleEtherwarpLavaBlock(); })
                .withTooltip("Stops you etherwarping into lava.");
        etherwarpLavaBlock.add(new Toggle("Only In Kuudra",
                PhantomConfig::isEtherwarpLavaBlockOnlyInKuudra, PhantomConfig::setEtherwarpLavaBlockOnlyInKuudra)
                .withTooltip("Only blocks it inside Kuudra."));
        m.add(etherwarpLavaBlock);
        m.add(new Toggle("Chest Tracker HUD",
                PhantomConfig::isChestTrackerVisible, PhantomConfig::setChestTrackerVisible)
                .withTooltip("Shows your chest tracking on screen."));

        Group explosion = m.group("Exploison Hider", PhantomConfig::isExplosionFilterEnabled, PhantomConfig::setExplosionFilterEnabled)
                .withTooltip("Hides explosion effects.");
        explosion.add(new Slider("Hide Radius",
                PhantomConfig::getExplosionHideRadiusRaw, PhantomConfig::setExplosionHideRadius, "")
                .withTooltip("How close an explosion has to be to get hidden."));
        m.add(explosion);

        m.add(new Toggle("Party Commands",
                PhantomConfig::isPartyCmdsEnabled, PhantomConfig::setPartyCmdsEnabled)
                .withTooltip("Lets your party use chat commands with you."));

        if (RemoteFeatureGate.isEnabled()) {
            Group shitters = m.group("Shitter List", PhantomConfig::isShitterHighlightEnabled, PhantomConfig::setShitterHighlightEnabled)
                    .withTooltip("Recolours the names of players on your shitter list.");
            shitters.add(new Toggle("Auto Kick Shitters",
                    ShitterList::isAutoKickEnabled,
                    ShitterList::setAutoKickEnabled)
                    .withTooltip("Kicks players on your shitter list from your party."));
            m.add(shitters);
        } else {
            m.add(new Toggle("Shitter List",
                    PhantomConfig::isShitterHighlightEnabled, PhantomConfig::setShitterHighlightEnabled)
                    .withTooltip("Recolours the names of players on your shitter list."));
        }

        Group split = m.group("Split Timer", PhantomConfig::isSplitTimerEnabled, PhantomConfig::setSplitTimerEnabled)
                .withTooltip("Shows the time of each part of a run.");
        split.add(new Toggle("Supply Times",
                PhantomConfig::isSupplyTimesEnabled, PhantomConfig::setSupplyTimesEnabled)
                .withTooltip("Shows how long each supply took."));
        m.add(split);

        m.add(new Toggle("Run Breakdown",
                PhantomConfig::isRunBreakdownEnabled, PhantomConfig::setRunBreakdownEnabled)
                .withTooltip("Prints each split and the total time in chat when a run ends."));

        if (RemoteFeatureGate.isEnabled()) {
            Group autoKick = m.group("Auto Kick", PhantomConfig::isAutoKickEnabled, PhantomConfig::setAutoKickEnabled)
                    .withTooltip("Kicks players who don't meet your requirements.");
            autoKick.add(new OptionalIntInput("Catacombs Level", PhantomConfig::getAkMinCatacombs, PhantomConfig::setAkMinCatacombs)
                    .withTooltip("The lowest Catacombs level a player needs."));
            autoKick.add(new OptionalIntInput("Foraging Level", PhantomConfig::getAkMinForaging, PhantomConfig::setAkMinForaging)
                    .withTooltip("The lowest Foraging level a player needs."));
            autoKick.add(new OptionalIntInput("Magical Power", PhantomConfig::getAkMinMagicalPower, PhantomConfig::setAkMinMagicalPower)
                    .withTooltip("The lowest Magical Power a player needs."));
            autoKick.add(new OptionalIntInput("Infernal Comps", PhantomConfig::getAkMinInfernal, PhantomConfig::setAkMinInfernal)
                    .withTooltip("The fewest Infernal completions a player needs."));
            autoKick.add(new OptionalIntInput("Fiery Comps", PhantomConfig::getAkMinFiery, PhantomConfig::setAkMinFiery)
                    .withTooltip("The fewest Fiery completions a player needs."));
            autoKick.add(new OptionalIntInput("Burning Comps", PhantomConfig::getAkMinBurning, PhantomConfig::setAkMinBurning)
                    .withTooltip("The fewest Burning completions a player needs."));
            autoKick.add(new OptionalIntInput("Hot Comps", PhantomConfig::getAkMinHot, PhantomConfig::setAkMinHot)
                    .withTooltip("The fewest Hot completions a player needs."));
            autoKick.add(new OptionalIntInput("Basic Comps", PhantomConfig::getAkMinBasic, PhantomConfig::setAkMinBasic)
                    .withTooltip("The fewest Basic completions a player needs."));
            autoKick.add(new Toggle("Rend",
                    PhantomConfig::isAkRequireRend, PhantomConfig::setAkRequireRend)
                    .withTooltip("Players must have Rend to stay."));
            autoKick.add(new OptionalIntInput("Gdrag Level", PhantomConfig::getAkMinGdragLevel, PhantomConfig::setAkMinGdragLevel)
                    .withTooltip("The lowest Golden Dragon pet level a player needs."));
            m.add(autoKick);

            m.add(new Toggle("Kuudra Profile Viewer",
                    PhantomConfig::isProfileViewerEnabled, PhantomConfig::setProfileViewerEnabled)
                    .withTooltip("Adds the /kuudra command and a profile prompt when players join your party."));
        }
    }
}
