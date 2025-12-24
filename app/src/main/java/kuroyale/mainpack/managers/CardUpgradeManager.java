package kuroyale.mainpack.managers;

import kuroyale.cardpack.CardRarity;
import kuroyale.cardpack.CardRarityMapper;
import kuroyale.mainpack.util.CardDataRepository;
import kuroyale.mainpack.util.UpgradeCostCalculator;

/**
 * Manages card upgrade operations.
 * High cohesion: All card upgrade logic in one place.
 */
public class CardUpgradeManager {
    private final EconomyManager economyManager;
    private final CardDataRepository cardDataRepository;
    private final PersistenceManager persistenceManager;
    
    public CardUpgradeManager(EconomyManager economyManager, CardDataRepository cardDataRepository,
                             PersistenceManager persistenceManager) {
        this.economyManager = economyManager;
        this.cardDataRepository = cardDataRepository;
        this.persistenceManager = persistenceManager;
    }
    
    /**
     * Result of an upgrade operation.
     */
    public static class UpgradeResult {
        public final boolean success;
        public final int newLevel;
        public final String message;
        
        public UpgradeResult(boolean success, int newLevel, String message) {
            this.success = success;
            this.newLevel = newLevel;
            this.message = message;
        }
    }
    
    /**
     * Checks if a card can be upgraded.
     */
    public boolean canUpgrade(int cardId) {
        if (!cardDataRepository.canUpgrade(cardId)) {
            return false; // Already at max level
        }
        
        int currentLevel = cardDataRepository.getCardLevel(cardId);
        CardRarity rarity = CardRarityMapper.getRarity(cardId);
        int cost = UpgradeCostCalculator.calculateUpgradeCost(rarity, currentLevel);
        
        return economyManager.canAfford(cost);
    }
    
    /**
     * Gets the upgrade cost for a card.
     */
    public int getUpgradeCost(int cardId) {
        int currentLevel = cardDataRepository.getCardLevel(cardId);
        if (currentLevel >= 3) {
            return -1; // At max level
        }
        
        CardRarity rarity = CardRarityMapper.getRarity(cardId);
        return UpgradeCostCalculator.calculateUpgradeCost(rarity, currentLevel);
    }
    
    /**
     * Gets the current level of a card.
     */
    public int getCurrentLevel(int cardId) {
        return cardDataRepository.getCardLevel(cardId);
    }
    
    /**
     * Upgrades a card if possible.
     * @param cardId The card to upgrade
     * @return UpgradeResult indicating success or failure
     */
    public UpgradeResult upgradeCard(int cardId) {
        // Check if already at max level
        if (!cardDataRepository.canUpgrade(cardId)) {
            return new UpgradeResult(false, cardDataRepository.getCardLevel(cardId), 
                                   "Card is already at maximum level");
        }
        
        int currentLevel = cardDataRepository.getCardLevel(cardId);
        CardRarity rarity = CardRarityMapper.getRarity(cardId);
        int cost = UpgradeCostCalculator.calculateUpgradeCost(rarity, currentLevel);
        
        // Check if player can afford
        if (!economyManager.canAfford(cost)) {
            return new UpgradeResult(false, currentLevel, 
                                   "Insufficient gold. Need " + cost + " gold.");
        }
        
        // Consume gold
        if (!economyManager.consumeGold(cost)) {
            return new UpgradeResult(false, currentLevel, "Failed to consume gold");
        }
        
        // Upgrade the card
        int newLevel = currentLevel + 1;
        cardDataRepository.setCardLevel(cardId, newLevel);
        
        // Save to persistence
        kuroyale.mainpack.models.PlayerProfile profile = persistenceManager.loadPlayerProfile();
        profile.setCardLevel(cardId, newLevel);
        profile.setTotalGold(economyManager.getCurrentGold());
        persistenceManager.savePlayerProfile(profile);
        
        return new UpgradeResult(true, newLevel, "Card upgraded to Level " + newLevel + "!");
    }
}
