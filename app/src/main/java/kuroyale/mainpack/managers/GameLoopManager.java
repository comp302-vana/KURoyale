package kuroyale.mainpack.managers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;
import kuroyale.mainpack.SimpleAI;

/**
 * Handles game loop orchestration and timeline coordination.
 * High cohesion: All game loop timing and coordination logic in one place.
 */
public class GameLoopManager {
    private final GameStateManager gameStateManager;  // Single-player mode
    private final DualPlayerStateManager dualPlayerStateManager;  // PvP mode
    private final EntityLifecycleManager entityLifecycleManager;
    private final EntityRenderer entityRenderer;
    private final VictoryConditionManager victoryConditionManager;
    private final Label gameTimerLabel;
    private final double ENTITY_UPDATE_INTERVAL;
    private Timeline gameLoop;
    private double timePassedSinceLastEntityUpdate = 0;
    private SimpleAI aiOpponent;
    private final boolean isPvPMode;

    // Constructor for single-player mode
    public GameLoopManager(GameStateManager gameStateManager, EntityLifecycleManager entityLifecycleManager,
                          EntityRenderer entityRenderer, VictoryConditionManager victoryConditionManager,
                          Label gameTimerLabel, double entityUpdateInterval) {
        this.gameStateManager = gameStateManager;
        this.dualPlayerStateManager = null;
        this.entityLifecycleManager = entityLifecycleManager;
        this.entityRenderer = entityRenderer;
        this.victoryConditionManager = victoryConditionManager;
        this.gameTimerLabel = gameTimerLabel;
        this.ENTITY_UPDATE_INTERVAL = entityUpdateInterval;
        this.isPvPMode = false;
    }
    
    // Constructor for PvP mode
    public GameLoopManager(DualPlayerStateManager dualPlayerStateManager, EntityLifecycleManager entityLifecycleManager,
                          EntityRenderer entityRenderer, VictoryConditionManager victoryConditionManager,
                          Label gameTimerLabel, double entityUpdateInterval) {
        this.gameStateManager = null;
        this.dualPlayerStateManager = dualPlayerStateManager;
        this.entityLifecycleManager = entityLifecycleManager;
        this.entityRenderer = entityRenderer;
        this.victoryConditionManager = victoryConditionManager;
        this.gameTimerLabel = gameTimerLabel;
        this.ENTITY_UPDATE_INTERVAL = entityUpdateInterval;
        this.isPvPMode = true;
    }

    public void setAIOpponent(SimpleAI aiOpponent) {
        this.aiOpponent = aiOpponent;
    }

    public Timeline getGameLoop() {
        return gameLoop;
    }

    public void startGameLoop() {
        final double TICK_DURATION = 0.1;

        gameLoop = new Timeline(new KeyFrame(Duration.seconds(TICK_DURATION), e -> {
            // Update elixir based on mode
            if (isPvPMode) {
                dualPlayerStateManager.updateBothPlayersElixir(TICK_DURATION);
            } else {
                gameStateManager.updateElixir(TICK_DURATION);
            }

            // Update timer (same interface for both)
            boolean timeUp;
            if (isPvPMode) {
                timeUp = dualPlayerStateManager.updateTimer(TICK_DURATION);
                // Update timer label manually (DualPlayerStateManager doesn't have updateTimerLabel)
                int totalSeconds = dualPlayerStateManager.getTotalSeconds();
                int minutes = totalSeconds / 60;
                int seconds = totalSeconds % 60;
                String timeText = String.format("%02d:%02d", minutes, seconds);
                if (gameTimerLabel != null) {
                    gameTimerLabel.setText(timeText);
                }
            } else {
                timeUp = gameStateManager.updateTimer(TICK_DURATION);
            }
            
            if (timeUp) {
                victoryConditionManager.tieBreaker(gameLoop, gameTimerLabel);
                gameLoop.stop();
                if (gameTimerLabel != null) {
                    gameTimerLabel.setText("00:00");
                }
            }

            // Update entities (movement and combat)
            timePassedSinceLastEntityUpdate += TICK_DURATION;
            if (timePassedSinceLastEntityUpdate >= ENTITY_UPDATE_INTERVAL) {
                entityRenderer.setEntityDirty(false);
                entityRenderer.setStaticDirty(false);
                entityLifecycleManager.updateEntities();
                if (entityRenderer.isEntityDirty()) {
                    entityRenderer.renderEntities();
                    entityRenderer.renderTowerHealthBars();
                }
                if (entityRenderer.isStaticDirty()) {
                    entityRenderer.renderStaticObjects();
                }
                // Update AI opponent (only in single-player mode)
                if (!isPvPMode && aiOpponent != null) {
                    aiOpponent.update(TICK_DURATION, gameStateManager.getTotalSeconds());
                }
                timePassedSinceLastEntityUpdate = 0;
            }
        }));

        gameLoop.setCycleCount(Timeline.INDEFINITE);
        gameLoop.play();
    }

    public void pauseGameLoop() {
        if (gameLoop != null) {
            gameLoop.pause();
        }
    }
}
