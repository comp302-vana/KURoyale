package kuroyale.arenapack;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ArenaMap {

    private final int rows = 18;
    private final int cols = 10;

    private final ArenaTile[][] grid;

    public ArenaMap() {
        grid = new ArenaTile[rows][cols];
        initTiles();
    }


    private void initTiles() {

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                TileType type;

                if (r >= 0 && r <= 6)
                    type = TileType.GRASS;
                else if (r >= 7 && r <= 9)
                    type = TileType.RIVER;
                else
                    type = TileType.GRASS;

                grid[r][c] = new ArenaTile(r, c, type);
            }
        }
    }

    public boolean placeObject(int row, int col, ArenaObjectType type) {

        ArenaTile tile = grid[row][col];

        if (tile.getTileType() == TileType.RIVER)
            return false;

        if (!tile.isEmpty())
            return false;

        tile.setPlacedObject(new PlacedObject(type));
        return true;
    }

    public void clearObject(int row, int col) {
        grid[row][col].setPlacedObject(null);
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

                    if (obj == null) pw.print("EMPTY");
                    else pw.print(obj.getType().name());

                    if (c < cols - 1) pw.print(",");
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
                if (line == null) break;

                String[] tokens = line.split(",");

                for (int c = 0; c < cols; c++) {

                    String tok = tokens[c];

                    if (tok.equals("EMPTY")) {
                        grid[r][c].setPlacedObject(null);
                    } else {
                        ArenaObjectType type = ArenaObjectType.valueOf(tok);
                        grid[r][c].setPlacedObject(new PlacedObject(type));
                    }
                }
            }

            System.out.println("Arena loaded from: " + filePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

public PlacedObject getObject(int row, int col) {
    if (row < 0 || row >= 18 || col < 0 || col >= 10)
        return null;

    return grid[row][col].getPlacedObject();
}


}
