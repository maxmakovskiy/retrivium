package ch.heigvd.dai.retrivium.server;

import ch.heigvd.dai.bm25.BM25;
import ch.heigvd.dai.bm25.utils.RankingResult;
import ch.heigvd.dai.retrivium.client.ClientMessage;
import ch.heigvd.dai.retrivium.utils.FileUtils;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServer {
    private final int port;
    private final char lineFeed;
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
    }

    public void launch() {
        try (ServerSocket serverSocket = new ServerSocket(port);
                ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            System.out.println("[Server] Listening on port " + port);
            while (!serverSocket.isClosed()) {
                System.out.println("[Server] Waiting for incoming connection...");
                Socket clientSocket = serverSocket.accept();
                executor.submit(new ClientHandler(clientSocket, targetDir, lineFeed));
                System.out.println(
                        "[Server] Accepted connection from "
                                + clientSocket.getInetAddress().getHostName());
            }
        } catch (IOException e) {
            System.err.println("[Server] error : " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final char lineFeed;
        private final File targetDir;
        private final BM25 bm25;

        public ClientHandler(Socket clientSocket, File targetDir, char lineFeed) {
            this.clientSocket = clientSocket;
            this.lineFeed = lineFeed;
            this.targetDir = targetDir;
            bm25 = new BM25();
        }

        private boolean isFileIndexed(String filename) {
            for (int i = 0; i < bm25.getIndex().getNumOfDocs(); i++) {
                if (filename.equals(bm25.getIndex().getDocumentName(i))) {
                    return true;
                }
            }
            return false;
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
                    try {
                        String content = FileUtils.readFile(file);
                        docs.add(content);
                    } catch (IOException e) {
                        System.out.println("[Server] Cannot read a file " + file.getName());
                        System.out.println("Skipping");
                    }
                }
            }

            bm25.buildIndex(bm25.tokenize(docs), docNames);
        }

        @Override
        public void run() {
            try (clientSocket;
                    BufferedReader br =
                            new BufferedReader(
                                    new InputStreamReader(
                                            clientSocket.getInputStream(),
                                            StandardCharsets.UTF_8));
                    BufferedWriter bw =
                            new BufferedWriter(
                                    new OutputStreamWriter(
                                            clientSocket.getOutputStream(),
                                            StandardCharsets.UTF_8))) {

                String clientInfo =
                        clientSocket.getInetAddress().getHostAddress()
                                + " : "
                                + clientSocket.getPort();

                System.out.println("[Server] Client connected from " + clientInfo);

                System.out.printf(
                        "[Server] [%s] Indexing documents : %s", clientInfo, targetDir.getPath());
                indexFiles();

                while (!clientSocket.isClosed()) {
                    String clientRequest = br.readLine();

                    if (clientRequest == null) {
                        System.out.printf("[Server] Client %s disconnected", clientInfo);
                        break;
                    }
                    System.out.println(
                            "[Server] received command "
                                    + clientRequest
                                    + " from client : "
                                    + clientInfo);

                    String[] clientRequestParts =
                            clientRequest.split(" ", 2); // Cannot resolve symbol 'clientRequest'

                    ClientMessage command = null;
                    try {
                        command = ClientMessage.valueOf(clientRequestParts[0]);
                    } catch (Exception e) {
                        System.err.printf("[Server] [%s] Error: %s", clientInfo, e.getMessage());
                    }

                    String response = null;

                    switch (command) {
                        case LIST -> {
                            // access to the files
                            String[] fileNames = new String[bm25.getIndex().getNumOfDocs()];
                            // store files name into array

                            for (int i = 0; i < fileNames.length; i++) {
                                fileNames[i] = bm25.getIndex().getDocumentName(i);
                            }

                            if (fileNames.length == 0) {
                                response = ServerMessage.NOTHING_INDEXED.name();
                            } else {
                                response =
                                        ServerMessage.FILES.name()
                                                + " "
                                                + String.join(" ", fileNames);
                            }

                            System.out.println(
                                    "[Server] Sending list of documents to " + clientInfo);
                        }

                        case SHOW -> {
                            String filename = clientRequestParts[1];

                            if (isFileIndexed(filename)) {
                                File targetFile = new File(targetDir, filename);

                                StringBuilder content = new StringBuilder();
                                try (FileReader fileReader =
                                                new FileReader(targetFile, StandardCharsets.UTF_8);
                                        BufferedReader fileBuf = new BufferedReader(fileReader)) {
                                    int c;
                                    while ((c = fileBuf.read()) != -1) {
                                        content.append((char) c);
                                    }
                                } catch (IOException e) {
                                    System.out.println(
                                            "[Server] Impossible to read : "
                                                    + targetFile.getPath()
                                                    + " for "
                                                    + clientInfo);
                                }

                                response =
                                        String.format(
                                                "%s %s", ServerMessage.CONTENT.name(), content);
                            } else {
                                response = ServerMessage.FILE_DOESNT_EXIST.name();
                            }

                            System.out.println("[Server] Sending show document to " + clientInfo);
                        }

                        case QUERY -> {
                            String[] payload = clientRequestParts[1].split(" ", 2);

                            if (payload.length != 2) {
                                response = ServerMessage.INVALID.name();
                                System.out.println(
                                        "[Server] query payload is ill-formed from " + clientInfo);
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
                                    System.out.println(
                                            "[Server] ["
                                                    + clientInfo
                                                    + "] query is empty or <k> is not positive");
                                }
                            } catch (NumberFormatException e) {
                                response = ServerMessage.INVALID.name();
                                System.out.println(
                                        "[Server] [" + clientInfo + "] query does not contain <k>");
                            }

                            System.out.println("[Server] Sending query document to " + clientInfo);
                        }

                        case UPLOAD -> {
                            String[] payload = clientRequestParts[1].split(" ", 2);
                            String docName = payload[0];
                            String doc = payload[1];
                            try {

                                File newFile = new File(targetDir, docName);
                                try (Writer fileWriter =
                                                new FileWriter(newFile, StandardCharsets.UTF_8);
                                        BufferedWriter bufDoc = new BufferedWriter(fileWriter)) {
                                    bufDoc.write(doc);
                                }

                                indexFiles();
                                response = ServerMessage.UPLOADED.name() + " " + docName;
                                System.out.println(
                                        "[Server] Sending upload document "
                                                + docName
                                                + " to "
                                                + clientInfo);

                            } catch (IOException e) {
                                System.err.println("[Server] error : " + e.getMessage());
                            }
                        }

                        case QUIT -> {
                            System.out.println(
                                    "[Server] Client " + clientInfo + "requests disconnect");
                            System.out.println(
                                    "[Server] Disconnected Client "
                                            + clientSocket.getInetAddress().getHostAddress());
                        }

                        case null, default -> {
                            System.out.println(
                                    "[Server] Unknown command sent by client ["
                                            + clientInfo
                                            + "], reply with "
                                            + ServerMessage.INVALID
                                            + ".");
                            response =
                                    ServerMessage.INVALID + " Unknown command. Please try again.";
                        }
                    }

                    bw.write(response + lineFeed);
                    bw.flush();
                    System.out.println(
                            "[Server] Sent response to client : "
                                    + clientSocket.getInetAddress().getHostAddress());
                }

            } catch (IOException e) {
                System.err.println("[Server] IO exception: " + e.getMessage());
                System.out.println("[Server] Terminating ...");
            }
        }
    }
}
