package kuroyale.mainpack.network;

import java.util.function.Consumer;
import kuroyale.entitiypack.subclasses.AliveEntity;

/**
 * Central coordinator for network battle synchronization.
 * Handles message routing and coordinate transformation.
 */
public class NetworkBattleManager {
    private static NetworkBattleManager instance;
    
    private NetworkManager networkManager;
    private EntityRegistry entityRegistry;
    private boolean isHost;
    private boolean isClient;
    
    // Callbacks for game components
    private Consumer<NetworkMessage> onCardPlacementReceived;
    private Consumer<NetworkMessage> onEntitySpawnReceived;
    private Consumer<NetworkMessage> onEntityUpdateReceived;
    private Consumer<NetworkMessage> onTowerUpdateReceived;
    private Consumer<NetworkMessage> onGameStateReceived;
    private Consumer<NetworkMessage> onGameEndReceived;
    
    private NetworkBattleManager() {
        this.networkManager = NetworkManager.getInstance();
        this.entityRegistry = new EntityRegistry();
        this.isHost = networkManager.isHost();
        this.isClient = !isHost;
        
        // Register message handler with NetworkManager
        networkManager.registerBattleMessageHandler(this::handleMessage);
    }
    
    public static NetworkBattleManager getInstance() {
        if (instance == null) {
            instance = new NetworkBattleManager();
        }
        return instance;
    }
    
    /**
     * Reset the battle manager (for new game).
     */
    public static void reset() {
        if (instance != null) {
            instance.entityRegistry.clear();
            EntityIdGenerator.getInstance().reset();
        }
        instance = null;
    }
    
    /**
     * Central message handler - routes messages to appropriate handlers.
     * This is called from network thread, so we enqueue messages for thread-safe processing.
     */
    private void handleMessage(NetworkMessage message) {
        // Enqueue message for processing on game loop/FX thread
        NetworkMessageQueue.getInstance().enqueue(message);
    }
    
    /**
     * Process queued messages (called from game loop or FX thread).
     * This ensures all arenaMap/UI mutations happen on the correct thread.
     */
    public void processQueuedMessages() {
        NetworkMessageQueue.getInstance().processMessagesOnFXThread(this::routeMessage);
    }
    
    /**
     * Route message to appropriate handler (called on FX thread).
     */
    private void routeMessage(NetworkMessage message) {
        switch (message.getType()) {
            case CARD_PLACEMENT_REQUEST:
                if (isHost && onCardPlacementReceived != null) {
                    onCardPlacementReceived.accept(message);
                }
                break;
                
            case SPELL_CAST_REQUEST:
                if (isHost && onCardPlacementReceived != null) {
                    onCardPlacementReceived.accept(message);
                }
                break;
                
            case SPELL_CAST_EVENT:
                if (isClient && onEntitySpawnReceived != null) {
                    onEntitySpawnReceived.accept(message);
                }
                break;
                
            case ENTITY_SPAWN:
                if (isClient && onEntitySpawnReceived != null) {
                    onEntitySpawnReceived.accept(message);
                }
                break;
                
            case ENTITY_UPDATE:
            case ENTITY_DEATH:
                if (isClient && onEntityUpdateReceived != null) {
                    onEntityUpdateReceived.accept(message);
                }
                break;
                
            case TOWER_UPDATE:
            case TOWER_DESTROY:
                if (isClient && onTowerUpdateReceived != null) {
                    onTowerUpdateReceived.accept(message);
                }
                break;
                
            case ELIXIR_UPDATE:
            case GAME_STATE_SNAPSHOT:
                if (isClient && onGameStateReceived != null) {
                    onGameStateReceived.accept(message);
                }
                break;
                
            case GAME_END:
                if (isClient && onGameEndReceived != null) {
                    onGameEndReceived.accept(message);
                }
                break;
                
            case PLACEMENT_REJECTED:
                // Placement rejection is sent to entity spawn handler on client
                if (isClient && onEntitySpawnReceived != null) {
                    onEntitySpawnReceived.accept(message);
                }
                break;
                
            default:
                // Ignore lobby messages
                break;
        }
    }
    
    // ========== Message Sending Methods ==========
    
    /**
     * Send card placement request (Client → Host) with requestId.
     */
    public void sendCardPlacementRequest(int cardId, int row, int col, int requestId) {
        if (!isClient) {
            System.err.println("NetworkBattleManager: Only client can send card placement request");
            return;
        }
        
        String data = requestId + "|" + cardId + "|" + row + "|" + col;
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.CARD_PLACEMENT_REQUEST,
            2, // Player 2 (client)
            data,
            String.valueOf(System.currentTimeMillis())
        );
        networkManager.sendMessage(msg);
    }
    
    /**
     * Send spell cast request (Client → Host) with requestId.
     */
    public void sendSpellCastRequest(int spellId, int row, int col, int requestId) {
        if (!isClient) {
            System.err.println("NetworkBattleManager: Only client can send spell cast request");
            return;
        }
        
        String data = requestId + "|" + spellId + "|" + row + "|" + col;
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.SPELL_CAST_REQUEST,
            2, // Player 2 (client)
            data,
            String.valueOf(System.currentTimeMillis())
        );
        networkManager.sendMessage(msg);
    }
    
    /**
     * Send spell cast event (Host → Client) for VFX.
     */
    public void sendSpellCastEvent(int spellId, int row, int col) {
        if (!isHost) {
            System.err.println("NetworkBattleManager: Only host can send spell cast event");
            return;
        }
        
        String data = spellId + "|" + row + "|" + col;
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.SPELL_CAST_EVENT,
            1,
            data,
            String.valueOf(System.currentTimeMillis())
        );
        networkManager.sendMessage(msg);
    }
    
    /**
     * Send entity spawn notification (Host → Client).
     */
    public void sendEntitySpawn(AliveEntity entity, int row, int col) {
        if (!isHost) {
            System.err.println("NetworkBattleManager: Only host can send entity spawn");
            return;
        }
        
        String data = entity.getEntityId() + "|" + 
                     entity.getCard().getId() + "|" +
                     row + "|" + col + "|" +
                     (entity.isPlayer() ? "1" : "2") + "|" +
                     entity.getHP() + "|" +
                     ((kuroyale.cardpack.subclasses.AliveCard) entity.getCard()).getHp();
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.ENTITY_SPAWN,
            1,
            data,
            String.valueOf(System.currentTimeMillis())
        );
        networkManager.sendMessage(msg);
        System.out.println("Host: ENTITY_SPAWN sent - ID=" + entity.getEntityId() + 
                         ", card=" + entity.getCard().getId() + 
                         ", owner=" + (entity.isPlayer() ? "1" : "2") + 
                         ", position=(" + row + ", " + col + ")");
    }
    
    /**
     * Send entity update (Host → Client).
     */
    public void sendEntityUpdate(AliveEntity entity) {
        if (!isHost) {
            return;
        }
        
        String data = entity.getEntityId() + "|" +
                     entity.getHP() + "|" +
                     entity.getRow() + "|" +
                     entity.getCol();
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.ENTITY_UPDATE,
            1,
            data,
            String.valueOf(System.currentTimeMillis())
        );
        networkManager.sendMessage(msg);
    }
    
    /**
     * Send entity death (Host → Client).
     */
    public void sendEntityDeath(long entityId) {
        if (!isHost) {
            return;
        }
        
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.ENTITY_DEATH,
            1,
            String.valueOf(entityId),
            String.valueOf(System.currentTimeMillis())
        );
        networkManager.sendMessage(msg);
    }
    
    /**
     * Send tower update (Host → Client) using TowerId enum.
     */
    public void sendTowerUpdate(kuroyale.mainpack.network.TowerId towerId, double hp, double maxHp) {
        if (!isHost) {
            return;
        }
        
        String data = towerId.name() + "|" + hp + "|" + maxHp;
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.TOWER_UPDATE,
            1,
            data,
            String.valueOf(System.currentTimeMillis())
        );
        networkManager.sendMessage(msg);
    }
    
    /**
     * Legacy method for string-based tower ID (for backward compatibility).
     * @deprecated Use sendTowerUpdate(TowerId, double, double) instead
     */
    @Deprecated
    public void sendTowerUpdate(String towerId, double hp, double maxHp) {
        try {
            kuroyale.mainpack.network.TowerId id = kuroyale.mainpack.network.TowerId.valueOf(towerId);
            sendTowerUpdate(id, hp, maxHp);
        } catch (IllegalArgumentException e) {
            // Fallback to old string format
            String data = towerId + "|" + hp + "|" + maxHp;
            NetworkMessage msg = new NetworkMessage(
                NetworkMessage.MessageType.TOWER_UPDATE,
                1,
                data,
                String.valueOf(System.currentTimeMillis())
            );
            networkManager.sendMessage(msg);
        }
    }
    
    /**
     * Send tower destruction (Host → Client) using TowerId enum.
     */
    public void sendTowerDestroy(kuroyale.mainpack.network.TowerId towerId) {
        if (!isHost) {
            return;
        }
        
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.TOWER_DESTROY,
            1,
            towerId.name(),
            String.valueOf(System.currentTimeMillis())
        );
        networkManager.sendMessage(msg);
        System.out.println("Host: TOWER_DESTROY sent - " + towerId);
    }
    
    /**
     * Legacy method for backward compatibility.
     */
    @Deprecated
    public void sendTowerDestroy(String towerId) {
        try {
            kuroyale.mainpack.network.TowerId id = kuroyale.mainpack.network.TowerId.valueOf(towerId);
            sendTowerDestroy(id);
        } catch (IllegalArgumentException e) {
            // Fallback to old string format
            NetworkMessage msg = new NetworkMessage(
                NetworkMessage.MessageType.TOWER_DESTROY,
                1,
                towerId,
                String.valueOf(System.currentTimeMillis())
            );
            networkManager.sendMessage(msg);
        }
    }
    
    /**
     * Send elixir update (Host → Client).
     */
    public void sendElixirUpdate(double player1Elixir, double player2Elixir) {
        if (!isHost) {
            return;
        }
        
        String data = player1Elixir + "|" + player2Elixir;
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.ELIXIR_UPDATE,
            1,
            data,
            String.valueOf(System.currentTimeMillis())
        );
        networkManager.sendMessage(msg);
    }
    
    /**
     * Send game state snapshot (Host → Client).
     * Format: "timer|elixir1|elixir2|towers|entities"
     */
    public void sendGameStateSnapshot(String snapshotData) {
        if (!isHost) {
            return;
        }
        
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.GAME_STATE_SNAPSHOT,
            1,
            snapshotData,
            String.valueOf(System.currentTimeMillis())
        );
        networkManager.sendMessage(msg);
    }
    
    /**
     * Send game end (Host → Client).
     */
    public void sendGameEnd(int winner, String reason) {
        if (!isHost) {
            return;
        }
        
        String data = winner + "|" + reason;
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.GAME_END,
            1,
            data,
            String.valueOf(System.currentTimeMillis())
        );
        networkManager.sendMessage(msg);
    }
    
    /**
     * Send placement rejected (Host → Client) with requestId.
     */
    public void sendPlacementRejected(int requestId, String reason) {
        if (!isHost) {
            return;
        }
        
        String data = requestId + "|" + reason;
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.PLACEMENT_REJECTED,
            1,
            data,
            String.valueOf(System.currentTimeMillis())
        );
        networkManager.sendMessage(msg);
    }
    
    /**
     * Legacy method for backward compatibility.
     */
    @Deprecated
    public void sendPlacementRejected(String reason) {
        sendPlacementRejected(0, reason);
    }
    
    // ========== Callback Registration ==========
    
    public void setOnCardPlacementReceived(Consumer<NetworkMessage> handler) {
        this.onCardPlacementReceived = handler;
    }
    
    public void setOnEntitySpawnReceived(Consumer<NetworkMessage> handler) {
        this.onEntitySpawnReceived = handler;
    }
    
    public void setOnEntityUpdateReceived(Consumer<NetworkMessage> handler) {
        this.onEntityUpdateReceived = handler;
    }
    
    public void setOnTowerUpdateReceived(Consumer<NetworkMessage> handler) {
        this.onTowerUpdateReceived = handler;
    }
    
    public void setOnGameStateReceived(Consumer<NetworkMessage> handler) {
        this.onGameStateReceived = handler;
    }
    
    public void setOnGameEndReceived(Consumer<NetworkMessage> handler) {
        this.onGameEndReceived = handler;
    }
    
    // ========== Getters ==========
    
    public EntityRegistry getEntityRegistry() {
        return entityRegistry;
    }
    
    public boolean isHost() {
        return isHost;
    }
    
    public boolean isClient() {
        return isClient;
    }
}

