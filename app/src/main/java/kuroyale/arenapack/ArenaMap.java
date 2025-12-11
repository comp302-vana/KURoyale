package kuroyale.arenapack;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import kuroyale.entitiypack.Entity;
import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.entitiypack.subclasses.TowerEntity;

public class ArenaMap {

    private final int rows = 18;
    private final int cols = 32;

    private final ArenaTile[][] grid;
    private final ArenaTile[][] collisions;
    private final AliveEntity[][] entities;

    public ArenaMap() {
        grid = new ArenaTile[rows][cols];
        collisions = new ArenaTile[rows][cols];
        entities = new AliveEntity[rows][cols];
        initTiles();
    }

    private void initTiles() {

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                TileType type;

                if (c >= 0 && c < cols / 2 - 1)
                    type = TileType.GRASS;
                else if (c >= cols / 2 - 1 && c <= cols / 2)
                    type = TileType.RIVER;
                else
                    type = TileType.GRASS;

                grid[r][c] = new ArenaTile(r, c, type);
                collisions[r][c] = new ArenaTile(r, c, type);
            }
        }
    }

    public boolean placeObject(int row, int col, ArenaObjectType type) {

        ArenaTile tile = grid[row][col];
        if (type == ArenaObjectType.BRIDGE) {

            if (tile.getTileType() != TileType.RIVER)
                return false;
            if (col % 2 == 1) // Only allow placing on even column (right side of river pair)
                return false;

            // Should place on BOTH tiles of the river width (col and col-1)
            // col is 16, col-1 is 15. The river is at 15 and 16.
            // Wait, river is at `c >= cols/2 - 1 && c <= cols/2`.
            // COLS=32. COLS/2=16. River is 15, 16.
            // If col=16 (even), then col-1=15.

            if (tile.getPlacedObject() == null) {
                // Set object on THIS tile
                tile.setPlacedObject(new PlacedObject(type));

                // Set logic on LEFT tile too if it's river
                if (col > 0 && grid[row][col - 1].getTileType() == TileType.RIVER) {
                    grid[row][col - 1].setPlacedObject(new PlacedObject(type));
                    collisions[row][col - 1].setPlacedObject(new PlacedObject(type));
                }

                return true;
            }
            if (tile.getPlacedObject().getType() == ArenaObjectType.BRIDGE)
                return false;

            tile.setPlacedObject(new PlacedObject(type));
            // Ensure left tile also updated
            if (col > 0 && grid[row][col - 1].getTileType() == TileType.RIVER) {
                grid[row][col - 1].setPlacedObject(new PlacedObject(type));
                collisions[row][col - 1].setPlacedObject(new PlacedObject(type));
            }
            return true;
        }

        if (type == ArenaObjectType.ENTITY) {
            // Check grid for bridges first (bridges are in grid, not collisions)
            PlacedObject gridObj = grid[row][col].getPlacedObject();

            // If this tile is a bridge, allow entities here (bridges are at columns 15-16)
            if (gridObj != null && gridObj.getType() == ArenaObjectType.BRIDGE) {
                // Bridge tile - entity can walk on bridge, but don't overwrite bridge object
                // Just return true to indicate placement is allowed (entity is tracked
                // separately in entities grid)
                return true;
            }

            // TEMPORARY: Allow entities on river tiles for testing
            // Check if tile is river - temporarily allow entities on river tiles
            if (grid[row][col].getTileType() == TileType.RIVER) {
                // River tiles are temporarily walkable for testing
                // Just return true to allow placement
            }

            // REMOVED: Side-restriction - movement should be free to cross to enemy side
            // Spawn restriction is now enforced in the drag-drop handler instead

            // Check collisions grid for blocking objects
            PlacedObject collisionObj = collisions[row][col].getPlacedObject();

            // Also check grid for non-bridge objects that might block
            if (gridObj != null && gridObj.getType() != ArenaObjectType.BRIDGE
                    && gridObj.getType() != ArenaObjectType.ENTITY) {
                // There's a non-bridge, non-entity object in grid - block placement
                System.out.println("Blocked by grid object: " + gridObj.getType() + " at (" + row + "," + col + ")");
                return false;
            }

            if (collisionObj == null) {
                // Empty tile - can place entity (but only if grid doesn't have blocking object)
                if (gridObj == null || gridObj.getType() == ArenaObjectType.ENTITY) {
                    tile.setPlacedObject(new PlacedObject(type));
                    return true;
                }
                System.out.println("Cannot place - grid has non-entity object at (" + row + "," + col + ")");
                return false;
            } else if (collisionObj.getType() == ArenaObjectType.ENTITY) {
                // Already has entity - can replace
                tile.setPlacedObject(new PlacedObject(type));
                return true;
            } else {
                // Other object type blocks entity placement
                System.out.println(
                        "Blocked by collision object: " + collisionObj.getType() + " at (" + row + "," + col + ")");
                return false;
            }
        }

        // For non-ENTITY types, check river restriction
        if (grid[row][col].getTileType() == TileType.RIVER && type != ArenaObjectType.BRIDGE) {
            return false;
        }

        int s = (type == ArenaObjectType.ENEMY_TOWER || type == ArenaObjectType.OUR_TOWER) ? 2
                : (type == ArenaObjectType.ENEMY_KING || type == ArenaObjectType.OUR_KING) ? 3 : 0;

        for (int r = row - s; r <= row; r++) {
            for (int c = col - s; c <= col; c++) {
                if (!collisions[r][c].isEmpty())
                    return false;
            }
        }

        grid[row][col].setPlacedObject(new PlacedObject(type));

        for (int r = row - s; r <= row; r++) {
            for (int c = col - s; c <= col; c++) {
                collisions[r][c].setPlacedObject(new PlacedObject(type));
            }
        }
        return true;
    }

    public void clearObject(int row, int col) {
        if (grid[row][col].getTileType() == TileType.RIVER)
            col += col % 2;
        if (grid[row][col].getPlacedObject() == null)
            return;
        ArenaObjectType type = grid[row][col].getPlacedObject().getType();

        // NEVER clear bridges - they're permanent structures
        if (type == ArenaObjectType.BRIDGE) {
            return;
        }

        // Only clear ENTITY objects, not bridges or other static objects
        if (type == ArenaObjectType.ENTITY) {
            grid[row][col].setPlacedObject(null);
            // Also clear from collisions if it was there
            if (collisions[row][col].getPlacedObject() != null &&
                    collisions[row][col].getPlacedObject().getType() == ArenaObjectType.ENTITY) {
                collisions[row][col].setPlacedObject(null);
            }
            return;
        }

        // For towers/kings, clear the full area
        grid[row][col].setPlacedObject(null);
        int s = (type == ArenaObjectType.ENEMY_TOWER || type == ArenaObjectType.OUR_TOWER) ? 2
                : (type == ArenaObjectType.ENEMY_KING || type == ArenaObjectType.OUR_KING) ? 3 : 0;

        for (int r = row - s; r <= row; r++) {
            for (int c = col - s; c <= col; c++) {
                collisions[r][c].setPlacedObject(null);
            }
        }
    }

    public void setEntity(int row, int col, AliveEntity entity) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            this.entities[row][col] = entity;
        }
    }

    public AliveEntity getEntity(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            return this.entities[row][col];
        }
        return null;
    }

    /** GET ALL PLACED OBJECTS WITH COORDINATES */
    public List<SavedObject> getAllObjects() {
        List<SavedObject> list = new ArrayList<>();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                PlacedObject p = grid[r][c].getPlacedObject();
                if (p != null) {
                    list.add(new SavedObject(r, c, p.getType()));
                }
            }
        }
        return list;
    }

    /** CLEAR ALL TILES */
    private void clearAll() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].setPlacedObject(null);
                collisions[r][c].setPlacedObject(null);
            }
        }
    }

    /** LOAD LIST INTO MAP */
    public void loadObjects(List<SavedObject> list) {
        clearAll();
        for (SavedObject s : list) {
            placeObject(s.row, s.col, s.type);
        }
    }

    /** SAVE TO CSV */
    public void saveToFile(String filePath) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {

                    PlacedObject obj = grid[r][c].getPlacedObject();

                    if (obj == null)
                        pw.print("EMPTY");
                    else
                        pw.print(obj.getType().name());

                    if (c < cols - 1)
                        pw.print(",");
                }
                pw.println();
            }

            System.out.println("Arena saved to: " + filePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** LOAD CSV */
    public void loadFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            for (int r = 0; r < rows; r++) {

                String line = br.readLine();
                if (line == null)
                    break;

                String[] tokens = line.split(",");

                for (int c = 0; c < cols; c++) {

                    String tok = tokens[c];

                    if (tok.equals("EMPTY")) {
                        grid[r][c].setPlacedObject(null);
                        collisions[r][c].setPlacedObject(null);
                    } else {
                        ArenaObjectType type = ArenaObjectType.valueOf(tok);
                        grid[r][c].setPlacedObject(new PlacedObject(type));
                        collisions[r][c].setPlacedObject(new PlacedObject(type));

                        // BRIDGE FIX: Ensure bridges cover both river tiles (left and right)
                        if (type == ArenaObjectType.BRIDGE) {
                            if (c > 0 && grid[r][c - 1].getTileType() == TileType.RIVER) {
                                grid[r][c - 1].setPlacedObject(new PlacedObject(type));
                                collisions[r][c - 1].setPlacedObject(new PlacedObject(type));
                            }
                        }

                        int s = (type == ArenaObjectType.ENEMY_TOWER || type == ArenaObjectType.OUR_TOWER) ? 2
                                : (type == ArenaObjectType.ENEMY_KING || type == ArenaObjectType.OUR_KING) ? 3 : 0;

                        // Create a single TowerEntity instance for all tiles in the tower
                        TowerEntity towerEntity = null;
                        switch (type) {
                            case ArenaObjectType.OUR_KING:
                                towerEntity = new TowerEntity(true, true);
                                towerEntity.setPosition(r, c); // Set position to the bottom-right corner
                                break;
                            case ArenaObjectType.ENEMY_KING:
                                towerEntity = new TowerEntity(true, false);
                                towerEntity.setPosition(r, c);
                                break;
                            case ArenaObjectType.OUR_TOWER:
                                towerEntity = new TowerEntity(false, true);
                                towerEntity.setPosition(r, c);
                                break;
                            case ArenaObjectType.ENEMY_TOWER:
                                towerEntity = new TowerEntity(false, false);
                                towerEntity.setPosition(r, c);
                                break;
                            default:
                                break;
                        }

                        // Use the same TowerEntity instance for all tiles in the tower
                        if (towerEntity != null) {
                            for (int rr = r - s; rr <= r; rr++) {
                                for (int cc = c - s; cc <= c; cc++) {
                                    collisions[rr][cc].setPlacedObject(new PlacedObject(type));
                                    entities[rr][cc] = towerEntity; // Same instance for all tiles
                                }
                            }
                        }
                    }
                }
            }

            System.out.println("Arena loaded from: " + filePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PlacedObject getObject(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols)
            return null;

        return grid[row][col].getPlacedObject();
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public boolean isWalkable(int r, int c) {
        // Check collisions grid for blocking objects (Towers/Kings occupy multiple
        // tiles)
        if (!collisions[r][c].isEmpty()) {
            // It has an object, but is it a bridge?
            PlacedObject colObj = collisions[r][c].getPlacedObject();
            if (colObj != null && colObj.getType() == ArenaObjectType.BRIDGE) {
                return true;
            }
            // Not a bridge, so it's a blocking object (Tower, King, etc.)
            return false;
        }

        // Check grid objects (fallback if not in collision but in grid)
        PlacedObject obj = grid[r][c].getPlacedObject();
        if (obj != null) {
            // Bridges are walkable
            if (obj.getType() == ArenaObjectType.BRIDGE) {
                return true;
            }
            // Other objects are not walkable
            return false;
        }

        // If collision grid is empty, check river
        if (grid[r][c].getTileType() == TileType.RIVER) {
            // River tiles are NOT walkable unless there is a bridge (already checked above)
            return false;
        }

        // Grass tiles are walkable
        return true;
    }

    public void moveEntitiy(int oldR, int oldC, int newR, int newC) {
        AliveEntity ent = entities[oldR][oldC];
        entities[newR][newC] = ent;
        entities[oldR][oldC] = null;
    }

    /**
     * Findings the next step to take to reach the target using BFS.
     * Returns int[]{nextRow, nextCol} or null if no path found.
     */
    public int[] findPath(int startR, int startC, int targetR, int targetC) {
        // BFS for shortest path
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        queue.add(new int[] { startR, startC });

        boolean[][] visited = new boolean[rows][cols];
        visited[startR][startC] = true;

        // Store parent pointers to reconstruct path: parent[r][c] = {prevR, prevC}
        int[][][] parent = new int[rows][cols][2];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                parent[r][c] = new int[] { -1, -1 };

        boolean found = false;
        int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } }; // R, L, D, U

        // Best-effort tracking: keep track of closest tile to target we found
        int[] closestTile = null;
        double minDst = Double.MAX_VALUE;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0];
            int c = curr[1];

            // Check if we reached target
            if (r == targetR && c == targetC) {
                found = true;
                break;
            }

            // Update best effort
            double dst = Math.abs(r - targetR) + Math.abs(c - targetC);
            if (dst < minDst) {
                minDst = dst;
                closestTile = curr;
            }

            for (int[] d : dirs) {
                int nr = r + d[0];
                int nc = c + d[1];

                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !visited[nr][nc]) {
                    // It's walkable IF: returns true OR it is the target destination (attacking)
                    boolean walkable = isWalkable(nr, nc);

                    // Allow moving into the target tile (even if occupied by enemy) so we path
                    // towards it
                    if (nr == targetR && nc == targetC) {
                        walkable = true;
                    }

                    if (walkable) {
                        visited[nr][nc] = true;
                        parent[nr][nc][0] = r;
                        parent[nr][nc][1] = c;
                        queue.add(new int[] { nr, nc });
                    }
                }
            }
        }

        // Destination for backtracking
        int destR = targetR;
        int destC = targetC;

        if (!found) {
            // Path not found to exact target. Use closest reachable tile.
            if (closestTile != null && (closestTile[0] != startR || closestTile[1] != startC)) {
                destR = closestTile[0];
                destC = closestTile[1];
                found = true;
            } else {
                return null;
            }
        }

        if (found) {
            // Reconstruct path backwards from dest
            int curR = destR;
            int curC = destC;

            // Backtrack until parent is start
            int limit = 2000;
            while (limit-- > 0) {
                int pr = parent[curR][curC][0];
                int pc = parent[curR][curC][1];

                if (pr == startR && pc == startC) {
                    // curR, curC is the immediate next step
                    return new int[] { curR, curC };
                }

                if (pr == -1) {// Should not happen if found logic correct
                    // Unless start == dest
                    if (curR == startR && curC == startC)
                        return null;
                    return null;
                }

                curR = pr;
                curC = pc;
            }
        }

        return null;
    }
}
