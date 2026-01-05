package kuroyale.mainpack.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Handles sending and receiving NetworkMessage objects over the network.
 */
public class MessageProtocol {
    
    /**
     * Sends a NetworkMessage over the ObjectOutputStream.
     */
    public static void sendMessage(ObjectOutputStream out, NetworkMessage message) throws IOException {
        out.writeObject(message);
        out.flush();
    }
    
    /**
     * Receives a NetworkMessage from the ObjectInputStream.
     */
    public static NetworkMessage receiveMessage(ObjectInputStream in) throws IOException, ClassNotFoundException {
        return (NetworkMessage) in.readObject();
    }
}

