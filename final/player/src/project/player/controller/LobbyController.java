package project.player.controller;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Optional;

public class LobbyController {

    @FXML
    private TextField gameIdField;

    @FXML
    private Label statusLabel;

    @FXML
    private void handleJoin() {

        String gameId = gameIdField.getText().trim();

        if (gameId.isEmpty()) {
            statusLabel.setText("Please enter a Game ID.");
            return;
        }

        // Setup lobby search logic here

        boolean gameExists = false;

        if (gameExists) {

            statusLabel.setText("Joining game: " + gameId);

            // switch to board scene

        } else {

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Game Not Found");
            alert.setHeaderText(null);
            alert.setContentText(
                    "Game not found. Would you like to create a lobby?"
            );

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {

                statusLabel.setText("Creating lobby: " + gameId);

                // create lobby logic here
            }
        }
    }

    @FXML
    private void handleQuit() {


        //disconnect from the router (if necessary)

        Platform.exit();
    }
}
