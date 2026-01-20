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
import kuroyale.mainpack.network.CoordinateTransformer;
import kuroyale.mainpack.network.NetworkMessage;

/**
 * Handles entity placement via drag-and-drop, validation, and spawning.
 * High cohesion: All card placement logic in one place.
 */
public class EntityPlacementManager {
    private final ArenaMap arenaMap;
    private final GameStateManager gameStateManager; // Single-player mode
    private final DualPlayerStateManager dualPlayerStateManager; // PvP mode
    private final CardManager cardManager; // Player 1 cards
    private final CardManager cardManagerP2; // Player 2 cards (PvP mode)
    private final CardFactory cardFactory;
    private final EntityRenderer entityRenderer;
    private final SpellSystem spellSystem;
    private final int rows;
    private final int cols;
    private final boolean isPvPMode;
    private QuestManager questManager;
    private ComboManager comboManager;
    private PersistenceManager persistenceManager;
    private AchievementManager achievementManager;
    private ChallengeManager challengeManager;
    private kuroyale.mainpack.network.NetworkBattleManager networkBattleManager;
    private boolean isClient;

    // Constructor for single-player mode
    public EntityPlacementManager(ArenaMap arenaMap, GameStateManager gameStateManager,
            CardManager cardManager, EntityRenderer entityRenderer,
            SpellSystem spellSystem, int rows, int cols) {
        this.arenaMap = arenaMap;
        this.gameStateManager = gameStateManager;
        this.dualPlayerStateManager = null;
        this.cardManager = cardManager;
        this.cardManagerP2 = null;
        this.cardFactory = CardFactory.getInstance();
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
        this.cardFactory = CardFactory.getInstance();
        this.entityRenderer = entityRenderer;
        this.spellSystem = spellSystem;
        this.rows = rows;
        this.cols = cols;
        this.isPvPMode = true;
    }

    // setter method for quest,persistance and achievement manager
    public void setQuestManager(QuestManager questManager) {
        this.questManager = questManager;
    }

    public void setPersistenceManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    public void setAchievementManager(AchievementManager achievementManager) {
        this.achievementManager = achievementManager;
    }

    public void setChallengeManager(ChallengeManager challengeManager) {
        this.challengeManager = challengeManager;
    }

    public void setComboManager(ComboManager comboManager) {
        this.comboManager = comboManager;
    }

    /**
     * Set network battle manager for network multiplayer mode.
     */
    public void setNetworkBattleManager(kuroyale.mainpack.network.NetworkBattleManager battleManager,
            boolean isClient) {
        this.networkBattleManager = battleManager;
        this.isClient = isClient;
    }

    /**
     * Determine which player (1 or 2) based on drop location.
     * Player 1 controls left side (col < 15), Player 2 controls right side (col >=
     * 16).
     * In network mode, client is always player 2, host is player 1.
     */
    private int determinePlayerFromDropLocation(int targetCol) {
        if (networkBattleManager != null) {
            // Network mode: determine by role
            return networkBattleManager.isHost() ? 1 : 2;
        }
        if (!isPvPMode)
            return 1; // Single-player mode always uses player 1
        // Use column-based zone: left side (col < 15) = Player 1, right side (col >=
        // 16) = Player 2
        return (targetCol < 15) ? 1 : 2;
    }

    /**
     * Adjust column for placement based on player side.
     * FIRST clamps starting column to valid zone, then searches.
     * Player 1 (left side) searches leftwards (cc--).
     * Player 2 (right side) searches rightwards (cc++).
     * Never allows shifting across the river boundary.
     * 
     * @param row        Row coordinate
     * @param desiredCol Desired column coordinate (may be on river)
     * @param playerId   1 for left side, 2 for right side
     * @return Final column where entity was placed, or -1 if placement failed
     */
    private int adjustColForPlacement(int row, int desiredCol, int playerId) {
        int[] riverBounds = CoordinateTransformer.getRiverBoundaries();
        int riverLeftLimit = riverBounds[0]; // 15
        int riverRightLimit = riverBounds[1]; // 16

        // CRITICAL: First clamp starting column to valid zone
        int startCol;
        if (playerId == 1) {
            // Player 1: clamp to [0, riverLeftLimit-1] = [0, 14]
            startCol = Math.min(desiredCol, riverLeftLimit - 1);
            startCol = Math.max(0, startCol); // Ensure >= 0
        } else {
            // Player 2: clamp to [riverRightLimit, cols-1] = [16, 31]
            startCol = Math.max(desiredCol, riverRightLimit);
            startCol = Math.min(cols - 1, startCol); // Ensure <= cols-1
        }

        // Try exact clamped position first
        if (arenaMap.placeObject(row, startCol, ArenaObjectType.ENTITY)) {
            return startCol;
        }

        // Search in appropriate direction within valid zone
        if (playerId == 1) {
            // Player 1 (left side): search leftwards within [0, riverLeftLimit-1]
            int cc = startCol - 1;
            while (cc >= 0 && cc < riverLeftLimit) {
                if (arenaMap.placeObject(row, cc, ArenaObjectType.ENTITY)) {
                    return cc;
                }
                cc--;
            }
        } else {
            // Player 2 (right side): search rightwards within [riverRightLimit, cols-1]
            int cc = startCol + 1;
            while (cc < cols && cc >= riverRightLimit) {
                if (arenaMap.placeObject(row, cc, ArenaObjectType.ENTITY)) {
                    return cc;
                }
                cc++;
            }
        }

        return -1; // Placement failed
    }

    /**
     * Check if we're in network mode.
     */
    private boolean isNetworkMode() {
        return networkBattleManager != null;
    }

    public void handleCardDrop(DragEvent event, int targetRow, int targetCol) {
        var db = event.getDragboard();
        boolean success = false;

        if (db.hasString()) {
            String typeStr = db.getString();
            int cardID = Integer.parseInt(typeStr);

            // Network mode: handle differently
            if (isNetworkMode() && isClient) {
                // CLIENT SIDE: Transform coordinates and send to host
                handleClientCardDrop(event, cardID, targetRow, targetCol);
                return;
            }

            Card cardToCheck = cardFactory.createCard(cardID);
            int cost = cardToCheck.getCost();

            // Decorator Pattern: Apply challenge-specific cost modification
            if (challengeManager != null) {
                cost = challengeManager.getModifiedCost(cost, cardID);
            }

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
            boolean isBuilding = (cardID >= 16 && cardID <= 24);
            boolean isTroop = (cardID >= 1 && cardID <= 15);
            boolean isCommonCard = (kuroyale.cardpack.CardRarityMapper
                    .getRarity(cardID) == kuroyale.cardpack.CardRarity.COMMON);

            if (isSpell) {
                // Spells can be cast anywhere, but consume elixir from the appropriate player
                executeSpell(cardID, targetRow, targetCol, playerId == 1);

                // Check for spell synergy combo refund
                int refundAmount = 0;
                if (comboManager != null && comboManager.shouldRefundLastSpell()) {
                    refundAmount = 1;
                    System.out.println("Spell Synergy combo: Refunding 1 Elixir");
                }

                int actualCost = Math.max(1, cost - refundAmount); // Minimum 1 elixir
                if (isPvPMode) {
                    dualPlayerStateManager.consumeElixir(playerId, actualCost);
                } else {
                    gameStateManager.consumeElixir(actualCost);
                }

                // Notify combo manager about spell play (for detection)
                if (comboManager != null) {
                    comboManager.onCardPlayed(cardID, null, playerId, targetRow, targetCol); // Spells don't have
                                                                                             // entities
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

            // Network mode: Host's own placement - use unified authoritative method
            if (isNetworkMode() && !isClient && playerId == 1) {
                // Host placing own card - use unified authoritative placement
                int finalCol = placeCardAuthoritative(1, cardID, targetRow, targetCol, 0, true);
                event.setDropCompleted(finalCol >= 0);
                event.consume();
                return;
            }

            // Create entity with correct isPlayer flag
            boolean isPlayer = (playerId == 1);
            AliveEntity playedEntity;
            if (cardID <= 15) {
                playedEntity = new UnitEntity(((UnitCard) cardFactory.createCard(cardID)), isPlayer);
            } else {
                playedEntity = new BuildingEntity(((BuildingCard) cardFactory.createCard(cardID)), isPlayer);
            }
            System.out.println(playedEntity.getCard().getName() + " by Player " + playerId);

            entityRenderer.ensureEntityNode(playedEntity, playedEntity.getCard());

            // Use direction-aware placement
            int finalCol = adjustColForPlacement(targetRow, targetCol, playerId);
            if (finalCol >= 0) {
                // Assign entity ID in network mode (host only)
                if (isNetworkMode() && !isClient && networkBattleManager != null) {
                    long entityId = kuroyale.mainpack.network.EntityIdGenerator.getInstance().generateId();
                    playedEntity.setEntityId(entityId);
                    networkBattleManager.getEntityRegistry().registerEntity(entityId, playedEntity);
                }

                // Consume elixir from appropriate player
                if (isPvPMode || isNetworkMode()) {
                    dualPlayerStateManager.consumeElixir(playerId, cost);
                } else {
                    gameStateManager.consumeElixir(cost);
                }

                playedEntity.setPosition(targetRow, finalCol);
                arenaMap.setEntity(targetRow, finalCol, playedEntity);
                arenaMap.addEntity(playedEntity);

                entityRenderer.setEntityDirty(true);

                // Send to client if network mode and host
                if (isNetworkMode() && !isClient && networkBattleManager != null) {
                    networkBattleManager.sendEntitySpawn(playedEntity, targetRow, finalCol);
                    System.out.println("Host: Sent entity spawn: ID=" + playedEntity.getEntityId() +
                            ", card=" + cardID + ", player=" + playerId +
                            " at (" + targetRow + ", " + finalCol + ")");
                }

                int slotIndex = activeCardManager.findCardSlotIndex(cardID);
                System.out.println("Card played: " + cardID + " at slot: " + slotIndex + " by Player " + playerId);
                if (slotIndex >= 0) {
                    activeCardManager.cycleCardInSlot(slotIndex);
                }

                System.out.printf("Entity placed at (%d, %d)\n", targetRow, finalCol);
                success = true;
                // Notify quest manager about card played
                if (questManager != null) {
                    questManager.onCardPlayed(cardID, isSpell, isBuilding, isTroop, cost, isCommonCard);
                }
                if (comboManager != null) {
                    comboManager.onCardPlayed(cardID, playedEntity, playerId, targetRow, finalCol);
                }
            } else {
                System.out.println("no");
            }

        }

        event.setDropCompleted(success);
        event.consume();
    }

    /**
     * Handle card drop on client side (network mode).
     * CLIENT SEND-ONLY: No local state mutations.
     * Transforms coordinates and sends request to host.
     */
    private void handleClientCardDrop(DragEvent event, int cardID, int clientRow, int clientCol) {
        // Transform client coordinates to absolute
        int[] absoluteCoords = CoordinateTransformer.clientToAbsolute(clientRow, clientCol, rows, cols);
        int absoluteRow = absoluteCoords[0];
        int absoluteCol = absoluteCoords[1];

        // Validate zone: client (player 2) can only place in right side (col >= 16)
        if (!CoordinateTransformer.isValidPlayer2Zone(absoluteRow, absoluteCol, rows, cols)) {
            System.out.println(
                    "Client: Invalid placement zone - requested absolute (" + absoluteRow + ", " + absoluteCol +
                            ") is not in Player 2 zone (col >= 16)");
            event.setDropCompleted(false);
            event.consume();
            return;
        }

        // Generate requestId for tracking
        int requestId = (int) (System.currentTimeMillis() & 0x7FFFFFFF);

        System.out.println("Client: CARD_PLACEMENT_REQUEST - requestId=" + requestId +
                ", card=" + cardID +
                ", clientView=(" + clientRow + ", " + clientCol +
                "), absolute=(" + absoluteRow + ", " + absoluteCol + ")");

        // Check if it's a spell
        boolean isSpell = (cardID >= 25 && cardID <= 28);
        if (isSpell) {
            // Send spell request to host (host will execute and send updates)
            networkBattleManager.sendSpellCastRequest(cardID, absoluteRow, absoluteCol, requestId);
        } else {
            // Send placement request to host (with absolute coordinates and requestId)
            networkBattleManager.sendCardPlacementRequest(cardID, absoluteRow, absoluteCol, requestId);
        }

        // CLIENT DOES NOT:
        // - Consume elixir locally (host is authoritative)
        // - Cycle card locally (host will send ELIXIR_UPDATE, client cycles on
        // ENTITY_SPAWN)
        // - Spawn entity locally (host sends ENTITY_SPAWN)
        // - Execute spells locally (host executes and sends updates)

        event.setDropCompleted(true);
        event.consume();
    }

    /**
     * Unified authoritative placement method (HOST ONLY).
     * Called for both host's own UI drops and client requests.
     * 
     * @param playerId    1 for host, 2 for client
     * @param cardID      Card ID to place
     * @param row         Requested row (absolute)
     * @param col         Requested column (absolute)
     * @param requestId   Request ID for tracking (0 for host's own placement)
     * @param fromLocalUI If true, cycle card slot and update local UI
     * @return Final column where placed, or -1 if rejected
     */
    public int placeCardAuthoritative(int playerId, int cardID, int row, int col, int requestId, boolean fromLocalUI) {
        Card cardToCheck = cardFactory.createCard(cardID);
        int cost = cardToCheck.getCost();

        // Validate zone
        boolean validZone = CoordinateTransformer.isValidPlacementZone(row, col, playerId, rows, cols);
        if (!validZone) {
            System.out.println("Host: CARD_PLACEMENT REJECTED - requestId=" + requestId +
                    ", player=" + playerId + ", card=" + cardID +
                    ", requested=(" + row + ", " + col + ") - invalid zone");
            if (requestId > 0) {
                networkBattleManager.sendPlacementRejected(requestId, "Invalid zone");
            }
            return -1;
        }

        // Check elixir
        double currentElixir = dualPlayerStateManager.getElixir(playerId);
        if (currentElixir < cost) {
            System.out.println("Host: CARD_PLACEMENT REJECTED - requestId=" + requestId +
                    ", player=" + playerId + ", card=" + cardID +
                    ", requested=(" + row + ", " + col +
                    ") - not enough elixir (have " + currentElixir + ", need " + cost + ")");
            if (requestId > 0) {
                networkBattleManager.sendPlacementRejected(requestId, "Not enough elixir");
            }
            return -1;
        }

        // Check if it's a spell
        boolean isSpell = (cardID >= 25 && cardID <= 28);
        if (isSpell) {
            // Host executes spell authoritatively
            executeSpell(cardID, row, col, playerId == 1);

            // Check for spell synergy combo refund
            int refundAmount = 0;
            if (comboManager != null && comboManager.shouldRefundLastSpell()) {
                refundAmount = 1;
                System.out.println("Spell Synergy combo: Refunding 1 Elixir");
            }

            int actualCost = Math.max(1, cost - refundAmount); // Minimum 1 elixir
            dualPlayerStateManager.consumeElixir(playerId, actualCost);

            // Notify combo manager about spell play (for detection)
            if (comboManager != null) {
                comboManager.onCardPlayed(cardID, null, playerId, row, col); // Spells don't have entities
            }

            // Send spell event to client (for VFX)
            if (requestId > 0) {
                networkBattleManager.sendSpellCastEvent(cardID, row, col);
            }

            // Send elixir update
            networkBattleManager.sendElixirUpdate(
                    dualPlayerStateManager.getElixir(1),
                    dualPlayerStateManager.getElixir(2));

            // Cycle card if from local UI
            if (fromLocalUI) {
                CardManager activeCardManager = (playerId == 1) ? cardManager : cardManagerP2;
                int slotIndex = activeCardManager.findCardSlotIndex(cardID);
                if (slotIndex >= 0) {
                    activeCardManager.cycleCardInSlot(slotIndex);
                }
            }

            return col; // Spells don't have a "placement" cell, return requested col
        }

        // Create entity
        boolean isPlayer = (playerId == 1);
        AliveEntity entity;
        if (cardID <= 15) {
            entity = new UnitEntity(((UnitCard) cardFactory.createCard(cardID)), isPlayer);
        } else {
            entity = new BuildingEntity(((BuildingCard) cardFactory.createCard(cardID)), isPlayer);
        }

        // Assign entity ID (host assigns IDs)
        long entityId = kuroyale.mainpack.network.EntityIdGenerator.getInstance().generateId();
        entity.setEntityId(entityId);

        // Register entity
        networkBattleManager.getEntityRegistry().registerEntity(entityId, entity);

        // Use direction-aware placement with clamping
        int finalCol = adjustColForPlacement(row, col, playerId);

        if (finalCol >= 0) {
            entity.setPosition(row, finalCol);
            arenaMap.setEntity(row, finalCol, entity);
            arenaMap.addEntity(entity);

            // Consume elixir
            dualPlayerStateManager.consumeElixir(playerId, cost);

            // Render entity
            entityRenderer.ensureEntityNode(entity, entity.getCard());
            entityRenderer.setEntityDirty(true);

            // Send entity spawn to client with authoritative final position
            networkBattleManager.sendEntitySpawn(entity, row, finalCol);

            // Send elixir update
            networkBattleManager.sendElixirUpdate(
                    dualPlayerStateManager.getElixir(1),
                    dualPlayerStateManager.getElixir(2));

            if (comboManager != null) {
                comboManager.onCardPlayed(cardID, entity, playerId, row, finalCol);
            }

            // Cycle card if from local UI
            if (fromLocalUI) {
                CardManager activeCardManager = (playerId == 1) ? cardManager : cardManagerP2;
                int slotIndex = activeCardManager.findCardSlotIndex(cardID);
                if (slotIndex >= 0) {
                    activeCardManager.cycleCardInSlot(slotIndex);
                }
            }

            System.out.println("Host: CARD_PLACEMENT processed - requestId=" + requestId +
                    ", player=" + playerId + ", requested=(" + row + ", " + col +
                    "), final=(" + row + ", " + finalCol +
                    "), entityId=" + entityId + ", card=" + cardID);
            return finalCol;
        } else {
            System.out.println("Host: CARD_PLACEMENT REJECTED - requestId=" + requestId +
                    ", player=" + playerId + ", requested=(" + row + ", " + col +
                    ") - placement failed: no valid cell found");
            if (requestId > 0) {
                networkBattleManager.sendPlacementRejected(requestId, "Placement failed: no valid cell found");
            }
            return -1;
        }
    }

    /**
     * Handle card placement message from client (HOST SIDE).
     * Called by NetworkBattleManager when CARD_PLACEMENT_REQUEST message is
     * received.
     */
    public void handleNetworkCardPlacement(int cardID, int row, int col, int playerId, int requestId) {
        placeCardAuthoritative(playerId, cardID, row, col, requestId, false);
    }

    private void executeSpell(int spellCardID, int targetRow, int targetCol, boolean isPlayerSpell) {
        spellSystem.executeSpell(spellCardID, targetRow, targetCol, isPlayerSpell, entityRenderer);

        // Mark entities dirty for cleanup (dead entities will be removed in next update
        // cycle)
        entityRenderer.setEntityDirty(true);
    }
}
