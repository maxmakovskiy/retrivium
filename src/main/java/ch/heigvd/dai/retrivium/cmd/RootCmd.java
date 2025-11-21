package ch.heigvd.dai.retrivium.cmd;

import picocli.CommandLine;

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
