package kuroyale.mainpack.util;

import java.util.HashMap;
import java.util.Map;
import kuroyale.cardpack.CardRarity;

/**
 * Calculates upgrade costs based on card rarity and level transition.
 * Uses Strategy pattern for cost calculation.
 */
public class UpgradeCostCalculator {
    // Cost map: (rarity, levelTransition) -> cost
    private static final Map<String, Integer> COST_MAP = new HashMap<>();
    
    static {
        // Common: Level 1→2: 200, Level 2→3: 500
        COST_MAP.put(key(CardRarity.COMMON, 1, 2), 200);
        COST_MAP.put(key(CardRarity.COMMON, 2, 3), 500);
        
        // Rare: Level 1→2: 400, Level 2→3: 1000
        COST_MAP.put(key(CardRarity.RARE, 1, 2), 400);
        COST_MAP.put(key(CardRarity.RARE, 2, 3), 1000);
        
        // Epic: Level 1→2: 800, Level 2→3: 2000
        COST_MAP.put(key(CardRarity.EPIC, 1, 2), 800);
        COST_MAP.put(key(CardRarity.EPIC, 2, 3), 2000);
        
        // Legendary: Level 1→2: 1500, Level 2→3: 4000
        COST_MAP.put(key(CardRarity.LEGENDARY, 1, 2), 1500);
        COST_MAP.put(key(CardRarity.LEGENDARY, 2, 3), 4000);
    }
    
    private static String key(CardRarity rarity, int fromLevel, int toLevel) {
        return rarity.name() + "_" + fromLevel + "_" + toLevel;
    }
    
    /**
     * Calculates the upgrade cost for a card based on rarity and current level.
     * @param rarity The card's rarity
     * @param currentLevel The current level (1 or 2)
     * @return The cost to upgrade to the next level, or -1 if invalid/at max level
     */
    public static int calculateUpgradeCost(CardRarity rarity, int currentLevel) {
        if (currentLevel < 1 || currentLevel > 2) {
            return -1; // Invalid level or already at max
        }
        
        int nextLevel = currentLevel + 1;
        String key = key(rarity, currentLevel, nextLevel);
        return COST_MAP.getOrDefault(key, -1);
    }
    
    /**
     * Checks if a card can be upgraded (not at max level).
     */
    public static boolean canUpgrade(int currentLevel) {
        return currentLevel >= 1 && currentLevel < 3;
    }
}
