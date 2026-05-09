package edu.cs4b.protocol;

public class CreateGameMessage implements Message {
    private String playerId;
    private String gameId;

    public CreateGameMessage(String playerId, String gameId) {
        this.playerId = playerId;
        this.gameId = gameId;
    }

    public String getGameId() {
        return this.gameId;
    }

    public String getPlayerId() {
        return this.playerId;
    }
}