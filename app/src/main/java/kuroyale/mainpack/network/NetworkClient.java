package kuroyale.mainpack.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Handles the client side of network multiplayer.
 */
public class NetworkClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread receiveThread;
    private boolean isRunning = false;
    private Consumer<NetworkMessage> onMessageReceived;
    
    // Client player data
    private String clientPlayerName;
    private String clientDeckName;
    private boolean clientReady = false;
    
    // Host player data
    private String hostPlayerName;
    private String hostDeckName;
    private boolean hostReady = false;
    
    // Direct connection mode: client connects directly to host IP
    public NetworkClient(String hostIP, int port, String playerName, Consumer<NetworkMessage> onMessageReceived) throws IOException {
        this.clientPlayerName = playerName;
        this.onMessageReceived = onMessageReceived;
        this.isRunning = true;
        
        // Connect to host
        socket = new Socket(hostIP, port);
        socket.setSoTimeout(0);
        System.out.println("Client: Connected to host at " + hostIP + ":" + port);
        
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        
        // Send connection message
        sendMessage(new NetworkMessage(
            NetworkMessage.MessageType.CONNECT,
            2,
            playerName,
            getCurrentTimestamp()
        ));
        
        // Start receiving messages from host
        receiveThread = new Thread(this::receiveMessages);
        receiveThread.setDaemon(true);
        receiveThread.start();
    }
    
    // Relay mode: client connects to relay server (outbound connection - no NAT issues)
    // Note: boolean parameter distinguishes this from direct mode constructor
    private NetworkClient(boolean useRelay, String relayIP, int relayPort, String playerName, Consumer<NetworkMessage> onMessageReceived) throws IOException {
        this.clientPlayerName = playerName;
        this.onMessageReceived = onMessageReceived;
        this.isRunning = true;
        
        // Connect to relay server (outbound - no NAT issues)
        System.out.println("Client: Connecting to relay server at " + relayIP + ":" + relayPort);
        try {
            socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(relayIP, relayPort), 10000);
            socket.setSoTimeout(0);
            System.out.println("Client: Connected to relay server");
        } catch (java.net.ConnectException e) {
            throw new IOException("Cannot connect to relay server. Make sure the relay server is running.", e);
        } catch (java.net.SocketTimeoutException e) {
            throw new IOException("Connection timeout. Check relay server IP/port.", e);
        } catch (java.net.UnknownHostException e) {
            throw new IOException("Unknown relay server: " + relayIP, e);
        }
        
        // CRITICAL: For relay mode, create ObjectOutputStream FIRST, flush, then ObjectInputStream
        // Relay server expects this order
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        
        // Send CONNECT message to identify as CLIENT (playerId = 2)
        // Format: "roomId:playerName" (using "default" room for now)
        String connectData = "default:" + playerName;
        sendMessage(new NetworkMessage(
            NetworkMessage.MessageType.CONNECT,
            2, // CLIENT identifier
            connectData,
            getCurrentTimestamp()
        ));
        System.out.println("Client: Sent CONNECT message to relay (identified as CLIENT, room=default)");
        
        // Start receiving messages from relay (which forwards host messages)
        receiveThread = new Thread(this::receiveMessages);
        receiveThread.setDaemon(true);
        receiveThread.start();
    }
    
    /**
     * Create NetworkClient in relay mode.
     * This is a factory method to avoid constructor signature conflicts.
     */
    public static NetworkClient createRelayClient(String relayIP, int relayPort, String playerName, Consumer<NetworkMessage> onMessageReceived) throws IOException {
        return new NetworkClient(true, relayIP, relayPort, playerName, onMessageReceived);
    }
    
    private void receiveMessages() {
        try {
            while (isRunning && socket != null && !socket.isClosed()) {
                NetworkMessage message = MessageProtocol.receiveMessage(in);
                System.out.println("Client: Received message: " + message.getType() + 
                                 " (playerId=" + message.getPlayerId() + ")");
                handleMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            if (isRunning) {
                System.err.println("Client: Error receiving message: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            if (isRunning) {
                System.err.println("Client: Unexpected error in receiveMessages: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void handleMessage(NetworkMessage message) {
        switch (message.getType()) {
            case PLAYER_JOINED:
                hostPlayerName = message.getData();
                break;
            case DECK_SELECTED:
                if (message.getPlayerId() == 1) {
                    hostDeckName = message.getData();
                }
                break;
            case READY_STATUS:
                if (message.getPlayerId() == 1) {
                    hostReady = Boolean.parseBoolean(message.getData());
                }
                break;
            case LOBBY_UPDATE:
                parseLobbyUpdate(message.getData());
                break;
            case START_GAME:
                // Game starting - message handler in lobby controller will handle navigation
                System.out.println("Client: Game starting message received");
                break;
            case DISCONNECT:
                System.out.println("Client: Host disconnected");
                // Clear host data
                hostPlayerName = null;
                hostDeckName = null;
                hostReady = false;
                // Close connection
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    System.err.println("Client: Error closing socket: " + e.getMessage());
                }
                break;
        }
        
        // Forward all messages (including battle messages) to registered handlers
        if (onMessageReceived != null) {
            // Debug: Log battle messages to help diagnose issues
            if (message.getType() == NetworkMessage.MessageType.ENTITY_SPAWN ||
                message.getType() == NetworkMessage.MessageType.ENTITY_UPDATE ||
                message.getType() == NetworkMessage.MessageType.ENTITY_DEATH ||
                message.getType() == NetworkMessage.MessageType.TOWER_UPDATE ||
                message.getType() == NetworkMessage.MessageType.PLACEMENT_REJECTED ||
                message.getType() == NetworkMessage.MessageType.SPELL_CAST_EVENT) {
                System.out.println("Client: Received battle message: " + message.getType() + 
                                 " (playerId=" + message.getPlayerId() + ")");
            }
            onMessageReceived.accept(message);
        } else {
            // Debug: Warn if handler is null when battle message arrives
            if (message.getType() == NetworkMessage.MessageType.ENTITY_SPAWN ||
                message.getType() == NetworkMessage.MessageType.ENTITY_UPDATE ||
                message.getType() == NetworkMessage.MessageType.PLACEMENT_REJECTED) {
                System.err.println("Client: WARNING - Battle message received but onMessageReceived is null! " +
                                 "Message type: " + message.getType());
            }
        }
    }
    
    private void parseLobbyUpdate(String data) {
        // Format: hostName:hostDeck:hostReady|clientName:clientDeck:clientReady
        String[] parts = data.split("\\|");
        if (parts.length >= 1) {
            String[] hostData = parts[0].split(":");
            if (hostData.length >= 3) {
                hostPlayerName = hostData[0];
                hostDeckName = hostData[1].isEmpty() ? null : hostData[1];
                hostReady = Boolean.parseBoolean(hostData[2]);
            }
        }
        if (parts.length >= 2) {
            String[] clientData = parts[1].split(":");
            if (clientData.length >= 3) {
                clientPlayerName = clientData[0];
                clientDeckName = clientData[1].isEmpty() ? null : clientData[1];
                clientReady = Boolean.parseBoolean(clientData[2]);
            }
        }
    }
    
    public void sendMessage(NetworkMessage message) {
        if (out != null) {
            try {
                // Debug: Log battle messages being sent
                if (message.getType() == NetworkMessage.MessageType.CARD_PLACEMENT_REQUEST ||
                    message.getType() == NetworkMessage.MessageType.SPELL_CAST_REQUEST) {
                    System.out.println("Client: Sending battle message: " + message.getType() + 
                                     " (playerId=" + message.getPlayerId() + ", data=" + message.getData() + ")");
                }
                MessageProtocol.sendMessage(out, message);
            } catch (IOException e) {
                System.err.println("Client: Error sending message: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Client: Cannot send message - output stream is null!");
        }
    }
    
    public void setClientDeck(String deckName) {
        this.clientDeckName = deckName;
        System.out.println("Client: Sending deck selection: " + deckName);
        sendMessage(new NetworkMessage(
            NetworkMessage.MessageType.DECK_SELECTED,
            2,
            deckName,
            getCurrentTimestamp()
        ));
    }
    
    public void setClientReady(boolean ready) {
        this.clientReady = ready;
        sendMessage(new NetworkMessage(
            NetworkMessage.MessageType.READY_STATUS,
            2,
            String.valueOf(ready),
            getCurrentTimestamp()
        ));
    }
    
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
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
    
    public void close() {
        isRunning = false;
        try {
            if (socket != null && !socket.isClosed()) {
                sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.DISCONNECT,
                    2,
                    "",
                    getCurrentTimestamp()
                ));
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Client: Error closing connection: " + e.getMessage());
        }
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

