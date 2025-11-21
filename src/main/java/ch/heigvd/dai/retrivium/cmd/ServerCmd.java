package ch.heigvd.dai.retrivium.cmd;

import ch.heigvd.dai.retrivium.server.TcpServer;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "server", description = "Start the server which runs search engine")
public class ServerCmd implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to use (default: ${DEFAULT-VALUE}).",
            defaultValue = "6433")
    protected int port;

    @Override
    public Integer call() {
        System.out.println("Starting server ...");

        TcpServer server = new TcpServer(port, '\n');
        server.launch();

        return 0;
    }
}
