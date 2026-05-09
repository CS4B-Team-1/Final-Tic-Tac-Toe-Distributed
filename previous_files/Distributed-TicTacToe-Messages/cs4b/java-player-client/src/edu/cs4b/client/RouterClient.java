package edu.cs4b.client;

import edu.cs4b.protocol.Envelope;
import edu.cs4b.protocol.EnvelopeType;
import edu.cs4b.protocol.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client library for connecting to the message router.
 *
 * Usage:
 *   RouterClient client = new RouterClient("localhost", 5000);
 *   client.connect();
 *
 *   client.subscribe("lobby");
 *   client.addMessageListener("lobby", (channel, senderId, msg) -> { ... });
 *   client.send("lobby", new TextMessage("hello!"));
 *
 *   client.unsubscribe("lobby");
 *   client.disconnect();
 */
public class RouterClient {

    private final String host;
    private final int port;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private Thread listenerThread;
    private volatile boolean running = false;

    private String clientId;

    // Channel name -> list of listeners
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<MessageListener>> listeners
            = new ConcurrentHashMap<>();

    public RouterClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Connect to the router. Blocks until the connection is established
     * and the server assigns a client ID.
     */
    public void connect() throws IOException {
        socket = new Socket(host, port);

        // OOS before OIS — must match the server's order
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        // Read the connection ACK to get our client ID
        try {
            Envelope ack = (Envelope) in.readObject();
            if (ack.getType() == EnvelopeType.ACK) {
                clientId = ack.getSenderId();
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to read connection ACK", e);
        }

        // Start the listener thread
        running = true;
        listenerThread = new Thread(this::listenLoop, "RouterClient-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        System.out.println("Connected to router at " + host + ":" + port + " as " + clientId);
    }

    /**
     * Disconnect from the router gracefully.
     */
    public void disconnect() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        System.out.println("Disconnected from router");
    }

    /**
     * Subscribe to a channel. Messages sent to this channel by other
     * clients will be delivered to registered MessageListeners.
     */
    public void subscribe(String channel) throws IOException {
        send(Envelope.subscribe(channel));
    }

    /**
     * Subscribe to a channel and register a listener in one call.
     */
    public void subscribe(String channel, MessageListener listener) throws IOException {
        addMessageListener(channel, listener);
        subscribe(channel);
    }

    /**
     * Unsubscribe from a channel.
     */
    public void unsubscribe(String channel) throws IOException {
        send(Envelope.unsubscribe(channel));
    }

    /**
     * Send a message to a channel.
     */
    public void send(String channel, Message message) throws IOException {
        send(Envelope.message(channel, message));
    }

    /**
     * Register a listener for messages on a specific channel.
     */
    public void addMessageListener(String channel, MessageListener listener) {
        listeners.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Remove a listener from a channel.
     */
    public void removeMessageListener(String channel, MessageListener listener) {
        List<MessageListener> channelListeners = listeners.get(channel);
        if (channelListeners != null) {
            channelListeners.remove(listener);
        }
    }

    /**
     * Get the client ID assigned by the router.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Check if currently connected.
     */
    public boolean isConnected() {
        return running && socket != null && !socket.isClosed();
    }

    // --- Private helpers ---

    private synchronized void send(Envelope envelope) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected to router");
        }
        out.writeObject(envelope);
        out.flush();
    }

    private void listenLoop() {
        try {
            while (running) {
                Envelope envelope = (Envelope) in.readObject();
                handleEnvelope(envelope);
            }
        } catch (EOFException e) {
            // Server closed the connection
        } catch (IOException e) {
            if (running) {
                System.err.println("Connection to router lost: " + e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Received unknown message class: " + e.getMessage());
        } finally {
            running = false;
        }
    }

    private void handleEnvelope(Envelope envelope) {
        switch (envelope.getType()) {
            case MESSAGE -> {
                String channel = envelope.getChannel();
                Message payload = envelope.getPayload();
                String senderId = envelope.getSenderId();

                // Check all registered listener keys to find matches.
                // A listener registered under "/game/*" should fire when
                // a message arrives on "/game/abc123", because the router
                // delivered it based on the wildcard subscription.
                for (var entry : listeners.entrySet()) {
                    String key = entry.getKey();
                    // Match if the key is the exact channel, OR if the key
                    // is a wildcard pattern that matches the channel.
                    if (key.equals(channel) || matchesWildcard(key, channel)) {
                        for (MessageListener listener : entry.getValue()) {
                            listener.onMessage(channel, senderId, payload);
                        }
                    }
                }
            }
            case ACK -> {
                // ACKs are silently consumed; could add a callback here if needed
            }
            case ERROR -> {
                System.err.println("[Router Error] " + envelope.getPayload());
            }
            default -> {
                // Ignore unexpected envelope types
            }
        }
    }

    /**
     * Check if a wildcard pattern matches a specific channel name.
     * "/game/*" matches any channel starting with "/game/".
     */
    private boolean matchesWildcard(String pattern, String channelName) {
        if (!pattern.endsWith("/*")) {
            return false;
        }
        String prefix = pattern.substring(0, pattern.length() - 1);
        return channelName.startsWith(prefix);
    }
}
