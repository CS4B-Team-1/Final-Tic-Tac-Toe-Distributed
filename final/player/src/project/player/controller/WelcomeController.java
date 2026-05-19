package project.player.controller;

import java.util.UUID;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import project.player.controller.SingletonData.UsernameData;
import project.player.handler.RouterHandler;
import project.player.handler.SceneHandler;

public class WelcomeController {
        
    /*
        player Username - the name the player enters at the beginning of the program. Only used for display purposes in the GUI
        player ID - the unique ID assigned to the player by the server on connection. This is the identifier for all players in the message system

        Sender ID - see Envelope.java, the Player ID is the Sender ID.
    */
    private UsernameData usernameData = UsernameData.getInstance(); //store username data through here

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
            alert.initOwner(SceneHandler.getStage()); // centers the alert
            alert.show();
            return;
        }

        //try to connect to router
        try {
            RouterHandler.connectRouter();

            //store username & unique ID in singleton data class to pass data and the unique ID between controllers smoothly
            usernameData.setUsername(username); 
            usernameData.setPlayerId(
                UUID.randomUUID().toString()
            );

            SceneHandler.switchScene("LobbyScene.fxml");

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Failed");
            alert.setHeaderText(null);
            alert.setContentText(
                    "Could not connect to the router. Make sure the router is running."
            );
            alert.initOwner(SceneHandler.getStage()); // centers the alert
            alert.show();

            e.printStackTrace();
        }
    }
}