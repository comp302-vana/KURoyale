package kuroyale.arenapack;

import javax.swing.text.html.parser.Entity;

public class PlacedObject {

    private final ArenaObjectType type;
    private Entity entity;

    public PlacedObject(ArenaObjectType type) {
        this.type = type;
    }

    public ArenaObjectType getType() {
        return type;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
