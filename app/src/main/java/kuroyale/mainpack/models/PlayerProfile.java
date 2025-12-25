package kuroyale.mainpack.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private Set<Integer> unlockedCards; // cardIds that are unlocked
    private int chestCount; // number of accumulated chests

    public PlayerProfile() {
        this.totalGold = 0;
        this.cardLevels = new HashMap<>();
        this.goldHistory = new ArrayList<>();
        this.unlockedCards = new HashSet<>();

        // Initialize all 28 cards to Level 1
        for (int i = 1; i <= 28; i++) {
            cardLevels.put(i, 1);
        }

        // Starter cards: IDs 1-8 are unlocked by default
        for (int i = 1; i <= 8; i++) {
            unlockedCards.add(i);
        }

        this.chestCount = 0;
    }

    public PlayerProfile(int totalGold, Map<Integer, Integer> cardLevels, List<GoldTransaction> goldHistory) {
        this(totalGold, cardLevels, goldHistory, null);
    }

    public PlayerProfile(int totalGold, Map<Integer, Integer> cardLevels, List<GoldTransaction> goldHistory,
            Set<Integer> unlockedCards) {
        this.totalGold = totalGold;
        this.cardLevels = cardLevels != null ? new HashMap<>(cardLevels) : new HashMap<>();
        this.goldHistory = goldHistory != null ? new ArrayList<>(goldHistory) : new ArrayList<>();

        if (unlockedCards != null && !unlockedCards.isEmpty()) {
            this.unlockedCards = new HashSet<>(unlockedCards);
        } else {
            // Default: starter cards 1-8 unlocked
            this.unlockedCards = new HashSet<>();
            for (int i = 1; i <= 8; i++) {
                this.unlockedCards.add(i);
            }
        }

        this.chestCount = 0;
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

    // ===== Card Unlock Methods =====

    public boolean isCardUnlocked(int cardId) {
        return unlockedCards.contains(cardId);
    }

    public void unlockCard(int cardId) {
        if (cardId >= 1 && cardId <= 28) {
            unlockedCards.add(cardId);
        }
    }

    public Set<Integer> getUnlockedCards() {
        return new HashSet<>(unlockedCards); // Return copy for immutability
    }

    public void setUnlockedCards(Set<Integer> cards) {
        this.unlockedCards = new HashSet<>(cards);
    }

    // ===== Chest Methods =====

    public int getChestCount() {
        return chestCount;
    }

    public void setChestCount(int chestCount) {
        this.chestCount = Math.max(0, chestCount);
    }

    public void addChest() {
        this.chestCount++;
    }

    public boolean consumeChest() {
        if (chestCount > 0) {
            chestCount--;
            return true;
        }
        return false;
    }
}
