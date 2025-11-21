package ch.heigvd.dai.retrivium.server;

import ch.heigvd.dai.bm25.BM25;
import ch.heigvd.dai.retrivium.client.ClientMessage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class TcpServer {

    private final int port;
    private final char lineFeed;
    private final BM25 bm25;
    private final File targetDir;

    /**
     * Creates instance of TcpServer
     *
     * @param port
     * @param targetDir with files to search through
     * @lineFeed
     */
    public TcpServer(int port, File targetDir, char lineFeed) {
        this.port = port;
        this.lineFeed = lineFeed;
        this.targetDir = targetDir;

        bm25 = new BM25();
    }

    private void indexFiles() {
        File[] files = targetDir.listFiles();
        if (files == null) {
            System.out.println("You have provided an empty folder : " + targetDir.getPath());
            return;
        }

        ArrayList<String> docNames = new ArrayList<>();
        ArrayList<String> docs = new ArrayList<>();

        for (File file : files) {
            if (file.isFile()) {

                docNames.add(file.getName());

                StringBuilder content = new StringBuilder();

                try (FileReader reader = new FileReader(file.getPath(), StandardCharsets.UTF_8);
                        BufferedReader buf = new BufferedReader(reader); ) {
                    int c;
                    while ((c = buf.read()) != -1) {
                        content.append((char) c);
                    }
                } catch (IOException e) {

                    System.out.println("Impossible to read : " + file.getPath());
                    System.out.println("Skipping ...");

                    continue;
                }

                docs.add(content.toString());
            }
        }

        bm25.buildIndex(bm25.tokenize(docs), docNames);
    }

    public void launch() {
        System.out.println("[Server] Indexing documents : " + targetDir.getPath());
        indexFiles();

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
                                System.out.println("[Server] Received LIST command");
                                System.out.println("[Server] Sending all available files");

                                // TODO:
                                // check folder and collect all the real names
                                String[] docNames = new String[bm25.getIndex().getNumOfDocs()];
                                for (int i = 0; i < docNames.length; i++) {
                                    docNames[i] = bm25.getIndex().getDocumentName(i);
                                }

                                response = ServerMessage.FILES.name() + String.join(" ", docNames);
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

                                response =
                                        ServerMessage.RELEVANT.name()
                                                + String.join(
                                                        " ",
                                                        new String[] {"file2.txt", "file1.txt"});
                            }
                            case SHOW -> {
                                // TODO:
                                // check if file is indeed presented on the server
                                // read it and send

                                response =
                                        ServerMessage.CONTENT.name()
                                                + "a dog is the human's best friend and likes to"
                                                + " play";
                            }
                            case UPLOAD -> {
                                String[] payload = clientRequestParts[1].split(" ", 2);
                                String docName = payload[0];
                                String doc = payload[1];

                                File newFile = new File(targetDir.getParent(), docName);
                                Writer fileWriter = new FileWriter(newFile, StandardCharsets.UTF_8);
                                BufferedWriter bufDoc = new BufferedWriter(fileWriter);

                                bufDoc.write(doc);
                                bufDoc.flush();
                                bufDoc.close();

                                indexFiles();
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
