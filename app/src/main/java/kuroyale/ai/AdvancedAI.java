package kuroyale.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;
import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardCategory;
import kuroyale.cardpack.subclasses.AliveCard;
import kuroyale.entitiypack.subclasses.AliveEntity;

public class AdvancedAI implements AIStrategy {
    private final Random random = new Random();
    private static final double HIGH_ELIXIR_THRESHOLD = 7.0;

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

        // 2. Analyze Player's Strategy
        PlayerStrategy playerStrategy = analyzePlayerStrategy(map, rows, cols);
        
        // 3. Select Counter Card
        Card chosenCard = selectCounterCard(affordableCards, playerStrategy, currentElixir);

        // Fallback to random if strategy selection failed
        if (chosenCard == null) {
            chosenCard = affordableCards.get(random.nextInt(affordableCards.size()));
        }

        int cardID = chosenCard.getId();
        boolean isSpell = (cardID >= 25 && cardID <= 28);

        // 4. Determine Position
        if (isSpell) {
            // For spells, try to target player units if they exist
            int r = random.nextInt(rows);
            int c = random.nextInt(cols / 2 - 1); // Player's side
            
            // Try to target player units if available
            AliveEntity target = findBestSpellTarget(map, rows, cols);
            if (target != null) {
                r = target.getRow();
                c = target.getCol();
            }
            
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
     * Analyzes the player's current strategy based on entities on the field
     */
    private PlayerStrategy analyzePlayerStrategy(ArenaMap map, int rows, int cols) {
        List<AliveEntity> playerEntities = new ArrayList<>();
        
        for (AliveEntity entity : map.getEntities()) {
            if (entity.isPlayer()) {
                playerEntities.add(entity);
            }
        }
        
        if (playerEntities.isEmpty()) {
            return PlayerStrategy.NONE;
        }
        
        // Count different types of player units
        int swarmCount = 0;
        int tankCount = 0;
        int buildingCount = 0;
        int aoeCount = 0;
        
        for (AliveEntity entity : playerEntities) {
            Card card = entity.getCard();
            CardCategory category = card.getCategory();
            
            if (category == CardCategory.SWARM) {
                swarmCount++;
            } else if (category == CardCategory.SINGLE_TARGET && card instanceof AliveCard && ((AliveCard) card).getHp() > 500) {
                tankCount++;
            } else if (category == CardCategory.AOE) {
                aoeCount++;
            }
            
            // Check if it's a building
            int cardID = card.getId();
            if (cardID >= 16 && cardID <= 24) {
                buildingCount++;
            }
        }
        
        // Determine strategy based on counts
        if (swarmCount >= 2) {
            return PlayerStrategy.SWARM;
        } else if (tankCount >= 1) {
            return PlayerStrategy.TANK;
        } else if (buildingCount >= 1) {
            return PlayerStrategy.BUILDING;
        } else if (aoeCount >= 1) {
            return PlayerStrategy.AOE;
        } else if (playerEntities.size() >= 3) {
            return PlayerStrategy.SWARM; // Many units = swarm-like
        }
        
        return PlayerStrategy.SINGLE_TARGET;
    }

    /**
     * Selects a card that counters the player's strategy
     */
    private Card selectCounterCard(List<Card> affordableCards, PlayerStrategy playerStrategy, double elixir) {
        List<Card> counterCards = new ArrayList<>();
        
        for (Card card : affordableCards) {
            int cardID = card.getId();
            CardCategory category = card.getCategory();
            
            switch (playerStrategy) {
                case SWARM:
                    // Counter swarms with AoE: Valkyrie (7), Bomber (6), AoE spells (25-28)
                    if (cardID == 7 || cardID == 6) {
                        counterCards.add(card);
                    } else if (category == CardCategory.AOE) {
                        counterCards.add(card);
                    } else if (cardID >= 25 && cardID <= 28 && category == CardCategory.SPELL) {
                        // AoE spells are excellent vs swarms
                        counterCards.add(card);
                    }
                    break;
                    
                case TANK:
                    // Counter tanks with high damage single target: Mini P.E.K.K.A (3), Musketeer (2)
                    if (cardID == 3 || cardID == 2) {
                        counterCards.add(card);
                    } else if (category == CardCategory.SINGLE_TARGET && card.getDamage() > 100) {
                        counterCards.add(card);
                    }
                    break;
                    
                case BUILDING:
                    // Counter buildings with building-targeting units: Giant (4), Hog Rider (5)
                    if (cardID == 4 || cardID == 5) {
                        counterCards.add(card);
                    } else if (card.getTarget() == kuroyale.cardpack.CardTarget.BUILDINGS) {
                        counterCards.add(card);
                    }
                    break;
                    
                case AOE:
                    // Counter AoE with fast single targets or ranged units
                    if (cardID == 2 || cardID == 3) { // Musketeer, Mini P.E.K.K.A
                        counterCards.add(card);
                    } else if (category == CardCategory.SINGLE_TARGET && card instanceof AliveCard) {
                        AliveCard aliveCard = (AliveCard) card;
                        if (aliveCard.getRange() > 0) {
                            counterCards.add(card);
                        }
                    }
                    break;
                    
                case SINGLE_TARGET:
                    // Counter single targets with swarms or tanks
                    if (category == CardCategory.SWARM) {
                        counterCards.add(card);
                    } else if (cardID == 1 && card instanceof AliveCard && ((AliveCard) card).getHp() > 500) { // Knight as tank
                        counterCards.add(card);
                    }
                    break;
                    
                case NONE:
                default:
                    // No clear strategy: prefer offensive if high elixir, defensive otherwise
                    if (elixir >= HIGH_ELIXIR_THRESHOLD) {
                        if (cardID == 4 || cardID == 5 || cardID == 3) {
                            counterCards.add(card);
                        }
                    } else {
                        if (cardID == 1 || cardID == 7) {
                            counterCards.add(card);
                        }
                    }
                    break;
            }
        }
        
        if (!counterCards.isEmpty()) {
            return counterCards.get(random.nextInt(counterCards.size()));
        }
        
        // Fallback: return random affordable card
        return affordableCards.get(random.nextInt(affordableCards.size()));
    }

    /**
     * Finds the best target for a spell (prefers clustered units)
     */
    private AliveEntity findBestSpellTarget(ArenaMap map, int rows, int cols) {
        List<AliveEntity> playerEntities = new ArrayList<>();
        
        for (AliveEntity entity : map.getEntities()) {
            if (entity.isPlayer()) {
                playerEntities.add(entity);
            }
        }
        
        if (playerEntities.isEmpty()) {
            return null;
        }
        
        // Find entity with most nearby player entities (best AoE target)
        AliveEntity bestTarget = null;
        int maxNearby = 0;
        
        for (AliveEntity entity : playerEntities) {
            int nearby = 0;
            for (AliveEntity other : playerEntities) {
                if (entity != other) {
                    int dr = entity.getRow() - other.getRow();
                    int dc = entity.getCol() - other.getCol();
                    double dist = Math.sqrt(dr * dr + dc * dc);
                    if (dist <= 3.0) { // Within 3 tiles
                        nearby++;
                    }
                }
            }
            if (nearby > maxNearby) {
                maxNearby = nearby;
                bestTarget = entity;
            }
        }
        
        return bestTarget != null ? bestTarget : playerEntities.get(0);
    }

    /**
     * Enum representing different player strategies
     */
    private enum PlayerStrategy {
        SWARM,      // Many weak units
        TANK,       // High HP units
        BUILDING,   // Building-focused
        AOE,        // Area damage units
        SINGLE_TARGET, // Single target units
        NONE        // No clear strategy
    }
}
