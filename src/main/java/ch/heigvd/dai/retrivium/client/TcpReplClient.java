package ch.heigvd.dai.retrivium.client;

import ch.heigvd.dai.retrivium.server.ServerMessage;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpReplClient {
    private final String serverIP;
    private final int port;
    private final char lineFeed;

    public TcpReplClient(String serverIP, int port, char lineFeed) {
        this.serverIP = serverIP;
        this.port = port;
        this.lineFeed = lineFeed;
    }

    private void help() {
        System.out.println("Usage:");
        System.out.println("  " + ClientMessage.HELLO + " <your name> - Say hello with a name.");
        System.out.println(
                "  " + ClientMessage.HELLO_WITHOUT_NAME + " - Say hello without a name.");
        System.out.println(
                "  " + ClientMessage.INVALID + " - Send an invalid command to the server.");
        System.out.println("  " + ClientMessage.QUIT + " - Close the connection to the server.");
        System.out.println("  " + ClientMessage.HELP + " - Display this help message.");
    }

    public void launch() throws RuntimeException {

        System.out.println("[Client] Connecting to " + serverIP + ":" + port + "...");

        try (Socket socket = new Socket(serverIP, port);
                Reader reader =
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
                BufferedReader in = new BufferedReader(reader);
                Writer writer =
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
                BufferedWriter out = new BufferedWriter(writer);
                Reader systemInReader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
                BufferedReader bsir = new BufferedReader(systemInReader)) {

            System.out.println("[Client] Connected to " + serverIP + ":" + port);
            System.out.println();

            // Display help message
            help();

            // Run REPL until user quits
            while (!socket.isClosed()) {
                // Display prompt
                System.out.print("> ");

                // Read user input
                String userInput = bsir.readLine();

                try {
                    // Split user input to parse command (also known as message)
                    String[] userInputParts = userInput.split(" ", 2);
                    ClientMessage command = ClientMessage.valueOf(userInputParts[0].toUpperCase());

                    // Prepare request
                    String request = null;

                    switch (command) {
                        case HELLO -> {
                            String name = userInputParts[1];
                            request = ClientMessage.HELLO + " " + name;
                        }
                        case HELLO_WITHOUT_NAME -> {
                            request = ClientMessage.HELLO.name();
                        }
                        case INVALID -> {
                            request = ClientMessage.INVALID.name();
                        }
                        case QUIT -> {
                            socket.close();
                            continue;
                        }
                        case HELP -> {
                            help();
                            continue;
                        }
                    }

                    if (request != null) {
                        // Send request to server
                        out.write(request + lineFeed);
                        out.flush();
                    }

                } catch (Exception e) {
                    System.out.println("Invalid command. Please try again.");
                    continue;
                }

                // Read response from server and parse it
                String serverResponse = in.readLine();

                // If serverResponse is null, the server has disconnected
                if (serverResponse == null) {
                    socket.close();
                    continue;
                }

                // Split response to parse message (also known as command)
                String[] serverResponseParts = serverResponse.split(" ", 2);

                ServerMessage message = null;
                try {
                    message = ServerMessage.valueOf(serverResponseParts[0]);
                } catch (IllegalArgumentException e) {
                    // Do nothing
                }

                //                Handle response from server
                switch (message) {
                    case HI -> {
                        // As we know from the server implementation, the message is always the
                        // second part
                        String helloMessage = serverResponseParts[1];
                        System.out.println(helloMessage);
                    }

                    case INVALID -> {
                        if (serverResponseParts.length < 2) {
                            System.out.println("Invalid message. Please try again.");
                            break;
                        }

                        String invalidMessage = serverResponseParts[1];
                        System.out.println(invalidMessage);
                    }
                    case null, default ->
                            System.out.println("Invalid/unknown command sent by server, ignore.");
                }
            }

            System.out.println("[Client] Closing connection and quitting...");
        } catch (RuntimeException e) {
            System.out.println("[Client] Unable to configure socker : " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.out.println("[Client] Unable to connect : " + e.getMessage());
        }
    }
}
