package project.player;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import project.player.handler.RouterHandler;
import project.player.handler.SceneHandler;

public class TicTacToe extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        SceneHandler.setStage(primaryStage);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/project/player/fxml/WelcomeScene.fxml"));
        Scene scene = new Scene(loader.load());

        SceneHandler.getStage().setTitle("Distributed Tic-Tac-Toe");
        SceneHandler.getStage().setScene(scene);
        SceneHandler.getStage().show();

        SceneHandler.getStage().setOnCloseRequest(event -> RouterHandler.disconnectRouter());
    }
}