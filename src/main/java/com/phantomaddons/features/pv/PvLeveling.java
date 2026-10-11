package com.phantomaddons.features.pv;

public final class PvLeveling {

    private PvLeveling() {}

    static final long[] SKILL = {
            50, 125, 200, 300, 500, 750, 1000, 1500, 2000, 3500,
            5000, 7500, 10000, 15000, 20000, 30000, 50000, 75000, 100000, 200000,
            300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000, 1100000, 1200000,
            1300000, 1400000, 1500000, 1600000, 1700000, 1800000, 1900000, 2000000, 2100000, 2200000,
            2300000, 2400000, 2500000, 2600000, 2750000, 2900000, 3100000, 3400000, 3700000, 4000000,
            4300000, 4600000, 4900000, 5200000, 5500000, 5800000, 6100000, 6400000, 6700000, 7000000,
    };

    static final long[] RUNECRAFTING = {
            50, 100, 125, 160, 200, 250, 315, 400, 500, 625,
            785, 1000, 1250, 1600, 2000, 2465, 3125, 4000, 5000, 6200,
            7800, 9800, 12200, 15300, 19050,
    };

    static final long[] SOCIAL = {
            50, 100, 150, 250, 500, 750, 1000, 1250, 1500, 2000,
            2500, 3000, 3750, 4500, 6000, 8000, 10000, 12500, 15000, 20000,
            25000, 30000, 35000, 40000, 50000,
    };

    static final long[] PET = {
            100, 110, 120, 130, 145, 160, 175, 190, 210, 230,
            250, 275, 300, 330, 360, 400, 440, 490, 540, 600,
            660, 730, 800, 880, 960, 1050, 1150, 1260, 1380, 1510,
            1650, 1800, 1960, 2130, 2310, 2500, 2700, 2920, 3160, 3420,
            3700, 4000, 4350, 4750, 5200, 5700, 6300, 7000, 7800, 8700,
            9700, 10800, 12000, 13300, 14700, 16200, 17800, 19500, 21300, 23200,
            25200, 27400, 29800, 32400, 35200, 38200, 41400, 44800, 48400, 52200,
            56200, 60400, 64800, 69400, 74200, 79200, 84700, 90700, 97200, 104200,
            111700, 119700, 128200, 137200, 146700, 156700, 167700, 179700, 192700, 206700,
            221700, 237700, 254700, 272700, 291700, 311700, 333700, 357700, 383700, 411700,
            441700, 476700, 516700, 561700, 611700, 666700, 726700, 791700, 861700, 936700,
            1016700, 1101700, 1191700, 1286700, 1386700, 1496700, 1616700, 1746700, 1886700,
    };

    public record Level(int level, int cap, double progress, double into, double needed) {
        public boolean maxed() { return level >= cap; }
    }

    static Level compute(double xp, long[] costs, int offset, int startLevel, int cap) {
        int level = startLevel;
        double remaining = xp;
        int i = offset;
        while (level < cap && i < costs.length && remaining >= costs[i]) {
            remaining -= costs[i];
            level++;
            i++;
        }
        if (level >= cap || i >= costs.length) return new Level(Math.min(level, cap), cap, 1, remaining, 0);
        return new Level(level, cap, remaining / costs[i], remaining, costs[i]);
    }

    static Level skill(double xp, long[] table, int cap) {
        return compute(xp, table, 0, 0, Math.min(cap, table.length));
    }

    static Level overflow(double xp) {
        double remaining = xp;
        int level = 0;
        while (level < SKILL.length && remaining >= SKILL[level]) {
            remaining -= SKILL[level];
            level++;
        }
        if (level < SKILL.length) return new Level(level, Integer.MAX_VALUE, remaining / SKILL[level], remaining, SKILL[level]);
        long slope = 600_000L, next = 7_000_000L + slope;
        while (remaining >= next) {
            level++;
            remaining -= next;
            next += slope;
            if (level % 10 == 0) slope *= 2;
        }
        return new Level(level, Integer.MAX_VALUE, remaining / next, remaining, next);
    }

    static int rarityOffset(String rarity) {
        return switch (rarity) {
            case "UNCOMMON" -> 6;
            case "RARE" -> 11;
            case "EPIC" -> 16;
            case "LEGENDARY", "MYTHIC" -> 20;
            default -> 0;
        };
    }

    static Level pet(double xp, String rarity, int maxLevel) {
        Level base = compute(xp, PET, rarityOffset(rarity), 1, 100);
        if (maxLevel <= 100 || base.level() < 100) return base;
        double spent = 0;
        for (int i = 0; i < 99; i++) spent += PET[rarityOffset(rarity) + i];
        double remaining = xp - spent;
        int level = 101;
        double step = 1886700;
        while (level < maxLevel && remaining >= (level == 101 ? 5555 : step)) {
            remaining -= level == 101 ? 5555 : step;
            level++;
        }
        if (level >= maxLevel) return new Level(maxLevel, maxLevel, 1, remaining, 0);
        double need = level == 101 ? 5555 : step;
        return new Level(level, maxLevel, remaining / need, remaining, need);
    }
}
