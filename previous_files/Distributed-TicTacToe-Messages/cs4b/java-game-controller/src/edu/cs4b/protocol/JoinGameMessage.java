package edu.cs4b.protocol;

public class JoinGameMessage implements Message {
    private String playerId;
    private String gameId;

    public JoinGameMessage(String playerId, String gameId) {
        this.playerId = playerId;
        this.gameId = gameId;
    }

    public String getPlayerId() {
        return this.playerId;
    }
    
    public String getGameId() {
        return this.gameId;
    }
}