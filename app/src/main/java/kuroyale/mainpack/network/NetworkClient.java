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
    
    public NetworkClient(String hostIP, int port, String playerName, Consumer<NetworkMessage> onMessageReceived) throws IOException {
        this.clientPlayerName = playerName;
        this.onMessageReceived = onMessageReceived;
        this.isRunning = true;
        
        // Connect to host with timeout
        System.out.println("Client: Attempting to connect to " + hostIP + ":" + port);
        try {
            socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(hostIP, port), 10000); // 10 second timeout
            socket.setSoTimeout(0);
            System.out.println("Client: Successfully connected to host at " + hostIP + ":" + port);
        } catch (java.net.ConnectException e) {
            System.err.println("Client: Connection refused - Host may not be running or port not open");
            throw new IOException("Connection refused. Make sure:\n1. Host has started the lobby\n2. Port " + port + " is open\n3. Firewall allows connections\n4. You're using the correct IP address", e);
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("Client: Connection timeout - Host not reachable");
            throw new IOException("Connection timeout. Check:\n1. Host IP address is correct\n2. Port " + port + " is forwarded (for internet play)\n3. Firewall settings", e);
        } catch (java.net.UnknownHostException e) {
            System.err.println("Client: Unknown host - " + hostIP);
            throw new IOException("Unknown host: " + hostIP + "\nCheck the IP address is correct", e);
        }
        
        // CRITICAL: Client must create ObjectInputStream FIRST, then ObjectOutputStream
        // This is because ObjectOutputStream writes a header that ObjectInputStream needs to read
        in = new ObjectInputStream(socket.getInputStream());
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush(); // Flush header immediately so host can read it
        
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
    
    private void receiveMessages() {
        try {
            while (isRunning && socket != null && !socket.isClosed()) {
                NetworkMessage message = MessageProtocol.receiveMessage(in);
                handleMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            if (isRunning) {
                System.err.println("Client: Error receiving message: " + e.getMessage());
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
        
        if (onMessageReceived != null) {
            onMessageReceived.accept(message);
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
                MessageProtocol.sendMessage(out, message);
            } catch (IOException e) {
                System.err.println("Client: Error sending message: " + e.getMessage());
            }
        }
    }
    
    public void setClientDeck(String deckName) {
        this.clientDeckName = deckName;
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

