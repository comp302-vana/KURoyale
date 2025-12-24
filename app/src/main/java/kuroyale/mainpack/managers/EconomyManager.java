package kuroyale.mainpack.managers;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import kuroyale.cardpack.CardRarity;
import kuroyale.mainpack.util.UpgradeCostCalculator;

/**
 * Manages the game economy: gold tracking, earnings, and upgrade costs.
 * High cohesion: All economy-related logic in one place.
 */
public class EconomyManager {
    private IntegerProperty currentGold = new SimpleIntegerProperty(0);
    private final PersistenceManager persistenceManager;
    
    public EconomyManager(int initialGold, PersistenceManager persistenceManager) {
        this.currentGold.set(initialGold);
        this.persistenceManager = persistenceManager;
    }
    
    public int getCurrentGold() {
        return currentGold.get();
    }
    
    public IntegerProperty currentGoldProperty() {
        return currentGold;
    }
    
    /**
     * Awards gold after a match based on the result.
     * @param result Match result: "VICTORY", "DEFEAT", or "DRAW"
     */
    public void awardGold(String result) {
        int goldAmount = 0;
        switch (result) {
            case "VICTORY":
                goldAmount = 150;
                break;
            case "DEFEAT":
                goldAmount = 50;
                break;
            case "DRAW":
                goldAmount = 75;
                break;
            default:
                System.err.println("Unknown match result: " + result);
                return;
        }
        
        currentGold.set(currentGold.get() + goldAmount);
        
        // Save gold history via persistence manager
        kuroyale.mainpack.models.PlayerProfile profile = persistenceManager.loadPlayerProfile();
        profile.setTotalGold(currentGold.get());
        profile.addGoldTransaction(goldAmount, result);
        persistenceManager.savePlayerProfile(profile);
    }
    
    /**
     * Checks if the player can afford a certain cost.
     */
    public boolean canAfford(int cost) {
        return currentGold.get() >= cost && cost > 0;
    }
    
    /**
     * Consumes gold if the player has enough.
     * @param amount The amount to consume
     * @return true if successful, false if insufficient gold
     */
    public boolean consumeGold(int amount) {
        if (canAfford(amount)) {
            currentGold.set(currentGold.get() - amount);
            return true;
        }
        return false;
    }
    
    /**
     * Gets the upgrade cost for a card based on rarity and current level.
     */
    public int getUpgradeCost(CardRarity rarity, int currentLevel) {
        return UpgradeCostCalculator.calculateUpgradeCost(rarity, currentLevel);
    }
    
    /**
     * Adds gold (for testing or special cases).
     */
    public void addGold(int amount) {
        if (amount > 0) {
            currentGold.set(currentGold.get() + amount);
        }
    }
}
