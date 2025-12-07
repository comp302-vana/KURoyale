package kuroyale.arenapack;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import kuroyale.entitiypack.Entity;
import kuroyale.entitiypack.subclasses.TowerEntity;

public class ArenaMap {

    private final int rows = 18;
    private final int cols = 32;

    private final ArenaTile[][] grid;
    private final ArenaTile[][] collisions;
    private final Entity[][] entities;

    public ArenaMap() {
        grid = new ArenaTile[rows][cols];
        collisions = new ArenaTile[rows][cols];
        entities = new Entity[rows][cols];
        initTiles();
    }

    private void initTiles() {

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                TileType type;

                if (c >= 0 && c < cols/2 - 1)
                    type = TileType.GRASS;
                else if (c >= cols/2 - 1 && c <= cols/2)
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
            if (col % 2 == 1)
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
        
        if (grid[row][col].getTileType() == TileType.RIVER)
            return false;
        
        if (type == ArenaObjectType.ENTITY) {
            if (col >= cols/2-1) {
                return false;
            } else if (collisions[row][col].getPlacedObject() == null) {
                tile.setPlacedObject(new PlacedObject(type));
                return true;
            } else if (collisions[row][col].getPlacedObject().getType() != ArenaObjectType.ENTITY) {
                return false;
            }

            tile.setPlacedObject(new PlacedObject(type));
            return true;
        }
        
        int s = (type == ArenaObjectType.ENEMY_TOWER || type == ArenaObjectType.OUR_TOWER) ? 2 :
        (type == ArenaObjectType.ENEMY_KING || type == ArenaObjectType.OUR_KING) ? 3 : 0;
        
        for (int r = row-s; r<=row; r++) {
            for (int c = col-s; c<=col; c++) {
                if (!collisions[r][c].isEmpty())
                    return false;
            }
        }

        grid[row][col].setPlacedObject(new PlacedObject(type));
        
        for (int r = row-s; r<=row; r++) {
            for (int c = col-s; c<=col; c++) {
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
        grid[row][col].setPlacedObject(null);
        
        int s = (type == ArenaObjectType.ENEMY_TOWER || type == ArenaObjectType.OUR_TOWER) ? 2 :
        (type == ArenaObjectType.ENEMY_KING || type == ArenaObjectType.OUR_KING) ? 3 : 0;

        for (int r = row-s; r<=row; r++) {
            for (int c = col-s; c<=col; c++) {
                collisions[r][c].setPlacedObject(null);
            }
        }
    }

    public void setEntity(int row, int col, Entity entity) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            this.entities[row][col] = entity;
        }
    }

    public Entity getEntity(int row, int col) {
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
                    } else {
                        ArenaObjectType type = ArenaObjectType.valueOf(tok);
                        grid[r][c].setPlacedObject(new PlacedObject(type));
                        int s = (type == ArenaObjectType.ENEMY_TOWER || type == ArenaObjectType.OUR_TOWER) ? 2 :
                        (type == ArenaObjectType.ENEMY_KING || type == ArenaObjectType.OUR_KING) ? 3 : 0;

                        for (int rr = r-s; rr<=r; rr++) {
                            for (int cc = c-s; cc<=c; cc++) {
                                collisions[rr][cc].setPlacedObject(new PlacedObject(type));
                                switch (type) {
                                    case ArenaObjectType.OUR_KING:
                                        entities[rr][cc] = new TowerEntity(true, true);
                                        break;
                                    case ArenaObjectType.ENEMY_KING:
                                        entities[rr][cc] = new TowerEntity(true, false);
                                        break;
                                    case ArenaObjectType.OUR_TOWER:
                                        entities[rr][cc] = new TowerEntity(false, true);
                                        break;
                                    case ArenaObjectType.ENEMY_TOWER:
                                        entities[rr][cc] = new TowerEntity(false, false);
                                        break;
                                    default:
                                        break;
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

    public int getRows() {return rows;}
    public int getCols() {return cols;}

    public boolean isWalkable(int r, int c){
        if (grid[r][c].getPlacedObject()!=null){
            return false;
        }
        if (grid[r][c].getTileType()==TileType.RIVER){
            return false;
        }
        return true;
    }
    public void moveEntitiy(int oldR, int oldC, int newR, int newC){
        Entity entitiy=entities[oldR][oldC];
        entities[newR][newC]=entitiy;
        entities[oldR][oldC]=null;
    }
}
