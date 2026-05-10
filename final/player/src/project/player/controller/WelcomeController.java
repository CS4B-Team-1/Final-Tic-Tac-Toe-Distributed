package project.player.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import project.player.TicTacToe;

public class WelcomeController {

    @FXML
    private Label serverStatusLabel;

    @FXML
    private TextField usernameField;

    @FXML
    public void initialize() {

  
        // Router implementation here

        boolean connected = true;

        if (connected) {
            serverStatusLabel.setText("Server connection successful.");
        } else {
            serverStatusLabel.setText(
                    "Server connection failed. Please try again."
            );
        }
    }

    @FXML
    private void handleStart() {

        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Please enter a valid username.");
            alert.show();
            return;
        }

        try {
            TicTacToe.switchScene("LobbyScene.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
