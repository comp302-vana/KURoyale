package kuroyale.mainpack.network;

import java.io.Serializable;

/**
 * Represents a network message between host and client.
 * Format: MESSAGE_TYPE|player_id|data|timestamp
 */
public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        CONNECT,
        PLAYER_JOINED,
        DECK_SELECTED,
        READY_STATUS,
        START_GAME,
        DISCONNECT,
        LOBBY_UPDATE
    }
    
    private MessageType type;
    private int playerId;
    private String data;
    private String timestamp;
    
    public NetworkMessage(MessageType type, int playerId, String data, String timestamp) {
        this.type = type;
        this.playerId = playerId;
        this.data = data;
        this.timestamp = timestamp;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public int getPlayerId() {
        return playerId;
    }
    
    public String getData() {
        return data;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return type + "|" + playerId + "|" + data + "|" + timestamp;
    }
}

