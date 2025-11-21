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
                            case LIST -> {
                                System.out.println(
                                        "[Server] Received LIST command");
                                System.out.println("[Server] Sending all available files");

                                // TODO:
                                // check folder and collect all the real names

                                response = ServerMessage.FILES.name() + String.join(" ",
                                        new String[] {"file1.txt", "file2.txt", "file3.txt"});
                            }
                            case QUERY -> {
                                // TODO:
                                // check if topK > 0
                                // check if query is not empty

                                String[] payload = clientRequestParts[1].split(" ");
                                int topK = Integer.parseInt(payload[0]);
                                String query = payload[1];

                                // TODO:
                                // do the actual search

                                response = ServerMessage.RELEVANT.name() + String.join(" ",
                                        new String[] {"file2.txt", "file1.txt"});
                            }
                            case SHOW -> {
                                // TODO:
                                // check if file is indeed presented on the server
                                // read it and send

                                response = ServerMessage.CONTENT.name() + "a dog is the human's best friend and likes to play";
                            }
                            case ASK_UPLOAD -> {
                                // TODO:
                                // estimate if server has enought place to store file
                                // generate token
                                // store (filename, token) pair
                                // send response

                                response = ServerMessage.ALLOWED.name() + 1;
                            }
                            case UPLOAD -> {
                                String[] payload = clientRequestParts[1].split(" ", 2);
                                String token = payload[0];
                                String file = payload[1];

                                // TODO:
                                // check if (file, token) does exist and token is correct
                                // save file
                                // trigger re-indexing

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
