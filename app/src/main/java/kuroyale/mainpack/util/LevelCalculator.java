package kuroyale.mainpack.util;

/**
 * Utility class for calculating stat multipliers based on card level.
 * Level 1: Base stats (multiplier 1.0)
 * Level 2: +10% increase (multiplier 1.1)
 * Level 3: +20% increase (multiplier 1.2)
 */
public class LevelCalculator {
    private static final double LEVEL_1_MULTIPLIER = 1.0;
    private static final double LEVEL_2_MULTIPLIER = 1.1;  // +10%
    private static final double LEVEL_3_MULTIPLIER = 1.2;  // +20%
    
    /**
     * Returns the HP multiplier for a given level.
     * @param level Card level (1-3)
     * @return Multiplier for HP calculation
     */
    public static double getHPMultiplier(int level) {
        switch (level) {
            case 1: return LEVEL_1_MULTIPLIER;
            case 2: return LEVEL_2_MULTIPLIER;
            case 3: return LEVEL_3_MULTIPLIER;
            default: return LEVEL_1_MULTIPLIER; // Default to level 1
        }
    }
    
    /**
     * Returns the Damage multiplier for a given level.
     * @param level Card level (1-3)
     * @return Multiplier for Damage calculation
     */
    public static double getDamageMultiplier(int level) {
        return getHPMultiplier(level); // Same multipliers for HP and Damage
    }
    
    /**
     * Calculates the adjusted stat value based on base value and level.
     * @param baseValue The base stat value (Level 1)
     * @param level The card level (1-3)
     * @return The adjusted stat value for the given level
     */
    public static double calculateStat(double baseValue, int level) {
        return baseValue * getHPMultiplier(level);
    }
    
    /**
     * Rounds damage values appropriately (damage should be integers).
     */
    public static int calculateDamage(double baseDamage, int level) {
        return (int) Math.round(calculateStat(baseDamage, level));
    }
}
