package project.player.controller;
import java.util.Optional;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import project.player.TicTacToe;

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

        // TODO: Replace this with actual router/game controller logic later.
        boolean gameExists = true;

        if (gameExists) {
            statusLabel.setText("Joining game: " + gameId);

            try {
                TicTacToe.switchScene("TicTacToeBoard.fxml");
            } catch (Exception e) {
                e.printStackTrace();
                statusLabel.setText("Could not load board scene.");
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Game Not Found");
            alert.setHeaderText(null);
            alert.setContentText("Game not found. Would you like to create a lobby?");

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                statusLabel.setText("Creating lobby: " + gameId);

                try {
                    TicTacToe.switchScene("TicTacToeBoard.fxml");
                } catch (Exception e) {
                    e.printStackTrace();
                    statusLabel.setText("Could not load board scene.");
                }
            }
        }
    }

    @FXML
    private void handleQuit() {
        Platform.exit();
    }
}