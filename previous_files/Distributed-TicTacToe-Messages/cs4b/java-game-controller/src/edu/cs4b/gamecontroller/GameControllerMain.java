package edu.cs4b.gamecontroller;

import edu.cs4b.client.MessageListener;
import edu.cs4b.client.RouterClient;
import edu.cs4b.protocol.*;
import edu.cs4b.gamecontroller.Game;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GameController client
 *
 * The GameController connects to the router and subscribes to /game/* ,
 * allowing it to receive messages from any game channel
 *
 * For now, this only prints received messages so we can test routing
 * TODO: validate moves and send messages back to the correct channels
 **/

public class GameControllerMain {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 4000;
    private static final String ALL_GAME_CHANNELS = "/game/*";
    private static final String PLAYERS = "/players";
    private static final String PLAYER_X = "X";
    private static final String PLAYER_O = "O";

    private static ConcurrentHashMap<String, Game> games = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        // Parse command-line arguments
        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--host" -> host = args[++i];
                case "--port" -> port = Integer.parseInt(args[++i]);
            }
        }

        RouterClient client = new RouterClient(host, port);
        
        try {
            client.connect();

            MessageListener listener = (channel, senderId, message) -> {
                System.out.println();
                System.out.println("[GameController] message received");
                System.out.println("Channel: " + channel);
                System.out.println("Sender: " + senderId);
                System.out.println("Message type: " + message.getClass().getSimpleName());

                if (message instanceof JoinMessage join) {
                    System.out.println("Player joined: " + join.getPlayerName());
                } else if (message instanceof MakeMoveMessage move) {
                    makeMoveMessageReceived(client, channel, move);
                } else if (message instanceof TextMessage text) {
                    System.out.println("Text: " + text.getText());
                } else if (message instanceof CreateGameMessage game) {
                    createGame(game, client, channel);
                } else if (message instanceof JoinGameMessage joinGame) {
                    handleJoinGame(client, channel, joinGame);
                }else {
                    System.out.println("Message: " + message);
                }

                System.out.println();
            };

            // Subscribe to all game channels
            client.subscribe(ALL_GAME_CHANNELS, listener);

            System.out.println("GameController connected.");
            System.out.println("Subscribed to " + ALL_GAME_CHANNELS);
            System.out.println("Waiting for game messages...");

            // Keep the controller running so it can keep listening for messages
            while (true) {
                Thread.sleep(1000);
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("GameController stopped.");
        }
    }

    /*
        Private helper for handleJoinGame()
    */
    private static Game createGame(CreateGameMessage message, RouterClient client, String channel) {
        try {
            Game game = new Game(message.getGameId(), message.getPlayerId());
            games.put(message.getGameId(), game);
            client.send(PLAYERS + "/" + message.getPlayerId(), new GameCreatedMessage(message.getGameId(), channel, "online"));

            System.out.println("Game created: " + message.getGameId() + " by " + message.getPlayerId());

            return game;
        } catch (IOException e) {
            System.err.println("Error while creating game");
            e.printStackTrace();
            return null;
        }
    }

    /*
        Handler for when a player sends a JoinGameMessage to join a game.
            - search for game, if it doesnt exist, create a new one
            - assign symbol (X first, then O), and add player to the game
            - Notify everyone in the game channel that someone joined
            - If game is full, send a StartGameMessage to the router
    */
    private static void handleJoinGame(RouterClient client, String channel, JoinGameMessage join) {

        String playerId = join.getPlayerId();
        String gameId = join.getGameId();

        try {

            // search for existing game, if it doesnt exist, create a new one
            Game game = games.get(gameId);
            if (game == null) game = createGame(new CreateGameMessage(join.getPlayerId(), join.getGameId()), client, channel);
            if (game == null) throw new IOException("Error while creating game");
            // Assign symbol (X first, then O),
            // and add player to the game
            // TODO: Game() already creates the first player as "X", second player should be "O"; this overwrites the first player's symbol to be "O".

            // String symbol = (game.getPlayers().size() == 0) ? "X" : "O";
            if (!game.getPlayers().containsKey(join.getPlayerId())) {
                String symbol = "O";
                game.addPlayer(playerId, symbol);
            }
    
            // Notify everyone in the game channel
            client.send(channel,
                new JoinMessage(playerId)
            );

            // Start game if full (by the way StartGameMessage only needs gameID as its attribute)
            if (game.getPlayers().size() == 2) {
                client.send(channel, new StartGameMessage(gameId));
            }

        } catch (IOException e) {
            System.err.println("Join game error: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private static void makeMoveMessageReceived(RouterClient client, String channel, MakeMoveMessage move) {
        try {
            // grab the game and the symbol corresponding to the move made
            Game game = games.get(move.getGameId());

            // if the game is null, no game exists for that game ID
            if (game == null) {
                sendMoveRejection(client, move, "Game does not exist for game ID " + move.getGameId() + ".");
                return;
            }

            // get a list of players from the game
            String symbol = game.getPlayers().get(move.getPlayerId());
            List<String> players = new ArrayList<>(game.getPlayers().keySet());

            // check if the given move's player is in the list of players
            if (!players.contains(move.getPlayerId())) {
                sendMoveRejection(client, move, "You are not a player in this game.");
                return;
            }

            // grab the player ID of the other player
            String nextPlayerId = "";
            // just gets the first instance of a different player ID from the one who made the move
            for (String player: players) {
                if (!player.equals(move.getPlayerId())) {
                    nextPlayerId = player;
                    break;
                }
            }

            // proceed only if the following conditions are met:
            // - the player is in the list of the game's players
            // - there is a "next Player" (second player)
            // - the player symbol is either an X or O
            // - move is valid (an empty spot on the board)
            // - it's the move's player's turn
            String reason = "";
            boolean success = false;
            if (!players.contains(move.getPlayerId()))
                reason = "You are not a player in this game.";
            else if (nextPlayerId.isEmpty())
                reason = "No second player.";
            else if (!symbol.equals(PLAYER_X) && !symbol.equals(PLAYER_O))
                reason = "Invalid player symbol.";
            else if (!game.getCurrentTurn().equals(move.getPlayerId()))
                reason = "Not currently your turn. Current player's turn: " + game.getCurrentTurn();
            else if (!checkIfMoveValid(move.getGameId(), move.getRow(), move.getColumn()))
                reason = "Invalid move at " + move.getRow() + move.getColumn() + ".";
            else
                success = true;

            if (success) {
                // if it's valid, update the game board
                updateGameBoard(move.getGameId(), move.getRow(), move.getColumn(), symbol);
                // check the state of the board
                GameStatus boardStatus = checkGameEnd(move.getGameId());

                // checks if the game is ongoing AND does a compare & set for the next turn's player ID
                if ((boardStatus == GameStatus.GAME_ONGOING)) {
                    // send the MoveAcceptedMessage to the players
                    game.setCurrentTurn(nextPlayerId);
                    client.send(channel + move.getGameId(), new MoveAcceptedMessage(
                                                move.getGameId(), 
                                                move.getPlayerId(), 
                                                move.getRow(), 
                                                move.getColumn(), 
                                                game.getBoard(),
                                                nextPlayerId, 
                                                boardStatus));
                } else if (boardStatus == GameStatus.INVALID_STATUS){
                    sendMoveRejection(client, move, "Game status is currently invalid.");
                } else {    
                    // TODO: proceed with win/lose/draw
                    winningMessages(client, boardStatus, move.getGameId());
                }

            // if any of the previous checks fail, the move is invalid and needs to be rejected
            } else {
                sendMoveRejection(client, move, reason);
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendMoveRejection(RouterClient client, MakeMoveMessage message, String reason) throws IOException {
        client.send(PLAYERS + "/" + message.getPlayerId(), new MoveRejectedMessage(
                                            message.getGameId(), 
                                            message.getPlayerId(),
                                            message.getRow(), 
                                            message.getColumn(), 
                                            reason)); 
    }

    private static boolean checkIfMoveValid(String gameId, int row, int column) {
        Game game = games.get(gameId);
        boolean isValid = false;
        if (game != null) {
            isValid = game.getValueAtPosition(row, column) == 0;
        }
        return isValid;
    }

    private static void updateGameBoard(String gameId, int row, int column, String symbol) {
        Game game = games.get(gameId);
        if (game != null) {
            game.updateBoard(row, column, symbol);
        } else {
            System.err.println("Game " + gameId + " does not exist");
        }
    }

    private static GameStatus checkGameEnd(String gameId) {
        Game game = games.get(gameId);
        if (game != null) {
            int status = game.checkWinner();
            if (status == 0) { // even if status is 0 (ongoing), need to check if there's a tie
                if (!game.getBoard().contains(Integer.valueOf(0)))
                    // if no 0 was found after no winner is found, the game is a tie
                    return GameStatus.TIE_GAME;
                else return GameStatus.GAME_ONGOING;
            } else if (status == 1) {
                return GameStatus.PLAYER_X_WIN;
            } else return GameStatus.PLAYER_O_WIN;
        } else return GameStatus.INVALID_STATUS;
    }


    // Game End flow:

    // Handle cleanup of the game
    //  - Delete the game
    //  - Unsubscribe players from the game

    // winningMessages function
    // Sends out game draw, won, over messages
    private static void winningMessages(RouterClient client, GameStatus status, String gameId) {
        // Find ID of Player O
        Optional<String> findO = games.get(gameId).getPlayers().entrySet().stream().filter(entry->"O".equals(entry.getValue())).map(ConcurrentHashMap.Entry::getKey).findFirst();
        String playerO = findO.get();

        // Find ID of Player X
        Optional<String> findX = games.get(gameId).getPlayers().entrySet().stream().filter(entry->"X".equals(entry.getValue())).map(ConcurrentHashMap.Entry::getKey).findFirst();
        String playerX = findX.get();

        // Check if GameStatus a draw
        if (status == GameStatus.TIE_GAME) {

            // Send GameDrawMessage to both players
            try {
                client.send(PLAYERS + "/" + playerX, new GameDrawMessage(gameId, ""));
            } catch (IOException e) {
                System.out.println("ERROR: Failed to send GameDrawMessage to Player X!");
            }
            try {
                client.send(PLAYERS + "/" + playerO, new GameDrawMessage(gameId, ""));
            } catch (IOException e) {
                System.out.println("ERROR: Failed to send GameDrawMessage to Player O!");
            }

                // Check if GameStatus a Player X win
        } else if (status == GameStatus.PLAYER_X_WIN) {

            // Send GameWonMessage to Player X
            try {
                client.send(PLAYERS + "/" + playerX, new GameWonMessage(gameId, "", "", ""));
            } catch (IOException e) {
                System.out.println("ERROR: Failed to send GameWonMessage to Player X!");
            }

            // Send GameLostMessage to Player O
            try {
                client.send(PLAYERS + "/" + playerO, new GameOverMessage(gameId, "", ""));
            } catch (IOException e) {
                System.out.println("ERROR: Failed to send GameOverMessage to Player O!");
            }
            // Check if GameStatus a Player O win
        } else if (status == GameStatus.PLAYER_O_WIN) {

            // Send GameWonMessage to Player O
            try {
                client.send(PLAYERS + "/" + playerO, new GameWonMessage(gameId, "", "", ""));
            } catch (IOException e) {
                System.out.println("ERROR: Failed to send GameWonMessage to Player O!");
            }

            // Send GameLostMessage to Player O
            try {
                client.send(PLAYERS + "/" + playerX, new GameOverMessage(gameId, "", ""));
            } catch (IOException e) {
                System.out.println("ERROR: Failed to send GameOverMessage to Player X!");
            }
        }

        // Remove game from concurrent hash map
        games.remove(gameId);
        return;
    }
}
