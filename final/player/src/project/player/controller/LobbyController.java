package project.player.controller;

import project.client.MessageListener;
import project.client.RouterClient;
import project.player.TicTacToe;
import project.player.controller.SingletonData.UsernameData;
import project.player.handler.RouterHandler;
import project.player.handler.SceneHandler;
import project.protocol.*;

import java.io.IOException;
import java.util.Optional;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;

public class LobbyController {

    private final RouterClient client = RouterHandler.getRouterClient(); //acces router client through here
    private static final String LOBBY = "/lobby";
    private static final String PLAYERS = "/players/";
    private static final String channel = "/game/";

    /*
        player Username - the name the player enters at the beginning of the program. Only used for display purposes in the GUI
        player ID - the unique ID assigned to the player by the server on connection. This is the identifier for all players in the message system

        Sender ID - see Envelope.java, the Player ID is the Sender ID.
    */
    private UsernameData usernameData = UsernameData.getInstance(); //access username data through here

    // - must handle the case of sending leave game message when the player presses quit while waiting for another player to join.
    // - DONT SEND (or maybe send a LeaveGameMessage with  null attributes) a LeaveGameMessage before the user has created a game.
    private MessageListener Lobbylistener = (channel, senderId, message) -> {

        System.out.println(">>> RECEIVED MESSAGE: " + message.getClass().getSimpleName());

        String prefix = "[" + channel + "] ";
        if (message instanceof GameCreatedMessage createGameMessage) {
            //Note: BoardController.java needs to subscribe to the game channel on initialize().

            // when the game is successfully created, add a listener to the client to tell the server that the player has left the game if they close the window.
            handleGameCreated(createGameMessage);
        } else if (message instanceof GameNotFoundMessage gameNotFound) {

            handleGameNotFound(gameNotFound);
        } else if (message instanceof StartGameMessage startGameMessage) {

            System.out.println(prefix + senderId + " Games Started on Game ID:  " + startGameMessage.getGameId()); 
            handleGameStarted(startGameMessage);
        } else if (message instanceof GameFullMessage) {
            handleGameFull();
        } else{

            System.out.println(prefix + senderId + " sent: " + message); 
        }
    };

    @FXML
    private TextField gameIdField;

    @FXML
    private Label statusLabel;

    @FXML
    private Button joinButton;

    @FXML
    private Button quitButton;

    @FXML
    public void initialize() {

        //try to subscribe to lobby, with stored username
        try {

            //subscribe to personal system messages channel and lobby channel
            client.subscribe(LOBBY, Lobbylistener);
            client.subscribe(PLAYERS + usernameData.getPlayerId(), Lobbylistener);

            System.out.println(usernameData.getUsername() + " joined: " + LOBBY);

        } catch (Exception e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Connection Failed");
                alert.setHeaderText(null);
                alert.setContentText(
                        "Failed to Subscribe to " + LOBBY + "."
                );
                alert.show();

                e.printStackTrace();
            });
        }
    }

    @FXML
    private void handleJoin() {

        String gameId = gameIdField.getText().trim();

        if (gameId.isEmpty()) {

            statusLabel.setText(
                "Please enter a Game ID."
            );
            return;
        }

        try {

            JoinGameMessage joinGameMessage = new JoinGameMessage(usernameData.getPlayerId(), gameId );
            client.send(channel, joinGameMessage);

            statusLabel.setText(
                "Joining game: " + gameId + "..."
            );

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    @FXML
    private void handleQuit() {

        //disconnect from the router (if necessary)

        Platform.exit();
    }

    private void handleGameCreated(GameCreatedMessage createGameMessage) {
        SceneHandler.getStage().setOnCloseRequest(event -> {
            try {
                client.send(channel,
                    new LeaveGameMessage(usernameData.getPlayerId(), createGameMessage.getGameId())
                );
            } catch (IOException e) {
                System.out.println("Failed to send LeaveGameMessage on window close.");
                e.printStackTrace();
            }
        });

        // Change status label to inform player
        Platform.runLater(() -> {
            statusLabel.setText(
                "Game '" + createGameMessage.getGameId() + "' created.\nSearching for opponent..."
            );

            // Disable buttons to control flow
            // player must press the top right X (or alt+f4 etc.) which calls setOnCloseRequest().
            // - must handle the case of sending leave game message when the player presses quit while waiting for another player to join.
            // - DONT SEND (or maybe send a LeaveGameMessage with  null attributes) a LeaveGameMessage before the user has created a game.
            joinButton.setDisable(true);
            quitButton.setDisable(true);
        });
    }

    private void handleGameFull() {
        Platform.runLater(() -> {
            Alert gameFullAlert = new Alert(AlertType.WARNING);
            gameFullAlert.setContentText("Game is currently full. Please join other game");
            gameFullAlert.show();

            statusLabel.setText("");
        });
    }

    private void handleGameNotFound(GameNotFoundMessage gameNotFound) {

        Platform.runLater(() -> {

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

            alert.setTitle("Game Not Found");
            alert.setHeaderText(null);

            alert.setContentText(
                "Game with ID '" +
                gameNotFound.getGameId() +
                "' was not found.\nWould you like to create a new game with this ID?"
            );

            ButtonType yesButton = new ButtonType("Yes");
            ButtonType noButton = new ButtonType("No");

            alert.getButtonTypes().setAll(
                yesButton,
                noButton
            );

            Optional<ButtonType> userPrompt =
                alert.showAndWait();

            if (userPrompt.isPresent()
                    && userPrompt.get() == yesButton) {

                try {

                    CreateGameMessage createMessage =
                        new CreateGameMessage(
                            usernameData.getPlayerId(),
                            gameNotFound.getGameId()
                        );

                    client.send(channel, createMessage);

                } catch (IOException e) {

                    e.printStackTrace();
                }
            }
        });
    }

    private void handleGameStarted(StartGameMessage startGameMessage) {

        String filename = "/project/player/fxml/TicTacToeBoard.fxml";

        Platform.runLater(() -> {

            try {

                FXMLLoader loader = new FXMLLoader(getClass().getResource(filename));
                Parent root = loader.load();

                BoardController boardController = loader.getController();

                // set gameId and playerId for the BoardController
                // boardController.setGameId(startGameMessage.getGameId());
                // boardController.setPlayerId(usernameData.getPlayerId());
                boardController.createGame(startGameMessage.getGameId(), usernameData.getPlayerId(), startGameMessage);
                boardController.setLobbyScene(SceneHandler.getStage().getScene());

                joinButton.setDisable(false);
                quitButton.setDisable(false);
                statusLabel.setText("");

                // switch scene to game board
                Scene scene = new Scene(root);
                SceneHandler.getStage().setScene(scene);



            } catch (Exception e) { e.printStackTrace();}

        });
    }

    
}