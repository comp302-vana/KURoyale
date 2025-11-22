package kuroyale.arenapack;

public class PlacedObject {

    private final ArenaObjectType type;

    public PlacedObject(ArenaObjectType type) {
        this.type = type;
    }

    public ArenaObjectType getType() {
        return type;
    }
}
