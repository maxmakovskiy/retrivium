package ch.heigvd.dai.retrivium.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FileUtils {
    public static String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();

        try (FileReader fileReader = new FileReader(file.getPath(), StandardCharsets.UTF_8);
                BufferedReader fileBuf = new BufferedReader(fileReader)) {
            int c;
            while ((c = fileBuf.read()) != -1) {
                content.append((char) c);
            }
        }

        return content.toString();
    }
}
