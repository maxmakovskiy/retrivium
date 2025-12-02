package ch.heigvd.dai.retrivium.cmd;

import picocli.CommandLine;

/**
 * Define the existing commands from the default cmd wish display either the client side or the server side
 */
@CommandLine.Command(
        description = "A small TCP-based search engine",
        version = "1.0.0",
        subcommands = {
            ClientCmd.class,
            ServerCmd.class,
        },
        scope = CommandLine.ScopeType.INHERIT,
        mixinStandardHelpOptions = true)
public class RootCmd {}
