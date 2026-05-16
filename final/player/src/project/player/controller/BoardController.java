package project.player.controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import project.client.MessageListener;
import project.client.RouterClient;
import project.player.handler.RouterHandler;
import project.protocol.GameStatus;
import project.protocol.Move;
import project.protocol.MoveAcceptedMessage;
import project.protocol.MoveRejectedMessage;

public class BoardController {

    // TODO: rework to not be default
    private String playerSymbol = this.PLAYER_X;
    private String currentTurn = this.PLAYER_X;
    private String playerId;
    private String gameId;

    private String clientGameID;
    private String clientPlayerID;

    public BoardController(String cGameID, String cPlayerID) {
        this.clientGameID = cGameID;
        this.clientPlayerID = cPlayerID;
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

    private final String PLAYER_X = "X";
    private final String PLAYER_O = "O";
    private final String GAME_CHANNEL = "/game/";

    
    //Constructor
    public BoardController() {
        try {

            MessageListener listener = (channel, senderId, message) -> {
                System.out.println("[" + senderId + "] message received");
                System.out.println("Channel: " + channel);
                System.out.println("Sender: " + senderId);
                System.out.println("Message type: " + message.getClass().getSimpleName());

                if (message instanceof MoveAcceptedMessage move) {
                    handleMoveAccepted(move);
                } else if (message instanceof MoveRejectedMessage move) {
                    // TODO: update board with error message
                    Alert alert = new Alert(AlertType.WARNING);
                    alert.setContentText(move.getReason());
                    alert.show();
                } else {
                    System.err.println("Undefined message for BoardController: " + message);
                }
                System.out.println();
            };

            RouterHandler.getRouterClient().subscribe(GAME_CHANNEL + this.gameId, listener);

        } catch (IOException e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setContentText("BoardController client could not connect to the server");
            alert.setOnCloseRequest((event) -> {
                // TODO:
                handleGameExit();
            });
            alert.show();
            e.printStackTrace();
        }
    }

    private void handleMoveAccepted(MoveAcceptedMessage message) {

        int index = Move.toIndex(message.getRow(), message.getCol());
        String symbol = "";
        this.currentTurn = message.getPlayerId();

        if (this.currentTurn != this.playerId) {
            if (this.playerSymbol == PLAYER_X) {
                symbol = PLAYER_O;
            } else {
                symbol = PLAYER_X;
            }
        } else symbol = this.playerSymbol;

        updateGUI(index, symbol);

        if (message.getGameStatus() == GameStatus.GAME_ONGOING) {
            // TODO: update the Current Turn Label
            this.currentTurn = message.getNextTurn();
            // strip UUID from playerID when displaying?
            System.out.println("Now " + message.getNextTurn() + "'s turn.");
        } else if (message.getGameStatus() == GameStatus.INVALID_STATUS) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setContentText("Invalid game state, exit current game");
            alert.setOnCloseRequest((event) -> {
                // TODO:
                handleGameExit();
            });
            alert.show();
        } else {
            // TODO: display winner/tie
        }
    }

    private void handleGameExit() {
        // TODO: handle game exit -> unsubscribe, send delete game message to GameController
    }

    //Updates the GUI and board after the computer has made a move.
    private void updateGUI(int index, String symbol) {
            Color color;

            if (symbol == PLAYER_X){
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
        MouseButton button = event.getButton();
        Button boardButton = (Button)event.getSource();
        String buttonID = boardButton.getId();
        int buttonMove = 0;
        if (button.compareTo(MouseButton.PRIMARY) == 0) {
            buttonMove = 1;
            if (boardButton.getText().isEmpty()) {
                //numActiveTiles++;
            }
            boardButton.setText(PLAYER_X);
            boardButton.setTextFill(Color.RED);
        }
        else if (button.compareTo(MouseButton.SECONDARY) == 0){
            buttonMove = -1;
            if (boardButton.getText().isEmpty()) {
                //numActiveTiles++;
            }
            boardButton.setText(PLAYER_O);
            boardButton.setTextFill(Color.BLUE);
        } else {
            System.out.println("unknown button clicked");
        }
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
