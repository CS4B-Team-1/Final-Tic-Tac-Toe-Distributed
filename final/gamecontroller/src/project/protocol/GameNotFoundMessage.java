package project.protocol;

public class GameNotFoundMessage implements Message {

    private String gameId;

    public GameNotFoundMessage(String gameId) {
        this.gameId = gameId;
    }

    public String getGameId() {
        return gameId;
    }
}
