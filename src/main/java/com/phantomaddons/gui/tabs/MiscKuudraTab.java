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
                PhantomConfig::getKuudraSizeScale, PhantomConfig::setKuudraSizeScale));
        m.add(new Toggle("Tuxedo Warning",
                PhantomConfig::isTuxedoWarningEnabled, PhantomConfig::setTuxedoWarningEnabled));

        m.heading("Shop and chests");
        Group shop = m.group("Shop Keybinds", PhantomConfig::isShopKeybindsEnabled, PhantomConfig::setShopKeybindsEnabled);
        shop.add(new KeyCapture("Main Key", PhantomConfig::getShopMainKey,   PhantomConfig::setShopMainKey));
        shop.add(new KeyCapture("Cannon Key", PhantomConfig::getShopCannonKey, PhantomConfig::setShopCannonKey));
        m.add(shop);

        m.add(new Toggle("Middle Click Perk Menu",
                PhantomConfig::isMiddleClickShopGuiEnabled, PhantomConfig::setMiddleClickShopGuiEnabled));

        Group profit = m.group("Profit Tracker", PhantomConfig::isProfitTrackerEnabled, PhantomConfig::setProfitTrackerEnabled);
        profit.add(new Toggle("Show During Run",
                PhantomConfig::isProfitShowDuringRun, PhantomConfig::setProfitShowDuringRun));
        profit.add(new Cycle("Armor",
                () -> PhantomConfig.isProfitArmorSalvage() ? "Salvage" : "Sell",
                () -> PhantomConfig.setProfitArmorSalvage(!PhantomConfig.isProfitArmorSalvage())));
        profit.add(new Cycle("Faction",
                () -> PhantomConfig.isProfitFactionMage() ? "Mage" : "Barbarian",
                () -> PhantomConfig.setProfitFactionMage(!PhantomConfig.isProfitFactionMage())));
        profit.add(new Toggle("Reroll Calculator",
                PhantomConfig::isProfitRerollCalc, PhantomConfig::setProfitRerollCalc));
        profit.add(new Toggle("Block Reroll On Expensive Items",
                PhantomConfig::isBlockExpensiveRerollEnabled, PhantomConfig::setBlockExpensiveRerollEnabled));
        profit.add(new Cycle("Bazaar Sell",
                () -> PhantomConfig.isProfitBazaarInstaSell() ? "Instasell" : "Sell Order",
                () -> PhantomConfig.setProfitBazaarInstaSell(!PhantomConfig.isProfitBazaarInstaSell())));
        profit.add(new Cycle("Bazaar Buy",
                () -> PhantomConfig.isProfitBazaarInstaBuy() ? "Instabuy" : "Buy Order",
                () -> PhantomConfig.setProfitBazaarInstaBuy(!PhantomConfig.isProfitBazaarInstaBuy())));
        profit.add(new Toggle("Chest Value GUI",
                PhantomConfig::isChestValueGuiEnabled, PhantomConfig::setChestValueGuiEnabled));
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
                }));
        profit.add(new RangeSlider("Pet Level", 1, 100, "%.0f",
                () -> (float) PhantomConfig.getKuudraPetLevel(),
                v  -> PhantomConfig.setKuudraPetLevel(Math.round(v))));
        profit.add(new RangeSlider("Essence of Crimson", 0, 10, "%.0f",
                () -> (float) PhantomConfig.getEssenceOfCrimson(),
                v  -> PhantomConfig.setEssenceOfCrimson(Math.round(v)))
                .withTooltip("Each level increases Crimson Essence gains by 1%, up to 10% at level 10"));
        profit.add(new RangeSlider("Echo of Essence", 0, 10, "%.0f",
                () -> (float) PhantomConfig.getEchoOfEssence(),
                v  -> PhantomConfig.setEchoOfEssence(Math.round(v)))
                .withTooltip("Each level boosts Essence of Crimson's own bonus by 3%, up to 30% at level 10 (13% total Crimson Essence gain when both are maxed)"));
        m.add(profit);

        m.add(new Toggle("Highlight Unopened Chests",
                PhantomConfig::isHighlightUnopenedChestsEnabled, PhantomConfig::setHighlightUnopenedChestsEnabled));

        m.heading("Runs");
        Group kicked = m.group("Kicked Notification", PhantomConfig::isKickedNotificationEnabled, PhantomConfig::setKickedNotificationEnabled);
        m.sound(kicked, PhantomConfig.SOUND_KICKED);
        m.add(kicked);
        Group autoRequeue = m.group("Auto Requeue", PhantomConfig::isAutoRequeueEnabled, PhantomConfig::setAutoRequeueEnabled);
        autoRequeue.add(new Toggle("Party Chat Message",
                PhantomConfig::isAutoRequeueMessageEnabled, PhantomConfig::setAutoRequeueMessageEnabled));
        m.add(autoRequeue);
        m.heading("Hiding and extras");
        m.add(new Toggle("Hide Damage Title",
                PhantomConfig::isHideDamageTitleEnabled, PhantomConfig::setHideDamageTitleEnabled));
        m.add(new Toggle("Hollow Wand Announcer",
                PhantomConfig::isHollowWandEnabled, PhantomConfig::setHollowWandEnabled));
        Group hideBossBar = m.group("Hide Boss Bar", PhantomConfig::isHideBossBarEnabled, PhantomConfig::setHideBossBarEnabled);
        hideBossBar.add(new Toggle("Only In Kuudra",
                PhantomConfig::isHideBossBarOnlyInKuudra, PhantomConfig::setHideBossBarOnlyInKuudra));
        m.add(hideBossBar);

        m.add(new Toggle("Hide Elle Dialogue",
                PhantomConfig::isHideElleDialogueEnabled, PhantomConfig::setHideElleDialogue));
        Group etherwarpLavaBlock = m.group("Etherwarp Lava Block", PhantomConfig::isEtherwarpLavaBlockEnabled, v -> { if (v != PhantomConfig.isEtherwarpLavaBlockEnabled()) PhantomConfig.toggleEtherwarpLavaBlock(); });
        etherwarpLavaBlock.add(new Toggle("Only In Kuudra",
                PhantomConfig::isEtherwarpLavaBlockOnlyInKuudra, PhantomConfig::setEtherwarpLavaBlockOnlyInKuudra));
        m.add(etherwarpLavaBlock);
        m.add(new Toggle("Chest Tracker HUD",
                PhantomConfig::isChestTrackerVisible, PhantomConfig::setChestTrackerVisible));

        Group explosion = m.group("Exploison Hider", PhantomConfig::isExplosionFilterEnabled, PhantomConfig::setExplosionFilterEnabled);
        explosion.add(new Slider("Hide Radius",
                PhantomConfig::getExplosionHideRadiusRaw, PhantomConfig::setExplosionHideRadius, ""));
        m.add(explosion);

        m.add(new Toggle("Party Commands",
                PhantomConfig::isPartyCmdsEnabled, PhantomConfig::setPartyCmdsEnabled));

        if (RemoteFeatureGate.isEnabled()) {
            Group shitters = m.group("Shitter List", PhantomConfig::isShitterHighlightEnabled, PhantomConfig::setShitterHighlightEnabled)
                    .withTooltip("Recolours the names of players on your shitter list");
            shitters.add(new Toggle("Auto Kick Shitters",
                    ShitterList::isAutoKickEnabled,
                    ShitterList::setAutoKickEnabled));
            m.add(shitters);
        } else {
            m.add(new Toggle("Shitter List",
                    PhantomConfig::isShitterHighlightEnabled, PhantomConfig::setShitterHighlightEnabled)
                    .withTooltip("Recolours the names of players on your shitter list"));
        }

        Group split = m.group("Split Timer", PhantomConfig::isSplitTimerEnabled, PhantomConfig::setSplitTimerEnabled);
        split.add(new Toggle("Supply Times",
                PhantomConfig::isSupplyTimesEnabled, PhantomConfig::setSupplyTimesEnabled));
        m.add(split);

        m.add(new Toggle("Run Breakdown",
                PhantomConfig::isRunBreakdownEnabled, PhantomConfig::setRunBreakdownEnabled)
                .withTooltip("Prints each split and the total time in chat when a run ends"));

        if (RemoteFeatureGate.isEnabled()) {
            Group autoKick = m.group("Auto Kick", PhantomConfig::isAutoKickEnabled, PhantomConfig::setAutoKickEnabled);
            autoKick.add(new OptionalIntInput("Catacombs Level", PhantomConfig::getAkMinCatacombs, PhantomConfig::setAkMinCatacombs));
            autoKick.add(new OptionalIntInput("Foraging Level", PhantomConfig::getAkMinForaging, PhantomConfig::setAkMinForaging));
            autoKick.add(new OptionalIntInput("Magical Power", PhantomConfig::getAkMinMagicalPower, PhantomConfig::setAkMinMagicalPower));
            autoKick.add(new OptionalIntInput("Infernal Comps", PhantomConfig::getAkMinInfernal, PhantomConfig::setAkMinInfernal));
            autoKick.add(new OptionalIntInput("Fiery Comps", PhantomConfig::getAkMinFiery, PhantomConfig::setAkMinFiery));
            autoKick.add(new OptionalIntInput("Burning Comps", PhantomConfig::getAkMinBurning, PhantomConfig::setAkMinBurning));
            autoKick.add(new OptionalIntInput("Hot Comps", PhantomConfig::getAkMinHot, PhantomConfig::setAkMinHot));
            autoKick.add(new OptionalIntInput("Basic Comps", PhantomConfig::getAkMinBasic, PhantomConfig::setAkMinBasic));
            autoKick.add(new Toggle("Rend",
                    PhantomConfig::isAkRequireRend, PhantomConfig::setAkRequireRend));
            autoKick.add(new OptionalIntInput("Gdrag Level", PhantomConfig::getAkMinGdragLevel, PhantomConfig::setAkMinGdragLevel));
            m.add(autoKick);

            m.add(new Toggle("Kuudra Profile Viewer",
                    PhantomConfig::isProfileViewerEnabled, PhantomConfig::setProfileViewerEnabled)
                    .withTooltip("/kuudra and party join prompt"));
        }
    }
}
