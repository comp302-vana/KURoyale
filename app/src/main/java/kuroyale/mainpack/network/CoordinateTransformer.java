package kuroyale.mainpack.network;

/**
 * Utility class for coordinate transformation between client view and absolute coordinates.
 * 
 * ABSOLUTE COORDINATE SYSTEM:
 * - River runs vertically at approximately cols/2 (columns 15-16 are bridge/river)
 * - Player 1 (host) controls LEFT side: col < RIVER_LEFT_LIMIT
 * - Player 2 (client) controls RIGHT side: col >= RIVER_RIGHT_LIMIT
 * - Rows are NOT flipped (same for both players)
 * 
 * CLIENT VIEW:
 * - Client sees a horizontally mirrored view (flip columns across river)
 * - This makes client's right side appear as their "left" side
 * - Rows remain the same
 */
public class CoordinateTransformer {
    // River boundary: columns 15-16 are the bridge/river
    // Player 1 zone: col < 15 (left side)
    // Player 2 zone: col >= 16 (right side)
    private static final int RIVER_LEFT_LIMIT = 15;
    private static final int RIVER_RIGHT_LIMIT = 16;
    
    /**
     * Transform client view coordinates to absolute coordinates.
     * Client view is horizontally mirrored (columns flipped).
     * 
     * @param clientRow Row in client's view (0-17)
     * @param clientCol Column in client's view (0-31)
     * @param rows Total arena rows (18)
     * @param cols Total arena columns (32)
     * @return int array [absoluteRow, absoluteCol]
     */
    public static int[] clientToAbsolute(int clientRow, int clientCol, int rows, int cols) {
        // Flip columns (X-axis) for horizontal mirror, keep rows (Y-axis) the same
        int absoluteRow = clientRow; // Rows stay the same
        int absoluteCol = cols - 1 - clientCol; // Flip columns
        return new int[] { absoluteRow, absoluteCol };
    }
    
    /**
     * Transform absolute coordinates to client view coordinates.
     * Client view is horizontally mirrored (columns flipped).
     * 
     * @param absoluteRow Row in absolute coordinates (0-17)
     * @param absoluteCol Column in absolute coordinates (0-31)
     * @param rows Total arena rows (18)
     * @param cols Total arena columns (32)
     * @return int array [clientRow, clientCol]
     */
    public static int[] absoluteToClient(int absoluteRow, int absoluteCol, int rows, int cols) {
        // Flip columns (X-axis) for horizontal mirror, keep rows (Y-axis) the same
        int clientRow = absoluteRow; // Rows stay the same
        int clientCol = cols - 1 - absoluteCol; // Flip columns
        return new int[] { clientRow, clientCol };
    }
    
    /**
     * Validate if placement is in Player 1's zone (host, LEFT side).
     * Player 1 controls: col < RIVER_LEFT_LIMIT (left side of river).
     * 
     * @param absoluteRow Row coordinate (not used for zone check)
     * @param absoluteCol Column coordinate (determines zone)
     * @param rows Total arena rows (not used)
     * @param cols Total arena columns (not used)
     * @return true if in Player 1's zone
     */
    public static boolean isValidPlayer1Zone(int absoluteRow, int absoluteCol, int rows, int cols) {
        return absoluteCol < RIVER_LEFT_LIMIT;
    }
    
    /**
     * Validate if placement is in Player 2's zone (client, RIGHT side).
     * Player 2 controls: col >= RIVER_RIGHT_LIMIT (right side of river).
     * 
     * @param absoluteRow Row coordinate (not used for zone check)
     * @param absoluteCol Column coordinate (determines zone)
     * @param rows Total arena rows (not used)
     * @param cols Total arena columns (not used)
     * @return true if in Player 2's zone
     */
    public static boolean isValidPlayer2Zone(int absoluteRow, int absoluteCol, int rows, int cols) {
        return absoluteCol >= RIVER_RIGHT_LIMIT;
    }
    
    /**
     * Validate placement zone for a specific player.
     * 
     * @param absoluteRow Row coordinate (not used for zone check)
     * @param absoluteCol Column coordinate (determines zone)
     * @param playerId 1 for host (left), 2 for client (right)
     * @param rows Total arena rows (not used)
     * @param cols Total arena columns (not used)
     * @return true if placement is in the player's zone
     */
    public static boolean isValidPlacementZone(int absoluteRow, int absoluteCol, int playerId, int rows, int cols) {
        if (playerId == 1) {
            return isValidPlayer1Zone(absoluteRow, absoluteCol, rows, cols);
        } else {
            return isValidPlayer2Zone(absoluteRow, absoluteCol, rows, cols);
        }
    }
    
    /**
     * Get the river boundary limits.
     * @return array [RIVER_LEFT_LIMIT, RIVER_RIGHT_LIMIT]
     */
    public static int[] getRiverBoundaries() {
        return new int[] { RIVER_LEFT_LIMIT, RIVER_RIGHT_LIMIT };
    }
    
    /**
     * Transform absolute coordinates of a footprint's top-left corner to client view.
     * SIZE-AWARE: Accounts for multi-tile width to ensure perfect symmetry.
     * 
     * Formula: clientTopLeftCol = cols - widthTiles - absoluteTopLeftCol
     * This ensures the mirrored footprint maintains the same distance from edges.
     * For widthTiles=1, this reduces to cols-1-col (same as absoluteToClient).
     * 
     * @param absoluteTopLeftRow Top-left row in absolute coordinates
     * @param absoluteTopLeftCol Top-left column in absolute coordinates (top-left of footprint)
     * @param widthTiles Width of the footprint in tiles (Princess=3, King=4)
     * @param rows Total arena rows
     * @param cols Total arena columns
     * @return int array [clientTopLeftRow, clientTopLeftCol]
     */
    public static int[] absoluteTopLeftToClientTopLeft(int absoluteTopLeftRow, int absoluteTopLeftCol, 
                                                       int widthTiles, int rows, int cols) {
        // Rows stay the same
        int clientTopLeftRow = absoluteTopLeftRow;
        
        // Mirror top-left column accounting for footprint width
        // For proper horizontal mirroring of a multi-tile object:
        // - Original object occupies columns [absoluteTopLeftCol, absoluteTopLeftCol + widthTiles - 1]
        // - Right edge of original: absoluteTopLeftCol + widthTiles - 1
        // - Mirrored right edge: (cols - 1) - (absoluteTopLeftCol + widthTiles - 1) = cols - absoluteTopLeftCol - widthTiles
        // - Mirrored top-left: mirrored right edge - (widthTiles - 1) = cols - absoluteTopLeftCol - widthTiles - widthTiles + 1
        // Simplified: clientTopLeftCol = cols - absoluteTopLeftCol - 2*widthTiles + 1
        // But actually, simpler: mirror the right edge, then subtract width to get left edge
        // Right edge mirrored: cols - 1 - (absoluteTopLeftCol + widthTiles - 1) = cols - absoluteTopLeftCol - widthTiles
        // Left edge: (cols - absoluteTopLeftCol - widthTiles) - (widthTiles - 1) = cols - absoluteTopLeftCol - 2*widthTiles + 1
        
        // Actually, the correct formula for mirroring is:
        // If original right edge is at R, mirrored right edge is at (cols-1-R)
        // Original right edge: absoluteTopLeftCol + widthTiles - 1
        // Mirrored right edge: cols - 1 - (absoluteTopLeftCol + widthTiles - 1) = cols - absoluteTopLeftCol - widthTiles
        // Mirrored left edge: (cols - absoluteTopLeftCol - widthTiles) - (widthTiles - 1) = cols - absoluteTopLeftCol - 2*widthTiles + 1
        
        // Wait, let me recalculate more carefully:
        // Original: left at C, right at C+W-1
        // Mirror: right at (cols-1) - (C+W-1) = cols-1-C-W+1 = cols-C-W
        // Mirror: left at (cols-C-W) - (W-1) = cols-C-W-W+1 = cols-C-2*W+1
        
        int clientTopLeftCol = cols - absoluteTopLeftCol - 2 * widthTiles + 1;
        
        return new int[] { clientTopLeftRow, clientTopLeftCol };
    }
    
    /**
     * Legacy method name for backward compatibility.
     */
    public static int[] absoluteToClientTopLeft(int absoluteTopLeftRow, int absoluteTopLeftCol, 
                                                int widthTiles, int rows, int cols) {
        return absoluteTopLeftToClientTopLeft(absoluteTopLeftRow, absoluteTopLeftCol, widthTiles, rows, cols);
    }
    
    /**
     * Transform client view coordinates of a footprint's top-left corner to absolute coordinates.
     * Inverse of absoluteToClientTopLeft.
     * 
     * @param clientTopLeftRow Top-left row in client view
     * @param clientTopLeftCol Top-left column in client view (top-left of footprint)
     * @param widthTiles Width of the footprint in tiles (Princess=3, King=4)
     * @param rows Total arena rows
     * @param cols Total arena columns
     * @return int array [absoluteTopLeftRow, absoluteTopLeftCol]
     */
    public static int[] clientToAbsoluteTopLeft(int clientTopLeftRow, int clientTopLeftCol,
                                                int widthTiles, int rows, int cols) {
        // Rows stay the same
        int absoluteTopLeftRow = clientTopLeftRow;
        
        // Inverse mirror: absoluteCol = cols - widthTiles - clientCol
        int absoluteTopLeftCol = cols - widthTiles - clientTopLeftCol;
        
        return new int[] { absoluteTopLeftRow, absoluteTopLeftCol };
    }
    
    /**
     * Transform absolute coordinates of a footprint's bottom-right corner to client view top-left.
     * Towers are stored with bottom-right anchor, but rendered from top-left.
     * 
     * @param absoluteBottomRightRow Bottom-right row in absolute coordinates
     * @param absoluteBottomRightCol Bottom-right column in absolute coordinates
     * @param footprintSize Size of the footprint (Princess=3, King=4)
     * @param rows Total arena rows
     * @param cols Total arena columns
     * @return int array [clientTopLeftRow, clientTopLeftCol] for rendering
     */
    public static int[] absoluteBottomRightToClientTopLeft(int absoluteBottomRightRow, int absoluteBottomRightCol, 
                                                           int footprintSize, int rows, int cols) {
        // Convert bottom-right to top-left in absolute space
        int absoluteTopLeftRow = absoluteBottomRightRow - (footprintSize - 1);
        int absoluteTopLeftCol = absoluteBottomRightCol - (footprintSize - 1);
        
        // Mirror the top-left corner to client view using footprint-aware transform
        return absoluteToClientTopLeft(absoluteTopLeftRow, absoluteTopLeftCol, footprintSize, rows, cols);
    }
    
    /**
     * Transform absolute coordinates of a footprint's bottom-right anchor to client view bottom-right anchor.
     * This accounts for the footprint size when mirroring, ensuring perfect symmetry.
     * 
     * @param absoluteBottomRightRow Bottom-right row in absolute coordinates (anchor)
     * @param absoluteBottomRightCol Bottom-right column in absolute coordinates (anchor)
     * @param footprintSize Size of the footprint (Princess=3, King=4)
     * @param rows Total arena rows
     * @param cols Total arena columns
     * @return int array [clientBottomRightRow, clientBottomRightCol] for anchor positioning
     */
    public static int[] absoluteBottomRightToClientBottomRight(int absoluteBottomRightRow, int absoluteBottomRightCol,
                                                              int footprintSize, int rows, int cols) {
        // Get the client-view top-left
        int[] clientTopLeft = absoluteBottomRightToClientTopLeft(absoluteBottomRightRow, absoluteBottomRightCol, 
                                                                 footprintSize, rows, cols);
        int clientTopLeftRow = clientTopLeft[0];
        int clientTopLeftCol = clientTopLeft[1];
        
        // Convert back to bottom-right in client view
        int clientBottomRightRow = clientTopLeftRow + (footprintSize - 1);
        int clientBottomRightCol = clientTopLeftCol + (footprintSize - 1);
        
        return new int[] { clientBottomRightRow, clientBottomRightCol };
    }
}
