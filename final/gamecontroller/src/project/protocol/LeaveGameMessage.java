package project.protocol;

public class LeaveGameMessage implements Message {
    private String gameId;
    private String playerId;

    public LeaveGameMessage(String playerID, String gameID) {
        this.gameId = gameID;
        this.playerId = playerID;
    }

    public String getGameId() {
        return gameId;
    }

    public String getPlayerId() {
        return playerId;
    }
}
