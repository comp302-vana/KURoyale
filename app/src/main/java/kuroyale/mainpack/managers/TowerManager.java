package kuroyale.mainpack.managers;

import java.util.ArrayList;
import java.util.List;
import kuroyale.arenapack.ArenaMap;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.entitiypack.subclasses.TowerEntity;
import kuroyale.mainpack.PointsCounter;

/**
 * Handles tower breaking logic, points calculation, and king destruction.
 * High cohesion: All tower-related game logic in one place.
 */
public class TowerManager {
    private final ArenaMap arenaMap;
    private final PointsCounter pointsCounter;
    private final int rows;
    private final int cols;

    public TowerManager(ArenaMap arenaMap, PointsCounter pointsCounter, int rows, int cols) {
        this.arenaMap = arenaMap;
        this.pointsCounter = pointsCounter;
        this.rows = rows;
        this.cols = cols;
    }

    public static class TowerDestroyResult {
        public final boolean isGameEnd;
        public final boolean playerWon;
        public final boolean isKing;
        public final boolean kingIsPlayer; // Only valid if isKing is true

        public TowerDestroyResult(boolean isGameEnd, boolean playerWon, boolean isKing, boolean kingIsPlayer) {
            this.isGameEnd = isGameEnd;
            this.playerWon = playerWon;
            this.isKing = isKing;
            this.kingIsPlayer = kingIsPlayer;
        }
    }

    public TowerDestroyResult handleTowerDestroyed(AliveEntity entity) {
        if (entity instanceof TowerEntity tower) {
            if (tower.isKing()) {
                if (tower.isPlayer()) {
                    pointsCounter.setEnemyPoints(3);
                } else {
                    pointsCounter.setOurPoints(3);
                }
                boolean playerWon = !tower.isPlayer();
                return new TowerDestroyResult(true, playerWon, true, tower.isPlayer());
            } else {
                if (tower.isPlayer()) {
                    pointsCounter.addToEnemy();
                } else {
                    pointsCounter.addToUs();
                }
            }
        }
        return new TowerDestroyResult(false, false, false, false);
    }

    public List<TowerEntity> getTowersToKillWhenKingDies(boolean isPlayer) {
        List<TowerEntity> towersToKill = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                AliveEntity entity = arenaMap.getEntity(r, c);
                if (entity instanceof TowerEntity tower && tower.isPlayer() == isPlayer && tower.getHP() > 0) {
                    towersToKill.add(tower);
                }
            }
        }
        return towersToKill;
    }
}
