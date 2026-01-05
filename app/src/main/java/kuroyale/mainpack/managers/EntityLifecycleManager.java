package kuroyale.mainpack.managers;

import java.util.ArrayList;
import java.util.List;
import kuroyale.arenapack.ArenaMap;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.entitiypack.subclasses.TowerEntity;
import kuroyale.entitiypack.subclasses.UnitEntity;
import kuroyale.entitiypack.subclasses.BuildingEntity;

/**
 * Handles entity lifecycle: collection, filtering, and removal.
 * High cohesion: All entity lifecycle management in one place.
 */
public class EntityLifecycleManager {
    private final ArenaMap arenaMap;
    private final CombatManager combatManager;
    private final EntityRenderer entityRenderer;
    private final EntityUpdater entityUpdater;
    private final TowerManager towerManager;
    private final int rows;
    private final int cols;
    private QuestManager questManager;
    private PersistenceManager persistenceManager;
    private AchievementManager achievementManager;
    private java.util.function.Consumer<TowerManager.TowerDestroyResult> towerDestroyCallback;

    public EntityLifecycleManager(ArenaMap arenaMap, CombatManager combatManager, EntityRenderer entityRenderer,
                                 EntityUpdater entityUpdater, TowerManager towerManager, int rows, int cols) {
        this.arenaMap = arenaMap;
        this.combatManager = combatManager;
        this.entityRenderer = entityRenderer;
        this.entityUpdater = entityUpdater;
        this.towerManager = towerManager;
        this.rows = rows;
        this.cols = cols;
    }

    public void setTowerDestroyCallback(java.util.function.Consumer<TowerManager.TowerDestroyResult> callback) {
        this.towerDestroyCallback = callback;
    }

    //setters for managers
    public void setQuestManager(QuestManager questManager) {
        this.questManager = questManager;
    }

    public void setPersistenceManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }
    
    public void setAchievementManager(AchievementManager achievementManager) {
        this.achievementManager = achievementManager;
    }
    

    public void updateEntities() {
        List<AliveEntity> entitiesToUpdate = new ArrayList<>();
        List<AliveEntity> deadEntities = new ArrayList<>();

        // Collect all entities
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                AliveEntity entity = arenaMap.getEntity(r, c);
                if (entity != null && entity.getHP() > 0) {
                    entitiesToUpdate.add(entity);
                } else if (entity != null && entity.getHP() <= 0) {
                    deadEntities.add(entity);
                }
            }
        }

        // Remove dead entities first
        for (AliveEntity deadEntity : deadEntities) {
            TowerManager.TowerDestroyResult result = removeDeadEntity(deadEntity);
            if (result.isGameEnd && towerDestroyCallback != null) {
                towerDestroyCallback.accept(result);
            }
        }

        // Update cooldowns via CombatManager
        combatManager.updateCooldowns(entitiesToUpdate);

        // Update each entity
        for (AliveEntity entity : entitiesToUpdate) {
            int entityRow = -1, entityCol = -1;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (arenaMap.getEntity(r, c) == entity) {
                        entityRow = r;
                        entityCol = c;
                        break;
                    }
                }
                if (entityRow >= 0)
                    break;
            }

            if (entityRow < 0 || entityCol < 0 || entity.getHP() <= 0) {
                continue;
            }

            entity.setPosition(entityRow, entityCol);

            if (entity instanceof UnitEntity) {
                entityUpdater.updateUnitEntity((UnitEntity) entity);
                if (entity.getHP() <= 0) {
                    TowerManager.TowerDestroyResult result = removeDeadEntity(entity);
                    if (result.isGameEnd && towerDestroyCallback != null) {
                        towerDestroyCallback.accept(result);
                    }
                }
            } else if (entity instanceof BuildingEntity && !(entity instanceof TowerEntity)) {
                entityUpdater.updateBuildingEntity((BuildingEntity) entity);
                if (entity.getHP() <= 0) {
                    TowerManager.TowerDestroyResult result = removeDeadEntity(entity);
                    if (result.isGameEnd && towerDestroyCallback != null) {
                        towerDestroyCallback.accept(result);
                    }
                }
            } else if (entity instanceof TowerEntity) {
                entityUpdater.updateTowerEntity((TowerEntity) entity);
                if (entity.getHP() <= 0) {
                    TowerManager.TowerDestroyResult result = removeDeadEntity(entity);
                    if (result.isGameEnd && towerDestroyCallback != null) {
                        towerDestroyCallback.accept(result);
                    }
                }
            }
        }

        entityRenderer.setEntityDirty(true);
    }

    public TowerManager.TowerDestroyResult removeDeadEntity(AliveEntity entity) {
        arenaMap.removeEntity(entity);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (arenaMap.getEntity(r, c) == entity) {
                    arenaMap.setEntity(r, c, null);
                    arenaMap.clearObject(r, c);
                    combatManager.removeEntity(entity);

                    if (entity instanceof TowerEntity) {
                        entityRenderer.setStaticDirty(true);
                    }
                }
            }
        }
        entityRenderer.removeEntitySprite(entity);
        
        // Handle tower destruction logic
        TowerManager.TowerDestroyResult result = null;
        if (entity instanceof TowerEntity) {
            result = towerManager.handleTowerDestroyed(entity);
            if (questManager != null) {
                TowerEntity tower = (TowerEntity) entity;
                boolean isCrownTower = !tower.isKing();
                boolean isPlayerTower = tower.isPlayer();
                questManager.onTowerDestroyed(isCrownTower, isPlayerTower);
            }
            
            if (persistenceManager != null && entity instanceof TowerEntity) {
                TowerEntity tower = (TowerEntity) entity;
                if (!tower.isPlayer()) { // Enemy tower destroyed
                    kuroyale.mainpack.models.PlayerProfile profile = persistenceManager.loadPlayerProfile();
                    kuroyale.mainpack.models.PlayerStatistics stats = profile.getStatistics();
                    
                    if (stats != null) {
                        if (tower.isKing()) {
                            stats.incrementTotalKingTowersDestroyed();
                        } else {
                            stats.incrementTotalCrownTowersDestroyed();
                        }
                        
                        // Save updated statistics
                        profile.setStatistics(stats);
                        persistenceManager.savePlayerProfile(profile);
                        
                        // Update achievements
                        if (achievementManager != null) {
                            achievementManager.updateFromStatistics(stats);
                        }
                    }
                }
            }
            return result;
        }
    return new TowerManager.TowerDestroyResult(false, false, false, false);
    }
}
