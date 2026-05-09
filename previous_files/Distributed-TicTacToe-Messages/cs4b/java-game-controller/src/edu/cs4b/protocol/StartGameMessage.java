package edu.cs4b.protocol;


//If you need to find the game that was just started, search the gameID in the games hashmap
public class StartGameMessage implements Message {

    private String gameId;

    public StartGameMessage(String gameId) {

        this.gameId = gameId;
    }

    public String getGameId() {
        return this.gameId;
    }

}