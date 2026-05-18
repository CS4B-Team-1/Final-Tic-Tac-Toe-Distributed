package project.protocol;

public class MoveRejectedMessage implements Message {
    private String gameId;
    private String playerId;
    private int row;
    private int column;
    private RejectReason reason;

    public enum RejectReason {
        INVALID_PLAYER,
        NO_SECOND_PLAYER,
        INVALID_SYMBOL,
        NOT_CURRENT_TURN,
        INVALID_MOVE,
        INVALID_STATUS,
        NO_GAME_EXISTS
    }

    public MoveRejectedMessage(String gameId, String playerId, int row, int column, RejectReason reason) {
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

    public RejectReason getReason() {
        return this.reason;
    }
}