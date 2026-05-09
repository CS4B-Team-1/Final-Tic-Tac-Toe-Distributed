package edu.cs4b.router;

import edu.cs4b.protocol.Envelope;
import edu.cs4b.protocol.EnvelopeType;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Handles a single client connection on its own thread.
 *
 * THREADING MODEL
 *
 * Each connected client gets its own ClientHandler running on its own
 * thread (see RouterMain). This means:
 *   - The run() method's read loop blocks on in.readObject(), waiting
 *     for this client to send something. That's fine because this
 *     thread has nothing else to do — it's dedicated to this client.
 *   - When this client sends a MESSAGE, this thread (the sender's
 *     thread) is the one that iterates over the channel's subscribers
 *     and delivers the message to each one via sendEnvelope().
 *
 * This means sendEnvelope() can be called from MULTIPLE threads:
 *   - This handler's own thread (to send ACKs back to this client)
 *   - OTHER handlers' threads (when they broadcast a message to a
 *     channel this client is subscribed to)
 * That's why sendEnvelope() is synchronized — see the comment there.
 *
 * Lifecycle:
 *   1. Set up ObjectOutputStream then ObjectInputStream (order matters!)
 *   2. Send ACK with assigned clientId
 *   3. Enter read loop: receive envelopes and dispatch by type
 *   4. On disconnect (normal or crash): clean up channel subscriptions
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChannelManager channelManager;
    private final String clientId;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket socket, ChannelManager channelManager, String clientId) {
        this.socket = socket;
        this.channelManager = channelManager;
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public void run() {
        try {
            // IMPORTANT: ObjectOutputStream must be created BEFORE ObjectInputStream
            // on BOTH sides (client and server).
            //
            // Why? ObjectOutputStream writes a stream header when constructed.
            // ObjectInputStream reads that header when constructed. If both sides
            // try to create ObjectInputStream first, they'll both block waiting
            // for the other's header — classic deadlock!
            //
            // Convention: always create OOS first, flush it (to send the header),
            // then create OIS (which reads the other side's header).
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();  // flush the stream header immediately
            in = new ObjectInputStream(socket.getInputStream());

            // Send connection ACK with the assigned client ID so the client
            // knows what ID the server gave it.
            sendEnvelope(Envelope.ack(null, clientId));
            System.out.println("[Router] " + clientId + " connected from "
                    + socket.getRemoteSocketAddress());

            // BLOCKING READ LOOP
            // readObject() blocks until the client sends something.
            // This is the main event loop for this client's thread.
            // It exits when the client disconnects (EOFException) or
            // the connection drops (IOException).
            while (!socket.isClosed()) {
                Envelope envelope = (Envelope) in.readObject();
                handleEnvelope(envelope);
            }

        } catch (EOFException e) {
            // Normal disconnection: the client closed its socket, so
            // readObject() hit the end of the stream. Not an error.
        } catch (IOException e) {
            // Abnormal disconnection: network failure, client crashed, etc.
            System.out.println("[Router] " + clientId + " connection lost: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            // The client sent a serialized object whose class isn't on our
            // classpath. This would happen if a client sends a custom Message
            // type that the router doesn't have compiled.
            System.out.println("[Router] " + clientId + " sent unknown class: " + e.getMessage());
        } finally {
            // finally block ALWAYS runs, whether we exited normally or via
            // exception. This guarantees cleanup happens no matter what.
            handleDisconnect();
        }
    }

    private void handleEnvelope(Envelope envelope) {
        switch (envelope.getType()) {
            case SUBSCRIBE -> {
                channelManager.subscribe(envelope.getChannel(), this);
                sendEnvelope(Envelope.ack(envelope.getChannel(), clientId));
            }
            case UNSUBSCRIBE -> {
                channelManager.unsubscribe(envelope.getChannel(), this);
                sendEnvelope(Envelope.ack(envelope.getChannel(), clientId));
            }
            case MESSAGE -> {
                // Stamp the sender ID so recipients know who sent it,
                // then broadcast to everyone on the channel except the sender.
                Envelope stamped = envelope.withSenderId(clientId);
                channelManager.broadcast(envelope.getChannel(), stamped, this);
            }
            default -> {
                sendEnvelope(Envelope.error("Unexpected envelope type: " + envelope.getType()));
            }
        }
    }

    /**
     * Send an envelope to this client's output stream.
     *
     * WHY SYNCHRONIZED?
     *
     * ObjectOutputStream is NOT thread-safe. If two threads call
     * writeObject() at the same time, the serialized bytes get
     * interleaved and the stream is corrupted.
     *
     * When does this happen? Consider this scenario:
     *   - Client A sends a message to the "lobby" channel.
     *   - Client B also sends a message to the "lobby" channel at
     *     the same time.
     *   - Client C is subscribed to "lobby".
     *   - Client A's thread calls C.sendEnvelope() to deliver A's message.
     *   - Client B's thread calls C.sendEnvelope() to deliver B's message.
     *   - Two threads writing to C's ObjectOutputStream simultaneously!
     *
     * The "synchronized" keyword ensures only one thread at a time can
     * execute this method for a given ClientHandler instance. The second
     * thread will wait until the first finishes.
     *
     * Performance note: this means broadcasts are serialized per-recipient.
     * That's fine at classroom scale. A production system might use a
     * per-client send queue instead.
     */
    public synchronized void sendEnvelope(Envelope envelope) {
        try {
            out.writeObject(envelope);
            out.flush();  // flush after every write to ensure prompt delivery
        } catch (IOException e) {
            System.out.println("[Router] Failed to send to " + clientId + ": " + e.getMessage());
        }
    }

    /**
     * Clean up when a client disconnects. Removes the client from all
     * channels so they stop receiving broadcasts, then closes the socket.
     */
    private void handleDisconnect() {
        channelManager.unsubscribeAll(this);
        try {
            socket.close();
        } catch (IOException e) {
            // Socket might already be closed — that's fine, ignore it.
        }
        System.out.println("[Router] " + clientId + " disconnected");
    }
}
