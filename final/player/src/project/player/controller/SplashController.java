package controller;

import java.io.IOException;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

//Changes if the board will be one player or two players
public class SplashController {

    private Parent root;
    private Stage stage;
    private Scene scene;

    public void handleOnePlayer(Event event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("..\\TicTacToeBoard.fxml"));
        stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }
    public void handleTwoPlayer(Event event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("..\\TicTacToeBoard.fxml"));
        root = loader.load();
        BoardController boardController = loader.getController();
        boardController.setIsOnePlayerGame(false);
        // root = FXMLLoader.load(getClass().getResource("..\\TicTacToeBoard.fxml"));
        stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
