package kuroyale.mainpack.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Handles the host (server) side of network multiplayer.
 */
public class NetworkHost {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream clientOut;
    private ObjectInputStream clientIn;
    private Thread acceptThread;
    private Thread receiveThread;
    private boolean isRunning = false;
    private Consumer<NetworkMessage> onMessageReceived;
    
    // Host player data
    private String hostPlayerName;
    private String hostDeckName;
    private boolean hostReady = false;
    
    // Client player data
    private String clientPlayerName;
    private String clientDeckName;
    private boolean clientReady = false;
    
    // Track if game has started (to prevent LOBBY_UPDATE during battle)
    private boolean gameStarted = false;
    
    // Direct connection mode: host listens for incoming connections
    public NetworkHost(int port, String playerName, Consumer<NetworkMessage> onMessageReceived) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.hostPlayerName = playerName;
        this.onMessageReceived = onMessageReceived;
        this.isRunning = true;
        
        // Start accepting connections
        acceptThread = new Thread(this::acceptConnection);
        acceptThread.setDaemon(true);
        acceptThread.start();
    }
    
    // Relay mode: host connects to relay server (outbound connection - no NAT issues)
    // Note: boolean parameter distinguishes this from direct mode constructor
    private NetworkHost(boolean useRelay, String relayIP, int relayPort, String playerName, Consumer<NetworkMessage> onMessageReceived) throws IOException {
        this.hostPlayerName = playerName;
        this.onMessageReceived = onMessageReceived;
        this.isRunning = true;
        
        // Connect to relay server (outbound - no NAT issues)
        System.out.println("Host: Connecting to relay server at " + relayIP + ":" + relayPort);
        try {
            clientSocket = new Socket();
            clientSocket.connect(new java.net.InetSocketAddress(relayIP, relayPort), 10000);
            clientSocket.setSoTimeout(0);
            System.out.println("Host: Connected to relay server");
        } catch (java.net.ConnectException e) {
            throw new IOException("Cannot connect to relay server. Make sure the relay server is running.", e);
        } catch (java.net.SocketTimeoutException e) {
            throw new IOException("Connection timeout. Check relay server IP/port.", e);
        } catch (java.net.UnknownHostException e) {
            throw new IOException("Unknown relay server: " + relayIP, e);
        }
        
        // CRITICAL: Create ObjectOutputStream FIRST, flush, then ObjectInputStream
        clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
        clientOut.flush();
        clientIn = new ObjectInputStream(clientSocket.getInputStream());
        
        // Send CONNECT message to identify as HOST (playerId = 1)
        // Format: "roomId:playerName" (using "default" room for now)
        String connectData = "default:" + playerName;
        sendMessage(new NetworkMessage(
            NetworkMessage.MessageType.CONNECT,
            1, // HOST identifier
            connectData,
            getCurrentTimestamp()
        ));
        System.out.println("Host: Sent CONNECT message to relay (identified as HOST, room=default)");
        
        // Start receiving messages from relay (which forwards client messages)
        receiveThread = new Thread(this::receiveMessages);
        receiveThread.setDaemon(true);
        receiveThread.start();
    }
    
    /**
     * Create NetworkHost in relay mode.
     * This is a factory method to avoid constructor signature conflicts.
     */
    public static NetworkHost createRelayHost(String relayIP, int relayPort, String playerName, Consumer<NetworkMessage> onMessageReceived) throws IOException {
        return new NetworkHost(true, relayIP, relayPort, playerName, onMessageReceived);
    }
    
    private void acceptConnection() {
        // Keep accepting connections in a loop
        while (isRunning && serverSocket != null && !serverSocket.isClosed()) {
            try {
                // Don't accept if we already have a connected client
                if (clientSocket != null && !clientSocket.isClosed() && clientSocket.isConnected()) {
                    // Wait a bit before checking again
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }
                
                System.out.println("Host: Waiting for client connection on port " + serverSocket.getLocalPort());
                Socket newClientSocket = serverSocket.accept();
                System.out.println("Host: Client connected from " + newClientSocket.getRemoteSocketAddress());
                
                // Clean up any existing connection first
                cleanupClientConnection();
                
                // Set up new client connection
                clientSocket = newClientSocket;
                // CRITICAL: Host must create ObjectOutputStream FIRST, flush it, then create ObjectInputStream
                // This ensures the header is written before client tries to read it
                clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
                clientOut.flush(); // Flush header immediately so client can read it
                // Small delay to ensure client has time to create its ObjectInputStream
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                clientIn = new ObjectInputStream(clientSocket.getInputStream());
                
                // Clear previous client data (in case of reconnection)
                clientPlayerName = null;
                clientDeckName = null;
                clientReady = false;
                
                // Send welcome message with host info
                sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.PLAYER_JOINED,
                    1,
                    hostPlayerName,
                    getCurrentTimestamp()
                ));
                
                // Start receiving messages from client
                receiveThread = new Thread(this::receiveMessages);
                receiveThread.setDaemon(true);
                receiveThread.start();
                
            } catch (IOException e) {
                if (isRunning && !serverSocket.isClosed()) {
                    System.err.println("Host: Error accepting connection: " + e.getMessage());
                    // Wait a bit before retrying
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        System.out.println("Host: Accept thread stopped");
    }
    
    private void receiveMessages() {
        try {
            while (isRunning && clientSocket != null && !clientSocket.isClosed()) {
                NetworkMessage message = MessageProtocol.receiveMessage(clientIn);
                handleMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            if (isRunning) {
                System.err.println("Host: Error receiving message: " + e.getMessage());
                // Client connection lost - cleanup
                if (clientSocket != null && (clientSocket.isClosed() || !clientSocket.isConnected())) {
                    clientPlayerName = null;
                    clientDeckName = null;
                    clientReady = false;
                    cleanupClientConnection();
                    // Accept thread will automatically accept new connections in the loop
                }
            }
        }
    }
    
    private void handleMessage(NetworkMessage message) {
        switch (message.getType()) {
            case CONNECT:
                // CONNECT from client (playerId=2) means client joined/reconnected
                // Works in both direct mode and relay mode
                if (message.getPlayerId() == 2) {
                    // Parse CONNECT data: "roomId:playerName" format
                    String connectData = message.getData();
                    String newClientName = null;
                    if (connectData != null && connectData.contains(":")) {
                        String[] parts = connectData.split(":", 2);
                        newClientName = parts.length > 1 ? parts[1] : connectData;
                    } else {
                        newClientName = connectData; // Fallback for old format
                    }
                    
                    // Check if this is a reconnection (client was already known)
                    boolean isReconnection = (clientPlayerName != null);
                    boolean nameChanged = isReconnection && !clientPlayerName.equals(newClientName);
                    
                    if (nameChanged) {
                        System.out.println("Host: Client reconnected with different name, resetting state");
                        clientDeckName = null;
                        clientReady = false;
                    } else if (isReconnection) {
                        System.out.println("Host: Client reconnected with same name: " + newClientName);
                        // Reset ready status on reconnection to allow fresh lobby state
                        clientReady = false;
                    }
                    
                    clientPlayerName = newClientName;
                    
                    // Always send PLAYER_JOINED back to client (like in direct mode)
                    // This ensures client knows host is present, even on reconnection
                    sendMessage(new NetworkMessage(
                        NetworkMessage.MessageType.PLAYER_JOINED,
                        1, // Host playerId
                        hostPlayerName,
                        getCurrentTimestamp()
                    ));
                    System.out.println("Host: Client " + (isReconnection ? "reconnected" : "joined") + " via relay: " + clientPlayerName);
                    
                    // Broadcast lobby update to sync state
                    broadcastLobbyUpdate();
                }
                break;
            case DECK_SELECTED:
                if (message.getPlayerId() == 2) {
                    clientDeckName = message.getData();
                    System.out.println("Host: Client deck selected: " + clientDeckName);
                    broadcastLobbyUpdate();
                }
                break;
            case READY_STATUS:
                if (message.getPlayerId() == 2) {
                    clientReady = Boolean.parseBoolean(message.getData());
                    broadcastLobbyUpdate();
                }
                break;
            case DISCONNECT:
                System.out.println("Host: Client disconnected");
                // Clear client data
                clientPlayerName = null;
                clientDeckName = null;
                clientReady = false;
                // Close connection
                cleanupClientConnection();
                // Accept thread will automatically accept new connections in the loop
                break;
        }
        
        // Forward all messages (including battle messages) to registered handlers
        if (onMessageReceived != null) {
            // Debug: Log battle messages to help diagnose issues
            if (message.getType() == NetworkMessage.MessageType.CARD_PLACEMENT_REQUEST ||
                message.getType() == NetworkMessage.MessageType.SPELL_CAST_REQUEST ||
                message.getType() == NetworkMessage.MessageType.ENTITY_SPAWN ||
                message.getType() == NetworkMessage.MessageType.ENTITY_UPDATE) {
                System.out.println("Host: Received battle message: " + message.getType() + 
                                 " (playerId=" + message.getPlayerId() + ")");
            }
            onMessageReceived.accept(message);
        } else {
            // Debug: Warn if handler is null when battle message arrives
            if (message.getType() == NetworkMessage.MessageType.CARD_PLACEMENT_REQUEST ||
                message.getType() == NetworkMessage.MessageType.SPELL_CAST_REQUEST) {
                System.err.println("Host: WARNING - Battle message received but onMessageReceived is null! " +
                                 "Message type: " + message.getType());
            }
        }
    }
    
    private void cleanupClientConnection() {
        try {
            if (receiveThread != null && receiveThread.isAlive()) {
                receiveThread.interrupt();
            }
            if (clientIn != null) {
                try {
                    clientIn.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (clientOut != null) {
                try {
                    clientOut.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            clientSocket = null;
            clientIn = null;
            clientOut = null;
            receiveThread = null;
            System.out.println("Host: Cleaned up client connection");
        } catch (Exception e) {
            System.err.println("Host: Error cleaning up client connection: " + e.getMessage());
        }
    }
    
    public void sendMessage(NetworkMessage message) {
        if (clientOut != null) {
            try {
                MessageProtocol.sendMessage(clientOut, message);
            } catch (IOException e) {
                System.err.println("Host: Error sending message: " + e.getMessage());
            }
        }
    }
    
    public void setHostDeck(String deckName) {
        this.hostDeckName = deckName;
        // Don't send DECK_SELECTED or LOBBY_UPDATE during battle
        if (!gameStarted) {
            sendMessage(new NetworkMessage(
                NetworkMessage.MessageType.DECK_SELECTED,
                1,
                deckName,
                getCurrentTimestamp()
            ));
            broadcastLobbyUpdate();
        }
    }
    
    public void setHostReady(boolean ready) {
        this.hostReady = ready;
        // Don't send READY_STATUS or LOBBY_UPDATE during battle
        if (!gameStarted) {
            sendMessage(new NetworkMessage(
                NetworkMessage.MessageType.READY_STATUS,
                1,
                String.valueOf(ready),
                getCurrentTimestamp()
            ));
            broadcastLobbyUpdate();
        }
    }
    
    public void startGame() {
        gameStarted = true; // Mark game as started to prevent LOBBY_UPDATE during battle
        sendMessage(new NetworkMessage(
            NetworkMessage.MessageType.START_GAME,
            1,
            "",
            getCurrentTimestamp()
        ));
    }
    
    private void broadcastLobbyUpdate() {
        // Don't send LOBBY_UPDATE during battle - only in lobby
        if (gameStarted) {
            return; // Game has started, don't send lobby updates
        }
        
        // Send lobby state to client
        // Handle null values properly (convert to empty string to avoid "null" string)
        String hostDeck = (hostDeckName != null) ? hostDeckName : "";
        String clientDeck = (clientDeckName != null) ? clientDeckName : "";
        String hostName = (hostPlayerName != null) ? hostPlayerName : "";
        String clientName = (clientPlayerName != null) ? clientPlayerName : "";
        
        String lobbyData = hostName + ":" + hostDeck + ":" + hostReady + "|" +
                          clientName + ":" + clientDeck + ":" + clientReady;
        sendMessage(new NetworkMessage(
            NetworkMessage.MessageType.LOBBY_UPDATE,
            0,
            lobbyData,
            getCurrentTimestamp()
        ));
    }
    
    public boolean isClientConnected() {
        if (serverSocket != null) {
            // Direct mode: check if client socket is connected
            return clientSocket != null && !clientSocket.isClosed() && clientSocket.isConnected();
        } else {
            // Relay mode: client is connected if we have a relay connection and know client name
            return clientSocket != null && !clientSocket.isClosed() && clientSocket.isConnected() 
                   && clientPlayerName != null;
        }
    }
    
    public String getHostPlayerName() {
        return hostPlayerName;
    }
    
    public String getClientPlayerName() {
        return clientPlayerName;
    }
    
    public String getHostDeckName() {
        return hostDeckName;
    }
    
    public String getClientDeckName() {
        return clientDeckName;
    }
    
    public boolean isHostReady() {
        return hostReady;
    }
    
    public boolean isClientReady() {
        return clientReady;
    }
    
    /**
     * Reset lobby state when returning to lobby after a game.
     * This resets gameStarted flag and ready status to allow new lobby interactions.
     */
    public void resetLobbyState() {
        gameStarted = false; // Reset game state to allow lobby updates
        hostReady = false; // Reset ready status
        // Note: We keep client data and connection intact for reconnection scenarios
        System.out.println("Host: Lobby state reset (gameStarted=false, hostReady=false)");
        
        // Broadcast lobby update to sync state with client
        if (clientPlayerName != null) {
            broadcastLobbyUpdate();
        }
    }
    
    public void close() {
        isRunning = false;
        gameStarted = false; // Reset game state when closing
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.DISCONNECT,
                    1,
                    "",
                    getCurrentTimestamp()
                ));
                clientSocket.close();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Host: Error closing connection: " + e.getMessage());
        }
        cleanupClientConnection();
    }
    
    private String getCurrentTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }
    
    public Consumer<NetworkMessage> getOnMessageReceived() {
        return onMessageReceived;
    }
    
    public void setOnMessageReceived(Consumer<NetworkMessage> handler) {
        this.onMessageReceived = handler;
    }
}

