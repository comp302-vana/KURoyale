package kuroyale.mainpack.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Rendezvous/Forward Server for NAT traversal.
 * 
 * This server solves the NAT problem by allowing both host and client
 * to connect OUTBOUND to a publicly accessible server. The relay then
 * forwards NetworkMessage objects between them.
 * 
 * Architecture:
 * - Host connects to relay → identified by playerId=1
 * - Client connects to relay → identified by playerId=2
 * - Relay forwards messages: Client→Relay→Host and Host→Relay→Client
 * 
 * IMPORTANT: This server contains NO game logic. It only forwards messages.
 * All game logic remains in the game client (host is authoritative).
 */
public class RelayServer {
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private Thread acceptThread;
    
    // Track connections: HOST (playerId=1) and CLIENT (playerId=2)
    private volatile RelayConnection hostConnection = null;
    private volatile RelayConnection clientConnection = null;
    
    // Map to track all connections
    private final Map<Socket, RelayConnection> connections = new ConcurrentHashMap<>();
    
    private static final int DEFAULT_PORT = 8081;
    
    /**
     * Inner class to represent a connection to the relay.
     */
    private static class RelayConnection {
        final Socket socket;
        final ObjectInputStream in;
        final ObjectOutputStream out;
        final boolean isHost;
        final Thread receiveThread;
        
        RelayConnection(Socket socket, ObjectInputStream in, ObjectOutputStream out, boolean isHost) {
            this.socket = socket;
            this.in = in;
            this.out = out;
            this.isHost = isHost;
            this.receiveThread = null; // Will be set by relay
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
                System.out.println("Relay: New connection from " + clientSocket.getRemoteSocketAddress());
                
                // Handle each connection in a separate thread
                Thread connectionThread = new Thread(() -> handleConnection(clientSocket));
                connectionThread.setDaemon(true);
                connectionThread.start();
                
            } catch (IOException e) {
                if (isRunning && !serverSocket.isClosed()) {
                    System.err.println("Relay: Error accepting connection: " + e.getMessage());
                }
            }
        }
    }
    
    private void handleConnection(Socket socket) {
        RelayConnection connection = null;
        try {
            // CRITICAL: Create ObjectOutputStream FIRST, flush, then ObjectInputStream
            // This matches the order expected by clients
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            
            // Wait for CONNECT message to identify as HOST or CLIENT
            NetworkMessage connectMsg = MessageProtocol.receiveMessage(in);
            
            if (connectMsg.getType() != NetworkMessage.MessageType.CONNECT) {
                System.err.println("Relay: Expected CONNECT message, got: " + connectMsg.getType());
                socket.close();
                return;
            }
            
            // Determine if this is HOST (playerId == 1) or CLIENT (playerId == 2)
            boolean isHost = (connectMsg.getPlayerId() == 1);
            
            connection = new RelayConnection(socket, in, out, isHost);
            connections.put(socket, connection);
            
            System.out.println("Relay: Connection identified as " + (isHost ? "HOST" : "CLIENT"));
            
            // Register connection
            synchronized (this) {
                if (isHost) {
                    if (hostConnection != null) {
                        System.out.println("Relay: Replacing existing host connection");
                        hostConnection.close();
                    }
                    hostConnection = connection;
                } else {
                    if (clientConnection != null) {
                        System.out.println("Relay: Replacing existing client connection");
                        clientConnection.close();
                    }
                    clientConnection = connection;
                }
            }
            
            // Forward the CONNECT message to peer if peer is already connected
            // This allows the host to know when client joins, and vice versa
            RelayConnection peer = isHost ? clientConnection : hostConnection;
            if (peer != null && !peer.socket.isClosed()) {
                try {
                    MessageProtocol.sendMessage(peer.out, connectMsg);
                    System.out.println("Relay: Forwarded CONNECT message from " + 
                                     (isHost ? "HOST" : "CLIENT") + " to " + 
                                     (peer.isHost ? "HOST" : "CLIENT"));
                } catch (IOException e) {
                    System.err.println("Relay: Error forwarding CONNECT message: " + e.getMessage());
                }
            }
            
            // Start forwarding messages from this connection
            forwardMessages(connection);
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Relay: Error handling connection: " + e.getMessage());
            if (connection != null) {
                connection.close();
            }
            cleanupConnection(socket);
        }
    }
    
    /**
     * Forward messages from a connection to its peer.
     * HOST messages → CLIENT
     * CLIENT messages → HOST
     */
    private void forwardMessages(RelayConnection connection) {
        try {
            while (isRunning && connection.socket != null && !connection.socket.isClosed()) {
                // Receive message from this connection
                NetworkMessage message = MessageProtocol.receiveMessage(connection.in);
                
                // Forward to peer
                RelayConnection peer = connection.isHost ? clientConnection : hostConnection;
                
                if (peer != null && !peer.socket.isClosed()) {
                    try {
                        MessageProtocol.sendMessage(peer.out, message);
                        System.out.println("Relay: Forwarded " + message.getType() + " from " + 
                                         (connection.isHost ? "HOST" : "CLIENT") + " to " + 
                                         (peer.isHost ? "HOST" : "CLIENT"));
                    } catch (IOException e) {
                        System.err.println("Relay: Error forwarding message: " + e.getMessage());
                        break;
                    }
                } else {
                    System.out.println("Relay: Peer not connected, dropping message");
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (isRunning) {
                System.out.println("Relay: Connection closed: " + e.getMessage());
            }
        } finally {
            cleanupConnection(connection.socket);
        }
    }
    
    private void cleanupConnection(Socket socket) {
        RelayConnection conn = connections.remove(socket);
        if (conn != null) {
            conn.close();
            synchronized (this) {
                if (hostConnection == conn) {
                    hostConnection = null;
                    System.out.println("Relay: Host disconnected");
                }
                if (clientConnection == conn) {
                    clientConnection = null;
                    System.out.println("Relay: Client disconnected");
                }
            }
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

