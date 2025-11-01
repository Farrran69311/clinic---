package clinic.persistence;

import clinic.model.Patient;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PatientRepository {
    private static final String HEADER = "id|name|gender|birthday|phone|address|emergencyContact|notes";

    private final Path file;

    public PatientRepository(Path file) {
        this.file = file;
    }

    public List<Patient> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<Patient> patients = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 8) {
                continue;
            }
            patients.add(new Patient(
                row[0],
                row[1],
                row[2],
                row[3].isEmpty() ? null : LocalDate.parse(row[3]),
                row[4],
                row[5],
                row[6],
                row[7]
            ));
        }
        return patients;
    }

    public void save(Patient patient) throws IOException {
        List<Patient> patients = findAll();
        boolean updated = false;
        for (int i = 0; i < patients.size(); i++) {
            if (patients.get(i).getId().equals(patient.getId())) {
                patients.set(i, patient);
                updated = true;
                break;
            }
        }
        if (!updated) {
            patients.add(patient);
        }
        write(patients);
    }

    public void deleteById(String id) throws IOException {
        List<Patient> patients = findAll();
        patients.removeIf(p -> p.getId().equals(id));
        write(patients);
    }

    private void write(List<Patient> patients) throws IOException {
        List<String[]> rows = new ArrayList<>(patients.size());
        for (Patient patient : patients) {
            rows.add(new String[]{
                patient.getId(),
                patient.getName(),
                patient.getGender() == null ? "" : patient.getGender(),
                patient.getBirthday() == null ? "" : patient.getBirthday().toString(),
                patient.getPhone() == null ? "" : patient.getPhone(),
                patient.getAddress() == null ? "" : patient.getAddress(),
                patient.getEmergencyContact() == null ? "" : patient.getEmergencyContact(),
                patient.getNotes() == null ? "" : patient.getNotes()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }
}
