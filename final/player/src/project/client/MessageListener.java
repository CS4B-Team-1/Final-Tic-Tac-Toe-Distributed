package project.client;

import project.protocol.Message;

/**
 * Callback interface for receiving messages on a channel.
 *
 * Implement this to handle incoming messages. Since it's a
 * functional interface, you can use lambda expressions:
 *
 *   client.addMessageListener("lobby", (channel, senderId, message) -> {
 *       System.out.println(senderId + " sent: " + message);
 *   });
 */
@FunctionalInterface
public interface MessageListener {
    void onMessage(String channel, String senderId, Message message);
}