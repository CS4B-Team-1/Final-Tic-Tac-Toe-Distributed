package project.player.handler;

import java.io.IOException;

import project.client.RouterClient;

public class RouterHandler {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 4000;
    private static RouterClient router = null;

    public static void connectRouter() throws IOException {
        if (router != null && router.isConnected()) {
            return;
        }

        router = new RouterClient(DEFAULT_HOST, DEFAULT_PORT);
        router.connect();
    }

    public static RouterClient getRouterClient() {
        return router;
    }

    public static void disconnectRouter() {
        if (router != null && router.isConnected()) {
            router.disconnect();
        }
    }
}
