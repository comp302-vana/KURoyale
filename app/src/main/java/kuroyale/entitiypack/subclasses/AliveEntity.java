package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.CardType;
import kuroyale.cardpack.subclasses.AliveCard;
import kuroyale.entitiypack.Entity;
import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;

import java.util.ArrayDeque;
import java.util.Queue;

public class AliveEntity extends Entity {
    public static final int[][] directions = { { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 },
            { 0, -1 }, { 1, -1 } };
    private static final int ROWS = ArenaMap.getRows();
    private static final int COLS = ArenaMap.getCols();
    private static AliveEntity[] ourEntities = new AliveEntity[ROWS * COLS];
    private static AliveEntity[] enemyEntities = new AliveEntity[ROWS * COLS];
    private static float[] ourDistances = new float[ROWS * COLS];
    private static float[] enemyDistances = new float[ROWS * COLS];
    private int row;
    private int col;

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

    public boolean canFly() {
        return card.getType() == CardType.AIR;
    }

    public static void resetGraphs(ArenaMap map) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Entity entity = map.getEntity(r, c);
                if (entity instanceof AliveEntity aliveEntity && aliveEntity.getHP() > 0) {
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

    public static void fillGraph(boolean isPlayer) {
        // BFS to fill ourEntities, ourDistances, enemyEntities, and enemyDistances
        float[] targetDistances = isPlayer ? enemyDistances : ourDistances;
        AliveEntity[] targetEntities = isPlayer ? enemyEntities : ourEntities;
        Queue<int[]> queue = new ArrayDeque<>();
        for (int i = 0; i < targetEntities.length; i++) {
            if (targetEntities[i] != null && targetEntities[i].getHP() > 0) {
                int row = i / COLS;
                int colmun = i % COLS;
                queue.add(new int[] { row, colmun });
            }
        }

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            float curDist = targetDistances[cur[0] * COLS + cur[1]];

            for (int[] dir : directions) {
                boolean diagonal = (dir[0] != 0 && dir[1] != 0);
                float dist = diagonal ? 1.4f : 1.0f;
                int newR = cur[0] + dir[0];
                int newC = cur[1] + dir[1];
                if (newR >= 0 && newR < ROWS && newC >= 0 && newC < COLS) {
                    if (targetDistances[newR * COLS + newC] > curDist + dist) {
                        targetDistances[newR * COLS + newC] = (curDist + dist);
                        targetEntities[newR * COLS + newC] = targetEntities[cur[0] * COLS + cur[1]];
                        queue.add(new int[] { newR, newC });
                    }
                }
            }
        }
    }

    public AliveEntity findClosestTarget(ArenaMap arenaMap) {
        float minDist = Float.MAX_VALUE;
        AliveEntity closestEnemy = null;
        AliveEntity[] targetGraph = isPlayer() ? enemyEntities : ourEntities;
        float[] distanceGraph = isPlayer() ? enemyDistances : ourDistances;
        for (int[] dir : directions) {
            int newR = row + dir[0];
            int newC = col + dir[1];
            if (newR >= 0 && newR < ROWS && newC >= 0 && newC < COLS) {
                if (!canFly() && !arenaMap.isWalkable(newR, newC, canFly()))
                    continue;
                int index = newR * COLS + newC;
                float dist = distanceGraph[index];
                if (dist < minDist && targetGraph[index] != null) {
                    minDist = dist;
                    closestEnemy = targetGraph[index];
                }
            }
        }
        return closestEnemy;
    }

    public void move(ArenaMap map, AliveEntity target) {
        if (target == null) {
            return; // No target found, don't move
        }

        int targetC = target.getCol();
        int targetR = target.getRow();
        int currentC = this.col;
        int currentR = this.row;

        if ((((currentC < COLS / 2 - 1) && (targetC > COLS / 2 + 1)) ||
                ((currentC > COLS / 2 + 1) && (targetC < COLS / 2 - 1))) && !canFly()) {
            int closestBridgeRow = targetR;
            int closestBridgeDist = ROWS;

            for (int r : map.getBridges()) {
                int bridgeDist = Math.abs(targetR - r);
                if (bridgeDist < closestBridgeDist) {
                    closestBridgeDist = bridgeDist;
                    closestBridgeRow = r;
                }
            }
            targetC = COLS / 2;
            targetR = closestBridgeRow;
        }

        // Try to move directly toward target first
        int[] bestDir = null;
        double minDist = ROWS*ROWS + COLS*COLS;

        int nr, nc;
        // Find the best direction that gets us closer to target
        for (int[] direciton : directions) {
            nr = currentR + direciton[0];
            nc = currentC + direciton[1];

            // in bounds AND is walkable AND no entity AND (is null OR bridge)
            if (
                    nr >= 0 && nr < ROWS && nc >= 0 && nc < COLS && map.isWalkable(nr, nc, canFly())
                    && map.getEntity(nr, nc) == null &&
                    (map.getObject(nr, nc) == null || map.getObject(nr, nc).getType() == ArenaObjectType.BRIDGE)) {

                // Calculate distance to target from this new position
                double dist = Math.sqrt(Math.pow((targetR - nr), 2) + Math.pow((targetC - nc), 2));
                if (dist < minDist) {
                    minDist = dist;
                    bestDir = direciton;
                }
            }
        }

        // If we found a valid direction, move that way
        if (bestDir != null) {
            int newR = currentR + bestDir[0];
            int newC = currentC + bestDir[1];
            this.row = newR;
            this.col = newC;
            map.moveEntitiy(currentR, currentC, newR, newC);
        }
    }
}
