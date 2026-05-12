package project.player.handler;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import project.player.TicTacToe;

public class SceneHandler {

    private static Stage mainStage = null;

    public static void setStage(Stage stage) {
        mainStage = stage;
    }

    public static Stage getStage() {
        return mainStage;
    }

    public static void switchScene(String fxmlFile) throws Exception {
        FXMLLoader loader = new FXMLLoader(TicTacToe.class.getResource("/project/player/fxml/" + fxmlFile));
        Scene scene = new Scene(loader.load());

        mainStage.setScene(scene);
        mainStage.show();
    }
}
