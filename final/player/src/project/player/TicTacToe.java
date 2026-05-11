package project.player;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TicTacToe extends Application {
    private static Stage mainStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        mainStage = primaryStage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/project/player/fxml/WelcomeScene.fxml"));
        Scene scene = new Scene(loader.load());

        mainStage.setTitle("Distributed Tic-Tac-Toe");
        mainStage.setScene(scene);
        mainStage.show();
    }

    public static void switchScene(String fxmlFile) throws Exception {
        FXMLLoader loader = new FXMLLoader(TicTacToe.class.getResource("/project/player/fxml/" + fxmlFile));
        Scene scene = new Scene(loader.load());

        mainStage.setScene(scene);
        mainStage.show();
    }
    public static void main(String[] args){
        launch(args);
    }
}