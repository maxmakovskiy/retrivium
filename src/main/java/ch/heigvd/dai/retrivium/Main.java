package ch.heigvd.dai.retrivium;

import ch.heigvd.dai.retrivium.cmd.RootCmd;
import java.io.File;
import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        String jarFilename =
                new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath())
                        .getName();

        RootCmd root = new RootCmd();

        int exitCode =
                new CommandLine(root)
                        .setCommandName(jarFilename)
                        .setCaseInsensitiveEnumValuesAllowed(true)
                        .execute(args);

        System.exit(exitCode);
    }
}
