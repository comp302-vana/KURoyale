package kuroyale.mainpack.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository for managing card level data in memory.
 * Acts as a cache/accessor for card levels loaded from persistence.
 */
public class CardDataRepository {
    private final Map<Integer, Integer> cardLevels; // cardId -> level
    
    public CardDataRepository(Map<Integer, Integer> initialLevels) {
        this.cardLevels = new HashMap<>(initialLevels);
        // Ensure all 28 cards are present, defaulting to Level 1
        for (int i = 1; i <= 28; i++) {
            cardLevels.putIfAbsent(i, 1);
        }
    }
    
    public int getCardLevel(int cardId) {
        return cardLevels.getOrDefault(cardId, 1);
    }
    
    public void setCardLevel(int cardId, int level) {
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("Card level must be between 1 and 3");
        }
        cardLevels.put(cardId, level);
    }
    
    public Map<Integer, Integer> getAllCardLevels() {
        return new HashMap<>(cardLevels);
    }
    
    public boolean canUpgrade(int cardId) {
        return getCardLevel(cardId) < 3;
    }
}
