package kuroyale.mainpack.network;

import java.io.Serializable;

/**
 * Represents a network message between host and client.
 * Format: MESSAGE_TYPE|player_id|data|timestamp
 */
public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        // Lobby messages
        CONNECT,
        PLAYER_JOINED,
        DECK_SELECTED,
        READY_STATUS,
        START_GAME,
        DISCONNECT,
        LOBBY_UPDATE,
        // Battle messages
        CARD_PLACEMENT_REQUEST,   // Client → Host: card placement request (with requestId)
        SPELL_CAST_REQUEST,        // Client → Host: spell cast request (with requestId)
        SPELL_CAST_EVENT,          // Host → Client: spell executed (for VFX)
        ENTITY_SPAWN,             // Host → Client: entity spawned
        ENTITY_UPDATE,            // Host → Client: entity state update
        ENTITY_DEATH,             // Host → Client: entity died
        TOWER_UPDATE,             // Host → Client: tower health update
        TOWER_DESTROY,            // Host → Client: tower destroyed
        ELIXIR_UPDATE,            // Host → Client: elixir state update
        GAME_STATE_SNAPSHOT,      // Host → Client: full game state
        GAME_END,                 // Host → Client: game ended
        PLACEMENT_REJECTED        // Host → Client: placement rejected (with requestId)
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

