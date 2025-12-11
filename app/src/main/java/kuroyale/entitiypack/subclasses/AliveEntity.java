package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.subclasses.AliveCard;
import kuroyale.entitiypack.Entity;
import kuroyale.arenapack.ArenaMap;
import java.util.ArrayDeque;
import java.util.Queue;

public class AliveEntity extends Entity {
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

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public AliveEntity findClosestTarget(ArenaMap map) {
        int rows = map.getRows();
        int cols = map.getCols();
        boolean[][] visited = new boolean[rows][cols];

        Queue<int[]> q = new ArrayDeque<>();
        q.add(new int[] { row, col });
        visited[row][col] = true;

        int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

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
                    default -> {
                    }
                }
            }

            // Expand to walkable neighbors, but also check adjacent non-walkable tiles for
            // towers
            for (int[] d : dirs) {
                int nr = r + d[0], nc = c + d[1];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !visited[nr][nc]) {
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
                            default -> {
                            }
                        }
                    }

                    // Only add to queue if the tile is walkable (for pathfinding)
                    if (map.isWalkable(nr, nc)) {
                        visited[nr][nc] = true;
                        q.add(new int[] { nr, nc });
                    }
                }
            }
        }

        // If BFS didn't find any target through walkable paths, do a direct search for
        // towers/kings
        // This handles cases where towers are on non-walkable tiles
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
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
                        default -> {
                        }
                    }
                }
            }
        }

        return null;
    }

    public void move(ArenaMap map) {
        AliveEntity target = findClosestTarget(map);
        if (target == null) {
            return; // No target found, don't move
        }

        int targetC = target.getCol();
        int targetR = target.getRow();
        int currentC = this.col;
        int currentR = this.row;

        // Use BFS pathfinding
        int[] nextStep = map.findPath(currentR, currentC, targetR, targetC);

        if (nextStep != null) {
            int newR = nextStep[0];
            int newC = nextStep[1];

            this.row = newR;
            this.col = newC;
            map.moveEntitiy(currentR, currentC, newR, newC);
        } else {
            // Fallback: if no path found (usually shouldn't happen if target is reachable),
            // stay put
            // Or try to move closer blindly (old logic) if pathfinding fails (e.g. target
            // is fully blocked)
            // For now, staying put is safer than glitches.
        }
    }

}
