package project.gamecontroller;

import project.client.MessageListener;
import project.client.RouterClient;
import project.protocol.*;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GameController client
 *
 * The GameController connects to the router and subscribes to /game/* ,
 * allowing it to receive messages from any game channel
 **/

public class GameControllerMain {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 4000;
    private static final String ALL_GAME_CHANNELS = "/game/*";
    private static final String GAME_CHANNEL = "/game/";
    private static final String PLAYERS = "/players/";

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
                } else if (message instanceof CreateGameMessage game) {
                    createGame(game, client, channel);
                } else if (message instanceof JoinGameMessage joinGame) {
                    handleJoinGame(client, channel, joinGame);
                }else if (message instanceof LeaveGameMessage leaveGame) {
                    handleLeaveGame(client, leaveGame);
                }else{
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

    // handleLeaveGame() function
    // When one player leaves, unsubscribe the other player and the game
    private static void handleLeaveGame(RouterClient client, LeaveGameMessage leaveGame) {
        try {
            List<String> players = new ArrayList<>(games.get(leaveGame.getGameId()).getPlayers().keySet());
            // Unsubscribe the game
            //client.unsubscribe(leaveGame.getGameId());
            // Remove the game from the list of games
            games.remove(leaveGame.getGameId());
            
            // grab the player ID of the other player
            String otherPlayerId = "";
            // just gets the first instance of a different player ID from the one who made the move
            for (String player: players) {
                if (!player.equals(leaveGame.getPlayerId())) {
                    otherPlayerId = player;
                    break;
                }
            }
            client.send(PLAYERS + otherPlayerId, new LeaveGameMessage(otherPlayerId, leaveGame.getGameId()));

        } catch (Exception e) {
            System.out.println("ERROR: Failed to send LeaveGameMessage to other player!");
        }
    }


    /*
        Private helper for handleJoinGame()
    */
    private static Game createGame(CreateGameMessage message, RouterClient client, String channel) {
        try {

            Game game = new Game(message.getGameId(), message.getPlayerId());
            games.put(message.getGameId(), game);

            client.send(
                PLAYERS + message.getPlayerId(),
                new GameCreatedMessage(message.getGameId(), channel, "online")
            );

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
            - search for game, if it doesnt exist, send GameNotFoundMessage to the player and return.
            - assign symbol to second player "O" and add them to the game
            - Notify everyone in the game channel that someone joined
            - send a StartGameMessage to the router

        The only responses that should be sent back to the player are:
            - GameNotFoundMessage if the game ID they sent doesn't match any existing games
            - StartGameMessage if they successfully join a game (this will trigger the game to start and the first move to be made)
            
    */
    private static void handleJoinGame(RouterClient client, String channel, JoinGameMessage joinGameMessage) {

        String playerId = joinGameMessage.getPlayerId();
        String gameId = joinGameMessage.getGameId();

        try {

            // If game doesn't exist, send GameNotFoundMessage to the player and return.
            Game game = games.get(gameId);
            if (game == null) {
                
                client.send(PLAYERS + playerId, new GameNotFoundMessage(gameId));
                return;
            }

            if (!game.getPlayers().containsKey(joinGameMessage.getPlayerId())) {
                String symbol = "O";
                game.addPlayer(playerId, symbol);
            }

            //keep this wrapped in an if statement to protect against another thread sending a leave game message in the middle of another thread executing handleJoinGame.
            if (game.getPlayers().size() == 2) {

                List<String> playerIds = new ArrayList<>(game.getPlayers().keySet());

                String player1 = playerIds.get(0);
                String symbol1 = game.getPlayers().get(player1);

                String player2 = playerIds.get(1);
                String symbol2 = game.getPlayers().get(player2);

                //send message to both players
                client.send(
                    PLAYERS + player1,
                    new StartGameMessage(gameId, player1, symbol1)
                );

                client.send(
                    PLAYERS + player2,
                    new StartGameMessage(gameId, player1, symbol2)
                );

                System.out.println("Game " + gameId + " started with players: " + game.getPlayers());
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
                sendMoveRejection(client, move, MoveRejectedMessage.RejectReason.NO_GAME_EXISTS);
                return;
            }

            // get a list of players from the game
            String symbol = game.getPlayers().get(move.getPlayerId());
            List<String> players = new ArrayList<>(game.getPlayers().keySet());

            // check if the given move's player is in the list of players
            if (!players.contains(move.getPlayerId())) {
                sendMoveRejection(client, move, MoveRejectedMessage.RejectReason.INVALID_PLAYER);
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
            MoveRejectedMessage.RejectReason reason = null;
            boolean success = false;
            if (!players.contains(move.getPlayerId()))
                reason = MoveRejectedMessage.RejectReason.INVALID_PLAYER;
            else if (nextPlayerId.isEmpty())
                reason = MoveRejectedMessage.RejectReason.NO_SECOND_PLAYER;
            else if (!symbol.equals(PLAYER_X) && !symbol.equals(PLAYER_O))
                reason = MoveRejectedMessage.RejectReason.INVALID_SYMBOL;
            else if (!game.getCurrentTurn().equals(move.getPlayerId()))
                reason = MoveRejectedMessage.RejectReason.NOT_CURRENT_TURN;
            else if (!checkIfMoveValid(move.getGameId(), move.getRow(), move.getColumn()))
                reason = MoveRejectedMessage.RejectReason.INVALID_MOVE;
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
                    sendMoveRejection(client, move, MoveRejectedMessage.RejectReason.INVALID_STATUS);
                } else {
                    winningMessages(client, boardStatus, move.getGameId(), move, game.getBoard());
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

    private static void sendMoveRejection(RouterClient client, MakeMoveMessage message, MoveRejectedMessage.RejectReason reason) throws IOException {
        client.send(PLAYERS + message.getPlayerId(), new MoveRejectedMessage(
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
    private static void winningMessages(RouterClient client, GameStatus status, String gameId, MakeMoveMessage winningMakeMoveMessage, List<Integer> board) {
        // // Find ID of Player O
        // Optional<String> findO = games.get(gameId).getPlayers().entrySet().stream().filter(entry->"O".equals(entry.getValue())).map(ConcurrentHashMap.Entry::getKey).findFirst();
        // String playerO = findO.get();

        // // Find ID of Player X
        // Optional<String> findX = games.get(gameId).getPlayers().entrySet().stream().filter(entry->"X".equals(entry.getValue())).map(ConcurrentHashMap.Entry::getKey).findFirst();
        // String playerX = findX.get();

        // Check if GameStatus a draw
        if (status == GameStatus.TIE_GAME) {

            // Send Tie game status to both players in game channel
            try {
                client.send(GAME_CHANNEL + gameId, new MoveAcceptedMessage(gameId, winningMakeMoveMessage.getPlayerId(), winningMakeMoveMessage.getRow(), winningMakeMoveMessage.getColumn(), board, "", GameStatus.TIE_GAME));
            } catch (IOException e) {
                System.out.println("ERROR: Failed to send TIE_GAME status message to game " + gameId + " channel!");
            }
        } else if (status == GameStatus.PLAYER_X_WIN) {

            // Send Player X winning status to both players in the game channel
            try {
                client.send(GAME_CHANNEL + gameId, new MoveAcceptedMessage(gameId, winningMakeMoveMessage.getPlayerId(), winningMakeMoveMessage.getRow(), winningMakeMoveMessage.getColumn(), board, "", GameStatus.PLAYER_X_WIN));
            } catch (IOException e) {
                System.out.println("ERROR: Failed to send PLAYER_X_WIN status message to game " + gameId + " channel!");
            }
        } else if (status == GameStatus.PLAYER_O_WIN) {

            // Send Player O winning status to both players in the game channel
            try {
                client.send(GAME_CHANNEL + gameId, new MoveAcceptedMessage(gameId, winningMakeMoveMessage.getPlayerId(), winningMakeMoveMessage.getRow(), winningMakeMoveMessage.getColumn(), board, "", GameStatus.PLAYER_O_WIN));
            } catch (IOException e) {
                System.out.println("ERROR: Failed to send PLAYER_O_WIN status message to game " + gameId + " channel!");
            }
        }

        // Remove game from concurrent hash map
        games.remove(gameId);
        return;
    }
}
