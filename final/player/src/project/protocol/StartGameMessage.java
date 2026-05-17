package project.protocol;


//If you need to find the game that was just started, search the gameID in the games hashmap
public class StartGameMessage implements Message {

    private String gameId;
    private String startingPlayerId;
    private String symbol;

    public StartGameMessage(String gameId, String startingPlayerId, String symbol) {
        this.gameId = gameId;
        this.startingPlayerId = startingPlayerId;
        this.symbol = symbol;
    }

    public String getGameId() {
        return this.gameId;
    }

    public String getStartingPlayerId() {
        return this.startingPlayerId;
    }

    public String getSymbol() {
        return this.symbol;
    }
}