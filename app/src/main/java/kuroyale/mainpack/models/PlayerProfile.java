package kuroyale.mainpack.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the player's profile data that needs to be persisted:
 * - Total gold amount
 * - Level of each of the 28 cards
 * - Gold earned history (for statistics)
 */
public class PlayerProfile {
    private int totalGold;
    private Map<Integer, Integer> cardLevels; // cardId -> level (1-3)
    private List<GoldTransaction> goldHistory;
    
    public PlayerProfile() {
        this.totalGold = 0;
        this.cardLevels = new HashMap<>();
        this.goldHistory = new ArrayList<>();
        
        // Initialize all 28 cards to Level 1
        for (int i = 1; i <= 28; i++) {
            cardLevels.put(i, 1);
        }
    }
    
    public PlayerProfile(int totalGold, Map<Integer, Integer> cardLevels, List<GoldTransaction> goldHistory) {
        this.totalGold = totalGold;
        this.cardLevels = cardLevels != null ? new HashMap<>(cardLevels) : new HashMap<>();
        this.goldHistory = goldHistory != null ? new ArrayList<>(goldHistory) : new ArrayList<>();
    }
    
    public int getTotalGold() {
        return totalGold;
    }
    
    public void setTotalGold(int totalGold) {
        this.totalGold = totalGold;
    }
    
    public Map<Integer, Integer> getCardLevels() {
        return new HashMap<>(cardLevels); // Return copy for immutability
    }
    
    public int getCardLevel(int cardId) {
        return cardLevels.getOrDefault(cardId, 1); // Default to Level 1 if not found
    }
    
    public void setCardLevel(int cardId, int level) {
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("Card level must be between 1 and 3");
        }
        cardLevels.put(cardId, level);
    }
    
    public List<GoldTransaction> getGoldHistory() {
        return new ArrayList<>(goldHistory); // Return copy for immutability
    }
    
    public void addGoldTransaction(GoldTransaction transaction) {
        goldHistory.add(transaction);
    }
    
    public void addGoldTransaction(int amount, String reason) {
        goldHistory.add(new GoldTransaction(System.currentTimeMillis(), amount, reason));
    }
}
