package kuroyale.mainpack.managers;

import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;
import kuroyale.arenapack.PlacedObject;
import kuroyale.cardpack.CardCategory;
import kuroyale.cardpack.CardFactory;
import kuroyale.cardpack.CardType;
import kuroyale.cardpack.subclasses.AliveCard;
import kuroyale.cardpack.subclasses.BuildingCard;
import kuroyale.cardpack.subclasses.UnitCard;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.entitiypack.subclasses.UnitEntity;
import kuroyale.entitiypack.subclasses.BuildingEntity;
import kuroyale.entitiypack.subclasses.TowerEntity;

/**
 * Handles entity updates (movement, combat, targeting).
 * High cohesion: All entity update logic in one place.
 */
public class EntityUpdater {
    private final ArenaMap arenaMap;
    private final CombatManager combatManager;
    private final EntityRenderer entityRenderer;
    private final int rows;
    private final int cols;
    private final double ENTITY_UPDATE_INTERVAL;
    private GameStateManager gameStateManager;
    private DualPlayerStateManager dualPlayerStateManager;
    private boolean isPvPMode;
    private ComboManager comboManager;

    public EntityUpdater(ArenaMap arenaMap, CombatManager combatManager, EntityRenderer entityRenderer,
                        int rows, int cols, double entityUpdateInterval) {
        this.arenaMap = arenaMap;
        this.combatManager = combatManager;
        this.entityRenderer = entityRenderer;
        this.rows = rows;
        this.cols = cols;
        this.ENTITY_UPDATE_INTERVAL = entityUpdateInterval;
    }

    public void setGameStateManager(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
        this.isPvPMode = false;
    }
    
    public void setDualPlayerStateManager(DualPlayerStateManager dualPlayerStateManager) {
        this.dualPlayerStateManager = dualPlayerStateManager;
        this.isPvPMode = true;
    }
    
    public void setComboManager(ComboManager comboManager) {
        this.comboManager = comboManager;
    }

    public void updateUnitEntity(UnitEntity unit) {
        if (combatManager.isStunned(unit)) {
            return;
        }

        int currentRow = unit.getRow();
        int currentCol = unit.getCol();
        AliveEntity target = unit.findClosestTarget(arenaMap);

        if (target == null) {
            System.err.printf("NO target found for unit %s\n", unit.getCard().getName());
            return;
        }

        int targetRow = target.getRow();
        int targetCol = target.getCol();

        if (targetRow < 0 || targetCol < 0) {
            return;
        }

        int dRow = Math.abs(targetRow - currentRow);
        int dCol = Math.abs(targetCol - currentCol);
        double distance = dRow + dCol;

        double unitRange = unit.getRange();
        boolean isMelee = unitRange <= 1.0;
        if (isMelee) {
            unitRange = 1.0;
        }

        double actSpeed = ((AliveCard) unit.getCard()).getActSpeed();
        double attackCooldownTime = actSpeed > 0 ? 1.0 / actSpeed : 1.0;

        boolean adjacentEnemyTowerObject = false;
        if (isMelee && target instanceof TowerEntity) {
            for (int[] direction : AliveEntity.directions) {
                int rr = currentRow + direction[0];
                int cc = currentCol + direction[1];
                if (rr < 0 || rr >= rows || cc < 0 || cc >= cols)
                    continue;

                AliveEntity neighborEntity = arenaMap.getEntity(rr, cc);
                if (neighborEntity == target) {
                    adjacentEnemyTowerObject = true;
                    break;
                }

                var obj = arenaMap.getObject(rr, cc);
                if (obj != null && obj.getType() != null) {
                    switch (obj.getType()) {
                        case ENEMY_TOWER, ENEMY_KING -> {
                            if (unit.isPlayer()) {
                                adjacentEnemyTowerObject = true;
                                break;
                            }
                        }
                        case OUR_TOWER, OUR_KING -> {
                            if (!unit.isPlayer()) {
                                adjacentEnemyTowerObject = true;
                                break;
                            }
                        }
                        default -> {}
                    }
                }

                if (adjacentEnemyTowerObject)
                    break;
            }
        }

        boolean inRange = adjacentEnemyTowerObject ||
                (isMelee ? (distance <= unitRange) : (distance <= unitRange));

        if (inRange) {
            double currentCooldown = combatManager.getAttackCooldown(unit);
            if (currentCooldown <= 0) {
                unit.attack(target);
                combatManager.setAttackCooldown(unit, attackCooldownTime);

                entityRenderer.setEntityDirty(true);

                if (target.getHP() <= 0) {
                    // Removal handled by caller
                }
            }
        } else {
            String speedStr = getUnitSpeed(unit);
            double speedMultiplier = 1 / getSpeedMultiplier(speedStr);
            
            // Apply combo speed multiplier
            if (comboManager != null) {
                speedMultiplier *= comboManager.getSpeedMultiplier(unit);
            }

            if (unit.getTicksSinceLastMove() >= (speedMultiplier / ENTITY_UPDATE_INTERVAL)) {
                unit.resetTicksSinceLastMove();
                int oldRow = currentRow;
                int oldCol = currentCol;

                unit.move(arenaMap, target);

                int newRow = unit.getRow();
                int newCol = unit.getCol();

                if ((newRow != oldRow || newCol != oldCol) &&
                    (newRow >= 0 && newRow < rows) &&
                    (newCol >= 0 && newCol < cols)) {
                    PlacedObject oldObj = arenaMap.getObject(oldRow, oldCol);
                    if (oldObj == null || oldObj.getType() == ArenaObjectType.ENTITY) {
                        arenaMap.clearObject(oldRow, oldCol);
                    }
                    boolean placementSuccess = arenaMap.placeObject(newRow, newCol, ArenaObjectType.ENTITY);

                    if (!placementSuccess) {
                        unit.setPosition(oldRow, oldCol);
                        arenaMap.moveEntitiy(newRow, newCol, oldRow, oldCol);
                        if (oldObj == null || oldObj.getType() == ArenaObjectType.ENTITY) {
                            arenaMap.placeObject(oldRow, oldCol, ArenaObjectType.ENTITY);
                        }
                    }
                } else {
                    AliveEntity entityAtNewPos = arenaMap.getEntity(newRow, newCol);
                    if (entityAtNewPos != unit) {
                        unit.setPosition(oldRow, oldCol);
                        if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                            arenaMap.moveEntitiy(newRow, newCol, oldRow, oldCol);
                        }
                    }
                }
            } else {
                unit.incTicksSinceLastMove();
            }
        }
    }

    public void updateBuildingEntity(BuildingEntity building) {
        int currentRow = building.getRow();
        int currentCol = building.getCol();
        
        BuildingCard buildingCard = building.getBuildingCard();
        CardCategory category = buildingCard.getCategory();
        
        // Spawner buildings handling
        if (category == CardCategory.SPAWNER_BUILDING) {
            handleSpawnerBuilding(building, buildingCard);
        }
        //Handle elixir collector separately
        if (category == CardCategory.SPECIAL_BUILDING){
            handleSpecialBuilding(building,buildingCard);
        }

        AliveEntity target = building.findClosestTarget(arenaMap);

        if (target == null) {
            building.reduceLifetime(ENTITY_UPDATE_INTERVAL);
            if (building.getHP() <= 0) {
                arenaMap.setEntity(currentRow, currentCol, null);
                arenaMap.clearObject(currentRow, currentCol);
            }
            return;
        }

        double distance;
        if (target instanceof TowerEntity towerTarget) {
            int towerSize = towerTarget.isKing() ? 3 : 2;
            int bottomRightRow = -1, bottomRightCol = -1;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (arenaMap.getEntity(r, c) == target) {
                        bottomRightRow = r;
                        bottomRightCol = c;
                        break;
                    }
                }
                if (bottomRightRow >= 0) break;
            }

            if (bottomRightRow < 0 || bottomRightCol < 0) {
                return;
            }

            double minDistance = Double.MAX_VALUE;
            int startRow = bottomRightRow - towerSize + 1;
            int startCol = bottomRightCol - towerSize + 1;

            for (int tr = startRow; tr <= bottomRightRow; tr++) {
                for (int tc = startCol; tc <= bottomRightCol; tc++) {
                    int rowDiff = Math.abs(tr - currentRow);
                    int colDiff = Math.abs(tc - currentCol);
                    double cellDistance = rowDiff + colDiff;
                    if (cellDistance < minDistance) {
                        minDistance = cellDistance;
                    }
                }
            }

            distance = minDistance;
        } else {
            int targetRow = -1, targetCol = -1;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (arenaMap.getEntity(r, c) == target) {
                        targetRow = r;
                        targetCol = c;
                        break;
                    }
                }
                if (targetRow >= 0) break;
            }

            if (targetRow < 0 || targetCol < 0) {
                return;
            }

            int rowDiff = Math.abs(targetRow - currentRow);
            int colDiff = Math.abs(targetCol - currentCol);
            distance = rowDiff + colDiff;
        }

        boolean canAttackTower = false;
        if (target instanceof TowerEntity && distance == 1) {
            canAttackTower = true;
        }

        AliveCard aliveCard = (AliveCard) building.getCard();
        double actSpeed = aliveCard.getActSpeed();
        double attackCooldownTime = actSpeed > 0 ? 1.0 / actSpeed : 1.0;
        
        // Apply combo range boost
        double buildingRange = building.getRange();
        if (comboManager != null) {
            buildingRange += comboManager.getRangeBoost(building);
        }

        if (distance <= buildingRange + 0.5 || canAttackTower) {
            double currentCooldown = combatManager.getAttackCooldown(building);
            if (currentCooldown <= 0) {
                building.attack(target);
                combatManager.setAttackCooldown(building, attackCooldownTime);
                entityRenderer.setEntityDirty(true);

                if (target.getHP() <= 0) {
                    // Removal handled by caller
                }
            }
        }

        building.reduceLifetime(ENTITY_UPDATE_INTERVAL);
    }

    public void updateTowerEntity(TowerEntity tower) {
        int currentRow = tower.getRow();
        int currentCol = tower.getCol();

        AliveEntity target = tower.findClosestTarget(arenaMap);

        if (target == null) {
            return;
        }

        double distance;
        if (target instanceof TowerEntity towerTarget) {
            int targetTowerSize = towerTarget.isKing() ? 3 : 2;
            int bottomRightRow = -1, bottomRightCol = -1;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (arenaMap.getEntity(r, c) == target) {
                        bottomRightRow = r;
                        bottomRightCol = c;
                        break;
                    }
                }
                if (bottomRightRow >= 0) break;
            }

            if (bottomRightRow < 0 || bottomRightCol < 0) {
                return;
            }

            int attackerTowerSize = tower.isKing() ? 3 : 2;
            int attackerStartRow = currentRow - attackerTowerSize + 1;
            int attackerStartCol = currentCol - attackerTowerSize + 1;

            double minDistance = Double.MAX_VALUE;
            int targetStartRow = bottomRightRow - targetTowerSize + 1;
            int targetStartCol = bottomRightCol - targetTowerSize + 1;

            for (int ar = attackerStartRow; ar <= currentRow; ar++) {
                for (int ac = attackerStartCol; ac <= currentCol; ac++) {
                    for (int tr = targetStartRow; tr <= bottomRightRow; tr++) {
                        for (int tc = targetStartCol; tc <= bottomRightCol; tc++) {
                            int rowDiff = Math.abs(tr - ar);
                            int colDiff = Math.abs(tc - ac);
                            double cellDistance = rowDiff + colDiff;
                            if (cellDistance < minDistance) {
                                minDistance = cellDistance;
                            }
                        }
                    }
                }
            }

            distance = minDistance;
        } else {
            int targetRow = -1, targetCol = -1;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (arenaMap.getEntity(r, c) == target) {
                        targetRow = r;
                        targetCol = c;
                        break;
                    }
                }
                if (targetRow >= 0) break;
            }

            if (targetRow < 0 || targetCol < 0) {
                return;
            }

            int attackerTowerSize = tower.isKing() ? 3 : 2;
            int attackerStartRow = currentRow - attackerTowerSize + 1;
            int attackerStartCol = currentCol - attackerTowerSize + 1;

            double minDistance = Double.MAX_VALUE;
            for (int ar = attackerStartRow; ar <= currentRow; ar++) {
                for (int ac = attackerStartCol; ac <= currentCol; ac++) {
                    int rowDiff = Math.abs(targetRow - ar);
                    int colDiff = Math.abs(targetCol - ac);
                    double cellDistance = rowDiff + colDiff;
                    if (cellDistance < minDistance) {
                        minDistance = cellDistance;
                    }
                }
            }

            distance = minDistance;
        }

        AliveCard aliveCard = (AliveCard) tower.getCard();
        double actSpeed = aliveCard.getActSpeed();
        double attackCooldownTime = actSpeed > 0 ? 1.0 / actSpeed : 1.0;

        if (distance <= tower.getRange() + 0.5) {
            double currentCooldown = combatManager.getAttackCooldown(tower);
            if (currentCooldown <= 0) {
                tower.attack(target);
                combatManager.setAttackCooldown(tower, attackCooldownTime);
                entityRenderer.setEntityDirty(true);

                if (target.getHP() <= 0) {
                    // Removal handled by caller
                }
            }
        }
    }

    private void handleSpawnerBuilding(BuildingEntity building, BuildingCard buildingCard){
        UnitCard spawnedUnit = buildingCard.getSpawnedUnit();
        if(spawnedUnit == null) return;

        double spawnInterval = buildingCard.getActSpeed();
        building.addTimeSinceLastSpawn(ENTITY_UPDATE_INTERVAL);

        if(building.getTimeSinceLastSpawn() >= spawnInterval){
            int spawnCount = 1;
            //Barb hut is a special case
            if(buildingCard.getId() == 23){
                spawnCount = 2;
            }

            for(int i=0; i<spawnCount; i++){
                spawnUnitAdjacentToBuilding(building, spawnedUnit);
            }

            building.resetTimeSinceLastSpawn();
        }
    }

    private void spawnUnitAdjacentToBuilding(BuildingEntity building, UnitCard unitCard){
        int buildingRow = building.getRow();
        int buildingCol = building.getCol();
        boolean isPlayer = building.isPlayer();

        //The order for directions:above,below,right,left
        int[][] directions = {{-1, 0}, {1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : directions) {
            int spawnRow = buildingRow + dir[0];
            int spawnCol = buildingCol + dir[1];

            if(spawnRow >= 0 && spawnRow < rows && spawnCol >= 0 && spawnCol < cols){
                boolean canFly = unitCard.getType() == CardType.AIR;
                if (arenaMap.isWalkable(spawnRow, spawnCol, canFly) && 
                    arenaMap.getEntity(spawnRow, spawnCol) == null) {
                    
                    // Create and place unit
                    UnitCard spawnCard = (UnitCard) CardFactory.createCard(unitCard.getId());
                    spawnCard.setCount(1); // Each spawned unit is a single entity
                    UnitEntity spawnedUnit = new UnitEntity(spawnCard, isPlayer);
                    spawnedUnit.setPosition(spawnRow, spawnCol);
                    
                    arenaMap.setEntity(spawnRow, spawnCol, spawnedUnit);
                    arenaMap.placeObject(spawnRow, spawnCol, ArenaObjectType.ENTITY);
                    arenaMap.addEntity(spawnedUnit);
                    
                    entityRenderer.ensureEntityNode(spawnedUnit, spawnCard);
                    entityRenderer.setEntityDirty(true);
                    
                    return;
                }
            }
        }
    }

    private void handleSpecialBuilding(BuildingEntity building, BuildingCard buildingCard){
        if (buildingCard.getId() == 24) {
            double generationInterval = buildingCard.getActSpeed();
            building.addTimeSinceLastElixirGeneration(ENTITY_UPDATE_INTERVAL);
            
            if (building.getTimeSinceLastElixirGeneration() >= generationInterval) {
                
                // Generate an elixir
                if (isPvPMode && dualPlayerStateManager != null) {
                    int playerId = building.isPlayer() ? 1 : 2;
                    double currentElixir = dualPlayerStateManager.getElixir(playerId);
                    dualPlayerStateManager.setElixir(playerId, currentElixir + 1.0);
                } else if (!isPvPMode && gameStateManager != null) {
                    double currentElixir = gameStateManager.getCurrentElixir();
                    gameStateManager.setCurrentElixir(currentElixir + 1.0);
                }
                
                building.incrementElixirGenerated();
                building.resetTimeSinceLastElixirGeneration();
            }
        }
    }

    private String getUnitSpeed(UnitEntity unit) {
        try {
            java.lang.reflect.Method method = UnitEntity.class.getDeclaredMethod("getSpeed");
            method.setAccessible(true);
            return (String) method.invoke(unit);
        } catch (Exception e) {
            return "Medium";
        }
    }

    private double getSpeedMultiplier(String speed) {
        if (speed == null) return 1.0;
        switch (speed.toLowerCase()) {
            case "very fast": return 3.0;
            case "fast": return 2.0;
            case "medium": return 1.0;
            case "slow": return 0.5;
            case "very slow": return 0.25;
            default: return 1.0;
        }
    }
}
