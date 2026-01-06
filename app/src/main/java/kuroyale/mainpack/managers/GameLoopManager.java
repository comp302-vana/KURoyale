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
    private double timePassedSinceLastNetworkSync = 0;
    private double timePassedSinceLastElixirSync = 0;
    private double timePassedSinceLastSnapshot = 0;
    private SimpleAI aiOpponent;
    private final boolean isPvPMode;
    private kuroyale.mainpack.network.NetworkBattleManager networkBattleManager;
    private kuroyale.arenapack.ArenaMap arenaMap;
    private kuroyale.mainpack.managers.TowerManager towerManager;
    private boolean isNetworkMode;
    private boolean isNetworkClient;

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
    
    /**
     * Set network battle manager for network synchronization.
     */
    public void setNetworkBattleManager(kuroyale.mainpack.network.NetworkBattleManager battleManager, 
                                       kuroyale.arenapack.ArenaMap arenaMap,
                                       kuroyale.mainpack.managers.TowerManager towerManager) {
        this.networkBattleManager = battleManager;
        this.arenaMap = arenaMap;
        this.towerManager = towerManager;
        this.isNetworkMode = (battleManager != null);
        this.isNetworkClient = (battleManager != null && battleManager.isClient());
    }

    public Timeline getGameLoop() {
        return gameLoop;
    }

    public void startGameLoop() {
        final double TICK_DURATION = 0.1;

        gameLoop = new Timeline(new KeyFrame(Duration.seconds(TICK_DURATION), e -> {
            // Update elixir based on mode
            // Network client doesn't regenerate elixir locally (receives from host)
            if (isNetworkMode && isNetworkClient) {
                // Client: don't update elixir locally, wait for host updates
            } else if (isPvPMode) {
                dualPlayerStateManager.updateBothPlayersElixir(TICK_DURATION);
            } else {
                gameStateManager.updateElixir(TICK_DURATION);
            }

            // Update timer (same interface for both)
            // CRITICAL: Client does NOT update timer locally (host is authoritative)
            boolean timeUp = false;
            if (isNetworkMode && isNetworkClient) {
                // Client: timer is updated via GAME_STATE_SNAPSHOT from host
                // Don't update timer locally
            } else if (isPvPMode) {
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
                // Clear dirty flags FIRST
                entityRenderer.setEntityDirty(false);
                entityRenderer.setStaticDirty(false);
                
                // Process network messages AFTER clearing dirty flags (so they can set dirty flags)
                if (isNetworkMode && networkBattleManager != null) {
                    networkBattleManager.processQueuedMessages();
                }
                
                // In network mode, client doesn't run entity physics (receives updates from host)
                // Host runs full physics and sends updates
                if (!(isNetworkMode && isNetworkClient)) {
                    entityLifecycleManager.updateEntities();
                }
                
                // Network sync: Send entity updates (host only)
                if (isNetworkMode && networkBattleManager != null) {
                    timePassedSinceLastNetworkSync += ENTITY_UPDATE_INTERVAL;
                    
                    // Send entity state updates every 100ms (10Hz)
                    if (timePassedSinceLastNetworkSync >= 0.1) {
                        sendEntityStateUpdates();
                        timePassedSinceLastNetworkSync = 0;
                    }
                    
                    // Send elixir updates every 200ms (5Hz) - separate timer
                    timePassedSinceLastElixirSync += ENTITY_UPDATE_INTERVAL;
                    if (timePassedSinceLastElixirSync >= 0.2 && dualPlayerStateManager != null) {
                        sendElixirUpdate();
                        timePassedSinceLastElixirSync = 0;
                    }
                }
                
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
    
    /**
     * Send entity state updates to client (host only).
     */
    private void sendEntityStateUpdates() {
        if (networkBattleManager == null || arenaMap == null) return;
        
        // Track last known states to detect changes
        // Collect all entities and send their states
        java.util.Set<kuroyale.entitiypack.subclasses.AliveEntity> seenEntities = 
            new java.util.HashSet<>();
        java.util.Set<kuroyale.entitiypack.subclasses.TowerEntity> seenTowers = 
            new java.util.HashSet<>();
        
        for (int r = 0; r < arenaMap.getRows(); r++) {
            for (int c = 0; c < arenaMap.getCols(); c++) {
                kuroyale.entitiypack.subclasses.AliveEntity entity = arenaMap.getEntity(r, c);
                if (entity != null && !seenEntities.contains(entity)) {
                    seenEntities.add(entity);
                    
                    // Handle towers separately
                    if (entity instanceof kuroyale.entitiypack.subclasses.TowerEntity tower) {
                        if (!seenTowers.contains(tower)) {
                            seenTowers.add(tower);
                            
                            kuroyale.mainpack.network.TowerId towerId = towerManager.identifyTower(tower);
                            if (towerId != null) {
                                // Check if tower is dead (HP <= 0)
                                if (tower.getHP() <= 0) {
                                    // Send tower destroy message
                                    networkBattleManager.sendTowerDestroy(towerId);
                                    System.out.println("Host: TOWER_DESTROY sent - " + towerId + " (HP=" + tower.getHP() + ")");
                                } else {
                                    // Send tower update using TowerId
                                    double maxHp = ((kuroyale.cardpack.subclasses.AliveCard) tower.getCard()).getHp();
                                    networkBattleManager.sendTowerUpdate(towerId, tower.getHP(), maxHp);
                                }
                            }
                        }
                    } else {
                        // Regular entity
                        if (entity.getHP() > 0) {
                            // Assign ID if not assigned (for entities that were spawned before network sync)
                            if (entity.getEntityId() <= 0) {
                                long entityId = kuroyale.mainpack.network.EntityIdGenerator.getInstance().generateId();
                                entity.setEntityId(entityId);
                                networkBattleManager.getEntityRegistry().registerEntity(entityId, entity);
                            }
                            // Send entity update
                            networkBattleManager.sendEntityUpdate(entity);
                        } else {
                            // Send entity death
                            if (entity.getEntityId() > 0) {
                                networkBattleManager.sendEntityDeath(entity.getEntityId());
                            }
                        }
                    }
                }
            }
        }
    }
    
    
    /**
     * Send elixir update to client (host only).
     */
    private void sendElixirUpdate() {
        if (networkBattleManager == null || dualPlayerStateManager == null) return;
        
        double player1Elixir = dualPlayerStateManager.getElixir(1);
        double player2Elixir = dualPlayerStateManager.getElixir(2);
        networkBattleManager.sendElixirUpdate(player1Elixir, player2Elixir);
    }
    
    /**
     * Send game state snapshot to client (host only).
     * Includes: timer, elixir, tower HPs, all entities.
     */
    private void sendGameStateSnapshot() {
        if (networkBattleManager == null || dualPlayerStateManager == null || arenaMap == null) return;
        
        // Build snapshot data
        StringBuilder snapshot = new StringBuilder();
        
        // Timer
        int totalSeconds = dualPlayerStateManager.getTotalSeconds();
        snapshot.append(totalSeconds).append("|");
        
        // Elixir
        double player1Elixir = dualPlayerStateManager.getElixir(1);
        double player2Elixir = dualPlayerStateManager.getElixir(2);
        snapshot.append(player1Elixir).append("|").append(player2Elixir).append("|");
        
        // Tower HPs (by TowerId)
        if (towerManager != null) {
            for (kuroyale.mainpack.network.TowerId towerId : kuroyale.mainpack.network.TowerId.values()) {
                kuroyale.entitiypack.subclasses.TowerEntity tower = towerManager.getTowerById(towerId);
                if (tower != null) {
                    double hp = tower.getHP();
                    double maxHp = ((kuroyale.cardpack.subclasses.AliveCard) tower.getCard()).getHp();
                    snapshot.append(towerId.name()).append(":").append(hp).append(":").append(maxHp).append(";");
                }
            }
        }
        snapshot.append("|");
        
        // Entities: entityId:cardId:isPlayer:row:col:hp:maxHp;
        java.util.Set<kuroyale.entitiypack.subclasses.AliveEntity> seenEntities = new java.util.HashSet<>();
        for (int r = 0; r < arenaMap.getRows(); r++) {
            for (int c = 0; c < arenaMap.getCols(); c++) {
                kuroyale.entitiypack.subclasses.AliveEntity entity = arenaMap.getEntity(r, c);
                if (entity != null && !(entity instanceof kuroyale.entitiypack.subclasses.TowerEntity) && !seenEntities.contains(entity)) {
                    seenEntities.add(entity);
                    long entityId = entity.getEntityId();
                    if (entityId > 0) {
                        int cardId = entity.getCard().getId();
                        boolean isPlayer = entity.isPlayer();
                        double hp = entity.getHP();
                        double maxHp = ((kuroyale.cardpack.subclasses.AliveCard) entity.getCard()).getHp();
                        snapshot.append(entityId).append(":").append(cardId).append(":")
                                .append(isPlayer ? "1" : "2").append(":").append(r).append(":")
                                .append(c).append(":").append(hp).append(":").append(maxHp).append(";");
                    }
                }
            }
        }
        
        networkBattleManager.sendGameStateSnapshot(snapshot.toString());
    }
}
