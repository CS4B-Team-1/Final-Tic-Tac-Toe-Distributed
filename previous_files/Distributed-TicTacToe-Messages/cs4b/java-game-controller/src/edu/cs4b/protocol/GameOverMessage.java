package edu.cs4b.protocol;

public class GameOverMessage implements Message {
    private String gameId;
    private String result;
    private String finalBoard;

    public GameOverMessage(String gameId, String result, String finalBoard) {
        this.gameId = gameId;
        this.result = result;
        this.finalBoard = finalBoard;
    }

    public String getGameId() {
        return gameId;
    }

    public String getResult() {
        return result;
    }

    public String getFinalBoard() {
        return finalBoard;
    }
}