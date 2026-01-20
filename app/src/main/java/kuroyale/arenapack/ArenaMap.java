package kuroyale.arenapack;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import kuroyale.entitiypack.subclasses.AliveEntity;
import kuroyale.entitiypack.subclasses.TowerEntity;

public class ArenaMap {

    private static final int rows = 18;
    private static final int cols = 32;

    private final ArenaTile[][] grid;
    private final ArenaTile[][] collisions;
    private final AliveEntity[][] entityArray;

    private List<AliveEntity> entityList;
    private List<Integer> bridges;

    public ArenaMap() {
        grid = new ArenaTile[rows][cols];
        collisions = new ArenaTile[rows][cols];
        entityArray = new AliveEntity[rows][cols];
        entityList = new LinkedList<>();
        bridges = new LinkedList<>();
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
            if (tile.getPlacedObject() == null) {
                tile.setPlacedObject(new PlacedObject(type));
                return true;
            }
            if (tile.getPlacedObject().getType() == ArenaObjectType.BRIDGE)
                return false;

            tile.setPlacedObject(new PlacedObject(type));
            return true;
        }

        if (type == ArenaObjectType.ENTITY) {
            // Check grid for bridges first (bridges are in grid, not collisions)
            PlacedObject gridObj = grid[row][col].getPlacedObject();

            // If this tile is a bridge, allow entities here (bridges are at columns 15-16)
            if (gridObj != null && gridObj.getType() == ArenaObjectType.BRIDGE) {
                return true;
            }

            // Check if tile is river
            if (grid[row][col].getTileType() == TileType.RIVER) {

            }

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

                if (grid[r][c].getPlacedObject() != null &&
                        grid[r][c].getPlacedObject().getType() == type) {
                    grid[r][c].setPlacedObject(null);
                }
            }
        }
    }

    public void setEntity(int row, int col, AliveEntity entity) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            this.entityArray[row][col] = entity;
        }
    }

    public AliveEntity getEntity(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            return this.entityArray[row][col];
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
                    } else {
                        ArenaObjectType type = ArenaObjectType.valueOf(tok);
                        grid[r][c].setPlacedObject(new PlacedObject(type));
                        int s = (type == ArenaObjectType.ENEMY_TOWER || type == ArenaObjectType.OUR_TOWER) ? 2
                                : (type == ArenaObjectType.ENEMY_KING || type == ArenaObjectType.OUR_KING) ? 3 : 0;

                        // Create a single TowerEntity INSTANCE for all tiles in the tower
                        TowerEntity towerEntity = null;
                        switch (type) {
                            case ArenaObjectType.BRIDGE:
                                bridges.add(r);
                                break;
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

                        // Use the same TowerEntity INSTANCE for all tiles in the tower
                        if (towerEntity != null) {
                            for (int rr = r - s; rr <= r; rr++) {
                                for (int cc = c - s; cc <= c; cc++) {
                                    collisions[rr][cc].setPlacedObject(new PlacedObject(type));
                                    entityArray[rr][cc] = towerEntity; // Same INSTANCE for all tiles
                                }
                            }
                            entityList.add(towerEntity);
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

    public void setObject(int row, int col, ArenaObjectType type) {
        grid[row][col].setPlacedObject(new PlacedObject(type));
    }

    public static int getRows() {
        return rows;
    }

    public static int getCols() {
        return cols;
    }

    // REQUIRES: 0 <= r < ROWS and 0 <= c < COLS (r and c are within map
    // boundaries).
    // The grid array is initialized and contains valid, non-null Tile objects.
    // MODIFIES: None (this is a pure query method and does not alter the state).
    // EFFECTS: Returns true if the unit can traverse the tile at (r, c),
    // specifically:
    // 1. If the tile contains a BRIDGE, returns true (regardless of unit type).
    // 2. If the tile contains any other solid object (e.g., TOWER), returns false.
    // 3. If the tile is a RIVER, returns true ONLY IF 'flying' is true.
    // 4. Otherwise (e.g., empty GRASS tile), returns true.
    public boolean isWalkable(int r, int c, boolean flying) {
        PlacedObject obj = grid[r][c].getPlacedObject();
        if (obj != null) {
            if (obj.getType() == ArenaObjectType.BRIDGE) {
                return true;
            }
            return false;
        }
        if (grid[r][c].getTileType() == TileType.RIVER) {
            return flying;
        }
        // Grass tiles are walkable
        return true;
    }

    public void moveEntitiy(int oldR, int oldC, int newR, int newC) {
        AliveEntity ent = entityArray[oldR][oldC];
        entityArray[newR][newC] = ent;
        entityArray[oldR][oldC] = null;
    }

    public List<AliveEntity> getEntities() {
        return entityList;
    }

    public void addEntity(AliveEntity entity) {
        entityList.add(entity);
    }

    public void removeEntity(AliveEntity entity) {
        entityList.remove(entity);
    }

    public void removeEntity(int row, int col) {
        for (AliveEntity entity : entityList) {
            if (entity.getRow() == row && entity.getCol() == col) {
                entityList.remove(entity);
                return;
            }
        }
    }

    public void clearEntities() {
        entityList.clear();
    }

    public List<Integer> getBridges() {
        return bridges;
    }
}
