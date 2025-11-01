package clinic.persistence;

import clinic.model.MeetingMinute;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MeetingMinuteRepository {
    private static final String HEADER = "id|sessionId|recordedAt|authorDoctorId|summary|actionItems";

    private final Path file;

    public MeetingMinuteRepository(Path file) {
        this.file = file;
    }

    public List<MeetingMinute> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<MeetingMinute> minutes = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 2) {
                continue;
            }
            String id = row[0];
            String sessionId = row[1];
            String raw2 = valueAt(row, 2);
            String raw3 = valueAt(row, 3);
            String raw4 = valueAt(row, 4);
            String raw5 = valueAt(row, 5);

            LocalDateTime recordedAt = parseDateTime(raw2);
            String authorDoctorId = raw3;
            String summary = raw4;
            String actionItems = raw5;

            if (recordedAt == null && !raw3.isEmpty()) {
                LocalDateTime fallback = parseDateTime(raw3);
                if (fallback != null) {
                    recordedAt = fallback;
                    authorDoctorId = raw2;
                    summary = raw4;
                    actionItems = raw5;
                }
            }

            minutes.add(new MeetingMinute(
                id,
                sessionId,
                recordedAt,
                authorDoctorId,
                summary,
                actionItems
            ));
        }
        return minutes;
    }

    public List<MeetingMinute> findBySession(String sessionId) throws IOException {
        return findAll().stream()
            .filter(m -> m.getSessionId().equals(sessionId))
            .collect(Collectors.toList());
    }

    public void save(MeetingMinute minute) throws IOException {
        List<MeetingMinute> minutes = findAll();
        boolean updated = false;
        for (int i = 0; i < minutes.size(); i++) {
            if (minutes.get(i).getId().equals(minute.getId())) {
                minutes.set(i, minute);
                updated = true;
                break;
            }
        }
        if (!updated) {
            minutes.add(minute);
        }
        write(minutes);
    }

    public void deleteById(String id) throws IOException {
        List<MeetingMinute> minutes = findAll();
        minutes.removeIf(m -> m.getId().equals(id));
        write(minutes);
    }

    private void write(List<MeetingMinute> minutes) throws IOException {
        List<String[]> rows = new ArrayList<>(minutes.size());
        for (MeetingMinute minute : minutes) {
            rows.add(new String[]{
                minute.getId(),
                minute.getSessionId(),
                minute.getRecordedAt() == null ? "" : minute.getRecordedAt().toString(),
                minute.getAuthorDoctorId() == null ? "" : minute.getAuthorDoctorId(),
                minute.getSummary(),
                minute.getActionItems()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }

    private String valueAt(String[] row, int index) {
        return index >= 0 && index < row.length ? row[index] : "";
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
