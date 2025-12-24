package kuroyale.mainpack.managers;

import kuroyale.arenapack.ArenaMap;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.entitiypack.subclasses.TowerEntity;
import kuroyale.mainpack.PointsCounter;
import kuroyale.mainpack.managers.SceneNavigationManager;
import kuroyale.mainpack.models.GameMode;

/**
 * Handles victory conditions, tie-breaker logic, and game end determination.
 * High cohesion: All victory/end game logic in one place.
 */
public class VictoryConditionManager {
    private final ArenaMap arenaMap;
    private final PointsCounter pointsCounter;
    private final int rows;
    private final int cols;
    private final SceneNavigationManager sceneNavigationManager;
    private EconomyManager economyManager;
    private GameMode gameMode = GameMode.SINGLE_PLAYER_AI;

    public VictoryConditionManager(ArenaMap arenaMap, PointsCounter pointsCounter, int rows, int cols,
                                  SceneNavigationManager sceneNavigationManager) {
        this.arenaMap = arenaMap;
        this.pointsCounter = pointsCounter;
        this.rows = rows;
        this.cols = cols;
        this.sceneNavigationManager = sceneNavigationManager;
    }
    
    public void setEconomyManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }
    
    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public void tieBreaker(javafx.animation.Timeline gameLoop, javafx.scene.control.Label gameTimerLabel) {
        if (pointsCounter.getEnemyPoints() > pointsCounter.getOurPoints()) {
            endGame(false, false, gameLoop);
            return;
        } else if (pointsCounter.getEnemyPoints() < pointsCounter.getOurPoints()) {
            endGame(true, false, gameLoop);
            return;
        }
        double minPlayer = Double.MAX_VALUE;
        double minEnemy = Double.MAX_VALUE;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                AliveEntity entity = arenaMap.getEntity(r, c);
                if (entity instanceof TowerEntity && entity.getHP() > 0) {
                    if (entity.isPlayer()) {
                        if (entity.getHP() < minPlayer) {
                            minPlayer = entity.getHP();
                        }
                    } else {
                        if (entity.getHP() < minEnemy) {
                            minEnemy = entity.getHP();
                        }
                    }
                }
            }
        }
        if (minPlayer > minEnemy) {
            endGame(true, false, gameLoop);
            return;
        } else if (minPlayer < minEnemy) {
            endGame(false, false, gameLoop);
            return;
        }
        // Stop the game loop
        if (gameLoop != null) {
            gameLoop.stop();
        }

        // Draw case
        if (gameTimerLabel != null) {
            gameTimerLabel.setText("00:00");
        }
        endGame(false, true, gameLoop); // isDraw = true, playerWon value is ignored in draw case
    }

    public void endGame(boolean playerWon, boolean isDraw, javafx.animation.Timeline gameLoop) {
        // Award gold based on match result (only in single-player mode, not PvP)
        if (economyManager != null && gameMode == GameMode.SINGLE_PLAYER_AI) {
            if (isDraw) {
                economyManager.awardGold("DRAW");
            } else if (playerWon) {
                economyManager.awardGold("VICTORY");
            } else {
                economyManager.awardGold("DEFEAT");
            }
        }
        
        sceneNavigationManager.showGameEndScreen(playerWon, isDraw, gameLoop, gameMode);
    }
}
