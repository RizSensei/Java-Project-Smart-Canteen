package net;

import java.io.*;
import java.net.Socket;

public class ClientConnection {

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

    public ClientConnection(MessageListener listener) {
        this.listener = listener;
    }

    /** Connect and send an optional registration command (e.g. REGISTER_STAFF). */
    public void connect(String registrationCommand) throws IOException {
        socket = new Socket(HOST, PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        if (registrationCommand != null) {
            out.println(registrationCommand);
        }

        Thread reader = new Thread(this::readLoop, "client-listener");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                listener.onMessage(line);
            }
        } catch (IOException e) {
            if (running) listener.onMessage("ERROR:" + e.getMessage());
        }
    }

    public synchronized void send(String command) {
        if (out != null) out.println(command);
    }

    public void close() {
        running = false;
        try {
            if (out != null) out.println("QUIT");
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}