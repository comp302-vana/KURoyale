package kuroyale.mainpack.network;

/**
 * Generates unique entity IDs for network synchronization.
 * IDs are assigned by the host and sent to clients.
 */
public class EntityIdGenerator {
    private static EntityIdGenerator instance;
    private long counter = 0;
    private long lastTimestamp = 0;
    
    private EntityIdGenerator() {}
    
    public static EntityIdGenerator getInstance() {
        if (instance == null) {
            instance = new EntityIdGenerator();
        }
        return instance;
    }
    
    /**
     * Generates a unique entity ID.
     * Format: (timestamp_ms << 16) | counter
     * This ensures uniqueness even with rapid entity creation.
     */
    public synchronized long generateId() {
        long currentTime = System.currentTimeMillis();
        
        // Reset counter if timestamp changed
        if (currentTime != lastTimestamp) {
            counter = 0;
            lastTimestamp = currentTime;
        }
        
        // Generate ID: upper 48 bits = timestamp, lower 16 bits = counter
        long id = (currentTime << 16) | (counter & 0xFFFF);
        counter++;
        
        return id;
    }
    
    /**
     * Reset the generator (useful for testing or new game).
     */
    public synchronized void reset() {
        counter = 0;
        lastTimestamp = 0;
    }
}

