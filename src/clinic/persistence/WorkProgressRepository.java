package clinic.persistence;

import clinic.model.WorkProgress;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WorkProgressRepository {
    private static final String HEADER = "id|patientId|description|status|lastUpdated|ownerDoctorId";

    private final Path file;

    public WorkProgressRepository(Path file) {
        this.file = file;
    }

    public List<WorkProgress> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<WorkProgress> progresses = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 6) {
                continue;
            }
            progresses.add(new WorkProgress(
                row[0],
                row[1],
                row[2],
                row[3],
                row[4].isEmpty() ? null : LocalDate.parse(row[4]),
                row[5]
            ));
        }
        return progresses;
    }

    public List<WorkProgress> findByPatient(String patientId) throws IOException {
        return findAll().stream()
            .filter(p -> p.getPatientId().equals(patientId))
            .collect(Collectors.toList());
    }

    public void save(WorkProgress progress) throws IOException {
        List<WorkProgress> progresses = findAll();
        boolean updated = false;
        for (int i = 0; i < progresses.size(); i++) {
            if (progresses.get(i).getId().equals(progress.getId())) {
                progresses.set(i, progress);
                updated = true;
                break;
            }
        }
        if (!updated) {
            progresses.add(progress);
        }
        write(progresses);
    }

    public void deleteById(String id) throws IOException {
        List<WorkProgress> progresses = findAll();
        progresses.removeIf(p -> p.getId().equals(id));
        write(progresses);
    }

    private void write(List<WorkProgress> progresses) throws IOException {
        List<String[]> rows = new ArrayList<>(progresses.size());
        for (WorkProgress progress : progresses) {
            rows.add(new String[]{
                progress.getId(),
                progress.getPatientId(),
                progress.getDescription(),
                progress.getStatus(),
                progress.getLastUpdated() == null ? "" : progress.getLastUpdated().toString(),
                progress.getOwnerDoctorId() == null ? "" : progress.getOwnerDoctorId()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }
}
