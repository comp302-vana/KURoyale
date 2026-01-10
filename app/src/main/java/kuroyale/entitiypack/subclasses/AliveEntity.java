package kuroyale.entitiypack.subclasses;

import kuroyale.arenapack.ArenaMap;
import kuroyale.arenapack.ArenaObjectType;
import kuroyale.cardpack.CardTarget;
import kuroyale.cardpack.CardType;
import kuroyale.cardpack.subclasses.AliveCard;
import kuroyale.entitiypack.Entity;

public class AliveEntity extends Entity {
    public static final int[][] directions = { { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 },
            { 0, -1 }, { 1, -1 } };
    private static final int ROWS = ArenaMap.getRows();
    private static final int COLS = ArenaMap.getCols();
    private int row;
    private int col;

    private AliveCard card;
    private double HP;
    private long entityId = -1; // Network entity ID, -1 means not assigned yet

    public AliveEntity(AliveCard card, boolean isPlayer) {
        super(card, isPlayer);
        this.card = card;
        this.HP = card.getHp();
    }
    
    /**
     * Get the network entity ID.
     * Returns -1 if not assigned yet.
     */
    public long getEntityId() {
        return entityId;
    }
    
    /**
     * Set the network entity ID.
     * Should only be called once when entity is created on host.
     */
    public void setEntityId(long entityId) {
        this.entityId = entityId;
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
        // REQUIRES: -arenaMap is not null 
        // -arenaMap contains a valid list
        // of entities with at least one entity available.
        // -this.row and this.col are within the boundary of the arena.
        // EFFECTS: Returns an AliveEntity "closestEnemy" such that:
        // -closestEnemy.isPlayer() != this.isPlayer()
        // -closestEnemy is a target of this.card
        // -Euclidian distance between "this" and "closestEnemy" is the minimum
        // compared to the other valid candidates
        // -Returns null if no entity satisfies this conditions
        // -uses Euclidian distance
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

    /**
     * Operation: move
     * 
     * Responsibilities:
     * - Moves the entity one step toward the target entity.
     * - Handles bridge crossing for non-flying ground units when the target is across the river.
     * - Finds the best valid direction (using 8-directional movement) that minimizes the distance to the target.
     * - Updates the entity's position and the arena map state.
     * 
     * Requires:
     * - map != null (The map object must not be null).
     * - target != null (The target entity must not be null).
     * - this.entity must have a valid position within the arena bounds (0 <= row < ROWS, 0 <= col < COLS).
     * - map.getBridges() != null (This is required specifically when bridge crossing is needed).
     * 
     * Bridge Crossing Logic (for non-flying units):
     * - If the entity is on the left side (currentC < COLS/2 - 1) and the target is on the right side (targetC > (COLS+1)/2).
     * - If the entity is on the right side (currentC > (COLS+1)/2 + 1) and the target is on the left side (targetC < COLS/2).
     * - In these cases, movement is redirected toward the closest bridge (at COLS/2 column) at the closest bridge row.
     * - Flying units (canFly() == true) skip this bridge logic and move directly toward the target.
     * 
     * Movement Validation:
     * - The new position (nr, nc) must be within arena bounds (nr >= 0 && nr < ROWS && nc >= 0 && nc < COLS).
     * - The new position must be walkable based on the unit type (map.isWalkable(nr, nc, canFly())).
     * - The new position must not have an existing entity (map.getEntity(nr, nc) == null).
     * - The new position must be empty OR contain a bridge (map.getObject(nr, nc) == null || is BRIDGE).
     * 
     * Direction Selection:
     * - Evaluates all 8 possible directions: {1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1}, {0,-1}, {1,-1}.
     * - Selects the direction that minimizes the squared Euclidean distance to the target.
     * - The distance is calculated using the formula: dist = (nc - targetC)^2 + (nr - targetR)^2.
     * 
     * Ensures:
     * - If target == null, the method returns immediately with no state changes.
     * - If no valid direction is found, the entity's position remains unchanged.
     * - If a valid direction is found:
     *   - this.row and this.col are updated to the new position.
     *   - map.moveEntity(oldRow, oldCol, newRow, newCol) is called to update the arena map.
     *   - The new position is exactly one step (in the best valid direction) closer to the target.
     * 
     * @param map The arena map containing the game state
     * @param target The target entity to move toward
     */
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
