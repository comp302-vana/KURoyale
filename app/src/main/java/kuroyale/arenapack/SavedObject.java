package kuroyale.arenapack;

public class SavedObject {
    public final int row;
    public final int col;
    public final ArenaObjectType type;

    public SavedObject(int row, int col, ArenaObjectType type) {
        this.row = row;
        this.col = col;
        this.type = type;
    }
}
