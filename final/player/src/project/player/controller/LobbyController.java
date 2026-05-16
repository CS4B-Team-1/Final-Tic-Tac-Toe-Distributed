package project.player.controller;

import project.client.MessageListener;
import project.client.RouterClient;
import project.player.TicTacToe;
import project.player.controller.SingletonData.UsernameData;

import project.protocol.*;

import java.io.IOException;
import java.util.Optional;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class LobbyController {

    private final RouterClient client = TicTacToe.getRouterClient(); //acces router client through here
    private static final String LOBBY = "/lobby";
    private static final String PLAYERS = "/players/";
    private static final String channel = "/game/";

    UsernameData usernameData = UsernameData.getInstance(); //access username data through here

    MessageListener Lobbylistener = (channel, senderId, message) -> {

        System.out.println(">>> RECEIVED MESSAGE: " + message.getClass().getSimpleName());

        String prefix = "[" + channel + "] ";
        if (message instanceof GameCreatedMessage createdGame) {
            /*
                This is when we switch scenes to the game board.
            */
            //Note: BoardController.java needs to subscribe to the game channel on initialize().


            // handleGameCreated(createdGame);

        } else if (message instanceof GameNotFoundMessage gameNotFound) {
            handleGameNotFound(gameNotFound);
        } else{
            System.out.println(prefix + senderId + " sent: " + message); 
        }
    };

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

    @FXML
    private TextField gameIdField;

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {

        //try to subscribe to lobby, with stored username
        try {

            //subscribe to personal system messages channel and lobby channel
            client.subscribe(LOBBY, Lobbylistener);
            client.subscribe(PLAYERS + usernameData.getPlayerId(), Lobbylistener);

            System.out.println(usernameData.getUsername() + " joined: " + LOBBY);

        } catch (Exception e) {

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Failed");
            alert.setHeaderText(null);
            alert.setContentText(
                    "Failed to Subscribe to " + LOBBY + "."
            );
            alert.show();

            e.printStackTrace();

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

            JoinGameMessage joinGameMessage = new JoinGameMessage( usernameData.getPlayerId(), gameId );
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
}