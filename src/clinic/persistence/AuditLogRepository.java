package clinic.persistence;

import clinic.model.AuditLog;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogRepository {
    private static final String HEADER = "id|timestamp|userId|role|action|entityType|entityId|detail|result|ipAddress";

    private final Path file;

    public AuditLogRepository(Path file) {
        this.file = file;
    }

    public List<AuditLog> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<AuditLog> logs = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 10) {
                continue;
            }
            logs.add(new AuditLog(
                row[0],
                row[1].isEmpty() ? null : LocalDateTime.parse(row[1]),
                row[2],
                row[3],
                row[4],
                row[5],
                row[6],
                row[7],
                row[8],
                row[9]
            ));
        }
        return logs;
    }

    public void append(AuditLog log) throws IOException {
        List<AuditLog> logs = findAll();
        logs.add(log);
        write(logs);
    }

    private void write(List<AuditLog> logs) throws IOException {
        List<String[]> rows = new ArrayList<>(logs.size());
        for (AuditLog log : logs) {
            rows.add(new String[]{
                log.getId(),
                log.getTimestamp().toString(),
                log.getUserId() == null ? "" : log.getUserId(),
                log.getRole() == null ? "" : log.getRole(),
                log.getAction() == null ? "" : log.getAction(),
                log.getEntityType() == null ? "" : log.getEntityType(),
                log.getEntityId() == null ? "" : log.getEntityId(),
                log.getDetail(),
                log.getResult(),
                log.getIpAddress() == null ? "" : log.getIpAddress()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }
}
