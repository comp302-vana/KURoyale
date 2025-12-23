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
import kuroyale.mainpack.managers.CardManager;
import kuroyale.mainpack.managers.SpellSystem;
import kuroyale.mainpack.managers.EntityUpdater;
import kuroyale.mainpack.managers.TowerManager;
import kuroyale.mainpack.managers.SceneNavigationManager;
import kuroyale.mainpack.managers.VictoryConditionManager;
import kuroyale.mainpack.managers.EntityLifecycleManager;
import kuroyale.mainpack.managers.EntityPlacementManager;
import kuroyale.mainpack.managers.ArenaSetupManager;
import kuroyale.mainpack.managers.GameLoopManager;

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

    private ArenaMap arenaMap = new ArenaMap();

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

    private SimpleAI aiOpponent;

    public static void main(String[] args) {
        UIManager.launch(UIManager.class, args);
    }

    @FXML
    private void initialize() {
        entityLayer.setPrefSize(cols * tileSize, rows * tileSize);
        staticLayer.setPrefSize(cols * tileSize, rows * tileSize);

        pointsCounter = new PointsCounter();
        pointsCounter.setLayoutX(((cols*tileSize)/2)-55);
        pointsCounter.setLayoutY(5);

        clipImage(getImageFromPane(cardSlot0), 6);
        clipImage(getImageFromPane(cardSlot1), 6);
        clipImage(getImageFromPane(cardSlot2), 6);
        clipImage(getImageFromPane(cardSlot3), 6);

        // Initialize core manager classes first
        entityRenderer = new EntityRenderer(arenaMap, entityLayer, staticLayer, pointsCounter, rows, cols, tileSize);
        combatManager = new CombatManager(ENTITY_UPDATE_INTERVAL);
        gameStateManager = new GameStateManager(gameTimerLabel, elixirCountLabel, elixirProgressBar);
        cardManager = new CardManager(cardSlot0, cardSlot1, cardSlot2, cardSlot3,
                                     card1CostLabel, card2CostLabel, card3CostLabel, card4CostLabel);
        spellSystem = new SpellSystem(arenaMap, combatManager, rows, cols);
        entityUpdater = new EntityUpdater(arenaMap, combatManager, entityRenderer, rows, cols, ENTITY_UPDATE_INTERVAL);

        // Initialize new managers (careful with dependencies)
        sceneNavigationManager = new SceneNavigationManager(arenaGrid, gameStateManager);
        towerManager = new TowerManager(arenaMap, pointsCounter, rows, cols);
        victoryConditionManager = new VictoryConditionManager(arenaMap, pointsCounter, rows, cols, sceneNavigationManager);
        entityLifecycleManager = new EntityLifecycleManager(arenaMap, combatManager, entityRenderer, entityUpdater, towerManager, rows, cols);
        entityPlacementManager = new EntityPlacementManager(arenaMap, gameStateManager, cardManager, entityRenderer, spellSystem, rows, cols);
        arenaSetupManager = new ArenaSetupManager(arenaMap, arenaGrid, rows, cols, tileSize, entityRenderer);
        gameLoopManager = new GameLoopManager(gameStateManager, entityLifecycleManager, entityRenderer, victoryConditionManager, gameTimerLabel, ENTITY_UPDATE_INTERVAL);
        
        // Set callback for handling tower destroy results
        entityLifecycleManager.setTowerDestroyCallback(this::handleTowerDestroyResult);

        cardManager.loadDeck();

        arenaSetupManager.fillArenaGrid(entityPlacementManager);
        arenaSetupManager.loadDefaultArenaIfExists();
        entityRenderer.renderStaticObjects();

        // Initialize the AI opponent before starting game loop
        String difficulty = UIManager.getSelectedDifficulty();
        if ("Simple".equals(difficulty)) {
            aiOpponent = new SimpleAI(arenaMap, this);
            gameLoopManager.setAIOpponent(aiOpponent);
        }
        
        gameLoopManager.startGameLoop();

        // Verify all cards are draggable after initialization
        verifyAllCardsDraggable();
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
            victoryConditionManager.endGame(result.playerWon, false, gameLoopManager.getGameLoop());
            
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