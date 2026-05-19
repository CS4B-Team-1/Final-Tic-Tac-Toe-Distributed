package project.player.controller;

import java.io.IOException;

import javax.lang.model.util.AbstractAnnotationValueVisitorPreview;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import project.client.MessageListener;
import project.player.handler.RouterHandler;
import project.player.handler.SceneHandler;
import project.protocol.GameStatus;
import project.protocol.LeaveGameMessage;
import project.protocol.MakeMoveMessage;
import project.protocol.Move;
import project.protocol.MoveAcceptedMessage;
import project.protocol.MoveRejectedMessage;
import project.protocol.StartGameMessage;

public class BoardController {

    private String playerSymbol = "";
    private String currentTurn = "";

    private final String PLAYER_X = "X";
    private final String PLAYER_O = "O";
    private final String GAME_CHANNEL = "/game/";
    private final String PLAYER_CHANNEL = "/players/";

    private String clientGameID;
    private String clientPlayerID;

    public void setGameId(String gameId) {
        this.clientGameID = gameId;
    }

    public void setPlayerId(String playerId) {
        this.clientPlayerID = playerId;
    }

    @FXML
    private Button topLeft;
    @FXML
    private Button topCenter;
    @FXML
    private Button topRight;
    @FXML
    private Button middleLeft;
    @FXML
    private Button middleCenter;
    @FXML
    private Button middleRight;
    @FXML
    private Button bottomLeft;
    @FXML
    private Button bottomCenter;
    @FXML
    private Button bottomRight;
    @FXML
    private Label turnLabel;
    @FXML
    private Button leaveButton;
  
    //Constructor
    public BoardController() {}

    // must be called AFTER clientGameID and clientPlayerID have been populated
    // otherwise will subscribe to a null  channel
    public void createGame(String clientGameId, String clientPlayerId, StartGameMessage startGameMessage) {
        try {
            this.clientGameID = clientGameId;
            this.clientPlayerID = clientPlayerId;
            this.playerSymbol = startGameMessage.getSymbol();
            this.currentTurn = startGameMessage.getStartingPlayerId();

            // creates a listener for the specific game's channel for the player
            MessageListener gameListener = (channel, senderId, message) -> {
                System.out.println("[" + senderId + "] message received");
                System.out.println("Channel: " + channel);
                System.out.println("Sender: " + senderId);
                System.out.println("Message type: " + message.getClass().getSimpleName());

                if (message instanceof MoveAcceptedMessage move) {
                    Platform.runLater(() -> {
                        handleMoveAccepted(move);
                    });
                } else if (message instanceof LeaveGameMessage leaveGame) {
                    exitGame(leaveGame);
                } else {
                    System.err.println("Undefined message for BoardController: " + message);
                }
                System.out.println();
            };

            // creates a separate listener to add to the player's channel to receive error message like MoveRejectedMessage
            MessageListener errorListener = (channel, senderId, message) -> {
                System.out.println("[" + senderId + "] message received");
                System.out.println("Channel: " + channel);
                System.out.println("Sender: " + senderId);
                System.out.println("Message type: " + message.getClass().getSimpleName());
                if (message instanceof MoveRejectedMessage move) {
                    Platform.runLater(() -> {
                        handleMoveRejected(move);
                    });
                }
                System.out.println();
            };

            RouterHandler.getRouterClient().subscribe(GAME_CHANNEL + this.clientGameID, gameListener);
            RouterHandler.getRouterClient().subscribe(PLAYER_CHANNEL + this.clientPlayerID, errorListener);

        } catch (IOException e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setContentText("BoardController client could not connect to the server");
            alert.setOnCloseRequest((event) -> { handleGameExit(); });
            alert.show();
            e.printStackTrace();
        }
    }

    private void handleMoveAccepted(MoveAcceptedMessage message) {

        int index = Move.toIndex(message.getRow(), message.getCol());
        String symbol = "";
        String nextSymbol = "";
        this.currentTurn = message.getPlayerId();

        if (!this.currentTurn.equals(this.clientPlayerID)) {
            if (this.playerSymbol.equals(PLAYER_X)) {
                symbol = PLAYER_O;
                nextSymbol = this.playerSymbol;
            } else {
                symbol = PLAYER_X;
                nextSymbol = this.playerSymbol;
            }
        } else {
            if (this.playerSymbol.equals(PLAYER_X)) {
                symbol = this.playerSymbol;
                nextSymbol = PLAYER_O;
            } else {
                symbol = this.playerSymbol;
                nextSymbol = PLAYER_X;
            }
        }

        updateGUI(index, symbol);
        GameStatus gameStatus = message.getGameStatus();

        if (message.getGameStatus() == GameStatus.GAME_ONGOING) {
            this.currentTurn = message.getNextTurn();
            this.turnLabel.setText(nextSymbol + "'s turn");
            // strip UUID from playerID when displaying?
            System.out.println("Now " + message.getNextTurn() + "'s turn.");
        } else if ((message.getGameStatus() == GameStatus.INVALID_STATUS) || (message.getGameStatus() == null)) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setContentText("Invalid game state, exit current game");
            alert.setOnCloseRequest((event) -> { handleGameExit(); });
            alert.show();
        } else {
            if (gameStatus == GameStatus.PLAYER_X_WIN) {
                // TODO: display winning screen, leave game
            } else if (gameStatus == GameStatus.PLAYER_O_WIN) {
                // TODO: display winning screen, leave game
            } else {
                // TODO: display tie game screen, leave game
            }
        }
    }

    private void handleMoveRejected(MoveRejectedMessage move) {
        String reasonMessage = "";
        MoveRejectedMessage.RejectReason reason = move.getReason();
        Alert alert = new Alert(AlertType.WARNING);

        switch (reason) {
            case INVALID_MOVE:
                reasonMessage = "Invalid move at row " + move.getRow() + " column " + move.getCol() + ".";
                break;
            case NOT_CURRENT_TURN:
                String symbol = "";
                if (this.playerSymbol.equals(PLAYER_X))
                    symbol = PLAYER_O;
                else
                    symbol = PLAYER_X;
                reasonMessage = "Not currently your turn. Currently " + symbol + "'s turn.";
                break;
            case INVALID_PLAYER:
                reasonMessage = "You are not a player in this game. Exiting game.";
                alert.setOnCloseRequest((event) -> { handleGameExit(); });
                break;
            case NO_SECOND_PLAYER:
                reasonMessage = "No second player detected. Exiting game.";
                alert.setOnCloseRequest((event) -> { handleGameExit(); });
                break;
            case INVALID_SYMBOL:
                reasonMessage = "Invalid player symbol detected. Exiting game.";
                alert.setOnCloseRequest((event) -> { handleGameExit(); });
                break;
            case INVALID_STATUS:
                reasonMessage = "Invalid game status. Exiting game.";
                alert.setOnCloseRequest((event) -> { handleGameExit(); });
                break;
            case NO_GAME_EXISTS:
                reasonMessage = "No game exists for game ID " + move.getGameId();
                alert.setOnCloseRequest((event) -> { handleGameExit(); });
                break;
            default:
                reasonMessage = "Invalid message received from server. Exiting game.";
                alert.setOnCloseRequest((event) -> { handleGameExit(); });
        }

        alert.setContentText(reasonMessage);
        alert.show();
    }

    // exitGame function
    // Exit game if other player has left the game
    private void exitGame(LeaveGameMessage leaveGame) {
        System.out.println("exitGame ID is: " + this.clientPlayerID);
        /*
        String reasonMessage = "Opponent has left the game.";
        Alert alert = new Alert(AlertType.);
        alert.setContentText(reasonMessage);
        alert.show();
*/
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Notification");
            alert.setHeaderText("Game Ended");
            alert.setContentText("Opponent has left the game.");
        });
        try {
            // Unsubscribe player from game
            RouterHandler.getRouterClient().unsubscribe(GAME_CHANNEL + this.clientGameID);
            /*
            // create new window for popup
            // create FXML loader object to load
            FXMLLoader loader = new FXMLLoader(getClass().getResource("..\\fxml\\LobbyScene.fxml"));
            // load FXML onto Scene
            Scene scene = new Scene(loader.load());
            SceneHandler.getStage().setScene(scene);
            */
/*
            // Bring player back to lobby
            System.out.println("Load in lobby");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("..\\fxml\\LobbyScene.fxml"));
            System.out.println("Set lobby");
            System.out.println("Current player ID is " + this.clientPlayerID);
            SceneHandler.getStage().setScene(new Scene(loader.load()));
            System.out.println("2Current player ID is " + this.clientPlayerID);
*/
        } catch (Exception e) {
            System.out.println("ERROR: Exit Game Failed!");
        }
        return;
    }

    // handleGameExit function
    // Exits player from game
    private void handleGameExit() {
        // Unsubscribe players from channel
        try {
            System.out.println("handleGameExit ID is: " + this.clientPlayerID);
            // Send LeaveGameMessage
            RouterHandler.getRouterClient().send(GAME_CHANNEL + this.clientGameID, new LeaveGameMessage(this.clientPlayerID, this.clientGameID));
            // Unsubscribe from the game
            RouterHandler.getRouterClient().unsubscribe(GAME_CHANNEL + this.clientGameID);
            // Bring player back to the lobby
            FXMLLoader loader = new FXMLLoader(getClass().getResource("..\\fxml\\LobbyScene.fxml"));
            SceneHandler.getStage().setScene(new Scene(loader.load()));
        } catch (Exception e) {
            System.out.println("ERROR: Handling Game Exit Failed!");
        }
        return;
    }



    //Updates the GUI and board after the computer has made a move.
    private void updateGUI(int index, String symbol) {
            Color color;

            if (symbol.equals(PLAYER_X)){
                color = Color.RED;
            }else{
                color = Color.BLUE;
            }

            //Update Gui
            switch(index) {
                case 0:
                    topLeft.setText(symbol);
                    topLeft.setTextFill(color);
                    topLeft.setMouseTransparent(true);
                    break;
                case 1:
                    topCenter.setText(symbol);
                    topCenter.setTextFill(color);
                    topCenter.setMouseTransparent(true);
                    break;
                case 2:
                    topRight.setText(symbol);
                    topRight.setTextFill(color);
                    topRight.setMouseTransparent(true);
                    break;
                case 3:
                    middleLeft.setText(symbol);
                    middleLeft.setTextFill(color);
                    middleLeft.setMouseTransparent(true);
                    break;
                case 4:
                    middleCenter.setText(symbol);
                    middleCenter.setTextFill(color);
                    middleCenter.setMouseTransparent(true);
                    break;
                case 5:
                    middleRight.setText(symbol);
                    middleRight.setTextFill(color);
                    middleRight.setMouseTransparent(true);
                    break;
                case 6:
                    bottomLeft.setText(symbol);
                    bottomLeft.setTextFill(color);
                    bottomLeft.setMouseTransparent(true);
                    break;
                case 7:
                    bottomCenter.setText(symbol);
                    bottomCenter.setTextFill(color);
                    bottomCenter.setMouseTransparent(true);
                    break;
                case 8:
                    bottomRight.setText(symbol);
                    bottomRight.setTextFill(color);
                    bottomRight.setMouseTransparent(true);
                    break;
                default:
                    System.err.println("Invalid move index");
            }
    }

    //Checks if left or right click happenes on a tile and if left click it puts an X and right click puts a O on the board for when there is 2 human players. 
    public void toggleBoardButton(MouseEvent event) {
        Button boardButton = (Button)event.getSource();
        String buttonID = boardButton.getId();

        switch(buttonID) {
            case "topLeft":
                try {
                    RouterHandler.getRouterClient().send(GAME_CHANNEL, new MakeMoveMessage(this.clientGameID, this.clientPlayerID, 0, 0));
                } catch (IOException e) {
                    System.out.println("ERROR: Failed to send MakeMoveMessage!");
                }
                break;
            case "topCenter":
                try {
                    RouterHandler.getRouterClient().send(GAME_CHANNEL, new MakeMoveMessage(this.clientGameID, this.clientPlayerID, 0, 1));
                } catch (IOException e) {
                    System.out.println("ERROR: Failed to send MakeMoveMessage!");
                }
                break;
            case "topRight":
                try {
                    RouterHandler.getRouterClient().send(GAME_CHANNEL, new MakeMoveMessage(this.clientGameID, this.clientPlayerID, 0, 2));
                } catch (IOException e) {
                    System.out.println("ERROR: Failed to send MakeMoveMessage!");
                }
                break;
            case "middleLeft":
                try {
                    RouterHandler.getRouterClient().send(GAME_CHANNEL, new MakeMoveMessage(this.clientGameID, this.clientPlayerID, 1, 0));
                } catch (IOException e) {
                    System.out.println("ERROR: Failed to send MakeMoveMessage!");
                }
                break;
            case "middleCenter":
                try {
                    RouterHandler.getRouterClient().send(GAME_CHANNEL, new MakeMoveMessage(this.clientGameID, this.clientPlayerID, 1, 1));
                } catch (IOException e) {
                    System.out.println("ERROR: Failed to send MakeMoveMessage!");
                }
                break;
            case "middleRight":
                try {
                    RouterHandler.getRouterClient().send(GAME_CHANNEL, new MakeMoveMessage(this.clientGameID, this.clientPlayerID, 1, 2));
                } catch (IOException e) {
                    System.out.println("ERROR: Failed to send MakeMoveMessage!");
                }
                break;
            case "bottomLeft":
                try {
                    RouterHandler.getRouterClient().send(GAME_CHANNEL, new MakeMoveMessage(this.clientGameID, this.clientPlayerID, 2, 0));
                } catch (IOException e) {
                    System.out.println("ERROR: Failed to send MakeMoveMessage!");
                }
                break;
            case "bottomCenter":
                try {
                    RouterHandler.getRouterClient().send(GAME_CHANNEL, new MakeMoveMessage(this.clientGameID, this.clientPlayerID, 2, 1));
                } catch (IOException e) {
                    System.out.println("ERROR: Failed to send MakeMoveMessage!");
                }
                break;
            case "bottomRight":
                try {
                    RouterHandler.getRouterClient().send(GAME_CHANNEL, new MakeMoveMessage(this.clientGameID, this.clientPlayerID, 2, 2));
                } catch (IOException e) {
                    System.out.println("ERROR: Failed to send MakeMoveMessage!");
                }
                break;
            default:
                break;
        }
    }

    public void handleLeaveButton() {
        handleGameExit();
    }

    //Checks if there is a winner and if there is a winner or a tie it will diplay a popup on weather either happened and when the popup is closed it resets the board.
    public void dispayWinnerCheck(){
        // TODO: update with actual winner, no longer a "check" (GameControllerMain does the checking)
        String outcomeString = "Display Winner";

        if (outcomeString != null) {
            try { 
                // create new window for popup
                Stage outcomePopup = new Stage();
                outcomePopup.initModality(Modality.APPLICATION_MODAL); // locks application, forces user to exit window before continuing
                // create FXML loader object to load
                FXMLLoader outcomeLoader = new FXMLLoader(getClass().getResource("..\\fxml\\OutcomePopup.fxml"));
                // load FXML onto Scene
                outcomePopup.setScene(new Scene(outcomeLoader.load()));

                // grab Controller instance to modify Label text
                OutcomePopupController outcomePopupController = outcomeLoader.getController();
                // set Label text to outcome
                outcomePopupController.setWinner(outcomeString);
                // sets up popup to reset board when closed
                // outcomePopup.setOnHidden(hiddenEvent -> resetBoard());
                // display popup
                outcomePopup.show();

                return; // if player wins, dont execute computer turn

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
