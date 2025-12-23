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
    private final GameStateManager gameStateManager;
    private final CardManager cardManager;
    private final EntityRenderer entityRenderer;
    private final SpellSystem spellSystem;
    private final int rows;
    private final int cols;

    public EntityPlacementManager(ArenaMap arenaMap, GameStateManager gameStateManager,
                                 CardManager cardManager, EntityRenderer entityRenderer,
                                 SpellSystem spellSystem, int rows, int cols) {
        this.arenaMap = arenaMap;
        this.gameStateManager = gameStateManager;
        this.cardManager = cardManager;
        this.entityRenderer = entityRenderer;
        this.spellSystem = spellSystem;
        this.rows = rows;
        this.cols = cols;
    }

    public void handleCardDrop(DragEvent event, int targetRow, int targetCol) {
        var db = event.getDragboard();
        boolean success = false;

        if (db.hasString()) {
            String typeStr = db.getString();
            int cardID = Integer.parseInt(typeStr);

            Card cardToCheck = CardFactory.createCard(cardID);
            int cost = cardToCheck.getCost();

            if (gameStateManager.getCurrentElixir() < cost) {
                System.out.println("Not enough:" + cost);
                event.setDropCompleted(false);
                event.consume();
                return;
            }

            boolean isSpell = (cardID >= 25 && cardID <= 28);

            if (isSpell) {
                executeSpell(cardID, targetRow, targetCol, true);
                gameStateManager.consumeElixir(cost);
                
                int slotIndex = cardManager.findCardSlotIndex(cardID);
                if (slotIndex >= 0) {
                    cardManager.cycleCardInSlot(slotIndex);
                }
                
                System.out.println("Spell cast at (" + targetRow + ", " + targetCol + ")");
                event.setDropCompleted(true);
                event.consume();
                return;
            }

            if (!isSpell && targetCol >= cols / 2 - 1) {
                System.out.println("Cannot place troops on enemy side or bridge.");
                event.setDropCompleted(false);
                event.consume();
                return;
            }

            AliveEntity playedEntity;
            if (cardID <= 15) {
                playedEntity = new UnitEntity(((UnitCard) CardFactory.createCard(cardID)), true);
            } else {
                playedEntity = new BuildingEntity(((BuildingCard) CardFactory.createCard(cardID)), true);
            }
            System.out.println(playedEntity.getCard().getName());

            entityRenderer.ensureEntityNode(playedEntity, playedEntity.getCard());

            boolean placementOK;
            int cc = targetCol;
            do {
                placementOK = arenaMap.placeObject(targetRow, cc, ArenaObjectType.ENTITY);
                cc--;
            } while (!placementOK && cc >= 0);
            cc++;
            if (placementOK) {
                gameStateManager.consumeElixir(cost);

                playedEntity.setPosition(targetRow, cc);
                arenaMap.setEntity(targetRow, cc, playedEntity);
                arenaMap.addEntity(playedEntity);

                entityRenderer.setEntityDirty(true);

                int slotIndex = cardManager.findCardSlotIndex(cardID);
                System.out.println("Card played: " + cardID + " at slot: " + slotIndex);
                if (slotIndex >= 0) {
                    cardManager.cycleCardInSlot(slotIndex);
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
