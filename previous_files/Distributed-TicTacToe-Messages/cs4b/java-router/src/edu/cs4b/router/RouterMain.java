package edu.cs4b.router;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entry point for the message router.
 *
 * Listens on a TCP port and spawns a new ClientHandler thread for each
 * incoming connection. The router is a "dumb relay" — it routes messages
 * to channel subscribers without understanding the message content.
 *
 * ARCHITECTURE: THREAD-PER-CLIENT
 *
 * This server uses the simplest possible multithreading model:
 *   - The main thread sits in an accept loop, waiting for new connections.
 *   - For each new connection, it creates a dedicated thread that handles
 *     that client for its entire lifetime.
 *
 * This works well for a small number of clients (classroom scale: ~10-30).
 * Each thread costs ~1MB of stack memory, and the OS can handle hundreds
 * of threads easily.
 *
 * For thousands of clients, you'd want a more advanced model (like
 * java.nio with Selectors, or a thread pool), but that's unnecessary
 * complexity for our use case.
 *
 * Usage:
 *   java edu.cs4b.router.RouterMain [port]
 *
 * Default port is 5000.
 */
public class RouterMain {

    private static final int DEFAULT_PORT = 4000;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }

        // Single ChannelManager shared across ALL client threads.
        // That's why it uses ConcurrentHashMap internally — see ChannelManager.java.
        ChannelManager channelManager = new ChannelManager();

        // AtomicInteger for generating unique client IDs.
        // "Atomic" means incrementAndGet() is thread-safe — even if two clients
        // connect at the exact same instant, they'll get different IDs.
        // A regular int++ would NOT be safe here (read-modify-write race condition).
        AtomicInteger clientCounter = new AtomicInteger(0);

        // try-with-resources: ServerSocket is AutoCloseable, so it will be
        // automatically closed when we exit the try block (e.g., on error).
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Router] Listening on port " + port);

            // ACCEPT LOOP — this runs forever (until the process is killed).
            // accept() blocks until a client connects, then returns a Socket
            // representing that specific connection.
            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientId = "client-" + clientCounter.incrementAndGet();

                // Create a handler and start it on a new thread.
                // The thread name is set to the clientId for easier debugging
                // (visible in thread dumps, log messages, etc.).
                ClientHandler handler = new ClientHandler(clientSocket, channelManager, clientId);
                Thread thread = new Thread(handler, clientId);

                // Daemon thread: the JVM will exit when only daemon threads remain.
                // Without this, the JVM would hang forever even after the main
                // thread finishes, because client threads would keep it alive.
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("[Router] Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
