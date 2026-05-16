package project.player;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import project.client.RouterClient;

public class TicTacToe extends Application {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 4000;

    private static Stage mainStage;
    private static RouterClient routerClient; //private static, but functionality is still accessible through public static methods
    private static String playerName;

    @Override
    public void start(Stage primaryStage) throws Exception {
        mainStage = primaryStage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/project/player/fxml/WelcomeScene.fxml"));
        Scene scene = new Scene(loader.load());

        mainStage.setTitle("Distributed Tic-Tac-Toe");
        mainStage.setScene(scene);
        mainStage.show();

        // Ensure we disconnect from the router when the application is closed
        mainStage.setOnCloseRequest(event -> disconnectRouter());
    }

    public static void main(String[] args) {
        launch(args);
    }

    /*
        The follow functions are all public static.
        On whatever part you are working on, if you import this file into your controller,
        you can call these functions to connect to the router,
        get the router client, get the player name,
        disconnect from the router, and switch scenes.
     */
    public static void connectRouter(String username) throws IOException {
        if (routerClient != null && routerClient.isConnected()) {
            return;
        }

        playerName = username;

        routerClient = new RouterClient(DEFAULT_HOST, DEFAULT_PORT);
        routerClient.connect();
    }

    public static RouterClient getRouterClient() {
        return routerClient;
    }

    public static String getPlayerName() {
        return playerName;
    }

    public static void disconnectRouter() {
        if (routerClient != null && routerClient.isConnected()) {
            routerClient.disconnect();
        }
    }

    public static void switchScene(String fxmlFile) throws Exception {
        FXMLLoader loader = new FXMLLoader(TicTacToe.class.getResource("/project/player/fxml/" + fxmlFile));
        Scene scene = new Scene(loader.load());

        mainStage.setScene(scene);
        mainStage.show();
    }
}