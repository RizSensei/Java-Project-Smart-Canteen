package staff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Persistent socket connection for the staff client.
 * Registers itself as a staff, then listens for broadcast messages
 * (NEW_ORDER, ORDER_UPDATED) and forwards them to a callback.
 */
public class StaffConnection {

    public static final String HOST = "localhost";
    public static final int PORT = 6000;

    public interface MessageListener {
        void onMessage(String message);
    }

    private final MessageListener listener;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = true;

    public StaffConnection(MessageListener listener) {
        this.listener = listener;
    }

    /** Opens the socket, registers as staff, starts the listener thread. */
    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Identify ourselves so the server adds us to the broadcast list
        out.println("REGISTER_STAFF");

        // Background thread: read incoming broadcasts forever
        Thread reader = new Thread(this::readLoop, "staff-listener");
        reader.setDaemon(true); // doesn't block JVM exit
        reader.start();
    }

    private void readLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                listener.onMessage(line);
            }
        } catch (IOException e) {
            if (running) {
                listener.onMessage("ERROR:" + e.getMessage());
            }
        }
    }

    /** Send a command. Responses arrive asynchronously via the listener. */
    public synchronized void send(String command) {
        if (out != null)
            out.println(command);
    }

    /** Close the connection cleanly. */
    public void close() {
        running = false;
        try {
            if (out != null)
                out.println("QUIT");
            if (socket != null)
                socket.close();
        } catch (IOException ignored) {
        }
    }
}