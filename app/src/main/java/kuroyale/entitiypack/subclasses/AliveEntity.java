package kuroyale.entitiypack.subclasses;

import kuroyale.cardpack.CardTarget;
import kuroyale.cardpack.CardType;
import kuroyale.cardpack.subclasses.AliveCard;
import kuroyale.entitiypack.Entity;
import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;

public class AliveEntity extends Entity {
    public static final int[][] directions = { { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 },
            { 0, -1 }, { 1, -1 } };
    private static final int ROWS = ArenaMap.getRows();
    private static final int COLS = ArenaMap.getCols();
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

    public AliveEntity findClosestTarget(ArenaMap arenaMap) {

        int minDist = ROWS*ROWS + COLS*COLS;
        AliveEntity closestEnemy = null;

        for (AliveEntity candidate : arenaMap.getEntities()) {
            if (isPlayer() == candidate.isPlayer()) continue;

            CardType candidateType = candidate.getCard().getType();
            switch (card.getTarget()) {
                case CardTarget.BUILDINGS:
                    if (candidateType != CardType.BUILDING) continue;
                    break;
                case CardTarget.GROUND:
                    if (candidateType == CardType.AIR) continue;
                    break;
                default:
                    break;
            }

            int dRow = candidate.getRow() - this.row;
            int dCol = candidate.getCol() - this.col;
            int distance = dRow*dRow + dCol*dCol;
            if (distance < minDist) {
                minDist = distance;
                closestEnemy = candidate;
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

        if ((((currentC < COLS / 2 - 1) && (targetC > (COLS + 1) / 2 + 1)) ||
                ((currentC > (COLS + 1) / 2 + 1) && (targetC < COLS / 2 - 1))) && !canFly()) {
            int closestBridgeRow = currentR;
            int closestBridgeDist = ROWS;

            for (int r : map.getBridges()) {
                int bridgeDist = Math.abs(currentR - r);
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
        
        int nr, nc, dx, dy;
        dx = currentC - targetC;
        dy = currentR - targetR;
        int minDist = dx*dx + dy*dy;
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
                
                dx = nc - targetC;
                dy = nr - targetR;
                int dist = dx*dx + dy*dy;
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
