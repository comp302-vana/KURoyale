package kuroyale.mainpack.models;

/**
 * Represents a single gold transaction in the player's history.
 */
public class GoldTransaction {
    private final long timestamp;
    private final int amount;
    private final String reason; // VICTORY, DEFEAT, DRAW, UPGRADE, etc.
    
    public GoldTransaction(long timestamp, int amount, String reason) {
        this.timestamp = timestamp;
        this.amount = amount;
        this.reason = reason;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public String getReason() {
        return reason;
    }
    
    @Override
    public String toString() {
        return String.format("GoldTransaction{timestamp=%d, amount=%d, reason='%s'}", 
                            timestamp, amount, reason);
    }
}
