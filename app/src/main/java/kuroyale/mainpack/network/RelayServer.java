package kuroyale.mainpack.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Multi-room TCP Relay Server for NAT traversal.
 * 
 * This server solves the NAT problem by allowing both host and client
 * to connect OUTBOUND to a publicly accessible server. The relay then
 * forwards NetworkMessage objects between them within isolated rooms.
 * 
 * Architecture:
 * - Host connects to relay → identified by playerId=1
 * - Client connects to relay → identified by playerId=2
 * - Relay forwards messages: Client→Relay→Host and Host→Relay→Client
 * - Messages are validated for direction (CLIENT→HOST or HOST→CLIENT only)
 * - Multiple rooms: Each game session runs in an isolated room
 * - Room lifecycle: Created on first connection, removed when both peers disconnect
 * 
 * IMPORTANT: This server contains NO game logic. It only forwards messages.
 * All game logic remains in the game client (host is authoritative).
 */
public class RelayServer {
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private Thread acceptThread;
    
    // Multi-room support: Map<roomId, RelayRoom>
    // Each room is isolated - messages only forward within the same room
    private final Map<String, RelayRoom> rooms = new ConcurrentHashMap<>();
    
    // Map to track all connections for cleanup
    private final Map<Socket, RelayConnection> connections = new ConcurrentHashMap<>();
    
    private static final int DEFAULT_PORT = 8081;
    
    // CLIENT → HOST ONLY message types
    private static final Set<NetworkMessage.MessageType> CLIENT_TO_HOST_ONLY = new HashSet<>();
    static {
        CLIENT_TO_HOST_ONLY.add(NetworkMessage.MessageType.CARD_PLACEMENT_REQUEST);
        CLIENT_TO_HOST_ONLY.add(NetworkMessage.MessageType.SPELL_CAST_REQUEST);
        CLIENT_TO_HOST_ONLY.add(NetworkMessage.MessageType.CONNECT);
        CLIENT_TO_HOST_ONLY.add(NetworkMessage.MessageType.DISCONNECT);
        CLIENT_TO_HOST_ONLY.add(NetworkMessage.MessageType.READY_STATUS);
        CLIENT_TO_HOST_ONLY.add(NetworkMessage.MessageType.DECK_SELECTED);
    }
    
    // HOST → CLIENT ONLY message types
    private static final Set<NetworkMessage.MessageType> HOST_TO_CLIENT_ONLY = new HashSet<>();
    static {
        HOST_TO_CLIENT_ONLY.add(NetworkMessage.MessageType.ENTITY_SPAWN);
        HOST_TO_CLIENT_ONLY.add(NetworkMessage.MessageType.ENTITY_UPDATE);
        HOST_TO_CLIENT_ONLY.add(NetworkMessage.MessageType.ENTITY_DEATH);
        HOST_TO_CLIENT_ONLY.add(NetworkMessage.MessageType.TOWER_UPDATE);
        HOST_TO_CLIENT_ONLY.add(NetworkMessage.MessageType.TOWER_DESTROY);
        HOST_TO_CLIENT_ONLY.add(NetworkMessage.MessageType.ELIXIR_UPDATE);
        HOST_TO_CLIENT_ONLY.add(NetworkMessage.MessageType.GAME_STATE_SNAPSHOT);
        HOST_TO_CLIENT_ONLY.add(NetworkMessage.MessageType.GAME_END);
        HOST_TO_CLIENT_ONLY.add(NetworkMessage.MessageType.SPELL_CAST_EVENT);
        HOST_TO_CLIENT_ONLY.add(NetworkMessage.MessageType.PLACEMENT_REJECTED);
        HOST_TO_CLIENT_ONLY.add(NetworkMessage.MessageType.LOBBY_UPDATE);
        HOST_TO_CLIENT_ONLY.add(NetworkMessage.MessageType.PLAYER_JOINED);
        HOST_TO_CLIENT_ONLY.add(NetworkMessage.MessageType.START_GAME);
    }
    
    /**
     * Represents an isolated game room with host and client connections.
     * Thread-safe: All operations are synchronized to prevent race conditions.
     * Lifecycle: Created when first peer connects, removed when both peers disconnect.
     */
    private static class RelayRoom {
        private volatile RelayConnection host = null;
        private volatile RelayConnection client = null;
        private final String roomId;
        
        RelayRoom(String roomId) {
            this.roomId = roomId;
        }
        
        /**
         * Attach a host connection to this room.
         * If a host already exists, closes the old connection first (replacement).
         * Thread-safe.
         */
        synchronized void attachHost(RelayConnection host) {
            if (this.host != null && this.host != host) {
                System.out.println(String.format("[%d] Relay: Replacing existing host in room %s", 
                    System.currentTimeMillis(), roomId));
                this.host.close();
            }
            this.host = host;
        }
        
        /**
         * Attach a client connection to this room.
         * If a client already exists, closes the old connection first (replacement).
         * Thread-safe.
         */
        synchronized void attachClient(RelayConnection client) {
            if (this.client != null && this.client != client) {
                System.out.println(String.format("[%d] Relay: Replacing existing client in room %s", 
                    System.currentTimeMillis(), roomId));
                this.client.close();
            }
            this.client = client;
        }
        
        /**
         * Get the peer connection (opposite role) in this room.
         * Thread-safe.
         */
        synchronized RelayConnection getPeer(boolean isHost) {
            return isHost ? client : host;
        }
        
        /**
         * Remove a connection from this room (on disconnect).
         * Thread-safe.
         */
        synchronized void removeConnection(RelayConnection conn) {
            if (host == conn) {
                host = null;
            }
            if (client == conn) {
                client = null;
            }
        }
        
        /**
         * Check if room is empty (both peers disconnected).
         * Thread-safe.
         */
        synchronized boolean isEmpty() {
            return host == null && client == null;
        }
        
        String getRoomId() {
            return roomId;
        }
    }
    
    /**
     * Represents a connection to the relay server.
     * Contains socket, streams, role (host/client), room, and player info.
     */
    private static class RelayConnection {
        final Socket socket;
        final ObjectInputStream in;
        final ObjectOutputStream out;
        final boolean isHost;
        final String roomId;
        final String playerName;
        final long connectTime;
        
        RelayConnection(Socket socket, ObjectInputStream in, ObjectOutputStream out, 
                       boolean isHost, String roomId, String playerName) {
            this.socket = socket;
            this.in = in;
            this.out = out;
            this.isHost = isHost;
            this.roomId = roomId;
            this.playerName = playerName;
            this.connectTime = System.currentTimeMillis();
        }
        
        void close() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }
    }
    
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port, using default: " + DEFAULT_PORT);
            }
        }
        
        RelayServer server = new RelayServer();
        try {
            server.start(port);
            System.out.println("========================================");
            System.out.println("Relay Server running on port " + port);
            System.out.println("Multi-room support enabled");
            System.out.println("Press Ctrl+C to stop");
            System.out.println("========================================");
            
            // Keep server running
            Thread.currentThread().join();
        } catch (IOException e) {
            System.err.println("Failed to start relay server: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("\nRelay server stopped");
        }
    }
    
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        isRunning = true;
        
        acceptThread = new Thread(this::acceptConnections);
        acceptThread.setDaemon(false);
        acceptThread.start();
    }
    
    /**
     * Accept incoming connections and spawn handler threads.
     */
    private void acceptConnections() {
        while (isRunning && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                String remoteAddr = clientSocket.getRemoteSocketAddress().toString();
                long timestamp = System.currentTimeMillis();
                System.out.println(String.format("[%d] Relay: New connection from %s", timestamp, remoteAddr));
                
                // Handle each connection in a separate thread
                Thread connectionThread = new Thread(() -> handleConnection(clientSocket, remoteAddr));
                connectionThread.setDaemon(true);
                connectionThread.start();
                
            } catch (IOException e) {
                if (isRunning && !serverSocket.isClosed()) {
                    System.err.println("Relay: Error accepting connection: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Handle a new connection: establish streams, receive CONNECT, register in room.
     */
    private void handleConnection(Socket socket, String remoteAddr) {
        RelayConnection connection = null;
        try {
            // CRITICAL: Create ObjectOutputStream FIRST, flush, then ObjectInputStream
            // This matches the order expected by clients
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            
            // Wait for CONNECT message to identify as HOST or CLIENT
            long receiveTime = System.currentTimeMillis();
            NetworkMessage connectMsg = MessageProtocol.receiveMessage(in);
            
            if (connectMsg.getType() != NetworkMessage.MessageType.CONNECT) {
                System.err.println(String.format("[%d] Relay: Expected CONNECT message, got: %s from %s", 
                    receiveTime, connectMsg.getType(), remoteAddr));
                socket.close();
                return;
            }
            
            // Parse CONNECT message data: "roomId:playerName"
            // Format: roomId:playerName (e.g., "room123:Alice")
            String connectData = connectMsg.getData();
            String roomId;
            String playerName;
            if (connectData != null && connectData.contains(":")) {
                String[] parts = connectData.split(":", 2);
                roomId = parts[0];
                playerName = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : "Unknown";
            } else {
                // Fallback: if no colon, treat entire string as playerName, use "default" room
                playerName = connectData != null ? connectData : "Unknown";
                roomId = "default";
            }
            
            // Determine if this is HOST (playerId == 1) or CLIENT (playerId == 2)
            boolean isHost = (connectMsg.getPlayerId() == 1);
            String role = isHost ? "HOST" : "CLIENT";
            
            System.out.println(String.format("[%d] Relay: CONNECT received from %s - role=%s, playerId=%d, playerName=%s, roomId=%s", 
                receiveTime, remoteAddr, role, connectMsg.getPlayerId(), playerName, roomId));
            
            // Get or create room (thread-safe via ConcurrentHashMap)
            RelayRoom room = rooms.computeIfAbsent(roomId, RelayRoom::new);
            
            connection = new RelayConnection(socket, in, out, isHost, roomId, playerName);
            connections.put(socket, connection);
            
            // Register connection in room (thread-safe)
            if (isHost) {
                room.attachHost(connection);
            } else {
                room.attachClient(connection);
            }
            
            System.out.println(String.format("[%d] Relay: Connection registered - role=%s, roomId=%s, playerName=%s", 
                receiveTime, role, roomId, playerName));
            
            // Forward the CONNECT message to peer if peer is already connected in the same room
            RelayConnection peer = room.getPeer(isHost);
            if (peer != null && !peer.socket.isClosed()) {
                try {
                    MessageProtocol.sendMessage(peer.out, connectMsg);
                    System.out.println(String.format("[%d] Relay: FORWARDED CONNECT from %s (%s) to %s (%s) in room %s", 
                        receiveTime, role, playerName, peer.isHost ? "HOST" : "CLIENT", peer.playerName, roomId));
                } catch (IOException e) {
                    System.err.println(String.format("[%d] Relay: Error forwarding CONNECT message: %s", receiveTime, e.getMessage()));
                }
            } else {
                System.out.println(String.format("[%d] Relay: CONNECT forwarded - peer not connected yet in room %s", receiveTime, roomId));
            }
            
            // Start forwarding messages from this connection
            forwardMessages(connection, room);
            
        } catch (IOException | ClassNotFoundException e) {
            long errorTime = System.currentTimeMillis();
            System.err.println(String.format("[%d] Relay: Error handling connection from %s: %s", errorTime, remoteAddr, e.getMessage()));
            if (connection != null) {
                connection.close();
            }
            cleanupConnection(socket);
        }
    }
    
    /**
     * Forward messages from a connection to its peer in the same room.
     * Enforces message direction: CLIENT → HOST or HOST → CLIENT only.
     * Thread-safe: Room operations are synchronized.
     */
    private void forwardMessages(RelayConnection connection, RelayRoom room) {
        try {
            while (isRunning && connection.socket != null && !connection.socket.isClosed()) {
                // Receive message from this connection
                long receiveTime = System.currentTimeMillis();
                NetworkMessage message = MessageProtocol.receiveMessage(connection.in);
                
                String senderRole = connection.isHost ? "HOST" : "CLIENT";
                int playerId = connection.isHost ? 1 : 2;
                String senderInfo = String.format("%s (playerId=%d, room=%s, player=%s)", 
                    senderRole, playerId, connection.roomId, connection.playerName);
                
                System.out.println(String.format("[%d] Relay: Received %s from %s", 
                    receiveTime, message.getType(), senderInfo));
                
                // Validate message direction
                boolean isValidDirection = false;
                String dropReason = null;
                
                if (connection.isHost) {
                    // HOST can only send HOST → CLIENT messages
                    if (HOST_TO_CLIENT_ONLY.contains(message.getType())) {
                        isValidDirection = true;
                    } else if (CLIENT_TO_HOST_ONLY.contains(message.getType())) {
                        dropReason = "HOST attempted to send CLIENT→HOST only message";
                    } else {
                        dropReason = "Unknown message type direction";
                    }
                } else {
                    // CLIENT can only send CLIENT → HOST messages
                    if (CLIENT_TO_HOST_ONLY.contains(message.getType())) {
                        isValidDirection = true;
                    } else if (HOST_TO_CLIENT_ONLY.contains(message.getType())) {
                        dropReason = "CLIENT attempted to send HOST→CLIENT only message";
                    } else {
                        dropReason = "Unknown message type direction";
                    }
                }
                
                if (!isValidDirection) {
                    System.err.println(String.format("[%d] Relay: DROPPED %s from %s - Reason: %s", 
                        receiveTime, message.getType(), senderInfo, dropReason));
                    continue; // Drop illegal message
                }
                
                // Get peer from room (thread-safe)
                RelayConnection peer = room.getPeer(connection.isHost);
                
                if (peer != null && !peer.socket.isClosed()) {
                    try {
                        MessageProtocol.sendMessage(peer.out, message);
                        String peerRole = peer.isHost ? "HOST" : "CLIENT";
                        int peerPlayerId = peer.isHost ? 1 : 2;
                        System.out.println(String.format("[%d] Relay: FORWARDED %s from %s to %s (playerId=%d, room=%s, player=%s)", 
                            receiveTime, message.getType(), senderRole, peerRole, peerPlayerId, peer.roomId, peer.playerName));
                    } catch (IOException e) {
                        System.err.println(String.format("[%d] Relay: Error forwarding %s: %s", receiveTime, message.getType(), e.getMessage()));
                        break;
                    }
                } else {
                    System.out.println(String.format("[%d] Relay: DROPPED %s from %s - Reason: Peer not connected in room %s", 
                        receiveTime, message.getType(), senderInfo, connection.roomId));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (isRunning) {
                long errorTime = System.currentTimeMillis();
                System.out.println(String.format("[%d] Relay: Connection closed: %s", errorTime, e.getMessage()));
            }
        } finally {
            cleanupConnection(connection.socket);
        }
    }
    
    /**
     * Clean up a disconnected connection and remove empty rooms.
     * Thread-safe: Room operations are synchronized.
     */
    private void cleanupConnection(Socket socket) {
        RelayConnection conn = connections.remove(socket);
        if (conn != null) {
            String role = conn.isHost ? "HOST" : "CLIENT";
            long cleanupTime = System.currentTimeMillis();
            System.out.println(String.format("[%d] Relay: %s disconnected - room=%s, player=%s", 
                cleanupTime, role, conn.roomId, conn.playerName));
            
            // Remove from room (thread-safe)
            RelayRoom room = rooms.get(conn.roomId);
            if (room != null) {
                synchronized (room) {
                    room.removeConnection(conn);
                    // Clean up empty rooms (both peers disconnected)
                    if (room.isEmpty()) {
                        rooms.remove(conn.roomId);
                        System.out.println(String.format("[%d] Relay: Room %s removed (empty)", cleanupTime, conn.roomId));
                    }
                }
            }
            
            conn.close();
        }
    }
    
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error stopping relay server: " + e.getMessage());
        }
    }
}
