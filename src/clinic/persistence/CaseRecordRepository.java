package clinic.persistence;

import clinic.model.CaseRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CaseRecordRepository {
    private static final String HEADER = "id|patientId|title|summary|tags|attachment";

    private final Path file;

    public CaseRecordRepository(Path file) {
        this.file = file;
    }

    public List<CaseRecord> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<CaseRecord> records = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 6) {
                continue;
            }
            records.add(new CaseRecord(row[0], row[1], row[2], row[3], row[4], row[5]));
        }
        return records;
    }

    public List<CaseRecord> findByPatient(String patientId) throws IOException {
        return findAll().stream()
            .filter(r -> r.getPatientId().equals(patientId))
            .collect(Collectors.toList());
    }

    public void save(CaseRecord record) throws IOException {
        List<CaseRecord> records = findAll();
        boolean updated = false;
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).getId().equals(record.getId())) {
                records.set(i, record);
                updated = true;
                break;
            }
        }
        if (!updated) {
            records.add(record);
        }
        write(records);
    }

    public void deleteById(String id) throws IOException {
        List<CaseRecord> records = findAll();
        records.removeIf(r -> r.getId().equals(id));
        write(records);
    }

    private void write(List<CaseRecord> records) throws IOException {
        List<String[]> rows = new ArrayList<>(records.size());
        for (CaseRecord record : records) {
            rows.add(new String[]{
                record.getId(),
                record.getPatientId(),
                record.getTitle(),
                record.getSummary(),
                record.getTags(),
                record.getAttachmentPath() == null ? "" : record.getAttachmentPath()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }
}
