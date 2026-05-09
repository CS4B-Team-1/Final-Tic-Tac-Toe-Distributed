package edu.cs4b.protocol;

public class GameCreatedMessage implements Message {
    private String gameId;
    private String channel;
    private String status;


    public GameCreatedMessage(String gameId, String channel, String status) {
        this.gameId = gameId;
        this.channel = channel;
        this.status = status; 
    }

    public String getGameId() {
        return this.gameId;
    }

    public String getChannel() {
        return this.channel;
    }

    public String getStatus() {
        return this.status;
    }
}