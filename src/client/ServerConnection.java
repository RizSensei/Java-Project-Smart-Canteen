package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Opens a single connection to the CanteenServer.
 * Sends one command and reads one response.
 * Used by both StudentClient and StaffDashboard.
 */
public class ServerConnection {

    public static final String HOST = "localhost";
    public static final int PORT = 6000;

    /**
     * Send a command to the server and return its response.
     * Opens a fresh connection, sends one line, reads one line, closes.
     */
    public static String send(String command) throws IOException {
        try (
            Socket socket = new Socket(HOST, PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()))
        ) {
            out.println(command);
            return in.readLine();
        }
    }
}