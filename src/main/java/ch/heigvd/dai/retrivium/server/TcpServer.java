package ch.heigvd.dai.retrivium.server;

import ch.heigvd.dai.bm25.BM25;
import ch.heigvd.dai.bm25.utils.RankingResult;
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

    private boolean isFileIndexed(String filename) {
        for (int i = 0; i < bm25.getIndex().getNumOfDocs(); i++) {
            if (filename.equals(bm25.getIndex().getDocumentName(i))) {
                return true;
            }
        }
        return false;
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

                                String[] docNames = new String[bm25.getIndex().getNumOfDocs()];
                                for (int i = 0; i < docNames.length; i++) {
                                    docNames[i] = bm25.getIndex().getDocumentName(i);
                                }

                                if (docNames.length == 0) {
                                    response = ServerMessage.NOTHING_INDEXED.name();
                                } else {
                                    response =
                                            ServerMessage.FILES.name()
                                                    + " "
                                                    + String.join(" ", docNames);
                                }

                            }
                            case QUERY -> {
                                String[] payload = clientRequestParts[1].split(" ", 2);

                                if (payload.length != 2) {
                                    response = ServerMessage.INVALID.name();
                                    System.out.println("[Server] query payload is ill-formed");
                                    break;
                                }

                                try {
                                    int topK = Integer.parseInt(payload[0]);
                                    String query = payload[1];

                                    if (topK > 0 && !query.isEmpty()) {
                                        ArrayList<RankingResult> results =
                                                bm25.retrieveTopK(bm25.tokenize(query), topK);
                                        String[] filenames = new String[results.size()];

                                        for (int i = 0; i < filenames.length; i++) {
                                            int docIdx = results.get(i).getDocIndex();
                                            filenames[i] = bm25.getIndex().getDocumentName(docIdx);
                                        }

                                        if (results.isEmpty()) {
                                            response = ServerMessage.NOTHING_RELEVANT.name();
                                        } else {
                                            response =
                                                    String.format(
                                                            "%s %s",
                                                            ServerMessage.RELEVANT,
                                                            String.join(" ", filenames));
                                        }

                                    } else {
                                        response = ServerMessage.INVALID.name();
                                        System.out.println("[Server] query is empty or <k> is not positive");
                                    }
                                } catch (NumberFormatException e) {
                                    response = ServerMessage.INVALID.name();
                                    System.out.println("[Server] query does not contain <k>");
                                }
                            }
                            case SHOW -> {
                                String filename = clientRequestParts[1];

                                if (isFileIndexed(filename)) {
                                    File targetFile = new File(targetDir, filename);

                                    StringBuilder content = new StringBuilder();
                                    try (FileReader fileReader =
                                                    new FileReader(
                                                            targetFile, StandardCharsets.UTF_8);
                                            BufferedReader fileBuf =
                                                    new BufferedReader(fileReader)) {
                                        int c;
                                        while ((c = fileBuf.read()) != -1) {
                                            content.append((char) c);
                                        }
                                    } catch (IOException e) {
                                        System.out.println(
                                                "Impossible to read : " + targetFile.getPath());
                                    }

                                    response =
                                            String.format(
                                                    "%s %s", ServerMessage.CONTENT.name(), content);
                                } else {
                                    response = ServerMessage.FILE_DOESNT_EXIST.name();
                                }

                            }
                            case UPLOAD -> {
                                String[] payload = clientRequestParts[1].split(" ", 2);
                                String docName = payload[0];
                                String doc = payload[1];

                                File newFile = new File(targetDir, docName);
                                Writer fileWriter = new FileWriter(newFile, StandardCharsets.UTF_8);
                                BufferedWriter bufDoc = new BufferedWriter(fileWriter);

                                bufDoc.write(doc);
                                bufDoc.flush();
                                bufDoc.close();

                                indexFiles();
                                response = ServerMessage.UPLOADED.name() + " " + docName;
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
