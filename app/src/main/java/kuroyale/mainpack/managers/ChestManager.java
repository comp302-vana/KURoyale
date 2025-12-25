package kuroyale.mainpack.managers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardFactory;
import kuroyale.cardpack.CardRarity;
import kuroyale.cardpack.CardRarityMapper;
import kuroyale.mainpack.models.ChestReward;
import kuroyale.mainpack.models.PlayerProfile;

/**
 * Manages chest opening mechanics and rewards.
 * Reads configuration from chest_config.txt for easy customization.
 */
public class ChestManager {
    private static final Random random = new Random();

    // Default values (overridden by config file if present)
    private static int noCardChance = 25;
    private static int commonChance = 40;
    private static int rareChance = 25;
    private static int epicChance = 8;
    private static int legendaryChance = 2;
    private static int minGold = 20;
    private static int maxGold = 50;

    // Config loaded flag
    private static boolean configLoaded = false;

    /**
     * Loads configuration from chest_config.txt file.
     */
    private static void loadConfig() {
        if (configLoaded)
            return;

        try {
            InputStream is = ChestManager.class.getResourceAsStream("/kuroyale/config/chest_config.txt");
            if (is == null) {
                System.out.println("[ChestManager] Config file not found, using defaults");
                configLoaded = true;
                return;
            }

            Map<String, Integer> configValues = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // Skip comments and empty lines
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    // Parse KEY=VALUE format
                    String[] parts = line.split("=");
                    if (parts.length == 2) {
                        String key = parts[0].trim().toUpperCase();
                        try {
                            int value = Integer.parseInt(parts[1].trim());
                            configValues.put(key, value);
                        } catch (NumberFormatException e) {
                            System.out.println("[ChestManager] Invalid value for " + key);
                        }
                    }
                }
            }

            // Apply config values
            if (configValues.containsKey("NO_CARD_CHANCE")) {
                noCardChance = configValues.get("NO_CARD_CHANCE");
            }
            if (configValues.containsKey("COMMON_CHANCE")) {
                commonChance = configValues.get("COMMON_CHANCE");
            }
            if (configValues.containsKey("RARE_CHANCE")) {
                rareChance = configValues.get("RARE_CHANCE");
            }
            if (configValues.containsKey("EPIC_CHANCE")) {
                epicChance = configValues.get("EPIC_CHANCE");
            }
            if (configValues.containsKey("LEGENDARY_CHANCE")) {
                legendaryChance = configValues.get("LEGENDARY_CHANCE");
            }
            if (configValues.containsKey("MIN_GOLD")) {
                minGold = configValues.get("MIN_GOLD");
            }
            if (configValues.containsKey("MAX_GOLD")) {
                maxGold = configValues.get("MAX_GOLD");
            }

            System.out.println("[ChestManager] Config loaded - NoCard: " + noCardChance +
                    "%, Common: " + commonChance + "%, Rare: " + rareChance +
                    "%, Epic: " + epicChance + "%, Legendary: " + legendaryChance + "%");

        } catch (Exception e) {
            System.out.println("[ChestManager] Error loading config: " + e.getMessage());
        }

        configLoaded = true;
    }

    /**
     * Opens a basic chest and returns the rewards.
     */
    public static ChestReward openBasicChest(PlayerProfile profile) {
        // Ensure config is loaded
        loadConfig();

        // Calculate gold reward
        int goldAmount = calculateGoldReward();

        // Roll for card (with chance of no card)
        Card unlockedCard = null;
        boolean isNewCard = false;

        Set<Integer> unlockedCards = profile.getUnlockedCards();

        // First check NO_CARD_CHANCE
        int noCardRoll = random.nextInt(100);
        if (noCardRoll >= noCardChance) {
            // Only try to unlock a card if there are locked cards remaining
            if (unlockedCards.size() < 28) {
                unlockedCard = rollCard(profile);
                if (unlockedCard != null) {
                    // Check if this is a new unlock
                    isNewCard = !profile.isCardUnlocked(unlockedCard.getId());
                    if (isNewCard) {
                        profile.unlockCard(unlockedCard.getId());
                    }
                }
            }
        }

        // Note: Gold will be added by caller (DeckBuilder) via EconomyManager
        // Card unlock is handled here since we have the profile

        return new ChestReward(goldAmount, unlockedCard, isNewCard);
    }

    /**
     * Calculates random gold reward between minGold and maxGold.
     */
    private static int calculateGoldReward() {
        loadConfig();
        return minGold + random.nextInt(maxGold - minGold + 1);
    }

    /**
     * Rolls for a card based on rarity weights.
     * Returns null if no card could be selected.
     */
    private static Card rollCard(PlayerProfile profile) {
        Set<Integer> unlockedCards = profile.getUnlockedCards();

        // Try up to 10 times to find a locked card
        for (int attempt = 0; attempt < 10; attempt++) {
            // Roll rarity
            CardRarity rarity = rollRarity();

            // Get all locked cards of this rarity
            List<Integer> lockedCardsOfRarity = getLockedCardsByRarity(rarity, unlockedCards);

            if (!lockedCardsOfRarity.isEmpty()) {
                // Pick a random locked card of this rarity
                int cardId = lockedCardsOfRarity.get(random.nextInt(lockedCardsOfRarity.size()));
                return CardFactory.createCard(cardId);
            }
        }

        // If we couldn't find a locked card after 10 attempts, return null
        return null;
    }

    /**
     * Rolls for a rarity based on drop rates from config.
     */
    private static CardRarity rollRarity() {
        int roll = random.nextInt(100);

        if (roll < legendaryChance) {
            return CardRarity.LEGENDARY;
        } else if (roll < legendaryChance + epicChance) {
            return CardRarity.EPIC;
        } else if (roll < legendaryChance + epicChance + rareChance) {
            return CardRarity.RARE;
        } else {
            return CardRarity.COMMON;
        }
    }

    /**
     * Gets all locked card IDs of a specific rarity.
     */
    private static List<Integer> getLockedCardsByRarity(CardRarity rarity, Set<Integer> unlockedCards) {
        List<Integer> lockedCards = new ArrayList<>();

        for (int cardId = 1; cardId <= 28; cardId++) {
            if (!unlockedCards.contains(cardId) && CardRarityMapper.getRarity(cardId) == rarity) {
                lockedCards.add(cardId);
            }
        }

        return lockedCards;
    }

    /**
     * Forces config reload on next chest open.
     * Useful for testing config changes without restarting.
     */
    public static void reloadConfig() {
        configLoaded = false;
        loadConfig();
    }
}
