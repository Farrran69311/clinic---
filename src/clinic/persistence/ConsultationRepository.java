package clinic.persistence;

import clinic.model.Consultation;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConsultationRepository {
    private static final String HEADER = "id|patientId|doctorId|appointmentId|summary|prescriptionId|createdAt";

    private final Path file;

    public ConsultationRepository(Path file) {
        this.file = file;
    }

    public List<Consultation> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<Consultation> consultations = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 7) {
                continue;
            }
            consultations.add(new Consultation(
                row[0],
                row[1],
                row[2],
                row[3],
                row[4],
                row[5],
                row[6].isEmpty() ? LocalDateTime.now() : LocalDateTime.parse(row[6])
            ));
        }
        return consultations;
    }

    public void save(Consultation consultation) throws IOException {
        List<Consultation> consultations = findAll();
        boolean updated = false;
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getId().equals(consultation.getId())) {
                consultations.set(i, consultation);
                updated = true;
                break;
            }
        }
        if (!updated) {
            consultations.add(consultation);
        }
        write(consultations);
    }

    public void deleteById(String id) throws IOException {
        List<Consultation> consultations = findAll();
        consultations.removeIf(c -> c.getId().equals(id));
        write(consultations);
    }

    private void write(List<Consultation> consultations) throws IOException {
        List<String[]> rows = new ArrayList<>(consultations.size());
        for (Consultation consultation : consultations) {
            rows.add(new String[]{
                consultation.getId(),
                consultation.getPatientId(),
                consultation.getDoctorId(),
                consultation.getAppointmentId() == null ? "" : consultation.getAppointmentId(),
                consultation.getSummary(),
                consultation.getPrescriptionId() == null ? "" : consultation.getPrescriptionId(),
                consultation.getCreatedAt().toString()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }
}
