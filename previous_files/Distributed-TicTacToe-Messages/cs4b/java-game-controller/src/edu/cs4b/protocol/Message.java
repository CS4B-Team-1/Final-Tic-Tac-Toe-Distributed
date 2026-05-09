package edu.cs4b.protocol;

import java.io.Serializable;

/**
 * Marker interface for all application-level messages.
 *
 * To create a new message type, simply define a class that implements
 * this interface and add whatever fields you need. For example:
 *
 *   public class MoveMessage implements Message {
 *       private final int row;
 *       private final int col;
 *       // constructor, getters...
 *   }
 *
 * The message will be automatically serialized and sent over the network
 * inside an Envelope.
 */
public interface Message extends Serializable {
}
