package kuroyale.mainpack.managers;

import javafx.scene.input.DragEvent;
import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;
import kuroyale.cardpack.Card;
import kuroyale.cardpack.CardFactory;
import kuroyale.cardpack.subclasses.UnitCard;
import kuroyale.cardpack.subclasses.BuildingCard;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.entitiypack.subclasses.UnitEntity;
import kuroyale.entitiypack.subclasses.BuildingEntity;

/**
 * Handles entity placement via drag-and-drop, validation, and spawning.
 * High cohesion: All card placement logic in one place.
 */
public class EntityPlacementManager {
    private final ArenaMap arenaMap;
    private final GameStateManager gameStateManager;  // Single-player mode
    private final DualPlayerStateManager dualPlayerStateManager;  // PvP mode
    private final CardManager cardManager;  // Player 1 cards
    private final CardManager cardManagerP2;  // Player 2 cards (PvP mode)
    private final EntityRenderer entityRenderer;
    private final SpellSystem spellSystem;
    private final int rows;
    private final int cols;
    private final boolean isPvPMode;

    // Constructor for single-player mode
    public EntityPlacementManager(ArenaMap arenaMap, GameStateManager gameStateManager,
                                 CardManager cardManager, EntityRenderer entityRenderer,
                                 SpellSystem spellSystem, int rows, int cols) {
        this.arenaMap = arenaMap;
        this.gameStateManager = gameStateManager;
        this.dualPlayerStateManager = null;
        this.cardManager = cardManager;
        this.cardManagerP2 = null;
        this.entityRenderer = entityRenderer;
        this.spellSystem = spellSystem;
        this.rows = rows;
        this.cols = cols;
        this.isPvPMode = false;
    }
    
    // Constructor for PvP mode
    public EntityPlacementManager(ArenaMap arenaMap, DualPlayerStateManager dualPlayerStateManager,
                                 CardManager cardManager, CardManager cardManagerP2,
                                 EntityRenderer entityRenderer, SpellSystem spellSystem,
                                 int rows, int cols) {
        this.arenaMap = arenaMap;
        this.gameStateManager = null;
        this.dualPlayerStateManager = dualPlayerStateManager;
        this.cardManager = cardManager;
        this.cardManagerP2 = cardManagerP2;
        this.entityRenderer = entityRenderer;
        this.spellSystem = spellSystem;
        this.rows = rows;
        this.cols = cols;
        this.isPvPMode = true;
    }
    
    /**
     * Determine which player (1 or 2) based on drop location.
     * Player 1 controls left side (col < cols/2 - 1), Player 2 controls right side (col >= cols/2).
     */
    private int determinePlayerFromDropLocation(int targetCol) {
        if (!isPvPMode) return 1;  // Single-player mode always uses player 1
        return (targetCol < cols / 2 - 1) ? 1 : 2;
    }

    public void handleCardDrop(DragEvent event, int targetRow, int targetCol) {
        var db = event.getDragboard();
        boolean success = false;

        if (db.hasString()) {
            String typeStr = db.getString();
            int cardID = Integer.parseInt(typeStr);

            Card cardToCheck = CardFactory.createCard(cardID);
            int cost = cardToCheck.getCost();

            // Determine player and get appropriate managers
            int playerId = determinePlayerFromDropLocation(targetCol);
            CardManager activeCardManager = (playerId == 1) ? cardManager : cardManagerP2;
            
            // Check elixir
            double currentElixir;
            if (isPvPMode) {
                currentElixir = dualPlayerStateManager.getElixir(playerId);
            } else {
                currentElixir = gameStateManager.getCurrentElixir();
            }
            
            if (currentElixir < cost) {
                System.out.println("Not enough elixir:" + cost);
                event.setDropCompleted(false);
                event.consume();
                return;
            }

            boolean isSpell = (cardID >= 25 && cardID <= 28);

            if (isSpell) {
                // Spells can be cast anywhere, but consume elixir from the appropriate player
                executeSpell(cardID, targetRow, targetCol, playerId == 1);
                
                if (isPvPMode) {
                    dualPlayerStateManager.consumeElixir(playerId, cost);
                } else {
                    gameStateManager.consumeElixir(cost);
                }
                
                int slotIndex = activeCardManager.findCardSlotIndex(cardID);
                if (slotIndex >= 0) {
                    activeCardManager.cycleCardInSlot(slotIndex);
                }
                
                System.out.println("Spell cast at (" + targetRow + ", " + targetCol + ") by Player " + playerId);
                event.setDropCompleted(true);
                event.consume();
                return;
            }

            // Validate placement: in PvP, players can only place on their side
            if (isPvPMode) {
                if (playerId == 1 && targetCol >= cols / 2 - 1) {
                    System.out.println("Player 1 cannot place troops on enemy side or bridge.");
                    event.setDropCompleted(false);
                    event.consume();
                    return;
                }
                if (playerId == 2 && targetCol < cols / 2) {
                    System.out.println("Player 2 cannot place troops on enemy side or bridge.");
                    event.setDropCompleted(false);
                    event.consume();
                    return;
                }
            } else {
                // Single-player: can't place on enemy side
                if (!isSpell && targetCol >= cols / 2 - 1) {
                    System.out.println("Cannot place troops on enemy side or bridge.");
                    event.setDropCompleted(false);
                    event.consume();
                    return;
                }
            }

            // Create entity with correct isPlayer flag
            boolean isPlayer = (playerId == 1);
            AliveEntity playedEntity;
            if (cardID <= 15) {
                playedEntity = new UnitEntity(((UnitCard) CardFactory.createCard(cardID)), isPlayer);
            } else {
                playedEntity = new BuildingEntity(((BuildingCard) CardFactory.createCard(cardID)), isPlayer);
            }
            System.out.println(playedEntity.getCard().getName() + " by Player " + playerId);

            entityRenderer.ensureEntityNode(playedEntity, playedEntity.getCard());

            boolean placementOK;
            int cc = targetCol;
            do {
                placementOK = arenaMap.placeObject(targetRow, cc, ArenaObjectType.ENTITY);
                cc--;
            } while (!placementOK && cc >= 0);
            cc++;
            if (placementOK) {
                // Consume elixir from appropriate player
                if (isPvPMode) {
                    dualPlayerStateManager.consumeElixir(playerId, cost);
                } else {
                    gameStateManager.consumeElixir(cost);
                }

                playedEntity.setPosition(targetRow, cc);
                arenaMap.setEntity(targetRow, cc, playedEntity);
                arenaMap.addEntity(playedEntity);

                entityRenderer.setEntityDirty(true);

                int slotIndex = activeCardManager.findCardSlotIndex(cardID);
                System.out.println("Card played: " + cardID + " at slot: " + slotIndex + " by Player " + playerId);
                if (slotIndex >= 0) {
                    activeCardManager.cycleCardInSlot(slotIndex);
                }

                System.out.println("yey");
                System.out.printf("(%d, %d)\n", targetRow, cc);
                success = true;
            } else {
                System.out.println("no");
            }
        }

        event.setDropCompleted(success);
        event.consume();
    }

    private void executeSpell(int spellCardID, int targetRow, int targetCol, boolean isPlayerSpell) {
        spellSystem.executeSpell(spellCardID, targetRow, targetCol, isPlayerSpell, entityRenderer);
        
        // Mark entities dirty for cleanup (dead entities will be removed in next update cycle)
        entityRenderer.setEntityDirty(true);
    }
}
