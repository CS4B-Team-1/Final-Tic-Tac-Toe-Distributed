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

        serverStatusLabel.setText("Enter a username to connect.");
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
            TicTacToe.connectRouter(username);
            TicTacToe.switchScene("LobbyScene.fxml");
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Failed");
            alert.setHeaderText(null);
            alert.setContentText(
                    "Could not connect to the router. Make sure the router is running."
            );
            alert.show();

            e.printStackTrace();
        }
    }
}