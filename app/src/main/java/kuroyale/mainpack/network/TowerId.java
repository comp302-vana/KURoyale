package kuroyale.mainpack.network;

/**
 * Unique identifier for each tower in the arena.
 * Towers are identified by player and position (left/right/king).
 */
public enum TowerId {
    P1_LEFT,    // Player 1's left princess tower
    P1_RIGHT,   // Player 1's right princess tower
    P1_KING,    // Player 1's king tower
    P2_LEFT,    // Player 2's left princess tower
    P2_RIGHT,   // Player 2's right princess tower
    P2_KING;    // Player 2's king tower
    
    /**
     * Determine if this tower belongs to a specific player.
     * @param playerId 1 for Player 1, 2 for Player 2
     * @return true if this tower belongs to the specified player
     */
    public boolean belongsToPlayer(int playerId) {
        if (playerId == 1) {
            return this == P1_LEFT || this == P1_RIGHT || this == P1_KING;
        } else {
            return this == P2_LEFT || this == P2_RIGHT || this == P2_KING;
        }
    }
    
    /**
     * Check if this is a king tower.
     * @return true if this is a king tower
     */
    public boolean isKing() {
        return this == P1_KING || this == P2_KING;
    }
    
    /**
     * Get the player ID for this tower.
     * @return 1 for Player 1 towers, 2 for Player 2 towers
     */
    public int getPlayerId() {
        switch (this) {
            case P1_LEFT:
            case P1_RIGHT:
            case P1_KING:
                return 1;
            case P2_LEFT:
            case P2_RIGHT:
            case P2_KING:
                return 2;
            default:
                return 0;
        }
    }
}
