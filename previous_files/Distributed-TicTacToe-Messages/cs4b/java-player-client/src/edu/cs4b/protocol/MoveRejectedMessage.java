package edu.cs4b.protocol;

public class MoveRejectedMessage implements Message {
    private String gameId;
    private String playerId;
    private int row;
    private int column;
    private String reason;

    public MoveRejectedMessage(String gameId, String playerId, int row, int column, String reason) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.row = row;
        this.column = column;
        this.reason = reason;
    }

    public String getGameId() {
        return this.gameId;
    }

    public String getPlayerId() {
        return this.playerId;
    }

    public int getRow() {
        return this.row;
    }
    
    public int getCol() {
        return this.column;
    }

    public String getReason() {
        return this.reason;
    }
}