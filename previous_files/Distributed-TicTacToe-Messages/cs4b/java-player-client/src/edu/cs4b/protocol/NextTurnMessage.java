package edu.cs4b.protocol;

public class NextTurnMessage implements Message {
    private final String previousTurnPlayerId;
    private final String currentTurnPlayerId;
    private final String gameId;

    public NextTurnMessage(String gameId, String previousId, String currentId) {
        this.previousTurnPlayerId = previousId;
        this.currentTurnPlayerId = currentId;
        this.gameId = gameId;
    }

    public String getPreviousTurnPlayerId() {
        return this.previousTurnPlayerId;
    }

    public String getCurrentTurnPlayerId() {
        return this.currentTurnPlayerId;
    }

    public String getGameId() {
        return this.gameId;
    }

}
