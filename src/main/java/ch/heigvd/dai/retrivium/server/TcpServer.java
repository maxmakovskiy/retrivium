package ch.heigvd.dai.retrivium.server;

import ch.heigvd.dai.retrivium.client.ClientMessage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpServer {

    private final int port;
    private final char lineFeed;

    public TcpServer(int port, char lineFeed) {
        this.port = port;
        this.lineFeed = lineFeed;
    }

    public void launch() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Server] Listening on port " + port);

            while (!serverSocket.isClosed()) {
                try (Socket socket = serverSocket.accept();
                        Reader reader =
                                new InputStreamReader(
                                        socket.getInputStream(), StandardCharsets.UTF_8);
                        BufferedReader in = new BufferedReader(reader);
                        Writer writer =
                                new OutputStreamWriter(
                                        socket.getOutputStream(), StandardCharsets.UTF_8);
                        BufferedWriter out = new BufferedWriter(writer)) {
                    System.out.println(
                            "[Server] New client connected from "
                                    + socket.getInetAddress().getHostAddress()
                                    + ":"
                                    + socket.getPort());

                    // Run REPL until client disconnects
                    while (!socket.isClosed()) {
                        // Read response from client
                        String clientRequest = in.readLine();

                        // If clientRequest is null, the client has disconnected
                        // The server can close the connection and wait for a new client
                        if (clientRequest == null) {
                            socket.close();
                            continue;
                        }

                        // Split user input to parse command (also known as message)
                        String[] clientRequestParts = clientRequest.split(" ", 2);

                        ClientMessage command = null;
                        try {
                            command = ClientMessage.valueOf(clientRequestParts[0]);
                        } catch (Exception e) {
                            // Do nothing
                        }

                        // Prepare response
                        String response = null;

                        // Handle request from client
                        switch (command) {
                            case HELLO -> {
                                if (clientRequestParts.length < 2) {
                                    System.out.println(
                                            "[Server] "
                                                    + command
                                                    + " command received without <name> parameter."
                                                    + " Replying with "
                                                    + ServerMessage.INVALID
                                                    + ".");
                                    response =
                                            ServerMessage.INVALID
                                                    + " Missing <name> parameter. Please try"
                                                    + " again.";
                                    break;
                                }

                                String name = clientRequestParts[1];

                                System.out.println(
                                        "[Server] Received HELLO command with name: " + name);
                                System.out.println("[Server] Replying with HI command");

                                response = ServerMessage.HI + " Hi, " + name + "!";
                            }
                            case null, default -> {
                                System.out.println(
                                        "[Server] Unknown command sent by client, reply with "
                                                + ServerMessage.INVALID
                                                + ".");
                                response =
                                        ServerMessage.INVALID
                                                + " Unknown command. Please try again.";
                            }
                        }

                        // Send response to client
                        out.write(response + lineFeed);
                        out.flush();
                    }

                    System.out.println("[Server] Closing connection");
                } catch (IOException e) {
                    System.out.println("[Server] IO exception: " + e);
                }
            }
        } catch (IOException e) {
            System.out.println("[Server] error : " + e.getMessage());
            System.out.println("[Server] Terminating ...");
        }
    }
}
