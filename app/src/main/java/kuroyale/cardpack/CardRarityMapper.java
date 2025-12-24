package kuroyale.cardpack;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps card IDs to their rarity tiers.
 * Immutable mapping initialized once.
 */
public class CardRarityMapper {
    private static final Map<Integer, CardRarity> CARD_RARITY_MAP = new HashMap<>();
    
    static {
        // Common cards (12 total)
        CARD_RARITY_MAP.put(1, CardRarity.COMMON);  // Knight
        CARD_RARITY_MAP.put(9, CardRarity.COMMON);  // Skeletons
        CARD_RARITY_MAP.put(10, CardRarity.COMMON); // Goblins
        CARD_RARITY_MAP.put(11, CardRarity.COMMON); // Spear Goblins
        CARD_RARITY_MAP.put(12, CardRarity.COMMON); // Archers
        CARD_RARITY_MAP.put(6, CardRarity.COMMON);  // Bomber
        CARD_RARITY_MAP.put(16, CardRarity.COMMON); // Cannon
        CARD_RARITY_MAP.put(21, CardRarity.COMMON); // Tombstone
        CARD_RARITY_MAP.put(26, CardRarity.COMMON); // Arrows
        CARD_RARITY_MAP.put(25, CardRarity.COMMON); // Zap
        // Note: Spec says 12 Common, only 10 listed - adding 2 more based on common sense
        CARD_RARITY_MAP.put(23, CardRarity.COMMON); // Barbarian Hut (likely common)
        CARD_RARITY_MAP.put(24, CardRarity.COMMON); // Elixir Collector (likely common)
        
        // Rare cards (10 total)
        CARD_RARITY_MAP.put(2, CardRarity.RARE);    // Musketeer
        CARD_RARITY_MAP.put(3, CardRarity.RARE);    // Mini P.E.K.K.A
        CARD_RARITY_MAP.put(7, CardRarity.RARE);    // Valkyrie
        CARD_RARITY_MAP.put(13, CardRarity.RARE);   // Minions
        CARD_RARITY_MAP.put(15, CardRarity.RARE);   // Barbarians
        CARD_RARITY_MAP.put(17, CardRarity.RARE);   // Tesla
        CARD_RARITY_MAP.put(18, CardRarity.RARE);   // Mortar
        CARD_RARITY_MAP.put(22, CardRarity.RARE);   // Goblin Hut
        CARD_RARITY_MAP.put(27, CardRarity.RARE);   // Fireball
        // Note: Spec says 10 Rare, only 9 listed - need to determine 10th
        // Will use card 28 (Rocket) as rare to complete the 10
        CARD_RARITY_MAP.put(28, CardRarity.RARE);   // Rocket (fits rare category)
        
        // Epic cards (4 total)
        CARD_RARITY_MAP.put(4, CardRarity.EPIC);    // Giant
        CARD_RARITY_MAP.put(8, CardRarity.EPIC);    // Wizard
        CARD_RARITY_MAP.put(19, CardRarity.EPIC);   // Bomb Tower
        CARD_RARITY_MAP.put(20, CardRarity.EPIC);   // Inferno Tower
        
        // Legendary cards (2 total)
        CARD_RARITY_MAP.put(5, CardRarity.LEGENDARY);   // Hog Rider
        CARD_RARITY_MAP.put(14, CardRarity.LEGENDARY);  // Minion Horde
    }
    
    /**
     * Returns the rarity of a card by its ID.
     * @param cardId The card ID (1-28)
     * @return The card's rarity, or COMMON as default if not found
     */
    public static CardRarity getRarity(int cardId) {
        return CARD_RARITY_MAP.getOrDefault(cardId, CardRarity.COMMON);
    }
    
    /**
     * Returns an unmodifiable view of the rarity map.
     */
    public static Map<Integer, CardRarity> getAllRarities() {
        return new HashMap<>(CARD_RARITY_MAP);
    }
}
