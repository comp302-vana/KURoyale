package kuroyale.mainpack;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.List;
import kuroyale.arenapack.ArenaMap;
import kuroyale.cardpack.Card;
import kuroyale.entitiypack.Entity;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.entitiypack.subclasses.TowerEntity;

import kuroyale.mainpack.managers.EntityRenderer;
import kuroyale.mainpack.managers.CombatManager;
import kuroyale.mainpack.managers.GameStateManager;
import kuroyale.mainpack.managers.NotificationManager;
import kuroyale.mainpack.managers.PersistenceManager;
import kuroyale.mainpack.managers.QuestManager;
import kuroyale.mainpack.managers.CardManager;
import kuroyale.mainpack.managers.ChallengeManager;
import kuroyale.mainpack.managers.SpellSystem;
import kuroyale.mainpack.managers.EntityUpdater;
import kuroyale.mainpack.managers.TowerManager;
import kuroyale.mainpack.managers.SceneNavigationManager;
import kuroyale.mainpack.managers.VictoryConditionManager;
import kuroyale.mainpack.managers.EntityLifecycleManager;
import kuroyale.mainpack.managers.EntityPlacementManager;
import kuroyale.mainpack.challengeHelpers.ChallengeValidator;
import kuroyale.mainpack.managers.AchievementManager;
import kuroyale.mainpack.managers.ArenaSetupManager;
import kuroyale.mainpack.managers.GameLoopManager;
import kuroyale.mainpack.managers.DualPlayerStateManager;
import kuroyale.mainpack.managers.EconomyManager;
import kuroyale.mainpack.managers.ComboManager;
import kuroyale.mainpack.managers.SpellRangeIndicatorManager;
import kuroyale.mainpack.models.Challenge;
import kuroyale.mainpack.models.GameMode;
import kuroyale.mainpack.models.PlayerProfile;
import kuroyale.deckpack.Deck;
import kuroyale.mainpack.network.NetworkManager;
import kuroyale.mainpack.network.NetworkBattleManager;
import kuroyale.mainpack.network.NetworkMessage;
import kuroyale.deckpack.DeckManager;
import javafx.scene.layout.VBox;

public class GameEngine {
    @FXML
    private GridPane arenaGrid;
    @FXML
    private Pane entityLayer;
    @FXML
    private Pane staticLayer;
    @FXML
    private AnchorPane cardSlot0;
    @FXML
    private AnchorPane cardSlot1;
    @FXML
    private AnchorPane cardSlot2;
    @FXML
    private AnchorPane cardSlot3;
    @FXML
    private Label card1CostLabel;
    @FXML
    private Label card2CostLabel;
    @FXML
    private Label card3CostLabel;
    @FXML
    private Label card4CostLabel;
    @FXML
    private Label gameTimerLabel;
    @FXML
    private Label elixirCountLabel;
    @FXML
    private ProgressBar elixirProgressBar;
    @FXML
    private PointsCounter pointsCounter;
    @FXML
    private Button pauseButton;
    @FXML
    private Button speedButton;

    // PvP mode UI elements (Player 2)
    @FXML
    private VBox player2CardContainer;
    @FXML
    private AnchorPane cardSlotP2_0;
    @FXML
    private AnchorPane cardSlotP2_1;
    @FXML
    private AnchorPane cardSlotP2_2;
    @FXML
    private AnchorPane cardSlotP2_3;
    @FXML
    private Label card1CostLabelP2;
    @FXML
    private Label card2CostLabelP2;
    @FXML
    private Label card3CostLabelP2;
    @FXML
    private Label card4CostLabelP2;
    @FXML
    private AnchorPane player2ElixirContainer;
    @FXML
    private ProgressBar elixirProgressBarP2;
    @FXML
    private Label elixirCountLabelP2;
    @FXML
    private Label player1Label;
    @FXML
    private Label player2Label;

    private ArenaMap arenaMap = new ArenaMap();

    // Game mode tracking
    private static GameMode currentGameMode = GameMode.SINGLE_PLAYER_AI;
    private static Deck player1Deck;
    private static Deck player2Deck;

    // PvP managers (null in single-player mode)
    private DualPlayerStateManager dualPlayerStateManager;
    private CardManager cardManagerP2;

    private final int rows = ArenaMap.getRows();
    private final int cols = ArenaMap.getCols();
    private final int tileSize = 32;

    private final double ENTITY_UPDATE_INTERVAL = 0.1; // Update entities every 0.1 seconds

    // Manager classes - each handles a specific responsibility
    private EntityRenderer entityRenderer;
    private CombatManager combatManager;
    private GameStateManager gameStateManager;
    private CardManager cardManager;
    private SpellSystem spellSystem;
    private EntityUpdater entityUpdater;

    // New managers
    private TowerManager towerManager;
    private SceneNavigationManager sceneNavigationManager;
    private VictoryConditionManager victoryConditionManager;
    private EntityLifecycleManager entityLifecycleManager;
    private EntityPlacementManager entityPlacementManager;
    private ArenaSetupManager arenaSetupManager;
    private GameLoopManager gameLoopManager;
    private SpellRangeIndicatorManager spellRangeIndicatorManager;
    private QuestManager questManager;
    private AchievementManager achievementManager;
    private ChallengeManager challengeManager;
    private ComboManager comboManager;
    private static Challenge.ChallengeType activeChallengeType = null;

    private SimpleAI aiOpponent;

    private int kingjester = 0;

    public static void main(String[] args) {
        UIManager.launch(UIManager.class, args);
    }

    // Static methods for setting game mode and decks (called from
    // PvPDeckSelectionController)
    public static void setGameMode(GameMode mode) {
        currentGameMode = mode;
    }

    public static GameMode getGameMode() {
        return currentGameMode;
    }

    public static void setPlayer1Deck(Deck deck) {
        player1Deck = deck;
    }

    public static void setPlayer2Deck(Deck deck) {
        player2Deck = deck;
    }

    @FXML
    private void initialize() {
        entityLayer.setPrefSize(cols * tileSize, rows * tileSize);
        staticLayer.setPrefSize(cols * tileSize, rows * tileSize);

        pointsCounter = new PointsCounter();
        pointsCounter.setLayoutX(((cols * tileSize) / 2) - 55);
        pointsCounter.setLayoutY(5);

        // Clip images for Player 1 cards
        clipImage(getImageFromPane(cardSlot0), 6);
        clipImage(getImageFromPane(cardSlot1), 6);
        clipImage(getImageFromPane(cardSlot2), 6);
        clipImage(getImageFromPane(cardSlot3), 6);

        // Determine game mode
        boolean isNetworkMode = NetworkManager.getInstance().isConnected();
        boolean isPvPMode = (currentGameMode == GameMode.LOCAL_PVP) || isNetworkMode;
        boolean isClient = false;
        if (isNetworkMode) {
            isClient = !NetworkManager.getInstance().isHost();
        }

        // Disable pause and speed buttons for network client
        if (isClient) {
            if (pauseButton != null) {
                pauseButton.setDisable(true);
            }
            if (speedButton != null) {
                speedButton.setDisable(true);
            }
        }

        // Setup UI visibility based on mode
        setupUIForGameMode(isPvPMode, isNetworkMode);

        // Clip images for Player 2 cards if PvP mode
        if (isPvPMode && cardSlotP2_0 != null) {
            clipImage(getImageFromPane(cardSlotP2_0), 6);
            clipImage(getImageFromPane(cardSlotP2_1), 6);
            clipImage(getImageFromPane(cardSlotP2_2), 6);
            clipImage(getImageFromPane(cardSlotP2_3), 6);
        }

        // Initialize core manager classes first
        entityRenderer = new EntityRenderer(arenaMap, entityLayer, staticLayer, pointsCounter, rows, cols, tileSize);
        combatManager = new CombatManager(ENTITY_UPDATE_INTERVAL);
        spellSystem = new SpellSystem(arenaMap, combatManager, rows, cols);
        entityUpdater = new EntityUpdater(arenaMap, combatManager, entityRenderer, rows, cols, ENTITY_UPDATE_INTERVAL);

        // Initialize state and card managers based on mode
        if (isNetworkMode) {
            // Network mode: use DualPlayerStateManager (both players need elixir tracking)
            dualPlayerStateManager = new DualPlayerStateManager(
                    elixirCountLabel, elixirProgressBar,
                    elixirCountLabelP2, elixirProgressBarP2);
            // Create dummy GameStateManager for compatibility
            gameStateManager = new GameStateManager(gameTimerLabel, elixirCountLabel, elixirProgressBar);

            // Network mode: only one card manager (each player sees only their deck)
            cardManager = new CardManager(cardSlot0, cardSlot1, cardSlot2, cardSlot3,
                    card1CostLabel, card2CostLabel, card3CostLabel, card4CostLabel);
            // Set elixir manager for EntityUpdater
            entityUpdater.setDualPlayerStateManager(dualPlayerStateManager);

            // Load deck based on whether we're host or client
            NetworkManager networkManager = NetworkManager.getInstance();
            DeckManager deckManager = new DeckManager();
            String deckName;

            if (networkManager.isHost()) {
                // Host (Player 1) loads their deck
                deckName = networkManager.getPlayer1Deck();
            } else {
                // Client (Player 2) loads their deck
                deckName = networkManager.getPlayer2Deck();
            }

            if (deckName != null && !deckName.isEmpty()) {
                Deck deck = deckManager.loadDeck(deckName);
                if (deck != null) {
                    cardManager.loadDeckForPlayer(deck);
                } else {
                    System.err.println("Warning: Could not load deck: " + deckName);
                    cardManager.loadDeck(); // Fallback
                }
            } else {
                System.err.println("Warning: No deck name in network mode, using default");
                cardManager.loadDeck(); // Fallback
            }

            // Hide Player 2 card container in network mode (each player only sees their own
            // deck)
            if (player2CardContainer != null) {
                player2CardContainer.setVisible(false);
                player2CardContainer.setManaged(false);
            }

            // In network mode, show the appropriate elixir bar based on whether we're host
            // or client
            if (networkManager.isHost()) {
                // Host (Player 1): show player 1 elixir bar, hide player 2
                if (player2ElixirContainer != null) {
                    player2ElixirContainer.setVisible(false);
                    player2ElixirContainer.setManaged(false);
                }
            } else {
                // Client (Player 2): hide player 1 elixir bar, show player 2 on the left side
                if (elixirCountLabel != null) {
                    elixirCountLabel.setVisible(false);
                    elixirCountLabel.setManaged(false);
                }
                if (elixirProgressBar != null) {
                    elixirProgressBar.setVisible(false);
                    elixirProgressBar.setManaged(false);
                }
                // Move player 2 elixir container to the left side for client
                if (player2ElixirContainer != null) {
                    // Remove right anchor and set left anchor to position it on the left
                    javafx.scene.layout.AnchorPane.clearConstraints(player2ElixirContainer);
                    javafx.scene.layout.AnchorPane.setLeftAnchor(player2ElixirContainer, 10.0);
                    javafx.scene.layout.AnchorPane.setTopAnchor(player2ElixirContainer, 90.0);
                    javafx.scene.layout.AnchorPane.setBottomAnchor(player2ElixirContainer, 90.0);
                }
            }

        } else if (isPvPMode) {
            // Local PvP mode: use DualPlayerStateManager
            dualPlayerStateManager = new DualPlayerStateManager(
                    elixirCountLabel, elixirProgressBar,
                    elixirCountLabelP2, elixirProgressBarP2);
            // Create dummy GameStateManager for compatibility (not used in PvP)
            gameStateManager = new GameStateManager(gameTimerLabel, elixirCountLabel, elixirProgressBar);

            // Initialize both card managers
            cardManager = new CardManager(cardSlot0, cardSlot1, cardSlot2, cardSlot3,
                    card1CostLabel, card2CostLabel, card3CostLabel, card4CostLabel);
            cardManagerP2 = new CardManager(cardSlotP2_0, cardSlotP2_1, cardSlotP2_2, cardSlotP2_3,
                    card1CostLabelP2, card2CostLabelP2, card3CostLabelP2, card4CostLabelP2);
            // Set elixir manager for EntityUpdater
            entityUpdater.setDualPlayerStateManager(dualPlayerStateManager);
            // Load decks for both players
            if (player1Deck != null) {
                cardManager.loadDeckForPlayer(player1Deck);
            } else {
                // Fallback: use numbered deck system (Deck1)
                Deck defaultDeck1 = DeckManager.loadDeckByNumber(1);
                if (defaultDeck1 != null) {
                    cardManager.loadDeckForPlayer(defaultDeck1);
                } else {
                    cardManager.loadDeck(); // Last resort: use current deck
                    System.err.println("Warning: Player 1 deck not set and Deck1 not found, using current deck");
                }
            }
            if (player2Deck != null) {
                cardManagerP2.loadDeckForPlayer(player2Deck);
            } else {
                // Fallback: use numbered deck system (try Deck2, then Deck1)
                Deck defaultDeck2 = DeckManager.loadDeckByNumber(2);
                if (defaultDeck2 != null) {
                    cardManagerP2.loadDeckForPlayer(defaultDeck2);
                    System.out.println("Player 2: Using Deck2 as fallback");
                } else {
                    // Try Deck1 if Deck2 doesn't exist
                    Deck defaultDeck1 = DeckManager.loadDeckByNumber(1);
                    if (defaultDeck1 != null) {
                        cardManagerP2.loadDeckForPlayer(defaultDeck1);
                        System.out.println("Player 2: Using Deck1 as fallback (Deck2 not found)");
                    } else {
                        cardManagerP2.loadDeck(); // Last resort: use current deck
                        System.err.println(
                                "Warning: Player 2 deck not set and no numbered decks found, using current deck");
                    }
                }
            }
        } else {
            // Single-player mode: use GameStateManager (existing code)
            gameStateManager = new GameStateManager(gameTimerLabel, elixirCountLabel, elixirProgressBar);
            cardManager = new CardManager(cardSlot0, cardSlot1, cardSlot2, cardSlot3,
                    card1CostLabel, card2CostLabel, card3CostLabel, card4CostLabel);
            // Set elixir manager for EntityUpdater
            entityUpdater.setGameStateManager(gameStateManager);
            // Use slot-based deck system: load the selected deck number (defaults to Deck1)
            int selectedDeckNumber = DeckManager.getSelectedDeckNumber();
            Deck selectedDeck = DeckManager.loadDeckByNumber(selectedDeckNumber);

            if (selectedDeck != null) {
                cardManager.loadDeckForPlayer(selectedDeck);
            } else {
                // Fallback: try Deck1 if selected deck doesn't exist
                Deck defaultDeck1 = DeckManager.loadDeckByNumber(1);
                if (defaultDeck1 != null) {
                    cardManager.loadDeckForPlayer(defaultDeck1);
                } else {
                    // Last resort: use old loadDeck() method
                    cardManager.loadDeck();
                    System.err.println("Warning: No numbered decks found, using current deck");
                }
            }
        }

        // Initialize persistence and economy for victory rewards
        PersistenceManager persistenceManager = new PersistenceManager();
        PlayerProfile profile = persistenceManager.loadPlayerProfile();
        EconomyManager economyManager = new EconomyManager(profile.getTotalGold(), persistenceManager);

        // Initialize new managers (careful with dependencies)
        sceneNavigationManager = new SceneNavigationManager(arenaGrid, gameStateManager);
        towerManager = new TowerManager(arenaMap, pointsCounter, rows, cols);
        towerManager.setEntityRenderer(entityRenderer);
        victoryConditionManager = new VictoryConditionManager(arenaMap, pointsCounter, rows, cols,
                sceneNavigationManager);
        victoryConditionManager.setEconomyManager(economyManager);
        victoryConditionManager.setGameMode(currentGameMode); // Set game mode for victory messages
        entityLifecycleManager = new EntityLifecycleManager(arenaMap, combatManager, entityRenderer, entityUpdater,
                towerManager, rows, cols);
        questManager = new QuestManager();
        achievementManager = new AchievementManager();
        challengeManager = new ChallengeManager(profile);

        // EntityPlacementManager needs to know about dual player state if PvP or
        // network
        if (isPvPMode || isNetworkMode) {
            // Network mode: pass null for cardManagerP2 (only one deck)
            CardManager p2Manager = isNetworkMode ? null : cardManagerP2;
            entityPlacementManager = new EntityPlacementManager(arenaMap, dualPlayerStateManager, cardManager,
                    p2Manager, entityRenderer, spellSystem, rows, cols);
        } else {
            entityPlacementManager = new EntityPlacementManager(arenaMap, gameStateManager, cardManager, entityRenderer,
                    spellSystem, rows, cols);
        }

        // Set network mode flag in EntityPlacementManager if network mode
        if (isNetworkMode) {
            NetworkBattleManager battleManager = NetworkBattleManager.getInstance();
            entityPlacementManager.setNetworkBattleManager(battleManager, isClient);

            // CRITICAL: Set client flag in renderer for coordinate transformation
            if (isClient) {
                entityRenderer.setIsClient(true);
                System.out.println("Client: EntityRenderer.setIsClient(true) set for coordinate transformation");
            }

            // Set up network message handlers
            setupNetworkHandlers(battleManager, entityPlacementManager);
        }

        // Load quest/achievement data from profile
        entityLifecycleManager.setQuestManager(questManager);
        questManager.setDailyQuests(profile.getDailyQuests());
        questManager.setLastResetTimestamp(profile.getLastQuestResetTimestamp());
        achievementManager.setAchievements(profile.getAchievements());
        questManager.initializeDailyQuests();
        arenaSetupManager = new ArenaSetupManager(arenaMap, arenaGrid, rows, cols, tileSize, entityRenderer);

        // Initialize spell range indicator manager (uses entityLayer for overlay)
        spellRangeIndicatorManager = new SpellRangeIndicatorManager(entityLayer, tileSize);
        arenaSetupManager.setSpellRangeIndicatorManager(spellRangeIndicatorManager);

        // GameLoopManager needs to use appropriate state manager
        if (isPvPMode || isNetworkMode) {
            gameLoopManager = new GameLoopManager(dualPlayerStateManager, entityLifecycleManager, entityRenderer,
                    victoryConditionManager, gameTimerLabel, ENTITY_UPDATE_INTERVAL);
        } else {
            gameLoopManager = new GameLoopManager(gameStateManager, entityLifecycleManager, entityRenderer,
                    victoryConditionManager, gameTimerLabel, ENTITY_UPDATE_INTERVAL);
        }

        // Set network battle manager for synchronization (both host and client)
        if (isNetworkMode) {
            NetworkBattleManager battleManager = NetworkBattleManager.getInstance();
            gameLoopManager.setNetworkBattleManager(battleManager, arenaMap, towerManager);
        }

        // Set callback for handling tower destroy results
        entityLifecycleManager.setTowerDestroyCallback(this::handleTowerDestroyResult);

        arenaSetupManager.fillArenaGrid(entityPlacementManager);
        arenaSetupManager.loadDefaultArenaIfExists();
        entityRenderer.renderStaticObjects();
        javafx.application.Platform.runLater(() -> {
            if (arenaGrid != null && arenaGrid.getScene() != null) {
                javafx.scene.Node root = arenaGrid.getScene().getRoot();
                if (root instanceof javafx.scene.layout.AnchorPane) {
                    NotificationManager notificationManager = new NotificationManager(
                            (javafx.scene.layout.AnchorPane) root);
                    victoryConditionManager.setNotificationManager(notificationManager);
                }
                // Initialize ComboManager (needs root pane from scene)
                if (root instanceof Pane && comboManager == null) {
                    comboManager = new ComboManager(arenaMap, entityRenderer, (Pane) root, entityLayer);
                    // Wire up ComboManager
                    entityPlacementManager.setComboManager(comboManager);
                    entityUpdater.setComboManager(comboManager);
                    Entity.setComboManager(comboManager);
                    entityLifecycleManager.setComboManager(comboManager);
                    victoryConditionManager.setComboManager(comboManager);
                    // Reset for new match
                    comboManager.resetForNewMatch();
                }
            }
        });

        // Initialize the AI opponent before starting game loop (only in single-player
        // mode)
        if (!isPvPMode) {
            String difficulty = UIManager.getSelectedDifficulty();
            if ("Simple".equals(difficulty)) {
                aiOpponent = new SimpleAI(arenaMap, this);
                gameLoopManager.setAIOpponent(aiOpponent);
            }
        }

        activeChallengeType = ChallengeController.getSelectedChallengeType();
        if (activeChallengeType != null) {
            // Challenge validation was already done before switching to battle scene
            challengeManager.startChallenge(activeChallengeType);
        }

        victoryConditionManager.setQuestManager(questManager);
        victoryConditionManager.setAchievementManager(achievementManager);
        victoryConditionManager.setPersistenceManager(persistenceManager);
        victoryConditionManager.setChallengeManager(challengeManager);
        if (!isPvPMode) {
            victoryConditionManager.setGameStateManager(gameStateManager);
        }
        // Initialize match tracking for challenges
        victoryConditionManager.initializeMatchTracking();

        // Set ChallengeManager on CardManager for cost display (only if challenge is
        // active)
        if (activeChallengeType != null) {
            cardManager.setChallengeManager(challengeManager);
            if (isPvPMode && cardManagerP2 != null) {
                cardManagerP2.setChallengeManager(challengeManager);
            }
        } else {
            // Clear challenge manager for normal battles
            cardManager.setChallengeManager(null);
            if (isPvPMode && cardManagerP2 != null) {
                cardManagerP2.setChallengeManager(null);
            }
        }
        entityPlacementManager.setQuestManager(questManager);
        entityPlacementManager.setPersistenceManager(persistenceManager);
        entityPlacementManager.setAchievementManager(achievementManager);
        entityPlacementManager.setChallengeManager(challengeManager);
        spellSystem.setQuestManager(questManager);
        spellSystem.setPersistenceManager(persistenceManager);
        spellSystem.setAchievementManager(achievementManager);
        entityLifecycleManager.setQuestManager(questManager);
        entityLifecycleManager.setPersistenceManager(persistenceManager);
        entityLifecycleManager.setAchievementManager(achievementManager);
        economyManager.setAchievementManager(achievementManager);
        // Start new match tracking
        questManager.startNewMatch();
        gameLoopManager.startGameLoop();

        // Verify all cards are draggable after initialization
        verifyAllCardsDraggable();
    }

    /**
     * Set up network message handlers for battle synchronization.
     */
    private void setupNetworkHandlers(NetworkBattleManager battleManager, EntityPlacementManager placementManager) {
        boolean isHost = NetworkManager.getInstance().isHost();

        if (isHost) {
            // Host: Handle card placement requests from client
            battleManager.setOnCardPlacementReceived(msg -> {
                if (msg.getType() == NetworkMessage.MessageType.CARD_PLACEMENT_REQUEST) {
                    // Parse message: "requestId|cardId|row|col"
                    String[] parts = msg.getData().split("\\|");
                    if (parts.length >= 4) {
                        int requestId = Integer.parseInt(parts[0]);
                        int cardID = Integer.parseInt(parts[1]);
                        int row = Integer.parseInt(parts[2]);
                        int col = Integer.parseInt(parts[3]);
                        System.out.println("Host: CARD_PLACEMENT_REQUEST received - requestId=" + requestId +
                                ", player=2, card=" + cardID +
                                ", requested=(" + row + ", " + col + ")");
                        placementManager.handleNetworkCardPlacement(cardID, row, col, 2, requestId);
                    }
                } else if (msg.getType() == NetworkMessage.MessageType.SPELL_CAST_REQUEST) {
                    // Parse message: "requestId|spellId|row|col"
                    String[] parts = msg.getData().split("\\|");
                    if (parts.length >= 4) {
                        int requestId = Integer.parseInt(parts[0]);
                        int spellId = Integer.parseInt(parts[1]);
                        int row = Integer.parseInt(parts[2]);
                        int col = Integer.parseInt(parts[3]);
                        System.out.println("Host: SPELL_CAST_REQUEST received - requestId=" + requestId +
                                ", player=2, spell=" + spellId +
                                ", target=(" + row + ", " + col + ")");
                        // Use unified placement method for spells
                        placementManager.placeCardAuthoritative(2, spellId, row, col, requestId, false);
                    }
                }
            });
        } else {
            // Client: Handle entity spawns from host
            battleManager.setOnEntitySpawnReceived(msg -> {
                // Handle placement rejection
                if (msg.getType() == NetworkMessage.MessageType.PLACEMENT_REJECTED) {
                    System.out.println("Client: Placement rejected: " + msg.getData());
                    // TODO: Refund elixir or show error message
                    return;
                }

                // Parse entity spawn message: "entityId|cardId|row|col|owner|hp|maxHp"
                String[] parts = msg.getData().split("\\|");
                if (parts.length >= 7) {
                    long entityId = Long.parseLong(parts[0]);
                    int cardID = Integer.parseInt(parts[1]);
                    int absoluteRow = Integer.parseInt(parts[2]);
                    int absoluteCol = Integer.parseInt(parts[3]);
                    int ownerId = Integer.parseInt(parts[4]);
                    double hp = Double.parseDouble(parts[5]);
                    double maxHp = Double.parseDouble(parts[6]);

                    // Spawn entity from host (could be host's own entity or client's confirmed
                    // placement)
                    kuroyale.mainpack.network.EntityRegistry registry = battleManager.getEntityRegistry();
                    AliveEntity existingEntity = registry.getEntity(entityId);

                    if (existingEntity != null) {
                        // Entity already exists (shouldn't happen, but update if it does)
                        existingEntity.setHP(hp);
                        existingEntity.setPosition(absoluteRow, absoluteCol);
                        System.out.println("Client: Updated existing entity ID=" + entityId);
                    } else {
                        // Spawn new entity from host
                        boolean isPlayer = (ownerId == 1);
                        AliveEntity entity;
                        if (cardID <= 15) {
                            entity = new kuroyale.entitiypack.subclasses.UnitEntity(
                                    ((kuroyale.cardpack.subclasses.UnitCard) kuroyale.cardpack.CardFactory
                                            .createCard(cardID)),
                                    isPlayer);
                        } else {
                            entity = new kuroyale.entitiypack.subclasses.BuildingEntity(
                                    ((kuroyale.cardpack.subclasses.BuildingCard) kuroyale.cardpack.CardFactory
                                            .createCard(cardID)),
                                    isPlayer);
                        }

                        entity.setEntityId(entityId);
                        entity.setHP(hp);

                        // Use EXACT authoritative position from host (no search, host already validated
                        // and placed)
                        // If cell is occupied, remove old occupant (reconciliation)
                        AliveEntity existingEntityAtPos = arenaMap.getEntity(absoluteRow, absoluteCol);
                        if (existingEntityAtPos != null && existingEntityAtPos != entity) {
                            // Remove old occupant
                            arenaMap.removeEntity(existingEntityAtPos);
                            arenaMap.setEntity(absoluteRow, absoluteCol, null);
                            arenaMap.clearObject(absoluteRow, absoluteCol);
                            System.out.println("Client: ENTITY_SPAWN - Removed old occupant at (" + absoluteRow + ", "
                                    + absoluteCol + ")");
                        }

                        // Place at exact host-provided position (authoritative)
                        boolean placementOK = arenaMap.placeObject(absoluteRow, absoluteCol,
                                kuroyale.arenapack.ArenaObjectType.ENTITY);

                        if (placementOK) {
                            entity.setPosition(absoluteRow, absoluteCol);
                            arenaMap.setEntity(absoluteRow, absoluteCol, entity);
                            arenaMap.addEntity(entity);

                            // Register entity
                            registry.registerEntity(entityId, entity);

                            entityRenderer.ensureEntityNode(entity, entity.getCard());
                            entityRenderer.setEntityDirty(true);
                            System.out.println("Client: dirty set TRUE by network msg ENTITY_SPAWN");

                            // Cycle card slot on client when entity spawns (host already cycled)
                            int slotIndex = cardManager.findCardSlotIndex(cardID);
                            if (slotIndex >= 0) {
                                cardManager.cycleCardInSlot(slotIndex);
                            }

                            System.out.println("Client: ENTITY_SPAWN - ID=" + entityId + ", card=" + cardID +
                                    ", owner=" + ownerId + ", position=(" + absoluteRow + ", " + absoluteCol + ")");
                        } else {
                            System.out.println("Client: ENTITY_SPAWN FAILED - ID=" + entityId +
                                    ", card=" + cardID + ", position=(" + absoluteRow + ", " + absoluteCol +
                                    ") - placement failed (cell blocked)");
                        }
                    }
                }
            });

            // Client: Handle entity state updates from host
            battleManager.setOnEntityUpdateReceived(msg -> {
                if (msg.getType() == NetworkMessage.MessageType.ENTITY_UPDATE) {
                    // Parse: "entityId|hp|row|col"
                    String[] parts = msg.getData().split("\\|");
                    if (parts.length >= 4) {
                        long entityId = Long.parseLong(parts[0]);
                        double hp = Double.parseDouble(parts[1]);
                        int row = Integer.parseInt(parts[2]);
                        int col = Integer.parseInt(parts[3]);

                        kuroyale.mainpack.network.EntityRegistry registry = battleManager.getEntityRegistry();
                        AliveEntity entity = registry.getEntity(entityId);

                        if (entity != null) {
                            // Update entity state
                            entity.setHP(hp);

                            // CRITICAL: Find old position and clear it
                            int oldRow = -1, oldCol = -1;
                            for (int r = 0; r < arenaMap.getRows(); r++) {
                                for (int c = 0; c < arenaMap.getCols(); c++) {
                                    if (arenaMap.getEntity(r, c) == entity) {
                                        oldRow = r;
                                        oldCol = c;
                                        break;
                                    }
                                }
                                if (oldRow >= 0)
                                    break;
                            }

                            // Update position if changed - MUST clear old cell first
                            if (oldRow >= 0 && (oldRow != row || oldCol != col)) {
                                // Clear old cell
                                arenaMap.setEntity(oldRow, oldCol, null);
                                arenaMap.clearObject(oldRow, oldCol);

                                // Set new cell
                                arenaMap.placeObject(row, col, kuroyale.arenapack.ArenaObjectType.ENTITY);
                                arenaMap.setEntity(row, col, entity);
                            } else if (oldRow < 0) {
                                // Entity not in map yet - place it
                                arenaMap.placeObject(row, col, kuroyale.arenapack.ArenaObjectType.ENTITY);
                                arenaMap.setEntity(row, col, entity);
                            }

                            entity.setPosition(row, col);
                            entityRenderer.setEntityDirty(true);
                            System.out.println("Client: dirty set TRUE by network msg ENTITY_UPDATE");
                        }
                    }
                } else if (msg.getType() == NetworkMessage.MessageType.ENTITY_DEATH) {
                    // Parse: "entityId"
                    long entityId = Long.parseLong(msg.getData());
                    kuroyale.mainpack.network.EntityRegistry registry = battleManager.getEntityRegistry();
                    AliveEntity entity = registry.getEntity(entityId);

                    if (entity != null) {
                        // Remove entity
                        entityLifecycleManager.removeDeadEntity(entity);
                        registry.removeEntity(entityId);
                        entityRenderer.setEntityDirty(true);
                        System.out.println("Client: dirty set TRUE by network msg ENTITY_DEATH");
                        System.out.println("Client: Entity died: ID=" + entityId);
                    }
                }
            });

            // Client: Handle tower updates from host
            battleManager.setOnTowerUpdateReceived(msg -> {
                if (msg.getType() == NetworkMessage.MessageType.TOWER_UPDATE) {
                    // Parse: "TowerId|hp|maxHp"
                    String[] parts = msg.getData().split("\\|");
                    if (parts.length >= 3) {
                        try {
                            kuroyale.mainpack.network.TowerId towerId = kuroyale.mainpack.network.TowerId
                                    .valueOf(parts[0]);
                            double hp = Double.parseDouble(parts[1]);
                            double maxHp = Double.parseDouble(parts[2]);

                            // Update tower health using TowerId
                            towerManager.syncTowerHealthFromNetwork(towerId, hp);

                            // If HP <= 0, tower is destroyed (syncTowerHealthFromNetwork handles removal)
                            if (hp <= 0) {
                                System.out.println(
                                        "Client: TOWER_UPDATE - Tower " + towerId + " destroyed (HP=" + hp + ")");
                            }

                            entityRenderer.setEntityDirty(true);
                            entityRenderer.setStaticDirty(true);
                            System.out.println("Client: dirty set TRUE by network msg TOWER_UPDATE");
                            System.out
                                    .println("Client: TOWER_UPDATE received - " + towerId + " HP=" + hp + "/" + maxHp);
                        } catch (IllegalArgumentException e) {
                            // Fallback to legacy string format
                            String towerIdStr = parts[0];
                            double hp = Double.parseDouble(parts[1]);
                            towerManager.syncTowerHealthFromNetwork(towerIdStr, hp);
                            entityRenderer.setEntityDirty(true);
                            System.out.println("Client: TOWER_UPDATE (legacy) - " + towerIdStr + " HP=" + hp);
                        }
                    }
                } else if (msg.getType() == NetworkMessage.MessageType.TOWER_DESTROY) {
                    // Parse: "TowerId" (enum name)
                    try {
                        kuroyale.mainpack.network.TowerId towerId = kuroyale.mainpack.network.TowerId
                                .valueOf(msg.getData());
                        System.out.println("Client: TOWER_DESTROY received - " + towerId);

                        // Find tower BEFORE removal (getTowerById will return null after removal)
                        kuroyale.entitiypack.subclasses.TowerEntity tower = towerManager.getTowerById(towerId);

                        // Remove tower using TowerManager method (clears arenaMap)
                        towerManager.removeTowerFromNetwork(towerId);

                        // Remove from entity registry if it exists
                        if (tower != null) {
                            kuroyale.mainpack.network.EntityRegistry registry = battleManager.getEntityRegistry();
                            if (tower.getEntityId() > 0) {
                                registry.removeEntity(tower.getEntityId());
                            }

                            // Remove health bar node if it exists
                            entityRenderer.removeEntitySprite(tower);
                        }

                        // Mark renderer dirty to remove health bar and sprite (also done in
                        // removeTowerFromNetwork, but ensure it's set)
                        entityRenderer.setEntityDirty(true);
                        entityRenderer.setStaticDirty(true);

                        System.out.println("Client: TOWER_DESTROY - Tower " + towerId + " removed, visuals cleared");
                    } catch (IllegalArgumentException e) {
                        System.out.println("Client: TOWER_DESTROY - Invalid TowerId: " + msg.getData());
                    }
                }
            });

            // Client: Handle elixir updates from host
            battleManager.setOnGameStateReceived(msg -> {
                if (msg.getType() == NetworkMessage.MessageType.ELIXIR_UPDATE) {
                    // Parse: "player1Elixir|player2Elixir"
                    String[] parts = msg.getData().split("\\|");
                    if (parts.length >= 2) {
                        double player1Elixir = Double.parseDouble(parts[0]);
                        double player2Elixir = Double.parseDouble(parts[1]);

                        // Update elixir (client is player 2)
                        if (dualPlayerStateManager != null) {
                            // Set elixir directly (don't generate locally)
                            dualPlayerStateManager.setElixir(1, player1Elixir);
                            dualPlayerStateManager.setElixir(2, player2Elixir);
                        }
                    }
                }
            });

            // Client: Handle game end
            battleManager.setOnGameEndReceived(msg -> {
                if (msg.getType() == NetworkMessage.MessageType.GAME_END) {
                    // Parse: "winner|reason"
                    String[] parts = msg.getData().split("\\|");
                    if (parts.length >= 2) {
                        int winner = Integer.parseInt(parts[0]);
                        String reason = parts[1];

                        // Stop game loop
                        if (gameLoopManager != null && gameLoopManager.getGameLoop() != null) {
                            gameLoopManager.getGameLoop().stop();
                        }

                        // Determine if client won (client is player 2)
                        boolean clientWon = (winner == 2);
                        boolean isDraw = reason.contains("DRAW") || reason.contains("draw");

                        System.out.println("Client: Game ended - Winner: " + winner + ", Reason: " + reason
                                + ", Client won: " + clientWon);

                        // Show victory/defeat screen using SceneNavigationManager
                        if (sceneNavigationManager != null) {
                            sceneNavigationManager.showGameEndScreen(clientWon, isDraw,
                                    gameLoopManager != null ? gameLoopManager.getGameLoop() : null,
                                    GameMode.NETWORK_MULTIPLAYER, comboManager);
                        }
                    }
                }
            });
        }
    }

    private void setupUIForGameMode(boolean isPvPMode, boolean isNetworkMode) {
        if (isPvPMode) {
            // Show PvP elements
            if (player2CardContainer != null) {
                player2CardContainer.setVisible(true);
                player2CardContainer.setManaged(true);
            }
            if (player2ElixirContainer != null) {
                player2ElixirContainer.setVisible(true);
                player2ElixirContainer.setManaged(true);
            }
            // Optional: show player labels
            if (player1Label != null)
                player1Label.setVisible(true);
            if (player2Label != null)
                player2Label.setVisible(true);
        } else {
            // Hide PvP elements (already hidden by default, but ensure)
            if (player2CardContainer != null) {
                player2CardContainer.setVisible(false);
                player2CardContainer.setManaged(false);
            }
            if (player2ElixirContainer != null) {
                player2ElixirContainer.setVisible(false);
                player2ElixirContainer.setManaged(false);
            }
            if (player1Label != null)
                player1Label.setVisible(false);
            if (player2Label != null)
                player2Label.setVisible(false);
        }
    }

    private void verifyAllCardsDraggable() {
        AnchorPane[] cardSlots = { cardSlot0, cardSlot1, cardSlot2, cardSlot3 };
        System.out.println("=== Verifying all cards are draggable ===");
        for (int i = 0; i < cardSlots.length; i++) {
            Pane innerPane = getInnerPaneFromSlot(cardSlots[i]);
            if (innerPane != null) {
                boolean hasHandler = innerPane.getOnDragDetected() != null;
                System.out.println("Slot " + i + " has drag handler: " + hasHandler);
                List<Card> currentHand = cardManager.getCurrentHand();
                if (i < currentHand.size()) {
                    System.out.println(
                            "  Card: " + currentHand.get(i).getName() + " (ID: " + currentHand.get(i).getId() + ")");
                }
            } else {
                System.out.println("Slot " + i + ": Inner pane not found!");
            }
        }
        System.out.println("=== End verification ===");
    }

    /** RENDER STUFF - moved to EntityRenderer **/

    /** ARENA LOGIC - moved to ArenaSetupManager **/

    private ImageView getImageFromPane(AnchorPane ap) {
        for (Node n : ap.getChildren()) {
            if (n instanceof Pane p) {
                return (ImageView) p.getChildren().get(1);
            }
        }
        return null;
    }

    private Pane getInnerPaneFromSlot(AnchorPane ap) {
        for (Node n : ap.getChildren()) {
            if (n instanceof Pane p) {
                return p;
            }
        }
        return null;
    }

    // Rendering methods moved to EntityRenderer
    // Arena loading moved to ArenaSetupManager

    /** SCENE LOGIC **/
    private void clipImage(ImageView img, int arch) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(2 * arch);
        clip.setArcHeight(2 * arch);
        clip.widthProperty().bind(img.fitWidthProperty());
        clip.heightProperty().bind(img.fitHeightProperty());
        img.setClip(clip);
    }

    /** GAMEPLAY - moved to managers **/

    // Public method for SimpleAI to call
    public void executeSpell(int spellCardID, int targetRow, int targetCol, boolean isPlayerSpell) {
        spellSystem.executeSpell(spellCardID, targetRow, targetCol, isPlayerSpell, entityRenderer);

        // Remove dead entities affected by spell
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                AliveEntity entity = arenaMap.getEntity(r, c);
                if (entity != null && entity.getHP() <= 0) {
                    TowerManager.TowerDestroyResult result = entityLifecycleManager.removeDeadEntity(entity);
                    handleTowerDestroyResult(result);
                }
            }
        }

        entityRenderer.setEntityDirty(true);
    }

    // Handle tower destruction result (king death logic)
    private void handleTowerDestroyResult(TowerManager.TowerDestroyResult result) {
        if (result.isGameEnd && result.isKing) {
            // First call to kingIsDown - kill all towers of that side
            java.util.List<TowerEntity> towersToKill = towerManager.getTowersToKillWhenKingDies(result.kingIsPlayer);
            for (TowerEntity tower : towersToKill) {
                tower.setHP(0);
                TowerManager.TowerDestroyResult innerResult = entityLifecycleManager.removeDeadEntity(tower);
                // Handle nested results (shouldn't happen, but preserve original logic)
                if (innerResult.isGameEnd) {
                    handleTowerDestroyResult(innerResult);
                }
                // Set points (original code did this in kingIsDown)
                if (tower.isPlayer()) {
                    pointsCounter.setEnemyPoints(3);
                } else {
                    pointsCounter.setOurPoints(3);
                }
            }

            // End the game
            if (kingjester == 15) {
                // Send game end message to client if in network mode
                NetworkManager netManager = NetworkManager.getInstance();
                if (netManager != null && netManager.isConnected() && netManager.isHost()) {
                    NetworkBattleManager battleManager = NetworkBattleManager.getInstance();
                    if (battleManager != null) {
                        int winner = result.playerWon ? 1 : 2; // Player 1 wins if result.playerWon is true
                        String reason = result.isKing ? "King destroyed" : "Game ended";
                        battleManager.sendGameEnd(winner, reason);
                    }
                }

                victoryConditionManager.endGame(result.playerWon, false, gameLoopManager.getGameLoop());
            } else {
                kingjester++;
            }

            // Second call to kingIsDown (original code does this twice)
            java.util.List<TowerEntity> towersToKillAgain = towerManager
                    .getTowersToKillWhenKingDies(result.kingIsPlayer);
            for (TowerEntity tower : towersToKillAgain) {
                tower.setHP(0);
                entityLifecycleManager.removeDeadEntity(tower);
                if (tower.isPlayer()) {
                    pointsCounter.setEnemyPoints(3);
                } else {
                    pointsCounter.setOurPoints(3);
                }
            }

            gameLoopManager.getGameLoop().pause();
            gameStateManager.setPaused(true);
        }
    }

    // Entity update methods moved to EntityUpdater

    // Card management methods moved to CardManager

    // Game state methods moved to GameStateManager

    @FXML
    private void btnBackClicked(ActionEvent event) throws IOException {
        sceneNavigationManager.switchToStartBattleScene(event);
        gameLoopManager.getGameLoop().pause();
        gameStateManager.setPaused(true);
    }

    @FXML
    private void btnPauseClicked(ActionEvent event) {
        sceneNavigationManager.handlePauseButton(event, gameLoopManager.getGameLoop());
    }

    @FXML
    private void btnSpeedClicked(ActionEvent event) {
        sceneNavigationManager.handleSpeedButton(event, gameLoopManager.getGameLoop());
    }
}