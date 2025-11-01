package clinic.persistence;

import clinic.model.ExpertSession;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExpertSessionRepository {
    private static final String HEADER = "id|title|hostDoctorId|scheduledAt|status|meetingUrl|notes";

    private final Path file;

    public ExpertSessionRepository(Path file) {
        this.file = file;
    }

    public List<ExpertSession> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<ExpertSession> sessions = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 7) {
                continue;
            }
            sessions.add(new ExpertSession(
                row[0],
                row[1],
                row[2],
                row[3].isEmpty() ? null : LocalDateTime.parse(row[3]),
                row[4],
                row[5],
                row[6]
            ));
        }
        return sessions;
    }

    public void save(ExpertSession session) throws IOException {
        List<ExpertSession> sessions = findAll();
        boolean updated = false;
        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i).getId().equals(session.getId())) {
                sessions.set(i, session);
                updated = true;
                break;
            }
        }
        if (!updated) {
            sessions.add(session);
        }
        write(sessions);
    }

    public void deleteById(String id) throws IOException {
        List<ExpertSession> sessions = findAll();
        sessions.removeIf(s -> s.getId().equals(id));
        write(sessions);
    }

    private void write(List<ExpertSession> sessions) throws IOException {
        List<String[]> rows = new ArrayList<>(sessions.size());
        for (ExpertSession session : sessions) {
            rows.add(new String[]{
                session.getId(),
                session.getTitle(),
                session.getHostDoctorId(),
                session.getScheduledAt() == null ? "" : session.getScheduledAt().toString(),
                session.getStatus(),
                session.getMeetingUrl() == null ? "" : session.getMeetingUrl(),
                session.getNotes()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }
}
