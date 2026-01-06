package kuroyale.mainpack.network;

import java.util.concurrent.ConcurrentLinkedQueue;
import javafx.application.Platform;

/**
 * Thread-safe message queue for network messages.
 * Network threads push messages here, game loop/FX thread drains and processes them.
 */
public class NetworkMessageQueue {
    private static NetworkMessageQueue instance;
    private final ConcurrentLinkedQueue<NetworkMessage> messageQueue = new ConcurrentLinkedQueue<>();
    
    private NetworkMessageQueue() {}
    
    public static NetworkMessageQueue getInstance() {
        if (instance == null) {
            instance = new NetworkMessageQueue();
        }
        return instance;
    }
    
    /**
     * Add a message to the queue (called from network thread).
     * Thread-safe: can be called from any thread.
     */
    public void enqueue(NetworkMessage message) {
        messageQueue.offer(message);
    }
    
    /**
     * Process all pending messages (called from game loop or FX thread).
     * Thread-safe: should only be called from the game loop or FX thread.
     */
    public void processMessages(java.util.function.Consumer<NetworkMessage> handler) {
        NetworkMessage msg;
        while ((msg = messageQueue.poll()) != null) {
            handler.accept(msg);
        }
    }
    
    /**
     * Process messages on FX thread (for UI updates).
     */
    public void processMessagesOnFXThread(java.util.function.Consumer<NetworkMessage> handler) {
        if (Platform.isFxApplicationThread()) {
            processMessages(handler);
        } else {
            Platform.runLater(() -> processMessages(handler));
        }
    }
    
    /**
     * Clear all pending messages.
     */
    public void clear() {
        messageQueue.clear();
    }
    
    /**
     * Get the number of pending messages.
     */
    public int size() {
        return messageQueue.size();
    }
}

