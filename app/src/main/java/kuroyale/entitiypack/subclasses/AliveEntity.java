package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.subclasses.AliveCard;
import kuroyale.entitiypack.Entity;
import kuroyale.arenapack.ArenaMap;
import java.util.ArrayDeque;
import java.util.Queue;

public class AliveEntity extends Entity {
    private static final int ROWS = ArenaMap.getRows();
    private static final int COLS = ArenaMap.getCols();
    private static final int[][] directions = {{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1}};
    private static Entity[] ourEntities = new Entity[ROWS * COLS];
    private static Entity[] enemyEntities = new Entity[ROWS * COLS];
    private static short[] ourDistances = new short[ROWS * COLS];
    private static short[] enemyDistances = new short[ROWS * COLS];
    
    private AliveCard card;
    private double HP;

    public AliveEntity(AliveCard card, boolean isPlayer) {
        super(card, isPlayer);
        this.card = card;
        this.HP = card.getHp();
    }

    public void reduceHP(double damage) {
        HP -= damage;
        if (HP < 0) {

        }
    }

    public double getHP() {
        return HP;
    }

    public void setHP(double value) {
        HP = value;
    }

    public double getRange() {
        return card.getRange();
    }

    private int row;
    private int col;

    public void setPosition(int r, int c) {
        row = r;
        col = c;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    public static void resetGraphs(ArenaMap map) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Entity entity = map.getEntity(r, c);
                if (entity instanceof AliveEntity aliveEntity) {
                    if (aliveEntity.isPlayer()) {
                        ourEntities[r * COLS + c] = aliveEntity;
                        ourDistances[r * COLS + c] = 0;
                    } else {
                        enemyEntities[r * COLS + c] = aliveEntity;
                        enemyDistances[r * COLS + c] = 0;
                    }
                } else {
                    ourEntities[r * COLS + c] = null;
                    enemyEntities[r * COLS + c] = null;
                    ourDistances[r * COLS + c] = Short.MAX_VALUE;
                    enemyDistances[r * COLS + c] = Short.MAX_VALUE;
                }
            }
        }
    }

    public static void fillEnemyGraph() {
        // BFS to fill enemyEntities and enemyDistances
        Queue<int[]> queue = new ArrayDeque<>();
        for (int i = 0; i < enemyEntities.length; i++) {
            if (enemyEntities[i] != null) {
                int row = i / COLS;
                int colmun = i % COLS;
                queue.add(new int[]{row, colmun});
            }
        }

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int curDist = enemyDistances[cur[0] * COLS + cur[1]];

            for (int[] dir : directions) {
                int newR = cur[0] + dir[0];
                int newC = cur[1] + dir[1];
                if (newR >= 0 && newR < ROWS && newC >= 0 && newC < COLS) {
                    if (enemyDistances[newR * COLS + newC] > curDist + 1) {
                        enemyDistances[newR * COLS + newC] = (short)(curDist + 1);
                        enemyEntities[newR * COLS + newC] = enemyEntities[cur[0] * COLS + cur[1]];
                        queue.add(new int[]{newR, newC});
                    }
                }
            }
        }
    }

    public static void fillOurGraph() {
        // BFS to fill ourEntities and ourDistances
        Queue<int[]> queue = new ArrayDeque<>();
        for (int i = 0; i < ourEntities.length; i++) {
            if (ourEntities[i] != null) {
                int row = i / COLS;
                int colmun = i % COLS;
                queue.add(new int[]{row, colmun});
            }
        }

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int curDist = ourDistances[cur[0] * COLS + cur[1]];

            for (int[] dir : directions) {
                int newR = cur[0] + dir[0];
                int newC = cur[1] + dir[1];
                if (newR >= 0 && newR < ROWS && newC >= 0 && newC < COLS) {
                    if (ourDistances[newR * COLS + newC] > curDist + 1) {
                        ourDistances[newR * COLS + newC] = (short)(curDist + 1);
                        ourEntities[newR * COLS + newC] = ourEntities[cur[0] * COLS + cur[1]];
                        queue.add(new int[]{newR, newC});
                    }
                }
            }
        }
    }

    public AliveEntity findClosestTarget() {
        short minDist = Short.MAX_VALUE;
        AliveEntity closestEnemy = null;
        Entity[] targetGraph = isPlayer() ? enemyEntities : ourEntities;
        short[] distanceGraph = isPlayer() ? enemyDistances : ourDistances;
        for (int[] dir : directions) {
            int newR = row + dir[0];
            int newC = col + dir[1];
            if (newR >= 0 && newR < ROWS && newC >= 0 && newC < COLS) {
                int index = newR * COLS + newC;
                short dist = distanceGraph[index];
                if (dist < minDist) {
                    minDist = dist;
                    closestEnemy = (AliveEntity) targetGraph[index];
                }
            }
        }
        return closestEnemy;
    }

    /*
    public AliveEntity findClosestTarget(ArenaMap map) {
        boolean[][] visited = new boolean[ROWS][COLS];

        Queue<int[]> q = new ArrayDeque<>();
        q.add(new int[]{row, col});
        visited[row][col] = true;

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int r = cur[0], c = cur[1];

            // Check for enemy entity at current position
            Entity e = map.getEntity(r, c);
            if (e instanceof AliveEntity a && a.isPlayer() != isPlayer() && a.getHP() > 0) {
                return a;
            }

            // Check for enemy towers/kings at current position
            var obj = map.getObject(r, c);
            if (obj != null && obj.getType() != null) {
                switch (obj.getType()) {
                    case ENEMY_TOWER, ENEMY_KING -> {
                        if (isPlayer()) {
                            Entity tower = map.getEntity(r, c);
                            if (tower instanceof AliveEntity towerEntity && towerEntity.getHP() > 0) {
                                return towerEntity;
                            }
                        }
                    }
                    case OUR_TOWER, OUR_KING -> {
                        if (!isPlayer()) {
                            Entity tower = map.getEntity(r, c);
                            if (tower instanceof AliveEntity towerEntity && towerEntity.getHP() > 0) {
                                return towerEntity;
                            }
                        }
                    }
                    default -> {}
                }
            }

            // Expand to walkable neighbors, but also check adjacent non-walkable tiles for towers
            for (int[] d : directions) {
                int nr = r + d[0], nc = c + d[1];
                if (nr >= 0 && nr < ROWS && nc >= 0 && nc < COLS && !visited[nr][nc]) {
                    // Check if this tile has an enemy tower/king (even if not walkable)
                    var neighborObj = map.getObject(nr, nc);
                    if (neighborObj != null && neighborObj.getType() != null) {
                        switch (neighborObj.getType()) {
                            case ENEMY_TOWER, ENEMY_KING -> {
                                if (isPlayer()) {
                                    Entity tower = map.getEntity(nr, nc);
                                    if (tower instanceof AliveEntity towerEntity && towerEntity.getHP() > 0) {
                                        return towerEntity;
                                    }
                                }
                            }
                            case OUR_TOWER, OUR_KING -> {
                                if (!isPlayer()) {
                                    Entity tower = map.getEntity(nr, nc);
                                    if (tower instanceof AliveEntity towerEntity && towerEntity.getHP() > 0) {
                                        return towerEntity;
                                    }
                                }
                            }
                            default -> {}
                        }
                    }
                    
                    // Only add to queue if the tile is walkable (for pathfinding)
                    if (map.isWalkable(nr, nc)) {
                        visited[nr][nc] = true;
                        q.add(new int[]{nr, nc});
                    }
                }
            }
        }
        
        // If BFS didn't find any target through walkable paths, do a direct search for towers/kings
        // This handles cases where towers are on non-walkable tiles
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                var obj = map.getObject(r, c);
                if (obj != null && obj.getType() != null) {
                    switch (obj.getType()) {
                        case ENEMY_TOWER, ENEMY_KING -> {
                            if (isPlayer()) {
                                Entity tower = map.getEntity(r, c);
                                if (tower instanceof AliveEntity towerEntity && towerEntity.getHP() > 0) {
                                    return towerEntity;
                                }
                            }
                        }
                        case OUR_TOWER, OUR_KING -> {
                            if (!isPlayer()) {
                                Entity tower = map.getEntity(r, c);
                                if (tower instanceof AliveEntity towerEntity && towerEntity.getHP() > 0) {
                                    return towerEntity;
                                }
                            }
                        }
                        default -> {}
                    }
                }
            }
        }
        
        return null;
    }
    */

    public void move(ArenaMap map){
        AliveEntity target = findClosestTarget();
        if (target == null) {
            return; // No target found, don't move
        }
        
        int targetC = target.getCol();
        int targetR = target.getRow();
        int currentC = this.col;
        int currentR = this.row;
        
        // Try to move directly toward target first
        int bestDir = -1;
        int minDist = Integer.MAX_VALUE;
        int currentDist = Math.abs(targetR - currentR) + Math.abs(targetC - currentC);
        
        // Find the best direction that gets us closer to target
        for (int i = 0; i < directions.length; i++) {
            int[] d = directions[i];
            int nr = currentR + d[0];
            int nc = currentC + d[1];
            
            if (nr >= 0 && nr < ROWS && nc >= 0 && nc < COLS && map.isWalkable(nr, nc)) {
                // Calculate distance to target from this new position
                int dist = Math.abs(targetR - nr) + Math.abs(targetC - nc);
                if (dist < minDist && dist < currentDist) {
                    minDist = dist;
                    bestDir = i;
                }
            }
        }
        
        // If we can't move directly toward target (path blocked), try to move toward bridge
        if (bestDir < 0) {
            // Find nearest bridge
            int bridgeRow = -1, bridgeCol = -1;
            int minBridgeDist = Integer.MAX_VALUE;
            
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    var obj = map.getObject(r, c);
                    if (obj != null && obj.getType() == kuroyale.arenapack.ArenaObjectType.BRIDGE) {
                        int dist = Math.abs(r - currentR) + Math.abs(c - currentC);
                        if (dist < minBridgeDist) {
                            minBridgeDist = dist;
                            bridgeRow = r;
                            bridgeCol = c;
                        }
                    }
                }
            }
            
            // If we found a bridge, move toward it
            if (bridgeRow >= 0 && bridgeCol >= 0) {
                for (int i = 0; i < directions.length; i++) {
                    int[] d = directions[i];
                    int nr = currentR + d[0];
                    int nc = currentC + d[1];
                    
                    if (nr >= 0 && nr < ROWS && nc >= 0 && nc < COLS && map.isWalkable(nr, nc)) {
                        int dist = Math.abs(bridgeRow - nr) + Math.abs(bridgeCol - nc);
                        int currentBridgeDist = Math.abs(bridgeRow - currentR) + Math.abs(bridgeCol - currentC);
                        if (dist < currentBridgeDist && (bestDir < 0 || dist < minDist)) {
                            minDist = dist;
                            bestDir = i;
                        }
                    }
                }
            }
        }
        
        // If we found a valid direction, move that way
        if (bestDir >= 0) {
            int[] d = directions[bestDir];
            int newR = currentR + d[0];
            int newC = currentC + d[1];
            this.row = newR;
            this.col = newC;
            map.moveEntitiy(currentR, currentC, newR, newC);
        }
    }

    private static String printDistances(boolean isPlayer) {
        short[] graph = isPlayer ? ourDistances : enemyDistances;
        String result = "\n";
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                short val = graph[r * COLS + c];
                if (val == Short.MAX_VALUE) {
                    result += "   ";
                } else if (val == 0) {
                    result += " * ";
                }else {
                    result += (val < 10 ? " " : "") + val + " ";
                }
            }
            result += "\n";
        }
        System.err.println(result);
        return result;
    }

    private static String printEntities(boolean isPlayer) {
        Entity[] graph = isPlayer ? ourEntities : enemyEntities;
        String result = "\n";
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Entity e = graph[r * COLS + c];
                if (e == null) {
                    result += "   ";
                } else if (e instanceof TowerEntity) {
                    result += " * ";
                } else {
                    result += " " + ((AliveEntity)e).getCard().getName().substring(0, 2);
                }
            }
            result += "\n";
        }
        System.err.println(result);
        return result;
    }
}
