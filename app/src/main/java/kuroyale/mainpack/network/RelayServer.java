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
 * Rendezvous/Forward Server for NAT traversal with message direction enforcement and room support.
 * 
 * This server solves the NAT problem by allowing both host and client
 * to connect OUTBOUND to a publicly accessible server. The relay then
 * forwards NetworkMessage objects between them.
 * 
 * Architecture:
 * - Host connects to relay → identified by playerId=1
 * - Client connects to relay → identified by playerId=2
 * - Relay forwards messages: Client→Relay→Host and Host→Relay→Client
 * - Messages are validated for direction (CLIENT→HOST or HOST→CLIENT only)
 * - Room support: Multiple games can run simultaneously via room codes
 * 
 * IMPORTANT: This server contains NO game logic. It only forwards messages.
 * All game logic remains in the game client (host is authoritative).
 */
public class RelayServer {
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private Thread acceptThread;
    
    // Room support: Map<roomCode, Room>
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    
    // Map to track all connections
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
     * Represents a game room with host and client connections.
     */
    private static class Room {
        volatile RelayConnection host = null;
        volatile RelayConnection client = null;
        final String roomCode;
        
        Room(String roomCode) {
            this.roomCode = roomCode;
        }
        
        synchronized void setHost(RelayConnection host) {
            if (this.host != null && this.host != host) {
                this.host.close();
            }
            this.host = host;
        }
        
        synchronized void setClient(RelayConnection client) {
            if (this.client != null && this.client != client) {
                this.client.close();
            }
            this.client = client;
        }
        
        synchronized RelayConnection getPeer(boolean isHost) {
            return isHost ? client : host;
        }
        
        synchronized void removeConnection(RelayConnection conn) {
            if (host == conn) {
                host = null;
            }
            if (client == conn) {
                client = null;
            }
        }
        
        synchronized boolean isEmpty() {
            return host == null && client == null;
        }
    }
    
    /**
     * Inner class to represent a connection to the relay.
     */
    private static class RelayConnection {
        final Socket socket;
        final ObjectInputStream in;
        final ObjectOutputStream out;
        final boolean isHost;
        final String roomCode;
        final String playerName;
        final long connectTime;
        
        RelayConnection(Socket socket, ObjectInputStream in, ObjectOutputStream out, 
                       boolean isHost, String roomCode, String playerName) {
            this.socket = socket;
            this.in = in;
            this.out = out;
            this.isHost = isHost;
            this.roomCode = roomCode;
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
            
            // Parse CONNECT message data: "playerName|roomCode" or just "playerName" (backward compatible)
            String connectData = connectMsg.getData();
            String playerName;
            String roomCode;
            if (connectData != null && connectData.contains("|")) {
                String[] parts = connectData.split("\\|", 2);
                playerName = parts[0];
                roomCode = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : "default";
            } else {
                playerName = connectData != null ? connectData : "Unknown";
                roomCode = "default";
            }
            
            // Determine if this is HOST (playerId == 1) or CLIENT (playerId == 2)
            boolean isHost = (connectMsg.getPlayerId() == 1);
            String role = isHost ? "HOST" : "CLIENT";
            
            System.out.println(String.format("[%d] Relay: CONNECT received from %s - role=%s, playerId=%d, playerName=%s, roomCode=%s", 
                receiveTime, remoteAddr, role, connectMsg.getPlayerId(), playerName, roomCode));
            
            // Get or create room
            Room room = rooms.computeIfAbsent(roomCode, Room::new);
            
            connection = new RelayConnection(socket, in, out, isHost, roomCode, playerName);
            connections.put(socket, connection);
            
            // Register connection in room
            synchronized (room) {
                if (isHost) {
                    room.setHost(connection);
                } else {
                    room.setClient(connection);
                }
            }
            
            System.out.println(String.format("[%d] Relay: Connection registered - role=%s, roomCode=%s, playerName=%s", 
                receiveTime, role, roomCode, playerName));
            
            // Forward the CONNECT message to peer if peer is already connected in the same room
            RelayConnection peer = room.getPeer(isHost);
            if (peer != null && !peer.socket.isClosed()) {
                try {
                    MessageProtocol.sendMessage(peer.out, connectMsg);
                    System.out.println(String.format("[%d] Relay: FORWARDED CONNECT from %s (%s) to %s (%s) in room %s", 
                        receiveTime, role, playerName, peer.isHost ? "HOST" : "CLIENT", peer.playerName, roomCode));
                } catch (IOException e) {
                    System.err.println(String.format("[%d] Relay: Error forwarding CONNECT message: %s", receiveTime, e.getMessage()));
                }
            } else {
                System.out.println(String.format("[%d] Relay: CONNECT forwarded - peer not connected yet in room %s", receiveTime, roomCode));
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
     */
    private void forwardMessages(RelayConnection connection, Room room) {
        try {
            while (isRunning && connection.socket != null && !connection.socket.isClosed()) {
                // Receive message from this connection
                long receiveTime = System.currentTimeMillis();
                NetworkMessage message = MessageProtocol.receiveMessage(connection.in);
                
                String senderRole = connection.isHost ? "HOST" : "CLIENT";
                int playerId = connection.isHost ? 1 : 2;
                String senderInfo = String.format("%s (playerId=%d, room=%s, player=%s)", 
                    senderRole, playerId, connection.roomCode, connection.playerName);
                
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
                
                // Get peer from room
                RelayConnection peer = room.getPeer(connection.isHost);
                
                if (peer != null && !peer.socket.isClosed()) {
                    try {
                        MessageProtocol.sendMessage(peer.out, message);
                        String peerRole = peer.isHost ? "HOST" : "CLIENT";
                        int peerPlayerId = peer.isHost ? 1 : 2;
                        System.out.println(String.format("[%d] Relay: FORWARDED %s from %s to %s (playerId=%d, room=%s, player=%s)", 
                            receiveTime, message.getType(), senderRole, peerRole, peerPlayerId, peer.roomCode, peer.playerName));
                    } catch (IOException e) {
                        System.err.println(String.format("[%d] Relay: Error forwarding %s: %s", receiveTime, message.getType(), e.getMessage()));
                        break;
                    }
                } else {
                    System.out.println(String.format("[%d] Relay: DROPPED %s from %s - Reason: Peer not connected in room %s", 
                        receiveTime, message.getType(), senderInfo, connection.roomCode));
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
    
    private void cleanupConnection(Socket socket) {
        RelayConnection conn = connections.remove(socket);
        if (conn != null) {
            String role = conn.isHost ? "HOST" : "CLIENT";
            long cleanupTime = System.currentTimeMillis();
            System.out.println(String.format("[%d] Relay: %s disconnected - room=%s, player=%s", 
                cleanupTime, role, conn.roomCode, conn.playerName));
            
            // Remove from room
            Room room = rooms.get(conn.roomCode);
            if (room != null) {
                synchronized (room) {
                    room.removeConnection(conn);
                    // Clean up empty rooms
                    if (room.isEmpty()) {
                        rooms.remove(conn.roomCode);
                        System.out.println(String.format("[%d] Relay: Room %s removed (empty)", cleanupTime, conn.roomCode));
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
