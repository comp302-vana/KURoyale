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
                clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
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
                clientPlayerName = message.getData();
                broadcastLobbyUpdate();
                break;
            case DECK_SELECTED:
                if (message.getPlayerId() == 2) {
                    clientDeckName = message.getData();
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
        
        if (onMessageReceived != null) {
            onMessageReceived.accept(message);
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
        sendMessage(new NetworkMessage(
            NetworkMessage.MessageType.DECK_SELECTED,
            1,
            deckName,
            getCurrentTimestamp()
        ));
        broadcastLobbyUpdate();
    }
    
    public void setHostReady(boolean ready) {
        this.hostReady = ready;
        sendMessage(new NetworkMessage(
            NetworkMessage.MessageType.READY_STATUS,
            1,
            String.valueOf(ready),
            getCurrentTimestamp()
        ));
        broadcastLobbyUpdate();
    }
    
    public void startGame() {
        sendMessage(new NetworkMessage(
            NetworkMessage.MessageType.START_GAME,
            1,
            "",
            getCurrentTimestamp()
        ));
    }
    
    private void broadcastLobbyUpdate() {
        // Send lobby state to client
        String lobbyData = hostPlayerName + ":" + hostDeckName + ":" + hostReady + "|" +
                          clientPlayerName + ":" + clientDeckName + ":" + clientReady;
        sendMessage(new NetworkMessage(
            NetworkMessage.MessageType.LOBBY_UPDATE,
            0,
            lobbyData,
            getCurrentTimestamp()
        ));
    }
    
    public boolean isClientConnected() {
        return clientSocket != null && !clientSocket.isClosed() && clientSocket.isConnected();
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
    }
    
    private String getCurrentTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }
}

