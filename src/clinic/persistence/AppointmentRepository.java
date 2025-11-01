package clinic.persistence;

import clinic.model.Appointment;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AppointmentRepository {
    private static final String HEADER = "id|patientId|doctorId|datetime|status|notes";

    private final Path file;

    public AppointmentRepository(Path file) {
        this.file = file;
    }

    public List<Appointment> findAll() throws IOException {
        List<String[]> rows = CsvDataStore.readRecords(file);
        List<Appointment> appointments = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length < 6) {
                continue;
            }
            appointments.add(new Appointment(
                row[0],
                row[1],
                row[2],
                row[3].isEmpty() ? LocalDateTime.now() : LocalDateTime.parse(row[3]),
                row[4],
                row[5]
            ));
        }
        return appointments;
    }

    public void save(Appointment appointment) throws IOException {
        List<Appointment> appointments = findAll();
        boolean updated = false;
        for (int i = 0; i < appointments.size(); i++) {
            if (appointments.get(i).getId().equals(appointment.getId())) {
                appointments.set(i, appointment);
                updated = true;
                break;
            }
        }
        if (!updated) {
            appointments.add(appointment);
        }
        write(appointments);
    }

    public void deleteById(String id) throws IOException {
        List<Appointment> appointments = findAll();
        appointments.removeIf(a -> a.getId().equals(id));
        write(appointments);
    }

    private void write(List<Appointment> appointments) throws IOException {
        List<String[]> rows = new ArrayList<>(appointments.size());
        for (Appointment appointment : appointments) {
            rows.add(new String[]{
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getDoctorId(),
                appointment.getDateTime().toString(),
                appointment.getStatus(),
                appointment.getNotes()
            });
        }
        CsvDataStore.writeRecords(file, HEADER, rows);
    }
}
