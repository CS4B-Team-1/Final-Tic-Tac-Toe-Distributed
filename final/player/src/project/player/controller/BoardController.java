package project.player.controller;

import java.io.IOException;
import java.util.ArrayList;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import project.client.MessageListener;
import project.client.RouterClient;
import project.protocol.Message;
import project.protocol.Move;
import project.protocol.MoveAcceptedMessage;
import project.protocol.MoveRejectedMessage;

public class BoardController {

    private RouterClient client;

    // TODO: rework to not be default
    private String playerSymbol = this.PLAYER_X;
    private String currentTurn = this.PLAYER_X;
    private String playerId;

    ArrayList<Integer> boardGrid;
    boolean isOnePlayerGame = true;

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

    private int numActiveTiles;

    private final int GRID_SIZE = 9; 
    private final String PLAYER_X = "X";
    private final String PLAYER_O = "O";
    private final String GAME_CHANNEL = "/game/";

    
    //Constructor
    public BoardController() {
        // TODO: remove when client is able to be passed to BoardController
        this.client = new RouterClient("localhost", 4000);
        try {
            client.connect();

            MessageListener listener = (channel, senderId, message) -> {
                // TODO: remove possibly?
                System.out.println("[" + senderId + "] message received");
                System.out.println("Channel: " + channel);
                System.out.println("Sender: " + senderId);
                System.out.println("Message type: " + message.getClass().getSimpleName());

                if (message instanceof MoveAcceptedMessage move) {
                    handleMoveAccepted(move);
                } else if (message instanceof MoveRejectedMessage move) {
                    // TODO: update board with error message
                } else {
                    System.out.println("Message: " + message);
                }
                System.out.println();
            };

            //client.subscribe(GAME_CHANNEL + this.gameId, listener);

            boardGrid = new ArrayList<Integer>();
            // Set all boardGrid to empty
            for (int i = 0; i < GRID_SIZE; i++) {
                boardGrid.add(0);
            }
        } catch (IOException e) {
            System.err.println("BoardController client could not connect to the server");
            e.printStackTrace();
        }
    }

    private void handleMoveAccepted(MoveAcceptedMessage message) {
        // TODO: update the tile from the given move
        int index = Move.toIndex(message.getRow(), message.getCol());
        String symbol = "";

        if (message.getPlayerId() != this.playerId) {
            if (this.playerSymbol == PLAYER_X) {
                symbol = PLAYER_O;
            } else {
                symbol = PLAYER_X;
            }
        } else symbol = this.playerSymbol;

        updateGUI(index, symbol);

        // TODO: handle winning

        // TODO: update the Current Turn field
        System.out.println("Now " + message.getNextTurn() + "'s turn.");
    }

    //Updates the GUI and board after the computer has made a move.
    private void updateGUI(int index, String symbol) {
            Color color;
            int buttonMove = 0;

            if (symbol == PLAYER_X){
                color = Color.RED;
                buttonMove = 1;
            }else{
                color = Color.BLUE;
                buttonMove = -1;
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

            // Update Back End Board Grid
            switch(index) {
                case 0:
                    boardGrid.set(index, buttonMove);
                    break;
                case 1:
                    boardGrid.set(index, buttonMove);
                    break;
                case 2:
                    boardGrid.set(index, buttonMove);
                    break;
                case 3:
                    boardGrid.set(index, buttonMove);
                    break;
                case 4:
                    boardGrid.set(index, buttonMove);
                    break;
                case 5:
                    boardGrid.set(index, buttonMove);
                    break;
                case 6:
                    boardGrid.set(index, buttonMove);
                    break;
                case 7:
                    boardGrid.set(index, buttonMove);
                    break;
                case 8:
                    boardGrid.set(index, buttonMove);
                    break;
                default:
                    System.err.println("Invalid move index");
            }

            // Increment spaces used up for winnerCheck()
            numActiveTiles++; // Not checking if its an overwrite or not.

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
                numActiveTiles++;
            }
            boardButton.setText(PLAYER_X);
            boardButton.setTextFill(Color.RED);
        }
        else if (button.compareTo(MouseButton.SECONDARY) == 0){
            buttonMove = -1;
            if (boardButton.getText().isEmpty()) {
                numActiveTiles++;
            }
            boardButton.setText(PLAYER_O);
            boardButton.setTextFill(Color.BLUE);
        } else {
            System.out.println("unknown button clicked");
        }

        // Update boardGrid array
        // -1 = X   0 = empty   1 = O
        switch(buttonID) {
            case "topLeft":
                boardGrid.set(0, buttonMove);
                break;
            case "topCenter":
                boardGrid.set(1, buttonMove);
                break;
            case "topRight":
                boardGrid.set(2, buttonMove);
                break;
            case "middleLeft":
                boardGrid.set(3, buttonMove);
                break;
            case "middleCenter":
                boardGrid.set(4, buttonMove);
                break;
            case "middleRight":
                boardGrid.set(5, buttonMove);
                break;
            case "bottomLeft":
                boardGrid.set(6, buttonMove);
                break;
            case "bottomCenter":
                boardGrid.set(7, buttonMove);
                break;
            case "bottomRight":
                boardGrid.set(8, buttonMove);
                break;
            default:
                break;
        }
        System.out.println(boardGrid);      // Print out boardGrid to check states
        this.dispayWinnerCheck();

        if (this.isOnePlayerGame){
            this.computerTurn();
            this.dispayWinnerCheck();
        }

        //disable the button to disallow
        // overwriting moves (can be removed if you want to allow players to change their move before the game ends)
        boardButton.setMouseTransparent(true);
    }

    //Checks if there is a winner and if there is a winner or a tie it will diplay a popup on weather either happened and when the popup is closed it resets the board.
    public void dispayWinnerCheck(){
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
                outcomePopup.setOnHidden(hiddenEvent -> resetBoard());
                // display popup
                outcomePopup.show();

                return; // if player wins, dont execute computer turn

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /*
    //Checks if there is a winner or tie game and returns null if neither is the case.
    private String winnerCheck() {
        // check all rows
        String rowsResult = checkRows();
        // check all columns
        String columnsResult = checkColumns();
        // check both diagonals
        String diagonalsResult = checkDiagonals();

        String outcomeString = " wins";
        if (rowsResult != null)
            outcomeString = rowsResult + outcomeString;
        else if (columnsResult != null)
            outcomeString = columnsResult + outcomeString;
        else if (diagonalsResult != null)
            outcomeString = diagonalsResult + outcomeString;
        else if (numActiveTiles == (GRID_SIZE)) {
            outcomeString = "Tie game";
        } else 
            outcomeString = null;

        return outcomeString;
    } 

    private String checkRows() {
        // check first row
        String stringCheck = topLeft.getText();
        if (!stringCheck.isEmpty()) {
            if ((topCenter.getText().compareTo(stringCheck) == 0) && (topRight.getText().compareTo(stringCheck) == 0))
                return stringCheck;
        }
        // check middle row
        stringCheck = middleLeft.getText();
        if (!stringCheck.isEmpty()) {
            if ((middleCenter.getText().compareTo(stringCheck) == 0) && (middleRight.getText().compareTo(stringCheck) == 0))
                return stringCheck;
        }
        // check bottom row
        stringCheck = bottomLeft.getText();
        if (!stringCheck.isEmpty()) {
            if ((bottomCenter.getText().compareTo(stringCheck) == 0) && (bottomRight.getText().compareTo(stringCheck) == 0))
                return stringCheck;
        }
        // if all checks fail, return null
        return null;
    }

    private String checkColumns() {
        // check first column
        String stringCheck = topLeft.getText();
        if (!stringCheck.isEmpty()) {
            if ((middleLeft.getText().compareTo(stringCheck) == 0) && (bottomLeft.getText().compareTo(stringCheck) == 0))
                return stringCheck;
        }
        // check middle column
        stringCheck = topCenter.getText();
        if (!stringCheck.isEmpty()) {
            if ((middleCenter.getText().compareTo(stringCheck) == 0) && (bottomCenter.getText().compareTo(stringCheck) == 0))
                return stringCheck;
        }
        // check last column
        stringCheck = topRight.getText();
        if (!stringCheck.isEmpty()) {
            if ((middleRight.getText().compareTo(stringCheck) == 0) && (bottomRight.getText().compareTo(stringCheck) == 0))
                return stringCheck;
        }
        // if all checks fail, return null
        return null;
    }

    private String checkDiagonals() {
        // check top-left to bottom-right diagonal
        String stringCheck = topLeft.getText();
        if (!stringCheck.isEmpty()) {
            if ((middleCenter.getText().compareTo(stringCheck) == 0) && (bottomRight.getText().compareTo(stringCheck) == 0))
                return stringCheck;
        }
        // check bottom-left to top-right diagonal
        stringCheck = bottomLeft.getText();
        if (!stringCheck.isEmpty()) {
            if ((middleCenter.getText().compareTo(stringCheck) == 0) && (topRight.getText().compareTo(stringCheck) == 0))
                return stringCheck;
        }
        // if all checks fail, return null
        return null;
    }
    */

    // resets the board by setting all button text to blank and number of active tiles to 0.
    private void resetBoard() {

        /*
        topLeft.setText("");
        topCenter.setText("");
        topRight.setText("");
        middleLeft.setText("");
        middleCenter.setText("");
        middleRight.setText("");
        bottomLeft.setText("");
        bottomCenter.setText("");
        bottomRight.setText("");
        numActiveTiles = 0;

        topLeft.setMouseTransparent(false);
        topCenter.setMouseTransparent(false);  
        topRight.setMouseTransparent(false);
        middleLeft.setMouseTransparent(false);
        middleCenter.setMouseTransparent(false);
        middleRight.setMouseTransparent(false);
        bottomLeft.setMouseTransparent(false);
        bottomCenter.setMouseTransparent(false);
        bottomRight.setMouseTransparent(false);

        // Clear boardGrid
        for (int i = 0; i < GRID_SIZE; i++) {
            boardGrid.set(i, 0);
        }
        */
    }

    // TODO: remove
    public void computerTurn() {
        // create the computer
        int move = 0;
        this.updateGUI(move);

    }
}
