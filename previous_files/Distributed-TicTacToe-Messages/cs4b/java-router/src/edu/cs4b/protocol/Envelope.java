package edu.cs4b.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * The wire-level wrapper for all communication between clients and the router.
 *
 * Every piece of data sent over the network is wrapped in an Envelope.
 * The router inspects the type and channel to decide what to do, and
 * forwards the payload without interpreting it.
 *
 * WHY byte[] INSTEAD OF Message?
 *
 * The payload is stored as raw bytes (byte[]) rather than a Message object.
 * This is critical for keeping the router "dumb":
 *
 *   - Java's ObjectInputStream deserializes the ENTIRE object graph. If we
 *     stored a Message object directly in the Envelope, the router would
 *     need every student's custom Message class on its classpath — otherwise
 *     it throws ClassNotFoundException.
 *
 *   - By serializing the Message to bytes on the CLIENT side and storing
 *     those bytes in the Envelope, the router just sees a byte[] (which
 *     Java always knows how to deserialize). It forwards the bytes without
 *     ever needing to know what class they represent.
 *
 *   - The RECEIVING client deserializes the bytes back into a Message
 *     object. Since clients have the message classes on their classpath,
 *     this works fine.
 *
 * The factory methods (message(), getPayload()) handle the byte conversion
 * automatically, so students never deal with byte[] directly.
 */
public class Envelope implements Serializable {

    private static final long serialVersionUID = 1L;

    private final EnvelopeType type;
    private final String channel;
    private final String senderId;

    // Raw serialized bytes of the Message object. The router forwards
    // these without deserializing — it doesn't need the Message class.
    private final byte[] payloadBytes;

    public Envelope(EnvelopeType type, String channel, String senderId, byte[] payloadBytes) {
        this.type = type;
        this.channel = channel;
        this.senderId = senderId;
        this.payloadBytes = payloadBytes;
    }

    public EnvelopeType getType() {
        return type;
    }

    public String getChannel() {
        return channel;
    }

    public String getSenderId() {
        return senderId;
    }

    /**
     * Deserialize the payload bytes back into a Message object.
     * Called by the RECEIVING client — never by the router.
     *
     * Returns null if there is no payload (e.g., for SUBSCRIBE/ACK envelopes).
     */
    public Message getPayload() {
        if (payloadBytes == null) {
            return null;
        }
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(payloadBytes));
            return (Message) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to deserialize message payload", e);
        }
    }

    /**
     * Get the raw payload bytes. Used internally — students should
     * use getPayload() instead.
     */
    public byte[] getPayloadBytes() {
        return payloadBytes;
    }

    /** Creates a new Envelope with the senderId replaced. */
    public Envelope withSenderId(String newSenderId) {
        return new Envelope(type, channel, newSenderId, payloadBytes);
    }

    @Override
    public String toString() {
        return "Envelope{type=" + type
                + ", channel='" + channel + "'"
                + ", senderId='" + senderId + "'"
                + ", payloadBytes=" + (payloadBytes != null ? payloadBytes.length + " bytes" : "null")
                + "}";
    }

    // --- Factory methods for common envelope types ---

    public static Envelope subscribe(String channel) {
        return new Envelope(EnvelopeType.SUBSCRIBE, channel, null, null);
    }

    public static Envelope unsubscribe(String channel) {
        return new Envelope(EnvelopeType.UNSUBSCRIBE, channel, null, null);
    }

    /**
     * Create a MESSAGE envelope. Serializes the Message to bytes so the
     * router can forward it without knowing the Message class.
     */
    public static Envelope message(String channel, Message payload) {
        return new Envelope(EnvelopeType.MESSAGE, channel, null, serializeMessage(payload));
    }

    public static Envelope ack(String channel, String senderId) {
        return new Envelope(EnvelopeType.ACK, channel, senderId, null);
    }

    public static Envelope error(String errorMessage) {
        return new Envelope(EnvelopeType.ERROR, null, null,
                serializeMessage(new TextMessage(errorMessage)));
    }

    // --- Helper to serialize a Message to byte[] ---

    private static byte[] serializeMessage(Message message) {
        if (message == null) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(message);
            oos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize message: " + message, e);
        }
    }
}
