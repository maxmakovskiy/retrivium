package ch.heigvd.dai.retrivium.cmd;

import ch.heigvd.dai.retrivium.client.TcpReplClient;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "client", description = "Start the repl-based client.")
public class ClientCmd implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-H", "--host"},
            description = "Host to connect to.",
            required = true)
    protected String serverIP;

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to use (default: ${DEFAULT-VALUE}).",
            defaultValue = "6433")
    protected int port;

    @Override
    public Integer call() {
        TcpReplClient client = new TcpReplClient(serverIP, port, '\n');
        try {
            client.launch();
        } catch (RuntimeException e) {
            System.out.println("[Client] Terminating...");
            return 1;
        }

        return 0;
    }
}
