package project.player.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ResultController {

    @FXML
    private Label resultLabel;

    public void setResult(String result) {

        switch (result.toLowerCase()) {

            case "win":
                resultLabel.setText("You win!");
                break;

            case "loss":
                resultLabel.setText("You lose.");
                break;

            case "draw":
                resultLabel.setText("Tie!");
                break;

            default:
                resultLabel.setText("Game Finished");
        }
    }
}
