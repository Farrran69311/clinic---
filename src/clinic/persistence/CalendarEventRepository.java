package clinic.persistence;

import clinic.model.CalendarEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CalendarEventRepository {
    private static final String HEADER = "id|title|start|end|relatedPatientId|ownerDoctorId|location|notes";

    private final Path file;

    public CalendarEventRepository(Path file) {
        this.file = file;
    }

    public List<CalendarEvent> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<CalendarEvent> events = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 8) {
                continue;
            }
            events.add(new CalendarEvent(
                row[0],
                row[1],
                row[2].isEmpty() ? null : LocalDateTime.parse(row[2]),
                row[3].isEmpty() ? null : LocalDateTime.parse(row[3]),
                row[4],
                row[5],
                row[6],
                row[7]
            ));
        }
        return events;
    }

    public List<CalendarEvent> findByOwner(String doctorId) throws IOException {
        return findAll().stream()
            .filter(e -> doctorId == null || doctorId.equals(e.getOwnerDoctorId()))
            .collect(Collectors.toList());
    }

    public void save(CalendarEvent event) throws IOException {
        List<CalendarEvent> events = findAll();
        boolean updated = false;
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getId().equals(event.getId())) {
                events.set(i, event);
                updated = true;
                break;
            }
        }
        if (!updated) {
            events.add(event);
        }
        write(events);
    }

    public void deleteById(String id) throws IOException {
        List<CalendarEvent> events = findAll();
        events.removeIf(e -> e.getId().equals(id));
        write(events);
    }

    private void write(List<CalendarEvent> events) throws IOException {
        List<String[]> rows = new ArrayList<>(events.size());
        for (CalendarEvent event : events) {
            rows.add(new String[]{
                event.getId(),
                event.getTitle(),
                event.getStart() == null ? "" : event.getStart().toString(),
                event.getEnd() == null ? "" : event.getEnd().toString(),
                event.getRelatedPatientId() == null ? "" : event.getRelatedPatientId(),
                event.getOwnerDoctorId() == null ? "" : event.getOwnerDoctorId(),
                event.getLocation() == null ? "" : event.getLocation(),
                event.getNotes()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }
}
