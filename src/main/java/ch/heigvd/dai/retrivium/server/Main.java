package ch.heigvd.dai.retrivium.server;

public class Main {
    public static void main(String[] args) {
        TcpServer server = new TcpServer(1234, '\n');
        server.launch();
    }
}
