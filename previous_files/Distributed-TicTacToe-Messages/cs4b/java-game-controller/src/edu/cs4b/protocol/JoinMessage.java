package edu.cs4b.protocol;

/**
 * Sent when a player joins a channel.
 * Demonstrates a Message with a single String field.
 */
public class JoinMessage implements Message {

    private static final long serialVersionUID = 1L;

    private final String playerName;

    public JoinMessage(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    @Override
    public String toString() {
        return "JoinMessage{playerName='" + playerName + "'}";
    }
}
