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
    private final GameStateManager gameStateManager;
    private final EntityLifecycleManager entityLifecycleManager;
    private final EntityRenderer entityRenderer;
    private final VictoryConditionManager victoryConditionManager;
    private final Label gameTimerLabel;
    private final double ENTITY_UPDATE_INTERVAL;
    private Timeline gameLoop;
    private double timePassedSinceLastEntityUpdate = 0;
    private SimpleAI aiOpponent;

    public GameLoopManager(GameStateManager gameStateManager, EntityLifecycleManager entityLifecycleManager,
                          EntityRenderer entityRenderer, VictoryConditionManager victoryConditionManager,
                          Label gameTimerLabel, double entityUpdateInterval) {
        this.gameStateManager = gameStateManager;
        this.entityLifecycleManager = entityLifecycleManager;
        this.entityRenderer = entityRenderer;
        this.victoryConditionManager = victoryConditionManager;
        this.gameTimerLabel = gameTimerLabel;
        this.ENTITY_UPDATE_INTERVAL = entityUpdateInterval;
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
            gameStateManager.updateElixir(TICK_DURATION);

            if (gameStateManager.updateTimer(TICK_DURATION)) {
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
                // Update AI opponent
                if (aiOpponent != null) {
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
