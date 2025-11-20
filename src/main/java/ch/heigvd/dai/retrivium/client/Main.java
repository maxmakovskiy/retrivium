package ch.heigvd.dai.retrivium.client;

public class Main {
    public static void main(String[] args) {
        TcpReplClient client = new TcpReplClient("localhost", 1234, '\n');
        client.launch();
    }
}
