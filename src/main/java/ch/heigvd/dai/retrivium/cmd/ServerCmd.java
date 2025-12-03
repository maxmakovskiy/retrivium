package ch.heigvd.dai.retrivium.cmd;

import ch.heigvd.dai.retrivium.server.TcpServer;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/** PicoCLI class that represents server CLI */
@CommandLine.Command(name = "server", description = "Start the server which runs search engine")
public class ServerCmd implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to use (default: ${DEFAULT-VALUE}).",
            defaultValue = "6433")
    protected int port;

    @CommandLine.Option(
            names = {"-D", "--data-directory"},
            description = "Directory that contains documents that will be used for search")
    protected File dataDir;

    @Override
    public Integer call() {
        System.out.println("Starting server ...");

        TcpServer server = new TcpServer(port, dataDir, '\n');
        server.launch();

        return 0;
    }
}
