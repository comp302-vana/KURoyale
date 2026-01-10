package kuroyale.mainpack;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
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
import kuroyale.mainpack.models.Challenge;
import kuroyale.mainpack.models.GameMode;
import kuroyale.mainpack.models.PlayerProfile;
import kuroyale.deckpack.Deck;
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
    private QuestManager questManager;
    private AchievementManager achievementManager;
    private ChallengeManager challengeManager;
    private static Challenge.ChallengeType activeChallengeType = null;

    private SimpleAI aiOpponent;

    private int kingjester=0;

    public static void main(String[] args) {
        UIManager.launch(UIManager.class, args);
    }
    
    // Static methods for setting game mode and decks (called from PvPDeckSelectionController)
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
        pointsCounter.setLayoutX(((cols*tileSize)/2)-55);
        pointsCounter.setLayoutY(5);

        // Clip images for Player 1 cards
        clipImage(getImageFromPane(cardSlot0), 6);
        clipImage(getImageFromPane(cardSlot1), 6);
        clipImage(getImageFromPane(cardSlot2), 6);
        clipImage(getImageFromPane(cardSlot3), 6);

        // Determine game mode
        boolean isPvPMode = (currentGameMode == GameMode.LOCAL_PVP);
        
        // Setup UI visibility based on mode
        setupUIForGameMode(isPvPMode);
        
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
        if (isPvPMode) {
            // PvP mode: use DualPlayerStateManager
            dualPlayerStateManager = new DualPlayerStateManager(
                elixirCountLabel, elixirProgressBar,
                elixirCountLabelP2, elixirProgressBarP2
            );
            // Create dummy GameStateManager for compatibility (not used in PvP)
            gameStateManager = new GameStateManager(gameTimerLabel, elixirCountLabel, elixirProgressBar);
            
            // Initialize both card managers
            cardManager = new CardManager(cardSlot0, cardSlot1, cardSlot2, cardSlot3,
                                         card1CostLabel, card2CostLabel, card3CostLabel, card4CostLabel);
            cardManagerP2 = new CardManager(cardSlotP2_0, cardSlotP2_1, cardSlotP2_2, cardSlotP2_3,
                                           card1CostLabelP2, card2CostLabelP2, card3CostLabelP2, card4CostLabelP2);
            
            // Load decks for both players
            if (player1Deck != null) {
                cardManager.loadDeckForPlayer(player1Deck);
            } else {
                cardManager.loadDeck(); // Fallback to current deck
            }
            if (player2Deck != null) {
                cardManagerP2.loadDeckForPlayer(player2Deck);
            } else {
                System.err.println("Warning: Player 2 deck not set in PvP mode!");
            }
        } else {
            // Single-player mode: use GameStateManager (existing code)
            gameStateManager = new GameStateManager(gameTimerLabel, elixirCountLabel, elixirProgressBar);
            cardManager = new CardManager(cardSlot0, cardSlot1, cardSlot2, cardSlot3,
                                         card1CostLabel, card2CostLabel, card3CostLabel, card4CostLabel);
            cardManager.loadDeck();
        }

        // Initialize persistence and economy for victory rewards
        PersistenceManager persistenceManager = new PersistenceManager();
        PlayerProfile profile = persistenceManager.loadPlayerProfile();
        EconomyManager economyManager = new EconomyManager(profile.getTotalGold(), persistenceManager);
        
        // Initialize new managers (careful with dependencies)
        sceneNavigationManager = new SceneNavigationManager(arenaGrid, gameStateManager);
        towerManager = new TowerManager(arenaMap, pointsCounter, rows, cols);
        victoryConditionManager = new VictoryConditionManager(arenaMap, pointsCounter, rows, cols, sceneNavigationManager);
        victoryConditionManager.setEconomyManager(economyManager);
        victoryConditionManager.setGameMode(currentGameMode); // Set game mode for victory messages
        entityLifecycleManager = new EntityLifecycleManager(arenaMap, combatManager, entityRenderer, entityUpdater, towerManager, rows, cols);
        questManager = new QuestManager();
        achievementManager = new AchievementManager();
        challengeManager = new ChallengeManager(profile);

        // EntityPlacementManager needs to know about dual player state if PvP
        if (isPvPMode) {
            entityPlacementManager = new EntityPlacementManager(arenaMap, dualPlayerStateManager, cardManager, cardManagerP2, entityRenderer, spellSystem, rows, cols);
        } else {
            entityPlacementManager = new EntityPlacementManager(arenaMap, gameStateManager, cardManager, entityRenderer, spellSystem, rows, cols);
        }
        
        // Load quest/achievement data from profile
        entityLifecycleManager.setQuestManager(questManager);
        questManager.setDailyQuests(profile.getDailyQuests());
        questManager.setLastResetTimestamp(profile.getLastQuestResetTimestamp());
        achievementManager.setAchievements(profile.getAchievements());
        questManager.initializeDailyQuests();
        arenaSetupManager = new ArenaSetupManager(arenaMap, arenaGrid, rows, cols, tileSize, entityRenderer);
        
        // GameLoopManager needs to use appropriate state manager
        if (isPvPMode) {
            gameLoopManager = new GameLoopManager(dualPlayerStateManager, entityLifecycleManager, entityRenderer, victoryConditionManager, gameTimerLabel, ENTITY_UPDATE_INTERVAL);
        } else {
            gameLoopManager = new GameLoopManager(gameStateManager, entityLifecycleManager, entityRenderer, victoryConditionManager, gameTimerLabel, ENTITY_UPDATE_INTERVAL);
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
                    NotificationManager notificationManager = new NotificationManager((javafx.scene.layout.AnchorPane) root);
                    victoryConditionManager.setNotificationManager(notificationManager);
                }
            }
        });

        // Initialize the AI opponent before starting game loop (only in single-player mode)
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
        
        // Set ChallengeManager on CardManager for cost display (only if challenge is active)
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
    
    private void setupUIForGameMode(boolean isPvPMode) {
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
            if (player1Label != null) player1Label.setVisible(true);
            if (player2Label != null) player2Label.setVisible(true);
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
            if (player1Label != null) player1Label.setVisible(false);
            if (player2Label != null) player2Label.setVisible(false);
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
            if (kingjester==15) {
                victoryConditionManager.endGame(result.playerWon, false, gameLoopManager.getGameLoop());
            }
            else{
                kingjester++;
            }
            
            // Second call to kingIsDown (original code does this twice)
            java.util.List<TowerEntity> towersToKillAgain = towerManager.getTowersToKillWhenKingDies(result.kingIsPlayer);
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