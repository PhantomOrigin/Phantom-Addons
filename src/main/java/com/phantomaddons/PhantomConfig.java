package com.phantomaddons;

import com.mojang.blaze3d.platform.InputConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.phantomaddons.features.customisation.items.ItemCustomization;
import com.phantomaddons.features.customisation.items.ItemTransformSettings;
import com.phantomaddons.features.misckuudra.profile.RemoteFeatureGate;
import com.phantomaddons.utils.KuudraTierDetector;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PhantomConfig {

    private static final Gson GSON        = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = com.phantomaddons.utils.PhantomPaths.resolve("phantomaddons.json");

    public enum RoleMode       { DPS, STUN, AUTO }
    public enum LavaMode       { DEFAULT, COLOURED }
    public enum WaypointType   { CIRCLE, SQUARE }
    public enum SecondSupplyPreference { DOUBLE_PEARL, ETHERWARP }
    public enum KuudraTalisman { NONE, KIDNEY, LUNG, HEART }
    public enum UiTheme        { DARK, LIGHT }

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

    private static RoleMode               roleMode               = RoleMode.AUTO;
    private static LavaMode               lavaMode               = LavaMode.DEFAULT;
    private static UiTheme                uiTheme                = UiTheme.DARK;
    private static WaypointType           waypointType           = WaypointType.CIRCLE;
    private static SecondSupplyPreference secondSupplyPreference = SecondSupplyPreference.DOUBLE_PEARL;
    private static KuudraTalisman         kuudraTalisman         = KuudraTalisman.NONE;
    private static KuudraPetRarity        kuudraPetRarity        = KuudraPetRarity.LEGENDARY;

    private static boolean profitTrackerForced = false; // not saved — session-only
    private static final LinkedHashMap<Integer,Integer> slotBindings = new LinkedHashMap<>();

    //? if <26.3 {
    private static final int KEY_SCHEME = 1;
    //?} else {
    /*private static final int KEY_SCHEME = 2;
    *///?}

    private static Data cfg = freshState();

    private static final int[][] PEARL_DELAY = {
            {0, 3000, 4000, 5000, 6000, 6000},
            {0, 2750, 3750, 4500, 5500, 5500},
            {0, 2500, 3250, 4000, 5000, 5000},
            {0, 2250, 3000, 3500, 4250, 4250},
    };

    private static Data freshState() {
        Data d = new Data();
        d.dpsValue                = 40;
        d.stunPreviewLeftEnabled  = false;
        d.stunPreviewRightEnabled = false;
        d.stunPreviewBackEnabled  = false;
        d.loadoutsEnabled         = false;
        d.splitPbs        = defaultPbs();
        d.totalRunPbs     = defaultTotalPbs();
        d.pbRecords       = new PbRecord[6];
        d.totalRunTimeSum = new double[6];
        d.totalRunCount   = new int[6];
        return d;
    }

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

    // ── Profit tracker ───────────────────────────────────────────────────────────

    public static boolean         isProfitTrackerEnabled()    { return cfg.profitTrackerEnabled; }
    public static boolean         isProfitTrackerForced()     { return profitTrackerForced; }
    public static boolean         toggleProfitTrackerForced() { return profitTrackerForced = !profitTrackerForced; }
    public static boolean         isProfitShowDuringRun()     { return cfg.profitShowDuringRun; }
    public static boolean         isProfitArmorSalvage()      { return cfg.profitArmorSalvage; }
    public static boolean         isProfitFactionMage()       { return cfg.profitFactionMage; }
    public static boolean         isHighlightUnopenedChestsEnabled()   { return cfg.highlightUnopenedChestsEnabled; }
    public static boolean         isProfitRerollCalc()        { return cfg.profitRerollCalc; }
    public static boolean         isBlockExpensiveRerollEnabled() { return cfg.blockExpensiveReroll; }
    public static boolean         isProfitBazaarInstaSell()   { return cfg.profitBazaarInstaSell; }
    public static boolean         isProfitBazaarInstaBuy()    { return cfg.profitBazaarInstaBuy; }
    public static float           getProfitHudX()             { return cfg.profitHudX; }
    public static float           getProfitHudY()             { return cfg.profitHudY; }
    public static float           getProfitHudScale()         { return cfg.profitHudScale; }
    public static KuudraPetRarity getKuudraPetRarity()        { return kuudraPetRarity; }
    public static int             getKuudraPetLevel()         { return cfg.kuudraPetLevel; }
    public static int             getEssenceOfCrimson()       { return cfg.essenceOfCrimson; }
    public static int             getEchoOfEssence()          { return cfg.echoOfEssence; }
    public static double          getKuudraPetEssenceMultiplier() {
        double crimsonBonus = 0.01 * cfg.essenceOfCrimson * (1.0 + 0.03 * cfg.echoOfEssence);
        return kuudraPetRarity.essenceMultiplier(cfg.kuudraPetLevel) * (1.0 + crimsonBonus);
    }

    public static void setProfitTrackerEnabled(boolean v)       { cfg.profitTrackerEnabled  = v; save(); }
    public static void setProfitShowDuringRun(boolean v)        { cfg.profitShowDuringRun   = v; save(); }
    public static void setProfitArmorSalvage(boolean v)         { cfg.profitArmorSalvage    = v; save(); }
    public static void setProfitFactionMage(boolean v)          { cfg.profitFactionMage     = v; save(); }
    public static void setHighlightUnopenedChestsEnabled(boolean v)      { cfg.highlightUnopenedChestsEnabled = v; save(); }
    public static void setProfitRerollCalc(boolean v)           { cfg.profitRerollCalc      = v; save(); }
    public static void setBlockExpensiveRerollEnabled(boolean v) { cfg.blockExpensiveReroll = v; save(); }
    public static void setProfitBazaarInstaSell(boolean v)      { cfg.profitBazaarInstaSell = v; save(); }
    public static void setProfitBazaarInstaBuy(boolean v)       { cfg.profitBazaarInstaBuy  = v; save(); }
    public static void setProfitHudX(float v)                   { cfg.profitHudX    = v; save(); }
    public static void setProfitHudY(float v)                   { cfg.profitHudY    = v; save(); }
    public static void setProfitHudScale(float v)               { cfg.profitHudScale = v; save(); }
    public static void setKuudraPetRarity(KuudraPetRarity v)    { kuudraPetRarity = v; save(); }
    public static void setKuudraPetLevel(int v)                 { cfg.kuudraPetLevel = Math.clamp(v, 1, 100); save(); }
    public static void setEssenceOfCrimson(int v)                { cfg.essenceOfCrimson = Math.clamp(v, 0, 10); save(); }
    public static void setEchoOfEssence(int v)                   { cfg.echoOfEssence = Math.clamp(v, 0, 10); save(); }

    public static boolean isChestValueGuiEnabled()  { return cfg.chestValueGuiEnabled; }
    public static float   getChestValueHudX()       { return cfg.chestValueHudX; }
    public static float   getChestValueHudY()       { return cfg.chestValueHudY; }
    public static float   getChestValueHudScale()   { return cfg.chestValueHudScale; }

    public static void setChestValueGuiEnabled(boolean v) { cfg.chestValueGuiEnabled = v; save(); }
    public static void setChestValueHudX(float v)         { cfg.chestValueHudX    = v; save(); }
    public static void setChestValueHudY(float v)         { cfg.chestValueHudY    = v; save(); }
    public static void setChestValueHudScale(float v)     { cfg.chestValueHudScale = v; save(); }

    public static float getChestTrackerHudX()       { return cfg.chestTrackerHudX; }
    public static float getChestTrackerHudY()       { return cfg.chestTrackerHudY; }
    public static float getChestTrackerHudScale()   { return cfg.chestTrackerHudScale; }
    public static void  setChestTrackerHudX(float v)     { cfg.chestTrackerHudX     = v; save(); }
    public static void  setChestTrackerHudY(float v)     { cfg.chestTrackerHudY     = v; save(); }
    public static void  setChestTrackerHudScale(float v) { cfg.chestTrackerHudScale = v; save(); }

    // ── Getters ───────────────────────────────────────────────────────────────

    // General
    public static RoleMode getRoleMode()                 { return roleMode; }
    public static boolean  isDpsMode()                   { return roleMode == RoleMode.DPS; }
    public static boolean  isStunMode()                  { return roleMode == RoleMode.STUN; }
    public static boolean  isAutoMode()                  { return roleMode == RoleMode.AUTO; }
    public static int      getDpsValue()                 { return cfg.dpsValue; }
    public static int      getDpsRefillAmount()          { return cfg.dpsRefillAmount; }
    public static int      getAutoGfsDisableHpPercent()  { return cfg.autoGfsDisableHpPercent; }
    public static int      getStunValue()                { return cfg.stunValue; }
    public static boolean  isEatenTimerEnabled()              { return cfg.eatenTimerEnabled; }
    public static boolean  isEatenTimerSubtractPingEnabled()  { return cfg.eatenTimerSubtractPingEnabled; }
    public static boolean  isPickoblockEnabled()         { return cfg.pickoblockEnabled; }
    public static boolean  isEtherwarpLavaBlockEnabled() { return cfg.etherwarpLavaBlock; }
    public static boolean  isEtherwarpLavaBlockOnlyInKuudra() { return cfg.etherwarpLavaBlockOnlyInKuudra; }
    public static boolean  isHideElleDialogueEnabled()   { return cfg.hideElleDialogue; }
    public static boolean  isAutoGfsEnabled()            { return cfg.autoGfsEnabled; }

    // Lava
    public static boolean  isLavaTweaksEnabled() { return cfg.lavaTweaksEnabled; }
    public static LavaMode getLavaMode()         { return lavaMode; }
    public static float    getLavaOpacity()      { return cfg.lavaTweaksEnabled ? cfg.lavaOpacity : 1.0f; }
    public static int      getLavaColor()        { return cfg.lavaColor; }
    public static boolean  isLavaAsWater()       { return cfg.lavaTweaksEnabled && cfg.lavaAsWater; }
    public static boolean  isLavaColorOverride() { return cfg.lavaTweaksEnabled && cfg.lavaColorOverride; }

    // Misc
    public static boolean isFastDpsWarningEnabled()   { return cfg.fastDpsWarning; }
    public static boolean isChestTrackerVisible()     { return cfg.chestTrackerVisible; }
    public static boolean isSoloDetectorEnabled()     { return cfg.soloDetectorEnabled; }
    public static boolean isCannonAutoCloseEnabled()  { return cfg.cannonAutoClose; }
    public static boolean isKuudraDirectionEnabled()   { return cfg.kuudraDirectionEnabled; }    public static boolean isTuxedoWarningEnabled()     { return cfg.tuxedoWarningEnabled; }
    public static boolean isBoneTimingAssistEnabled()  { return cfg.boneTimingAssistEnabled; }
    public static float   getBoneTimingAssistOffset()  { return cfg.boneTimingAssistOffset; }
    public static boolean isBoneTimingHitboxOutlineEnabled() { return cfg.boneTimingHitboxOutlineEnabled; }
    public static boolean isBoneTimingHitboxOutlineFilled()  { return cfg.boneTimingHitboxOutlineFilled; }
    public static int     getBoneTimingHitboxOutlineColor()  { return cfg.boneTimingHitboxOutlineColor; }
    public static boolean isBoneTimingHitboxOutlineOnlyCurrentDirection() { return cfg.boneTimingHitboxOutlineOnlyCurrentDirection; }
    public static boolean isBoneTimingHitboxHighlightInRangeEnabled() { return cfg.boneTimingHitboxHighlightInRange; }
    public static boolean isShopKeybindsEnabled()     { return cfg.shopKeybindsEnabled; }
    public static boolean isMiddleClickShopGuiEnabled() { return cfg.middleClickShopGuiEnabled; }
    public static int     getShopMainKey()            { return cfg.shopMainKey; }
    public static int     getShopCannonKey()          { return cfg.shopCannonKey; }
    public static boolean isExplosionFilterEnabled()  { return cfg.explosionFilterEnabled; }
    public static float   getExplosionHideRadius()    { return cfg.explosionHideRadius * 50f; }
    public static float   getExplosionHideRadiusRaw() { return cfg.explosionHideRadius; }
    public static float   getExplosionSizeMultiplier(){ return cfg.explosionSizeMultiplier * 3.0f; }
    public static float   getExplosionSizeRaw()       { return cfg.explosionSizeMultiplier; }
    public static boolean isChestAnnouncerEnabled()   { return cfg.chestAnnouncerEnabled; }

    public static boolean isAutoKickEnabled()     { return RemoteFeatureGate.isEnabled() && cfg.autoKickEnabled; }
    public static boolean isProfileViewerEnabled() { return RemoteFeatureGate.isEnabled() && cfg.profileViewerEnabled; }
    public static int     getAkMinCatacombs()     { return cfg.akMinCatacombs; }
    public static int     getAkMinForaging()      { return cfg.akMinForaging; }
    public static int     getAkMinMagicalPower()  { return cfg.akMinMagicalPower; }
    public static int     getAkMinInfernal()      { return cfg.akMinInfernal; }
    public static int     getAkMinFiery()         { return cfg.akMinFiery; }
    public static int     getAkMinBurning()       { return cfg.akMinBurning; }
    public static int     getAkMinHot()           { return cfg.akMinHot; }
    public static int     getAkMinBasic()         { return cfg.akMinBasic; }
    public static boolean isAkRequireRend()       { return cfg.akRequireRend; }
    public static int     getAkMinGdragLevel()    { return cfg.akMinGdragLevel; }
    public static boolean isPartyCmdsEnabled()        { return cfg.partyCmdsEnabled; }
    public static boolean isAutoRequeueEnabled()      { return cfg.autoRequeueEnabled; }
    public static boolean isAutoRequeueMessageEnabled() { return cfg.autoRequeueMessageEnabled; }
    public static boolean isAutoUpdatesEnabled()      { return cfg.autoUpdatesEnabled; }
    public static boolean isDeveloperFeaturesEnabled() { return cfg.developerFeaturesEnabled; }
    public static boolean isShareSupplyData() { return cfg.shareSupplyData; }
    public static boolean isChangelogEnabled() { return cfg.changelogEnabled; }
    public static String getLastChangelogVersion() { return cfg.lastChangelogVersion == null ? "" : cfg.lastChangelogVersion; }
    public static boolean isFirstRun() { return firstRun; }
    public static String getSkippedUpdateVersion() { return cfg.skippedUpdateVersion == null ? "" : cfg.skippedUpdateVersion; }
    public static boolean isTentacleHighlightEnabled() { return cfg.developerFeaturesEnabled && cfg.tentacleHighlightEnabled; }
    public static boolean isTentacleSpotBeaconsEnabled() { return cfg.developerFeaturesEnabled && cfg.tentacleSpotBeaconsEnabled; }
    public static UiTheme  getUiTheme() { return uiTheme; }
    public static float    getUiGuiScale() { return cfg.uiGuiScale; }
    public static boolean  isTentacleReachEnabled() { return cfg.tentacleReachEnabled; }
    public static boolean  isTentacleReachGround() { return cfg.tentacleReachGround; }
    public static boolean  isTentacleReachLava() { return cfg.tentacleReachLava; }
    public static float    getTentacleReachOpacity() { return Math.max(0.02f, Math.min(1.0f, cfg.tentacleReachOpacity)); }
    public enum PvTheme { MATCH, DARK, LIGHT }
    public static boolean  isPvEnabled()      { return RemoteFeatureGate.isEnabled() && cfg.pvEnabled; }
    public static float    getPvGuiScale()    { return cfg.pvGuiScale; }
    public enum PvContent { SMALL, MEDIUM, LARGE }
    public static PvContent getPvContent()    { return safeEnum(PvContent.class, cfg.pvContent, PvContent.MEDIUM); }
    public static float    getPvElementScale(){ return cfg.pvElementScale; }
    public static PvTheme  getPvTheme()       { return safeEnum(PvTheme.class, cfg.pvTheme, PvTheme.MATCH); }
    public static UiTheme  getEffectivePvTheme() {
        return switch (getPvTheme()) {
            case MATCH -> uiTheme;
            case DARK  -> UiTheme.DARK;
            case LIGHT -> UiTheme.LIGHT;
        };
    }
    public static void setPvEnabled(boolean v)      { cfg.pvEnabled = v; save(); }
    public static boolean  isPvEnabledSetting()        { return cfg.pvEnabled; }
    public static boolean  isPvPromptAnswered()        { return cfg.pvPromptAnswered; }
    public static void setPvPromptAnswered(boolean v)  { cfg.pvPromptAnswered = v; save(); }
    public static boolean  hasOpenedPv()               { return cfg.pvOpened; }
    public static void markPvOpened()                  { if (!cfg.pvOpened) { cfg.pvOpened = true; save(); } }
    public static void setPvGuiScale(float v)       { cfg.pvGuiScale = Math.max(0.6f, Math.min(2.5f, v)); save(); }
    public static void setPvContent(PvContent v)    { cfg.pvContent = v.name(); save(); }
    public static void setPvElementScale(float v)   { cfg.pvElementScale = Math.max(0.6f, Math.min(1.6f, v)); save(); }
    public static void setPvTheme(PvTheme v)        { cfg.pvTheme = v.name(); save(); }
    public static boolean isPvArabicNumerals()      { return cfg.pvArabicNumerals; }
    public static void setPvArabicNumerals(boolean v) { cfg.pvArabicNumerals = v; save(); }
    public static boolean isPvClickNames()        { return cfg.pvClickNames; }
    public static void setPvClickNames(boolean v) { cfg.pvClickNames = v; save(); }
    public static boolean isPvPartyFinderPreview()        { return cfg.pvPartyFinderPreview; }
    public static void setPvPartyFinderPreview(boolean v) { cfg.pvPartyFinderPreview = v; save(); }
    public static boolean isPvTabIcons()          { return cfg.pvTabIcons; }
    public static void setPvTabIcons(boolean v)   { cfg.pvTabIcons = v; save(); }
    public static boolean isPvOverflowSkills()      { return cfg.pvOverflowSkills; }
    public static void setPvOverflowSkills(boolean v) { cfg.pvOverflowSkills = v; save(); }
    public static boolean isPvRememberPage()        { return cfg.pvRememberPage; }
    public static void setPvRememberPage(boolean v) { cfg.pvRememberPage = v; save(); }
    public static boolean isPvNameSuggestions()        { return cfg.pvNameSuggestions; }
    public static void setPvNameSuggestions(boolean v) { cfg.pvNameSuggestions = v; save(); }
    public static boolean isShopMenuGuiEnabled()        { return cfg.shopMenuGui; }
    public static void setShopMenuGuiEnabled(boolean v) { cfg.shopMenuGui = v; save(); }
    public static boolean isCaseInsensitiveCommands()        { return cfg.caseInsensitiveCommands; }
    public static void setCaseInsensitiveCommands(boolean v) { cfg.caseInsensitiveCommands = v; save(); }
    public static boolean isDonatorNamesEnabled()   { return cfg.donatorNamesEnabled; }
    public static void setDonatorNamesEnabled(boolean v) { cfg.donatorNamesEnabled = v; save(); }
    public static boolean  isUiTransparencyEnabled() { return cfg.uiTransparencyEnabled; }
    public static boolean isAutoSprintEnabled()       { return cfg.autoSprintEnabled; }
    public static boolean isSlotBindsEnabled()        { return cfg.slotBindsEnabled; }
    public static int     getSlotBindSetKey()          { return cfg.slotBindSetKey; }
    public static boolean isSlotBindsShowOnHover()     { return cfg.slotBindsShowOnHover; }
    public static void    setSlotBindsShowOnHover(boolean v) { cfg.slotBindsShowOnHover = v; save(); }
    public static LinkedHashMap<Integer,Integer> getSlotBindings() { return slotBindings; }
    public static void    setSlotBindSetKey(int v)    { cfg.slotBindSetKey = v; save(); }

    // Pearl
    public static boolean        isPearlWaypointsEnabled() { return cfg.pearlWaypointsEnabled; }
    public static boolean        isShowAllWaypoints()      { return cfg.showAllWaypoints; }
    public static boolean        isPearlFlatEnabled()      { return cfg.pearlFlatEnabled; }
    public static boolean        isPearlSkyEnabled()       { return cfg.pearlSkyEnabled; }
    public static boolean        isPearlDoubleEnabled()    { return cfg.pearlDoubleEnabled; }
    public static float          getDoublePearlDelayS()    { return cfg.doublePearlDelayS; }
    public static long           getDoublePearlDelayMs()   { return (long)(cfg.doublePearlDelayS * 1000); }
    public static WaypointType   getWaypointType()         { return waypointType; }
    public static boolean        isWaypointFillEnabled()   { return cfg.waypointFill; }
    public static boolean        isPearlTickUpdate()       { return cfg.pearlTickUpdate; }
    public static boolean        isDropLocationsEnabled()  { return cfg.dropLocationsEnabled; }
    public static boolean        isWaypointLinesEnabled()            { return cfg.waypointLinesEnabled; }
    public static boolean        isWaypointLinesSuppliesEnabled()    { return cfg.waypointLinesSuppliesEnabled; }
    public static boolean        isWaypointLinesFlatPearlsEnabled()  { return cfg.waypointLinesFlatPearlsEnabled; }
    public static SecondSupplyPreference getSecondSupplyPreference() { return secondSupplyPreference; }
    public static boolean        isPearlTimerEnabled()     { return cfg.pearlTimerEnabled; }
    public static float          getPearlTimerHeight()     { return cfg.pearlTimerHeight; }
    public static float          getPearlTimerSize()       { return cfg.pearlTimerSize; }
    public static float          getPearlCircleSize()      { return cfg.pearlCircleSize; }
    public static KuudraTalisman getKuudraTalisman()       { return kuudraTalisman; }
    public static int            getLowPing()              { return cfg.lowPing; }
    public static float          getWaypointFillAlpha()    { return cfg.waypointFillAlpha; }
    public static float          getBeaconAlpha()          { return cfg.beaconAlpha; }
    public static int            getWpColNormal()          { return cfg.wpColNormal; }
    public static int            getWpColCorrect()         { return cfg.wpColCorrect; }
    public static int            getWpColHovered()         { return cfg.wpColHovered; }
    public static int            getWpColReady()           { return cfg.wpColReady; }
    public static int            getBeaconColNormal()      { return cfg.beaconColNormal; }
    public static int            getBeaconColCorrect()     { return cfg.beaconColCorrect; }
    public static float          getBuildBeaconAlpha()     { return cfg.buildBeaconAlpha; }
    public static int            getElleOutlineColor()     { return cfg.elleOutlineColor; }
    public static int            getElleBeaconColor()      { return cfg.elleBeaconColor; }
    public static String         getKuudraApiKey()         { return cfg.kuudraApiKey; }
    public static boolean        isBlockSlot9Enabled()     { return cfg.blockSlot9Enabled; }
    public static boolean        isBlockAtomsplitEnabled() { return cfg.blockAtomsplitEnabled; }
    public static boolean        isIchorRadiusEnabled()    { return cfg.ichorRadiusEnabled; }
    public static boolean        isRightPeakBlockHighlightEnabled() { return cfg.rightPeakBlockHighlightEnabled; }
    public static boolean        isStunPreviewEnabled()           { return cfg.stunPreviewEnabled; }
    public static boolean        isStunPreviewLeftEnabled()       { return cfg.stunPreviewLeftEnabled; }
    public static boolean        isStunPreviewRightEnabled()      { return cfg.stunPreviewRightEnabled; }
    public static boolean        isStunPreviewBackEnabled()       { return cfg.stunPreviewBackEnabled; }
    public static boolean        isDpsWaypointEnabled()      { return cfg.dpsWaypointEnabled; }
    public static boolean        isBuildBeaconsEnabled()         { return cfg.buildBeaconsEnabled; }
    public static boolean        isElleHighlightEnabled()        { return cfg.elleHighlightEnabled; }
    public static boolean        isElleHighlightBeaconEnabled()  { return cfg.elleHighlightBeaconEnabled; }
    public static boolean        isRendDamageEnabled()           { return cfg.rendDamageEnabled; }
    public static boolean        isRendPullAttributionEnabled()  { return cfg.rendPullAttributionEnabled; }
    public static boolean        isRendTrackerEnabled()          { return cfg.rendTrackerEnabled; }
    public static boolean        isBackboneProgressBarEnabled()   { return cfg.backboneProgressBarEnabled; }
    public static boolean        isBackboneProgressBarOutsideKuudraEnabled() { return cfg.backboneProgressBarOutsideKuudraEnabled; }
    public static float          getBackboneProgressBarHudX()     { return cfg.backboneProgressBarHudX; }
    public static float          getBackboneProgressBarHudY()     { return cfg.backboneProgressBarHudY; }
    public static float          getBackboneProgressBarHudScale() { return cfg.backboneProgressBarHudScale; }
    public static boolean        isKuudraHighlightEnabled()      { return cfg.kuudraHighlightEnabled; }
    public static boolean        isKuudraHighlightFilled()       { return cfg.kuudraHighlightFilled; }
    public static boolean        isEtherwarpSlotBlockerEnabled()  { return cfg.etherwarpSlotBlockerEnabled; }
    public static boolean        isSupplyWaypointsEnabled()        { return cfg.supplyWaypointsEnabled; }
    public static boolean        isSupplyBeaconsEnabled()          { return cfg.supplyBeaconsEnabled; }
    public static boolean        isNoPreAnnounceEnabled()          { return cfg.noPreAnnounceEnabled; }
    public static boolean        isSupplyLocationAnnounceEnabled() { return cfg.supplyLocationAnnounceEnabled; }
    public static boolean        isSupplySpawnCountdownEnabled()   { return cfg.supplySpawnCountdownEnabled; }
    public static boolean        isBuildProgressHudEnabled()       { return cfg.buildProgressHudEnabled; }
    public static boolean        isStunReminderEnabled()           { return cfg.stunReminderEnabled; }
    public static RoleMode       getStunReminderRoleMode()         { return safeEnum(RoleMode.class, cfg.stunReminderRoleMode, RoleMode.AUTO); }
    public static int            getStunReminderTravelMs()         { return cfg.stunReminderTravelMs; }
    public static int            getStunReminderMaxPercent()       { return cfg.stunReminderMaxPercent; }
    public static float          getStunReminderHudX()             { return cfg.stunReminderHudX; }
    public static float          getStunReminderHudY()             { return cfg.stunReminderHudY; }
    public static float          getStunReminderHudScale()         { return cfg.stunReminderHudScale; }
    public static boolean        isAnnounceFreshEnabled()          { return cfg.announceFreshEnabled; }
    public static boolean        isSupplyHitboxEnabled()           { return cfg.supplyHitboxEnabled; }
    public static boolean        isSupplyRodRadiusEnabled()        { return cfg.supplyRodRadiusEnabled; }
    public static boolean        isSupplyPearlHitboxEnabled()      { return cfg.supplyPearlHitboxEnabled; }
    public static boolean        isPearlRefillEnabled()            { return cfg.pearlRefillEnabled; }
    public static boolean        isPearlRefillOutsideKuudraEnabled() { return cfg.pearlRefillOutsideKuudraEnabled; }
    public static boolean        isPearlRefillDungeonsEnabled()    { return cfg.pearlRefillDungeonsEnabled; }
    public static boolean        isHideSelfieEnabled()             { return cfg.hideSelfieEnabled; }
    public static boolean        isPreventPlacingPlayerHeadsEnabled()      { return cfg.preventPlacingPlayerHeadsEnabled; }
    public static boolean        isPreventPlacingPlayerHeadsExceptGarden() { return cfg.preventPlacingPlayerHeadsExceptGarden; }
    public static boolean        isPreventPlacingWeaponsEnabled()          { return cfg.preventPlacingWeaponsEnabled; }
    public static boolean        isBlockBossPhaseJumpEnabled()             { return cfg.blockBossPhaseJumpEnabled; }
    public static boolean        isBlockCloseItemEnabled()                 { return cfg.blockCloseItemEnabled; }
    public static boolean        isSupplyGiantHitboxEnabled()              { return cfg.supplyGiantHitboxEnabled; }
    public static boolean        isGiantHitboxEnabled()                    { return cfg.giantHitboxEnabled; }
    public static boolean        isTentacleGrabWarningEnabled()            { return cfg.tentacleGrabWarningEnabled; }
    public static float          getTentacleGrabWarningMinConfidence()     { return cfg.tentacleGrabWarningMinConfidence; }
    public static boolean        isTentacleGrabLineEnabled()               { return cfg.tentacleGrabLineEnabled; }
    public static boolean        isTentacleGrabHudEnabled()                { return cfg.tentacleGrabHudEnabled; }
    public static boolean        isGiantHitboxFilled()                     { return cfg.giantHitboxFilled; }
    public static float          getGiantHitboxFillOpacity()               { return cfg.giantHitboxFillOpacity; }
    public static int            getGiantHitboxColor()                     { return cfg.giantHitboxColor; }

    public static int getPickupDurationMs() {
        int tier = KuudraTierDetector.getTier();
        if (tier < 1 || tier > 5) tier = 5;
        return PEARL_DELAY[kuudraTalisman.ordinal()][tier];
    }

    // Dungeons
    public static boolean isAutoGfsToxicEnabled()    { return cfg.autoGfsToxic; }
    public static boolean isAutoGfsTwilightEnabled() { return cfg.autoGfsTwilight; }
    public static int     getToxicAmount()           { return cfg.toxicAmount; }
    public static int     getTwilightAmount()        { return cfg.twilightAmount; }

    // Chest tracker
    public static int getChestTotal()   { return cfg.chestTotal; }
    public static int getChestSuccess() { return cfg.chestSuccess; }
    public static int getChestFail()    { return cfg.chestFail; }

    // Split timer
    public static boolean isSplitTimerEnabled()  { return cfg.splitTimerEnabled; }
    public static boolean isSupplyTimesEnabled() { return cfg.supplyTimesEnabled; }
    public static boolean isRunBreakdownEnabled() { return cfg.runBreakdownEnabled; }
    public static boolean isShitterHighlightEnabled() { return cfg.shitterHighlightEnabled; }

    // Item customisation
    public static boolean isItemCustomizationEnabled() { return cfg.itemCustomizationEnabled; }

    public static double[] getSplitPb(int tier) {
        if (tier < 1 || tier > 5) tier = 5;
        return cfg.splitPbs[tier];
    }

    public static void setSplitPb(int tier, double[] pb) {
        if (tier < 1 || tier > 5) return;
        cfg.splitPbs[tier] = pb.clone();
        save();
    }

    public static double getTotalRunPb(int tier) {
        if (tier < 1 || tier > 5) tier = 5;
        return cfg.totalRunPbs[tier];
    }

    public static void setTotalRunPb(int tier, double time) {
        if (updateTotalRunPb(tier, time)) save();
    }

    public static int getHighestTierPlayed() {
        for (int t = 5; t >= 1; t--) if (cfg.totalRunPbs[t] < 9999) return t;
        return 5;
    }

    public static void recordTotalRunTime(int tier, double seconds) {
        if (tier < 1 || tier > 5) return;
        cfg.totalRunTimeSum[tier] += seconds;
        cfg.totalRunCount[tier]++;
        save();
    }

    public static double getTotalAverage(int tier) {
        if (tier < 1 || tier > 5 || cfg.totalRunCount[tier] <= 0) return -1;
        return cfg.totalRunTimeSum[tier] / cfg.totalRunCount[tier];
    }

    public static int getTotalRunCount(int tier) {
        if (tier < 1 || tier > 5) return 0;
        return cfg.totalRunCount[tier];
    }

    public static int getTotalTimedRuns() {
        int n = 0;
        for (int t = 1; t <= 5; t++) n += cfg.totalRunCount[t];
        return n;
    }

    public static double getTotalTimedSeconds() {
        double s = 0;
        for (int t = 1; t <= 5; t++) s += cfg.totalRunTimeSum[t];
        return s;
    }

    public static int getTotalAverageHighestTier() {
        for (int t = 5; t >= 1; t--) if (cfg.totalRunCount[t] > 0) return t;
        return -1;
    }

    public static PbRecord getPbRecord(int tier) {
        if (tier < 1 || tier > 5) tier = 5;
        return cfg.pbRecords[tier];
    }

    public static void setPbRecord(int tier, PbRecord record) {
        if (tier < 1 || tier > 5) return;
        cfg.pbRecords[tier]   = record;
        cfg.totalRunPbs[tier] = record.totalTime;
        if (record.splits != null && record.splits.length == 7)
            cfg.splitPbs[tier] = record.splits.clone();
        save();
    }

    public static boolean updateTotalRunPb(int tier, double time) {
        if (tier < 1 || tier > 5) return false;
        if (time >= cfg.totalRunPbs[tier]) return false;
        cfg.totalRunPbs[tier] = time;
        if (cfg.pbRecords[tier] == null) cfg.pbRecords[tier] = new PbRecord();
        cfg.pbRecords[tier].totalTime = time;
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
    public static void setDpsValue(int v)               { cfg.dpsValue = v;           save(); }
    public static void setDpsRefillAmount(int v)        { cfg.dpsRefillAmount = Math.max(4, v); save(); }
    public static void setAutoGfsDisableHpPercent(int v) { cfg.autoGfsDisableHpPercent = Math.max(25, Math.min(100, v)); save(); }
    public static void setStunValue(int v)              { cfg.stunValue = v;          save(); }
    public static void setEatenTimerEnabled(boolean v)             { cfg.eatenTimerEnabled = v;             save(); }
    public static void setEatenTimerSubtractPingEnabled(boolean v) { cfg.eatenTimerSubtractPingEnabled = v;  save(); }
    public static void setPickoblockEnabled(boolean v)  { cfg.pickoblockEnabled = v;  save(); }
    public static void setHideElleDialogue(boolean v)   { cfg.hideElleDialogue = v;   save(); }
    public static void setAutoGfsEnabled(boolean v)     { cfg.autoGfsEnabled = v;     save(); }
    public static void toggleEtherwarpLavaBlock()       { cfg.etherwarpLavaBlock = !cfg.etherwarpLavaBlock; save(); }
    public static void setEtherwarpLavaBlockOnlyInKuudra(boolean v) { cfg.etherwarpLavaBlockOnlyInKuudra = v; save(); }

    // Lava
    public static void setLavaTweaksEnabled(boolean v) { cfg.lavaTweaksEnabled = v;    save(); rebuildChunks(); }
    public static void setLavaMode(LavaMode m)         { lavaMode = m;             save(); rebuildChunks(); }
    public static void setLavaOpacity(float v)         { cfg.lavaOpacity = clamp01(v); save(); rebuildChunks(); }
    public static void setLavaColor(int c)             { cfg.lavaColor = c;            save(); rebuildChunks(); }
    public static void setLavaAsWater(boolean v)       { cfg.lavaAsWater = v;          save(); rebuildChunks(); }
    public static void setLavaColorOverride(boolean v) { cfg.lavaColorOverride = v;    save(); rebuildChunks(); }

    // Misc
    public static void setFastDpsWarningEnabled(boolean v)  { cfg.fastDpsWarning = v;           save(); }
    public static void setChestTrackerVisible(boolean v)    { cfg.chestTrackerVisible = v;      save(); }
    public static void setSoloDetectorEnabled(boolean v)    { cfg.soloDetectorEnabled = v;      save(); }
    public static void setCannonAutoCloseEnabled(boolean v) { cfg.cannonAutoClose = v;          save(); }
    public static void setKuudraDirectionEnabled(boolean v) { cfg.kuudraDirectionEnabled = v;   save(); }    public static void setTuxedoWarningEnabled(boolean v)   { cfg.tuxedoWarningEnabled = v;     save(); }
    public static void setBoneTimingAssistEnabled(boolean v) { cfg.boneTimingAssistEnabled = v; save(); }
    public static void setBoneTimingAssistOffset(float v) { cfg.boneTimingAssistOffset = Math.clamp(v, -3.0f, 1.0f); save(); }
    public static void setBoneTimingHitboxOutlineEnabled(boolean v) { cfg.boneTimingHitboxOutlineEnabled = v; save(); }
    public static void setBoneTimingHitboxOutlineFilled(boolean v)  { cfg.boneTimingHitboxOutlineFilled = v;  save(); }
    public static void setBoneTimingHitboxOutlineColor(int v)       { cfg.boneTimingHitboxOutlineColor = v & 0xFFFFFF; save(); }
    public static void setBoneTimingHitboxOutlineOnlyCurrentDirection(boolean v) { cfg.boneTimingHitboxOutlineOnlyCurrentDirection = v; save(); }
    public static void setBoneTimingHitboxHighlightInRange(boolean v) { cfg.boneTimingHitboxHighlightInRange = v; save(); }
    public static void setShopKeybindsEnabled(boolean v)    { cfg.shopKeybindsEnabled = v;      save(); }
    public static void setMiddleClickShopGuiEnabled(boolean v) { cfg.middleClickShopGuiEnabled = v; save(); }
    public static void setShopMainKey(int v)                { cfg.shopMainKey = v;              save(); }
    public static void setShopCannonKey(int v)              { cfg.shopCannonKey = v;            save(); }
    public static void setExplosionFilterEnabled(boolean v) { cfg.explosionFilterEnabled = v;   save(); }
    public static void setExplosionHideRadius(float v)      { cfg.explosionHideRadius = clamp01(v); save(); }
    public static void setExplosionSizeMultiplier(float v)  { cfg.explosionSizeMultiplier = clamp01(v); save(); }
    public static void setChestAnnouncerEnabled(boolean v)  { cfg.chestAnnouncerEnabled = v;   save(); }

    public static void setAutoKickEnabled(boolean v)   { if (RemoteFeatureGate.isEnabled()) { cfg.autoKickEnabled = v; save(); } }
    public static void setProfileViewerEnabled(boolean v) { if (RemoteFeatureGate.isEnabled()) { cfg.profileViewerEnabled = v; save(); } }
    public static void setAkMinCatacombs(int v)        { cfg.akMinCatacombs = v;    save(); }
    public static void setAkMinForaging(int v)         { cfg.akMinForaging = v;     save(); }
    public static void setAkMinMagicalPower(int v)     { cfg.akMinMagicalPower = v; save(); }
    public static void setAkMinInfernal(int v)         { cfg.akMinInfernal = v;     save(); }
    public static void setAkMinFiery(int v)            { cfg.akMinFiery = v;        save(); }
    public static void setAkMinBurning(int v)          { cfg.akMinBurning = v;      save(); }
    public static void setAkMinHot(int v)              { cfg.akMinHot = v;          save(); }
    public static void setAkMinBasic(int v)            { cfg.akMinBasic = v;        save(); }
    public static void setAkRequireRend(boolean v)     { cfg.akRequireRend = v;     save(); }
    public static void setAkMinGdragLevel(int v)       { cfg.akMinGdragLevel = v;   save(); }
    public static void setPartyCmdsEnabled(boolean v)       { cfg.partyCmdsEnabled = v;        save(); }
    public static void setAutoRequeueEnabled(boolean v)     { cfg.autoRequeueEnabled = v;      save(); }
    public static void setAutoRequeueMessageEnabled(boolean v) { cfg.autoRequeueMessageEnabled = v; save(); }
    public static void setAutoUpdatesEnabled(boolean v)     { cfg.autoUpdatesEnabled = v;      save(); }
    public static void setDeveloperFeaturesEnabled(boolean v) { cfg.developerFeaturesEnabled = v; save(); }
    public static void setShareSupplyData(boolean v) { cfg.shareSupplyData = v; save(); }
    public static void setChangelogEnabled(boolean v) { cfg.changelogEnabled = v; save(); }
    public static void setSkippedUpdateVersion(String v) { cfg.skippedUpdateVersion = v == null ? "" : v; save(); }
    public static void setLastChangelogVersion(String v) { cfg.lastChangelogVersion = v == null ? "" : v; save(); }
    public static void setTentacleHighlightEnabled(boolean v) { cfg.tentacleHighlightEnabled = v; save(); }
    public static void setTentacleSpotBeaconsEnabled(boolean v) { cfg.tentacleSpotBeaconsEnabled = v; save(); }
    public static void setUiTheme(UiTheme v) { uiTheme = v; save(); }
    public static void setTentacleReachEnabled(boolean v) { cfg.tentacleReachEnabled = v; save(); }
    public static void setTentacleReachGround(boolean v) { cfg.tentacleReachGround = v; save(); }
    public static void setTentacleReachLava(boolean v) { cfg.tentacleReachLava = v; save(); }
    public static void setTentacleReachOpacity(float v) { cfg.tentacleReachOpacity = Math.max(0.02f, Math.min(1.0f, v)); save(); }
    public static void setUiGuiScale(float v) { cfg.uiGuiScale = Math.max(0.7f, Math.min(1.6f, v)); save(); }
    public static void setUiTransparencyEnabled(boolean v) { cfg.uiTransparencyEnabled = v; save(); }
    public static void setAutoSprintEnabled(boolean v)      { cfg.autoSprintEnabled = v;       save(); }
    public static void setSlotBindsEnabled(boolean v)       { cfg.slotBindsEnabled = v;        save(); }
    public static void clearSlotBinding(int invSlot)        { slotBindings.remove(invSlot); save(); }
    public static void putSlotBinding(int invSlot, int hotbarIndex) { slotBindings.put(invSlot, hotbarIndex); save(); }

    // Pearl
    public static void setPearlWaypointsEnabled(boolean v)  { cfg.pearlWaypointsEnabled = v;    save(); }
    public static void setShowAllWaypoints(boolean v)        { cfg.showAllWaypoints = v;         save(); }
    public static void setPearlFlatEnabled(boolean v)        { cfg.pearlFlatEnabled = v;         save(); }
    public static void setPearlSkyEnabled(boolean v)         { cfg.pearlSkyEnabled = v;          save(); }
    public static void setPearlDoubleEnabled(boolean v)      { cfg.pearlDoubleEnabled = v;       save(); }
    public static void setDoublePearlDelayS(float v)         { cfg.doublePearlDelayS = Math.max(0.05f, Math.min(0.55f, v)); save(); }
    public static void setWaypointType(WaypointType t)       { waypointType = t;             save(); }
    public static void setWaypointFillEnabled(boolean v)     { cfg.waypointFill = v;             save(); }
    public static void setPearlTickUpdate(boolean v)         { cfg.pearlTickUpdate = v;          save(); }
    public static void setDropLocationsEnabled(boolean v)    { cfg.dropLocationsEnabled = v;     save(); }
    public static void setWaypointLinesEnabled(boolean v)           { cfg.waypointLinesEnabled = v;           save(); }
    public static void setWaypointLinesSuppliesEnabled(boolean v)   { cfg.waypointLinesSuppliesEnabled = v;   save(); }
    public static void setWaypointLinesFlatPearlsEnabled(boolean v) { cfg.waypointLinesFlatPearlsEnabled = v; save(); }
    public static void setSecondSupplyPreference(SecondSupplyPreference p) { secondSupplyPreference = p; save(); }
    public static void setPearlTimerEnabled(boolean v)       { cfg.pearlTimerEnabled = v;        save(); }
    public static void setPearlTimerHeight(float v)          { cfg.pearlTimerHeight = v;         save(); }
    public static void setPearlTimerSize(float v)            { cfg.pearlTimerSize = v;           save(); }
    public static void setPearlCircleSize(float v)           { cfg.pearlCircleSize = v;          save(); }
    public static void setKuudraTalisman(KuudraTalisman t)   { kuudraTalisman = t;           save(); }
    public static void setLowPing(int v)                     { cfg.lowPing = Math.max(0, v);     save(); }
    public static void setWaypointFillAlpha(float v)         { cfg.waypointFillAlpha = v;        save(); }
    public static void setBeaconAlpha(float v)               { cfg.beaconAlpha = v;              save(); }
    public static void setWpColNormal(int v)                 { cfg.wpColNormal = v & 0xFFFFFF;      save(); }
    public static void setWpColCorrect(int v)                { cfg.wpColCorrect = v & 0xFFFFFF;     save(); }
    public static void setWpColHovered(int v)                { cfg.wpColHovered = v & 0xFFFFFF;     save(); }
    public static void setWpColReady(int v)                  { cfg.wpColReady = v & 0xFFFFFF;       save(); }
    public static void setBeaconColNormal(int v)             { cfg.beaconColNormal = v & 0xFFFFFF;  save(); }
    public static void setBeaconColCorrect(int v)            { cfg.beaconColCorrect = v & 0xFFFFFF; save(); }
    public static void setBuildBeaconAlpha(float v)          { cfg.buildBeaconAlpha = clamp01(v);   save(); }
    public static void setElleOutlineColor(int v)            { cfg.elleOutlineColor = v & 0xFFFFFF; save(); }
    public static void setElleBeaconColor(int v)             { cfg.elleBeaconColor = v & 0xFFFFFF;  save(); }
    public static void setKuudraApiKey(String v)             { cfg.kuudraApiKey = v == null ? "" : v.trim(); save(); }
    public static void setBlockSlot9Enabled(boolean v)       { cfg.blockSlot9Enabled = v;        save(); }
    public static void setBlockAtomsplitEnabled(boolean v)   { cfg.blockAtomsplitEnabled = v;    save(); }
    public static void setIchorRadiusEnabled(boolean v)      { cfg.ichorRadiusEnabled = v;       save(); }
    public static void setRightPeakBlockHighlightEnabled(boolean v) { cfg.rightPeakBlockHighlightEnabled = v; save(); }
    public static void setStunPreviewEnabled(boolean v)          { cfg.stunPreviewEnabled = v;            save(); }
    public static void setStunPreviewLeftEnabled(boolean v)      { cfg.stunPreviewLeftEnabled = v;         save(); }
    public static void setStunPreviewRightEnabled(boolean v)     { cfg.stunPreviewRightEnabled = v;        save(); }
    public static void setStunPreviewBackEnabled(boolean v)      { cfg.stunPreviewBackEnabled = v;         save(); }
    public static void setDpsWaypointEnabled(boolean v)     { cfg.dpsWaypointEnabled = v;        save(); }
    public static void setBuildBeaconsEnabled(boolean v)         { cfg.buildBeaconsEnabled = v;           save(); }
    public static void setElleHighlightEnabled(boolean v)        { cfg.elleHighlightEnabled = v;          save(); }
    public static void setElleHighlightBeaconEnabled(boolean v)  { cfg.elleHighlightBeaconEnabled = v;    save(); }
    public static void setRendDamageEnabled(boolean v)           { cfg.rendDamageEnabled = v;             save(); }
    public static void setRendPullAttributionEnabled(boolean v)  { cfg.rendPullAttributionEnabled = v;    save(); }
    public static void setRendTrackerEnabled(boolean v)          { cfg.rendTrackerEnabled = v;            save(); }
    public static void setBackboneProgressBarEnabled(boolean v)   { cfg.backboneProgressBarEnabled = v;    save(); }
    public static void setBackboneProgressBarOutsideKuudraEnabled(boolean v) { cfg.backboneProgressBarOutsideKuudraEnabled = v; save(); }
    public static void setBackboneProgressBarHudX(float v)        { cfg.backboneProgressBarHudX     = clamp01(v); }
    public static void setBackboneProgressBarHudY(float v)        { cfg.backboneProgressBarHudY     = clamp01(v); }
    public static void setBackboneProgressBarHudScale(float v)    { cfg.backboneProgressBarHudScale = clampHudScale(v); }
    public static void setStunReminderHudX(float v)               { cfg.stunReminderHudX     = clamp01(v); }
    public static void setStunReminderHudY(float v)               { cfg.stunReminderHudY     = clamp01(v); }
    public static void setStunReminderHudScale(float v)           { cfg.stunReminderHudScale = clampHudScale(v); }
    public static void setKuudraHighlightEnabled(boolean v)     { cfg.kuudraHighlightEnabled = v;        save(); }
    public static void setKuudraHighlightFilled(boolean v)      { cfg.kuudraHighlightFilled = v;         save(); }
    public static void setEtherwarpSlotBlockerEnabled(boolean v) { cfg.etherwarpSlotBlockerEnabled = v; save(); }
    public static void setSupplyWaypointsEnabled(boolean v)          { cfg.supplyWaypointsEnabled = v;              save(); }
    public static void setSupplyBeaconsEnabled(boolean v)            { cfg.supplyBeaconsEnabled = v;                save(); }
    public static void setNoPreAnnounceEnabled(boolean v)            { cfg.noPreAnnounceEnabled = v;                save(); }
    public static void setSupplyLocationAnnounceEnabled(boolean v)   { cfg.supplyLocationAnnounceEnabled = v;       save(); }
    public static void setSupplySpawnCountdownEnabled(boolean v)     { cfg.supplySpawnCountdownEnabled = v;         save(); }
    public static void setBuildProgressHudEnabled(boolean v)         { cfg.buildProgressHudEnabled = v;             save(); }
    public static void setStunReminderEnabled(boolean v)             { cfg.stunReminderEnabled = v;                 save(); }
    public static void setStunReminderRoleMode(RoleMode m)           { cfg.stunReminderRoleMode = m.name();         save(); }
    public static void setStunReminderTravelMs(int v)                { cfg.stunReminderTravelMs = Math.max(0, Math.min(20000, v)); save(); }
    public static void setStunReminderMaxPercent(int v)              { cfg.stunReminderMaxPercent = Math.max(1, Math.min(100, v)); save(); }
    public static void setAnnounceFreshEnabled(boolean v)            { cfg.announceFreshEnabled = v;                save(); }
    public static void setSupplyHitboxEnabled(boolean v)             { cfg.supplyHitboxEnabled = v;                 save(); }
    public static void setSupplyRodRadiusEnabled(boolean v)          { cfg.supplyRodRadiusEnabled = v;              save(); }
    public static void setSupplyPearlHitboxEnabled(boolean v)        { cfg.supplyPearlHitboxEnabled = v;            save(); }
    public static void setPearlRefillEnabled(boolean v)              { cfg.pearlRefillEnabled = v;                  save(); }
    public static void setPearlRefillOutsideKuudraEnabled(boolean v) { cfg.pearlRefillOutsideKuudraEnabled = v;     save(); }
    public static void setPearlRefillDungeonsEnabled(boolean v)      { cfg.pearlRefillDungeonsEnabled = v;          save(); }
    public static void setHideSelfieEnabled(boolean v)               { cfg.hideSelfieEnabled = v;                    save(); }
    public static void setPreventPlacingPlayerHeadsEnabled(boolean v)      { cfg.preventPlacingPlayerHeadsEnabled = v;      save(); }
    public static void setPreventPlacingPlayerHeadsExceptGarden(boolean v) { cfg.preventPlacingPlayerHeadsExceptGarden = v; save(); }
    public static void setPreventPlacingWeaponsEnabled(boolean v)          { cfg.preventPlacingWeaponsEnabled = v;          save(); }
    public static void setBlockBossPhaseJumpEnabled(boolean v)             { cfg.blockBossPhaseJumpEnabled = v;             save(); }
    public static void setBlockCloseItemEnabled(boolean v)                 { cfg.blockCloseItemEnabled = v;                 save(); }
    public static void setSupplyGiantHitboxEnabled(boolean v)              { cfg.supplyGiantHitboxEnabled = v;              save(); }
    public static void setGiantHitboxEnabled(boolean v)                    { cfg.giantHitboxEnabled = v;                    save(); }
    public static void setTentacleGrabWarningEnabled(boolean v)            { cfg.tentacleGrabWarningEnabled = v;            save(); }
    public static void setTentacleGrabWarningMinConfidence(float v)        { cfg.tentacleGrabWarningMinConfidence = clamp01(v); save(); }
    public static void setTentacleGrabLineEnabled(boolean v)               { cfg.tentacleGrabLineEnabled = v;               save(); }
    public static void setTentacleGrabHudEnabled(boolean v)                { cfg.tentacleGrabHudEnabled = v;                save(); }
    public static void setGiantHitboxFilled(boolean v)                     { cfg.giantHitboxFilled = v;                     save(); }
    public static void setGiantHitboxFillOpacity(float v)                  { cfg.giantHitboxFillOpacity = clamp01(v);       save(); }
    public static void setGiantHitboxColor(int v)                          { cfg.giantHitboxColor = v & 0xFFFFFF;           save(); }

    // HUD layout (no auto-save — HudEditorScreen calls save() on close)
    public static float getMountTimerHudX()     { return cfg.mountTimerHudX; }
    public static float getMountTimerHudY()     { return cfg.mountTimerHudY; }
    public static float getMountTimerHudScale() { return cfg.mountTimerHudScale; }
    public static float getDirectionHudX()     { return cfg.directionHudX; }
    public static float getDirectionHudY()     { return cfg.directionHudY; }
    public static float getDirectionHudScale() { return cfg.directionHudScale; }
    public static float getSplitHudX()         { return cfg.splitHudX; }
    public static float getSplitHudY()         { return cfg.splitHudY; }
    public static float getSplitHudScale()     { return cfg.splitHudScale; }

    public static void setMountTimerHudX(float v)     { cfg.mountTimerHudX     = clamp01(v); }
    public static void setMountTimerHudY(float v)     { cfg.mountTimerHudY     = clamp01(v); }
    public static void setMountTimerHudScale(float v) { cfg.mountTimerHudScale = clampHudScale(v); }
    public static void setDirectionHudX(float v)     { cfg.directionHudX     = clamp01(v); }
    public static void setDirectionHudY(float v)     { cfg.directionHudY     = clamp01(v); }
    public static void setDirectionHudScale(float v) { cfg.directionHudScale = clampHudScale(v); }
    public static void setSplitHudX(float v)         { cfg.splitHudX         = clamp01(v); }
    public static void setSplitHudY(float v)         { cfg.splitHudY         = clamp01(v); }
    public static void setSplitHudScale(float v)     { cfg.splitHudScale     = clampHudScale(v); }
    public static float getPearlTitleHudX()          { return cfg.pearlTitleHudX; }
    public static float getPearlTitleHudY()          { return cfg.pearlTitleHudY; }
    public static float getPearlTitleHudScale()      { return cfg.pearlTitleHudScale; }
    public static void setPearlTitleHudX(float v)    { cfg.pearlTitleHudX    = clamp01(v); }
    public static void setPearlTitleHudY(float v)    { cfg.pearlTitleHudY    = clamp01(v); }
    public static void setPearlTitleHudScale(float v){ cfg.pearlTitleHudScale = clampHudScale(v); }
    public static boolean isSmoothCratePickupEnabled()      { return cfg.smoothCratePickupEnabled; }
    public static void setSmoothCratePickupEnabled(boolean v) { cfg.smoothCratePickupEnabled = v; save(); }
    public static float getSupplyProgressHudX()           { return cfg.supplyProgressHudX; }
    public static float getSupplyProgressHudY()           { return cfg.supplyProgressHudY; }
    public static float getSupplyProgressHudScale()       { return cfg.supplyProgressHudScale; }
    public static void setSupplyProgressHudX(float v)     { cfg.supplyProgressHudX     = clamp01(v); }
    public static void setSupplyProgressHudY(float v)     { cfg.supplyProgressHudY     = clamp01(v); }
    public static void setSupplyProgressHudScale(float v) { cfg.supplyProgressHudScale = clampHudScale(v); }
    public static float getBuildProgressHudX()            { return cfg.buildProgressHudX; }
    public static float getBuildProgressHudY()            { return cfg.buildProgressHudY; }
    public static float getBuildProgressHudScale()        { return cfg.buildProgressHudScale; }
    public static void setBuildProgressHudX(float v)      { cfg.buildProgressHudX      = clamp01(v); }
    public static void setBuildProgressHudY(float v)      { cfg.buildProgressHudY      = clamp01(v); }
    public static void setBuildProgressHudScale(float v)  { cfg.buildProgressHudScale  = clampHudScale(v); }
    public static float getNotificationHudX()             { return cfg.notificationHudX; }
    public static float getNotificationHudY()             { return cfg.notificationHudY; }
    public static float getNotificationHudScale()         { return cfg.notificationHudScale; }
    public static void setNotificationHudX(float v)       { cfg.notificationHudX      = clamp01(v); }
    public static void setNotificationHudY(float v)       { cfg.notificationHudY      = clamp01(v); }
    public static void setNotificationHudScale(float v)   { cfg.notificationHudScale  = clampHudScale(v); }
    public static float getCratePriorityHudX()            { return cfg.cratePriorityHudX; }
    public static float getCratePriorityHudY()            { return cfg.cratePriorityHudY; }
    public static float getCratePriorityHudScale()        { return cfg.cratePriorityHudScale; }
    public static void setCratePriorityHudX(float v)      { cfg.cratePriorityHudX     = clamp01(v); }
    public static void setCratePriorityHudY(float v)      { cfg.cratePriorityHudY     = clamp01(v); }
    public static void setCratePriorityHudScale(float v)  { cfg.cratePriorityHudScale = clampHudScale(v); }
    public static float getTentacleWarnHudX()             { return cfg.tentacleWarnHudX; }
    public static float getTentacleWarnHudY()             { return cfg.tentacleWarnHudY; }
    public static float getTentacleWarnHudScale()         { return cfg.tentacleWarnHudScale; }
    public static void setTentacleWarnHudX(float v)       { cfg.tentacleWarnHudX     = clamp01(v); }
    public static void setTentacleWarnHudY(float v)       { cfg.tentacleWarnHudY     = clamp01(v); }
    public static void setTentacleWarnHudScale(float v)   { cfg.tentacleWarnHudScale = clampHudScale(v); }
    public static boolean isSupplyRecoveryMsgEnabled()      { return cfg.supplyRecoveryMsgEnabled; }
    public static void setSupplyRecoveryMsgEnabled(boolean v){ cfg.supplyRecoveryMsgEnabled = v;  save(); }
    public static boolean isFreshNotifyEnabled()            { return cfg.freshNotifyEnabled; }
    public static void setFreshNotifyEnabled(boolean v)     { cfg.freshNotifyEnabled = v;         save(); }
    public static boolean isBuildStartedNotifyEnabled()     { return cfg.buildStartedNotifyEnabled; }
    public static boolean isBuildStartTimerEnabled()        { return cfg.buildStartTimerEnabled; }
    public static void setBuildStartTimerEnabled(boolean v) { cfg.buildStartTimerEnabled = v; save(); }
    public static void setBuildStartedNotifyEnabled(boolean v){ cfg.buildStartedNotifyEnabled = v; save(); }
    public static boolean isFastDpsNotifyEnabled()          { return cfg.fastDpsNotifyEnabled; }
    public static void setFastDpsNotifyEnabled(boolean v)   { cfg.fastDpsNotifyEnabled = v;       save(); }
    public static boolean isSoloNotifyEnabled()             { return cfg.soloNotifyEnabled; }
    public static void setSoloNotifyEnabled(boolean v)      { cfg.soloNotifyEnabled = v;           save(); }
    public static boolean isNoPreNotifyEnabled()            { return cfg.noPreNotifyEnabled; }
    public static void setNoPreNotifyEnabled(boolean v)     { cfg.noPreNotifyEnabled = v;          save(); }
    public static boolean isSupplyGrabbedNotifyEnabled()    { return cfg.supplyGrabbedNotifyEnabled; }
    public static void setSupplyGrabbedNotifyEnabled(boolean v) { cfg.supplyGrabbedNotifyEnabled = v; save(); }
    public static boolean isSupplyDroppedNotifyEnabled()    { return cfg.supplyDroppedNotifyEnabled; }
    public static void setSupplyDroppedNotifyEnabled(boolean v) { cfg.supplyDroppedNotifyEnabled = v; save(); }
    public static boolean isCratePriorityEnabled()          { return cfg.cratePriorityEnabled; }
    public static void setCratePriorityEnabled(boolean v)   { cfg.cratePriorityEnabled = v;        save(); }
    public static boolean isHideArmorStandsEnabled()              { return cfg.hideArmorStandsEnabled; }
    public static void setHideArmorStandsEnabled(boolean v)       { cfg.hideArmorStandsEnabled = v; save(); }
    public static boolean isHideArmorStandsBuild()                { return cfg.hideArmorStandsBuild; }
    public static void setHideArmorStandsBuild(boolean v)         { cfg.hideArmorStandsBuild = v; save(); }
    public static boolean isHideArmorStandsRightCannon()          { return cfg.hideArmorStandsRightCannon; }
    public static void setHideArmorStandsRightCannon(boolean v)   { cfg.hideArmorStandsRightCannon = v; save(); }
    public static boolean isHideArmorStandsLeftCannon()           { return cfg.hideArmorStandsLeftCannon; }
    public static void setHideArmorStandsLeftCannon(boolean v)    { cfg.hideArmorStandsLeftCannon = v; save(); }
    public static boolean isHideArmorStandsShop()                 { return cfg.hideArmorStandsShop; }
    public static void setHideArmorStandsShop(boolean v)          { cfg.hideArmorStandsShop = v; save(); }
    public static boolean isHideArmorStandsOthers()               { return cfg.hideArmorStandsOthers; }
    public static void setHideArmorStandsOthers(boolean v)        { cfg.hideArmorStandsOthers = v; save(); }
    public static boolean isCannonHitboxesEnabled()               { return cfg.cannonHitboxesEnabled; }
    public static void setCannonHitboxesEnabled(boolean v)        { cfg.cannonHitboxesEnabled = v; save(); }
    public static boolean isCannonHitboxesLeftEnabled()           { return cfg.cannonHitboxesLeftEnabled; }
    public static void setCannonHitboxesLeftEnabled(boolean v)    { cfg.cannonHitboxesLeftEnabled = v; save(); }
    public static boolean isCannonHitboxesRightEnabled()          { return cfg.cannonHitboxesRightEnabled; }
    public static void setCannonHitboxesRightEnabled(boolean v)   { cfg.cannonHitboxesRightEnabled = v; save(); }
    public static boolean isKuudraHpHudEnabled()            { return cfg.kuudraHpHudEnabled; }
    public static void setKuudraHpHudEnabled(boolean v)     { cfg.kuudraHpHudEnabled = v; save(); }
    public static boolean isKuudraHpShowRaw()               { return cfg.kuudraHpShowRaw; }
    public static void setKuudraHpShowRaw(boolean v)        { cfg.kuudraHpShowRaw = v; save(); }
    public static boolean isKuudraHpHideBar()               { return cfg.kuudraHpHideBar; }
    public static void setKuudraHpHideBar(boolean v)        { cfg.kuudraHpHideBar = v; save(); }
    public static boolean isHollowWandEnabled()             { return cfg.hollowWandEnabled; }
    public static void setHollowWandEnabled(boolean v)      { cfg.hollowWandEnabled = v; save(); }
    public static boolean isKickedNotificationEnabled()     { return cfg.kickedNotificationEnabled; }
    public static void setKickedNotificationEnabled(boolean v) { cfg.kickedNotificationEnabled = v; save(); }
    public static boolean isHideBossBarEnabled()              { return cfg.hideBossBarEnabled; }
    public static boolean isHideBossBarOnlyInKuudra()         { return cfg.hideBossBarOnlyInKuudra; }
    public static boolean isHideFallingBlocksEnabled()        { return cfg.hideFallingBlocksEnabled; }
    public static void setHideBossBarEnabled(boolean v)       { cfg.hideBossBarEnabled = v; save(); }
    public static void setHideBossBarOnlyInKuudra(boolean v)  { cfg.hideBossBarOnlyInKuudra = v; save(); }
    public static void setHideFallingBlocksEnabled(boolean v) { cfg.hideFallingBlocksEnabled = v; save(); }
    public static boolean isHideEntityFireEnabled()           { return cfg.hideEntityFireEnabled; }
    public static void setHideEntityFireEnabled(boolean v)    { cfg.hideEntityFireEnabled = v; save(); }
    public static boolean isLavaBobberFixEnabled()             { return cfg.lavaBobberFixEnabled; }
    public static void setLavaBobberFixEnabled(boolean v)      { cfg.lavaBobberFixEnabled = v; save(); }
    public static boolean isLegacyRodPhysicsEnabled()          { return cfg.legacyRodPhysicsEnabled; }
    public static void setLegacyRodPhysicsEnabled(boolean v)   { cfg.legacyRodPhysicsEnabled = v; save(); }
    public static boolean isHideDamageTitleEnabled()           { return cfg.hideDamageTitleEnabled; }
    public static void setHideDamageTitleEnabled(boolean v)    { cfg.hideDamageTitleEnabled = v; save(); }
    public static boolean isHideDeadEntitiesEnabled()          { return cfg.hideDeadEntitiesEnabled; }
    public static void setHideDeadEntitiesEnabled(boolean v)   { cfg.hideDeadEntitiesEnabled = v; save(); }
    public static boolean isEtherwarpWaypointsEnabled()        { return cfg.etherwarpWaypointsEnabled; }
    public static void setEtherwarpWaypointsEnabled(boolean v) { cfg.etherwarpWaypointsEnabled = v; save(); }
    public static float getSelfPlayerScale()                  { return cfg.selfPlayerScale; }
    public static void setSelfPlayerScale(float v)            { cfg.selfPlayerScale = Math.max(1f, Math.min(300f, v)); save(); }
    public static float getOtherPlayerScale()                 { return cfg.otherPlayerScale; }
    public static void setOtherPlayerScale(float v)           { cfg.otherPlayerScale = Math.max(1f, Math.min(300f, v)); save(); }
    public static float getKuudraSizeScale()                  { return cfg.kuudraSizeScale; }
    public static void setKuudraSizeScale(float v)            { cfg.kuudraSizeScale = Math.max(1f, Math.min(200f, v)); save(); }
    public static float getKuudraHpHudX()                   { return cfg.kuudraHpHudX; }
    public static float getKuudraHpHudY()                   { return cfg.kuudraHpHudY; }
    public static float getKuudraHpHudScale()               { return cfg.kuudraHpHudScale; }
    public static void setKuudraHpHudX(float v)             { cfg.kuudraHpHudX = clamp01(v); }
    public static void setKuudraHpHudY(float v)             { cfg.kuudraHpHudY = clamp01(v); }
    public static void setKuudraHpHudScale(float v)         { cfg.kuudraHpHudScale = clampHudScale(v); }

    // Wardrobe keybinds
    public static boolean isWardrobeEnabled()         { return cfg.wardrobeEnabled; }
    public static void setWardrobeEnabled(boolean v)  { cfg.wardrobeEnabled = v; save(); }
    public static boolean isLoadoutsEnabled()         { return cfg.loadoutsEnabled; }
    public static void setLoadoutsEnabled(boolean v)  { cfg.loadoutsEnabled = v; save(); }
    public static int  getOpenGuiKey()                { return cfg.openGuiKey; }
    public static int  getWardrobeOpenKey()           { return cfg.wardrobeOpenKey; }
    public static void setOpenGuiKey(int v)           { cfg.openGuiKey = v; save(); }
    public static void setWardrobeOpenKey(int v)      { cfg.wardrobeOpenKey = v; save(); }
    public static int  getStatsOpenKey()              { return cfg.statsOpenKey; }
    public static void setStatsOpenKey(int v)         { cfg.statsOpenKey = v; save(); }
    public static int  getPetsOpenKey()               { return cfg.petsOpenKey; }
    public static void setPetsOpenKey(int v)          { cfg.petsOpenKey = v; save(); }
    public static int  getEqWardrobeOpenKey()         { return cfg.eqWardrobeOpenKey; }
    public static void setEqWardrobeOpenKey(int v)    { cfg.eqWardrobeOpenKey = v; save(); }
    public static int  getLoadoutsOpenKey()           { return cfg.loadoutsOpenKey; }
    public static void setLoadoutsOpenKey(int v)      { cfg.loadoutsOpenKey = v; save(); }
    public static int[] getWardrobeSlotKeys()         { return cfg.wardrobeSlotKeys.clone(); }
    public static void setWardrobeSlotKey(int i, int v){ cfg.wardrobeSlotKeys[i] = v; save(); }
    public static int[] getLoadoutSlotKeys()          { return cfg.loadoutSlotKeys.clone(); }
    public static void setLoadoutSlotKey(int i, int v) { cfg.loadoutSlotKeys[i] = v; save(); }
    public static int  getWardrobeNextPageKey()       { return cfg.wardrobeNextPageKey; }
    public static void setWardrobeNextPageKey(int v)  { cfg.wardrobeNextPageKey = v; save(); }
    public static int  getWardrobePrevPageKey()       { return cfg.wardrobePrevPageKey; }
    public static void setWardrobePrevPageKey(int v)  { cfg.wardrobePrevPageKey = v; save(); }
    public static int  getWardrobeUnequipKey()        { return cfg.wardrobeUnequipKey; }
    public static void setWardrobeUnequipKey(int v)   { cfg.wardrobeUnequipKey = v; save(); }
    public static boolean isWardrobeDisableUnequipEnabled()      { return cfg.wardrobeDisableUnequipEnabled; }
    public static void setWardrobeDisableUnequipEnabled(boolean v) { cfg.wardrobeDisableUnequipEnabled = v; save(); }
    public static boolean isWardrobeAutoCloseEnabled()           { return cfg.wardrobeAutoCloseEnabled; }
    public static void setWardrobeAutoCloseEnabled(boolean v)    { cfg.wardrobeAutoCloseEnabled = v; save(); }
    public static int     getWardrobeExtraAutoCloseMs()          { return cfg.wardrobeExtraAutoCloseMs; }
    public static void setWardrobeExtraAutoCloseMs(int v)        { cfg.wardrobeExtraAutoCloseMs = Math.max(0, v); save(); }
    public static int  getLastEquippedWardrobeSlot()          { return cfg.lastEquippedWardrobeSlot; }
    public static void setLastEquippedWardrobeSlot(int v)     { cfg.lastEquippedWardrobeSlot = v; save(); }

    // Dungeons
    public static void setAutoGfsToxic(boolean v)    { cfg.autoGfsToxic = v;    save(); }
    public static void setAutoGfsTwilight(boolean v) { cfg.autoGfsTwilight = v; save(); }
    public static void setToxicAmount(int v)         { cfg.toxicAmount = v;     save(); }
    public static void setTwilightAmount(int v)      { cfg.twilightAmount = v;  save(); }

    // Chest tracker
    public static void setChestCounts(int t, int s, int f) { cfg.chestTotal = t; cfg.chestSuccess = s; cfg.chestFail = f; save(); }
    public static void resetChestCounts()                   { cfg.chestTotal = 0; cfg.chestSuccess = 0; cfg.chestFail = 0; save(); }

    // Split timer
    public static void setSplitTimerEnabled(boolean v)  { cfg.splitTimerEnabled = v;  save(); }
    public static void setSupplyTimesEnabled(boolean v) { cfg.supplyTimesEnabled = v; save(); }
    public static void setRunBreakdownEnabled(boolean v) { cfg.runBreakdownEnabled = v; save(); }
    public static void setShitterHighlightEnabled(boolean v) { cfg.shitterHighlightEnabled = v; save(); }

    // Item customisation
    public static void setItemCustomizationEnabled(boolean v) { cfg.itemCustomizationEnabled = v; save(); }

    // ── Load / Save ───────────────────────────────────────────────────────────

    private static boolean firstRun;

    private static void resetKeysIfNumberedDifferently(Data d) {
        int saved = d.keyScheme == 0 ? 1 : d.keyScheme;
        if (saved != KEY_SCHEME) {
            Data fresh = new Data();
            d.shopMainKey = fresh.shopMainKey;
            d.shopCannonKey = fresh.shopCannonKey;
            d.slotBindSetKey = fresh.slotBindSetKey;
            d.openGuiKey = fresh.openGuiKey;
            d.wardrobeOpenKey = fresh.wardrobeOpenKey;
            d.statsOpenKey = fresh.statsOpenKey;
            d.petsOpenKey = fresh.petsOpenKey;
            d.eqWardrobeOpenKey = fresh.eqWardrobeOpenKey;
            d.loadoutsOpenKey = fresh.loadoutsOpenKey;
            d.wardrobeSlotKeys = fresh.wardrobeSlotKeys;
            d.loadoutSlotKeys = fresh.loadoutSlotKeys;
            d.wardrobeNextPageKey = fresh.wardrobeNextPageKey;
            d.wardrobePrevPageKey = fresh.wardrobePrevPageKey;
            d.wardrobeUnequipKey = fresh.wardrobeUnequipKey;
            com.phantomaddons.PhantomAddons.LOGGER.info("[PhantomAddons] The saved key bindings were made for another Minecraft version; they were reset.");
        }
        d.keyScheme = KEY_SCHEME;
    }

    public static void load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) { firstRun = true; return; }
        try (Reader r = new FileReader(file)) {
            Data d = GSON.fromJson(r, Data.class);
            if (d == null) return;
            Data cur = cfg;

            roleMode               = safeEnum(RoleMode.class, d.roleMode, RoleMode.AUTO);
            lavaMode               = safeEnum(LavaMode.class, d.lavaMode, LavaMode.DEFAULT);
            waypointType           = safeEnum(WaypointType.class, d.waypointType, WaypointType.CIRCLE);
            secondSupplyPreference = safeEnum(SecondSupplyPreference.class, d.secondSupplyPreference, SecondSupplyPreference.DOUBLE_PEARL);
            kuudraTalisman         = safeEnum(KuudraTalisman.class, d.kuudraTalisman, KuudraTalisman.NONE);
            uiTheme = d.uiTheme != null
                    ? safeEnum(UiTheme.class, d.uiTheme, UiTheme.DARK)
                    : (d.uiDarkMode ? UiTheme.DARK : UiTheme.LIGHT);
            if (d.kuudraPetRarity != null) {
                try { kuudraPetRarity = KuudraPetRarity.valueOf(d.kuudraPetRarity); }
                catch (IllegalArgumentException ignored) {}
            }

            d.dpsRefillAmount         = Math.max(4, d.dpsRefillAmount);
            d.autoGfsDisableHpPercent = d.autoGfsDisableHpPercent >= 25 ? Math.min(100, d.autoGfsDisableHpPercent) : 40;
            d.lavaOpacity             = clamp01(d.lavaOpacity);
            d.explosionHideRadius     = clamp01(d.explosionHideRadius);
            d.explosionSizeMultiplier = clamp01(d.explosionSizeMultiplier);
            d.uiGuiScale = d.uiGuiScale <= 0f ? 1.0f : Math.max(0.7f, Math.min(1.6f, d.uiGuiScale));
            d.pvGuiScale     = d.pvGuiScale     <= 0f ? 1.0f : Math.max(0.6f, Math.min(2.5f, d.pvGuiScale));
            d.pvElementScale = d.pvElementScale <= 0f ? 1.0f : Math.max(0.6f, Math.min(1.6f, d.pvElementScale));

            slotBindings.clear();
            if (d.slotBindPairs != null) {
                for (int[] pair : d.slotBindPairs)
                    if (pair != null && pair.length == 2) slotBindings.put(pair[0], pair[1]);
            }

            d.doublePearlDelayS = Math.max(0.05f, Math.min(0.55f, d.doublePearlDelayS));
            d.pearlTimerHeight  = clamp01(d.pearlTimerHeight);
            d.pearlTimerSize    = clamp01(d.pearlTimerSize);
            d.pearlCircleSize   = clamp01(d.pearlCircleSize);
            resetKeysIfNumberedDifferently(d);
            d.lowPing           = Math.max(0, d.lowPing);
            d.waypointFillAlpha = clamp01(d.waypointFillAlpha);
            d.beaconAlpha       = clamp01(d.beaconAlpha);
            d.wpColNormal      &= 0xFFFFFF;
            d.wpColCorrect     &= 0xFFFFFF;
            d.wpColHovered     &= 0xFFFFFF;
            d.wpColReady       &= 0xFFFFFF;
            d.beaconColNormal  &= 0xFFFFFF;
            d.beaconColCorrect &= 0xFFFFFF;
            d.buildBeaconAlpha  = clamp01(d.buildBeaconAlpha);
            d.stunReminderTravelMs   = Math.max(0, Math.min(20000, d.stunReminderTravelMs));
            d.stunReminderMaxPercent = Math.max(1, Math.min(100, d.stunReminderMaxPercent));
            d.elleOutlineColor &= 0xFFFFFF;
            d.elleBeaconColor  &= 0xFFFFFF;
            if (d.kuudraApiKey == null) d.kuudraApiKey = "";
            if (d.stunPreviewLeftEnabled  == null) d.stunPreviewLeftEnabled  = false;
            if (d.stunPreviewRightEnabled == null) d.stunPreviewRightEnabled = d.stunPreviewEnabled;
            if (d.stunPreviewBackEnabled  == null) d.stunPreviewBackEnabled  = false;
            d.supplyBeaconsEnabled          |= d.supplyWaypointsEnabled;
            d.supplyLocationAnnounceEnabled |= d.supplyWaypointsEnabled;
            d.giantHitboxFillOpacity        = clamp01(d.giantHitboxFillOpacity);
            d.tentacleGrabWarningMinConfidence = clamp01(d.tentacleGrabWarningMinConfidence);
            d.giantHitboxColor             &= 0xFFFFFF;
            d.boneTimingAssistOffset        = Math.clamp(d.boneTimingAssistOffset, -3.0f, 1.0f);
            d.boneTimingHitboxOutlineColor &= 0xFFFFFF;

            if (d.loadoutsEnabled == null) d.loadoutsEnabled = d.wardrobeEnabled;
            if (d.wardrobeSlotKeys == null || d.wardrobeSlotKeys.length != 9)
                d.wardrobeSlotKeys = cur.wardrobeSlotKeys;
            if (d.loadoutSlotKeys == null || d.loadoutSlotKeys.length != 12)
                d.loadoutSlotKeys = cur.loadoutSlotKeys;
            d.wardrobeExtraAutoCloseMs = Math.max(0, d.wardrobeExtraAutoCloseMs);

            double[][] splitPbs = cur.splitPbs;
            if (d.splitPbs != null && d.splitPbs.length == 6)
                for (int i = 1; i <= 5; i++)
                    if (d.splitPbs[i] != null && d.splitPbs[i].length == 7)
                        splitPbs[i] = d.splitPbs[i].clone();
            d.splitPbs = splitPbs;
            d.totalRunPbs     = d.totalRunPbs     != null && d.totalRunPbs.length     == 6 ? d.totalRunPbs.clone()     : cur.totalRunPbs;
            d.totalRunTimeSum = d.totalRunTimeSum != null && d.totalRunTimeSum.length == 6 ? d.totalRunTimeSum.clone() : cur.totalRunTimeSum;
            d.totalRunCount   = d.totalRunCount   != null && d.totalRunCount.length   == 6 ? d.totalRunCount.clone()   : cur.totalRunCount;
            if (d.pbRecords != null && d.pbRecords.length == 6) {
                for (int t = 1; t <= 5; t++)
                    if (d.pbRecords[t] != null) d.totalRunPbs[t] = d.pbRecords[t].totalTime;
            } else {
                d.pbRecords = cur.pbRecords;
                for (int t = 1; t <= 5; t++)
                    if (d.totalRunPbs[t] < 9999) {
                        d.pbRecords[t] = new PbRecord();
                        d.pbRecords[t].totalTime = d.totalRunPbs[t];
                        if (d.splitPbs[t] != null) d.pbRecords[t].splits = d.splitPbs[t].clone();
                    }
            }

            d.kuudraPetLevel   = d.kuudraPetLevel > 0 ? Math.clamp(d.kuudraPetLevel, 1, 100) : cur.kuudraPetLevel;
            d.essenceOfCrimson = Math.clamp(d.essenceOfCrimson, 0, 10);
            d.echoOfEssence    = Math.clamp(d.echoOfEssence, 0, 10);

            cfg = d;

            ItemCustomization.loadFrom(d.itemCategorySettings, d.itemCustomCategories);

            if (d.notificationSounds != null) {
                notificationSounds.clear();
                notificationSounds.putAll(d.notificationSounds);
            }

        } catch (IOException e) { PhantomAddons.LOGGER.error("Failed to load config", e); }
    }

    public static void save() {
        Data d = cfg.clone();
        d.keyScheme              = KEY_SCHEME;

        d.roleMode               = roleMode.name();
        d.lavaMode               = lavaMode.name();
        d.uiTheme                = uiTheme.name();
        d.uiDarkMode             = uiTheme == UiTheme.DARK;
        d.waypointType           = waypointType.name();
        d.secondSupplyPreference = secondSupplyPreference.name();
        d.kuudraTalisman         = kuudraTalisman.name();
        d.kuudraPetRarity        = kuudraPetRarity.name();

        d.slotBindPairs = slotBindings.entrySet().stream()
                .map(e -> new int[]{e.getKey(), e.getValue()})
                .toArray(int[][]::new);
        d.wardrobeSlotKeys = cfg.wardrobeSlotKeys.clone();
        d.loadoutSlotKeys  = cfg.loadoutSlotKeys.clone();

        d.itemCategorySettings = ItemCustomization.serialiseBuiltin();
        d.itemCustomCategories = ItemCustomization.serialiseCustom();
        d.notificationSounds   = new LinkedHashMap<>(notificationSounds);

        pendingWrite = d;
        lastSaveRequestMs = System.currentTimeMillis();
        startWriterThread();
    }

    private static final long SAVE_DEBOUNCE_MS = 400L;

    private static volatile Data pendingWrite      = null;
    private static volatile long lastSaveRequestMs = 0L;
    private static volatile boolean writerStarted  = false;

    private static synchronized void startWriterThread() {
        if (writerStarted) return;
        writerStarted = true;

        Thread writer = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (pendingWrite == null) continue;
                if (System.currentTimeMillis() - lastSaveRequestMs < SAVE_DEBOUNCE_MS) continue;
                flushPending();
            }
        }, "PhantomAddons-ConfigWriter");
        writer.setDaemon(true);
        writer.start();

        Runtime.getRuntime().addShutdownHook(
                new Thread(PhantomConfig::flushPending, "PhantomAddons-ConfigFlush"));
    }

    private static synchronized void flushPending() {
        Data d = pendingWrite;
        if (d == null) return;
        pendingWrite = null;
        try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(d, w);
        } catch (IOException e) { PhantomAddons.LOGGER.error("Failed to save config", e); }
    }

    public static void saveNow() {
        flushPending();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static float clampHudScale(float v) { return Math.max(0.2f, Math.min(3f, v)); }

    private static <T extends Enum<T>> T safeEnum(Class<T> type, String val, T fallback) {
        try { return Enum.valueOf(type, val); } catch (Exception e) { return fallback; }
    }

    private static void rebuildChunks() {
        Minecraft mc = Minecraft.getInstance();
        //? if <26.2 {
        /*if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
        *///?} else {
        if (mc.levelRenderer != null && mc.level != null && mc.gameRenderer != null) {
            mc.levelRenderer.invalidateCompiledGeometry(mc.level, mc.options, mc.gameRenderer.mainCamera(), mc.getBlockColors());
        }
        //?}
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
    public static final String SOUND_BONE_THROW_NOW  = "bone_throw_now";

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

    private static final LinkedHashMap<String, NotificationSound> notificationSounds
            = new LinkedHashMap<>();

    private static NotificationSound defaultSound(String key) {
        if (SOUND_PEARL_NOW.equals(key))
            return new NotificationSound("minecraft:block.note_block.pling", 2.0f);
        if (SOUND_FAST_DPS.equals(key))
            return new NotificationSound("minecraft:entity.experience_orb.pickup", 2.0f);
        if (SOUND_WARDROBE_SWAP.equals(key))
            return new NotificationSound("minecraft:ui.button.click", 1.5f);
        if (SOUND_BACKBONE_DONE.equals(key))
            // Deliberately a different clip (not just a different pitch of note_block.pling) from
            // SOUND_BONE_THROW_NOW — both firing close together with the identical sound event
            // caused Minecraft's sound engine to cut one off / restart it, sounding like stutter.
            return new NotificationSound("minecraft:entity.player.levelup", 1.0f);
        if (SOUND_BONE_THROW_NOW.equals(key))
            return new NotificationSound("minecraft:block.note_block.pling", 2.0f);
        return new NotificationSound("minecraft:entity.experience_orb.pickup", 1.0f);
    }

    public static NotificationSound getNotificationSound(String key) {
        return notificationSounds.computeIfAbsent(key, PhantomConfig::defaultSound);
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

    private static class Data implements Cloneable {
        String  roleMode           = "AUTO";
        int     dpsValue           = 32;
        int     dpsRefillAmount    = 10;
        int     autoGfsDisableHpPercent = 40;
        int     stunValue          = 9;
        boolean eatenTimerEnabled             = false;
        boolean eatenTimerSubtractPingEnabled = false;
        boolean pickoblockEnabled             = false;
        boolean etherwarpLavaBlock = true;
        boolean etherwarpLavaBlockOnlyInKuudra = false;
        boolean hideElleDialogue   = false;
        boolean autoGfsEnabled     = true;

        boolean lavaTweaksEnabled = true;
        String  lavaMode          = "DEFAULT";
        float   lavaOpacity       = 1f;
        int     lavaColor         = 0xFFAA0000;
        boolean lavaAsWater       = false;
        boolean lavaColorOverride = false;

        boolean fastDpsWarning          = true;
        boolean chestTrackerVisible     = true;
        boolean soloDetectorEnabled     = true;
        boolean cannonAutoClose         = false;
        boolean shopKeybindsEnabled     = false;
        boolean middleClickShopGuiEnabled = false;
        int     shopMainKey             = InputConstants.KEY_1;
        int     shopCannonKey           = InputConstants.KEY_2;
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
        boolean shareSupplyData = false;
        boolean changelogEnabled = true;
        String lastChangelogVersion = "";
        String skippedUpdateVersion = "";
        boolean tentacleHighlightEnabled = false;
        boolean tentacleSpotBeaconsEnabled = false;
        boolean uiDarkMode = true;
        String  uiTheme = null;
        boolean uiTransparencyEnabled = true;
        float   uiGuiScale = 1.0f;
        boolean tentacleReachEnabled = false;
        boolean tentacleReachGround = false;
        boolean tentacleReachLava = false;
        float   tentacleReachOpacity = 0.10f;
        boolean pvEnabled      = false;
        boolean pvPromptAnswered = false;
        boolean pvOpened       = false;
        float   pvGuiScale     = 1.0f;
        String  pvContent      = "MEDIUM";
        float   pvElementScale = 1.0f;
        String  pvTheme        = "MATCH";
        boolean pvArabicNumerals = false;
        boolean pvOverflowSkills = false;
        boolean pvRememberPage = false;
        boolean pvNameSuggestions = true;
        boolean caseInsensitiveCommands = false;
        boolean shopMenuGui = false;
        boolean donatorNamesEnabled = true;
        boolean pvTabIcons = true;
        boolean pvPartyFinderPreview = false;
        boolean pvClickNames = false;
        boolean autoSprintEnabled       = false;
        boolean slotBindsEnabled        = false;
        int     slotBindSetKey          = -1;
        boolean slotBindsShowOnHover    = true;
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
        int     elleOutlineColor      = 0xFF0000;
        int     elleBeaconColor       = 0x00FFFF;
        String  kuudraApiKey               = ""; // legacy: kept in the file, no longer used or shown
        boolean blockSlot9Enabled          = false;
        boolean blockAtomsplitEnabled      = false;
        boolean ichorRadiusEnabled         = false;
        boolean rightPeakBlockHighlightEnabled = false;
        boolean stunPreviewEnabled         = false;
        Boolean stunPreviewLeftEnabled     = null; // null = not yet saved under its own key; see load()
        Boolean stunPreviewRightEnabled    = null;
        Boolean stunPreviewBackEnabled     = null;
        boolean dpsWaypointEnabled    = false;
        boolean buildBeaconsEnabled        = false;
        boolean elleHighlightEnabled       = false;
        boolean elleHighlightBeaconEnabled = true;
        boolean rendDamageEnabled          = false;
        boolean rendPullAttributionEnabled = true;
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
        boolean supplySpawnCountdownEnabled  = true;
        boolean buildProgressHudEnabled      = false;
        boolean stunReminderEnabled          = false;
        String  stunReminderRoleMode         = "AUTO";
        int     stunReminderTravelMs         = 0;
        int     stunReminderMaxPercent       = 94;
        float   stunReminderHudX             = 0.5f;
        float   stunReminderHudY             = 0.25f;
        float   stunReminderHudScale         = 1.0f;
        boolean announceFreshEnabled         = false;
        boolean supplyHitboxEnabled          = false;
        boolean supplyRodRadiusEnabled       = false;
        boolean supplyPearlHitboxEnabled     = false;
        boolean pearlRefillEnabled           = false;
        boolean pearlRefillOutsideKuudraEnabled = false;
        boolean pearlRefillDungeonsEnabled   = false;
        boolean hideSelfieEnabled            = false;
        boolean preventPlacingPlayerHeadsEnabled      = false;
        boolean preventPlacingPlayerHeadsExceptGarden = true;
        boolean preventPlacingWeaponsEnabled          = false;
        boolean blockBossPhaseJumpEnabled             = false;
        boolean blockCloseItemEnabled                 = false;
        boolean supplyGiantHitboxEnabled              = false;
        boolean giantHitboxEnabled                    = false;
        boolean tentacleGrabWarningEnabled            = false;
        float   tentacleGrabWarningMinConfidence      = 0.5f;
        boolean tentacleGrabLineEnabled               = true;
        boolean tentacleGrabHudEnabled                = false;
        boolean giantHitboxFilled                     = false;
        float   giantHitboxFillOpacity                = 0.05f;
        int     giantHitboxColor                      = 0xFFFFFF;
        boolean kuudraDirectionEnabled  = false;        boolean tuxedoWarningEnabled    = true;
        boolean boneTimingAssistEnabled = false;
        float   boneTimingAssistOffset = 0f;
        boolean boneTimingHitboxOutlineEnabled = false;
        boolean boneTimingHitboxOutlineFilled  = true;
        int     boneTimingHitboxOutlineColor   = 0x00FFFF;
        boolean boneTimingHitboxOutlineOnlyCurrentDirection = false;
        boolean boneTimingHitboxHighlightInRange = false;

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
        float tentacleWarnHudX       = 0.5f;
        float tentacleWarnHudY       = 0.35f;
        float tentacleWarnHudScale   = 3.0f;
        boolean supplyRecoveryMsgEnabled = false;
        boolean freshNotifyEnabled       = false;
        boolean buildStartedNotifyEnabled= false;
        boolean buildStartTimerEnabled = true;
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
        boolean cannonHitboxesEnabled      = true;
        boolean cannonHitboxesLeftEnabled  = true;
        boolean cannonHitboxesRightEnabled = false;
        boolean kuudraHpHudEnabled       = false;
        boolean kuudraHpShowRaw          = false;
        boolean kuudraHpHideBar          = false;
        boolean hollowWandEnabled           = false;
        boolean kickedNotificationEnabled   = false;
        boolean hideBossBarEnabled         = false;
        boolean hideBossBarOnlyInKuudra    = false;
        boolean hideFallingBlocksEnabled  = false;
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
        Boolean loadoutsEnabled     = null; // null = not yet saved under its own key; see load()
        int     openGuiKey          = -1;
        int     wardrobeOpenKey     = -1;
        int     statsOpenKey        = -1;
        int     petsOpenKey         = -1;
        int     eqWardrobeOpenKey   = -1;
        int     loadoutsOpenKey     = -1;
        int[]   wardrobeSlotKeys    = {InputConstants.KEY_1, InputConstants.KEY_2, InputConstants.KEY_3, InputConstants.KEY_4, InputConstants.KEY_5,
                                       InputConstants.KEY_6, InputConstants.KEY_7, InputConstants.KEY_8, InputConstants.KEY_9};
        int[]   loadoutSlotKeys     = {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
        int     wardrobeNextPageKey = InputConstants.KEY_RIGHT;
        int     wardrobePrevPageKey = InputConstants.KEY_LEFT;
        int     wardrobeUnequipKey  = InputConstants.KEY_U;
        int     keyScheme           = 0;
        boolean wardrobeDisableUnequipEnabled = true;
        boolean wardrobeAutoCloseEnabled      = true;
        int     wardrobeExtraAutoCloseMs      = 0;
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
        boolean highlightUnopenedChestsEnabled  = true;
        boolean profitRerollCalc       = true;
        boolean blockExpensiveReroll   = true;
        boolean profitBazaarInstaSell  = true;
        boolean profitBazaarInstaBuy   = false;
        float   profitHudX             = 0.01f;
        float   profitHudY             = 0.5f;
        float   profitHudScale         = 1.0f;
        String  kuudraPetRarity        = "LEGENDARY";
        int     kuudraPetLevel         = 100;
        int     essenceOfCrimson       = 10;
        int     echoOfEssence          = 10;

        boolean chestValueGuiEnabled   = true;
        float   chestValueHudX         = 0.3f;
        float   chestValueHudY         = 0.3f;
        float   chestValueHudScale     = 1.0f;

        float   chestTrackerHudX       = 0.556f;
        float   chestTrackerHudY       = 0.958f;
        float   chestTrackerHudScale   = 1.0f;

        boolean   splitTimerEnabled  = true;
        boolean   supplyTimesEnabled = true;
        boolean   runBreakdownEnabled = true;
        boolean   shitterHighlightEnabled = true;
        double[][] splitPbs          = null;
        double[]   totalRunPbs       = null;
        double[]   totalRunTimeSum   = null;
        int[]      totalRunCount     = null;
        PbRecord[] pbRecords         = null;

        boolean                                          itemCustomizationEnabled = true;
        Map<String, ItemTransformSettings>               itemCategorySettings     = null;
        List<ItemCustomization.CustomCategory> itemCustomCategories     = null;
        LinkedHashMap<String, NotificationSound> notificationSounds     = null;

        @Override protected Data clone() {
            try { return (Data) super.clone(); }
            catch (CloneNotSupportedException e) { throw new AssertionError(e); }
        }
    }
}
