package edu.cs4b.protocol;

public class MakeMoveMessage implements Message {
    private String gameId;
    private String playerId;
    private int row;
    private int column;

    public MakeMoveMessage(String gameId, String playerId, int row, int column) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.row = row;
        this.column = column;
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

    public int getColumn() {
        return this.column;
    }
}