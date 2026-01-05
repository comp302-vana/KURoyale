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

