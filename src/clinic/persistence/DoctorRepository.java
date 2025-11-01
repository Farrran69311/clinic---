package clinic.persistence;

import clinic.model.Doctor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DoctorRepository {
    private static final String HEADER = "id|name|department|phone|schedule|rating|title|level|specialties";

    private final Path file;

    public DoctorRepository(Path file) {
        this.file = file;
    }

    public List<Doctor> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<Doctor> doctors = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 5) {
                continue;
            }
            Double rating = parseRating(row, 5);
            doctors.add(new Doctor(
                row[0],
                row[1],
                valueOrNull(row, 2),
                valueOrNull(row, 3),
                valueOrNull(row, 4),
                rating,
                valueOrNull(row, 6),
                valueOrNull(row, 7),
                valueOrNull(row, 8)
            ));
        }
        return doctors;
    }

    public void save(Doctor doctor) throws IOException {
        List<Doctor> doctors = findAll();
        boolean updated = false;
        for (int i = 0; i < doctors.size(); i++) {
            if (doctors.get(i).getId().equals(doctor.getId())) {
                doctors.set(i, doctor);
                updated = true;
                break;
            }
        }
        if (!updated) {
            doctors.add(doctor);
        }
        write(doctors);
    }

    public void deleteById(String id) throws IOException {
        List<Doctor> doctors = findAll();
        doctors.removeIf(d -> d.getId().equals(id));
        write(doctors);
    }

    private void write(List<Doctor> doctors) throws IOException {
        List<String[]> rows = new ArrayList<>(doctors.size());
        for (Doctor doctor : doctors) {
            rows.add(new String[]{
                doctor.getId(),
                doctor.getName(),
                optionalString(doctor.getDepartment()),
                optionalString(doctor.getPhone()),
                optionalString(doctor.getSchedule()),
                doctor.getRating() == null ? "" : String.format(Locale.ROOT, "%.1f", doctor.getRating()),
                optionalString(doctor.getTitle()),
                optionalString(doctor.getLevel()),
                optionalString(doctor.getSpecialties())
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }

    private String optionalString(String value) {
        return value == null ? "" : value;
    }

    private String valueOrNull(String[] row, int index) {
        if (row.length <= index) {
            return null;
        }
        String value = row[index];
        return value == null || value.isBlank() ? null : value;
    }

    private Double parseRating(String[] row, int index) {
        if (row.length <= index) {
            return null;
        }
        String value = row[index];
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
