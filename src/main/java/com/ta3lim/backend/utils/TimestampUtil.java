package com.ta3lim.backend.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimestampUtil {

    private static final String FILE_PATH = "C:\\dumps\\lastRunPushDbDumpToCloudTime.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void initializeTimestampFile() {
        if (!Files.exists(Paths.get(FILE_PATH))) {
            writeTimestamp(LocalDateTime.now()); // Initialize with the current timestamp
        }
    }

    public static LocalDateTime readTimestamp() {
        try {
            if (!Files.exists(Paths.get(FILE_PATH))) {
                writeTimestamp(LocalDateTime.now());
            }
            String content = new String(Files.readAllBytes(Paths.get(FILE_PATH)));
            return LocalDateTime.parse(content, FORMATTER);
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void writeTimestamp(LocalDateTime timestamp) {
        try {
            Files.write(Paths.get(FILE_PATH), timestamp.format(FORMATTER).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
