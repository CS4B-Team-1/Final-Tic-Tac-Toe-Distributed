package edu.cs4b.protocol;

public class PlayerJoinedMessage implements Message {
    private String playerId;
    private String gameId;
    private String symbol;
    
    public PlayerJoinedMessage(String playerId, String gameId, String symbol/*X or O*/) {
        this.playerId = playerId;
        this.gameId = gameId;
    }

    public String getPlayerId() {
        return this.playerId;
    }
    
    public String getGameId() {
        return this.gameId;
    }

    public String getSymbol() {
        return this.symbol;
    }
}