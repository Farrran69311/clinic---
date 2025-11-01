package clinic.persistence;

import clinic.model.Prescription;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionRepository {
    private static final String HEADER = "id|consultationId|medicineId|quantity|usage|status";

    private final Path file;

    public PrescriptionRepository(Path file) {
        this.file = file;
    }

    public List<Prescription> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<Prescription> prescriptions = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 6) {
                continue;
            }
            prescriptions.add(new Prescription(
                row[0],
                row[1],
                row[2],
                row[3].isEmpty() ? 0 : Integer.parseInt(row[3]),
                row[4],
                row[5]
            ));
        }
        return prescriptions;
    }

    public void save(Prescription prescription) throws IOException {
        List<Prescription> prescriptions = findAll();
        boolean updated = false;
        for (int i = 0; i < prescriptions.size(); i++) {
            if (prescriptions.get(i).getId().equals(prescription.getId())) {
                prescriptions.set(i, prescription);
                updated = true;
                break;
            }
        }
        if (!updated) {
            prescriptions.add(prescription);
        }
        write(prescriptions);
    }

    public void deleteById(String id) throws IOException {
        List<Prescription> prescriptions = findAll();
        prescriptions.removeIf(p -> p.getId().equals(id));
        write(prescriptions);
    }

    private void write(List<Prescription> prescriptions) throws IOException {
        List<String[]> rows = new ArrayList<>(prescriptions.size());
        for (Prescription prescription : prescriptions) {
            rows.add(new String[]{
                prescription.getId(),
                prescription.getConsultationId(),
                prescription.getMedicineId(),
                Integer.toString(prescription.getQuantity()),
                prescription.getUsage(),
                prescription.getStatus()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }
}
