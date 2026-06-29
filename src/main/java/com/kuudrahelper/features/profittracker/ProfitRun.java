package com.kuudrahelper.features.profittracker;

public class ProfitRun {
    public long timestamp;      // System.currentTimeMillis() when the chest was collected
    public long durationMs;     // run duration (SUPPLIES → END phase), 0 if not tracked
    public long itemsValue;     // AH value of armor/weapons (or 0 if using salvage mode)
    public long attributeValue; // AH value of attribute shards
    public long essenceValue;   // coin value of Crimson Essence (from drops + salvaged armor)
    public long keyCost;        // coin cost of key(s) used this run
    public long kismetCost;     // coin cost of Kismet Feathers used this run (negative on HUD)

    public ProfitRun() {}

    public long totalGains()    { return itemsValue + attributeValue + essenceValue; }
    public long totalExpenses() { return keyCost + kismetCost; }
    public long profit()        { return totalGains() - totalExpenses(); }
}
