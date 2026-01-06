package kuroyale.mainpack.network;

import java.util.HashMap;
import java.util.Map;
import kuroyale.entitiypack.subclasses.AliveEntity;

/**
 * Registry for tracking entities by their network IDs.
 * Used for synchronization between host and client.
 */
public class EntityRegistry {
    private final Map<Long, AliveEntity> entitiesById;
    
    public EntityRegistry() {
        this.entitiesById = new HashMap<>();
    }
    
    /**
     * Register an entity with its network ID.
     */
    public void registerEntity(long entityId, AliveEntity entity) {
        entitiesById.put(entityId, entity);
    }
    
    /**
     * Get an entity by its network ID.
     */
    public AliveEntity getEntity(long entityId) {
        return entitiesById.get(entityId);
    }
    
    /**
     * Remove an entity from the registry.
     */
    public void removeEntity(long entityId) {
        entitiesById.remove(entityId);
    }
    
    /**
     * Check if an entity with the given ID exists.
     */
    public boolean hasEntity(long entityId) {
        return entitiesById.containsKey(entityId);
    }
    
    /**
     * Clear all entities from the registry.
     */
    public void clear() {
        entitiesById.clear();
    }
    
    /**
     * Get the number of registered entities.
     */
    public int size() {
        return entitiesById.size();
    }
}

