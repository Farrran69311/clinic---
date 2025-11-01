package clinic.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CsvDataStore {
    private CsvDataStore() {
    }

    public static List<String[]> readRecords(Path file) throws IOException {
        ensureFile(file);
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String[]> records = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            records.add(line.split("\\|", -1));
        }
        return records;
    }

    public static void writeRecords(Path file, String header, List<String[]> records) throws IOException {
        ensureFile(file);
        List<String> lines = new ArrayList<>(records.size() + 1);
        lines.add(header);
        for (String[] record : records) {
            lines.add(String.join("|", record));
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    public static String randomId() {
        return UUID.randomUUID().toString();
    }

    public static String timestamp() {
        return LocalDateTime.now().toString();
    }

    private static void ensureFile(Path file) throws IOException {
        if (Files.notExists(file)) {
            Files.createDirectories(file.getParent());
            Files.createFile(file);
        }
    }
}
