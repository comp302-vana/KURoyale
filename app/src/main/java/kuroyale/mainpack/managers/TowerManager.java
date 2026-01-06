package kuroyale.mainpack.managers;

import java.util.ArrayList;
import java.util.List;
import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;
import kuroyale.arenapack.PlacedObject;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.entitiypack.subclasses.TowerEntity;
import kuroyale.mainpack.PointsCounter;
import kuroyale.mainpack.network.TowerId;

/**
 * Handles tower breaking logic, points calculation, and king destruction.
 * High cohesion: All tower-related game logic in one place.
 */
public class TowerManager {
    private final ArenaMap arenaMap;
    private final PointsCounter pointsCounter;
    private final int rows;
    private final int cols;
    private kuroyale.mainpack.network.NetworkManager networkManager;
    private kuroyale.mainpack.GameEngine gameEngine;
    private kuroyale.mainpack.managers.EntityRenderer entityRenderer;

    public TowerManager(ArenaMap arenaMap, PointsCounter pointsCounter, int rows, int cols) {
        this.arenaMap = arenaMap;
        this.pointsCounter = pointsCounter;
        this.rows = rows;
        this.cols = cols;
    }
    
    public void setEntityRenderer(kuroyale.mainpack.managers.EntityRenderer entityRenderer) {
        this.entityRenderer = entityRenderer;
    }
    
    public void setNetworkManager(kuroyale.mainpack.network.NetworkManager networkManager) {
        this.networkManager = networkManager;
    }
    
    public void setGameEngine(kuroyale.mainpack.GameEngine gameEngine) {
        this.gameEngine = gameEngine;
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
    
    /**
     * Identify tower by its absolute position and type.
     * Uses absolute column position to determine player (P1 vs P2) and left/right.
     * This ensures host and client identify the same tower with the same TowerId,
     * regardless of local-relative isPlayer() values.
     * 
     * @param tower The tower entity to identify
     * @return TowerId enum value, or null if tower not found
     */
    public TowerId identifyTower(TowerEntity tower) {
        // Compute bounding box (min/max rows and cols)
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        boolean found = false;
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (arenaMap.getEntity(r, c) == tower) {
                    found = true;
                    minRow = Math.min(minRow, r);
                    maxRow = Math.max(maxRow, r);
                    minCol = Math.min(minCol, c);
                    maxCol = Math.max(maxCol, c);
                }
            }
        }
        
        if (!found) {
            return null; // Tower not found
        }
        
        // Use absolute column position to determine player (P1 vs P2)
        // River is at cols 15-16, so:
        // - Left side (col < 15) = Player 1
        // - Right side (col >= 16) = Player 2
        int centerCol = (minCol + maxCol) / 2;
        int centerRow = (minRow + maxRow) / 2;
        
        // Determine player based on absolute column position (not isPlayer which is local-relative)
        int playerId;
        if (centerCol < 15) {
            playerId = 1; // Left side = Player 1
        } else if (centerCol >= 16) {
            playerId = 2; // Right side = Player 2
        } else {
            // Tower spans river (cols 15-16) - use minCol for more deterministic result
            playerId = (minCol < 15) ? 1 : 2;
        }
        
        // Identify tower type
        if (tower.isKing()) {
            return (playerId == 1) ? TowerId.P1_KING : TowerId.P2_KING;
        } else {
            // For princess towers, use row position to determine LEFT vs RIGHT
            // LEFT tower is typically at a lower row (top of player's area)
            // RIGHT tower is typically at a higher row (bottom of player's area)
            // Use centerRow to determine this (rows don't flip, so this is consistent)
            boolean isLeft = centerRow < rows / 2;
            
            if (playerId == 1) {
                return isLeft ? TowerId.P1_LEFT : TowerId.P1_RIGHT;
            } else {
                return isLeft ? TowerId.P2_LEFT : TowerId.P2_RIGHT;
            }
        }
    }
    
    /**
     * Get tower by TowerId.
     * 
     * @param towerId The TowerId to find
     * @return The TowerEntity matching the ID, or null if not found
     */
    public TowerEntity getTowerById(TowerId towerId) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                AliveEntity entity = arenaMap.getEntity(r, c);
                if (entity instanceof TowerEntity tower) {
                    TowerId id = identifyTower(tower);
                    if (id == towerId) {
                        return tower;
                    }
                }
            }
        }
        return null;
    }
    
    private String getTowerType(TowerEntity tower) {
        // Legacy method - use TowerId instead
        TowerId id = identifyTower(tower);
        if (id == null) return null;
        
        switch (id) {
            case P1_KING: return "P1_KING";
            case P1_LEFT: return "P1_LEFT";
            case P1_RIGHT: return "P1_RIGHT";
            case P2_KING: return "P2_KING";
            case P2_LEFT: return "P2_LEFT";
            case P2_RIGHT: return "P2_RIGHT";
            default: return null;
        }
    }
    
    /**
     * Syncs tower health from network using TowerId.
     * Updates the specific tower identified by TowerId.
     * 
     * @param towerId The TowerId enum value
     * @param newHp The new HP value from host
     */
    public void syncTowerHealthFromNetwork(TowerId towerId, double newHp) {
        TowerEntity tower = getTowerById(towerId);
        if (tower != null) {
            double currentHP = tower.getHP();
            double diff = Math.abs(currentHP - newHp);
            // Only apply HP updates if difference is significant (> 0.5 HP)
            // This prevents visual jitter from micro-corrections while still maintaining sync
            if (diff > 0.5) {
                tower.setHP(newHp);
                System.out.println("CLIENT: TOWER_UPDATE - " + towerId + " HP: " + currentHP + " -> " + newHp + " (diff: " + diff + ")");
            }
            
            // If HP <= 0, remove tower visuals
            if (newHp <= 0) {
                removeTowerFromNetwork(towerId);
            }
        } else {
            System.out.println("CLIENT: TOWER_UPDATE - Warning: Tower " + towerId + " not found");
        }
    }
    
    /**
     * Legacy method for string-based tower type (for backward compatibility during transition).
     * @deprecated Use syncTowerHealthFromNetwork(TowerId, double) instead
     */
    @Deprecated
    public void syncTowerHealthFromNetwork(String towerType, double newHp) {
        // Try to parse TowerId from string
        TowerId towerId = null;
        try {
            towerId = TowerId.valueOf(towerType);
        } catch (IllegalArgumentException e) {
            // Fallback to old string matching
            System.out.println("CLIENT: Legacy tower sync for type: " + towerType);
            // Old logic for backward compatibility
            java.util.Set<TowerEntity> updatedTowers = new java.util.HashSet<>();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    AliveEntity entity = arenaMap.getEntity(r, c);
                    if (entity instanceof TowerEntity tower && !updatedTowers.contains(tower)) {
                        TowerId id = identifyTower(tower);
                        if (id != null && id.name().equals(towerType)) {
                            syncTowerHealthFromNetwork(id, newHp);
                            updatedTowers.add(tower);
                        }
                    }
                }
            }
            return;
        }
        
        if (towerId != null) {
            syncTowerHealthFromNetwork(towerId, newHp);
        }
    }
    
    /**
     * Remove tower from network (client side).
     * Clears tower from arenaMap using bounding box and marks renderer dirty.
     * 
     * @param towerId The TowerId of the tower to remove
     */
    public void removeTowerFromNetwork(TowerId towerId) {
        TowerEntity tower = getTowerById(towerId);
        if (tower == null) {
            System.out.println("CLIENT: removeTowerFromNetwork - Tower " + towerId + " not found");
            return;
        }
        
        System.out.println("CLIENT: removeTowerFromNetwork - Removing tower " + towerId);
        
        // Set HP to 0
        tower.setHP(0);
        
        // Compute bounding box to find all cells occupied by this tower
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        boolean found = false;
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (arenaMap.getEntity(r, c) == tower) {
                    found = true;
                    minRow = Math.min(minRow, r);
                    maxRow = Math.max(maxRow, r);
                    minCol = Math.min(minCol, c);
                    maxCol = Math.max(maxCol, c);
                }
            }
        }
        
        if (!found) {
            System.out.println("CLIENT: removeTowerFromNetwork - Tower " + towerId + " not found in arenaMap grid");
            return;
        }
        
        // Clear all cells in tower bounding box from arenaMap
        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                if (r >= 0 && r < rows && c >= 0 && c < cols) {
                    // Remove entity reference
                    if (arenaMap.getEntity(r, c) == tower) {
                        arenaMap.setEntity(r, c, null);
                    }
                    // Remove static object reference (if towers are stored as static objects)
                    PlacedObject obj = arenaMap.getObject(r, c);
                    if (obj != null && (obj.getType() == ArenaObjectType.OUR_TOWER ||
                                       obj.getType() == ArenaObjectType.ENEMY_TOWER ||
                                       obj.getType() == ArenaObjectType.OUR_KING ||
                                       obj.getType() == ArenaObjectType.ENEMY_KING)) {
                        arenaMap.clearObject(r, c);
                    }
                }
            }
        }
        
        // Mark renderer dirty if available
        if (entityRenderer != null) {
            entityRenderer.setEntityDirty(true);
            entityRenderer.setStaticDirty(true);
        }
        
        System.out.println("CLIENT: removeTowerFromNetwork - Tower " + towerId + " footprint cleared (" + 
                         minRow + "," + minCol + ") to (" + maxRow + "," + maxCol + ")");
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
