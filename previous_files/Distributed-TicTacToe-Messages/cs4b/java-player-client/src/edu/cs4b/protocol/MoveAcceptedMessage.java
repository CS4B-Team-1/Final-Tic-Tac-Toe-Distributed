package edu.cs4b.protocol;

import java.util.List;

public class MoveAcceptedMessage implements Message {
    private String gameId;
    private String playerId;    // the playerId of the player that made the move
    private int row;
    private int col;
    private List<Integer> updatedBoard;
    private String nextTurn;    // the playerId of the player whose turn is next
    private GameStatus gameStatus;

    public MoveAcceptedMessage(String gameId, String playerId, int row, int col,
                               List<Integer> updatedBoard, String nextTurn, GameStatus gameStatus) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.row = row;
        this.col = col;
        this.updatedBoard = updatedBoard;
        this.nextTurn = nextTurn;
        this.gameStatus = gameStatus;
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
        return this.col;
    }

    public List<Integer> getUpdatedBoard() {
        return this.updatedBoard;
    }

    public String getNextTurn() {
        return this.nextTurn;
    }

    public GameStatus getGameStatus() {
        return this.gameStatus;
    }
}