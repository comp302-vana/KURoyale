package kuroyale.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;
import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardCategory;
import kuroyale.entitiypack.subclasses.AliveEntity;

public class MediumAI implements AIStrategy {
    private final Random random = new Random();
    private static final double HIGH_ELIXIR_THRESHOLD = 7.0;
    private static final int DEFENSIVE_SIDE_COL = 16; // AI's side starts at column 16

    @Override
    public AIAction decideMove(List<Card> hand, double currentElixir, ArenaMap map, int rows, int cols) {
        // 1. Filter Affordable Cards
        List<Card> affordableCards = new ArrayList<>();
        for (Card card : hand) {
            if (card != null && currentElixir >= card.getCost()) {
                affordableCards.add(card);
            }
        }

        if (affordableCards.isEmpty()) {
            return null; // No move possible
        }

        // 2. Determine Strategy: Offensive (high elixir) or Defensive (player attacking)
        boolean isOffensive = currentElixir >= HIGH_ELIXIR_THRESHOLD;
        boolean playerAttacking = isPlayerAttacking(map, rows, cols);

        // 3. Select Card Based on Strategy
        Card chosenCard = null;
        if (isOffensive) {
            // High elixir: prefer offensive units (buildings-targeting, high damage)
            chosenCard = selectOffensiveCard(affordableCards);
        } else if (playerAttacking) {
            // Player attacking: prefer defensive cards (tanks, defensive buildings, AoE)
            chosenCard = selectDefensiveCard(affordableCards);
        } else {
            // Default: random affordable card
            chosenCard = affordableCards.get(random.nextInt(affordableCards.size()));
        }

        // Fallback to random if strategy selection failed
        if (chosenCard == null) {
            chosenCard = affordableCards.get(random.nextInt(affordableCards.size()));
        }

        int cardID = chosenCard.getId();
        boolean isSpell = (cardID >= 25 && cardID <= 28);

        // 4. Determine Position
        if (isSpell) {
            // Spells can be placed anywhere on player's side
            int r = random.nextInt(rows);
            int c = random.nextInt(cols / 2 - 1); // Player's side
            return new AIAction(chosenCard, r, c);
        } else {
            // Units need valid placement
            int enemySideStartCol = cols / 2 + 1;
            
            for (int attempt = 0; attempt < 10; attempt++) {
                int r = random.nextInt(rows);
                int c = enemySideStartCol + random.nextInt(cols - enemySideStartCol);

                // Collision Logic
                int cc = c;
                boolean placementOK;
                do {
                    placementOK = map.placeObject(r, cc, ArenaObjectType.ENTITY);
                    if (!placementOK) cc--; 
                } while (!placementOK && cc >= enemySideStartCol);
                cc++; // Adjust back to valid spot

                if (map.placeObject(r, cc, ArenaObjectType.ENTITY)) {
                    return new AIAction(chosenCard, r, cc);
                }
            }
        }
        return null; // No valid move found after attempts
    }

    /**
     * Checks if the player has units on the AI's side (indicating an attack)
     */
    private boolean isPlayerAttacking(ArenaMap map, int rows, int cols) {
        int aiSideStartCol = cols / 2 + 1; // AI's side starts here
        
        for (AliveEntity entity : map.getEntities()) {
            if (entity.isPlayer() && entity.getCol() >= aiSideStartCol) {
                // Player has a unit on AI's side
                return true;
            }
        }
        return false;
    }

    /**
     * Selects an offensive card (prefers buildings-targeting, high damage units)
     */
    private Card selectOffensiveCard(List<Card> affordableCards) {
        List<Card> preferredCards = new ArrayList<>();
        
        for (Card card : affordableCards) {
            int cardID = card.getId();
            // Prefer: Giant (4), Hog Rider (5), Mini P.E.K.K.A (3), or high damage units
            if (cardID == 4 || cardID == 5 || cardID == 3) {
                preferredCards.add(card);
            } else if (card.getCategory() == CardCategory.SINGLE_TARGET && card.getDamage() > 100) {
                preferredCards.add(card);
            }
        }
        
        if (!preferredCards.isEmpty()) {
            return preferredCards.get(random.nextInt(preferredCards.size()));
        }
        
        // Fallback: return random affordable card
        return affordableCards.get(random.nextInt(affordableCards.size()));
    }

    /**
     * Selects a defensive card (prefers tanks, defensive buildings, AoE units)
     */
    private Card selectDefensiveCard(List<Card> affordableCards) {
        List<Card> preferredCards = new ArrayList<>();
        
        for (Card card : affordableCards) {
            int cardID = card.getId();
            // Prefer: Knight (1) - tank, Valkyrie (7) - AoE, Bomber (6) - AoE
            // Defensive buildings (IDs 16-24), or AoE spells
            if (cardID == 1 || cardID == 7 || cardID == 6) {
                preferredCards.add(card);
            } else if (card.getCategory() == CardCategory.DEFENSIVE_BUILDING) {
                preferredCards.add(card);
            } else if (card.getCategory() == CardCategory.AOE) {
                preferredCards.add(card);
            } else if (cardID >= 25 && cardID <= 28 && card.getCategory() == CardCategory.SPELL) {
                // AoE spells are good for defense
                preferredCards.add(card);
            }
        }
        
        if (!preferredCards.isEmpty()) {
            return preferredCards.get(random.nextInt(preferredCards.size()));
        }
        
        // Fallback: return random affordable card
        return affordableCards.get(random.nextInt(affordableCards.size()));
    }
}
