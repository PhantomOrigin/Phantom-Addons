package com.kuudrahelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kuudrahelper.features.customisation.items.ItemCustomization;
import com.kuudrahelper.features.customisation.items.ItemTransformSettings;
import com.kuudrahelper.utils.KuudraTierDetector;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class KuudraConfig {

    public static final boolean API_KEY_FEATURES_UNLOCKED = false; // Toggles the ability to use Hypixel API features. MEANS DONT TOUCH NERDS!

    private static final Gson GSON        = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("phantomaddons.json");

    public enum RoleMode       { DPS, STUN, AUTO }
    public enum LavaMode       { DEFAULT, COLOURED }
    public enum WaypointType   { CIRCLE, SQUARE }
    public enum SecondSupplyPreference { DOUBLE_PEARL, ETHERWARP }
    public enum KuudraTalisman { NONE, KIDNEY, LUNG, HEART }

    // ── General ───────────────────────────────────────────────────────────────

    private static RoleMode roleMode          = RoleMode.AUTO;
    private static int      dpsValue          = 40;
    private static int      stunValue         = 9;
    private static int      dpsRefillAmount   = 10;
    private static int      autoGfsDisableHpPercent = 40;
    private static boolean  eatenTimerEnabled              = false;
    private static boolean  eatenTimerSubtractPingEnabled  = false;
    private static boolean  etherwarpLavaBlock = true;
    private static boolean  pickoblockEnabled  = false;
    private static boolean  hideElleDialogue   = false;
    private static boolean  autoGfsEnabled     = true;

    // ── Lava ──────────────────────────────────────────────────────────────────

    private static LavaMode lavaMode          = LavaMode.DEFAULT;
    private static float    lavaOpacity       = 1.0f;
    private static int      lavaColor         = 0xFFAA0000;
    private static boolean  lavaAsWater       = false;
    private static boolean  lavaColorOverride = false;

    // ── Water ─────────────────────────────────────────────────────────────────

    private static float    waterOpacity       = 1.0f;
    private static int      waterColor         = 0xFF2244AA;
    private static boolean  waterAsLava        = false;
    private static boolean  waterColorOverride = false;

    // ── Misc ──────────────────────────────────────────────────────────────────

    private static boolean fastDpsWarning           = true;
    private static boolean chestTrackerVisible      = true;
    private static boolean soloDetectorEnabled      = true;
    private static boolean cannonAutoClose          = false;
    private static boolean kuudraDirectionEnabled   = false;
    private static boolean shopKeybindsEnabled      = false;
    private static int     shopMainKey              = 49;
    private static int     shopCannonKey            = 50;
    private static boolean explosionFilterEnabled   = false;
    private static float   explosionHideRadius      = 0.3f;

    // ── Auto Kick ─────────────────────────────────────────────────────────────
    // Thresholds are "minimum required"; -1 means the field is empty/unset and isn't checked.

    private static boolean autoKickEnabled       = false;
    private static boolean profileViewerEnabled  = true;
    private static int     akMinCatacombs        = -1;
    private static int     akMinForaging         = -1;
    private static int     akMinMagicalPower     = -1;
    private static int     akMinInfernal         = -1;
    private static int     akMinFiery            = -1;
    private static int     akMinBurning          = -1;
    private static int     akMinHot              = -1;
    private static int     akMinBasic            = -1;
    private static boolean akRequireRend         = false;
    private static int     akMinGdragLevel       = -1;
    private static float   explosionSizeMultiplier  = 0.33f;
    private static boolean chestAnnouncerEnabled    = true;
    private static boolean partyCmdsEnabled         = true;
    private static boolean autoRequeueEnabled       = true;
    private static boolean autoRequeueMessageEnabled = true;
    private static boolean autoUpdatesEnabled       = true;
    private static boolean developerFeaturesEnabled = false;
    private static boolean autoSprintEnabled        = false;
    private static boolean slotBindsEnabled         = false;
    private static int     slotBindSetKey           = -1;
    private static int     slotBindShowKey          = -1;
    private static final java.util.LinkedHashMap<Integer,Integer> slotBindings = new java.util.LinkedHashMap<>();

    // ── Profit tracker ───────────────────────────────────────────────────────────

    public enum KuudraPetRarity {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY;
        public double bonusPerLevel() {
            return switch (this) {
                case RARE          -> 0.0015;
                case EPIC, LEGENDARY -> 0.002;
                default            -> 0.0;
            };
        }
        public double essenceMultiplier(int level) { return 1.0 + bonusPerLevel() * level; }
    }

    private static boolean         profitTrackerForced    = false; // not saved — session-only
    private static boolean         profitTrackerEnabled   = false;
    private static boolean         profitShowDuringRun    = false;
    private static boolean         profitArmorSalvage     = true;  // true = salvage, false = sell AH
    private static boolean         profitFactionMage      = true;  // true = mage (mycelium), false = barbarian (red sand)
    private static boolean         profitHighlightChests  = true;
    private static boolean         profitRerollCalc       = true;
    private static boolean         profitBazaarInstaSell  = true;  // sell items: instasell vs sell order
    private static boolean         profitBazaarInstaBuy   = false; // buy items: instabuy vs buy order (false = buy order = cheaper)
    private static float           profitHudX             = 0.01f;
    private static float           profitHudY             = 0.5f;
    private static float           profitHudScale         = 1.0f;
    private static KuudraPetRarity kuudraPetRarity        = KuudraPetRarity.LEGENDARY;
    private static int             kuudraPetLevel         = 100;

    private static boolean chestValueGuiEnabled   = true;
    private static float   chestValueHudX         = 0.3f;
    private static float   chestValueHudY         = 0.3f;
    private static float   chestValueHudScale     = 1.0f;

    public static boolean         isProfitTrackerEnabled()    { return profitTrackerEnabled; }
    public static boolean         isProfitTrackerForced()     { return profitTrackerForced; }
    public static boolean         toggleProfitTrackerForced() { return profitTrackerForced = !profitTrackerForced; }
    public static boolean         isProfitShowDuringRun()     { return profitShowDuringRun; }
    public static boolean         isProfitArmorSalvage()      { return profitArmorSalvage; }
    public static boolean         isProfitFactionMage()       { return profitFactionMage; }
    public static boolean         isProfitHighlightChests()   { return profitHighlightChests; }
    public static boolean         isProfitRerollCalc()        { return profitRerollCalc; }
    public static boolean         isProfitBazaarInstaSell()   { return profitBazaarInstaSell; }
    public static boolean         isProfitBazaarInstaBuy()    { return profitBazaarInstaBuy; }
    public static float           getProfitHudX()             { return profitHudX; }
    public static float           getProfitHudY()             { return profitHudY; }
    public static float           getProfitHudScale()         { return profitHudScale; }
    public static KuudraPetRarity getKuudraPetRarity()        { return kuudraPetRarity; }
    public static int             getKuudraPetLevel()         { return kuudraPetLevel; }
    public static double          getKuudraPetEssenceMultiplier() {
        return kuudraPetRarity.essenceMultiplier(kuudraPetLevel);
    }

    public static void setProfitTrackerEnabled(boolean v)       { profitTrackerEnabled  = v; save(); }
    public static void setProfitShowDuringRun(boolean v)        { profitShowDuringRun   = v; save(); }
    public static void setProfitArmorSalvage(boolean v)         { profitArmorSalvage    = v; save(); }
    public static void setProfitFactionMage(boolean v)          { profitFactionMage     = v; save(); }
    public static void setProfitHighlightChests(boolean v)      { profitHighlightChests = v; save(); }
    public static void setProfitRerollCalc(boolean v)           { profitRerollCalc      = v; save(); }
    public static void setProfitBazaarInstaSell(boolean v)      { profitBazaarInstaSell = v; save(); }
    public static void setProfitBazaarInstaBuy(boolean v)       { profitBazaarInstaBuy  = v; save(); }
    public static void setProfitHudX(float v)                   { profitHudX    = v; save(); }
    public static void setProfitHudY(float v)                   { profitHudY    = v; save(); }
    public static void setProfitHudScale(float v)               { profitHudScale = v; save(); }
    public static void setKuudraPetRarity(KuudraPetRarity v)    { kuudraPetRarity = v; save(); }
    public static void setKuudraPetLevel(int v)                 { kuudraPetLevel = Math.clamp(v, 1, 100); save(); }

    public static boolean isChestValueGuiEnabled()  { return chestValueGuiEnabled; }
    public static float   getChestValueHudX()       { return chestValueHudX; }
    public static float   getChestValueHudY()       { return chestValueHudY; }
    public static float   getChestValueHudScale()   { return chestValueHudScale; }

    public static void setChestValueGuiEnabled(boolean v) { chestValueGuiEnabled = v; save(); }
    public static void setChestValueHudX(float v)         { chestValueHudX    = v; save(); }
    public static void setChestValueHudY(float v)         { chestValueHudY    = v; save(); }
    public static void setChestValueHudScale(float v)     { chestValueHudScale = v; save(); }

    // ── Pearl ─────────────────────────────────────────────────────────────────

    private static boolean        pearlWaypointsEnabled = true;
    private static boolean        showAllWaypoints      = true;
    private static boolean        pearlFlatEnabled      = true;
    private static boolean        pearlSkyEnabled       = true;
    private static boolean        pearlDoubleEnabled    = true;
    private static float          doublePearlDelayS     = 0.2f;
    private static WaypointType   waypointType          = WaypointType.CIRCLE;
    private static boolean        waypointFill          = true;
    private static boolean        pearlTickUpdate       = true;
    private static boolean        dropLocationsEnabled  = true;
    private static boolean        waypointLinesEnabled        = false;
    private static boolean        waypointLinesSuppliesEnabled   = true;
    private static boolean        waypointLinesFlatPearlsEnabled = true;
    private static SecondSupplyPreference secondSupplyPreference = SecondSupplyPreference.DOUBLE_PEARL;
    private static boolean        pearlTimerEnabled     = true;
    private static float          pearlTimerHeight      = 0.35f;
    private static float          pearlTimerSize        = 0.5f;
    private static float          pearlCircleSize       = 0.5f;
    private static KuudraTalisman kuudraTalisman        = KuudraTalisman.NONE;
    private static int            lowPing               = 0;
    private static float          waypointFillAlpha     = 0.25f;
    private static float          beaconAlpha           = 0.63f;
    private static int            wpColNormal           = 0xFFFFFF;
    private static int            wpColCorrect          = 0xFF4444;
    private static int            wpColHovered          = 0xFFAA00;
    private static int            wpColReady            = 0x33FF33;
    private static int            beaconColNormal       = 0xFFFFFF;
    private static int            beaconColCorrect      = 0x00C800;
    private static float          buildBeaconAlpha      = 0.63f;
    private static boolean        blockSlot9Enabled          = false;
    private static boolean        stunPreviewEnabled         = false;
    private static boolean        buildBeaconsEnabled        = false;
    private static boolean        elleHighlightEnabled       = false;
    private static boolean        rendDamageEnabled          = false;
    private static boolean        rendTrackerEnabled         = false;
    private static boolean        backboneProgressBarEnabled = false;
    private static boolean        backboneProgressBarOutsideKuudraEnabled = false;
    private static float          backboneProgressBarHudX     = 0.5f;
    private static float          backboneProgressBarHudY     = 0.6f;
    private static float          backboneProgressBarHudScale = 1.0f;
    private static boolean        kuudraHighlightEnabled     = false;
    private static boolean        kuudraHighlightFilled      = false;
    private static boolean        etherwarpSlotBlockerEnabled = false;
    private static boolean        supplyWaypointsEnabled       = false; // legacy — kept for migration
    private static boolean        supplyBeaconsEnabled         = false;
    private static boolean        noPreAnnounceEnabled         = false;
    private static boolean        supplyLocationAnnounceEnabled = false;
    private static boolean        supplyProgressHudEnabled     = false;
    private static boolean        buildProgressHudEnabled      = false;
    private static boolean        announceFreshEnabled         = false;
    private static boolean        supplyHitboxEnabled          = false;
    private static boolean        supplyRodRadiusEnabled       = false;
    private static boolean        supplyPearlHitboxEnabled     = false;
    private static boolean        pearlRefillEnabled           = false;
    private static boolean        pearlRefillOutsideKuudraEnabled = false;
    private static boolean        hideSelfieEnabled            = false;
    private static boolean        preventPlacingPlayerHeadsEnabled      = false;
    private static boolean        preventPlacingPlayerHeadsExceptGarden = true;
    private static boolean        preventPlacingWeaponsEnabled          = false;
    private static boolean        supplyGiantHitboxEnabled              = false;
    private static boolean        giantHitboxEnabled                    = false;
    private static boolean        giantHitboxFilled                     = false;
    private static float          giantHitboxFillOpacity                = 0.05f;
    private static int            giantHitboxColor                      = 0xFFFFFF;

    // ── HUD layout ────────────────────────────────────────────────────────────

    private static float mountTimerHudX     = 0.5f;
    private static float mountTimerHudY     = 0.56f;
    private static float mountTimerHudScale = 1.0f;
    private static float directionHudX     = 0.5f;
    private static float directionHudY     = 0.25f;
    private static float directionHudScale = 1.0f;
    private static float splitHudX         = 0.005f;
    private static float splitHudY         = 0.01f;
    private static float splitHudScale     = 1.0f;
    private static float pearlTitleHudX    = 0.5f;
    private static float pearlTitleHudY    = 0.5f;
    private static float pearlTitleHudScale = 1.0f;
    private static boolean smoothCratePickupEnabled = false;
    private static float supplyProgressHudX     = 0.5f;
    private static float supplyProgressHudY     = 0.35f;
    private static float supplyProgressHudScale = 1.0f;
    private static float buildProgressHudX      = 0.5f;
    private static float buildProgressHudY      = 0.45f;
    private static float buildProgressHudScale  = 1.0f;
    private static float notificationHudX       = 0.5f;
    private static float notificationHudY       = 0.15f;
    private static float notificationHudScale   = 1.5f;
    private static float cratePriorityHudX      = 0.5f;
    private static float cratePriorityHudY      = 0.6f;
    private static float cratePriorityHudScale  = 2.0f;

    private static boolean supplyRecoveryMsgEnabled = false;
    private static boolean freshNotifyEnabled       = false;
    private static boolean buildStartedNotifyEnabled= false;
    private static boolean fastDpsNotifyEnabled       = false;
    private static boolean soloNotifyEnabled          = false;
    private static boolean noPreNotifyEnabled         = false;
    private static boolean supplyGrabbedNotifyEnabled = false;
    private static boolean supplyDroppedNotifyEnabled = false;
    private static boolean cratePriorityEnabled     = false;
    private static boolean hideArmorStandsEnabled      = false;
    private static boolean hideArmorStandsBuild        = true;
    private static boolean hideArmorStandsRightCannon  = true;
    private static boolean hideArmorStandsLeftCannon   = true;
    private static boolean hideArmorStandsShop         = true;
    private static boolean hideArmorStandsOthers       = true;

    private static boolean kuudraHpHudEnabled       = false;
    private static boolean kuudraHpShowRaw          = false;
    private static boolean kuudraHpHideBar          = false;
    private static boolean hollowWandEnabled        = false;
    private static boolean kickedNotificationEnabled = false;
    private static boolean hideBossBarEnabled       = false;
    private static boolean hideFallingBlocksEnabled  = false;
    private static boolean manaDrainAnnouncerEnabled = false;
    private static boolean hideEntityFireEnabled  = false;
    private static boolean lavaBobberFixEnabled    = false;
    private static boolean legacyRodPhysicsEnabled = false;
    private static boolean hideDamageTitleEnabled  = false;
    private static boolean hideDeadEntitiesEnabled    = false;
    private static boolean etherwarpWaypointsEnabled  = false;
    private static float   selfPlayerScale        = 100.0f;
    private static float   otherPlayerScale       = 100.0f;
    private static float   kuudraSizeScale        = 100.0f;

    private static float kuudraHpHudX     = 0.5f;
    private static float kuudraHpHudY     = 0.07f;
    private static float kuudraHpHudScale = 1.0f;

    private static final int[][] PEARL_DELAY = {
            {0, 3000, 4000, 5000, 6000, 6000},
            {0, 2750, 3750, 4500, 5500, 5500},
            {0, 2500, 3250, 4000, 5000, 5000},
            {0, 2250, 3000, 3500, 4250, 4250},
    };

    // ── Dungeons ──────────────────────────────────────────────────────────────

    // ── Wardrobe keybinds ─────────────────────────────────────────────────────

    private static boolean wardrobeEnabled      = false;
    private static int     wardrobeOpenKey      = -1;
    private static int     statsOpenKey         = -1;
    private static int     petsOpenKey          = -1;
    private static int     eqWardrobeOpenKey    = -1;
    private static int     loadoutsOpenKey      = -1;
    private static int[]   wardrobeSlotKeys     = {49,50,51,52,53,54,55,56,57};
    private static int[]   loadoutSlotKeys      = {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
    private static int     wardrobeNextPageKey  = 262; // GLFW_KEY_RIGHT
    private static int     wardrobePrevPageKey  = 263; // GLFW_KEY_LEFT
    private static int     wardrobeUnequipKey   = 85;  // GLFW_KEY_U
    private static boolean wardrobeDisableUnequipEnabled = true;
    private static boolean wardrobeAutoCloseEnabled      = true;
    private static int     lastEquippedWardrobeSlot      = -1;

    private static boolean autoGfsToxic    = false;
    private static boolean autoGfsTwilight = false;
    private static int     toxicAmount     = 10;
    private static int     twilightAmount  = 10;

    // ── Chest tracker ─────────────────────────────────────────────────────────

    private static int chestTotal   = 0;
    private static int chestSuccess = 0;
    private static int chestFail    = 0;

    // ── Split timer ───────────────────────────────────────────────────────────

    private static boolean splitTimerEnabled  = true;
    private static boolean supplyTimesEnabled = true;

    private static double[][] splitPbs    = defaultPbs();
    private static double[]   totalRunPbs = defaultTotalPbs();
    private static PbRecord[] pbRecords   = new PbRecord[6];

    private static double[][] defaultPbs() {
        double[][] a = new double[6][7];
        for (double[] row : a) Arrays.fill(row, 9999.0);
        return a;
    }

    private static double[] defaultTotalPbs() {
        double[] a = new double[6];
        Arrays.fill(a, 9999.0);
        return a;
    }

    // ── Item customisation ────────────────────────────────────────────────────

    private static boolean itemCustomizationEnabled = true;

    // ── Getters ───────────────────────────────────────────────────────────────

    // General
    public static RoleMode getRoleMode()                 { return roleMode; }
    public static boolean  isDpsMode()                   { return roleMode == RoleMode.DPS; }
    public static boolean  isStunMode()                  { return roleMode == RoleMode.STUN; }
    public static boolean  isAutoMode()                  { return roleMode == RoleMode.AUTO; }
    public static int      getDpsValue()                 { return dpsValue; }
    public static int      getDpsRefillAmount()          { return dpsRefillAmount; }
    public static int      getAutoGfsDisableHpPercent()  { return autoGfsDisableHpPercent; }
    public static int      getStunValue()                { return stunValue; }
    public static boolean  isEatenTimerEnabled()              { return eatenTimerEnabled; }
    public static boolean  isEatenTimerSubtractPingEnabled()  { return eatenTimerSubtractPingEnabled; }
    public static boolean  isPickoblockEnabled()         { return pickoblockEnabled; }
    public static boolean  isEtherwarpLavaBlockEnabled() { return etherwarpLavaBlock; }
    public static boolean  isHideElleDialogueEnabled()   { return hideElleDialogue; }
    public static boolean  isAutoGfsEnabled()            { return autoGfsEnabled; }

    // Lava
    public static LavaMode getLavaMode()         { return lavaMode; }
    public static float    getLavaOpacity()      { return lavaOpacity; }
    public static int      getLavaColor()        { return lavaColor; }
    public static boolean  isLavaAsWater()       { return lavaAsWater; }
    public static boolean  isLavaColorOverride() { return lavaColorOverride; }

    // Water
    public static float    getWaterOpacity()      { return waterOpacity; }
    public static int      getWaterColor()        { return waterColor; }
    public static boolean  isWaterAsLava()        { return waterAsLava; }
    public static boolean  isWaterColorOverride() { return waterColorOverride; }

    // Misc
    public static boolean isFastDpsWarningEnabled()   { return fastDpsWarning; }
    public static boolean isChestTrackerVisible()     { return chestTrackerVisible; }
    public static boolean isSoloDetectorEnabled()     { return soloDetectorEnabled; }
    public static boolean isCannonAutoCloseEnabled()  { return cannonAutoClose; }
    public static boolean isKuudraDirectionEnabled()   { return kuudraDirectionEnabled; }
    public static boolean isShopKeybindsEnabled()     { return shopKeybindsEnabled; }
    public static int     getShopMainKey()            { return shopMainKey; }
    public static int     getShopCannonKey()          { return shopCannonKey; }
    public static boolean isExplosionFilterEnabled()  { return explosionFilterEnabled; }
    public static float   getExplosionHideRadius()    { return explosionHideRadius * 50f; }
    public static float   getExplosionHideRadiusRaw() { return explosionHideRadius; }
    public static float   getExplosionSizeMultiplier(){ return explosionSizeMultiplier * 3.0f; }
    public static float   getExplosionSizeRaw()       { return explosionSizeMultiplier; }
    public static boolean isChestAnnouncerEnabled()   { return chestAnnouncerEnabled; }

    public static boolean isAutoKickEnabled()     { return API_KEY_FEATURES_UNLOCKED && autoKickEnabled; }
    public static boolean isProfileViewerEnabled() { return API_KEY_FEATURES_UNLOCKED && profileViewerEnabled; }
    public static int     getAkMinCatacombs()     { return akMinCatacombs; }
    public static int     getAkMinForaging()      { return akMinForaging; }
    public static int     getAkMinMagicalPower()  { return akMinMagicalPower; }
    public static int     getAkMinInfernal()      { return akMinInfernal; }
    public static int     getAkMinFiery()         { return akMinFiery; }
    public static int     getAkMinBurning()       { return akMinBurning; }
    public static int     getAkMinHot()           { return akMinHot; }
    public static int     getAkMinBasic()         { return akMinBasic; }
    public static boolean isAkRequireRend()       { return akRequireRend; }
    public static int     getAkMinGdragLevel()    { return akMinGdragLevel; }
    public static boolean isPartyCmdsEnabled()        { return partyCmdsEnabled; }
    public static boolean isAutoRequeueEnabled()      { return autoRequeueEnabled; }
    public static boolean isAutoRequeueMessageEnabled() { return autoRequeueMessageEnabled; }
    public static boolean isAutoUpdatesEnabled()      { return autoUpdatesEnabled; }
    public static boolean isDeveloperFeaturesEnabled() { return developerFeaturesEnabled; }
    public static boolean isAutoSprintEnabled()       { return autoSprintEnabled; }
    public static boolean isSlotBindsEnabled()        { return slotBindsEnabled; }
    public static int     getSlotBindSetKey()          { return slotBindSetKey; }
    public static int     getSlotBindShowKey()         { return slotBindShowKey; }
    public static void    setSlotBindShowKey(int v)    { slotBindShowKey = v; save(); }
    public static java.util.LinkedHashMap<Integer,Integer> getSlotBindings() { return slotBindings; }
    public static void    setSlotBindSetKey(int v)    { slotBindSetKey = v; save(); }

    // Pearl
    public static boolean        isPearlWaypointsEnabled() { return pearlWaypointsEnabled; }
    public static boolean        isShowAllWaypoints()      { return showAllWaypoints; }
    public static boolean        isPearlFlatEnabled()      { return pearlFlatEnabled; }
    public static boolean        isPearlSkyEnabled()       { return pearlSkyEnabled; }
    public static boolean        isPearlDoubleEnabled()    { return pearlDoubleEnabled; }
    public static float          getDoublePearlDelayS()    { return doublePearlDelayS; }
    public static long           getDoublePearlDelayMs()   { return (long)(doublePearlDelayS * 1000); }
    public static WaypointType   getWaypointType()         { return waypointType; }
    public static boolean        isWaypointFillEnabled()   { return waypointFill; }
    public static boolean        isPearlTickUpdate()       { return pearlTickUpdate; }
    public static boolean        isDropLocationsEnabled()  { return dropLocationsEnabled; }
    public static boolean        isWaypointLinesEnabled()            { return waypointLinesEnabled; }
    public static boolean        isWaypointLinesSuppliesEnabled()    { return waypointLinesSuppliesEnabled; }
    public static boolean        isWaypointLinesFlatPearlsEnabled()  { return waypointLinesFlatPearlsEnabled; }
    public static SecondSupplyPreference getSecondSupplyPreference() { return secondSupplyPreference; }
    public static boolean        isPearlTimerEnabled()     { return pearlTimerEnabled; }
    public static float          getPearlTimerHeight()     { return pearlTimerHeight; }
    public static float          getPearlTimerSize()       { return pearlTimerSize; }
    public static float          getPearlCircleSize()      { return pearlCircleSize; }
    public static KuudraTalisman getKuudraTalisman()       { return kuudraTalisman; }
    public static int            getLowPing()              { return lowPing; }
    public static float          getWaypointFillAlpha()    { return waypointFillAlpha; }
    public static float          getBeaconAlpha()          { return beaconAlpha; }
    public static int            getWpColNormal()          { return wpColNormal; }
    public static int            getWpColCorrect()         { return wpColCorrect; }
    public static int            getWpColHovered()         { return wpColHovered; }
    public static int            getWpColReady()           { return wpColReady; }
    public static int            getBeaconColNormal()      { return beaconColNormal; }
    public static int            getBeaconColCorrect()     { return beaconColCorrect; }
    public static float          getBuildBeaconAlpha()     { return buildBeaconAlpha; }
    public static boolean        isBlockSlot9Enabled()     { return blockSlot9Enabled; }
    public static boolean        isStunPreviewEnabled()           { return stunPreviewEnabled; }
    public static boolean        isBuildBeaconsEnabled()         { return buildBeaconsEnabled; }
    public static boolean        isElleHighlightEnabled()        { return elleHighlightEnabled; }
    public static boolean        isRendDamageEnabled()           { return rendDamageEnabled; }
    public static boolean        isRendTrackerEnabled()          { return rendTrackerEnabled; }
    public static boolean        isBackboneProgressBarEnabled()   { return backboneProgressBarEnabled; }
    public static boolean        isBackboneProgressBarOutsideKuudraEnabled() { return backboneProgressBarOutsideKuudraEnabled; }
    public static float          getBackboneProgressBarHudX()     { return backboneProgressBarHudX; }
    public static float          getBackboneProgressBarHudY()     { return backboneProgressBarHudY; }
    public static float          getBackboneProgressBarHudScale() { return backboneProgressBarHudScale; }
    public static boolean        isKuudraHighlightEnabled()      { return kuudraHighlightEnabled; }
    public static boolean        isKuudraHighlightFilled()       { return kuudraHighlightFilled; }
    public static boolean        isEtherwarpSlotBlockerEnabled()  { return etherwarpSlotBlockerEnabled; }
    public static boolean        isSupplyWaypointsEnabled()        { return supplyWaypointsEnabled; }
    public static boolean        isSupplyBeaconsEnabled()          { return supplyBeaconsEnabled; }
    public static boolean        isNoPreAnnounceEnabled()          { return noPreAnnounceEnabled; }
    public static boolean        isSupplyLocationAnnounceEnabled() { return supplyLocationAnnounceEnabled; }
    public static boolean        isSupplyProgressHudEnabled()      { return supplyProgressHudEnabled; }
    public static boolean        isBuildProgressHudEnabled()       { return buildProgressHudEnabled; }
    public static boolean        isAnnounceFreshEnabled()          { return announceFreshEnabled; }
    public static boolean        isSupplyHitboxEnabled()           { return supplyHitboxEnabled; }
    public static boolean        isSupplyRodRadiusEnabled()        { return supplyRodRadiusEnabled; }
    public static boolean        isSupplyPearlHitboxEnabled()      { return supplyPearlHitboxEnabled; }
    public static boolean        isPearlRefillEnabled()            { return pearlRefillEnabled; }
    public static boolean        isPearlRefillOutsideKuudraEnabled() { return pearlRefillOutsideKuudraEnabled; }
    public static boolean        isHideSelfieEnabled()             { return hideSelfieEnabled; }
    public static boolean        isPreventPlacingPlayerHeadsEnabled()      { return preventPlacingPlayerHeadsEnabled; }
    public static boolean        isPreventPlacingPlayerHeadsExceptGarden() { return preventPlacingPlayerHeadsExceptGarden; }
    public static boolean        isPreventPlacingWeaponsEnabled()          { return preventPlacingWeaponsEnabled; }
    public static boolean        isSupplyGiantHitboxEnabled()              { return supplyGiantHitboxEnabled; }
    public static boolean        isGiantHitboxEnabled()                    { return giantHitboxEnabled; }
    public static boolean        isGiantHitboxFilled()                     { return giantHitboxFilled; }
    public static float          getGiantHitboxFillOpacity()               { return giantHitboxFillOpacity; }
    public static int            getGiantHitboxColor()                     { return giantHitboxColor; }

    public static int getPickupDurationMs() {
        int tier = KuudraTierDetector.getTier();
        if (tier < 1 || tier > 5) tier = 5;
        return PEARL_DELAY[kuudraTalisman.ordinal()][tier];
    }

    // Dungeons
    public static boolean isAutoGfsToxicEnabled()    { return autoGfsToxic; }
    public static boolean isAutoGfsTwilightEnabled() { return autoGfsTwilight; }
    public static int     getToxicAmount()           { return toxicAmount; }
    public static int     getTwilightAmount()        { return twilightAmount; }

    // Chest tracker
    public static int getChestTotal()   { return chestTotal; }
    public static int getChestSuccess() { return chestSuccess; }
    public static int getChestFail()    { return chestFail; }

    // Split timer
    public static boolean isSplitTimerEnabled()  { return splitTimerEnabled; }
    public static boolean isSupplyTimesEnabled() { return supplyTimesEnabled; }

    // Item customisation
    public static boolean isItemCustomizationEnabled() { return itemCustomizationEnabled; }

    public static double[] getSplitPb(int tier) {
        if (tier < 1 || tier > 5) tier = 5;
        return splitPbs[tier];
    }

    public static void setSplitPb(int tier, double[] pb) {
        if (tier < 1 || tier > 5) return;
        splitPbs[tier] = pb.clone();
        save();
    }

    public static double getTotalRunPb(int tier) {
        if (tier < 1 || tier > 5) tier = 5;
        return totalRunPbs[tier];
    }

    public static void setTotalRunPb(int tier, double time) {
        if (updateTotalRunPb(tier, time)) save();
    }

    public static int getHighestTierPlayed() {
        for (int t = 5; t >= 1; t--) if (totalRunPbs[t] < 9999) return t;
        return 5;
    }

    public static PbRecord getPbRecord(int tier) {
        if (tier < 1 || tier > 5) tier = 5;
        return pbRecords[tier];
    }

    public static void setPbRecord(int tier, PbRecord record) {
        if (tier < 1 || tier > 5) return;
        pbRecords[tier]   = record;
        totalRunPbs[tier] = record.totalTime;
        if (record.splits != null && record.splits.length == 7)
            splitPbs[tier] = record.splits.clone();
        save();
    }

    public static boolean updateTotalRunPb(int tier, double time) {
        if (tier < 1 || tier > 5) return false;
        if (time >= totalRunPbs[tier]) return false;
        totalRunPbs[tier] = time;
        if (pbRecords[tier] == null) pbRecords[tier] = new PbRecord();
        pbRecords[tier].totalTime = time;
        return true;
    }

    public static String formatTime(double seconds) {
        if (seconds >= 9999) return "N/A";
        if (seconds >= 60) {
            int mins = (int)(seconds / 60);
            int secs = (int)(seconds % 60);
            return String.format("%d:%02ds", mins, secs);
        }
        return String.format("%.2fs", seconds);
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    // General
    public static void setRoleMode(RoleMode m)          { roleMode = m;           save(); }
    public static void setDpsValue(int v)               { dpsValue = v;           save(); }
    public static void setDpsRefillAmount(int v)        { dpsRefillAmount = Math.max(4, v); save(); }
    public static void setAutoGfsDisableHpPercent(int v) { autoGfsDisableHpPercent = Math.max(25, Math.min(100, v)); save(); }
    public static void setStunValue(int v)              { stunValue = v;          save(); }
    public static void setEatenTimerEnabled(boolean v)             { eatenTimerEnabled = v;             save(); }
    public static void setEatenTimerSubtractPingEnabled(boolean v) { eatenTimerSubtractPingEnabled = v;  save(); }
    public static void setPickoblockEnabled(boolean v)  { pickoblockEnabled = v;  save(); }
    public static void setHideElleDialogue(boolean v)   { hideElleDialogue = v;   save(); }
    public static void setAutoGfsEnabled(boolean v)     { autoGfsEnabled = v;     save(); }
    public static void toggleEtherwarpLavaBlock()       { etherwarpLavaBlock = !etherwarpLavaBlock; save(); }

    // Lava
    public static void setLavaMode(LavaMode m)         { lavaMode = m;             save(); rebuildChunks(); }
    public static void setLavaOpacity(float v)         { lavaOpacity = clamp01(v); save(); rebuildChunks(); }
    public static void setLavaColor(int c)             { lavaColor = c;            save(); rebuildChunks(); }
    public static void setLavaAsWater(boolean v)       { lavaAsWater = v;          save(); rebuildChunks(); }
    public static void setLavaColorOverride(boolean v) { lavaColorOverride = v;    save(); rebuildChunks(); }

    // Water
    public static void setWaterOpacity(float v)         { waterOpacity = clamp01(v); save(); rebuildChunks(); }
    public static void setWaterColor(int c)              { waterColor = c;            save(); rebuildChunks(); }
    public static void setWaterAsLava(boolean v)          { waterAsLava = v;           save(); rebuildChunks(); }
    public static void setWaterColorOverride(boolean v)   { waterColorOverride = v;    save(); rebuildChunks(); }

    // Misc
    public static void setFastDpsWarningEnabled(boolean v)  { fastDpsWarning = v;           save(); }
    public static void setChestTrackerVisible(boolean v)    { chestTrackerVisible = v;      save(); }
    public static void setSoloDetectorEnabled(boolean v)    { soloDetectorEnabled = v;      save(); }
    public static void setCannonAutoCloseEnabled(boolean v) { cannonAutoClose = v;          save(); }
    public static void setKuudraDirectionEnabled(boolean v) { kuudraDirectionEnabled = v;   save(); }
    public static void setShopKeybindsEnabled(boolean v)    { shopKeybindsEnabled = v;      save(); }
    public static void setShopMainKey(int v)                { shopMainKey = v;              save(); }
    public static void setShopCannonKey(int v)              { shopCannonKey = v;            save(); }
    public static void setExplosionFilterEnabled(boolean v) { explosionFilterEnabled = v;   save(); }
    public static void setExplosionHideRadius(float v)      { explosionHideRadius = clamp01(v); save(); }
    public static void setExplosionSizeMultiplier(float v)  { explosionSizeMultiplier = clamp01(v); save(); }
    public static void setChestAnnouncerEnabled(boolean v)  { chestAnnouncerEnabled = v;   save(); }

    public static void setAutoKickEnabled(boolean v)   { if (API_KEY_FEATURES_UNLOCKED) { autoKickEnabled = v; save(); } }
    public static void setProfileViewerEnabled(boolean v) { if (API_KEY_FEATURES_UNLOCKED) { profileViewerEnabled = v; save(); } }
    public static void setAkMinCatacombs(int v)        { akMinCatacombs = v;    save(); }
    public static void setAkMinForaging(int v)         { akMinForaging = v;     save(); }
    public static void setAkMinMagicalPower(int v)     { akMinMagicalPower = v; save(); }
    public static void setAkMinInfernal(int v)         { akMinInfernal = v;     save(); }
    public static void setAkMinFiery(int v)            { akMinFiery = v;        save(); }
    public static void setAkMinBurning(int v)          { akMinBurning = v;      save(); }
    public static void setAkMinHot(int v)              { akMinHot = v;          save(); }
    public static void setAkMinBasic(int v)            { akMinBasic = v;        save(); }
    public static void setAkRequireRend(boolean v)     { akRequireRend = v;     save(); }
    public static void setAkMinGdragLevel(int v)       { akMinGdragLevel = v;   save(); }
    public static void setPartyCmdsEnabled(boolean v)       { partyCmdsEnabled = v;        save(); }
    public static void setAutoRequeueEnabled(boolean v)     { autoRequeueEnabled = v;      save(); }
    public static void setAutoRequeueMessageEnabled(boolean v) { autoRequeueMessageEnabled = v; save(); }
    public static void setAutoUpdatesEnabled(boolean v)     { autoUpdatesEnabled = v;      save(); }
    public static void setDeveloperFeaturesEnabled(boolean v) { developerFeaturesEnabled = v; save(); }
    public static void setAutoSprintEnabled(boolean v)      { autoSprintEnabled = v;       save(); }
    public static void setSlotBindsEnabled(boolean v)       { slotBindsEnabled = v;        save(); }
    public static void clearSlotBinding(int invSlot)        { slotBindings.remove(invSlot); save(); }
    public static void putSlotBinding(int invSlot, int hotbarIndex) { slotBindings.put(invSlot, hotbarIndex); save(); }

    // Pearl
    public static void setPearlWaypointsEnabled(boolean v)  { pearlWaypointsEnabled = v;    save(); }
    public static void setShowAllWaypoints(boolean v)        { showAllWaypoints = v;         save(); }
    public static void setPearlFlatEnabled(boolean v)        { pearlFlatEnabled = v;         save(); }
    public static void setPearlSkyEnabled(boolean v)         { pearlSkyEnabled = v;          save(); }
    public static void setPearlDoubleEnabled(boolean v)      { pearlDoubleEnabled = v;       save(); }
    public static void setDoublePearlDelayS(float v)         { doublePearlDelayS = Math.max(0.05f, Math.min(0.55f, v)); save(); }
    public static void setWaypointType(WaypointType t)       { waypointType = t;             save(); }
    public static void setWaypointFillEnabled(boolean v)     { waypointFill = v;             save(); }
    public static void setPearlTickUpdate(boolean v)         { pearlTickUpdate = v;          save(); }
    public static void setDropLocationsEnabled(boolean v)    { dropLocationsEnabled = v;     save(); }
    public static void setWaypointLinesEnabled(boolean v)           { waypointLinesEnabled = v;           save(); }
    public static void setWaypointLinesSuppliesEnabled(boolean v)   { waypointLinesSuppliesEnabled = v;   save(); }
    public static void setWaypointLinesFlatPearlsEnabled(boolean v) { waypointLinesFlatPearlsEnabled = v; save(); }
    public static void setSecondSupplyPreference(SecondSupplyPreference p) { secondSupplyPreference = p; save(); }
    public static void setPearlTimerEnabled(boolean v)       { pearlTimerEnabled = v;        save(); }
    public static void setPearlTimerHeight(float v)          { pearlTimerHeight = v;         save(); }
    public static void setPearlTimerSize(float v)            { pearlTimerSize = v;           save(); }
    public static void setPearlCircleSize(float v)           { pearlCircleSize = v;          save(); }
    public static void setKuudraTalisman(KuudraTalisman t)   { kuudraTalisman = t;           save(); }
    public static void setLowPing(int v)                     { lowPing = Math.max(0, v);     save(); }
    public static void setWaypointFillAlpha(float v)         { waypointFillAlpha = v;        save(); }
    public static void setBeaconAlpha(float v)               { beaconAlpha = v;              save(); }
    public static void setWpColNormal(int v)                 { wpColNormal = v & 0xFFFFFF;      save(); }
    public static void setWpColCorrect(int v)                { wpColCorrect = v & 0xFFFFFF;     save(); }
    public static void setWpColHovered(int v)                { wpColHovered = v & 0xFFFFFF;     save(); }
    public static void setWpColReady(int v)                  { wpColReady = v & 0xFFFFFF;       save(); }
    public static void setBeaconColNormal(int v)             { beaconColNormal = v & 0xFFFFFF;  save(); }
    public static void setBeaconColCorrect(int v)            { beaconColCorrect = v & 0xFFFFFF; save(); }
    public static void setBuildBeaconAlpha(float v)          { buildBeaconAlpha = clamp01(v);   save(); }
    public static void setBlockSlot9Enabled(boolean v)       { blockSlot9Enabled = v;        save(); }
    public static void setStunPreviewEnabled(boolean v)          { stunPreviewEnabled = v;            save(); }
    public static void setBuildBeaconsEnabled(boolean v)         { buildBeaconsEnabled = v;           save(); }
    public static void setElleHighlightEnabled(boolean v)        { elleHighlightEnabled = v;          save(); }
    public static void setRendDamageEnabled(boolean v)           { rendDamageEnabled = v;             save(); }
    public static void setRendTrackerEnabled(boolean v)          { rendTrackerEnabled = v;            save(); }
    public static void setBackboneProgressBarEnabled(boolean v)   { backboneProgressBarEnabled = v;    save(); }
    public static void setBackboneProgressBarOutsideKuudraEnabled(boolean v) { backboneProgressBarOutsideKuudraEnabled = v; save(); }
    public static void setBackboneProgressBarHudX(float v)        { backboneProgressBarHudX     = Math.max(0, Math.min(1, v)); }
    public static void setBackboneProgressBarHudY(float v)        { backboneProgressBarHudY     = Math.max(0, Math.min(1, v)); }
    public static void setBackboneProgressBarHudScale(float v)    { backboneProgressBarHudScale = Math.max(0.2f, Math.min(3f, v)); }
    public static void setKuudraHighlightEnabled(boolean v)     { kuudraHighlightEnabled = v;        save(); }
    public static void setKuudraHighlightFilled(boolean v)      { kuudraHighlightFilled = v;         save(); }
    public static void setEtherwarpSlotBlockerEnabled(boolean v) { etherwarpSlotBlockerEnabled = v; save(); }
    public static void setSupplyWaypointsEnabled(boolean v)          { supplyWaypointsEnabled = v;              save(); }
    public static void setSupplyBeaconsEnabled(boolean v)            { supplyBeaconsEnabled = v;                save(); }
    public static void setNoPreAnnounceEnabled(boolean v)            { noPreAnnounceEnabled = v;                save(); }
    public static void setSupplyLocationAnnounceEnabled(boolean v)   { supplyLocationAnnounceEnabled = v;       save(); }
    public static void setSupplyProgressHudEnabled(boolean v)        { supplyProgressHudEnabled = v;            save(); }
    public static void setBuildProgressHudEnabled(boolean v)         { buildProgressHudEnabled = v;             save(); }
    public static void setAnnounceFreshEnabled(boolean v)            { announceFreshEnabled = v;                save(); }
    public static void setSupplyHitboxEnabled(boolean v)             { supplyHitboxEnabled = v;                 save(); }
    public static void setSupplyRodRadiusEnabled(boolean v)          { supplyRodRadiusEnabled = v;              save(); }
    public static void setSupplyPearlHitboxEnabled(boolean v)        { supplyPearlHitboxEnabled = v;            save(); }
    public static void setPearlRefillEnabled(boolean v)              { pearlRefillEnabled = v;                  save(); }
    public static void setPearlRefillOutsideKuudraEnabled(boolean v) { pearlRefillOutsideKuudraEnabled = v;     save(); }
    public static void setHideSelfieEnabled(boolean v)               { hideSelfieEnabled = v;                    save(); }
    public static void setPreventPlacingPlayerHeadsEnabled(boolean v)      { preventPlacingPlayerHeadsEnabled = v;      save(); }
    public static void setPreventPlacingPlayerHeadsExceptGarden(boolean v) { preventPlacingPlayerHeadsExceptGarden = v; save(); }
    public static void setPreventPlacingWeaponsEnabled(boolean v)          { preventPlacingWeaponsEnabled = v;          save(); }
    public static void setSupplyGiantHitboxEnabled(boolean v)              { supplyGiantHitboxEnabled = v;              save(); }
    public static void setGiantHitboxEnabled(boolean v)                    { giantHitboxEnabled = v;                    save(); }
    public static void setGiantHitboxFilled(boolean v)                     { giantHitboxFilled = v;                     save(); }
    public static void setGiantHitboxFillOpacity(float v)                  { giantHitboxFillOpacity = clamp01(v);       save(); }
    public static void setGiantHitboxColor(int v)                          { giantHitboxColor = v & 0xFFFFFF;           save(); }

    // HUD layout (no auto-save — HudEditorScreen calls save() on close)
    public static float getMountTimerHudX()     { return mountTimerHudX; }
    public static float getMountTimerHudY()     { return mountTimerHudY; }
    public static float getMountTimerHudScale() { return mountTimerHudScale; }
    public static float getDirectionHudX()     { return directionHudX; }
    public static float getDirectionHudY()     { return directionHudY; }
    public static float getDirectionHudScale() { return directionHudScale; }
    public static float getSplitHudX()         { return splitHudX; }
    public static float getSplitHudY()         { return splitHudY; }
    public static float getSplitHudScale()     { return splitHudScale; }

    public static void setMountTimerHudX(float v)     { mountTimerHudX     = Math.max(0, Math.min(1, v)); }
    public static void setMountTimerHudY(float v)     { mountTimerHudY     = Math.max(0, Math.min(1, v)); }
    public static void setMountTimerHudScale(float v) { mountTimerHudScale = Math.max(0.2f, Math.min(3f, v)); }
    public static void setDirectionHudX(float v)     { directionHudX     = Math.max(0, Math.min(1, v)); }
    public static void setDirectionHudY(float v)     { directionHudY     = Math.max(0, Math.min(1, v)); }
    public static void setDirectionHudScale(float v) { directionHudScale = Math.max(0.2f, Math.min(3f, v)); }
    public static void setSplitHudX(float v)         { splitHudX         = Math.max(0, Math.min(1, v)); }
    public static void setSplitHudY(float v)         { splitHudY         = Math.max(0, Math.min(1, v)); }
    public static void setSplitHudScale(float v)     { splitHudScale     = Math.max(0.2f, Math.min(3f, v)); }
    public static float getPearlTitleHudX()          { return pearlTitleHudX; }
    public static float getPearlTitleHudY()          { return pearlTitleHudY; }
    public static float getPearlTitleHudScale()      { return pearlTitleHudScale; }
    public static void setPearlTitleHudX(float v)    { pearlTitleHudX    = Math.max(0, Math.min(1, v)); }
    public static void setPearlTitleHudY(float v)    { pearlTitleHudY    = Math.max(0, Math.min(1, v)); }
    public static void setPearlTitleHudScale(float v){ pearlTitleHudScale = Math.max(0.2f, Math.min(3f, v)); }
    public static boolean isSmoothCratePickupEnabled()      { return smoothCratePickupEnabled; }
    public static void setSmoothCratePickupEnabled(boolean v) { smoothCratePickupEnabled = v; save(); }
    public static float getSupplyProgressHudX()           { return supplyProgressHudX; }
    public static float getSupplyProgressHudY()           { return supplyProgressHudY; }
    public static float getSupplyProgressHudScale()       { return supplyProgressHudScale; }
    public static void setSupplyProgressHudX(float v)     { supplyProgressHudX     = Math.max(0, Math.min(1, v)); }
    public static void setSupplyProgressHudY(float v)     { supplyProgressHudY     = Math.max(0, Math.min(1, v)); }
    public static void setSupplyProgressHudScale(float v) { supplyProgressHudScale = Math.max(0.2f, Math.min(3f, v)); }
    public static float getBuildProgressHudX()            { return buildProgressHudX; }
    public static float getBuildProgressHudY()            { return buildProgressHudY; }
    public static float getBuildProgressHudScale()        { return buildProgressHudScale; }
    public static void setBuildProgressHudX(float v)      { buildProgressHudX      = Math.max(0, Math.min(1, v)); }
    public static void setBuildProgressHudY(float v)      { buildProgressHudY      = Math.max(0, Math.min(1, v)); }
    public static void setBuildProgressHudScale(float v)  { buildProgressHudScale  = Math.max(0.2f, Math.min(3f, v)); }
    public static float getNotificationHudX()             { return notificationHudX; }
    public static float getNotificationHudY()             { return notificationHudY; }
    public static float getNotificationHudScale()         { return notificationHudScale; }
    public static void setNotificationHudX(float v)       { notificationHudX      = Math.max(0, Math.min(1, v)); }
    public static void setNotificationHudY(float v)       { notificationHudY      = Math.max(0, Math.min(1, v)); }
    public static void setNotificationHudScale(float v)   { notificationHudScale  = Math.max(0.2f, Math.min(3f, v)); }
    public static float getCratePriorityHudX()            { return cratePriorityHudX; }
    public static float getCratePriorityHudY()            { return cratePriorityHudY; }
    public static float getCratePriorityHudScale()        { return cratePriorityHudScale; }
    public static void setCratePriorityHudX(float v)      { cratePriorityHudX     = Math.max(0, Math.min(1, v)); }
    public static void setCratePriorityHudY(float v)      { cratePriorityHudY     = Math.max(0, Math.min(1, v)); }
    public static void setCratePriorityHudScale(float v)  { cratePriorityHudScale = Math.max(0.2f, Math.min(3f, v)); }
    public static boolean isSupplyRecoveryMsgEnabled()      { return supplyRecoveryMsgEnabled; }
    public static void setSupplyRecoveryMsgEnabled(boolean v){ supplyRecoveryMsgEnabled = v;  save(); }
    public static boolean isFreshNotifyEnabled()            { return freshNotifyEnabled; }
    public static void setFreshNotifyEnabled(boolean v)     { freshNotifyEnabled = v;         save(); }
    public static boolean isBuildStartedNotifyEnabled()     { return buildStartedNotifyEnabled; }
    public static void setBuildStartedNotifyEnabled(boolean v){ buildStartedNotifyEnabled = v; save(); }
    public static boolean isFastDpsNotifyEnabled()          { return fastDpsNotifyEnabled; }
    public static void setFastDpsNotifyEnabled(boolean v)   { fastDpsNotifyEnabled = v;       save(); }
    public static boolean isSoloNotifyEnabled()             { return soloNotifyEnabled; }
    public static void setSoloNotifyEnabled(boolean v)      { soloNotifyEnabled = v;           save(); }
    public static boolean isNoPreNotifyEnabled()            { return noPreNotifyEnabled; }
    public static void setNoPreNotifyEnabled(boolean v)     { noPreNotifyEnabled = v;          save(); }
    public static boolean isSupplyGrabbedNotifyEnabled()    { return supplyGrabbedNotifyEnabled; }
    public static void setSupplyGrabbedNotifyEnabled(boolean v) { supplyGrabbedNotifyEnabled = v; save(); }
    public static boolean isSupplyDroppedNotifyEnabled()    { return supplyDroppedNotifyEnabled; }
    public static void setSupplyDroppedNotifyEnabled(boolean v) { supplyDroppedNotifyEnabled = v; save(); }
    public static boolean isCratePriorityEnabled()          { return cratePriorityEnabled; }
    public static void setCratePriorityEnabled(boolean v)   { cratePriorityEnabled = v;        save(); }
    public static boolean isHideArmorStandsEnabled()              { return hideArmorStandsEnabled; }
    public static void setHideArmorStandsEnabled(boolean v)       { hideArmorStandsEnabled = v; save(); }
    public static boolean isHideArmorStandsBuild()                { return hideArmorStandsBuild; }
    public static void setHideArmorStandsBuild(boolean v)         { hideArmorStandsBuild = v; save(); }
    public static boolean isHideArmorStandsRightCannon()          { return hideArmorStandsRightCannon; }
    public static void setHideArmorStandsRightCannon(boolean v)   { hideArmorStandsRightCannon = v; save(); }
    public static boolean isHideArmorStandsLeftCannon()           { return hideArmorStandsLeftCannon; }
    public static void setHideArmorStandsLeftCannon(boolean v)    { hideArmorStandsLeftCannon = v; save(); }
    public static boolean isHideArmorStandsShop()                 { return hideArmorStandsShop; }
    public static void setHideArmorStandsShop(boolean v)          { hideArmorStandsShop = v; save(); }
    public static boolean isHideArmorStandsOthers()               { return hideArmorStandsOthers; }
    public static void setHideArmorStandsOthers(boolean v)        { hideArmorStandsOthers = v; save(); }
    public static boolean isKuudraHpHudEnabled()            { return kuudraHpHudEnabled; }
    public static void setKuudraHpHudEnabled(boolean v)     { kuudraHpHudEnabled = v; save(); }
    public static boolean isKuudraHpShowRaw()               { return kuudraHpShowRaw; }
    public static void setKuudraHpShowRaw(boolean v)        { kuudraHpShowRaw = v; save(); }
    public static boolean isKuudraHpHideBar()               { return kuudraHpHideBar; }
    public static void setKuudraHpHideBar(boolean v)        { kuudraHpHideBar = v; save(); }
    public static boolean isHollowWandEnabled()             { return hollowWandEnabled; }
    public static void setHollowWandEnabled(boolean v)      { hollowWandEnabled = v; save(); }
    public static boolean isKickedNotificationEnabled()     { return kickedNotificationEnabled; }
    public static void setKickedNotificationEnabled(boolean v) { kickedNotificationEnabled = v; save(); }
    public static boolean isHideBossBarEnabled()              { return hideBossBarEnabled; }
    public static boolean isHideFallingBlocksEnabled()        { return hideFallingBlocksEnabled; }
    public static void setHideBossBarEnabled(boolean v)       { hideBossBarEnabled = v; save(); }
    public static void setHideFallingBlocksEnabled(boolean v) { hideFallingBlocksEnabled = v; save(); }
    public static boolean isManaDrainAnnouncerEnabled()       { return manaDrainAnnouncerEnabled; }
    public static void setManaDrainAnnouncerEnabled(boolean v){ manaDrainAnnouncerEnabled = v; save(); }
    public static boolean isHideEntityFireEnabled()           { return hideEntityFireEnabled; }
    public static void setHideEntityFireEnabled(boolean v)    { hideEntityFireEnabled = v; save(); }
    public static boolean isLavaBobberFixEnabled()             { return lavaBobberFixEnabled; }
    public static void setLavaBobberFixEnabled(boolean v)      { lavaBobberFixEnabled = v; save(); }
    public static boolean isLegacyRodPhysicsEnabled()          { return legacyRodPhysicsEnabled; }
    public static void setLegacyRodPhysicsEnabled(boolean v)   { legacyRodPhysicsEnabled = v; save(); }
    public static boolean isHideDamageTitleEnabled()           { return hideDamageTitleEnabled; }
    public static void setHideDamageTitleEnabled(boolean v)    { hideDamageTitleEnabled = v; save(); }
    public static boolean isHideDeadEntitiesEnabled()          { return hideDeadEntitiesEnabled; }
    public static void setHideDeadEntitiesEnabled(boolean v)   { hideDeadEntitiesEnabled = v; save(); }
    public static boolean isEtherwarpWaypointsEnabled()        { return etherwarpWaypointsEnabled; }
    public static void setEtherwarpWaypointsEnabled(boolean v) { etherwarpWaypointsEnabled = v; save(); }
    public static float getSelfPlayerScale()                  { return selfPlayerScale; }
    public static void setSelfPlayerScale(float v)            { selfPlayerScale = Math.max(1f, Math.min(300f, v)); save(); }
    public static float getOtherPlayerScale()                 { return otherPlayerScale; }
    public static void setOtherPlayerScale(float v)           { otherPlayerScale = Math.max(1f, Math.min(300f, v)); save(); }
    public static float getKuudraSizeScale()                  { return kuudraSizeScale; }
    public static void setKuudraSizeScale(float v)            { kuudraSizeScale = Math.max(1f, Math.min(200f, v)); save(); }
    public static float getKuudraHpHudX()                   { return kuudraHpHudX; }
    public static float getKuudraHpHudY()                   { return kuudraHpHudY; }
    public static float getKuudraHpHudScale()               { return kuudraHpHudScale; }
    public static void setKuudraHpHudX(float v)             { kuudraHpHudX = Math.max(0, Math.min(1, v)); }
    public static void setKuudraHpHudY(float v)             { kuudraHpHudY = Math.max(0, Math.min(1, v)); }
    public static void setKuudraHpHudScale(float v)         { kuudraHpHudScale = Math.max(0.2f, Math.min(3f, v)); }

    // Wardrobe keybinds
    public static boolean isWardrobeEnabled()         { return wardrobeEnabled; }
    public static void setWardrobeEnabled(boolean v)  { wardrobeEnabled = v; save(); }
    public static int  getWardrobeOpenKey()           { return wardrobeOpenKey; }
    public static void setWardrobeOpenKey(int v)      { wardrobeOpenKey = v; save(); }
    public static int  getStatsOpenKey()              { return statsOpenKey; }
    public static void setStatsOpenKey(int v)         { statsOpenKey = v; save(); }
    public static int  getPetsOpenKey()               { return petsOpenKey; }
    public static void setPetsOpenKey(int v)          { petsOpenKey = v; save(); }
    public static int  getEqWardrobeOpenKey()         { return eqWardrobeOpenKey; }
    public static void setEqWardrobeOpenKey(int v)    { eqWardrobeOpenKey = v; save(); }
    public static int  getLoadoutsOpenKey()           { return loadoutsOpenKey; }
    public static void setLoadoutsOpenKey(int v)      { loadoutsOpenKey = v; save(); }
    public static int[] getWardrobeSlotKeys()         { return wardrobeSlotKeys.clone(); }
    public static void setWardrobeSlotKey(int i, int v){ wardrobeSlotKeys[i] = v; save(); }
    public static int[] getLoadoutSlotKeys()          { return loadoutSlotKeys.clone(); }
    public static void setLoadoutSlotKey(int i, int v) { loadoutSlotKeys[i] = v; save(); }
    public static int  getWardrobeNextPageKey()       { return wardrobeNextPageKey; }
    public static void setWardrobeNextPageKey(int v)  { wardrobeNextPageKey = v; save(); }
    public static int  getWardrobePrevPageKey()       { return wardrobePrevPageKey; }
    public static void setWardrobePrevPageKey(int v)  { wardrobePrevPageKey = v; save(); }
    public static int  getWardrobeUnequipKey()        { return wardrobeUnequipKey; }
    public static void setWardrobeUnequipKey(int v)   { wardrobeUnequipKey = v; save(); }
    public static boolean isWardrobeDisableUnequipEnabled()      { return wardrobeDisableUnequipEnabled; }
    public static void setWardrobeDisableUnequipEnabled(boolean v) { wardrobeDisableUnequipEnabled = v; save(); }
    public static boolean isWardrobeAutoCloseEnabled()           { return wardrobeAutoCloseEnabled; }
    public static void setWardrobeAutoCloseEnabled(boolean v)    { wardrobeAutoCloseEnabled = v; save(); }
    public static int  getLastEquippedWardrobeSlot()          { return lastEquippedWardrobeSlot; }
    public static void setLastEquippedWardrobeSlot(int v)     { lastEquippedWardrobeSlot = v; save(); }

    // Dungeons
    public static void setAutoGfsToxic(boolean v)    { autoGfsToxic = v;    save(); }
    public static void setAutoGfsTwilight(boolean v) { autoGfsTwilight = v; save(); }
    public static void setToxicAmount(int v)         { toxicAmount = v;     save(); }
    public static void setTwilightAmount(int v)      { twilightAmount = v;  save(); }

    // Chest tracker
    public static void setChestCounts(int t, int s, int f) { chestTotal = t; chestSuccess = s; chestFail = f; save(); }
    public static void resetChestCounts()                   { chestTotal = 0; chestSuccess = 0; chestFail = 0; save(); }

    // Split timer
    public static void setSplitTimerEnabled(boolean v)  { splitTimerEnabled = v;  save(); }
    public static void setSupplyTimesEnabled(boolean v) { supplyTimesEnabled = v; save(); }

    // Item customisation
    public static void setItemCustomizationEnabled(boolean v) { itemCustomizationEnabled = v; save(); }

    // ── Load / Save ───────────────────────────────────────────────────────────

    public static void load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) return;
        try (Reader r = new FileReader(file)) {
            Data d = GSON.fromJson(r, Data.class);
            if (d == null) return;

            roleMode           = safeEnum(RoleMode.class, d.roleMode, RoleMode.AUTO);
            dpsValue           = d.dpsValue;
            dpsRefillAmount    = Math.max(4, d.dpsRefillAmount);
            autoGfsDisableHpPercent = d.autoGfsDisableHpPercent >= 25 ? Math.min(100, d.autoGfsDisableHpPercent) : 40;
            stunValue          = d.stunValue;
            eatenTimerEnabled             = d.eatenTimerEnabled;
            eatenTimerSubtractPingEnabled = d.eatenTimerSubtractPingEnabled;
            pickoblockEnabled             = d.pickoblockEnabled;
            etherwarpLavaBlock = d.etherwarpLavaBlock;
            hideElleDialogue   = d.hideElleDialogue;
            autoGfsEnabled     = d.autoGfsEnabled;

            lavaMode          = safeEnum(LavaMode.class, d.lavaMode, LavaMode.DEFAULT);
            lavaOpacity       = clamp01(d.lavaOpacity);
            lavaColor         = d.lavaColor;
            lavaAsWater       = d.lavaAsWater;
            lavaColorOverride = d.lavaColorOverride;

            waterOpacity       = clamp01(d.waterOpacity);
            waterColor         = d.waterColor;
            waterAsLava        = d.waterAsLava;
            waterColorOverride = d.waterColorOverride;

            fastDpsWarning          = d.fastDpsWarning;
            chestTrackerVisible     = d.chestTrackerVisible;
            soloDetectorEnabled     = d.soloDetectorEnabled;
            cannonAutoClose         = d.cannonAutoClose;
            shopKeybindsEnabled     = d.shopKeybindsEnabled;
            shopMainKey             = d.shopMainKey;
            shopCannonKey           = d.shopCannonKey;
            explosionFilterEnabled  = d.explosionFilterEnabled;
            explosionHideRadius     = clamp01(d.explosionHideRadius);
            explosionSizeMultiplier = clamp01(d.explosionSizeMultiplier);
            chestAnnouncerEnabled   = d.chestAnnouncerEnabled;
            autoKickEnabled         = d.autoKickEnabled;
            profileViewerEnabled    = d.profileViewerEnabled;
            akMinCatacombs          = d.akMinCatacombs;
            akMinForaging           = d.akMinForaging;
            akMinMagicalPower       = d.akMinMagicalPower;
            akMinInfernal           = d.akMinInfernal;
            akMinFiery              = d.akMinFiery;
            akMinBurning            = d.akMinBurning;
            akMinHot                = d.akMinHot;
            akMinBasic              = d.akMinBasic;
            akRequireRend           = d.akRequireRend;
            akMinGdragLevel         = d.akMinGdragLevel;
            partyCmdsEnabled        = d.partyCmdsEnabled;
            autoRequeueEnabled      = d.autoRequeueEnabled;
            autoRequeueMessageEnabled = d.autoRequeueMessageEnabled;
            autoUpdatesEnabled      = d.autoUpdatesEnabled;
            developerFeaturesEnabled = d.developerFeaturesEnabled;
            autoSprintEnabled       = d.autoSprintEnabled;
            slotBindsEnabled        = d.slotBindsEnabled;
            slotBindSetKey          = d.slotBindSetKey;
            slotBindShowKey         = d.slotBindShowKey;
            slotBindings.clear();
            if (d.slotBindPairs != null) {
                for (int[] pair : d.slotBindPairs)
                    if (pair != null && pair.length == 2) slotBindings.put(pair[0], pair[1]);
            }

            pearlWaypointsEnabled = d.pearlWaypointsEnabled;
            showAllWaypoints      = d.showAllWaypoints;
            pearlFlatEnabled      = d.pearlFlatEnabled;
            pearlSkyEnabled       = d.pearlSkyEnabled;
            pearlDoubleEnabled    = d.pearlDoubleEnabled;
            doublePearlDelayS     = Math.max(0.05f, Math.min(0.55f, d.doublePearlDelayS));
            waypointType          = safeEnum(WaypointType.class, d.waypointType, WaypointType.CIRCLE);
            waypointFill          = d.waypointFill;
            pearlTickUpdate       = d.pearlTickUpdate;
            dropLocationsEnabled  = d.dropLocationsEnabled;
            waypointLinesEnabled           = d.waypointLinesEnabled;
            waypointLinesSuppliesEnabled   = d.waypointLinesSuppliesEnabled;
            waypointLinesFlatPearlsEnabled = d.waypointLinesFlatPearlsEnabled;
            secondSupplyPreference = safeEnum(SecondSupplyPreference.class, d.secondSupplyPreference, SecondSupplyPreference.DOUBLE_PEARL);
            pearlTimerEnabled     = d.pearlTimerEnabled;
            pearlTimerHeight      = clamp01(d.pearlTimerHeight);
            pearlTimerSize        = clamp01(d.pearlTimerSize);
            pearlCircleSize       = clamp01(d.pearlCircleSize);
            kuudraTalisman        = safeEnum(KuudraTalisman.class, d.kuudraTalisman, KuudraTalisman.NONE);
            lowPing               = Math.max(0, d.lowPing);
            waypointFillAlpha     = clamp01(d.waypointFillAlpha);
            beaconAlpha           = clamp01(d.beaconAlpha);
            wpColNormal           = d.wpColNormal      & 0xFFFFFF;
            wpColCorrect          = d.wpColCorrect     & 0xFFFFFF;
            wpColHovered          = d.wpColHovered     & 0xFFFFFF;
            wpColReady            = d.wpColReady       & 0xFFFFFF;
            beaconColNormal       = d.beaconColNormal  & 0xFFFFFF;
            beaconColCorrect      = d.beaconColCorrect & 0xFFFFFF;
            buildBeaconAlpha      = clamp01(d.buildBeaconAlpha);
            blockSlot9Enabled          = d.blockSlot9Enabled;
            stunPreviewEnabled         = d.stunPreviewEnabled;
            buildBeaconsEnabled        = d.buildBeaconsEnabled;
            elleHighlightEnabled       = d.elleHighlightEnabled;
            rendDamageEnabled          = d.rendDamageEnabled;
            rendTrackerEnabled         = d.rendTrackerEnabled;
            backboneProgressBarEnabled = d.backboneProgressBarEnabled;
            backboneProgressBarOutsideKuudraEnabled = d.backboneProgressBarOutsideKuudraEnabled;
            backboneProgressBarHudX     = d.backboneProgressBarHudX;
            backboneProgressBarHudY     = d.backboneProgressBarHudY;
            backboneProgressBarHudScale = d.backboneProgressBarHudScale;
            kuudraHighlightEnabled     = d.kuudraHighlightEnabled;
            kuudraHighlightFilled      = d.kuudraHighlightFilled;
            etherwarpSlotBlockerEnabled = d.etherwarpSlotBlockerEnabled;
            supplyWaypointsEnabled           = d.supplyWaypointsEnabled;
            // Migrate legacy flag — if old flag was on, enable the two rendering/announce features
            supplyBeaconsEnabled          = d.supplyBeaconsEnabled || d.supplyWaypointsEnabled;
            noPreAnnounceEnabled          = d.noPreAnnounceEnabled;
            supplyLocationAnnounceEnabled = d.supplyLocationAnnounceEnabled || d.supplyWaypointsEnabled;
            supplyProgressHudEnabled      = d.supplyProgressHudEnabled;
            buildProgressHudEnabled       = d.buildProgressHudEnabled;
            announceFreshEnabled          = d.announceFreshEnabled;
            supplyHitboxEnabled           = d.supplyHitboxEnabled;
            supplyRodRadiusEnabled        = d.supplyRodRadiusEnabled;
            supplyPearlHitboxEnabled      = d.supplyPearlHitboxEnabled;
            pearlRefillEnabled            = d.pearlRefillEnabled;
            pearlRefillOutsideKuudraEnabled = d.pearlRefillOutsideKuudraEnabled;
            hideSelfieEnabled             = d.hideSelfieEnabled;
            preventPlacingPlayerHeadsEnabled      = d.preventPlacingPlayerHeadsEnabled;
            preventPlacingPlayerHeadsExceptGarden = d.preventPlacingPlayerHeadsExceptGarden;
            preventPlacingWeaponsEnabled          = d.preventPlacingWeaponsEnabled;
            supplyGiantHitboxEnabled              = d.supplyGiantHitboxEnabled;
            giantHitboxEnabled                    = d.giantHitboxEnabled;
            giantHitboxFilled                     = d.giantHitboxFilled;
            giantHitboxFillOpacity                = clamp01(d.giantHitboxFillOpacity);
            giantHitboxColor                      = d.giantHitboxColor & 0xFFFFFF;
            kuudraDirectionEnabled = d.kuudraDirectionEnabled;

            mountTimerHudX     = d.mountTimerHudX;
            mountTimerHudY     = d.mountTimerHudY;
            mountTimerHudScale = d.mountTimerHudScale;
            directionHudX     = d.directionHudX;
            directionHudY     = d.directionHudY;
            directionHudScale = d.directionHudScale;
            splitHudX         = d.splitHudX;
            splitHudY         = d.splitHudY;
            splitHudScale     = d.splitHudScale;
            pearlTitleHudX    = d.pearlTitleHudX;
            pearlTitleHudY    = d.pearlTitleHudY;
            pearlTitleHudScale = d.pearlTitleHudScale;
            smoothCratePickupEnabled = d.smoothCratePickupEnabled;
            supplyProgressHudX     = d.supplyProgressHudX;
            supplyProgressHudY     = d.supplyProgressHudY;
            supplyProgressHudScale = d.supplyProgressHudScale;
            buildProgressHudX      = d.buildProgressHudX;
            buildProgressHudY      = d.buildProgressHudY;
            buildProgressHudScale  = d.buildProgressHudScale;
            notificationHudX       = d.notificationHudX;
            notificationHudY       = d.notificationHudY;
            notificationHudScale   = d.notificationHudScale;
            cratePriorityHudX      = d.cratePriorityHudX;
            cratePriorityHudY      = d.cratePriorityHudY;
            cratePriorityHudScale  = d.cratePriorityHudScale;
            supplyRecoveryMsgEnabled = d.supplyRecoveryMsgEnabled;
            freshNotifyEnabled       = d.freshNotifyEnabled;
            buildStartedNotifyEnabled= d.buildStartedNotifyEnabled;
            fastDpsNotifyEnabled     = d.fastDpsNotifyEnabled;
            soloNotifyEnabled        = d.soloNotifyEnabled;
            noPreNotifyEnabled       = d.noPreNotifyEnabled;
            supplyGrabbedNotifyEnabled = d.supplyGrabbedNotifyEnabled;
            supplyDroppedNotifyEnabled = d.supplyDroppedNotifyEnabled;
            cratePriorityEnabled     = d.cratePriorityEnabled;
            hideArmorStandsEnabled     = d.hideArmorStandsEnabled;
            hideArmorStandsBuild       = d.hideArmorStandsBuild;
            hideArmorStandsRightCannon = d.hideArmorStandsRightCannon;
            hideArmorStandsLeftCannon  = d.hideArmorStandsLeftCannon;
            hideArmorStandsShop        = d.hideArmorStandsShop;
            hideArmorStandsOthers      = d.hideArmorStandsOthers;
            kuudraHpHudEnabled       = d.kuudraHpHudEnabled;
            kuudraHpShowRaw          = d.kuudraHpShowRaw;
            kuudraHpHideBar          = d.kuudraHpHideBar;
            hollowWandEnabled            = d.hollowWandEnabled;
            kickedNotificationEnabled    = d.kickedNotificationEnabled;
            hideBossBarEnabled         = d.hideBossBarEnabled;
            hideFallingBlocksEnabled  = d.hideFallingBlocksEnabled;
            manaDrainAnnouncerEnabled  = d.manaDrainAnnouncerEnabled;
            hideEntityFireEnabled      = d.hideEntityFireEnabled;
            lavaBobberFixEnabled       = d.lavaBobberFixEnabled;
            legacyRodPhysicsEnabled    = d.legacyRodPhysicsEnabled;
            hideDamageTitleEnabled     = d.hideDamageTitleEnabled;
            hideDeadEntitiesEnabled    = d.hideDeadEntitiesEnabled;
            etherwarpWaypointsEnabled  = d.etherwarpWaypointsEnabled;
            selfPlayerScale            = d.selfPlayerScale;
            otherPlayerScale           = d.otherPlayerScale;
            kuudraSizeScale            = d.kuudraSizeScale;
            kuudraHpHudX             = d.kuudraHpHudX;
            kuudraHpHudY             = d.kuudraHpHudY;
            kuudraHpHudScale         = d.kuudraHpHudScale;

            wardrobeEnabled     = d.wardrobeEnabled;
            wardrobeOpenKey     = d.wardrobeOpenKey;
            statsOpenKey        = d.statsOpenKey;
            petsOpenKey         = d.petsOpenKey;
            eqWardrobeOpenKey   = d.eqWardrobeOpenKey;
            loadoutsOpenKey     = d.loadoutsOpenKey;
            if (d.wardrobeSlotKeys != null && d.wardrobeSlotKeys.length == 9)
                wardrobeSlotKeys = d.wardrobeSlotKeys;
            if (d.loadoutSlotKeys != null && d.loadoutSlotKeys.length == 12)
                loadoutSlotKeys = d.loadoutSlotKeys;
            wardrobeNextPageKey = d.wardrobeNextPageKey;
            wardrobePrevPageKey = d.wardrobePrevPageKey;
            wardrobeUnequipKey  = d.wardrobeUnequipKey;
            wardrobeDisableUnequipEnabled = d.wardrobeDisableUnequipEnabled;
            wardrobeAutoCloseEnabled      = d.wardrobeAutoCloseEnabled;
            lastEquippedWardrobeSlot      = d.lastEquippedWardrobeSlot;

            autoGfsToxic    = d.autoGfsToxic;
            autoGfsTwilight = d.autoGfsTwilight;
            toxicAmount     = d.toxicAmount;
            twilightAmount  = d.twilightAmount;

            chestTotal   = d.chestTotal;
            chestSuccess = d.chestSuccess;
            chestFail    = d.chestFail;

            splitTimerEnabled  = d.splitTimerEnabled;
            supplyTimesEnabled = d.supplyTimesEnabled;
            if (d.splitPbs != null && d.splitPbs.length == 6)
                for (int i = 1; i <= 5; i++)
                    if (d.splitPbs[i] != null && d.splitPbs[i].length == 7)
                        splitPbs[i] = d.splitPbs[i].clone();
            if (d.totalRunPbs != null && d.totalRunPbs.length == 6)
                totalRunPbs = d.totalRunPbs.clone();
            if (d.pbRecords != null && d.pbRecords.length == 6) {
                pbRecords = d.pbRecords;
                for (int t = 1; t <= 5; t++)
                    if (pbRecords[t] != null) totalRunPbs[t] = pbRecords[t].totalTime;
            } else {
                for (int t = 1; t <= 5; t++)
                    if (totalRunPbs[t] < 9999) {
                        pbRecords[t] = new PbRecord();
                        pbRecords[t].totalTime = totalRunPbs[t];
                        if (splitPbs[t] != null) pbRecords[t].splits = splitPbs[t].clone();
                    }
            }

            profitTrackerEnabled  = d.profitTrackerEnabled;
            profitShowDuringRun   = d.profitShowDuringRun;
            profitArmorSalvage    = d.profitArmorSalvage;
            profitFactionMage     = d.profitFactionMage;
            profitHighlightChests = d.profitHighlightChests;
            profitRerollCalc      = d.profitRerollCalc;
            profitBazaarInstaSell = d.profitBazaarInstaSell;
            profitBazaarInstaBuy  = d.profitBazaarInstaBuy;
            profitHudX            = d.profitHudX;
            profitHudY            = d.profitHudY;
            profitHudScale        = d.profitHudScale;
            if (d.kuudraPetRarity != null) {
                try { kuudraPetRarity = KuudraPetRarity.valueOf(d.kuudraPetRarity); }
                catch (IllegalArgumentException ignored) {}
            }
            if (d.kuudraPetLevel > 0) kuudraPetLevel = Math.clamp(d.kuudraPetLevel, 1, 100);
            chestValueGuiEnabled  = d.chestValueGuiEnabled;
            chestValueHudX        = d.chestValueHudX;
            chestValueHudY        = d.chestValueHudY;
            chestValueHudScale    = d.chestValueHudScale;

            itemCustomizationEnabled = d.itemCustomizationEnabled;
            ItemCustomization.loadFrom(d.itemCategorySettings, d.itemCustomCategories);

            if (d.notificationSounds != null) {
                notificationSounds.clear();
                notificationSounds.putAll(d.notificationSounds);
            }

        } catch (IOException e) { KuudraHelperMod.LOGGER.error("Failed to load config", e); }
    }

    public static void save() {
        Data d = new Data();

        d.roleMode           = roleMode.name();
        d.dpsValue           = dpsValue;
        d.dpsRefillAmount    = dpsRefillAmount;
        d.autoGfsDisableHpPercent = autoGfsDisableHpPercent;
        d.stunValue          = stunValue;
        d.eatenTimerEnabled             = eatenTimerEnabled;
        d.eatenTimerSubtractPingEnabled = eatenTimerSubtractPingEnabled;
        d.pickoblockEnabled             = pickoblockEnabled;
        d.etherwarpLavaBlock = etherwarpLavaBlock;
        d.hideElleDialogue   = hideElleDialogue;
        d.autoGfsEnabled     = autoGfsEnabled;

        d.lavaMode          = lavaMode.name();
        d.lavaOpacity       = lavaOpacity;
        d.lavaColor         = lavaColor;
        d.lavaAsWater       = lavaAsWater;
        d.lavaColorOverride = lavaColorOverride;

        d.waterOpacity       = waterOpacity;
        d.waterColor         = waterColor;
        d.waterAsLava        = waterAsLava;
        d.waterColorOverride = waterColorOverride;

        d.fastDpsWarning          = fastDpsWarning;
        d.chestTrackerVisible     = chestTrackerVisible;
        d.soloDetectorEnabled     = soloDetectorEnabled;
        d.cannonAutoClose         = cannonAutoClose;
        d.shopKeybindsEnabled     = shopKeybindsEnabled;
        d.shopMainKey             = shopMainKey;
        d.shopCannonKey           = shopCannonKey;
        d.explosionFilterEnabled  = explosionFilterEnabled;
        d.explosionHideRadius     = explosionHideRadius;
        d.explosionSizeMultiplier = explosionSizeMultiplier;
        d.chestAnnouncerEnabled   = chestAnnouncerEnabled;
        d.autoKickEnabled         = autoKickEnabled;
        d.profileViewerEnabled    = profileViewerEnabled;
        d.akMinCatacombs          = akMinCatacombs;
        d.akMinForaging           = akMinForaging;
        d.akMinMagicalPower       = akMinMagicalPower;
        d.akMinInfernal           = akMinInfernal;
        d.akMinFiery              = akMinFiery;
        d.akMinBurning            = akMinBurning;
        d.akMinHot                = akMinHot;
        d.akMinBasic              = akMinBasic;
        d.akRequireRend           = akRequireRend;
        d.akMinGdragLevel         = akMinGdragLevel;
        d.partyCmdsEnabled        = partyCmdsEnabled;
        d.autoRequeueEnabled      = autoRequeueEnabled;
        d.autoRequeueMessageEnabled = autoRequeueMessageEnabled;
        d.autoUpdatesEnabled      = autoUpdatesEnabled;
        d.developerFeaturesEnabled = developerFeaturesEnabled;
        d.autoSprintEnabled       = autoSprintEnabled;
        d.slotBindsEnabled        = slotBindsEnabled;
        d.slotBindSetKey          = slotBindSetKey;
        d.slotBindShowKey         = slotBindShowKey;
        d.slotBindPairs           = slotBindings.entrySet().stream()
                .map(e -> new int[]{e.getKey(), e.getValue()})
                .toArray(int[][]::new);

        d.pearlWaypointsEnabled = pearlWaypointsEnabled;
        d.showAllWaypoints      = showAllWaypoints;
        d.pearlFlatEnabled      = pearlFlatEnabled;
        d.pearlSkyEnabled       = pearlSkyEnabled;
        d.pearlDoubleEnabled    = pearlDoubleEnabled;
        d.doublePearlDelayS     = doublePearlDelayS;
        d.waypointType          = waypointType.name();
        d.waypointFill          = waypointFill;
        d.pearlTickUpdate       = pearlTickUpdate;
        d.dropLocationsEnabled  = dropLocationsEnabled;
        d.waypointLinesEnabled           = waypointLinesEnabled;
        d.waypointLinesSuppliesEnabled   = waypointLinesSuppliesEnabled;
        d.waypointLinesFlatPearlsEnabled = waypointLinesFlatPearlsEnabled;
        d.secondSupplyPreference = secondSupplyPreference.name();
        d.pearlTimerEnabled     = pearlTimerEnabled;
        d.pearlTimerHeight      = pearlTimerHeight;
        d.pearlTimerSize        = pearlTimerSize;
        d.pearlCircleSize       = pearlCircleSize;
        d.kuudraTalisman        = kuudraTalisman.name();
        d.lowPing               = lowPing;
        d.waypointFillAlpha     = waypointFillAlpha;
        d.beaconAlpha           = beaconAlpha;
        d.wpColNormal           = wpColNormal;
        d.wpColCorrect          = wpColCorrect;
        d.wpColHovered          = wpColHovered;
        d.wpColReady            = wpColReady;
        d.beaconColNormal       = beaconColNormal;
        d.beaconColCorrect      = beaconColCorrect;
        d.buildBeaconAlpha      = buildBeaconAlpha;
        d.blockSlot9Enabled          = blockSlot9Enabled;
        d.stunPreviewEnabled         = stunPreviewEnabled;
        d.buildBeaconsEnabled        = buildBeaconsEnabled;
        d.elleHighlightEnabled       = elleHighlightEnabled;
        d.rendDamageEnabled          = rendDamageEnabled;
        d.rendTrackerEnabled         = rendTrackerEnabled;
        d.backboneProgressBarEnabled = backboneProgressBarEnabled;
        d.backboneProgressBarOutsideKuudraEnabled = backboneProgressBarOutsideKuudraEnabled;
        d.backboneProgressBarHudX     = backboneProgressBarHudX;
        d.backboneProgressBarHudY     = backboneProgressBarHudY;
        d.backboneProgressBarHudScale = backboneProgressBarHudScale;
        d.kuudraHighlightEnabled     = kuudraHighlightEnabled;
        d.kuudraHighlightFilled      = kuudraHighlightFilled;
        d.etherwarpSlotBlockerEnabled = etherwarpSlotBlockerEnabled;
        d.supplyWaypointsEnabled           = supplyWaypointsEnabled;
        d.supplyBeaconsEnabled          = supplyBeaconsEnabled;
        d.noPreAnnounceEnabled          = noPreAnnounceEnabled;
        d.supplyLocationAnnounceEnabled = supplyLocationAnnounceEnabled;
        d.supplyProgressHudEnabled      = supplyProgressHudEnabled;
        d.buildProgressHudEnabled       = buildProgressHudEnabled;
        d.announceFreshEnabled          = announceFreshEnabled;
        d.supplyHitboxEnabled           = supplyHitboxEnabled;
        d.supplyRodRadiusEnabled        = supplyRodRadiusEnabled;
        d.supplyPearlHitboxEnabled      = supplyPearlHitboxEnabled;
        d.pearlRefillEnabled            = pearlRefillEnabled;
        d.pearlRefillOutsideKuudraEnabled = pearlRefillOutsideKuudraEnabled;
        d.hideSelfieEnabled             = hideSelfieEnabled;
        d.preventPlacingPlayerHeadsEnabled      = preventPlacingPlayerHeadsEnabled;
        d.preventPlacingPlayerHeadsExceptGarden = preventPlacingPlayerHeadsExceptGarden;
        d.preventPlacingWeaponsEnabled          = preventPlacingWeaponsEnabled;
        d.supplyGiantHitboxEnabled              = supplyGiantHitboxEnabled;
        d.giantHitboxEnabled                    = giantHitboxEnabled;
        d.giantHitboxFilled                     = giantHitboxFilled;
        d.giantHitboxFillOpacity                = giantHitboxFillOpacity;
        d.giantHitboxColor                      = giantHitboxColor;
        d.kuudraDirectionEnabled  = kuudraDirectionEnabled;

        d.mountTimerHudX     = mountTimerHudX;
        d.mountTimerHudY     = mountTimerHudY;
        d.mountTimerHudScale = mountTimerHudScale;
        d.directionHudX     = directionHudX;
        d.directionHudY     = directionHudY;
        d.directionHudScale = directionHudScale;
        d.splitHudX         = splitHudX;
        d.splitHudY         = splitHudY;
        d.splitHudScale     = splitHudScale;
        d.pearlTitleHudX    = pearlTitleHudX;
        d.pearlTitleHudY    = pearlTitleHudY;
        d.pearlTitleHudScale = pearlTitleHudScale;
        d.smoothCratePickupEnabled = smoothCratePickupEnabled;
        d.supplyProgressHudX     = supplyProgressHudX;
        d.supplyProgressHudY     = supplyProgressHudY;
        d.supplyProgressHudScale = supplyProgressHudScale;
        d.buildProgressHudX      = buildProgressHudX;
        d.buildProgressHudY      = buildProgressHudY;
        d.buildProgressHudScale  = buildProgressHudScale;
        d.notificationHudX       = notificationHudX;
        d.notificationHudY       = notificationHudY;
        d.notificationHudScale   = notificationHudScale;
        d.cratePriorityHudX      = cratePriorityHudX;
        d.cratePriorityHudY      = cratePriorityHudY;
        d.cratePriorityHudScale  = cratePriorityHudScale;
        d.supplyRecoveryMsgEnabled = supplyRecoveryMsgEnabled;
        d.freshNotifyEnabled       = freshNotifyEnabled;
        d.buildStartedNotifyEnabled= buildStartedNotifyEnabled;
        d.fastDpsNotifyEnabled     = fastDpsNotifyEnabled;
        d.soloNotifyEnabled        = soloNotifyEnabled;
        d.noPreNotifyEnabled       = noPreNotifyEnabled;
        d.supplyGrabbedNotifyEnabled = supplyGrabbedNotifyEnabled;
        d.supplyDroppedNotifyEnabled = supplyDroppedNotifyEnabled;
        d.cratePriorityEnabled     = cratePriorityEnabled;
        d.hideArmorStandsEnabled     = hideArmorStandsEnabled;
        d.hideArmorStandsBuild       = hideArmorStandsBuild;
        d.hideArmorStandsRightCannon = hideArmorStandsRightCannon;
        d.hideArmorStandsLeftCannon  = hideArmorStandsLeftCannon;
        d.hideArmorStandsShop        = hideArmorStandsShop;
        d.hideArmorStandsOthers      = hideArmorStandsOthers;
        d.kuudraHpHudEnabled       = kuudraHpHudEnabled;
        d.kuudraHpShowRaw          = kuudraHpShowRaw;
        d.kuudraHpHideBar          = kuudraHpHideBar;
        d.hollowWandEnabled             = hollowWandEnabled;
        d.kickedNotificationEnabled     = kickedNotificationEnabled;
        d.hideBossBarEnabled         = hideBossBarEnabled;
        d.hideFallingBlocksEnabled  = hideFallingBlocksEnabled;
        d.manaDrainAnnouncerEnabled  = manaDrainAnnouncerEnabled;
        d.hideEntityFireEnabled      = hideEntityFireEnabled;
        d.lavaBobberFixEnabled       = lavaBobberFixEnabled;
        d.legacyRodPhysicsEnabled    = legacyRodPhysicsEnabled;
        d.hideDamageTitleEnabled     = hideDamageTitleEnabled;
        d.hideDeadEntitiesEnabled    = hideDeadEntitiesEnabled;
        d.etherwarpWaypointsEnabled  = etherwarpWaypointsEnabled;
        d.selfPlayerScale            = selfPlayerScale;
        d.otherPlayerScale           = otherPlayerScale;
        d.kuudraSizeScale            = kuudraSizeScale;
        d.kuudraHpHudX             = kuudraHpHudX;
        d.kuudraHpHudY             = kuudraHpHudY;
        d.kuudraHpHudScale         = kuudraHpHudScale;

        d.wardrobeEnabled     = wardrobeEnabled;
        d.wardrobeOpenKey     = wardrobeOpenKey;
        d.statsOpenKey        = statsOpenKey;
        d.petsOpenKey         = petsOpenKey;
        d.eqWardrobeOpenKey   = eqWardrobeOpenKey;
        d.loadoutsOpenKey     = loadoutsOpenKey;
        d.wardrobeSlotKeys    = wardrobeSlotKeys.clone();
        d.loadoutSlotKeys     = loadoutSlotKeys.clone();
        d.wardrobeNextPageKey = wardrobeNextPageKey;
        d.wardrobePrevPageKey = wardrobePrevPageKey;
        d.wardrobeUnequipKey  = wardrobeUnequipKey;
        d.wardrobeDisableUnequipEnabled = wardrobeDisableUnequipEnabled;
        d.wardrobeAutoCloseEnabled      = wardrobeAutoCloseEnabled;
        d.lastEquippedWardrobeSlot      = lastEquippedWardrobeSlot;

        d.autoGfsToxic    = autoGfsToxic;
        d.autoGfsTwilight = autoGfsTwilight;
        d.toxicAmount     = toxicAmount;
        d.twilightAmount  = twilightAmount;

        d.chestTotal   = chestTotal;
        d.chestSuccess = chestSuccess;
        d.chestFail    = chestFail;

        d.splitTimerEnabled      = splitTimerEnabled;
        d.supplyTimesEnabled     = supplyTimesEnabled;
        d.splitPbs               = splitPbs;
        d.totalRunPbs            = totalRunPbs;
        d.pbRecords              = pbRecords;

        d.profitTrackerEnabled  = profitTrackerEnabled;
        d.profitShowDuringRun   = profitShowDuringRun;
        d.profitArmorSalvage    = profitArmorSalvage;
        d.profitFactionMage     = profitFactionMage;
        d.profitHighlightChests = profitHighlightChests;
        d.profitRerollCalc      = profitRerollCalc;
        d.profitBazaarInstaSell = profitBazaarInstaSell;
        d.profitBazaarInstaBuy  = profitBazaarInstaBuy;
        d.profitHudX            = profitHudX;
        d.profitHudY            = profitHudY;
        d.profitHudScale        = profitHudScale;
        d.kuudraPetRarity       = kuudraPetRarity.name();
        d.kuudraPetLevel        = kuudraPetLevel;
        d.chestValueGuiEnabled  = chestValueGuiEnabled;
        d.chestValueHudX        = chestValueHudX;
        d.chestValueHudY        = chestValueHudY;
        d.chestValueHudScale    = chestValueHudScale;

        d.itemCustomizationEnabled = itemCustomizationEnabled;
        d.itemCategorySettings     = ItemCustomization.serialiseBuiltin();
        d.itemCustomCategories     = ItemCustomization.serialiseCustom();
        d.notificationSounds       = new java.util.LinkedHashMap<>(notificationSounds);

        try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(d, w);
        } catch (IOException e) { KuudraHelperMod.LOGGER.error("Failed to save config", e); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static <T extends Enum<T>> T safeEnum(Class<T> type, String val, T fallback) {
        try { return Enum.valueOf(type, val); } catch (Exception e) { return fallback; }
    }

    private static void rebuildChunks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }

    // ── PB data model ─────────────────────────────────────────────────────────

    public static class PlayerTime {
        public String player = "";
        public double time   = 0;
        public int    pct    = -1;  // build % at fresh time; -1 if unknown
        public PlayerTime() {}
        public PlayerTime(String player, double time) { this.player = player; this.time = time; }
        public PlayerTime(String player, double time, int pct) { this.player = player; this.time = time; this.pct = pct; }
    }

    public static class PbRecord {
        public double           totalTime = 9999;
        public double[]         splits    = new double[7];
        public long             dateMs    = 0;
        public List<PlayerTime> supplies  = new ArrayList<>();
        public List<PlayerTime> freshes   = new ArrayList<>();
        public PbRecord() { Arrays.fill(splits, 9999.0); }
    }

    // ── Notification sounds ───────────────────────────────────────────────────

    public static final String SOUND_PEARL_NOW       = "pearl_now";
    public static final String SOUND_FAST_DPS        = "fast_dps";
    public static final String SOUND_FRESH           = "fresh";
    public static final String SOUND_BUILD_STARTED   = "build_started";
    public static final String SOUND_NO_PRE          = "no_pre";
    public static final String SOUND_SOLO            = "solo";
    public static final String SOUND_KICKED          = "kicked";
    public static final String SOUND_SUPPLY_GRABBED  = "supply_grabbed";
    public static final String SOUND_SUPPLY_DROPPED  = "supply_dropped";
    public static final String SOUND_WARDROBE_SWAP   = "wardrobe_swap";
    public static final String SOUND_BACKBONE_DONE   = "backbone_done";

    public static class NotificationSound {
        public boolean enabled = false;
        public String  soundId = "minecraft:entity.experience_orb.pickup";
        public float   volume  = 1.0f;
        public float   pitch   = 1.0f;
        public NotificationSound() {}
        public NotificationSound(String id, float pitch) {
            this.soundId = id; this.pitch = pitch;
        }
    }

    private static final java.util.LinkedHashMap<String, NotificationSound> notificationSounds
            = new java.util.LinkedHashMap<>();

    private static NotificationSound defaultSound(String key) {
        if (SOUND_PEARL_NOW.equals(key))
            return new NotificationSound("minecraft:block.note_block.pling", 2.0f);
        if (SOUND_FAST_DPS.equals(key))
            return new NotificationSound("minecraft:entity.experience_orb.pickup", 2.0f);
        if (SOUND_WARDROBE_SWAP.equals(key))
            return new NotificationSound("minecraft:ui.button.click", 1.5f);
        if (SOUND_BACKBONE_DONE.equals(key))
            return new NotificationSound("minecraft:block.note_block.pling", 1.5f);
        return new NotificationSound("minecraft:entity.experience_orb.pickup", 1.0f);
    }

    public static NotificationSound getNotificationSound(String key) {
        return notificationSounds.computeIfAbsent(key, KuudraConfig::defaultSound);
    }

    public static boolean isNotificationSoundEnabled(String key)          { return getNotificationSound(key).enabled; }
    public static void    setNotificationSoundEnabled(String key, boolean v) { getNotificationSound(key).enabled = v; save(); }
    public static String  getNotificationSoundId(String key)              { return getNotificationSound(key).soundId; }
    public static void    setNotificationSoundId(String key, String v)    { getNotificationSound(key).soundId = v; save(); }
    public static float   getNotificationSoundVolume(String key)          { return getNotificationSound(key).volume; }
    public static void    setNotificationSoundVolume(String key, float v) { getNotificationSound(key).volume = v; save(); }
    public static float   getNotificationSoundPitch(String key)           { return getNotificationSound(key).pitch; }
    public static void    setNotificationSoundPitch(String key, float v)  { getNotificationSound(key).pitch = v; save(); }

    public static void playNotificationSound(String key) {
        NotificationSound ns = getNotificationSound(key);
        if (!ns.enabled || ns.soundId == null || ns.soundId.isBlank()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        net.minecraft.resources.Identifier id =
                net.minecraft.resources.Identifier.tryParse(ns.soundId);
        if (id == null) return;
        net.minecraft.sounds.SoundEvent ev =
                net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id);
        mc.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(ev, ns.pitch, ns.volume));
    }

    // ── JSON model ────────────────────────────────────────────────────────────

    private static class Data {
        String  roleMode           = "AUTO";
        int     dpsValue           = 32;
        int     dpsRefillAmount    = 10;
        int     autoGfsDisableHpPercent = 40;
        int     stunValue          = 9;
        boolean eatenTimerEnabled             = false;
        boolean eatenTimerSubtractPingEnabled = false;
        boolean pickoblockEnabled             = false;
        boolean etherwarpLavaBlock = true;
        boolean hideElleDialogue   = false;
        boolean autoGfsEnabled     = true;

        String  lavaMode          = "DEFAULT";
        float   lavaOpacity       = 1f;
        int     lavaColor         = 0xFFAA0000;
        boolean lavaAsWater       = false;
        boolean lavaColorOverride = false;

        float   waterOpacity       = 1f;
        int     waterColor         = 0xFF2244AA;
        boolean waterAsLava        = false;
        boolean waterColorOverride = false;

        boolean fastDpsWarning          = true;
        boolean chestTrackerVisible     = true;
        boolean soloDetectorEnabled     = true;
        boolean cannonAutoClose         = false;
        boolean shopKeybindsEnabled     = false;
        int     shopMainKey             = 49;
        int     shopCannonKey           = 50;
        boolean explosionFilterEnabled  = false;
        float   explosionHideRadius     = 0.3f;
        float   explosionSizeMultiplier = 0.33f;
        boolean chestAnnouncerEnabled   = true;
        boolean autoKickEnabled         = false;
        boolean profileViewerEnabled    = true;
        int     akMinCatacombs         = -1;
        int     akMinForaging          = -1;
        int     akMinMagicalPower      = -1;
        int     akMinInfernal          = -1;
        int     akMinFiery             = -1;
        int     akMinBurning           = -1;
        int     akMinHot               = -1;
        int     akMinBasic             = -1;
        boolean akRequireRend          = false;
        int     akMinGdragLevel        = -1;
        boolean partyCmdsEnabled        = true;
        boolean autoRequeueEnabled      = true;
        boolean autoRequeueMessageEnabled = true;
        boolean autoUpdatesEnabled      = true;
        boolean developerFeaturesEnabled = false;
        boolean autoSprintEnabled       = false;
        boolean slotBindsEnabled        = false;
        int     slotBindSetKey          = -1;
        int     slotBindShowKey         = -1;
        int[][] slotBindPairs           = new int[0][];

        boolean pearlWaypointsEnabled = true;
        boolean showAllWaypoints      = true;
        boolean pearlFlatEnabled      = true;
        boolean pearlSkyEnabled       = true;
        boolean pearlDoubleEnabled    = true;
        float   doublePearlDelayS     = 0.2f;
        String  waypointType          = "CIRCLE";
        boolean waypointFill          = true;
        boolean pearlTickUpdate       = true;
        boolean dropLocationsEnabled  = true;
        boolean waypointLinesEnabled           = false;
        boolean waypointLinesSuppliesEnabled   = true;
        boolean waypointLinesFlatPearlsEnabled = true;
        String  secondSupplyPreference = "DOUBLE_PEARL";
        boolean pearlTimerEnabled     = true;
        float   pearlTimerHeight      = 0.35f;
        float   pearlTimerSize        = 0.5f;
        float   pearlCircleSize       = 0.5f;
        String  kuudraTalisman        = "HEART";
        int     lowPing               = 0;
        float   waypointFillAlpha     = 0.25f;
        float   beaconAlpha           = 0.63f;
        int     wpColNormal           = 0xFFFFFF;
        int     wpColCorrect          = 0xFF4444;
        int     wpColHovered          = 0xFFAA00;
        int     wpColReady            = 0x33FF33;
        int     beaconColNormal       = 0xFFFFFF;
        int     beaconColCorrect      = 0x00C800;
        float   buildBeaconAlpha      = 0.63f;
        boolean blockSlot9Enabled          = false;
        boolean stunPreviewEnabled         = false;
        boolean buildBeaconsEnabled        = false;
        boolean elleHighlightEnabled       = false;
        boolean rendDamageEnabled          = false;
        boolean rendTrackerEnabled         = false;
        boolean backboneProgressBarEnabled = false;
        boolean backboneProgressBarOutsideKuudraEnabled = false;
        float backboneProgressBarHudX     = 0.5f;
        float backboneProgressBarHudY     = 0.6f;
        float backboneProgressBarHudScale = 1.0f;
        boolean kuudraHighlightEnabled     = false;
        boolean kuudraHighlightFilled      = false;
        boolean etherwarpSlotBlockerEnabled = false;
        boolean supplyWaypointsEnabled           = false;
        boolean supplyBeaconsEnabled         = false;
        boolean noPreAnnounceEnabled         = false;
        boolean supplyLocationAnnounceEnabled = false;
        boolean supplyProgressHudEnabled     = false;
        boolean buildProgressHudEnabled      = false;
        boolean announceFreshEnabled         = false;
        boolean supplyHitboxEnabled          = false;
        boolean supplyRodRadiusEnabled       = false;
        boolean supplyPearlHitboxEnabled     = false;
        boolean pearlRefillEnabled           = false;
        boolean pearlRefillOutsideKuudraEnabled = false;
        boolean hideSelfieEnabled            = false;
        boolean preventPlacingPlayerHeadsEnabled      = false;
        boolean preventPlacingPlayerHeadsExceptGarden = true;
        boolean preventPlacingWeaponsEnabled          = false;
        boolean supplyGiantHitboxEnabled              = false;
        boolean giantHitboxEnabled                    = false;
        boolean giantHitboxFilled                     = false;
        float   giantHitboxFillOpacity                = 0.05f;
        int     giantHitboxColor                      = 0xFFFFFF;
        boolean kuudraDirectionEnabled  = false;

        float mountTimerHudX     = 0.5f;
        float mountTimerHudY     = 0.56f;
        float mountTimerHudScale = 1.0f;
        float directionHudX     = 0.5f;
        float directionHudY     = 0.25f;
        float directionHudScale = 1.0f;
        float splitHudX         = 0.005f;
        float splitHudY         = 0.01f;
        float splitHudScale     = 1.0f;
        float pearlTitleHudX    = 0.5f;
        float pearlTitleHudY    = 0.5f;
        float pearlTitleHudScale = 1.0f;
        boolean smoothCratePickupEnabled = false;
        float supplyProgressHudX     = 0.5f;
        float supplyProgressHudY     = 0.35f;
        float supplyProgressHudScale = 1.0f;
        float buildProgressHudX      = 0.5f;
        float buildProgressHudY      = 0.45f;
        float buildProgressHudScale  = 1.0f;
        float notificationHudX       = 0.5f;
        float notificationHudY       = 0.15f;
        float notificationHudScale   = 1.5f;
        float cratePriorityHudX      = 0.5f;
        float cratePriorityHudY      = 0.6f;
        float cratePriorityHudScale  = 2.0f;
        boolean supplyRecoveryMsgEnabled = false;
        boolean freshNotifyEnabled       = false;
        boolean buildStartedNotifyEnabled= false;
        boolean fastDpsNotifyEnabled     = false;
        boolean soloNotifyEnabled        = false;
        boolean noPreNotifyEnabled       = false;
        boolean supplyGrabbedNotifyEnabled = false;
        boolean supplyDroppedNotifyEnabled = false;
        boolean cratePriorityEnabled     = false;
        boolean hideArmorStandsEnabled     = false;
        boolean hideArmorStandsBuild       = true;
        boolean hideArmorStandsRightCannon = true;
        boolean hideArmorStandsLeftCannon  = true;
        boolean hideArmorStandsShop        = true;
        boolean hideArmorStandsOthers      = true;
        boolean kuudraHpHudEnabled       = false;
        boolean kuudraHpShowRaw          = false;
        boolean kuudraHpHideBar          = false;
        boolean hollowWandEnabled           = false;
        boolean kickedNotificationEnabled   = false;
        boolean hideBossBarEnabled         = false;
        boolean hideFallingBlocksEnabled  = false;
        boolean manaDrainAnnouncerEnabled  = false;
        boolean hideEntityFireEnabled      = false;
        boolean lavaBobberFixEnabled       = false;
        boolean legacyRodPhysicsEnabled    = false;
        boolean hideDamageTitleEnabled     = false;
        boolean hideDeadEntitiesEnabled    = false;
        boolean etherwarpWaypointsEnabled  = false;
        float   selfPlayerScale            = 100.0f;
        float   otherPlayerScale           = 100.0f;
        float   kuudraSizeScale            = 100.0f;
        float   kuudraHpHudX             = 0.5f;
        float   kuudraHpHudY             = 0.07f;
        float   kuudraHpHudScale         = 1.0f;

        boolean wardrobeEnabled     = false;
        int     wardrobeOpenKey     = -1;
        int     statsOpenKey        = -1;
        int     petsOpenKey         = -1;
        int     eqWardrobeOpenKey   = -1;
        int     loadoutsOpenKey     = -1;
        int[]   wardrobeSlotKeys    = {49,50,51,52,53,54,55,56,57};
        int[]   loadoutSlotKeys     = {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
        int     wardrobeNextPageKey = 262;
        int     wardrobePrevPageKey = 263;
        int     wardrobeUnequipKey  = 85;
        boolean wardrobeDisableUnequipEnabled = true;
        boolean wardrobeAutoCloseEnabled      = true;
        int     lastEquippedWardrobeSlot      = -1;

        boolean autoGfsToxic    = false;
        boolean autoGfsTwilight = false;
        int     toxicAmount     = 10;
        int     twilightAmount  = 10;

        int chestTotal   = 0;
        int chestSuccess = 0;
        int chestFail    = 0;

        boolean profitTrackerEnabled   = false;
        boolean profitShowDuringRun    = false;
        boolean profitArmorSalvage     = true;
        boolean profitFactionMage      = true;
        boolean profitHighlightChests  = true;
        boolean profitRerollCalc       = true;
        boolean profitBazaarInstaSell  = true;
        boolean profitBazaarInstaBuy   = false;
        float   profitHudX             = 0.01f;
        float   profitHudY             = 0.5f;
        float   profitHudScale         = 1.0f;
        String  kuudraPetRarity        = "LEGENDARY";
        int     kuudraPetLevel         = 100;

        boolean chestValueGuiEnabled   = true;
        float   chestValueHudX         = 0.3f;
        float   chestValueHudY         = 0.3f;
        float   chestValueHudScale     = 1.0f;

        boolean   splitTimerEnabled  = true;
        boolean   supplyTimesEnabled = true;
        double[][] splitPbs          = null;
        double[]   totalRunPbs       = null;
        PbRecord[] pbRecords         = null;

        boolean                                          itemCustomizationEnabled = true;
        Map<String, ItemTransformSettings>               itemCategorySettings     = null;
        java.util.List<ItemCustomization.CustomCategory> itemCustomCategories     = null;
        java.util.LinkedHashMap<String, NotificationSound> notificationSounds     = null;
    }
}