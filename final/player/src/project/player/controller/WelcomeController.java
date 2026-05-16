package project.player.controller;

import java.util.UUID;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import project.player.TicTacToe;
import project.player.controller.SingletonData.UsernameData;


public class WelcomeController {
        
    UsernameData usernameData = UsernameData.getInstance(); //store username data through here

    @FXML
    private Label serverStatusLabel;

    @FXML
    private TextField usernameField;

    @FXML
    public void initialize() {

        serverStatusLabel.setText("Enter a username to connect.");

    }

    @FXML
    private void handleStart() {
        String username = usernameField.getText().trim();


        //input check username
        if (username.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Please enter a valid username.");
            alert.show();
            return;
        }

        //try to connect to router
        try {
            TicTacToe.connectRouter(username);

            //store username in singleton data class to pass data and the unique ID
            usernameData.setUsername(username); 
            usernameData.setPlayerId(
                UUID.randomUUID().toString()
            );


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