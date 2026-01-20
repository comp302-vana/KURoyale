package kuroyale.mainpack.network;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Singleton manager that abstracts whether we're a host or client.
 * 
 * Supports two modes:
 * 1. Direct connection (default): Host listens, client connects directly
 * 2. Relay mode: Both connect to relay server for NAT traversal
 */
public class NetworkManager {
    private static NetworkManager instance;
    
    // Relay server configuration (for NAT traversal)
    // Oracle Cloud relay server
    private static final String RELAY_SERVER_IP = "80.225.92.3";
    private static final int RELAY_SERVER_PORT = 8081;
    
    // Runtime state: tracks whether current connection is using relay mode
    private static boolean relayModeEnabled = false;
    
    private NetworkHost host;
    private NetworkClient client;
    private boolean isHost = false;
    
    private NetworkManager() {}
    
    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }
    
    public void startHost(int port, String playerName, Consumer<NetworkMessage> onMessageReceived) throws IOException {
        startHost(port, playerName, onMessageReceived, false); // Default to local
    }
    
    public void startHost(int port, String playerName, Consumer<NetworkMessage> onMessageReceived, boolean useInternet) throws IOException {
        close();
        // Longer delay to ensure old connection threads fully exit and relay server cleans up
        try {
            Thread.sleep(500); // Increased from 100ms to 500ms for better cleanup
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        relayModeEnabled = useInternet; // Set state BEFORE creating connection
        if (useInternet) {
            // Internet mode: connect to relay server
            host = NetworkHost.createRelayHost(RELAY_SERVER_IP, RELAY_SERVER_PORT, playerName, onMessageReceived);
            System.out.println("Host: Using internet mode (relay server: " + RELAY_SERVER_IP + ":" + RELAY_SERVER_PORT + ")");
        } else {
            // Local mode: listen for incoming connections
            host = new NetworkHost(port, playerName, onMessageReceived);
            System.out.println("Host: Using local mode (listening on port " + port + ")");
        }
        isHost = true;
    }
    
    public void startClient(String hostIP, int port, String playerName, Consumer<NetworkMessage> onMessageReceived) throws IOException {
        startClient(hostIP, port, playerName, onMessageReceived, false); // Default to local
    }
    
    public void startClient(String hostIP, int port, String playerName, Consumer<NetworkMessage> onMessageReceived, boolean useInternet) throws IOException {
        close();
        // Longer delay to ensure old connection threads fully exit and relay server cleans up
        try {
            Thread.sleep(500); // Increased from 100ms to 500ms for better cleanup
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        relayModeEnabled = useInternet; // Set state BEFORE creating connection
        if (useInternet) {
            // Internet mode: connect to relay server (hostIP is ignored)
            client = NetworkClient.createRelayClient(RELAY_SERVER_IP, RELAY_SERVER_PORT, playerName, onMessageReceived);
            System.out.println("Client: Using internet mode (relay server: " + RELAY_SERVER_IP + ":" + RELAY_SERVER_PORT + ")");
        } else {
            // Local mode: connect directly to host IP
            client = new NetworkClient(hostIP, port, playerName, onMessageReceived);
            System.out.println("Client: Using local mode (connecting to " + hostIP + ":" + port + ")");
        }
        isHost = false;
    }
    
    /**
     * Check if relay mode is enabled.
     * Returns the current runtime state of relay mode.
     */
    public static boolean isRelayModeEnabled() {
        return relayModeEnabled;
    }
    
    /**
     * Get relay server IP (for display purposes).
     */
    public static String getRelayServerIP() {
        return RELAY_SERVER_IP;
    }
    
    /**
     * Get relay server port (for display purposes).
     */
    public static int getRelayServerPort() {
        return RELAY_SERVER_PORT;
    }
    
    public boolean isHost() {
        return isHost;
    }
    
    public boolean isConnected() {
        if (isHost) {
            return host != null && host.isClientConnected();
        } else {
            return client != null && client.isConnected();
        }
    }
    
    public String getPlayer1Name() {
        if (isHost) {
            return host != null ? host.getHostPlayerName() : null;
        } else {
            return client != null ? client.getHostPlayerName() : null;
        }
    }
    
    public String getPlayer2Name() {
        if (isHost) {
            return host != null ? host.getClientPlayerName() : null;
        } else {
            return client != null ? client.getClientPlayerName() : null;
        }
    }
    
    public String getPlayer1Deck() {
        if (isHost) {
            return host != null ? host.getHostDeckName() : null;
        } else {
            return client != null ? client.getHostDeckName() : null;
        }
    }
    
    public String getPlayer2Deck() {
        if (isHost) {
            return host != null ? host.getClientDeckName() : null;
        } else {
            return client != null ? client.getClientDeckName() : null;
        }
    }
    
    public boolean isPlayer1Ready() {
        if (isHost) {
            return host != null && host.isHostReady();
        } else {
            return client != null && client.isHostReady();
        }
    }
    
    public boolean isPlayer2Ready() {
        if (isHost) {
            return host != null && host.isClientReady();
        } else {
            return client != null && client.isClientReady();
        }
    }
    
    public void setPlayerDeck(String deckName) {
        if (isHost) {
            if (host != null) {
                host.setHostDeck(deckName);
            }
        } else {
            if (client != null) {
                client.setClientDeck(deckName);
            }
        }
    }
    
    public void setPlayerReady(boolean ready) {
        if (isHost) {
            if (host != null) {
                host.setHostReady(ready);
            }
        } else {
            if (client != null) {
                client.setClientReady(ready);
            }
        }
    }
    
    public void startGame() {
        if (isHost && host != null) {
            host.startGame();
        }
    }
    
    /**
     * Send a network message. Works for both host and client.
     * Host sends to client, client sends to host.
     */
    public void sendMessage(NetworkMessage message) {
        if (isHost) {
            if (host != null) {
                host.sendMessage(message);
            }
        } else {
            if (client != null) {
                client.sendMessage(message);
            }
        }
    }
    
    // Store handlers separately to avoid nesting
    private Consumer<NetworkMessage> battleMessageHandler = null;
    private Consumer<NetworkMessage> lobbyMessageHandler = null;
    
    /**
     * Register a battle message handler (separate from lobby handler).
     * This allows the battle system to receive network messages.
     * Note: This REPLACES any existing battle handler to prevent nesting.
     */
    public void registerBattleMessageHandler(Consumer<NetworkMessage> handler) {
        // Store battle handler
        battleMessageHandler = handler;
        updateCombinedHandler();
    }
    
    /**
     * Register a lobby message handler (for START_GAME messages).
     * Note: This REPLACES any existing lobby handler to prevent nesting.
     */
    public void registerLobbyMessageHandler(Consumer<NetworkMessage> handler) {
        // Store lobby handler
        lobbyMessageHandler = handler;
        updateCombinedHandler();
    }
    
    /**
     * Update the combined handler that calls both lobby and battle handlers.
     */
    private void updateCombinedHandler() {
        Consumer<NetworkMessage> combinedHandler = msg -> {
            // Call lobby handler if registered
            if (lobbyMessageHandler != null) {
                lobbyMessageHandler.accept(msg);
            }
            // Call battle handler if registered
            if (battleMessageHandler != null) {
                battleMessageHandler.accept(msg);
            }
        };
        
        // Set the combined handler (replaces any existing handler)
        if (isHost && host != null) {
            host.setOnMessageReceived(combinedHandler);
        } else if (client != null) {
            client.setOnMessageReceived(combinedHandler);
        }
    }
    
    /**
     * Clear lobby message handler (when leaving lobby).
     */
    public void clearLobbyMessageHandler() {
        lobbyMessageHandler = null;
        updateCombinedHandler();
    }
    
    /**
     * Clear battle message handler (when leaving battle).
     */
    public void clearBattleMessageHandler() {
        battleMessageHandler = null;
        updateCombinedHandler();
    }
    
    /**
     * Reset lobby state (gameStarted flag, ready status, etc.)
     * Call this when returning to lobby after a game.
     */
    public void resetLobbyState() {
        if (host != null) {
            host.resetLobbyState();
        }
        if (client != null) {
            client.resetLobbyState();
        }
    }
    
    public void close() {
        if (host != null) {
            // Clear message handler before closing to prevent stale references
            host.setOnMessageReceived(null);
            host.close();
            host = null;
        }
        if (client != null) {
            // Clear message handler before closing to prevent stale references
            client.setOnMessageReceived(null);
            client.close();
            client = null;
        }
        relayModeEnabled = false; // Reset state when connections are closed
        isHost = false; // Reset host/client state
    }
    
    /**
     * Fully reset NetworkManager state. Use this when returning to menu/lobby
     * to ensure clean state for next game.
     */
    public void reset() {
        close(); // This already clears everything
        // Additional reset if needed in future
    }
}

