package ch.heigvd.dai.retrivium.client;

import ch.heigvd.dai.retrivium.server.ServerMessage;
import ch.heigvd.dai.retrivium.utils.FileUtils;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** TCP client that uses REPL pattern to interact with user */
public class TcpReplClient {
    private final String serverIP;
    private final int port;
    private final char lineFeed;

    /**
     * Constructs {@code TcpReplClient}
     *
     * @param serverIP of the server
     * @param port of the server
     * @param lineFeed parameter(s) use with the command
     */
    public TcpReplClient(String serverIP, int port, char lineFeed) {
        this.serverIP = serverIP;
        this.port = port;
        this.lineFeed = lineFeed;
    }

    /** Displays help message in {@link System#out} */
    private void help() {
        String[] helpMessages =
                new String[] {
                    "- List all the files currently presented and indexed on the server",
                    "<k> <query> - Find top k relevant files to the given query",
                    "<filename> - Download file from server",
                    "<filename> <file> - Upload file to the server",
                    "- Close the connection to the server",
                    "- Display this help message"
                };
        ClientMessage[] cmdMessages =
                new ClientMessage[] {
                    ClientMessage.LIST, ClientMessage.QUERY, ClientMessage.SHOW,
                    ClientMessage.UPLOAD, ClientMessage.QUIT, ClientMessage.HELP
                };

        System.out.println("Usage:");

        for (int i = 0; i < helpMessages.length; i++) {
            System.out.printf("  %s %s%n", cmdMessages[i], helpMessages[i]);
        }
    }

    /**
     * Connects to the server defined by {@link TcpReplClient#serverIP} and {@link
     * TcpReplClient#port}
     *
     * @throws RuntimeException if client cannot connect to the server
     */
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
                String[] userInputParts = userInput.split(" ", 2);

                if (userInput.isEmpty()) {
                    continue;
                }

                try {
                    ClientMessage command = ClientMessage.valueOf(userInputParts[0].toUpperCase());

                    // Prepare request
                    String request = null;

                    switch (command) {
                        case LIST -> {
                            request = ClientMessage.LIST.name();
                        }
                        case QUERY -> {
                            request = ClientMessage.QUERY + " " + userInputParts[1];
                        }
                        case SHOW -> {
                            request = ClientMessage.SHOW + " " + userInputParts[1];
                        }
                        case UPLOAD -> {
                            String filename = userInputParts[1];

                            String content;
                            try {
                                content = FileUtils.readFile(new File(filename));
                                request = ClientMessage.UPLOAD + " " + filename + " " + content;
                            } catch (IOException e) {
                                System.out.println("[Client] Cannot read a file : " + e);
                                throw e;
                            }
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

                // Handle response from server
                switch (message) {
                    case FILES -> {
                        // As we know from the server implementation, the message is always the
                        // second part
                        String[] docs = serverResponseParts[1].split(" ");
                        System.out.println(
                                "There are " + docs.length + " documents presented on the server:");
                        for (String docName : docs) {
                            System.out.println(docName);
                        }
                    }
                    case NOTHING_INDEXED -> {
                        System.out.println("Sever has no documents to search through");
                    }
                    case RELEVANT -> {
                        String[] docs = serverResponseParts[1].split(" ");
                        System.out.println(
                                "There is "
                                        + docs.length
                                        + " relevant documents to your query (starting from most"
                                        + " relevant):");
                        for (String docName : docs) {
                            System.out.println(docName);
                        }
                    }
                    case NOTHING_RELEVANT -> {
                        System.out.println("There is no relevant documents to your query");
                    }
                    case CONTENT -> {
                        String doc = serverResponseParts[1];
                        System.out.println("Demanded document :");
                        System.out.println(doc);
                        // TODO:
                        // What if document is very long
                        // so, we should split it to the multiple lines
                        // and only then display it
                    }
                    case FILE_DOESNT_EXIST -> {
                        System.out.println("Server does not has the demanded document");
                    }
                    case UPLOADED -> {
                        System.out.println("Uploaded " + serverResponseParts[1]);
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
