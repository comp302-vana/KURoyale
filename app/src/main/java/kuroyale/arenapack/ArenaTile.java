package kuroyale.arenapack;

public class ArenaTile {

    private final int row;
    private final int col;
    private final TileType tileType;

    private PlacedObject placedObject; 

    public ArenaTile(int row, int col, TileType tileType) {
        this.row = row;
        this.col = col;
        this.tileType = tileType;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public TileType getTileType() {
        return tileType;
    }

    public PlacedObject getPlacedObject() {
        return placedObject;
    }

    public void setPlacedObject(PlacedObject placedObject) {
        this.placedObject = placedObject;
    }

    public boolean isEmpty() {
        return placedObject == null;
    }
}
