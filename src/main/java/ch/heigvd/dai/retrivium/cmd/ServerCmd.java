package ch.heigvd.dai.retrivium.cmd;

import picocli.CommandLine;

import java.util.concurrent.Callable;


@CommandLine.Command(
        name = "server",
        description = "Start the server which runs search engine")
public class ServerCmd implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to use (default: ${DEFAULT-VALUE}).",
            defaultValue = "6433")
    protected int port;

    @Override
    public Integer call() {
        System.out.println("Starting server on localhost:" + port);
        return 0;
    }
}
