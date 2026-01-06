package kuroyale.mainpack.network;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Singleton manager that abstracts whether we're a host or client.
 */
public class NetworkManager {
    private static NetworkManager instance;
    
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
        close();
        host = new NetworkHost(port, playerName, onMessageReceived);
        isHost = true;
    }
    
    public void startClient(String hostIP, int port, String playerName, Consumer<NetworkMessage> onMessageReceived) throws IOException {
        close();
        client = new NetworkClient(hostIP, port, playerName, onMessageReceived);
        isHost = false;
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
    
    /**
     * Register a battle message handler (separate from lobby handler).
     * This allows the battle system to receive network messages.
     */
    public void registerBattleMessageHandler(Consumer<NetworkMessage> handler) {
        // Store battle handler and combine with existing lobby handler
        // Both handlers will be called for battle messages
        if (isHost && host != null) {
            // Update the host's message handler to also call battle handler
            Consumer<NetworkMessage> existingHandler = host.getOnMessageReceived();
            host.setOnMessageReceived(msg -> {
                if (existingHandler != null) existingHandler.accept(msg);
                handler.accept(msg);
            });
        } else if (client != null) {
            // Update the client's message handler to also call battle handler
            Consumer<NetworkMessage> existingHandler = client.getOnMessageReceived();
            client.setOnMessageReceived(msg -> {
                if (existingHandler != null) existingHandler.accept(msg);
                handler.accept(msg);
            });
        }
    }
    
    public void close() {
        if (host != null) {
            host.close();
            host = null;
        }
        if (client != null) {
            client.close();
            client = null;
        }
    }
}

