package controller;


import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class OutcomePopupController {

    @FXML
    private Label outcomeLabel;
    @FXML
    private Button outcomeButton;

    public void setWinner(String text) {
        outcomeLabel.setText(text);
    }
    public void onOutcomeButtonClicked(Event event) {
        ((Node) event.getSource()).getScene().getWindow().hide();
    }
}
